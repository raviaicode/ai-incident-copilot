package dev.incidentcopilot.simulator;

import dev.incidentcopilot.contracts.incidents.IncidentMode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncidentScenarioServiceTest {
    @Test void activatesAndRecoversKnownScenario() {
        IncidentControlClient client = mock();
        var service = new IncidentScenarioService(client, new SimpleMeterRegistry());
        IncidentRun run = service.activate("payment-db-pool-exhaustion", "tenant-217", 60, null);
        assertEquals(IncidentRun.Status.ACTIVE, run.status());
        verify(client).activate(IncidentMode.DB_POOL_EXHAUSTION, "tenant-217", Duration.ofSeconds(60), 100);
        IncidentRun recovered = service.recover(run.incidentId());
        assertEquals(IncidentRun.Status.RECOVERED, recovered.status());
        verify(client).deactivate(IncidentMode.DB_POOL_EXHAUSTION);
    }

    @Test void enforcesScenarioAndDurationSafetyLimits() {
        var service = new IncidentScenarioService(mock(IncidentControlClient.class), new SimpleMeterRegistry());
        assertThrows(java.util.NoSuchElementException.class, () -> service.activate("unknown", "tenant-217", 60, null));
        assertThrows(IllegalArgumentException.class, () -> service.activate("payment-db-pool-exhaustion", "tenant-217", 901, null));
        assertThrows(IllegalArgumentException.class, () -> service.activate("payment-db-pool-exhaustion", "invalid", 60, null));
    }
}
