# Stage 3.2 — Required Fields & Acceptance Criteria

## 1. Назначение документа

Документ фиксирует required fields и UX/product acceptance criteria для MVP search flows Travel Assistant.

Цель документа — определить, когда AI assistant может запускать поиск, когда обязан задать уточняющий вопрос, какие данные можно считать optional или derived, и какие критерии подтверждают корректную работу MVP flow.

Документ не является API-контрактом, DTO schema, database schema, prompt engineering, технической архитектурой или UI-дизайном.

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
- `docs/product/stage-2/edge-cases.md`;
- `docs/product/stage-2/assistant-behaviour-rules.md`;
- `docs/product/stage-2/combined-search-levels.md`;
- `docs/product/stage-2/data-requirements.md`;
- `docs/product/stage-3/screen-map.md`.

Связанный следующий документ Stage 3: `docs/product/stage-3/mvp-search-flow-details.md`.

Ограничения:

- MVP работает через естественный текстовый запрос и AI clarification flow.
- Provider/API data является primary source of truth для цен, availability, расписаний, параметров offers, ограничений и freshness.
- LLM/assistant может интерпретировать, уточнять, объяснять и сравнивать, но не должен выдумывать travel facts.
- Финальный MVP должен использовать предоставленный контракт существующего travel API, но этот документ не проектирует контракт.
- Booking, payment, guarantees, visa/legal advice как обязательная функция, long-term account history и mobile app не входят в MVP.
- Stage 4 Visual Design / UI Concept не начинается в рамках этого документа.

## 3. Термины и уровни обязательности

**Required field** — поле, без которого search flow нельзя запускать надежно. Если required field отсутствует и не может быть безопасно derived из контекста, ассистент задает уточнение.

**Optional field** — поле, которое улучшает ranking, explanation или filtering, но отсутствие которого не блокирует search. Optional field может быть уточнено, если пользователь явно делает его критичным.

**Derived / inferred field** — поле, которое ассистент может вывести из текста пользователя или текущей search session. Derived field должно быть видно пользователю как извлеченное значение или assumption, если оно влияет на поиск.

**Unknown data** — данные, которые отсутствуют, неполны, устарели или не подтверждены provider/API data. Unknown data нельзя превращать в уверенный provider fact.

**Minimum search condition** — минимальный набор required fields, при котором можно запустить provider search без случайных или непроверяемых результатов.

## 4. Общие UX-принципы сбора данных

- Ассистент сначала определяет intent: hotel, flight, combined, open destination, compare, save, resume или unsupported action.
- Ассистент не требует все возможные параметры сразу.
- За один шаг ассистент задает один приоритетный вопрос или короткий набор связанных вопросов, если без них search будет ненадежным.
- Если missing required fields принадлежат одной теме, их можно уточнить вместе: например dates + duration или guests + rooms.
- Если пользователь дал слишком общий запрос, ассистент уточняет intent и минимальный набор required fields, а не запускает широкий search без рамок.
- Если пользователь дал противоречивые параметры, ассистент называет конфликт и предлагает ослабить конкретные constraints до запуска поиска или перед повторным поиском.
- Если часть данных можно вывести из контекста, ассистент использует их как derived fields и показывает, что было принято из текущей session.
- Если пользователь не хочет уточнять required fields, ассистент честно объясняет, что надежный search невозможен, и предлагает безопасный fallback: сузить запрос, выбрать flexibility или сохранить известные параметры.
- Optional preferences не должны блокировать search, если пользователь не объявил их обязательными.
- Partial results показываются только с явными unknown fields и limitations.
- No results отличается от provider/API error.
- Save / shortlist в MVP действует только в рамках текущей search session.

## 5. Hotel Search Flow

### 5.1 Required fields

| Field | Почему требуется | Можно ли derived/inferred |
|---|---|---|
| Destination | Без направления hotel search становится случайным. | Да, из запроса или combined context. |
| Check-in and check-out dates или date range + duration | Цена и availability зависят от дат. | Частично: "на выходные" можно преобразовать в assumption только после подтверждения или явной flexibility. |
| Guests count | Влияет на availability, цену и room fit. | Да, если пользователь сказал "на двоих", "семьей", "один". |
| Rooms count или room assumption | Нужен для семьи/группы и pricing. | Да, по умолчанию 1 room для 1-2 взрослых, если это явно показано как assumption. |

Minimum search condition для hotel search: destination + dates/date range + guests count + rooms count или подтвержденное room assumption.

