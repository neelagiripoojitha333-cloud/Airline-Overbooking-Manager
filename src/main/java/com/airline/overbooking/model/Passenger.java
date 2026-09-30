package com.airline.overbooking.model;

import java.util.Objects;

public class Passenger {
    private String passengerId;
    private String name;
    private int age;
    private FareClass fareClass;
    private BookingStatus bookingStatus;

    public Passenger(String passengerId, String name, int age, FareClass fareClass,
                     BookingStatus bookingStatus) {
        setPassengerId(passengerId);
        setName(name);
        setAge(age);
        setFareClass(fareClass);
        setBookingStatus(bookingStatus);
    }

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = requireText(passengerId, "passengerId");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "name");
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("age must not be negative");
        }
        this.age = age;
    }

    public FareClass getFareClass() {
        return fareClass;
    }

    public void setFareClass(FareClass fareClass) {
        this.fareClass = Objects.requireNonNull(fareClass, "fareClass must not be null");
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = Objects.requireNonNull(bookingStatus, "bookingStatus must not be null");
    }

    @Override
    public String toString() {
        return "Passenger{" +
                "passengerId='" + passengerId + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", fareClass=" + fareClass +
                ", bookingStatus=" + bookingStatus +
                '}';
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}