# Проверки и исторический журнал

Этот раздел содержит отчеты о проверках, аудитах, готовности и чистке документации проекта Travel Assistant.

`docs/reviews/**` является историческим журналом: документы фиксируют состояние, выводы, решения задач чистки и рекомендации на момент конкретной проверки. Они не являются основным roadmap, продуктовой или архитектурной основой, реестром ADR, активным списком реализации или разрешением выполнять будущую работу.

## Источники истины

Для текущих задач используй источники в таком порядке:

- `docs/roadmap/roadmap.md` — основной roadmap, статусы и границы этапов, перенесенные пункты и следующий разрешенный шаг.
- `docs/product/product-baseline.md` — актуальная продуктовая основа MVP.
- `docs/architecture/architecture-baseline.md` — актуальная архитектурная основа и источник решения о стеке backend.
- `AGENTS.md` — обязательные правила работы Codex/AI-агентов в этом репозитории.
- `docs/decisions/README.md` — ADR/decision taxonomy; accepted ADR files отсутствуют, пока отдельная задача их не создаст.

Если отчет конфликтует с текущим roadmap или baseline, приоритет имеют действующие документы-источники истины. Отчет следует читать как исторический контекст, если он явно не является последней проверкой для текущей задачи.

## Как Codex должен использовать этот раздел

