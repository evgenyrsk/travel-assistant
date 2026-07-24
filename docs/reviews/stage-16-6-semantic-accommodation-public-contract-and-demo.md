# Stage 16.6 — Public contract и demo polling

## Статус

Завершён. Следующий разрешённый шаг — Stage 16.7.

## Scope

- [x] Product endpoints сохранены без добавления нового публичного route.
- [x] `appliedPreferences.accommodationConcept` публикует только `glamping`.
- [x] Offer contract публикует только bounded `concept`, `verdict` и
  `evidenceSources`; raw rationale отсутствует.
- [x] Search lifecycle различает `searching`, offers/no-offers,
  no-semantic-matches и `failed`.
- [x] `metadata.analysis` содержит bounded counters/status и
  `pollAfterMillis` только для `searching`.
- [x] Demo shell опрашивает существующий offers endpoint с backoff от 1 до 3
  секунд и прекращает polling через 120 секунд.
- [x] `failed` и no-semantic-matches не показывают обычные отели и имеют
  отдельные empty states.
- [x] `partial` явно сообщается пользователю; карточка показывает «Подходит»
  или «Вероятно подходит» и только разрешённые типы оснований.
- [x] OpenAPI conformance проверяет lifecycle, analysis metadata,
  semantic-match schema и закрытый concept catalog.
- [x] Operational endpoints не добавлены в product-client subset; readiness
  остаётся `not_ready`.

## Out of scope

- REAL OpenRouter/provider вызовы;
- изменение ranking или success responses обычного поиска;
- generated SDK и readiness promotion;
- automatic fallback на обычные отели;
- observability events/metrics и quality evaluation Stage 16.7.

## Review findings и fixes

- Обычный поиск сохраняет прежнее ranking explanation; semantic explanation
  выбирается только при `accommodationConcept=glamping`.
- Poll interval из backend нормализуется в диапазон 1–3 секунды, а client-side
  backoff ограничен тем же диапазоном.
- UI не отображает неизвестные evidence sources или произвольные поля model
  output.
- Отсутствующий semantic contract остаётся валидным для обычного поиска.

## Проверки

- [x] Backend targeted API DTO tests.
- [x] Backend `./gradlew test`.
- [x] Frontend `npm test`.
- [x] Frontend `npm run lint`.
- [x] Frontend `npm run build`.
- [x] OpenAPI conformance `npm test` и `npm run check`.
- [x] `git diff --check`.

## Итог

FAKE semantic flow теперь доступен через существующий публичный API и demo
shell как async lifecycle. REAL vision остаётся выключенным до закрытия policy,
ZDR/model и quality gates.
