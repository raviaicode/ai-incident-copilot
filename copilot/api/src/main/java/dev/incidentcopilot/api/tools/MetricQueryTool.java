package dev.incidentcopilot.api.tools;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MetricQueryTool {
    private final RestClient client;
    private final String prometheusUrl;
    MetricQueryTool(RestClient.Builder builder, @Value("${investigation.sources.prometheus}") String prometheusUrl) {
        this.client = builder.build(); this.prometheusUrl = prometheusUrl;
    }
    public JsonNode query(QueryRequest request) {
        if (request == null || request.query() == null || request.query().isBlank() || request.query().length() > 500) throw new IllegalArgumentException("query must contain 1 to 500 characters");
        return client.get().uri(prometheusUrl + "/api/v1/query?query={query}", request.query()).retrieve().body(JsonNode.class);
    }
    public record QueryRequest(String query) {
        public String summary() { return query; }
    }
}
