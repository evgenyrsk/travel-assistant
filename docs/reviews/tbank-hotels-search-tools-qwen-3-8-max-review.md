# Qwen 3.8 Max review Hotels search tools 0.15.0–0.15.4

**Роль:** completed review/follow-up report для experimental Hotels MCP.
Документ фиксирует состояние working tree на момент проверки и не разрешает
booking/payment mutations.

## Scope

Проверены semantic breakfast journey, строгие low-level search filters,
поведение при provider failure, ranking/selection scope, fake-transport tests и
готовность к двум bounded read-only smoke-кейсам. Banking logic, реальные
мутации, remote transport и core Kotlin backend не менялись и не проверялись.

## Исходный дефект

Natural-language запрос с обязательным завтраком приводил к unfiltered
`plan_stay`, каталогу фильтров, многократному угадыванию low-level payload,
нескольким provider `500` и сравнению отелей без завтрака как будто условие
выполнено.

## Реализованное исправление

- `plan_stay` принимает `breakfastIncluded=true` и до pagination/cache/journey
  формирует `meal_types=breakfast` в подтверждённой discriminator-форме;
- `requiredConditions` и `conditionsApplied` сохраняются в plan/get/compare и
  при `repeat_stay_plan`;
- четыре OpenAPI filter shapes (`array`, `boolean`, `radio`, `range`) отражены
  в JSON Schema и runtime-валидации;
- malformed filters отклоняются до HTTP;
- обязательное условие не ослабляется после error или пустого результата;
- provider `4xx` и `5xx` получают разные безопасные reason, без raw body;
- documentation запрещает filter-discovery и low-level перебор для semantic
  breakfast flow.

## Qwen review

Модель: `bailian-token-plan-personal/qwen3.8-max`, thinking enabled через
OpenCode. Review выполнялся без Hotels/Banking MCP, provider-сети и чтения
секретов.

Первичный verdict: **READY** для двух bounded read-only smoke-кейсов; findings
`P0–P2` отсутствуют. Были найдены:

| Finding | Категория | Follow-up |
| --- | --- | --- |
| Нет regression test для `no_matching_stays` | P3 test gap | Закрыт: один fake provider call, запрет ослабления условия |
| Breakfast requirement не проверялся в repeat flow | P3 test gap | Закрыт: exact filter и condition metadata проверяются на новых датах |
| `4xx`/`5xx` имели общий reason | P3 usability | Закрыт: `provider_rejected_required_request` и `provider_unavailable` |
| Строка `breakfast` отсутствует как enum evidence в локальном OpenAPI | Contract gap | Не маскируется; проверяется bounded live smoke, fail-closed сохраняется |
| MCP строже nullable/date-time частей OpenAPI | Contract gap | Оставлено намеренно; date-only уже принимался provider, null не нужен journey |

Повторный Qwen follow-up подтвердил закрытие трёх P3 и снова дал
**READY**. Новых `P0–P2` не обнаружено.

Дополнительный review-only аудит 0.15.0 также подтвердил отсутствие `P0–P2` и
дал `READY` для bounded live smoke. Его безопасные `P3` закрыты в `0.15.1`:

| Finding | Follow-up 0.15.1 |
| --- | --- |
| `401/403` не отличались от обычного отказа required filter | Добавлен `provider_auth_rejected` с указанием восстановить auth profile вне model conversation; retry/fallback остаются запрещены |
| Не проверялись conditions в `get_stay_options` и network fail-closed | Добавлены точечные fake-transport assertions |
| Low-level filtered failures имеют structured-success форму | Семантика явно закреплена в tool descriptions и README; клиент проверяет `status` |
| `rates.filters` не имеют локального OpenAPI evidence | Поле явно помечено как неподтверждённый untyped pass-through; агент должен опускать его без точного контракта |

Строгая типизация `rates.filters` намеренно не добавлялась: перенос search filter
schema на другой endpoint без evidence создал бы выдуманный контракт.

