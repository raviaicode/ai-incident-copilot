package dev.incidentcopilot.order;
import dev.incidentcopilot.contracts.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
@Service class OrderApplicationService {
  private static final Logger log=LoggerFactory.getLogger(OrderApplicationService.class);
  private final OrderRepository repository; private final TenantClient tenants; private final KafkaTemplate<String,Object> kafka;
  OrderApplicationService(OrderRepository repository, TenantClient tenants, KafkaTemplate<String,Object> kafka) { this.repository=repository; this.tenants=tenants; this.kafka=kafka; }
  @Transactional CreateResult create(TenantContext context, BigDecimal amount) {
    if (!tenants.isActive(context.tenantId())) throw new UnknownTenantException(context.tenantId());
    UUID orderId=UUID.randomUUID(); repository.save(new OrderRecord(orderId, context.tenantId(), context.correlationId(), amount));
    var event=new OrderCreated(UUID.randomUUID(), orderId, context.tenantId(), context.correlationId(), amount);
    kafka.send("orders.created", context.tenantId()+":"+orderId, event);
    log.info("Order accepted and OrderCreated published orderId={} amount={}", orderId, amount);
    return new CreateResult(orderId, context.correlationId());
  }
  record CreateResult(UUID orderId, String correlationId) {}
  static class UnknownTenantException extends RuntimeException { UnknownTenantException(String id) { super("Unknown tenant: "+id); } }
}
