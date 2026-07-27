# Локальная демонстрация MVP

## Назначение

Этот runbook запускает локальную demo shell и backend Travel Assistant как одну
демонстрационную сессию. Он не описывает deployment и не превращает demo shell
в будущий продуктовый web-клиент.

Доступны два явных профиля:

- `--fake` — детерминированные `FAKE` LLM, Hotels и semantic analysis;
- `--real` — OpenRouter LLM и публичный Hotels API с реальными provider data,
  при этом semantic analysis остаётся `FAKE`.

Production defaults остаются `FAKE`; launcher меняет режимы только для своих
дочерних локальных процессов. REAL semantic analysis не активирован: launcher
явно фиксирует `ACCOMMODATION_ANALYSIS_MODE=FAKE` в обоих профилях до отдельного
Stage 16.9.

## Подготовка

Требуются Java 17, Node.js 18+ и `npm`. Порты `8080` и `4173` должны быть
свободны. Другие порты можно задать через `DEMO_BACKEND_PORT` и
`DEMO_FRONTEND_PORT`.

Создайте локальный `.env` из примера и не добавляйте его в Git:

```bash
cp .env.example .env
```

Для `--real` обязательны:

```text
OPENROUTER_API_KEY=<local-secret>
OPENROUTER_MODEL=<operator-selected-model>
```

Рекомендуемые значения публичного Hotels API уже совпадают с defaults backend:

```text
HOTELS_API_PUBLIC_BASE_URL=https://hotels.tbank.ru/
HOTELS_API_PUBLIC_TIMEOUT_MS=60000
HOTELS_API_USER_LANGUAGE=RU
```

Launcher читает env-файл как пары `KEY=VALUE`, не выполняет его как shell-код,
импортирует только перечисленные demo/provider-настройки и не выводит значения
secrets. Опасные или посторонние переменные вроде `NODE_OPTIONS` из файла
игнорируются. Значения, уже заданные в shell environment, имеют приоритет над
`.env`.

## Предварительная проверка

```bash
node scripts/local-demo.mjs --fake --check-only
node scripts/local-demo.mjs --real --check-only
```

Проверяются версия Java и Node.js, наличие `npm`/Gradle wrapper, обязательные
REAL-параметры и доступность портов. Сетевых запросов к providers в режиме
`--check-only` нет. После успешной проверки launcher выводит только безопасную
сводку трёх режимов без API key, model slug или endpoint:

```text
Runtime modes: LLM=FAKE, Hotels=FAKE, Semantic=FAKE
Runtime modes: LLM=OPENROUTER, Hotels=REAL, Semantic=FAKE
```

Первая строка соответствует `--fake`, вторая — `--real`. Значение
`ACCOMMODATION_ANALYSIS_MODE` из локального env-файла не импортируется и не
может включить REAL semantic analysis через demo launcher.

## Запуск

Детерминированный профиль:

```bash
node scripts/local-demo.mjs --fake
```

Основной демонстрационный профиль:

```bash
node scripts/local-demo.mjs --real
```

После сообщения `Demo shell: http://127.0.0.1:4173` откройте этот адрес в
браузере. Логи процессов хранятся локально в `.tmp/local-demo/` с ограниченными
правами и не входят в Git.

## Основной сценарий

1. Отправьте полный запрос: «Найди отель в Казани с 10 по 14 августа 2026 года
   для двух взрослых без детей, одна комната».
2. Убедитесь, что ассистент показывает вопрос «Проверить отели по этим
   параметрам?» и еще не показывает карточки.
3. Отправьте отдельное сообщение «Да».
4. Дождитесь загрузки предложений.
5. Проверьте, что demo shell показывает не более пяти карточек из provider pool
   размером до 20.
6. Нажмите «Подробнее» у одной выбранной карточки.
7. Проверьте, что появились только доступные сведения выбранного отеля, а
   остальные карточки не загрузили details автоматически.

До подтверждения `hotelSearchId` не должен появляться. Браузер обращается только
к локальным `/api/v1/**`; OpenRouter и Hotels API вызываются backend. Публичные
URL используют только opaque `hotelSearchId` и `offerId`, а provider `hotelId`
остаётся внутри backend.

Для проверки refinement после первого результата можно отдельной репликой
установить один или несколько поддержанных фильтров, проверить новый полный
confirmation prompt и подтвердить новый поиск. Предыдущий `hotelSearchId`
остаётся доступен до завершения локального процесса.

Детерминированный semantic flow проверяется только через `--fake`. В профиле
`--real` semantic-запрос использует безопасную mixed-mode policy Stage 16.8a:
Hotels API и semantic analyzer не вызываются, а поиск завершается terminal
`failed` без показа обычных hotel offers.

## Завершение

Нажмите `Ctrl+C` в терминале launcher. Он отправит сигнал завершения backend и
demo shell и при необходимости завершит их process groups принудительно.

## Диагностика

| Ситуация | Действие |
|---|---|
| Java 17 не найдена | Настройте `JAVA_HOME` или `PATH` и повторите `--check-only` |
| Порт занят | Завершите локальный процесс либо задайте другой `DEMO_*_PORT` |
| REAL-конфигурация отклонена | Проверьте наличие ключа и модели без вывода их значений |
| Неясно, какие provider modes активны | Повторите `--check-only` и проверьте строку `Runtime modes` |
| Backend не стал доступен | Проверьте `.tmp/local-demo/backend.log` локально |
| Demo shell не стала доступна | Проверьте `.tmp/local-demo/frontend.log` локально |
| Provider flow завершился ошибкой | Не повторяйте REAL smoke автоматически; зафиксируйте только безопасную категорию |

Логи, `.env`, raw OpenRouter/Hotels responses, request headers и secrets нельзя
публиковать в документации, чате или Git.

## Границы

Локальное демо не подтверждает production readiness, SLA, публичный rollout,
auth, durable storage, CORS или готовность product web/mobile clients. Rates,
deeplink, shortlist, отдельный comparison flow, booking и payment не входят в
этот демонстрационный срез. REAL semantic/model call и передача provider
descriptions или images внешней модели также не входят в launcher profiles.
