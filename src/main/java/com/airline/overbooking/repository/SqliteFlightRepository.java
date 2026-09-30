package com.airline.overbooking.repository;

import com.airline.overbooking.model.Flight;
import com.airline.overbooking.model.Passenger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SqliteFlightRepository {
    private final SqliteDatabase database;
    private final SqliteBookingRepository bookingRepository;

    public SqliteFlightRepository(SqliteDatabase database, SqliteBookingRepository bookingRepository) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        this.bookingRepository = Objects.requireNonNull(bookingRepository, "bookingRepository must not be null");
    }

    public void insert(Flight flight) throws SQLException {
        Objects.requireNonNull(flight, "flight must not be null");
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO flights (flight_number, origin, destination, capacity)
                     VALUES (?, ?, ?, ?)
                     """)) {
            bindFlight(statement, flight);
            statement.executeUpdate();
        }
    }

    public Optional<Flight> findByFlightNumber(String flightNumber) throws SQLException {
        String validFlightNumber = requireText(flightNumber, "flightNumber");
        Flight flight;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT flight_number, origin, destination, capacity
                     FROM flights WHERE flight_number = ?
                     """)) {
            statement.setString(1, validFlightNumber);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                flight = readFlight(result);
            }
        }
        return Optional.of(withBookings(flight));
    }

    public List<Flight> findAll() throws SQLException {
        List<Flight> flights = new ArrayList<>();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT flight_number, origin, destination, capacity
                     FROM flights ORDER BY flight_number
                     """);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                flights.add(readFlight(result));
            }
        }

        for (int index = 0; index < flights.size(); index++) {
            flights.set(index, withBookings(flights.get(index)));
        }
        return flights;
    }

    public boolean update(Flight flight) throws SQLException {
        Objects.requireNonNull(flight, "flight must not be null");
        int updatedRows;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE flights SET origin = ?, destination = ?, capacity = ?
                     WHERE flight_number = ?
                     """)) {
            statement.setString(1, flight.getOrigin());
            statement.setString(2, flight.getDestination());
            statement.setInt(3, flight.getCapacity());
            statement.setString(4, flight.getFlightNumber());
            updatedRows = statement.executeUpdate();
        }
        return updatedRows > 0;
    }

    public boolean delete(String flightNumber) throws SQLException {
        String validFlightNumber = requireText(flightNumber, "flightNumber");
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM flights WHERE flight_number = ?")) {
            statement.setString(1, validFlightNumber);
            return statement.executeUpdate() > 0;
        }
    }

    private Flight withBookings(Flight flight) throws SQLException {
        List<Passenger> passengers = bookingRepository.findByFlightNumber(flight.getFlightNumber())
                .stream()
                .map(SqliteBookingRepository.BookingRecord::passenger)
                .toList();
        return new Flight(flight.getFlightNumber(), flight.getOrigin(), flight.getDestination(),
                flight.getCapacity(), passengers);
    }

    private static Flight readFlight(ResultSet result) throws SQLException {
        return new Flight(result.getString("flight_number"), result.getString("origin"),
                result.getString("destination"), result.getInt("capacity"), List.of());
    }

    private static void bindFlight(PreparedStatement statement, Flight flight) throws SQLException {
        statement.setString(1, flight.getFlightNumber());
        statement.setString(2, flight.getOrigin());
        statement.setString(3, flight.getDestination());
        statement.setInt(4, flight.getCapacity());
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}