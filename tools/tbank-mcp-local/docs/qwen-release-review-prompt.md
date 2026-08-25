# Prompt для release-review T-Bank MCP

Передай Qwen 3.8 Max следующий prompt целиком после выполнения естественных
smoke-кейсов. Это review-only задача.

```text
Проведи строгий review-only аудит текущего working tree репозитория
/Users/evgenyrsk/Projects/travel-assistant.

Scope:
- tools/tbank-hotels-mcp, ожидаемая версия 0.22.0;
- tools/tbank-banking-mcp, ожидаемая версия 0.13.1;
- tools/tbank-mcp-local, ожидаемая версия 0.5.0;
- ADR-0003, ADR-0004;
- tools/tbank-hotels-mcp/docs/journey-tools-plan.md;
- tools/tbank-hotels-mcp/docs/portability-and-distribution-roadmap.md;
- docs/reviews/tbank-mcp-local-compatibility-batch.md;
- docs/reviews/tbank-payment-handoff-preview-hardening.md;
- docs/reviews/tbank-payment-contract-readiness-foundation.md;
- результаты человеческих smoke-кейсов, если они приложены пользователем.

Ограничения:
- ничего не изменяй;
- не читай .env, opencode.json, request.txt, PEM/private keys, session store,
  cookies, tokens и локальные message*.txt;
- не запускай MCP tools и не обращайся к provider/network;
- не выполняй booking/payment/cancel/update и даже prepare/execute mutations;
- локальные tests разрешены только в очищенном от TBANK_* окружении.

Проверь:
1. Tool contract compatibility: names, schemas, annotations, versions,
   manifests и clean restart.
2. Secret boundaries: key-file path вместо PEM в client config, отсутствие
   secrets в args/stdout/errors/manifests, разделение Hotels и Banking env.
3. Setup/doctor/client-config UX для OpenCode и Codex CLI;
   standalone и combined profiles не должны объединять полномочия MCP.
4. Offline guarantee команды verify: unit/protocol tests, manifests,
   conformance и ноль provider requests даже при credentials у parent process.
5. Natural-language journey: обычный поиск, обязательный breakfast,
   сравнение, rates, preview_only, customer reads и spending personalization
   без угадывания provider DTO.
6. Privacy-first user flows:
   - tbank_hotels_summarize_bookings должен возвращать только раздельные counts,
     без itinerary, отелей, городов, дат, цен, гостей, bookingRef/orderId и без
     потенциально ложного total при пересекающихся provider-категориях;
   - tbank_banking_build_portfolio_travel_profile должен использовать
     агрегированные category signals внутри MCP, но не раскрывать accountRef,
     количество/названия счетов, абсолютные суммы и category breakdown;
   - provenance должен честно различать «использовано внутри» и «не включено в
     ответ»; high travel signal не должен трактоваться как доказанная частота
     поездок или доход пользователя;
   - обычный privacy-запрос не должен заставлять модель дополнительно вызывать
     list_accounts, spending_summary или подробный list_bookings.
7. Safety: opaque identifiers, PII redaction, bounded concurrency/cache,
   fail-closed required conditions, no automatic write activation.
8. Payment boundary:
   - handoff одноразовый и атомарно поглощается;
   - сумма передаётся как canonical `amountDecimal`, с честным ограничением:
     исходная lexical precision provider JSON не восстанавливается;
   - freshness и process-local source-account binding проверяются до intent;
   - connection status и intent используют одинаковый fail-closed
     `executionReadiness`;
   - unknown outcome запрещает automatic retry;
   - `/v1/pay` и marketplace gateway не объявляются Hotels payment contract.
9. Honest capability tiers: booking_execute и payment_execute должны остаться
   NO-GO до подтверждения status/setup/gateway/antifraud/idempotency/
   reconciliation и trusted-confirmation contracts.
10. Documentation consistency: roadmap, READMEs, manifests и фактические версии.

Для каждого finding укажи P0–P3, confidence, точный file:line evidence,
impact, минимальный fix и regression test. Не называй отсутствие внешнего
контракта дефектом реализации: помести это в Contract gaps.

Обязательно выполни локально без сети:
- npm --prefix tools/tbank-mcp-local run verify
- git diff --check

Финальный отчёт:
1. Executive verdict по tiers: hotels_read, customer_read, banking_read,
   preview_only, booking_execute, payment_execute.
2. Findings P0–P3, включая явное «нет», если категория пуста.
3. Compatibility/security matrix.
4. Проверка пяти естественных кейсов по предоставленному trace; не выдумывай
   live evidence, которого нет.
5. Contract gaps и внешние blockers.
6. Checks performed с точными результатами.
7. Вердикт READY / CONDITIONAL READY / NO-GO для локального read-only release.
8. Короткий список только тех следующих шагов, которые реально блокируют этот
   release; future remote transport и mutations вынеси отдельно.
```
