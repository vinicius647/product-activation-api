package br.com.vdb.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// No Flyway migration exists yet, so Hibernate creates the schema for tests only.
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
class ActivationControllerIntegrationTest {

    private static final String BODY = """
            {"externalId": "%s", "customerId": "CUST-10001", "productCode": "INTERNET-500"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void activatesAndFinds() throws Exception {
        String location = mockMvc.perform(post("/api/v1/activations")
                        .with(jwt().authorities(() -> "SCOPE_activations:write"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted("EXT-1")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.externalId").value("EXT-1"))
                .andExpect(jsonPath("$.customerId").value("CUST-10001"))
                .andExpect(jsonPath("$.productCode").value("INTERNET-500"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location).with(jwt().authorities(() -> "SCOPE_activations:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").value("EXT-1"));
    }

    @Test
    void returnsNotFoundForUnknownActivation() throws Exception {
        mockMvc.perform(get("/api/v1/activations/{id}", UUID.randomUUID())
                        .with(jwt().authorities(() -> "SCOPE_activations:read")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateExternalId() throws Exception {
        var request = post("/api/v1/activations")
                .with(jwt().authorities(() -> "SCOPE_activations:write"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted("EXT-2"));

        mockMvc.perform(request).andExpect(status().isCreated());
        mockMvc.perform(request).andExpect(status().isConflict());
    }

    @Test
    void validatesRequest() throws Exception {
        mockMvc.perform(post("/api/v1/activations")
                        .with(jwt().authorities(() -> "SCOPE_activations:write"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\": \"\", \"customerId\": \"CUST-10001\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/activations/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresWriteScope() throws Exception {
        mockMvc.perform(post("/api/v1/activations")
                        .with(jwt().authorities(() -> "SCOPE_activations:read"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted("EXT-3")))
                .andExpect(status().isForbidden());
    }

    @Test
    void issuesLocalTokenAcceptedByApi() throws Exception {
        String response = mockMvc.perform(post("/dev/token"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = response.replaceAll(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        // Authenticated and authorized: reaches the use case, which answers 404 for an unknown id.
        mockMvc.perform(get("/api/v1/activations/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void exposesProbesMetricsAndDocs() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
