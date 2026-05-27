# Stage 3.3 — MVP Search Flow Details

## 1. Назначение документа

Документ описывает MVP search flows Travel Assistant на уровне UX/product logic: от первого естественного запроса до уточнений, запуска поиска, отображения результатов, refinement, save/shortlist и восстановления текущей search session.

Документ не является визуальным дизайном, wireframe, API-контрактом, DTO schema, database schema, технической архитектурой, LLM system prompt или production prompt chain.

## 2. Источники и ограничения

Основные источники:

- `README.md`;
- `docs/product/README.md`;
- `docs/roadmap/roadmap.md`;
- `docs/ROADMAP.md`;
- `docs/product/stage-0/`;
- `docs/product/stage-1/`;
- `docs/product/stage-2/`;
- `docs/product/stage-3/screen-map.md`;
- `docs/product/stage-3/required-fields-and-acceptance-criteria.md`.

Ограничения:

- Stage 3 остается этапом MVP UX / Navigation и не начинает Stage 4 Visual Design / UI Concept.
- Provider/API data является primary source of truth для цен, availability, расписаний, параметров offers, ограничений и freshness.
- Assistant assumptions и unknown data должны быть отделены от provider facts.
- Финальный MVP должен использовать предоставленный контракт существующего travel API, но этот документ не проектирует этот контракт.
- Booking, payment, price guarantee, legal/visa advice, long-term account history и cross-device sync не входят в MVP.
- Save / shortlist в MVP действует только в рамках текущей search session.

## 3. Общая модель search session

**Search session** — текущий пользовательский контекст поиска поездки, в котором ассистент хранит product-level состояние: intent, known fields, missing required fields, user preferences, constraints, assumptions, provider facts, unknown data, current results, saved items, comparison candidates и stale markers.

Search session начинается, когда пользователь:

1. отправляет новый travel request в chat;
2. выбирает продолжить текущий незавершенный search;
3. возвращается к saved/shortlisted item в пределах текущей session.

Search session завершается, когда пользователь:

1. явно начинает новый независимый travel request;
2. закрывает или теряет текущий session context;
3. выбирает завершить поиск без дальнейшего refinement;
4. переходит к unsupported действию, которое MVP не выполняет, например booking/payment, и не выбирает supported next step.

В MVP search session не равна долгосрочной истории аккаунта. Она может поддерживать текущий диалог, saved offers, comparison set и stale warnings, но не обещает восстановление прошлых поездок после завершения session.

Минимальное состояние session:

| State group | Что хранится на product-level |
|---|---|
| Intent state | current intent, previous intent, ambiguity, unsupported action marker |
| Request fields | origin, destination, dates, duration, guests/passengers, rooms, budget, preferences, hard constraints |
| Data confidence | provider facts, assistant assumptions, unknown fields, freshness/source markers |
| Flow state | missing required fields, ready to search, loading, results, no results, partial, provider error |
| User actions | selected offers, saved items, comparison candidates, rejected offers, refinements |

## 4. Intent Detection

Ассистент сначала определяет intent, потому что от него зависят required fields, допустимые уточнения, тип provider search и формат результатов.

Intent detection опирается на:

- явные слова пользователя: "отель", "рейс", "билет", "перелет", "поездка", "куда-нибудь";
- сущности: origin, destination, dates, duration, travelers, budget;
- действия: find/search, compare, save, refine, resume;
- контекст текущей search session;
- наличие hotel-specific или flight-specific preferences.

Если intent меняется по ходу диалога, ассистент не начинает новую session молча. Он сообщает, что меняется фокус, показывает сохраненные shared fields и уточняет, нужно ли заменить текущий search или добавить новый search part в эту же session.

### 4.1 Hotel-only intent

Hotel-only intent определяется, когда пользователь просит проживание, отель, апартаменты или место остановиться без явного запроса на перелет.

Примеры:

- "Найди отель в Барселоне на 10-14 июня для двоих."
- "Нужен спокойный отель у моря с завтраком."
- "Подбери жилье в центре Рима."

Если в текущей combined session уже есть flight context, hotel-only запрос может быть refinement hotel part, а не новая session.

### 4.2 Flight-only intent

