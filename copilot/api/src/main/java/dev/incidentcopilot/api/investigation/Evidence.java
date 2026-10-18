package dev.incidentcopilot.api.investigation;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record Evidence(UUID evidenceId, UUID invocationId, String tool, Instant capturedAt, JsonNode data) {}
