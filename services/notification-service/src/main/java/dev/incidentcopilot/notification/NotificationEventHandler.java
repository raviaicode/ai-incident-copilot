package dev.incidentcopilot.notification;
import dev.incidentcopilot.contracts.*; import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service class NotificationEventHandler {
 private final NotificationRepository repository; NotificationEventHandler(NotificationRepository repository){this.repository=repository;}
 @KafkaListener(topics="payments.processed",groupId="notification-service") @Transactional void onPaymentProcessed(PaymentProcessed event){
  new TenantContext(event.tenantId(),event.correlationId()); if(repository.existsBySourceEventIdAndTenantId(event.eventId(),event.tenantId())) return; repository.save(new NotificationRecord(UUID.randomUUID(),event));
 }
}
