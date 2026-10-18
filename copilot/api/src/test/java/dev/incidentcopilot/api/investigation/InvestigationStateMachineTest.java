package dev.incidentcopilot.api.investigation;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.incidentcopilot.api.audit.AuditedToolExecutor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvestigationStateMachineTest {
    @Test void auditedToolEvidenceAdvancesInvestigationToCollection() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC);
        var store = new InvestigationStore(new ObjectMapper(), clock);
        var investigation = store.create("tenant-217", "payment-service", "Why are payments failing?");
        var executor = new AuditedToolExecutor(new SimpleMeterRegistry(), store);
        executor.execute("queryMetrics", investigation.investigationId().toString(), "tenant-217", "incident metrics", () -> java.util.Map.of("mode", "DB_POOL_EXHAUSTION"));
        var updated = store.get(investigation.investigationId());
        assertEquals(InvestigationStatus.COLLECTING_EVIDENCE, updated.status());
        assertEquals(1, updated.evidence().size());
        assertEquals("queryMetrics", updated.evidence().getFirst().tool());
    }

    @Test void rejectsInvalidStateTransitionsAndUnknownInvestigations() {
        var store = new InvestigationStore(new ObjectMapper());
        var investigation = store.create("tenant-217", "payment-service", "Why are payments failing?");
        assertThrows(IllegalStateException.class, () -> store.startAnalysis(investigation.investigationId()));
        assertThrows(java.util.NoSuchElementException.class, () -> store.get(java.util.UUID.randomUUID()));
    }
}
