# Stage 12.8 — проверка MVP с итеративным уточнением

## Цель

Подтвердить сквозной hotel-only сценарий Stage 12: первичный поиск, изменение
необязательных условий в чате, повторное подтверждение и новый provider search.
Проверка должна также доказать безопасное поведение при пустой выдаче и
ошибках без автоматических повторов или изменения пользовательских условий.

## Проверенный сценарий

| Сценарий | Результат |
|---|---|
| Первичный поиск без фильтров | Поиск выполняется только после «Да» и сохраняет полный пул до 20 предложений |
| Несколько фильтров одной репликой | Бюджет, звёзды, рейтинг и бесплатная отмена входят в полный повторный confirmation |
| Снятие одного фильтра | Удаляется только явно названное условие; остальные сохраняются |
| Отказ от повторного поиска | Новый `hotelSearchId` не создаётся, предыдущий поиск остаётся доступен |
| Успешная пустая выдача | Создаётся `COMPLETED_NO_OFFERS` и возвращается один typed-совет без нового provider call |
| Ошибка LLM | Новый поиск не создаётся, ранее сохранённые preferences не теряются |
| Ошибка provider после refinement | Новый `hotelSearchId` отсутствует, предыдущий поиск доступен, preference остаётся в session context |
| Provider mapping | Четыре filters передаются в проверенном порядке только после подтверждения; `sort` отсутствует |

Targeted integration tests дополнены проверками пустой выдачи и provider
failure после refinement. В обоих случаях зафиксировано отсутствие скрытого
третьего запроса, автоматического retry и mutation предыдущего поиска.

## Контролируемый REAL smoke

22 июля 2026 года выполнен один локальный browser smoke в opt-in профиле
`OPENROUTER + REAL Hotels API`. Использовался синтетический запрос на Казань,
одну комнату и двух взрослых. Автоматические повторы не выполнялись.

Проверено:

- initial confirmation появился без `hotelSearchId` и без карточек;
- после отдельного «Да» backend сохранил пул из 20 предложений, demo shell
  показала первые 5;
- одна refinement-реплика задала максимальную цену 80 000 RUB, звёзды 4–5,
  минимальный рейтинг 8 и обязательную бесплатную отмену;
- повторный confirmation содержал полный core criteria и все четыре условия;
- до второго «Да» provider search не повторялся;
- после второго «Да» создан новый opaque search reference, получен новый пул
  из 20 предложений и показаны первые 5;
- предыдущий search остался доступен и вернул сохранённые 20 предложений;
- demo shell показала применённые условия без provider DTO или внутренних
  идентификаторов.

В отчёт не переносились secret values, raw LLM/provider responses, названия
отелей, response headers или opaque identifiers.

## Закрытые вопросы

- Четыре поддерживаемых фильтра работают в полном chat-first flow.
- Каждое изменение provider request требует нового явного подтверждения.
- Успешный refinement создаёт новый `hotelSearchId`, не изменяя предыдущий.
- Пустая выдача и provider failure имеют разные typed lifecycle outcomes.
- Совет ослабить условие не применяется автоматически и не запускает поиск.
- Presentation-limit 5 не уменьшает backend candidate pool до 20.

## Отложенные вопросы

- Пользовательская сортировка не входит в MVP: Hotels API отклонил `sort` с
  `sorting_is_not_allowed_yet`, локальная имитация сортировки не добавлена.
- `search-filters-availability` не используется, поскольку проверенный ответ
  не дал пригодной семантики.
- Включение taxes/fees в `shownPrice` остаётся неизвестным; цена переносится
  без перерасчёта как total за период.
- Официальные server-to-server гарантии, SLA и rate limits публичных Hotels API
  не подтверждены.
- Stores остаются process-local; auth, resume и cross-device sync отсутствуют.
- OpenAPI сохраняет общий статус `not_ready`, generated clients не создаются.

Эти ограничения не блокируют локальную демонстрацию, но не позволяют заявлять
готовность к промышленному использованию.

## Проверки

- targeted `AssistantHotelRefinementIntegrationTest` — пройден;
- полный backend test suite — пройден;
- frontend tests, lint и build — пройдены;
- OpenAPI conformance tests и `check` — пройдены с ожидаемым
  `status=not_ready` и `readinessClaim=false`;
- launcher tests и `FAKE`/`REAL` preflight — пройдены;
- один REAL browser smoke — пройден;
- `git diff --check` — пройден.

## Вне этапа

Не добавлены production behavior, новый filter, sorting, pagination,
автоматический retry, автоматическое ослабление условий, новые endpoints,
auth, durable storage, SDK, product client, booking или payment.

## Итог

`PASS_STAGE_12_8_ITERATIVE_REFINEMENT_MVP_VERIFICATION`.

Stage 12 завершён в ограниченных границах демонстрационного hotel-only MVP.
Следующий этап не активирован и должен быть выбран отдельным roadmap-aligned
решением. Это завершение не означает production readiness.
