package dev.incidentcopilot.contracts;
import java.math.BigDecimal;
import java.util.UUID;
public record OrderCreated(UUID eventId, UUID orderId, String tenantId, String correlationId, BigDecimal amount) {}
