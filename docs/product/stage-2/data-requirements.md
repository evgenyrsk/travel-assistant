# Stage 2 — Data Requirements

## Цель документа

Описать продуктовые требования к данным без database schema, API schema, DTO, OpenAPI или provider adapter implementation.

## Принцип источника данных

- Provider/API data является primary source of truth для цен, availability, расписаний, параметров offers, ограничений и freshness.
- LLM/assistant может интерпретировать, объяснять, сравнивать и структурировать provider data.
- LLM/assistant не должен выдумывать provider facts.
- Если provider data отсутствует, неполная или устаревшая, это должно быть явно отмечено как unknown data или provider limitation.
- Assistant assumptions должны храниться и отображаться отдельно от provider facts.
- Финальный MVP должен использовать предоставленный API-контракт для получения реальных travel offers.
- Stage 2 не проектирует API-контракт.

## Data groups

| Data group | Required for MVP | Why needed | Related use cases | Notes |
|---|---|---|---|---|
| Search request data | yes | Позволяет определить intent, понять required fields, уточнить пробелы и выполнить поиск. | UC-01, UC-02, UC-03, UC-04, UC-11, UC-12 | Не является API request schema. |
| Hotel offer data | yes | Нужно для hotel search, ranking, comparison и explanation. | UC-01, UC-05, UC-06, UC-13, UC-15 | Финальный MVP получает реальные hotel offers из предоставленного travel API. |
| Flight offer data | yes | Нужно для flight search, ranking, comparison и explanation. | UC-02, UC-05, UC-06, UC-13, UC-15 | Финальный MVP получает реальные flight offers из предоставленного travel API. |
| Combined search data | open | Нужно для Level 2/3 combined behaviour; объем зависит от Stage 3 решения. | UC-03, UC-11, UC-12 | Level 1/2 In MVP; Level 3 Open; Level 4 Post-MVP/Open. |
| Search session data | yes | Нужно для уточнений, сохранения, сравнения, resume и changes during search. | UC-04, UC-07, UC-08, UC-12 | Не задает storage model. |
| Provider/API data handling | yes | Нужно для anti-hallucination, explainability, unknown handling и real offers в MVP. | UC-06, UC-09, UC-13, UC-15 | API contract остается open input для future technical stages. |

## Search request data

**Required for MVP:** yes.

**Fields:**
- origin;
- destination;
- dates;
- duration;
- guests/passengers;
- budget;
- preferences;
- constraints;
- flexibility;
- intent type.

**Why needed:** эти данные определяют, можно ли искать, что уточнять и какие offers релевантны.

**Related use cases:** UC-01, UC-02, UC-03, UC-04, UC-10, UC-11, UC-12.

**Notes:** обязательность отдельных полей зависит от intent и должна быть финализирована на Stage 3.

## Hotel offer data

**Required for MVP:** yes.

**Fields:**
- hotel name;
- location;
- price;
- currency;
- dates;
- rating;
- review score;
- amenities;
- cancellation policy availability;
- source/provider;
- data freshness;
- confidence/unknown fields.

**Why needed:** hotel offers должны быть ранжируемыми, сравнимыми и объяснимыми.

**Related use cases:** UC-01, UC-05, UC-06, UC-13, UC-15.

**Notes:** missing rating, cancellation policy или amenities должны быть unknown data, а не assistant assumptions.

## Flight offer data

**Required for MVP:** yes.

**Fields:**
- airline;
- origin;
- destination;
- departure/arrival time;
- duration;
- stops;
- baggage availability;
- price;
- currency;
- source/provider;
- data freshness;
- confidence/unknown fields.

**Why needed:** flight offers должны поддерживать базовое сравнение цены, времени, пересадок и удобства.

**Related use cases:** UC-02, UC-05, UC-06, UC-13, UC-15.

**Notes:** baggage availability и freshness нельзя додумывать, если provider их не вернул.

## Combined search data

**Required for MVP:** open.

**Fields:**
- shared dates;
- shared destination;
- total budget;
- hotel budget part;
- flight budget part;
- package assumptions;
- unresolved constraints.

**Why needed:** эти данные нужны для Level 2 same-dialog assistance и возможного Level 3 coordinated combined search.

**Related use cases:** UC-03, UC-11, UC-12.

**Notes:** budget split может быть assistant assumption, если provider/API не подтверждает package price.

## Search session data

**Required for MVP:** yes.

**Fields:**
- current user goal;
- extracted parameters;
- missing required parameters;
- user preferences;
- selected offers;
- rejected offers;
- comparison candidates;
- assistant assumptions;
- provider facts;
- unknown fields.

**Why needed:** search session поддерживает clarification, resume, save, compare и изменение constraints.

**Related use cases:** UC-04, UC-05, UC-07, UC-08, UC-12.

**Notes:** долгосрочная история и account-level storage остаются Post-MVP/Open.

## Provider/API data handling

**Required for MVP:** yes.

**Provider facts:** данные, полученные из provider/API или другого утвержденного источника travel offers: prices, availability, schedules, hotel attributes, flight attributes, baggage, restrictions, source, freshness.

**Assistant assumptions:** интерпретации ассистента, которые не подтверждены provider facts: предполагаемый budget tier, guessed priority, tentative budget split, likely conflict before provider verification.

**Unknown data:** данные, которых нет, которые неполны, устарели, противоречивы или не имеют freshness/source confirmation.

**Why needed:** это основа anti-hallucination, explainability и доверия к real provider/API data.

**Related use cases:** UC-06, UC-09, UC-10, UC-13, UC-15.

**Notes:** если API-контракт еще не предоставлен, это фиксируется как open input для future technical stages, а не как перенос real API integration в Post-MVP.
