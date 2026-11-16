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
    public record AttemptDto(UUID id, UUID testId, String testTitle, String status, Integer score, String resultSummary, Instant startedAt, Instant completedAt, List<AnswerView> answers) {}
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
        int score = answers.stream().mapToInt(a -> a.getValue() != null ? a.getValue() : 0).sum();
        attempt.setScore(score);
        attempt.setStatus("COMPLETED");
        attempt.setCompletedAt(Instant.now());
        attempt.setResultSummary(buildSummary(attempt.getTest(), score, answers.size()));
        attemptRepo.save(attempt);
        return toAttemptDto(attempt);
    }

    private String buildSummary(PsychTest test, int score, int answered) {
        if ("PSYCHOLOGICAL".equals(test.getType())) {
            if (score >= 13) return "Bem-estar elevado no período avaliado (pontuação " + score + "). Continue cuidando de seus hábitos e rotina.";
            if (score >= 9) return "Bem-estar moderado (pontuação " + score + "). Há pontos positivos e aspectos que merecem atenção.";
            return "Sinais de bem-estar reduzido (pontuação " + score + "). Considere conversar com seu psicólogo sobre como você tem se sentido.";
        }
        if ("VOCATIONAL".equals(test.getType())) {
            return "Perfil vocacional exploratório registrado (pontuação total " + score + ", " + answered + " respostas). Seus interesses indicam áreas a aprofundar; discuta o resultado com um profissional para orientação detalhada.";
        }
        return "Teste concluído. Pontuação: " + score + ".";
    }

    private AttemptDto toAttemptDto(TestAttempt a) {
        List<AnswerView> av = answerRepo.findByAttemptId(a.getId()).stream()
                .map(x -> new AnswerView(x.getQuestion().getId(), x.getOption() != null ? x.getOption().getId() : null, x.getValue())).toList();
        return new AttemptDto(a.getId(), a.getTest().getId(), a.getTest().getTitle(), a.getStatus(), a.getScore(), a.getResultSummary(), a.getStartedAt(), a.getCompletedAt(), av);
    }
}
