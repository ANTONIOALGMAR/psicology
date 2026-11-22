package com.psicology.backend.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChatIntegrationTest {

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
                        .content("{\"name\":\"User " + role + "\",\"email\":\"" + email + "\",\"password\":\"senha12345\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("id").asText() + "|" + email;
    }

    @Test
    void chatbotAnswersOutsideHoursDetectsCrisisAndKeepsAudit() throws Exception {
        String admin = token("admin@psicare.com", "Admin@123");
        String[] psy = createUser(admin, "PSYCHOLOGIST").split("\\|");
        String[] patient = createUser(admin, "PATIENT").split("\\|");
        String psyToken = token(psy[1], "senha12345");
        String patientToken = token(patient[1], "senha12345");

        mvc.perform(post("/api/psychologist/patients/link").header("Authorization", "Bearer " + psyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + patient[1] + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/privacy/consents").header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"PRIVACY\",\"accepted\":true}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/chat/status").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.psychologistName").value("User PSYCHOLOGIST"))
                .andExpect(jsonPath("$.consentRequired").value(false));

        MvcResult first = mvc.perform(post("/api/chat/messages").header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Como faço para agendar uma consulta?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crisis").value(false))
                .andExpect(jsonPath("$.botMessage.content").value(org.hamcrest.Matchers.containsString("Agenda")))
                .andReturn();
        String sessionId = mapper.readTree(first.getResponse().getContentAsString()).get("sessionId").asText();

        mvc.perform(post("/api/chat/messages").header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Estou pensando em me matar\",\"sessionId\":\"" + sessionId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crisis").value(true))
                .andExpect(jsonPath("$.botMessage.content").value(org.hamcrest.Matchers.containsString("188")))
                .andExpect(jsonPath("$.botMessage.content").value(org.hamcrest.Matchers.containsString("192")))
                .andExpect(jsonPath("$.botMessage.content").value(org.hamcrest.Matchers.containsString("não substitui")));

        mvc.perform(get("/api/chat/history?sessionId=" + sessionId)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        mvc.perform(get("/api/psychologist/alerts").header("Authorization", "Bearer " + psyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].patientName").value("User PATIENT"))
                .andExpect(jsonPath("$[0].content").doesNotExist());

        mvc.perform(get("/api/admin/audit?action=CHAT_CRISIS_DETECTED").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mvc.perform(get("/api/admin/chat").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(4));

        mvc.perform(get("/api/admin/chat").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/psychologist/availability").header("Authorization", "Bearer " + psyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timezone\":\"America/Sao_Paulo\",\"startTime\":\"08:00\",\"endTime\":\"17:30\",\"weekendEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("08:00:00"));

        mvc.perform(get("/api/psychologist/availability").header("Authorization", "Bearer " + psyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekendEnabled").value(true));
    }
}
