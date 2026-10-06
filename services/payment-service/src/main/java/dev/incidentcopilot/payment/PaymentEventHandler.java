package dev.incidentcopilot.payment;
import dev.incidentcopilot.contracts.*; import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener; import org.springframework.kafka.core.KafkaTemplate; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import dev.incidentcopilot.contracts.observability.TenantLogContext; import org.slf4j.Logger; import org.slf4j.LoggerFactory;
@Service class PaymentEventHandler {
 private static final Logger log=LoggerFactory.getLogger(PaymentEventHandler.class);
 private final PaymentRepository repository; private final KafkaTemplate<String,Object> kafka;
 PaymentEventHandler(PaymentRepository repository,KafkaTemplate<String,Object> kafka){this.repository=repository;this.kafka=kafka;}
 @KafkaListener(topics="orders.created",groupId="payment-service") @Transactional
 void onOrderCreated(OrderCreated event){
   try(var ignored=TenantLogContext.open(event.tenantId(),event.correlationId())) {
    new TenantContext(event.tenantId(),event.correlationId());
    if(repository.existsBySourceEventIdAndTenantId(event.eventId(),event.tenantId())) { log.info("Duplicate OrderCreated ignored eventId={}",event.eventId()); return; }
    UUID paymentId=UUID.randomUUID(); repository.save(new PaymentRecord(paymentId,event.orderId(),event.eventId(),event.tenantId(),event.correlationId()));
    var processed=new PaymentProcessed(UUID.randomUUID(),event.orderId(),paymentId,event.tenantId(),event.correlationId());
    kafka.send("payments.processed",event.tenantId()+":"+event.orderId(),processed);
    log.info("Payment processed orderId={} paymentId={}",event.orderId(),paymentId);
   }
 }
}
