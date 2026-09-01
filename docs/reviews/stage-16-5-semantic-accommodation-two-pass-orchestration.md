# Stage 16.5 — Semantic accommodation two-pass orchestration

## Статус

Завершён. Следующий разрешённый шаг — Stage 16.6.

## Scope

- [x] Provider search и semantic analysis подключены только для managed
  semantic preference.
- [x] Coarse pass получает максимум 20 ranked candidates: name и одно search
  image.
- [x] Deep pass получает максимум 6 candidates: filtered descriptions,
  amenities и до трёх details images.
- [x] Deep priority: `PROBABLE`, single-source `MATCH`, `UNKNOWN`, затем
  остальные candidates в прежнем deterministic order.
- [x] Не более трёх details calls и двух analysis calls выполняются
  параллельно; scheduler сохраняет общий budget 45 секунд.
- [x] Deep verdict заменяет coarse; unavailable details/deep analysis сохраняют
  coarse и дают `partial`.
- [x] Empty provider result отличается от zero semantic matches.
- [x] Coarse classifier outage завершает search как `failed` без обычных
  отелей и без retry.
- [x] Добавлен bounded process-local LRU details cache на 128 entries.
- [x] Details, загруженные deep pass, переиспользуются при последующем открытии
  выбранной карточки.
- [x] Runtime composition создаёт analysis runtime и закрывает scheduler/client
  при shutdown; default mode остаётся `FAKE`.
- [x] `OPENROUTER` дополнительно требует явный
  `ACCOMMODATION_ANALYSIS_EXTERNAL_CONTENT_APPROVED=true`.

## Out of scope

- public response fields/OpenAPI/frontend polling;
- durable/distributed cache или multi-instance scheduler;
- automatic retries;
- ordinary-hotel fallback;
- REAL provider/model probe;
- Stage 16 observability и quality dataset.

## Security и privacy review

- Orchestrator создаёт `candidate-001` identifiers и не передаёт
  session/search/offer/provider IDs analysis client.
- Provider reference используется только для внутреннего details call/cache.
- Deep input использует уже отфильтрованные provider details модели.
- Model output/rationale не хранится; в search сохраняются только bounded
  semantic match и evidence sources.
- REAL activation fail-closed без отдельного external-content approval flag,
  allowlist и OpenRouter config.

## Проверки

- [x] Coarse cap 20 и deep cap 6.
- [x] Details concurrency <= 3 и analysis concurrency <= 2.
- [x] Deep replacement и partial fallback.
- [x] Zero provider offers.
- [x] Zero semantic matches.
- [x] Classifier outage.
- [x] Bounded LRU eviction.
- [x] Details cache reuse без второго provider call.
- [x] OpenRouter activation gate.
- [x] Ordinary search regression.
- [x] Backend `./gradlew test` — passed.
- [x] `git diff --check` — gate commit.

## Итог

Backend behavior стабилизирован перед изменением публичного контракта. REAL
vision не запускался; текущий исполняемый semantic flow использует
детерминированный FAKE adapter по умолчанию.
