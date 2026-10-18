package dev.incidentcopilot.notification;
import dev.incidentcopilot.contracts.PaymentProcessed; import dev.incidentcopilot.contracts.incidents.*; import java.time.Instant; import java.util.*; import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;
class NotificationEventHandlerTest {
 @Test void deliversOnlyOnce(){
  NotificationRepository repository=mock(); var handler=new NotificationEventHandler(repository,mock(IncidentRegistry.class)); var event=new PaymentProcessed(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"tenant-217","86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138");
  handler.onPaymentProcessed(event); verify(repository).save(any()); when(repository.existsBySourceEventIdAndTenantId(event.eventId(),event.tenantId())).thenReturn(true); handler.onPaymentProcessed(event); verify(repository,times(1)).save(any());
 }
 @Test void failsDeliveryForTargetTenantDuringIncident(){
  NotificationRepository repository=mock(); IncidentRegistry incidents=mock(); var event=new PaymentProcessed(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"tenant-217","86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138");
  when(incidents.active(IncidentMode.NOTIFICATION_DELIVERY_FAILURE,"tenant-217")).thenReturn(Optional.of(new IncidentActivation(IncidentMode.NOTIFICATION_DELIVERY_FAILURE,"tenant-217",Instant.now(),Instant.now().plusSeconds(60),100)));
  org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->new NotificationEventHandler(repository,incidents).onPaymentProcessed(event));
  verify(incidents).recordEffect(IncidentMode.NOTIFICATION_DELIVERY_FAILURE,"tenant-217"); verifyNoInteractions(repository);
 }
}
