package dev.incidentcopilot.api.investigation;

import dev.incidentcopilot.api.audit.ToolInvocation;

public interface InvestigationEvidenceSink {
    void requireInvestigation(java.util.UUID investigationId);
    void record(ToolInvocation invocation, Object data);
}
