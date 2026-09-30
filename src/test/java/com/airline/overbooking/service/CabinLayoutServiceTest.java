package com.airline.overbooking.service;

import com.airline.overbooking.model.FareClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CabinLayoutServiceTest {
    private final CabinLayoutService service = new CabinLayoutService();

    @Test
    void createsOneSelectableSeatPerCapacityAndMapsAllCabins() {
        var seats = service.createSeats(180);

        assertEquals(180, seats.size());
        assertEquals("1A", seats.get(0).seatNumber());
        assertEquals("30F", seats.get(179).seatNumber());
        assertEquals(FareClass.FIRST, seats.get(0).fareClass());
        assertEquals(FareClass.BUSINESS, seats.get(12).fareClass());
        assertEquals(FareClass.PREMIUM_ECONOMY, seats.get(36).fareClass());
        assertEquals(FareClass.ECONOMY, seats.get(72).fareClass());
    }

    @Test
    void handlesPartialRowsAndZeroCapacity() {
        var seats = service.createSeats(8);

        assertEquals(8, seats.size());
        assertEquals("2B", seats.get(7).seatNumber());
        assertEquals(0, service.createSeats(0).size());
    }

    @Test
    void rejectsNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> service.createSeats(-1));
    }
}
