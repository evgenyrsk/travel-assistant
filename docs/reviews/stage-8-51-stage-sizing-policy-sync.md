# Stage 8.51 — Stage Sizing Policy Sync

## 1. Scope

Stage 8.51 — docs-only process/governance sync. Зафиксировать обновлённое
правило размера stage в репозитории: medium-small stages разрешены там, где
это безопасно, при сохранении строгого scope control и отдельных commits.

Stage 8.51 не меняет production code, tests, runtime behavior, routes,
API, OpenAPI, frontend, generated clients, product/architecture direction
или primary roadmap/status wording.

## 2. Reason for change

До Stage 8.50 проект работал micro-stages, потому что приближался к
первому runtime-touching step и защищал safety boundaries (CreateHotelSearchUseCase,
real hotelSearchId, show_hotel_results, markConsumed, actual execution,
Stage 7 strict handoff).

Stage 8.50 стал первым controlled runtime-touching stage с non-results
behavior. После него micro-stage overhead стал неоправданным для internal
skeleton work, где один boundary и один risk profile позволяют безопасно
объединять closely related design + model + mapper + composition + unit-test
work в один stage.

Обновление policy нужно, чтобы:

- future prompts to code assistant явно разрешали medium-small stages;
- dangerous work (runtime wiring, execution, markConsumed, show_hotel_results,
  provider calls, OpenAPI/frontend, durable storage, booking flow) оставалось
  split и separately committed;
- process rule было зафиксировано в authoritative governance doc.

## 3. Updated process policy

Stage sizing policy добавлен в `AGENTS.md` → `Roadmap and Scope Control` →
`Stage Sizing Policy`:

- Default stage size: medium-small rather than micro, where safe.
- Combining allowed: closely related design, model, mapper, composition,
  and unit-test work within one boundary and one risk profile.
- Review/design-only stages may cover several related blockers.
- Backend skeleton stages may include model + use case + mapper + tests
  within one internal boundary.
- Runtime wiring stages must remain narrow: one flow/branch at a time.
- Must remain separate: runtime wiring + actual execution, `markConsumed`,
  `show_hotel_results`, provider calls, OpenAPI/frontend changes, durable
  storage, booking flow.
- Actual execution stages must be focused and separately committed.
- OpenAPI/frontend changes must remain separate after backend behavior stable.
- Every stage still requires explicit scope, validation, review report,
  and separate commit.

## 4. Files changed

| File | Change |
|---|---|
| `AGENTS.md` | Добавлен `### Stage Sizing Policy` subsection в `## Roadmap and Scope Control`. |
| `docs/reviews/stage-8-51-stage-sizing-policy-sync.md` | Новый review document. |
| `docs/reviews/README.md` | Добавлена одна запись Stage 8.51. |

## 5. Safety guardrails preserved

- Все existing scope control rules сохранены.
- Safety guardrails не ослаблены.
- Dangerous work remains explicitly split.
- Каждый stage всё ещё требует отдельный commit.
- Runtime wiring stages remain narrow.
- Policy не разрешает broad uncontrolled tasks.

## 6. Future prompt guidance

Future prompts to code assistant (Codex/opencode) должны включать:

```
Process update:
We are using medium-sized stages where safe.
A stage may combine closely related changes only when they share one boundary
and one risk profile.
Do not combine runtime wiring with actual execution, `markConsumed`,
`show_hotel_results`, provider calls, OpenAPI/frontend changes, durable
storage, or booking flow.
Each stage still requires strict scope control, validation, review report,
and a separate commit.
```

Эта guidance зафиксирована в `AGENTS.md` `Stage Sizing Policy` subsection.

## 7. Explicit non-goals

Stage 8.51 не создаёт и не меняет:

- Production code.
- Tests.
- Runtime/routes/API/OpenAPI/frontend/generated clients.
- Product baseline или architecture baseline.
- Primary roadmap/status wording.
- Stage 8 implementation code.
- Broad documentation refactor.
- Unrelated sections в существующих docs.

## 8. Validation

- `git status --short`: подтверждено — только AGENTS.md + 2 docs files.
- `git diff --check`: no errors.
- Tests не запускались: stage is docs-only process sync; production code и tests не менялись.
- Inspection: `Stage Sizing Policy` added в `AGENTS.md` under `Roadmap and Scope Control`; existing scope rules preserved; no roadmap/status/product/architecture changes.

## 9. Verdict

**Passed** — medium-sized stage policy documented.

Stage 8.51 зафиксировал stage sizing policy в `AGENTS.md` — authoritative
governance doc, который читается каждым AI agent session. Policy разрешает
medium-small stages для closely related work within one boundary и one risk
profile, но сохраняет narrow stages для runtime wiring и separate stages для
actual execution, `markConsumed`, `show_hotel_results`, provider calls,
OpenAPI/frontend, durable storage и booking flow. Каждый stage всё ещё
требует explicit scope, validation, review report и отдельный commit.
Production code, tests, runtime, roadmap, product и architecture direction
не менялись.
