# Stage 3.1 — MVP Screen Map / UX Navigation

## 1. Назначение документа

Документ фиксирует карту экранов и навигационную модель web-first MVP Travel Assistant на уровне UX-навигации.

Цель документа — показать, какие зоны интерфейса нужны пользователю, чтобы пройти путь от естественного запроса к уточнениям, результатам, сравнению, деталям оффера и сохраненным вариантам.

Документ не является визуальным дизайном, wireframe, технической архитектурой, API-контрактом или спецификацией компонентов.

Связанный следующий документ Stage 3: `docs/product/stage-3/required-fields-and-acceptance-criteria.md`.

## 2. Источники и ограничения

Основные источники:

- `README.md`;
- `docs/product/README.md`;
- `docs/roadmap/roadmap.md`;
- `docs/ROADMAP.md`;
- `docs/product/stage-0/mvp-boundaries.md`;
- `docs/product/stage-1/stage-1-summary.md`;
- `docs/product/stage-1/user-journeys.md`;
- `docs/product/stage-2/use-cases.md`;
- `docs/product/stage-2/assistant-behaviour-rules.md`;
- `docs/product/stage-2/combined-search-levels.md`;
- `docs/product/stage-2/data-requirements.md`;
- `docs/product/stage-2/stage-2-summary.md`.

Ограничения:

- Stage 3.1 описывает UX-навигацию, но не закрывает весь Stage 3.
- Primary roadmap синхронизирован: Stage 3 отвечает за MVP UX / Navigation, а Stage 4 остается будущим этапом Visual Design / UI Concept.
- Документ не меняет смысл Stage 0-2.
- Документ не принимает архитектурные решения, не проектирует API, DTO, database schema, provider adapters, LLM prompts или frontend implementation.
- Финальный MVP должен использовать provider/API data как primary source of truth для travel facts.
- Booking, payment, гарантии цены, полноценная долгосрочная история и account-level storage не входят в MVP.

## 3. MVP UX Scope

В UX scope MVP входят:

- web-first entry point с естественным текстовым запросом;
- основной AI chat interface как главный способ взаимодействия;
- уточнение недостающих параметров поездки без превращения сценария в длинную форму;
- отображение результатов отелей и перелетов в структурированном виде;
- базовое сравнение нескольких вариантов;
- просмотр деталей одного offer;
- сохранение offer или подборки в рамках текущей search session;
- возврат к текущему поиску и уточнение запроса;
- empty, loading, error и no results states;
- явное отображение unknown data, provider limitations и freshness, если данные доступны на продуктовом уровне.

В MVP UX scope не входят:

- бронирование и оплата;
- полноценный личный кабинет;
- долгосрочная история поездок с авторизацией;
- mobile app как отдельный первый клиент;
- сложные фильтры, карта, календарь цен и package builder;
- full combined package ranking как единая сущность, если Stage 3 отдельно не подтвердит Level 3/Level 4 scope;
- pixel-perfect layout и дизайн-система.

## 4. Основные пользовательские входы

Основные входы в MVP:

| Entry point | Назначение | MVP status |
|---|---|---|
| Новый естественный запрос в chat | Старт hotel, flight или combined сценария | In MVP |
| Ответ на уточняющий вопрос | Заполнение missing required data | In MVP |
| Выбор offer из results overview | Переход к offer details или сравнению | In MVP |
| Команда сравнения | Сравнение 2-5 offers или saved candidates | In MVP в базовом виде |
| Команда сохранения | Сохранение offer или подборки в текущей session | In MVP для текущей session |
| Возврат к текущему поиску | Продолжение активной search session | In MVP для текущей session |
| Запрос booking/payment | Безопасный отказ и предложение поддерживаемого next step | In MVP fallback |

AI chat является главным entry point MVP. Стартовый экран не должен быть отдельной marketing landing page: он должен сразу вести к рабочему сценарию запроса.

## 5. Карта экранов MVP

