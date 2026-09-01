# Stage 16.8b — Async UX и прозрачность runtime modes

## Статус

Завершён 27 июля 2026 года. Следующий разрешённый отдельный шаг — Stage 16.8c.
REAL semantic activation остаётся заблокированным будущим этапом.

## Scope

- [x] Initial `searching` message сообщает о запуске проверки типа размещения
  и не выглядит как duplicate.
- [x] Настоящий duplicate сохраняет отдельное сообщение «Этот поиск уже
  выполняется».
- [x] Terminal frontend states заменяют loading status и не утверждают, что
  анализ продолжается после публикации результата.
- [x] Launcher показывает отдельные LLM, Hotels и semantic modes без secrets,
  model slug или endpoint.
- [x] `--fake` использует `FAKE / FAKE / FAKE`, а `--real` —
  `OPENROUTER / REAL / FAKE`.
- [x] Demo launcher не активирует REAL semantic analysis и не импортирует его
  mode из env-файла.

## Реализация и review findings

Backend initial copy изменён на «Проверка типа размещения запущена». Он
фиксирует исторический факт запуска и остаётся корректным после завершения
polling. Duplicate message не изменён и покрыт отдельным regression test.

Production frontend уже заменял loading status для terminal
`completed_with_offers`, `completed_no_semantic_matches` и `failed`. Изменение
frontend logic не потребовалось; tests теперь явно блокируют возврат stale
loading presentation после terminal response.

Launcher принудительно устанавливает `ACCOMMODATION_ANALYSIS_MODE=FAKE` для
обоих локальных профилей и печатает только allowlisted mode values. Даже если
shell environment содержит opt-in semantic configuration, demo launcher не
активирует её до отдельного Stage 16.9.

Critical, Major и релевантных Minor замечаний после review не осталось.

## Проверки

- [x] Targeted backend test:
  `./gradlew test --tests 'com.travelassistant.backend.application.assistant.ComposeConfirmedSearchTransitionResponseUseCaseTest'`.
- [x] Полный backend regression gate без Gradle cache:
  `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test --rerun-tasks`.
- [x] Frontend: `npm test` — 47 tests, `npm run lint`, `npm run build`.
- [x] Launcher: `node --test scripts/local-demo.test.mjs` — 8 tests.
- [x] Network-free `--fake --check-only` показал
  `LLM=FAKE, Hotels=FAKE, Semantic=FAKE`.
- [x] Network-free `--real --check-only` с synthetic configuration показал
  `LLM=OPENROUTER, Hotels=REAL, Semantic=FAKE`.
- [x] `git diff --check` после финальной documentation sync.

OpenAPI и conformance tool не изменены: Stage 16.8b меняет message copy,
frontend regression coverage и локальный launcher, но не wire shape, status,
endpoint или schema. Полный OpenAPI conformance gate остаётся частью отдельного
Stage 16.8c closure.

## Вне scope

- Stage 16.8c negative fixtures и общий closure;
- Stage 16.9 и REAL semantic activation;
- REAL OpenRouter semantic call или controlled live probe;
- передача provider descriptions/images внешней модели;
- изменение taxonomy `GLAMPING`, ranking, provider mapping или обычного hotel
  search;
- durable storage, deployment manifests, auth или изменения `main`.

## Итог

Async transcript больше не сохраняет вводящее в заблуждение утверждение о
неготовых результатах, terminal UI явно завершает loading presentation, а
operator видит три независимых runtime mode. Локальный REAL-профиль остаётся
совместимым с fail-closed policy Stage 16.8a и не включает REAL semantic
analysis.
