# Travel Assistant Frontend

Минимальный zero-dependency frontend-сценарий Stage 7.51 с отдельной hotel search формой.

## Запуск

Backend должен быть доступен локально на `http://127.0.0.1:8080`. Frontend server проксирует `/api/v1/**` в backend, поэтому изменения CORS не нужны.

```bash
cd app
npm run dev
```

По умолчанию UI доступен на `http://127.0.0.1:4173`. Другой backend можно указать через `BACKEND_URL`, другой frontend port — через `PORT`.

## Проверки

```bash
npm test
npm run lint
npm run build
```

Frontend использует ручной `fetch` client. Generated clients не создаются и не используются.
