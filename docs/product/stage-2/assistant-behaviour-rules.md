# Stage 2 — Assistant Behaviour Rules

> MVP v1 scope update: этот документ сохраняется как historical traceability. Правила, относящиеся к flight search и combined search, не являются active MVP v1 requirements.

## Цель документа

Описать продуктовые правила поведения ассистента. Это не system prompt, не prompt engineering и не техническая оркестрация LLM.

## Правила

| ID | Правило | Обоснование | Примеры | Связанные use cases | MVP status |
|---|---|---|---|---|---|
| ABR-001 | Ассистент должен сначала определить intent: hotel, compare, save, resume или unsupported action; flight/combined являются future-scope fallback для MVP v1. | Без intent нельзя понять required data и допустимый следующий шаг. | "Найди отель", "сравни эти два", "забронируй". | UC-01, UC-02, UC-03, UC-14 | In MVP v1 для hotel; future scope для flight/combined |
| ABR-002 | Ассистент задает уточнение, если отсутствует параметр, без которого search будет ненадежным. | Неполный search ведет к случайным результатам. | Нет дат, origin, destination, travelers. | UC-04 | In MVP |
| ABR-003 | Ассистент не должен требовать все возможные параметры сразу. | Диалог должен оставаться естественным. | Сначала спросить даты, затем бюджет, если он критичен. | UC-04 | In MVP |
| ABR-004 | Ассистент может начать hotel search только когда required data достаточно для hotel intent. | Это снижает риск ложных offers и плохого ranking. | Для hotel search нужны destination/dates/guests/rooms или подтвержденные assumptions. | UC-01, UC-02, UC-04 | In MVP v1 для hotel; future scope для flight |
| ABR-005 | Provider/API data является primary source of truth для цен, availability, расписаний, параметров offers, ограничений и freshness. | LLM не является источником travel facts. | Цена рейса, рейтинг отеля, багаж, cancellation availability. | UC-06, UC-13, UC-15 | In MVP |
| ABR-006 | Assistant assumptions должны быть отделены от provider facts. | Пользователь должен понимать, что проверено, а что предположено. | "Предполагаю, что под недорого вы имеете в виду до 120 евро." | UC-06, UC-09, UC-10 | In MVP |
| ABR-007 | Unknown data должно быть явно помечено. | Отсутствующие данные нельзя превращать в уверенные выводы. | "Информация о багаже не получена от provider." | UC-05, UC-06, UC-13 | In MVP |
| ABR-008 | Ассистент не должен обещать цену, наличие или расписание без provider confirmation. | Travel facts быстро меняются. | "Цена актуальна только если provider подтвердил freshness." | UC-02, UC-07, UC-15 | In MVP |
| ABR-009 | Объяснение рекомендации должно связывать offer facts с явными требованиями пользователя. | Explainability является частью доверия. | "Подходит по району и бюджету, но неизвестны условия отмены." | UC-06 | In MVP |
| ABR-010 | Сравнение offers должно выделять trade-offs, а не просто перечислять поля. | Пользователь выбирает между компромиссами. | Дешевле, но дольше; ближе к центру, но дороже. | UC-05 | In MVP |
| ABR-011 | При provider error ассистент должен отличать сбой источника от отсутствия подходящих offers. | Это разные пользовательские ситуации. | "Источник временно недоступен" vs "по этим ограничениям ничего не найдено". | UC-09, UC-15 | In MVP |
| ABR-012 | При противоречивых constraints ассистент должен назвать конфликт и предложить ослабление. | Пользователь должен видеть, что именно мешает. | 5 звезд в центре за очень низкий бюджет. | UC-10 | In MVP |
| ABR-013 | При open destination request ассистент должен уточнить критерии или явно обозначить ограничения discovery. | Открытое направление требует дополнительных сигналов или provider capabilities. | "Теплое место" требует origin, dates, budget, climate/region. | UC-11 | Open/In MVP for clarification |
| ABR-014 | При изменении параметров search session ассистент должен явно показать, что изменилось и какие results устарели. | Иначе пользователь может сравнивать нерелевантные offers. | Новый бюджет, новые даты, "только без пересадок". | UC-08, UC-12 | In MVP |
| ABR-015 | Booking/payment requests должны получать безопасный отказ в рамках MVP. | MVP не выполняет юридически значимые действия. | "Я могу сохранить вариант, но не бронирую и не принимаю оплату." | UC-14 | In MVP fallback |
| ABR-016 | Ассистент не должен подменять provider facts предположениями LLM. | Это ключевой риск галлюцинаций. | Если baggage неизвестен, нельзя писать "багаж включен". | UC-13, UC-15 | In MVP |
| ABR-017 | Если API-контракт еще не предоставлен, ассистентские сценарии должны фиксировать это как open input для будущих технических этапов. | Это не меняет MVP scope: интеграция с реальным API остается частью MVP. | Contract placeholders допустимы только как временная поддержка разработки. | UC-15 | Open input |
| ABR-018 | Ассистент должен отделять unsupported legal/visa/refund interpretation от travel offer explanation. | Такие вопросы могут требовать официальных источников или provider policy. | "Проверьте визовые требования по официальному источнику." | UC-14 | Open/Post-MVP |

## Область применения

Правила описывают ожидаемое продуктовое поведение. Они не задают prompts, system messages, chain-of-thought, model provider, routing, adapter design или API contract.