Budget не является универсальным required field, но становится required, если пользователь явно просит "недорого", "до X", "лучшее за бюджет" или если без бюджета ranking будет слишком широким для полезного результата.

### 5.2 Optional fields

| Field | Как влияет на UX/result |
|---|---|
| Budget / price preference | Улучшает ranking и no-results fallback. |
| Hotel category | Помогает фильтровать звездность/уровень. |
| Location preference | Помогает выбрать район, центр, пляж, аэропорт. |
| Amenities | Используется как preference или hard constraint, если пользователь говорит "обязательно". |
| Meal plan | Optional для MVP; влияет на сравнение, если provider facts есть. |
| Cancellation flexibility | Optional; нельзя додумывать, если provider не вернул facts. |
| Accessibility needs | Optional по форме, но становится required для запуска релевантного поиска, если пользователь обозначил это как обязательное условие. |

### 5.3 Derived / inferred fields

- Intent: hotel search.
- Destination из фразы "отель в Барселоне".
- Dates/duration из "на майские", "на выходные", "на неделю" как assumption, если даты неоднозначны.
- Guests count из "на двоих", "с ребенком", "один".
- Budget tier из "недорого", "комфортно", "премиально" как assistant assumption, а не provider fact.
- Location preference из "рядом с центром", "у моря", "возле аэропорта".
- Hard constraints из слов "обязательно", "только", "без".

### 5.4 Missing data behaviour

- Если нет destination, ассистент уточняет город/регион или переводит сценарий в open destination discovery.
- Если нет дат или duration, ассистент уточняет период до provider search.
- Если нет guests count, ассистент уточняет состав путешественников.
- Если rooms count не указан, ассистент может предложить assumption и дать пользователю подтвердить.
- Если budget не указан, ассистент может начать search только если остальные fields достаточны и пользователь не сделал цену ключевым критерием.
- Если пользователь не хочет уточнять даты, ассистент предлагает date flexibility как явный constraint; без периода или flexibility поиск не запускается.
- Если required fields противоречат друг другу, ассистент не скрывает конфликт и предлагает ослабить constraints.

### 5.5 Acceptance criteria

- Given пользователь просит "Найди отель в Барселоне на 10-14 июня для двоих", When ассистент извлекает destination, dates и guests, Then hotel search может быть запущен без дополнительного required clarification.
- Given пользователь просит "Найди отель в Барселоне", When даты не указаны, Then ассистент не запускает provider search и уточняет даты или допустимую гибкость.
- Given пользователь просит "Хочу хороший отель в центре недорого", When destination и даты отсутствуют, Then ассистент уточняет destination/date fields и может предложить интерпретацию "недорого" как assumption.
- Given provider возвращает hotel offers с missing cancellation policy, When results отображаются, Then cancellation policy показывается как unknown data и не используется как уверенная причина рекомендации.
- Given пользователь просит "обязательно доступная среда", When accessibility needs стали hard constraint, Then ассистент не должен показывать offers без подтвержденных accessibility facts как полностью подходящие.
- Given provider возвращает пустой hotel result, When search был запущен с required fields, Then ассистент показывает no results state и предлагает 1-3 конкретных изменения constraints.

## 6. Flight Search Flow

### 6.1 Required fields

| Field | Почему требуется | Можно ли derived/inferred |
|---|---|---|
| Origin | Без города/аэропорта отправления нельзя найти route. | Да, из профиля/контекста только если он доступен в session и показан пользователю. |
| Destination | Без пункта назначения route не определен. | Да, из запроса или combined context. |
| Departure date или date range | Цена и расписание зависят от даты. | Частично, если пользователь задал flexible period. |
| One-way / round-trip intent | Нужно понять, требуется ли return date. | Да, из "туда-обратно", "в одну сторону", "слетать". |
| Return date или trip duration для round-trip | Нужны для обратного сегмента. | Да, из "на 5 дней", "на неделю". |
| Passengers count | Влияет на цену и availability. | Да, из "на двоих", "с ребенком", "один". |

Minimum search condition для flight search:

- one-way: origin + destination + departure date/date range + passengers count;
- round-trip: origin + destination + departure date/date range + return date/duration + passengers count.

### 6.2 Optional fields

