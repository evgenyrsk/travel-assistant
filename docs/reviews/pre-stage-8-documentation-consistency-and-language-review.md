# Pre-Stage 8 — проверка согласованности и языка документации

## 1. Verdict

Passed — documentation consistency and language review completed.

## 2. Scope

Выполнена документационная проверка после закрытия Stage 7 и до отдельного решения о начале Stage 8.

В рамках задачи:

- проверены активные статусы и роли документов;
- устранены устаревшие ссылки на продолжающийся Stage 7;
- уточнены формулировки об источниках истины;
- облегчены смешанные русско-английские фразы в активной документации;
- смысл Stage 7 и порядок roadmap не изменялись.

Не менялись:

- backend code и backend tests;
- frontend code и frontend tests;
- OpenAPI contracts;
- generated clients;
- manifest;
- Gradle/CI;
- `tools/openapi-conformance/**`.

Stage 8 не начинался.

## 3. Inputs reviewed

Проверены:

- `AGENTS.md`;
- `README.md`;
- `docs/ROADMAP.md`;
- `docs/roadmap/roadmap.md`;
- `docs/reviews/README.md`;
- `docs/product/product-baseline.md`;
- `docs/architecture/architecture-baseline.md`;
- `services/backend/README.md`;
- `app/README.md`;
- активные и справочные документы в `docs/development/**`;
- исторические отчеты Stage 7 как источник фактов без их переписывания.

## 4. Status consistency review

Активные статусные документы согласованы:

- Stage 7 завершен в границах ограниченной hotel-only основы MVP;
- Stage 8 запланирован, но не начат;
- закрытие Stage 7 не означает готовность к промышленному использованию;
- generated clients, расширение manifest, real provider, booking, durable storage, auth, CI gate и полная browser-to-backend проверка остаются будущей работой.

Прямого конфликта статусов между `docs/roadmap/roadmap.md`, продуктовой и архитектурной основами не найдено. Исправлены устаревшие фразы в `docs/development/roadmap.md` и `docs/development/milestones.md`, которые все еще говорили о «следующем шаге Stage 7» вместо общего следующего этапа.

## 5. Source-of-truth review

Роли активных документов уточнены без удаления или объединения файлов:

| Документ | Роль |
|---|---|
| `docs/roadmap/roadmap.md` | Основной roadmap и единственный источник текущих статусов и следующего разрешенного шага |
| `docs/ROADMAP.md` | Навигационная карта назначения этапов без текущих статусов |
| `docs/product/product-baseline.md` | Текущая продуктовая основа и границы MVP |
| `docs/architecture/architecture-baseline.md` | Текущая архитектурная основа и принятое решение о стеке backend |
| `docs/reviews/README.md` | Индекс исторических отчетов и правил их чтения |
| `services/backend/README.md`, `app/README.md` | Инструкции и описание фактического состояния соответствующих модулей |
| `docs/development/**` | Активные инженерные правила и явно помеченные справочные материалы |
| `AGENTS.md` | Обязательная входная точка правил для Codex/AI-агентов |

Выявлен конфликт языковой политики: `docs/development/documentation-guidelines.md` предписывал английский для инженерных документов, тогда как более приоритетный `AGENTS.md` задает русский текст по умолчанию для активной документации. Правило в development-документе приведено в соответствие с `AGENTS.md`; последовательно англоязычные технические документы при этом не переписывались.

## 6. Language cleanup

Точечно исправлены:

- `readiness`, `scope`, `carryover`, `implementation`, `review-only` и похожие слова внутри обычных русских предложений;
- тяжелые описания ролей документов через `primary roadmap`, `source of truth`, `active backlog`;
- смешанные формулировки текущего состояния Stage 7 в продуктовой и архитектурной основах;
- вводные описания backend и frontend.

Намеренно оставлены технические термины и идентификаторы:

- `frontend`, `backend`, `OpenAPI`, `API`, `LLM`, `CI`, `Gradle`, `manifest`, `generated clients`;
- пути, команды, endpoint paths, JSON fields, class names и commit messages;
- устойчивые термины вроде `process-local`, `fake provider`, `runtime` и `E2E`, когда перевод снижал бы точность;
- последовательно англоязычные инженерные документы, чтобы не превращать точечную чистку в массовый перевод.

## 7. Files changed

| Файл | Изменение |
|---|---|
| `README.md` | Уточнены навигационные роли и облегчены смешанные формулировки |
| `docs/ROADMAP.md` | Роль обзорного roadmap описана по-русски и без конкуренции с основным roadmap |
| `docs/roadmap/roadmap.md` | Очищены текущие статусные формулировки и описание перенесенных пунктов |
| `docs/reviews/README.md` | Уточнена роль исторического журнала и добавлен этот отчет |
| `docs/product/product-baseline.md` | Текущий статус Stage 7 и продуктовая роль изложены яснее |
| `docs/architecture/architecture-baseline.md` | Текущий архитектурный статус и перенесенные вопросы изложены яснее |
| `services/backend/README.md` | Облегчено описание фактической Stage 7 основы без изменения поведения |
| `app/README.md` | Уточнено описание минимального frontend-сценария |
| `docs/development/README.md` | Роли активных и справочных инженерных документов изложены по-русски |
| `docs/development/roadmap.md` | Удалена устаревшая привязка следующего шага к Stage 7 |
| `docs/development/milestones.md` | Удалена устаревшая привязка следующего шага к Stage 7 |
| `docs/development/documentation-guidelines.md` | Языковая политика синхронизирована с `AGENTS.md` |
| `docs/development/implementation-strategy.md` | Очищены вводные формулировки и роль справочного документа |
| `docs/reviews/pre-stage-8-documentation-consistency-and-language-review.md` | Зафиксированы результаты этой проверки, границы изменений и оставшиеся замечания |

## 8. Remaining notes

- Исторические отчеты `docs/reviews/stage-*` намеренно не переписывались, даже если в них есть смешанная терминология или устаревшие на сегодня статусы.
- Последовательно англоязычные инженерные правила не переводились массово; отдельная полная локализация потребовала бы самостоятельного решения.
- Большая историческая таблица Stage 7 в основном roadmap сохранена для прослеживаемости. Эта проверка очищает текущие сводки, а не переписывает историю этапа.

## 9. Validation

- `git diff --check` — passed.
- Поиск смешанных терминов выполнен командой из задачи; совпадения разобраны по контексту, технические идентификаторы и исторические артефакты не заменялись автоматически.
- Поиск статусов подтверждает: Stage 7 завершен, Stage 8 запланирован и не начат, перенесенные пункты не представлены как выполненные.
- Ссылки на этот отчет проверены в `docs/reviews/README.md`.
