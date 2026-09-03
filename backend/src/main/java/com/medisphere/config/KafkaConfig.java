package com.medisphere.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka topic configuration.
 *
 * <p>Defines the Kafka topics used by the application.
 * Topics are auto-created on startup if Kafka is reachable,
 * but startup is non-fatal if Kafka is temporarily offline.</p>
 */
@Configuration
public class KafkaConfig {

    public static final String VITALS_INGEST_TOPIC = "vitals.ingest";
    public static final String VITALS_VALIDATED_TOPIC = "vitals.validated";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.admin.auto-create:false}")
    private boolean autoCreateTopics;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        KafkaAdmin admin = new KafkaAdmin(configs);
        admin.setFatalIfBrokerNotAvailable(false);
        admin.setAutoCreate(autoCreateTopics);
        return admin;
    }

    @Bean
    public NewTopic vitalsIngestTopic() {
        return TopicBuilder.name(VITALS_INGEST_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic vitalsValidatedTopic() {
        return TopicBuilder.name(VITALS_VALIDATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

