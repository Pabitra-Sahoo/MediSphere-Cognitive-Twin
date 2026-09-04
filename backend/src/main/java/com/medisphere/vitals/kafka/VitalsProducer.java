package com.medisphere.vitals.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.config.KafkaConfig;
import com.medisphere.vitals.dto.VitalsEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for publishing streaming vitals events to the {@code vitals.ingest} topic.
 */
@Component
public class VitalsProducer {

    private static final Logger log = LoggerFactory.getLogger(VitalsProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public VitalsProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a vitals event to the ingest topic, partitioned by patientId.
     *
     * @param event the vitals event payload
     * @return CompletableFuture of the Kafka SendResult
     */
    public CompletableFuture<SendResult<String, String>> sendVitals(VitalsEventDTO event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String partitionKey = event.getPatientId();

            log.debug("Publishing vitals event '{}' for patient '{}' to topic '{}'",
                    event.getEventId(), partitionKey, KafkaConfig.VITALS_INGEST_TOPIC);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    KafkaConfig.VITALS_INGEST_TOPIC,
                    partitionKey,
                    payload
            );

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish vitals event '{}' for patient '{}': {}",
                            event.getEventId(), partitionKey, ex.getMessage(), ex);
                } else {
                    log.info("Successfully published vitals event '{}' for patient '{}' to offset {}",
                            event.getEventId(), partitionKey, result.getRecordMetadata().offset());
                }
            });

            return future;
        } catch (Exception e) {
            log.error("Serialization failure for vitals event '{}': {}", event.getEventId(), e.getMessage(), e);
            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }
}
