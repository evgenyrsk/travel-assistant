# Reviews и audit trail

Этот раздел содержит review, audit, readiness и cleanup reports проекта Travel Assistant.

`docs/reviews/**` является audit trail: эти документы фиксируют состояние, findings, решения cleanup-задач и рекомендации на момент конкретной проверки. Они не являются primary roadmap, product baseline, architecture baseline, ADR registry, implementation backlog или разрешением выполнять future work.

## Источники истины

Для текущих задач используй источники в таком порядке:

- `docs/roadmap/roadmap.md` — primary roadmap, статусы этапов, progression, carryover и следующий разрешенный шаг.
- `docs/product/product-baseline.md` — актуальный product/MVP baseline.
- `docs/architecture/architecture-baseline.md` — актуальный architecture baseline и backend stack authority.
- `AGENTS.md` — обязательные правила работы Codex/AI-агентов в этом репозитории.
- `docs/decisions/README.md` — ADR/decision taxonomy; accepted ADR files отсутствуют, пока отдельная задача их не создаст.

Если review artifact конфликтует с current roadmap или baseline, приоритет имеют current source-of-truth документы. Review artifact следует читать как исторический контекст, если он явно не является последним cleanup/readiness gate для текущей задачи.

## Как Codex должен использовать этот раздел

- Перед documentation cleanup задачами используй последние relevant reviews как context, но не выполняй их recommendations без отдельной явной задачи.
- Не трактуй historical findings как active backlog.
- Не переписывай historical review reports только потому, что их status wording устарел; вместо этого обновляй index/navigation или создавай новый cleanup report.
- Не считай old Java/Spring Boot references текущим backend stack: текущий backend stack зафиксирован как Kotlin + Ktor в `docs/architecture/architecture-baseline.md`.
- Не начинай broader implementation work из-за recommendations в review artifacts.

## Текущий cleanup context

| Документ | Роль | Как читать |
|---|---|---|
| `documentation-redundancy-structure-audit.md` | Review context / Stage 7.0e audit | Deep audit структуры документации. Его findings читаются как context для bounded cleanup tasks, а не как open-ended blocker. |
| `stage-7-status-navigation-sync-cleanup.md` | Completed cleanup report / Stage 7.0f-a | Подтверждает, что stale active wording про pending restart readiness review удален из active/navigation/source-of-truth docs. |
| `stage-7-reviews-index-historical-labeling-cleanup.md` | Completed cleanup report / Stage 7.0f-b | Фиксирует создание этого reviews index и минимальную historical role labeling работу. |
| `stage-7-prompt-governance-deduplication-cleanup.md` | Completed cleanup report / Stage 7.0f-c | Фиксирует deduplication prompt/governance guidance вокруг `AGENTS.md`. |
| `stage-7-development-docs-merge-shortening-cleanup.md` | Completed cleanup report / Stage 7.0f-d | Фиксирует сокращение `docs/development/**` до secondary reference layer. |
| `stage-7-product-architecture-index-role-labels-cleanup.md` | Completed cleanup report / Stage 7.0f-e | Фиксирует role labels и source-of-truth hierarchy для `docs/product/**` и `docs/architecture/**`. |
| `stage-7-roadmap-readability-cleanup.md` | Completed cleanup report / Stage 7.0f-f | Фиксирует сокращение roadmap-facing status wording и подтверждает, что roadmap remains source of truth. |

## Stage 7 implementation reports

