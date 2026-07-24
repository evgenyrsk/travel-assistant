# Travel Assistant Backend

Kotlin + Ktor backend является удалённым ядром hotel-only MVP: здесь находятся
application/domain logic, LLM/provider orchestration, подтверждение поиска,
process-local state и platform-neutral HTTP API. Локальная demo shell из
`../../app/` является только демонстрационным клиентом.

OpenAPI и generated clients сохраняют статус `not_ready`:

- `../../docs/architecture/stage-6/openapi-draft.yaml`.

Это рабочий локальный MVP, а не готовность к промышленному использованию.

## Запуск

Из директории `services/backend`:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home \
./gradlew run
```

Совместный запуск backend и demo shell выполняется из корня репозитория:

```bash
node scripts/local-demo.mjs --fake
node scripts/local-demo.mjs --real
```

Подробности находятся в
[`docs/guides/local-mvp-demo.md`](../../docs/guides/local-mvp-demo.md).

## Provider modes

Оба provider mode по умолчанию используют `FAKE`. REAL-интеграции включаются
только явно и завершают startup ошибкой при некорректной конфигурации.

### OpenRouter

```text
LLM_PROVIDER_MODE=OPENROUTER
OPENROUTER_API_KEY=<local-secret>
OPENROUTER_MODEL=<operator-selected-model>
OPENROUTER_BASE_URL=https://openrouter.ai/api/v1
OPENROUTER_TIMEOUT_MS=30000
```

`OPENROUTER_MODEL` не имеет default в коде. Ключ хранится только в локальном
environment. OpenRouter использует отдельный application-owned `HttpClient`;
его `Authorization` не может попасть в Hotels API transport.

### Hotels API

```text
HOTEL_PROVIDER_MODE=REAL
HOTELS_API_PUBLIC_BASE_URL=https://hotels.tbank.ru/
HOTELS_API_PUBLIC_TIMEOUT_MS=60000
HOTELS_API_USER_LANGUAGE=RU
```

Публичный Hotels API используется без `Authorization`. Destination search,
exact-hotel details/rates и on-demand details делят один application-owned
`HttpClient`. Browser-клиенты не обращаются к Hotels API напрямую и не получают
provider `hotelId`, room ID или `bookHash`.

## Активный HTTP-контракт

Все пути находятся под `/api/v1`.

Основной platform-neutral client flow:

- `POST /assistant/sessions`;
- `POST /assistant/sessions/{sessionId}/messages`;
- `GET /hotel-searches/{searchId}/offers`;
- `GET /hotel-searches/{searchId}/offers/{offerId}/details`.

Дополнительно:

- `GET /health` — operational endpoint;
- `POST /hotel-searches` — диагностический endpoint;
- shortlist и explanation routes — `501 Not Implemented` placeholders.

`hotelSearchId` появляется только вместе с
`nextAction=show_hotel_results`. `offerId` является opaque process-local
идентификатором Travel Assistant и не содержит provider reference. Details
разрешаются только внутри указанного search.

## Текущее поведение MVP

- LLM извлекает обязательные hotel constraints и пять необязательных
  preference: максимальную общую стоимость, звёзды, минимальный гостевой
  рейтинг, бесплатную отмену и включённый завтрак.
- Явная точная одиночная категория звёзд (`пятизвёздочный`, `5 звёзд`) проходит
  дополнительную детерминированную application-проверку, если LLM не заполнил
  `stars`; диапазоны и команды снятия ограничения по-прежнему не угадываются.
- Assistant flow всегда использует один номер как внутренний MVP-инвариант и
  не показывает его в обычном confirmation; явный multi-room запрос
  блокируется до provider call;
  явно переданное значение по-прежнему валидируется.
- `clientContext.timezone` объединяется с backend `Clock` и даёт текущую
  локальную дату для интерпретации «сегодня/завтра». Клиентский timestamp не
  принимается. Без корректной timezone относительные или не содержащие год
  даты требуют уточнения, а даты в прошлом не доходят до confirmation/search.
- Каждый ребёнок требует явного возраста `0..17`.
- Search не запускается до полного confirmation prompt и отдельного ответа
  пользователя.
- Изменение preference требует нового подтверждения и создаёт новый search;
  предыдущий process-local search остаётся доступен.
- Один provider request получает пул до 20 offers; demo shell показывает до
  пяти уже ранжированных предложений.
- Offer может содержать первый безопасный HTTPS `imageUrl` из search response.
  Подтверждённый шаблон `{size}` разрешается как `1024x768` только для
  `extranet-cdn.tinkoff.ru`; отсутствие изображения не отклоняет offer и не
  запускает details lookup.
- Требование включённого завтрака передаётся provider как
  `meal_types=["breakfast"]`. Offer содержит nullable `breakfastIncluded`:
  неподтверждённый `mealType` остаётся unknown.
- `completed_no_offers` отличается от provider failure и может содержать один
  безопасный `refinementSuggestion` без автоматического retry.
- Details загружаются только для явно выбранного offer. Массовой N+1-загрузки
  нет.
- Если autocomplete однозначно вернул конкретный отель и не вернул location,
  после confirmation backend выполняет один details и один v3 rates request.
  Строковый hotel reference не используется как `destinationId`; из тарифов
  выбирается один подходящий вариант для availability и общей цены.
- Если OpenRouter пропустил отсутствующий `destination`, application может
  дополнить его только из явно названного отеля. В лог попадает фиксированная
  категория `DESTINATION_ENRICHED`, но не пользовательский текст или название.
- Description sections проходят fail-closed allowlist; certification,
  registry, owner и contact data не входят в публичный details response.
- Неизвестные rating, amenities, taxes/fees и другие optional facts не
  заменяются нулевыми или выдуманными значениями.
- Provider DTO, raw response, internal exceptions, secrets и provider IDs не
  входят в публичные ответы.

Stores остаются process-local. CORS не установлен: default policy — deny, без
wildcard и credentials.

## Безопасная диагностика LLM

При opt-in OpenRouter runtime выводит только категориальные события:

```text
component=llm source=openrouter event=<FIXED_ENUM>
component=llm source=assistant event=<FIXED_ENUM>
```

Первая строка отражает transport/decoder outcome, вторая — итоговый fallback
после application validation или confirmation planning. Prompt, текст
пользователя, raw response, API key, model slug, URL, session/search IDs и
provider metadata не записываются. Успех имеет уровень `INFO`, ошибки —
`WARNING`. Для retryable failure существующая `SINGLE_RETRY` policy может дать
два последовательных события; дополнительный retry не выполняется.

## Проверка

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home \
./gradlew test
```

Полная локальная проверка также включает frontend и OpenAPI conformance gates,
описанные в `../../docs/development/quality-gates.md`.

## Намеренно не реализовано

- auth, account history, durable storage и multi-instance coordination;
- cross-device resume и product web/mobile clients;
- generated clients и финальная OpenAPI readiness;
- CORS allowlist и deployment infrastructure;
- pagination, пользовательская сортировка и автоматическое ослабление фильтров;
- публичный room/rates flow, deeplink, shortlist, comparison и chat-команды
  выбора карточки;
- booking, payment, flights и combined itinerary;
- production SLA, observability и security hardening.
