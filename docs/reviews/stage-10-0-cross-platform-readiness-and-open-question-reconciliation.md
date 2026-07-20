# Stage 10.0 — готовность к cross-platform и сверка открытых вопросов

## Роль документа

Это review/design-only отчет о выборе первого ограниченного направления Stage 10.
Текущий статус проекта задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель и текущая точка

Stage 9 завершил внутренний pilot chat-first сценария: запрос на естественном
языке, уточнение параметров, подтверждение, provider-backed поиск и вывод до
пяти ранжированных предложений. Stage 10.0 должен определить, какой следующий
клиентский срез не перепишет product/domain logic и не расширит hotel-only MVP.

Этап не меняет production code, public API, runtime, OpenAPI или frontend.

## Фактическая готовность

| Область | Фактическое состояние | Вывод |
|---|---|---|
| Web-клиент | Chat-first интерфейс адаптируется к desktop и mobile viewport | Подходящая основа первого cross-platform среза |
| Интеграции | OpenRouter и Hotels API вызываются только backend; `FAKE` остается default | Клиенту не нужны provider credentials или provider DTO |
| Состояние | Transcript хранится в странице, backend stores — process-local | Нет обещания resume, cross-device sync или account history |
| Результаты | Показаны до пяти карточек из пула до 20 предложений | Проверенный ограниченный presentation flow |
| Hotel details | Отдельный реализованный пользовательский flow отсутствует | Остается разрывом полного MVP v1 |
| Shortlist | Routes существуют как `Not Implemented` placeholders | Остается разрывом полного MVP v1 |
| Explanation/comparison | Есть ranking и `matchSummary`; отдельный explanation route — placeholder | Не считать полноценным интерактивным сравнением |
| Native clients | iOS/Android implementation отсутствует | Не начинать без подтвержденной native-only ценности |

Завершение Stage 9 означает завершение ограниченного внутреннего pilot-среза,
но не доказывает реализацию всех концептуальных возможностей MVP v1.

## Выбор первого cross-platform направления

Первый ограниченный target — устанавливаемый responsive web-клиент (PWA) для
текущего chat-first hotel flow.

Причины выбора:

- текущий интерфейс уже работает на desktop и mobile viewport;
- backend и public HTTP boundary можно переиспользовать без дублирования
  domain/application logic;
- native-only сценарий, требующий iOS или Android клиента, не подтвержден;
- PWA позволяет отдельно проверить installability, standalone layout и mobile
  ergonomics до появления стоимости двух native codebases.

Первый PWA-срез остается online-only. Он не должен кэшировать `/api/**`,
transcript, hotel offers, provider responses, secrets или configuration.
Допустимо кэшировать только локальные статические assets после отдельной
проверки cache policy. Offline hotel search и stale provider facts запрещены.

## Сверка перенесенных вопросов

| Вопрос | Принятая policy | Статус для Stage 10.1 |
|---|---|---|
| Официальный server-to-server статус Hotels API | Наблюдаемый public API не объявляется официальным. Публичный rollout блокируется до письменного подтверждения владельца API | Не блокирует локальную PWA-основу |
| SLA и rate limits | Не выдумывать значения; сохранять один ограниченный search call без pagination и агрессивных повторов | Не блокирует локальную PWA-основу, блокирует внешний rollout |
| Taxes/fees в `shownPrice` | Показывать provider amount как total за выбранный период без перерасчета; не утверждать, что налоги и сборы включены | Решение закрыто для отображения |
| `LIMITED` | Не выводить статус из количества комнат или эвристического threshold; REAL mapping использует только подтвержденные `AVAILABLE`/`UNKNOWN` | Решение закрыто для текущего provider |
| Durable storage и auth | Не нужны для первого локального PWA-среза; обязательны только для обещаний resume, account history или cross-device sync | Не блокируют Stage 10.1 |
| Source/freshness | Не создавать отсутствующие provider facts; отдельное отображение возможно только после контрактного решения | Не блокирует Stage 10.1 |
| Accessibility | Существующие keyboard focus и `prefers-reduced-motion` сохраняются; mobile и screen-reader QA нужны до расширенного пилота | Проверка следующего клиентского цикла |

Для Stage 10.1 не осталось owner decisions. Неизвестные внешние условия не
заменены предположениями: им назначены явные rollout gates.

## Архитектурные последствия

- PWA обращается только к Travel Assistant `/api/v1/**`.
- OpenRouter key, Hotels API details и provider DTO не попадают в клиент.
- Текущий легковесный frontend сохраняется; новый framework не требуется.
- Diagnostic page остается отдельным техническим инструментом.
- Native iOS/Android clients, generated clients, auth и durable storage не
  создаются автоматически.
- Выбор PWA не требует ADR: backend/domain boundaries не меняются. Изменение
  client architecture или появление native clients потребует отдельного
  решения.

## Границы полного MVP v1

`hotel details`, current-session shortlist и отдельный интерактивный
explanation/comparison flow остаются заявленными возможностями MVP v1, но не
реализованы текущим pilot-срезом. Stage 10.1 не должен маскировать этот разрыв.
Он не блокирует PWA foundation, но блокирует заявление о полной реализации MVP
v1 и закрытие Stage 10 без отдельного решения или реализации.

## Следующий безопасный этап

Рекомендуется Stage 10.1 — bounded PWA foundation:

- web app manifest и локальные installability assets;
- standalone/mobile metadata и safe-area layout;
- явная online-only/cache boundary без кэширования API и пользовательских
  данных;
- статические и frontend tests без новых provider/network calls;
- browser QA в desktop и mobile viewport;
- без native clients, runtime/API changes, auth и durable storage.

После Stage 10.1 нужен отдельный выбор между mobile/accessibility verification
и закрытием `hotel details`/shortlist/explanation gaps. Он не активируется этим
документом.

## Verdict

`PASS_STAGE_10_PLANNING_PWA_FOUNDATION_ALLOWED`.

Stage 10 активирован только на уровне planning. Stage 10.1 разрешен как
ограниченная PWA foundation; native expansion, внешний rollout и полная
готовность MVP не заявлены.
