package dev.incidentcopilot.order;
import dev.incidentcopilot.contracts.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class OrderApplicationServiceTest {
  @Test void persistsAndPublishesTenantAwareOrder() {
    OrderRepository repository=mock(); TenantClient tenants=mock(); KafkaTemplate<String,Object> kafka=mock();
    when(tenants.isActive("tenant-217")).thenReturn(true);
    var service=new OrderApplicationService(repository, tenants, kafka);
    var result=service.create(new TenantContext("tenant-217","86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138"), new BigDecimal("12.50"));
    assertNotNull(result.orderId()); verify(repository).save(any()); verify(kafka).send(eq("orders.created"), startsWith("tenant-217:"), any(OrderCreated.class));
  }
  @Test void rejectsUnknownTenantBeforeWriting() {
    OrderRepository repository=mock(); TenantClient tenants=mock(); KafkaTemplate<String,Object> kafka=mock();
    var service=new OrderApplicationService(repository, tenants, kafka);
    assertThrows(OrderApplicationService.UnknownTenantException.class, () -> service.create(new TenantContext("tenant-999","86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138"), BigDecimal.ONE));
    verifyNoInteractions(repository, kafka);
  }
}
