package dev.incidentcopilot.api.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DeploymentHistoryTool {
    private final Map<String, List<DeploymentRecord>> history;
    DeploymentHistoryTool(ObjectMapper mapper) throws IOException {
        try (var input = new ClassPathResource("deployments.json").getInputStream()) {
            history = Map.copyOf(mapper.readValue(input, new TypeReference<Map<String, List<DeploymentRecord>>>() {}));
        }
    }
    public List<DeploymentRecord> get(String service) {
        if (service == null || !service.matches("[a-z][a-z0-9-]{1,63}")) throw new IllegalArgumentException("service is invalid");
        return Optional.ofNullable(history.get(service)).orElseThrow(() -> new NoSuchElementException("No deployment history for service: " + service));
    }
    public record DeploymentRecord(String version, Instant deployedAt, String commit, String environment, String status) {}
}
