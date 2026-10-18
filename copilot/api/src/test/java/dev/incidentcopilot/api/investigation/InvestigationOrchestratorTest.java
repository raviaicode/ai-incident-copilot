package dev.incidentcopilot.api.investigation;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.incidentcopilot.api.audit.AuditedToolExecutor;
import dev.incidentcopilot.api.tools.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvestigationOrchestratorTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test void collectsEvidenceAndCompletesWithSupportedHypothesis() throws Exception {
        var store = new InvestigationStore(mapper);
        var logs = mock(LogSearchTool.class);
        var metrics = mock(MetricQueryTool.class);
        var traces = mock(TraceLookupTool.class);
        var kafka = mock(KafkaLagTool.class);
        var deployments = mock(DeploymentHistoryTool.class);
        when(logs.search(any())).thenReturn(mapper.readTree("""
                {"hits":{"hits":[{"_source":{"message":"Injected database pool exhaustion"}}]}}
                """));
        when(metrics.query(any())).thenReturn(mapper.readTree("""
                {"status":"success","data":{"result":[{"metric":{"mode":"DB_POOL_EXHAUSTION"},"value":[1,"1"]}]}}
                """));
        when(kafka.get("payment-service", "orders.created"))
                .thenReturn(new KafkaLagTool.KafkaLagResult("payment-service", "orders.created", 0, List.of()));
        when(deployments.get("payment-service")).thenReturn(List.of(
                new DeploymentHistoryTool.DeploymentRecord("2.4.1", Instant.parse("2026-10-18T08:00:00Z"), "abc1234", "local", "CURRENT")));

        var orchestrator = new InvestigationOrchestrator(store,
                new AuditedToolExecutor(new SimpleMeterRegistry(), store), new HypothesisCorrelator(),
                logs, metrics, traces, kafka, deployments);
        var created = store.create("tenant-217", "payment-service", "Why are payments failing?");

        InvestigationSnapshot result = orchestrator.run(created.investigationId());

        assertEquals(InvestigationStatus.COMPLETED, result.status());
        assertEquals(4, result.evidence().size());
        assertEquals("DB_POOL_EXHAUSTION", result.hypotheses().getFirst().code());
        assertEquals(Hypothesis.Assessment.SUPPORTED, result.hypotheses().getFirst().assessment());
        assertEquals(.95, result.hypotheses().getFirst().confidence());
        verifyNoInteractions(traces);
    }
}
