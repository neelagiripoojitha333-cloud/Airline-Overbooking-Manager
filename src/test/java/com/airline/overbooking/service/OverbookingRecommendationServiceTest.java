package com.airline.overbooking.service;

import com.airline.overbooking.model.BookingStatus;
import com.airline.overbooking.model.FareClass;
import com.airline.overbooking.model.Flight;
import com.airline.overbooking.model.Passenger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OverbookingRecommendationServiceTest {
    @Test
    void recommendsOnlyAdditionsWithinTheRiskLimitAndUsesFareClass() throws Exception {
        List<FareClass> estimatedFareClasses = new ArrayList<>();
        OverbookingRecommendationService service = new OverbookingRecommendationService((passenger, days) -> {
            estimatedFareClasses.add(passenger.getFareClass());
            return passenger.getFareClass() == FareClass.FIRST ? 0.05 : 0.20;
        });
        Flight flight = new Flight("AB123", "JFK", "LAX", 3, List.of(
                passenger("P-1", FareClass.ECONOMY, BookingStatus.CONFIRMED),
                passenger("P-2", FareClass.BUSINESS, BookingStatus.CONFIRMED),
                passenger("P-3", FareClass.FIRST, BookingStatus.WAITLISTED),
                passenger("P-4", FareClass.FIRST, BookingStatus.CANCELLED)));

        OverbookingRecommendationService.Recommendation result = service.recommend(flight, 7);

        assertEquals(3, result.capacity());
        assertEquals(2, result.confirmedBookings());
        assertEquals(0.40, result.predictedNoShows(), 0.000001);
        assertEquals(1, result.recommendedAdditionalBookings());
        assertEquals(0.0, result.overCapacityProbability(), 0.000001);
        assertEquals(OverbookingRecommendationService.MAX_OVER_CAPACITY_PROBABILITY,
                result.probabilityLimit());
        assertEquals(List.of(FareClass.ECONOMY, FareClass.BUSINESS, FareClass.FIRST), estimatedFareClasses);
    }

    @Test
    void recommendsNoAdditionalBookingsWhenCurrentRiskExceedsLimit() throws Exception {
        OverbookingRecommendationService service = new OverbookingRecommendationService(
                (passenger, days) -> 0.01);
        Flight flight = new Flight("AB123", "JFK", "LAX", 1, List.of(
                passenger("P-1", FareClass.ECONOMY, BookingStatus.CONFIRMED),
                passenger("P-2", FareClass.BUSINESS, BookingStatus.CONFIRMED)));

        OverbookingRecommendationService.Recommendation result = service.recommend(flight, 2);

        assertEquals(0, result.recommendedAdditionalBookings());
        assertEquals(0.9801, result.overCapacityProbability(), 0.000001);
    }

    @Test
    void stopsBeforeTheNextBookingWouldCrossTheRiskCeiling() throws Exception {
        OverbookingRecommendationService service = new OverbookingRecommendationService(
                (passenger, days) -> passenger.getFareClass() == FareClass.FIRST ? 0.05 : 0.95);
        Flight flight = new Flight("AB123", "JFK", "LAX", 1, List.of(
                passenger("P-1", FareClass.ECONOMY, BookingStatus.CONFIRMED),
                passenger("P-2", FareClass.BUSINESS, BookingStatus.CONFIRMED)));

        OverbookingRecommendationService.Recommendation result = service.recommend(flight, 7);

        assertEquals(0, result.recommendedAdditionalBookings());
        assertEquals(0.0025, result.overCapacityProbability(), 0.000001);
        assertEquals(2, result.confirmedBookings());
    }

    @Test
    void rejectsNegativeDepartureDaysAndInvalidEstimatorProbabilities() {
        Flight emptyFlight = new Flight("AB123", "JFK", "LAX", 10, List.of());
        OverbookingRecommendationService service = new OverbookingRecommendationService(
                (passenger, days) -> 1.5);
        Flight confirmedFlight = new Flight("CD456", "SEA", "SFO", 10, List.of(
                passenger("P-1", FareClass.ECONOMY, BookingStatus.CONFIRMED)));

        assertThrows(IllegalArgumentException.class, () -> service.recommend(emptyFlight, -1));
        assertThrows(IllegalStateException.class, () -> service.recommend(confirmedFlight, 7));
    }

    private Passenger passenger(String id, FareClass fareClass, BookingStatus status) {
        return new Passenger(id, "Passenger " + id, 30, fareClass, status);
    }
}
