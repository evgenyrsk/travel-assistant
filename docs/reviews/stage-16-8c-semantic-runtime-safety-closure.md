# Stage 16.8c — Semantic runtime safety regression и closure

## Статус

Завершён 27 июля 2026 года. Stage 16 закрыт в разрешённом FAKE scope.
Stage 16.9 и REAL semantic activation остаются заблокированными и не являются
активным backlog.

## Scope

- [x] Широкие FAKE-patterns `дом...` и `гор...` заменены явными допустимыми
  словоформами с lexical boundaries.
- [x] `город`, `городской`, `домашний`, `горячий` и обычные business/hotel
  descriptions не создают glamping evidence.
- [x] Nature-only и amenity-only evidence без glamping structure не формируют
  видимый `MATCH` или `PROBABLE`.
- [x] Explicit glamping label, dome, yurt, safari tent, tiny house и
  structure с supporting evidence сохранены как deterministic positive
  fixtures.
- [x] Обычные hotel, hostel, apartment block, empty camping pitch и standard
  cottage fixtures остаются `NO_MATCH`.
- [x] Полный FAKE flow остаётся network-free и детерминированно возвращает
  `completed_no_semantic_matches` для обычных synthetic hotels.
- [x] Полные backend, frontend, launcher и OpenAPI conformance gates пройдены.

## Реализация и review findings

FAKE-анализатор теперь считает nature и amenities только supporting signals.
Видимый verdict требует explicit glamping label или structural evidence:

| Evidence | FAKE verdict |
|---|---|
| Explicit label без отрицательного сигнала | `MATCH` |
| Structure и независимый supporting signal | `MATCH` |
| Только structure | `PROBABLE` |
| Nature и/или amenity без structure | `UNKNOWN` |
| Только отрицательный формат | `NO_MATCH` |
| Structure/explicit вместе с отрицательным форматом | `PROBABLE` |

Review обнаружил существующий Minor: pattern apartment block не принимал
`апарт-отель` из-за soft-sign boundary. Pattern исправлен и покрыт negative
fixture. Critical, Major и других релевантных Minor замечаний после
исправлений не осталось.

Provider-neutral validation, OpenRouter adapter, taxonomy `GLAMPING`, ranking,
provider mapping, ordinary hotel search и public wire contract не изменены.

## Проверки

- [x] Targeted backend tests для `FakeAccommodationAnalysisClientTest` и
  `SemanticRuntimeSafetyIntegrationTest`.
- [x] Backend:
  `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test --rerun-tasks`.
- [x] Frontend: `npm test` — 47 tests, `npm run lint`, `npm run build`.
- [x] Launcher: `node --test scripts/local-demo.test.mjs` — 8 tests.
- [x] Network-free `--fake --check-only` показал
  `LLM=FAKE, Hotels=FAKE, Semantic=FAKE`.
- [x] Network-free `--real --check-only` с synthetic configuration показал
  `LLM=OPENROUTER, Hotels=REAL, Semantic=FAKE`.
- [x] OpenAPI conformance: `npm test` — 12 tests; read-only `./check` вернул
  `status=not_ready`, `readinessClaim=false`, `blockingFindings=[]`.
- [x] Rights-safe semantic evaluation harness: `npm test` — 4 tests.
- [x] `git diff --check` после documentation sync.
- [ ] REAL semantic smoke — намеренно не выполнялся: права на provider content,
  exact model/provider endpoint, controlled ZDR probe и quality dataset не
  подтверждены.

## Scope control

Не выполнялись REAL OpenRouter semantic call, передача provider
descriptions/images внешней модели, model bake-off, Stage 16.9, изменение
taxonomy, ranking/provider mapping, durable storage, deployment manifests или
изменения `main`.

## Итог

Stage 16.8a–16.8c закрывают mixed-runtime safety, async UX и deterministic FAKE
false positives. Любая подготовка или активация REAL semantic runtime требует
отдельной явной задачи Stage 16.9 и прохождения всех внешних gates.
