package dev.incidentcopilot.contracts.observability;

import dev.incidentcopilot.contracts.TenantHeaders;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;

class CorrelationLoggingFilterTest {
    @Test void exposesContextDuringRequestAndClearsItAfterward() throws Exception {
        var request = new MockHttpServletRequest(); var response = new MockHttpServletResponse();
        request.addHeader(TenantHeaders.TENANT_ID, "tenant-217");
        request.addHeader(TenantHeaders.CORRELATION_ID, "86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138");
        new CorrelationLoggingFilter().doFilter(request, response, (req, res) -> {
            assertEquals("tenant-217", MDC.get("tenantId"));
            assertEquals("86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138", MDC.get("correlationId"));
        });
        assertNull(MDC.get("tenantId")); assertNull(MDC.get("correlationId"));
        assertEquals("86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138", response.getHeader(TenantHeaders.CORRELATION_ID));
    }
}
