package dev.incidentcopilot.api.investigation;

import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class HypothesisCorrelator {
    public List<Hypothesis> correlate(List<Evidence> evidence) {
        List<Hypothesis> hypotheses = new ArrayList<>();
        hypotheses.add(score("DB_POOL_EXHAUSTION", "Payment database pool exhaustion", evidence,
                List.of(new Signal("searchLogs", "Injected database pool exhaustion", .55),
                        new Signal("queryMetrics", "DB_POOL_EXHAUSTION", .30),
                        new Signal("getDeploymentHistory", "2.4.1", .10))));
        hypotheses.add(score("KAFKA_CONSUMER_LAG", "Payment Kafka consumer lag", evidence,
                List.of(new Signal("searchLogs", "Injected Kafka consumer lag", .50),
                        new Signal("getKafkaConsumerLag", "positive-lag", .35),
                        new Signal("queryMetrics", "KAFKA_CONSUMER_LAG", .25))));
        hypotheses.add(score("NOTIFICATION_DELIVERY_FAILURE", "Notification delivery failure", evidence,
                List.of(new Signal("searchLogs", "Injected notification delivery failure", .55),
                        new Signal("queryMetrics", "NOTIFICATION_DELIVERY_FAILURE", .30))));
        return hypotheses.stream().sorted(Comparator.comparingDouble(Hypothesis::confidence).reversed()).toList();
    }

    private Hypothesis score(String code, String title, List<Evidence> evidence, List<Signal> signals) {
        List<Evidence> supporting = new ArrayList<>();
        double score = 0;
        for (Signal signal : signals) {
            Optional<Evidence> match = evidence.stream().filter(item -> item.tool().equals(signal.tool()) && matches(item, signal.marker())).findFirst();
            if (match.isPresent()) { score += signal.weight(); supporting.add(match.get()); }
        }
        score = Math.min(1, Math.round(score * 100.0) / 100.0);
        long independentTools = supporting.stream().map(Evidence::tool).distinct().count();
        Hypothesis.Assessment assessment = score >= .75 && independentTools >= 2 ? Hypothesis.Assessment.SUPPORTED
                : score >= .25 ? Hypothesis.Assessment.POSSIBLE : Hypothesis.Assessment.REJECTED;
        String rationale = supporting.isEmpty() ? "No matching evidence was collected."
                : "Matched " + supporting.size() + " signal(s) across " + independentTools + " independent tool(s).";
        return new Hypothesis(code, title, score, assessment, rationale, supporting.stream().map(Evidence::evidenceId).toList());
    }

    private boolean matches(Evidence evidence, String marker) {
        if (marker.equals("positive-lag")) return evidence.data().path("totalLag").asLong(0) > 0;
        return evidence.data().toString().contains(marker);
    }

    private record Signal(String tool, String marker, double weight) {}
}
