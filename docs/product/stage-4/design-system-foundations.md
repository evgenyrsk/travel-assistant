# Stage 4 — Design System Foundations

## 1. Назначение документа

Документ описывает foundational design system для Travel Assistant на продуктово-дизайнерском уровне.

Он задает рабочие принципы цветов, типографики, spacing, layout, состояний и accessibility, чтобы будущая frontend-реализация могла построить UI system без преждевременного production-кода.

Документ не является финальным набором design tokens, Tailwind theme, shadcn/ui configuration, React component implementation или брендбуком.

## 2. Design System Goals

Design system Travel Assistant должна:

- поддерживать chat-first hotel search flow;
- делать structured hotel results сканируемыми;
- отделять provider facts, assistant assumptions, user-provided constraints и unknown data;
- помогать пользователю видеть loading, no results, partial, stale и provider error states;
- сохранять спокойный, доверительный travel-oriented стиль;
- быть пригодной для desktop-first web MVP и mobile-aware адаптации;
- оставаться совместимой с будущим frontend stack, если он будет подтвержден на technical stages.

## 3. Цветовая система

### 3.1 Принципы

Цвет не должен быть единственным носителем смысла. Все critical states должны дополнительно использовать текст, iconography, border style, labels или layout.

Цветовая система должна быть:

- calm, low-noise, readable;
- достаточно контрастной для accessibility;
- не перегруженной travel-ассоциациями;
- пригодной для offer cards, chat, details, comparison и status states.

### 3.2 Draft palette direction

Финальные HEX-цвета не утверждаются на Stage 4. Рабочая draft-направленность:

| Role | Draft direction | Назначение |
|---|---|---|
| Neutral background | warm/cool off-white или very light neutral | Основной фон приложения. |
| Surface | white или near-white | Карточки, panels, inputs. |
| Surface muted | light neutral | Empty, skeleton, secondary blocks. |
| Text primary | near-black neutral | Основной текст и важные факты. |
| Text secondary | medium neutral | Meta, helper text, secondary labels. |
| Primary accent | calm blue/teal | Primary actions, active navigation, trusted AI affordance. |
| Travel accent | soft sky/sage/sand as supporting accent | Ненавязчивые travel cues. |
| Success | restrained green | Saved, completed, available. |
| Warning | amber/ochre | Stale, assumptions, constraints warning. |
| Error | red with restrained saturation | Provider error, blocking issue. |
| Info | blue/cyan | Clarification, unknown, source/freshness info. |

Draft palette не должна превращаться в однотонную синюю, фиолетовую, бежевую или темную dashboard-схему. Нейтрали должны доминировать, акценты должны помогать навигации и состояниям.

## 4. Semantic Color Roles

| Semantic role | Использование | UX-правило |
|---|---|---|
| `background` | App shell, page background | Не конкурирует с cards/results. |
| `surface` | Cards, panels, chat area | Должен иметь достаточный контраст с background. |
| `surface-elevated` | Details panel, modal, bottom sheet | Используется экономно для focus layers. |
| `text-primary` | Названия, цены, primary facts | Не использовать muted цвет для цены и hard facts. |
| `text-secondary` | Meta, labels, timestamps | Должен оставаться readable. |
| `action-primary` | Main CTA | Один dominant action в области. |
| `action-secondary` | Secondary buttons, links | Не спорит с primary action. |
| `status-success` | Saved, complete | Не использовать для unverified availability. |
| `status-warning` | Assumption, stale, partial | Не превращать warning в error. |
| `status-error` | Provider/API error, blocking validation | Не использовать для no results. |
| `status-info` | Unknown, clarification, source/freshness | Помогает объяснить data confidence. |
| `focus-ring` | Keyboard focus | Должен быть контрастным и видимым. |

## 5. Typography Principles

### 5.1 Общие принципы

- Interface typography должна быть highly readable.
- Display-scale typography используется только на start/entry moments, если это не мешает рабочему сценарию.
- Offer cards, filters и chat должны использовать compact hierarchy, а не hero-style headings.
- Цены, dates, location, rating и key reason должны иметь predictable hierarchy.
- Не использовать negative letter spacing.
- Не масштабировать font-size от viewport width.

### 5.2 Роли текста

| Role | Назначение |
|---|---|
| Page title | Короткое название текущего workspace или flow. |
| Section title | Results, saved, comparison, details. |
| Card title | Hotel name или item name. |
| Fact text | Price, dates, rating, location, guests. |
| Rationale text | Почему вариант подходит, trade-offs. |
| Meta text | Source, freshness, unknown labels. |
| Chat message | User/assistant dialog content. |
| Control label | Inputs, filters, chips. |

## 6. Spacing Scale

Рабочая spacing scale должна быть простой и кратной базовому шагу. Draft direction:

| Token idea | Примерный шаг | Использование |
|---|---:|---|
| `space-1` | 4px | Icon gaps, tight metadata. |
| `space-2` | 8px | Small control gaps, chip spacing. |
| `space-3` | 12px | Card internal grouping. |
| `space-4` | 16px | Default component padding. |
| `space-5` | 20px | Panel spacing. |
| `space-6` | 24px | Section gaps. |
| `space-8` | 32px | Major layout gaps. |

Spacing должен помогать сканировать card hierarchy. Cards не должны становиться чрезмерно воздушными: hotel results являются рабочей выдачей, а не маркетинговыми блоками.

## 7. Border Radius

Рабочее направление:

- small radius для inputs, buttons, chips и compact controls;
- medium radius для cards и panels;
- larger radius только для modal/bottom sheet containers, если это соответствует future UI kit;
- избегать чрезмерно округлых card surfaces.

