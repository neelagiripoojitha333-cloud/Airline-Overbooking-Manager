"""Estimate a passenger's chance of missing a flight with a transparent score.

Example command:
    python src/main/python/no_show_predictor.py --fare-class ECONOMY \
        --booking-status CONFIRMED --previous-no-show-count 2 \
        --previous-flight-count 8 --days-before-departure 1

The example prints a probability from 0.0 to 1.0. Missing features use
documented neutral defaults; invalid values are rejected with a clear error.
"""

import argparse
from collections.abc import Sequence


FARE_CLASS_ADJUSTMENTS = {
    "ECONOMY": 0.08,
    "PREMIUM_ECONOMY": 0.04,
    "BUSINESS": -0.02,
    "FIRST": -0.05,
}

BOOKING_STATUS_ADJUSTMENTS = {
    "CONFIRMED": 0.0,
    "WAITLISTED": 0.10,
    "CANCELLED": -0.20,
    "BOARDED": -0.20,
    "BUMPED": 0.05,
}


def estimate_no_show_probability(
    fare_class: str | None = None,
    booking_status: str | None = None,
    previous_no_show_count: int | None = None,
    previous_flight_count: int | None = None,
    days_before_departure: int | None = None,
) -> float:
    """Return a rule-based no-show estimate, clamped to the inclusive 0-1 range.

    The score starts at 0.10, adds fare/status adjustments, adds up to 0.40
    based on the passenger's historical no-show rate, and adds up to 0.075
    when the booking was made far in advance (up to 30 days). Missing fare and
    status default to ECONOMY and CONFIRMED; missing history defaults to zero,
    and missing departure timing defaults to seven days.
    """
    normalized_fare_class = _normalize_category(
        fare_class, FARE_CLASS_ADJUSTMENTS, "ECONOMY", "fare_class"
    )
    normalized_booking_status = _normalize_category(
        booking_status, BOOKING_STATUS_ADJUSTMENTS, "CONFIRMED", "booking_status"
    )
    no_shows = _nonnegative_integer(
        previous_no_show_count, "previous_no_show_count", default=0
    )
    previous_flights = _nonnegative_integer(
        previous_flight_count, "previous_flight_count", default=0
    )
    days = _nonnegative_integer(days_before_departure, "days_before_departure", default=7)

    if no_shows > previous_flights:
        raise ValueError("previous_no_show_count cannot exceed previous_flight_count")

    historical_rate = no_shows / previous_flights if previous_flights else 0.0
    score = (
        0.10
        + FARE_CLASS_ADJUSTMENTS[normalized_fare_class]
        + BOOKING_STATUS_ADJUSTMENTS[normalized_booking_status]
        + 0.40 * historical_rate
        + 0.0025 * min(days, 30)
    )
    return min(1.0, max(0.0, score))


def _normalize_category(
    value: str | None,
    adjustments: dict[str, float],
    default: str,
    field_name: str,
) -> str:
    if value is None:
        return default
    if not isinstance(value, str):
        raise ValueError(f"{field_name} must be a string")

    normalized = value.strip().upper().replace(" ", "_")
    if normalized not in adjustments:
        allowed = ", ".join(adjustments)
        raise ValueError(f"{field_name} must be one of: {allowed}")
    return normalized


def _nonnegative_integer(value: int | None, field_name: str, default: int) -> int:
    if value is None:
        return default
    if type(value) is not int or value < 0:
        raise ValueError(f"{field_name} must be a non-negative integer")
    return value


def main(argv: Sequence[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--fare-class")
    parser.add_argument("--booking-status")
    parser.add_argument("--previous-no-show-count", type=int)
    parser.add_argument("--previous-flight-count", type=int)
    parser.add_argument("--days-before-departure", type=int)
    arguments = parser.parse_args(argv)

    try:
        probability = estimate_no_show_probability(
            fare_class=arguments.fare_class,
            booking_status=arguments.booking_status,
            previous_no_show_count=arguments.previous_no_show_count,
            previous_flight_count=arguments.previous_flight_count,
            days_before_departure=arguments.days_before_departure,
        )
    except ValueError as error:
        parser.error(str(error))

    print(f"{probability:.6f}")


if __name__ == "__main__":
    main()