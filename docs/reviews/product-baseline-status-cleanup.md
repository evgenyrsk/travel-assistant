# Cleanup статуса product baseline

Дата: 2026-06-03

## Цель cleanup

Устранить stale status wording в `docs/product/product-baseline.md`, найденный в Stage 7 restart readiness review, без изменения продуктовых требований, MVP scope, roadmap, архитектуры или implementation files.

## Что было устаревшим

В `docs/product/product-baseline.md` раздел `## 2. Текущий статус продукта` все еще фиксировал:

- `Stage 6 - Planned / not started`;
- что продукт находится перед Stage 6 planning/scope definition;
- что API/OpenAPI contracts еще не начинались.

Эти формулировки противоречили текущему состоянию после завершения Stage 6, backend stack correction и Stage 7 restart readiness review.

## Что было исправлено

- Stage 6 обновлен до завершенного contract/design phase.
- Stage 7 отмечен как `In progress` после corrective stabilization и restart readiness review.
- Добавлено краткое уточнение, что Java/Spring Boot skeleton drift исправлен на минимальный Kotlin + Ktor backend skeleton.
- Зафиксировано, что Stage 7 restart readiness review прошел с minor notes.
- Зафиксировано, что дальнейшая Stage 7 implementation работа, включая Stage 7.2, требует отдельной явной roadmap-aligned задачи.

## Какие файлы изменены

- `docs/product/product-baseline.md`
- `docs/reviews/product-baseline-status-cleanup.md`

## Что намеренно не менялось

- Backend/frontend implementation.
- Backend skeleton.
- OpenAPI contracts.
- Roadmap.
- Architecture decisions.
- Backend stack.
- Product requirements.
- MVP boundaries.
- Business scenarios, use cases и functional requirements.
- ADR.

## Результаты проверки

- `git diff --check` — passed.
- Ручная проверка `docs/product/product-baseline.md` подтвердила, что stale wording `Stage 6 - Planned / not started` больше не содержится в файле.

## Подтверждение границ cleanup

- Backend/frontend implementation не менялись.
- Stage 7.2 не начинался.
- Roadmap не переписывался.
- MVP scope не расширялся.
- Backend stack не менялся.
- Cleanup ограничен stale product baseline status wording и коротким cleanup report.

## Рекомендуемый следующий шаг

Следующий безопасный шаг: отдельной явной roadmap-aligned задачей активировать Stage 7.2 или ближайшую следующую Stage 7 implementation task.
