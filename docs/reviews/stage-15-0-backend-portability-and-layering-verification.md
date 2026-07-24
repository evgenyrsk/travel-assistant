# Stage 15.0 — портируемость backend и границы слоёв

## Scope

- проверить, что business behavior и интеграции принадлежат Kotlin + Ktor backend;
- зафиксировать deployment-ограничения process-local stores;
- добавить source-level защиту dependency direction без новой библиотеки;
- усилить frontend guard против provider, LLM, DTO и ranking symbols;
- исправить stale-утверждение architecture baseline об отсутствии `ADR-0001`.

## Out of scope

- изменение product API или business behavior;
- durable storage, distributed coordination и horizontal scaling;
- Docker, Kubernetes, cloud-манифесты и vendor-specific deployment;
- логи, metrics и operational endpoints последующих sub-stage.

## Карта ответственности

| Область | Владеет |
|---|---|
| `domain/` | Provider-independent hotel/assistant модели, инварианты ranking и business facts |
| `application/` | Validation, session/search/confirmation lifecycle, ranking use cases, LLM/provider orchestration и boundary interfaces |
| `api/` | Тонкий Ktor transport: request parsing, HTTP status и mapping application results |
| `infrastructure/` | OpenRouter и Hotels API adapters, transport DTO, provider mapping и runtime configuration |
| `app/` | Presentation/demo shell и local reverse proxy только к `/api/v1/**`; business rules и provider contracts запрещены |

## Deployment-вывод

Backend запускается как Java 17 process, получает bind address и port из
`HOST`/`PORT`, а provider/LLM mode — из environment. `app/` не является runtime-
зависимостью backend. Поэтому service process может быть размещён в
произвольной внутренней инфраструктуре с Java 17 и разрешённым outbound access.

Текущие session, confirmation, search и idempotency stores хранятся в process
memory. До отдельного persistence-этапа допустим только single-instance
deployment: restart теряет активные сессии, а независимые replicas не
координируют state. Horizontal scaling, HA и rolling replacement без session loss
не заявлены.

## Architecture guards

`BackendLayeringArchitectureTest` сканирует production Kotlin imports и блокирует:

- зависимости Domain от Application, API, Infrastructure и Ktor;
- зависимости Application от API, Infrastructure и Ktor;
- прямые зависимости API от Infrastructure.

Frontend static test блокирует provider hosts/secrets, transport DTO, LLM contracts,
ranking policies и backend use-case symbols. Это не заменяе code review, но создаёт
воспроизводимый regression gate для текущего module layout.

## Проверки

- `./gradlew test --tests 'com.travelassistant.backend.BackendLayeringArchitectureTest'`;
- `npm test` с усиленным frontend boundary guard;
- `git diff --check`.

## Verdict

`PASSED_WITH_SINGLE_INSTANCE_CONSTRAINT`.

Stage 15.1–15.2 может добавить request correlation и operational events, не
перенося business behavior из backend.
