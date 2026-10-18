package dev.incidentcopilot.api.tools;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LogSearchTool {
    private static final Set<String> SERVICES = Set.of("tenant-service", "order-service", "payment-service", "notification-service", "incident-simulator", "copilot-api");
    private final RestClient client;
    private final String opensearchUrl;

    LogSearchTool(RestClient.Builder builder, @Value("${investigation.sources.opensearch}") String opensearchUrl) {
        this.client = builder.build(); this.opensearchUrl = opensearchUrl;
    }

    public JsonNode search(SearchRequest request) {
        validate(request);
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(Map.of("term", Map.of("tenantId.keyword", request.tenantId())));
        filters.add(Map.of("range", Map.of("@timestamp", Map.of("gte", "now-" + request.fromMinutes() + "m", "lte", "now"))));
        if (request.correlationId() != null && !request.correlationId().isBlank()) filters.add(Map.of("term", Map.of("correlationId.keyword", request.correlationId())));
        if (request.service() != null && !request.service().isBlank()) filters.add(Map.of("term", Map.of("service.name.keyword", request.service())));
        if (request.level() != null && !request.level().isBlank()) filters.add(Map.of("term", Map.of("log.level.keyword", request.level().toUpperCase(Locale.ROOT))));
        Map<String, Object> body = Map.of(
                "size", request.limit(),
                "sort", List.of(Map.of("@timestamp", Map.of("order", "desc"))),
                "query", Map.of("bool", Map.of("filter", filters)));
        return client.post().uri(opensearchUrl + "/incident-logs-*/_search")
                .body(body).retrieve().body(JsonNode.class);
    }

    private void validate(SearchRequest request) {
        if (request == null || request.tenantId() == null || !request.tenantId().matches("tenant-[A-Za-z0-9-]{1,57}")) throw new IllegalArgumentException("A valid tenantId is required");
        if (request.fromMinutes() < 1 || request.fromMinutes() > 1440) throw new IllegalArgumentException("fromMinutes must be between 1 and 1440");
        if (request.limit() < 1 || request.limit() > 200) throw new IllegalArgumentException("limit must be between 1 and 200");
        if (request.service() != null && !request.service().isBlank() && !SERVICES.contains(request.service())) throw new IllegalArgumentException("Unknown service");
        if (request.correlationId() != null && !request.correlationId().isBlank()) {
            try { UUID.fromString(request.correlationId()); } catch (RuntimeException exception) { throw new IllegalArgumentException("correlationId must be a UUID"); }
        }
    }

    public record SearchRequest(String tenantId, String correlationId, String service, String level, int fromMinutes, int limit) {
        public SearchRequest { if (fromMinutes == 0) fromMinutes = 30; if (limit == 0) limit = 50; }
        public String summary() { return "tenant=" + tenantId + ",service=" + Objects.toString(service, "*") + ",fromMinutes=" + fromMinutes + ",limit=" + limit; }
    }
}
