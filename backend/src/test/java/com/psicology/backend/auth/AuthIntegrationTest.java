package com.psicology.backend.auth;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void registerLoginAndMeFlow() throws Exception {
        String email = "user" + UUID.randomUUID() + "@test.com";
        String reg = "{\"name\":\"Teste\",\"email\":\"" + email + "\",\"password\":\"segredo1\",\"role\":\"PATIENT\"}";

        MvcResult regRes = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();
        String token = mapper.readTree(regRes.getResponse().getContentAsString()).get("token").asText();

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"segredo1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Teste"));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@psicare.com\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailRejected() throws Exception {
        String email = "dup" + UUID.randomUUID() + "@test.com";
        String body = "{\"name\":\"Dup\",\"email\":\"" + email + "\",\"password\":\"segredo1\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
