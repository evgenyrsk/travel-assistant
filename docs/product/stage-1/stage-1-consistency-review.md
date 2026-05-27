# Stage 1 Consistency Review

## 1. Review scope

Проверялись документы Stage 1:

- `target-audience.md`;
- `business-scenarios.md`;
- `user-journeys.md`;
- `business-requirements.md`;
- `functional-requirements.md`;
- `non-functional-requirements.md`;
- `assumptions-and-open-questions.md`;
- `stage-1-summary.md`.

Также проверялись индексные и roadmap-документы:

- `docs/product/README.md`;
- `docs/roadmap/roadmap.md`.

Review проверяет связность, traceability, MVP/post-MVP consistency, scope control, терминологию, roadmap consistency, открытые вопросы, риски и готовность к Stage 2. Review не выполняет Stage 2 и не исправляет смысловые вопросы молча.

## 2. Executive summary

**Verdict:** Passed with minor notes.

Stage 1 в целом согласован и пригоден как вход для Stage 2. Документы покрывают целевые сегменты, сценарии S-01 - S-10, journeys, BR, FR, NFR, assumptions, open questions и risks. Код, API-контракты, схемы БД, UI-макеты, выбор финального стека и выбор LLM provider не создавались.

Найдены неблокирующие замечания:

- combined search имеет Open-статус для MVP, но некоторые FR с `In MVP` прямо включают S-03;
- NFR не имеют явной связи со сценариями в том же формате, что BR и FR;
- терминология частично смешивает русские и английские формы: provider/провайдер, offer/offers/оффер, session/search session;
- README проекта вне Stage 1 все еще говорит, что текущий этап — Этап 0; это не блокирует Stage 2, но требует навигационного обновления отдельной задачей или небольшим follow-up.

## 3. Traceability review

| Item | Status | Notes |
|---|---|---|
| S-01 Hotel search | Passed | Связан с FR-001, FR-002, FR-003, FR-004, FR-006, FR-007, FR-008; покрыт BR-001, BR-003, BR-004, BR-007, BR-011, BR-015; есть journey J-01. |
| S-02 Flight search | Passed | Связан с FR-001, FR-002, FR-003, FR-005, FR-006, FR-007, FR-008; покрыт BR-001, BR-003, BR-004, BR-008, BR-011, BR-015; есть journey J-02. |
| S-03 Combined search | Passed with note | Сценарий, BR-009 и Q-001 фиксируют Open-статус для MVP; FR-001, FR-002, FR-006, FR-007, FR-008 и FR-013 включают S-03 как связанный сценарий с `In MVP`, что требует уточнения границы "intent/support" vs "full combined search". |
| S-04 Clarification | Passed | Связан с FR-001, FR-002, FR-003, FR-013, FR-014; покрыт BR-002, BR-005; есть journey J-04. |
| S-05 Comparison | Passed | Связан с FR-008, FR-009, FR-011, FR-014; покрыт BR-004, BR-007, BR-008, BR-010, BR-011; есть journey J-05. |
| S-06 Recommendation explanation | Passed | Связан с FR-008, FR-014; покрыт BR-004, BR-007, BR-008, BR-010; включен в J-01, J-02, J-03, J-05. |
| S-07 Save result | Passed | Связан с FR-010, FR-011; покрыт BR-013 и BR-016; есть journey J-06. |
| S-08 Return to previous search | Passed | Связан с FR-011, FR-014; покрыт BR-012, BR-013, BR-016; есть journey J-07. |
| S-09 Nothing found | Passed | Связан с FR-012, FR-013, FR-014; покрыт BR-005, BR-010, BR-015; присутствует в J-01, J-02, J-03. |
| S-10 Contradictory request | Passed | Связан с FR-002, FR-003, FR-013, FR-014; покрыт BR-002, BR-005, BR-006, BR-010; присутствует в J-01, J-03, J-04. |
| BR-* coverage | Passed | Все BR-001 - BR-016 имеют связанные сценарии. |
| FR-* business basis | Passed | Все FR-001 - FR-014 имеют источник требования через BR и/или сценарии. |
| NFR-* traceability | Passed with note | NFR имеют rationale и verification approach, но не имеют явного поля "Связанные сценарии". Это не ломает Stage 1, но усложняет traceability matrix. |
| User journeys | Passed | J-01 - J-07 покрывают основные сценарии и не проектируют UI в деталях. |
| MVP scope | Passed with note | MVP scope согласован по большинству пунктов; combined search требует отдельного уточнения на Stage 2/3. |

## 4. MVP / Post-MVP consistency

Согласованные пункты:

- **Hotel search:** последовательно трактуется как MVP.
- **Flight search:** последовательно трактуется как MVP.
- **Clarification flow:** последовательно трактуется как MVP.
- **Comparison:** последовательно трактуется как MVP в базовом виде.
- **Recommendation explanation:** последовательно трактуется как MVP.
- **Saved result:** последовательно разделен на MVP для текущей сессии и post-MVP для долгосрочного хранения.
- **Return to previous search:** последовательно разделен на MVP для текущей сессии и post-MVP для долгосрочной истории.
- **Personalization:** явно указанные preferences в текущем сценарии входят в MVP; долгосрочная персонализация — post-MVP.
- **Authorization:** не входит в ранний MVP и вынесена в Q-010.
- **Real API providers:** не входят в Stage 1/MVP-ранние этапы; sources описаны как abstract providers.
- **LLM provider choice:** не выполняется на Stage 1.
- **Booking/payment:** не входит в MVP.
- **Mobile apps:** не являются обязательной первой платформой; cross-platform остается продуктовым ограничением.

Расхождения:

- **Combined search:** S-03 и BR-009 имеют Open-статус для MVP, но `stage-1-summary.md` включает "Работа через абстрактных providers и mock/stub данные" в MVP, а FR с `In MVP` ссылаются на S-03. Это можно читать как "MVP должен понимать combined intent и иметь groundwork", но не как обязательный full combined search. На Stage 2/3 нужно явно развести эти уровни.

## 5. Scope control review

Stage 1 не вышел за рамки продуктового этапа:

- код не создавался;
- API contracts не создавались;
- схема БД не создавалась;
- UI-макеты не создавались;
- финальный технический стек не выбирался;
- конкретный LLM provider не выбирался;
- реальные providers/API не интегрировались;
- Stage 2 не выполнялся.

Допустимые пограничные формулировки:

- `provider abstraction`, `LLM provider abstraction`, `mock/stub данные` используются как продуктовые и архитектурные ограничения, а не как технические контракты.
- Journeys упоминают продуктовые состояния, но не проектируют экраны.

## 6. Terminology review

Термины в целом понятны, но есть смешение языковых форм:

- **Travel Assistant:** используется единообразно.
- **assistant / ассистент:** в русских документах преобладает "ассистент"; английское `assistant` встречается в названиях требований и контексте AI. Риск низкий.
- **provider / providers / провайдер:** используются смешанно. Лучше договориться о форме: например, "provider" как термин архитектурной абстракции и "провайдер" в обычном тексте.
- **offer / offers / оффер:** используются смешанно. Лучше выбрать один базовый термин для Stage 2.
- **saved result / сохраненный результат:** смысл согласован, но термин "подборка" и "вариант" используются рядом. Это допустимо, но Stage 2 должен уточнить объекты сохранения.
- **search session / текущая сессия / контекст поиска:** смысл согласован, но терминологически стоит нормализовать перед use cases.
- **combined search / combined trip / связка "перелет + отель":** смысл близкий, но требует единого термина и уровня поддержки в MVP.
- **MVP / post-MVP:** используется последовательно, кроме нюанса combined search.

## 7. Roadmap consistency

`docs/roadmap/roadmap.md` соответствует текущему состоянию Stage 1:

- Этап 0 отмечен как завершенный.
- Этап 1 отмечен как завершенный.
- Stage 1 artifacts перечислены.
- Ограничения Stage 1 явно запрещают код, API-контракты, UI-макеты, схему БД, выбор финального стека и реальные интеграции.
- Будущие этапы не были переупорядочены.

Сохраняется осознанное расхождение, уже зафиксированное в Q-011: старый roadmap разделял бизнес-требования, пользовательские сценарии и функциональные требования между Stage 1 - Stage 3, а фактическая задача Stage 1 включила их в один продуктовый пакет. Это не блокирует Stage 2, если Stage 2 будет трактоваться как детализация use cases, а Stage 3 — как финализация MVP boundaries и acceptance criteria.

Дополнительное навигационное замечание: `README.md` все еще содержит формулировку "Текущий этап — Этап 0". Этот файл не входил в список разрешенных обновлений текущей review-задачи, поэтому изменение не выполнялось.

## 8. Open questions and risks review

Open questions Q-001 - Q-011 покрывают ключевые неопределенности:

- combined search;
- обязательные параметры hotel и flight search;
- критерии успешной рекомендации;
- открытое направление;
- объем сохранения;
- порог уточняющих вопросов;
- глубина ranking/comparison;
- обозначение uncertainty;
- авторизация;
- конфликт roadmap по объему Stage 1.

Risks R-001 - R-006 покрывают требуемые risk areas:

- MVP scope creep по combined search;
- AI/LLM hallucination;
- provider/API coupling;
- качество mock/stub данных;
- сохранение результатов;
- privacy.

Все риски имеют impact, likelihood, possible mitigation и stage where it should be resolved.

Замечание: provider reliability покрыта через FR-012, NFR-006, NFR-011 и R-003/R-004, но отдельного риска именно "provider reliability / downtime" нет. Это можно добавить на следующем review или Stage 2, если нужно усилить negative flows.

## 9. Findings

### Critical findings

Нет.

### Major findings

#### MJ-S1-001. Combined search имеет Open-статус, но участвует в In MVP functional requirements

**Описание:** S-03 и BR-009 явно оставляют full combined search открытым для MVP, однако FR-001, FR-002, FR-006, FR-007, FR-008 и FR-013 имеют `MVP status: In MVP` и включают S-03 как связанный сценарий.