| Field | Как влияет на UX/result |
|---|---|
| Cabin class | Default может быть economy, если пользователь не указал и это явно не критично. |
| Budget | Улучшает ranking и no-results fallback. |
| Baggage | Важный preference; становится hard constraint, если пользователь говорит "с багажом обязательно". |
| Preferred airlines | Улучшает ranking/filtering, если provider facts доступны. |
| Direct flight preference | Optional, но hard constraint при "только прямой". |
| Departure time preference | Улучшает ranking и сравнение. |
| Airport preference | Optional, если город имеет несколько аэропортов. |

### 6.3 Derived / inferred fields

- Intent: flight search.
- Round-trip из "слетать", "на выходные", "туда-обратно".
- One-way из "в одну сторону".
- Return date из departure date + duration.
- Passengers count из "на двоих", "с семьей".
- Direct flight preference из "без пересадок".
- Baggage preference из "с чемоданом", "только ручная кладь".
- Time preference из "утром", "после работы", "ночной рейс".

### 6.4 Missing data behaviour

- Если нет origin, ассистент уточняет город отправления.
- Если нет destination, ассистент уточняет destination или переводит запрос в open destination discovery.
- Если нет departure date/date range, ассистент уточняет период.
- Если round-trip intent есть, но нет return date/duration, ассистент уточняет duration или дату возврата.
- Если passengers count отсутствует, ассистент уточняет пассажиров до search.
- Если direct flight preference может сделать поиск пустым, ассистент может запустить поиск после required fields, но должен показать no-results/alternative state без выдумывания facts.
- Если пользователь не хочет уточнять required dates, ассистент предлагает flexible date range; без date range или flexibility search не запускается.

### 6.5 Acceptance criteria

- Given пользователь просит "Найди рейс из Москвы в Рим 10 июня обратно 15 июня на двоих", When ассистент извлекает route, dates, round-trip и passengers, Then flight search может быть запущен.
- Given пользователь просит "Хочу в Рим на выходные", When origin отсутствует, Then ассистент уточняет origin и сохраняет destination/period как known parameters.
- Given пользователь просит "Билет в Лиссабон в июне", When duration/return date не указаны и intent выглядит round-trip, Then ассистент уточняет duration или one-way/round-trip.
- Given пользователь просит "только прямой рейс", When provider не возвращает direct offers, Then ассистент показывает no results for direct flights и предлагает ослабить direct constraint.
- Given provider не вернул baggage facts, When flight cards отображаются, Then baggage показывается как unknown data и не используется как причина "подходит с багажом".
- Given provider/API недоступен, When required fields достаточны, Then ассистент показывает provider error state, а не no results.

## 7. Combined Hotel + Flight Search Flow

### 7.1 Supported MVP levels

MVP поддерживает:

- Level 1 — Combined intent recognition: ассистент понимает, что пользователь хочет поездку как связку hotel + flight или открытую поездку.
- Level 2 — Same-dialog hotel and flight assistance: ассистент помогает подобрать flight и hotel в одной search session, переиспользуя общий контекст.

Level 3 coordinated combined search остается Open для отдельного Stage 3 решения. Full combined package ranking как единая package recommendation остается Post-MVP/Open и не должен появляться как скрытое UX-обещание.

### 7.2 Required fields

Для Level 1 recognition required только intent evidence: пользовательская формулировка должна указывать на поездку целиком, например "слетать и остановиться", "поездка на 5 дней", "перелет и отель".

Для Level 2 same-dialog assistance required fields зависят от запуска конкретного подпоиска:

| Field | Для чего требуется | Можно ли derived/inferred |
|---|---|---|
| Origin | Required для flight part и open destination discovery. | Да, из session context. |
| Destination или destination type | Required для concrete hotel/flight search; destination type допускается для open discovery. | Да, из запроса. |
| Dates/date range или duration + period | Required для hotel и flight provider search. | Да, если пользователь дал flexible period. |
| Passengers / guests count | Required для обоих подпоисков. | Да, из "на двоих", "с семьей". |
| One-way / round-trip intent | Required для flight part. | Да, из формулировки. |

Total budget не является always required для Level 2, но становится required, если пользователь просит "уложиться в X", "найти лучшее за бюджет" или сравнивать общую стоимость поездки.

### 7.3 Optional fields

| Field | Как влияет на UX/result |
|---|---|
| Total budget | Улучшает prioritization; budget split может быть assistant assumption. |
| Budget split | Optional; не должен выглядеть как provider fact. |
| Priority between hotel and flight | Помогает trade-off: дешевле лететь vs лучше жить. |
| Flexibility level | Позволяет расширить dates/destination constraints. |
| Hotel preferences | Используются в hotel part. |
| Flight preferences | Используются в flight part. |
| Travel style | Помогает open destination и ranking explanation. |

