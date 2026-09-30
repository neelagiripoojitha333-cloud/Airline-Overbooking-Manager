package com.airline.overbooking.service;

import com.airline.overbooking.model.BookingStatus;
import com.airline.overbooking.model.FareClass;
import com.airline.overbooking.model.Passenger;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoShowPredictionServiceTest {
    private final NoShowPredictionService service = new NoShowPredictionService(
            "python", Path.of("src/main/python/no_show_predictor.py"));

    @Test
    void invokesPythonPredictorUsingPassengerAndHistoryFeatures() throws Exception {
        Passenger passenger = new Passenger("P-1001", "Avery Chen", 32,
                FareClass.ECONOMY, BookingStatus.CONFIRMED);

        double probability = service.estimateProbability(passenger, 2, 8, 1);

        assertEquals(0.2825, probability, 0.000001);
        assertTrue(probability >= 0.0 && probability <= 1.0);
    }

    @Test
    void rejectsInconsistentHistoryBeforeStartingPython() {
        Passenger passenger = new Passenger("P-1001", "Avery Chen", 32,
                FareClass.ECONOMY, BookingStatus.CONFIRMED);

        assertThrows(IllegalArgumentException.class,
                () -> service.estimateProbability(passenger, 2, 1, 7));
    }
}