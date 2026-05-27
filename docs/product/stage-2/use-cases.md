# Stage 2 — Use Cases

## Цель документа

Развернуть сценарии Stage 1 в проверяемые use cases без перехода к API contracts, DTO, database schema, UI-макетам, prompt engineering или реализации providers.

Stage 2 учитывает Stage 1 Scope Correction: интеграция с существующим travel API входит в MVP, но контракт API еще не предоставлен. Поэтому use cases описывают продуктовое поведение и работу с provider facts, assistant assumptions и unknown data, не проектируя контракт или adapters.

## Статусы

- **In MVP:** поведение нужно для финального MVP.
- **Post-MVP:** поведение важно, но не нужно для первого MVP.
- **Open:** требуется решение Stage 3 или будущего технического этапа.

## UC-01. Hotel search by natural language request

**Связанные scenario IDs:** S-01.

**Связанные BR IDs:** BR-001, BR-003, BR-004, BR-007, BR-011, BR-015.

**Связанные FR IDs:** FR-001, FR-002, FR-003, FR-004, FR-006, FR-007, FR-008, FR-014.

**Актор:** пользователь Travel Assistant.

**Цель пользователя:** получить короткий список подходящих отелей по естественному запросу.

**Предусловия:** пользователь находится в search session; booking/payment не выполняются; provider/API data рассматривается как source of truth для offers.

**Триггер:** пользователь просит подобрать отель.

**Основной поток:**
1. Ассистент определяет hotel intent.
2. Ассистент извлекает направление, даты или длительность, гостей, бюджет, район, качество и preferences.
3. Если обязательных параметров не хватает, ассистент задает уточнение.
4. Когда данных достаточно, ассистент получает hotel offers через provider abstraction.
5. В финальном MVP offers должны приходить из существующего travel API по предоставленному контракту.
6. Ассистент нормализует продуктовый вид результата, ранжирует offers и объясняет выбор.

**Альтернативные потоки:** пользователь задает гибкие даты; бюджет задан словами; слишком много вариантов; provider возвращает частичные данные.

**Edge cases:** EC-001, EC-004, EC-010, EC-014, EC-018, EC-021.

**Ответственность ассистента:** не выдумывать цены, availability, rating или amenities; отделять provider facts от assumptions; показывать unknown data.

**Нужные данные:** destination, dates/duration, guests, budget или price preference, hotel preferences, provider facts, data freshness.

**Ожидаемый результат:** короткий список hotel offers с причинами рекомендации и явными ограничениями данных.

**MVP status:** In MVP.

**Заметки:** API contract не проектируется на Stage 2.

## UC-02. Flight search by natural language request

**Связанные scenario IDs:** S-02.

**Связанные BR IDs:** BR-001, BR-003, BR-004, BR-008, BR-011, BR-015.

**Связанные FR IDs:** FR-001, FR-002, FR-003, FR-005, FR-006, FR-007, FR-008, FR-014.

**Актор:** пользователь Travel Assistant.

**Цель пользователя:** найти удобный перелет по маршруту, датам, бюджету и ограничениям.

**Предусловия:** search session активна; provider facts являются источником данных о расписании, цене, багаже и availability.

**Триггер:** пользователь просит подобрать перелет.

**Основной поток:**
1. Ассистент определяет flight intent.
2. Ассистент извлекает origin, destination, dates, passengers, budget, stops, baggage и time preferences.
3. Ассистент уточняет критичные пробелы.
4. Ассистент получает flight offers через provider abstraction.
5. В финальном MVP flight offers должны приходить из существующего travel API по предоставленному контракту.
6. Ассистент ранжирует варианты по цене, длительности, пересадкам, времени и ограничениям.

**Альтернативные потоки:** "на выходные" без дат; прямых рейсов нет; багаж неизвестен; бюджет конфликтует с датами.

**Edge cases:** EC-002, EC-006, EC-011, EC-014, EC-019, EC-022.

**Ответственность ассистента:** не обещать цену или наличие без provider confirmation; объяснять trade-off между ценой и удобством.

**Нужные данные:** origin, destination, dates, passengers, baggage preference, stops preference, price, currency, freshness.

**Ожидаемый результат:** несколько flight offers с объяснением и ограничениями.

**MVP status:** In MVP.

**Заметки:** live pricing guarantees и booking не входят в MVP.

## UC-03. Combined search request

