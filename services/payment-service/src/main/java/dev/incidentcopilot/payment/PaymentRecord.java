package dev.incidentcopilot.payment;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="payments", uniqueConstraints=@UniqueConstraint(name="uk_payment_tenant_event",columnNames={"tenantId","sourceEventId"}), indexes=@Index(name="idx_payment_tenant_order",columnList="tenantId,orderId"))
class PaymentRecord {
 @Id UUID id; @Column(nullable=false) UUID orderId; @Column(nullable=false) UUID sourceEventId; @Column(nullable=false) String tenantId; @Column(nullable=false) String correlationId; @Column(nullable=false) String status;
 protected PaymentRecord(){} PaymentRecord(UUID id,UUID orderId,UUID sourceEventId,String tenantId,String correlationId){this.id=id;this.orderId=orderId;this.sourceEventId=sourceEventId;this.tenantId=tenantId;this.correlationId=correlationId;this.status="PROCESSED";}
}
