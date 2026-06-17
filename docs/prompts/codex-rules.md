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
- Роль затрагиваемых документов: source-of-truth, navigation/index, guide/rules, review/audit artifact или historical artifact.
- Явное указание, если задача меняет source-of-truth; если можно обновить существующий source-of-truth, новый документ не создается.
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

## Documentation cleanup reminders

- Active/source-of-truth documentation uses Russian prose by default; English is reserved for technical terms, paths, commands, APIs, libraries, class names, status labels, commit/review labels and established project terms.
- Ordinary English prose in Russian active documents is a readability issue unless technically necessary.
- `docs/roadmap/roadmap.md` is the detailed roadmap/status source of truth; `docs/ROADMAP.md` and `README.md` must stay navigational.
- Status-heavy docs should use compact tables/checklists instead of long status paragraphs.
- Historical artifacts are preserved as audit trail and are not mass-normalized for language or style.
- Do not combine governance cleanup, roadmap refactor, language normalization and historical labeling unless the task explicitly asks for combined scope.
- Do not do beautification without a verifiable documentation goal.

## Минимальный reusable snippet

```text
Соблюдай AGENTS.md как canonical source repository governance.
docs/prompts/** используй только как prompt/template guidance.
Перед изменением документации определи роль каждого затронутого документа.
Не расширяй scope, не начинай будущие этапы, не создавай competing source of truth и не меняй unrelated files.
Для active/source-of-truth документации используй русский prose; английский оставляй для технических терминов и established labels.
Запусти проверки, указанные в задаче, и явно отрази результат в финальном отчете.
```