**Связанные scenario IDs:** S-03.

**Связанные BR IDs:** BR-001, BR-002, BR-003, BR-009, BR-011, BR-015.

**Связанные FR IDs:** FR-001, FR-002, FR-003, FR-004, FR-005, FR-006, FR-007, FR-008, FR-013.

**Актор:** пользователь Travel Assistant.

**Цель пользователя:** подобрать поездку как связку перелета и проживания с общими ограничениями.

**Предусловия:** пользователь описывает поездку целиком; уровень combined search должен соответствовать решению Stage 2/3.

**Триггер:** запрос вида "хочу слетать на 5 дней куда-нибудь в теплое место до 1000 евро на двоих".

**Основной поток:**
1. Ассистент распознает combined intent.
2. Ассистент выделяет shared constraints: origin, destination или destination type, dates/duration, total budget, travelers.
3. Ассистент отделяет hotel preferences от flight preferences.
4. Ассистент уточняет критичные пробелы.
5. Для MVP минимум: поддерживает один диалог с hotel и flight assistance в общем контексте.
6. Coordinated search может быть In MVP или Open до Stage 3.
7. Full package ranking как единая сущность остается Post-MVP или Open.

**Альтернативные потоки:** направление открыто; хватает данных только для одного типа поиска; общий бюджет нужно разложить на flight/hotel assumptions.

**Edge cases:** EC-003, EC-007, EC-008, EC-012, EC-020, EC-025.

**Ответственность ассистента:** явно показывать уровень поддержки combined search; не выдавать loosely related hotel и flight results как проверенный package без данных.

**Нужные данные:** shared dates, origin, destination, duration, travelers, total budget, flight/hotel constraints, unresolved constraints.

**Ожидаемый результат:** пользователь понимает, что ассистент может сделать в рамках combined request и какие ограничения остаются.

**MVP status:** Open частично: Level 1 и Level 2 In MVP; Level 3 Open for Stage 3; Level 4 Post-MVP/Open.

**Заметки:** см. `combined-search-levels.md`.

## UC-04. Missing information clarification

**Связанные scenario IDs:** S-04.

**Связанные BR IDs:** BR-001, BR-002, BR-005.

**Связанные FR IDs:** FR-001, FR-002, FR-003, FR-013, FR-014.

**Актор:** пользователь.

**Цель пользователя:** продолжить диалог без заполнения большой формы.

**Предусловия:** запрос неполный или содержит расплывчатые параметры.

**Триггер:** ассистент обнаруживает критичный missing field.

**Основной поток:**
1. Ассистент определяет intent и найденные параметры.
2. Ассистент классифицирует missing fields как required, useful или optional.
3. Ассистент задает один приоритетный вопрос или короткий набор связанных вопросов.
4. Ответ пользователя обновляет search session.

**Альтернативные потоки:** пользователь не знает точные даты; пользователь хочет оставить бюджет гибким; intent неясен.

**Edge cases:** EC-001, EC-002, EC-003, EC-004, EC-005.

**Ответственность ассистента:** не блокировать сценарий необязательными вопросами; не делать поиск без required fields.

**Нужные данные:** intent type, extracted parameters, missing required parameters, allowed flexibility.

**Ожидаемый результат:** ассистент получает достаточно данных для поиска или честно фиксирует unknown data.

**MVP status:** In MVP.

**Заметки:** required fields финализируются на Stage 3.

## UC-05. Compare offers

**Связанные scenario IDs:** S-05.

**Связанные BR IDs:** BR-004, BR-007, BR-008, BR-010, BR-011.

**Связанные FR IDs:** FR-008, FR-009, FR-011, FR-014.

**Актор:** пользователь.

**Цель пользователя:** понять trade-off между несколькими offers.

**Предусловия:** есть 2-5 offers или saved candidates.

**Триггер:** пользователь просит "сравни", "какой лучше" или выбирает несколько вариантов.

**Основной поток:** ассистент определяет критерии сравнения, использует provider facts, отмечает unknown fields и формирует короткий вывод.

**Альтернативные потоки:** критерии не указаны; данные неполные; варианты принадлежат разным типам.

**Edge cases:** EC-017, EC-021, EC-026.

**Ответственность ассистента:** сравнивать только проверяемые параметры; assumptions показывать отдельно.

**Нужные данные:** comparison candidates, user priorities, provider facts, unknown fields.

