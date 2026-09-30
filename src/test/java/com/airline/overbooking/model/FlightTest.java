package com.airline.overbooking.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlightTest {
    @Test
    void createsFlightWithAllRequestedValues() {
        Passenger passenger = createPassenger();
        Flight flight = new Flight("AB123", "JFK", "LAX", 180, List.of(passenger));

        assertEquals("AB123", flight.getFlightNumber());
        assertEquals("JFK", flight.getOrigin());
        assertEquals("LAX", flight.getDestination());
        assertEquals(180, flight.getCapacity());
        assertEquals(List.of(passenger), flight.getBookings());
    }

    @Test
    void settersUpdateFlightValuesAndBookingsCanBeAdded() {
        Flight flight = new Flight("AB123", "JFK", "LAX", 180, List.of());
        Passenger passenger = createPassenger();

        flight.setFlightNumber("AB456");
        flight.setOrigin("SFO");
        flight.setDestination("SEA");
        flight.setCapacity(200);
        flight.addPassenger(passenger);

        assertEquals("AB456", flight.getFlightNumber());
        assertEquals("SFO", flight.getOrigin());
        assertEquals("SEA", flight.getDestination());
        assertEquals(200, flight.getCapacity());
        assertEquals(List.of(passenger), flight.getBookings());
    }

    @Test
    void rejectsInvalidFlightValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new Flight(" ", "JFK", "LAX", 180, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Flight("AB123", " ", "LAX", 180, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Flight("AB123", "JFK", " ", 180, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Flight("AB123", "JFK", "LAX", -1, List.of()));
    }

    private Passenger createPassenger() {
        return new Passenger("P-1001", "Avery Chen", 32, FareClass.ECONOMY,
                BookingStatus.CONFIRMED);
    }
}