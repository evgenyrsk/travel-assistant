# Stage 14.4 — безопасная диагностика LLM и понятные сообщения о сбоях

## Цель

Сделать неожиданный fallback в chat-first flow диагностируемым без раскрытия
пользовательского сообщения, LLM-ответа, модели, ключа или session data, а
также перестать объяснять временный сбой как отсутствие параметров поездки.

## Результат воспроизведения

Контрольный запрос с формулировкой «с супругой на завтра на одну ночь ... с
завтраками» был выполнен один раз через текущий OpenRouter contract. Ответ
имел HTTP status `200`, корректно выделил Москву, две даты 2026 года, двух
взрослых, один номер и требование завтрака. Candidate прошёл production decoder
и application validator.

Детерминированная ошибка разбора этой фразы не подтверждена. Точную причину
ранее показанного fallback восстановить невозможно: runtime использовал
`OpenRouterDiagnosticObserver.NONE`, а публичный ответ намеренно не содержал
internal reason.

## Что изменено

- Production runtime использует `SafeLlmDiagnosticLogger`.
- OpenRouter transport и application fallback записывают только фиксированные
  enum-категории.
- Временный provider/client failure просит повторить сообщение, а не повторно
  перечислять уже указанные параметры.
- Невалидный или неоднозначный candidate, противоречивые параметры и
  unsupported intent получают разные безопасные пользовательские тексты.
- System prompt явно закрепляет вычисление выезда по числу ночей и двух
  взрослых для формулировки о поездке с супругом или партнёром.
- Exact phrase закреплена route-level regression test с timezone устройства,
  одним номером по умолчанию и требованием завтрака.

## Формат логов

Разрешены только две формы:

```text
component=llm source=openrouter event=<FIXED_ENUM>
component=llm source=assistant event=<FIXED_ENUM>
```

Логи не содержат prompt, user message, raw response, API key, model slug, URL,
provider metadata, `sessionId`, `hotelSearchId` или provider IDs. Успешное
декодирование имеет уровень `INFO`, failure/fallback — `WARNING`.

## Retry policy

Новый retry не добавлен. Runtime OpenRouter уже использует
`LlmCandidateRetryPolicy.SINGLE_RETRY` только для разрешённых retryable
категорий. Последовательность диагностических событий позволяет увидеть обе
попытки, но не расширяет лимит запросов.

## Проверки

- разные fallback reasons дают корректные публичные сообщения без raw reason;
- application observer получает только typed category;
- logger формирует только фиксированную безопасную строку;
- exact phrase доходит до понятного confirmation без `hotelSearchId`;
- OpenRouter prompt содержит правила одной ночи и поездки с супругом;
- targeted и полный backend suite выполняются отдельно от live-вызова.

## Границы

- публичная API shape, OpenAPI и demo frontend не меняются;
- provider calls, hotel search и confirmation lifecycle не меняются;
- prompt и response bodies не сохраняются;
- correlation IDs, metrics, tracing и промышленный observability stack не
  добавляются;
- результат не является production readiness.

## Verdict

`IMPLEMENTED_AND_LOCALLY_VERIFIED` после прохождения обязательных gates.

Stage 14.4 закрывает диагностический пробел обнаруженного сценария. Повторная
ручная проверка REAL demo остаётся пользовательской проверкой, а отсутствие
live image facts по-прежнему не закрывает Stage 14.1c.
