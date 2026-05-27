# Stage 2 — Use Cases

## Цель документа

Развернуть сценарии Stage 1 в проверяемые use cases без перехода к API contracts, DTO, database schema, UI-макетам, prompt engineering или реализации providers.

Stage 2 учитывает Stage 1 Scope Correction: интеграция с существующим travel API входит в MVP, но контракт API еще не предоставлен. Поэтому use cases описывают продуктовое поведение и работу с provider facts, assistant assumptions и unknown data, не проектируя контракт или adapters.

## Статусы

- **In MVP:** поведение нужно для финального MVP.
- **Post-MVP:** поведение важно, но не нужно для первого MVP.
- **Open:** требуется решение Stage 3 или будущего технического этапа.

## UC-01. Hotel search by natural language request

**Linked scenario IDs:** S-01.

**Linked BR IDs:** BR-001, BR-003, BR-004, BR-007, BR-011, BR-015.

**Linked FR IDs:** FR-001, FR-002, FR-003, FR-004, FR-006, FR-007, FR-008, FR-014.

**Actor:** пользователь Travel Assistant.

**User goal:** получить короткий список подходящих отелей по естественному запросу.

**Preconditions:** пользователь находится в search session; booking/payment не выполняются; provider/API data рассматривается как source of truth для offers.

**Trigger:** пользователь просит подобрать отель.

**Main flow:**
1. Ассистент определяет hotel intent.
2. Ассистент извлекает направление, даты или длительность, гостей, бюджет, район, качество и preferences.
3. Если обязательных параметров не хватает, ассистент задает уточнение.
4. Когда данных достаточно, ассистент получает hotel offers через provider abstraction.
5. В финальном MVP offers должны приходить из существующего travel API по предоставленному контракту.
6. Ассистент нормализует продуктовый вид результата, ранжирует offers и объясняет выбор.

**Alternative flows:** пользователь задает гибкие даты; бюджет задан словами; слишком много вариантов; provider возвращает частичные данные.

**Edge cases:** EC-001, EC-004, EC-010, EC-014, EC-018, EC-021.

**Assistant responsibilities:** не выдумывать цены, availability, rating или amenities; отделять provider facts от assumptions; показывать unknown data.

**Data needed:** destination, dates/duration, guests, budget или price preference, hotel preferences, provider facts, data freshness.

**Expected result:** короткий список hotel offers с причинами рекомендации и явными ограничениями данных.

**MVP status:** In MVP.

**Notes:** API contract не проектируется на Stage 2.

## UC-02. Flight search by natural language request

**Linked scenario IDs:** S-02.

**Linked BR IDs:** BR-001, BR-003, BR-004, BR-008, BR-011, BR-015.

**Linked FR IDs:** FR-001, FR-002, FR-003, FR-005, FR-006, FR-007, FR-008, FR-014.

**Actor:** пользователь Travel Assistant.

**User goal:** найти удобный перелет по маршруту, датам, бюджету и ограничениям.

**Preconditions:** search session активна; provider facts являются источником данных о расписании, цене, багажe и availability.

**Trigger:** пользователь просит подобрать перелет.

**Main flow:**
1. Ассистент определяет flight intent.
2. Ассистент извлекает origin, destination, dates, passengers, budget, stops, baggage и time preferences.
3. Ассистент уточняет критичные пробелы.
4. Ассистент получает flight offers через provider abstraction.
5. В финальном MVP flight offers должны приходить из существующего travel API по предоставленному контракту.
6. Ассистент ранжирует варианты по цене, длительности, пересадкам, времени и ограничениям.

**Alternative flows:** "на выходные" без дат; прямых рейсов нет; багаж неизвестен; бюджет конфликтует с датами.

**Edge cases:** EC-002, EC-006, EC-011, EC-014, EC-019, EC-022.

**Assistant responsibilities:** не обещать цену или наличие без provider confirmation; объяснять trade-off между ценой и удобством.

**Data needed:** origin, destination, dates, passengers, baggage preference, stops preference, price, currency, freshness.

**Expected result:** несколько flight offers с объяснением и ограничениями.

**MVP status:** In MVP.

**Notes:** live pricing guarantees и booking не входят в MVP.