| Документ | Роль | Как читать |
|---|---|---|
| `stage-7-2-backend-application-foundation.md` | Completed implementation report / Stage 7.2 | Минимальная Kotlin + Ktor backend application foundation без business logic, provider integration, DB/storage, frontend или generated clients. |
| `stage-7-3-assistant-session-creation-boundary.md` | Completed implementation report / Stage 7.3 | Минимальный assistant session creation use-case boundary без persistence, LLM orchestration или provider integration. |
| `stage-7-4-assistant-message-intake-boundary.md` | Completed implementation report / Stage 7.4 | Минимальный assistant message intake boundary без assistant replies, clarification flow, persistence, LLM orchestration или provider integration. |
| `stage-7-5-minimal-clarification-response-boundary.md` | Completed implementation report / Stage 7.5 | Минимальный placeholder clarification reply на message intake response без stateful clarification flow, LLM, requirements extraction, storage или provider integration. |
| `stage-7-6-local-assistant-session-state-boundary.md` | Completed implementation report / Stage 7.6 | Process-local assistant session state boundary без durable persistence, retrieval endpoint, message history, LLM, provider integration или frontend/generated clients. |
| `stage-7-7-session-local-clarification-state-boundary.md` | Completed implementation report / Stage 7.7 | Session-local clarification state metadata boundary без real clarification logic, requirements extraction, dynamic questions, durable persistence, LLM или provider integration. |
| `stage-7-8-internal-hotel-requirements-slot-metadata-boundary.md` | Completed implementation report / Stage 7.8 | Internal hotel requirements slot metadata boundary без requirements extraction, slot filling, dynamic clarification, durable persistence, LLM или provider integration. |
| `stage-7-9-internal-slot-coverage-clarification-planning-boundary.md` | Completed implementation report / Stage 7.9 | Internal slot coverage / clarification planning boundary без requirements extraction, slot filling, dynamic clarification, durable persistence, LLM или provider integration. |
| `stage-7-10-backend-api-contract-alignment-checkpoint.md` | Completed review report / Stage 7.10 | Backend API / contract alignment checkpoint без runtime changes, OpenAPI rewrite, generated clients или future behavior activation. |
| `stage-7-11-assistant-api-runtime-contract-alignment-cleanup.md` | Completed implementation report / Stage 7.11 | Assistant API runtime contract alignment cleanup без requirements extraction, slot filling, generated clients, LLM, provider integration, DB/storage или frontend. |

## Inventory review artifacts

| Документ | Классификация | Статус роли |
|---|---|---|
| `project-consistency-audit.md` | Historical audit trail | Stage 7 audit, который выявил backend stack blocker. Содержит old Java/Spring Boot state as-of audit time; current stack superseded by Kotlin + Ktor correction. |
| `backend-stack-decision-sync.md` | Historical audit trail / completed cleanup report | Stage 7.0a documentation/governance sync. Верхний контекст исторический; postscript фиксирует Stage 7.0b correction. |
| `backend-skeleton-correction.md` | Completed cleanup report | Stage 7.0b report о замене Java/Spring Boot skeleton на Kotlin + Ktor skeleton. |
| `stage-7-restart-readiness-review.md` | Historical readiness gate / reference-only after Stage 7.0f-a | Readiness review passed with minor notes. Subsequent status/navigation sync handled by Stage 7.0f-a. |
| `product-baseline-status-cleanup.md` | Completed cleanup report | Follow-up cleanup after restart readiness review; confirms product baseline status wording was updated. |
| `documentation-redundancy-structure-audit.md` | Review context | Stage 7.0e audit and context for bounded remaining documentation cleanup candidates. |
| `stage-7-status-navigation-sync-cleanup.md` | Completed cleanup report | Stage 7.0f-a narrow cleanup. Confirms Stage 7 is no longer blocked by backend stack drift or restart readiness review. |
| `stage-7-reviews-index-historical-labeling-cleanup.md` | Completed cleanup report | Stage 7.0f-b narrow cleanup. Confirms reviews index and role labeling. |
| `stage-7-prompt-governance-deduplication-cleanup.md` | Completed cleanup report | Stage 7.0f-c narrow cleanup. Confirms `AGENTS.md` as canonical governance and `docs/prompts/**` / `.github/**` as secondary guidance. |
| `stage-7-development-docs-merge-shortening-cleanup.md` | Completed cleanup report | Stage 7.0f-d narrow cleanup. Confirms `docs/development/**` as secondary future/reference guidance. |
| `stage-7-product-architecture-index-role-labels-cleanup.md` | Completed cleanup report | Stage 7.0f-e narrow cleanup. Confirms product/architecture source-of-truth hierarchy and index role labels. |
| `stage-7-roadmap-readability-cleanup.md` | Completed cleanup report | Stage 7.0f-f narrow cleanup. Confirms primary roadmap readability cleanup without Stage 7.2 activation. |
| `stage-7-2-backend-application-foundation.md` | Completed implementation report | Stage 7.2 minimal Kotlin + Ktor backend application foundation. |
| `stage-7-3-assistant-session-creation-boundary.md` | Completed implementation report | Stage 7.3 minimal assistant session creation use-case boundary. |
| `stage-7-4-assistant-message-intake-boundary.md` | Completed implementation report | Stage 7.4 minimal assistant message intake boundary. |
| `stage-7-5-minimal-clarification-response-boundary.md` | Completed implementation report | Stage 7.5 minimal placeholder clarification reply on assistant message intake. |
| `stage-7-6-local-assistant-session-state-boundary.md` | Completed implementation report | Stage 7.6 process-local assistant session state boundary. |
| `stage-7-7-session-local-clarification-state-boundary.md` | Completed implementation report | Stage 7.7 session-local clarification state metadata boundary. |
| `stage-7-8-internal-hotel-requirements-slot-metadata-boundary.md` | Completed implementation report | Stage 7.8 internal hotel requirements slot metadata boundary. |
| `stage-7-9-internal-slot-coverage-clarification-planning-boundary.md` | Completed implementation report | Stage 7.9 internal slot coverage / clarification planning boundary. |
| `stage-7-10-backend-api-contract-alignment-checkpoint.md` | Completed review report | Stage 7.10 backend API / contract alignment checkpoint. |
| `stage-7-11-assistant-api-runtime-contract-alignment-cleanup.md` | Completed implementation report | Stage 7.11 assistant API runtime contract alignment cleanup. |
| `pre-stage-6-documentation-consistency-review.md` | Historical audit trail | Pre-Stage 6 review. Status wording was correct at the time; not current source of truth. |
| `roadmap-structure-and-process-fitness-review.md` | Historical audit trail | Pre-Stage 6 roadmap/process review. Some recommendations were later addressed or superseded. |
| `global-documentation-quality-review.md` | Historical audit trail / partly superseded | Broad pre-Stage 6 quality review. Findings remain useful context, but status and baseline layer are partly superseded by later work. |
| `documentation-refactoring-plan.md` | Reference-only / partly superseded | Older controlled refactoring plan. Product/architecture baselines and later Stage 7 cleanup reports supersede parts of it. Preserve as planning audit trail, not active backlog. |

