# Stage 13.0 — готовность деталей выбранного отеля и сверка открытых вопросов

## Цель

Выбрать следующий ограниченный hotel-only срез после завершения Stage 12 и
отделить вопросы, которые блокируют его реализацию, от внешних ограничений
будущего публичного rollout.

Stage 13.0 является review/design-only этапом. Он не меняет runtime, публичный
API, OpenAPI или demo shell.

## Текущая точка

Stage 12 подтвердил первичный поиск, четыре provider filters, повторное
confirmation и новый поиск после refinement. Backend сохраняет до 20
предложений, а demo shell показывает первые 5. Пользователь уже может сузить
выдачу, но не может запросить структурированные дополнительные сведения об
одном выбранном варианте.

## Сверка доступных Hotels API capabilities

| Capability | Контракт | Решение |
|---|---|---|
| Autocomplete | `POST /search-api/search/autocomplete` | Уже используется для location resolution |
| Search и filters | `POST /api/v1/hotels/search` | Уже используются в Stage 9–12 |
| Static hotel details | `GET /api/v1/hotels/{hotelId}` | Выбран как следующий on-demand capability после contract verification |
| Rates | `POST /api/v3/hotels/{hotelId}/rates` | Later: time-sensitive rate model, auth и `bookHash` требуют отдельной сверки |
| Deeplink | `POST /api/v3/hotels/urls/search` | Later: host/auth/headers и продуктовый handoff не подтверждены |
| Booking, payment, cancellation execution | Несколько booking-oriented endpoint | Future и вне hotel-only демонстрационного scope |

Static details потенциально дают location, images, facilities, rules и payment
methods. Они полезны для ответа на явный вопрос пользователя об одном отеле и
не требуют преждевременно моделировать room/rate или booking lifecycle.

## Выбранная продуктовая граница

Stage 13 развивает сценарий «расскажи подробнее о выбранном варианте»:

1. Пользователь сначала получает сохранённую выдачу.
2. Пользователь явно выбирает предложение в текущем контексте.
3. Backend разрешает выбор через собственные `hotelSearchId` и `offerId`.
4. Только после явного выбора выполняется один on-demand details request.
5. Assistant возвращает только подтверждённые provider facts и сохраняет
   неизвестные поля как unknown.

Подтверждение, необходимое перед новым hotel search, не требуется для
read-only details request: он не меняет критерии, не создаёт новый поиск и не
начинает booking. Запрос всё равно запрещено выполнять автоматически для всех
карточек или без явного выбора пользователя.

## Идентификаторы и граница API

- `hotelId` остаётся opaque provider reference внутри backend.
- Клиент и LLM не конструируют и не получают provider `hotelId`.
- Выбор пользователя должен разрешаться через сохранённый `offerId` в пределах
  указанного или текущего `hotelSearchId`.
- Provider DTO не становятся domain или public API DTO.
- Новый публичный endpoint или `nextAction` не добавляется до отдельного этапа
  platform-neutral contract alignment.

Это сохраняет ADR-0001: продуктовые клиенты интегрируются только с Travel
Assistant API и не обращаются к Hotels API напрямую.

## Сверка открытых вопросов Stage 12

| Вопрос | Решение |
|---|---|
| Пользовательская сортировка | Закрыта как deferred: provider отклоняет `sort`, локальная имитация не добавляется |
| `search-filters-availability` | Не является blocker: проверенный пустой payload не используется, действует deterministic no-results advice |
| Taxes/fees в `shownPrice` | Сохраняется как unknown; текущая цена передаётся без перерасчёта как provider total |
| Официальный S2S-статус, SLA и rate limits | Не блокируют локальный opt-in demo, но остаются обязательным внешним gate до rollout |
| Process-local stores | Приняты для demo; durable resume, auth и cross-device sync остаются отдельным scope |

Эти пункты больше не считаются блокерами следующего contract-only этапа. Они
не считаются доказанными provider guarantees и не создают production-readiness
claim.

## Что нужно проверить перед реализацией

Stage 13.1 должен подтвердить на одном контролируемом примере:

- фактические host, path и HTTP method для details;
- анонимный доступ либо точную auth-ошибку;
- совместимость `hotelId` из search response с details path;
- envelope и nullability location, images, facilities, rules и payment methods;
- безопасные `404` и provider error semantics;
- отсутствие session/device/tracing данных в обезличенном fixture.

При contract drift production DTO не меняются в том же этапе. Автоматические
retry и альтернативные endpoint probes запрещены.

## План Stage 13

| Этап | Scope |
|---|---|
| Stage 13.0 | Выбор on-demand details capability и сверка открытых вопросов |
| Stage 13.1 | Один контролируемый details contract probe и обезличенный fixture |
| Stage 13.2 | Provider-neutral details model/boundary и fixture-driven mapping без runtime |
| Stage 13.3 | Selected-offer resolution policy внутри сохранённого search context |
| Stage 13.4 | Details transport/orchestration через `MockEngine`, без public wiring |
| Stage 13.5 | Platform-neutral API/assistant response alignment отдельным контрактным этапом |
| Stage 13.6 | Opt-in runtime, demo shell и bounded verification |

Каждый этап активируется отдельно. Неуспешный Stage 13.1 блокирует последующую
реализацию и приводит только к отдельной contract reconciliation.

## Вне Stage 13.0

Не выполняются live calls, не создаются DTO, mapper, transport, endpoint,
runtime wiring, UI, auth, storage, rates, deeplink, shortlist, comparison,
booking или payment. Исторические review-артефакты не переписываются.

## Verdict

`READY_FOR_STAGE_13_1_CONTROLLED_HOTEL_DETAILS_CONTRACT_VERIFICATION`.

Следующий безопасный этап — Stage 13.1 после отдельного разрешения на один
контролируемый details probe. Дополнительные owner inputs для Stage 13.0 не
требуются.
