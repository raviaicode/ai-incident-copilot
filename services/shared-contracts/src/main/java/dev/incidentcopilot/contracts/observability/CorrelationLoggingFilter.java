package dev.incidentcopilot.contracts.observability;

import dev.incidentcopilot.contracts.TenantHeaders;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(Ordered.HIGHEST_PRECEDENCE)
public final class CorrelationLoggingFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tenantId = request.getHeader(TenantHeaders.TENANT_ID);
        String correlationId = request.getHeader(TenantHeaders.CORRELATION_ID);
        putIfPresent("tenantId", tenantId);
        putIfPresent("correlationId", correlationId);
        if (correlationId != null) response.setHeader(TenantHeaders.CORRELATION_ID, correlationId);
        try { chain.doFilter(request, response); }
        finally { MDC.remove("tenantId"); MDC.remove("correlationId"); }
    }
    private static void putIfPresent(String key, String value) { if (value != null && !value.isBlank()) MDC.put(key, value); }
}
