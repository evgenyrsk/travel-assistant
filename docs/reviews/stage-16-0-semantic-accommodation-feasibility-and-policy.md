# Stage 16.0 — Semantic accommodation feasibility and policy

## Статус

Завершён как bounded design/policy slice. Stage 16 активирован, следующий
разрешённый шаг — Stage 16.1. REAL vision не активирован.

## Scope

- [x] Проверена реализуемость поверх существующего hotel-only backend и
  действующих API routes без нового публичного endpoint.
- [x] Зафиксирован первый закрытый concept `GLAMPING` и его границы.
- [x] Разделены provider facts и semantic assessment ассистента.
- [x] Зафиксированы privacy, data-rights и model-compatibility gates.
- [x] Определены минимальный evaluation dataset и rollout thresholds.

## Out of scope

- backend/frontend implementation Stage 16.1–16.7;
- новый публичный endpoint;
- передача реальных provider descriptions или images внешней модели;
- controlled REAL details/image/model probe;
- активация OpenRouter vision;
- `APARTMENT`, booking, payment, durable cache и multi-instance scheduler.

## Feasibility verdict

Функциональность реализуема в текущей архитектуре. Search уже предоставляет до
20 offers с названием и первым безопасным изображением, а details boundary —
descriptions, amenities и дополнительные images для выбранного offer. Для
semantic flow потребуется отдельный application port, асинхронное состояние
search, bounded details enrichment и расширение существующего offers contract.
Текстовый `LlmClient` не должен становиться multimodal boundary.

## Taxonomy

`GLAMPING` трактуется широко: оборудованные tents, domes, yurts, safari tents,
tiny houses и отдельные cabins в природном формате. Обычные hotel rooms,
apartment blocks, пустые camping pitches и стандартные cottages без признаков
glamping исключаются. `APARTMENT` остаётся неактивным до отдельного определения
и quality evaluation.

## Data и privacy gates

| Gate | Статус | Следствие |
|---|---|---|
| Право передавать provider descriptions внешней модели | Не подтверждено | REAL text enrichment запрещён |
| Право передавать provider images внешней модели | Не подтверждено | REAL image analysis запрещён |
| Выбранная модель принимает multiple image inputs и strict schema | Не проверено на runtime endpoint | Controlled probe отложен |
| Endpoint совместим с `require_parameters=true`, `data_collection=deny`, `zdr=true` | Не проверено | REAL adapter не активируется |
| Exact HTTPS image-host allowlist согласован | Не определён | REAL URL forwarding запрещён |

До закрытия gates разрешены Stage 16.1, provider-neutral classification core,
детерминированный FAKE mode, synthetic fixtures и evaluation harness без
provider content. Реальные вызовы и автоматические retries не выполнялись.

## Quality evaluation policy

- минимум 100 вручную размеченных кандидатов из нескольких направлений;
- precision `MATCH` >= 90%;
- precision `MATCH + PROBABLE` >= 80%;
- recall по широкому определению glamping >= 70%;
- false-positive rate для обычных отелей <= 5%;
- два независимых reviewer для пограничной части;
- provider images не коммитятся без подтверждённых прав; при отсутствии
  разрешения dataset хранится в одобренном внутреннем контуре, а репозиторий
  содержит только harness, schema и агрегированный report.

## Решения

- semantic concept является managed enum, а не свободным prompt;
- semantic verdict является assistant assessment, не provider fact;
- ordinary hotel search остаётся синхронным и неизменным;
- semantic search получит bounded async lifecycle в отдельном sub-stage;
- отсутствие semantic matches не приводит к автоматическому показу обычных
  отелей;
- запрос на booking получает явное объяснение границы и продолжается как
  подбор проживания.

## Проверки

- [x] Scope согласован с product и architecture baselines.
- [x] Stage sequencing зафиксирован в основном roadmap.
- [x] Navigation roadmap обновлён без дублирования mutable status.
- [x] REAL provider/model calls отсутствуют.
- [x] `git diff --check` — gate этого commit.

## Итог

Stage 16.0 готов к закрытию. Реализация может перейти к Stage 16.1 и далее к
FAKE classification slice. REAL vision остаётся заблокированным до явного
закрытия всех внешних gates.
