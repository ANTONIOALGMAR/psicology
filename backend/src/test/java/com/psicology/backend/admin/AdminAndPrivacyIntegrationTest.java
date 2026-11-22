package com.psicology.backend.admin;

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
class AdminAndPrivacyIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String registerPatientWithConsents() throws Exception {
        String email = "lgpd" + UUID.randomUUID() + "@test.com";
        String body = """
                {"name":"Titular","email":"%s","password":"segredo1","role":"PATIENT",
                 "acceptTerms":true,"acceptPrivacy":true}
                """.formatted(email);
        MvcResult res = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String adminToken() throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@psicare.com\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void registrationConsentsPrivacyOverviewExportAndRbac() throws Exception {
        String token = registerPatientWithConsents();

        MvcResult overview = mvc.perform(get("/api/privacy/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consents.length()").value(2))
                .andExpect(jsonPath("$.requests.length()").value(0))
                .andReturn();
        JsonNode consents = mapper.readTree(overview.getResponse().getContentAsString()).get("consents");
        assertThat(consents.toString()).contains("TERMS").contains("PRIVACY").contains("true");

        mvc.perform(post("/api/privacy/consents").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"DATA_SHARING\",\"accepted\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false));

        mvc.perform(get("/api/privacy/export").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").exists())
                .andExpect(jsonPath("$.journalEntries").isArray())
                .andExpect(jsonPath("$.consents.length()").value(3));

        String requestBody = "{\"type\":\"DATA_EXPORT\",\"details\":\"Solicitação de teste\"}";
        mvc.perform(post("/api/privacy/requests").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mvc.perform(post("/api/privacy/requests").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanManageUsersTestsQuestionsAndAudit() throws Exception {
        String admin = adminToken();

        mvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isNumber())
                .andExpect(jsonPath("$.admins").isNumber());

        String email = "psy" + UUID.randomUUID() + "@test.com";
        MvcResult created = mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nova Psicóloga\",\"email\":\"" + email + "\",\"password\":\"senha12345\",\"role\":\"PSYCHOLOGIST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PSYCHOLOGIST"))
                .andReturn();
        String userId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(post("/api/admin/users/" + userId + "/reset-password")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"outrasenha123\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"outrasenha123\"}"))
                .andExpect(status().isOk());

        mvc.perform(put("/api/admin/users/" + userId + "/role")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"PATIENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PATIENT"));
        mvc.perform(put("/api/admin/users/" + userId + "/enabled")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        MvcResult test = mvc.perform(post("/api/admin/tests").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"VOCATIONAL\",\"title\":\"Teste Admin\",\"description\":\"Criado via API\",\"active\":true}"))
                .andExpect(status().isOk()).andReturn();
        String testId = mapper.readTree(test.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(post("/api/admin/tests/" + testId + "/questions")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Você gosta de trabalhar com pessoas?\",\"kind\":\"SCALE\",\"options\":[{\"label\":\"Sim\",\"value\":2},{\"label\":\"Não\",\"value\":0}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options.length()").value(2));
        mvc.perform(get("/api/admin/tests/" + testId + "/questions").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(put("/api/admin/tests/" + testId + "/active?active=false")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/admin/audit?action=ADMIN_TEST_CREATED")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void adminCannotDisableTheLastActiveAdmin() throws Exception {
        String admin = adminToken();
        mvc.perform(put("/api/admin/users/aaaaaaaa-0000-0000-0000-000000000001/enabled")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
