package dev.incidentcopilot.notification;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
interface NotificationRepository extends JpaRepository<NotificationRecord,UUID>{boolean existsBySourceEventIdAndTenantId(UUID eventId,String tenantId);Optional<NotificationRecord> findByOrderIdAndTenantId(UUID orderId,String tenantId);}
