package com.airline.overbooking.repository;

import com.airline.overbooking.model.BookingStatus;
import com.airline.overbooking.model.CheckInStatus;
import com.airline.overbooking.model.FareClass;
import com.airline.overbooking.model.Passenger;
import com.airline.overbooking.model.ResolutionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SqliteBookingRepository {
    private final SqliteDatabase database;
    private final BookingIndex bookingIndex;

    public SqliteBookingRepository(SqliteDatabase database) throws SQLException {
        this.database = Objects.requireNonNull(database, "database must not be null");
        bookingIndex = new BookingIndex();
        rebuildIndex();
    }

    public void insert(String bookingId, String flightNumber, Passenger passenger) throws SQLException {
        insert(bookingId, flightNumber, passenger, null);
    }

    public void insert(String bookingId, String flightNumber, Passenger passenger, String seatNumber)
            throws SQLException {
        String validBookingId = requireText(bookingId, "bookingId");
        String validFlightNumber = requireText(flightNumber, "flightNumber");
        Passenger validPassenger = Objects.requireNonNull(passenger, "passenger must not be null");

        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO bookings (
                         booking_id, flight_number, passenger_id, passenger_name,
                         age, fare_class, booking_status, seat_number
                     ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, validBookingId);
            statement.setString(2, validFlightNumber);
            bindPassenger(statement, 3, validPassenger);
            statement.setString(8, seatNumber);
            statement.executeUpdate();
        }
        bookingIndex.upsertBooking(validBookingId, validPassenger);
    }

    public Optional<BookingRecord> findByBookingId(String bookingId) throws SQLException {
        String validBookingId = requireText(bookingId, "bookingId");
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT booking_id, flight_number, passenger_id, passenger_name,
                            age, fare_class, booking_status, seat_number, check_in_status,
                            checked_in_at, resolution_status, previous_flight_number,
                            previous_checked_in_at, keep_on_current
                     FROM bookings WHERE booking_id = ?
                     """)) {
            statement.setString(1, validBookingId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readBooking(result)) : Optional.empty();
            }
        }
    }

    public List<BookingRecord> findByFlightNumber(String flightNumber) throws SQLException {
        String validFlightNumber = requireText(flightNumber, "flightNumber");
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT booking_id, flight_number, passenger_id, passenger_name,
                            age, fare_class, booking_status, seat_number, check_in_status,
                            checked_in_at, resolution_status, previous_flight_number,
                            previous_checked_in_at, keep_on_current
                     FROM bookings WHERE flight_number = ? ORDER BY booking_id
                     """)) {
            statement.setString(1, validFlightNumber);
            try (ResultSet result = statement.executeQuery()) {
                List<BookingRecord> bookings = new ArrayList<>();
                while (result.next()) {
                    bookings.add(readBooking(result));
                }
                return bookings;
            }
        }
    }

    public List<BookingRecord> findAll() throws SQLException {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT booking_id, flight_number, passenger_id, passenger_name,
                            age, fare_class, booking_status, seat_number, check_in_status,
                            checked_in_at, resolution_status, previous_flight_number,
                            previous_checked_in_at, keep_on_current
                     FROM bookings ORDER BY booking_id
                     """);
             ResultSet result = statement.executeQuery()) {
            List<BookingRecord> bookings = new ArrayList<>();
            while (result.next()) {
                bookings.add(readBooking(result));
            }
            return bookings;
        }
    }

    public boolean update(String bookingId, String flightNumber, Passenger passenger) throws SQLException {
        Optional<BookingRecord> existing = findByBookingId(bookingId);
        return existing.isPresent() && update(bookingId, flightNumber, passenger, existing.get().seatNumber());
    }

    public boolean update(String bookingId, String flightNumber, Passenger passenger, String seatNumber)
            throws SQLException {
        String validBookingId = requireText(bookingId, "bookingId");
        String validFlightNumber = requireText(flightNumber, "flightNumber");
        Passenger validPassenger = Objects.requireNonNull(passenger, "passenger must not be null");
        int updatedRows;

        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE bookings
                     SET flight_number = ?, passenger_id = ?, passenger_name = ?, age = ?,
                         fare_class = ?, booking_status = ?, seat_number = ?
                     WHERE booking_id = ?
                     """)) {
            statement.setString(1, validFlightNumber);
            bindPassenger(statement, 2, validPassenger);
            statement.setString(7, seatNumber);
            statement.setString(8, validBookingId);
            updatedRows = statement.executeUpdate();
        }

        if (updatedRows > 0) {
            bookingIndex.upsertBooking(validBookingId, validPassenger);
            return true;
        }
        return false;
    }

    public boolean delete(String bookingId) throws SQLException {
        String validBookingId = requireText(bookingId, "bookingId");
        int deletedRows;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM bookings WHERE booking_id = ?")) {
            statement.setString(1, validBookingId);
            deletedRows = statement.executeUpdate();
        }

        if (deletedRows > 0) {
            bookingIndex.deleteBooking(validBookingId);
            return true;
        }
        return false;
    }

    public boolean checkIn(String bookingId, Instant checkedInAt) throws SQLException {
        String validBookingId = requireText(bookingId, "bookingId");
        Objects.requireNonNull(checkedInAt, "checkedInAt must not be null");
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE bookings
                            SET check_in_status = 'CHECKED_IN', checked_in_at = ?
                            WHERE booking_id = ? AND booking_status = 'CONFIRMED'
                                AND check_in_status = 'NOT_CHECKED_IN'
                                AND resolution_status <> 'REACCOMMODATED'
                     """)) {
            statement.setString(1, checkedInAt.toString());
            statement.setString(2, validBookingId);
            return statement.executeUpdate() > 0;
        }
    }

    public void applyPhysicalSeatAssignments(String flightNumber, Map<String, String> assignments,
                                             Set<String> overCapacityBookingIds) throws SQLException {
        String validFlightNumber = requireText(flightNumber, "flightNumber");
        Objects.requireNonNull(assignments, "assignments must not be null");
        Objects.requireNonNull(overCapacityBookingIds, "overCapacityBookingIds must not be null");
        if (assignments.size() > flightCapacity(validFlightNumber)) {
            throw new IllegalArgumentException("Assigned passenger count exceeds aircraft capacity");
        }
        Set<String> assignedSeats = new HashSet<>();
        for (Map.Entry<String, String> assignment : assignments.entrySet()) {
            validateSeatNumber(assignment.getValue(), flightCapacity(validFlightNumber));
            if (!assignedSeats.add(assignment.getValue())) {
                throw new IllegalArgumentException("A physical seat cannot be assigned more than once");
            }
            requireText(assignment.getKey(), "bookingId");
        }
        if (assignments.keySet().stream().anyMatch(overCapacityBookingIds::contains)) {
            throw new IllegalArgumentException("A booking cannot be seated and over-capacity simultaneously");
        }

        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement clear = connection.prepareStatement("""
                        UPDATE bookings SET seat_number = NULL
                        WHERE flight_number = ? AND booking_status IN ('CONFIRMED', 'BOARDED')
                        """)) {
                    clear.setString(1, validFlightNumber);
                    clear.executeUpdate();
                }
                try (PreparedStatement assign = connection.prepareStatement("""
                        UPDATE bookings
                        SET seat_number = ?, resolution_status = 'NONE'
                        WHERE booking_id = ? AND flight_number = ?
                            AND booking_status = 'CONFIRMED' AND check_in_status = 'CHECKED_IN'
                        """);
                     PreparedStatement markExcess = connection.prepareStatement("""
                        UPDATE bookings SET seat_number = NULL, resolution_status = 'OVER_CAPACITY'
                        WHERE booking_id = ? AND flight_number = ?
                            AND booking_status = 'CONFIRMED' AND check_in_status = 'CHECKED_IN'
                        """)) {
                    for (Map.Entry<String, String> assignment : assignments.entrySet()) {
                        assign.setString(1, assignment.getValue());
                        assign.setString(2, assignment.getKey());
                        assign.setString(3, validFlightNumber);
                        if (assign.executeUpdate() != 1) {
                            throw new SQLException("Booking is no longer eligible for a seat: "
                                    + assignment.getKey());
                        }
                    }
                    for (String bookingId : overCapacityBookingIds) {
                        markExcess.setString(1, bookingId);
                        markExcess.setString(2, validFlightNumber);
                        if (markExcess.executeUpdate() != 1) {
                            throw new SQLException("Booking is no longer eligible for over-capacity status: "
                                    + bookingId);
                        }
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public boolean assignSeatToBooking(String bookingId, String flightNumber, String seatNumber)
            throws SQLException {
        String validBookingId = requireText(bookingId, "bookingId");
        String validFlightNumber = requireText(flightNumber, "flightNumber");
        int capacity = flightCapacity(validFlightNumber);
        validateSeatNumber(seatNumber, capacity);
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE bookings SET seat_number = ?
                     WHERE booking_id = ? AND flight_number = ? AND booking_status = 'CONFIRMED'
                     """)) {
            statement.setString(1, seatNumber);
            statement.setString(2, validBookingId);
            statement.setString(3, validFlightNumber);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean markKeepOnCurrentFlight(String bookingId, boolean keep) throws SQLException {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE bookings SET keep_on_current = ?
                     WHERE booking_id = ? AND resolution_status = 'OVER_CAPACITY'
                     """)) {
            statement.setInt(1, keep ? 1 : 0);
            statement.setString(2, requireText(bookingId, "bookingId"));
            return statement.executeUpdate() > 0;
        }
    }

    public boolean markOverCapacityResolved(String bookingId) throws SQLException {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE bookings SET resolution_status = 'RESOLVED', seat_number = NULL
                     WHERE booking_id = ? AND resolution_status = 'OVER_CAPACITY'
                     """)) {
            statement.setString(1, requireText(bookingId, "bookingId"));
            return statement.executeUpdate() > 0;
        }
    }

    public boolean reaccommodate(String bookingId, String nextFlightNumber) throws SQLException {
        String validBookingId = requireText(bookingId, "bookingId");
        String validNextFlight = requireText(nextFlightNumber, "nextFlightNumber");
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE bookings
                     SET previous_flight_number = flight_number,
                         previous_checked_in_at = checked_in_at,
                         flight_number = ?, seat_number = NULL,
                         check_in_status = 'NOT_CHECKED_IN', checked_in_at = NULL,
                         resolution_status = 'REACCOMMODATED', keep_on_current = 0
                     WHERE booking_id = ? AND resolution_status = 'OVER_CAPACITY'
                     """)) {
            statement.setString(1, validNextFlight);
            statement.setString(2, validBookingId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean undoResolution(String bookingId) throws SQLException {
        Optional<BookingRecord> booking = findByBookingId(bookingId);
        if (booking.isEmpty()) {
            return false;
        }
        BookingRecord record = booking.get();
        if (record.resolutionStatus() == ResolutionStatus.REACCOMMODATED) {
            try (Connection connection = database.openConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         UPDATE bookings
                         SET flight_number = previous_flight_number, seat_number = NULL,
                             check_in_status = 'CHECKED_IN', checked_in_at = previous_checked_in_at,
                             resolution_status = 'OVER_CAPACITY', previous_flight_number = NULL,
                             previous_checked_in_at = NULL, keep_on_current = 0
                         WHERE booking_id = ? AND resolution_status = 'REACCOMMODATED'
                         """)) {
                statement.setString(1, record.bookingId());
                return statement.executeUpdate() > 0;
            }
        }
        if (record.resolutionStatus() == ResolutionStatus.RESOLVED) {
            try (Connection connection = database.openConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         UPDATE bookings SET resolution_status = 'OVER_CAPACITY'
                         WHERE booking_id = ? AND resolution_status = 'RESOLVED'
                         """)) {
                statement.setString(1, record.bookingId());
                return statement.executeUpdate() > 0;
            }
        }
        if (record.keepOnCurrent()) {
            return markKeepOnCurrentFlight(record.bookingId(), false);
        }
        return false;
    }

    public Passenger searchIndexedPassenger(String bookingId) {
        return bookingIndex.searchBooking(requireText(bookingId, "bookingId"));
    }

    public int indexedBookingCount() {
        return bookingIndex.size();
    }

    public int flightCapacity(String flightNumber) throws SQLException {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT capacity FROM flights WHERE flight_number = ?")) {
            statement.setString(1, requireText(flightNumber, "flightNumber"));
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Flight not found: " + flightNumber);
                }
                return result.getInt("capacity");
            }
        }
    }

    private void rebuildIndex() throws SQLException {
        for (BookingRecord booking : findAll()) {
            bookingIndex.upsertBooking(booking.bookingId(), booking.passenger());
        }
    }

    private static BookingRecord readBooking(ResultSet result) throws SQLException {
        Passenger passenger = new Passenger(
                result.getString("passenger_id"),
                result.getString("passenger_name"),
                result.getInt("age"),
                FareClass.valueOf(result.getString("fare_class")),
                BookingStatus.valueOf(result.getString("booking_status"))
        );
        return new BookingRecord(
                result.getString("booking_id"), result.getString("flight_number"), passenger,
                result.getString("seat_number"),
                CheckInStatus.valueOf(result.getString("check_in_status")),
                parseInstant(result.getString("checked_in_at")),
                ResolutionStatus.valueOf(result.getString("resolution_status")),
                result.getString("previous_flight_number"),
                parseInstant(result.getString("previous_checked_in_at")),
                result.getInt("keep_on_current") != 0);
    }

    private static Instant parseInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }

    private static void validateSeatNumber(String seatNumber, int capacity) {
        String validSeat = requireText(seatNumber, "seatNumber").toUpperCase();
        if (validSeat.length() < 2) {
            throw new IllegalArgumentException("seatNumber must contain a row and seat letter");
        }
        char letter = validSeat.charAt(validSeat.length() - 1);
        int column = switch (letter) {
            case 'A' -> 0;
            case 'B' -> 1;
            case 'C' -> 2;
            case 'D' -> 3;
            case 'E' -> 4;
            case 'F' -> 5;
            default -> throw new IllegalArgumentException("seatNumber must end in A-F");
        };
        int row;
        try {
            row = Integer.parseInt(validSeat.substring(0, validSeat.length() - 1));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("seatNumber must start with a positive row number", exception);
        }
        long seatIndex = (long) (row - 1) * 6 + column;
        if (row < 1 || seatIndex >= capacity) {
            throw new IllegalArgumentException("seatNumber is outside this aircraft's physical capacity");
        }
    }
    private static void bindPassenger(PreparedStatement statement, int startIndex, Passenger passenger)
            throws SQLException {
        statement.setString(startIndex, passenger.getPassengerId());
        statement.setString(startIndex + 1, passenger.getName());
        statement.setInt(startIndex + 2, passenger.getAge());
        statement.setString(startIndex + 3, passenger.getFareClass().name());
        statement.setString(startIndex + 4, passenger.getBookingStatus().name());
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public record BookingRecord(String bookingId, String flightNumber, Passenger passenger, String seatNumber,
                                CheckInStatus checkInStatus, Instant checkedInAt,
                                ResolutionStatus resolutionStatus, String previousFlightNumber,
                                Instant previousCheckedInAt, boolean keepOnCurrent) {
    }
}