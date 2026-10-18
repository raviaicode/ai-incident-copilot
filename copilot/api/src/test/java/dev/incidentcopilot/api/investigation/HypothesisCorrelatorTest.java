package dev.incidentcopilot.api.investigation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HypothesisCorrelatorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HypothesisCorrelator correlator = new HypothesisCorrelator();

    @Test void supportsRootCauseOnlyWithIndependentSignals() throws Exception {
        Evidence logs = evidence("searchLogs", "{\"message\":\"Injected database pool exhaustion\"}");
        Evidence metrics = evidence("queryMetrics", "{\"metric\":{\"mode\":\"DB_POOL_EXHAUSTION\"},\"value\":[1,5]}");
        Hypothesis hypothesis = correlator.correlate(List.of(logs, metrics)).getFirst();
        assertEquals("DB_POOL_EXHAUSTION", hypothesis.code());
        assertEquals(Hypothesis.Assessment.SUPPORTED, hypothesis.assessment());
        assertEquals(.85, hypothesis.confidence());
    }

    @Test void keepsSingleSignalAsPossibleInsteadOfDeclaringRootCause() throws Exception {
        Hypothesis hypothesis = correlator.correlate(List.of(evidence("searchLogs", "{\"message\":\"Injected database pool exhaustion\"}"))).getFirst();
        assertEquals(Hypothesis.Assessment.POSSIBLE, hypothesis.assessment());
    }

    private Evidence evidence(String tool, String json) throws Exception {
        return new Evidence(UUID.randomUUID(), UUID.randomUUID(), tool, Instant.now(), mapper.readTree(json));
    }
}
