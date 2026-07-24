# Stage 11.0 — готовность локальной REAL-демонстрации MVP

## Роль и цель

Это отчет о воспроизводимости локального демонстрационного среза Travel
Assistant. Текущий статус проекта задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md), а инструкция запуска находится
в [`docs/guides/local-mvp-demo.md`](../guides/local-mvp-demo.md).

Этап не означает production readiness и не превращает локальную demo shell в
будущий продуктовый web-клиент.

## Что добавлено

- `scripts/local-demo.mjs` с обязательными `--fake`/`--real` профилями;
- безопасное чтение локального `.env` без выполнения shell-кода;
- проверка Java 17, Node.js, `npm`, Gradle wrapper, конфигурации и портов;
- единый lifecycle backend и demo shell, health wait и корректное завершение;
- локальные логи в игнорируемой `.tmp/local-demo/` с правами `0600`;
- unit tests launcher и русскоязычный runbook.

Production defaults остались `FAKE`. Профиль `--real` задает `OPENROUTER` и
`REAL` только дочерним локальным процессам. Профиль `--fake` удаляет
`OPENROUTER_API_KEY` из дочернего окружения.

## Контролируемый REAL smoke

Дата проверки: 21 июля 2026 года.

Использован один синтетический сценарий:

1. полный запрос на отель в Казани на 10–14 августа 2026 года для двух взрослых
   без детей и одной комнаты;
2. ожидание confirmation prompt;
3. отдельное подтверждение «Да»;
4. загрузка REAL Hotels offers;
5. проверка числа отображенных карточек.

| Проверка | Результат |
|---|---|
| Backend health | `200` |
| Demo shell | `200` |
| Confirmation prompt | Получен |
| Карточки до подтверждения | `0` |
| REAL provider pool | `20` предложений |
| Отображение demo shell | `5` карточек |
| Завершение процессов | Штатно через `Ctrl+C` |

Raw OpenRouter/Hotels responses, headers, provider data и secrets не
публиковались. Автоматический повтор пользовательского сценария и смена
модели/provider не выполнялись.

## Проверки

- launcher tests: `6/6`;
- `--fake --check-only`: пройден;
- `--real --check-only`: пройден без provider-вызовов;
- фактический FAKE lifecycle: backend и demo shell вернули `200` и штатно
  завершились;
- frontend tests: `20/20`;
- frontend lint и build: пройдены;
- полный backend `./gradlew test`: пройден;
- OpenAPI conformance tests: `10/10`;
- conformance check: blocking findings отсутствуют, `status=not_ready`,
  `readinessClaim=false`;
- `git diff --check`, secret/scope scan и локальные ссылки: пройдены.

## Подтвержденные границы

- browser обращается только к локальным `/api/v1/**`;
- OpenRouter и Hotels API вызываются backend;
- search не создается до явного подтверждения;
- `hotelSearchId` используется только после успешного confirmed flow;
- presentation limit `5` не меняет provider pool до `20`;
- launcher не меняет public API, OpenAPI schemas, backend behavior или frontend
  UX;
- `.env`, `.tmp/` и raw provider data не входят в Git.

## Что не входило в этап

- deployment и QA infrastructure;
- product web/mobile clients и SDK;
- CORS, auth, durable storage, resume и cross-device sync;
- booking, payment, hotel details, shortlist и отдельный comparison flow;
- изменение provider/LLM retry policy;
- заявление production readiness или готовности внешнего rollout.

## Риски и ограничения

- состояние остается process-local, transcript — browser-local;
- официальный server-to-server статус и долгосрочная стабильность публичного
  Hotels API не подтверждены;
- REAL demo зависит от локального ключа, выбранной OpenRouter model и внешней
  доступности providers;
- весь OpenAPI и generated clients остаются `not_ready`.

## Verdict

`PASS_STAGE_11_0_LOCAL_REAL_MVP_DEMO_READY`.

Локальный chat-first hotel flow воспроизводим в REAL-профиле. Следующий этап не
активирован и должен быть выбран отдельным roadmap-решением по итогам
демонстрации.
