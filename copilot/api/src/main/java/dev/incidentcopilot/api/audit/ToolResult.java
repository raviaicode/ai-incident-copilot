package dev.incidentcopilot.api.audit;

import java.time.Instant;
import java.util.UUID;

public record ToolResult<T>(UUID invocationId, String tool, Instant capturedAt, T data) {}
