package dev.incidentcopilot.api;

import dev.incidentcopilot.api.investigation.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investigations")
class InvestigationController {
    private final InvestigationStore store; private final InvestigationOrchestrator orchestrator;
    InvestigationController(InvestigationStore store, InvestigationOrchestrator orchestrator) { this.store=store; this.orchestrator=orchestrator; }

    @PostMapping ResponseEntity<InvestigationSnapshot> create(@RequestBody CreateInvestigation request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(store.create(request.tenantId(), request.service(), request.question()));
    }
    @GetMapping List<InvestigationSnapshot> list() { return store.list(); }
    @GetMapping("/{id}") InvestigationSnapshot get(@PathVariable UUID id) { return store.get(id); }
    @PostMapping("/{id}/run") InvestigationSnapshot run(@PathVariable UUID id) { return orchestrator.run(id); }
    @PostMapping("/{id}/analyze") InvestigationSnapshot analyze(@PathVariable UUID id) { return orchestrator.analyze(id); }

    record CreateInvestigation(String tenantId, String service, String question) {}
}
