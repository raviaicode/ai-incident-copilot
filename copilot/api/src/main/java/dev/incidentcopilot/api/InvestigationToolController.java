package dev.incidentcopilot.api;

import com.fasterxml.jackson.databind.JsonNode;
import dev.incidentcopilot.api.audit.*;
import dev.incidentcopilot.api.tools.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class InvestigationToolController {
    private static final String INVESTIGATION_HEADER = "X-Investigation-ID";
    private final AuditedToolExecutor executor;
    private final LogSearchTool logs;
    private final MetricQueryTool metrics;
    private final TraceLookupTool traces;
    private final KafkaLagTool kafka;
    private final DeploymentHistoryTool deployments;

    InvestigationToolController(AuditedToolExecutor executor, LogSearchTool logs, MetricQueryTool metrics,
            TraceLookupTool traces, KafkaLagTool kafka, DeploymentHistoryTool deployments) {
        this.executor = executor; this.logs = logs; this.metrics = metrics; this.traces = traces; this.kafka = kafka; this.deployments = deployments;
    }

    @GetMapping("/tools") List<ToolDescriptor> catalog() {
        return List.of(
                new ToolDescriptor("searchLogs", "POST", "/api/tools/logs/search", "Search tenant-scoped structured logs by correlation, service, level, and time window."),
                new ToolDescriptor("queryMetrics", "POST", "/api/tools/metrics/query", "Execute a read-only Prometheus instant query."),
                new ToolDescriptor("getTrace", "GET", "/api/tools/traces/{traceId}", "Fetch one trace from Tempo."),
                new ToolDescriptor("getKafkaConsumerLag", "GET", "/api/tools/kafka/consumer-lag", "Calculate consumer lag from committed and end offsets."),
                new ToolDescriptor("getDeploymentHistory", "GET", "/api/tools/deployments/{service}", "Read versioned local deployment history for a service."));
    }

    @PostMapping("/tools/logs/search") ToolResult<JsonNode> searchLogs(@RequestHeader(INVESTIGATION_HEADER) String investigationId,
            @RequestBody LogSearchTool.SearchRequest request) {
        return executor.execute("searchLogs", investigationId, request.tenantId(), request.summary(), () -> logs.search(request));
    }

    @PostMapping("/tools/metrics/query") ToolResult<JsonNode> queryMetrics(@RequestHeader(INVESTIGATION_HEADER) String investigationId,
            @RequestBody MetricQueryTool.QueryRequest request) {
        return executor.execute("queryMetrics", investigationId, null, request.summary(), () -> metrics.query(request));
    }

    @GetMapping("/tools/traces/{traceId}") ToolResult<JsonNode> getTrace(@RequestHeader(INVESTIGATION_HEADER) String investigationId,
            @PathVariable String traceId) {
        return executor.execute("getTrace", investigationId, null, "traceId=" + traceId, () -> traces.get(traceId));
    }

    @GetMapping("/tools/kafka/consumer-lag") ToolResult<KafkaLagTool.KafkaLagResult> getKafkaLag(
            @RequestHeader(INVESTIGATION_HEADER) String investigationId, @RequestParam String groupId, @RequestParam String topic) {
        return executor.execute("getKafkaConsumerLag", investigationId, null, "groupId=" + groupId + ",topic=" + topic, () -> kafka.get(groupId, topic));
    }

    @GetMapping("/tools/deployments/{service}") ToolResult<List<DeploymentHistoryTool.DeploymentRecord>> getDeployments(
            @RequestHeader(INVESTIGATION_HEADER) String investigationId, @PathVariable String service) {
        return executor.execute("getDeploymentHistory", investigationId, null, "service=" + service, () -> deployments.get(service));
    }

    @GetMapping("/tool-invocations") List<ToolInvocation> invocations(@RequestParam(defaultValue = "50") int limit) { return executor.recent(limit); }
    record ToolDescriptor(String name, String method, String path, String description) {}
}
