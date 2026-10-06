package dev.incidentcopilot.order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
@Component class TenantClient {
  private final RestClient client;
  TenantClient(RestClient.Builder builder, @Value("${tenant-service.url:http://localhost:8081}") String url) { client=builder.baseUrl(url).build(); }
  boolean isActive(String tenantId) {
    try { client.get().uri("/tenants/{id}", tenantId).retrieve().toBodilessEntity(); return true; }
    catch (HttpClientErrorException.NotFound ignored) { return false; }
  }
}
