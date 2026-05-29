# Roadmap Structure and Process Fitness Review

## 1. Review Context

Этот review проводится перед началом Stage 6 как дополнительный review-only шаг после:

- Pre-Stage 6 Documentation Consistency Review;
- Pre-Stage 6 Controlled Documentation Cleanup.

На момент review:

- Stage 0 - Completed;
- Stage 1 - Completed;
- Stage 2 - Completed;
- Stage 3 - Completed;
- Stage 4 - Completed;
- Stage 4.1 - Completed;
- Stage 5 - Completed;
- Stage 6 - Planned / not started.

Эта задача не меняет roadmap, не выполняет cleanup, не начинает Stage 6, не создает Stage 6 deliverables и не меняет product/architecture decisions.

Основной roadmap `docs/roadmap/roadmap.md` должен быть source of truth по этапам, статусам, stage gates, carryover и следующему разрешенному шагу.

## 2. Review Scope

Проверены файлы:

- `docs/roadmap/roadmap.md`;
- `docs/ROADMAP.md`;
- `README.md`;
- `docs/product/README.md`;
- `docs/architecture/README.md`;
- `docs/decisions/README.md`;
- `docs/development/roadmap.md`;
- `docs/development/milestones.md`;
- `docs/development/implementation-strategy.md`;
- `AGENTS.md`;
- `docs/prompts/codex-rules.md`;
- `docs/prompts/review-template.md`;
- `docs/prompts/task-template.md`.

## 3. Expected Roadmap Role

Основной roadmap должен быть:

- source of truth по этапам и статусам;
- stage-based project map;
- навигационной картой к stage deliverables, summary, reviews и carryover;
- местом фиксации high-level quality gates и stage boundaries;
- защитным документом для AI/code agents, чтобы не начинать future scope преждевременно.

Основной roadmap не должен быть:

- task tracker;
- implementation backlog;
- ADR registry;
- architecture spec;
- product requirements document;
- future ideas dump;
- replacement для development roadmap, architecture docs или product docs.

## 4. Best-Practice Roadmap Structure

Для этого проекта целевая структура каждого stage в roadmap должна быть достаточно единообразной:

- Status;
- Purpose;
- Scope;
- Key deliverables;
- Quality gate;
- Out of scope;
- Carryover / next-stage notes;
- Links.

Для future/planned stages дополнительно нужны:

- Activation condition;
- Explicit exclusions;
- Required preceding decisions.

Эта структура не требует масштабного rewrite. Ее можно вводить controlled cleanup-ом: сначала Stage 6 и Current Status, затем только наиболее неоднозначные sections.

## 5. Findings Summary

| Severity | Count |
|---|---:|
| Critical | 0 |
| Major | 4 |
| Minor | 5 |
| Notes | 3 |

Severity definitions:

- Critical: проблема, которая может привести к roadmap drift, scope creep, premature implementation или ошибочному старту Stage 6.
- Major: существенная структурная проблема, дублирование, неоднозначность или missing governance, влияющая на управляемость roadmap.
- Minor: навигационная, wording, consistency или readability проблема.
- Note: рекомендация или наблюдение без необходимости немедленного исправления.

## 6. Detailed Findings

### [MJ-001] Current Status не отражает полный baseline Stage 0-5

Severity: Major  
Area: Roadmap / Status clarity  
Files:

- `docs/roadmap/roadmap.md`

Finding:
`Current Status` явно показывает Stage 3, Stage 4 и Stage 5 как Completed, но не показывает Stage 0, Stage 1, Stage 2 и Stage 4.1. Детальные sections ниже подтверждают завершение Stage 0-5, но верхняя status table не отражает полный baseline, который важен перед Stage 6.

Why it matters:
AI/code agents обычно читают верхний status block первым. Неполный status baseline может создать сомнение, все ли pre-Stage 6 prerequisites закрыты.

Recommendation:
В controlled cleanup добавить в `Current Status` полный компактный status baseline Stage 0, Stage 1, Stage 2, Stage 3, Stage 4, Stage 4.1, Stage 5 и Stage 6 Planned / not started.

