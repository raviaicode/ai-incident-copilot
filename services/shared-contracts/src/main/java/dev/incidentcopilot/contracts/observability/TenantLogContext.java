package dev.incidentcopilot.contracts.observability;

import org.slf4j.MDC;

public final class TenantLogContext implements AutoCloseable {
    private final String previousTenant = MDC.get("tenantId");
    private final String previousCorrelation = MDC.get("correlationId");

    private TenantLogContext(String tenantId, String correlationId) {
        MDC.put("tenantId", tenantId); MDC.put("correlationId", correlationId);
    }
    public static TenantLogContext open(String tenantId, String correlationId) { return new TenantLogContext(tenantId, correlationId); }
    @Override public void close() { restore("tenantId", previousTenant); restore("correlationId", previousCorrelation); }
    private static void restore(String key, String value) { if (value == null) MDC.remove(key); else MDC.put(key, value); }
}
