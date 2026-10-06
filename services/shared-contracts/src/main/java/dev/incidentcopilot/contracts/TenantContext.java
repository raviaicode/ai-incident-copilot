package dev.incidentcopilot.contracts;

import java.util.UUID;

public record TenantContext(String tenantId, String correlationId) {
    public TenantContext {
        if (tenantId == null || !tenantId.matches("tenant-[A-Za-z0-9-]{1,57}")) {
            throw new IllegalArgumentException("X-Tenant-ID must match tenant-<identifier>");
        }
        try { UUID.fromString(correlationId); }
        catch (Exception exception) { throw new IllegalArgumentException("X-Correlation-ID must be a UUID"); }
    }
}
