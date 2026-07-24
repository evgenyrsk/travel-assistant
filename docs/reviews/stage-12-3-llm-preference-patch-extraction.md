# Stage 12.3 — LLM extraction typed preference patch

## Цель

Добавить типизированный structured-output контракт для извлечения явных
изменений предпочтений hotel search и безопасно преобразовать его в операции
`KEEP`/`SET`/`CLEAR` Stage 12.2. Этап не активирует refinement в runtime, не
формирует Hotels API filters и не запускает provider search.

## Что добавлено

`LlmHotelSearchPreferencesPatch` представляет только подтвержденные
provider-neutral значения:

| Поле | Значение `SET` | Снятие ограничения |
|---|---|---|
| `max-total-price` | Положительная общая стоимость; отсутствие валюты означает будущий default `RUB` | Ключ в `clear` |
| `stars` | Непустой набор категорий `0..5` | Ключ в `clear` |
| `min-guest-rating` | Один из порогов `5`, `6`, `7`, `8`, `9` | Ключ в `clear` |
| `free-cancellation` | Только `true` | Ключ в `clear` |

Отсутствующее значение означает `KEEP`. Одновременные `SET` и `CLEAR` одного
поля считаются конфликтом. Пользовательская сортировка не входит в контракт,
поскольку provider отклонил ее на Stage 12.1b.

`MapLlmHotelSearchPreferencesPatchUseCase` проверяет значения и атомарно
преобразует LLM-модель в существующий `HotelSearchPreferencesPatch`. Он не
зависит от routes, provider DTO или transport. Изолированный тест подтверждает,
что полученный patch может быть применен существующим session-bound use case
без потери обязательных критериев.

## Структурированный ответ OpenRouter

Добавлен явный `OpenRouterCandidateContract.HOTEL_SEARCH_REFINEMENT`. Его
строгая JSON Schema содержит обязательный объект `preferencePatch`, в котором
каждое nullable поле означает отсутствие изменения, а `clear` содержит только
канонические имена предпочтений.

Системная инструкция фиксирует следующие правила:

- optional preferences не попадают в `missingRequiredFields`;
- бюджет относится ко всему периоду проживания;
- точный рейтинг ограничен дискретными порогами `5..9`;
- `8.5`, `10` и расплывчатая формулировка требуют уточнения без округления;
- бесплатная отмена устанавливается только явным `true`;
- снятие ограничения выражается через `clear`;
- sort preferences не извлекаются.

Транспортный DTO не принимает неизвестные поля, повторяющиеся значения
stars/clear или
неизвестные имена для `clear`. `LlmCandidateValidator` дополнительно проверяет
бизнес-диапазоны, валюту `RUB`, отсутствие конфликтующих операций и запрет
считать preferences обязательными полями.

## Fail-closed граница runtime

`OpenRouterLlmClient` по умолчанию и `LlmProviderFactory` продолжают использовать
`CORE_HOTEL_SEARCH`. В этом профиле:

- `preferencePatch` отсутствует в отправляемой JSON Schema;
- неожиданный `preferencePatch` в ответе отклоняется как invalid candidate;
- `AssistantLlmRouteWiringUseCase` и session runtime не изменены;
- confirmation и provider search не могут быть запущены новым patch-контрактом.

Профиль refinement должен быть активирован только на Stage 12.5 после Stage
12.4, когда Hotels API mapping сможет фактически применить все четыре фильтра.
Это исключает ложное подтверждение фильтров, которые provider request еще не
использует.

## Проверки

Точечные тесты покрывают:

- строгую refinement schema и typed mapping нескольких `SET` плюс `CLEAR`;
- сохранение core-профиля по умолчанию и fail-closed отказ от неожиданного
  refinement output;
- уточнение для неподдерживаемого рейтинга без добавления preference в
  `missingRequiredFields`;
- `KEEP`, `SET`, `CLEAR`, session accumulation и сохранение core criteria;
- неверную цену, валюту, stars, rating, cancellation и конфликт операций;
- запрет preference patch для unsupported intent.

Выполнены и прошли:

- точечные тесты `OpenRouterLlmClientTest`, `LlmCandidateValidatorTest` и
  `MapLlmHotelSearchPreferencesPatchUseCaseTest`;
- полный `services/backend` test suite;
- `git diff --check`.

## Границы этапа

Stage 12.3 не меняет:

- `LlmProviderFactory` runtime profile;
- `AssistantLlmRouteWiringUseCase`, routes или public API;
- Hotels API DTO, filter mapping, transport или provider execution;
- confirmation lifecycle, OpenAPI, demo shell или generated clients;
- pagination, auth, durable storage, booking или payment.

Live OpenRouter и Hotels API calls не выполнялись.

## Следующий этап

Stage 12.4 должен детерминированно преобразовать четыре domain preferences в
подтвержденные Hotels API filter DTO, сохранить один request с `offset=0` и
`limit=20` и не активировать refinement runtime. После этого Stage 12.5 сможет
одновременно включить refinement-профиль LLM, session patch и новый
confirmation/search flow.

## Verdict

`PASS_STAGE_12_3_LLM_PREFERENCE_PATCH_EXTRACTION`.

Typed LLM extraction и fail-closed преобразование готовы. Provider mapping и
runtime activation остаются отдельными этапами.
