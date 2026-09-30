package com.airline.overbooking.repository;

import com.airline.overbooking.model.BookingStatus;
import com.airline.overbooking.model.FareClass;
import com.airline.overbooking.model.Passenger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingIndexTest {
    @Test
    void insertsAndSearchesBookingsByBookingId() {
        BookingIndex index = new BookingIndex();
        Passenger passenger = passenger("P-1001");

        index.insertBooking("BOOK-1001", passenger);

        assertSame(passenger, index.searchBooking("BOOK-1001"));
        assertNull(index.searchBooking("BOOK-MISSING"));
        assertEquals(1, index.size());
    }

    @Test
    void supportsMultipleBookingsAndDeletion() {
        BookingIndex index = new BookingIndex(2);
        Passenger first = passenger("P-1001");
        Passenger second = passenger("P-1002");
        Passenger third = passenger("P-1003");

        index.insertBooking("BOOK-2", second);
        index.insertBooking("BOOK-1", first);
        index.insertBooking("BOOK-3", third);

        assertSame(first, index.searchBooking("BOOK-1"));
        assertSame(second, index.deleteBooking("BOOK-2"));
        assertNull(index.searchBooking("BOOK-2"));
        assertSame(third, index.searchBooking("BOOK-3"));
        assertEquals(2, index.size());
    }

    @Test
    void duplicateBookingIdsAreRejected() {
        BookingIndex index = new BookingIndex();
        Passenger first = passenger("P-1001");
        index.insertBooking("BOOK-1001", first);

        assertThrows(IllegalArgumentException.class,
                () -> index.insertBooking("BOOK-1001", passenger("P-1002")));

        assertSame(first, index.searchBooking("BOOK-1001"));
        assertEquals(1, index.size());
    }

    @Test
    void upsertUpdatesExistingBookingsAndInsertsMissingOnes() {
        BookingIndex index = new BookingIndex();
        Passenger original = passenger("P-1001");
        Passenger replacement = passenger("P-2001");
        index.insertBooking("BOOK-1001", original);

        index.upsertBooking("BOOK-1001", replacement);
        index.upsertBooking("BOOK-1002", original);

        assertSame(replacement, index.searchBooking("BOOK-1001"));
        assertSame(original, index.searchBooking("BOOK-1002"));
        assertEquals(2, index.size());
    }

    private Passenger passenger(String passengerId) {
        return new Passenger(passengerId, "Avery Chen", 32, FareClass.ECONOMY,
                BookingStatus.CONFIRMED);
    }
}