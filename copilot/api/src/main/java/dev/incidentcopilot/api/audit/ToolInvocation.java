package dev.incidentcopilot.api.audit;

import java.time.Instant;
import java.util.UUID;

public record ToolInvocation(UUID invocationId, UUID investigationId, String tool, String tenantId,
        String requestSummary, Status status, Instant startedAt, Instant completedAt, long durationMs, String error) {
    public enum Status { SUCCEEDED, FAILED }
}
