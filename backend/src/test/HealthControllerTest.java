package com.ras.safetyform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ras.safetyform.controller.HealthController;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthControllerTest {

    @Test
    void healthReturnsRunningStatus() {
        Map<String, String> response = new HealthController().health();

        assertEquals("running", response.get("status"));
    }
}
