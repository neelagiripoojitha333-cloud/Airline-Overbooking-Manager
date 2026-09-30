package com.airline.overbooking.repository;

import com.airline.overbooking.model.BookingStatus;
import com.airline.overbooking.model.FareClass;
import com.airline.overbooking.model.Flight;
import com.airline.overbooking.model.Passenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteFlightRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    private SqliteBookingRepository bookingRepository;
    private SqliteFlightRepository flightRepository;

    @BeforeEach
    void setUp() throws SQLException {
        SqliteDatabase database = new SqliteDatabase(temporaryDirectory.resolve("airline.db"));
        bookingRepository = new SqliteBookingRepository(database);
        flightRepository = new SqliteFlightRepository(database, bookingRepository);
    }

    @Test
    void performsFlightCreateReadUpdateAndDelete() throws SQLException {
        Flight flight = new Flight("AB123", "JFK", "LAX", 180, List.of());
        flightRepository.insert(flight);

        Flight stored = flightRepository.findByFlightNumber("AB123").orElseThrow();
        assertEquals("JFK", stored.getOrigin());
        assertEquals(180, stored.getCapacity());

        stored.setOrigin("BOS");
        stored.setCapacity(200);
        assertTrue(flightRepository.update(stored));
        assertEquals("BOS", flightRepository.findByFlightNumber("AB123").orElseThrow().getOrigin());
        assertEquals(1, flightRepository.findAll().size());

        assertTrue(flightRepository.delete("AB123"));
        assertFalse(flightRepository.delete("AB123"));
        assertTrue(flightRepository.findByFlightNumber("AB123").isEmpty());
    }

    @Test
    void flightReadIncludesPersistedBookingsAndFlightDeleteIsRestricted() throws SQLException {
        flightRepository.insert(new Flight("AB123", "JFK", "LAX", 180, List.of()));
        Passenger passenger = new Passenger("P-1001", "Avery Chen", 32,
                FareClass.BUSINESS, BookingStatus.CONFIRMED);
        bookingRepository.insert("BOOK-1001", "AB123", passenger);

        Flight stored = flightRepository.findByFlightNumber("AB123").orElseThrow();
        assertEquals(1, stored.getBookings().size());
        assertEquals("P-1001", stored.getBookings().get(0).getPassengerId());
        assertThrows(SQLException.class, () -> flightRepository.delete("AB123"));
    }
}