| Экран / зона | Назначение | Основное содержимое | MVP status |
|---|---|---|---|
| Start / Entry screen | Начать новый travel request | Chat input, короткий контекст текущей возможности, пустое состояние results | In MVP |
| AI Chat Interface | Вести диалог, уточнять параметры, объяснять результаты | Сообщения пользователя и ассистента, уточнения, команды save/compare/resume | In MVP |
| Trip Parameters / Clarification Area | Показать извлеченные параметры и missing fields | Intent, destination, origin, dates, guests/passengers, budget, preferences, unknown fields | In MVP |
| Results Overview | Показать найденные offers после поиска | Список hotel cards, flight cards или разделенные группы результатов | In MVP |
| Hotel Result Cards | Кратко показать варианты отелей | Название, локация, цена, даты, рейтинг/review score, amenities, freshness/unknown markers, reason summary | In MVP |
| Flight Result Cards | Кратко показать варианты перелетов | Airline, route, departure/arrival, duration, stops, baggage availability, price, freshness/unknown markers, reason summary | In MVP |
| Offer Details | Показать детали одного offer | Provider facts, assumptions, unknown data, explanation, actions: save, compare, back to results | In MVP |
| Saved / Shortlisted Results | Показать сохраненные варианты текущей session | Saved offers, saved comparison set, freshness warning, return to search/results | In MVP для текущей session |
| Comparison View / Area | Сравнить выбранные offers | Trade-offs, provider facts, unknown fields, assistant recommendation | In MVP в базовом виде |
| Empty State | Показать стартовое отсутствие результатов | Ввод запроса и нейтральная подсказка к началу | In MVP |
| Loading State | Показать, что ассистент думает или ищет | Статус обработки запроса, уточнение или поиск у provider/API | In MVP |
| Error State | Показать ошибку без смешивания с no results | Provider unavailable, assistant failure, retry/change constraints | In MVP |
| No Results State | Объяснить отсутствие подходящих offers | Причина, если известна, и 1-3 предложения ослабить ограничения | In MVP |

Результаты должны отображаться комбинированно: chat остается главным контекстом объяснений и уточнений, а structured results показываются в отдельной области рядом с chat или под ним в зависимости от viewport. Это не фиксирует layout, но фиксирует принцип: результаты не должны существовать только как длинный текст внутри chat.

## 6. Навигационная модель

Навигационная модель строится вокруг active search session.

Основные правила:

- Chat является главным entry point и постоянным способом управления сценарием.
- Start screen переходит в active chat после первого запроса.
- Clarification area появляется, когда ассистент извлек параметры и видит missing required или useful fields.
- Results overview появляется после успешного поиска через provider/API data source или временные mock/fake providers на ранней разработке.
- Hotel offers и flight offers визуально и навигационно разделяются по типу, чтобы пользователь не путал проживание, перелеты и combined context.
- Offer details открывается из карточки результата или из saved/shortlisted results.
- Comparison view открывается из results overview, offer details, chat-команды сравнения или saved list.
- Saved/shortlisted results доступны из active session и из offer details.
- Возврат к сравнению должен быть возможен из offer details и saved/shortlisted results.
- Возврат к уточнению запроса происходит через chat-команду или редактирование/уточнение параметров в текущей session.

Базовая карта переходов:

```text
Start / Entry
  -> AI Chat Interface
  -> Clarification Area
  -> Loading State
  -> Results Overview
      -> Hotel Result Cards
      -> Flight Result Cards
      -> Offer Details
          -> Save / Shortlist
          -> Comparison View
          -> Results Overview
      -> Comparison View
      -> Saved / Shortlisted Results
  -> No Results State
  -> Error State
  -> AI Chat Interface for refinement
```

Combined search в MVP должен поддерживать как минимум Level 1 и Level 2:

- Level 1: ассистент распознает combined intent.
- Level 2: ассистент помогает искать hotel и flight в одной search session с общим контекстом.

Level 3 coordinated combined search остается open для решения Stage 3, а full package ranking не должен появляться как скрытое UX-обещание.

## 7. Основные UX-потоки

### 7.1 Первый запуск / стартовый сценарий

