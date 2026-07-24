# Stage 10.4 — граница интеграции сервиса и ответственность клиентов

## Роль и цель

Это review/design artifact. Этап определяет, что Travel Assistant предоставляет
будущим платформенным командам, и исключает преждевременную реализацию Android,
iOS или другого клиента в текущем репозитории.

Текущий статус этапов задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md), а принятое архитектурное
решение —
[`ADR-0001`](../decisions/adr-0001-service-core-and-client-integration-boundary.md).

## Исходная готовность

- Локальная web/PWA demo shell уже реализует chat-first hotel flow.
- Backend владеет business logic, validation, ranking, LLM/provider
  orchestration и secrets.
- Stage 10.3 согласовал с runtime три platform-neutral endpoint.
- Весь OpenAPI остается `not_ready`; generated clients не создавались.
- Backend state остается process-local, поэтому resume и cross-device sync не
  обещаются.

## Принятое разделение ответственности

| Область | Travel Assistant service | Будущая клиентская команда |
|---|---|---|
| Business rules и orchestration | Реализует и сохраняет едиными | Не дублирует |
| Public HTTP contract | Версионирует, документирует и проверяет | Потребляет |
| Provider/LLM integrations и secrets | Полностью изолирует на backend | Не получает |
| UI/UX и navigation | Поддерживает только локальную demo shell | Определяет для своей платформы |
| API client/SDK и toolchain | Не выбирает заранее | Выбирает в отдельной задаче |
| Platform release lifecycle | Вне scope сервиса | Владеет |

Текущий web/PWA остается локальной demo shell, а не будущим продуктовым
клиентом. Product web, Android, iOS и desktop не включаются в ближайшие этапы и
не получают особого backend behavior.

## Интеграционная поверхность

Будущая команда интегрируется только через:

- `POST /api/v1/assistant/sessions`;
- `POST /api/v1/assistant/sessions/{sessionId}/messages`;
- `GET /api/v1/hotel-searches/{searchId}/offers`.

`GET /health` остается operational, прямой `POST /hotel-searches` —
diagnostic-only, shortlist/explanation placeholders не входят в клиентский
контракт.

Backend/domain modules не публикуются как встраиваемое ядро. Provider DTO,
OpenRouter/Hotels API hosts, credentials и ranking rules не должны переходить в
клиент. `sessionId`, `hotelSearchId` и `offerId` остаются opaque identifiers.

## Закрытие открытых вопросов

| Вопрос | Решение |
|---|---|
| Нужно ли сейчас выбирать Android или iOS | Нет |
| Где находится ядро | В самостоятельном backend-сервисе |
| Кто реализует product web/mobile UI | Будущая платформенная команда |
| Нужен ли общий UI/KMP-модуль | Нет, пока нет отдельной задачи |
| Кто выбирает SDK/toolchain | Команда конкретной платформы совместно с владельцем сервиса |
| Что предоставляет сервис | HTTP API, OpenAPI, lifecycle/error semantics и проверенные примеры |
| Требуется ли CORS | Только для будущего cross-origin web; сейчас default-deny |
| Поддерживаются ли resume/cross-device | Нет |

Дополнительные решения владельца для следующего service-side этапа не
требуются. Конкретная платформа, SDK и UI должны определяться отдельной задачей
будущей интеграционной команды.

## Manifest и readiness

Manifest сохраняет пустой `generatedClientTargets` и явно фиксирует отсутствие
выбранного platform SDK. Это осознанная граница, а не разрешение считать SDK
готовым:

- `status=not_ready`;
- `readinessClaim=false`;
- endpoint-level `readiness=not_ready`;
- generation/compile checks не выполнялись.

## Что не входило в этап

- Android/iOS/desktop projects и native UI;
- generated SDK или выбор generator;
- backend/frontend production code и tests;
- изменение runtime, routes или OpenAPI schemas;
- CORS, auth, durable storage, offline mode и cross-device sync;
- live provider/LLM calls.

## Следующий безопасный этап

Stage 11.0 — локальная готовность демонстрационного MVP:

- сохранить текущую web/PWA только как demo shell;
- добавить воспроизводимый локальный запуск в явных `REAL` и `FAKE` профилях;
- выполнить один контролируемый REAL browser smoke;
- не создавать platform SDK, product UI или deployment infrastructure.

## Проверки и verdict

- Проверены roadmap, product/architecture baselines и Stage 10.0–10.3 artifacts.
- Tool-local conformance summary не предполагает наличие target и закреплен
  точечной проверкой.
- `tools/openapi-conformance`: `npm test` — 10/10 проверок пройдены.
- `tools/openapi-conformance`: `npm run check` — пройден, blocking findings
  отсутствуют, `status=not_ready`, `readinessClaim=false`.
- `git diff --check`, scope/secret scan и ручная проверка локальных ссылок —
  пройдены.
- Backend/frontend suites не запускались: их code, tests и contracts не
  изменялись.

**Verdict:** `PASS_STAGE_10_4_SERVICE_CLIENT_OWNERSHIP_DEFINED`.

Stage 10.4 завершает Stage 10. Stage 11.0 разрешен только как локальная
демонстрационная готовность без реализации product clients и без заявления
production readiness.
