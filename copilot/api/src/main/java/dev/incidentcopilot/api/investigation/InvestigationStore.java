package dev.incidentcopilot.api.investigation;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.incidentcopilot.api.audit.ToolInvocation;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InvestigationStore implements InvestigationEvidenceSink {
    private static final Set<String> SERVICES = Set.of("tenant-service", "order-service", "payment-service", "notification-service");
    private final Map<UUID, Aggregate> investigations = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final Clock clock;

    @Autowired
    InvestigationStore(ObjectMapper mapper) { this(mapper, Clock.systemUTC()); }
    InvestigationStore(ObjectMapper mapper, Clock clock) { this.mapper = mapper; this.clock = clock; }

    public InvestigationSnapshot create(String tenantId, String service, String question) {
        if (tenantId == null || !tenantId.matches("tenant-[A-Za-z0-9-]{1,57}")) throw new IllegalArgumentException("A valid tenantId is required");
        if (!SERVICES.contains(service)) throw new IllegalArgumentException("Unknown service");
        if (question == null || question.isBlank() || question.length() > 500) throw new IllegalArgumentException("question must contain 1 to 500 characters");
        Instant now = clock.instant();
        Aggregate aggregate = new Aggregate(UUID.randomUUID(), tenantId, service, question.trim(), now);
        investigations.put(aggregate.id, aggregate);
        return aggregate.snapshot();
    }

    public synchronized InvestigationSnapshot get(UUID id) { return aggregate(id).snapshot(); }
    public synchronized List<InvestigationSnapshot> list() { return investigations.values().stream().map(Aggregate::snapshot).sorted(Comparator.comparing(InvestigationSnapshot::createdAt).reversed()).toList(); }
    public void requireInvestigation(UUID id) { aggregate(id); }

    public synchronized void startCollection(UUID id) {
        Aggregate value = aggregate(id);
        if (value.status != InvestigationStatus.CREATED) throw new IllegalStateException("Investigation cannot be run from status " + value.status);
        value.status = InvestigationStatus.COLLECTING_EVIDENCE; value.updatedAt = clock.instant();
    }

    public synchronized void startAnalysis(UUID id) {
        Aggregate value = aggregate(id);
        if (value.status != InvestigationStatus.COLLECTING_EVIDENCE) throw new IllegalStateException("Investigation is not collecting evidence");
        value.status = InvestigationStatus.ANALYZING; value.updatedAt = clock.instant();
    }

    public synchronized InvestigationSnapshot complete(UUID id, List<Hypothesis> hypotheses) {
        Aggregate value = aggregate(id);
        if (value.status != InvestigationStatus.ANALYZING) throw new IllegalStateException("Investigation is not ready for analysis");
        value.hypotheses = List.copyOf(hypotheses);
        Optional<Hypothesis> supported = hypotheses.stream().filter(item -> item.assessment() == Hypothesis.Assessment.SUPPORTED).findFirst();
        value.status = supported.isPresent() ? InvestigationStatus.COMPLETED : InvestigationStatus.INSUFFICIENT_EVIDENCE;
        value.conclusion = supported.map(item -> item.title() + " (confidence " + String.format(Locale.ROOT, "%.2f", item.confidence()) + ")")
                .orElse("Insufficient independent evidence to report a root cause.");
        value.updatedAt = clock.instant();
        return value.snapshot();
    }

    public synchronized InvestigationSnapshot fail(UUID id, String reason) {
        Aggregate value = aggregate(id); value.status = InvestigationStatus.FAILED; value.failureReason = reason; value.updatedAt = clock.instant(); return value.snapshot();
    }

    @Override public synchronized void record(ToolInvocation invocation, Object data) {
        Aggregate value = aggregate(invocation.investigationId());
        if (value.status == InvestigationStatus.CREATED) value.status = InvestigationStatus.COLLECTING_EVIDENCE;
        if (value.status != InvestigationStatus.COLLECTING_EVIDENCE) throw new IllegalStateException("Evidence cannot be added in status " + value.status);
        value.evidence.add(new Evidence(UUID.randomUUID(), invocation.invocationId(), invocation.tool(), invocation.completedAt(), mapper.valueToTree(data)));
        value.updatedAt = clock.instant();
    }

    private Aggregate aggregate(UUID id) {
        return Optional.ofNullable(investigations.get(id)).orElseThrow(() -> new NoSuchElementException("Unknown investigation: " + id));
    }

    private static final class Aggregate {
        private final UUID id; private final String tenantId; private final String service; private final String question; private final Instant createdAt;
        private Instant updatedAt; private InvestigationStatus status = InvestigationStatus.CREATED; private final List<Evidence> evidence = new ArrayList<>();
        private List<Hypothesis> hypotheses = List.of(); private String conclusion; private String failureReason;
        private Aggregate(UUID id, String tenantId, String service, String question, Instant now) { this.id=id; this.tenantId=tenantId; this.service=service; this.question=question; this.createdAt=now; this.updatedAt=now; }
        private InvestigationSnapshot snapshot() { return new InvestigationSnapshot(id, tenantId, service, question, status, createdAt, updatedAt, List.copyOf(evidence), hypotheses, conclusion, failureReason); }
    }
}
