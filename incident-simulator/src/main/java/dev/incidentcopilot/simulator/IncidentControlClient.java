package dev.incidentcopilot.simulator;

import dev.incidentcopilot.contracts.incidents.IncidentMode;
import java.time.Duration;

interface IncidentControlClient {
    void activate(IncidentMode mode, String tenantId, Duration duration, int intensity);
    void deactivate(IncidentMode mode);
}
