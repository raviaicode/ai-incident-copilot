package dev.incidentcopilot.payment;
import dev.incidentcopilot.contracts.*; import java.util.UUID;
import dev.incidentcopilot.contracts.incidents.*;
import org.springframework.kafka.annotation.KafkaListener; import org.springframework.kafka.core.KafkaTemplate; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import dev.incidentcopilot.contracts.observability.TenantLogContext; import org.slf4j.Logger; import org.slf4j.LoggerFactory;
@Service class PaymentEventHandler {
 private static final Logger log=LoggerFactory.getLogger(PaymentEventHandler.class);
 private final PaymentRepository repository; private final KafkaTemplate<String,Object> kafka; private final IncidentRegistry incidents;
 PaymentEventHandler(PaymentRepository repository,KafkaTemplate<String,Object> kafka,IncidentRegistry incidents){this.repository=repository;this.kafka=kafka;this.incidents=incidents;}
 @KafkaListener(topics="orders.created",groupId="payment-service") @Transactional
 void onOrderCreated(OrderCreated event){
   try(var ignored=TenantLogContext.open(event.tenantId(),event.correlationId())) {
    new TenantContext(event.tenantId(),event.correlationId());
    var databaseFailure=incidents.active(IncidentMode.DB_POOL_EXHAUSTION,event.tenantId()).filter(activation -> activation.affects(event.eventId()));
    if(databaseFailure.isPresent()) {
      incidents.recordEffect(IncidentMode.DB_POOL_EXHAUSTION,event.tenantId());
      log.error("Injected database pool exhaustion mode={} intensity={}",IncidentMode.DB_POOL_EXHAUSTION,databaseFailure.get().intensity());
      throw new IllegalStateException("Injected database pool exhaustion for "+event.tenantId());
    }
    incidents.active(IncidentMode.KAFKA_CONSUMER_LAG,event.tenantId()).ifPresent(activation -> {
      incidents.recordEffect(IncidentMode.KAFKA_CONSUMER_LAG,event.tenantId());
      log.warn("Injected Kafka consumer lag mode={} delayMs={}",IncidentMode.KAFKA_CONSUMER_LAG,activation.intensity());
      try { Thread.sleep(activation.intensity()); }
      catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException("Interrupted during injected Kafka lag",exception); }
    });
    if(repository.existsBySourceEventIdAndTenantId(event.eventId(),event.tenantId())) { log.info("Duplicate OrderCreated ignored eventId={}",event.eventId()); return; }
    UUID paymentId=UUID.randomUUID(); repository.save(new PaymentRecord(paymentId,event.orderId(),event.eventId(),event.tenantId(),event.correlationId()));
    var processed=new PaymentProcessed(UUID.randomUUID(),event.orderId(),paymentId,event.tenantId(),event.correlationId());
    kafka.send("payments.processed",event.tenantId()+":"+event.orderId(),processed);
    log.info("Payment processed orderId={} paymentId={}",event.orderId(),paymentId);
   }
 }
}