## Natural-language live smoke и follow-up 0.15.2–0.15.3

После восстановления `searchReady=true` пользователь передал обычный запрос без
названий tools и технических параметров: найти в Санкт-Петербурге пять лучших
отелей для двух взрослых с обязательным завтраком и ничего не бронировать.
Orchestration прошёл ровно через `plan_stay(breakfastIncluded=true)` и
`compare_stay_options`; low-level filter tools, retries и writes не вызывались.

Ответ модели связал завтрак с показанными ценами, но в итоговой таблице не
показал доказательство `mealName` для каждого варианта. Чтобы исключить
неоднозначность, `0.15.2` возвращает для search-feed option и rate явное поле
`displayedPriceBreakfastEvidence`:

- `confirmed_by_meal_name` — `mealName` явно подтверждает breakfast;
- `not_confirmed_for_displayed_price` — MCP не связывает завтрак с показанной
  ценой; это не равнозначно утверждению, что завтрака нет.

Классификация консервативна и проверена fake transport assertions. Повторный
breakfast smoke `0.15.2` и control search затем прошли отдельными bounded
read-only кейсами.

Control smoke без обязательного завтрака также прошёл через два journey tools
без retries и writes. Он выявил два presentation gaps: итоговая таблица скрыла
цену, рейтинг, отзывы и отмену, а вывод добавил отель вне возвращённого top-5.
Кроме того, бинарного meal evidence недостаточно для различения явного
исключения питания и отсутствия доказательства.

Follow-up `0.15.3`:

- добавил `excluded_by_meal_name` к `confirmed_by_meal_name` и
  `not_confirmed_for_displayed_price`;
- добавил `presentationGuidance` с обязательными сравнительными полями;
- ограничил пользовательский вывод элементами `comparison`, если пользователь
  явно не запросил альтернативы;
- сохранил консервативную трактовку неизвестных `mealName`.

Повторный control smoke сохранил правильный top-5 scope, но модель снова не
показала цену и отмену: сырой `price` является вложенным provider fact.
Follow-up `0.15.4` добавил совместимый плоский массив `comparisonRows` с
`priceAmount`, `priceCurrency`, локацией, рейтингом, отзывами, отменой и meal
evidence. `presentationGuidance` теперь прямо указывает использовать этот массив
для пользовательской таблицы; исходный `comparison` сохранён без breaking
change.

## Checks

| Проверка | Результат |
| --- | --- |
| `node --check tools/tbank-hotels-mcp/src/server.mjs` | Passed |
| `npm test` из `tools/tbank-hotels-mcp` | 47 passed, 0 failed; fake transport only |
| `git diff --check` | Passed |
| Version alignment | `package.json`, `SERVER_VERSION`, tests и active docs: `0.15.4` |

## Live smoke preflight

Новый OpenCode-процесс поднял Hotels MCP `0.15.0`, но вернул
`searchReady=false`: `transport=not_configured`,
`authentication=not_configured`. Mobile broker customer reads оставались
готовыми, однако search profile из пользовательского терминала не был
унаследован. Provider search не выполнялся.

Следовательно, два live-кейса остаются pending до перезапуска OpenCode в
окружении с уже настроенными `TBANK_HOTELS_API_BASE_URL` и search auth profile.
Нельзя копировать секреты в prompt или читать их из локального credential
storage ради smoke.

Разрешённые сценарии после readiness:

1. Санкт-Петербург, 15–16.09.2026, 2 взрослых,
   `breakfastIncluded=true`, затем compare top-5.
2. Москва, те же даты и состав, без breakfast condition, затем compare top-5.

Для каждого допускаются только `plan_stay` и `compare_stay_options`; при
`requirements_unavailable`, provider error или попытке low-level filter tool
выполнение немедленно останавливается. Booking/payment tools запрещены.

## Verdict

Локальный implementation/review gate пройден. Live evidence не заявляется:
preflight честно заблокирован отсутствующим search transport/auth в новом
OpenCode-процессе.