## UC-03. Combined search request

**Linked scenario IDs:** S-03.

**Linked BR IDs:** BR-001, BR-002, BR-003, BR-009, BR-011, BR-015.

**Linked FR IDs:** FR-001, FR-002, FR-003, FR-004, FR-005, FR-006, FR-007, FR-008, FR-013.

**Actor:** пользователь Travel Assistant.

**User goal:** подобрать поездку как связку перелета и проживания с общими ограничениями.

**Preconditions:** пользователь описывает поездку целиком; уровень combined search должен соответствовать решению Stage 2/3.

**Trigger:** запрос вида "хочу слетать на 5 дней куда-нибудь в теплое место до 1000 евро на двоих".

**Main flow:**
1. Ассистент распознает combined intent.
2. Ассистент выделяет shared constraints: origin, destination или destination type, dates/duration, total budget, travelers.
3. Ассистент отделяет hotel preferences от flight preferences.
4. Ассистент уточняет критичные пробелы.
5. Для MVP минимум: поддерживает один диалог с hotel и flight assistance в общем контексте.
6. Coordinated search может быть In MVP или Open до Stage 3.
7. Full package ranking как единая сущность остается Post-MVP или Open.

**Alternative flows:** направление открыто; хватает данных только для одного типа поиска; общий бюджет нужно разложить на flight/hotel assumptions.

**Edge cases:** EC-003, EC-007, EC-008, EC-012, EC-020, EC-025.

**Assistant responsibilities:** явно показывать уровень поддержки combined search; не выдавать loosely related hotel и flight results как проверенный package без данных.

**Data needed:** shared dates, origin, destination, duration, travelers, total budget, flight/hotel constraints, unresolved constraints.

**Expected result:** пользователь понимает, что ассистент может сделать в рамках combined request и какие ограничения остаются.

**MVP status:** Open частично: Level 1 и Level 2 In MVP; Level 3 Open for Stage 3; Level 4 Post-MVP/Open.

**Notes:** см. `combined-search-levels.md`.

## UC-04. Missing information clarification

**Linked scenario IDs:** S-04.

**Linked BR IDs:** BR-001, BR-002, BR-005.

**Linked FR IDs:** FR-001, FR-002, FR-003, FR-013, FR-014.

**Actor:** пользователь.

**User goal:** продолжить диалог без заполнения большой формы.

**Preconditions:** запрос неполный или содержит расплывчатые параметры.

**Trigger:** ассистент обнаруживает критичный missing field.

**Main flow:**
1. Ассистент определяет intent и найденные параметры.
2. Ассистент классифицирует missing fields как required, useful или optional.
3. Ассистент задает один приоритетный вопрос или короткий набор связанных вопросов.
4. Ответ пользователя обновляет search session.

**Alternative flows:** пользователь не знает точные даты; пользователь хочет оставить бюджет гибким; intent неясен.

**Edge cases:** EC-001, EC-002, EC-003, EC-004, EC-005.

**Assistant responsibilities:** не блокировать сценарий необязательными вопросами; не делать поиск без required fields.

**Data needed:** intent type, extracted parameters, missing required parameters, allowed flexibility.

**Expected result:** ассистент получает достаточно данных для поиска или честно фиксирует unknown data.

**MVP status:** In MVP.

**Notes:** required fields финализируются на Stage 3.

## UC-05. Compare offers

**Linked scenario IDs:** S-05.

**Linked BR IDs:** BR-004, BR-007, BR-008, BR-010, BR-011.

**Linked FR IDs:** FR-008, FR-009, FR-011, FR-014.

**Actor:** пользователь.

**User goal:** понять trade-off между несколькими offers.

**Preconditions:** есть 2-5 offers или saved candidates.

**Trigger:** пользователь просит "сравни", "какой лучше" или выбирает несколько вариантов.

**Main flow:** ассистент определяет критерии сравнения, использует provider facts, отмечает unknown fields и формирует короткий вывод.

**Alternative flows:** критерии не указаны; данные неполные; варианты принадлежат разным типам.

**Edge cases:** EC-017, EC-021, EC-026.

**Assistant responsibilities:** сравнивать только проверяемые параметры; assumptions показывать отдельно.

