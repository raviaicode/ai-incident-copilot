package dev.incidentcopilot.payment;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
interface PaymentRepository extends JpaRepository<PaymentRecord,UUID>{ boolean existsBySourceEventIdAndTenantId(UUID eventId,String tenantId); Optional<PaymentRecord> findByOrderIdAndTenantId(UUID orderId,String tenantId); }
