package com.psicology.backend.chat;

import com.psicology.backend.audit.AuditService;
import com.psicology.backend.domain.chat.ChatMessage;
import com.psicology.backend.domain.chat.ChatMessageRepository;
import com.psicology.backend.domain.chat.PsychologistAvailability;
import com.psicology.backend.domain.chat.PsychologistAvailabilityRepository;
import com.psicology.backend.domain.link.PatientLink;
import com.psicology.backend.domain.link.PatientLinkRepository;
import com.psicology.backend.domain.privacy.ConsentRepository;
import com.psicology.backend.domain.user.User;
import com.psicology.backend.domain.user.UserRepository;
import com.psicology.backend.dto.AdminDtos.PagedResponse;
import com.psicology.backend.dto.ChatDtos.*;
import com.psicology.backend.privacy.PrivacyService;
import com.psicology.backend.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private static final String DEFAULT_ZONE = "America/Sao_Paulo";
    private static final LocalTime DEFAULT_START = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END = LocalTime.of(18, 0);
    private static final String DISCLAIMER = "\n\nResposta automática: não substitui a orientação de um profissional.";

    private static final List<String> CRISIS_TERMS = List.of(
            "suicid", "me matar", "matar a mim", "acabar com a minha vida", "acabar com minha vida",
            "tirar a minha vida", "tirar minha vida", "quero morrer", "nao quero viver",
            "nao aguento mais", "me machucar", "me cortar", "autolesao", "autolesoes", "cvv");

    private final ChatMessageRepository messageRepo;
    private final PsychologistAvailabilityRepository availabilityRepo;
    private final PatientLinkRepository linkRepo;
    private final UserRepository userRepo;
    private final ConsentRepository consentRepo;
    private final AuditService auditService;

    public ChatService(ChatMessageRepository messageRepo, PsychologistAvailabilityRepository availabilityRepo,
                       PatientLinkRepository linkRepo, UserRepository userRepo, ConsentRepository consentRepo,
                       AuditService auditService) {
        this.messageRepo = messageRepo;
        this.availabilityRepo = availabilityRepo;
        this.linkRepo = linkRepo;
        this.userRepo = userRepo;
        this.consentRepo = consentRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ChatStatus status(UUID patientId) {
        PatientLink link = activeLink(patientId);
        String name = link == null ? null : link.getPsychologist().getName();
        boolean online = link != null && isOnline(availabilityEntity(link.getPsychologist().getId()));
        boolean consent = hasPrivacyConsent(patientId);
        String message;
        if (link == null) {
            message = "Você ainda não possui um profissional vinculado. Este orientador pode dar dicas gerais, mas não substitui o atendimento.";
        } else if (online) {
            message = name + " está dentro do horário de atendimento. Use a Agenda para combinar um horário ou acione os canais de emergência se for urgente.";
        } else {
            message = "Fora do horário de " + name + ". Este orientador automático pode ajudar com orientações gerais até o próximo atendimento.";
        }
        return new ChatStatus(online, name, message, !consent);
    }

    @Transactional
    public ChatReplyResponse send(UUID patientId, ChatSendRequest req, AppUserDetails actor, HttpServletRequest request) {
        String text = req.content() == null ? "" : req.content().trim();
        if (text.isEmpty()) throw new IllegalArgumentException("Escreva uma mensagem");
        if (text.length() > 2000) throw new IllegalArgumentException("Mensagem muito longa (máximo 2000 caracteres)");
        if (!hasPrivacyConsent(patientId)) {
            throw new IllegalArgumentException("Aceite a política de privacidade em Privacidade para usar o orientador automático");
        }

        UUID sessionId = req.sessionId() != null ? req.sessionId() : UUID.randomUUID();
        boolean crisis = isCrisis(text);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setUserId(patientId);
        userMessage.setSessionId(sessionId);
        userMessage.setSender("USER");
        userMessage.setContent(text);
        userMessage.setCrisis(crisis);
        messageRepo.save(userMessage);

        PatientLink link = activeLink(patientId);
        boolean online = link != null && isOnline(availabilityEntity(link.getPsychologist().getId()));
        String reply = crisis ? crisisReply() : online ? onlineReply() : faqReply(text);

        ChatMessage botMessage = new ChatMessage();
        botMessage.setUserId(patientId);
        botMessage.setSessionId(sessionId);
        botMessage.setSender("BOT");
        botMessage.setContent(reply);
        messageRepo.save(botMessage);

        if (crisis) {
            auditService.record(actor, "CHAT_CRISIS_DETECTED", "CHAT", sessionId.toString(), patientId,
                    "Alerta de crise detectado (conteúdo da mensagem não incluído)", request);
        }
        return new ChatReplyResponse(sessionId, ChatMessageResponse.of(userMessage),
                ChatMessageResponse.of(botMessage), crisis);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> sessionHistory(UUID patientId, UUID sessionId) {
        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .filter(m -> m.getUserId().equals(patientId))
                .map(ChatMessageResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> myHistory(UUID patientId) {
        return messageRepo.findByUserIdOrderByCreatedAtDesc(patientId).stream()
                .map(ChatMessageResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<CrisisAlertResponse> crisisAlerts(UUID psychologistId) {
        List<UUID> patientIds = linkRepo.findByPsychologistIdAndStatus(psychologistId, "ACTIVE").stream()
                .map(l -> l.getPatient().getId()).toList();
        if (patientIds.isEmpty()) return List.of();
        Map<UUID, String> names = userRepo.findAllById(patientIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        return messageRepo.findByCrisisTrueAndUserIdInAndCreatedAtAfterOrderByCreatedAtDesc(
                        patientIds, Instant.now().minus(30, ChronoUnit.DAYS)).stream()
                .collect(Collectors.toMap(ChatMessage::getUserId, m -> m, (first, second) -> first, LinkedHashMap::new))
                .values().stream()
                .map(m -> new CrisisAlertResponse(m.getId(), m.getUserId(),
                        names.getOrDefault(m.getUserId(), ""), m.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ChatMessageResponse> adminHistory(Pageable pageable) {
        Page<ChatMessage> messages = messageRepo.findAllByOrderByCreatedAtDesc(pageable);
        List<UUID> userIds = messages.getContent().stream().map(ChatMessage::getUserId).distinct().toList();
        Map<UUID, String> emails = userRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail, (a, b) -> a));
        Page<ChatMessageResponse> page = messages.map(m -> ChatMessageResponse.of(m, emails.get(m.getUserId())));
        return PagedResponse.of(page);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(UUID psychologistId) {
        PsychologistAvailability a = availabilityEntity(psychologistId);
        return new AvailabilityResponse(psychologistId, a.getTimezone(), a.getStartTime(), a.getEndTime(), a.isWeekendEnabled());
    }

    @Transactional
    public AvailabilityResponse saveAvailability(UUID psychologistId, AvailabilityRequest req) {
        LocalTime start = parseTime(req.startTime(), DEFAULT_START);
        LocalTime end = parseTime(req.endTime(), DEFAULT_END);
        if (!end.isAfter(start)) throw new IllegalArgumentException("O horário final deve ser posterior ao inicial");
        String zone = req.timezone() == null || req.timezone().isBlank() ? DEFAULT_ZONE : req.timezone().trim();
        try { ZoneId.of(zone); } catch (Exception e) { throw new IllegalArgumentException("Fuso horário inválido"); }

        PsychologistAvailability a = availabilityEntity(psychologistId);
        a.setPsychologistId(psychologistId);
        a.setTimezone(zone);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setWeekendEnabled(Boolean.TRUE.equals(req.weekendEnabled()));
        availabilityRepo.save(a);
        return new AvailabilityResponse(psychologistId, zone, start, end, a.isWeekendEnabled());
    }

    private boolean isOnline(PsychologistAvailability a) {
        ZoneId zone;
        try { zone = ZoneId.of(a.getTimezone()); } catch (Exception e) { zone = ZoneId.of(DEFAULT_ZONE); }
        ZonedDateTime now = ZonedDateTime.now(zone);
        DayOfWeek day = now.getDayOfWeek();
        if ((day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) && !a.isWeekendEnabled()) return false;
        LocalTime time = now.toLocalTime();
        LocalTime start = a.getStartTime() == null ? DEFAULT_START : a.getStartTime();
        LocalTime end = a.getEndTime() == null ? DEFAULT_END : a.getEndTime();
        return !time.isBefore(start) && !time.isAfter(end);
    }

    private PsychologistAvailability availabilityEntity(UUID psychologistId) {
        return availabilityRepo.findById(psychologistId).orElseGet(PsychologistAvailability::new);
    }

    private PatientLink activeLink(UUID patientId) {
        return linkRepo.findByPatientIdAndStatus(patientId, "ACTIVE").stream().findFirst().orElse(null);
    }

    private boolean hasPrivacyConsent(UUID userId) {
        return consentRepo.findFirstByUserIdAndKindAndVersionOrderByCreatedAtDesc(
                        userId, "PRIVACY", PrivacyService.CURRENT_VERSION)
                .map(c -> c.isAccepted()).orElse(false);
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return LocalTime.parse(value.length() == 5 ? value + ":00" : value); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Horário inválido (use HH:mm)"); }
    }

    private boolean isCrisis(String text) {
        String normalized = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return CRISIS_TERMS.stream().anyMatch(normalized::contains);
    }

    private String crisisReply() {
        return "Percebi que você pode estar em uma situação de risco. Esta é uma resposta automática e não substitui atendimento profissional. "
                + "Se houver risco imediato, ligue 192 (SAMU) ou 190, ou vá ao pronto-socorro mais próximo. "
                + "Para conversar gratuitamente, 24 horas, ligue 188 (CVV) ou acesse cvv.org.br. "
                + "Se puder, peça a alguém de confiança para ficar com você agora. "
                + "O alerta foi registrado para acompanhamento do profissional." + DISCLAIMER;
    }

    private String onlineReply() {
        return "Neste momento o profissional está dentro do horário de atendimento. Este orientador é automático e não substitui a conversa com ele. "
                + "Use o menu Agenda para ver ou combinar horários. Se precisar de ajuda imediata, ligue 188 (CVV) ou 192 (SAMU)." + DISCLAIMER;
    }

    private String faqReply(String text) {
        String normalized = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String answer;
        if (normalized.contains("agenda") || normalized.contains("agendar") || normalized.contains("cancelar")
                || normalized.contains("remarcar") || normalized.contains("horario")) {
            answer = "Você pode ver, cancelar ou solicitar horários no menu Agenda. Se precisar remarcar, escolha outro horário livre. ";
        } else if (normalized.contains("ansios") || normalized.contains("ansiedade")) {
            answer = "Para momentos de ansiedade, tente inspirar por 4 segundos e expirar por 6, por alguns minutos. "
                    + "Também ajuda nomear 5 coisas que você vê, 4 que você sente, 3 que escuta, 2 que cheira e 1 que prova. ";
        } else if (normalized.contains("sono") || normalized.contains("dormir") || normalized.contains("insonia")) {
            answer = "Para o sono, mantenha horários regulares, evite telas e cafeína perto de dormir e deixe o ambiente escuro e silencioso. ";
        } else if (normalized.contains("estresse") || normalized.contains("estressada")) {
            answer = "Em momentos de estresse, faça pausas curtas, respire devagar e tente separar o que está sob seu controle do que não está. ";
        } else if (normalized.contains("raiva") || normalized.contains("irritad")) {
            answer = "Quando a raiva aparecer, afaste-se um pouco da situação, respire fundo e tente nomear o que você está sentindo antes de agir. ";
        } else if (normalized.contains("remedio") || normalized.contains("medicacao") || normalized.contains("medicamento")) {
            answer = "Não altere, aumente ou interrompa nenhuma medicação por conta própria. Converse sempre com o profissional responsável. ";
        } else if (normalized.contains("trabalho") || normalized.contains("estudo") || normalized.contains("faculdade")) {
            answer = "Dividir tarefas em passos pequenos e definir pausas costuma ajudar. Leve essa dificuldade para a próxima sessão. ";
        } else {
            answer = "Entendi seu relato. Este orientador funciona fora do horário e oferece apenas dicas gerais; seu profissional poderá conversar com você no próximo atendimento. ";
        }
        return answer + "Se a situação for urgente, ligue 188 (CVV) ou 192 (SAMU)." + DISCLAIMER;
    }
}
