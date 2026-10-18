package dev.incidentcopilot.contracts.observability;

import dev.incidentcopilot.contracts.incidents.*;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ObservabilityAutoConfiguration {
    @Bean @ConditionalOnMissingBean CorrelationLoggingFilter correlationLoggingFilter() { return new CorrelationLoggingFilter(); }
    @Bean @ConditionalOnProperty(name="incident.injection.enabled", havingValue="true")
    IncidentRegistry incidentRegistry(@Value("${incident.injection.allowed}") String allowed, MeterRegistry meters) {
        Set<IncidentMode> modes = new HashSet<>();
        for (String value : allowed.split(",")) modes.add(IncidentMode.valueOf(value.trim()));
        return new IncidentRegistry(modes, meters);
    }
    @Bean @ConditionalOnBean(IncidentRegistry.class)
    IncidentControlController incidentControlController(IncidentRegistry registry,
            @Value("${incident.injection.token}") String token) { return new IncidentControlController(registry, token); }
}
