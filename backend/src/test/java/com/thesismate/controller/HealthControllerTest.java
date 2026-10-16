package com.thesismate.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {

    @Test
    void healthReturnsUp() {
        var response = new HealthController().health();
        assertEquals("UP", response.get("status"));
    }
}
