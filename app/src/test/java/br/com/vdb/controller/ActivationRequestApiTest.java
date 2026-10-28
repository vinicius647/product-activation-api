package br.com.vdb.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.activation.scheduler.enabled=false")
@AutoConfigureMockMvc
class ActivationRequestApiTest {

    private static final String BODY = """
            {"externalId": "%s", "customerId": "%s", "productCode": "%s"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsRequest() throws Exception {
        String externalId = newExternalId();

        mockMvc.perform(create(externalId, "CUST-10001", "INTERNET-500").with(writer("client-a")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/requests/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.externalId").value(externalId))
                .andExpect(jsonPath("$.customerId").value("CUST-10001"))
                .andExpect(jsonPath("$.productCode").value("INTERNET-500"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void findsCreatedRequest() throws Exception {
        String location = mockMvc.perform(create(newExternalId(), "CUST-1", "INTERNET-500").with(writer("client-a")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location).with(reader("client-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void returnsNotFoundForUnknownRequest() throws Exception {
        mockMvc.perform(get("/api/v1/requests/{id}", UUID.randomUUID()).with(reader("client-a")))
                .andExpect(status().isNotFound());
    }

    @Test
    void hidesRequestsOfAnotherClient() throws Exception {
        String location = mockMvc.perform(create(newExternalId(), "CUST-1", "INTERNET-500").with(writer("client-a")))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location).with(reader("client-b")))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsExistingRequestWhenSameDataIsSentAgain() throws Exception {
        String externalId = newExternalId();
        String id = idOf(mockMvc.perform(create(externalId, "CUST-1", "INTERNET-500").with(writer("client-a")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(create(externalId, "CUST-1", "INTERNET-500").with(writer("client-a")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void rejectsSameExternalIdWithDifferentData() throws Exception {
        String externalId = newExternalId();
        mockMvc.perform(create(externalId, "CUST-1", "INTERNET-500").with(writer("client-a")))
                .andExpect(status().isCreated());

        mockMvc.perform(create(externalId, "CUST-1", "TV-HD").with(writer("client-a")))
                .andExpect(status().isConflict());
    }

    @Test
    void validatesRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/requests").with(writer("client-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\": \"\", \"productCode\": \"INTERNET 500\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsOnlyRequestsOfTheCallerWithFilters() throws Exception {
        String client = "client-" + UUID.randomUUID();
        mockMvc.perform(create(newExternalId(), "CUST-A", "INTERNET-500").with(writer(client)));
        mockMvc.perform(create(newExternalId(), "CUST-A", "TV-HD").with(writer(client)));
        mockMvc.perform(create(newExternalId(), "CUST-B", "INTERNET-500").with(writer(client)));
        mockMvc.perform(create(newExternalId(), "CUST-A", "INTERNET-500").with(writer("other-client")));

        mockMvc.perform(get("/api/v1/requests")
                        .param("customerId", "CUST-A").param("status", "PENDING")
                        .param("page", "0").param("size", "1")
                        .with(reader(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].customerId").value("CUST-A"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void rejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/requests").param("size", "500").with(reader("client-a")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresToken() throws Exception {
        mockMvc.perform(get("/api/v1/requests/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresWriteScopeToCreate() throws Exception {
        mockMvc.perform(create(newExternalId(), "CUST-1", "INTERNET-500").with(reader("client-a")))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptsTokenIssuedByLocalEndpoint() throws Exception {
        String response = mockMvc.perform(post("/dev/token"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = response.replaceAll(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/v1/requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void exposesProbesMetricsAndDocs() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder create(String externalId, String customerId, String productCode) {
        return post("/api/v1/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(externalId, customerId, productCode));
    }

    static RequestPostProcessor writer(String clientId) {
        return jwt().jwt(token -> token.subject(clientId)).authorities(() -> "SCOPE_requests:write");
    }

    static RequestPostProcessor reader(String clientId) {
        return jwt().jwt(token -> token.subject(clientId)).authorities(() -> "SCOPE_requests:read");
    }

    private static String newExternalId() {
        return "EXT-" + UUID.randomUUID();
    }

    private static String idOf(String json) {
        return json.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}
