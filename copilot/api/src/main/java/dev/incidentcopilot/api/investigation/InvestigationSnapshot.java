package dev.incidentcopilot.api.investigation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvestigationSnapshot(UUID investigationId, String tenantId, String service, String question,
        InvestigationStatus status, Instant createdAt, Instant updatedAt, List<Evidence> evidence,
        List<Hypothesis> hypotheses, String conclusion, String failureReason) {}
