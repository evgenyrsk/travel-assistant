# ADR-0002 — provider-neutral semantic gateway boundary

- **Статус:** Accepted
- **Дата:** 2026-07-28
- **Связанный этап:** Stage 17.0

## Контекст

Stage 16 создал application-owned `AccommodationAnalysisClient` и opt-in
OpenRouter adapter. Для переноса в корпоративную инфраструктуру нельзя считать
доступ к внешним моделям разрешённым или связывать semantic orchestration с
OpenRouter, конкретным model slug либо inference runtime.

Нужна стабильная граница, через которую можно использовать self-hosted model,
корпоративную AI platform или разрешённый cloud endpoint без изменения
business logic, taxonomy, ranking и публичного API Travel Assistant.

## Решение

1. `AccommodationAnalysisClient` остаётся единственным application-owned port
   semantic classification. Domain/application code не знает provider, model,
   inference runtime, credentials или transport DTO.
2. Для корпоративного контура используется инфраструктурный mode
   `INTERNAL_GATEWAY`. OpenRouter сохраняется отдельным optional adapter и не
   является production dependency.
3. Gateway предоставляет narrow versioned contract
   [`POST /v1/accommodation-analysis`](../architecture/stage-17/internal-semantic-gateway-contract-v1.md).
   Запрос содержит concept, ephemeral
   candidate ID и bounded provider content; ответ содержит только candidate ID,
   typed verdict и bounded evidence codes.
4. Gateway deployment задаётся opaque `deploymentId`. Travel Assistant не
   интерпретирует его как model slug и проверяет, что response вернулся от того
   же deployment и contract version.
5. Точная модель, provider, quantization, hardware и inference settings
   принадлежат gateway deployment. Их смена не меняет Travel Assistant code или
   public wire contract.
6. `FAKE` остаётся default. Network adapter активируется только явной
   конфигурацией, content approval, exact HTTPS endpoint, secret и image-host
   allowlist.
7. Автоматические retries и fallback между models/providers запрещены. Ошибка,
   schema drift или deployment drift возвращают существующий typed `failed`
   outcome. Резервный deployment переключается оператором после отдельной
   проверки.
8. Вначале выбирается один production deployment для coarse и deep passes.
   Multi-model routing разрешается только отдельным решением после измеренного
   выигрыша качества или стоимости.

## Contract v1

Request:

- `schema_version=1`;
- opaque `deployment_id`;
- managed `concept`;
- список candidates с ephemeral ID, bounded hotel name, descriptions,
  amenities и максимум тремя разрешёнными HTTPS image URL.

Response:

- тот же `schema_version` и `deployment_id`;
- ровно типизированные `candidate_id`, `verdict`, `source` и `signal`;
- без свободного rationale, provider IDs, session/search/offer IDs или
  пользовательского сообщения.

## Последствия

- Self-hosted и корпоративные модели подключаются за gateway без изменений
  orchestration.
- OpenRouter можно отключить или удалить отдельным будущим этапом без изменения
  application port.
- Смена механизма auth, включая workload identity или mTLS, остаётся локальным
  infrastructure/composition изменением. Текущий adapter использует bearer
  secret и не утверждает готовность конкретной корпоративной identity scheme.
- Gateway implementation, deployment manifests, GPU/runtime и model weights не
  входят в этот ADR и выбираются корпоративной платформой.
- Public API/OpenAPI, taxonomy `GLAMPING`, ranking и provider mapping не
  меняются.

## Рассмотренные альтернативы

- **Использовать OpenRouter contract как внутренний стандарт.** Отклонено:
  переносит provider-specific routing и multimodal wire в корпоративную
  границу.
- **Добавить direct adapter для каждой модели.** Отклонено: credentials,
  transport и model capabilities начали бы разрастаться в Travel Assistant.
- **Автоматически выбирать модель в Travel Assistant.** Отклонено: снижает
  воспроизводимость evaluation и усложняет безопасный rollback.

## Границы

ADR не разрешает REAL semantic calls, передачу provider content, model bake-off,
deployment manifests, durable storage или rollout. Каждый такой шаг требует
закрытых rights/infrastructure gates и отдельного решения.
