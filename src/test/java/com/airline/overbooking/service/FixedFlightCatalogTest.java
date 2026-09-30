package com.airline.overbooking.service;

import com.airline.overbooking.model.FareClass;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedFlightCatalogTest {
    private final FixedFlightCatalog catalog = new FixedFlightCatalog();

    @Test
    void providesExactlyTheTenSpecifiedFlightsWithFarePrices() {
        List<FixedFlightCatalog.ScheduledFlight> flights = catalog.flights();
        Set<String> flightNumbers = flights.stream()
                .map(scheduled -> scheduled.flight().getFlightNumber())
                .collect(Collectors.toSet());

        assertEquals(10, flights.size());
        assertEquals(Set.of("AI1123", "AI1434", "AI2125", "AI3056", "AI4178",
                "AI5289", "AI6310", "AI7421", "AI8532", "AI9643"), flightNumbers);
        assertEquals(5_500, flights.get(0).fares().get(FareClass.ECONOMY));
        assertEquals(450_000, flights.get(1).fares().get(FareClass.FIRST));
        assertEquals(95_000, flights.get(1).fares().get(FareClass.PREMIUM_ECONOMY));
        assertEquals(155_000, flights.get(9).fares().get(FareClass.FIRST));
        assertTrue(flights.stream().allMatch(flight -> flight.fares().size() == 4
                && flight.currency().equals("INR")));
    }

    @Test
    void suppliesCompleteFutureScheduleAndCountryDetails() {
        List<FixedFlightCatalog.ScheduledFlight> flights = catalog.flights();
        FixedFlightCatalog.ScheduledFlight london = flights.get(1);
        FixedFlightCatalog.ScheduledFlight tokyo = flights.get(3);
        FixedFlightCatalog.ScheduledFlight singapore = flights.get(5);

        assertTrue(flights.stream().allMatch(flight -> !flight.departureDate().isBefore(
                LocalDate.of(2026, 9, 30))));
        assertEquals("London", london.destinationCity());
        assertEquals("United Kingdom", london.destinationCountry());
        assertEquals(2, london.originFlag().codePointCount(0, london.originFlag().length()));
        assertEquals(LocalDate.of(2026, 10, 4), tokyo.arrivalDate());
        assertEquals(LocalDate.of(2026, 10, 6), singapore.arrivalDate());
        assertEquals(300, london.flight().getCapacity());
        assertEquals(180, flights.get(0).flight().getCapacity());
    }
}
