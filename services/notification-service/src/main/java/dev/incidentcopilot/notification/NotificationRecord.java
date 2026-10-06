package dev.incidentcopilot.notification;
import dev.incidentcopilot.contracts.PaymentProcessed; import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="notifications",uniqueConstraints=@UniqueConstraint(name="uk_notification_tenant_event",columnNames={"tenantId","sourceEventId"}),indexes=@Index(name="idx_notification_tenant_order",columnList="tenantId,orderId"))
class NotificationRecord {
 @Id UUID id; @Column(nullable=false) UUID orderId; @Column(nullable=false) UUID sourceEventId; @Column(nullable=false) String tenantId; @Column(nullable=false) String correlationId; @Column(nullable=false) String status;
 protected NotificationRecord(){} NotificationRecord(UUID id,PaymentProcessed event){this.id=id;this.orderId=event.orderId();this.sourceEventId=event.eventId();this.tenantId=event.tenantId();this.correlationId=event.correlationId();this.status="DELIVERED";}
}
