# Локальная demo shell Travel Assistant

Легковесная chat-first оболочка для локальной демонстрации hotel-only MVP без
внешних зависимостей и сгенерированных клиентов. Это не будущий продуктовый
web-клиент: его UI, SDK и lifecycle будет определять отдельная интеграционная
команда.

Главная страница начинает пользовательский сценарий с естественного сообщения:

1. первое сообщение создаёт Assistant session;
2. следующие сообщения продолжают ту же локальную session;
3. `ask_clarification` и `show_boundary_message` отображаются в истории диалога;
4. подтверждение отправляется обычным сообщением;
5. при `show_hotel_results` frontend загружает предложения по `hotelSearchId`;
6. область результатов показывает первые пять предложений в порядке backend-ранжирования.

История диалога хранится только в памяти текущей страницы. OpenRouter и Hotels API
не вызываются из браузера: demo shell обращается только к Travel Assistant
backend через `/api/v1/**`.

Интерфейс адаптируется к desktop и mobile, сохраняет заметный keyboard focus и
учитывает `prefers-reduced-motion`. Внешние web fonts, изображения и frontend
dependencies не используются.

Stage 10.1 добавил ограниченную PWA foundation: web app manifest, локальные
installability icons, standalone/mobile metadata и safe-area layout. Клиент
остается online-only: service worker отсутствует, а frontend server возвращает
`Cache-Control: no-store` для локальных assets и проксируемых `/api/v1/**`
responses. Transcript, hotel offers и provider data не кэшируются.

Прежняя структурированная форма сохранена как диагностическая страница:

```text
http://127.0.0.1:4173/diagnostic.html
```

Она вызывает hotel-search route напрямую и не является основным
демонстрационным сценарием.

## Запуск

Backend должен быть доступен локально на `http://127.0.0.1:8080`. Frontend
server проксирует `/api/v1/**`, поэтому для текущего same-origin web/PWA flow
изменение CORS не требуется.

```bash
cd app
npm run dev
```

По умолчанию frontend доступен на `http://127.0.0.1:4173`. Другой backend можно
указать через `BACKEND_URL`, другой frontend port — через `PORT`.

## Кроссплатформенная граница

Demo shell использует platform-neutral JSON/HTTP API Travel Assistant. API client
поддерживает внедрение абсолютного `baseUrl` и не зависит от DOM, cookies,
browser storage или provider contracts. Это позволяет отдельным web, iOS,
Android и desktop clients переиспользовать `/api/v1/**`, не дублируя
provider/LLM orchestration и business rules.

Ограниченный продуктовый контракт Stage 10.3 состоит из трёх endpoint:

- `POST /api/v1/assistant/sessions`;
- `POST /api/v1/assistant/sessions/{sessionId}/messages`;
- `GET /api/v1/hotel-searches/{searchId}/offers`.

`hotelSearchId` присутствует только при `nextAction=show_hotel_results`.
Assistant message содержит от 1 до 4000 Unicode code points; malformed JSON и
unknown fields отклоняются безопасным `VALIDATION_ERROR`. Прямой
`POST /api/v1/hotel-searches` остается endpoint диагностической страницы и не
входит в основной клиентский контракт.

Текущий server обслуживает локальную same-origin demo shell через proxy. Для
будущего cross-origin продуктового web-клиента понадобится configurable CORS
allowlist; native clients не ограничиваются browser CORS. Product UI, native
SDK, generated clients, auth, durable session resume и cross-device sync в этом
репозитории не реализуются без отдельной задачи.
CORS остается выключенным по умолчанию; wildcard и credentialed origins не
разрешены.

## Проверки

```bash
npm test
npm run lint
npm run build
```

Автоматические тесты проверяют API paths, продолжение session, безопасные
clarification/boundary outcomes и ограничение presentation-слоя пятью уже
ранжированными предложениями.

Frontend не реализует ranking, pagination, booking, payment, durable transcript
storage или прямые provider calls.
