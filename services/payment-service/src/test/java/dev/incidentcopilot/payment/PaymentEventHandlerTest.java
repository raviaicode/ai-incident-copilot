package dev.incidentcopilot.payment;
import dev.incidentcopilot.contracts.*; import java.math.BigDecimal; import java.util.UUID;
import org.junit.jupiter.api.Test; import org.springframework.kafka.core.KafkaTemplate;
import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;
class PaymentEventHandlerTest {
 @Test void processesOnceAndPropagatesContext(){
  PaymentRepository repository=mock(); KafkaTemplate<String,Object> kafka=mock(); var handler=new PaymentEventHandler(repository,kafka);
  var event=new OrderCreated(UUID.randomUUID(),UUID.randomUUID(),"tenant-217","86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138",BigDecimal.TEN);
  handler.onOrderCreated(event); verify(repository).save(any()); verify(kafka).send(eq("payments.processed"),startsWith("tenant-217:"),argThat(v -> ((PaymentProcessed)v).correlationId().equals(event.correlationId())));
 }
 @Test void ignoresDuplicateDelivery(){
  PaymentRepository repository=mock(); KafkaTemplate<String,Object> kafka=mock(); var event=new OrderCreated(UUID.randomUUID(),UUID.randomUUID(),"tenant-217","86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138",BigDecimal.TEN);
  when(repository.existsBySourceEventIdAndTenantId(event.eventId(),event.tenantId())).thenReturn(true); new PaymentEventHandler(repository,kafka).onOrderCreated(event); verify(repository,never()).save(any()); verifyNoInteractions(kafka);
 }
}