Allowed timing:
Before Stage 6.

### [MJ-002] Stage 3/4 dashboard sections дублируют roadmap body и содержат устаревший next-step wording

Severity: Major  
Area: Roadmap / Structure / Stale notes  
Files:

- `docs/roadmap/roadmap.md`

Finding:
В начале roadmap есть отдельные `Stage 3 Dashboard` и `Stage 4 Dashboard`, затем ниже идут полноценные sections Stage 0-5. Эти dashboards частично дублируют body, а Stage 3 closure notes все еще говорят "Нужно не начинать Stage 5 Technical Architecture", хотя Stage 5 уже Completed.

Why it matters:
Dashboard sections могут быть полезны во время активного этапа, но после закрытия Stage 5 они ухудшают читаемость roadmap как source of truth и создают stale guidance для agents.

Recommendation:
В controlled cleanup либо свернуть Stage 3/4 dashboards в короткие historical notes, либо перенести их детали в соответствующие stage sections. Устаревший wording заменить на historical note без текущей инструкции "не начинать Stage 5".

Allowed timing:
Before Stage 6.

### [MJ-003] Future/planned stages не имеют явных activation conditions и quality gates

Severity: Major  
Area: Roadmap / Future stage governance  
Files:

- `docs/roadmap/roadmap.md`
- `docs/ROADMAP.md`

Finding:
Stage 6-10 отмечены как Planned и имеют краткий scope, а Stage 6 дополнительно имеет clarification. Но future stages не имеют единообразных activation conditions, quality gates, explicit exclusions и required preceding decisions.

Why it matters:
Перед Stage 6 особенно важно, чтобы roadmap ясно отвечал: что именно активирует Stage 6, какие решения должны быть заранее доступны и что остается запрещено. Без этого Stage 6 planning может случайно стать implementation/API/DB/storage/auth/DevOps/testing backlog.

Recommendation:
В controlled cleanup добавить для Stage 6 минимальные `Activation condition`, `Quality gate`, `Explicit exclusions` и `Required preceding decisions`. Для Stage 7-10 достаточно кратких future-stage guardrails без превращения roadmap в backlog.

Allowed timing:
Before Stage 6.

### [MJ-004] AGENTS.md задает приоритет источников, конфликтующий с primary roadmap role

Severity: Major  
Area: Cross-doc / Agent governance  
Files:

- `AGENTS.md`
- `docs/roadmap/roadmap.md`
- `docs/development/roadmap.md`

Finding:
`docs/roadmap/roadmap.md` называет себя primary roadmap. Однако `AGENTS.md` в разделе "Источники истины проекта" ставит `docs/development/roadmap.md` как "Поэтапный roadmap" выше milestones, strategy и product/architecture docs, но не упоминает `docs/roadmap/roadmap.md` как primary roadmap в этом приоритетном списке.

Why it matters:
Для AI/code agents это наиболее рискованная cross-doc ambiguity: secondary development roadmap может быть ошибочно принят за более приоритетный источник, чем main roadmap. Это повышает риск premature implementation.

Recommendation:
В controlled cleanup обновить `AGENTS.md`, чтобы `docs/roadmap/roadmap.md` был явно указан как primary roadmap и source of truth по этапам/статусам, а `docs/development/roadmap.md` был отмечен как secondary future/planned reference material.

Allowed timing:
Before Stage 6.

### [MN-001] Формат stage sections неоднороден

Severity: Minor  
Area: Roadmap / Consistency  
Files:

- `docs/roadmap/roadmap.md`

Finding:
Stage 0-2 используют Goal, Artifacts, Open questions, Recommendations/carryover. Stage 3 добавляет Entry/Exit criteria. Stage 4 использует Key results. Stage 5 находится в Future Stages, хотя Completed, и имеет Scope/Artifacts/Guardrails. Stage 6-10 имеют только Status/Scope.

Why it matters:
Неоднородный формат делает roadmap менее предсказуемым и сложнее поддерживаемым.

