# Шаблон задачи для Codex/opencode

Используй этот шаблон для маленьких атомарных задач Travel Assistant. Заполняй только то, что нужно для конкретной задачи, но всегда фиксируй текущий этап и границы задачи.

Canonical repository governance находится в `AGENTS.md`. Этот шаблон не заменяет `AGENTS.md`; он только помогает сформулировать конкретную задачу.

## Контекст

- Краткий контекст задачи:
- Связанная веха:
- Связанные решения или обсуждения:
- Canonical repository governance: `AGENTS.md`
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

- Соблюдать `AGENTS.md`.
- Не менять:
  -
  -
- Не начинать следующий этап:
- Не трогать unrelated files:

## Критерии приемки

-
-
-

## Ограничения по roadmap и границам задачи

Не дублируй полный набор global rules из `AGENTS.md`. Здесь укажи только task-specific constraints:

-

## Формат финального отчета

Если задача не задает другой формат, используй обязательный формат из `AGENTS.md`. Если нужен task-specific отчет, перечисли разделы здесь:

1. Созданные файлы
2. Изменённые файлы
3. Краткое описание изменений
4. Принятые решения
5. Открытые вопросы
6. Рекомендации, не выполнены
