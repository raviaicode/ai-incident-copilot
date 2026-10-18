package dev.incidentcopilot.contracts.incidents;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IncidentRegistryTest {
    @Test void activatesScopesAndDeactivatesAnAllowedIncident() {
        var registry = new IncidentRegistry(Set.of(IncidentMode.DB_POOL_EXHAUSTION), new SimpleMeterRegistry());
        registry.activate(IncidentMode.DB_POOL_EXHAUSTION, "tenant-217", Duration.ofSeconds(30), 25);
        assertTrue(registry.active(IncidentMode.DB_POOL_EXHAUSTION, "tenant-217").isPresent());
        assertTrue(registry.active(IncidentMode.DB_POOL_EXHAUSTION, "tenant-999").isEmpty());
        assertTrue(registry.deactivate(IncidentMode.DB_POOL_EXHAUSTION));
        assertTrue(registry.active().isEmpty());
    }

    @Test void rejectsUnsafeActivationParameters() {
        var registry = new IncidentRegistry(Set.of(IncidentMode.KAFKA_CONSUMER_LAG), new SimpleMeterRegistry());
        assertThrows(IllegalArgumentException.class, () -> registry.activate(IncidentMode.DB_POOL_EXHAUSTION, "tenant-217", Duration.ofSeconds(30), 1));
        assertThrows(IllegalArgumentException.class, () -> registry.activate(IncidentMode.KAFKA_CONSUMER_LAG, "bad", Duration.ofSeconds(30), 1));
        assertThrows(IllegalArgumentException.class, () -> registry.activate(IncidentMode.KAFKA_CONSUMER_LAG, "tenant-217", Duration.ofMinutes(16), 1));
    }

    @Test void failurePercentageIsDeterministicForAStableEventKey() {
        var activation = new IncidentActivation(IncidentMode.NOTIFICATION_DELIVERY_FAILURE, "tenant-217", java.time.Instant.now(), java.time.Instant.now().plusSeconds(30), 50);
        assertEquals(activation.affects("event-42"), activation.affects("event-42"));
        assertTrue(new IncidentActivation(IncidentMode.NOTIFICATION_DELIVERY_FAILURE, "tenant-217", java.time.Instant.now(), java.time.Instant.now().plusSeconds(30), 100).affects("any-event"));
    }
}
