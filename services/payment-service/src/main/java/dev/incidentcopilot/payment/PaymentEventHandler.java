package dev.incidentcopilot.payment;
import dev.incidentcopilot.contracts.*; import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener; import org.springframework.kafka.core.KafkaTemplate; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service class PaymentEventHandler {
 private final PaymentRepository repository; private final KafkaTemplate<String,Object> kafka;
 PaymentEventHandler(PaymentRepository repository,KafkaTemplate<String,Object> kafka){this.repository=repository;this.kafka=kafka;}
 @KafkaListener(topics="orders.created",groupId="payment-service") @Transactional
 void onOrderCreated(OrderCreated event){
   new TenantContext(event.tenantId(),event.correlationId());
   if(repository.existsBySourceEventIdAndTenantId(event.eventId(),event.tenantId())) return;
   UUID paymentId=UUID.randomUUID(); repository.save(new PaymentRecord(paymentId,event.orderId(),event.eventId(),event.tenantId(),event.correlationId()));
   var processed=new PaymentProcessed(UUID.randomUUID(),event.orderId(),paymentId,event.tenantId(),event.correlationId());
   kafka.send("payments.processed",event.tenantId()+":"+event.orderId(),processed);
 }
}
