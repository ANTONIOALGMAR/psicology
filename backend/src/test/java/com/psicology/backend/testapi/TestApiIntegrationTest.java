package com.psicology.backend.testapi;

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
class TestApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String registerAndToken() throws Exception {
        String email = "t" + UUID.randomUUID() + "@test.com";
        String reg = "{\"name\":\"Paciente\",\"email\":\"" + email + "\",\"password\":\"Senha@123\",\"role\":\"PATIENT\"}";
        MvcResult res = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void listTestsIsPublicAndSeeded() throws Exception {
        mvc.perform(get("/api/tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title").isNotEmpty());
    }

    @Test
    void patientCanStartAnswerAndSubmitAttempt() throws Exception {
        String token = registerAndToken();
        MvcResult testsRes = mvc.perform(get("/api/tests")).andExpect(status().isOk()).andReturn();
        JsonNode tests = mapper.readTree(testsRes.getResponse().getContentAsString());
        assertThat(tests.size()).isGreaterThan(0);
        String testId = tests.get(0).get("id").asText();

        MvcResult detailRes = mvc.perform(get("/api/tests/" + testId)).andExpect(status().isOk()).andReturn();
        JsonNode detail = mapper.readTree(detailRes.getResponse().getContentAsString());
        JsonNode questions = detail.get("questions");
        assertThat(questions.size()).isGreaterThan(0);

        MvcResult attemptRes = mvc.perform(post("/api/tests/" + testId + "/attempts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn();
        String attemptId = mapper.readTree(attemptRes.getResponse().getContentAsString()).get("id").asText();

        StringBuilder answersJson = new StringBuilder("[");
        for (int i = 0; i < questions.size(); i++) {
            JsonNode q = questions.get(i);
            String qid = q.get("id").asText();
            String optId = q.get("options").get(0).get("id").asText();
            if (i > 0) answersJson.append(',');
            answersJson.append("{\"questionId\":\"").append(qid).append("\",\"optionId\":\"").append(optId).append("\"}");
        }
        answersJson.append("]");

        mvc.perform(post("/api/attempts/" + attemptId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answersJson.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").isNumber());

        mvc.perform(get("/api/attempts/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(attemptId));
    }
}
