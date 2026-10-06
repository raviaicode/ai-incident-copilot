package dev.incidentcopilot.contracts.observability;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ObservabilityAutoConfiguration {
    @Bean @ConditionalOnMissingBean CorrelationLoggingFilter correlationLoggingFilter() { return new CorrelationLoggingFilter(); }
}
