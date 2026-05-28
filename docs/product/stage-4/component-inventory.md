# Stage 4 — Component Inventory

## 1. Назначение документа

Документ описывает MVP UI component inventory для Travel Assistant на уровне UX/design specification.

Он не создает React/Vue/Next.js компоненты, не задает props/API, не выбирает библиотеку и не является implementation contract. Компоненты описаны как будущие UI building blocks, которые должны поддерживать Stage 3 Hotel-Only MVP v1.

## 2. Статусы

| Status | Значение |
|---|---|
| MVP v1 | Нужен для hotel-only MVP v1. |
| MVP v1 fallback | Нужен для корректной обработки unsupported/future-scope actions. |
| Future expansion | Нужен после hotel flow, например flight или combined search. |
| Post-MVP/Open | Важен позже, но не нужен для первого MVP. |

## 3. Component Inventory

| Component | Назначение | Где используется | Основные состояния | UX-правила | Status |
|---|---|---|---|---|---|
| App Shell / Layout | Общая структура рабочего пространства. | Start, chat, results, details, saved. | Empty, active session, loading, results, details open. | Chat и results должны сосуществовать; не превращать первый экран в landing page. | MVP v1 |
| Chat Panel | Главная conversational surface. | Entry, clarification, refinement, explanation, save/compare commands. | Empty, thinking, waiting for user, answer, error fallback. | Chat управляет сценарием, но results не должны жить только в chat-тексте. | MVP v1 |
| Message Bubble | Сообщение пользователя или ассистента. | Chat Panel. | User, assistant, system/status, error, saved confirmation. | Сообщения должны быть короткими, с actions/links при необходимости; facts не прятать в длинной прозе. | MVP v1 |
| Assistant Clarification Card | Структурированное уточнение missing fields. | Chat, clarification area. | Missing required, assumption confirmation, contradiction, future-scope fallback. | Один приоритетный вопрос или короткий набор связанных вопросов; не анкета. | MVP v1 |
| Search Intent Summary | Видимое summary текущего запроса и extracted fields. | Clarification area, results header, details context. | Draft, ready to search, changed, stale, incomplete. | Показывать destination, dates, guests, rooms, budget/preferences, user-provided constraints и assumptions. | MVP v1 |
| Hotel Offer Card | Краткое представление hotel offer. | Hotel results, saved, comparison candidates. | Default, highlighted/recommended, selected, saved, partial, stale, unavailable/error. | Название, location, price, rating/review, key reasons, unknown/source markers; не обещать booking. | MVP v1 |
| Flight Offer Card | Краткое представление flight offer. | Future flight results. | Default, selected, partial, stale, unknown baggage/source. | Не показывать в MVP v1 как доступный search result; только future reference. | Future expansion |
| Offer Details View | Детальный просмотр offer. | From result card, saved, comparison. | Loading, facts available, partial data, stale, saved. | Разделять provider facts, rationale, assumptions, unknown; actions: save, compare, back. | MVP v1 для hotel |
| Filter Panel | Ограниченный набор фильтров/refinement controls. | Results screen, mobile bottom sheet. | Collapsed, expanded, changed, disabled during loading. | MVP filters должны быть минимальными: budget, location, rating/amenities if available; advanced filters post-MVP. | MVP v1 basic |
| Sort Control | Сортировка результатов. | Results header. | Recommended, price, rating, location fit, changed. | Default сортировка должна отражать assistant recommendation; user can override. | MVP v1 basic |
| Chips / Tags | Compact labels для preferences, facts, status и filters. | Summary, cards, details, filters, comparison. | Selected, removable, disabled, warning, info. | Не полагаться только на цвет; tags для unknown/assumption должны быть понятны. | MVP v1 |
| Buttons | Команды пользователя. | Everywhere. | Primary, secondary, ghost, destructive/error-safe, loading, disabled. | Один primary action в локальной области; booking/payment CTA не показывать в MVP. | MVP v1 |
| Forms / Inputs | Text input и structured corrections. | Chat input, clarification, filter panel. | Empty, focused, filled, invalid, disabled, loading. | Не заменять chat большой формой; inputs появляются для точечных уточнений. | MVP v1 |
| Date Selector | Выбор check-in/check-out или date range. | Clarification, parameter edit, filters. | Empty, selected range, flexible/approximate, invalid, loading. | Dates required before hotel search; flexibility должна быть видимой assumption/constraint. | MVP v1 |
| Price Range | Budget или price preference. | Clarification, filters, no-results recovery. | Empty, range selected, budget tier, invalid, changed. | Budget не всегда required, но если пользователь сделал цену ключевой, UI должен это показать. | MVP v1 |
| Saved Item Control | Save/shortlist action. | Cards, details, comparison, saved screen. | Unsaved, saved, saving, stale saved, disabled. | Save действует только в текущей session; freshness warning рядом с saved context. | MVP v1 |
| Comparison Control | Добавить/убрать offer из comparison. | Cards, details, comparison view. | Not selected, selected, max reached, disabled, stale candidate. | Comparison supports 2-5 hotel offers; если критерий неясен, ассистент уточняет. | MVP v1 basic |
| Loading State Components | Skeletons/status indicators. | Chat, results, cards, details, save action. | Assistant thinking, provider search, updating, saving. | Не показывать fake facts; различать thinking и provider search. | MVP v1 |
| Empty State Components | Пустые состояния. | Start, results, saved, comparison. | No request yet, no saved items, no comparison candidates. | Давать next action, не маркетинговый текст. | MVP v1 |
| Error State Components | Ошибки source/assistant/action. | Results, chat, details. | Provider error, assistant failure, retry available, unsupported action. | Provider error не равен no results; technical details скрыты. | MVP v1 |
| Notification / Toast | Небольшое подтверждение действия. | Save, add/remove comparison, copied/shared future action. | Success, info, warning, error. | Не использовать toast как единственный носитель critical info. | MVP v1 basic |
| Modal / Bottom Sheet | Focused overlay для details, filters или confirmation. | Mobile details/filter, desktop optional modal. | Open, loading, error, close. | На desktop details может быть panel; на mobile bottom sheet допустим. | MVP v1 pattern |