**Ожидаемый результат:** пользователь понимает различия и следующий шаг.

**MVP status:** In MVP в базовом виде.

**Заметки:** сложная таблица сравнения или UX не проектируется.

## UC-06. Explain recommendation

**Связанные scenario IDs:** S-06.

**Связанные BR IDs:** BR-004, BR-007, BR-008, BR-010.

**Связанные FR IDs:** FR-008, FR-014.

**Актор:** пользователь.

**Цель пользователя:** понять, почему вариант рекомендован.

**Предусловия:** есть один или несколько offers и исходный запрос.

**Триггер:** ассистент показывает рекомендацию или пользователь спрашивает "почему".

**Основной поток:** ассистент связывает provider facts с требованиями пользователя, показывает плюсы, компромиссы, unknown data и assumptions.

**Альтернативные потоки:** данных provider недостаточно; вариант нарушает одно из требований; пользователь спрашивает про нерекомендованный offer.

**Edge cases:** EC-018, EC-023, EC-024, EC-026.

**Ответственность ассистента:** не использовать LLM как источник travel facts; не скрывать неопределенность.

**Нужные данные:** original request, user preferences, offer facts, ranking reasons, unknown fields.

**Ожидаемый результат:** проверяемое объяснение без галлюцинаций.

**MVP status:** In MVP.

**Заметки:** prompt или chain-of-thought не описываются.

## UC-07. Save selected result

**Связанные scenario IDs:** S-07.

**Связанные BR IDs:** BR-013, BR-016.

**Связанные FR IDs:** FR-010, FR-011.

**Актор:** пользователь.

**Цель пользователя:** сохранить вариант или подборку в рамках поддерживаемого объема.

**Предусловия:** есть найденный offer или comparison set.

**Триггер:** пользователь говорит "сохрани этот вариант".

**Основной поток:** ассистент фиксирует selected offer в search session, подтверждает сохранение и помечает, какие данные могут устареть.

**Альтернативные потоки:** пользователь хочет долгосрочное хранение; offer больше не актуален; нужно сохранить несколько вариантов.

**Edge cases:** EC-027, EC-028.

**Ответственность ассистента:** не обещать долгосрочную историю, если она не поддержана; предупреждать про freshness.

**Нужные данные:** selected offer, session id or context, provider facts snapshot, freshness.

**Ожидаемый результат:** пользователь может вернуться к сохраненному объекту в текущей search session.

**MVP status:** In MVP для текущей search session; Post-MVP для аккаунта и долгосрочной истории.

**Заметки:** storage design не описывается.

## UC-08. Resume current search session

**Связанные scenario IDs:** S-08.

**Связанные BR IDs:** BR-012, BR-013, BR-016.

**Связанные FR IDs:** FR-011, FR-014.

**Актор:** пользователь.

**Цель пользователя:** продолжить поиск без повторного ввода параметров.

**Предусловия:** есть текущая search session или сохраненный контекст.

**Триггер:** пользователь просит вернуться к поиску.

**Основной поток:** ассистент показывает восстановленные параметры, selected offers, unknown/stale fields и предлагает продолжить.

**Альтернативные потоки:** доступна только текущая сессия; есть несколько похожих поисков; provider facts устарели.

**Edge cases:** EC-027, EC-028, EC-029.

**Ответственность ассистента:** не подавать старые цены и availability как актуальные без provider refresh.

**Нужные данные:** current user goal, extracted parameters, selected offers, rejected offers, freshness.

**Ожидаемый результат:** пользователь продолжает сценарий с прозрачным состоянием данных.

**MVP status:** In MVP для текущей search session; Post-MVP для долгосрочной истории.

**Заметки:** авторизация остается Open/Post-MVP.

## UC-09. No results found

**Связанные scenario IDs:** S-09.

**Связанные BR IDs:** BR-005, BR-010, BR-015.

**Связанные FR IDs:** FR-012, FR-013, FR-014.

**Актор:** пользователь.

**Цель пользователя:** понять, почему не найдено вариантов, и что изменить.

**Предусловия:** поиск был возможен, но provider или фильтрация не дали подходящих offers.

**Триггер:** provider возвращает пустой список или ranking отфильтровал все offers.

**Основной поток:** ассистент отличает empty result от provider error, объясняет возможные причины и предлагает 1-3 изменения constraints.