**Data needed:** comparison candidates, user priorities, provider facts, unknown fields.

**Expected result:** пользователь понимает различия и следующий шаг.

**MVP status:** In MVP в базовом виде.

**Notes:** сложная таблица сравнения или UX не проектируется.

## UC-06. Explain recommendation

**Linked scenario IDs:** S-06.

**Linked BR IDs:** BR-004, BR-007, BR-008, BR-010.

**Linked FR IDs:** FR-008, FR-014.

**Actor:** пользователь.

**User goal:** понять, почему вариант рекомендован.

**Preconditions:** есть один или несколько offers и исходный запрос.

**Trigger:** ассистент показывает рекомендацию или пользователь спрашивает "почему".

**Main flow:** ассистент связывает provider facts с требованиями пользователя, показывает плюсы, компромиссы, unknown data и assumptions.

**Alternative flows:** данных provider недостаточно; вариант нарушает одно из требований; пользователь спрашивает про нерекомендованный offer.

**Edge cases:** EC-018, EC-023, EC-024, EC-026.

**Assistant responsibilities:** не использовать LLM как источник travel facts; не скрывать неопределенность.

**Data needed:** original request, user preferences, offer facts, ranking reasons, unknown fields.

**Expected result:** проверяемое объяснение без галлюцинаций.

**MVP status:** In MVP.

**Notes:** prompt или chain-of-thought не описываются.

## UC-07. Save selected result

**Linked scenario IDs:** S-07.

**Linked BR IDs:** BR-013, BR-016.

**Linked FR IDs:** FR-010, FR-011.

**Actor:** пользователь.

**User goal:** сохранить вариант или подборку в рамках поддерживаемого объема.

**Preconditions:** есть найденный offer или comparison set.

**Trigger:** пользователь говорит "сохрани этот вариант".

**Main flow:** ассистент фиксирует selected offer в search session, подтверждает сохранение и помечает, какие данные могут устареть.

**Alternative flows:** пользователь хочет долгосрочное хранение; offer больше не актуален; нужно сохранить несколько вариантов.

**Edge cases:** EC-027, EC-028.

**Assistant responsibilities:** не обещать долгосрочную историю, если она не поддержана; предупреждать про freshness.

**Data needed:** selected offer, session id or context, provider facts snapshot, freshness.

**Expected result:** пользователь может вернуться к сохраненному объекту в текущей search session.

**MVP status:** In MVP для текущей search session; Post-MVP для аккаунта и долгосрочной истории.

**Notes:** storage design не описывается.

## UC-08. Resume current search session

**Linked scenario IDs:** S-08.

**Linked BR IDs:** BR-012, BR-013, BR-016.

**Linked FR IDs:** FR-011, FR-014.

**Actor:** пользователь.

**User goal:** продолжить поиск без повторного ввода параметров.

**Preconditions:** есть текущая search session или сохраненный контекст.

**Trigger:** пользователь просит вернуться к поиску.

**Main flow:** ассистент показывает восстановленные параметры, selected offers, unknown/stale fields и предлагает продолжить.

**Alternative flows:** доступна только текущая сессия; есть несколько похожих поисков; provider facts устарели.

**Edge cases:** EC-027, EC-028, EC-029.

**Assistant responsibilities:** не подавать старые цены и availability как актуальные без provider refresh.

**Data needed:** current user goal, extracted parameters, selected offers, rejected offers, freshness.

**Expected result:** пользователь продолжает сценарий с прозрачным состоянием данных.

**MVP status:** In MVP для текущей search session; Post-MVP для долгосрочной истории.

**Notes:** авторизация остается Open/Post-MVP.

## UC-09. No results found

**Linked scenario IDs:** S-09.

**Linked BR IDs:** BR-005, BR-010, BR-015.

**Linked FR IDs:** FR-012, FR-013, FR-014.

**Actor:** пользователь.

**User goal:** понять, почему не найдено вариантов, и что изменить.

**Preconditions:** поиск был возможен, но provider или фильтрация не дали подходящих offers.

**Trigger:** provider returns empty list или ranking отфильтровал все offers.

**Main flow:** ассистент отличает empty result от provider error, объясняет возможные причины и предлагает 1-3 изменения constraints.

