package com.psicology.backend.testapi;

import com.psicology.backend.domain.test.*;
import com.psicology.backend.domain.user.User;
import com.psicology.backend.domain.user.UserRepository;
import com.psicology.backend.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class TestController {
    private final PsychTestRepository testRepo;
    private final TestQuestionRepository questionRepo;
    private final TestOptionRepository optionRepo;
    private final TestAttemptRepository attemptRepo;
    private final TestAnswerRepository answerRepo;
    private final UserRepository userRepository;

    public TestController(PsychTestRepository testRepo, TestQuestionRepository questionRepo, TestOptionRepository optionRepo, TestAttemptRepository attemptRepo, TestAnswerRepository answerRepo, UserRepository userRepository) {
        this.testRepo = testRepo;
        this.questionRepo = questionRepo;
        this.optionRepo = optionRepo;
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
        this.userRepository = userRepository;
    }

    public record TestSummary(UUID id, String type, String title, String description, int questionCount) {}
    public record OptionDto(UUID id, String label, int value) {}
    public record QuestionDto(UUID id, int position, String prompt, String kind, List<OptionDto> options) {}
    public record TestDetail(UUID id, String type, String title, String description, List<QuestionDto> questions) {}
    public record AnswerReq(UUID questionId, UUID optionId, String textAnswer) {}
    public record AttemptDto(UUID id, UUID testId, String testTitle, String status, Integer score, Integer maxScore, String resultSummary, String psychologistFeedback, Instant startedAt, Instant completedAt, List<AnswerView> answers) {}
    public record AnswerView(UUID questionId, UUID optionId, Integer value) {}

    @GetMapping("/tests")
    public List<TestSummary> listTests() {
        return testRepo.findByActiveTrueOrderByCreatedAtDesc().stream().map(t -> {
            int count = questionRepo.findByTestIdOrderByPositionAsc(t.getId()).size();
            return new TestSummary(t.getId(), t.getType(), t.getTitle(), t.getDescription(), count);
        }).toList();
    }

    @GetMapping("/tests/{id}")
    @Transactional(readOnly = true)
    public TestDetail getTest(@PathVariable UUID id) {
        PsychTest t = testRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<TestQuestion> qs = questionRepo.findByTestIdOrderByPositionAsc(id);
        List<QuestionDto> qdtos = qs.stream().map(q -> new QuestionDto(q.getId(), q.getPosition(), q.getPrompt(), q.getKind(),
                q.getOptions().stream().map(o -> new OptionDto(o.getId(), o.getLabel(), o.getValue())).toList())).toList();
        return new TestDetail(t.getId(), t.getType(), t.getTitle(), t.getDescription(), qdtos);
    }

    @PostMapping("/tests/{id}/attempts")
    @Transactional
    public AttemptDto startAttempt(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
        PsychTest test = testRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User patient = userRepository.findById(principal.getId()).orElseThrow();
        Optional<TestAttempt> existing = attemptRepo.findFirstByPatientIdAndTestIdAndStatusOrderByStartedAtDesc(patient.getId(), test.getId(), "IN_PROGRESS");
        TestAttempt attempt = existing.orElseGet(() -> {
            TestAttempt a = new TestAttempt();
            a.setTest(test);
            a.setPatient(patient);
            a.setStatus("IN_PROGRESS");
            return attemptRepo.save(a);
        });
        return toAttemptDto(attempt);
    }

    @GetMapping("/attempts/me")
    @Transactional(readOnly = true)
    public List<AttemptDto> myAttempts(@AuthenticationPrincipal AppUserDetails principal) {
        return attemptRepo.findByPatientIdOrderByStartedAtDesc(principal.getId()).stream().map(this::toAttemptDto).toList();
    }

    @GetMapping("/attempts/{id}")
    @Transactional(readOnly = true)
    public AttemptDto getAttempt(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
        TestAttempt a = attemptRepo.findByIdAndPatientId(id, principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toAttemptDto(a);
    }

    @PostMapping("/attempts/{id}/answers")
    @Transactional
    public AttemptDto saveAnswers(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id, @RequestBody List<AnswerReq> reqs) {
        TestAttempt attempt = attemptRepo.findByIdAndPatientId(id, principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if ("COMPLETED".equals(attempt.getStatus())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tentativa já finalizada");
        for (AnswerReq r : reqs) {
            if (r.questionId() == null) continue;
            TestQuestion q = questionRepo.findById(r.questionId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questão inválida"));
            if (!q.getTest().getId().equals(attempt.getTest().getId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questão não pertence ao teste");
            TestAnswer ans = answerRepo.findByAttemptIdAndQuestionId(attempt.getId(), q.getId()).orElse(new TestAnswer());
            ans.setAttempt(attempt);
            ans.setQuestion(q);
            ans.setTextAnswer(r.textAnswer());
            if (r.optionId() != null) {
                TestOption opt = optionRepo.findById(r.optionId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opção inválida"));
                ans.setOption(opt);
                ans.setValue(opt.getValue());
            } else {
                ans.setOption(null);
                ans.setValue(null);
            }
            answerRepo.save(ans);
        }
        return toAttemptDto(attempt);
    }

    @PostMapping("/attempts/{id}/submit")
    @Transactional
    public AttemptDto submit(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id, @RequestBody(required = false) List<AnswerReq> reqs) {
        if (reqs != null && !reqs.isEmpty()) saveAnswers(principal, id, reqs);
        TestAttempt attempt = attemptRepo.findByIdAndPatientId(id, principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if ("COMPLETED".equals(attempt.getStatus())) return toAttemptDto(attempt);
        List<TestAnswer> answers = answerRepo.findByAttemptId(attempt.getId());
        List<TestQuestion> questions = questionRepo.findByTestIdOrderByPositionAsc(attempt.getTest().getId());
        int maxScore = computeMaxScore(questions);
        int score = answers.stream().mapToInt(a -> a.getValue() != null ? a.getValue() : 0).sum();
        attempt.setScore(score);
        attempt.setMaxScore(maxScore);
        attempt.setStatus("COMPLETED");
        attempt.setCompletedAt(Instant.now());
        attempt.setResultSummary(buildSummary(attempt.getTest(), score, maxScore, answers.size(), questions.size()));
        attempt.setPsychologistFeedback(buildPsychologistFeedback(attempt.getTest(), score, maxScore, answers.size(), questions.size()));
        attemptRepo.save(attempt);
        return toAttemptDto(attempt);
    }

    private int computeMaxScore(List<TestQuestion> questions) {
        int total = 0;
        for (TestQuestion q : questions) {
            int qMax = q.getOptions() == null ? 0 : q.getOptions().stream().mapToInt(TestOption::getValue).max().orElse(0);
            total += Math.max(0, qMax);
        }
        return total;
    }

    private String buildSummary(PsychTest test, int score, int maxScore, int answered, int totalQuestions) {
        String range = maxScore > 0 ? " (" + score + "/" + maxScore + ")" : " (" + score + ")";
        if ("PSYCHOLOGICAL".equals(test.getType())) {
            double pct = maxScore > 0 ? (score * 100.0 / maxScore) : 0;
            if (pct >= 75) return "Bem-estar elevado no período avaliado" + range + ". Continue cuidando de seus hábitos e rotina.";
            if (pct >= 50) return "Bem-estar moderado" + range + ". Há pontos positivos e aspectos que merecem atenção.";
            return "Sinais de bem-estar reduzido" + range + ". Considere conversar com seu psicólogo sobre como você tem se sentido.";
        }
        if ("VOCATIONAL".equals(test.getType())) {
            return "Perfil vocacional exploratório registrado" + range + ", " + answered + "/" + totalQuestions + " respostas. Discuta o resultado com um profissional para orientação detalhada.";
        }
        return "Teste concluído. Pontuação: " + score + (maxScore > 0 ? "/" + maxScore : "") + ".";
    }

    private String buildPsychologistFeedback(PsychTest test, int score, int maxScore, int answered, int totalQuestions) {
        String instrument = test.getTitle() != null ? test.getTitle() : "Instrumento";
        String typeLabel = "PSYCHOLOGICAL".equals(test.getType()) ? "autoavaliação psicológica" : "VOCATIONAL".equals(test.getType()) ? "mapeamento vocacional exploratório" : "instrumento de apoio";
        double pct = maxScore > 0 ? (score * 100.0 / maxScore) : 0;
        String band;
        String clinicalNote;
        if ("PSYCHOLOGICAL".equals(test.getType())) {
            if (pct >= 75) { band = "Faixa alta/positiva"; clinicalNote = "Indícios de bem-estar preservado no recorte avaliado. Reforçar fatores de proteção, rotina e estratégias já eficazes."; }
            else if (pct >= 50) { band = "Faixa moderada/atenção"; clinicalNote = "Há sinais mistos. Vale explorar áreas de estresse, sono, suporte social e funcionamento diário nas sessões."; }
            else { band = "Faixa baixa/alerta clínico"; clinicalNote = "Sinais de sofrimento ou bem-estar reduzido. Priorizar escuta qualificada, avaliação de risco e plano de cuidado individualizado."; }
        } else if ("VOCATIONAL".equals(test.getType())) {
            if (pct >= 66) { band = "Interesses amplos/definidos"; clinicalNote = "O paciente apresenta respostas consistentes com interesses mais claros. Aprofundar valores, habilidades e contexto de vida para decisão profissional."; }
            else if (pct >= 33) { band = "Interesses em exploração"; clinicalNote = "Perfil indica exploração em andamento. Usar o resultado como ponto de partida para entrevista vocacional e investigação de áreas de interesse."; }
            else { band = "Interesses iniciais/pouco definidos"; clinicalNote = "Resultado sugere baixa definição no momento. Investigar barreiras, autoconhecimento, expectativas familiares e experiências prévias."; }
        } else {
            band = "Resultado registrado"; clinicalNote = "Interpretar junto ao histórico clínico e contexto atual do paciente.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Respaldo do teste: ").append(instrument).append(" (").append(typeLabel).append(").\n");
        if (test.getDescription() != null && !test.getDescription().isBlank()) sb.append("Objetivo: ").append(test.getDescription().trim()).append("\n");
        sb.append("Pontuação: ").append(score).append(maxScore > 0 ? "/" + maxScore : "").append(" (").append(String.format(Locale.forLanguageTag("pt-BR"), "%.0f", pct)).append("%)");
        sb.append(" | Respostas: ").append(answered).append("/").append(totalQuestions).append(".\n");
        sb.append("Faixa interpretativa: ").append(band).append(".\n");
        sb.append("Leitura para o psicólogo: ").append(clinicalNote).append("\n");
        sb.append("Observação ética: resultado de instrumento de apoio, não constitui diagnóstico isolado. Interpretar com entrevista clínica, história e contexto do paciente.");
        return sb.toString();
    }

    private AttemptDto toAttemptDto(TestAttempt a) {
        List<AnswerView> av = answerRepo.findByAttemptId(a.getId()).stream()
                .map(x -> new AnswerView(x.getQuestion().getId(), x.getOption() != null ? x.getOption().getId() : null, x.getValue())).toList();
        Integer maxScore = a.getMaxScore();
        String feedback = a.getPsychologistFeedback();
        if ("COMPLETED".equals(a.getStatus()) && (feedback == null || feedback.isBlank())) {
            List<TestQuestion> questions = questionRepo.findByTestIdOrderByPositionAsc(a.getTest().getId());
            maxScore = computeMaxScore(questions);
            int score = a.getScore() != null ? a.getScore() : 0;
            feedback = buildPsychologistFeedback(a.getTest(), score, maxScore, av.size(), questions.size());
        }
        return new AttemptDto(a.getId(), a.getTest().getId(), a.getTest().getTitle(), a.getStatus(), a.getScore(), maxScore, a.getResultSummary(), feedback, a.getStartedAt(), a.getCompletedAt(), av);
    }
}