Flight-only intent определяется, когда пользователь просит рейс, билет, перелет, маршрут или варианты добраться самолетом без запроса на проживание.

Примеры:

- "Найди рейс из Москвы в Рим 10 июня обратно 15 июня."
- "Хочу билет в Лиссабон без пересадок."
- "Покажи перелеты вечером с багажом."

Если destination уже есть из hotel или combined context, ассистент может использовать его как derived field, но должен показать это пользователю.

### 4.3 Combined hotel + flight intent

Combined intent определяется, когда пользователь описывает поездку целиком или явно просит перелет и проживание вместе.

Примеры:

- "Подбери поездку в Барселону на 5 дней с перелетом и отелем."
- "Хочу слетать на море и где-то остановиться."
- "Найди вариант на двоих до 1500 евро: билеты и отель."

MVP поддерживает Level 1 combined intent recognition и Level 2 same-dialog hotel and flight assistance. Level 3 coordinated combined search остается decision point для отдельного Stage 3 решения. Level 4 full package ranking остается Post-MVP/Open.

### 4.4 Open destination discovery intent

Open destination discovery определяется, когда destination неизвестен или задан как тип направления, настроение, климат или стиль отдыха.

Примеры:

- "Куда-нибудь в теплое место на неделю."
- "Хочу недорогую поездку на море."
- "Посоветуй направление для спокойного отдыха с ребенком."

В MVP open destination discovery является clarification-first flow. Полноценный provider-backed discovery зависит от provider capabilities и остается Open, если capabilities не подтверждены.

### 4.5 Ambiguous intent

Ambiguous intent возникает, когда запрос можно трактовать несколькими способами.

Примеры:

- "Хочу поездку в Париж."
- "Нужно что-то на выходные."
- "Подбери вариант до 1000 евро."

Ассистент обязан уточнить intent до запуска provider search. Он предлагает 2-3 понятные интерпретации, например: "искать отель, перелет или оба варианта в одной поездке?"

## 5. Общий lifecycle MVP search flow

1. User sends initial request.
2. Assistant detects intent.
3. Assistant extracts known fields.
4. Assistant separates provider facts, assistant assumptions and unknowns.
5. Assistant identifies missing required fields for the detected intent.
6. If intent is ambiguous, assistant asks an intent clarification question.
7. If required fields are missing, assistant asks one priority clarification question or a short related set.
8. If assumptions affect search, assistant shows or confirms them.
9. When minimum search condition is met and there is no blocking contradiction, assistant starts provider search.
10. System receives provider results, empty result, partial result or provider error.
11. Assistant presents a short summary in chat and structured results area.
12. User opens details, compares, saves or refines.
13. Refinement updates session fields and marks affected results stale.
14. Search repeats only when updated fields are sufficient.
15. Session ends or remains available for current-session save/shortlist and comparison.

Assistant can start search without additional questions when:

- intent is clear;
- required fields for the selected flow are present or safely derived;
- assumptions that affect search are visible;
- optional preferences are not framed as hard constraints;
- no unresolved contradiction blocks search;
- user does not request unsupported action.

Assistant must clarify first when:

- intent is ambiguous;
- a required field is missing;
- destination is open and discovery criteria are too broad;
- user input is contradictory;
- an optional field is expressed as "обязательно", "только", "без";
- a derived assumption may materially change search;
- provider search would be random or unverifiable.

## 6. Hotel Search Flow

### 6.1 Entry examples

- "Найди отель в Барселоне на 10-14 июня для двоих."
- "Хочу недорогой отель в центре Стамбула на выходные."
- "Подбери семейный отель у моря с завтраком."
- "Нужен отель, где тихо и можно работать."

### 6.2 Step-by-step flow

1. User sends hotel request.
2. Assistant detects hotel-only intent or hotel part of combined session.
3. Assistant extracts destination, dates/date range, duration, guests, rooms, budget, location preference, amenities and hard constraints.
4. Assistant marks assumptions, for example "1 room for 2 adults" or "недорого = budget preference".
5. Assistant checks hotel required fields: destination, check-in/check-out or date range + duration, guests count, rooms count or room assumption.
6. If required data is missing, assistant asks clarification.
7. If constraints conflict, assistant names the conflict before search or warns that provider search may return no results.
8. When minimum condition is met, assistant starts hotel provider search.
9. Results area shows hotel cards; chat gives a short summary and explains ranking basis.
10. User can open details, compare, save or refine.

