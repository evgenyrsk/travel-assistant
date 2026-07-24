# Stage 14.0 — закрытие рабочего hotel-only MVP

## Цель

Подтвердить единый рабочий срез от естественного hotel request до предложений
и on-demand деталей выбранного offer, синхронизировать активную документацию и
остановить функциональное расширение MVP.

Итоговый статус — `demo-ready MVP`, но не production readiness.

## Acceptance matrix

| Сценарий | Доказательство | Результат |
|---|---|---|
| Полный запрос → confirmation → offers | Backend integration tests и REAL browser smoke | Пройдено |
| Уточнение обязательных полей и возраста ребёнка | Assistant/constraints regression tests | Пройдено |
| Изменение фильтров → новое confirmation → новый search | `AssistantHotelRefinementIntegrationTest` | Пройдено |
| Предыдущий `hotelSearchId` остаётся доступен | Refinement integration tests | Пройдено |
| Пустая выдача отличается от provider failure | No-results и failure integration tests | Пройдено |
| Явный выбор offer → один details request | Runtime/frontend tests и REAL browser smoke | Пройдено |
| Остальные карточки не загружают details | Frontend flow test и browser network inspection | Пройдено |
| Provider `hotelId` отсутствует в public boundary | API/runtime tests и scope scan | Пройдено |
| Ошибки OpenRouter/Hotels API безопасны | Adapter, route и integration tests | Пройдено |
| Один REAL flow до details без retry | Browser smoke 23 июля 2026 года | Пройдено |

## REAL smoke

- Профиль: локальный `--real`; `.env` игнорируется Git, preflight выполнен без
  вывода значений credentials.
- Запрос: Казань, 10–14 августа 2026 года, два взрослых, без детей, одна
  комната.
- До подтверждения показан полный confirmation prompt; карточки отсутствовали.
- После отдельного «Да» backend получил 20 offers, demo shell показала 5.
- Нажатие «Подробнее» у одной карточки выполнило один локальный details GET и
  показало provider-backed описание, адрес, удобства и время заезда/выезда.
- Автоматических retry, запросов details других карточек и прямых browser calls
  к OpenRouter/Hotels API не было.
- В публичном URL использованы только opaque process-local search/offer IDs.

Полные provider/LLM bodies, headers, credentials и внутренние identifiers не
сохранялись в документации или Git.

## Проверки

- полный backend Gradle suite;
- frontend tests, lint и build;
- OpenAPI conformance tests и `npm run check`;
- launcher tests и `FAKE`/`REAL` preflight;
- локальный browser QA на 320×568 и 390×844;
- один REAL browser smoke без retry;
- `git diff --check`, link, secret и provider-ID scans.

OpenAPI conformance намеренно остаётся `not_ready`, `readinessClaim=false`;
четыре endpoint классифицированы как `platform_client_candidate`.

## Обновлённая активная документация

- product и architecture baselines;
- primary и navigation roadmap;
- backend README;
- local demo runbook;
- reviews index.

Исторические review-артефакты не переписывались.

## Остаточные ограничения

- stores process-local; auth, durable storage и multi-instance coordination
  отсутствуют;
- `FAKE` остаётся runtime default;
- официальный S2S status, SLA и rate limits публичного Hotels API не
  подтверждены;
- OpenAPI/generated clients не готовы к внешней интеграции;
- rates, deeplink, shortlist, comparison, booking и payment не реализованы;
- product web/mobile clients, CORS и deployment остаются отдельными задачами.

## Verdict

`PASS_STAGE_14_0_WORKING_HOTEL_MVP_CLOSURE`.

Рабочий hotel-only MVP завершён в локальных демонстрационных границах.
Функциональное расширение остановлено до отдельного product/roadmap decision.
