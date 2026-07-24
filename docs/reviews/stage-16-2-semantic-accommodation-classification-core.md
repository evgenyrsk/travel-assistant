# Stage 16.2 — Semantic accommodation classification core

## Статус

Завершён. Следующий разрешённый шаг — Stage 16.3 без REAL activation.

## Scope

- [x] Добавлен application-owned `AccommodationAnalysisClient`.
- [x] Добавлены managed verdicts `MATCH`, `PROBABLE`, `NO_MATCH`, `UNKNOWN`.
- [x] Добавлены bounded evidence sources `name`, `description`, `amenities`,
  `image` и закрытые internal signals.
- [x] Analysis contract использует только ephemeral candidate IDs.
- [x] Complete batch validation отклоняет duplicate, missing и unknown IDs.
- [x] Один положительный signal ограничивается `PROBABLE`; `MATCH` требует
  explicit glamping label либо минимум два независимых positive signals.
- [x] Positive/negative contradiction ограничивает результат `PROBABLE`.
- [x] Deep result заменяет coarse; отсутствующий deep result сохраняет coarse и
  устанавливает `partial`.
- [x] Selection показывает только `MATCH` и `PROBABLE`, группирует `MATCH`
  первым и сохраняет прежний deterministic rank внутри группы.
- [x] Добавлен deterministic network-free FAKE adapter.

## Out of scope

- OpenRouter transport, JSON schema и image URL policy;
- runtime composition/configuration;
- async search lifecycle и background jobs;
- details enrichment/cache;
- public API/OpenAPI/frontend;
- observability и REAL calls.

## Security и privacy review

- Port не принимает session/search/offer/provider IDs по смыслу контракта;
  orchestrator обязан создавать ephemeral IDs.
- Raw rationale отсутствует в result model.
- Evidence и failure taxonomy закрыты enums.
- FAKE adapter не выполняет network calls и не анализирует содержимое image
  URL; synthetic URL в test подтверждает это ограничение.
- Logging в classification core отсутствует.

## Проверки

- [x] Все verdicts и evidence normalization.
- [x] Duplicate/missing/unknown candidate results.
- [x] Недостаточные основания для `MATCH`.
- [x] Противоречивые positive/negative signals.
- [x] Coarse/deep replacement и partial fallback.
- [x] MATCH/PROBABLE selection и стабильность прежнего ranking.
- [x] Deterministic FAKE fixtures.
- [x] Backend `./gradlew test` — gate sub-stage.
- [x] `git diff --check` — gate commit.

## Итог

Classification policy отделена от LLM/provider transport и готова для
OpenRouter adapter и последующего async orchestration. REAL provider/model
content не использовался.
