# Stage 13.2 — provider-neutral модель и mapping деталей отеля

## Цель

Добавить внутреннюю модель статических деталей выбранного отеля и проверить
provider mapping по обезличенному fixture Stage 13.1 без HTTP, routes и runtime
wiring.

## Реализация

Добавлены:

- `HotelDetails` в domain layer;
- tolerant `HotelsApiHotelDetailsResponseDto`;
- `HotelsApiHotelDetailsResponseMapper` и typed mapping errors;
- targeted fixture-driven tests.

Модель содержит только отображаемые сведения: название, optional hotel chain,
звёзды, адрес и coordinates, description sections, до десяти HTTPS images,
amenity groups, check-in/check-out, нормализованные `cash`/`card`, а также
provider-neutral source/freshness.

## Границы данных

- Provider `hotelId` возвращается mapper только как internal reference рядом с
  результатом и не входит в `HotelDetails`.
- Contacts, certification, owner/register data, provider codes и
  `structuredRules` не моделируются.
- Отсутствующие optional fields остаются `null`; значения не подставляются.
- Небезопасные image URLs отбрасываются, дубликаты удаляются, список ограничен
  десятью элементами.
- Неизвестные payment methods игнорируются, известные card schemes
  нормализуются в `card`.
- Невалидные identity, star rating, coordinates и time fields возвращают typed
  mapping error.

## Проверки

- Обезличенный Stage 13.1 fixture десериализуется при наличии неизвестных полей.
- Provider-specific certification/contact fields не переходят в domain.
- Minimal payload сохраняет optional facts как unknown.
- HTTPS image policy и лимит проверены отдельно.
- Identity/location/time errors типизированы.
- Targeted и полный backend test suite пройдены.
- `git diff --check` пройден.

## Вне Stage 13.2

Не добавлены HTTP transport, provider boundary, selected-offer resolution,
routes, OpenAPI, runtime wiring, demo UI или live calls.

## Verdict

`PASS_STAGE_13_2_PROVIDER_NEUTRAL_DETAILS_MAPPING`.

Следующий этап — Stage 13.3: opaque offer identity и разрешение выбранного
предложения без раскрытия provider reference.