Рекомендуемый draft:

| Role | Draft radius |
|---|---:|
| Inputs/buttons/chips | 6-8px |
| Cards/panels | 8px |
| Modal/bottom sheet | 10-12px |

## 8. Shadows and Elevation

Elevation должна поддерживать layered workflow, а не создавать декоративную глубину.

| Layer | Использование |
|---|---|
| Flat | Main app background, static sections. |
| Subtle border | Default cards and panels. |
| Low elevation | Hover/active cards, floating controls. |
| Medium elevation | Details overlay, modal, bottom sheet. |

Большинство surfaces должны опираться на border + background. Тени использовать экономно, чтобы UI оставался спокойным.

## 9. Grid and Layout Rules

### 9.1 Desktop layout

Desktop-first MVP должен поддерживать:

- persistent app shell;
- chat panel как основной command surface;
- results/details area как structured workspace;
- optional right/secondary panel для parameters, saved или details, если viewport позволяет;
- stable column widths, чтобы loading/empty/results не вызывали layout jumps.

Draft composition:

| Area | Роль |
|---|---|
| Left/primary panel | Chat and clarification. |
| Main workspace | Hotel results, comparison, details. |
| Secondary panel | Search intent summary, saved, filters, facts/assumptions. |

Это direction, а не обязательная grid implementation.

### 9.2 Mobile layout

Mobile-aware MVP должен:

- показывать один главный фокус за раз;
- сохранять быстрый доступ к chat input;
- использовать stacked layout, tabs, accordion или bottom sheet для details/filters;
- не требовать широких comparison tables;
- поддерживать touch-friendly controls.

## 10. Responsive Behavior

| Breakpoint idea | Behaviour |
|---|---|
| Narrow mobile | Chat, summary and results stack vertically; filters/details in bottom sheet or inline sections. |
| Large mobile / small tablet | Chat remains primary; results list follows; sticky summary/actions допустимы. |
| Tablet / desktop | Chat and results can be side-by-side. |
| Wide desktop | Add secondary context panel only if it helps, not as empty decoration. |

Responsive design должен сохранять одинаковую product logic: один search session, те же states, те же provider fact / assistant assumption / user-provided constraint / unknown distinctions.

## 11. Accessibility Principles

- Все interactive controls должны быть keyboard accessible.
- Focus states должны быть видимы.
- Text contrast должен соответствовать WCAG AA как минимум для обычного текста и controls.
- Critical statuses не должны передаваться только цветом.
- Buttons и links должны иметь понятные accessible names.
- Loading states должны быть озвучиваемыми для assistive technologies на future implementation stage.
- Motion должен быть restrained и отключаемым/минимальным для prefers-reduced-motion.
- Chat history и dynamic results должны быть структурированы так, чтобы screen reader мог понять новые сообщения и state changes.

## 12. Focus States

Focus state должен:

- быть видимым на светлых и muted surfaces;
- не зависеть только от box shadow, если контраст слабый;
- применяться к buttons, inputs, chips, cards with actions, saved controls, comparison controls, tabs и modal close;
- не сдвигать layout.

Draft direction: 2px focus ring с semantic focus color и небольшим offset.

## 13. Loading and Skeleton Principles

Loading states должны различать:

- assistant thinking;
- clarification preparation;
- provider hotel search;
- updating/refining existing results;
- saving/shortlisting;
- opening details.

Skeletons уместны для hotel cards и details, но не должны показывать fake facts. Текстовые статусы должны честно описывать stage: "Понимаю запрос", "Ищу отели", "Обновляю результаты".

Loading не должен обещать price/availability до provider facts.

## 14. Empty, Error and Success States

### Empty

Empty state должен помогать начать действие:

- start screen: natural-language request input;
- results area: search еще не запускался;
- saved list: nothing saved in current session.

### No results

No results означает successful provider search без подходящих offers. Он должен предлагать 1-3 конкретных relaxations.

### Error

Provider error означает source/API failure. Он не должен выглядеть как no results и не должен намекать, что вариантов нет.

### Partial

Partial state показывает доступные facts и unknown fields. Missing data не используется как reason for recommendation.

### Success

Success применяется к действиям пользователя: saved, added to comparison, search parameters updated. Success не подтверждает booking, final price или guaranteed availability.

## 15. Dark Mode Considerations

Dark mode не является обязательным MVP v1 deliverable на Stage 4, но foundations не должны его блокировать.

Если dark mode будет добавлен позже:

- semantic color roles должны маппиться отдельно;
- status colors должны сохранять meaning и contrast;
- shadows должны быть заменены на borders/surface contrast;
- hotel card readability должна быть проверена отдельно;
- provider facts и unknown markers не должны теряться на dark surfaces.

## 16. Связь с будущим frontend stack

Предварительный frontend ориентир в репозитории: Next.js + React + Tailwind + shadcn/ui как рабочая гипотеза. Stage 4 не утверждает этот стек и не создает implementation artifacts.

Для будущей реализации design foundations должны быть легко переведены в:

- semantic design tokens;
- Tailwind theme values;
- shadcn/ui component variants;
- responsive layout primitives;
- accessible component states;
- visual regression checks.

Эта связь является carryover для Stage 5/6, а не задачей Stage 4.

## 17. Open questions

- Какие final brand colors должны быть утверждены перед первым UI milestone?
- Нужен ли dark mode в MVP v1 или достаточно non-blocking token readiness?
- Какие minimum accessibility checks будут gate для frontend implementation?
- Какие hotel imagery/source data будут доступны и как они повлияют на card layout?
