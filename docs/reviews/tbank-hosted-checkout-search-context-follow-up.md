# T-Bank MCP — post-smoke rates и hosted checkout follow-up

**Статус:** completed local implementation; fresh-context review pending

**Версии:** Hotels `0.27.0`, Banking `0.16.0`, toolkit `0.10.0`

**Scope:** стабильность тарифов и безопасный переход на выбранный отель

**Out of scope:** booking/payment execution, exact-rate deep link, PII и card data

## Причина

Human smoke подтвердил рабочий read-only/preview flow, но выявил две UX-проблемы:

- тариф с завтраком получил номер внутри отфильтрованного подмножества, который
  не совпадал с номером в полной таблице;
- hosted checkout открывал выбранный отель, но не сохранял даты и гостей.

## Решение

- Provider rates получают стабильные `rateNumber`/`rateLabel` при загрузке и
  сохраняют их в select/preview/handoff.
- Готовая `ratesTableMarkdown` предназначена для однократного показа. При уже
  заданном критерии выбора агент завершает select и preview до одного итогового
  ответа и не перенумеровывает подмножество.
- Read-only проверка публичной страницы выбранного отеля подтвердила параметры
  `dateFrom`, `dateTo` и `guests` для одной комнаты без детей.
- Hosted checkout добавляет только эти allowlisted параметры. Для детей или
  нескольких комнат сохраняются только даты; полный состав гостей не заявляется.
- Exact-rate URL не подтверждён. MCP возвращает
  `exactRatePreserved=false` и
  `exactRateHandoffStatus=not_supported_by_verified_public_contract`.

## Evidence

- Публичная страница выбранного отеля открылась с нужным отелем, датами
  `15–16 сентября 2026` и `2 гостя` по URL с `dateFrom`, `dateTo`, `guests`.
- Проверка была read-only: без авторизации, ввода данных, кликов на оформление,
  бронирования или оплаты.
- Ни `bookHash`, ни `rateOptionId`, ни неподтверждённый exact-rate query contract
  в публичном URL не обнаружены.

## Acceptance checklist

- [x] Стабильный номер тарифа проходит rates → select → preview.
- [x] Presentation guidance запрещает повторную таблицу и перенумерацию.
- [x] URL содержит только выбранный hotel ID и allowlisted search parameters.
- [x] Operator template по-прежнему запрещает credentials, query и fragment.
- [x] Сложная occupancy не маскируется как полностью перенесённая.
- [x] Exact rate остаётся честным неподтверждённым gap.
- [x] Provider writes не выполнялись.

## Следующий gate

После полного offline verify нужен fresh-context review по обновлённому Qwen
prompt и один bounded human smoke: выбрать тариф, создать preview, получить
handoff и убедиться, что официальный интерфейс показывает нужный отель, даты и
число взрослых, но требует заново выбрать актуальный тариф.
