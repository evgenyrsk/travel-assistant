# Prompt для release-review T-Bank MCP

Передай Qwen 3.8 Max следующий prompt целиком после выполнения естественных
smoke-кейсов. Это review-only задача.

```text
Проведи строгий review-only аудит текущего working tree репозитория
/Users/evgenyrsk/Projects/travel-assistant.

Scope:
- tools/tbank-hotels-mcp, ожидаемая версия 0.28.0;
- tools/tbank-banking-mcp, ожидаемая версия 0.17.0;
- tools/tbank-mcp-local, ожидаемая версия 0.14.0;
- ADR-0003, ADR-0004, ADR-0005;
- tools/tbank-hotels-mcp/docs/journey-tools-plan.md;
- tools/tbank-hotels-mcp/docs/booking-payment-contract-readiness.md;
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
   Отдельно проверь модульную границу Hotels: `server.mjs` должен быть тонкой
   stdio-точкой входа, а config, tool contracts, framing, checkout boundary и
   domain runtime — отдельными модулями без cyclic imports и manifest drift.
2. Secret boundaries: key-file path вместо PEM в client config, отсутствие
   secrets в args/stdout/errors/manifests, разделение Hotels и Banking env.
3. Setup/doctor/client-config/connect UX для Cursor, OpenCode и Codex CLI; Claude Code не
   входит в acceptance matrix;
   standalone и combined profiles не должны объединять полномочия MCP.
   Combined config должен автоматически обеспечить один broker при Hotels-first,
   Banking-first и одновременном lazy start; завершение одного MCP не должно
   обрывать session, а logout/stop-broker должны завершать broker явно.
   `connect` должен устанавливать фиксированные версии в owner-only runtime,
   хранить только абсолютные executable paths/transport settings и выполнять
   secret-free регистрацию двух отдельных MCP. Combined/Banking профиль может
   запускать terminal-only mobile login, но номер, SMS-код, password/PIN и
   tokens не должны попадать в MCP args, config или stdout. Hotels-only профиль
   не должен требовать Python, mobile session или auth broker.
   Cursor-регистрация должна безопасно объединяться с существующим глобальным
   `~/.cursor/mcp.json`, сохранять чужие MCP entries и использовать официальный
   stdio shape `type`/`command`/`args` без credentials.
4. Offline guarantee команды verify: unit/protocol tests, manifests,
   conformance и ноль provider requests даже при credentials у parent process.
5. Natural-language journey: обычный поиск, обязательный breakfast,
   сравнение, rates, preview_only, customer reads и spending personalization
   без угадывания provider DTO.
   Номера тарифов должны быть стабильны во всём journey: нельзя перенумеровывать
   breakfast/refundable-подмножество. Готовую rates-таблицу нужно показывать
   ровно один раз; если критерий выбора уже задан, select и preview завершаются
   до одного консолидированного пользовательского ответа.
   Повторное «выбери среди ранее показанных» должно по умолчанию
   оставаться в предыдущей comparison-группе; выход на всю journey допустим
   только при явном `scope=all_journey_options`, а `optionIds` ограничивают ranking
   заданным подмножеством.
   Отдельно проверь typed `hotelPreferences` handoff через обязательный
   `tbank_hotels_plan_personalized_stay` (`hotelDefaults` остаётся
   compatibility alias): в Hotels не
   должны попадать счета, категории или суммы; `best_value` должен быть
   детерминированным MCP-derived score, диапазон цены за ночь — мягким, provider
   search body — без price filter, а варианты вне диапазона не должны
   скрываться. Band-aware price utility не должна награждать сильное отклонение
   ниже диапазона как лучший fit: умеренно более дешёвые/дорогие варианты могут
   оставаться альтернативами, но far-outside вариант не должен лидировать
   только из-за цены. `preferenceAlternatives` должен отдельно показывать
   лучшие доступные варианты ниже/выше мягкой полосы. `ranking=best_value` без
   переданного `hotelPreferences` не должен
   называться применением профиля. Provider `shownPrice` должен оставаться total
   за период, а MCP-derived `pricePerNight` — вычисляться по `stayNights`; эти
   значения нельзя смешивать в ranking, диапазоне или пользовательском тексте.
   `plan_stay` должен самостоятельно разрешать локацию при локализованном
   countryName: model-side цепочка `resolve_destination` с переводами или
   вариантами названия после первого `plan_stay` считается UX/load regression.
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
   fail-closed required conditions, no automatic write activation. Смена
   только локального ranking не должна создавать повторный provider search.
   Нормализация денег/времени/отмены не должна округлять или называть UTC
   локальным временем; отсутствие cancellation fact не должно превращаться в
   утверждение о возвратности. Обычный preview не должен раскрывать внутренние
   названия trusted headers или конфигурационные blockers пользователю.
   Один `TBANK_HOTELS_ENABLE_MUTATIONS=true` не должен активировать writes:
   нужен отдельный non-production reviewed execution profile, который combined
   launcher не наследует из parent environment; production profile отсутствует.
8. Payment boundary:
   - handoff одноразовый и атомарно поглощается;
   - сумма передаётся как canonical `amountDecimal`, с честным ограничением:
     исходная lexical precision provider JSON не восстанавливается;
   - freshness и process-local source-account binding проверяются до intent;
   - connection status и intent используют одинаковый fail-closed
     `executionReadiness`;
   - unknown outcome запрещает automatic retry;
   - `/v1/pay` и marketplace gateway не объявляются Hotels payment contract.
   - booking schema содержит подтверждённые `paymentMeans=pos`, nullable
     `isBusinessTrip` и UUID card reference;
   - `tbank_hotels_create_payment_form_preview` не принимает PII, PAN,
     card expiry, CVV/CVC, PIN, OTP, 3DS/browser data или redirect URL, не
     выполняет HTTP и не возвращает `paymentUrl`;
   - hosted payment form — единственный intended public payment flow;
     raw-card/fingerprint/3DS endpoints отсутствуют в tool manifest;
   - `paymentFormExecution` честно отделяет офлайн-подтверждённый Swagger
     contract от внешних origin/auth/IP/idempotency/reconciliation/handoff
     blockers и остаётся unavailable.
   - `tbank_hotels_create_checkout_handoff` принимает только `journeyId`, не
     содержит PII/bookHash/token/card/OTP/3DS, не выполняет provider request,
     booking или payment, открывает public page выбранного отеля с
     `selectionPreserved=true`; для простой occupancy из одной комнаты без
     детей допускаются только подтверждённые public query-параметры
     `dateFrom`, `dateTo`, `guests`, а для сложной occupancy — только даты;
     preservation-флаги обязаны точно описывать перенос. Exact rate не
     переносится: `exactRatePreserved=false` и отдельный статус указывает на
     отсутствие подтверждённого public exact-rate contract. Шаблон hosted URL
     обязан быть HTTPS без credentials, query и fragment; внутренний код может
     добавлять только перечисленные allowlisted query-параметры;
   - generic hosted-checkout handoff не должен ошибочно считаться exact
     provider `paymentUrl` handoff или основанием активировать execution.
9. Banking runtime boundary: MCP server и auth broker должны получать только
   `CuratedMobileSession` с шестью allowlisted read-операциями. Обычный runtime
   не должен иметь `pay`, transfers, login/OTP, marketplace, messenger,
   credential fields или raw HTTP session. Raw vendored session допустим только
   для local login CLI и явно запускаемого read-only auth probe вне MCP.
10. Honest capability tiers: booking_execute и payment_execute должны остаться
   NO-GO до подтверждения customer auth, trusted IP/device context,
   payment-state transition semantics, idempotency/reconciliation,
   owner-bound payment-link handoff и non-production approval.
11. Documentation consistency: roadmap, READMEs, manifests и фактические версии.
12. Artifact candidates: npm pack Hotels/toolkit и wheel Banking должны
    содержать только allowlisted runtime/docs, устанавливаться во временный
    каталог вне checkout и проходить соответствующие проверки. Banking wheel
    обязан включать console scripts `tbank-banking-mcp`, `tbank-auth-broker` и
    `tbank-banking-login`; toolkit должен находить отдельно установленные
    команды через `PATH` или валидированные абсолютные overrides, а repository
    layout использовать только как development fallback. Phone login должен
    оставаться terminal CLI вне MCP tool surface. Не считать это registry-
    публикацией или полной fresh-machine release; отдельно проверить public
    registry metadata, anonymous-search boundary, результаты registry upload,
    fresh install и честное описание отсутствующих checksums/SBOM/provenance.

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
4. Проверка восьми естественных кейсов по предоставленному trace; не выдумывай
   live evidence, которого нет.
5. Contract gaps и внешние blockers.
6. Checks performed с точными результатами.
7. Вердикт READY / CONDITIONAL READY / NO-GO для локального read-only release.
8. Короткий список только тех следующих шагов, которые реально блокируют этот
   release; future remote transport и mutations вынеси отдельно.
```
