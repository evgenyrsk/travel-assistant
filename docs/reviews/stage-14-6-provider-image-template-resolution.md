# Stage 14.6 — разрешение provider image template

## Цель

Установить точную причину отсутствия фотографий в REAL-карточках и исправить
её без image proxy, N+1, нового provider endpoint или выдуманных изображений.

## Подтверждённый контракт

23 июля 2026 года выполнены ограниченные анонимные проверки без
`Authorization`, cookies, redirects и автоматических повторов:

- `POST /api/v1/hotels/search` вернул `200 application/json` и 20 отелей;
- каждый отель содержал от 21 до 160 HTTPS image templates;
- template имеет буквальный сегмент `{size}` и поэтому не является готовым
  URI;
- `POST /search-api/v1/hotels/getHotelStaticInfo` также вернул
  `200 application/json`, но использует тот же формат image template;
- запрос неразрешённого template вернул `403 AccessDenied`;
- замена `{size}` на `1024x768` вернула `200 image/jpeg`.

Raw body и headers использовались только в `/tmp`. В репозиторий добавлен
обезличенный fixture с сохранёнными JSON types, nesting и template shape.

## Причина дефекта

Общая `HotelsApiSafeImageUrlPolicy` пыталась создать `URI` до разрешения
`{size}`. Строка с фигурными скобками отклонялась, поэтому public offer не
получал `imageUrl`, а frontend корректно показывал placeholder.

Отсутствие фотографий не было вызвано CSS, `referrerpolicy=no-referrer`,
отсутствием provider image facts или необходимостью details lookup.

## Изменения

- подтверждённый `{size}` заменяется на `1024x768` до URI validation;
- подстановка разрешена только для `extranet-cdn.tinkoff.ru`;
- допускается ровно один `{size}`;
- неизвестные или повторные placeholders отклоняются;
- полные безопасные HTTPS URL сохраняют прежнее поведение;
- search и details используют одну и ту же policy;
- public API и frontend contract не меняются.

## Проверки

- unit tests покрывают разрешение template и fail-closed варианты;
- provider-derived fixture проходит DTO и response mapper;
- один REAL browser smoke выполнил confirmation и один search;
- demo shell показала пять карточек с пятью `<img>`;
- все изображения имели `complete=true`, `naturalWidth>0` и не содержали
  неразрешённый `{size}`.

## Границы

- `getHotelStaticInfo` не подключён к runtime: основной search уже содержит
  необходимые templates;
- image proxy, cache и массовые details-запросы не добавлены;
- provider `hotelId`, raw response и служебные headers не раскрываются;
- `FAKE` остаётся default, stores остаются process-local;
- результат не является production readiness.

## Verdict

`PASS_STAGE_14_6_PROVIDER_IMAGE_TEMPLATE_RESOLUTION`.

Stage 14.1c теперь закрыт: ветка реальных изображений подтверждена без
расширения public contract или provider orchestration.
