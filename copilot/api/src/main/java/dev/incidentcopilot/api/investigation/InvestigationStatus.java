package dev.incidentcopilot.api.investigation;

public enum InvestigationStatus {
    CREATED,
    COLLECTING_EVIDENCE,
    ANALYZING,
    COMPLETED,
    INSUFFICIENT_EVIDENCE,
    FAILED
}
