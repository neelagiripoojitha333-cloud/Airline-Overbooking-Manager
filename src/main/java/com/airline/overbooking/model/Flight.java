package com.airline.overbooking.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Flight {
    private String flightNumber;
    private String origin;
    private String destination;
    private int capacity;
    private List<Passenger> bookings;

    public Flight(String flightNumber, String origin, String destination, int capacity,
                  List<Passenger> bookings) {
        setFlightNumber(flightNumber);
        setOrigin(origin);
        setDestination(destination);
        setCapacity(capacity);
        setBookings(bookings);
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = requireText(flightNumber, "flightNumber");
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = requireText(origin, "origin");
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = requireText(destination, "destination");
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        this.capacity = capacity;
    }

    public List<Passenger> getBookings() {
        return bookings;
    }

    public void setBookings(List<Passenger> bookings) {
        this.bookings = new ArrayList<>(Objects.requireNonNull(bookings, "bookings must not be null"));
    }

    public void addPassenger(Passenger passenger) {
        bookings.add(Objects.requireNonNull(passenger, "passenger must not be null"));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}