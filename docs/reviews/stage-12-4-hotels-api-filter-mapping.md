# Stage 12.4 — детерминированное преобразование фильтров Hotels API

## Цель

Преобразовать четыре внутренние настройки поиска в подтвержденный контракт
`POST /api/v1/hotels/search`, не активируя уточнение запроса в runtime и не
меняя публичный API.

## Что добавлено

Внутренний `HotelsApiSearchFilterDto` представляет четыре проверенные формы
provider-фильтров. `HotelsApiSearchFilterMapper` преобразует
`HotelSearchPreferences` в них в постоянном порядке:

| Настройка | Hotels API request |
|---|---|
| Максимальная общая стоимость | `$objectType=range`, `filterId=price`, `min=0`, `max=<amount>` |
| Звезды | `$objectType=array`, `filterId=stars`, отсортированные строковые `values` |
| Минимальный гостевой рейтинг | `$objectType=radio`, `filterId=review_rating`, строковый `value` |
| Бесплатная отмена | `$objectType=boolean`, `filterId=free_cancellation_allowed`, `value=true` |

Цена остается JSON-числом без преобразования через `Double`. Поддерживается
только `RUB`; автоматической конвертации нет. Пустой набор настроек не добавляет
поле `filters` и сохраняет прежний основной поиск.

Mapper защитно отклоняет неположительную стоимость, неподдерживаемую валюту и
звезды вне диапазона `0..5`. Эти случаи переходят во внутренний
`RequestRejectionReason.INVALID_PREFERENCES` и не раскрывают provider details.

## Ограниченный provider request

`HotelsApiSearchOrchestrator` по-прежнему выполняет ровно один запрос с
`offset=0` и `limit=20`. `nextOffset` и `isLoadingCompleted` не запускают второй
вызов, pagination или polling. Поле `sort` не формируется: наблюдаемый Hotels
API отклонил пользовательскую сортировку на Stage 12.1b.

Stage 12.4 не активирует refinement-профиль OpenRouter, не применяет session
patch и не меняет confirmation flow. Поэтому новый provider request станет
доступен пользователю только после отдельного Stage 12.5.

## Подтвержденные факты предложения

Внутренний `HotelOffer` дополнен nullable-полями `starRating` и
`freeCancellationUntil`. Реальный response mapper:

- сохраняет `starRating` только в диапазоне `0..5`;
- преобразует provider date-time бесплатной отмены в `Instant`;
- не придумывает значение отмены, если provider его не вернул;
- отклоняет неверный рейтинг звезд или неверный date-time как некорректные
  hotel data.

Публичный response пока не меняется. Вывод этих фактов остается задачей Stage
12.6 после стабилизации backend flow.

## Проверки

Точечные тесты подтверждают:

- точные `$objectType`, `filterId` и wire values всех четырех фильтров;
- постоянный порядок фильтров и сортировку значений звезд;
- отсутствие `filters` для пустых настроек и отсутствие `sort` всегда;
- сохранение decimal price как JSON-числа;
- защитный отказ для неверной стоимости, валюты и звезд;
- один request с `offset=0`, `limit=20` даже при наличии `nextOffset`;
- преобразование и validation `starRating`/`freeCancellationUntil`;
- безопасное преобразование mapping errors в application-owned причины.

Выполнены и прошли точечные тесты mapper/orchestrator/adapter и полный backend
test suite. `git diff --check` и финальная проверка границ также пройдены перед
закрытием этапа.

## Границы этапа

Stage 12.4 не меняет:

- routes, runtime composition, public API, OpenAPI или demo shell;
- production-профиль LLM и накопление preferences в session flow;
- действующее рекомендуемое ранжирование;
- transport, provider host, auth или конфигурацию;
- pagination, retries, filter availability, durable storage, booking или
  payment.

Live OpenRouter и Hotels API calls не выполнялись.

## Следующий этап

Stage 12.5 может атомарно подключить refinement-профиль LLM, применить typed
session patch, показать полный обновленный confirmation prompt и только после
явного «Да» выполнить один новый provider search с фильтрами. Предыдущий поиск
должен оставаться доступным, а ошибка не должна создавать новый
`hotelSearchId`.

## Verdict

`PASS_STAGE_12_4_HOTELS_API_FILTER_MAPPING`.

Все четыре подтвержденных фильтра готовы на provider boundary. Runtime
уточнения и публичное отображение новых фактов остаются отдельными этапами.
