package dev.incidentcopilot.api.audit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuditedToolExecutorTest {
    @Test void returnsEvidenceAndRecordsSuccessfulInvocation() {
        var executor = new AuditedToolExecutor(new SimpleMeterRegistry(), Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC));
        UUID investigationId = UUID.randomUUID();
        ToolResult<String> result = executor.execute("queryMetrics", investigationId.toString(), null, "up", () -> "evidence");
        assertEquals("evidence", result.data());
        assertEquals(result.invocationId(), executor.recent(1).getFirst().invocationId());
        assertEquals(ToolInvocation.Status.SUCCEEDED, executor.recent(1).getFirst().status());
    }

    @Test void recordsFailedInvocationAndRejectsMalformedInvestigationId() {
        var executor = new AuditedToolExecutor(new SimpleMeterRegistry());
        assertThrows(IllegalArgumentException.class, () -> executor.execute("getTrace", "bad-id", null, "trace", () -> "unused"));
        UUID investigationId = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> executor.execute("getTrace", investigationId.toString(), null, "trace", () -> { throw new IllegalStateException("offline"); }));
        assertEquals(ToolInvocation.Status.FAILED, executor.recent(1).getFirst().status());
    }
}
