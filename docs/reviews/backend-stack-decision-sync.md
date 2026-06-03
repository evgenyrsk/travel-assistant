# Синхронизация решения по backend stack

Дата: 2026-06-03

## 1. Краткое решение

Подтвержденный целевой backend stack Travel Assistant: Kotlin + Ktor.

Java/Spring Boot не является принятым backend stack для Travel Assistant. Существующий Java/Spring Boot skeleton в `services/backend/` считается архитектурным расхождением до отдельной correction задачи.

Любое будущее изменение backend stack требует явного architecture decision / ADR и согласованной с roadmap задачи.

## 2. Контекст

Глобальный audit `docs/reviews/project-consistency-audit.md` выявил Critical blocker: Stage 7 implementation нельзя безопасно продолжать из-за конфликта между документированным Kotlin + Ktor направлением и фактическим Java/Spring Boot skeleton в `services/backend/`.

Эта задача синхронизирует documentation/governance уровень. Она не исправляет файлы реализации и не начинает Stage 7.2.

## 3. Проверенные файлы

- `docs/reviews/project-consistency-audit.md`
- `README.md`
- `AGENTS.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/ARCHITECTURE.md`
- `docs/architecture/README.md`
- `docs/architecture/architecture-baseline.md`
- `docs/product/README.md`
- `docs/architecture/stage-5/*`
- `docs/architecture/stage-6/*`
- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`
- `docs/prompts/codex-rules.md`
- `docs/prompts/task-template.md`
- `docs/prompts/review-template.md`
- `.github/pull_request_template.md`
- `docs/decisions/README.md`
- `docs/guides/documentation-style-guide.md`

## 4. Измененные файлы

- `README.md`
- `AGENTS.md`
- `.github/pull_request_template.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/ARCHITECTURE.md`
- `docs/architecture/README.md`
- `docs/architecture/architecture-baseline.md`
- `docs/architecture/stage-6/openapi-contract-notes.md`
- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`
- `docs/prompts/codex-rules.md`
- `docs/prompts/task-template.md`
- `docs/prompts/review-template.md`
- `docs/reviews/backend-stack-decision-sync.md`

## 5. Подтвержденный backend stack

- Backend stack: Kotlin + Ktor.
- Domain/application logic должны оставаться независимыми от Ktor.
- Ktor routing должен быть тонким framework layer над application/domain use cases.
- Java/Spring Boot не должен добавляться или продолжаться без явного ADR и согласованной с roadmap задачи.
- Historical stage artifacts и future/reference development docs не должны использоваться как текущий источник истины по stack, если существуют `docs/roadmap/roadmap.md` и `docs/architecture/architecture-baseline.md`.

## 6. Оставшийся drift

- `services/backend/` остается Java/Spring Boot skeleton.
- `services/backend/README.md` остается run notes для фактического Stage 7.1 skeleton.
- `docs/architecture/stage-7/stage-7-1-backend-skeleton-review.md` остается историческим review фактического Java/Spring Boot skeleton.
- Некоторые historical Stage 6 review artifacts могут упоминать Spring Boot как контекст своего времени. Они оставлены как audit trail, если не являются текущим источником истины по stack.

## 7. Что не исправлялось в этой задаче

- Implementation files не менялись.
- Backend skeleton не переписывался.
- `services/backend/` не удалялся.
- Java/Spring/Ktor mismatch исправлен на documentation/governance уровне, но не исправлен в коде.
- Stage 7.2 и будущие этапы не активировались.
- Roadmap не переписывался; внесены только минимальные status/guardrail уточнения.
- MVP scope не расширялся.
- ADR не создавался.

## 8. Рекомендуемая следующая задача

Следующая задача: backend skeleton correction.

Цель следующей задачи: привести `services/backend/` в соответствие с подтвержденным stack Kotlin + Ktor или явно зафиксировать другой путь через ADR, если пользователь решит изменить stack.

После correction задачи нужен отдельный restart readiness review перед Stage 7.2 или любой дальнейшей implementation работой.

## 9. Результаты проверки

- `git diff --check` — passed.
- `git diff --no-index --check /dev/null docs/reviews/backend-stack-decision-sync.md` — whitespace warnings отсутствуют; exit code `1` ожидаем для сравнения нового файла с `/dev/null`.
- Поиск существующих repository-level markdown/documentation linters — отдельные `package.json`, `Makefile`, `pyproject.toml`, `mkdocs`, `markdownlint`, `remark` или `vale` tooling не найдены.
- Ручная проверка scope по итоговому diff — implementation files не изменялись; изменения ограничены документацией и governance.

## 10. Scope control

Эта задача ограничена documentation/governance synchronization. Она подтверждает stack decision и фиксирует оставшееся расхождение, но не устраняет его в файлах реализации.

## 11. Примечание после Stage 7.0b

Stage 7.0b заменил Java/Spring Boot skeleton в `services/backend/` на минимальный Kotlin + Ktor skeleton. Разделы выше сохраняются как исторический handoff Stage 7.0a и описывают состояние на момент stack decision sync.
