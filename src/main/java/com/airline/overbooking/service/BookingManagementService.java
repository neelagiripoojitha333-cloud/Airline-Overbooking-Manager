package com.airline.overbooking.service;

import com.airline.overbooking.model.BookingStatus;
import com.airline.overbooking.model.Passenger;
import com.airline.overbooking.repository.SqliteBookingRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BookingManagementService {
    private final SqliteBookingRepository repository;

    public BookingManagementService(SqliteBookingRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public List<SqliteBookingRepository.BookingRecord> listBookings(String flightNumber)
            throws SQLException {
        return repository.findByFlightNumber(flightNumber);
    }

    public Optional<SqliteBookingRepository.BookingRecord> findBooking(String bookingId)
            throws SQLException {
        return repository.findByBookingId(bookingId);
    }

    public void createBooking(String bookingId, String flightNumber, Passenger passenger)
            throws SQLException {
        createBooking(bookingId, flightNumber, passenger, null);
    }

    public void createBooking(String bookingId, String flightNumber, Passenger passenger, String seatNumber)
            throws SQLException {
        repository.insert(bookingId, flightNumber, Objects.requireNonNull(passenger,
                "passenger must not be null"), seatNumber);
    }

    public boolean updateBooking(String bookingId, String flightNumber, Passenger passenger)
            throws SQLException {
        return repository.update(bookingId, flightNumber, Objects.requireNonNull(passenger,
                "passenger must not be null"));
    }

            public boolean updateBooking(String bookingId, String flightNumber, Passenger passenger, String seatNumber)
                throws SQLException {
            return repository.update(bookingId, flightNumber, Objects.requireNonNull(passenger,
                "passenger must not be null"), seatNumber);
            }

    public boolean cancelBooking(String bookingId) throws SQLException {
        Optional<SqliteBookingRepository.BookingRecord> booking = repository.findByBookingId(bookingId);
        if (booking.isEmpty()) {
            return false;
        }

        Passenger passenger = booking.get().passenger();
        Passenger cancelledPassenger = new Passenger(passenger.getPassengerId(), passenger.getName(),
                passenger.getAge(), passenger.getFareClass(), BookingStatus.CANCELLED);
            return repository.update(bookingId, booking.get().flightNumber(), cancelledPassenger, null);
    }
}