## 4. Component Notes

### 4.1 App Shell / Layout

**Назначение:** задает общую рабочую область Travel Assistant.

**MVP UX rules:**

- Start state сразу показывает chat input.
- Active session сохраняет chat доступным.
- Results area не должна исчезать при каждом сообщении ассистента.
- Layout должен поддерживать empty, loading, results, no results, error и details states.

### 4.2 Chat Panel

**Назначение:** conversational control plane для запроса, уточнения, refinement, save/compare и explanation.

**MVP UX rules:**

- Не превращать chat в единственное место structured facts.
- Assistant messages должны быть short, actionable, explainable.
- Chat input должен поддерживать изменение параметров естественным языком.

### 4.3 Assistant Clarification Card

**Назначение:** делает уточнение видимым и actionable.

**MVP UX rules:**

- Отображает missing required field и причину.
- Может включать 2-3 quick replies или compact input.
- Не запрашивает все optional preferences сразу.
- Если речь о future-scope flight/combined, показывает fallback без запуска search.

### 4.4 Search Intent Summary

**Назначение:** показывает, что ассистент понял.

**MVP UX rules:**

- Разделяет known fields, user-provided constraints, assumptions и missing fields.
- При изменении параметров показывает changed fields.
- Если old results stale, summary должен это показывать.

### 4.5 Hotel Offer Card

**Назначение:** помогает быстро сканировать shortlist hotel offers.

**Core content:**

- hotel name;
- location;
- price/currency;
- dates or nights context;
- rating/review score if provider returned it;
- key amenities if provider returned them;
- 1-2 reason chips or short rationale;
- source/freshness/unknown marker if available.

**MVP UX rules:**

- Цена и availability только как provider facts.
- Missing cancellation/amenities/rating marked unknown.
- Save and compare controls visible but not noisy.
- Details action primary or clearly discoverable.

### 4.6 Offer Details View

**Назначение:** показывает полную аргументацию и data confidence.

**MVP UX rules:**

- Provider facts first.
- Assistant rationale separate.
- Assumptions and unknown fields visible.
- Booking/payment CTA absent.
- Stale saved item warns before user trusts old price/availability.

### 4.7 Filter Panel and Sort Control

**Назначение:** позволяет пользователю уточнить results без потери chat flow.

**MVP UX rules:**

- Filters should map to known product fields: budget, location, rating, amenities, flexibility.
- If filter changes required fields or hard constraints, affected results become stale before refresh.
- Sort default can be assistant recommendation, but user override should be visible.

### 4.8 Loading, Empty and Error Components

**MVP UX rules:**

- Assistant thinking and provider search are visually distinct.
- Empty state contains next action.
- No results suggests relaxations.
- Provider error suggests retry/fallback and does not imply absence of hotels.
- Partial results show unknown data inline.

## 5. Post-MVP / Future Component Carryover

Future expansion components should be designed later, not implemented in MVP v1:

- Flight Offer Card and Flight Details;
- Combined Trip Summary;
- Package/Itinerary Card;
- Map View;
- Price Calendar;
- Account Saved Trips;
- Cross-device Resume;
- Booking/Payment components.

These items should not appear as active MVP actions unless a future product decision changes scope.
