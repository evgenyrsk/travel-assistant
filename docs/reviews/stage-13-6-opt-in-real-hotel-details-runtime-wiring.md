# Stage 13.6 — opt-in REAL runtime деталей отеля

## Цель

Подключить Stage 13.4 details adapter к runtime только в явном режиме
`HOTEL_PROVIDER_MODE=REAL`, сохранив `FAKE` по умолчанию и единый lifecycle
Hotels API `HttpClient`.

## Реализация

- `HotelOfferProviderRuntime` владеет двумя application boundaries: search и
  details.
- REAL factory создаёт один `HttpClient`, один
  `PublicHotelsApiHttpTransport`, search orchestration и
  `HotelsApiHotelDetailsProviderAdapter`.
- Один close action закрывает общий client не более одного раза.
- `Application.kt` получает details boundary только из hotel provider runtime;
  параллельный client или независимый REAL wiring не создаётся.
- `FAKE` остаётся default и использует отдельный детерминированный
  `FakeHotelDetailsProvider` без network calls.

## Runtime policy

Details не загружаются при search или чтении offers. Единственный вызов
возможен после явного GET к selected-offer details endpoint. N+1, preload,
pagination и retry не добавлены.

## Проверки

- Factory tests подтверждают две boundary для `FAKE` и `REAL`.
- REAL factory создаёт ровно один `HttpClient`; runtime close идемпотентен.
- Детерминированные fake details покрыты отдельно.
- Application integration test через один `MockEngine` выполняет
  autocomplete, search и один selected details GET.
- Публичный `offerId` не равен provider reference; details response не содержит
  provider identity.
- Targeted и полный backend suite и `git diff --check` пройдены.

## Границы

Live-вызов не выполнялся. Public API/OpenAPI, frontend, configuration,
credentials, auth, durable storage, rates, deeplink и booking не менялись.

## Verdict

`PASS_STAGE_13_6_OPT_IN_REAL_HOTEL_DETAILS_RUNTIME_WIRING`.

Следующий этап — Stage 13.7: явная кнопка «Подробнее» и on-demand отображение
details в локальной demo shell.