### 7.4 Derived / inferred fields

- Combined intent из запроса на поездку целиком.
- Shared dates и travelers из общей формулировки.
- Hotel-specific preferences из "у моря", "с завтраком", "тихий район".
- Flight-specific preferences из "без пересадок", "вечером", "с багажом".
- Tentative budget split как assistant assumption, если пользователь дал только total budget.
- Priority из "главное дешево долететь", "лучше отель, перелет неважен".

### 7.5 Missing data behaviour

- Если intent unclear, ассистент уточняет: hotel, flight или both.
- Если хватает данных только для одного подпоиска, ассистент может предложить начать с него и сохранить missing fields для второго.
- Если destination open, flow переходит в Open Destination Discovery.
- Если total budget задан, но split неизвестен, ассистент не выдает package price как fact; он обозначает split as assumption или уточняет приоритет.
- Если пользователь ожидает "готовый пакет", ассистент объясняет MVP-level support: separate hotel and flight assistance in one session, without full package booking/ranking.
- Если constraints conflict across hotel and flight, ассистент называет конфликт и предлагает ослабить конкретные constraints.

### 7.6 Acceptance criteria

- Given пользователь просит "Подбери поездку в Барселону на 5 дней с перелетом и отелем на двоих", When origin отсутствует, Then ассистент распознает combined intent и уточняет origin перед flight search.
- Given пользователь указал origin, destination, dates и travelers, When combined request содержит hotel и flight needs, Then ассистент может вести hotel и flight searches в одной session, не заставляя пользователя повторять shared fields.
- Given пользователь дал только total budget, When ассистент предлагает распределение между flight и hotel, Then budget split помечается как assistant assumption.
- Given пользователь просит "собери лучший пакет", When Level 4 package ranking не входит в MVP, Then ассистент явно ограничивает результат раздельными hotel/flight offers и базовым сравнением.
- Given provider вернул hotel offers, но flight provider недоступен, When combined flow продолжается, Then hotel results могут быть показаны как partial results, а flight part получает provider error state.
- Given пользователь меняет dates в combined session, When старые hotel и flight offers зависят от dates, Then ассистент помечает affected offers как stale и предлагает повторный search.

### 7.7 Level 3 coordinated search decision status

Decision status: Open.

Stage 3.2 не включает Level 3 coordinated combined search в MVP окончательно. Минимальное возможное решение для будущей задачи: разрешить limited coordination checks между hotel и flight constraints без full package ranking, booking или package price guarantee.

До отдельного решения Level 3 нельзя представлять в UX как готовые travel packages.

## 8. Open Destination Discovery

### 8.1 Required fields

Open Destination Discovery в MVP поддерживается как clarification-first flow. Полноценный provider-backed destination discovery остается Open и зависит от provider capabilities.

Required для MVP clarification:

| Field | Почему требуется | Можно ли derived/inferred |
|---|---|---|
| Origin | Нужен для реального flight feasibility и бюджета. | Да, из текущей session, если есть. |
| Approximate dates/season или travel period | Нужен для сезонности, availability и цен. | Да, из "летом", "на майские", "зимой". |
| Trip duration | Нужна для hotel nights и round-trip planning. | Да, из "на неделю", "на выходные". |
| Budget или budget flexibility | Нужен, чтобы сузить широкий discovery request. | Да, только как tier assumption после подтверждения. |
| Travel style или destination preference | Нужен, чтобы "куда-нибудь" не было бесконечно широким. | Да, из "теплое", "море", "город", "спокойно". |

### 8.2 Optional fields

| Field | Как влияет на UX/result |
|---|---|
| Climate preference | Помогает сузить направления. |
| Interests | Пляж, музеи, еда, природа, nightlife, family-friendly. |
| Region constraints | Европа, Азия, без дальних перелетов, внутри страны. |
| Visa/passport constraints | Может учитываться только как user-provided constraint; ассистент не дает legal advice. |
| Flexibility level | Позволяет расширить dates/destination options. |
| Flight duration preference | Помогает исключить слишком дальние направления. |

### 8.3 Derived / inferred fields

- Open destination intent из "куда-нибудь", "на море", "в теплое место".
- Climate preference из "тепло", "снег", "море".
- Travel style из "спокойно", "активно", "романтично", "с детьми".
- Approximate season из "летом", "на праздники".
- Region preference из упоминаний страны/части света.

### 8.4 Missing data behaviour

