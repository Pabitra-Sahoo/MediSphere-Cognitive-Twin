package com.medisphere.risk.client;

import com.medisphere.risk.exception.MlServiceUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MlServiceClientTest {

    @Test
    @DisplayName("predictAndExplain throws MlServiceUnavailableException when ML service is unreachable")
    void testPredictAndExplainUnreachable() {
        // Point to an unused local port with minimal timeout (200ms)
        MlServiceClient client = new MlServiceClient("http://127.0.0.1:54321", 200, "test-key");

        assertThrows(MlServiceUnavailableException.class, () ->
                client.predictAndExplain("CARDIOVASCULAR", null, Map.of("age", 50.0))
        );
    }

    @Test
    @DisplayName("getModelCatalog throws MlServiceUnavailableException when ML service is unreachable")
    void testGetModelCatalogUnreachable() {
        MlServiceClient client = new MlServiceClient("http://127.0.0.1:54321", 200, "test-key");

        assertThrows(MlServiceUnavailableException.class, client::getModelCatalog);
    }
}
