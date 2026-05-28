# Stage 3.6 — Stage 3 Summary & Carryover

## 1. Назначение документа

Документ фиксирует итог Stage 3 — MVP UX / Navigation для Travel Assistant после refocus на Hotel-Only MVP v1.

Его задача — закрепить UX/product baseline для MVP v1, перечислить созданные Stage 3 документы, собрать ключевые решения и перенести оставшиеся вопросы в carryover для будущих этапов.

Документ не начинает Stage 4, не принимает архитектурные решения, не проектирует API, database schema, provider adapters или implementation details.

## 2. Stage 3 Scope

Stage 3 был сфокусирован на UX/product logic, navigation model, search flow boundaries, required fields, acceptance criteria и MVP/Post-MVP UX boundaries.

Stage 3 не был visual design этапом. В рамках Stage 3 не создавались visual style, UI kit, wireframes, high-fidelity layouts, React/Next.js/Kotlin/Ktor code, API contracts, DTO, endpoints, database tables или provider adapters.

Основной результат Stage 3: Hotel-Only MVP v1 UX baseline готов к передаче в Stage 4 Visual Design / UI Concept и будущие technical stages как продуктовая рамка, но не как техническая спецификация.

## 3. Current MVP v1 Baseline

MVP v1 = Hotel-Only MVP v1.

В MVP v1 входят:

- natural-language hotel request;
- AI clarification;
- hotel results;
- ranking / explanation;
- hotel offer details;
- save / shortlist в текущей search session;
- basic hotel comparison.

В MVP v1 не входят:

- flight search;
- combined hotel + flight search;
- package ranking;
- bundle optimization;
- dynamic packaging;
- full trip planning;
- booking/payment;
- long-term account history;
- cross-device resume.

Flight search перенесен в next expansion после hotel flow.

Combined hotel + flight search перенесен в later expansion после появления flight flow.

## 4. Stage 3 Documents

| Документ | Роль |
|---|---|
| `docs/product/stage-3/screen-map.md` | Stage 3.1: screen map, navigation model, UX zones, screen states и MVP/Post-MVP split для hotel-only flow. |
| `docs/product/stage-3/required-fields-and-acceptance-criteria.md` | Stage 3.2: required/optional/derived fields, missing data behaviour и acceptance criteria для hotel search flow. |
| `docs/product/stage-3/mvp-search-flow-details.md` | Stage 3.3: подробный hotel search lifecycle, clarification, refinement, save/shortlist, comparison и recovery states. |
| `docs/product/stage-3/combined-search-ux-decision.md` | Stage 3.4: historical combined decision; superseded для MVP v1, источник границы исключения combined search из MVP v1. |
| `docs/product/stage-3/stage-3-hotel-only-consistency-review.md` | Stage 3.5: consistency review после hotel-only refocus; verdict: Passed with minor notes. |
| `docs/product/stage-3/stage-3-summary-and-carryover.md` | Stage 3.6: итоговый UX baseline и carryover для будущих этапов. |
| `docs/product/stage-3/stage-3-plan-reconciliation.md` | Stage 3.7: completion audit; подтверждает, что обязательные Stage 3 work items закрыты или корректно перенесены. |

## 5. Key UX/Product Decisions

- AI chat является главным entry point MVP v1.
- Первый экран должен сразу вести к рабочему hotel request, а не быть marketing landing page.
- Structured hotel results должны отображаться отдельно от chat-текста: рядом с chat или под ним в зависимости от viewport.
- Search session является основной UX-единицей MVP: в ней живут known fields, assumptions, results, saved items, comparison candidates и stale markers.
- Hotel search запускается только после минимально достаточных required fields: destination, dates/date range, guests count и rooms count или видимая room assumption.
- Assistant assumptions должны быть видимыми пользователю, если они влияют на search, ranking или explanation.
- Provider facts, assistant assumptions и unknown data должны быть разделены в results, offer details, comparison и saved items.
- No results, provider error, partial result и stale data являются разными UX states.
- Save / shortlist ограничен текущей search session и не обещает booking, payment, price guarantee, availability guarantee или account-level storage.
- Flight-only и combined intents в MVP v1 получают future-scope fallback, а не search flow.
- Combined search decision superseded для MVP v1: limited Level 3 coordinated combined search не является active MVP v1 requirement.

