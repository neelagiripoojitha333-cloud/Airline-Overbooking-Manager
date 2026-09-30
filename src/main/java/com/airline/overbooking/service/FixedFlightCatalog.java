package com.airline.overbooking.service;

import com.airline.overbooking.model.FareClass;
import com.airline.overbooking.model.Flight;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public final class FixedFlightCatalog {
    private static final int DOMESTIC_CAPACITY = 180;
    private static final int INTERNATIONAL_CAPACITY = 300;

    public List<ScheduledFlight> flights() {
        return List.of(
                flight("AI1123", "Hyderabad", "HYD", "India", "\uD83C\uDDEE\uD83C\uDDF3",
                        "Delhi", "DEL", "India", "\uD83C\uDDEE\uD83C\uDDF3", 30, 9,
                        8, 30, 10, 55, 0, 2, 25, DOMESTIC_CAPACITY, 5_500, 9_500, 18_000, 32_000),
                flight("AI1434", "Delhi", "DEL", "India", "\uD83C\uDDEE\uD83C\uDDF3",
                        "London", "LHR", "United Kingdom", "\uD83C\uDDEC\uD83C\uDDEC", 1, 10,
                        2, 15, 7, 45, 0, 10, 0, INTERNATIONAL_CAPACITY,
                        48_000, 95_000, 240_000, 450_000),
                flight("AI2125", "Mumbai", "BOM", "India", "\uD83C\uDDEE\uD83C\uDDF3",
                        "Paris", "CDG", "France", "\uD83C\uDDEB\uD83C\uDDF7", 2, 10,
                        1, 30, 7, 20, 0, 9, 20, INTERNATIONAL_CAPACITY,
                        42_000, 82_000, 210_000, 380_000),
                flight("AI3056", "Delhi", "DEL", "India", "\uD83C\uDDEE\uD83C\uDDF3",
                        "Tokyo", "NRT", "Japan", "\uD83C\uDDEF\uD83C\uDDF5", 3, 10,
                        22, 0, 8, 50, 1, 7, 20, INTERNATIONAL_CAPACITY,
                        55_000, 105_000, 260_000, 480_000),
                flight("AI4178", "Mumbai", "BOM", "India", "\uD83C\uDDEE\uD83C\uDDF3",
                        "Dubai", "DXB", "United Arab Emirates", "\uD83C\uDDE6\uD83C\uDDEA", 4, 10,
                        4, 0, 5, 50, 0, 3, 20, INTERNATIONAL_CAPACITY,
                        22_000, 41_000, 95_000, 160_000),
                flight("AI5289", "Bengaluru", "BLR", "India", "\uD83C\uDDEE\uD83C\uDDF3",
                        "Singapore", "SIN", "Singapore", "\uD83C\uDDF8\uD83C\uDDEC", 5, 10,
                        23, 0, 6, 0, 1, 4, 30, INTERNATIONAL_CAPACITY,
                        28_000, 52_000, 110_000, 190_000),
                flight("AI6310", "Delhi", "DEL", "India", "\uD83C\uDDEE\uD83C\uDDF3",
                        "New York", "JFK", "United States", "\uD83C\uDDFA\uD83C\uDDF8", 6, 10,
                        2, 0, 8, 0, 0, 15, 30, INTERNATIONAL_CAPACITY,
                        75_000, 135_000, 320_000, 550_000),
                flight("AI7421", "Chennai", "MAA", "India", "\uD83C\uDDEE\uD83C\uDDF3",
                        "Bangkok", "BKK", "Thailand", "\uD83C\uDDF9\uD83C\uDDED", 7, 10,
                        6, 15, 11, 10, 0, 3, 25, INTERNATIONAL_CAPACITY,
                        18_000, 34_000, 78_000, 130_000),
                flight("AI8532", "Hyderabad", "HYD", "India", "\uD83C\uDDEE\uD83C\uDDF3",
                        "Frankfurt", "FRA", "Germany", "\uD83C\uDDEC\uD83C\uDDE7", 8, 10,
                        3, 40, 9, 10, 0, 9, 0, INTERNATIONAL_CAPACITY,
                        52_000, 98_000, 235_000, 420_000),
                flight("AI9643", "Kolkata", "CCU", "India", "\uD83C\uDDEE\uD83C\uDDF3",
                        "Kuala Lumpur", "KUL", "Malaysia", "\uD83C\uDDF2\uD83C\uDDFE", 9, 10,
                        10, 0, 16, 30, 0, 4, 0, INTERNATIONAL_CAPACITY,
                        24_000, 45_000, 90_000, 155_000)
        );
    }

    private static ScheduledFlight flight(
            String flightNumber,
            String originCity,
            String originCode,
            String originCountry,
            String originFlag,
            String destinationCity,
            String destinationCode,
            String destinationCountry,
            String destinationFlag,
            int day,
            int month,
            int departureHour,
            int departureMinute,
            int arrivalHour,
            int arrivalMinute,
            int arrivalDayOffset,
            long durationHours,
            long durationMinutes,
            int capacity,
            int economy,
            int premiumEconomy,
            int business,
            int first) {
        Flight flight = new Flight(flightNumber, originCode, destinationCode, capacity, List.of());
        LocalDate departureDate = LocalDate.of(2026, month, day);
        return new ScheduledFlight(flight, originCity, originCountry, originFlag, destinationCity,
                destinationCountry, destinationFlag, departureDate,
                LocalTime.of(departureHour, departureMinute), departureDate.plusDays(arrivalDayOffset),
                LocalTime.of(arrivalHour, arrivalMinute), Duration.ofHours(durationHours).plusMinutes(durationMinutes),
                "INR", Map.of(
                FareClass.ECONOMY, economy,
                FareClass.PREMIUM_ECONOMY, premiumEconomy,
                FareClass.BUSINESS, business,
                FareClass.FIRST, first));
    }

    public record ScheduledFlight(
            Flight flight,
            String originCity,
            String originCountry,
            String originFlag,
            String destinationCity,
            String destinationCountry,
            String destinationFlag,
            LocalDate departureDate,
            LocalTime departureTime,
            LocalDate arrivalDate,
            LocalTime arrivalTime,
            Duration duration,
            String currency,
            Map<FareClass, Integer> fares) {
        public ScheduledFlight {
            fares = Map.copyOf(fares);
        }

        public ScheduledFlight withStoredFlight(Flight storedFlight) {
            return new ScheduledFlight(storedFlight, originCity, originCountry, originFlag,
                    destinationCity, destinationCountry, destinationFlag, departureDate,
                    departureTime, arrivalDate, arrivalTime, duration, currency, fares);
        }
    }
}
