package dev.incidentcopilot.api.tools;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TraceLookupTool {
    private final RestClient client;
    private final String tempoUrl;
    TraceLookupTool(RestClient.Builder builder, @Value("${investigation.sources.tempo}") String tempoUrl) {
        this.client = builder.build(); this.tempoUrl = tempoUrl;
    }
    public JsonNode get(String traceId) {
        if (traceId == null || !traceId.matches("[a-fA-F0-9]{16}|[a-fA-F0-9]{32}")) throw new IllegalArgumentException("traceId must be 16 or 32 hexadecimal characters");
        return client.get().uri(tempoUrl + "/api/traces/{traceId}", traceId).retrieve().body(JsonNode.class);
    }
}
