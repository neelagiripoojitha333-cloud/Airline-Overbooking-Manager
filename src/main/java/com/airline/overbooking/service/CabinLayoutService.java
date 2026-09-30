package com.airline.overbooking.service;

import com.airline.overbooking.model.FareClass;

import java.util.ArrayList;
import java.util.List;

public class CabinLayoutService {
    private static final char[] SEAT_LETTERS = {'A', 'B', 'C', 'D', 'E', 'F'};

    public List<Seat> createSeats(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        int rowCount = (int) ((capacity + 5L) / SEAT_LETTERS.length);
        int firstClassRows = percentageOfRows(rowCount, 5);
        int businessClassEndRow = Math.max(firstClassRows, percentageOfRows(rowCount, 20));
        int premiumEconomyEndRow = Math.max(businessClassEndRow, percentageOfRows(rowCount, 40));
        List<Seat> seats = new ArrayList<>(capacity);

        for (int seatIndex = 0; seatIndex < capacity; seatIndex++) {
            int row = seatIndex / SEAT_LETTERS.length + 1;
            char seatLetter = SEAT_LETTERS[seatIndex % SEAT_LETTERS.length];
            FareClass fareClass = fareClassForRow(row, firstClassRows,
                    businessClassEndRow, premiumEconomyEndRow);
            seats.add(new Seat(row + String.valueOf(seatLetter), row, seatLetter, fareClass));
        }
        return List.copyOf(seats);
    }

    private static int percentageOfRows(int rowCount, int percentage) {
        return (rowCount * percentage + 99) / 100;
    }

    private static FareClass fareClassForRow(int row, int firstClassRows,
                                             int businessClassEndRow, int premiumEconomyEndRow) {
        if (row <= firstClassRows) {
            return FareClass.FIRST;
        }
        if (row <= businessClassEndRow) {
            return FareClass.BUSINESS;
        }
        if (row <= premiumEconomyEndRow) {
            return FareClass.PREMIUM_ECONOMY;
        }
        return FareClass.ECONOMY;
    }

    public record Seat(String seatNumber, int row, char letter, FareClass fareClass) {
    }
}
