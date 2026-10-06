package dev.incidentcopilot.notification;
import dev.incidentcopilot.contracts.*; import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import dev.incidentcopilot.contracts.observability.TenantLogContext; import org.slf4j.Logger; import org.slf4j.LoggerFactory;
@Service class NotificationEventHandler {
 private static final Logger log=LoggerFactory.getLogger(NotificationEventHandler.class);
 private final NotificationRepository repository; NotificationEventHandler(NotificationRepository repository){this.repository=repository;}
 @KafkaListener(topics="payments.processed",groupId="notification-service") @Transactional void onPaymentProcessed(PaymentProcessed event){
  try(var ignored=TenantLogContext.open(event.tenantId(),event.correlationId())) {
   new TenantContext(event.tenantId(),event.correlationId()); if(repository.existsBySourceEventIdAndTenantId(event.eventId(),event.tenantId())) { log.info("Duplicate PaymentProcessed ignored eventId={}",event.eventId()); return; }
   repository.save(new NotificationRecord(UUID.randomUUID(),event)); log.info("Notification delivered orderId={}",event.orderId());
  }
 }
}
