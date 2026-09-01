from __future__ import annotations

from typing import Any


class CuratedMobileSession:
    """Runtime allowlist over the vendored mobile client.

    MCP and auth-broker code receive this facade instead of MobileSession, so
    payment, transfer, marketplace, messenger and credential operations are not
    reachable through the normal runtime object graph.
    """

    __slots__ = ("__session",)

    def __init__(self, session: Any):
        self.__session = session

    def list_accounts(self) -> list[dict]:
        return self.__session.list_accounts()

    def spending_categories(self, account_id: str | None, start_ms: int, end_ms: int) -> dict:
        return self.__session.spending_categories(account_id, start_ms, end_ms)

    def hotel_booking(self, booking_id: str) -> dict:
        return self.__session.hotel_booking(booking_id)

    def hotel_voucher(self, booking_id: str) -> tuple[bytes, str]:
        return self.__session.hotel_voucher(booking_id)

    def hotel_customer_data(self) -> dict:
        return self.__session.hotel_customer_data()

    def hotel_bookings_list(self, *, active: bool, cancelled: bool, completed: bool) -> dict:
        return self.__session.hotel_bookings_list(
            active=active,
            cancelled=cancelled,
            completed=completed,
        )
