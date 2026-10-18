package dev.incidentcopilot.simulator;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
class IncidentScenarioController {
    private final IncidentScenarioService service;
    IncidentScenarioController(IncidentScenarioService service) { this.service = service; }

    @GetMapping("/scenarios") Collection<ScenarioDefinition> scenarios() { return service.scenarios(); }
    @GetMapping("/incidents") List<IncidentRun> incidents() { return service.runs(); }
    @PostMapping("/scenarios/{scenarioId}/activate") ResponseEntity<IncidentRun> activate(@PathVariable String scenarioId, @RequestBody ActivationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.activate(scenarioId, request.tenantId(), request.durationSeconds(), request.intensity()));
    }
    @DeleteMapping("/incidents/{incidentId}") IncidentRun recover(@PathVariable UUID incidentId) { return service.recover(incidentId); }

    @ExceptionHandler(NoSuchElementException.class) ResponseEntity<ErrorResponse> notFound(RuntimeException exception) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage())); }
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class}) ResponseEntity<ErrorResponse> badRequest(RuntimeException exception) { return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage())); }

    record ActivationRequest(String tenantId, long durationSeconds, Integer intensity) {}
    record ErrorResponse(String message) {}
}
