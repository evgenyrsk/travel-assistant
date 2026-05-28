# Stage 4 — Screen Specifications

## 1. Назначение документа

Документ описывает ключевые экраны Travel Assistant MVP на уровне visual/UX specification.

Он опирается на Stage 3 screen map и hotel-only search flow, но добавляет визуальную композицию, UI-блоки, состояния и mobile considerations. Документ не является wireframe, pixel-perfect mockup, frontend implementation или architecture spec.

## 2. MVP Scope Reminder

Active MVP v1:

- Entry / Start;
- travel request chat for hotel search;
- clarification flow;
- hotel results;
- hotel offer details;
- saved results in current search session;
- basic hotel comparison;
- no results / partial / provider error states.

Future expansion:

- flight results screen;
- combined itinerary / recommendation screen;
- combined hotel + flight package concepts.

## 3. Entry / Start Screen

**Цель экрана:** дать пользователю сразу начать natural-language hotel request.

**Пользовательский сценарий:** пользователь открывает Travel Assistant и описывает нужное проживание обычным языком.

**Основные UI-блоки:**

- app shell;
- prominent chat input;
- short capability note for hotel search MVP;
- empty results area;
- optional examples as compact prompts, not marketing hero;
- current-session saved area only if session exists.

**Primary action:** отправить travel request.

**Secondary actions:** продолжить текущую session, открыть saved текущей session, выбрать пример запроса.

**Loading state:** assistant thinking after first message.

**Empty state:** results area объясняет, что варианты появятся после запроса и уточнений.

**Error state:** если assistant cannot process request, показать recoverable message and retry.

**Mobile considerations:** chat input and examples stack; results area appears below after interaction; avoid wide split layout.

**Open questions:**

- Нужны ли suggested prompts в MVP v1 или достаточно placeholder text?
- Как явно обозначить hotel-only MVP без ощущения ограничения продукта?

## 4. Travel Request Chat Screen

**Цель экрана:** вести пользователя от запроса к достаточным параметрам, результатам, refinement и explanations.

**Пользовательский сценарий:** пользователь вводит hotel request, отвечает на уточнения, меняет параметры, просит объяснить или сравнить.

**Основные UI-блоки:**

- chat panel;
- message bubbles;
- assistant clarification cards;
- search intent summary;
- active status indicator;
- chat input;
- results workspace рядом или ниже.

**Primary action:** отправить сообщение / ответить на уточнение.

**Secondary actions:** edit parameters, confirm assumption, retry, view results, save/compare via inline actions.

**Loading state:**

- assistant thinking;
- provider search started after required fields are complete;
- updating results after refinement.

**Empty state:** no messages or only initial prompt.

**Error state:** assistant failure, unsupported action fallback, provider error surfaced separately in results area.

**Mobile considerations:** chat stays readable; search summary can collapse; results reachable via sticky affordance or stacked section.

**Open questions:**

- Нужно ли показывать chat и results через tabs на mobile?
- Какой объем extracted fields должен быть всегда видим в collapsed summary?

## 5. Clarification Flow

**Цель экрана/состояния:** собрать missing required fields без превращения процесса в длинную форму.

**Пользовательский сценарий:** ассистент не может запустить hotel search без destination, dates/date range, guests count, rooms count или visible room assumption.

**Основные UI-блоки:**

- assistant clarification card;
- missing fields list;
- quick reply options;
- focused input/date selector/price range if needed;
- search intent summary with known fields and assumptions.

**Primary action:** ответить на самый важный вопрос.

**Secondary actions:** confirm assumption, choose flexibility, edit previous answer, cancel/new search.

**Loading state:** assistant updates session after answer.

**Empty state:** not applicable; clarification appears only after intent/data extraction.

**Error state:** invalid date range, incomplete required answer, contradiction between constraints.

**Mobile considerations:** one question per view; date selector and controls should be touch-friendly; avoid dense multi-field panel.

**Open questions:**

- Нужно ли подтверждать default 1 room for 1-2 adults always или только когда UI считает это important assumption?
- Как визуально отличать required missing field от useful optional preference?

## 6. Hotel Results Screen

**Цель экрана:** показать короткий, понятный shortlist hotel offers with explanations.

**Пользовательский сценарий:** после successful hotel provider search пользователь сканирует варианты, открывает details, сохраняет или сравнивает.

**Основные UI-блоки:**

- results header with search summary;
- sort control;
- basic filter/refinement panel;
- hotel offer cards;
- source/freshness and unknown markers;
- comparison tray/control;
- saved affordance;
- chat context nearby.

**Primary action:** открыть details выбранного hotel offer.

**Secondary actions:** save, add to comparison, change filters, sort, ask assistant to refine.

**Loading state:** card skeletons for provider search; updating marker when refinement reruns.

**Empty state:** no results before search; после search использовать No Results screen.

**Error state:** provider error block, retry, modify constraints.

**Mobile considerations:** cards stack vertically; filters in bottom sheet or collapsible section; comparison tray must not cover chat input.

**Open questions:**

