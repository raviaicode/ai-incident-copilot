package dev.incidentcopilot.api.tools;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;

@Component
public class KafkaLagTool {
    private final AdminClient admin;
    KafkaLagTool(AdminClient admin) { this.admin = admin; }

    public KafkaLagResult get(String groupId, String topic) {
        validateIdentifier(groupId, "groupId"); validateIdentifier(topic, "topic");
        try {
            Map<TopicPartition, OffsetAndMetadata> committed = admin.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata().get(5, TimeUnit.SECONDS);
            List<TopicPartition> partitions = admin.describeTopics(List.of(topic)).allTopicNames().get(5, TimeUnit.SECONDS)
                    .get(topic).partitions().stream().map(info -> new TopicPartition(topic, info.partition())).toList();
            Map<TopicPartition, OffsetSpec> latest = new HashMap<>();
            partitions.forEach(partition -> latest.put(partition, OffsetSpec.latest()));
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets = admin.listOffsets(latest).all().get(5, TimeUnit.SECONDS);
            List<PartitionLag> values = partitions.stream().map(partition -> {
                long committedOffset = Optional.ofNullable(committed.get(partition)).map(OffsetAndMetadata::offset).orElse(0L);
                long endOffset = endOffsets.get(partition).offset();
                return new PartitionLag(partition.partition(), committedOffset, endOffset, Math.max(0, endOffset - committedOffset));
            }).toList();
            return new KafkaLagResult(groupId, topic, values.stream().mapToLong(PartitionLag::lag).sum(), values);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new InvestigationSourceException("Kafka lag lookup failed", exception);
        }
    }

    private void validateIdentifier(String value, String name) {
        if (value == null || !value.matches("[A-Za-z0-9._-]{1,100}")) throw new IllegalArgumentException(name + " is invalid");
    }

    public record KafkaLagResult(String groupId, String topic, long totalLag, List<PartitionLag> partitions) {}
    public record PartitionLag(int partition, long committedOffset, long endOffset, long lag) {}
}
