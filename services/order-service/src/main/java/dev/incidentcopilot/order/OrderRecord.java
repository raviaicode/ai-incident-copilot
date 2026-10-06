package dev.incidentcopilot.order;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="orders", indexes=@Index(name="idx_orders_tenant", columnList="tenantId"))
class OrderRecord {
  @Id UUID id; @Column(nullable=false) String tenantId; @Column(nullable=false) String correlationId;
  @Column(nullable=false) BigDecimal amount; @Column(nullable=false) String status;
  protected OrderRecord() {}
  OrderRecord(UUID id, String tenantId, String correlationId, BigDecimal amount) { this.id=id; this.tenantId=tenantId; this.correlationId=correlationId; this.amount=amount; this.status="ACCEPTED"; }
}
