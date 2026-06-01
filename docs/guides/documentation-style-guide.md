# Documentation Style Guide

## 1. Purpose

Этот style guide задает единые правила для документации Travel Assistant перед контролируемым refactoring.

Он нужен, чтобы:

- сохранить единый язык и понятный стиль;
- сделать структуру документации предсказуемой;
- повысить читаемость для человека и AI/code agents;
- отделить актуальный baseline от исторических артефактов;
- защитить проект от scope drift, преждевременной реализации и случайного старта Stage 6;
- уменьшить дублирование guardrails без потери важных ограничений.

Style guide не меняет product requirements, architecture decisions, порядок roadmap, статусы этапов или MVP scope. Stage 6 остается в статусе `Planned / not started` до отдельной явной задачи.

## 2. Documentation Language

Основной язык документации - русский.

Английские термины допустимы, если они являются:

- названием файла, директории, технологии или инструмента;
- устоявшимся техническим термином;
- roadmap/status термином, который осознанно используется в проекте;
- названием архитектурного паттерна или артефакта;
- термином, перевод которого ухудшает точность.

Связующий текст должен быть написан нормальным русским языком. Не нужно механически переводить `MVP`, `ADR`, `OpenAPI`, `provider`, `frontend`, `backend` или названия файлов, но фразы вокруг них должны быть естественными.

Избегай гибридных формулировок вида:

Bad:

- Stage должен remain Planned until explicit activation.
- Development docs are future reference material and not active backlog.
- Guardrails должны prevent scope leakage across docs.

Good:

- Этап должен оставаться в статусе `Planned / not started` до отдельной явной задачи.
- Development docs являются справочными материалами для будущей реализации и не являются активным backlog.
- Guardrails должны защищать границы задачи, но не делать документы нечитаемыми.

Если английский термин повторяется часто, его нужно использовать последовательно и при первом важном упоминании объяснить по-русски.

## 3. Terminology Rules

| Term | Preferred Usage | Notes |
|---|---|---|
| roadmap | Можно не переводить. Использовать для `docs/roadmap/roadmap.md` и progression этапов. | `docs/roadmap/roadmap.md` - primary roadmap и source of truth по статусам этапов. |
| stage | Можно использовать как часть названий `Stage 0`, `Stage 6`. В связующем тексте допустимо "этап". | Не смешивать в одной фразе без необходимости: лучше "Stage 6 остается в статусе `Planned / not started`" или "Этап 6 остается в статусе `Planned / not started`". |
| scope | Лучше объяснять по-русски как "границы", "объем", "рамки задачи". | Термин `MVP scope` допустим, если важно сохранить связь с roadmap. |
| baseline | Можно не переводить, но пояснять как "актуальное состояние". | Использовать для current product/UX/architecture baseline, не для любого historical document. |
| quality gate | Можно не переводить, если речь о review/checkpoint. | При первом упоминании пояснять: "контрольная проверка качества". |
| carryover | Можно не переводить, если это established roadmap term. | Означает переносимые вопросы, ограничения или решения; не является implementation backlog. |
| guardrails | Можно не переводить, но использовать осторожно. | Это защитные ограничения. Не размазывать длинные guardrails по всем документам. |
| ADR | Не переводить. | Accepted ADR - принятое архитектурное решение. Candidate или draft не являются accepted ADR. |
| MVP | Не переводить. | MVP v1 остается hotel-only. |
| provider | Можно не переводить. | Provider facts приходят от provider/source data, а не от LLM. |
| OpenAPI | Не переводить. | OpenAPI contracts нельзя создавать без отдельного roadmap step. |
| API contract | Можно использовать как technical term. | Provider abstraction не является API contract. |
| implementation backlog | Лучше объяснять как "активный список задач реализации". | Справочные документы о будущей реализации не должны выглядеть как активный implementation backlog. |
| current-session shortlist | Можно использовать как established product term. | Это shortlist текущей сессии, не account history и не persistent saved trips. |
| account history | Можно использовать как established product term. | Вне MVP v1; не создавать auth/account storage без отдельного решения. |

## 4. Document Roles

- `README.md` - entry point и карта проекта. Он должен помогать понять, что читать первым, а не дублировать весь roadmap.
- `docs/roadmap/roadmap.md` - primary roadmap и source of truth по stage status, progression, boundaries, carryover и следующему разрешенному шагу.
- `docs/ROADMAP.md` - краткий навигационный overview этапов. Он не является конкурирующим source of truth по текущим статусам.
- `docs/product/*` - product artifacts и product baseline. Stage documents сохраняют traceability, но не всегда являются актуальным MVP baseline.
- `docs/architecture/*` - architecture baseline и architecture artifacts. Stage 5 documents являются текущим conceptual architecture baseline до отдельного будущего решения.
- `docs/decisions/*` - accepted ADRs, drafts, candidates и decision inventory. Эти категории должны быть явно разделены.
- `docs/development/*` - справочные development materials для будущей реализации до явной активации implementation. Они не являются активным backlog.
- `docs/reviews/*` - quality gates и audit trail. Reviews не заменяют roadmap и не должны становиться списком задач.
- `docs/prompts/*` - шаблоны задач для AI/code agents. Они должны следовать primary roadmap и AGENTS.md.
- `AGENTS.md` - рабочие правила для AI/code agents. Он может содержать ключевые guardrails, но не должен дублировать все подробности каждого документа.

## 5. Baseline vs Historical Artifacts

Actual baseline - это актуальное состояние продукта, UX, архитектуры, roadmap или process rules, на которое можно опираться перед новой задачей.

