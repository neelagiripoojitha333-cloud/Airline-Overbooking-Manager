package com.airline.overbooking.repository;

import com.airline.overbooking.algorithm.BTree;
import com.airline.overbooking.model.Passenger;

public class BookingIndex {
    private final BTree<String, Passenger> bookings;

    public BookingIndex() {
        this(3);
    }

    public BookingIndex(int minimumDegree) {
        bookings = new BTree<>(minimumDegree);
    }

    public int size() {
        return bookings.size();
    }

    public void insertBooking(String bookingId, Passenger passenger) {
        bookings.insert(requireBookingId(bookingId), requirePassenger(passenger));
    }

    public Passenger searchBooking(String bookingId) {
        return bookings.search(requireBookingId(bookingId));
    }

    public Passenger deleteBooking(String bookingId) {
        return bookings.delete(requireBookingId(bookingId));
    }

    public void upsertBooking(String bookingId, Passenger passenger) {
        String validBookingId = requireBookingId(bookingId);
        Passenger validPassenger = requirePassenger(passenger);
        if (!bookings.update(validBookingId, validPassenger)) {
            bookings.insert(validBookingId, validPassenger);
        }
    }

    private String requireBookingId(String bookingId) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("bookingId must not be blank");
        }
        return bookingId;
    }

    private Passenger requirePassenger(Passenger passenger) {
        if (passenger == null) {
            throw new IllegalArgumentException("passenger must not be null");
        }
        return passenger;
    }
}