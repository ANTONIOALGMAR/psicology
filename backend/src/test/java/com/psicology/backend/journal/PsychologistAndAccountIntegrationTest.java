package com.psicology.backend.journal;

import com.fasterxml.jackson.databind.JsonNode;
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
class PsychologistAndAccountIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String register(String name, String email, String role) throws Exception {
        String reg = "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"Senha@123\",\"role\":\"" + role + "\"}";
        MvcResult res = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String login(String email, String password) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void psychologistCanLinkPatientViewJournalAndComment() throws Exception {
        String adminToken = login("admin@psicare.com", "Admin@123");
        String patientEmail = "pac" + UUID.randomUUID() + "@test.com";
        String psyEmail = "psi" + UUID.randomUUID() + "@test.com";
        String patientToken = register("Paciente", patientEmail, "PATIENT");
        mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Psico\",\"email\":\"" + psyEmail + "\",\"password\":\"Senha@12345\",\"role\":\"PSYCHOLOGIST\"}"))
                .andExpect(status().isOk());
        String psyToken = login(psyEmail, "Senha@12345");

        MvcResult created = mvc.perform(post("/api/journal")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"content\":\"Registro do paciente\",\"mood\":\"OK\"}"))
                .andExpect(status().isOk()).andReturn();
        String entryId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(get("/api/psychologist/patients/" + "00000000-0000-0000-0000-000000000000" + "/journal")
                        .header("Authorization", "Bearer " + psyToken))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/psychologist/patients/link")
                        .header("Authorization", "Bearer " + psyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + patientEmail + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(patientEmail));

        mvc.perform(get("/api/psychologist/patients")
                        .header("Authorization", "Bearer " + psyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(patientEmail));

        MvcResult journalRes = mvc.perform(get("/api/psychologist/patients")
                        .header("Authorization", "Bearer " + psyToken))
                .andExpect(status().isOk()).andReturn();
        String patientId = mapper.readTree(journalRes.getResponse().getContentAsString()).get(0).get("id").asText();

        mvc.perform(get("/api/psychologist/patients/" + patientId + "/journal")
                        .header("Authorization", "Bearer " + psyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(entryId));

        mvc.perform(post("/api/psychologist/journal/" + entryId + "/comments")
                        .header("Authorization", "Bearer " + psyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Feedback do profissional\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Feedback do profissional"));
    }

    @Test
    void userCanUpdateProfileAndChangePassword() throws Exception {
        String email = "acc" + UUID.randomUUID() + "@test.com";
        String token = register("Nome", email, "PATIENT");

        mvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Novo Nome\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo Nome"));

        mvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Senha@123\",\"newPassword\":\"NovaSenha@1\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"NovaSenha@1\"}"))
                .andExpect(status().isOk());
    }
}
