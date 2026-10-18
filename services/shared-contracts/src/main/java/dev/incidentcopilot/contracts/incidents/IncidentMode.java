package dev.incidentcopilot.contracts.incidents;

public enum IncidentMode {
    DB_POOL_EXHAUSTION,
    KAFKA_CONSUMER_LAG,
    NOTIFICATION_DELIVERY_FAILURE
}
