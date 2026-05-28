# Stage 4 — Visual Design Direction

## 1. Назначение документа

Stage 4 фиксирует направление визуального дизайна и UX-системы Travel Assistant для будущей frontend-реализации.

Документ переводит Stage 3 Hotel-Only MVP v1 UX baseline в визуальные принципы: каким должен ощущаться интерфейс, как балансировать chat-first взаимодействие и structured hotel results, какие визуальные решения поддерживают доверие к AI и как не смешивать provider facts, assistant assumptions и unknown data.

Документ не является high-fidelity макетом, UI kit, дизайн-токенами, React/Next.js implementation, Tailwind config, shadcn/ui customization, API-контрактом или архитектурным решением.

## 2. Источники и ограничения

Основные источники:

- `README.md`;
- `docs/product/README.md`;
- `docs/roadmap/roadmap.md`;
- `docs/product/stage-0/`;
- `docs/product/stage-1/`;
- `docs/product/stage-2/`;
- `docs/product/stage-3/screen-map.md`;
- `docs/product/stage-3/required-fields-and-acceptance-criteria.md`;
- `docs/product/stage-3/mvp-search-flow-details.md`;
- `docs/product/stage-3/stage-3-summary-and-carryover.md`;
- `docs/product/stage-3/stage-3-plan-reconciliation.md`.

Ограничения:

- Актуальный MVP v1 ограничен hotel search.
- Flight search является next expansion после hotel flow.
- Combined hotel + flight search является later expansion после flight flow.
- Stage 4 не меняет Stage 3 UX flow, required fields, acceptance criteria или MVP boundaries.
- Stage 4 не начинает Stage 5 Technical Architecture и не проектирует API/provider contracts.
- Booking, payment, price guarantee, package ranking, account-level history и cross-device resume не входят в MVP v1.

## 3. Цель Stage 4

Цель Stage 4 — создать продуктовую основу визуального дизайна, достаточную для будущей frontend-декомпозиции:

- определить дизайн-направление Travel Assistant;
- описать foundational design system без финальной реализации;
- сформировать inventory MVP UI components;
- описать ключевые экраны и состояния;
- зафиксировать interaction patterns для AI-assisted hotel search;
- собрать carryover для Stage 5 и будущих implementation stages.

Stage 4 должен ответить на вопрос: как интерфейс должен поддерживать доверительное AI-assisted планирование поездки, не превращаясь ни в обычную форму поиска, ни в непрозрачный чат с длинным текстом.

## 4. Роль визуального дизайна в Travel Assistant

Визуальный дизайн Travel Assistant должен помогать пользователю:

- быстро понять, что можно начать с естественного запроса;
- видеть, какие параметры ассистент понял;
- отвечать на уточнения без ощущения длинной анкеты;
- отличать найденные hotel offers от объяснений ассистента;
- понимать, какие данные являются provider facts, assumptions или unknown;
- сравнивать варианты по trade-offs, а не только по цене;
- сохранять полезные варианты в текущей search session;
- замечать stale, partial, no results и provider error states без паники и ложной уверенности.

Главная UX-задача визуального дизайна — сделать AI-помощника понятным и проверяемым. Интерфейс должен показывать ход рассуждения на уровне результата и статуса, но не раскрывать внутренние prompt/chain details.

## 5. Дизайн-принципы продукта

### 5.1 Trust through clarity

Пользователь должен видеть, что известно точно, что принято как assumption и чего система не знает. Provider facts, assistant assumptions и unknown data должны получать разные визуальные роли.

### 5.2 Chat-first, not chat-only

Chat является главным entry point и постоянным способом управления поиском, но structured results должны отображаться отдельно от длинного chat-текста.

### 5.3 Calm decision support

Интерфейс должен снижать когнитивную нагрузку: короткий список, ясная иерархия, объяснимые trade-offs, ограниченное число primary actions.

### 5.4 Progressive disclosure

Пользователь сначала видит summary, ключевые причины и действия. Детали, unknown fields, source/freshness и comparison раскрываются по необходимости.

### 5.5 Human control over AI

Ассистент предлагает, уточняет и объясняет, но пользователь должен легко менять параметры, подтверждать assumptions, сохранять или отклонять варианты.

### 5.6 Travel context without decoration overload

Travel-oriented ощущение достигается через контент, локации, карты/фото в будущем, понятные иконки и спокойную визуальную атмосферу, а не через шумные декоративные мотивы.

### 5.7 MVP scope visibility

Интерфейс не должен обещать flight search, combined package, booking или long-term account history в MVP v1. Unsupported и future-scope действия получают ясный fallback.

## 6. Желаемое ощущение от интерфейса

Интерфейс должен ощущаться:

- современным, но не экспериментальным ради эксперимента;
- чистым и спокойным;
- доверительным, особенно в местах с ценой, availability, source и unknown data;
- travel-oriented, но не похожим на рекламный лендинг;
- AI-assisted, но не магическим и непрозрачным;
- практичным для повторной работы с результатами;
- достаточно плотным на desktop, чтобы удобно сравнивать предложения;
- аккуратно адаптированным к mobile, чтобы chat и offers не конкурировали за внимание.