1. Пользователь открывает web MVP.
2. Видит рабочий chat entry point, а не marketing landing page.
3. Вводит естественный запрос: hotel, flight или combined.
4. Ассистент определяет intent и извлекает параметры.
5. Если данных достаточно, пользователь видит loading state поиска.
6. Если данных недостаточно, пользователь видит уточнение в chat и/или clarification area.

Ключевое UX-решение: первый экран должен сразу поддерживать действие "описать поездку", потому что естественный текстовый запрос является основным входом MVP.

### 7.2 Поиск отелей через AI assistant

1. Пользователь описывает отель естественным языком.
2. Ассистент определяет hotel intent.
3. Clarification area показывает найденные параметры: destination, dates/duration, guests, budget, preferences.
4. Если отсутствуют required fields, ассистент задает один приоритетный уточняющий вопрос или короткий набор связанных вопросов.
5. После достаточного уточнения появляется loading state provider search.
6. Results overview показывает hotel result cards.
7. Пользователь открывает offer details, сравнивает или сохраняет вариант.

Hotel offers должны отличаться от flight offers заголовком типа, hotel-specific полями и объяснением, связанным с локацией, ценой, качеством, amenities и unknown data.

### 7.3 Поиск авиабилетов через AI assistant

1. Пользователь описывает перелет естественным языком.
2. Ассистент определяет flight intent.
3. Clarification area показывает origin, destination, dates, passengers, baggage, stops/time preferences, budget.
4. Ассистент уточняет критичные пробелы: город отправления, даты, пассажиры, багаж или пересадки.
5. Loading state показывает, что выполняется поиск flight offers.
6. Results overview показывает flight result cards.
7. Пользователь открывает детали, сравнивает по цене/времени/пересадкам или меняет ограничения.

Flight offers должны отличаться route-first структурой, полями расписания, длительности, пересадок, багажа и цены.

### 7.4 Сравнение результатов

1. Пользователь выбирает 2-5 offers или просит "сравни эти варианты".
2. Если критерий сравнения не указан, ассистент использует явные preferences или спрашивает, что важнее.
3. Comparison view показывает trade-offs, а не только список полей.
4. Provider facts, assistant assumptions и unknown data разделяются.
5. Пользователь может перейти к offer details, сохранить один вариант или вернуться к results overview.

Сравнение в MVP является базовым: без сложных таблиц, package scoring, карты и advanced filters.

### 7.5 Просмотр деталей оффера

1. Пользователь открывает карточку из results overview, comparison view или saved list.
2. Offer details показывает provider facts и отделяет unknown fields.
3. Объяснение рекомендации связывает offer facts с исходным запросом и preferences.
4. Пользователь может сохранить offer, добавить к сравнению, вернуться к результатам или уточнить запрос через chat.

Offer details не должен обещать booking/payment, price guarantee или availability beyond provider confirmation.

### 7.6 Сохранение результата

1. Пользователь нажимает save/shortlist или просит ассистента сохранить вариант.
2. Система сохраняет offer или подборку в текущей search session.
3. Chat подтверждает сохранение.
4. Saved / Shortlisted Results показывает сохраненные элементы и предупреждает о freshness, если данные могут устареть.
5. Пользователь может вернуться к сохраненному offer, сравнению или уточнению запроса.

В MVP сохранение ограничено текущей session. Долгосрочное хранение с аккаунтом остается Post-MVP/Open.

### 7.7 Возврат к предыдущему поиску / уточнение запроса

1. Пользователь просит вернуться к текущему поиску или меняет часть условий.
2. Ассистент показывает восстановленные параметры, сохраненные offers и unknown/stale data.
3. Если пользователь меняет даты, бюджет, район, пересадки или baggage, система явно показывает, какие results устарели.
4. При необходимости поиск повторяется.
5. Пользователь возвращается к results overview, comparison view или saved list.

MVP поддерживает возврат в рамках текущей search session. Полноценный список прошлых поисков, поиск по истории и синхронизация между устройствами остаются Post-MVP.

## 8. Состояния экранов

