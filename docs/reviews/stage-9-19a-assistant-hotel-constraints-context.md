# Stage 9.19a — накопление контекста hotel constraints

## Роль документа

Это отчет о реализации Stage 9.19a. Он фиксирует внутренний process-local
контекст ограничений hotel search, правила обновления и результаты проверок.
Текущий статус проекта задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Сохранять подтвержденные из сообщений пользователя параметры hotel search между
ходами одной assistant session. Частично заполненный запрос должен переживать
уточнения, но не должен запускать provider search до полного набора полей и
отдельного явного подтверждения.

## Реализация

Добавлены application-owned компоненты:

- `AssistantHotelConstraints` — типизированное каноническое состояние;
- `AssistantHotelConstraintsStore` и
  `InMemoryAssistantHotelConstraintsStore` — process-local хранение по
  `AssistantSessionId`;
- `AccumulateAssistantHotelConstraintsUseCase` — детерминированное применение
  извлеченных LLM-ограничений как изменения текущего состояния;
- typed issues для явно неверных значений.

Контекст содержит только:

- `destination`;
- `check-in` и `check-out` как `LocalDate`;
- количество взрослых;
- заявленное количество детей и их возраста;
- количество номеров;
- typed markers полей, для которых требуется корректное новое значение.

Исходные сообщения, полный transcript, raw LLM candidate и provider facts не
сохраняются.

## Правила накопления

- Новое валидное значение заменяет прежнее.
- Поле, отсутствующее в новом candidate, сохраняет прежнее значение.
- Неизвестные ключи игнорируются и не попадают в store.
- Явно переданное неверное значение очищает прежнее значение этого поля и
  блокирует confirmation до валидной замены.
- Валидные поля из того же сообщения сохраняются, даже если другое поле
  отклонено.
- Канонический формат дат — `YYYY-MM-DD`.
- Возраста детей сохраняются в исходном порядке; допустим диапазон `0..17`.
- Положительное количество детей без полного списка возрастов оставляет
  `children-ages` среди недостающих полей.
- Изменение количества детей очищает несовместимый прежний список возрастов.

`confirmedConstraints` и `missingRequiredFields` для каждого следующего LLM
запроса формируются из этого состояния. Foundation-only
`HotelRequirementsCoveragePlan` остается совместимым legacy metadata, но больше
не является источником канонического LLM-контекста.

## Безопасная граница LLM

Накопление разрешено только для принятого hotel-only candidate с outcome
`INTERPRETED` или `NEEDS_CLARIFICATION`, без conflicts и warnings.
`AMBIGUOUS`, `UNSUPPORTED`, rejected candidates и fallback не изменяют
контекст.

`AssistantCandidateDecision.AskClarification` может нести принятый partial
candidate только внутри application layer. Public response по-прежнему содержит
только безопасный вопрос и существующий `nextAction`.

## Pending confirmation и lifecycle

Если активный confirmation получает correction-сообщение:

1. старый pending confirmation помечается consumed;
2. то же сообщение сразу проходит обычный LLM/context flow;
3. валидное исправление может создать новый pending confirmation;
4. hotel search в этом ходе не создается.

Контекст сохраняется после отказа и после успешного поиска до завершения
process-local assistant session. Отдельный durable lifecycle или очистка по TTL
на этом этапе не добавлены.

## Проверки

Точечные unit и integration tests покрывают:

- накопление частичных значений и замену конкретного поля;
- очистку явно неверного значения с сохранением других валидных изменений;
- границы возраста `0` и `17`, отклонение `-1` и `18`;
- запрос возраста при положительном количестве детей без `children-ages`;
- защиту от повторного использования устаревших возрастов;
- изоляцию context по `AssistantSessionId`;
- сценарий «город → даты → гости → confirmation → search»;
- correction активного pending в том же сообщении;
- отсутствие `hotelSearchId` до явного подтверждения;
- сохранение контекста после decline и успешного search;
- отсутствие raw context в public response.

## Границы этапа

Не изменены:

- public API, OpenAPI, frontend и generated clients;
- provider transport, DTO, mapper, ranking и runtime mode;
- strict `hotel-search;` handoff;
- `FAKE` как режим по умолчанию;
- LLM-вызов как синхронная граница;
- durable storage, auth, booking, payment и pagination.

## Риски и ограничения

- Store process-local: состояние теряется при перезапуске и не координируется
  между экземплярами.
- Качество извлечения delta по-прежнему зависит от `LlmClient`; этот этап только
  валидирует и накапливает принятый candidate.
- Полный transcript не хранится, поэтому контекст ограничен согласованным
  набором hotel constraints.
- Асинхронный LLM transport и реальный LLM provider остаются отдельными этапами.

## Verdict

`PASS_PROCESS_LOCAL_HOTEL_CONSTRAINTS_CONTEXT`.

Stage 9.19a завершен. Следующий разрешенный этап — Stage 9.19b: сквозной перевод
`LlmClient.generateCandidate()` и вызывающей application-цепочки на `suspend` с
сохранением текущего поведения `FakeLlmClient` и без `runBlocking` в production.