### 6.3 Clarification logic

Priority order:

1. Destination.
2. Dates/date range/duration.
3. Guests count.
4. Rooms count or room assumption.
5. Hard constraints that could make search invalid.
6. Budget only when user made price critical or ranking would be too broad.

Examples:

- Missing dates: "Для отелей нужны даты или хотя бы период. На какие даты искать?"
- Missing guests: "На скольких гостей искать проживание?"
- Room assumption: "Буду считать 1 номер для двоих, если не скажете иначе."
- Contradiction: "5 звезд в центре Парижа на неделю до 200 евро выглядит как конфликт ограничений. Что важнее сохранить: бюджет, центр или класс отеля?"

### 6.4 Search trigger

Hotel search can start when:

- destination is known;
- dates/date range + duration are known;
- guests count is known;
- rooms count is known or assistant has explicit visible assumption;
- hard constraints do not block the search unresolved.

Hotel search must not start when destination, dates/period or guests are unknown and cannot be safely derived.

### 6.5 Results display

Hotel results are shown as structured hotel result cards and a short assistant summary.

Each result should distinguish:

- provider facts: hotel name, location, price, currency, rating/review score, amenities, source/freshness if available;
- assistant reasoning: why it matches the request;
- unknown data: missing cancellation policy, unknown amenities, missing freshness or partial provider data;
- trade-offs: cheaper but farther, better rating but over budget, good location but unknown cancellation.

Chat should not contain the only copy of results. Structured results area remains the primary place for scan and comparison.

### 6.6 Refinement behaviour

User can refine by changing:

- dates;
- budget;
- location/radius;
- hotel category;
- amenities;
- guests/rooms;
- hard constraints.

Assistant updates session parameters, shows changed fields, marks affected hotel results stale, and reruns search only when required fields remain complete.

Example:

1. User: "Теперь только рядом с пляжем и до 150 евро."
2. Assistant updates location preference and budget.
3. Existing hotel offers are marked stale for location/budget.
4. Search reruns if dates/destination/guests/rooms remain complete.

### 6.7 Empty / partial / error handling

**No results:** provider search succeeds but no hotel offers match. Assistant explains that nothing was found for current constraints and suggests 1-3 relaxations: broader area, higher budget, flexible dates, fewer hard amenities.

**Partial results:** provider returns offers with missing fields. Assistant may show them if critical fields are present, but marks unknown data and does not use missing fields as recommendation reasons.

**Provider error:** provider/API is unavailable or failed. Assistant says this is a source problem, not proof that no hotels exist, and offers retry or changing constraints.

### 6.8 Save / shortlist behaviour

User can save one hotel offer or a comparison set in the current search session.

Saved hotel item includes product-level context:

- selected offer reference in current session;
- provider facts snapshot;
- unknown fields;
- assistant assumptions used in explanation;
- freshness/source marker if available;
- related search parameters.

Assistant confirms save and reminds that price/availability may need refresh before booking outside MVP.

## 7. Flight Search Flow

### 7.1 Entry examples

- "Найди рейс из Москвы в Рим 10 июня обратно 15 июня на двоих."
- "Хочу билет в Лиссабон в июне."
- "Покажи прямые рейсы в Стамбул на выходные."
- "Нужен перелет вечером, с багажом."

### 7.2 Step-by-step flow

1. User sends flight request.
2. Assistant detects flight-only intent or flight part of combined session.
3. Assistant extracts origin, destination, departure date/date range, return date/duration, one-way/round-trip intent, passengers, cabin class, budget, baggage, stops and time preferences.
4. Assistant marks defaults or assumptions, for example "economy" or "round-trip because user said 'слетать'".
5. Assistant checks flight required fields.
6. If required data is missing, assistant asks clarification.
7. If route/date/passenger fields are sufficient, assistant starts flight provider search.
8. Results area shows flight cards; chat summarizes price/time/convenience trade-offs.
9. User can open details, compare, save or refine.

### 7.3 Clarification logic

Priority order:

1. Origin.
2. Destination.
3. Departure date/date range.
4. One-way vs round-trip.
5. Return date or trip duration for round-trip.
6. Passengers count.
7. Hard constraints such as only direct, baggage required, strict departure time.

