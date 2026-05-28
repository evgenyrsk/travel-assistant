# Stage 3.4 — Combined Search UX Decision

> Superseded for MVP v1: после решения о hotel-only MVP v1 этот документ сохраняется как historical decision и future-scope reference. Limited Level 3 coordinated combined search не входит в MVP v1. Flight search является next expansion после hotel flow; combined hotel+flight возвращается как later expansion после flight flow.

## 1. Назначение документа

Документ фиксировал MVP-решение по Level 3 coordinated combined search до последующего refocus на hotel-only MVP v1.

Цель решения — определить, входит ли координация hotel + flight в MVP, какие UX-поведения допустимы, где проходит граница с full package ranking и какие acceptance criteria должны проверяться на следующих этапах.

Документ не является visual design, wireframe, API-контрактом, DTO schema, database schema, provider adapter design, ranking algorithm, LLM prompt или технической реализацией.

## 2. Источники и ограничения

Основные источники:

- `docs/roadmap/roadmap.md`;
- `docs/product/stage-2/combined-search-levels.md`;
- `docs/product/stage-2/use-cases.md`;
- `docs/product/stage-2/assistant-behaviour-rules.md`;
- `docs/product/stage-2/data-requirements.md`;
- `docs/product/stage-3/screen-map.md`;
- `docs/product/stage-3/required-fields-and-acceptance-criteria.md`;
- `docs/product/stage-3/mvp-search-flow-details.md`.

Ограничения:

- До hotel-only refocus MVP включал Level 1 combined intent recognition и Level 2 same-dialog hotel and flight assistance; для MVP v1 это superseded.
- Provider/API data остается primary source of truth для travel facts.
- Assistant assumptions должны быть явно отделены от provider facts.
- Финальный MVP должен использовать предоставленный контракт существующего travel API, но этот документ не проектирует контракт.
- Booking, payment, guaranteed package price, package-level availability и full package ranking не входят в MVP.
- Stage 4 Visual Design / UI Concept и Stage 5 Technical Architecture не начинаются в рамках этого решения.

## 3. Решение

**Historical decision:** включить в тогдашний MVP ограниченный Level 3 coordinated combined search как UX/product behaviour.

**Current MVP v1 status:** superseded. MVP v1 ограничен hotel search, а limited Level 3 переносится в future scope.

Ограниченный Level 3 означает, что в будущем ассистент может координировать общие параметры hotel и flight parts внутри одной search session, но не формирует готовый travel package как единую коммерческую сущность.

Future combined scope может включать:

- consistency checks между hotel и flight parts;
- переиспользование shared fields: origin, destination, dates/duration, travelers, budget и priorities;
- предупреждение о конфликтах между частями поездки;
- user-visible assumptions для budget split, room count, cabin class и других derived fields;
- stale markers, если изменение одного shared field делает прежние hotel или flight results устаревшими;
- partial combined selection: пользователь может сохранить выбранный hotel + flight как связанную подборку текущей session;
- trade-off explanation между отдельными hotel и flight offers.

Даже в future combined scope не включается без отдельного решения:

- full package ranking;
- автоматическое составление и ранжирование package offers;
- guaranteed combined price;
- booking/payment flow;
- ticketing, refund или fare-rule интерпретацию как обязательную функцию;
- скрытое объединение hotel и flight provider facts в один неподтвержденный provider fact;
- provider/API contract design, adapter design, database schema или implementation details.

## 4. Future UX границы limited Level 3

Limited Level 3 в future combined scope должен помогать пользователю понять, совместимы ли выбранные части поездки, но не должен обещать, что система продает или гарантирует пакет.

Допустимые UX-формулировки:

- "Эти варианты выглядят совместимыми по датам."
- "Перелет укладывается в даты проживания, но общий бюджет превышен."
- "Я считаю распределение бюджета как предположение: 40% на перелет и 60% на отель."
- "Сохранил связку в текущей сессии; цены и наличие нужно обновлять перед бронированием."

