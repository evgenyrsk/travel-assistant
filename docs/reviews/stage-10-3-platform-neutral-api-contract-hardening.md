# Stage 10.3 — укрепление платформонезависимого API-контракта

## Цель и границы

Этап согласовал фактический runtime и OpenAPI для минимального chat-first
контракта, одинакового для web, iOS, Android и desktop. В границы вошли API
validation, provider-neutral response metadata, contract tests и статическая
conformance-проверка. Live provider/LLM calls, CORS activation, SDK generation,
auth, durable storage и UI не входили в этап.

## Платформенный subset

| Endpoint | Классификация | Назначение |
|---|---|---|
| `POST /api/v1/assistant/sessions` | `platform_client_candidate` | Создание Assistant session; body необязателен |
| `POST /api/v1/assistant/sessions/{sessionId}/messages` | `platform_client_candidate` | Следующий пользовательский turn |
| `GET /api/v1/hotel-searches/{searchId}/offers` | `platform_client_candidate` | Чтение offers по opaque search ID |
| `GET /api/v1/health` | `operational` | Служебная проверка |
| `POST /api/v1/hotel-searches` | `diagnostic_excluded` | Прямой диагностический search |
| Shortlist и explanation routes | `placeholder_excluded` | Не реализованы |

Весь OpenAPI остается `not_ready`, `readinessClaim=false`, список targets для
generated clients пуст.

## Runtime и contract alignment

- `nextAction` ограничен значениями `ask_clarification`,
  `show_boundary_message`, `show_hotel_results`.
- `hotelSearchId` обязателен только для `show_hotel_results` и отсутствует в
  clarification/failure responses.
- Assistant session/message и terminal search/offers envelopes описывают только
  фактически возвращаемые поля.
- Search statuses ограничены `completed_with_offers` и
  `completed_no_offers`.
- `rating` и `amenities` остаются необязательными; неизвестные facts не
  подменяются значениями по умолчанию.
- Search/offers metadata больше не содержит утверждений о fake provider;
  `warnings` пуст, пока реального предупреждения нет.

## Message validation

- Допустимо от 1 до 4000 Unicode code points.
- Body можно не передавать только при создании session.
- Blank/missing message возвращает `400 VALIDATION_ERROR` для `message`.
- Malformed JSON, неизвестные поля, неподдерживаемая JSON-форма или media type
  возвращают `400 VALIDATION_ERROR` для `body`.
- После успешной проверки исходный текст передается application-слою без
  нормализации.

## Conformance и manifest

Conformance tool проверяет новые endpoint classifications, точную форму
chat-first schemas и ссылки всех девяти manifest entries на OpenAPI operation и
runtime route. Проверка не запускает backend и не повышает readiness. Runtime
evidence дает `PlatformClientContractTest`.

## CORS policy

CORS plugin не подключен. Текущая политика — default-deny. Будущая allowlist
может содержать только точные origin со scheme, host и port; wildcard и
credentials запрещены. Активация допустима только вместе с реальным
cross-origin web-клиентом в отдельном этапе.

## Проверки

- Targeted `PlatformClientContractTest` — пройден.
- Полный backend test suite — пройден.
- `tools/openapi-conformance`: `npm test`, `npm run check` — пройдены;
  blocking findings отсутствуют, итоговый status остается `not_ready`.
- Frontend: tests, lint, build — пройдены без изменения frontend behavior.
- `git diff --check`, secret scan и итоговая scope-проверка — пройдены;
  неожиданных файлов или изменений вне Stage 10.3 нет.

## Ограничения и verdict

Stage 10.3 завершает только ограниченное укрепление platform-neutral contract.
Он не является заявлением OpenAPI/SDK или production readiness. Stores остаются
process-local; resume/cross-device sync, auth и generated clients отсутствуют.

**Verdict:** chat-first subset согласован с текущим runtime и пригоден как
кандидат для выбора следующего клиента. Рекомендуемый Stage 10.4 — выбрать
первую дополнительную платформу и стратегию потребления API/SDK без реализации
native UI.