Examples:

- Missing origin: "Из какого города вылетать?"
- Ambiguous trip type: "Ищем билет в одну сторону или туда-обратно?"
- Missing duration: "На сколько дней планируете поездку, чтобы подобрать обратный рейс?"
- Hard direct constraint: "Понял, только прямой рейс. Если таких не будет, я покажу no results для этого ограничения и предложу варианты с пересадками отдельно."

### 7.4 Search trigger

One-way flight search can start when origin, destination, departure date/date range and passengers count are known.

Round-trip flight search can start when origin, destination, departure date/date range, return date or duration, and passengers count are known.

Flight search must not start when route, dates/period or passengers are missing and cannot be safely derived.

### 7.5 Results display

Flight results are shown as structured flight result cards and a short assistant summary.

Each result should distinguish:

- provider facts: airline, route, departure/arrival time, duration, stops, baggage availability, price, currency, source/freshness if available;
- assistant reasoning: why it matches user priorities;
- unknown data: baggage unknown, fare rules unknown, freshness missing;
- trade-offs: cheaper but longer, direct but more expensive, better time but missing baggage facts.

### 7.6 Refinement behaviour

User can refine by changing:

- dates or date flexibility;
- origin/destination airport;
- baggage;
- direct/stops preference;
- time of day;
- budget;
- passengers;
- one-way/round-trip.

Assistant updates fields, shows what changed, marks affected flight results stale and reruns provider search only when required fields remain complete.

If user changes flight intent from round-trip to one-way, return date becomes irrelevant but should remain visible as removed/unused context, not silently applied.

### 7.7 Empty / partial / error handling

**No results:** no flights match current constraints, for example only direct flights for a route/date. Assistant suggests 1-3 relaxations: allow stops, adjust date, widen airport, change time window.

**Partial results:** provider returns flights but missing baggage/fare/freshness facts. Assistant can show available fields and marks unknowns.

**Provider error:** assistant explains that flight source failed and does not claim no flights exist. It offers retry or continuing with hotel/open planning context if relevant.

### 7.8 Save / shortlist behaviour

User can save one flight offer or a flight comparison set in the current search session.

Saved flight item must preserve route, dates, passengers, provider facts snapshot, unknown fields, assumptions and freshness/source markers. It must not promise ticketing, booking, payment, live price or availability guarantee.

## 8. Combined Hotel + Flight Search Flow

### 8.1 Supported MVP levels

MVP supports:

- **Level 1 — combined intent recognition:** assistant recognizes that the user wants hotel + flight or a trip as a whole.
- **Level 2 — same-dialog hotel and flight assistance:** assistant keeps shared context and helps search hotel and flight parts in one search session.

**Level 3 — coordinated combined search:** decision point for a separate Stage 3 task. It may become a limited MVP behaviour only if explicitly accepted later.

**Level 4 — full combined package ranking:** Post-MVP/Open. It must not appear as a hidden promise in MVP UX.

### 8.2 Level 1 flow: parallel independent search

Level 1 UX behaviour:

1. User sends a combined request.
2. Assistant detects combined intent.
3. Assistant extracts shared fields: origin, destination, dates/duration, travelers, total budget and travel style.
4. Assistant separates hotel-specific and flight-specific preferences.
5. Assistant explains that MVP can help with hotel and flight in one session.
6. Assistant checks required fields for each part independently.
7. If both parts are ready, hotel and flight searches may run independently and results are shown in separate groups.
8. If only one part is ready, assistant can offer to search that part first and ask missing fields for the other.

Level 1 does not coordinate compatibility beyond shared session context.

### 8.3 Level 2 flow: user-guided combined comparison

Level 2 UX behaviour:

1. User provides enough shared fields for hotel and flight assistance.
2. Assistant runs hotel and flight searches as separate provider-backed parts.
3. Results are grouped by type: hotels and flights.
4. Assistant helps compare trade-offs using user priorities, for example cheaper flight vs better hotel.
5. If user gives total budget, assistant can propose a budget split only as assistant assumption.
6. User can save one hotel and one flight as a partial combined selection.
7. Assistant keeps selection clearly marked as a user-guided pairing, not a confirmed package.

