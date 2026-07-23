# Stage 14.3 — фильтр включённого завтрака и факт предложения

## Цель

Закрыть открытый вопрос о завтраке на основе уже сохранённых обезличенных
provider-derived fixtures и добавить ограниченную поддержку требования
«завтрак включён» во весь chat-first flow.

## Контрактные доказательства

- `GET /api/v2/hotels/search-filters` вернул фильтр `meal_types` с
  `$objectType=array` и значением `breakfast`.
- В popular filters встречается тот же `meal_types=["breakfast"]`.
- Обезличенный fixture поиска содержит `mealType=breakfast` с отображаемым
  названием «Завтрак включён» и `mealType=nomeal` с отсутствующим питанием.

Эти данные подтверждают wire-форму выбранного фильтра. Они не означают
поддержку всех типов питания и не являются гарантией SLA или официального
server-to-server статуса публичного API.

## Что добавлено

- `HotelSearchPreferences.breakfastIncludedRequired` с операциями
  `KEEP`/`SET`/`CLEAR`.
- Strict structured output OpenRouter с полем `breakfast-included`.
- Полный confirmation и idempotency basis учитывают требование завтрака.
- Provider mapping передаёт
  `{"$objectType":"array","filterId":"meal_types","values":["breakfast"]}`.
- Search response преобразует только доказанные значения:
  `breakfast -> true`, `nomeal -> false`, остальные значения -> `unknown`.
- Offers API содержит необязательный `breakfastIncluded`, а
  `appliedPreferences` — необязательный `breakfastIncludedRequired`.
- Demo shell показывает факт завтрака в карточке и среди активных условий.
- При пустой выдаче requirement может быть предложено снять, но не снимается
  автоматически.

## Безопасная семантика

- Фраза о необходимости включённого завтрака устанавливает requirement.
- Явное снятие требования очищает preference.
- Значение `false` не используется как запрос «найти только без завтрака».
- Half-board, full-board и другие meal plans не выводятся как наличие
  завтрака без отдельного подтверждённого mapping.
- Неизвестный `mealType` не заменяется выдуманным boolean.

## Проверки

- Catalog fixture закрепляет `meal_types=array` и `breakfast`.
- Request mapper проверяет точный filter payload и детерминированный порядок.
- Response mapper и provider fixture проверяют `true`/`false`/`unknown`.
- LLM, session patch, confirmation, idempotency и refinement flow покрыты
  targeted tests.
- Public JSON/OpenAPI и demo presentation покрыты backend/frontend tests.

## Вне этапа

- другие meal plans и выбор типа питания;
- вызов filter catalog на каждый поиск;
- sorting, pagination и автоматический retry;
- rates, booking, payment, auth и durable storage;
- новые provider credentials или live contract probe.

## Verdict

`IMPLEMENTED_AND_LOCALLY_VERIFIED`.

Фильтр завтрака подтверждён сохранённым provider contract и поддерживается как
пятое optional preference. Финальная ручная REAL-проверка выполняется через
локальную demo shell без автоматического повторного запроса.
