package dev.incidentcopilot.api.investigation;

import com.fasterxml.jackson.databind.JsonNode;
import dev.incidentcopilot.api.audit.*;
import dev.incidentcopilot.api.tools.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InvestigationOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(InvestigationOrchestrator.class);
    private final InvestigationStore store; private final AuditedToolExecutor executor; private final HypothesisCorrelator correlator;
    private final LogSearchTool logs; private final MetricQueryTool metrics; private final TraceLookupTool traces;
    private final KafkaLagTool kafka; private final DeploymentHistoryTool deployments;

    InvestigationOrchestrator(InvestigationStore store, AuditedToolExecutor executor, HypothesisCorrelator correlator,
            LogSearchTool logs, MetricQueryTool metrics, TraceLookupTool traces, KafkaLagTool kafka, DeploymentHistoryTool deployments) {
        this.store=store; this.executor=executor; this.correlator=correlator; this.logs=logs; this.metrics=metrics; this.traces=traces; this.kafka=kafka; this.deployments=deployments;
    }

    public InvestigationSnapshot run(UUID id) {
        store.startCollection(id);
        InvestigationSnapshot investigation = store.get(id);
        String investigationId = id.toString();
        String tenant = investigation.tenantId();
        String service = investigation.service();

        final JsonNode[] logResult = new JsonNode[1];
        collect(() -> logResult[0] = executor.execute("searchLogs", investigationId, tenant,
                "tenant=" + tenant + ",service=" + service + ",fromMinutes=60,limit=100",
                () -> logs.search(new LogSearchTool.SearchRequest(tenant, null, service, null, 60, 100))).data());
        collect(() -> executor.execute("queryMetrics", investigationId, tenant, "incident effects for tenant=" + tenant,
                () -> metrics.query(new MetricQueryTool.QueryRequest("sum by (mode,tenant) (incident_injection_effects_total{tenant=\"" + tenant + "\"})"))));
        if (service.equals("payment-service") || service.equals("notification-service")) {
            String topic = service.equals("payment-service") ? "orders.created" : "payments.processed";
            collect(() -> executor.execute("getKafkaConsumerLag", investigationId, tenant, "groupId=" + service + ",topic=" + topic,
                    () -> kafka.get(service, topic)));
        }
        collect(() -> executor.execute("getDeploymentHistory", investigationId, tenant, "service=" + service, () -> deployments.get(service)));
        findTraceId(logResult[0]).ifPresent(traceId -> collect(() -> executor.execute("getTrace", investigationId, tenant,
                "traceId=" + traceId, () -> traces.get(traceId))));
        return analyze(id);
    }

    public InvestigationSnapshot analyze(UUID id) {
        store.startAnalysis(id);
        try { return store.complete(id, correlator.correlate(store.get(id).evidence())); }
        catch (RuntimeException exception) { store.fail(id, exception.getMessage()); throw exception; }
    }

    private void collect(Runnable action) {
        try { action.run(); }
        catch (RuntimeException exception) { log.warn("Evidence source unavailable; investigation will continue reason={}", exception.getMessage()); }
    }

    private Optional<String> findTraceId(JsonNode response) {
        if (response == null) return Optional.empty();
        for (JsonNode hit : response.path("hits").path("hits")) {
            String traceId = hit.path("_source").path("traceId").asText();
            if (traceId.matches("[a-fA-F0-9]{32}")) return Optional.of(traceId);
        }
        return Optional.empty();
    }
}
