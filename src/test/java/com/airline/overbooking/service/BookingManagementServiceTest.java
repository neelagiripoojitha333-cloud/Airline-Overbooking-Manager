package com.airline.overbooking.service;

import com.airline.overbooking.model.BookingStatus;
import com.airline.overbooking.model.FareClass;
import com.airline.overbooking.model.Flight;
import com.airline.overbooking.model.Passenger;
import com.airline.overbooking.repository.SqliteBookingRepository;
import com.airline.overbooking.repository.SqliteDatabase;
import com.airline.overbooking.repository.SqliteFlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingManagementServiceTest {
    @TempDir
    Path temporaryDirectory;

    private BookingManagementService service;
    private SqliteBookingRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        SqliteDatabase database = new SqliteDatabase(temporaryDirectory.resolve("ui-bookings.db"));
        repository = new SqliteBookingRepository(database);
        service = new BookingManagementService(repository);
        new SqliteFlightRepository(database, repository)
                .insert(new Flight("AB123", "JFK", "LAX", 180, List.of()));
    }

    @Test
    void updatesAndCancelsWithoutRemovingBookingFromIndex() throws SQLException {
        Passenger passenger = new Passenger("P-1001", "Avery Chen", 32,
                FareClass.ECONOMY, BookingStatus.CONFIRMED);
        service.createBooking("BOOK-1001", "AB123", passenger);

        Passenger updated = new Passenger("P-1001", "Avery Chen", 32,
                FareClass.BUSINESS, BookingStatus.CONFIRMED);
        assertTrue(service.updateBooking("BOOK-1001", "AB123", updated));
        assertEquals(FareClass.BUSINESS,
                repository.searchIndexedPassenger("BOOK-1001").getFareClass());

        assertTrue(service.cancelBooking("BOOK-1001"));
        assertEquals(BookingStatus.CANCELLED,
                repository.searchIndexedPassenger("BOOK-1001").getBookingStatus());
        assertEquals(1, repository.indexedBookingCount());
        assertFalse(service.cancelBooking("MISSING"));
    }
}