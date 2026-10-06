package dev.incidentcopilot.tenant;

import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/tenants")
class TenantController {
    private final Set<String> active = Set.of("tenant-217", "tenant-218");
    @GetMapping("/{tenantId}") ResponseEntity<TenantResponse> get(@PathVariable String tenantId) {
        return active.contains(tenantId)
            ? ResponseEntity.ok(new TenantResponse(tenantId, "ACTIVE"))
            : ResponseEntity.notFound().build();
    }
    record TenantResponse(String tenantId, String status) {}
}