## 6. Hotel-Only MVP UX Baseline

MVP v1 user flow:

1. Пользователь вводит natural-language hotel request.
2. Ассистент определяет hotel intent или уточняет ambiguous intent.
3. Ассистент извлекает known fields и показывает assumptions, если они влияют на поиск.
4. Если required fields отсутствуют, ассистент задает минимально достаточное уточнение.
5. Когда required fields достаточны, запускается hotel provider search.
6. Пользователь видит hotel results как structured hotel cards и краткое объяснение в chat.
7. Пользователь открывает hotel offer details, сравнивает 2-5 offers, сохраняет offer/selection или уточняет параметры.
8. При изменении constraints affected hotel results помечаются stale.
9. No results, partial data и provider error не смешиваются между собой.

MVP v1 UX baseline должен использоваться как вход для Stage 4, Architecture, API/provider contract и Implementation stages, если отдельное product decision не изменит scope.

## 7. Source of Truth

- Primary roadmap `docs/roadmap/roadmap.md` остается source of truth для текущего статуса этапов, next step, stage gates и roadmap progress.
- Этот Stage 3 summary является source of truth для Hotel-Only MVP v1 UX baseline.
- Stage 3.1-3.3 документы являются детальными источниками для screen map, required fields, acceptance criteria и search flow behaviour.
- `docs/product/stage-3/combined-search-ux-decision.md` является source of truth для исключения combined search из MVP v1 и переноса limited Level 3 coordinated combined search в future scope.
- Stage 0/1/2 документы остаются useful historical traceability, но могут содержать superseded broader scope; их нельзя читать как active MVP v1 requirements, если они конфликтуют с Hotel-Only MVP v1 baseline.
- Architecture/API/DB/provider details должны фиксироваться только на соответствующих будущих этапах roadmap.

## 8. Carryover Items

### 8.1 Carryover to Stage 4 — Visual Design / UI Concept

- Visual layout for chat + hotel results.
- Hotel card visual hierarchy.
- Hotel details screen visual structure.
- Save / shortlist visual affordance.
- Empty/loading/error visual states.
- No results, partial data and stale data visual treatment.
- Responsive web-first layout.
- Visual treatment for provider facts, assistant assumptions and unknown data.
- Comparison view layout for 2-5 hotel offers.

Stage 4 должен использовать Stage 3 документы как UX baseline и не менять product flow decisions без отдельного product review.

### 8.2 Carryover to Architecture Stage

- Границы assistant orchestration.
- Session model на уровне системы.
- Separation of provider facts, assistant assumptions и unknown data.
- Source/freshness representation на уровне доменной модели.
- Stale marker model for changed hotel search constraints.
- Future extensibility for flight/combined expansion без включения их в MVP v1.
- Responsibility split между chat, application/domain use cases, provider access и ranking/explanation logic.

Architecture stage не должен возвращать flight/combined в MVP v1 без отдельного product decision.

### 8.3 Carryover to API / Provider Contract Stage

- Existing travel API hotel offer contract.
- Required hotel offer fields.
- Freshness/source markers.
- Ranking inputs available from provider.
- Error/no results/partial results provider behaviour.
- Availability and price confidence semantics.
- Mapping provider facts without leaking provider DTO into product/domain model.
- Contract limitations that affect open destination discovery.

Этот carryover не является API contract. Он перечисляет вопросы, которые нужно сверить, когда existing travel API contract будет предоставлен.

### 8.4 Carryover to Implementation Stage

- Hotel-only search session.
- Clarification state.
- Hotel results list.
- Hotel details.
- Save/shortlist within session.
- Basic comparison.
- Stale handling after refinement.
- Unsupported action fallback for booking/payment/legal requests.
- UX acceptance criteria from Stage 3.2 and Stage 3.3.
- Tests or validation scenarios for no results, provider error, partial data, unknown data and stale data.

