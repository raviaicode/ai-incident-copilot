package dev.incidentcopilot.notification;
import dev.incidentcopilot.contracts.PaymentProcessed; import java.util.UUID; import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;
class NotificationEventHandlerTest {
 @Test void deliversOnlyOnce(){
  NotificationRepository repository=mock(); var handler=new NotificationEventHandler(repository); var event=new PaymentProcessed(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"tenant-217","86f7b51e-0de3-4a2e-bfa6-3ec8d69d1138");
  handler.onPaymentProcessed(event); verify(repository).save(any()); when(repository.existsBySourceEventIdAndTenantId(event.eventId(),event.tenantId())).thenReturn(true); handler.onPaymentProcessed(event); verify(repository,times(1)).save(any());
 }
}
