# Stage 16.8 — Semantic runtime safety plan

## Статус

Планирование и решение завершены 24 июля 2026 года. Реализация не начата.
Следующий разрешённый шаг — Stage 16.8a. REAL semantic activation остаётся
заблокированным будущим этапом.

## Цель

Не допускать пользовательских утверждений о соответствии типу размещения,
если REAL Hotels API работает вместе с детерминированным `FAKE` semantic analyzer.
Отдельно исправить вводящий в заблуждение async-текст и сделать активные режимы
локального demo проверяемыми оператором.

## Основание

Ручной сценарий с предпочтением `GLAMPING` вернул обычные городские отели как
`PROBABLE`. Operational logs подтвердили REAL hotel search, coarse-анализ за
2 ms, deep-анализ за 4 ms, `matchCount=0` и `probableCount=2`. Такой профиль
использовал REAL provider data, но оставил `ACCOMMODATION_ANALYSIS_MODE=FAKE`.

`FakeAccommodationAnalysisClient` не анализирует изображения и использует
детерминированные text patterns. Широкие regex patterns для `дом...` и `гор...` могут
совпадать с обычными словами вроде `домашний`, `город` и `городской`; один
положительный signal сейчас достаточен для `PROBABLE`. Это допустимо только для
synthetic fixtures, но не для пользовательской оценки REAL offers.

Первоначально созданный semantic search корректно получает status `searching`,
однако текст `Поиск уже выполняется, результаты пока не готовы` звучит как
duplicate. После завершения polling карточки обновляются, а этот текст остаётся в
истории и противоречит видимому состоянию.

## Решения

| Решение | Зафиксированное поведение |
|---|---|
| Совместимость режимов | REAL Hotels + FAKE semantic не формирует `MATCH` или `PROBABLE` для пользователя |
| Fail-closed результат | Semantic search использует существующий terminal `failed` status и не показывает обычные hotel offers автоматически |
| Границы FAKE | `FakeAccommodationAnalysisClient` остаётся network-free implementation для synthetic fixtures и полного FAKE flow |
| Async-текст | Первоначальный `searching` сообщает о запуске анализа; слово `уже` используется только для настоящего duplicate |
| Terminal presentation | После завершения polling UI не утверждает, что результаты ещё не готовы |
| Прозрачность режимов | Launcher или diagnostics безопасно показывают отдельные LLM, Hotels и semantic modes без secrets и model slug |
| REAL activation | Stage 16.8 не включает OpenRouter semantic mode и не отправляет provider content во внешнюю модель |

## Последовательность реализации

### Stage 16.8a — Backend semantic runtime safety

- добавить composition-level compatibility policy для Hotels и semantic modes;
- при REAL Hotels + FAKE semantic сохранить обычный non-semantic search без
  изменений, а semantic search завершать fail-closed;
- не переносить provider mode awareness в domain;
- покрыть REAL/FAKE combinations и отсутствие analyzer/provider calls в
  запрещённой ветке;
- использовать существующий public `failed` status, если contract audit не
  выявит блокирующую неоднозначность.

### Stage 16.8b — Async UX и mode transparency

- заменить первоначальный processing copy на сообщение о запуске анализа;
- сохранить отдельное duplicate-сообщение;
- согласовать terminal UI с результатом polling;
- явно показать три независимых режима в launcher output или diagnostics;
- обновить local demo guide после фактического изменения поведения.

### Stage 16.8c — Regression и closure

- добавить negative fixtures для `город`, `городской`, `домашний`, обычных
  business/hotel descriptions и упоминаний реки или природы без glamping
  structure;
- подтвердить, что FAKE fixtures остаются детерминированными;
- проверить ordinary hotel search, semantic async lifecycle, duplicate
  confirmation, empty/failure states и отсутствие sensitive data;
- выполнить полные backend/frontend/OpenAPI conformance gates;
- создать отдельный closure review report; REAL calls не выполнять.

Каждый sub-stage выполняется отдельным commit и review report. Backend runtime
safety не объединяется с frontend/demo изменениями.

## Критерии приёмки

- [ ] REAL Hotels + FAKE semantic не показывает ни одной карточки с semantic
      verdict и завершается safe `failed` state.
- [ ] Обычный REAL hotel search без accommodation concept не меняется.
- [ ] Полный FAKE flow остаётся network-free и детерминированным.
- [ ] Первоначальное async message не выглядит как duplicate.
- [ ] При terminal result интерфейс не показывает stale утверждение о
      неготовых результатах.
- [ ] Operator может определить LLM, Hotels и semantic modes без чтения process
      environment и без раскрытия secrets/model slug.
- [ ] Tests блокируют ложные `PROBABLE` для обычных городских отелей.
- [ ] Logs и metrics не содержат hotel names, descriptions, image URLs,
      пользовательский текст или provider identifiers.

## Проверки

- backend: `cd services/backend && ./gradlew test`;
- frontend: `npm test`, `npm run lint`, `npm run build`;
- OpenAPI conformance tests/check;
- `git diff --check`;
- один локальный FAKE smoke и один REAL-profile smoke без semantic vision call и
  без автоматического live retry.

## Stage 16.9 — Future REAL activation gate

Stage 16.9 не активирован и не является implementation backlog. Для отдельного
старта необходимы все условия:

- явное разрешение на передачу provider descriptions и images конкретному
  внешнему model/provider endpoint;
- exact HTTPS image-host allowlist;
- controlled compatibility probe для `require_parameters=true`,
  `data_collection=deny` и `zdr=true` без автоматического retry;
- минимум 100 rights-approved вручную размеченных candidates;
- precision `MATCH` не ниже 90%, precision `MATCH + PROBABLE` не ниже 80%,
  recall не ниже 70% и false-positive rate обычных отелей не выше 5%;
- отдельное решение о rollout и monitoring после quality report.

## Вне границ Stage 16.8

- REAL semantic/model call;
- передача provider content внешней модели;
- новая accommodation category;
- изменение taxonomy `GLAMPING`;
- изменение provider mapping, ranking или обычного hotel search;
- новый public endpoint, durable storage или deployment manifests.
