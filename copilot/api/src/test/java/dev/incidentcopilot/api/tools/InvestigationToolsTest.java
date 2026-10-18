package dev.incidentcopilot.api.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestigationToolsTest {
    @Test void readsVersionedDeploymentHistory() throws Exception {
        var tool = new DeploymentHistoryTool(new ObjectMapper().findAndRegisterModules());
        var history = tool.get("payment-service");
        assertEquals("2.4.1", history.getFirst().version());
        assertThrows(java.util.NoSuchElementException.class, () -> tool.get("missing-service"));
    }

    @Test void validatesTraceAndMetricQueriesBeforeCallingSources() {
        var traces = new TraceLookupTool(RestClient.builder(), "http://tempo:3200");
        var metrics = new MetricQueryTool(RestClient.builder(), "http://prometheus:9090");
        assertThrows(IllegalArgumentException.class, () -> traces.get("not-a-trace"));
        assertThrows(IllegalArgumentException.class, () -> metrics.query(new MetricQueryTool.QueryRequest(" ")));
    }

    @Test void validatesKafkaIdentifiersBeforeCallingBroker() {
        AdminClient admin = mock();
        var tool = new KafkaLagTool(admin);
        assertThrows(IllegalArgumentException.class, () -> tool.get("bad group", "orders.created"));
        verifyNoInteractions(admin);
    }
}