Recommendation:
Не переписывать все сразу. Ввести target section pattern только для Stage 6 и, при следующем cleanup, постепенно выровнять completed stages на легком уровне: Status, Purpose/Goal, Key deliverables, Quality gate, Carryover, Links.

Allowed timing:
During Stage 6 planning.

### [MN-002] Stage 5 расположен под заголовком Future Stages, хотя он Completed

Severity: Minor  
Area: Roadmap / Structure  
Files:

- `docs/roadmap/roadmap.md`

Finding:
Section `## Future Stages` начинается перед Stage 5, но Stage 5 уже Completed. Это не ломает статус, потому что Stage 5 явно marked Completed, но заголовок больше не соответствует фактическому состоянию.

Why it matters:
Это снижает читаемость и может создать впечатление, что Stage 5 еще future/planned.

Recommendation:
В controlled cleanup переименовать блок в нейтральный, например `## Stage 5 and Future Stages`, или вынести Stage 5 на тот же уровень completed stages.

Allowed timing:
Before Stage 6.

### [MN-003] Open Decisions смешивают product, architecture, implementation и process items

Severity: Minor  
Area: Roadmap / Decision hygiene  
Files:

- `docs/roadmap/roadmap.md`

Finding:
`Open Decisions` содержит provider-backed discovery, contract timing, adapter design/reliability/production-hardening, account/auth storage и "следующий этап или cleanup task". Это полезный список, но он смешивает разные уровни решений и не указывает owner/stage.

Why it matters:
Смешанный список open decisions может выглядеть как общий backlog или как разрешение решать технические темы раньше нужного этапа.

Recommendation:
В controlled cleanup разделить этот блок на `Open product/architecture decisions`, `Deferred implementation decisions` и `Process next step`, либо добавить stage/owner hints.

Allowed timing:
During Stage 6 planning.

### [MN-004] README не ссылается на новый roadmap structure review

Severity: Minor  
Area: Navigation  
Files:

- `README.md`
- `docs/roadmap/roadmap.md`

Finding:
После создания этого review он будет важен для следующего cleanup, но пока README и main roadmap не содержат ссылку на него.

Why it matters:
Review может быть пропущен будущим agent, если ссылка не будет добавлена в navigation.

Recommendation:
После принятия review добавить минимальную ссылку на `docs/reviews/roadmap-structure-and-process-fitness-review.md` в README и `docs/roadmap/roadmap.md`.

Allowed timing:
Before Stage 6.

### [MN-005] Roadmap Rules есть, но не покрывают все governance points для source of truth

Severity: Minor  
Area: Roadmap governance  
Files:

- `docs/roadmap/roadmap.md`

Finding:
`Roadmap Rules` защищают порядок этапов и запрет premature API/code, но не фиксируют явно, что roadmap не является task tracker, implementation backlog, ADR registry, architecture spec или product requirements document.

Why it matters:
Эти правила уже есть в других docs, но main roadmap как source of truth выиграет от компактного self-contained governance block.

Recommendation:
В controlled cleanup расширить `Roadmap Rules` 3-5 короткими bullets о роли roadmap и запрете трактовать future stages как active backlog.

Allowed timing:
Before Stage 6.

### [NT-001] Main roadmap хорошо защищает MVP scope

Severity: Note  
Area: Scope boundaries  
Files:

- `docs/roadmap/roadmap.md`

Finding:
Roadmap последовательно фиксирует hotel-only MVP v1, исключает flight/combined/booking/payment/account history из MVP и сохраняет provider facts / LLM boundary.

Why it matters:
Это сильная основа для безопасного Stage 6 planning.

Recommendation:
Сохранять эти constraints как top-level scope note.

Allowed timing:
No immediate action.

### [NT-002] Development docs после cleanup лучше отделены от active backlog

Severity: Note  
Area: Cross-doc alignment  
Files:

- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`

Finding:
Development docs явно framed как future/planned reference material и не active implementation backlog.

Why it matters:
Это снижает риск premature implementation, хотя AGENTS.md еще нужно выровнять с primary roadmap.

Recommendation:
Сохранить current framing.

Allowed timing:
No immediate action.

### [NT-003] Architecture and decisions indexes хорошо поддерживают Stage 5 baseline

Severity: Note  
Area: Navigation / Architecture governance  
Files:

- `docs/architecture/README.md`
- `docs/decisions/README.md`

Finding:
Architecture README и Decisions README ясно разделяют Stage 5 architecture baseline, non-ADR decision inventory и accepted ADRs.

Why it matters:
Это помогает Stage 6 planning не превращать provider abstraction в API contract и future ADR candidates в accepted decisions.

Recommendation:
No immediate action.

Allowed timing:
No immediate action.

## 7. Redundant / Too Detailed Items Review

| File | Section / Item | Issue | Recommendation | Timing |
|---|---|---|---|---|
| `docs/roadmap/roadmap.md` | `Stage 3 Dashboard` | Дублирует Stage 3 body и содержит stale instruction про не начинать Stage 5. | Свернуть в historical note или объединить с Stage 3 section; убрать текущую инструкцию про Stage 5. | Before Stage 6 |
| `docs/roadmap/roadmap.md` | `Stage 4 Dashboard` | Частично дублирует Stage 4 body и Stage 5 status. | Свернуть до короткой completion note или оставить только ссылки на Stage 4 docs. | During Stage 6 planning |
| `docs/roadmap/roadmap.md` | Stage 4 carryover: "Convert component inventory into implementation backlog..." | Формулировка полезна как guardrail, но содержит backlog wording в completed stage. | Переформулировать как "не превращать component inventory в implementation tasks до явной Stage 6/implementation задачи". | During Stage 6 planning |
| `docs/roadmap/roadmap.md` | `Open Decisions` item "Adapter design, provider error taxonomy, reliability и production-hardening" | Похоже на implementation backlog без stage owner. | Разделить как deferred technical decisions; не делать active tasks. | Before Stage 6 |
| `docs/ROADMAP.md` | Stage 7 bullets | Кратко описывает подключение existing travel API; safe, но близко к implementation scope. | Сохранить как future stage note; при cleanup добавить, что Stage 7 активируется отдельно. | Later / future stage |
| `docs/development/implementation-strategy.md` | `Порядок реализации MVP` | Это future reference sequence, но по форме похоже на backlog. | Оставить в development docs; не дублировать в main roadmap. | No immediate action |

## 8. Missing / Underrepresented Items Review

| Missing / Weak Area | Why it matters | Recommendation | Timing |
|---|---|---|---|
| Full Stage 0-6 status baseline in Current Status | Быстрое считывание текущего состояния перед Stage 6. | Добавить компактную status table для Stage 0, 1, 2, 3, 4, 4.1, 5, 6. | Before Stage 6 |
| Activation condition for Stage 6 | Защищает от случайного старта Stage 6 и premature implementation. | Добавить explicit activation condition: отдельная задача на Stage 6 planning/scope definition. | Before Stage 6 |
| Quality gate for Stage 6 | Помогает определить, когда Stage 6 planning готов перейти к реализации. | Добавить review-only/high-level quality gate без API/DB/testing backlog. | During Stage 6 planning |
| Explicit exclusions for Stage 6 | Нужны прямо в future stage section. | Перечислить no API/OpenAPI, no DB/storage, no auth/security/DevOps/testing backlog, no code. | Before Stage 6 |
| Roadmap role/governance | Roadmap должен сам объяснять, чем он не является. | Добавить rules: not task tracker, not implementation backlog, not ADR registry, not architecture spec. | Before Stage 6 |
| Links to roadmap structure review | Будущий cleanup должен найти этот review. | Добавить ссылку после review принятия. | Before Stage 6 |
| Cross-doc source priority alignment | Agents должны следовать primary roadmap, а не secondary roadmap. | Обновить `AGENTS.md` source priority. | Before Stage 6 |

## 9. Stage-by-Stage Coverage Review

| Stage | Status | Purpose Clarity | Deliverables Coverage | Quality Gate | Carryover | Notes |
|---|---|---|---|---|---|---|
| Stage 0 | Completed | Good | Good | Partial | Good | Quality gate implicit through artifacts/open questions. |
| Stage 1 | Completed | Good | Good | Good | Good | Consistency review listed. |
| Stage 2 | Completed | Good | Good | Good | Good | Good scope guards around provider facts and no API/DB. |
| Stage 3 | Completed | Good | Good | Good | Good | Dashboard duplication and stale Stage 5 note should be cleaned. |
| Stage 4 | Completed | Good | Good | Good | Good | Stage 4.1 represented; dashboard duplicates body. |
| Stage 4.1 | Completed | Good | Good | Good | Partial | Present as Stage 4.1 consistency review, not as standalone stage section. |
| Stage 5 | Completed | Good | Good | Good | Good | Located under Future Stages heading despite Completed status. |
| Stage 6 | Planned / not started | Partial | Missing | Missing | Partial | Needs activation condition, quality gate, explicit exclusions and preceding decisions. |

## 10. Cross-Document Alignment Review

| Document | Role | Alignment with Main Roadmap | Issues |
|---|---|---|---|
| `docs/ROADMAP.md` | Short high-level stage list | Good | Stage 6 wording now safer; future stage activation remains implicit. |
| `README.md` | Root navigation | Good | Needs link to this review after creation if used for cleanup. |
| `docs/product/README.md` | Product docs index | Good | Stage 5 architecture links included; not source of status. |
| `docs/architecture/README.md` | Architecture docs index | Good | Clear Stage 5 baseline and Stage 6 boundary. |
| `docs/decisions/README.md` | ADR governance/index | Good | Correctly separates accepted ADRs from non-ADR decision inventory. |
| `docs/development/roadmap.md` | Secondary future/planned development reference | Good | Well-framed as not active backlog after cleanup. |
| `docs/development/milestones.md` | Future/planned milestones | Good | Well-framed as not active backlog after cleanup. |
| `docs/development/implementation-strategy.md` | Future implementation task strategy | Good | Contains future sequence but explicitly not active backlog. |
| `AGENTS.md` | Agent rules and source priority | Partial | Source priority should explicitly put `docs/roadmap/roadmap.md` above secondary development docs. |
| `docs/prompts/*` | Task/review templates and Codex rules | Good | Consistent with review-only and roadmap control, though less specific than AGENTS. |

## 11. Recommended Roadmap Improvement Plan

### Recommended before Stage 6

- Add full Stage 0-6 baseline to `Current Status`.
- Add link to this review in README and main roadmap navigation.
- Clean stale Stage 3 dashboard note that says not to start Stage 5.
- Clarify Stage 6 activation condition and explicit exclusions in main roadmap.
- Update `AGENTS.md` source priority so `docs/roadmap/roadmap.md` is primary roadmap and `docs/development/roadmap.md` is secondary future/planned reference.
- Add compact roadmap role/governance bullets: not task tracker, not implementation backlog, not ADR registry, not architecture spec.

### Can be handled during Stage 6 planning

- Add Stage 6 quality gate and expected output shape without creating Stage 6 deliverables in this review.
- Split `Open Decisions` by product/architecture/deferred implementation/process ownership.
- Gradually normalize stage section format where it helps readability.
- Decide whether Stage 4.1 should remain embedded in Stage 4 or receive a tiny standalone status line.

### Later / future stage

- Consider simplifying completed-stage dashboards after Stage 6 starts.
- Keep future Stage 7-10 sections short unless a future planning task activates them.
- Do not convert development roadmap sequence into main roadmap task tracker.

## 12. Final Verdict

Roadmap needs controlled cleanup before Stage 6.

The roadmap is broadly fit as the project source of truth: it preserves stage-based progression, MVP boundaries, no-implementation guardrails and Stage 6 Planned / not started status. There are no Critical blockers.

However, before Stage 6, a limited cleanup should address status completeness, stale dashboard wording, Stage 6 activation/exclusions and the AGENTS.md source-priority mismatch. These are documentation/process fixes, not product, architecture or implementation work.
