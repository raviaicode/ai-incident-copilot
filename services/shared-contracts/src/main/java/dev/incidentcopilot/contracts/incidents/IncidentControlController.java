package dev.incidentcopilot.contracts.incidents;

import java.time.Duration;
import java.util.Collection;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/incidents")
public class IncidentControlController {
    private final IncidentRegistry registry; private final String token;
    public IncidentControlController(IncidentRegistry registry, String token) {
        this.registry=registry; this.token=token;
    }
    @PostMapping("/{mode}") public ResponseEntity<?> activate(@RequestHeader("X-Simulator-Token") String supplied,
            @PathVariable IncidentMode mode, @RequestBody ActivationRequest request) {
        if (!token.equals(supplied)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try { return ResponseEntity.ok(registry.activate(mode, request.tenantId(), Duration.ofSeconds(request.durationSeconds()), request.intensity())); }
        catch (IllegalArgumentException exception) { return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage())); }
    }
    @DeleteMapping("/{mode}") public ResponseEntity<Void> deactivate(@RequestHeader("X-Simulator-Token") String supplied, @PathVariable IncidentMode mode) {
        if (!token.equals(supplied)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return registry.deactivate(mode) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
    @GetMapping public ResponseEntity<Collection<IncidentActivation>> active(@RequestHeader("X-Simulator-Token") String supplied) {
        return token.equals(supplied) ? ResponseEntity.ok(registry.active()) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    public record ActivationRequest(String tenantId, long durationSeconds, int intensity) {}
    public record ErrorResponse(String message) {}
}