- Если origin отсутствует, ассистент уточняет origin до provider-backed search или реального flight feasibility.
- Если нет периода, ассистент уточняет dates/season или предлагает выбрать flexibility.
- Если нет budget, ассистент уточняет budget tier или объясняет, что предложения будут слишком широкими.
- Если нет travel style, ассистент просит выбрать 1-2 критерия.
- Если provider discovery недоступен, ассистент не выдумывает реальные prices/availability и предлагает сузить направление для обычного hotel/flight search.
- Если пользователь спрашивает про visa/passport, ассистент может учесть это как constraint со слов пользователя, но не дает юридически значимого вывода.

### 8.5 Acceptance criteria

- Given пользователь говорит "Хочу куда-нибудь в теплое место на неделю", When origin и budget отсутствуют, Then ассистент уточняет origin, period/budget и travel style before provider-backed search.
- Given пользователь указал origin, season, duration, budget и climate preference, When provider discovery capability неизвестна, Then ассистент предлагает сузить destination criteria и не генерирует реальные prices/availability.
- Given пользователь указал "без визы для моего паспорта", When visa/passport facts не подтверждены официальным источником, Then ассистент не дает legal advice и помечает это как user-provided constraint/Open.
- Given open destination request становится concrete destination, When destination выбран или подтвержден, Then flow переходит в hotel, flight или combined search с сохранением shared context.
- Given constraints слишком широкие, When ассистент не может построить полезный shortlist, Then он просит выбрать 1-2 приоритета вместо запуска случайного search.

## 9. AI Clarification Flow

Ассистент задает уточняющий вопрос, когда:

- intent не определен;
- отсутствует required field для выбранного flow;
- пользовательский запрос противоречив;
- optional field был сформулирован как hard constraint;
- derived field может существенно изменить search и требует подтверждения;
- provider result будет слишком широким или непроверяемым без дополнительной рамки.

Ассистент может запустить поиск сразу, когда:

- intent определен;
- required fields для выбранного flow присутствуют или безопасно derived;
- assumptions явно показаны пользователю, если они влияют на поиск;
- нет unresolved contradiction, которое блокирует search;
- пользователь не просит unsupported action.

Правила уточнений:

- Не больше одного приоритетного вопроса или короткого набора связанных вопросов за шаг.
- Если missing fields много, сначала уточняется intent, затем dates/origin/destination/travelers в зависимости от flow.
- Если пользователь отказывается уточнять required field, ассистент объясняет ограничение и предлагает supported fallback.
- Если пользователь меняет параметр, ассистент показывает, какие offers стали stale.
- Если есть ambiguity, ассистент предлагает 2-3 понятные интерпретации, а не длинную форму.

Acceptance criteria:

- Given запрос "Хочу поездку в Париж", When intent unclear, Then ассистент уточняет hotel, flight или both.
- Given запрос содержит missing required fields, When ассистент отвечает, Then он не запускает provider search до получения required fields.
- Given пользователь отвечает на уточнение, When ответ заполняет missing field, Then assistant updates search session и переоценивает готовность к search.
- Given пользователь не хочет уточнять даты, When dates required для provider search, Then ассистент предлагает date flexibility или объясняет, что search не будет надежным.
- Given derived assumption используется, When оно влияет на search, Then assumption видно пользователю до или вместе с результатом.

## 10. Results, Empty, Loading and Error States

**Loading state:**

- Assistant thinking: ассистент интерпретирует запрос, извлекает fields или формулирует clarification.
- Provider search: поиск offers выполняется через provider/API data source или временный development placeholder на ранних этапах.
- Loading не должен обещать конкретные prices/availability до provider facts.

**Results state:**

- Results показывают provider facts, assistant assumptions и unknown data отдельно.
- Hotel и flight results различаются по типу offer и ключевым полям.
- Ranking explanation связывает facts с requirements/preferences.

**Empty state:**

- До первого запроса показывает возможность начать через chat.
- Для saved list показывает, что shortlist в текущей session пуст.

**No results state:**

- Используется, когда search выполнен, но offers не найдены.
- Ассистент предлагает 1-3 конкретных изменения constraints.
- No results не смешивается с provider/API error.

**Partial results state:**

- Используется, когда часть offers или частей combined flow доступна, а часть отсутствует.
- Unknown/missing provider facts явно обозначаются.
- Partial result не подается как полностью подтвержденный package.

**Error state:**

