package dev.incidentcopilot.contracts;
import java.util.UUID;
public record PaymentProcessed(UUID eventId, UUID orderId, UUID paymentId, String tenantId, String correlationId) {}