Implementation должна опираться на Hotel-Only MVP v1, если не будет отдельного product decision.

### 8.5 Carryover to Future Expansions

- Flight search.
- Combined hotel + flight search.
- Level 3 coordinated combined search.
- Package ranking.
- Bundle optimization.
- Dynamic packaging.
- Long-term trip planning.
- Authorization-based persistence.
- Resume across devices.
- Account-level saved trips/history.
- Cross-provider ranking beyond hotel-only offers.

Эти items не входят в MVP v1 и не должны появляться в implementation как неявное расширение scope.

## 9. Open Questions

- Какой объем provider-backed open destination discovery нужен в MVP v1, если он применим к hotel search?
- Когда и в каком виде будет предоставлен existing travel API hotel offer contract?
- Какие required hotel offer fields будут доступны из provider/API?
- Какие freshness/source markers будут доступны из provider/API?
- Какие ranking inputs можно получить как provider facts, а какие останутся assistant assumptions?
- Какой минимальный session persistence уровень нужен без авторизации?
- Нужен ли отдельный MVP/Post-MVP split для resume и authorization до Stage 4 или его достаточно перенести в Architecture/Implementation preparation?
- Как показывать user-provided visa/passport constraints без перехода к legal advice?

## 10. Explicit Non-Goals for MVP v1

- Flight search.
- Flight cards.
- Flight provider behaviour.
- Flight booking.
- Combined hotel + flight search.
- Level 3 coordinated combined search.
- Full package ranking.
- Package-level offer model.
- Bundle optimization.
- Dynamic packaging.
- End-to-end trip package.
- Complete travel itinerary.
- Full trip planning.
- Transport search.
- Booking/payment/ticketing/refunds.
- Production-hardening provider adapter behaviour.
- API/DB/provider implementation details inside product UX docs.
- Long-term account history and cross-device resume.

## 11. Risks Controlled by Stage 3

- MVP scope creep into flight/combined search.
- Confusing hotel-only MVP with full travel planning.
- Treating historical Stage 0/1/2 flight/combined text as active MVP v1 requirement.
- Starting visual design before UX baseline is stable.
- Starting architecture/API/DB/provider design from UX documents.
- Mixing provider facts with assistant assumptions.
- Treating no results, provider error, partial data and stale data as one generic error.
- Promising booking, payment, price guarantee or availability guarantee.

## 12. Stage 3 Completion Checklist

- [x] Screen map completed.
- [x] Required fields and acceptance criteria completed.
- [x] MVP search flow details completed.
- [x] Combined search decision completed.
- [x] Hotel-only consistency review completed.
- [x] Summary and carryover completed.
- [x] No critical/major findings remain.
- [x] Stage 4 not started.
- [x] Architecture/API/implementation not started.

## 13. Recommendations

- Закрыть Stage 3 в primary roadmap после создания этого summary.
- Использовать `docs/product/stage-3/stage-3-plan-reconciliation.md` как completion audit перед выбором следующей задачи.
- Следующий work item выбирать отдельной задачей: Stage 4 Visual Design / UI Concept или дополнительный product cleanup, если он будет явно нужен.
- На Stage 4 использовать Stage 3 docs as UX baseline and avoid changing MVP scope.
- На Architecture/API/Implementation stages сохранить Hotel-Only MVP v1 как active scope.
- Не возвращать flight/combined/package capabilities в MVP v1 без отдельного product decision.

## 14. Что не входит в этот шаг

- Начало Stage 4.
- Visual design, wireframes, UI kit или mockups.
- React/Next.js/Kotlin/Ktor code.
- API contracts, DTO, endpoints или OpenAPI.
- Database schema или database tables.
- Architecture decisions или ADR.
- Provider adapter design.
- Ranking algorithm design.
- Реальные integrations.
- Переписывание Stage 0/1/2.
- Удаление historical context.
- Расширение MVP v1.
