# Записи архитектурных решений

Эта директория предназначена для ADR — записей о значимых архитектурных решениях Travel Assistant.

## Когда создавать ADR

ADR нужен, если решение:

- меняет архитектурные границы продукта;
- влияет на публичные контракты;
- выбирает важную технологию или провайдера;
- задает долгосрочный подход к AI/LLM-оркестрации;
- определяет способ интеграции с travel API;
- влияет на хранение данных, безопасность или кроссплатформенность.

## Что не является ADR

- продуктовая постановка Этапа 0;
- список открытых вопросов;
- предварительная рекомендация без принятого решения;
- обычное обновление навигации или документации.

## Текущий статус

Standalone ADR-файлы пока не созданы.

Stage 5 создал non-ADR inventory архитектурных решений в `docs/architecture/stage-5/architecture-decisions-draft.md`. Этот документ фиксирует подтвержденные архитектурные guardrails, отложенные решения и future ADR candidates, но не создает accepted ADR и не активирует будущие решения.

## Accepted ADRs

Нет.

## Drafts / Candidates / Non-ADR Decision Inventory

- `docs/architecture/stage-5/architecture-decisions-draft.md` — non-ADR decision inventory Stage 5. Это architecture baseline context, а не список accepted ADR и не implementation backlog.

## Future Decisions

Будущие ADR нужно создавать только когда отдельная явная задача принимает или меняет решение, требующее ADR. Future ADR candidates из Stage 5 остаются candidates до такого решения.
