package dev.incidentcopilot.api.tools;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class LogSearchToolTest {
    @Test void buildsTenantScopedStructuredQuery() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var tool = new LogSearchTool(builder, "http://opensearch:9200");
        server.expect(requestTo("http://opensearch:9200/incident-logs-*/_search"))
                .andExpect(jsonPath("$.query.bool.filter[0].term['tenantId.keyword']").value("tenant-217"))
                .andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));
        assertTrue(tool.search(new LogSearchTool.SearchRequest("tenant-217", null, "payment-service", "error", 15, 25)).has("hits"));
        server.verify();
    }

    @Test void rejectsUnscopedOrOversizedSearches() {
        var tool = new LogSearchTool(RestClient.builder(), "http://opensearch:9200");
        assertThrows(IllegalArgumentException.class, () -> tool.search(new LogSearchTool.SearchRequest(null, null, null, null, 30, 50)));
        assertThrows(IllegalArgumentException.class, () -> tool.search(new LogSearchTool.SearchRequest("tenant-217", null, null, null, 30, 201)));
    }
}
