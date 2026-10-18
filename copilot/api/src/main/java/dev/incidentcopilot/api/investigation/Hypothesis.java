package dev.incidentcopilot.api.investigation;

import java.util.List;
import java.util.UUID;

public record Hypothesis(String code, String title, double confidence, Assessment assessment,
        String rationale, List<UUID> evidenceIds) {
    public enum Assessment { SUPPORTED, POSSIBLE, REJECTED }
}
