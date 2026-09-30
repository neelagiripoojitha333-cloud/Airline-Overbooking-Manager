package com.airline.overbooking.service;

import com.airline.overbooking.model.Passenger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class NoShowPredictionService {
    private final String pythonExecutable;
    private final Path predictorScript;

    public NoShowPredictionService(String pythonExecutable, Path predictorScript) {
        if (pythonExecutable == null || pythonExecutable.isBlank()) {
            throw new IllegalArgumentException("pythonExecutable must not be blank");
        }
        this.pythonExecutable = pythonExecutable;
        this.predictorScript = Objects.requireNonNull(predictorScript, "predictorScript must not be null")
                .toAbsolutePath();
    }

    public double estimateProbability(Passenger passenger, int previousNoShowCount,
                                     int previousFlightCount, int daysBeforeDeparture)
            throws IOException, InterruptedException {
        Objects.requireNonNull(passenger, "passenger must not be null");
        validateHistory(previousNoShowCount, previousFlightCount, daysBeforeDeparture);

        List<String> command = List.of(
                pythonExecutable,
                predictorScript.toString(),
                "--fare-class", passenger.getFareClass().name(),
                "--booking-status", passenger.getBookingStatus().name(),
                "--previous-no-show-count", Integer.toString(previousNoShowCount),
                "--previous-flight-count", Integer.toString(previousFlightCount),
                "--days-before-departure", Integer.toString(daysBeforeDeparture)
        );

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("No-show predictor failed with exit code " + exitCode + ": " + output);
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw exception;
        }

        final double probability;
        try {
            probability = Double.parseDouble(output);
        } catch (NumberFormatException exception) {
            throw new IOException("No-show predictor returned an invalid probability: " + output, exception);
        }
        if (!Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
            throw new IOException("No-show predictor returned a probability outside 0.0-1.0: " + output);
        }
        return probability;
    }

    private static void validateHistory(int previousNoShowCount, int previousFlightCount,
                                        int daysBeforeDeparture) {
        if (previousNoShowCount < 0 || previousFlightCount < 0
                || previousNoShowCount > previousFlightCount) {
            throw new IllegalArgumentException("no-show history counts must be non-negative and consistent");
        }
        if (daysBeforeDeparture < 0) {
            throw new IllegalArgumentException("daysBeforeDeparture must not be negative");
        }
    }
}