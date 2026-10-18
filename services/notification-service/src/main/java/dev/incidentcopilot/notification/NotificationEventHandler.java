package dev.incidentcopilot.notification;
import dev.incidentcopilot.contracts.*; import java.util.UUID;
import dev.incidentcopilot.contracts.incidents.*;
import org.springframework.kafka.annotation.KafkaListener; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import dev.incidentcopilot.contracts.observability.TenantLogContext; import org.slf4j.Logger; import org.slf4j.LoggerFactory;
@Service class NotificationEventHandler {
 private static final Logger log=LoggerFactory.getLogger(NotificationEventHandler.class);
 private final NotificationRepository repository; private final IncidentRegistry incidents; NotificationEventHandler(NotificationRepository repository,IncidentRegistry incidents){this.repository=repository;this.incidents=incidents;}
 @KafkaListener(topics="payments.processed",groupId="notification-service") @Transactional void onPaymentProcessed(PaymentProcessed event){
  try(var ignored=TenantLogContext.open(event.tenantId(),event.correlationId())) {
   new TenantContext(event.tenantId(),event.correlationId());
   var deliveryFailure=incidents.active(IncidentMode.NOTIFICATION_DELIVERY_FAILURE,event.tenantId()).filter(activation -> activation.affects(event.eventId()));
   if(deliveryFailure.isPresent()) { incidents.recordEffect(IncidentMode.NOTIFICATION_DELIVERY_FAILURE,event.tenantId()); log.error("Injected notification delivery failure mode={} intensity={}",IncidentMode.NOTIFICATION_DELIVERY_FAILURE,deliveryFailure.get().intensity()); throw new IllegalStateException("Injected notification delivery failure for "+event.tenantId()); }
   if(repository.existsBySourceEventIdAndTenantId(event.eventId(),event.tenantId())) { log.info("Duplicate PaymentProcessed ignored eventId={}",event.eventId()); return; }
   repository.save(new NotificationRecord(UUID.randomUUID(),event)); log.info("Notification delivered orderId={}",event.orderId());
  }
 }
}
