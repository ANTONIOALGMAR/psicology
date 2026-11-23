package com.psicology.backend.appointment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentAndTransferIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String token(String email, String password) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String createUser(String adminToken, String role) throws Exception {
        String email = role.toLowerCase() + UUID.randomUUID() + "@test.com";
        MvcResult res = mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"User " + role + "\",\"email\":\"" + email + "\",\"password\":\"Senha@12345\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("id").asText() + "|" + email;
    }

    @Test
    void agendaIsIsolatedAndTransferMovesFutureAppointments() throws Exception {
        String admin = token("admin@psicare.com", "Admin@123");
        String[] a = createUser(admin, "PSYCHOLOGIST").split("\\|");
        String[] b = createUser(admin, "PSYCHOLOGIST").split("\\|");
        String[] p = createUser(admin, "PATIENT").split("\\|");
        String psyAId = a[0], psyAEmail = a[1], psyBId = b[0], psyBEmail = b[1], patientId = p[0], patientEmail = p[1];
        String psyA = token(psyAEmail, "Senha@12345");
        String psyB = token(psyBEmail, "Senha@12345");
        String patient = token(patientEmail, "Senha@12345");

        mvc.perform(post("/api/psychologist/patients/link").header("Authorization", "Bearer " + psyA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + patientEmail + "\"}"))
                .andExpect(status().isOk());

        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES);
        Instant end = start.plus(50, ChronoUnit.MINUTES);
        String body = """
                {"patientId":"%s","startsAt":"%s","endsAt":"%s","mode":"ONLINE","title":"Sessão inicial"}
                """.formatted(patientId, start, end);

        MvcResult created = mvc.perform(post("/api/appointments").header("Authorization", "Bearer " + psyA)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.psychologistId").value(psyAId))
                .andReturn();
        String appointmentId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        // sobreposição na agenda do mesmo psicólogo é bloqueada
        mvc.perform(post("/api/appointments").header("Authorization", "Bearer " + psyA)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        // paciente vê apenas seus agendamentos
        mvc.perform(get("/api/appointments").header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(appointmentId));

        // outro psicólogo não vê agendamento nem dados clínicos do paciente
        mvc.perform(get("/api/appointments").header("Authorization", "Bearer " + psyB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/psychologist/patients/" + patientId + "/journal").header("Authorization", "Bearer " + psyB))
                .andExpect(status().isForbidden());

        // somente o responsável pode transferir
        String transfer = "{\"toPsychologistId\":\"" + psyBId + "\",\"reason\":\"Encaminhamento de especialidade\"}";
        mvc.perform(post("/api/psychologist/patients/" + patientId + "/transfer")
                        .header("Authorization", "Bearer " + psyB)
                        .contentType(MediaType.APPLICATION_JSON).content(transfer))
                .andExpect(status().isBadRequest());

        MvcResult result = mvc.perform(post("/api/psychologist/patients/" + patientId + "/transfer")
                        .header("Authorization", "Bearer " + psyA)
                        .contentType(MediaType.APPLICATION_JSON).content(transfer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rescheduledAppointments").value(1))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains(psyBId);

        mvc.perform(get("/api/psychologist/patients").header("Authorization", "Bearer " + psyA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/psychologist/patients").header("Authorization", "Bearer " + psyB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/appointments").header("Authorization", "Bearer " + psyB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].psychologistId").value(psyBId));

        mvc.perform(get("/api/admin/audit?action=PATIENT_TRANSFERRED").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}