Level 2 supports same-dialog assistance but does not promise package price, booking, guaranteed compatibility or automated package ranking.

### 8.4 Level 3 coordinated search status

Decision status: **Open / separate Stage 3 decision point**.

Until a separate Combined Search UX Decision is completed, Level 3 is not treated as completed MVP scope.

Allowed in this document:

- describe what Level 3 would coordinate;
- mark it as decision point;
- prevent Level 3 from being presented as package ranking.

Not allowed in this step:

- define package scoring;
- promise package-level recommendation;
- design API contracts or algorithms;
- close Stage 3 combined search decision.

Minimal possible Level 3 direction for a future decision: limited coordination checks for shared dates, destination, travelers, stale results and user-visible budget assumptions, without package ranking or booking.

### 8.5 Clarification logic

Combined clarification first asks for fields that unblock the widest part of the flow:

1. Intent confirmation if ambiguous: hotel, flight or both.
2. Origin, if flight or open destination is involved.
3. Destination or destination type.
4. Dates/date range or duration + period.
5. Travelers/passengers/guests.
6. One-way/round-trip for flight part.
7. Total budget only when user makes it critical.
8. Priority between hotel and flight if trade-offs are needed.

If required fields are complete for only one part, assistant should say so explicitly: "Для отелей данных уже достаточно, для перелета нужен город вылета."

### 8.6 Results display

Combined results are displayed in separated sections:

- hotel results;
- flight results;
- saved/shortlisted selections;
- comparison or trade-off summary.

The UI/product language must avoid "package" unless it clearly states that the pair is user-selected or partial. Hotel and flight facts remain separate provider facts.

### 8.7 Refinement behaviour

Refinement may affect one or both parts:

- changing dates affects both hotel and flight results;
- changing hotel location affects hotel results only;
- changing direct flight preference affects flight results only;
- changing total budget affects comparison assumptions and may affect both parts.

Assistant must show which results are stale and which remain usable.

### 8.8 Empty / partial / error handling

Combined flow often produces partial states:

- hotel results found, flight no results;
- flight results found, hotel provider error;
- one part ready, another missing required fields;
- both provider searches succeed but one has partial facts.

Assistant should present available results without hiding missing parts. It must avoid saying "поездка готова" when only one part is available.

If found only hotel or only flight:

- show available part as partial combined progress;
- explain what blocks the missing part;
- offer next step: clarify missing field, retry provider, relax constraints or save available part.

### 8.9 Save / shortlist behaviour

Combined save can store:

- one hotel offer;
- one flight offer;
- a user-selected hotel + flight pair;
- a small comparison set;
- a partial combined selection.

Saved combined selection must be labelled as partial/user-guided unless Level 3 or Level 4 is explicitly accepted later. It must include stale/freshness warnings for both parts independently.

## 9. Open Destination Discovery Flow

### 9.1 Entry examples

- "Куда-нибудь в теплое место на неделю."
- "Хочу недорогую поездку на море."
- "Подбери направление на выходные из Москвы."
- "Хочу спокойный отдых с ребенком, но не знаю куда."

### 9.2 Clarification-first behaviour

Open destination starts with clarification, not random provider search.

Step-by-step:

1. User sends open destination request.
2. Assistant detects open destination discovery intent.
3. Assistant extracts origin, period/season, duration, budget, travel style, climate/region and constraints.
4. Assistant identifies missing discovery fields.
5. Assistant asks for the smallest useful set, usually origin + period + budget/style if missing.
6. If destination becomes concrete, flow transitions to hotel, flight or combined search.
7. If destination remains open and provider-backed discovery is unavailable, assistant does not invent prices/availability.

### 9.3 MVP discovery boundaries

In MVP:

- assistant can clarify open destination criteria;
- assistant can help convert broad desire into concrete destination options only as assumptions or user-guided narrowing unless provider data confirms offers;
- assistant can transition to normal hotel/flight/combined search after destination is selected;
- assistant can treat visa/passport requirements as user-provided constraint but not as legal advice.

Post-MVP/Open:

- provider-backed destination discovery;
- real destination feasibility ranking;
- climate/visa/event enrichment as verified data source;
- package-level recommendation.

### 9.4 Search trigger