- Provider/API error объясняется отдельно от no results.
- Ассистент предлагает retry, изменение constraints или продолжение с доступной частью данных.
- Технические детали error taxonomy не проектируются в этом документе.

Acceptance criteria:

- Given provider returns empty list, When search constraints are valid, Then UI/assistant shows no results and suggests 1-3 relaxations.
- Given provider is unavailable, When search constraints are valid, Then UI/assistant shows provider error and does not claim no matching offers exist.
- Given provider returns partial hotel facts, When results are shown, Then missing fields appear as unknown data.
- Given combined flow has hotel results but flight error, When results are shown, Then hotel results are marked partial and flight part shows provider error.
- Given user changes dates after results, When previous offers depend on dates, Then affected offers are marked stale before comparison or save.

## 11. Save / Shortlist Behaviour

MVP supports save / shortlist only inside current search session.

Save can apply to:

- one hotel offer;
- one flight offer;
- a small comparison set;
- a partial combined selection, if it is clearly marked as partial and not a guaranteed package.

Save must store product-level context:

- selected offer reference inside current session;
- known provider facts snapshot;
- unknown fields;
- assistant assumptions used in explanation;
- freshness/source marker, if available;
- related search parameters.

Save must not promise:

- long-term account history;
- booking;
- payment;
- price guarantee;
- availability guarantee;
- cross-device sync.

Acceptance criteria:

- Given user selects a hotel offer, When user says "сохрани этот вариант", Then assistant confirms it is saved in current session.
- Given user asks for saved offers, When current session has saved items, Then assistant shows saved items with freshness/unknown warnings if applicable.
- Given user asks for last month's trip, When long-term history is unavailable in MVP, Then assistant explains the limitation and offers current session actions.
- Given user saves a partial combined selection, When shortlist is shown, Then it is marked as partial and not a confirmed package.
- Given saved offer has stale provider facts, When user reopens it, Then assistant does not present old price/availability as current without refresh.

## 12. MVP vs Post-MVP

| Area | MVP | Post-MVP / Open |
|---|---|---|
| Hotel search fields | Required destination, dates/date range, guests, rooms/assumption | Advanced loyalty, room-level inventory, deep policy interpretation |
| Flight search fields | Required origin, destination, dates, one-way/round-trip, passengers | Seat selection, fare-family optimization, ticketing |
| Combined search | Level 1 + Level 2 | Level 3 decision Open; Level 4 package ranking Post-MVP/Open |
| Open destination | Clarification-first flow; provider-backed discovery Open | Full destination discovery, climate/visa/provider enrichment |
| Clarification | Minimal required questions, assumptions visible | Rich preference onboarding, persistent profile |
| Results states | loading, results, no results, partial, provider error, stale | Advanced filters, maps, price calendars |
| Save / shortlist | Current session only | Account-level storage, long-term history, cross-device sync |
| Provider/API data | Real offers in final MVP via provided API contract | Production-hardening, adapter taxonomy, SLA handling |
| Unsupported actions | Safe fallback for booking/payment/legal requests | Booking, payment, refunds, legal/visa workflows |

## 13. Open Questions

- Входит ли limited Level 3 coordinated combined search в MVP, и какие exact acceptance criteria отделяют его от Level 4 package ranking?
- Какие provider capabilities будут доступны для open destination discovery?
- Какая минимальная freshness/source информация будет доступна от существующего travel API?
- Нужны ли default assumptions для rooms count, cabin class и passenger age groups, или они должны всегда подтверждаться?
- Какой минимальный session persistence уровень будет поддержан без авторизации?
- Как показывать user-provided visa/passport constraints без перехода к legal advice?

## 14. Что не входит в этот шаг

В этот шаг намеренно не входит:

- visual design, colors, typography, UI kit, wireframes или mockups;
- React/Next.js/Kotlin/Ktor код;
- API endpoints, DTO, OpenAPI, database schema или provider adapter design;
- LLM prompt engineering, tool calling или orchestration implementation;
- production error taxonomy и SLA;
- booking, payment, ticketing, refund или legal/visa advice;
- full package ranking;
- закрытие всего Stage 3;
- начало Stage 4.

## 15. Recommendations

- Stage 3.3 MVP Search Flow Details описан в `docs/product/stage-3/mvp-search-flow-details.md` на основе этих required fields.
- Отдельно принять Combined Search UX Decision по Level 3.
- После search flow details провести UX Consistency Review для Stage 3.
- На будущих технических этапах сверить required fields с предоставленным API-контрактом, не меняя этот документ задним числом без product review.
