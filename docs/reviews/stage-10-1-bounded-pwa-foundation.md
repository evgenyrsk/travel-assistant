# Stage 10.1 — ограниченная PWA foundation

## Роль документа

Это отчет о реализации первого ограниченного cross-platform среза. Текущий
статус проекта задает [`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Добавить устанавливаемую presentation foundation для существующего chat-first
hotel flow без native clients, offline search, изменения backend/public API или
сохранения пользовательских и provider-данных.

## Реализация

- добавлен `manifest.webmanifest` с `id`, `start_url`, `scope`,
  `display=standalone`, локализованным описанием и цветами текущего интерфейса;
- добавлены локальные непрозрачные PNG icons 192x192 и 512x512, пригодные для
  обычного и maskable назначения, а также 180x180 для Apple touch icon;
- основная и диагностическая страницы связываются с manifest и содержат
  theme/mobile metadata;
- viewport поддерживает `viewport-fit=cover`, layout учитывает safe-area через
  CSS `env()` и сохраняет прежнее responsive поведение;
- frontend server отдает `.webmanifest` как `application/manifest+json`, а PNG
  icons как `image/png`;
- локальная статика и проксируемые `/api/v1/**` responses получают
  `Cache-Control: no-store`.

## Online-only граница

Service worker и Cache Storage не добавлены. Transcript, hotel offers, API
responses, provider facts, secrets и configuration не кэшируются. Manifest не
означает поддержку offline hotel search или доступность stale результатов.

Статические assets также пока не кэшируются: их cache policy можно разрешить
только отдельным этапом, если это потребуется. Текущая реализация предпочитает
предсказуемое online-only поведение преждевременной offline-семантике.

## Проверки

Frontend tests проверяют:

- наличие manifest и mobile metadata на обеих страницах;
- standalone presentation и ожидаемые manifest fields;
- PNG signature и точные размеры 180/192/512;
- отсутствие service worker и Cache Storage;
- `application/manifest+json` и `Cache-Control: no-store` в frontend server.

Пройдены `npm test` (17 tests), `npm run lint` и `npm run build`.

Локальный browser QA подтвердил:

- основной экран загружается без console/page errors;
- desktop layout сохраняет двухколоночную структуру;
- при viewport 390x844 отсутствует горизонтальный overflow;
- manifest определяется страницей, а viewport содержит `viewport-fit=cover`;
- manifest и PNG icon возвращают `200`, корректные MIME и
  `Cache-Control: no-store`.

## Границы

- Backend, public API, OpenAPI и provider/LLM runtime не изменены.
- Native iOS/Android clients, generated clients и новый frontend framework не
  добавлены.
- Auth, durable storage, account history и cross-device sync не добавлены.
- Hotel details, shortlist и отдельный explanation/comparison flow не
  реализованы и не маскируются статусом PWA foundation.
- Готовность к внешнему rollout и одинаковая installability во всех браузерах
  не заявлены.

## Следующий этап

Рекомендуется Stage 10.2 — mobile/accessibility verification:

- desktop и mobile viewport browser matrix;
- standalone/safe-area visual check;
- keyboard order, labels, live regions и screen-reader-oriented audit;
- touch target и overflow regression;
- без native clients и расширения product flow.

После этой проверки отдельной задачей нужно выбрать последовательность закрытия
hotel details, current-session shortlist и интерактивного comparison/explanation.

## Verdict

`PASS_BOUNDED_PWA_FOUNDATION`.

Stage 10.1 реализует только online-only PWA foundation. Это не полное закрытие
MVP v1, Stage 10 или production readiness.
