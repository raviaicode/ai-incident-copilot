package dev.incidentcopilot.simulator;

import dev.incidentcopilot.contracts.incidents.IncidentMode;
import java.time.Instant;
import java.util.UUID;

public record IncidentRun(UUID incidentId, String scenarioId, IncidentMode mode, String targetService,
        String tenantId, int intensity, Instant startedAt, Instant expiresAt, Instant recoveredAt, Status status) {
    public enum Status { ACTIVE, EXPIRED, RECOVERED }
    IncidentRun currentAt(Instant now) {
        return status == Status.ACTIVE && !now.isBefore(expiresAt)
                ? new IncidentRun(incidentId, scenarioId, mode, targetService, tenantId, intensity, startedAt, expiresAt, recoveredAt, Status.EXPIRED)
                : this;
    }
    IncidentRun recoveredAt(Instant now) {
        return new IncidentRun(incidentId, scenarioId, mode, targetService, tenantId, intensity, startedAt, expiresAt, now, Status.RECOVERED);
    }
}
