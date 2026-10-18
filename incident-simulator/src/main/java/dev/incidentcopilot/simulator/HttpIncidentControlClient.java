package dev.incidentcopilot.simulator;

import dev.incidentcopilot.contracts.incidents.IncidentMode;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class HttpIncidentControlClient implements IncidentControlClient {
    private final RestClient client;
    private final String paymentUrl;
    private final String notificationUrl;
    private final String token;

    HttpIncidentControlClient(RestClient.Builder builder,
            @Value("${incident.targets.payment}") String paymentUrl,
            @Value("${incident.targets.notification}") String notificationUrl,
            @Value("${incident.control.token}") String token) {
        this.client = builder.build(); this.paymentUrl = paymentUrl; this.notificationUrl = notificationUrl; this.token = token;
    }

    public void activate(IncidentMode mode, String tenantId, Duration duration, int intensity) {
        client.post().uri(baseUrl(mode) + "/internal/incidents/{mode}", mode)
                .header("X-Simulator-Token", token)
                .body(new ActivationPayload(tenantId, duration.toSeconds(), intensity))
                .retrieve().toBodilessEntity();
    }

    public void deactivate(IncidentMode mode) {
        client.delete().uri(baseUrl(mode) + "/internal/incidents/{mode}", mode)
                .header("X-Simulator-Token", token).retrieve().toBodilessEntity();
    }

    private String baseUrl(IncidentMode mode) {
        return mode == IncidentMode.NOTIFICATION_DELIVERY_FAILURE ? notificationUrl : paymentUrl;
    }

    private record ActivationPayload(String tenantId, long durationSeconds, int intensity) {}
}
