# Global Documentation Quality Review

## 1. Review Context

Этот review проводится перед дальнейшей работой над Stage 6, потому что документация Travel Assistant стала достаточно объемной и сложной для чтения после завершения нескольких продуктовых, UX, архитектурных и процессных этапов.

На момент review:

- Stage 0 - Completed;
- Stage 1 - Completed;
- Stage 2 - Completed;
- Stage 3 - Completed;
- Stage 4 - Completed;
- Stage 4.1 - Completed;
- Stage 5 - Completed;
- Pre-Stage 6 documentation review/cleanup выполнены;
- Roadmap structure review/cleanup выполнены;
- Stage 6 - Planned / not started;
- Code/API/DB/UI implementation - Not started.

Эта задача является review-only. Она не переписывает документацию, не меняет структуру файлов, не меняет продуктовые или архитектурные решения, не начинает Stage 6 и не создает implementation backlog.

Цель review - оценить качество документации как проектной системы: читаемость, структуру, навигацию, единый язык, source-of-truth clarity, разделение актуального baseline и historical stage artifacts, а также безопасные направления будущего controlled refactoring перед Stage 6.

## 2. Review Scope

Проверенные зоны и файлы:

- Root docs: `README.md`, `AGENTS.md`, `docs/PROJECT_BRIEF.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`.
- Primary roadmap: `docs/roadmap/roadmap.md`.
- Reviews: `docs/reviews/pre-stage-6-documentation-consistency-review.md`, `docs/reviews/roadmap-structure-and-process-fitness-review.md`.
- Product docs: `docs/product/README.md`, `docs/product/stage-0/*`, `docs/product/stage-1/*`, `docs/product/stage-2/*`, `docs/product/stage-3/*`, `docs/product/stage-4/*`.
- Architecture docs: `docs/architecture/README.md`, `docs/architecture/stage-5/*`.
- Decisions / ADR: `docs/decisions/README.md`, `docs/architecture/stage-5/architecture-decisions-draft.md`.
- Development docs: `docs/development/roadmap.md`, `docs/development/milestones.md`, `docs/development/implementation-strategy.md`.
- Prompts / agent rules: `docs/prompts/codex-rules.md`, `docs/prompts/task-template.md`, `docs/prompts/review-template.md`.
- GitHub templates: `.github/ISSUE_TEMPLATE/codex_task.yml`, `.github/pull_request_template.md`.

Review проверяет качество документации, а не корректность продуктовых требований или архитектурных решений как таковых.

## 3. Current Documentation Problems

Основные классы проблем:

- Структура: документация выросла stage-by-stage, поэтому текущая карта похожа на набор этапных артефактов, а не на компактную систему baseline + history + reviews.
- Навигация: entry points есть, но читателю не всегда ясно, какие документы читать для актуального состояния, а какие нужны только для traceability.
- Дублирование: roadmap, README, product index, architecture index, development docs, reviews и agent rules повторяют одни и те же guardrails.
- Смешение русского и английского: технические термины часто оправданы, но связующий текст нередко становится гибридным: `future/planned reference material`, `quality gate`, `carryover`, `scope boundaries`, `active backlog`.
- Перегруженность guardrails: защита от premature implementation полезна, но repeated "not API/OpenAPI/DB/storage/auth/DevOps/testing backlog" встречается почти во всех зонах.
- Unclear source of truth: primary roadmap сейчас обозначен достаточно ясно, но development docs и prompt templates все еще могут визуально конкурировать с ним из-за детализации.
- Historical artifacts vs actual baseline: Stage 0-2 и часть Stage 3 documents сохраняют superseded flight/combined context, а Stage 5 documents содержат актуальный architecture baseline, но единого compact baseline layer нет.
- Roadmap vs task tracker risk: primary roadmap улучшен, но вместе с development roadmap/milestones общая система всё еще может читаться как backlog будущей реализации.

## 4. Target Documentation Principles

Целевые принципы для документации Travel Assistant:

