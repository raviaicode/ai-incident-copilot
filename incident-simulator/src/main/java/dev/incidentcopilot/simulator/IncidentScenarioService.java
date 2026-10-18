package dev.incidentcopilot.simulator;

import dev.incidentcopilot.contracts.incidents.IncidentMode;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class IncidentScenarioService {
    private static final Map<String, ScenarioDefinition> SCENARIOS = Map.of(
            "payment-db-pool-exhaustion", new ScenarioDefinition("payment-db-pool-exhaustion", "Payment database pool exhaustion", "Reject payment processing for one tenant to emulate an unavailable connection pool.", "payment-service", IncidentMode.DB_POOL_EXHAUSTION, 100, "failure-percent"),
            "payment-kafka-consumer-lag", new ScenarioDefinition("payment-kafka-consumer-lag", "Payment Kafka consumer lag", "Delay payment consumption for one tenant to create deterministic processing lag.", "payment-service", IncidentMode.KAFKA_CONSUMER_LAG, 5000, "milliseconds"),
            "notification-delivery-failure", new ScenarioDefinition("notification-delivery-failure", "Notification delivery failure", "Reject notification delivery for one tenant while payment processing remains healthy.", "notification-service", IncidentMode.NOTIFICATION_DELIVERY_FAILURE, 100, "failure-percent"));

    private final IncidentControlClient client;
    private final MeterRegistry meters;
    private final Clock clock;
    private final Map<UUID, IncidentRun> runs = new ConcurrentHashMap<>();

    @Autowired IncidentScenarioService(IncidentControlClient client, MeterRegistry meters) { this(client, meters, Clock.systemUTC()); }
    IncidentScenarioService(IncidentControlClient client, MeterRegistry meters, Clock clock) { this.client = client; this.meters = meters; this.clock = clock; }

    Collection<ScenarioDefinition> scenarios() { return SCENARIOS.values().stream().sorted(Comparator.comparing(ScenarioDefinition::id)).toList(); }

    synchronized IncidentRun activate(String scenarioId, String tenantId, long durationSeconds, Integer requestedIntensity) {
        ScenarioDefinition scenario = Optional.ofNullable(SCENARIOS.get(scenarioId)).orElseThrow(() -> new NoSuchElementException("Unknown scenario: " + scenarioId));
        if (durationSeconds < 1 || durationSeconds > 900) throw new IllegalArgumentException("durationSeconds must be between 1 and 900");
        if (tenantId == null || !tenantId.matches("tenant-[A-Za-z0-9-]{1,57}")) throw new IllegalArgumentException("Invalid tenantId");
        boolean alreadyActive = runs.values().stream().map(run -> refresh(run.incidentId())).anyMatch(run -> run.scenarioId().equals(scenarioId) && run.status() == IncidentRun.Status.ACTIVE);
        if (alreadyActive) throw new IllegalStateException("Scenario is already active: " + scenarioId);
        int intensity = requestedIntensity == null ? scenario.defaultIntensity() : requestedIntensity;
        if (intensity < 1 || intensity > 10_000) throw new IllegalArgumentException("intensity must be between 1 and 10000");
        Duration duration = Duration.ofSeconds(durationSeconds);
        client.activate(scenario.mode(), tenantId, duration, intensity);
        Instant now = clock.instant();
        IncidentRun run = new IncidentRun(UUID.randomUUID(), scenario.id(), scenario.mode(), scenario.targetService(), tenantId, intensity, now, now.plus(duration), null, IncidentRun.Status.ACTIVE);
        runs.put(run.incidentId(), run);
        meters.counter("incident_simulator_activations", "scenario", scenarioId, "tenant", tenantId).increment();
        return run;
    }

    synchronized IncidentRun recover(UUID incidentId) {
        IncidentRun run = refresh(incidentId);
        if (run.status() != IncidentRun.Status.ACTIVE) throw new IllegalStateException("Incident is not active: " + incidentId);
        client.deactivate(run.mode());
        IncidentRun recovered = run.recoveredAt(clock.instant());
        runs.put(incidentId, recovered);
        meters.counter("incident_simulator_recoveries", "scenario", run.scenarioId(), "tenant", run.tenantId()).increment();
        return recovered;
    }

    List<IncidentRun> runs() { return runs.keySet().stream().map(this::refresh).sorted(Comparator.comparing(IncidentRun::startedAt).reversed()).toList(); }

    private IncidentRun refresh(UUID incidentId) {
        IncidentRun run = Optional.ofNullable(runs.get(incidentId)).orElseThrow(() -> new NoSuchElementException("Unknown incident: " + incidentId));
        IncidentRun current = run.currentAt(clock.instant());
        if (current != run) runs.put(incidentId, current);
        return current;
    }
}
