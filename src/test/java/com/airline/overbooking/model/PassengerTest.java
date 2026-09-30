package com.airline.overbooking.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassengerTest {
    @Test
    void createsPassengerWithAllRequestedValues() {
        Passenger passenger = new Passenger(
                "P-1001", "Avery Chen", 32, FareClass.BUSINESS, BookingStatus.CONFIRMED);

        assertEquals("P-1001", passenger.getPassengerId());
        assertEquals("Avery Chen", passenger.getName());
        assertEquals(32, passenger.getAge());
        assertEquals(FareClass.BUSINESS, passenger.getFareClass());
        assertEquals(BookingStatus.CONFIRMED, passenger.getBookingStatus());
        assertTrue(passenger.toString().contains("P-1001"));
    }

    @Test
    void settersUpdatePassengerValues() {
        Passenger passenger = createPassenger();

        passenger.setName("Jordan Lee");
        passenger.setAge(41);
        passenger.setFareClass(FareClass.FIRST);
        passenger.setBookingStatus(BookingStatus.BOARDED);

        assertEquals("Jordan Lee", passenger.getName());
        assertEquals(41, passenger.getAge());
        assertEquals(FareClass.FIRST, passenger.getFareClass());
        assertEquals(BookingStatus.BOARDED, passenger.getBookingStatus());
    }

    @Test
    void rejectsInvalidPassengerValues() {
        assertThrows(IllegalArgumentException.class, () -> new Passenger(
                " ", "Avery Chen", 32, FareClass.ECONOMY, BookingStatus.CONFIRMED));
        assertThrows(IllegalArgumentException.class, () -> new Passenger(
                "P-1001", " ", 32, FareClass.ECONOMY, BookingStatus.CONFIRMED));

        Passenger passenger = createPassenger();
        assertThrows(IllegalArgumentException.class, () -> passenger.setAge(-1));
    }

    private Passenger createPassenger() {
        return new Passenger("P-1001", "Avery Chen", 32, FareClass.ECONOMY,
                BookingStatus.CONFIRMED);
    }
}