Provider-backed open destination search can start only if provider capability exists and required discovery fields are sufficient: origin, period/season or date range, duration, budget/flexibility and travel style/destination preference.

If provider capability is unknown, assistant should ask the user to choose or confirm a concrete destination before hotel/flight provider search.

### 9.5 Results display

If provider-backed discovery is available, results should show destination candidates with explicit source/freshness and known/unknown data.

If provider-backed discovery is unavailable, assistant may show a narrowed set of suggested directions only as assistant assumptions and should guide the user to pick one for regular search.

### 9.6 Refinement behaviour

User can refine:

- climate;
- region;
- duration;
- budget;
- flight length;
- travel style;
- family/accessibility constraints.

Assistant updates discovery criteria and either asks the next clarification or transitions to concrete search.

### 9.7 Empty / partial / error handling

**No results:** no destination/offer candidates match criteria if provider-backed discovery exists. Assistant suggests broadening period, budget, region or style.

**Partial results:** destination suggestions exist but hotel or flight facts are missing. Assistant marks them as not yet confirmed offers.

**Provider error:** assistant explains that discovery source failed and asks user to choose a destination manually or retry.

## 10. Result Refinement Flow

Refinement is a user change to active search parameters after intent detection, clarification, results, comparison or save.

Step-by-step:

1. User asks to change a parameter.
2. Assistant identifies affected intent part: hotel, flight, combined or discovery.
3. Assistant updates known fields and assumptions.
4. Assistant shows what changed and what stayed the same.
5. Assistant checks contradictions.
6. Assistant marks affected results stale.
7. Assistant reruns provider search if required fields remain complete.
8. Assistant updates results, no results, partial or error state.

Common refinement examples:

| User request | Expected behaviour |
|---|---|
| "Сделай дешевле" | Treat budget as changed or ask target range if unclear. |
| "Только без пересадок" | Apply hard flight constraint and rerun flight search if route/date/passengers complete. |
| "Ближе к центру" | Update hotel location preference and rerun hotel search if required fields complete. |
| "На день дольше" | Update dates/duration and mark hotel + flight results stale in combined session. |
| "Теперь хочу только отель" | Change intent focus, keep reusable fields, do not discard saved flight unless user confirms. |

If refinement makes required fields incomplete, assistant asks clarification before rerunning search.

## 11. Save / Shortlist Flow

Save / shortlist lets user keep useful offers inside current search session.

Step-by-step:

1. User clicks save/shortlist or asks in chat.
2. Assistant identifies target: one offer, multiple offers, comparison set or combined selection.
3. If target is ambiguous, assistant asks which item to save.
4. System saves current-session reference and product-level context.
5. Assistant confirms save.
6. Saved/Shortlisted Results area shows saved items.
7. User can return to saved item, compare saved items or refine search.

Saved item must preserve:

- known provider facts;
- unknown fields;
- assistant assumptions;
- source/freshness marker if available;
- related search parameters;
- partial/stale marker if relevant.

Save must not promise:

- booking;
- payment;
- account-level storage;
- cross-device sync;
- current price/availability after data becomes stale.

## 12. No Results, Partial Results and Error Recovery

| Situation | Meaning | Assistant behaviour | User next steps |
|---|---|---|---|
| No results | Search ran successfully but no matching offers were found. | Explain no matching offers under current constraints; suggest 1-3 relaxations. | Broaden dates, budget, location, stops, amenities or region. |
| Partial results | Some offers or facts are available, others missing. | Show known facts, mark unknown data, avoid overconfident recommendation. | Continue with available part, clarify missing fields, retry or save partial selection. |
| Provider error | Source failed or is unavailable. | State provider/source problem separately from no results. | Retry, change constraints, continue with available data or wait. |
| Contradiction | User constraints conflict before or after provider result. | Name conflict and ask which constraint to relax. | Choose priority or allow broader search. |
| Stale results | Session fields changed or saved data may be outdated. | Mark affected offers stale and offer refresh. | Refresh, compare with warning or discard old result. |

Assistant should never replace provider errors with invented offers or market claims. If it explains likely causes, they must be framed as assumptions, not facts.

## 13. Assistant Message Patterns

These are UX-role patterns, not production prompts.

