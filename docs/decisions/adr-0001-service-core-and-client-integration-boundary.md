# ADR-0001 — граница backend-сервиса и внешних клиентских интеграций

- **Статус:** Accepted
- **Дата:** 2026-07-21
- **Связанный этап:** Stage 10.4

## Контекст

Travel Assistant уже имеет backend с business logic, LLM/provider orchestration
и ограниченным chat-first HTTP-контрактом. В этом репозитории реализован
web/PWA, необходимый для локальной демонстрации MVP. Будущие web, Android, iOS и
другие клиенты будут отдельными продуктами и будут разрабатываться другими
командами.

Нужно отделить ответственность сервиса от решений о UI, mobile architecture и
клиентском SDK, не создавая преждевременную зависимость от конкретной
платформы.

## Решение

1. Travel Assistant развивается как самостоятельный backend-сервис и остается
   удаленным ядром продукта. Клиенты обращаются к нему через `/api/v1/**`.
2. Текущий web/PWA остается только локальной demo shell MVP. Он не определяет
   архитектуру и UI будущего продуктового web-клиента.
3. Product web, Android, iOS, desktop и другие клиенты не выбираются и не
   реализуются сейчас. Их UI, platform architecture и release lifecycle
   принадлежат будущим интеграционным командам.
4. Интеграционный контракт сервиса ограничен тремя chat-first endpoint:
   - `POST /api/v1/assistant/sessions`;
   - `POST /api/v1/assistant/sessions/{sessionId}/messages`;
   - `GET /api/v1/hotel-searches/{searchId}/offers`.
5. Backend/domain modules, provider DTO, ranking, LLM orchestration и secrets не
   передаются клиентским командам как встраиваемое ядро. Источник интеграции —
   versioned HTTP API, OpenAPI и проверенные примеры контрактов.
6. Выбор ручного API client, generated SDK, KMP, Swift/Kotlin stack и UI остается
   решением команды конкретной платформы либо отдельной совместной задачи.
7. `sessionId`, `hotelSearchId` и `offerId` остаются opaque identifiers. Клиент
   не восстанавливает из них domain/provider semantics.

## Последствия

- Backend behavior остается единым для demo shell и любых будущих клиентов.
- Команды платформ не копируют server-side business rules и не обращаются
  напрямую к OpenRouter или Hotels API.
- Репозиторий сервиса должен предоставлять понятный интеграционный контракт,
  lifecycle flow, error semantics и совместимые примеры, но не обязан содержать
  native UI или platform SDK.
- Generated-client target пока не объявляется. SDK readiness не заявляется.
- Cross-origin web потребует отдельной CORS allowlist; native transport сам по
  себе CORS не требует.
- Auth, durable storage, resume и cross-device sync остаются отдельными
  service/product decisions.

## Рассмотренные альтернативы

- **Выбрать Android или iOS сейчас.** Отклонено: платформа и ее UI не входят в
  текущую задачу сервиса и будут определяться будущей командой.
- **Создать общий KMP/mobile core.** Отклонено: преждевременно связывает сервис с
  клиентской технологией и дублирует ответственность backend.
- **Сгенерировать platform SDK заранее.** Отложено до отдельного запроса от
  интеграционной команды и согласования ее toolchain.

## Границы

ADR не разрешает product UI, generated clients, runtime rollout, CORS, auth,
durable storage или изменение public API. Локальная demo shell не считается
product web implementation. Текущий статус и следующие этапы определяет
`docs/roadmap/roadmap.md`.
