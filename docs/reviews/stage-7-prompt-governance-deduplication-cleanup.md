# Stage 7 Prompt/Governance Deduplication Cleanup

Дата: 2026-06-03

## Цель cleanup

Убрать конкурирующие и дублирующиеся Codex/governance правила из prompt templates, GitHub templates и navigation docs, сохранив `AGENTS.md` как canonical source repository governance.

Cleanup является узкой documentation stabilization задачей. Он не начинает Stage 7.2, не выполняет broad documentation refactoring, не удаляет и не объединяет документы, не меняет architecture decisions, ADR, OpenAPI, backend/frontend code или MVP scope.

## Что было проблемой

Правила для Codex и contributors повторялись в нескольких местах: `AGENTS.md`, `docs/prompts/**`, `.github/**` и README/navigation docs. Это создавало риск, что будущий agent или contributor прочитает устаревший prompt/template как competing governance source.

Основной риск был не в отсутствии правил, а в слишком большом количестве похожих правил с разной детализацией.

## Где были найдены дублирующиеся правила

- `docs/prompts/codex-rules.md` повторял roadmap control, scope control, ADR control, backend stack governance, documentation language и final report guidance из `AGENTS.md`.
- `docs/prompts/task-template.md` содержал длинные блоки запретов, частично повторяющие `AGENTS.md`.
- `docs/prompts/review-template.md` частично повторял global governance вместо того, чтобы оставаться review-specific checklist.
- `.github/pull_request_template.md` содержал длинный checklist с Codex governance rules.
- `.github/ISSUE_TEMPLATE/codex_task.yml` содержал повторяющиеся forbidden changes и final report format.
- `README.md` называл `docs/prompts/codex-rules.md` местом общих правил roadmap, границ задачи, ADR и отчетности, что могло конкурировать с `AGENTS.md`.

## Какая иерархия governance теперь используется

1. `AGENTS.md` — canonical global repository/agent governance: scope control, roadmap control, documentation language, validation, diff discipline, safe workflow и final response format.
2. `docs/prompts/**` — reusable prompt templates, task/review structures и prompt-writing guidance; не competing rules.
3. `.github/**` — lightweight GitHub issue/PR templates; не long-form Codex governance.
4. `README.md`, `docs/ROADMAP.md`, `docs/roadmap/roadmap.md` — navigation/status/roadmap context only.
5. `docs/development/**` — future/reference material only; не active backlog и не agent governance source.
6. `docs/reviews/**` — audit trail и cleanup reports; не active task backlog.

## Что изменено

- В `AGENTS.md` добавлен раздел `Иерархия governance-документов`.
- `docs/prompts/codex-rules.md` сокращен до prompt companion document, который ссылается на `AGENTS.md` как canonical source.
- `docs/prompts/task-template.md` и `docs/prompts/review-template.md` сохранены как templates, но больше не дублируют полный набор global guardrails.
- `.github/pull_request_template.md` и `.github/ISSUE_TEMPLATE/codex_task.yml` сокращены до lightweight GitHub-facing guidance.
- `README.md` обновлен: canonical rules теперь явно закреплены за `AGENTS.md`, а `docs/prompts/codex-rules.md` описан как prompt companion.
- `docs/ROADMAP.md`, `docs/roadmap/roadmap.md` и `docs/reviews/README.md` минимально обновлены для Stage 7.0f-c trace.

## Что намеренно не менялось

- Historical review reports и stage artifacts не переписывались.
- Документы не удалялись, не перемещались, не переименовывались и не объединялись.
- Roadmap порядок не менялся.
- Stage 7.2 не активировался.
- Backend/frontend code, OpenAPI, ADR, architecture decisions, provider contracts и MVP scope не менялись.
- `docs/development/**` не переписывались: текущие формулировки остаются future/reference guidance и не конкурируют с `AGENTS.md`.

## Как теперь использовать AGENTS.md, docs/prompts/** и .github/**

- Начинай с `AGENTS.md`, если нужна canonical repository governance.
- Используй `docs/prompts/codex-rules.md` только как reminder для составления prompts, не как полный набор правил.
- Используй `docs/prompts/task-template.md` для постановки атомарных задач.
- Используй `docs/prompts/review-template.md` для review-only задач.
- Используй `.github/**` как GitHub issue/PR templates; длинные Codex rules должны оставаться в `AGENTS.md`.
- Если prompt или template конфликтует с `AGENTS.md`, приоритет имеет `AGENTS.md`.

## Remaining documentation cleanup items

- Style guide stale wording cleanup.
- Development docs merge/shortening.
- Product/architecture index role labels.
- Roadmap readability cleanup.
- Broader documentation redundancy cleanup.

Эти items не являются active backlog и требуют отдельных явных roadmap-aligned задач.

## Final verdict

Passed — prompt/governance guidance дедуплицирован вокруг `AGENTS.md`. `docs/prompts/**` теперь является prompt/template layer, `.github/**` — lightweight GitHub workflow layer, а README/roadmap docs остаются navigation/status context.

Stage 7.2+ не активированы.

## Scope control confirmation

- Cleanup ограничен prompt/governance deduplication и минимальными navigation/status updates.
- Backend/frontend implementation не создавались.
- Stage 7.2 не начинался.
- Product scope и MVP boundaries не менялись.
- Architecture decisions, ADR, OpenAPI и provider/API contracts не менялись.
- Historical reports не переписывались.
- Unrelated files не изменялись.