**Альтернативные потоки:** provider недоступен; данные неполные; constraints нереалистичны.

**Edge cases:** EC-014, EC-015, EC-020, EC-024.

**Ответственность ассистента:** не выдумывать market facts; гипотезы обозначать как assumptions.

**Нужные данные:** search constraints, provider response status, empty result reason if available, rejected constraints.

**Ожидаемый результат:** понятный fallback и следующий шаг.

**MVP status:** In MVP.

**Заметки:** классификация технических ошибок относится к будущим этапам.

## UC-10. Contradictory request

**Связанные scenario IDs:** S-10.

**Связанные BR IDs:** BR-002, BR-005, BR-006, BR-010.

**Связанные FR IDs:** FR-002, FR-003, FR-013, FR-014.

**Актор:** пользователь.

**Цель пользователя:** понять конфликт и изменить запрос.

**Предусловия:** запрос содержит конфликтующие constraints.

**Триггер:** ассистент обнаруживает конфликт до поиска или после provider result.

**Основной поток:** ассистент называет конфликт, отделяет confirmed provider facts от assumptions, предлагает ослабить конкретные constraints.

**Альтернативные потоки:** пользователь настаивает; конфликт нельзя подтвердить без provider; есть редкие варианты с рисками.

**Edge cases:** EC-009, EC-010, EC-011, EC-012, EC-025.

**Ответственность ассистента:** не делать категоричных утверждений без данных; не скрывать конфликт.

**Нужные данные:** constraints, market/provider facts if available, assumptions, suggested relaxations.

**Ожидаемый результат:** пользователь понимает реалистичные варианты продолжения.

**MVP status:** In MVP.

**Заметки:** conflict thresholds уточняются на Stage 3.

## UC-11. Open destination request

**Связанные scenario IDs:** S-03, S-04.

**Связанные BR IDs:** BR-001, BR-002, BR-003, BR-005, BR-009.

**Связанные FR IDs:** FR-001, FR-002, FR-003, FR-013, FR-014.

**Актор:** пользователь.

**Цель пользователя:** получить варианты, когда направление задано как "куда-нибудь".

**Предусловия:** destination неизвестен, но есть тип отдыха, климат, бюджет, origin или период.

**Триггер:** запрос "куда-нибудь в теплое место", "на море недорого".

**Основной поток:** ассистент определяет open destination, уточняет origin/period/budget и предлагает сузить регион или критерии.

**Альтернативные потоки:** пользователь хочет, чтобы ассистент сам предложил направления; provider не поддерживает destination discovery; данных недостаточно.

**Edge cases:** EC-007, EC-008, EC-023.

**Ответственность ассистента:** не генерировать реальные цены и availability без provider facts; явно помечать предложения как assumptions, если provider data отсутствует.

**Нужные данные:** origin, travel period, budget, destination preferences, flexibility, provider capabilities.

**Ожидаемый результат:** направление уточнено или сформирован безопасный следующий шаг.

**MVP status:** Open: clarification In MVP; полноценный destination discovery зависит от Stage 3/provider capabilities.

**Заметки:** не добавляет новый BR/FR; следует из S-03/S-04/Q-005.

## UC-12. Change constraints during search

**Связанные scenario IDs:** S-05, S-08, S-10.

**Связанные BR IDs:** BR-002, BR-004, BR-011, BR-013.

**Связанные FR IDs:** FR-002, FR-007, FR-009, FR-011, FR-013.

**Актор:** пользователь.

**Цель пользователя:** изменить бюджет, даты, район, пересадки или preferences без начала заново.

**Предусловия:** есть активная search session.

**Триггер:** пользователь говорит "а если бюджет до 1200", "покажи без пересадок", "добавь багаж".

**Основной поток:** ассистент обновляет параметры, объясняет влияние изменений и при необходимости повторяет поиск через provider abstraction.

**Альтернативные потоки:** изменение конфликтует с выбранным offer; нужно сбросить часть результатов; provider facts устарели.

**Edge cases:** EC-029, EC-030.

**Ответственность ассистента:** показывать, какие параметры изменены, какие сохранены, какие offers нужно перепроверить.

**Нужные данные:** previous constraints, updated constraints, affected offers, freshness.

**Ожидаемый результат:** search session обновлена без потери контекста.

**MVP status:** In MVP для текущей session в базовом виде.

