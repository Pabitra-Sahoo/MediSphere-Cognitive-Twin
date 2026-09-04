package com.medisphere.vitals.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.config.KafkaConfig;
import com.medisphere.vitals.dto.VitalsEventDTO;
import com.medisphere.vitals.service.VitalsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens to the {@code vitals.ingest} topic and delegates to {@link VitalsService}.
 */
@Component
public class VitalsConsumer {

    private static final Logger log = LoggerFactory.getLogger(VitalsConsumer.class);

    private final VitalsService vitalsService;
    private final ObjectMapper objectMapper;

    public VitalsConsumer(VitalsService vitalsService, ObjectMapper objectMapper) {
        this.vitalsService = vitalsService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = KafkaConfig.VITALS_INGEST_TOPIC,
            groupId = "${spring.kafka.consumer.group-id:medisphere-consumer}"
    )
    public void consumeVitals(
            @Payload String payload,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(name = KafkaHeaders.OFFSET, required = false) Long offset) {

        log.info("Received vitals message from partition {} offset {} (key='{}')", partition, offset, key);

        try {
            VitalsEventDTO event = objectMapper.readValue(payload, VitalsEventDTO.class);
            vitalsService.processVitalsEvent(event);
        } catch (Exception e) {
            log.error("Failed to parse or process vitals payload from partition {} offset {}: {}",
                    partition, offset, e.getMessage(), e);
        }
    }
}
