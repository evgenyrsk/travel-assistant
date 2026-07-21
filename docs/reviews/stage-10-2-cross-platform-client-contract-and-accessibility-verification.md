# Stage 10.2 — проверка кроссплатформенного клиентского контракта и доступности

## Роль документа

Это отчет о verification-этапе. Он фиксирует проверенные свойства текущего
клиентского контракта и интерфейса, но не заменяет OpenAPI и не объявляет API
готовым для генерации SDK. Текущий статус проекта задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель и границы

Проверить, что существующий chat-first frontend использует независимый от
платформы JSON/HTTP boundary, а responsive web/PWA удовлетворяет ограниченному
mobile/accessibility gate. Production frontend и backend behavior, OpenAPI,
CORS и public API в рамках этапа не менялись.

## Проверка runtime и OpenAPI

| Возможность | Runtime | OpenAPI draft | Итог |
|---|---|---|---|
| Создание Assistant session | `POST /api/v1/assistant/sessions` | Описано | Формы запроса и ответа согласованы в проверяемом срезе |
| Следующее сообщение | `POST /api/v1/assistant/sessions/{sessionId}/messages` | Описано | Один и тот же контракт подходит любому HTTP-клиенту |
| Решение Assistant | `nextAction` и необязательный `hotelSearchId` | Описано | `hotelSearchId` отсутствует в ветках без созданного поиска |
| Получение предложений | `GET /api/v1/hotel-searches/{searchId}/offers` | Описано | Provider DTO не выходят в public response |
| Ошибки | безопасные validation, not-found и internal responses | Описано | Внутренние provider/LLM данные не раскрываются |

Статическая conformance-проверка не нашла расхождений между девятью
инвентаризированными runtime routes и OpenAPI paths. При этом весь OpenAPI
contract по-прежнему имеет статус `not_ready`, а `readinessClaim=false`.
Generated-client-ready subset и подтвержденный SDK-контракт не создавались.

Историческая классификация hotel-search/offers как `placeholder_excluded` в
Stage 7 manifest уже не отражает полностью реализованный runtime. Это не дефект
текущего API behavior, но явный вход для Stage 10.3 contract hardening.

## Test-only доказательства независимости клиента

Добавленные frontend tests подтверждают:

- `createApiClient` принимает абсолютный настраиваемый `baseUrl` и корректно
  кодирует opaque session/search identifiers;
- API client не зависит от DOM, cookies, `localStorage`, `sessionStorage`,
  `IndexedDB` или browser session API;
- запросы не добавляют `credentials`, cookie или `Authorization`;
- frontend sources не содержат OpenRouter/Hotels API hosts, credentials,
  provider DTO и `bookHash`;
- ограничение до пяти предложений не входит в API client или backend contract:
  оно остается presentation policy в chat flow.

## Матрица кроссплатформенной готовности

| Клиент | Статус | Ограничение |
|---|---|---|
| Same-origin web/PWA | Поддерживается | Frontend server проксирует `/api/v1/**`; CORS не требуется |
| Cross-origin web | Не включен | Нужна отдельная configurable CORS allowlist policy |
| iOS/Android | Архитектурно совместимы | Native SDK и contract-readiness не подтверждены |
| Desktop | Архитектурно совместим | Отдельный client/SDK не создан |
| Resume/cross-device | Не поддерживается | Session и search stores process-local; auth и durable storage отсутствуют |

Архитектурный инвариант: любой клиент обращается только к Travel Assistant
`/api/v1/**`. Provider/LLM orchestration, секреты, business validation и hotel
ranking остаются в backend и не дублируются на платформах.

## Mobile и accessibility QA

Локальный QA выполнялся в `FAKE`-режиме без внешних provider-вызовов.

| Viewport | Горизонтальный overflow | Основная кнопка | Итог |
|---|---:|---:|---|
| 320x568 | отсутствует | 242x48 CSS px | пройдено |
| 390x844 | отсутствует | 312x48 CSS px | пройдено |
| 768x1024 | отсутствует | 146x48 CSS px | пройдено |
| 1440x900 | отсутствует | 146x48 CSS px | пройдено |

Дополнительно подтверждены:

- последовательность keyboard focus: diagnostic link, поле сообщения, кнопка;
- обратный переход через `Shift+Tab` без focus trap;
- видимый focus outline и связанный с полем label;
- `role=log`, `role=alert` и polite live regions;
- primary touch target 48 CSS px; остальные интерактивные элементы превышают
  минимальный WCAG 2.2 AA target 24x24 CSS px;
- контрольные контрастные пары: основной button 7.26:1, focus 5.18:1,
  вспомогательный текст 4.96:1;
- `prefers-reduced-motion` отключает заметные transitions;
- `viewport-fit=cover`, standalone manifest и safe-area CSS boundary;
- успешный локальный FAKE chat turn, отсутствие console/page errors и возврат
  focus в поле сообщения.

Фактический installed standalone mode и hardware safe-area на iOS-устройстве
не проверялись: browser emulation подтверждает metadata/layout boundary, но не
заменяет device QA.

## Findings и ограничения

Critical/Major defects не обнаружены. Этап не устраняет следующие ограничения:

- OpenAPI остается draft с verdict `not_ready`;
- cross-origin browser access не поддерживается без CORS allowlist;
- runtime HTTP contract tests и generated clients отсутствуют;
- process-local state не поддерживает resume и cross-device sync;
- device-level installed PWA QA отложен;
- hotel details, shortlist и отдельный comparison/explanation flow не
  реализованы и не маскируются кроссплатформенной совместимостью.

## Проверки

- `app`: `npm test` — 20 tests passed;
- `app`: `npm run lint` — пройдено;
- `app`: `npm run build` — пройдено;
- `services/backend`: полный `./gradlew test` в `FAKE`-режиме — пройдено;
- `tools/openapi-conformance`: `npm test` — 16 tests passed;
- `tools/openapi-conformance`: `npm run check` — `not_ready`, без blocking
  findings и без readiness claim;
- локальный browser QA — пройден в заявленной матрице;
- `git diff --check` — пройдено.

## Следующий этап

Рекомендуется Stage 10.3 — platform-neutral API contract hardening:

- согласовать runtime и OpenAPI как активный контракт;
- добавить runtime contract tests;
- актуализировать manifest classification;
- отдельно принять configurable CORS allowlist policy.

Native clients, SDK generation, auth и durable storage не входят в этот
следующий ограниченный этап.

## Verdict

`PASS_BOUNDED_CROSS_PLATFORM_VERIFICATION`.

Same-origin web/PWA поддерживается, а native/desktop clients архитектурно могут
использовать тот же API. Это еще не SDK/contract readiness, поддержка
cross-origin web, resume/cross-device или production readiness.