**Alternative flows:** provider недоступен; данные неполные; constraints нереалистичны.

**Edge cases:** EC-014, EC-015, EC-020, EC-024.

**Assistant responsibilities:** не выдумывать market facts; гипотезы обозначать как assumptions.

**Data needed:** search constraints, provider response status, empty result reason if available, rejected constraints.

**Expected result:** понятный fallback и следующий шаг.

**MVP status:** In MVP.

**Notes:** technical error taxonomy относится к будущим этапам.

## UC-10. Contradictory request

**Linked scenario IDs:** S-10.

**Linked BR IDs:** BR-002, BR-005, BR-006, BR-010.

**Linked FR IDs:** FR-002, FR-003, FR-013, FR-014.

**Actor:** пользователь.

**User goal:** понять конфликт и изменить запрос.

**Preconditions:** запрос содержит конфликтующие constraints.

**Trigger:** ассистент обнаруживает конфликт до поиска или после provider result.

**Main flow:** ассистент называет конфликт, отделяет confirmed provider facts от assumptions, предлагает ослабить конкретные constraints.

**Alternative flows:** пользователь настаивает; конфликт нельзя подтвердить без provider; есть редкие варианты с рисками.

**Edge cases:** EC-009, EC-010, EC-011, EC-012, EC-025.

**Assistant responsibilities:** не делать категоричных утверждений без данных; не скрывать конфликт.

**Data needed:** constraints, market/provider facts if available, assumptions, suggested relaxations.

**Expected result:** пользователь понимает реалистичные варианты продолжения.

**MVP status:** In MVP.

**Notes:** conflict thresholds уточняются на Stage 3.

## UC-11. Open destination request

**Linked scenario IDs:** S-03, S-04.

**Linked BR IDs:** BR-001, BR-002, BR-003, BR-005, BR-009.

**Linked FR IDs:** FR-001, FR-002, FR-003, FR-013, FR-014.

**Actor:** пользователь.

**User goal:** получить варианты, когда направление задано как "куда-нибудь".

**Preconditions:** destination неизвестен, но есть тип отдыха, климат, бюджет, origin или период.

**Trigger:** запрос "куда-нибудь в теплое место", "на море недорого".

**Main flow:** ассистент определяет open destination, уточняет origin/period/budget и предлагает сузить регион или критерии.

**Alternative flows:** пользователь хочет, чтобы ассистент сам предложил направления; provider не поддерживает destination discovery; данных недостаточно.

**Edge cases:** EC-007, EC-008, EC-023.

**Assistant responsibilities:** не генерировать реальные цены и availability без provider facts; clearly label suggestions as assumptions if provider data is absent.

**Data needed:** origin, travel period, budget, destination preferences, flexibility, provider capabilities.

**Expected result:** направление уточнено или сформирован безопасный следующий шаг.

**MVP status:** Open: clarification In MVP; полноценный destination discovery зависит от Stage 3/provider capabilities.

**Notes:** не добавляет новый BR/FR; следует из S-03/S-04/Q-005.

## UC-12. Change constraints during search

**Linked scenario IDs:** S-05, S-08, S-10.

**Linked BR IDs:** BR-002, BR-004, BR-011, BR-013.

**Linked FR IDs:** FR-002, FR-007, FR-009, FR-011, FR-013.

**Actor:** пользователь.

**User goal:** изменить бюджет, даты, район, пересадки или preferences без начала заново.

**Preconditions:** есть активная search session.

**Trigger:** пользователь говорит "а если бюджет до 1200", "покажи без пересадок", "добавь багаж".

**Main flow:** ассистент обновляет параметры, объясняет влияние изменений и при необходимости повторяет поиск через provider abstraction.

**Alternative flows:** изменение конфликтует с выбранным offer; нужно сбросить часть результатов; provider facts устарели.

**Edge cases:** EC-029, EC-030.

**Assistant responsibilities:** показывать, какие параметры изменены, какие сохранены, какие offers нужно перепроверить.

**Data needed:** previous constraints, updated constraints, affected offers, freshness.

**Expected result:** search session обновлена без потери контекста.

**MVP status:** In MVP для текущей session в базовом виде.

