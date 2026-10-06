package dev.incidentcopilot.order;
import dev.incidentcopilot.contracts.*;
import java.math.BigDecimal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  private final OrderApplicationService service;
  OrderController(OrderApplicationService service) { this.service=service; }
  @PostMapping("/orders") ResponseEntity<?> create(@RequestHeader(value=TenantHeaders.TENANT_ID, required=false) String tenant,
      @RequestHeader(value=TenantHeaders.CORRELATION_ID, required=false) String correlation, @RequestBody CreateOrder request) {
    try { return ResponseEntity.accepted().body(service.create(new TenantContext(tenant, correlation), request.amount())); }
    catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_CONTEXT", e.getMessage())); }
    catch (OrderApplicationService.UnknownTenantException e) { return ResponseEntity.status(422).body(new ErrorResponse("UNKNOWN_TENANT", e.getMessage())); }
  }
  record CreateOrder(BigDecimal amount) { CreateOrder { if(amount==null || amount.signum()<=0) throw new IllegalArgumentException("amount must be positive"); } }
  record ErrorResponse(String code, String message) {}
}
