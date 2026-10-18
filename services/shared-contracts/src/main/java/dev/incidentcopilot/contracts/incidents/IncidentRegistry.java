package dev.incidentcopilot.contracts.incidents;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class IncidentRegistry {
    private final Set<IncidentMode> allowed;
    private final Clock clock;
    private final MeterRegistry meters;
    private final Map<IncidentMode, IncidentActivation> active = new ConcurrentHashMap<>();

    public IncidentRegistry(Set<IncidentMode> allowed, MeterRegistry meters) { this(allowed, meters, Clock.systemUTC()); }
    IncidentRegistry(Set<IncidentMode> allowed, MeterRegistry meters, Clock clock) {
        this.allowed = Set.copyOf(allowed); this.meters = meters; this.clock = clock;
        for (IncidentMode mode : allowed) {
            meters.gauge("incident_injection_active",
                    List.of(io.micrometer.core.instrument.Tag.of("mode", mode.name())), this,
                    registry -> registry.isModeActive(mode) ? 1 : 0);
        }
    }
    public IncidentActivation activate(IncidentMode mode, String tenantId, Duration duration, int intensity) {
        if (!allowed.contains(mode)) throw new IllegalArgumentException("Incident mode is not allowed by this service: " + mode);
        if (tenantId == null || !tenantId.matches("tenant-[A-Za-z0-9-]{1,57}")) throw new IllegalArgumentException("Invalid tenantId");
        if (duration.isNegative() || duration.isZero() || duration.compareTo(Duration.ofMinutes(15)) > 0) throw new IllegalArgumentException("durationSeconds must be between 1 and 900");
        if (intensity < 1 || intensity > 10_000) throw new IllegalArgumentException("intensity must be between 1 and 10000");
        Instant now = clock.instant();
        IncidentActivation activation = new IncidentActivation(mode, tenantId, now, now.plus(duration), intensity);
        active.put(mode, activation);
        meters.counter("incident_injection_triggered_total", "mode", mode.name(), "tenant", tenantId).increment();
        return activation;
    }
    public void recordEffect(IncidentMode mode, String tenantId) {
        meters.counter("incident_injection_effects", "mode", mode.name(), "tenant", tenantId).increment();
    }
    public Optional<IncidentActivation> active(IncidentMode mode, String tenantId) {
        IncidentActivation value = active.get(mode);
        if (value != null && !value.activeAt(clock.instant())) { active.remove(mode, value); value = null; }
        return value != null && value.tenantId().equals(tenantId) ? Optional.of(value) : Optional.empty();
    }
    public Collection<IncidentActivation> active() {
        Instant now = clock.instant();
        active.entrySet().removeIf(entry -> !entry.getValue().activeAt(now));
        return List.copyOf(active.values());
    }
    public boolean deactivate(IncidentMode mode) { return active.remove(mode) != null; }
    private boolean isModeActive(IncidentMode mode) {
        IncidentActivation activation = active.get(mode);
        return activation != null && activation.activeAt(clock.instant());
    }
}