| Pattern | UX role | Short example |
|---|---|---|
| Clarification question | Ask for missing required data before search. | "Чтобы найти перелет, нужен город вылета. Откуда летим?" |
| Assumption confirmation | Make derived assumption visible. | "Буду считать, что нужен 1 номер для двоих. Если нужно два номера, скажите." |
| Search started | Confirm that required data is sufficient and provider search began. | "Данных достаточно: Барселона, 10-14 июня, 2 гостя. Ищу отели по этим параметрам." |
| Results summary | Summarize results and trade-offs after provider response. | "Нашел несколько отелей: самый дешевый дальше от центра, лучший по рейтингу чуть выше бюджета." |
| No results explanation | Explain empty result and offer relaxations. | "По текущим условиям вариантов не найдено. Можно расширить район, поднять бюджет или сделать даты гибче." |
| Partial results explanation | Show available part and missing facts. | "Отели нашлись, но по части вариантов provider не вернул условия отмены. Я отмечу это как unknown." |
| Provider error explanation | Separate source failure from no results. | "Источник перелетов сейчас недоступен. Это не значит, что рейсов нет; можно повторить поиск или продолжить с отелями." |
| Refinement suggestion | Suggest concrete next step after conflict or weak results. | "Если важнее цена, можно разрешить пересадки; если важнее удобство, лучше расширить бюджет." |
| Saved item confirmation | Confirm current-session save and warn about freshness. | "Сохранил этот отель в текущей сессии. Перед бронированием цену и наличие нужно будет обновить." |

## 14. MVP vs Post-MVP

| Area | MVP | Post-MVP / Open |
|---|---|---|
| Hotel search | Natural-language search, clarification, provider-backed offers, result cards, details, refinement | Room-level booking, deep policy interpretation, loyalty programs |
| Flight search | Natural-language search, route/date/passenger clarification, flight cards, comparison, refinement | Ticketing, seat selection, fare-family optimization |
| Combined search | Level 1 + Level 2 in one search session | Level 3 decision Open; Level 4 package ranking Post-MVP/Open |
| Open destination | Clarification-first and transition to concrete search | Provider-backed destination discovery unless capability is confirmed |
| Results | Structured results, no results, partial, error, stale states | Advanced filters, map, price calendar, dashboards |
| Save / shortlist | Current search session only | Account-level storage, long-term history, sync |
| Assistant messages | UX patterns for clarification, assumptions, results and recovery | Production prompts, tool-calling chains, prompt optimization |
| Provider data | Real provider/API data in final MVP through provided contract | Production-hardening, provider taxonomy, SLA handling |
| Unsupported actions | Safe fallback for booking/payment/legal requests | Booking, payment, refunds, legal workflows |

## 15. Open Questions

- Входит ли limited Level 3 coordinated combined search в MVP, и какие exact UX acceptance criteria отделяют его от Level 4 package ranking?
- Какие provider capabilities будут доступны для open destination discovery?
- Какой минимальный freshness/source marker будет доступен из существующего travel API?
- Нужно ли всегда подтверждать defaults для rooms count, cabin class и passenger age groups?
- Какой минимальный уровень session persistence возможен без авторизации?
- Нужно ли в MVP показывать user-guided combined selection в отдельной зоне или достаточно saved/shortlist area?

## 16. Что не входит в этот шаг

В этот шаг намеренно не входит:

- visual design, UI concept, colors, typography, UI kit, wireframes или mockups;
- React/Next.js/Kotlin/Ktor код;
- API endpoints, DTO, OpenAPI, database schema или provider adapter design;
- LLM system prompt, production prompt chain, tool calling или orchestration implementation;
- закрытие всего Stage 3;
- финальное решение по Level 3 coordinated combined search;
- начало Stage 4;
- booking, payment, ticketing, refund, visa/legal advice;
- full package ranking.

## 17. Recommendations

- Следующим Stage 3 шагом принять Combined Search UX Decision, отдельно решив статус Level 3.
- После решения по combined search провести UX Consistency Review Stage 3.
- На будущих технических этапах сверить required fields и flow states с предоставленным travel API contract.
- На Stage 4 не менять product flow decisions без явного product review; Stage 4 должен заниматься visual/UI concept поверх уже описанных UX flows.
