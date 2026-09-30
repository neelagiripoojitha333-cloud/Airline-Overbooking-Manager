package com.airline.overbooking.repository;

import com.airline.overbooking.model.BookingStatus;
import com.airline.overbooking.model.FareClass;
import com.airline.overbooking.model.Flight;
import com.airline.overbooking.model.Passenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteBookingRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    private Path databasePath;
    private SqliteDatabase database;
    private SqliteFlightRepository flightRepository;
    private SqliteBookingRepository bookingRepository;

    @BeforeEach
    void setUp() throws SQLException {
        databasePath = temporaryDirectory.resolve("airline.db");
        database = new SqliteDatabase(databasePath);
        bookingRepository = new SqliteBookingRepository(database);
        flightRepository = new SqliteFlightRepository(database, bookingRepository);
        flightRepository.insert(new Flight("AB123", "JFK", "LAX", 180, List.of()));
        flightRepository.insert(new Flight("CD456", "SEA", "SFO", 140, List.of()));
    }

    @Test
    void performsBookingCreateReadUpdateAndDeleteAndSynchronizesIndex() throws SQLException {
        Passenger original = passenger("P-1001", BookingStatus.CONFIRMED);
        bookingRepository.insert("BOOK-1001", "AB123", original);

        SqliteBookingRepository.BookingRecord stored =
                bookingRepository.findByBookingId("BOOK-1001").orElseThrow();
        assertEquals("AB123", stored.flightNumber());
        assertEquals("P-1001", stored.passenger().getPassengerId());
        assertEquals("P-1001", bookingRepository.searchIndexedPassenger("BOOK-1001").getPassengerId());

        Passenger updated = passenger("P-1001", BookingStatus.BOARDED);
        assertTrue(bookingRepository.update("BOOK-1001", "CD456", updated));
        assertEquals("CD456", bookingRepository.findByBookingId("BOOK-1001").orElseThrow().flightNumber());
        assertEquals(BookingStatus.BOARDED,
                bookingRepository.searchIndexedPassenger("BOOK-1001").getBookingStatus());
        assertFalse(bookingRepository.update("MISSING", "AB123", updated));

        assertTrue(bookingRepository.delete("BOOK-1001"));
        assertFalse(bookingRepository.delete("BOOK-1001"));
        assertTrue(bookingRepository.findByBookingId("BOOK-1001").isEmpty());
        assertNull(bookingRepository.searchIndexedPassenger("BOOK-1001"));
        assertEquals(0, bookingRepository.indexedBookingCount());
    }

    @Test
    void rejectsDuplicateBookingIdsAndUnknownFlightsWithoutChangingIndex() throws SQLException {
        bookingRepository.insert("BOOK-1001", "AB123", passenger("P-1001", BookingStatus.CONFIRMED));

        assertThrows(SQLException.class, () -> bookingRepository.insert(
                "BOOK-1001", "AB123", passenger("P-1002", BookingStatus.CONFIRMED)));
        assertThrows(SQLException.class, () -> bookingRepository.insert(
                "BOOK-1002", "NO-FLIGHT", passenger("P-1003", BookingStatus.CONFIRMED)));

        assertEquals(1, bookingRepository.indexedBookingCount());
        assertEquals("P-1001", bookingRepository.searchIndexedPassenger("BOOK-1001").getPassengerId());
    }

    @Test
    void persistsSeatAndPreventsTwoActiveBookingsFromSharingIt() throws SQLException {
        bookingRepository.insert("BOOK-1001", "AB123",
                passenger("P-1001", BookingStatus.CONFIRMED), "1A");

        assertEquals("1A", bookingRepository.findByBookingId("BOOK-1001").orElseThrow().seatNumber());
        assertThrows(SQLException.class, () -> bookingRepository.insert("BOOK-1002", "AB123",
                passenger("P-1002", BookingStatus.CONFIRMED), "1A"));

        assertTrue(bookingRepository.update("BOOK-1001", "AB123",
                passenger("P-1001", BookingStatus.CANCELLED), null));
        bookingRepository.insert("BOOK-1002", "AB123",
                passenger("P-1002", BookingStatus.CONFIRMED), "1A");
        assertEquals("1A", bookingRepository.findByBookingId("BOOK-1002").orElseThrow().seatNumber());
    }

    @Test
    void migratesAnExistingBookingsTableWithoutSeatColumn() throws SQLException {
        Path legacyDatabasePath = temporaryDirectory.resolve("legacy-airline.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + legacyDatabasePath);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE flights (
                        flight_number TEXT PRIMARY KEY,
                        origin TEXT NOT NULL,
                        destination TEXT NOT NULL,
                        capacity INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE bookings (
                        booking_id TEXT PRIMARY KEY,
                        flight_number TEXT NOT NULL,
                        passenger_id TEXT NOT NULL,
                        passenger_name TEXT NOT NULL,
                        age INTEGER NOT NULL,
                        fare_class TEXT NOT NULL,
                        booking_status TEXT NOT NULL
                    )
                    """);
        }

        SqliteDatabase migratedDatabase = new SqliteDatabase(legacyDatabasePath);
        SqliteBookingRepository migratedRepository = new SqliteBookingRepository(migratedDatabase);
        new SqliteFlightRepository(migratedDatabase, migratedRepository)
                .insert(new Flight("EF789", "ORD", "MIA", 120, List.of()));
        migratedRepository.insert("BOOK-LEGACY", "EF789",
                passenger("P-LEGACY", BookingStatus.CONFIRMED), "1A");

        assertEquals("1A", migratedRepository.findByBookingId("BOOK-LEGACY").orElseThrow().seatNumber());
    }

    @Test
    void rebuildsBookingIndexFromDatabaseWhenRepositoryIsReopened() throws SQLException {
        bookingRepository.insert("BOOK-1001", "AB123", passenger("P-1001", BookingStatus.CONFIRMED));
        bookingRepository.insert("BOOK-1002", "CD456", passenger("P-1002", BookingStatus.WAITLISTED));

        SqliteDatabase reopenedDatabase = new SqliteDatabase(databasePath);
        SqliteBookingRepository reopenedRepository = new SqliteBookingRepository(reopenedDatabase);

        assertEquals(2, reopenedRepository.indexedBookingCount());
        assertEquals("P-1002", reopenedRepository.searchIndexedPassenger("BOOK-1002").getPassengerId());
        assertEquals(1, reopenedRepository.findByFlightNumber("CD456").size());
        assertEquals(2, reopenedRepository.findAll().size());
    }

    private Passenger passenger(String id, BookingStatus status) {
        return new Passenger(id, "Passenger " + id, 30, FareClass.ECONOMY, status);
    }
}