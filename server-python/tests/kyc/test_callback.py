"""Gọi lại backend có thử lại (plan AI Worker Y02.05)."""

import httpx
import respx

from app.core.callback import send_result
from app.core.config import Settings

S = Settings(backend_url="http://backend:8080", internal_token="t0k", callback_retries=3)
URL = "http://backend:8080/api/v1/internal/kyc/7/result"


async def _no_sleep(_: float) -> None:
    return None


@respx.mock
async def test_first_try_ok_with_token():
    route = respx.patch(URL).respond(200, json={"data": {"kyc_status": "VERIFIED"}})
    assert await send_result(7, {"a": 1}, S, sleep=_no_sleep)
    assert route.call_count == 1
    assert route.calls[0].request.headers["X-Internal-Token"] == "t0k"


@respx.mock
async def test_retries_on_5xx_then_succeeds():
    route = respx.patch(URL).mock(side_effect=[httpx.Response(503), httpx.Response(503), httpx.Response(200)])
    waits: list[float] = []

    async def sleep(s: float) -> None:
        waits.append(s)

    assert await send_result(7, {}, S, sleep=sleep)
    assert route.call_count == 3
    assert waits == [1, 2]


@respx.mock
async def test_does_not_retry_4xx():
    route = respx.patch(URL).respond(401)
    assert not await send_result(7, {}, S, sleep=_no_sleep)
    assert route.call_count == 1


@respx.mock
async def test_gives_up_after_retries_without_raising():
    route = respx.patch(URL).mock(side_effect=httpx.ConnectError("down"))
    assert not await send_result(7, {}, S, sleep=_no_sleep)
    assert route.call_count == 4