**Заметки:** сложная история версий Post-MVP/Open.

## UC-13. Provider returns partial data

**Связанные scenario IDs:** S-06, S-09.

**Связанные BR IDs:** BR-010, BR-015.

**Связанные FR IDs:** FR-006, FR-008, FR-012, FR-014.

**Актор:** provider/API как источник данных; пользователь получает результат через ассистента.

**Цель пользователя:** получить честный результат даже при неполных provider facts.

**Предусловия:** provider вернул offers с missing fields.

**Триггер:** часть данных отсутствует: baggage, cancellation policy, rating, freshness, amenities.

**Основной поток:** ассистент отображает доступные provider facts, помечает unknown fields и не использует unknown как основание для уверенной рекомендации.

**Альтернативные потоки:** отсутствует обязательное поле; данные противоречивы; explainability невозможна.

**Edge cases:** EC-016, EC-017, EC-018, EC-021, EC-024.

**Ответственность ассистента:** не заполнять пропуски догадками; предложить уточнить/перепроверить, если поле критично.

**Нужные данные:** provider facts, missing fields, field importance, freshness.

**Ожидаемый результат:** пользователь видит ограничения данных и может принять решение.

**MVP status:** In MVP.

**Заметки:** конкретные маппинги полей зависят от будущего API-контракта.

## UC-14. User asks for unsupported action

**Связанные scenario IDs:** S-01, S-02, S-03.

**Связанные BR IDs:** BR-014.

**Связанные FR IDs:** FR-001, FR-013, FR-014.

**Актор:** пользователь.

**Цель пользователя:** выполнить действие за пределами MVP, например booking или payment.

**Предусловия:** пользователь уже нашел offer или просит действие напрямую.

**Триггер:** "забронируй", "оплати", "гарантируй цену", "оформи возврат".

**Основной поток:** ассистент объясняет, что действие не поддержано в MVP, не совершает юридически значимых действий и предлагает безопасный следующий шаг.

**Альтернативные потоки:** пользователь просит visa/legal advice; refund policy не подтверждена provider facts.

**Edge cases:** EC-031, EC-032, EC-033, EC-034, EC-035.

**Ответственность ассистента:** не обещать booking/payment/guarantees; не интерпретировать policy без данных.

**Нужные данные:** unsupported action type, selected offer if any, known provider facts.

**Ожидаемый результат:** безопасный отказ с полезным продолжением в рамках MVP.

**MVP status:** In MVP как fallback; сами booking/payment Post-MVP.

**Заметки:** не расширяет MVP.

## UC-15. Real provider/API data used as source of travel offers

**Связанные scenario IDs:** S-01, S-02, S-03, S-06, S-09.

**Связанные BR IDs:** BR-003, BR-007, BR-008, BR-010, BR-015.

**Связанные FR IDs:** FR-004, FR-005, FR-006, FR-008, FR-012, FR-014.

**Актор:** пользователь; provider/API как источник travel facts.

**Цель пользователя:** получать реальные travel offers, а не LLM-generated facts.

**Предусловия:** финальный MVP имеет доступ к предоставленному контракту существующего travel API; до этого используются provider abstractions, mock/fake providers и contract placeholders только как промежуточные средства.

**Триггер:** любой supported search, требующий offers.

**Основной поток:**
1. Ассистент извлекает параметры и проверяет, что поиск возможен.
2. Offers поступают из provider/API data source через утвержденные абстракции.
3. LLM/assistant интерпретирует, структурирует, сравнивает и объясняет provider data.
4. LLM/assistant не генерирует цены, availability, schedules, baggage rules или hotel facts.
5. Unknown, stale или partial data явно помечаются.

**Альтернативные потоки:** API contract еще не предоставлен; provider data неполная; provider unavailable; freshness неизвестна.

**Edge cases:** EC-013, EC-014, EC-016, EC-017, EC-018, EC-019, EC-021, EC-024.

**Ответственность ассистента:** provider facts считать primary source of truth; assumptions хранить отдельно; не подменять отсутствие данных уверенностью.

**Нужные данные:** provider facts, source/provider, freshness, unknown fields, assistant assumptions.

**Ожидаемый результат:** финальный MVP использует реальные travel offers из существующего API/provider.

**MVP status:** In MVP.

**Заметки:** Stage 2 не проектирует API contract, endpoints, DTO, database schema или adapter implementation.
