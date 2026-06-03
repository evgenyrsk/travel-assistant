# Product Baseline

## 1. Назначение документа

Этот документ фиксирует актуальный product baseline Travel Assistant после завершения Stage 0-5 и последующей статусной синхронизации через Stage 7.0c.

Он нужен как компактная точка входа в текущее продуктовое состояние: что входит в MVP v1, что остается за его пределами, какие продуктовые границы уже подтверждены и где искать исходные stage artifacts.

Документ не заменяет historical stage artifacts в `docs/product/stage-*`, не переписывает требования и не добавляет новые product decisions. Если нужен текущий статус этапов, source of truth остается `docs/roadmap/roadmap.md`.

## 2. Текущий статус продукта

- Stage 0 - Completed.
- Stage 1 - Completed.
- Stage 2 - Completed.
- Stage 3 - Completed.
- Stage 4 - Completed.
- Stage 4.1 - Completed как `docs/product/stage-4/stage-4-consistency-review.md`.
- Stage 5 - Completed.
- Stage 6 - Completed как contract/design phase.
- Stage 7 - In progress; corrective stabilization completed through restart readiness review.

Stage 6 contract work завершен отдельными roadmap-задачами и не создавал production implementation. Stage 7 временно блокировался из-за backend stack drift, затем Java/Spring Boot skeleton drift был исправлен на минимальный Kotlin + Ktor backend skeleton. Stage 7 restart readiness review прошел с minor notes. Дальнейшая Stage 7 implementation работа, включая Stage 7.2, может продолжаться только через отдельную явную roadmap-aligned задачу.

## 3. Scope MVP v1

MVP v1 - это hotel-only travel assistant.

Актуальный MVP включает:

- AI-assisted hotel search and selection;
- естественный пользовательский запрос на подбор отеля;
- уточнение недостающих параметров перед поиском;
- provider-backed hotel facts, когда provider layer возвращает данные;
- ранжирование, объяснение и базовое сравнение hotel options;
- hotel details;
- save / shortlist в рамках текущей search session;
- явное разделение provider facts, user-provided constraints, assistant assumptions и unknown data.

MVP v1 не является полным планировщиком поездки, booking flow или аккаунтной историей путешествий.

## 4. Явно вне scope MVP v1

В MVP v1 явно не входят:

- flights;
- combined itinerary;
- combined hotel + flight search;
- booking;
- payment;
- account history;
- loyalty/profile system;
- account-level saved trips;
- cross-device sync как обязательная функция;
- production integrations за пределами явно запланированных provider abstractions;
- любой future scope, который не активирован отдельной задачей roadmap.

Flight search остается следующим расширением после hotel flow. Combined itinerary возвращается только после появления flight flow и отдельного product decision.

## 5. Пользовательский и бизнес-контекст

Travel Assistant помогает пользователю перейти от естественного, неполного или противоречивого запроса к понятному выбору отеля.

Основные пользователи MVP-контекста:

- casual travellers, которым нужно быстрее выбрать отель;
- budget travellers, которым важны цена и trade-offs;
- базовые business travellers, которым важны удобство, локация и понятные ограничения;
- пары, семьи и небольшие группы, которым нужно согласовать требования.

Проблема продукта: обычный поиск требует заранее заполнить форму и знать точные фильтры, а реальный запрос часто начинается словами вроде "недорого у моря", "удобно с ребенком", "ближе к центру" или "без переплаты".

AI важен не как источник фактов, а как слой понимания: он помогает распознать intent, уточнить параметры, объяснить trade-offs, сравнить варианты и сохранить прозрачность reasoning.

## 6. Основной продуктовый flow

Основной MVP flow:

1. Пользователь формулирует hotel request на естественном языке.
2. Ассистент определяет intent и извлекает известные параметры.
3. Если не хватает required fields, ассистент задает короткое уточнение.
4. Когда данных достаточно, application/provider layer получает hotel facts.
5. LLM помогает интерпретировать запрос, объяснять результат, сравнивать варианты и резюмировать выбор.
6. Пользователь видит hotel options, rationale, ограничения и unknown data.
7. Пользователь может уточнить параметры, сравнить 2-5 вариантов или сохранить вариант в current-session shortlist.

Assumptions и unknowns должны оставаться видимыми. Provider facts нельзя заменять уверенными догадками ассистента.

## 7. Продуктовые границы и guardrails

Ключевые границы продукта:

