package com.airline.overbooking.service;

import com.airline.overbooking.model.Flight;
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

class FlightManagementServiceTest {
    @TempDir
    Path temporaryDirectory;

    private FlightManagementService service;

    @BeforeEach
    void setUp() throws SQLException {
        SqliteDatabase database = new SqliteDatabase(temporaryDirectory.resolve("ui-flights.db"));
        SqliteBookingRepository bookings = new SqliteBookingRepository(database);
        service = new FlightManagementService(new SqliteFlightRepository(database, bookings));
    }

    @Test
    void supportsFlightManagementOperations() throws SQLException {
        Flight flight = new Flight("AB123", "JFK", "LAX", 180, List.of());
        service.createFlight(flight);

        assertEquals(1, service.listFlights().size());
        assertTrue(service.findFlight("AB123").isPresent());

        flight.setCapacity(200);
        assertTrue(service.updateFlight(flight));
        assertEquals(200, service.findFlight("AB123").orElseThrow().getCapacity());
        assertFalse(service.updateFlight(new Flight("MISSING", "JFK", "LAX", 100, List.of())));

        assertTrue(service.deleteFlight("AB123"));
        assertFalse(service.deleteFlight("AB123"));
    }
}