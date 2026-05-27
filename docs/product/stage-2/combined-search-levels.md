# Stage 2 — Combined Search Levels

## Цель документа

Разделить combined search на уровни, чтобы закрыть замечание MJ-S1-001 из Stage 1 Consistency Review без преждевременного расширения MVP.

## Уровни

| Level | Описание | Пользовательская ценность | Требуемые capabilities | Требуемые данные | Сложность | MVP recommendation | Риски | Связанные scenarios/use cases |
|---|---|---|---|---|---|---|---|---|
| Level 1 — Combined intent recognition | Ассистент понимает, что пользователь хочет связку отель + перелет или открытую поездку. | Пользователь может начать с естественной формулировки поездки целиком. | Классификация intent, извлечение общих constraints, триггер уточнения. | intent type, origin/destination при наличии, dates/duration, travelers, budget, preferences. | Low/Medium. | **In MVP.** Следует из FR-001 и S-03. | Ошибочно принять combined request за hotel-only или flight-only. | S-03, UC-03, UC-11 |
| Level 2 — Same-dialog hotel and flight assistance | Ассистент помогает отдельно подобрать перелет и отдельно отель в одной search session, сохраняя общий контекст. | Пользователь не повторяет параметры и видит связанный диалог. | Общая search session, переиспользование параметров, отдельный hotel/flight search, базовое обновление контекста. | shared dates, origin, destination, travelers, hotel preferences, flight preferences, budget. | Medium. | **In MVP.** Совместим с Stage 1: hotel и flight search входят в MVP, а combined intent должен сохранять общий контекст. | Пользователь может ожидать package ranking, которого еще нет. Нужно явно обозначать пределы. | S-01, S-02, S-03, UC-01, UC-02, UC-03 |
| Level 3 — Coordinated combined search | Ассистент согласовывает параметры между отелем и перелетом: даты, бюджет, город, длительность, состав путешественников. | Пользователь получает более жизнеспособные варианты поездки, а не два независимых списка. | Согласование constraints, assumptions по разделению бюджета, consistency checks, fallback для частично combined сценария. | total budget, flight budget part/open, hotel budget part/open, shared dates, destination, provider facts для обоих типов offers. | Medium/High. | **Open for Stage 3.** Может войти в MVP, если Stage 3 подтвердит минимальный уровень coordination без full package ranking. | Неявное расширение MVP; budget split может стать недостоверным без данных; provider limitations. | S-03, S-09, S-10, UC-03, UC-10, UC-12 |
| Level 4 — Full combined package ranking | Ассистент ранжирует готовые пакеты перелет + отель как единую сущность с общей ценой и trade-offs. | Пользователь видит готовые package recommendations. | Формирование package, combined price, проверка совместимости, package-level ranking, более строгая обработка freshness и availability. | Подтвержденные flight offers, подтвержденные hotel offers, compatibility rules, combined price, taxes/fees при наличии, freshness для обеих частей. | High. | **Post-MVP or Open.** Stage 1 не требует full package ranking как обязательную MVP-сущность. | Сильный scope creep; риск ложной уверенности; сложность объяснения и freshness. | S-03, UC-03, UC-05, UC-06 |

## Recommendation

- **In MVP:** Level 1 и Level 2.
- **Open for Stage 3:** Level 3. Stage 3 должен решить, какой минимальный coordinated combined search нужен для MVP и какие acceptance criteria отделяют его от Level 4.
- **Post-MVP/Open:** Level 4. Полное package ranking не следует автоматически из Stage 1 и не должно появляться без явного решения.

## Scope guardrails

- Combined search не должен превращать Stage 2 в API, database или adapter design.
- Если provider/API data неполная, ассистент должен показывать partial result и unknown data.
- Финальный MVP должен использовать предоставленный API-контракт для реальных offers; отсутствие контракта на Stage 2 остается open input для технических этапов.