- LLM не создает provider facts.
- Provider facts должны приходить от provider layer/source data.
- Assistant assumptions должны быть явно обозначены.
- Unknown data не нужно превращать в уверенные факты.
- Future features не являются текущим scope.
- Current-session shortlist не является account history.
- Save/shortlist не означает booking, payment, price guarantee или availability guarantee.

Подробные roadmap guardrails остаются в `docs/roadmap/roadmap.md` и `docs/guides/documentation-style-guide.md`.

## 8. Связь со stage artifacts

Stage 0-4.1 documents сохраняются как historical stage artifacts и audit trail. Они важны для понимания эволюции продукта, но не все их ранние формулировки являются active MVP v1 baseline.

Если старый исследовательский контекст шире текущего MVP boundary, приоритет имеют:

1. явный запрос текущей задачи;
2. `docs/roadmap/roadmap.md` для статусов и progression;
3. этот product baseline для compact product state;
4. Stage 3/4 summary and carryover documents для UX/product details;
5. historical stage artifacts для traceability.

Ключевые product artifacts:

- `docs/product/README.md` - индекс product-документов.
- `docs/product/stage-0/product-framing.md` - исходная продуктовая рамка.
- `docs/product/stage-0/mvp-boundaries.md` - ранняя рамка MVP, сохраненная как historical traceability.
- `docs/product/stage-1/stage-1-summary.md` - итог Stage 1 по аудитории, scenarios, requirements и open questions.
- `docs/product/stage-2/stage-2-summary.md` - итог Stage 2 по use cases, edge cases, behaviour rules и data requirements.
- `docs/product/stage-2/assistant-behaviour-rules.md` - правила поведения ассистента на продуктовом уровне.
- `docs/product/stage-2/data-requirements.md` - продуктовые требования к данным без API/DB schema.
- `docs/product/stage-3/screen-map.md` - screen map и navigation model для hotel-only MVP v1.
- `docs/product/stage-3/required-fields-and-acceptance-criteria.md` - required fields и acceptance criteria для hotel search flow.
- `docs/product/stage-3/mvp-search-flow-details.md` - подробный hotel search flow.
- `docs/product/stage-3/stage-3-summary-and-carryover.md` - основной UX/product baseline Stage 3.
- `docs/product/stage-3/stage-3-hotel-only-consistency-review.md` - review hotel-only refocus.
- `docs/product/stage-4/visual-design-direction.md` - visual/UX direction для Stage 4.
- `docs/product/stage-4/interaction-patterns.md` - interaction patterns для AI-assisted hotel search.
- `docs/product/stage-4/stage-4-summary-and-carryover.md` - итог Stage 4.
- `docs/product/stage-4/stage-4-consistency-review.md` - Stage 4.1 consistency review.

## 9. Открытые продуктовые вопросы / carryover

Актуальные open questions и carryover уже зафиксированы в roadmap и stage summaries. Этот раздел не добавляет новые вопросы.

Ключевые темы carryover:

- какой объем provider-backed open destination discovery нужен в MVP v1, если он применим к hotel search;
- когда и в каком виде будет предоставлен existing travel API hotel offer contract;
- какие hotel offer fields, source/freshness markers и ranking inputs будут доступны как provider facts;
- какой минимальный уровень session persistence нужен без account history;
- как показывать unknown data, partial provider data и stale results в будущей реализации;
- какие accessibility gates нужны перед frontend implementation.

Carryover не является active backlog. Любой следующий шаг должен быть выбран отдельной задачей и оставаться согласованным с primary roadmap.

## 10. Связанные документы

- `docs/roadmap/roadmap.md` - primary roadmap и source of truth по статусам этапов.
- `docs/product/README.md` - индекс product-документов.
- `docs/guides/documentation-style-guide.md` - правила языка, структуры и безопасного documentation refactoring.
- `docs/reviews/documentation-refactoring-plan.md` - план controlled documentation refactoring.
- `docs/product/stage-0/product-framing.md` - исходная продуктовая рамка.
- `docs/product/stage-0/mvp-boundaries.md` - historical MVP boundary artifact.
- `docs/product/stage-1/stage-1-summary.md` - Stage 1 summary.
- `docs/product/stage-2/stage-2-summary.md` - Stage 2 summary.
- `docs/product/stage-3/stage-3-summary-and-carryover.md` - Hotel-Only MVP v1 UX baseline.
- `docs/product/stage-4/stage-4-summary-and-carryover.md` - Stage 4 visual/UX summary.
- `docs/product/stage-4/stage-4-consistency-review.md` - Stage 4.1 consistency review.
