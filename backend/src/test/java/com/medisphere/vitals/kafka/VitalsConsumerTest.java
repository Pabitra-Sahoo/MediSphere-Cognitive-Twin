package com.medisphere.vitals.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medisphere.vitals.dto.VitalsEventDTO;
import com.medisphere.vitals.service.VitalsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VitalsConsumerTest {

    @Mock
    private VitalsService vitalsService;

    private ObjectMapper objectMapper;
    private VitalsConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new VitalsConsumer(vitalsService, objectMapper);
    }

    @Test
    @DisplayName("Valid JSON payload parses correctly and delegates to VitalsService")
    void testConsumeValidMessage() {
        String json = "{\n" +
                "  \"eventId\": \"evt-test-1\",\n" +
                "  \"patientId\": \"pat-001\",\n" +
                "  \"deviceId\": \"DEV-100\",\n" +
                "  \"heartRate\": 76.0,\n" +
                "  \"systolicBP\": 122.0,\n" +
                "  \"diastolicBP\": 82.0,\n" +
                "  \"oxygenSaturation\": 98.0,\n" +
                "  \"temperature\": 36.6,\n" +
                "  \"respiratoryRate\": 16.0,\n" +
                "  \"source\": \"WEARABLE\"\n" +
                "}";

        consumer.consumeVitals(json, "pat-001", 0, 100L);

        ArgumentCaptor<VitalsEventDTO> captor = ArgumentCaptor.forClass(VitalsEventDTO.class);
        verify(vitalsService).processVitalsEvent(captor.capture());

        VitalsEventDTO captured = captor.getValue();
        assertNotNull(captured);
        assertEquals("evt-test-1", captured.getEventId());
        assertEquals("pat-001", captured.getPatientId());
        assertEquals(76.0, captured.getHeartRate());
    }

    @Test
    @DisplayName("Malformed JSON payload does not bubble exception and skips service invocation")
    void testConsumeMalformedMessage() {
        String invalidJson = "{ invalid json content !!!";

        // Should not throw
        consumer.consumeVitals(invalidJson, "key", 0, 101L);

        verify(vitalsService, never()).processVitalsEvent(any());
    }
}