- Перед documentation cleanup задачами используй последние релевантные reviews как context, но не выполняй их recommendations без отдельной явной задачи.
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
| `stage-7-26-documentation-quality-calibration-audit.md` | Review context / Stage 7.26 audit | Калибрует качество active documentation перед Stage 7.27; findings читать как recommendations для отдельных bounded cleanup tasks, а не как active backlog. |
| `stage-7-27-documentation-governance-rules-cleanup.md` | Completed governance cleanup / Stage 7.27 | Усиливает documentation governance rules для Russian-first prose, roadmap/status readability, checklist/table formatting, document roles и source-of-truth protection. |
| `stage-7-28-roadmap-structure-refactor.md` | Completed roadmap structure cleanup / Stage 7.28 | Сокращает detailed roadmap/status duplication в navigation docs и переводит Stage 7 status в таблицы/checklist без изменения sequencing. |
| `stage-7-29-active-documentation-language-normalization.md` | Completed language normalization cleanup / Stage 7.29 | Нормализует ordinary English prose в active/navigation docs по Russian-first policy без изменения sequencing, product scope, architecture decisions или historical artifacts. |
| `stage-7-30-documentation-final-quality-gate.md` | Completed final quality gate / Stage 7.30 | Финально проверяет documentation stabilization track после Stage 7.26-7.29; verdict: pass with minor notes, next recommended step: Stage 7.31 handoff. |
| `stage-7-31-resume-development-handoff.md` | Completed documentation handoff / Stage 7.31 | Закрывает documentation stabilization handoff и фиксирует guardrails для возвращения к bounded Stage 7 technical work без generated-client readiness claim. |
| `stage-7-roadmap-role-separation-cleanup.md` | Completed roadmap role cleanup | Убирает mutable status matrix из `docs/ROADMAP.md` и закрепляет `docs/roadmap/roadmap.md` как единственный roadmap/status source of truth. |

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
| `stage-7-12-internal-requirements-slot-update-boundary.md` | Completed implementation report / Stage 7.12 | Internal requirements slot update boundary для explicit structured internal input без public API changes, message parsing, requirements extraction, LLM/provider integration или storage. |
| `stage-7-12b-kotlin-style-alignment-cleanup.md` | Completed cleanup report / Stage 7.12b | Kotlin style file split для internal slot update boundary без behavior или public API changes. |
| `stage-7-12c-kotlin-style-alignment-cleanup-review.md` | Completed review report / Stage 7.12c | Review / quality gate для Stage 7.12b cleanup без behavior, OpenAPI или generated-client changes. |
| `stage-7-12d-backend-foundation-consolidation-checkpoint.md` | Completed review report / Stage 7.12d | Backend foundation consolidation checkpoint; recommended generated-client/OpenAPI readiness checkpoint before client generation. |
| `stage-7-13-generated-client-openapi-readiness-checkpoint.md` | Completed review report / Stage 7.13 | Generated-client/OpenAPI readiness checkpoint; verdict not ready for generated clients or OpenAPI finalization. |
| `stage-7-14-generated-client-openapi-readiness-cleanup.md` | Completed cleanup report / Stage 7.14 | Placeholder strategy and error taxonomy readiness cleanup без generated clients, OpenAPI finalization или real hotel search. |
| `stage-7-14-generated-client-openapi-readiness-cleanup-review.md` | Completed review report / Stage 7.14a | Review / quality gate для Stage 7.14 cleanup без behavior, OpenAPI или generated-client changes. |
| `stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup.md` | Completed cleanup report / Stage 7.15 | Assistant response semantics / search readiness boundary cleanup без generated clients, OpenAPI finalization, real hotel search, requirements extraction, LLM/provider integration или storage. |
| `stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup-review.md` | Completed review report / Stage 7.15a | Review / quality gate для Stage 7.15 cleanup; verdict passed with minor documentation/status findings. |
| `stage-7-15b-stage-7-13-7-15-documentation-status-sync.md` | Completed documentation/status sync report / Stage 7.15b | Узкая синхронизация active status wording и reviews index для Stage 7.13-7.15 audit trail без backend behavior или OpenAPI/generated-client work. |
| `stage-7-16-generated-client-openapi-conformance-gate-planning.md` | Completed planning report / Stage 7.16 | План будущего generated-client/OpenAPI conformance gate без реализации gate, OpenAPI changes, generated clients или backend behavior changes. |
| `stage-7-17-generated-client-ready-subset-placeholder-exclusion-policy.md` | Completed policy report / Stage 7.17 | Generated-client-ready subset policy и placeholder exclusion policy без subset config, conformance gate, OpenAPI changes, generated clients или backend behavior changes. |
| `stage-7-18-conformance-gate-skeleton-planning-to-tooling.md` | Completed planning report / Stage 7.18 | Planning-to-tooling форма будущего conformance gate skeleton без реализации gate, subset config, scripts, tests, build tasks, OpenAPI changes, generated clients или backend behavior changes. |
| `stage-7-19-conformance-gate-skeleton-implementation-planning.md` | Completed planning/decision report / Stage 7.19 | Conformance gate skeleton implementation planning / tooling decision без реализации gate, skeleton, subset config, scripts, tests, build tasks, OpenAPI changes, generated clients или backend behavior changes. |
| `stage-7-20-standalone-read-only-conformance-gate-skeleton-implementation.md` | Completed implementation report / Stage 7.20 | Standalone read-only conformance gate skeleton under `tools/openapi-conformance/` с JSON `not_ready` report без generated clients, subset manifest, OpenAPI finalization, backend behavior changes или CI/Gradle integration. |
| `stage-7-20a-standalone-read-only-conformance-gate-skeleton-implementation-review.md` | Completed review report / Stage 7.20a | Review / quality gate для Stage 7.20 standalone read-only conformance gate skeleton; verdict: passed без generated clients, subset manifest, OpenAPI finalization, backend behavior changes или CI/Gradle integration. |
| `stage-7-21-openapi-conformance-report-depth-tests.md` | Completed implementation report / Stage 7.21 | Tool-local read-only conformance report depth and tests без generated-client readiness, subset manifest, OpenAPI finalization, backend behavior changes или CI/Gradle integration. |
| `stage-7-22-generated-client-ready-subset-manifest-planning.md` | Завершенный planning report / Stage 7.22 | Planning по generated-client-ready subset manifest без создания manifest, generated-client readiness, OpenAPI finalization, generated clients, tool changes, backend behavior changes или CI/Gradle integration. |
| `stage-7-23-generated-client-subset-manifest-schema-review.md` | Завершенный planning/review report / Stage 7.23 | Schema contract и future validation behavior для будущего `generated-client-ready-subset.yaml` без создания manifest, generated-client readiness, OpenAPI finalization, generated clients, tool changes, backend behavior changes или CI/Gradle integration. |
| `stage-7-24-openapi-conformance-manifest-validation-design.md` | Завершенный planning/design report / Stage 7.24 | Design будущего manifest detection/schema validation для standalone OpenAPI conformance tool без создания manifest, generated-client readiness, OpenAPI finalization, generated clients, tool changes, backend behavior changes или CI/Gradle integration. |
| `stage-7-25-openapi-conformance-manifest-detection-validation.md` | Завершенный implementation report / Stage 7.25 | Tool-local read-only manifest detection/validation для standalone OpenAPI conformance tool без создания manifest, generated-client readiness, OpenAPI finalization, generated clients, backend behavior changes или CI/Gradle integration. |
| `stage-7-32-resume-stage-7-technical-context-review.md` | Завершенный review/planning report / Stage 7.32 | Восстанавливает technical context после documentation stabilization и рекомендует Stage 7.33 manifest candidate definition без generated-client readiness, generated clients, OpenAPI/API contract changes, CI gate или Stage 8 activation. |
| `stage-7-33-ready-subset-manifest-candidate-definition.md` | Завершенный technical/documentation report / Stage 7.33 | Создает non-readiness `generated-client-ready-subset.yaml` candidate для skeleton validation без generated-client readiness, generated clients, OpenAPI/API contract changes, backend/frontend runtime changes, CI gate или Stage 8 activation. |
| `stage-7-34-manifest-candidate-validation-hardening.md` | Завершенный technical report / Stage 7.34 | Усиливает tool-local manifest validation guardrails против premature readiness promotion signals без generated-client readiness, generated clients, OpenAPI/API contract changes, backend/frontend runtime changes, CI gate или Stage 8 activation. |
| `stage-7-35-endpoint-candidate-review.md` | Завершенный review report / Stage 7.35 | Анализирует endpoint candidates для возможного будущего manifest expansion без изменения manifest, generated-client readiness, OpenAPI/API contracts, backend/frontend runtime, generated clients, CI gate или Stage 8 activation. |
| `stage-7-36-assistant-endpoint-candidate-clarification.md` | Завершенный review report / Stage 7.36 | Уточняет contract/runtime/security/product условия для двух assistant endpoint candidates без изменения manifest, OpenAPI/API contracts, backend/frontend runtime, generated clients, CI gate или Stage 8 activation. |
| `stage-7-37-assistant-endpoint-contract-runtime-alignment-notes.md` | Завершенный review/notes report / Stage 7.37 | Фиксирует alignment, gaps, unknowns и carryover для двух assistant endpoint candidates без изменения manifest, OpenAPI/API contracts, backend/frontend runtime, conformance tool, generated clients, CI gate или Stage 8 activation. |
| `stage-7-38-assistant-endpoint-alignment-cleanup-decision.md` | Завершенный decision/review report / Stage 7.38 | Классифицирует Assistant endpoint gaps по documentation, OpenAPI/contract, backend/runtime tests, conformance/tooling и future-only buckets без implementation, contract, runtime, manifest, generated-client readiness или Stage 8 activation. |
| `stage-7-39-assistant-endpoint-contract-shape-cleanup.md` | Завершенный contract/documentation cleanup report / Stage 7.39 | Уточняет Assistant endpoint contract shape в OpenAPI/contract notes без backend runtime behavior changes, backend tests, conformance tool changes, manifest expansion, generated clients, readiness claim или Stage 8 activation. |
| `stage-7-40-assistant-endpoint-runtime-contract-test-cleanup.md` | Завершенный backend test cleanup report / Stage 7.40 | Уточняет runtime contract tests для Assistant endpoints без production backend behavior changes, OpenAPI contract changes, conformance tool changes, manifest expansion, generated clients, readiness claim или Stage 8 activation. |
| `stage-7-41-assistant-endpoint-conformance-tooling-follow-up-decision.md` | Завершенный decision/review report / Stage 7.41 | Классифицирует будущие Assistant endpoint conformance/tooling checks после Stage 7.39-7.40 без conformance tool implementation, production backend changes, OpenAPI changes, manifest expansion, generated clients, readiness claim или Stage 8 activation. |
| `stage-7-42-assistant-endpoint-conformance-candidate-implementation.md` | Завершенный bounded implementation report / Stage 7.42 | Добавляет static/advisory Assistant endpoint conformance candidate checks без backend runtime HTTP checks, OpenAPI changes, manifest expansion, generated clients, CI/Gradle gate, readiness claim или Stage 8 activation. |
| `stage-7-43-assistant-endpoint-conformance-candidate-verification.md` | Завершенный review-only verification report / Stage 7.43 | Проверяет Stage 7.42 на соответствие Stage 7.41, bounded static/advisory scope и readiness safety; фиксирует один Minor hardening candidate без tool/backend/OpenAPI/manifest/generated-client/CI changes или readiness claim. |
| `stage-7-44-assistant-conformance-shape-guard-hardening.md` | Завершенный bounded implementation report / Stage 7.44 | Закрывает Stage 7.43 findings: отдельно проверяет property presence и required membership для `message`/`nextAction`, добавляет candidate inventory mismatch test и сохраняет advisory/runtime/readiness boundaries. |
| `stage-7-45-assistant-conformance-output-operator-guidance.md` | Завершенный documentation/tooling-guidance report / Stage 7.45 | Документирует запуск и интерпретацию conformance JSON output, Assistant static/advisory checks и non-readiness boundaries без изменения tool logic, tests, backend, OpenAPI, manifest, generated clients или CI/Gradle. |
| `stage-7-46-assistant-conformance-documentation-verification.md` | Завершенный review-only verification report / Stage 7.46 | Подтверждает соответствие Stage 7.45 operator guidance фактическому output, source-of-truth roles и readiness boundaries без изменения README, tool logic/tests или implementation areas. |
| `stage-7-47-stage-7-remaining-scope-review.md` | Завершенный review/decision report / Stage 7.47 | Сверяет весь Stage 7, закрывает дальнейшее дробление Assistant conformance подпотока и возвращает следующий шаг к практическому hotel search flow с `fake provider`. |
| `stage-7-48-minimal-backend-hotel-search-fake-provider.md` | Завершенный backend implementation report / Stage 7.48 | Добавляет process-local hotel search flow и детерминированный `FakeHotelOfferProvider` без real provider, ranking, frontend, generated clients, manifest/CI/tool changes или readiness claims. |
| `stage-7-49-minimal-hotel-offer-ranking.md` | Завершенный backend implementation report / Stage 7.49 | Добавляет deterministic provider-independent ranking и короткий `matchSummary` для fake hotel offers без LLM, real provider, frontend, OpenAPI/generated-client/manifest/CI/tool changes или readiness claims. |
| `stage-7-50-minimal-assistant-to-hotel-search-handoff.md` | Завершенный backend implementation report / Stage 7.50 | Связывает strict explicit Assistant message format с process-local hotel search и ranked offers через `show_hotel_results` / `hotelSearchId` без LLM, real provider, frontend, generated clients, manifest/CI/tool changes или readiness claims. |
| `stage-7-51-minimal-frontend-hotel-search-scenario.md` | Завершенный frontend implementation report / Stage 7.51 | Добавляет отдельную structured hotel search форму, ручной local API client и отображение ranked offers без generated clients, manifest expansion, backend/OpenAPI/tool/CI changes или readiness claims. |
| `stage-7-52-hotel-only-mvp-slice-final-review.md` | Завершенный review/decision report / Stage 7.52 | Подтверждает связность минимального hotel-only slice по коду, контрактам и раздельным automated checks; рекомендует отдельное финальное закрытие Stage 7 без новых implementation claims. |
| `stage-7-53-final-stage-7-closure-and-carryover.md` | Завершенный closure/carryover report / Stage 7.53 | Формально закрывает Stage 7 в границах bounded hotel-only foundation и переносит live E2E, generated clients, manifest, real provider, LLM и production work без readiness overclaim или Stage 8 activation. |
| `pre-stage-8-documentation-consistency-and-language-review.md` | Завершенная документационная проверка / Pre-Stage 8 | Проверяет согласованность активных статусов и ролей документов после Stage 7, устраняет устаревшие ссылки на Stage 7 и выравнивает русскоязычные формулировки без активации Stage 8. |
| `pre-stage-8-chat-first-product-direction-review.md` | Завершенное уточнение направления / Pre-Stage 8 | Фиксирует форму Stage 7.51 как временную technical demo shell, подтверждает целевой chat-first UX и рекомендует отдельный planning-only Stage 8.1 без начала LLM или real provider integration. |
| `stage-8-0-ai-orchestration-entry-review.md` | Завершенный review/planning report / Stage 8.0 | Проверяет точку входа в Stage 8, классифицирует carryover Stage 7 и подтверждает planning-only Stage 8.1 без изменения статуса roadmap, поведения приложения, UI или API contracts. |
| `stage-8-1-llm-client-boundary-design.md` | Завершенный design/review report / Stage 8.1 | Определяет внутреннюю provider-independent границу `LlmClient`, допустимые данные, validation, fallback и fake LLM testing model без кода, runtime changes или real LLM integration. |
| `stage-8-2-internal-llm-client-skeleton.md` | Завершенный backend implementation report / Stage 8.2 | Добавляет application-owned `LlmClient`, internal candidate/result models, validator, deterministic fake и targeted tests без route wiring, network или public API changes. |
| `stage-8-3-internal-llm-orchestration-use-case.md` | Завершенный backend implementation report / Stage 8.3 | Добавляет internal `GenerateLlmCandidateUseCase`, который вызывает `LlmClient`, применяет validator и возвращает typed accepted/fallback result без route wiring или runtime behavior changes. |
| `stage-8-4-internal-assistant-candidate-decision-planning.md` | Завершенный backend implementation report / Stage 8.4 | Добавляет internal `AssistantCandidateDecision` и `PlanAssistantCandidateDecisionUseCase`, которые превращают `LlmCandidateValidationResult` в safe proceed/clarification/fallback decision без route wiring. |
| `stage-8-5-internal-assistant-llm-pipeline-composition.md` | Завершенный backend implementation report / Stage 8.5 | Добавляет internal `PlanAssistantLlmDecisionUseCase`, который соединяет `GenerateLlmCandidateUseCase` и `PlanAssistantCandidateDecisionUseCase` без route wiring или runtime behavior changes. |
| `stage-8-6-internal-natural-language-assistant-handoff-planning.md` | Завершенный review/design report / Stage 8.6 | Определяет mapping из `AssistantCandidateDecision` в future assistant actions и рекомендует Stage 8.7 readiness gate перед route wiring. |
| `stage-8-7-assistant-llm-route-wiring-readiness-gate.md` | Завершенный review-only readiness gate / Stage 8.7 | Проверяет Assistant route/public contract readiness и рекомендует Stage 8.8 как narrow wiring только для clarification/fallback без `ProceedWithCandidate`. |
| `stage-8-8-minimal-assistant-llm-route-wiring.md` | Завершенный backend implementation report / Stage 8.8 | Подключает internal LLM pipeline к assistant runtime только для `AskClarification`/`Fallback`, сохраняет strict `hotel-search;` handoff и откладывает `ProceedWithCandidate` search creation. |
| `stage-8-9-proceed-with-candidate-criteria-contract-review.md` | Завершенный review/design report / Stage 8.9 | Проверяет будущий `ProceedWithCandidate -> hotel search` путь и рекомендует Stage 8.10 как internal criteria validator без route wiring или search creation. |
| `stage-8-10-proceed-candidate-criteria-validator-skeleton.md` | Завершенный backend implementation report / Stage 8.10 | Добавляет internal `ProceedWithCandidate` criteria validator/result model и targeted tests без route wiring, runtime changes или search creation. |
| `stage-8-11-explicit-confirmation-boundary-review.md` | Завершенный review/design report / Stage 8.11 | Фиксирует границу явного confirmation перед будущим `ProceedWithCandidate` search handoff и рекомендует Stage 8.12 как internal confirmation proposal skeleton. |
| `stage-8-12-confirmation-proposal-model-skeleton.md` | Завершенный backend implementation report / Stage 8.12 | Добавляет internal confirmation proposal model/builder для accepted `ProceedWithCandidate` criteria без route wiring, public contract changes или search creation. |
| `stage-8-13-internal-confirmation-planning-composition.md` | Завершенный backend implementation report / Stage 8.13 | Добавляет internal confirmation planning composition для `ProceedWithCandidate` без route wiring, public contract changes или search creation. |
| `stage-8-14-confirmation-prompt-route-wiring-readiness-gate.md` | Завершенный review-only readiness gate / Stage 8.14 | Проверяет readiness для text-only confirmation prompt через existing `ask_clarification` response shape без route wiring или search creation. |
| `stage-8-15-minimal-confirmation-prompt-route-wiring.md` | Завершенный backend implementation report / Stage 8.15 | Подключает internal confirmation planning к assistant route для text-only confirmation prompt без public contract changes, `hotelSearchId` или search creation. |
| `stage-8-16-post-confirmation-handling-boundary-review.md` | Завершенный review/design report / Stage 8.16 | Определяет boundary для будущего post-confirmation handling и рекомендует internal pending confirmation state skeleton без route wiring или search creation. |
| `stage-8-17-pending-confirmation-state-skeleton.md` | Завершенный backend implementation report / Stage 8.17 | Добавляет internal process-local pending confirmation state/store skeleton без route wiring, durable persistence или search creation. |
| `stage-8-18-confirmation-reply-recognition-boundary-review.md` | Завершенный review/design report / Stage 8.18 | Фиксирует правила future explicit confirmation recognition только при active pending state без route wiring, public contract changes или search creation. |
| `stage-8-19-confirmation-reply-classifier-skeleton.md` | Завершенный backend implementation report / Stage 8.19 | Добавляет internal deterministic confirmation reply classifier skeleton без route wiring, pending store wiring или search creation. |
| `stage-8-20-post-confirmation-decision-composition-skeleton.md` | Завершенный backend implementation report / Stage 8.20 | Добавляет internal post-confirmation decision composition поверх active pending state и classifier без route wiring или search creation. |
| `stage-8-21-post-confirmation-route-integration-readiness-gate.md` | Завершенный review/design report / Stage 8.21 | Проверяет readiness для post-confirmation route integration и рекомендует save-only pending confirmation wiring без consuming reply или search creation. |
| `stage-8-22-save-only-pending-confirmation-route-wiring.md` | Завершенный backend implementation report / Stage 8.22 | Подключает save-only `PendingConfirmationStore` wiring для `ConfirmationRequired` без consuming reply, `hotelSearchId` или search creation. |
| `stage-8-23-consuming-confirmation-reply-lifecycle-gate.md` | Завершенный review/design report / Stage 8.23 | Определяет lifecycle и `markConsumed` rules для future consuming confirmation reply wiring без `hotelSearchId` или search creation. |
| `stage-8-24-consuming-confirmation-reply-route-wiring.md` | Завершенный backend implementation report / Stage 8.24 | Подключает consuming confirmation reply route wiring для active pending state без `hotelSearchId`, `show_hotel_results` или search creation. |
| `stage-8-25-confirmed-to-search-creation-readiness-gate.md` | Завершенный review/design report / Stage 8.25 | Проверяет readiness для `PostConfirmationDecision.Confirmed(criteria)` -> hotel search creation и рекомендует internal mapper skeleton перед route search creation. |
| `stage-8-26-criteria-to-search-mapper-skeleton.md` | Завершенный backend implementation report / Stage 8.26 | Добавляет internal `ProceedWithCandidateCriteria` -> `HotelSearchCriteria` mapper skeleton без route wiring, `hotelSearchId`, `show_hotel_results` или search creation. |
| `stage-8-27-mapper-integration-readiness-gate.md` | Завершенный review/design report / Stage 8.27 | Проверяет mapper integration readiness для future confirmed-to-search route wiring и рекомендует internal confirmed-search planning skeleton без search creation. |
| `stage-8-28-confirmed-search-creation-plan-skeleton.md` | Завершенный backend implementation report / Stage 8.28 | Добавляет internal confirmed-search creation plan/use case skeleton без route wiring, `hotelSearchId`, `show_hotel_results`, `CreateHotelSearchUseCase` call или search creation. |
| `stage-8-29-command-construction-readiness-gate.md` | Завершенный review/design report / Stage 8.29 | Проверяет readiness для `ConfirmedSearchCreationPlan -> CreateHotelSearchCommand` и рекомендует internal command builder skeleton без route wiring или search creation. |
| `stage-8-30-confirmed-search-command-builder-skeleton.md` | Завершенный backend implementation report / Stage 8.30 | Добавляет internal confirmed-search command builder skeleton для `AssistantSessionId` + `ReadyToCreateSearch` -> `CreateHotelSearchCommand` без route wiring или search execution. |
| `stage-8-31-confirmed-search-execution-readiness-gate.md` | Завершенный review/design report / Stage 8.31 | Проверяет confirmed-search execution readiness и рекомендует internal execution result/use case skeleton без route wiring или actual search execution. |
| `stage-8-32-confirmed-search-execution-result-skeleton.md` | Завершенный backend implementation report / Stage 8.32 | Добавляет internal confirmed-search execution result/use case skeleton с typed policy и `PreparedButNotExecuted` без route wiring или actual search execution. |
| `stage-8-33-execution-skeleton-integration-gate.md` | Завершенный review/design report / Stage 8.33 | Проверяет integration readiness после `PreparedButNotExecuted` и рекомендует pending-state/idempotency guard skeleton без route wiring или actual execution. |
| `stage-8-34-pending-state-idempotency-guard-skeleton.md` | Завершенный backend implementation report / Stage 8.34 | Добавляет internal pending-state/idempotency guard skeleton для confirmed-search execution без route wiring, state mutation или actual search execution. |
| `stage-8-35-guard-integration-readiness-gate.md` | Завершенный review/design report / Stage 8.35 | Проверяет guard integration readiness и фиксирует, что перед actual execution нужен internal attempt/idempotency skeleton. |
| `stage-8-36-execution-attempt-idempotency-model-skeleton.md` | Завершенный backend implementation report / Stage 8.36 | Добавляет internal execution attempt/idempotency model/use case skeleton без attempt store, route wiring или actual search execution. |
| `stage-8-37-attempt-store-readiness-gate.md` | Завершенный review/design report / Stage 8.37 | Проверяет attempt store readiness и рекомендует process-local store skeleton перед любым actual confirmed-search execution. |
| `stage-8-38-in-memory-attempt-store-skeleton.md` | Завершенный backend implementation report / Stage 8.38 | Добавляет internal process-local `ConfirmedSearchExecutionAttemptStore` skeleton с typed transitions без route wiring или actual search execution. |

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
| `stage-7-12-internal-requirements-slot-update-boundary.md` | Completed implementation report | Stage 7.12 internal requirements slot update boundary. |
| `stage-7-12b-kotlin-style-alignment-cleanup.md` | Completed cleanup report | Stage 7.12b Kotlin style alignment cleanup for internal slot update boundary. |
| `stage-7-12c-kotlin-style-alignment-cleanup-review.md` | Completed review report | Stage 7.12c review / quality gate for Stage 7.12b cleanup. |
| `stage-7-12d-backend-foundation-consolidation-checkpoint.md` | Completed review report | Stage 7.12d backend foundation consolidation checkpoint. |
| `stage-7-13-generated-client-openapi-readiness-checkpoint.md` | Completed review report | Stage 7.13 generated-client / OpenAPI readiness checkpoint. |
| `stage-7-14-generated-client-openapi-readiness-cleanup.md` | Completed cleanup report | Stage 7.14 placeholder strategy and error taxonomy readiness cleanup. |
| `stage-7-14-generated-client-openapi-readiness-cleanup-review.md` | Completed review report | Stage 7.14a generated-client / OpenAPI readiness cleanup review. |
| `stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup.md` | Completed cleanup report | Stage 7.15 assistant response semantics / search readiness boundary cleanup. |
| `stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup-review.md` | Completed review report | Stage 7.15a assistant response semantics / search readiness boundary review. |
| `stage-7-15b-stage-7-13-7-15-documentation-status-sync.md` | Completed documentation/status sync report | Stage 7.15b documentation/status sync for Stage 7.13-7.15 audit trail. |
| `stage-7-16-generated-client-openapi-conformance-gate-planning.md` | Completed planning report | Stage 7.16 generated-client / OpenAPI conformance gate planning. |
| `stage-7-17-generated-client-ready-subset-placeholder-exclusion-policy.md` | Completed policy report | Stage 7.17 generated-client-ready subset / placeholder exclusion policy. |
| `stage-7-18-conformance-gate-skeleton-planning-to-tooling.md` | Completed planning report | Stage 7.18 conformance gate skeleton planning-to-tooling. |
| `stage-7-19-conformance-gate-skeleton-implementation-planning.md` | Completed planning/decision report | Stage 7.19 conformance gate skeleton implementation planning / tooling decision. |
| `stage-7-20-standalone-read-only-conformance-gate-skeleton-implementation.md` | Completed implementation report | Stage 7.20 standalone read-only conformance gate skeleton implementation. |
| `stage-7-20a-standalone-read-only-conformance-gate-skeleton-implementation-review.md` | Completed review report | Stage 7.20a standalone read-only conformance gate skeleton implementation review. |
| `stage-7-21-openapi-conformance-report-depth-tests.md` | Completed implementation report | Stage 7.21 tool-local read-only OpenAPI conformance report depth and tests. |
| `stage-7-22-generated-client-ready-subset-manifest-planning.md` | Завершенный planning report | Stage 7.22 planning по generated-client-ready subset manifest. |
| `stage-7-23-generated-client-subset-manifest-schema-review.md` | Завершенный planning/review report | Stage 7.23 schema review для будущего generated-client-ready subset manifest. |
| `stage-7-24-openapi-conformance-manifest-validation-design.md` | Завершенный planning/design report | Stage 7.24 design будущего manifest detection/schema validation для standalone OpenAPI conformance tool. |
| `stage-7-25-openapi-conformance-manifest-detection-validation.md` | Завершенный implementation report | Stage 7.25 tool-local read-only manifest detection/validation для standalone OpenAPI conformance tool. |
| `stage-7-26-documentation-quality-calibration-audit.md` | Review context / documentation quality audit | Stage 7.26 review-only audit; verdict: needs targeted cleanup before Stage 7.27 governance cleanup. |
| `stage-7-27-documentation-governance-rules-cleanup.md` | Completed governance cleanup | Stage 7.27 documentation-governance cleanup; strengthens rules without roadmap refactor, product/architecture changes, code changes or historical artifact rewrite. |
| `stage-7-28-roadmap-structure-refactor.md` | Completed roadmap structure cleanup | Stage 7.28 roadmap/status structure cleanup; keeps `docs/roadmap/roadmap.md` as detailed source of truth and leaves README / `docs/ROADMAP.md` as navigation docs. |
| `stage-7-29-active-documentation-language-normalization.md` | Completed language normalization cleanup | Stage 7.29 Russian-first cleanup for active/navigation docs без roadmap refactor, product/architecture/code changes or historical artifact rewrite. |
| `stage-7-30-documentation-final-quality-gate.md` | Completed final quality gate | Stage 7.30 review-only final gate; confirms documentation stabilization is ready for Stage 7.31 handoff without code/product/architecture/generated-client changes. |
| `stage-7-31-resume-development-handoff.md` | Completed documentation handoff | Stage 7.31 handoff report; confirms documentation stabilization closure and records guardrails for resumed Stage 7 technical work. |
| `stage-7-32-resume-stage-7-technical-context-review.md` | Завершенный review/planning report | Stage 7.32 restores Stage 7 technical context after documentation stabilization and recommends a bounded Stage 7.33 manifest candidate definition task. |
| `stage-7-33-ready-subset-manifest-candidate-definition.md` | Завершенный technical/documentation report | Stage 7.33 creates the first non-readiness ready-subset manifest candidate and keeps readiness/generation/CI/runtime work out of scope. |
| `stage-7-34-manifest-candidate-validation-hardening.md` | Завершенный technical report | Stage 7.34 hardens manifest candidate validation against premature readiness promotion signals while preserving not_ready/advisory semantics. |
| `stage-7-35-endpoint-candidate-review.md` | Завершенный review report | Stage 7.35 reviews endpoint candidates for future manifest expansion and keeps the manifest/readiness state unchanged. |
| `stage-7-36-assistant-endpoint-candidate-clarification.md` | Завершенный review report | Stage 7.36 clarifies assistant endpoint candidate conditions and recommends contract/runtime alignment before any manifest update. |
| `stage-7-37-assistant-endpoint-contract-runtime-alignment-notes.md` | Завершенный review/notes report | Stage 7.37 фиксирует contract/runtime alignment notes для двух assistant endpoint candidates и рекомендует отдельный cleanup decision перед любым manifest update. |
| `stage-7-38-assistant-endpoint-alignment-cleanup-decision.md` | Завершенный decision/review report | Stage 7.38 классифицирует Assistant endpoint alignment gaps и рекомендует Stage 7.39 contract shape cleanup как отдельную задачу. |
| `stage-7-39-assistant-endpoint-contract-shape-cleanup.md` | Завершенный contract/documentation cleanup report | Stage 7.39 уточняет Assistant endpoint contract shape и оставляет runtime tests, conformance/tooling, manifest expansion и readiness claim для отдельных future stages. |
| `stage-7-40-assistant-endpoint-runtime-contract-test-cleanup.md` | Завершенный backend test cleanup report | Stage 7.40 уточняет Assistant endpoint runtime contract tests и оставляет conformance/tooling, manifest expansion и readiness claim для отдельных future stages. |
| `stage-7-41-assistant-endpoint-conformance-tooling-follow-up-decision.md` | Завершенный decision/review report | Stage 7.41 классифицирует будущие Assistant endpoint conformance/tooling checks и рекомендует Stage 7.42 candidate implementation без readiness claim. |
| `stage-7-42-assistant-endpoint-conformance-candidate-implementation.md` | Завершенный bounded implementation report | Stage 7.42 реализует static/advisory Assistant endpoint candidate checks и рекомендует отдельный Stage 7.43 verification без readiness claim. |
| `stage-7-43-assistant-endpoint-conformance-candidate-verification.md` | Завершенный review-only verification report | Stage 7.43 подтверждает bounded/readiness-safe реализацию Stage 7.42 и рекомендует отдельный narrow Stage 7.44 hardening без readiness claim. |
| `stage-7-44-assistant-conformance-shape-guard-hardening.md` | Завершенный bounded implementation report | Stage 7.44 усиливает property-presence shape guards и negative inventory coverage без runtime checks, manifest/generated-client expansion или readiness claim. |
| `stage-7-45-assistant-conformance-output-operator-guidance.md` | Завершенный documentation/tooling-guidance report | Stage 7.45 добавляет compact operator guidance для conformance output и рекомендует отдельную Stage 7.46 verification без readiness claim. |
| `stage-7-46-assistant-conformance-documentation-verification.md` | Завершенный review-only verification report | Stage 7.46 подтверждает точность operator guidance относительно текущего tool output и рекомендует отдельный Stage 7.47 summary/carryover decision без readiness claim. |
| `stage-7-47-stage-7-remaining-scope-review.md` | Завершенный review/decision report | Stage 7.47 фиксирует обязательный остаток всего Stage 7 и рекомендует практический Stage 7.48 hotel search slice вместо новых conformance микроэтапов. |
| `stage-7-48-minimal-backend-hotel-search-fake-provider.md` | Завершенный backend implementation report | Stage 7.48 реализует минимальный fake-provider hotel search flow и рекомендует следующий практический ranking slice без Stage 7/readiness claims. |
| `stage-7-49-minimal-hotel-offer-ranking.md` | Завершенный backend implementation report | Stage 7.49 ранжирует local offers по availability, rating, total price и stable offer ID, используя существующий `matchSummary` без OpenAPI update или readiness claims. |
| `stage-7-50-minimal-assistant-to-hotel-search-handoff.md` | Завершенный backend implementation report | Stage 7.50 добавляет bounded Assistant-to-search handoff для explicit format, сохраняя ordinary clarification behavior и non-readiness boundaries. |
| `stage-7-51-minimal-frontend-hotel-search-scenario.md` | Завершенный frontend implementation report | Stage 7.51 добавляет минимальный ручной hotel search flow через существующие local backend endpoints без generated clients, manifest expansion или production UI claims. |
| `stage-7-52-hotel-only-mvp-slice-final-review.md` | Завершенный review/decision report | Stage 7.52 сверяет Stage 7.48-7.51, фиксирует отсутствие live browser-to-backend проверки и рекомендует Stage 7.53 как отдельное финальное закрытие/carryover решение. |
| `stage-7-53-final-stage-7-closure-and-carryover.md` | Завершенный closure/carryover report | Stage 7.53 закрывает Stage 7, сохраняет explicit non-production boundaries и оставляет Stage 8 запланированным, но не начатым. |
| `pre-stage-8-documentation-consistency-and-language-review.md` | Завершенная документационная проверка | Подтверждает согласованность активных статусов после закрытия Stage 7, уточняет роли источников истины и выполняет точечную языковую чистку перед отдельным решением о старте Stage 8. |
| `pre-stage-8-chat-first-product-direction-review.md` | Завершенное уточнение направления | Предотвращает трактовку structured form Stage 7.51 как целевого продукта и отделяет будущую LLM orchestration от последующей интеграции предоставленного real provider contract. |
| `stage-8-0-ai-orchestration-entry-review.md` | Завершенный review/planning report | Классифицирует Stage 7 carryover относительно Stage 8 и подтверждает Stage 8.1 как первый безопасный шаг только для планирования без активации реализации. |
| `stage-8-1-llm-client-boundary-design.md` | Завершенный design/review report | Фиксирует design внутреннего `LlmClient` и рекомендует отдельный Stage 8.2 с минимальным contract/fake/test boundary без public runtime wiring. |
| `stage-8-2-internal-llm-client-skeleton.md` | Завершенный backend implementation report | Фиксирует минимальный internal `LlmClient` skeleton и рекомендует отдельный Stage 8.3 orchestration use case без подключения к routes. |
| `stage-8-3-internal-llm-orchestration-use-case.md` | Завершенный backend implementation report | Фиксирует минимальный internal `GenerateLlmCandidateUseCase` и рекомендует отдельный Stage 8.4 decision planning boundary без route wiring. |
| `stage-8-4-internal-assistant-candidate-decision-planning.md` | Завершенный backend implementation report | Фиксирует минимальный internal decision layer и рекомендует отдельный Stage 8.5 internal pipeline composition без route wiring. |
| `stage-8-5-internal-assistant-llm-pipeline-composition.md` | Завершенный backend implementation report | Фиксирует минимальный internal Assistant LLM pipeline composition и рекомендует отдельный Stage 8.6 handoff planning без route wiring. |
| `stage-8-6-internal-natural-language-assistant-handoff-planning.md` | Завершенный review/design report | Фиксирует future action mapping для `AssistantCandidateDecision` и рекомендует отдельный Stage 8.7 readiness gate перед route wiring. |
| `stage-8-7-assistant-llm-route-wiring-readiness-gate.md` | Завершенный review-only readiness gate | Подтверждает conditional readiness для `AskClarification`/`Fallback` и откладывает `ProceedWithCandidate` route wiring. |
| `stage-8-8-minimal-assistant-llm-route-wiring.md` | Завершенный backend implementation report | Фиксирует narrow route wiring для `AskClarification`/`Fallback` через deterministic fake path без public contract changes и без `ProceedWithCandidate` hotel search. |
| `stage-8-9-proceed-with-candidate-criteria-contract-review.md` | Завершенный review/design report | Фиксирует required hotel-search criteria, complete/partial candidate rules и validation gate перед любым будущим `ProceedWithCandidate` search handoff. |
| `stage-8-10-proceed-candidate-criteria-validator-skeleton.md` | Завершенный backend implementation report | Фиксирует validator-only skeleton для complete/partial `ProceedWithCandidate` criteria без public contract changes или search creation. |
| `stage-8-11-explicit-confirmation-boundary-review.md` | Завершенный review/design report | Подтверждает, что accepted LLM criteria должны идти через text-only user confirmation до любого будущего search creation. |
| `stage-8-12-confirmation-proposal-model-skeleton.md` | Завершенный backend implementation report | Фиксирует proposal-model-only skeleton для human-readable confirmation summary без raw candidate leakage или search creation. |
| `stage-8-13-internal-confirmation-planning-composition.md` | Завершенный backend implementation report | Фиксирует composition-only планирование confirmation proposal / clarification / fallback без route wiring или search creation. |
| `stage-8-14-confirmation-prompt-route-wiring-readiness-gate.md` | Завершенный review-only readiness gate | Подтверждает conditional readiness для text-only confirmation prompt без public contract changes или search creation. |
| `stage-8-15-minimal-confirmation-prompt-route-wiring.md` | Завершенный backend implementation report | Фиксирует minimal route wiring для confirmation prompt через existing `ask_clarification` response shape без `hotelSearchId` или search creation. |
| `stage-8-16-post-confirmation-handling-boundary-review.md` | Завершенный review/design report | Фиксирует, что explicit confirmation recognition требует pending validated criteria/state и не должен создавать search без отдельного future step. |
| `stage-8-17-pending-confirmation-state-skeleton.md` | Завершенный backend implementation report | Фиксирует process-local pending confirmation state/store skeleton с expiry/consumed behavior без runtime wiring или search creation. |
| `stage-8-18-confirmation-reply-recognition-boundary-review.md` | Завершенный review/design report | Фиксирует confirmation reply recognition boundary: positive reply допустим только при active pending state, без search creation или public contract changes. |
| `stage-8-19-confirmation-reply-classifier-skeleton.md` | Завершенный backend implementation report | Фиксирует conservative confirmation reply classifier для positive/ambiguous/negative/correction/unknown без pending store wiring или route changes. |
| `stage-8-20-post-confirmation-decision-composition-skeleton.md` | Завершенный backend implementation report | Фиксирует internal post-confirmation decision outcomes для active/missing/expired/consumed pending state без runtime wiring, public contract changes или search creation. |
| `stage-8-21-post-confirmation-route-integration-readiness-gate.md` | Завершенный review/design report | Фиксирует, что consuming route wiring пока не готов, а safe next step — save-only pending confirmation state wiring без search creation. |
| `stage-8-22-save-only-pending-confirmation-route-wiring.md` | Завершенный backend implementation report | Фиксирует save-only pending confirmation route wiring с process-local store, TTL и unchanged public response shape. |
| `stage-8-23-consuming-confirmation-reply-lifecycle-gate.md` | Завершенный review/design report | Фиксирует conditional readiness, lifecycle и outcome mapping для future non-search consuming confirmation reply handling. |
| `stage-8-24-consuming-confirmation-reply-route-wiring.md` | Завершенный backend implementation report | Фиксирует non-search consuming confirmation reply route wiring, lifecycle consume rules и unchanged public response shape. |
| `stage-8-25-confirmed-to-search-creation-readiness-gate.md` | Завершенный review/design report | Фиксирует split-path verdict для future confirmed-to-search creation: сначала internal criteria-to-search mapper skeleton, затем отдельный route wiring gate. |
| `stage-8-26-criteria-to-search-mapper-skeleton.md` | Завершенный backend implementation report | Фиксирует deterministic criteria-to-search mapper skeleton без search side effects, public contract changes или route/runtime wiring. |
| `stage-8-27-mapper-integration-readiness-gate.md` | Завершенный review/design report | Фиксирует, что mapper готов для internal composition, но direct route search creation требует failure/idempotency/lifecycle guardrails. |
| `stage-8-28-confirmed-search-creation-plan-skeleton.md` | Завершенный backend implementation report | Фиксирует internal confirmed-search planning skeleton и lifecycle policy metadata без route/runtime wiring или search side effects. |
| `stage-8-29-command-construction-readiness-gate.md` | Завершенный review/design report | Фиксирует split-path verdict: нужен internal command builder до любого confirmed-search route execution. |
| `stage-8-30-confirmed-search-command-builder-skeleton.md` | Завершенный backend implementation report | Фиксирует session-bound command builder skeleton с lifecycle policy metadata без route/runtime wiring, search execution или public contract changes. |
| `stage-8-31-confirmed-search-execution-readiness-gate.md` | Завершенный review/design report | Фиксирует, что direct route execution не готов: нужны typed execution/failure/idempotency outcomes before route wiring. |
| `stage-8-32-confirmed-search-execution-result-skeleton.md` | Завершенный backend implementation report | Фиксирует internal execution result/use case skeleton: command-ready input produces `PreparedButNotExecuted` with lifecycle/failure/idempotency policy. |
| `stage-8-33-execution-skeleton-integration-gate.md` | Завершенный review/design report | Фиксирует, что actual execution пока не готов: нужен pending-state/idempotency guard до `CreateHotelSearchUseCase` route wiring. |
| `stage-8-34-pending-state-idempotency-guard-skeleton.md` | Завершенный backend implementation report | Фиксирует read-only guard: matching active pending state returns blocked-until-idempotency result; missing/expired/consumed/mismatch rejected. |
| `stage-8-35-guard-integration-readiness-gate.md` | Завершенный review/design report | Фиксирует, что guard достаточен как precondition boundary, но не как permission для `CreateHotelSearchUseCase` call. |
| `stage-8-36-execution-attempt-idempotency-model-skeleton.md` | Завершенный backend implementation report | Фиксирует attempt/idempotency skeleton: prepared attempts и duplicate branches остаются blocked без attempt store или execution policy. |
| `stage-8-37-attempt-store-readiness-gate.md` | Завершенный review/design report | Фиксирует, что process-local attempt store skeleton нужен до actual execution, но сам store не добавляется в Stage 8.37. |
| `stage-8-38-in-memory-attempt-store-skeleton.md` | Завершенный backend implementation report | Фиксирует process-local attempt store skeleton: prepared/in-progress/succeeded/failed transitions и duplicate handling без execution. |
| `stage-8-39-attempt-store-integration-readiness-gate.md` | Завершенный review/design report | Фиксирует, что store достаточен как storage primitive, но перед runtime wiring нужен application-level transition orchestration use case. |
| `stage-8-40-execute-confirmed-search-transition-use-case-skeleton.md` | Завершенный backend implementation report | Фиксирует internal orchestration use case skeleton: guard, attempt planning, store persistence и fake/no-op transition без route wiring или execution. |
| `stage-8-41-confirmed-search-transition-runtime-wiring-readiness-gate.md` | Завершенный review/design report | Фиксирует, что orchestration skeleton полезен, но runtime wiring blocked до explicit consume ordering, attempt TTL, retry policy и response mapping design. |
| `stage-8-42-attempt-lifecycle-and-response-mapping-policy.md` | Завершенный review/design report | Фиксирует explicit attempt lifecycle, TTL/stale, retry, consume ordering и response mapping policy для future wiring без production code changes. |
| `stage-8-43-attempt-ttl-and-stale-detection-model.md` | Завершенный backend implementation report | Фиксирует attempt TTL model (`expiresAt`), `STALE_EXECUTION` failure reason и narrow stale detection в store без retry или route wiring. |
| `stage-8-44-retry-transition-support.md` | Завершенный backend implementation report | Фиксирует retry transition support: `FAILED(STALE_EXECUTION)` и `FAILED(SEARCH_CREATION_FAILED)` allow retry; `FAILED(EXECUTION_STATE_UNKNOWN)` blocks; без route wiring или durable history. |
| `stage-8-45-stage-7-compatibility-proof.md` | Завершенный test-only compatibility report | Фиксирует test-based proof: Stage 7 strict handoff remains единственным search creation path; `ExecuteConfirmedSearchTransitionUseCase` не подключён к runtime; production code не менялся. |
| `stage-8-46-response-mapping-skeleton.md` | Завершенный backend implementation report | Фиксирует internal typed response mapping skeleton: directive model, message kinds и mapper use case без route wiring или actual search results. |
| `stage-8-47-confirmed-search-transition-integration-readiness-gate.md` | Завершенный review/design report | Фиксирует, что Stage 8.40–8.46 skeletons internally coherent, но route wiring blocked из-за consume ordering, integration composition и отсутствия actual execution. |
| `stage-8-48-integration-composition-skeleton.md` | Завершенный backend implementation report | Фиксирует internal integration composition skeleton: orchestration + response mapping + safe message planning + explicit non-consume instruction без route wiring. |
| `stage-8-49-non-results-route-wiring-readiness-gate.md` | Завершенный review/design report | Фиксирует, что Stage 8.50 может безопасно wire non-results composition к runtime при условии test updates и explicit non-consume behavior. |
| `stage-8-50-narrow-non-results-route-wiring.md` | Завершенный backend implementation report | Фиксирует narrow non-results route wiring: composition подключён к Confirmed branch; pending remains active; safe text; без hotelSearchId/show_hotel_results/markConsumed. |
| `stage-8-51-stage-sizing-policy-sync.md` | Завершенный docs-only process sync report | Фиксирует stage sizing policy в `AGENTS.md`: medium-small stages разрешены при одном boundary и risk profile; dangerous work remains split. |
| `stage-8-52-post-wiring-verification-and-actual-execution-readiness-plan.md` | Завершенный review/design report | Фиксирует post-wiring verification: runtime safe; actual execution blocked (B1-B5 chain); recommended sequence 8.53-8.55. |
| `stage-8-53-successful-execution-result-model-and-mapper-support.md` | Завершенный backend implementation report | Фиксирует internal successful result support: `RESULTS_READY` directive, `SHOW_HOTEL_RESULTS` mapping, `CONSUME_AFTER_SUCCESS` instruction, `hotelSearchId` propagation; без actual execution или route changes. |
| `stage-8-54-actual-execution-call-and-succeeded-recording.md` | Завершенный backend implementation report | Фиксирует actual local `CreateHotelSearchUseCase` call, `SUCCEEDED` recording с real `hotelSearchId`, route response с `show_hotel_results`; без `markConsumed`. |
| `stage-8-55-consume-after-success-policy-and-route-cleanup.md` | Завершенный backend implementation report | Фиксирует conditional `markConsumed` после successful search creation; pending consumed after success; duplicate-after-consume через LLM path. |
| `stage-8-56-end-to-end-confirmation-lifecycle-verification.md` | Завершенный review/design report | Фиксирует end-to-end verification полного Stage 8 confirmation lifecycle: happy path, failure/duplicate safety, Stage 7 compatibility; verdict passed with notes; backend core flow closeable. |
| `stage-8-57-stage-8-closure-and-readiness-gate.md` | Завершенный closure/readiness gate report | Формально закрывает Stage 8 как completed backend confirmation lifecycle; фиксирует carryover; verdict passed; следующий шаг — Stage 9 planning. |
| `stage-9-0-documentation-audit-and-stage-9-planning-readiness-review.md` | Завершенный documentation audit / planning readiness review | Проводит documentation audit перед Stage 9, устраняет stale wording в architecture docs, определяет рекомендуемое первое направление Stage 9 и предлагает Stage 9.1 scope. |
| `stage-9-1-hotel-provider-boundary-review-and-adapter-design.md` | Завершенный provider boundary review / adapter design report | Inspect hotel provider boundary, классифицирует boundary ownership, сравнивает 4 adapter design options, рекомендует Option A (сохранить текущий interface, adapter за ним) и определяет Stage 9.2 scope. |
| `stage-9-2-provider-result-contract-and-domain-mapping.md` | Завершенный provider result contract / domain mapping report | Gap analysis по 19 категориям, 12 normalization rules, domain mapping classification; рекомендует сохранить domain model без изменений для Stage 9.3. |
| `stage-9-3-provider-adapter-skeleton-and-fake-real-seam.md` | Завершенный provider adapter skeleton / fake-vs-real seam report | Реализует HotelProviderMode, HotelProviderConfig, RealHotelOfferProviderAdapter skeleton, HotelOfferProviderFactory; обновляет Application.kt; 8 новых tests; все existing tests pass. |
| `stage-9-4-provider-error-taxonomy-and-error-handling.md` | Завершенный provider error taxonomy / error handling report | Реализует HotelProviderErrorCategory (7 categories), HotelProviderException; подтверждает propagation через CreateHotelSearchUseCase и Stage 8 compatibility; 7 новых tests. |
| `stage-9-5-provider-integration-verification.md` | Завершенный provider integration verification report | Verifies FAKE + REAL end-to-end через application composition; 3 targeted integration tests; readiness verdict: ready for provider selection. |
| `stage-9-6-real-provider-selection-and-configuration-design.md` | Завершенный provider selection / configuration design report | Background comparison 7 provider candidates; configuration/secrets/sandbox design; verdict: shortlist selected — owner input required; next stage: 9.7 contract intake. |
| `stage-7-documentation-dedup-sync-cleanup.md` | Completed documentation cleanup report | Conservative dedup/status sync: removes stale active snapshots, consolidates milestone vocabulary, demotes legacy prompt templates to compatibility redirects and keeps historical audit trail intact. |
| `stage-7-roadmap-role-separation-cleanup.md` | Completed roadmap role cleanup | Разделяет `docs/ROADMAP.md` как non-authoritative stage-purpose map и `docs/roadmap/roadmap.md` как authoritative roadmap/status source. |
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
22. `docs/reviews/stage-7-12-internal-requirements-slot-update-boundary.md`
23. `docs/reviews/stage-7-12b-kotlin-style-alignment-cleanup.md`
24. `docs/reviews/stage-7-12c-kotlin-style-alignment-cleanup-review.md`
25. `docs/reviews/stage-7-12d-backend-foundation-consolidation-checkpoint.md`
26. `docs/reviews/stage-7-13-generated-client-openapi-readiness-checkpoint.md`
27. `docs/reviews/stage-7-14-generated-client-openapi-readiness-cleanup.md`
28. `docs/reviews/stage-7-14-generated-client-openapi-readiness-cleanup-review.md`
29. `docs/reviews/stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup.md`
30. `docs/reviews/stage-7-15-assistant-response-semantics-search-readiness-boundary-cleanup-review.md`
31. `docs/reviews/stage-7-15b-stage-7-13-7-15-documentation-status-sync.md`
32. `docs/reviews/stage-7-16-generated-client-openapi-conformance-gate-planning.md`
33. `docs/reviews/stage-7-17-generated-client-ready-subset-placeholder-exclusion-policy.md`
34. `docs/reviews/stage-7-18-conformance-gate-skeleton-planning-to-tooling.md`
35. `docs/reviews/stage-7-19-conformance-gate-skeleton-implementation-planning.md`
36. `docs/reviews/stage-7-20-standalone-read-only-conformance-gate-skeleton-implementation.md`
37. `docs/reviews/stage-7-20a-standalone-read-only-conformance-gate-skeleton-implementation-review.md`
38. `docs/reviews/stage-7-21-openapi-conformance-report-depth-tests.md`
39. `docs/reviews/stage-7-22-generated-client-ready-subset-manifest-planning.md`
40. `docs/reviews/stage-7-23-generated-client-subset-manifest-schema-review.md`
41. `docs/reviews/stage-7-24-openapi-conformance-manifest-validation-design.md`
42. `docs/reviews/stage-7-25-openapi-conformance-manifest-detection-validation.md`
43. `docs/reviews/stage-7-26-documentation-quality-calibration-audit.md`
44. `docs/reviews/stage-7-27-documentation-governance-rules-cleanup.md`
45. `docs/reviews/stage-7-28-roadmap-structure-refactor.md`
46. `docs/reviews/stage-7-29-active-documentation-language-normalization.md`
47. `docs/reviews/stage-7-30-documentation-final-quality-gate.md`
48. `docs/reviews/stage-7-31-resume-development-handoff.md`
49. `docs/reviews/stage-7-32-resume-stage-7-technical-context-review.md`
50. `docs/reviews/stage-7-33-ready-subset-manifest-candidate-definition.md`
51. `docs/reviews/stage-7-34-manifest-candidate-validation-hardening.md`
52. `docs/reviews/stage-7-35-endpoint-candidate-review.md`
53. `docs/reviews/stage-7-36-assistant-endpoint-candidate-clarification.md`
54. `docs/reviews/stage-7-37-assistant-endpoint-contract-runtime-alignment-notes.md`
55. `docs/reviews/stage-7-38-assistant-endpoint-alignment-cleanup-decision.md`
56. `docs/reviews/stage-7-39-assistant-endpoint-contract-shape-cleanup.md`
57. `docs/reviews/stage-7-40-assistant-endpoint-runtime-contract-test-cleanup.md`
58. `docs/reviews/stage-7-41-assistant-endpoint-conformance-tooling-follow-up-decision.md`
59. `docs/reviews/stage-7-42-assistant-endpoint-conformance-candidate-implementation.md`
60. `docs/reviews/stage-7-43-assistant-endpoint-conformance-candidate-verification.md`
61. `docs/reviews/stage-7-44-assistant-conformance-shape-guard-hardening.md`
62. `docs/reviews/stage-7-45-assistant-conformance-output-operator-guidance.md`
63. `docs/reviews/stage-7-46-assistant-conformance-documentation-verification.md`
64. `docs/reviews/stage-7-47-stage-7-remaining-scope-review.md`
65. `docs/reviews/stage-7-48-minimal-backend-hotel-search-fake-provider.md`
66. `docs/reviews/stage-7-49-minimal-hotel-offer-ranking.md`
67. `docs/reviews/stage-7-50-minimal-assistant-to-hotel-search-handoff.md`
68. `docs/reviews/stage-7-51-minimal-frontend-hotel-search-scenario.md`
69. `docs/reviews/stage-7-52-hotel-only-mvp-slice-final-review.md`
70. `docs/reviews/stage-7-53-final-stage-7-closure-and-carryover.md`
71. `docs/reviews/pre-stage-8-documentation-consistency-and-language-review.md`
72. `docs/reviews/pre-stage-8-chat-first-product-direction-review.md`
73. `docs/reviews/stage-7-documentation-dedup-sync-cleanup.md`
74. `docs/reviews/stage-7-roadmap-role-separation-cleanup.md`
75. `docs/reviews/stage-8-0-ai-orchestration-entry-review.md`
76. `docs/reviews/stage-8-1-llm-client-boundary-design.md`
77. `docs/reviews/stage-8-2-internal-llm-client-skeleton.md`
78. `docs/reviews/stage-8-3-internal-llm-orchestration-use-case.md`
79. `docs/reviews/stage-8-4-internal-assistant-candidate-decision-planning.md`
80. `docs/reviews/stage-8-5-internal-assistant-llm-pipeline-composition.md`
81. `docs/reviews/stage-8-6-internal-natural-language-assistant-handoff-planning.md`
82. `docs/reviews/stage-8-7-assistant-llm-route-wiring-readiness-gate.md`
83. `docs/reviews/stage-8-8-minimal-assistant-llm-route-wiring.md`
84. `docs/reviews/stage-8-9-proceed-with-candidate-criteria-contract-review.md`
85. `docs/reviews/stage-8-10-proceed-candidate-criteria-validator-skeleton.md`
86. `docs/reviews/stage-8-11-explicit-confirmation-boundary-review.md`
87. `docs/reviews/stage-8-12-confirmation-proposal-model-skeleton.md`
88. `docs/reviews/stage-8-13-internal-confirmation-planning-composition.md`
89. `docs/reviews/stage-8-14-confirmation-prompt-route-wiring-readiness-gate.md`
90. `docs/reviews/stage-8-15-minimal-confirmation-prompt-route-wiring.md`
91. `docs/reviews/stage-8-16-post-confirmation-handling-boundary-review.md`
92. `docs/reviews/stage-8-17-pending-confirmation-state-skeleton.md`
93. `docs/reviews/stage-8-18-confirmation-reply-recognition-boundary-review.md`
94. `docs/reviews/stage-8-19-confirmation-reply-classifier-skeleton.md`
95. `docs/reviews/stage-8-20-post-confirmation-decision-composition-skeleton.md`
96. `docs/reviews/stage-8-21-post-confirmation-route-integration-readiness-gate.md`
97. `docs/reviews/stage-8-22-save-only-pending-confirmation-route-wiring.md`
98. `docs/reviews/stage-8-23-consuming-confirmation-reply-lifecycle-gate.md`
99. `docs/reviews/stage-8-24-consuming-confirmation-reply-route-wiring.md`
100. `docs/reviews/stage-8-25-confirmed-to-search-creation-readiness-gate.md`
101. `docs/reviews/stage-8-26-criteria-to-search-mapper-skeleton.md`
102. `docs/reviews/stage-8-27-mapper-integration-readiness-gate.md`
103. `docs/reviews/stage-8-28-confirmed-search-creation-plan-skeleton.md`
104. `docs/reviews/stage-8-29-command-construction-readiness-gate.md`
105. `docs/reviews/stage-8-30-confirmed-search-command-builder-skeleton.md`
106. `docs/reviews/stage-8-31-confirmed-search-execution-readiness-gate.md`
107. `docs/reviews/stage-8-32-confirmed-search-execution-result-skeleton.md`
108. `docs/reviews/stage-8-33-execution-skeleton-integration-gate.md`
109. `docs/reviews/stage-8-34-pending-state-idempotency-guard-skeleton.md`
110. `docs/reviews/stage-8-35-guard-integration-readiness-gate.md`
111. `docs/reviews/stage-8-36-execution-attempt-idempotency-model-skeleton.md`
112. `docs/reviews/stage-8-37-attempt-store-readiness-gate.md`
113. `docs/reviews/stage-8-38-in-memory-attempt-store-skeleton.md`
114. `docs/reviews/stage-8-39-attempt-store-integration-readiness-gate.md`
115. `docs/reviews/stage-8-40-execute-confirmed-search-transition-use-case-skeleton.md`
116. `docs/reviews/stage-8-41-confirmed-search-transition-runtime-wiring-readiness-gate.md`
117. `docs/reviews/stage-8-42-attempt-lifecycle-and-response-mapping-policy.md`
118. `docs/reviews/stage-8-43-attempt-ttl-and-stale-detection-model.md`
119. `docs/reviews/stage-8-44-retry-transition-support.md`
120. `docs/reviews/stage-8-45-stage-7-compatibility-proof.md`
121. `docs/reviews/stage-8-46-response-mapping-skeleton.md`
122. `docs/reviews/stage-8-47-confirmed-search-transition-integration-readiness-gate.md`
123. `docs/reviews/stage-8-48-integration-composition-skeleton.md`
124. `docs/reviews/stage-8-49-non-results-route-wiring-readiness-gate.md`
125. `docs/reviews/stage-8-50-narrow-non-results-route-wiring.md`
126. `docs/reviews/stage-8-51-stage-sizing-policy-sync.md`
127. `docs/reviews/stage-8-52-post-wiring-verification-and-actual-execution-readiness-plan.md`
128. `docs/reviews/stage-8-53-successful-execution-result-model-and-mapper-support.md`
129. `docs/reviews/stage-8-54-actual-execution-call-and-succeeded-recording.md`
130. `docs/reviews/stage-8-55-consume-after-success-policy-and-route-cleanup.md`
131. `docs/reviews/stage-8-56-end-to-end-confirmation-lifecycle-verification.md`
132. `docs/reviews/stage-8-57-stage-8-closure-and-readiness-gate.md`
133. `docs/reviews/stage-9-0-documentation-audit-and-stage-9-planning-readiness-review.md`
134. `docs/reviews/stage-9-1-hotel-provider-boundary-review-and-adapter-design.md`
135. `docs/reviews/stage-9-2-provider-result-contract-and-domain-mapping.md`
136. `docs/reviews/stage-9-3-provider-adapter-skeleton-and-fake-real-seam.md`
137. `docs/reviews/stage-9-4-provider-error-taxonomy-and-error-handling.md`
138. `docs/reviews/stage-9-5-provider-integration-verification.md`
139. `docs/reviews/stage-9-6-real-provider-selection-and-configuration-design.md`

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

Некоторые из них содержат stale status wording, old Stage 6 status или old Java/Spring Boot context. Это не ошибка само по себе: они были корректны на момент соответствующего review. Их текущая роль задается этим index-документом и current source-of-truth documents.

## Remaining cleanup items

Этот index не превращает documentation cleanup в open-ended blocker. Остаются только bounded future cleanup candidates, если отдельная roadmap-aligned задача их активирует:

- style guide broader wording polish;
- broader documentation redundancy cleanup.

Эти items не являются active backlog и требуют отдельных явных roadmap-aligned задач.