## Current/latest документы

Для текущей documentation stabilization цепочки читать в таком порядке:

1. `docs/roadmap/roadmap.md`
2. `docs/product/product-baseline.md`
3. `docs/architecture/architecture-baseline.md`
4. `docs/reviews/documentation-redundancy-structure-audit.md`
5. `docs/reviews/stage-7-status-navigation-sync-cleanup.md`
6. `docs/reviews/README.md`
7. `docs/reviews/stage-7-reviews-index-historical-labeling-cleanup.md`
8. `docs/reviews/stage-7-prompt-governance-deduplication-cleanup.md`
9. `docs/reviews/stage-7-development-docs-merge-shortening-cleanup.md`
10. `docs/reviews/stage-7-product-architecture-index-role-labels-cleanup.md`
11. `docs/reviews/stage-7-roadmap-readability-cleanup.md`
12. `docs/reviews/stage-7-2-backend-application-foundation.md`
13. `docs/reviews/stage-7-3-assistant-session-creation-boundary.md`
14. `docs/reviews/stage-7-4-assistant-message-intake-boundary.md`
15. `docs/reviews/stage-7-5-minimal-clarification-response-boundary.md`
16. `docs/reviews/stage-7-6-local-assistant-session-state-boundary.md`
17. `docs/reviews/stage-7-7-session-local-clarification-state-boundary.md`
18. `docs/reviews/stage-7-8-internal-hotel-requirements-slot-metadata-boundary.md`
19. `docs/reviews/stage-7-9-internal-slot-coverage-clarification-planning-boundary.md`
20. `docs/reviews/stage-7-10-backend-api-contract-alignment-checkpoint.md`
21. `docs/reviews/stage-7-11-assistant-api-runtime-contract-alignment-cleanup.md`

## Historical / superseded документы

Следующие документы сохраняются как audit trail и не должны переписываться без отдельной задачи:

- `pre-stage-6-documentation-consistency-review.md`
- `roadmap-structure-and-process-fitness-review.md`
- `global-documentation-quality-review.md`
- `documentation-refactoring-plan.md`
- `project-consistency-audit.md`
- `backend-stack-decision-sync.md`
- `backend-skeleton-correction.md`
- `stage-7-restart-readiness-review.md`
- `product-baseline-status-cleanup.md`

Некоторые из них содержат stale status wording, old Stage 6 status или old Java/Spring Boot context. Это не ошибка само по себе: они были correct as-of review time. Их текущая роль задается этим index-документом и current source-of-truth documents.

## Remaining cleanup items

Этот index не превращает documentation cleanup в open-ended blocker. Остаются только bounded future cleanup candidates, если отдельная roadmap-aligned задача их активирует:

- style guide broader wording polish;
- broader documentation redundancy cleanup.

Эти items не являются active backlog и требуют отдельных явных roadmap-aligned задач.
