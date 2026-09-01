# Stage 16.4 — Semantic accommodation async lifecycle

## Статус

Завершён. Следующий разрешённый шаг — Stage 16.5.

## Scope

- [x] Добавлены search statuses `searching`,
  `completed_no_semantic_matches`, `failed` при сохранении прежних terminal
  statuses.
- [x] Добавлена typed analysis metadata с counts, `partial` status и
  `pollAfterMillis` только для `searching`.
- [x] Semantic search сохраняется до background launch и сразу получает opaque
  `hotelSearchId`.
- [x] Ordinary search остаётся синхронным и не проходит через scheduler.
- [x] Application-owned scheduler допускает не более одного job на search ID.
- [x] Общий job budget по умолчанию — 45 секунд; timeout/exception дают
  terminal `failed` без retry.
- [x] State store поддерживает atomic compare-by-status transition.
- [x] Late result не перезаписывает существующее terminal state.
- [x] Shutdown отменяет scope; cancelled job не публикует поздний результат.
- [x] Existing confirmation idempotency key уже включает semantic concept, а
  scheduler дополнительно блокирует duplicate launch.

## Out of scope

- provider/coarse/deep execution job;
- details concurrency/cache;
- analysis concurrency limits;
- runtime composition и shutdown hook в Ktor `Application`;
- public response metadata/OpenAPI/frontend polling;
- observability Stage 16.7.

## Architecture review

- Scheduler и job contracts находятся в application layer и не зависят от
  Ktor или infrastructure.
- Store transition использует expected `SEARCHING` status и не допускает
  terminal-to-terminal overwrite.
- `CreateHotelSearchUseCase` не вызывает provider синхронно при наличии
  semantic concept.
- При отсутствии configured launcher semantic request fail-closed и никогда не
  возвращает обычные отели.

## Проверки

- [x] `searching` сохранён до launcher invocation.
- [x] Provider не вызван в synchronous semantic creation path.
- [x] Duplicate scheduler launch не создаёт второй job.
- [x] Terminal completion публикуется атомарно.
- [x] Timeout даёт `failed` и выполняется один раз.
- [x] Shutdown cancellation оставляет незавершённый process-local search без
  поздней записи.
- [x] Late result не перезаписывает terminal state.
- [x] Ordinary search regression tests.
- [x] Backend `./gradlew test` — gate sub-stage.
- [x] `git diff --check` — gate commit.

## Итог

Lifecycle готов принять двухпроходный semantic job. До Stage 16.5 runtime не
запускает provider/analysis work для semantic request; безопасный fallback на
обычные отели отсутствует.
