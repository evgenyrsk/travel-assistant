# Stage 13.4 — transport и provider adapter деталей отеля

## Цель

Добавить изолированный backend transport path для загрузки деталей выбранного
отеля и provider adapter с typed outcomes без routes и runtime wiring.

## Реализация

- `PublicHotelsApiHttpTransport` поддерживает JSON GET с прежней защитой
  настроенного public host, timeout и safe status mapping.
- `404` выделен в безопасную transport-категорию `NOT_FOUND`.
- `HotelDetailsProviderBoundary` и `HotelDetailsProviderResult` принадлежат
  application layer.
- `HotelsApiHotelDetailsProviderAdapter` вызывает
  `GET /api/v1/hotels/{hotelId}`, декодирует Stage 13.2 DTO и применяет mapper.
- Opaque provider reference кодируется как один URL path segment; blank input
  отклоняется без HTTP-вызова.
- Ответ с другим `hotelId` отклоняется как identity mismatch.

Typed outcomes: `Loaded`, `NotFound`, `ResponseRejected` и
`ProviderUnavailable`. Provider body, URL, reference и exception text в них не
переносятся.

## Transport policy

- Разрешён только относительный path без query/fragment и смены host.
- Отправляются `Accept: application/json` и optional `X-User-Language`.
- `Authorization`, cookies, arbitrary headers и GET body не добавляются.
- Используется timeout public target.
- Redirect/retry policy существующего production client не меняется.
- Coroutine cancellation пробрасывается.

## Проверки

- Success fixture загружается через GET и маппится в `HotelDetails`.
- Opaque reference с `/` и пробелом остаётся одним encoded path segment.
- Host/path escape блокируется до request.
- `404`, `408`, `401`, `429`, `5xx` и прочие ошибки получают typed outcomes.
- Client-side GET timeout и общая transport policy сетевых ошибок не раскрывают
  детали.
- Malformed JSON, identity mismatch и mapping errors безопасно отклоняются.
- Все transport/adapter tests используют `MockEngine`.
- Targeted и полный backend test suite пройдены.
- `git diff --check` пройден.

## Границы этапа

`Application.kt`, routes, OpenAPI, public response, frontend и runtime provider
composition не менялись. Live call не выполнялся. Details не загружаются
автоматически; N+1 отсутствует.

## Verdict

`PASS_STAGE_13_4_HOTEL_DETAILS_TRANSPORT_ADAPTER`.

Следующий этап — Stage 13.5: platform-neutral details API с fake details
provider, без REAL runtime wiring.
