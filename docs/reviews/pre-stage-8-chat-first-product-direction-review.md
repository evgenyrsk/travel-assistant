# Pre-Stage 8 — уточнение chat-first направления продукта

## 1. Verdict

Passed — chat-first direction clarified before Stage 8.

## 2. Scope

Выполнено только уточнение активной документации перед отдельным решением о начале Stage 8.

Не менялись:

- backend и frontend code;
- backend и frontend tests;
- OpenAPI contracts;
- generated clients;
- manifest;
- Gradle/CI;
- `tools/openapi-conformance/**`.

Stage 8 не начинался. LLM integration и real provider integration не выполнялись.

## 3. Trigger

Локальный запуск frontend показал, что пользователь видит отдельную structured форму поиска отелей. Эта форма была сознательно создана в Stage 7.51 как минимальный ручной клиент для проверки связности Assistant session, hotel search, fake provider, ranking и отображения offers.

Без дополнительного пояснения форму можно ошибочно принять за целевой главный экран продукта. Такая трактовка конфликтовала бы с уже принятым product/UX направлением: Travel Assistant должен быть chat-first, а structured results должны дополнять диалог, а не заменять его.

## 4. Clarified product direction

Целевой пользовательский поток:

1. Пользователь описывает потребность естественным языком в чате.
2. Assistant определяет intent и извлекает известные параметры.
3. При нехватке обязательных данных Assistant задает короткие уточняющие вопросы.
4. Когда данных достаточно, application orchestration вызывает hotel provider boundary.
5. Provider/API остается источником hotel facts.
6. LLM помогает интерпретировать запрос, формулировать уточнения, объяснения и сравнения, но не создает provider facts.
7. Frontend показывает чат как основной контекст и структурированные hotel offers рядом с ним или под ним.

Текущая форма Stage 7 не является альтернативным продуктовым направлением.

## 5. Stage 7 interpretation

Stage 7 остается корректно завершенной технической основой. Он:

- создал Kotlin + Ktor backend foundation;
- добавил Assistant session/message boundaries;
- добавил минимальный hotel search;
- ввел `FakeHotelOfferProvider`;
- добавил детерминированное ранжирование и `matchSummary`;
- связал строгий Assistant message format с hotel search;
- добавил минимальную frontend-оболочку для ручной проверки API.

Stage 7 не завершил:

- natural-language intent/parameter extraction;
- динамический clarification flow;
- LLM orchestration;
- целевой Assistant chat UI;
- real provider integration.

Форма Stage 7.51 временно сохраняется без изменений как diagnostic/demo shell. Она напрямую использует session/search endpoints и не считается финальной frontend/backend интеграцией.

## 6. Stage 8 implications

Рекомендуемый первый шаг:

`Stage 8.1 — LLM Orchestration Boundary and Safety Plan`

Это отдельный planning-only этап. Он должен определить:

- provider-independent `LlmClient` boundary;
- какие пользовательские и служебные данные допустимо передавать в LLM;
- форму результатов извлечения intent и hotel-search параметров;
- стратегию уточняющих вопросов;
- границу между LLM orchestration и tool/provider calls;
- fallback при ошибке, timeout или невалидном ответе LLM;
- тестирование orchestration через fake LLM без внешних вызовов;
- минимальные сценарии для первого последующего implementation-этапа.

Stage 8.1 не должен:

- добавлять LLM SDK, dependencies, API keys или secrets;
- выполнять реальные LLM calls;
- менять frontend/backend behavior;
- выбирать concrete model provider;
- проектировать отсутствующий real provider contract;
- подключать flight search, booking или payment.

Предоставленный real provider API contract должен рассматриваться отдельной задачей после его получения. Provider integration остается за абстракцией и не требует переписывать Stage 7 задним числом.

## 7. Files changed

| Файл | Уточнение |
|---|---|
| `README.md` | `app/` обозначен как временная техническая оболочка, а не целевой UI |
| `app/README.md` | Зафиксирована роль формы как diagnostic/demo shell и целевое chat-first направление |
| `services/backend/README.md` | Уточнено, что форма напрямую проверяет endpoints и обходит будущий Assistant UI |
| `docs/product/product-baseline.md` | Явно разделены целевой chat-first flow и техническая форма Stage 7 |
| `docs/architecture/architecture-baseline.md` | Уточнены роли frontend, LLM orchestration и provider boundary |
| `docs/ROADMAP.md` | Назначение Stage 8 связано с chat-first orchestration и безопасной LLM boundary |
| `docs/roadmap/roadmap.md` | Stage 8 оставлен запланированным; рекомендован planning-only Stage 8.1 |
| `docs/reviews/README.md` | Добавлена ссылка на этот отчет |
| `docs/reviews/pre-stage-8-chat-first-product-direction-review.md` | Зафиксированы результаты текущего уточнения |

## 8. Remaining notes

- Real provider API contract будет предоставлен позднее; его поля и поведение нельзя выдумывать заранее.
- Реальное LLM-подключение требует отдельной ограниченной задачи Stage 8 после принятия Stage 8.1.
- Flight search остается будущим расширением после hotel flow.
- Booking/payment не входят в текущий MVP и требуют отдельных продуктовых и технических решений.
- Текущая форма не удалена, не скрыта и не переделана в mock chat UI.

## 9. Validation

- `git diff --check` — passed.
- Targeted search по `form|форма|chat|чат|assistant|ассистент|LLM|provider|real provider|booking|generated clients` выполнен. Совпадения в коде, технических идентификаторах и исторических артефактах сохранены; активные документы не представляют форму как целевой продукт.
- `git status --short --untracked-files=all`, `git diff --stat` и `git diff --name-status` подтверждают только разрешенные документационные изменения и новый отчет.
- Проверка запрещенных путей не выявила изменений кода, тестов, OpenAPI, generated clients, manifest, Gradle/CI или `tools/openapi-conformance/**`.
- `docs/reviews/stage-7-*` не изменялись.
- Активный roadmap сохраняет `Stage 7 завершен; Stage 8 не начат`; Stage 8.1 указан только как рекомендуемая отдельная planning-only задача.