Нежелательное ощущение:

- маркетинговый hero вместо рабочего продукта;
- длинный чат без структурированных карточек;
- перегруженный travel marketplace с десятками фильтров;
- игровая или чрезмерно яркая AI-эстетика;
- визуальные обещания booking/payment/package, которых нет в MVP v1.

## 7. Visual Style Direction

Рабочее направление:

- **Clean travel workspace:** интерфейс как рабочее пространство выбора поездки, а не promotional landing.
- **Soft professional palette:** светлый базовый фон, нейтральные поверхности, спокойные акценты, умеренная travel-свежесть.
- **Readable cards:** hotel offers должны быть легко сканируемыми по названию, локации, цене, рейтингу, ключевым reasons и limitations.
- **AI status as utility:** thinking/searching/clarifying states показываются как сервисные состояния, без театральной анимации.
- **Evidence-first details:** details и comparison сначала показывают проверяемые факты и соответствие запросу, затем assumptions и неизвестные поля.

## 8. UI-ориентиры без копирования продуктов

Допустимые ориентиры на уровне качеств:

- clear productivity dashboards для плотности и сканируемости;
- modern travel search interfaces для карточек офферов и фильтрации;
- conversational assistants для chat rhythm и уточнений;
- decision-support tools для comparison и rationale.

Нельзя копировать конкретный продукт, визуальный язык, layout, карточки, iconography или брендинг. Stage 4 фиксирует собственные принципы Travel Assistant.

## 9. Баланс Chat и Results

### 9.1 Chat responsibilities

Chat должен отвечать за:

- natural-language entry;
- уточнения;
- подтверждение assumptions;
- summary найденных результатов;
- объяснение trade-offs;
- команды save, compare, refine и resume;
- unsupported/future-scope fallback.

### 9.2 Results responsibilities

Results area должна отвечать за:

- структурированный список hotel offers;
- визуальную иерархию offer facts;
- быстрые действия: details, save, compare;
- markers для unknown, partial, stale и source/freshness;
- filters/sort в MVP-ограниченном виде;
- no results, loading и provider error states.

### 9.3 Combined chat/results composition

Desktop-first композиция должна поддерживать одновременное чтение chat context и hotel results. Mobile-aware композиция должна давать последовательный фокус: chat, summary, results, details через stacked layout, tabs или bottom-sheet patterns на будущей реализации.

## 10. Desktop-first / Mobile-aware подход

Stage 3 задает web-first MVP. Для Stage 4 это означает:

- основная рабочая композиция проектируется для desktop/tablet web;
- desktop должен поддерживать side-by-side chat + results, если viewport позволяет;
- mobile должен оставаться first-class адаптацией, но не отдельным mobile app scope;
- mobile UI не должен требовать горизонтального сравнения сложных таблиц;
- actions должны быть достижимы одной рукой в mobile layout, если в будущем используется bottom sheet;
- компоненты должны быть описаны так, чтобы их можно было адаптировать к будущим iOS/Android интерфейсам без изменения продуктовой логики.

## 11. Что входит в Stage 4

В Stage 4 входит:

- visual design direction;
- design system foundations;
- MVP component inventory;
- screen-level specifications;
- interaction patterns;
- visual treatment для provider facts, assistant assumptions, user-provided constraints, unknown, partial, stale, no results и provider error;
- рекомендации для будущей frontend-декомпозиции без реализации.

## 12. Что не входит в Stage 4

В Stage 4 не входит:

- production frontend/backend code;
- React/Vue/Next.js components;
- Tailwind/shadcn/ui configuration;
- final design tokens или утвержденные HEX как обязательный контракт;
- pixel-perfect mockups;
- интерактивный prototype;
- API contracts, DTO, OpenAPI, database schema;
- architecture decisions или ADR;
- real provider/LLM integrations;
- flight search или combined search как active MVP v1 UI;
- booking/payment flow;
- account-level saved trips/history.

## 13. Принятые Stage 4 visual decisions

- MVP v1 visual system строится вокруг Hotel-Only search flow.
- Chat является главным entry point, но не единственным носителем результата.
- Hotel results должны быть structured, card-based и scan-friendly.
- Visual system должна явно различать provider facts, assistant assumptions, user-provided constraints и unknown data.
- Error/no results/partial/stale states получают отдельные визуальные treatment.
- Flight и combined UI описываются только как future expansion references, если упоминаются в inventory или screen specs.

## 14. Open questions

- Нужны ли реальные hotel imagery/photos в MVP v1 или карточки должны начинаться с текстово-фактического представления до стабилизации provider data?
- Какие source/freshness markers фактически вернет existing travel API?
- Какой уровень session persistence будет доступен без авторизации и как это повлияет на saved UI?
- Нужен ли отдельный visual language для confidence/rationale или достаточно badges/sections в карточках и details?
- Какие accessibility requirements будут обязательны для первого web frontend milestone?
