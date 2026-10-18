package dev.incidentcopilot.contracts.incidents;

import java.time.Instant;

public record IncidentActivation(
        IncidentMode mode,
        String tenantId,
        Instant activatedAt,
        Instant expiresAt,
        int intensity) {
    public boolean activeAt(Instant instant) { return instant.isBefore(expiresAt); }
    public boolean affects(Object stableKey) { return intensity >= 100 || Math.floorMod(stableKey.hashCode(), 100) < intensity; }
}
