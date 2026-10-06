package dev.incidentcopilot.order;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
interface OrderRepository extends JpaRepository<OrderRecord, UUID> { Optional<OrderRecord> findByIdAndTenantId(UUID id, String tenantId); }
