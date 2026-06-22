# Travel Assistant Frontend

Минимальный frontend-сценарий Stage 7.51 без внешних зависимостей, с отдельной формой поиска отелей.

## Запуск

Backend должен быть доступен локально на `http://127.0.0.1:8080`. Локальный frontend-сервер проксирует `/api/v1/**` в backend, поэтому изменения CORS не нужны.

```bash
cd app
npm run dev
```

По умолчанию UI доступен на `http://127.0.0.1:4173`. Другой backend можно указать через `BACKEND_URL`, другой порт frontend — через `PORT`.

## Проверки

```bash
npm test
npm run lint
npm run build
```

Frontend использует ручной клиент на `fetch`. Generated clients не создаются и не используются.
