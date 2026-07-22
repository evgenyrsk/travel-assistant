# Stage 12.1a — дискретная политика минимального гостевого рейтинга

## Роль и цель

Это review/design-only решение после contract drift Stage 12.1. Актуальный
статус задает [`docs/roadmap/roadmap.md`](../roadmap/roadmap.md), продуктовые
границы —
[`docs/product/product-baseline.md`](../product/product-baseline.md).

Цель — согласовать provider-neutral семантику `minGuestRating` до создания
domain model, LLM schema, Hotels API DTO и mapping.

## Основания

Stage 12.1 подтвердил:

- Hotels API catalog описывает `review_rating` как `$objectType=radio`;
- допустимые catalog values: `9`, `8`, `7`, `6`, `5`;
- popular filter использует значение `8`;
- OpenAPI request schema для `RadioSearchFilterApi` ожидает одно строковое поле
  `value`;
- фактический filtered request еще не отправлялся.

Возможности catalog и фактический guest rating предложения — разные понятия.
Рейтинг отеля может оставаться дробным provider fact; дискретны только пороги,
которые текущий provider предлагает для фильтра поиска.

## Принятая продуктовая политика

`minGuestRating` может иметь только одно из значений:

```text
5, 6, 7, 8, 9
```

Правила:

- «рейтинг не ниже 8», «от 8» и «8+» устанавливают порог `8`;
- одна реплика может заменить существующий порог другим допустимым значением;
- «убери ограничение по рейтингу» очищает preference;
- отсутствие упоминания рейтинга сохраняет прежнее значение;
- отсутствие rating preference не задерживает первый поиск;
- `8.5`, `10`, значения ниже `5` и любые другие числа не округляются;
- «высокий», «хороший» или «лучший рейтинг» без точного порога требуют
  уточнения;
- clarification предлагает только допустимые пороги и не выбирает значение за
  пользователя.

Для `8.5` безопасный вопрос может предложить выбрать `8` или `9`, но ответ
пользователя обязателен. Значение `8` нельзя выбрать как скрытое ослабление, а
`9` — как скрытое усиление условия.

## Границы domain и LLM

Stage 12.2 должен представить порог через value object или enum-подобную
provider-neutral модель с конечным набором допустимых значений. Произвольный
`Double` для filter input не рекомендуется, поскольку он допускает
непредставимые provider values.

Stage 12.3 должен:

- извлекать только точное допустимое значение;
- возвращать clarification для неподдерживаемого или ненумерического порога;
- поддерживать явную операцию `CLEAR`;
- не менять nullable provider rating в `HotelOffer` и не округлять его.

Provider-specific `filterId`, `$objectType` и строковое `value` остаются вне
domain и LLM output.

## Предварительный request mapping

Следующая проверка может использовать только как гипотезу:

```json
{
  "$objectType": "radio",
  "filterId": "review_rating",
  "value": "8"
}
```

Этот payload следует из Swagger request schema и catalog values, но не считается
подтвержденным до Stage 12.1b. Аналогично требуют live-подтверждения `price`,
`stars`, `free_cancellation_allowed` и выбранный sort.

## Закрытые вопросы

| Вопрос | Решение |
|---|---|
| Произвольный порог `0..10` | Не поддерживается filter input |
| Допустимые пороги | Только `5`, `6`, `7`, `8`, `9` |
| Дробные значения | Уточнение, без округления |
| Порог `10` | Уточнение; provider catalog его не предлагает |
| Ненумерическая оценка | Уточнение |
| Снятие ограничения | Явный `CLEAR` |
| Фактический rating предложения | Сохраняется без округления |
| Отсутствующий provider rating | Остается unknown/null |

## Что не входит в этап

- production code и tests;
- новые live-вызовы;
- filter DTO, mapper, session state или LLM schema;
- локальная постфильтрация provider offers;
- public API, OpenAPI Travel Assistant и demo shell;
- изменение остальных preferences;
- pagination, auth, durable storage, booking и payment.

## Следующий этап

Stage 12.1b должен выполнить максимум два отдельно разрешенных вызова без
retries:

1. `POST /api/v1/hotels/search-filters-availability`;
2. filtered `POST /api/v1/hotels/search`.

Оба запроса должны проверить `review_rating` с `radio.value="8"`, остальные три
выбранных фильтра и sort. При `4xx` или структурном drift Stage 12.2 остается
заблокированным; альтернативный payload автоматически не отправляется.

## Verdict

`PASS_STAGE_12_1A_DISCRETE_GUEST_RATING_POLICY`.

Продуктовая неоднозначность закрыта. Stage 12.2 остается заблокирован до
фактической проверки request mapping в Stage 12.1b.
