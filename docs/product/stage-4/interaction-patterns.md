# Stage 4 — Interaction Patterns

## 1. Назначение документа

Документ фиксирует UX interaction patterns для Travel Assistant Stage 4.

Он описывает, как интерфейс должен вести себя в AI-assisted hotel search: уточнения, изменение параметров, comparison, saved results, loading, partial/no results, confidence/rationale и разделение facts от assumptions.

Документ не является production prompt, LLM orchestration design, frontend implementation или API contract.

## 2. Assistant Clarification Pattern

Ассистент задает уточняющие вопросы, когда:

- intent не определен;
- нет required field для hotel search;
- пользовательский запрос противоречив;
- optional field сформулирован как hard constraint;
- derived assumption существенно влияет на search;
- provider search был бы слишком широким или непроверяемым.

UX rules:

- задавать один priority question или короткий набор связанных вопросов;
- объяснять, зачем нужен missing field;
- показывать already understood fields рядом с вопросом;
- давать quick replies только если они реально сокращают путь;
- не запускать provider search до minimum search condition;
- если пользователь не хочет уточнять required fields, честно показать limitation and fallback.

## 3. Understood Request Pattern

Понятый запрос пользователя отображается как Search Intent Summary.

Summary должно показывать:

- intent: hotel search, compare, save, resume, unsupported/future-scope;
- destination;
- dates/date range or flexibility;
- guests count;
- rooms count or visible room assumption;
- budget/price preference if relevant;
- location/amenities/preferences;
- missing required fields;
- assumptions;
- user-provided constraints;
- unknown data.

UX rules:

- known fields, assumptions и missing fields визуально разделены;
- changed fields подсвечиваются после refinement;
- summary не должен становиться длинной формой;
- user can correct key fields without losing chat context.

## 4. Search Parameter Change Pattern

Пользователь может менять параметры через chat или structured controls.

Flow:

1. User changes dates, destination, guests, rooms, budget, location, amenities or priorities.
2. Assistant/UI identifies affected fields.
3. Summary shows what changed and what stayed.
4. Existing affected results become stale.
5. If required fields remain complete, search can rerun.
6. If required fields become incomplete, clarification resumes.

UX rules:

- stale results must not look current;
- material changes should be explicit;
- user should be able to compare old result only with warning;
- system should avoid silent reranking when user changed hard constraints.

## 5. Offer Comparison Pattern

Comparison helps user understand trade-offs across 2-5 hotel offers.

UX rules:

- comparison uses provider facts where available;
- unknown fields are marked, not filled by AI;
- assistant assumptions are separated from facts;
- comparison starts with user priorities if known;
- if priorities are unclear, ask what matters most or use transparent default;
- comparison should produce a short recommendation plus caveats.

Suggested comparison dimensions for MVP:

- price and budget fit;
- location fit;
- rating/review score if available;
- amenities/hard constraints;
- cancellation/flexibility if provider returned it;
- reason match to original request;
- unknown or partial data.

## 6. Save / Shortlist Pattern

Save keeps useful offers in current search session.

UX rules:

- user can save from card, details, comparison or chat command;
- if target is ambiguous, ask which offer to save;
- saved state is visible on card/details/saved screen;
- saved item includes context: provider facts snapshot, assumptions, unknown fields, source/freshness if available, related search parameters;
- save confirmation should mention current-session scope when relevant;
- save must not imply booking, price guarantee, availability guarantee, account storage or cross-device sync.

## 7. Return to Previous Results Pattern

MVP supports return within active search session.

UX rules:

- saved and current results remain reachable from app shell or session context;
- returning to old results should show stale/freshness markers if parameters changed or time passed;
- chat can summarize current session state;
- if session is unavailable, UI must say so without implying account-level history exists.

Post-MVP account history and cross-device resume remain future work.

## 8. AI Thinking / Searching / Clarifying Pattern

UI must distinguish AI and provider activity:

| State | Meaning | UX treatment |
|---|---|---|
| Assistant thinking | Parsing user request, extracting fields, drafting clarification. | Chat status or compact indicator. |
| Clarification needed | Required data missing or assumption needs confirmation. | Clarification card with field reason. |
| Ready to search | Required fields complete. | Summary + search started status. |
| Provider search | Fetching hotel offers from provider/API or future placeholder. | Results area loading/skeleton, not fake facts. |
| Updating results | Refinement or filter changed. | Stale markers + updating indicator. |

