package com.psicology.backend.journal;

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
class JournalIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String registerAndToken(String prefix) throws Exception {
        String email = prefix + UUID.randomUUID() + "@test.com";
        String reg = "{\"name\":\"" + prefix + "\",\"email\":\"" + email + "\",\"password\":\"Senha@123\",\"role\":\"PATIENT\"}";
        MvcResult res = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void patientCanCrudJournalEntry() throws Exception {
        String token = registerAndToken("diario");
        MvcResult created = mvc.perform(post("/api/journal")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dia\",\"content\":\"Meu registro\",\"mood\":\"BEM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Meu registro"))
                .andReturn();
        String id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(get("/api/journal").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mvc.perform(put("/api/journal/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dia 2\",\"content\":\"Atualizado\",\"mood\":\"OK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Atualizado"));

        mvc.perform(delete("/api/journal/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void journalRequiresAuth() throws Exception {
        mvc.perform(get("/api/journal")).andExpect(result -> { int st = result.getResponse().getStatus(); if (st != 401 && st != 403) throw new AssertionError("Expected 401 or 403 but was " + st); });
    }
}
