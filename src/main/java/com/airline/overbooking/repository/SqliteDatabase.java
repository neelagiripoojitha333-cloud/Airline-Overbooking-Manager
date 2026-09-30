package com.airline.overbooking.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class SqliteDatabase {
    private final String jdbcUrl;

    public SqliteDatabase(Path databasePath) throws SQLException {
        Path absolutePath = Objects.requireNonNull(databasePath, "databasePath must not be null")
                .toAbsolutePath()
                .normalize();
        try {
            Files.createDirectories(absolutePath.getParent());
        } catch (IOException exception) {
            throw new SQLException("Could not create the database directory", exception);
        }

        jdbcUrl = "jdbc:sqlite:" + absolutePath.toString().replace('\\', '/');
        initializeSchema();
    }

    public Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
        return connection;
    }

    private void initializeSchema() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS flights (
                        flight_number TEXT PRIMARY KEY,
                        origin TEXT NOT NULL,
                        destination TEXT NOT NULL,
                        capacity INTEGER NOT NULL CHECK (capacity >= 0)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS bookings (
                        booking_id TEXT PRIMARY KEY,
                        flight_number TEXT NOT NULL,
                        passenger_id TEXT NOT NULL,
                        passenger_name TEXT NOT NULL,
                        age INTEGER NOT NULL CHECK (age >= 0),
                        fare_class TEXT NOT NULL,
                        booking_status TEXT NOT NULL,
                        seat_number TEXT,
                        check_in_status TEXT NOT NULL DEFAULT 'NOT_CHECKED_IN',
                        checked_in_at TEXT,
                        resolution_status TEXT NOT NULL DEFAULT 'NONE',
                        previous_flight_number TEXT,
                        previous_checked_in_at TEXT,
                        keep_on_current INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (flight_number) REFERENCES flights(flight_number)
                            ON UPDATE CASCADE ON DELETE RESTRICT
                    )
                    """);
            ensureColumn(connection, "seat_number", "TEXT");
            ensureColumn(connection, "check_in_status", "TEXT NOT NULL DEFAULT 'NOT_CHECKED_IN'");
            ensureColumn(connection, "checked_in_at", "TEXT");
            ensureColumn(connection, "resolution_status", "TEXT NOT NULL DEFAULT 'NONE'");
            ensureColumn(connection, "previous_flight_number", "TEXT");
            ensureColumn(connection, "previous_checked_in_at", "TEXT");
            ensureColumn(connection, "keep_on_current", "INTEGER NOT NULL DEFAULT 0");
            statement.executeUpdate(
                    "CREATE INDEX IF NOT EXISTS bookings_flight_number_idx ON bookings(flight_number)");
            statement.executeUpdate("""
                    CREATE UNIQUE INDEX IF NOT EXISTS bookings_active_seat_idx
                    ON bookings(flight_number, seat_number)
                    WHERE seat_number IS NOT NULL AND booking_status IN ('CONFIRMED', 'BOARDED')
                    """);
        }
    }

    private static void ensureColumn(Connection connection, String columnName, String definition)
            throws SQLException {
        boolean columnExists = false;
        try (Statement statement = connection.createStatement();
             var columns = statement.executeQuery("PRAGMA table_info(bookings)")) {
            while (columns.next()) {
                if (columnName.equals(columns.getString("name"))) {
                    columnExists = true;
                    break;
                }
            }
        }
        if (!columnExists) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE bookings ADD COLUMN " + columnName + " " + definition);
            }
        }
    }
}
