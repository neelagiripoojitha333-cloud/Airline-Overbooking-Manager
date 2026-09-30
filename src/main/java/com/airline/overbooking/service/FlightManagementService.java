package com.airline.overbooking.service;

import com.airline.overbooking.model.Flight;
import com.airline.overbooking.repository.SqliteFlightRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FlightManagementService {
    private final SqliteFlightRepository repository;

    public FlightManagementService(SqliteFlightRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public List<Flight> listFlights() throws SQLException {
        return repository.findAll();
    }

    public Optional<Flight> findFlight(String flightNumber) throws SQLException {
        return repository.findByFlightNumber(flightNumber);
    }

    public void createFlight(Flight flight) throws SQLException {
        repository.insert(Objects.requireNonNull(flight, "flight must not be null"));
    }

    public boolean updateFlight(Flight flight) throws SQLException {
        return repository.update(Objects.requireNonNull(flight, "flight must not be null"));
    }

    public boolean deleteFlight(String flightNumber) throws SQLException {
        return repository.delete(flightNumber);
    }
}