**Notes:** сложная version history Post-MVP/Open.

## UC-13. Provider returns partial data

**Linked scenario IDs:** S-06, S-09.

**Linked BR IDs:** BR-010, BR-015.

**Linked FR IDs:** FR-006, FR-008, FR-012, FR-014.

**Actor:** provider/API как источник данных; пользователь получает результат через ассистента.

**User goal:** получить честный результат даже при неполных provider facts.

**Preconditions:** provider вернул offers с missing fields.

**Trigger:** часть данных отсутствует: baggage, cancellation policy, rating, freshness, amenities.

**Main flow:** ассистент отображает доступные provider facts, помечает unknown fields и не использует unknown как основание для уверенной рекомендации.

**Alternative flows:** отсутствует обязательное поле; данные противоречивы; explainability невозможна.

**Edge cases:** EC-016, EC-017, EC-018, EC-021, EC-024.

**Assistant responsibilities:** не заполнять пропуски догадками; предложить уточнить/перепроверить, если поле критично.

**Data needed:** provider facts, missing fields, field importance, freshness.

**Expected result:** пользователь видит ограничения данных и может принять решение.

**MVP status:** In MVP.

**Notes:** конкретные field mappings зависят от будущего API-контракта.

## UC-14. User asks for unsupported action

**Linked scenario IDs:** S-01, S-02, S-03.

**Linked BR IDs:** BR-014.

**Linked FR IDs:** FR-001, FR-013, FR-014.

**Actor:** пользователь.

**User goal:** выполнить действие за пределами MVP, например booking или payment.

**Preconditions:** пользователь уже нашел offer или просит действие напрямую.

**Trigger:** "забронируй", "оплати", "гарантируй цену", "оформи возврат".

**Main flow:** ассистент объясняет, что действие не поддержано в MVP, не совершает юридически значимых действий и предлагает безопасный следующий шаг.

**Alternative flows:** пользователь просит visa/legal advice; refund policy не подтверждена provider facts.

**Edge cases:** EC-031, EC-032, EC-033, EC-034, EC-035.

**Assistant responsibilities:** не обещать booking/payment/guarantees; не интерпретировать policy без данных.

**Data needed:** unsupported action type, selected offer if any, known provider facts.

**Expected result:** безопасный отказ с полезным продолжением в рамках MVP.

**MVP status:** In MVP как fallback; сами booking/payment Post-MVP.

**Notes:** не расширяет MVP.

## UC-15. Real provider/API data used as source of travel offers

**Linked scenario IDs:** S-01, S-02, S-03, S-06, S-09.

**Linked BR IDs:** BR-003, BR-007, BR-008, BR-010, BR-015.

**Linked FR IDs:** FR-004, FR-005, FR-006, FR-008, FR-012, FR-014.

**Actor:** пользователь; provider/API как источник travel facts.

**User goal:** получать реальные travel offers, а не LLM-generated facts.

**Preconditions:** финальный MVP имеет доступ к предоставленному контракту существующего travel API; до этого используются provider abstractions, mock/fake providers и contract placeholders только как промежуточные средства.

**Trigger:** любой supported search, требующий offers.

**Main flow:**
1. Ассистент извлекает параметры и проверяет, что поиск возможен.
2. Offers поступают из provider/API data source через утвержденные абстракции.
3. LLM/assistant интерпретирует, структурирует, сравнивает и объясняет provider data.
4. LLM/assistant не генерирует цены, availability, schedules, baggage rules или hotel facts.
5. Unknown, stale или partial data явно помечаются.

**Alternative flows:** API contract еще не предоставлен; provider data неполная; provider unavailable; freshness неизвестна.

**Edge cases:** EC-013, EC-014, EC-016, EC-017, EC-018, EC-019, EC-021, EC-024.

**Assistant responsibilities:** provider facts считать primary source of truth; assumptions хранить отдельно; не подменять отсутствие данных уверенностью.

**Data needed:** provider facts, source/provider, freshness, unknown fields, assistant assumptions.

**Expected result:** финальный MVP использует реальные travel offers из существующего API/provider.

**MVP status:** In MVP.

**Notes:** Stage 2 не проектирует API contract, endpoints, DTO, database schema или adapter implementation.