- Нужны ли hotel photos in MVP v1 при неизвестном provider contract?
- Какой minimum visible freshness/source marker нужен на card level?

## 7. Flight Results Screen

**MVP status:** Future expansion after hotel flow.

**Цель будущего экрана:** показать flight offers по route/date/passenger constraints.

**Пользовательский сценарий:** future user searches flights after flight flow is explicitly added.

**Основные UI-блоки future scope:**

- route/date summary;
- flight offer cards;
- stops/duration/baggage facts;
- price/source/freshness markers;
- comparison controls.

**Primary action:** open flight details.

**Secondary actions:** save, compare, refine route/date/stops/baggage.

**Loading/empty/error/mobile considerations:** должны быть описаны на future flight stage.

**Open questions:**

- Какие flight provider facts будут доступны?
- Какие required fields и acceptance criteria будут утверждены для flight expansion?

## 8. Combined Itinerary / Recommendation Screen

**MVP status:** Future expansion after flight flow; not active MVP v1.

**Цель будущего экрана:** показывать hotel + flight context without implying package guarantee.

**Пользовательский сценарий:** future user compares coordinated hotel and flight options.

**Основные UI-блоки future scope:**

- shared trip summary;
- hotel part;
- flight part;
- budget/date compatibility indicators;
- assumptions and unknown data;
- stale markers;
- save linked selection.

**Primary action:** future scope; likely inspect linked selection or compare alternatives.

**Secondary actions:** adjust dates/budget, save selection, split into hotel/flight details.

**Loading/empty/error/mobile considerations:** должны быть описаны на future combined stage.

**Open questions:**

- Будет ли provider поддерживать package-level offers?
- Как показывать approximate totals without implying guaranteed package price?

## 9. Offer Details Screen

**Цель экрана:** дать проверяемое объяснение конкретного hotel offer.

**Пользовательский сценарий:** пользователь хочет понять, почему отель подходит, какие есть компромиссы и какие данные неизвестны.

**Основные UI-блоки:**

- offer title and location;
- price/date/guest context;
- provider facts section;
- rationale/reasoning summary;
- assumptions section;
- unknown/partial data section;
- source/freshness;
- save and compare actions;
- back to results.

**Primary action:** save/shortlist или add to comparison, в зависимости от context.

**Secondary actions:** back to results, ask assistant why, refine search, remove from comparison.

**Loading state:** details skeleton if data loads separately.

**Empty state:** not applicable; if offer no longer available, show stale/unavailable state.

**Error state:** details unavailable/provider error; preserve card-level facts if available and mark stale/partial.

**Mobile considerations:** details can open as full screen or bottom sheet; primary actions sticky at bottom only if they do not hide content.

**Open questions:**

- Какие details fields будут guaranteed from provider?
- Нужно ли показывать photos/gallery in first UI milestone?

## 10. Saved Results Screen

**Цель экрана:** показать saved/shortlisted hotel offers within current search session.

**Пользовательский сценарий:** пользователь сохранил один или несколько вариантов и хочет вернуться к ним, сравнить или открыть details.

**Основные UI-блоки:**

- saved list;
- saved comparison set if applicable;
- freshness/stale warning;
- related search parameters;
- actions: details, compare, remove, return to search.

**Primary action:** открыть saved offer details или compare saved offers.

**Secondary actions:** remove from saved, return to current results, refresh if supported later.

**Loading state:** saving in progress, saved list updating.

**Empty state:** no saved items in current session with action to return to results/chat.

**Error state:** saved item stale/unavailable; provider refresh failed if refresh exists later.

**Mobile considerations:** saved list as stack; comparison should avoid wide table; warnings concise.

**Open questions:**

- Как долго current session saved state сохраняется без authorization?
- Нужен ли explicit "current session only" label на каждом saved item или только в saved screen header?

## 11. Error / No Results Screen

**Цель экрана:** дать понятный recovery path without false facts.

**Пользовательский сценарий:** search completed with no matching hotels, provider failed, data partial or stale.

**Основные UI-блоки:**

- state title;
- explanation;
- current constraints summary;
- suggested next actions;
- retry/change controls;
- chat input for refinement.

**Primary action:**

- No results: change constraints.
- Provider error: retry.
- Partial: continue with visible limitations or refine.
- Stale: refresh or compare with warning.

**Secondary actions:** return to previous results, save current partial item, ask assistant to suggest relaxation.

**Loading state:** retry or rerun search.

**Empty state:** not applicable.

**Error state:** if retry fails, keep provider error distinct from no results.

**Mobile considerations:** recovery actions should be visible above fold; avoid long explanatory blocks.

**Open questions:**

- Какие provider error categories будут доступны для user-facing copy?
- Какие relaxations should be generated by assistant vs predefined UI suggestions?

## 12. Cross-screen Rules

- Every screen must preserve active search session context.
- Provider facts, assistant assumptions, user-provided constraints and unknown data must remain visually distinct.
- Booking/payment actions must not appear as supported MVP actions.
- Flight and combined screens must not be linked as active MVP v1 flows.
- Search parameter changes should mark affected results stale.
- Mobile layout can change composition, but not product behaviour.
