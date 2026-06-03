# Prompt companion для Codex

Этот документ является secondary prompt companion для задач Travel Assistant, выполняемых через Codex или opencode.

Canonical repository governance находится в `AGENTS.md`. Если этот документ конфликтует с `AGENTS.md`, приоритет имеет `AGENTS.md`.

## Как использовать

- Используй этот файл только как помощь при составлении prompts.
- Не вставляй полный текст `AGENTS.md` в каждую задачу.
- В prompt достаточно коротко указать: "Соблюдай `AGENTS.md` как canonical source repository governance".
- Для конкретной задачи добавляй только task-specific context, files, acceptance criteria, validation и explicit out-of-scope items.

## Что стоит включать в task prompt

- Текущий roadmap stage или cleanup task.
- Почему задача относится к этому stage/scope.
- Что сделать и что явно не делать.
- Какие файлы прочитать перед изменениями.
- Какие файлы ожидаемо создать или обновить.
- Какие проверки запустить.
- Формат финального отчета, если он отличается от стандартного `AGENTS.md`.

## Что не стоит дублировать

Не повторяй длинными блоками правила из `AGENTS.md`:

- roadmap control;
- scope control;
- ADR control;
- backend stack governance;
- documentation language;
- validation and diff discipline;
- final response format;
- safe workflow and unrelated-files policy.

Если для задачи важен конкретный guardrail, укажи его коротко как task-specific constraint и оставь ссылку на `AGENTS.md` для полной версии правила.

## Минимальный reusable snippet

```text
Соблюдай AGENTS.md как canonical source repository governance.
docs/prompts/** используй только как prompt/template guidance.
Не расширяй scope, не начинай будущие этапы и не меняй unrelated files.
Запусти проверки, указанные в задаче, и явно отрази результат в финальном отчете.
```
