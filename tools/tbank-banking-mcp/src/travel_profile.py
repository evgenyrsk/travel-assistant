from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class SpendingBand:
    name: str
    minimum_monthly_spend: float
    hotel_min_per_night: int
    hotel_max_per_night: int


DEFAULT_BANDS = (
    SpendingBand("economy", 0, 3_000, 7_000),
    SpendingBand("balanced", 50_000, 6_000, 13_000),
    SpendingBand("comfort", 120_000, 10_000, 25_000),
    SpendingBand("premium", 250_000, 20_000, 60_000),
)

TRAVEL_MARKERS = (
    "travel",
    "hotel",
    "авиа",
    "отел",
    "путеше",
    "железнодорож",
    "такси",
)


def _number(value: Any) -> float:
    try:
        return max(0.0, float(value))
    except (TypeError, ValueError):
        return 0.0


def build_travel_profile(summary: dict[str, Any], days: int = 90) -> dict[str, Any]:
    if days < 30 or days > 366:
        raise ValueError("days must be between 30 and 366")
    total_spent = _number(summary.get("total_spent"))
    total_earned = _number(summary.get("total_earned"))
    monthly_spent = total_spent * 30.4375 / days
    monthly_earned = total_earned * 30.4375 / days
    categories = summary.get("categories") if isinstance(summary.get("categories"), list) else []
    travel_spend = 0.0
    for category in categories:
        if not isinstance(category, dict):
            continue
        name = str(category.get("category") or "").casefold()
        if any(marker in name for marker in TRAVEL_MARKERS):
            travel_spend += _number(category.get("amount"))
    travel_share = travel_spend / total_spent if total_spent else 0.0

    band = DEFAULT_BANDS[0]
    for candidate in DEFAULT_BANDS:
        if monthly_spent >= candidate.minimum_monthly_spend:
            band = candidate

    confidence = 0.45
    if days >= 84:
        confidence += 0.2
    if len(categories) >= 3:
        confidence += 0.15
    if total_earned > 0:
        confidence += 0.1
    confidence = min(confidence, 0.9)

    return {
        "profileType": "spending_based_travel_preference",
        "tier": band.name,
        "confidence": round(confidence, 2),
        "periodDays": days,
        "currency": summary.get("currency") or "RUB",
        "aggregates": {
            "estimatedMonthlySpend": round(monthly_spent, 2),
            "estimatedMonthlyInflow": round(monthly_earned, 2),
            "travelSpendShare": round(travel_share, 4),
        },
        "hotelDefaults": {
            "pricePerNight": {
                "min": band.hotel_min_per_night,
                "max": band.hotel_max_per_night,
                "currency": summary.get("currency") or "RUB",
            },
            "ranking": "best_value",
            "showAlternativesOutsideBand": True,
        },
        "privacy": {
            "rawTransactionsIncluded": False,
            "incomeTierClaimed": False,
            "userOverrideRecommended": True,
        },
        "explanation": (
            "Профиль основан на агрегированных расходах, а не на оценке дохода или "
            "кредитоспособности. Он задаёт только рекомендуемый диапазон и не скрывает альтернативы."
        ),
    }
