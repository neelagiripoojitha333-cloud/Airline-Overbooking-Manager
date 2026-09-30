package com.airline.overbooking.service;

import com.airline.overbooking.model.BookingStatus;
import com.airline.overbooking.model.FareClass;
import com.airline.overbooking.model.Flight;
import com.airline.overbooking.model.Passenger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OverbookingRecommendationService {
    public static final double MAX_OVER_CAPACITY_PROBABILITY = 0.01;

    private static final Passenger CONSERVATIVE_CANDIDATE = new Passenger(
            "RECOMMENDATION-CANDIDATE", "Candidate", 0, FareClass.FIRST, BookingStatus.CONFIRMED);

    private final NoShowEstimator estimator;

    public OverbookingRecommendationService(NoShowEstimator estimator) {
        this.estimator = Objects.requireNonNull(estimator, "estimator must not be null");
    }

    public OverbookingRecommendationService(NoShowPredictionService predictionService) {
        this((passenger, days) -> Objects.requireNonNull(predictionService,
                "predictionService must not be null").estimateProbability(passenger, 0, 0, days));
    }

    public Recommendation recommend(Flight flight, int daysBeforeDeparture)
            throws IOException, InterruptedException {
        Objects.requireNonNull(flight, "flight must not be null");
        if (daysBeforeDeparture < 0) {
            throw new IllegalArgumentException("daysBeforeDeparture must not be negative");
        }

        List<Double> showProbabilities = new ArrayList<>();
        double predictedNoShows = 0.0;
        int confirmedBookings = 0;
        for (Passenger passenger : flight.getBookings()) {
            if (passenger.getBookingStatus() != BookingStatus.CONFIRMED) {
                continue;
            }
            double noShowProbability = estimateNoShowProbability(passenger, daysBeforeDeparture);
            predictedNoShows += noShowProbability;
            showProbabilities.add(1.0 - noShowProbability);
            confirmedBookings++;
        }

        double[] attendanceDistribution = distributionFor(showProbabilities);
        double risk = probabilityOfExceeding(attendanceDistribution, flight.getCapacity());
        int additionalBookings = 0;

        if (risk <= MAX_OVER_CAPACITY_PROBABILITY) {
            double candidateShowProbability = 1.0 - estimateNoShowProbability(
                    CONSERVATIVE_CANDIDATE, daysBeforeDeparture);
            while (candidateShowProbability > 0.0) {
                double[] candidateDistribution = addShowProbability(
                        attendanceDistribution, candidateShowProbability);
                double candidateRisk = probabilityOfExceeding(candidateDistribution, flight.getCapacity());
                if (candidateRisk > MAX_OVER_CAPACITY_PROBABILITY) {
                    break;
                }
                attendanceDistribution = candidateDistribution;
                risk = candidateRisk;
                additionalBookings++;
            }
        }

        return new Recommendation(flight.getCapacity(), confirmedBookings, predictedNoShows,
                additionalBookings, risk, MAX_OVER_CAPACITY_PROBABILITY, daysBeforeDeparture);
    }

    private double estimateNoShowProbability(Passenger passenger, int daysBeforeDeparture)
            throws IOException, InterruptedException {
        double probability = estimator.estimate(passenger, daysBeforeDeparture);
        if (!Double.isFinite(probability) || probability < 0.0 || probability > 1.0) {
            throw new IllegalStateException("No-show estimator must return a probability from 0.0 to 1.0");
        }
        return probability;
    }

    private static double[] distributionFor(List<Double> showProbabilities) {
        double[] distribution = {1.0};
        for (double showProbability : showProbabilities) {
            distribution = addShowProbability(distribution, showProbability);
        }
        return distribution;
    }

    private static double[] addShowProbability(double[] distribution, double showProbability) {
        double[] updatedDistribution = new double[distribution.length + 1];
        for (int attendanceCount = 0; attendanceCount < distribution.length; attendanceCount++) {
            updatedDistribution[attendanceCount] += distribution[attendanceCount] * (1.0 - showProbability);
            updatedDistribution[attendanceCount + 1] += distribution[attendanceCount] * showProbability;
        }
        return updatedDistribution;
    }

    private static double probabilityOfExceeding(double[] distribution, int capacity) {
        double probability = 0.0;
        for (int attendanceCount = capacity + 1; attendanceCount < distribution.length; attendanceCount++) {
            probability += distribution[attendanceCount];
        }
        return probability;
    }

    @FunctionalInterface
    public interface NoShowEstimator {
        double estimate(Passenger passenger, int daysBeforeDeparture)
                throws IOException, InterruptedException;
    }

    public record Recommendation(int capacity, int confirmedBookings, double predictedNoShows,
                                 int recommendedAdditionalBookings, double overCapacityProbability,
                                 double probabilityLimit, int daysBeforeDeparture) {
    }
}
