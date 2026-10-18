package dev.incidentcopilot.payment;
import dev.incidentcopilot.contracts.*; import java.math.BigDecimal; import java.util.UUID;
import dev.incidentcopilot.contracts.incidents.*; import java.time.Instant; import java.util.Optional;
import org.junit.jupiter.api.Test; import org.springframework.kafka.core.KafkaTemplate;
import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;
class PaymentEventHandlerTest {
 @Test void processesOnceAndPropagatesContext(){
  PaymentRepository repository=mock(); KafkaTemplate<String,Object> kafka=mock(); IncidentRegistry incidents=mock(); var handler=new PaymentEventHandler(repository,kafka,incidents);
  var event=new OrderCreated(UUID.randomUUID(),UUID.randomUUID(),"tenant-217","86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138",BigDecimal.TEN);
  handler.onOrderCreated(event); verify(repository).save(any()); verify(kafka).send(eq("payments.processed"),startsWith("tenant-217:"),argThat(v -> ((PaymentProcessed)v).correlationId().equals(event.correlationId())));
 }
 @Test void ignoresDuplicateDelivery(){
  PaymentRepository repository=mock(); KafkaTemplate<String,Object> kafka=mock(); var event=new OrderCreated(UUID.randomUUID(),UUID.randomUUID(),"tenant-217","86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138",BigDecimal.TEN);
  when(repository.existsBySourceEventIdAndTenantId(event.eventId(),event.tenantId())).thenReturn(true); new PaymentEventHandler(repository,kafka,mock(IncidentRegistry.class)).onOrderCreated(event); verify(repository,never()).save(any()); verifyNoInteractions(kafka);
 }
 @Test void failsOnlyTargetTenantDuringDatabaseIncident(){
  PaymentRepository repository=mock(); KafkaTemplate<String,Object> kafka=mock(); IncidentRegistry incidents=mock();
  var event=new OrderCreated(UUID.randomUUID(),UUID.randomUUID(),"tenant-217","86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138",BigDecimal.TEN);
  when(incidents.active(IncidentMode.DB_POOL_EXHAUSTION,"tenant-217")).thenReturn(Optional.of(new IncidentActivation(IncidentMode.DB_POOL_EXHAUSTION,"tenant-217",Instant.now(),Instant.now().plusSeconds(60),100)));
  org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->new PaymentEventHandler(repository,kafka,incidents).onOrderCreated(event));
  verify(incidents).recordEffect(IncidentMode.DB_POOL_EXHAUSTION,"tenant-217"); verifyNoInteractions(repository,kafka);
 }
 @Test void delaysThenProcessesDuringKafkaLagIncident(){
  PaymentRepository repository=mock(); KafkaTemplate<String,Object> kafka=mock(); IncidentRegistry incidents=mock();
  var event=new OrderCreated(UUID.randomUUID(),UUID.randomUUID(),"tenant-217","86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138",BigDecimal.TEN);
  when(incidents.active(IncidentMode.KAFKA_CONSUMER_LAG,"tenant-217")).thenReturn(Optional.of(new IncidentActivation(IncidentMode.KAFKA_CONSUMER_LAG,"tenant-217",Instant.now(),Instant.now().plusSeconds(60),1)));
  new PaymentEventHandler(repository,kafka,incidents).onOrderCreated(event);
  verify(incidents).recordEffect(IncidentMode.KAFKA_CONSUMER_LAG,"tenant-217"); verify(repository).save(any());
 }
}