**Где найдено:** `business-scenarios.md`, `business-requirements.md`, `functional-requirements.md`, `stage-1-summary.md`.

**Impact:** может привести к разному пониманию: MVP должен только распознавать combined intent или полноценно собирать связку "перелет + отель".

**Recommendation:** на Stage 2/3 разделить уровни поддержки: `combined intent recognition`, `separate hotel/flight search in one conversation`, `full package ranking`.

**Blocking:** no.

### Minor notes

#### MN-S1-001. NFR не имеют явной scenario traceability

**Описание:** NFR-001 - NFR-015 имеют rationale и verification approach, но не указывают связанные сценарии или requirement IDs.

**Где найдено:** `non-functional-requirements.md`.

**Impact:** усложняет будущую traceability matrix, но не ломает Stage 1.

**Recommendation:** при подготовке Stage 2/3 добавить легкую матрицу NFR -> scenarios/BR/FR или отдельный traceability appendix.

**Blocking:** no.

#### MN-S1-002. Терминология provider/offer/session смешивает русские и английские формы

**Описание:** документы используют `provider`, `providers`, `провайдер`, `offer`, `offers`, `оффер`, `session`, `сессия`, `контекст поиска`.

**Где найдено:** `business-scenarios.md`, `business-requirements.md`, `functional-requirements.md`, `non-functional-requirements.md`, `user-journeys.md`.

**Impact:** риск небольших расхождений при детализации use cases.

**Recommendation:** на Stage 2 ввести короткий glossary или нормализовать термины в use cases.

**Blocking:** no.

#### MN-S1-003. README проекта содержит устаревшую навигационную формулировку о текущем этапе

**Описание:** `README.md` все еще говорит "Текущий этап — Этап 0", хотя `docs/roadmap/roadmap.md` отмечает Stage 1 завершенным.

**Где найдено:** `README.md`.

**Impact:** может сбивать нового участника проекта, но не влияет на содержательную готовность Stage 1.

**Recommendation:** обновить README отдельным follow-up или в задаче навигационной синхронизации.

**Blocking:** no.

#### MN-S1-004. Provider reliability выделена поведением, но не отдельным risk item

**Описание:** provider errors покрыты в FR-012, NFR-006 и NFR-011, но в risks нет отдельного R-* про downtime, stale data или нестабильность источников.

**Где найдено:** `assumptions-and-open-questions.md`, `functional-requirements.md`, `non-functional-requirements.md`.

**Impact:** риск не потерян, но может быть менее заметен при планировании Stage 2/3.

**Recommendation:** при следующем обновлении risks добавить отдельный risk или явно расширить R-003.

**Blocking:** no.

## 10. Stage 2 readiness

**Readiness verdict:** conditionally ready.

Stage 1 готов для перехода к Stage 2 при условии, что Stage 2 не будет считать full combined search автоматически включенным в MVP.

Входные документы для Stage 2:

- `business-scenarios.md`;
- `user-journeys.md`;
- `business-requirements.md`;
- `functional-requirements.md`;
- `non-functional-requirements.md`;
- `assumptions-and-open-questions.md`;
- `stage-1-summary.md`;
- этот consistency review.

Вопросы, которые нужно перенести в Stage 2:

- Q-001: уровень поддержки combined search;
- Q-002 и Q-003: обязательные параметры hotel и flight search;
- Q-004: критерии успешной рекомендации;
- Q-005: работа с открытым направлением;
- Q-006 и Q-010: объем сохранения и авторизация;
- Q-007: порог уточняющих вопросов;
- Q-009: язык uncertainty и provider errors.

## 11. Recommendations

- Не выполнять Stage 2 в рамках этого review.
- На Stage 2 создать use cases на основе S-01 - S-10 без API-контрактов и UI-макетов.
- На Stage 2 явно разделить combined intent recognition и full combined package search.
- На Stage 2 нормализовать термины provider, offer, saved result, search session и combined search.
- На Stage 3 финализировать MVP boundaries и acceptance criteria.
- Отдельным follow-up обновить `README.md`, чтобы он не называл Этап 0 текущим этапом.

## 12. Final verdict

Stage 1 проходит consistency review с minor notes. Блокеров для Stage 2 не найдено. Главный управленческий риск — не превратить Open-статус combined search в неявное обязательство MVP без решения Stage 2/3.

## 13. Follow-up cleanup status

После review выполнен локальный cleanup:

- `MN-S1-001`: добавлена краткая NFR traceability table в `non-functional-requirements.md`;
- `MN-S1-002`: наиболее явные терминологические расхождения нормализованы вокруг `provider`, `offer`, `search session` и `combined search`;
- `MN-S1-003`: корневой `README.md` обновлен и больше не называет Этап 0 текущим этапом;
- `MN-S1-004`: добавлен риск `R-007. Provider reliability и качество данных`;
- `MJ-S1-001`: не закрыт полностью; добавлено уточнение, что combined intent recognition может входить в MVP, а full combined package ranking остается Open до Stage 2/3.
