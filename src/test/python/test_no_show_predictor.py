import subprocess
import sys
import unittest
from pathlib import Path


PYTHON_SOURCE = Path(__file__).resolve().parents[2] / "main" / "python"
sys.path.insert(0, str(PYTHON_SOURCE))

from no_show_predictor import estimate_no_show_probability


class NoShowPredictorTests(unittest.TestCase):
    def test_estimates_probability_for_valid_passenger_features(self):
        probability = estimate_no_show_probability(
            fare_class="BUSINESS",
            booking_status="CONFIRMED",
            previous_no_show_count=1,
            previous_flight_count=5,
            days_before_departure=10,
        )

        self.assertIsInstance(probability, float)
        self.assertGreaterEqual(probability, 0.0)
        self.assertLessEqual(probability, 1.0)

    def test_probability_stays_in_range_for_varied_inputs(self):
        examples = [
            {"fare_class": "ECONOMY", "previous_no_show_count": 10,
             "previous_flight_count": 10, "days_before_departure": 100},
            {"fare_class": "FIRST", "previous_no_show_count": 0,
             "previous_flight_count": 0, "days_before_departure": 0},
        ]

        for features in examples:
            with self.subTest(features=features):
                probability = estimate_no_show_probability(**features)
                self.assertGreaterEqual(probability, 0.0)
                self.assertLessEqual(probability, 1.0)

    def test_high_risk_history_produces_higher_estimate(self):
        high_risk = estimate_no_show_probability(
            fare_class="ECONOMY",
            previous_no_show_count=8,
            previous_flight_count=10,
            days_before_departure=30,
        )
        low_risk = estimate_no_show_probability(
            fare_class="FIRST",
            previous_no_show_count=0,
            previous_flight_count=10,
            days_before_departure=0,
        )

        self.assertGreater(high_risk, low_risk)

    def test_missing_features_use_safe_defaults(self):
        self.assertEqual(0.1975, estimate_no_show_probability())

    def test_rejects_invalid_features(self):
        invalid_inputs = [
            {"fare_class": "LUXURY"},
            {"booking_status": "IN_PROGRESS"},
            {"previous_no_show_count": -1},
            {"previous_flight_count": 1.5},
            {"days_before_departure": -1},
            {"previous_no_show_count": 2, "previous_flight_count": 1},
        ]

        for features in invalid_inputs:
            with self.subTest(features=features):
                with self.assertRaises(ValueError):
                    estimate_no_show_probability(**features)

    def test_command_line_prints_probability(self):
        script = PYTHON_SOURCE / "no_show_predictor.py"
        result = subprocess.run(
            [
                sys.executable,
                str(script),
                "--fare-class", "ECONOMY",
                "--booking-status", "CONFIRMED",
                "--previous-no-show-count", "2",
                "--previous-flight-count", "8",
                "--days-before-departure", "1",
            ],
            check=True,
            capture_output=True,
            text=True,
        )

        probability = float(result.stdout.strip())
        self.assertAlmostEqual(0.2825, probability)


if __name__ == "__main__":
    unittest.main()