Недопустимые UX-формулировки:

- "Это готовый пакет."
- "Общая цена гарантирована."
- "Этот пакет доступен к бронированию."
- "Лучший package offer" без подтвержденного package-level provider fact.

## 5. Coordination checks в MVP

MVP-level coordination может проверять:

| Check | MVP behaviour | Что нельзя делать |
|---|---|---|
| Dates compatibility | Проверить, что flight dates согласуются с hotel check-in/check-out или явно назвать mismatch. | Не гарантировать, что изменение рейса автоматически меняет бронирование отеля. |
| Destination consistency | Проверить, что hotel destination и flight destination относятся к одному trip context. | Не выводить точную airport-to-hotel logistics без provider/map facts. |
| Travelers / guests consistency | Проверить, что passengers и guests не конфликтуют. | Не додумывать возрастные категории или room policies как provider facts. |
| Total budget | Показать approximate total из отдельных provider facts, если обе цены доступны, и явно отметить assumptions. | Не называть total package price гарантированной ценой. |
| Budget split | Предложить split как assistant assumption или спросить приоритет. | Не подавать split как provider-confirmed allocation. |
| Stale results | Пометить affected offers stale при изменении shared dates, travelers, destination или budget. | Не оставлять старые results как актуальные после material change. |
| Partial provider data | Показать partial result с unknown fields. | Не превращать missing facts в уверенные рекомендации. |

## 6. Future acceptance criteria

Эти критерии не являются active MVP v1 requirements. Они сохраняют контекст для будущего combined expansion после реализации hotel flow и flight flow.

- Given пользователь просит "подбери поездку с перелетом и отелем", When ассистент распознает combined intent, Then он поддерживает один shared context для hotel и flight parts.
- Given hotel и flight results доступны, When даты рейса и проживания не совпадают, Then ассистент явно показывает mismatch и предлагает уточнить даты или выбрать другой offer.
- Given пользователь задал total budget, When hotel и flight prices доступны как provider facts, Then ассистент может показать approximate total только как сумму отдельных facts и assumptions, без гарантии package price.
- Given пользователь меняет dates, travelers, destination или budget, When прежние hotel/flight results зависят от измененного field, Then affected results помечаются stale.
- Given пользователь сохраняет hotel + flight selection, When shortlist отображается, Then selection помечается как partial/user-guided и current-session only.
- Given provider вернул только hotel results или только flight results, When combined flow продолжается, Then ассистент показывает partial state и не заявляет, что package найден.
- Given пользователь просит "лучший готовый пакет", When future combined scope не поддерживает full package ranking, Then ассистент объясняет границу: можно сравнить и сохранить отдельные hotel/flight offers, но не сформировать гарантированный package offer.
- Given assistant uses budget split, room count или cabin class defaults, When эти assumptions влияют на поиск или сравнение, Then они видимы пользователю и не маркируются как provider facts.

## 7. Что остается Post-MVP / Open

- Full package ranking как единая сущность.
- Package-level offer model, если она появится в provider/API contract.
- Booking, payment, ticketing, refunds и legal workflows.
- Advanced weighted scoring для combined packages.
- Map/logistics-aware coordination между airport, hotel location и transfer time.
- Account-level saved trips и long-term package history.
- Production-hardening provider adapter behaviour и error taxonomy.

## 8. Последствия для Stage 3

- Open question по MVP-статусу Level 3 coordinated combined search superseded для MVP v1: limited Level 3 не входит в hotel-only MVP v1.
- Hotel-only UX Consistency Review завершен в `docs/product/stage-3/stage-3-hotel-only-consistency-review.md`.
- Stage 3 Summary & Carryover завершен в `docs/product/stage-3/stage-3-summary-and-carryover.md`; session persistence, resume и authorization перенесены в carryover без технического проектирования.
- Stage 4 не должен менять это решение без отдельного product review.
- Stage 5 должен учитывать решение как продуктовую границу, но не считать этот документ API или architecture contract.
