# Stage 14.1c — компактные карточки результатов demo shell

## Цель

Сделать локальную выдачу hotel offers компактной и читаемой, не меняя
backend ranking, provider flow или функциональные границы MVP.

## Изменения

- desktop использует горизонтальную карточку с media-секцией слева;
- mobile использует вертикальную карточку с изображением сверху;
- первый optional `imageUrl` отображается с `loading=lazy`, `decoding=async` и
  `referrerpolicy=no-referrer`;
- отсутствующий, небезопасный или не загрузившийся image заменяется нейтральным
  CSS-placeholder;
- `starRating` отображается только в диапазоне `1..5`;
- повторяющийся `matchSummary` удалён из карточек, но сохранён в API;
- правило backend-ранжирования показано один раз над всем списком;
- existing details раскрывается внутри выбранной карточки и не создаёт N+1.

## Browser QA

- 1440×900: горизонтальные карточки, два столбца media/content;
- 768×1024: горизонтальный layout сохранён без overflow;
- 390×844 и 320×568: один вертикальный столбец без overflow;
- основная кнопка «Подробнее» имеет высоту 44 CSS px;
- keyboard activation открывает только выбранный details, переносит focus
  внутрь него и выполняет ровно один details request;
- общее объяснение ранжирования присутствует один раз, per-card summary
  отсутствует.

## REAL smoke

Один разрешённый smoke выполнен без retry:

- естественный запрос вернул понятный confirmation без поиска;
- отдельное «Да» создало один search с 20 offers, demo shell показала пять;
- ни один offer не содержал public `imageUrl`, поэтому все карточки показали
  корректный placeholder;
- один явный выбор выполнил ровно один details request;
- details загрузился, запрещённые `ИНН`, `ОГРН`, `КПП`, registry и owner data в
  DOM отсутствовали;
- выбранный details также не содержал изображений.

Таким образом, REAL flow и fallback подтверждены, но live image branch не
подтверждён. Дополнительный provider request не выполнялся согласно no-retry
границе задачи.

## Границы

- public API и backend ranking не менялись;
- image proxy, cache, preloading и N+1 не добавлены;
- rates, deeplink, comparison, booking, auth, storage, CORS и deployment не
  входят в этап;
- demo shell остаётся локальной демонстрационной оболочкой, а не продуктовым
  web-клиентом.

## Verdict

`BLOCKED_STAGE_14_1C_LIVE_IMAGE_EVIDENCE`.

Production/frontend implementation и локальные gates готовы, но Stage 14.1c
не закрывается без отдельного решения: принять optional-image fallback как
достаточное evidence либо активировать ограниченный contract-reconciliation
для источника изображений. Итоговый Stage 14.0 остаётся `demo-ready MVP`, но не
production readiness.
