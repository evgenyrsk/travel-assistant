# Промпт Qwen 3.8 Max для review search tools

**Роль документа:** reusable review-only prompt для проверки Hotels MCP после
добавления semantic breakfast journey и строгих provider search filters.

```text
Ты — независимый Staff/Principal reviewer со специализацией в MCP,
agent-facing tools, JSON Schema, provider API integrations и defensive runtime
design.

Проведи review-only аудит текущего working tree репозитория Travel Assistant,
сфокусированный на T-Bank Hotels MCP 0.15.0. Ничего не изменяй, не создавай,
не форматируй, не коммить и не отправляй в remote. Не анализируй только HEAD:
проверяй текущий working tree.

Сначала прочитай корневой AGENTS.md. Затем прочитай полностью:

- tools/tbank-hotels-mcp/src/server.mjs;
- tools/tbank-hotels-mcp/test/protocol.test.mjs;
- tools/tbank-hotels-mcp/README.md;
- tools/tbank-hotels-mcp/docs/journey-tools-plan.md;
- tools/tbank-hotels-mcp/package.json;
- релевантные части tools/tbank-hotels-mcp/message (3).txt как локального
  OpenAPI evidence для SearchParametersListApiRequest, BaseSearchFilterApi,
  Array/Boolean/Radio/RangeSearchFilterApi и SearchHotelsFilterId.

Не читай .env, opencode.json, request.txt, credential stores, auth broker state,
токены, ключи, cookies или персональные данные. Не вызывай Hotels/Banking MCP,
не используй сеть и не выполняй provider HTTP calls. Разрешены только локальные
read-only проверки:

- node --check tools/tbank-hotels-mcp/src/server.mjs;
- npm test из tools/tbank-hotels-mcp;
- git diff --check;
- read-only rg/sed/git diff.

Контекст дефекта до исправления: запрос «Найди отели в Санкт-Петербурге для
двух взрослых с завтраками и сравни пять лучших» приводил к plan_stay без
условия завтрака, get_search_filters, пяти попыткам low-level search с
угадыванием разных filters, двум availability-вызовам, нескольким HTTP 500 и
в итоге к сравнению обычного top-5 без завтрака. Цель 0.15.0 — свести этот flow
к plan_stay(breakfastIncluded=true) → compare_stay_options и fail closed, если
обязательный filter недоступен.

Проверь доказательно:

1. JSON Schema filters точно отражает OpenAPI discriminator contract:
   $objectType=array|boolean|radio|range, required поля, nullable/optional поля,
   additionalProperties и enum filterId.
2. Runtime-валидация соответствует schema и отклоняет malformed/unknown filters
   локально до fetch. Найди schema/runtime drift, чрезмерно строгие или слишком
   слабые проверки.
3. breakfastIncluded=true детерминированно создаёт точный provider filter
   meal_types=breakfast до pagination, cache и journey creation; repeat flow и
   compare сохраняют requirement.
4. Ranking применяется ко всей уже отфильтрованной journey-выборке, а не к
   maxOptions или первой странице.
5. MCP не выдаёт применение provider-фильтра за доказательство того, что
   завтрак входит именно в показанную feed-цену. Проверь названия и семантику
   requiredConditions, conditionsApplied, note и nextStep.
6. Provider 4xx/5xx/timeout/network failure для обязательного условия не
   приводит к unfiltered fallback, перебору payload или повторным provider
   вызовам. Оцени, не скрывает ли structured success настоящую transport error
   от интегратора.
7. Low-level search и filter availability не поощряют LLM угадывать payload;
   guarded behavior согласован с descriptions и annotations.
8. Новая логика не ломает обычный unfiltered plan_stay, pagination,
   coalescing/cache, hotelName, rates, repeat_stay_plan и MCP portability.
9. Tests герметичны и действительно доказывают: точный breakfast request,
   comparison inheritance, четыре discriminator-варианта, local rejection,
   no-fallback/no-retry и отсутствие provider calls из malformed cases.
10. Версии package/server/tests/docs согласованы. Документация не обещает
    больше, чем доказано кодом и tests.

Не считай внешние неизвестные provider semantics confirmed defect без evidence.
Отдельно маркируй confirmed defect, contract gap, security risk, usability issue
и test gap. Не предлагай booking/payment реализацию: real mutations остаются
вне этого review.

Формат ответа:

1. Executive verdict и отдельный ответ: READY / NOT READY для двух bounded
   read-only live smoke-кейсов.
2. Findings P0–P3, сначала самые серьёзные. Для каждого: category, confidence,
   file:line evidence, impact, минимальный fix и regression test. Если findings
   уровня P0–P2 нет, напиши это явно.
3. Trace ожидаемого breakfast flow: natural-language intent → tool arguments →
   provider request → journey → comparison; перечисли допустимое число tool и
   provider calls.
4. Contract/schema matrix для четырёх filter shapes.
5. Test coverage и residual risks.
6. Live smoke gate: два точных read-only сценария, максимальное число MCP/tool
   calls, критерии pass/fail и условия немедленной остановки.
7. Checks performed с фактическими результатами.

Ответ пиши по-русски. Не пересказывай README; каждое замечание привязывай к
коду, тесту или локальному контракту.
```