- Основной язык документации - русский.
- Английские термины используются осознанно: названия файлов, технологий, артефактов, статусов roadmap, принятые technical terms и термины, где перевод ухудшает смысл.
- Связующий текст пишется нормальным русским языком, без лишнего гибридного канцелярита.
- `docs/roadmap/roadmap.md` - source of truth по этапам, статусам, stage progression, carryover и следующему разрешенному шагу.
- `README.md` - entry point и навигационная карта, не архив и не копия roadmap.
- Stage docs - deliverables и historical artifacts соответствующих этапов; они сохраняют traceability, но не всегда являются актуальным baseline.
- Baseline docs - компактное актуальное состояние продукта/UX/архитектуры, которое читатель может использовать перед новой задачей без чтения всех historical artifacts.
- ADR - только принятые архитектурные решения; candidates, drafts и inventories не должны выглядеть как accepted ADR.
- Reviews - quality gates и audit trail; они не заменяют roadmap и не становятся списком задач.
- Development docs - future/reference material до явной активации реализации; они не являются active backlog.
- Guardrails должны быть централизованы и ссылочно переиспользованы, а не размазаны по всем файлам.
- Любой cleanup должен быть controlled refactoring документации без изменения product scope, architecture decisions, roadmap order или Stage 6 status.

## 5. Findings Summary

| Severity | Count |
|---|---:|
| Critical | 0 |
| Major | 10 |
| Minor | 7 |
| Notes | 4 |

Severity definitions:

- Critical: проблема, которая может привести к roadmap drift, scope creep, потере важных решений или ошибочному старту Stage 6/implementation.
- Major: существенная проблема структуры, навигации, языка, дублирования или читаемости, которую желательно исправить перед Stage 6.
- Minor: локальная readability, wording, naming, link или consistency проблема.
- Note: наблюдение или рекомендация без необходимости немедленного исправления.

## 6. Detailed Findings

### [MJ-001] Нет компактного слоя actual baseline

Severity: Major  
Area: Structure / Baseline clarity  
Files:

- `docs/product/README.md`
- `docs/architecture/README.md`
- `docs/product/stage-3/stage-3-summary-and-carryover.md`
- `docs/product/stage-4/stage-4-summary-and-carryover.md`
- `docs/architecture/stage-5/stage-5-summary-and-carryover.md`

Finding:
Актуальный product/UX/architecture baseline существует, но распределен по stage summaries, carryover, roadmap, architecture README и отдельным stage deliverables. Читателю приходится собирать baseline из нескольких мест и одновременно фильтровать historical/superseded context.

Why it matters:
Перед Stage 6 это повышает риск пропустить важное ограничение, прочитать historical artifact как активное требование или использовать слишком широкий future context как текущий scope.

Recommendation:
В future controlled refactoring создать или выделить компактный baseline layer: например, summary/index documents, которые ссылаются на stage artifacts, но не дублируют их полностью. Не менять требования и решения.

Allowed timing:
- Before Stage 6

### [MJ-002] Product docs смешивают active MVP baseline и historical flight/combined context

Severity: Major  
Area: Product docs / Historical artifacts  
Files:

- `docs/product/stage-0/*`
- `docs/product/stage-1/*`
- `docs/product/stage-2/*`
- `docs/product/stage-3/combined-search-ux-decision.md`
- `docs/product/README.md`

Finding:
Stage 0-2 documents часто содержат flight/combined content с top-level notes о hotel-only refocus и superseded context. Это безопасно с точки зрения scope, но ухудшает читаемость: документы выглядят одновременно как требования, архив и migration notes.

Why it matters:
Новый читатель может читать отдельный Stage 1/2 файл без контекста primary roadmap и принять старые labels или future scenarios за active MVP v1.

Recommendation:
Не переписывать historical docs в рамках review. В controlled cleanup отделить actual product baseline от historical stage artifacts через clear index labels: `Current baseline`, `Historical stage deliverables`, `Superseded / future context`.

Allowed timing:
- Before Stage 6

### [MJ-003] Roadmap остается слишком тяжелым для primary source of truth

Severity: Major  
Area: Roadmap docs  
Files:

- `docs/roadmap/roadmap.md`

