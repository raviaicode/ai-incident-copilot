package dev.incidentcopilot.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.kafka.bootstrap-servers=localhost:1", "management.otlp.tracing.export.enabled=false"})
class CopilotApiApplicationTest {
    @Test void applicationContextStarts() {}
}
