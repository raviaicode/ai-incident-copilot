package dev.incidentcopilot.api.audit;

import io.micrometer.core.instrument.MeterRegistry;
import dev.incidentcopilot.api.investigation.InvestigationEvidenceSink;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditedToolExecutor {
    private static final Logger log = LoggerFactory.getLogger(AuditedToolExecutor.class);
    private static final int MAX_RECORDS = 500;
    private final Deque<ToolInvocation> invocations = new ConcurrentLinkedDeque<>();
    private final MeterRegistry meters;
    private final Clock clock;
    private final InvestigationEvidenceSink evidenceSink;

    @Autowired public AuditedToolExecutor(MeterRegistry meters, InvestigationEvidenceSink evidenceSink) { this(meters, Clock.systemUTC(), evidenceSink); }
    AuditedToolExecutor(MeterRegistry meters) { this(meters, Clock.systemUTC()); }
    AuditedToolExecutor(MeterRegistry meters, Clock clock) { this(meters, clock, new InvestigationEvidenceSink() {
        public void requireInvestigation(UUID ignored) {}
        public void record(ToolInvocation ignored, Object data) {}
    }); }
    AuditedToolExecutor(MeterRegistry meters, Clock clock, InvestigationEvidenceSink evidenceSink) { this.meters = meters; this.clock = clock; this.evidenceSink = evidenceSink; }

    public <T> ToolResult<T> execute(String tool, String investigationId, String tenantId,
            String requestSummary, Supplier<T> action) {
        UUID investigation = parseInvestigationId(investigationId);
        evidenceSink.requireInvestigation(investigation);
        UUID invocationId = UUID.randomUUID();
        Instant started = clock.instant();
        long startedNanos = System.nanoTime();
        try {
            T data = action.get();
            Instant completed = clock.instant();
            ToolInvocation invocation = new ToolInvocation(invocationId, investigation, tool, tenantId, requestSummary,
                    ToolInvocation.Status.SUCCEEDED, started, completed, elapsedMillis(startedNanos), null);
            evidenceSink.record(invocation, data);
            record(invocation);
            meters.counter("investigation_tool_calls", "tool", tool, "status", "succeeded").increment();
            log.info("Investigation tool succeeded tool={} investigationId={} invocationId={}", tool, investigation, invocationId);
            return new ToolResult<>(invocationId, tool, completed, data);
        } catch (RuntimeException exception) {
            Instant completed = clock.instant();
            record(new ToolInvocation(invocationId, investigation, tool, tenantId, requestSummary,
                    ToolInvocation.Status.FAILED, started, completed, elapsedMillis(startedNanos), exception.getMessage()));
            meters.counter("investigation_tool_calls", "tool", tool, "status", "failed").increment();
            log.warn("Investigation tool failed tool={} investigationId={} invocationId={} reason={}", tool, investigation, invocationId, exception.getMessage());
            throw exception;
        }
    }

    public List<ToolInvocation> recent(int limit) {
        if (limit < 1 || limit > 200) throw new IllegalArgumentException("limit must be between 1 and 200");
        return invocations.stream().limit(limit).toList();
    }

    private void record(ToolInvocation invocation) {
        invocations.addFirst(invocation);
        while (invocations.size() > MAX_RECORDS) invocations.pollLast();
    }

    private UUID parseInvestigationId(String value) {
        try { return UUID.fromString(value); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("X-Investigation-ID must be a UUID"); }
    }

    private long elapsedMillis(long startedNanos) { return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis(); }
}