Finding:
Primary roadmap уже лучше защищает source-of-truth role, но содержит status table, MVP notes, open decisions, dashboards, full completed stage sections, future stages и governance rules в одном длинном документе. Он частично выполняет роли status page, historical archive, governance document и navigation index.

Why it matters:
Primary roadmap должен быстро отвечать на вопрос "где мы и что можно делать дальше". Когда он становится слишком плотным, риск неправильного чтения растет, несмотря на хорошие guardrails.

Recommendation:
В future cleanup не переписывать roadmap полностью, а облегчить чтение: оставить compact current status, stage map, key links, activation rules и minimal governance; archival detail держать в stage docs/reviews.

Allowed timing:
- Before Stage 6

### [MJ-004] Development docs по форме похожи на implementation backlog

Severity: Major  
Area: Development docs / Process  
Files:

- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`

Finding:
Development docs явно помечены как future/planned reference material, но их уровень детализации включает backend skeleton, API contracts, LLM abstraction, hotel search abstraction, web skeleton, testing, security, observability и Docker/local development.

Why it matters:
Даже с предупреждениями такие документы визуально похожи на backlog реализации. Перед Stage 6 это может подтолкнуть future tasks к API/DB/auth/testing/production scope раньше явной активации.

Recommendation:
В controlled cleanup сохранить эти документы как reference, но усилить их separation from active work: короткий статус наверху, ссылки на primary roadmap, меньше backlog-like wording в заголовках и более явная роль "future reference".

Allowed timing:
- Before Stage 6

### [MJ-005] Guardrails дублируются слишком широко

Severity: Major  
Area: Documentation style / Agent rules  
Files:

- `AGENTS.md`
- `docs/prompts/codex-rules.md`
- `docs/prompts/task-template.md`
- `docs/prompts/review-template.md`
- `docs/roadmap/roadmap.md`
- `docs/product/README.md`
- `docs/architecture/README.md`
- `docs/development/*.md`

Finding:
Одни и те же ограничения повторяются во многих местах: не начинать Stage 6, не создавать API/OpenAPI contracts, DB schema, storage model, auth/security/DevOps/testing backlog, production implementation, provider-specific integration и implementation backlog.

Why it matters:
Повторение защищает проект, но делает документы служебными и тяжелыми. При будущих изменениях легко получить drift: одна копия guardrail обновится, другая останется старой.

Recommendation:
Сохранить guardrails, но централизовать их: один canonical process/style document или compact "Project governance rules", а в остальных документах оставить короткие ссылки и только локально важные ограничения.

Allowed timing:
- Before Stage 6

### [MJ-006] Смешение русского и английского ухудшает читаемость

Severity: Major  
Area: Language and style  
Files:

- `README.md`
- `docs/product/README.md`
- `docs/roadmap/roadmap.md`
- `docs/architecture/stage-5/*`
- `docs/development/*.md`
- `AGENTS.md`

Finding:
Документация регулярно смешивает русский связующий текст с английскими фразами: `future/planned reference material`, `active implementation backlog`, `quality gate`, `carryover`, `readiness`, `source of truth`, `scope guardrails`, `decision-critical unknowns`, `provider facts`.

Why it matters:
Часть английских терминов оправдана, но массовое смешение делает текст менее естественным и сложнее для быстрого чтения человеком.

Recommendation:
Ввести style guide: основной текст на русском, английские термины оставлять для технологий, файлов, артефактов, статусов roadmap и устойчивых technical terms. Для повторяющихся терминов дать glossary и использовать их последовательно.

Allowed timing:
- Before Stage 6

### [MJ-007] Reviews и carryover документы конкурируют с roadmap

Severity: Major  
Area: Reviews / Roadmap process  
Files:

- `docs/reviews/pre-stage-6-documentation-consistency-review.md`
- `docs/reviews/roadmap-structure-and-process-fitness-review.md`
- `docs/product/stage-*/stage-*-consistency-review.md`
- `docs/product/stage-*/stage-*-summary*.md`
- `docs/architecture/stage-5/stage-5-summary-and-carryover.md`

Finding:
Reviews, summaries и carryover documents часто содержат findings, open questions, next steps, recommendations и readiness/verdict. Это полезно как audit trail, но по форме часть таких документов похожа на mini-roadmap.

Why it matters:
Если читатель попадает в review вместо primary roadmap, он может принять historical recommendations за текущий next step.

Recommendation:
Разделить роли: reviews - quality gates и historical audit; roadmap - current status and next step; baseline docs - актуальное состояние. В index pages явно маркировать reviews как historical/quality gate artifacts.

Allowed timing:
- During Stage 6 planning

### [MJ-008] Architecture baseline распределен по множеству Stage 5 deliverables

Severity: Major  
Area: Architecture docs  
Files:

- `docs/architecture/README.md`
- `docs/architecture/stage-5/*`

Finding:
Stage 5 дал сильный architecture baseline, но он разбит на десять документов. Architecture README перечисляет deliverables и guardrails, но не дает краткого "read this first" baseline summary с главными boundaries и decisions.

Why it matters:
Перед Stage 6 нужно быстро понять architecture baseline без чтения всех Stage 5 files. Иначе возрастает риск перепутать conceptual architecture, future implementation и deferred decisions.

Recommendation:
В controlled cleanup добавить compact architecture baseline/index summary или усилить `docs/architecture/README.md`: что актуально, что historical, что deferred, какие files читать первыми.

Allowed timing:
- Before Stage 6

### [MJ-009] Decision inventory может выглядеть как accepted ADR

Severity: Major  
Area: Decisions / ADR  
Files:

- `docs/decisions/README.md`
- `docs/architecture/stage-5/architecture-decisions-draft.md`

Finding:
`docs/decisions/README.md` корректно говорит, что accepted ADRs отсутствуют. Но `architecture-decisions-draft.md` использует формулировки `ADR Candidate` и `Status: Confirmed`, что при чтении в изоляции может выглядеть как accepted ADR.

Why it matters:
Future ADR candidates must not be treated as accepted decisions. Ambiguous wording может привести к premature architecture lock-in или ошибочному выводу, что отдельный ADR уже принят.

Recommendation:
Не менять decisions сейчас. В controlled cleanup унифицировать terminology: `Confirmed architecture guardrail`, `Deferred decision`, `ADR candidate`, `Accepted ADR`. Избегать сочетания `ADR Candidate` + `Confirmed` без пояснения статуса.

Allowed timing:
- Before Stage 6

### [MJ-010] Prompt/task templates слабее отражают current source-of-truth hierarchy

Severity: Major  
Area: Prompts / Agent rules  
Files:

- `docs/prompts/task-template.md`
- `docs/prompts/codex-rules.md`
- `.github/ISSUE_TEMPLATE/codex_task.yml`
- `AGENTS.md`

Finding:
`AGENTS.md` теперь явно задает priority order с primary roadmap. Но reusable task template и GitHub issue template в required reading сильнее подсвечивают development docs и не всегда явно называют `docs/roadmap/roadmap.md` как primary source of truth.

Why it matters:
Практические шаблоны часто копируются в новые задачи. Если они не несут тот же source-of-truth hierarchy, future agents могут стартовать от secondary roadmap и получить implementation bias.

Recommendation:
В future process cleanup синхронизировать templates с `AGENTS.md`: primary roadmap first, development docs as secondary future/planned reference, ADR only if applicable.

Allowed timing:
- Before Stage 6

### [MN-001] README полезен, но карта документации перегружена

Severity: Minor  
Area: Root docs / Navigation  
Files:

- `README.md`

Finding:
README выполняет роль entry point, но documentation map уже длинная и перечисляет много stage directories, reviews, development docs, prompts и templates.

Why it matters:
Root README должен помогать начать чтение, а не требовать понять всю документационную систему сразу.

Recommendation:
В navigation cleanup сократить README до "read first" маршрута и ссылок на section indexes; подробную карту держать в index files.

Allowed timing:
- During Stage 6 planning

### [MN-002] `docs/ROADMAP.md` и `docs/roadmap/roadmap.md` требуют постоянного разграничения

Severity: Minor  
Area: Roadmap docs / Navigation  
Files:

- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `README.md`

Finding:
Два roadmap файла сейчас разграничены: один high-level stage list, другой primary roadmap. Но одинаковое название `ROADMAP` / `roadmap` требует повторных пояснений в README, product index и primary roadmap.

Why it matters:
Новый читатель может открыть верхнеуровневый `docs/ROADMAP.md` и принять его за current status source.

Recommendation:
Не переименовывать сейчас. В future cleanup оставить `docs/ROADMAP.md` очень коротким и явно навигационным; рассмотреть, нужен ли он как отдельный файл, если primary roadmap уже содержит stage map.

Allowed timing:
- Later / future stage

### [MN-003] Index pages не маркируют документы по роли достаточно явно

Severity: Minor  
Area: Navigation / Index docs  
Files:

- `docs/product/README.md`
- `docs/architecture/README.md`
- `docs/decisions/README.md`

Finding:
Index pages перечисляют документы и дают пояснения, но редко используют явные role labels вроде `Current baseline`, `Historical artifact`, `Quality gate`, `Decision inventory`, `Future reference`.

Why it matters:
Без role labels длинные списки сложно сканировать, особенно в product docs, где рядом лежат active baseline, superseded decisions, reviews и summaries.

Recommendation:
В navigation cleanup добавить легкие role labels без перемещения файлов.

Allowed timing:
- Before Stage 6

### [MN-004] Naming и статусы непоследовательны

Severity: Minor  
Area: Language / Consistency  
Files:

- `docs/product/*`
- `docs/architecture/*`
- `docs/roadmap/roadmap.md`

Finding:
В документах смешиваются `Stage` и `Этап`, `Completed` и русские описания, `Planned / not started`, `future`, `Post-MVP`, `outside MVP`, `superseded`, `historical traceability`.

Why it matters:
Смысл в целом понятен, но consistent status vocabulary помог бы быстрее отличать актуальное, историческое и future-only.

Recommendation:
В style guide закрепить небольшой словарь статусов и naming rules. Не менять массово без отдельной normalization task.

Allowed timing:
- During Stage 6 planning

### [MN-005] Reviews не имеют собственного index

Severity: Minor  
Area: Reviews / Navigation  
Files:

- `docs/reviews/*`
- `README.md`
- `docs/roadmap/roadmap.md`

Finding:
`docs/reviews/` содержит pre-Stage 6 review и roadmap structure review, теперь добавляется global documentation review. Отдельного `docs/reviews/README.md` нет.

Why it matters:
Когда review documents становятся отдельной зоной, без index сложно понять порядок чтения и какие reviews уже acted upon.

Recommendation:
Если reviews продолжат использоваться, создать lightweight reviews index в отдельной задаче. Не делать это в рамках текущего review.

Allowed timing:
- During Stage 6 planning

### [MN-006] Длинные таблицы и checklists ухудшают scanability

Severity: Minor  
Area: Readability  
Files:

- `docs/product/stage-2/use-cases.md`
- `docs/product/stage-3/mvp-search-flow-details.md`
- `docs/product/stage-3/required-fields-and-acceptance-criteria.md`
- `docs/architecture/stage-5/architecture-decisions-draft.md`
- `docs/development/milestones.md`

Finding:
Некоторые документы содержат длинные tables/checklists с большим количеством статусов, traceability и boundaries.

Why it matters:
Такая форма полезна для auditability, но тяжелая для человеческого чтения и быстрого принятия контекста.

Recommendation:
В future readability cleanup добавить короткие executive summaries перед длинными tables и сохранить detailed tables как reference.

Allowed timing:
- Later / future stage

### [MN-007] `docs/ARCHITECTURE.md` выглядит устаревшим рядом со Stage 5 baseline

Severity: Minor  
Area: Architecture docs / Navigation  
Files:

- `docs/ARCHITECTURE.md`
- `docs/architecture/README.md`
- `docs/architecture/stage-5/*`

Finding:
`docs/ARCHITECTURE.md` честно называет себя preliminary architecture notes до Stage 5, но Stage 5 уже Completed. Сейчас этот файл является root-level historical/preliminary note, а не architecture baseline.

Why it matters:
Root-level naming может заставить читателя открыть preliminary file вместо Stage 5 baseline.

Recommendation:
В navigation cleanup явно маркировать `docs/ARCHITECTURE.md` как preliminary/historical entry и направлять к `docs/architecture/README.md` для current baseline.

Allowed timing:
- Before Stage 6

### [NT-001] Primary roadmap source-of-truth role в целом восстановлен

Severity: Note  
Area: Roadmap  
Files:

- `docs/roadmap/roadmap.md`
- `AGENTS.md`
- `README.md`

Finding:
После cleanup primary roadmap clearly states current status: Stage 0-5 Completed, Stage 6 Planned / not started, implementation Not started.

Why it matters:
Это снижает риск roadmap drift и защищает Stage 6 от случайного старта.

Recommendation:
Сохранять status changes централизованными в primary roadmap.

Allowed timing:
- No immediate action

### [NT-002] Architecture guardrails защищают ключевые MVP constraints

Severity: Note  
Area: Architecture docs  
Files:

- `docs/architecture/README.md`
- `docs/architecture/stage-5/*`

Finding:
Architecture docs последовательно сохраняют hotel-only MVP, provider facts vs LLM boundary, no API/OpenAPI/DB/storage implementation, current-session shortlist only и future ADR candidate separation.

Why it matters:
Это сильная основа для будущего Stage 6 planning при условии, что carryover не станет backlog.

Recommendation:
Сохранить architecture baseline и улучшать только readability/navigation.

Allowed timing:
- No immediate action

### [NT-003] Reviews дают полезный audit trail

Severity: Note  
Area: Reviews  
Files:

- `docs/reviews/*`
- `docs/product/stage-*/stage-*-consistency-review.md`
- `docs/architecture/stage-5/stage-5-consistency-review.md`

Finding:
Review documents хорошо фиксируют why/when/status и помогают понять, почему Stage 6 еще не начат.

Why it matters:
Это полезно для governance, если reviews явно отделены от roadmap и current baseline.

Recommendation:
Сохранить reviews как quality gates, но улучшить indexing и role labels.

Allowed timing:
- No immediate action

### [NT-004] Critical blockers перед Stage 6 не обнаружены

Severity: Note  
Area: Overall  
Files:

- `docs/roadmap/roadmap.md`
- `docs/product/README.md`
- `docs/architecture/README.md`
- `docs/decisions/README.md`
- `docs/development/*.md`

Finding:
Review не нашел противоречия, которое прямо запускает Stage 6, расширяет MVP scope, создает API/DB/storage model, принимает future ADR candidates как accepted ADR или добавляет implementation backlog.

Why it matters:
Проблема не в correctness baseline, а в читаемости, структуре и долговременной поддерживаемости документации.

Recommendation:
Выполнить controlled documentation refactoring before Stage 6, но не блокировать проект как broken.

Allowed timing:
- No immediate action

## 7. Documentation Structure Review

| Area | Current State | Problem | Recommended Direction |
|---|---|---|---|
| Root docs | README, PROJECT_BRIEF, ARCHITECTURE, ROADMAP дают входные точки. | Root docs частично смешивают current navigation, preliminary notes и future vision. | README оставить коротким entry point; `docs/ARCHITECTURE.md` пометить как preliminary/historical; текущий baseline вести через section indexes. |
| Roadmap docs | `docs/roadmap/roadmap.md` является primary; `docs/ROADMAP.md` high-level. | Primary roadmap тяжелый; два roadmap файла требуют постоянных пояснений. | Сохранить primary roadmap как source of truth, сделать его легче; high-level roadmap оставить максимально коротким или позже объединить/пересмотреть. |
| Product docs | Stage 0-4 deliverables хорошо сохранены и индексированы. | Active MVP baseline смешан с historical/superseded context и reviews. | Выделить current product/UX baseline; stage docs маркировать как historical deliverables. |
| Architecture docs | Stage 5 baseline есть; architecture README появился и помогает. | Baseline распределен по многим файлам; guardrails повторяются. | Усилить compact architecture baseline/index summary без изменения решений. |
| Decisions / ADR | Standalone ADR нет; decisions README объясняет это. | Non-ADR inventory с `ADR Candidate` wording может быть принят за accepted ADR. | Уточнить ADR vocabulary и формат decision inventory. |
| Development docs | Подробные future reference docs. | Визуально похожи на implementation backlog. | Сократить backlog-like framing; оставить как secondary reference under primary roadmap. |
| Reviews | Несколько полезных quality gates. | Нет reviews index; reviews могут читаться как mini-roadmaps. | Добавить lightweight review index/role labels в отдельной задаче. |
| Prompts / agent rules | AGENTS и prompts защищают roadmap/scope. | Guardrails дублируются; templates не полностью синхронизированы с source-of-truth hierarchy. | Централизовать rules и обновить templates после style guide. |

## 8. Language and Style Review

| Pattern | Examples / Locations | Problem | Recommendation |
|---|---|---|---|
| Русско-английский связующий текст | `future/planned reference material`, `active implementation backlog`, `quality gate`, `carryover`, `scope boundaries` в roadmap/development/architecture docs. | Текст читается как служебная смесь, а не как нормальная русская документация. | Основной текст писать по-русски; английские термины оставить в glossary и использовать осознанно. |
| Оправданные английские technical terms | `OpenAPI`, `DTO`, `ADR`, `LLM`, `provider`, `frontend`, `backend`, `MVP`, `Stage`, названия файлов. | Перевод может ухудшить смысл или разорвать связь с артефактами. | Не переводить механически; закрепить допустимые термины в style guide. |
| Слишком длинные guardrail formulas | Повторы `no API/OpenAPI contracts, endpoint specs, DB schema, storage model, auth/security/DevOps/testing backlog...`. | Документы становятся служебными и тяжелыми. | Централизовать полный guardrail list; в локальных документах оставлять короткое применимое ограничение. |
| Непоследовательные статусы | `Completed`, `Planned / not started`, `future`, `outside MVP`, `Post-MVP`, `superseded`, `historical traceability`. | Читателю приходится угадывать разницу между future-only, historical и deferred. | Ввести единый словарь статусов и role labels. |
| Неестественные фразы | `architecture-level quality attributes`, `decision-critical unknowns`, `source-owned facts`, `readiness`, `scope leakage`. | Часть фраз звучит как машинный governance language. | Переписывать только в dedicated language normalization; сохранять technical precision. |
| Заголовки Stage/Этап | `Stage 3`, `Этап 0`, `Stage 4.1`, `Future Stages`. | Не ломает смысл, но снижает единообразие. | Выбрать naming convention: например, `Stage N — русское название`, статусы оставить consistent. |
| Таблицы без summary | Use cases, acceptance criteria, architecture decisions, milestones. | Трудно читать быстро. | Добавлять короткое summary перед длинными tables. |

## 9. Navigation and Source-of-Truth Review

| Document | Intended Role | Current Clarity | Recommendation |
|---|---|---|---|
| `README.md` | Entry point и краткая навигация. | Good but heavy. | Сократить до read-first route и section indexes during navigation cleanup. |
| `docs/ROADMAP.md` | High-level stage list, не current status source. | Mostly clear. | Держать очень коротким; не дублировать status/carryover из primary roadmap. |
| `docs/roadmap/roadmap.md` | Primary roadmap/source of truth. | Clear role, heavy content. | Облегчить структуру без потери governance. |
| `docs/product/README.md` | Product docs index. | Good map, weak role labels. | Добавить labels для current baseline / historical artifacts / quality gates. |
| `docs/architecture/README.md` | Architecture index и Stage 5 baseline entry. | Good, but could be stronger as read-first summary. | Добавить compact current architecture baseline и read order. |
| `docs/decisions/README.md` | ADR governance/index. | Good but fragile around decision inventory. | Уточнить accepted ADR vs non-ADR decision inventory vocabulary. |
| `AGENTS.md` | Canonical agent rules. | Strong but long. | После style guide вынести общие правила в central rule set или сократить дубли с prompts. |
| `docs/development/roadmap.md` | Secondary future/planned development reference. | Clearly framed, still backlog-like. | Подчеркнуть non-active status и не использовать как current task list. |
| `docs/prompts/task-template.md` | Reusable task prompt. | Useful but source hierarchy weaker than AGENTS. | Обновить required reading order to include primary roadmap first. |
| `docs/reviews/*` | Quality gates / audit trail. | Useful but no section index. | Добавить reviews index only if review zone remains active. |

## 10. Recommended Refactoring Strategy

### Step 1 — Documentation Style Guide

Что создать в отдельной будущей задаче:

- единые языковые правила;
- структура документации и роли документов;
- правила naming для Stage/Этап, baseline, reviews, summaries, decisions;
- правила статусов: Completed, Planned / not started, Historical, Superseded, Future-only, Deferred;
- правила использования английских терминов;
- правила guardrails: где находится canonical list, где допустимы короткие локальные reminders.

Это должен быть небольшой style/process document, а не переписывание всей документации.

### Step 2 — Navigation Cleanup

Что поправить в отдельной будущей задаче:

- README как короткий entry point;
- `docs/ROADMAP.md` как truly high-level stage list;
- index-файлы для product, architecture, decisions и reviews;
- ссылки на review documents только там, где они нужны для navigation;
- роли документов: current baseline, historical artifact, quality gate, future reference.

### Step 3 — Roadmap Readability Cleanup

Что поправить в отдельной будущей задаче:

- сделать primary roadmap быстрее читаемым;
- убрать или свернуть лишние дубли dashboards/history;
- оставить governance и Stage 6 activation/exclusions;
- не превратить roadmap в backlog;
- не менять статусы, порядок этапов или scope.

### Step 4 — Product / Architecture Baseline Cleanup

Что поправить в отдельной будущей задаче:

- отделить актуальный product/UX/architecture baseline от historical stage artifacts;
- улучшить summary/index docs;
- пометить superseded/future context без переписывания требований;
- не менять product requirements, MVP scope или architecture decisions.

### Step 5 — Language Normalization

Что поправить в отдельной будущей задаче:

- привести связующий текст к русскому языку;
- оставить только осознанные английские термины;
- не ломать названия файлов, статусов, технологий и accepted technical terms;
- выполнять normalization постепенно, начиная с indexes/README/roadmap, а не со всех stage artifacts.

## 11. Recommended Before Stage 6

Минимальный безопасный набор до Stage 6:

1. Создать маленький documentation style guide с language rules, role labels и canonical guardrails.
2. Обновить navigation/index docs так, чтобы было ясно: что читать как current baseline, что является historical artifact, что является review.
3. Облегчить primary roadmap без изменения roadmap order/status/scope: compact current status, Stage 6 activation/exclusions, short links to stage artifacts.
4. Усилить `docs/architecture/README.md` как architecture baseline entry point.
5. Уточнить ADR/decision terminology, чтобы future ADR candidates не выглядели accepted.
6. Синхронизировать prompt/task templates с `AGENTS.md` source-of-truth hierarchy.

Не рекомендуется перед Stage 6:

- переписывать все stage documents;
- перемещать или переименовывать все файлы;
- превращать cleanup в implementation planning;
- создавать API/OpenAPI contracts, DB schema, storage model или production code;
- менять product scope, architecture decisions или roadmap order.

## 12. Final Verdict

Documentation needs controlled refactoring before Stage 6.

Критических blockers не обнаружено: Stage 6 остается Planned / not started, Stage 0-5 завершены, MVP v1 остается hotel-only, implementation не начат, accepted ADR отсутствуют, future ADR candidates не должны считаться accepted decisions.

Но документация стала слишком тяжелой для дальнейшей работы без controlled cleanup. Главные проблемы - не correctness, а readability, role clarity, duplication, language normalization и separation between current baseline / historical artifacts / reviews / future reference. Перед Stage 6 стоит выполнить ограниченный documentation refactoring, не меняющий продуктовые и архитектурные решения.
