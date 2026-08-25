# ADR-0003 — граница Banking MCP и Hotels MCP

- **Статус:** Accepted
- **Дата:** 2026-08-21
- **Связанный этап:** отдельный experimental MCP toolstream, без изменения core roadmap stage

## Контекст

Для пользовательских сценариев нужны локальная авторизация Т-Банка по номеру
телефона, агрегированная финансовая аналитика, персонализация hotel search и в
будущем связанный с бронью payment flow. Смешивание mobile banking credentials,
истории операций и денежных tools с Hotels MCP нарушило бы least privilege и
сделало бы каждый hotel search процессом с банковскими полномочиями.

Внешний MIT-проект `icyberdeveloper/tbank-mcp` содержит capture-driven mobile
auth, read и payment implementations. Эти контракты не являются официальными
Hotels API contracts, а mobile session не доказана как допустимый customer auth
для Hotels API.

## Решение

1. Создать автономный `tools/tbank-banking-mcp` и подключать его к интегратору
   как второй MCP рядом с `tbank-hotels-mcp`.
2. Phone/SMS/password/PIN flow выполняется только локальным CLI или будущим
   доверенным UI вне LLM. MCP читает сохранённую сессию, но не принимает auth
   secrets как tool arguments.
3. Banking MCP предоставляет минимальный read-only набор: readiness, счета,
   агрегированные расходы и spending-based travel profile.
4. Raw account identifiers заменяются process-local `accountRef`. Hotels MCP не
   получает raw transactions, mobile tokens, cookies или account identifiers.
   Для персонализации между MCP передаётся только агрегированный, объяснимый и
   переопределяемый пользователем travel profile. Подтверждённые customer reads
   могут выполняться общим локальным auth broker по `ADR-0004`, при этом broker
   не раскрывает credential material Hotels MCP или модели.
5. Профиль влияет на default price range и ranking hints, но не скрывает
   варианты и не называется оценкой дохода или кредитоспособности.
6. Hotel payment остаётся отдельной cross-MCP orchestration boundary.
   На первом срезе разрешён только локальный `preview_only` intent.
7. Реальные банковские payment methods не экспортируются до подтверждения
   official booking/payment linkage, trusted confirmation UI, idempotency,
   reconciliation, antifraud/device requirements и non-production approval.
8. MIT-derived файлы хранят exact upstream revision, license copy и provenance.

## Последствия

- Обычный hotel search не получает банковские полномочия.
- Banking MCP можно отключить без потери поиска отелей.
- Пользователь может согласовать или изменить предложенный бюджет.
- Утечка Hotels service JWT не даёт доступа к счетам, а утечка banking session
  не должна автоматически активировать hotel booking execution.
- Реальная оплата потребует отдельного узкого решения и contract tests; наличие
  capture-driven `/v1/pay` реализации не считается таким решением.

## Рассмотренные альтернативы

- **Перенести все tools upstream в Hotels MCP.** Отклонено из-за смешения
  полномочий, секретов и blast radius.
- **Подключать upstream без facade.** Допустимо для личного эксперимента, но
  отклонено как основной путь: наружу выставляется слишком широкая денежная и
  персональная поверхность.
- **Считать расходы уровнем дохода.** Отклонено как недоказанный и потенциально
  дискриминационный вывод.
- **Оплачивать hotel order обычным банковским переводом.** Отклонено: это не
  доказывает связь с Hotels order/payment state.

## Границы

ADR не разрешает production rollout, реальные платежи, передачу OTP модели,
автоматическое включение персонализации, durable profile storage или признание
private mobile API официальным контрактом. Core Kotlin backend, публичный API и
текущий hotel-only MVP этим решением не изменяются.
