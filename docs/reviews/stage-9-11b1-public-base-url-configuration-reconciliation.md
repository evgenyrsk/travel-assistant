# Stage 9.11b1 — сверка конфигурации public base URL Hotels API

## 1. Цель

Stage 9.11b1 приводит default public target в соответствие с результатами
Stage 9.11b. Внутренние `/api/v1|v2|v3` endpoint-ы были подтверждены на
`https://hotels.tbank.ru/`, тогда как прежний `hotels.tcsbank.ru` не разрешился
через доступный DNS.

Этап меняет только configuration default и его targeted test. HTTP transport,
provider adapter и runtime composition не подключаются.

## 2. Изменение

`HotelsApiTargetConfig.DEFAULT_PUBLIC_BASE_URL` изменен:

```text
https://hotels.tcsbank.ru/ -> https://hotels.tbank.ru/
```

Ключ environment override остается прежним:
`HOTELS_API_PUBLIC_BASE_URL`. Явно заданное значение продолжает иметь приоритет
над default.

Private target, JWT configuration, timeout и optional headers не изменены.

## 3. Подтверждающие данные

Stage 9.11b зафиксировал ограниченную проверку без credentials, cookies и
session/device headers:

- валидный `POST /api/v1/hotels/search` вернул `200`;
- `GET /api/v2/hotels/search-filters` вернул `200`;
- v2/v3 URL routes вернули ожидаемую validation error на пустой body;
- search response совместим с текущим provider DTO.

Новые live API calls в Stage 9.11b1 не выполнялись.

## 4. Тесты

`HotelProviderConfigTest` теперь явно проверяет, что environment для режима
`REAL` без public override получает `https://hotels.tbank.ru/`.

Сохраняются существующие проверки:

- `FAKE` работает без Hotels API configuration;
- `REAL` требует private key;
- public base URL можно переопределить через environment;
- невалидные URL и timeout отклоняются;
- private key не попадает в `toString` и exception text.

## 5. Границы

Не изменены:

- `Application.kt` и `HotelOfferProviderFactory`;
- `PublicHotelsApiHttpTransport` и его request behavior;
- `RealHotelOfferProviderAdapter`;
- routes, public API и OpenAPI Travel Assistant;
- frontend и generated clients;
- private target, JWT signing и secrets;
- `FAKE` как provider mode по умолчанию.

Изменение default URL не означает runtime activation, постоянное разрешение на
server-to-server использование или готовность к промышленной эксплуатации.

## 6. Следующий этап

Stage 9.11c остается заблокирован до продуктовых решений по:

- распределению guests по rooms;
- child ages;
- timezone и wire date format;
- nullable review/rating;
- составу `shownPrice` и taxes/fees.

После этих решений Stage 9.11c может добавить provider mapping без transport
call и runtime wiring.

## 7. Итог

Public Hotels API configuration использует подтвержденный
`https://hotels.tbank.ru/` как default. Изменение ограничено configuration
boundary и не активирует real provider.
