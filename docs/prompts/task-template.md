# Шаблон задачи для Codex/opencode

Используй этот шаблон для маленьких атомарных задач Travel Assistant. Заполняй только то, что нужно для конкретной задачи, но всегда фиксируй текущий этап и границы задачи.

## Контекст

- Краткий контекст задачи:
- Связанная веха:
- Связанные решения или обсуждения:
- Primary roadmap и source of truth по статусам: `docs/roadmap/roadmap.md`
- Product baseline, если задача затрагивает MVP scope: `docs/product/product-baseline.md`
- Architecture baseline, если задача затрагивает architecture scope: `docs/architecture/architecture-baseline.md`
- Backend stack для implementation-задач: Kotlin + Ktor, если только будущий ADR явно не меняет это решение.

## Текущий этап

- Этап roadmap:
- Почему задача относится именно к этому этапу:
- Следующий этап, который нельзя начинать:
- Future/reference документы, которые нельзя трактовать как active backlog:

## Задача

Опиши одну конкретную задачу. Задача должна быть достаточно маленькой, чтобы ее можно было выполнить и проверить независимо.

```text
Сделать:
Не делать:
```

## Что прочитать перед началом

- `README.md`
- `AGENTS.md`
- `docs/roadmap/roadmap.md`
- `docs/ROADMAP.md`
- `docs/guides/documentation-style-guide.md`, если задача документационная
- `docs/product/product-baseline.md`, если задача затрагивает продуктовый scope
- `docs/architecture/architecture-baseline.md`, если задача затрагивает архитектурный baseline
- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`
- Релевантные продуктовые и архитектурные документы:
- Релевантные ADR в `docs/decisions/`, если есть:
- Файлы, которые нужно прочитать перед изменением:

## Ожидаемые изменения

- Создать:
- Обновить:
- Проверить:
- Неожидаемые изменения считать разрастанием границ задачи:

## Что нельзя менять

- Roadmap и порядок этапов.
- Следующий этап roadmap.
- Stage 6 deliverables, если Stage 6 не активирован отдельной задачей.
- API/OpenAPI contracts, endpoint specs, DB schema/storage model, auth/security/DevOps/testing backlog и production code, если они не активированы roadmap.
- Unrelated files.
- Реальные интеграции с travel API, если они не входят в текущий этап.
- Реальные интеграции с LLM-провайдерами, если они не входят в текущий этап.
- Production-инфраструктура, если она не входит в текущий этап.
- ADR и архитектурные решения, если задача явно не просит их менять.

## Критерии приемки

- 
- 
- 

## Ограничения по roadmap и границам задачи

- Не переопределять roadmap.
- Не менять порядок этапов roadmap.
- Не начинать следующий этап roadmap без явного запроса.
- Не превращать future/reference documents в active implementation backlog.
- Не начинать backend implementation без сверки backend stack с `docs/architecture/architecture-baseline.md`.
- Не вводить Java/Spring Boot backend implementation без явного ADR и согласованной с roadmap задачи.
- Если файлы реализации конфликтуют с подтвержденным stack Kotlin + Ktor, остановиться и зафиксировать архитектурное расхождение.
- Не превращать ADR candidates или decision inventory в accepted ADR.
- Не расширять границы задачи, если это не требуется для выполнения задачи.
- Не выполнять рекомендации.
- Добавлять будущую работу только в раздел `Рекомендации, не выполнены`.
- Не трогать unrelated files.
- Не добавлять реальные интеграции с travel API, если это явно не требуется текущим этапом roadmap.
- Если задача конфликтует с roadmap или ADR, не выполнять спорную часть и указать конфликт в финальном отчете.

## Формат финального отчета

Финальный отчет должен содержать:

1. Созданные файлы
2. Изменённые файлы
3. Краткое описание изменений
4. Принятые решения
5. Открытые вопросы
6. Рекомендации, не выполнены
