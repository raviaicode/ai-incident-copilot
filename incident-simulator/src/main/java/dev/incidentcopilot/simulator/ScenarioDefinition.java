package dev.incidentcopilot.simulator;

import dev.incidentcopilot.contracts.incidents.IncidentMode;

public record ScenarioDefinition(
        String id, String name, String description, String targetService,
        IncidentMode mode, int defaultIntensity, String intensityUnit) {}