Historical stage artifacts - документы, созданные в рамках этапов. Они сохраняют traceability и объясняют, как появились решения, но могут содержать superseded-контекст или материалы только для будущих этапов.

Reviews - проверочные документы. Они фиксируют findings, verdict, risks и audit trail. Они не являются roadmap и не должны быть источником текущего следующего шага.

Carryover - переносимые ограничения, вопросы и решения. Carryover не является активным backlog и не означает разрешение выполнять работу будущего этапа.

Future candidates - идеи, decisions или ADR candidates, которые могут стать актуальными позже. Они не являются текущими задачами и не считаются принятыми решениями.

В документации это должно отражаться через явные role labels:

- `Current baseline`;
- `Historical artifact`;
- `Quality gate`;
- `Carryover`;
- `Future reference`;
- `ADR candidate`;
- `Accepted ADR`.

## 6. Roadmap Style Rules

Roadmap показывает:

- этапы и статусы;
- цели этапов;
- ключевые deliverables;
- quality gates;
- carryover;
- следующий разрешенный шаг;
- краткие stage guardrails.

Roadmap не является:

- task tracker;
- implementation backlog;
- product requirements document;
- architecture specification;
- ADR registry;
- archive всех деталей этапов.

Roadmap должен оставаться читаемым. Он не должен дублировать полные product/architecture docs. Детали требований, UX и архитектуры должны жить в соответствующих baseline или stage artifact documents.

Future stages не являются активным backlog. Planned items нельзя выполнять без явной активации. Stage 6 должен оставаться в статусе `Planned / not started` до отдельной явной задачи, которая активирует Stage 6 planning/scope definition.

## 7. Product Documentation Style Rules

Product docs должны разделять:

- актуальный MVP baseline;
- historical stage artifacts;
- superseded scope;
- future expansion context;
- review/audit trail.

Hotel-only MVP boundary должен оставаться явным:

- MVP v1 включает hotel search flow;
- flights outside MVP;
- combined itinerary outside MVP;
- booking outside MVP;
- payment outside MVP;
- current-session shortlist only;
- no account history yet.

Facts, assumptions и unknowns должны быть разделены:

- provider facts приходят от provider/source data;
- user-provided constraints приходят от пользователя;
- assistant assumptions должны быть явно обозначены;
- unknown data не нужно заполнять догадками LLM.

Пользовательские сценарии и требования должны быть написаны читаемым русским языком. Английские термины допустимы, если они являются established terms или названиями артефактов.

## 8. Architecture Documentation Style Rules

Architecture docs должны описывать conceptual architecture и architecture baseline.

Они не должны:

- превращать provider abstraction в API contract;
- создавать преждевременные OpenAPI/API contracts;
- создавать DB schema/storage model без отдельного roadmap step;
- превращать NFR в DevOps/security/testing backlog;
- выбирать provider/vendor/tool без отдельного решения;
- создавать production implementation plan, если это не активировано roadmap.

LLM не создает provider facts. LLM может интерпретировать, объяснять, ранжировать, резюмировать и уточнять, но не должен выдумывать provider facts. Architecture docs должны сохранять разделение user-provided constraints, provider facts, assistant assumptions и unknown data.

## 9. Decisions / ADR Style Rules

Decisions / ADR docs должны явно разделять:

- Accepted ADRs - принятые архитектурные решения;
- Draft ADRs - черновики решений, которые еще не приняты;
- ADR candidates - возможные будущие ADR;
- Non-ADR decision inventory - список архитектурных guardrails, deferred decisions и context notes, который не является набором accepted ADR.

Future decision candidates не являются принятыми решениями. Формулировки должны избегать впечатления, что candidate уже accepted.

ADR должны фиксировать решения и rationale. ADR не должен быть backlog, task list или implementation plan.

## 10. Guardrails Style Rules

Guardrails должны быть централизованы.

Критичные guardrails должны оставаться в:

- `docs/roadmap/roadmap.md`;
- `AGENTS.md`;
- этом style guide;
- релевантных baseline/index documents в краткой форме.

Не нужно дублировать длинные списки ограничений во всех документах. В stage docs лучше использовать короткую локальную формулировку и ссылку на source of truth.

Guardrails должны защищать scope, но не делать документы нечитаемыми. Если guardrail повторяется в нескольких местах, один источник должен быть canonical, а остальные места должны содержать краткий reminder.

## 11. Writing Quality Rules

- Пиши короткими абзацами.
- Используй понятные заголовки.
- Не злоупотребляй таблицами: таблица нужна для сравнения, статусов или traceability, а не для каждого списка.
- Не используй канцелярит.
- Не пиши в стиле "AI generated": меньше механических повторов, больше ясных человеческих формулировок.
- Избегай повторов между README, roadmap, indexes, reviews и stage docs.
- Каждый документ должен иметь purpose.
- Каждый index должен помогать навигации и объяснять роли документов.
- Каждый review должен быть audit trail, а не roadmap.
- Если документ содержит длинный reference section, добавляй короткое summary перед ним.

## 12. Refactoring Safety Rules

Documentation refactoring не должен менять смысл.

Во время refactoring запрещено:

- менять product requirements;
- менять architecture decisions;
- менять roadmap status;
- менять порядок этапов roadmap;
- расширять MVP scope;
- возвращать flights, combined itinerary, booking или payment в MVP;
- начинать Stage 6;
- создавать Stage 6 deliverables;
- создавать API/OpenAPI contracts;
- создавать DB schema/storage model;
- создавать implementation backlog;
- превращать future ADR candidates в accepted ADR;
- удалять historical audit trail.

Refactoring должен сохранять ссылки, traceability и audit trail. Если ссылка меняется или документ получает новую роль, это должно быть явно отражено в navigation/index docs.
