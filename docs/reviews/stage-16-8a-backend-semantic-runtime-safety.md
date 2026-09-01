# Stage 16.8a — Backend semantic runtime safety

## Статус

Завершён 27 июля 2026 года. Следующий разрешённый отдельный шаг — Stage 16.8b.
REAL semantic activation остаётся заблокированным будущим этапом.

## Scope

- [x] Composition-level policy проверяет совместимость Hotels и semantic modes.
- [x] Комбинация REAL Hotels + FAKE semantic запрещена.
- [x] В запрещённой комбинации analyzer runtime и background scheduler не
  создаются.
- [x] Semantic request не вызывает Hotels API, сохраняется как terminal
  `failed` с пустыми offers и возвращает фактический сохранённый snapshot.
- [x] Terminal operational event использует bounded `FAILED` outcome без
  hotel, provider или user content.
- [x] Обычный REAL hotel search без semantic concept не изменён.
- [x] Полный FAKE Hotels + FAKE semantic flow остался network-free и
  детерминированным.

## Compatibility matrix

| Hotels mode | Semantic mode | Результат Stage 16.8a |
|---|---|---|
| `FAKE` | `FAKE` | Разрешён |
| `FAKE` | `OPENROUTER` | Без изменений |
| `REAL` | `FAKE` | Запрещён; semantic search завершается fail-closed |
| `REAL` | `OPENROUTER` | Без изменений |

Policy находится на runtime/composition boundary. Domain и public API не знают
о provider modes; существующие `failed` status и analysis metadata переиспользованы
без нового endpoint, wire status или изменения success contracts.

## Review findings и fixes

- При отклонённом launch первоначальная реализация могла вернуть исходный
  `searching` object вместо фактически сохранённого `failed`. Use case теперь
  возвращает результат atomic transition.
- Self-review выявил слабый fallback при неожиданном исчезновении записи между
  `save` и transition. Он заменён повторным сохранением подготовленного
  `failed` snapshot, чтобы fail-closed инвариант сохранялся.
- Compatibility policy покрыта полной матрицей; отдельный test подтверждает,
  что factory несовместимого semantic runtime не вызывается.
- Ktor integration tests подтверждают отсутствие Hotels API/analyzer dependency
  calls в запрещённой ветке и сохранение network-free FAKE flow.

Critical, Major и релевантных Minor замечаний после исправлений не осталось.

## Проверки

- [x] Targeted backend tests для policy, use case и runtime integration:
  `./gradlew test --tests ...`.
- [x] Полный backend regression gate:
  `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test`.
- [x] Обычный REAL non-semantic search покрыт существующим
  `ProviderSeamIntegrationTest.realProviderModeUsesPublicAutocompleteAndSearchComposition`.
- [x] Public failed response, idempotency и остальные backend contracts прошли
  полный regression suite.
- [x] REAL external calls и automatic retries не выполнялись.

Frontend и OpenAPI gates не запускались: Stage 16.8a не изменяет frontend,
OpenAPI, conformance tool или wire contract; эти области зарезервированы для
следующих отдельных sub-stage.

## Вне scope

- Stage 16.8b async copy, terminal UI и mode diagnostics;
- Stage 16.8c negative fixtures и общий closure;
- REAL OpenRouter semantic call или controlled live probe;
- передача provider descriptions/images внешней модели;
- изменение taxonomy `GLAMPING`, ranking или provider mapping;
- новый endpoint, durable storage и Stage 16.9.

## Итог

Backend больше не может публиковать пользовательские semantic verdict из
детерминированного FAKE analyzer поверх REAL hotel data. Небезопасный mixed mode
завершается до provider/analyzer work существующим terminal `failed` результатом,
а разрешённые runtime combinations сохраняют прежнее поведение.
