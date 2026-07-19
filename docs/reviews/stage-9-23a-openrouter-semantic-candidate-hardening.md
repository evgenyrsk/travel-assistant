# Stage 9.23a — усиление семантического контракта OpenRouter candidate

## Роль документа

Это отчет о блокирующем результате первого шага Stage 9.23 и ограниченном
исправлении LLM-контракта. Текущий статус проекта задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Устранить обнаруженную перед внутренним пилотом неоднозначность между strict
JSON Schema OpenRouter и семантическими правилами `LlmCandidate`, не ослабляя
application validation и не добавляя новые повторы, смену модели или раскрытие
исходного ответа провайдера.

## Наблюдение пилота

19 июля 2026 года выполнен один разрешенный ход через локальный chat-first
frontend с режимами `OPENROUTER` и локальным для процесса переопределением
`REAL` для Hotels API. Использовался синтетический полный запрос на поиск отеля.

Результат:

- Assistant session создана;
- ответ вернул безопасный `show_boundary_message`;
- confirmation prompt не создан;
- `hotelSearchId` отсутствовал;
- подтверждение не отправлялось;
- Hotels API не вызывался.

После остановки среды выполнен один отдельно разрешенный диагностический QA-вызов
с `FAKE` Hotels. Он также завершился `show_boundary_message` и зафиксировал
только безопасные события `CANDIDATE_DECODED,CANDIDATE_DECODED`. Второе событие
соответствует уже существующей политике не более одного дополнительного вызова.

Доказанный вывод: HTTP, auth и JSON decoding прошли, но оба декодированных
candidate были отвергнуты application validation. Точные значения candidate намеренно не
сохранялись. Пустое nullable-значение или несогласованность `outcome` с
сопутствующими полями остаются обоснованными гипотезами, а не установленным
фактом исходного ответа.

## Изменения

`OpenRouterLlmClient` теперь:

- явно описывает семантику `outcome`, `intent`, constraints, clarification,
  conflicts и warnings внутри JSON Schema;
- требует через system prompt использовать `null`, а не пустую строку, для
  отсутствующих nullable-значений;
- фиксирует `YYYY-MM-DD`, счетчики в виде десятичных строк и формат
  `children-ages`;
- явно описывает согласованный полный `INTERPRETED` и неполный вариант
  `NEEDS_CLARIFICATION`;
- преобразует пустой nullable constraint в отсутствие значения, а не переносит
  его как фиктивный доменный факт.

Application validator не ослаблен: отсутствующие обязательные поля по-прежнему
ведут к clarification/fallback, а не к поиску. Официальная
[документация OpenRouter по structured outputs](https://openrouter.ai/docs/guides/features/structured-outputs)
рекомендует добавлять descriptions к свойствам схемы; новые неподтвержденные
JSON Schema keywords не использованы.

## Проверки

Точечные тесты подтверждают:

- наличие семантических правил в system prompt;
- наличие descriptions у outcome и constraint properties;
- преобразование пустого nullable wire constraint в отсутствие доменного
  ограничения;
- прежние outcomes декодирования, ошибок, cancellation и безопасности
  `OpenRouterLlmClient`;
- прежнее поведение candidate validator и ограниченной политики повторов;
- прием полного candidate с `children=0` и пустым optional `children-ages`.

Точечные тесты и полный backend suite пройдены. Новый live-вызов после
исправления в Stage 9.23a не выполнялся.

## Границы

Не изменены:

- public API, routes, OpenAPI, frontend и generated clients;
- модель, API key, base URL, timeout и provider routing;
- максимум одна дополнительная LLM-попытка;
- Hotels API transport, ranking и runtime composition;
- `FAKE` как режим по умолчанию;
- process-local storage, booking, payment и pagination.

## Verdict

`PASS_SEMANTIC_CONTRACT_HARDENED_PILOT_NOT_COMPLETE`.

Stage 9.23a закрывает только выявленное рассогласование контракта. Stage 9.23
остается в работе. Следующий безопасный шаг — один отдельно разрешенный повтор полного
chat-first happy path. Если candidate снова будет отвергнут, нужно остановить
пилот и отдельно согласовать безопасную диагностику application-уровня либо
выбор другой явно настроенной модели; автоматическая смена модели запрещена.
