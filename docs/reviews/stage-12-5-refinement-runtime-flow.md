# Stage 12.5 — runtime-поток уточнения hotel search

## Цель

Подключить уже проверенные preferences к chat-first runtime: сохранить явные
изменения пользователя в контексте сессии, повторно показать полный набор
критериев и выполнить новый provider search только после отдельного
подтверждения.

## Реализованный поток

Production `LlmProviderFactory` использует строгий
`HOTEL_SEARCH_REFINEMENT` contract для opt-in OpenRouter runtime. Контракт
доступен с первого сообщения, поэтому пользователь может назвать необязательные
предпочтения как в исходном запросе, так и после первой выдачи. `FAKE` остается
режимом по умолчанию.

Для безопасного hotel-only candidate выполняются последовательные шаги:

1. явно извлеченные основные критерии накапливаются по `AssistantSessionId`;
2. `LlmHotelSearchPreferencesPatch` преобразуется в typed `KEEP`/`SET`/`CLEAR`;
3. patch атомарно применяется к `HotelSearchPreferences`;
4. полный обновленный контекст передается в confirmation plan;
5. provider не вызывается до отдельного ответа «Да»;
6. после подтверждения выполняется один новый поиск с текущими preferences.

Текущие preferences передаются в следующий LLM request через
`confirmedConstraints`. Отсутствующее в новой реплике значение означает
`KEEP`; явное снятие ограничения остается операцией `CLEAR`. Изменение основных
критериев больше не сбрасывает preferences.

## Confirmation и поиски

Confirmation prompt показывает основные критерии и все активные preferences в
стабильном порядке. Уточняющее сообщение само по себе не создает
`hotelSearchId` и не вызывает Hotels API.

После успешного подтвержденного уточнения создается новый process-local
`hotelSearchId`. Предыдущий поиск и его offers остаются доступны по прежнему
идентификатору. Idempotency basis уже учитывает preferences, поэтому разные
наборы ограничений являются разными поисковыми попытками.

Отказ от нового confirmation, LLM failure или отклоненный patch не создают
новый поиск. Сохраненные preferences не теряются. Provider failure продолжает
использовать существующую безопасную ветку без нового `hotelSearchId` и без
раскрытия внутренних причин.

## Проверки

Точечные и интеграционные тесты подтверждают:

- несколько `SET` в одной реплике и последующий `CLEAR`;
- сохранение preferences при изменении основных критериев;
- передачу активных preferences в следующий LLM request;
- полный confirmation prompt без преждевременного `hotelSearchId`;
- отсутствие Hotels API request до подтверждения;
- ровно один новый search request после «Да»;
- точные четыре provider filters и отсутствие `sort`;
- доступность предыдущих поисков после refinement;
- отсутствие нового поиска при отказе и LLM failure;
- активацию strict refinement schema в OpenRouter runtime;
- отсутствие регрессий в прежнем накоплении hotel constraints.

Выполнены точечные тесты Stage 12.5 и полный backend test suite. Также пройдены
`git diff --check`, проверка production-кода на `runBlocking` и проверка границ
diff.

## Границы этапа

Stage 12.5 не меняет public API, OpenAPI, routes shape или demo shell. Не
добавлены пользовательская сортировка, pagination, polling,
`search-filters-availability`, новые endpoint-ы, durable storage, auth,
booking или payment. Live OpenRouter и Hotels API calls не выполнялись: внешний
runtime проверен только через `MockEngine`.

Публичные поля `starRating`, cancellation fact и `appliedPreferences` остаются
за пределами этапа.

## Следующий этап

Stage 12.6 должен отдельно согласовать platform-neutral response: добавить
только подтвержденные необязательные facts и активные preferences в offers
response, обновить OpenAPI/conformance и demo shell без изменения уже
стабилизированного backend refinement flow.

## Verdict

`PASS_STAGE_12_5_REFINEMENT_RUNTIME_FLOW`.

Итеративное уточнение четырех preferences работает через повторное
подтверждение и новый ограниченный provider search. Публичное отображение
результата остается отдельным Stage 12.6.