| State | Где применяется | UX-смысл | MVP expectation |
|---|---|---|---|
| Empty | Start, results area, saved list | Пользователь еще не начал поиск или ничего не сохранил | Показать возможность начать через chat |
| Intent detected | Chat, clarification area | Ассистент понял hotel/flight/combined/compare/save/resume intent | Показать найденный тип запроса |
| Missing required data | Chat, clarification area | Нужны параметры до поиска | Задать минимально достаточное уточнение |
| Ready to search | Chat, clarification area | Данных достаточно для provider search | Показать summary параметров и перейти к loading |
| Loading: assistant thinking | Chat | Ассистент интерпретирует запрос или формирует ответ | Не обещать результаты до provider facts |
| Loading: provider search | Results area | Выполняется поиск offers | Явно показать, что идет поиск вариантов |
| Partial data | Results, offer details, comparison | Provider вернул неполные facts | Пометить unknown fields |
| Results available | Results overview | Offers найдены и ранжированы | Разделить hotel и flight offers |
| No results | Results area, chat | Поиск выполнен, но offers не найдены | Объяснить и предложить 1-3 изменения constraints |
| Provider error | Error state, chat | Источник данных недоступен или вернул ошибку | Отличать от no results и предложить retry/fallback |
| Contradictory request | Chat, clarification area | Constraints конфликтуют | Назвать конфликт и предложить ослабление |
| Saved | Offer details, saved list, chat | Offer или подборка сохранены | Подтвердить и показать доступ к shortlist |
| Stale data | Saved list, resume, details | Данные могут быть устаревшими | Не подавать старую цену/availability как актуальную |
| Unsupported action | Chat | Пользователь просит booking/payment/legal/unsupported action | Безопасно отказать и предложить поддерживаемый шаг |

## 9. MVP vs Post-MVP

| Область | MVP | Post-MVP / Open |
|---|---|---|
| Entry point | AI chat как главный вход | Мультиканальные entry points, voice, attachments |
| Results display | Structured results рядом с chat или под ним | Сложные dashboards, карта, календарь цен |
| Hotel search | Hotel result cards и details | Расширенные фильтры, reviews deep dive, room-level booking |
| Flight search | Flight result cards и details | Fare rules deep dive, seat selection, ticketing |
| Combined search | Level 1 и Level 2 | Level 3 open for Stage 3; Level 4 package ranking Post-MVP/Open |
| Comparison | Базовое сравнение 2-5 offers | Advanced comparison matrices, weighted scoring UI |
| Save/shortlist | В текущей search session | Account-level storage, долгосрочная история, sync |
| Resume | Текущая session | История всех поисков, поиск по истории, multi-device resume |
| Provider data | Real provider/API facts for final MVP | Production-hardening, adapter taxonomy, SLA/error hardening |
| Booking/payment | Безопасный отказ | Booking, payment, refunds, legal workflows |
| UX design | Screen map and navigation model | Visual design, component system, pixel-perfect layouts |

## 10. Открытые вопросы

- Нужно ли включать Level 3 coordinated combined search в MVP, и если да, какой минимальный UX должен показывать coordination без обещания full package ranking?
- Какие required fields считаются минимальными для hotel search, flight search и combined intent?
- Какой объем open destination discovery входит в MVP: только уточнение или поиск направлений при поддержке provider capabilities?
- Каким должен быть MVP-уровень session persistence без авторизации?
- Какие freshness/source markers должны быть видны пользователю на уровне UX, если provider/API возвращает такие данные?

## 11. Что не входит в этот шаг

В этот шаг намеренно не входит:

- визуальный дизайн, цвета, типографика и дизайн-система;
- high-fidelity wireframes или pixel-perfect layout;
- React/Next.js/Kotlin/Ktor код;
- API contracts, DTO, OpenAPI, database schema и provider adapter design;
- LLM prompt engineering, tool calling и orchestration implementation;
- финализация всех MVP acceptance criteria;
- изменение Stage 0-2 документов;
- закрытие всего Stage 3;
- начало Stage 4 как отдельного Visual Design / UI Concept этапа;
- booking, payment, account-level storage, mobile app и production-hardening.
