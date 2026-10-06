package dev.incidentcopilot.contracts;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {
    @Test void acceptsValidContext() {
        assertDoesNotThrow(() -> new TenantContext("tenant-217", "86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138"));
    }
    @Test void rejectsMissingOrMalformedValues() {
        assertThrows(IllegalArgumentException.class, () -> new TenantContext(null, null));
        assertThrows(IllegalArgumentException.class, () -> new TenantContext("other-217", "bad"));
        assertThrows(IllegalArgumentException.class, () -> new TenantContext("tenant-217", "bad"));
    }
}
