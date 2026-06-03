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
- Не начинай Stage 7.2 или broader implementation work из-за recommendations в review artifacts.

## Текущий cleanup context

| Документ | Роль | Как читать |
|---|---|---|
| `documentation-redundancy-structure-audit.md` | Current active review context / Stage 7.0e audit | Последний deep audit структуры документации. Он подтверждает, что документация еще не clean/non-redundant, и рекомендует staged cleanup. |
| `stage-7-status-navigation-sync-cleanup.md` | Completed cleanup report / Stage 7.0f-a | Подтверждает, что stale active wording про pending restart readiness review удален из active/navigation/source-of-truth docs. |
| `stage-7-reviews-index-historical-labeling-cleanup.md` | Completed cleanup report / Stage 7.0f-b | Фиксирует создание этого reviews index и минимальную historical role labeling работу. |
| `stage-7-prompt-governance-deduplication-cleanup.md` | Completed cleanup report / Stage 7.0f-c | Фиксирует deduplication prompt/governance guidance вокруг `AGENTS.md`. |

## Inventory review artifacts

| Документ | Классификация | Статус роли |
|---|---|---|
| `project-consistency-audit.md` | Historical audit trail | Stage 7 audit, который выявил backend stack blocker. Содержит old Java/Spring Boot state as-of audit time; current stack superseded by Kotlin + Ktor correction. |
| `backend-stack-decision-sync.md` | Historical audit trail / completed cleanup report | Stage 7.0a documentation/governance sync. Верхний контекст исторический; postscript фиксирует Stage 7.0b correction. |
| `backend-skeleton-correction.md` | Completed cleanup report | Stage 7.0b report о замене Java/Spring Boot skeleton на Kotlin + Ktor skeleton. |
| `stage-7-restart-readiness-review.md` | Historical readiness gate / reference-only after Stage 7.0f-a | Readiness review passed with minor notes. Subsequent status/navigation sync handled by Stage 7.0f-a. |
| `product-baseline-status-cleanup.md` | Completed cleanup report | Follow-up cleanup after restart readiness review; confirms product baseline status wording was updated. |
| `documentation-redundancy-structure-audit.md` | Current active review context | Stage 7.0e audit and current source for remaining documentation cleanup sequence. |
| `stage-7-status-navigation-sync-cleanup.md` | Completed cleanup report | Stage 7.0f-a narrow cleanup. Confirms Stage 7 is no longer blocked by backend stack drift or restart readiness review. |
| `stage-7-reviews-index-historical-labeling-cleanup.md` | Completed cleanup report | Stage 7.0f-b narrow cleanup. Confirms reviews index and role labeling. |
| `stage-7-prompt-governance-deduplication-cleanup.md` | Completed cleanup report | Stage 7.0f-c narrow cleanup. Confirms `AGENTS.md` as canonical governance and `docs/prompts/**` / `.github/**` as secondary guidance. |
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

Этот index не завершает broad documentation cleanup. Остаются отдельные future cleanup candidates:

- style guide stale wording cleanup;
- development docs merge/shortening;
- product/architecture index role labels;
- roadmap readability cleanup;
- broader documentation redundancy cleanup.

Эти items не являются active backlog и требуют отдельных явных roadmap-aligned задач.