UX rules:

- do not show invented prices, availability or hotel facts while loading;
- avoid theatrical AI animations;
- status language should be calm and concrete.

## 9. Partial Results Pattern

Partial results occur when critical offer data exists but some fields are missing.

UX rules:

- show available facts if the offer is still useful;
- mark unknown fields inline;
- do not use missing fields as recommendation reasons;
- allow user to inspect details;
- assistant summary must mention important limitations.

Examples:

- cancellation policy unknown;
- amenities incomplete;
- review score missing;
- freshness/source marker unavailable.

## 10. No Results Pattern

No results means provider search succeeded but no hotel offers matched.

UX rules:

- distinguish no results from provider error;
- show current constraints summary;
- suggest 1-3 concrete relaxations;
- offer to broaden area, adjust budget, change dates/flexibility or relax hard amenities;
- do not invent market facts unless provider data supports them.

## 11. Provider Error Pattern

Provider error means source/API failed or is unavailable.

UX rules:

- state source problem separately from "nothing found";
- offer retry or continue refining;
- avoid technical stack details;
- if partial data exists, show it with limitations;
- do not replace provider facts with assistant guesses.

## 12. Confidence / Rationale Pattern

Travel Assistant should explain why an offer is recommended without pretending to have certainty beyond facts.

Rationale should include:

- matched user requirements;
- relevant provider facts;
- trade-offs;
- assumptions used;
- unknown or missing data;
- confidence wording appropriate to data quality.

UX rules:

- avoid opaque AI score as primary explanation;
- if confidence is shown, it should be qualitative and tied to reasons;
- do not imply guaranteed availability or price;
- recommendation rationale should be short on cards and fuller in details/comparison.

Possible labels:

- Strong match;
- Good fit with caveats;
- Budget-friendly trade-off;
- Needs review due to unknown data.

These are draft language patterns, not final copy.

## 13. Avoiding User Overload

UX should reduce overload through:

- shortlists, not endless result feeds;
- progressive disclosure;
- compact search summary;
- limited visible filters;
- one primary action per card/screen area;
- clear empty/no results recovery;
- concise assistant messages;
- comparison limited to 2-5 offers.

Avoid:

- asking all optional preferences upfront;
- mixing chat explanations and full offer data in one long text;
- showing too many status badges;
- exposing provider/LLM implementation details;
- presenting future-scope actions as available.

## 14. Facts, User Constraints and AI Assumptions Pattern

Provider facts:

- price;
- currency;
- hotel name;
- location;
- availability if returned;
- rating/review score if returned;
- amenities if returned;
- cancellation policy if returned;
- source/freshness if returned.

Assistant assumptions:

- interpretation of "cheap", "quiet", "central";
- room count default;
- budget tier;
- preference priority;
- inferred travel style.

User-provided constraints:

- constraints explicitly stated by the user;
- visa/passport or accessibility requirements supplied by the user;
- hard constraints such as "only", "must", "without";
- preference priorities confirmed by the user.

Unknown data:

- missing provider fields;
- stale data;
- unsupported facts;
- unavailable source/freshness.

UX rules:

- facts, user-provided constraints, assumptions and unknown data must have separate labels/sections;
- assumptions that affect search should be confirmable or editable;
- user-provided constraints should not be presented as provider-verified facts unless confirmed by a valid source;
- unknown data should not be hidden in fine print;
- rationale must not convert assumption into fact.

## 15. Unsupported and Future-scope Actions

MVP v1 should safely handle:

- booking/payment requests;
- flight search requests;
- combined hotel + flight requests;
- legal/visa/refund interpretation;
- long-term account history expectations.

UX rules:

- acknowledge user intent;
- state current MVP boundary;
- offer supported next step;
- do not create disabled UI that looks broken;
- do not imply future-scope feature is already implemented.

## 16. Mobile Interaction Considerations

- Keep chat input accessible.
- Avoid side-by-side comparison on narrow viewports.
- Use bottom sheet or collapsible sections for filters/details if needed.
- Preserve provider/assumption/unknown distinctions.
- Keep recovery actions visible in no results/error states.
- Avoid sticky elements that cover content or controls.

## 17. Open questions

- Should confidence labels be shown on cards, details, or only in comparison?
- What is the minimum user-facing freshness/source language supported by provider data?
- How many quick replies are useful before clarification starts feeling like a form?
- Should stale results remain visible by default or collapse behind a warning after refinement?
