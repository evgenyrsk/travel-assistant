# Stage 9.22 — chat-first frontend

## Роль документа

Это итоговый отчет Stage 9.22. Он фиксирует переход основного frontend-сценария
к диалогу и не заменяет текущий статус в
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Сделать естественное сообщение основной точкой входа hotel-only MVP, сохранить
прежнюю структурированную форму как диагностический инструмент и не давать
браузеру прямой доступ к OpenRouter или Hotels API.

## Реализованный поток

Главная страница теперь:

1. создает Assistant session первым сообщением;
2. продолжает диалог через ту же process-local session;
3. показывает `ask_clarification` и `show_boundary_message` в transcript;
4. отправляет подтверждение как обычное следующее сообщение;
5. при `show_hotel_results` загружает offers по `hotelSearchId`;
6. показывает до пяти первых предложений в порядке backend ranking.

Область результатов не очищается при следующих сообщениях. Frontend не
ранжирует предложения и не запрашивает дополнительные страницы provider.

Прежняя форма перенесена на `/diagnostic.html`. Она по-прежнему вызывает
hotel-search route напрямую и не считается основным продуктовым сценарием.

## Визуальная система

Chat-first экран и диагностическая страница используют одну адаптивную систему:

- спокойный светлый фон и контрастный зеленый акцент без внешних изображений;
- единые скругления, границы, тени и типографическая иерархия;
- визуально разделенные transcript, composer и область рекомендаций;
- крупные controls: основная кнопка высотой `48px`, навигационная ссылка — не
  менее `44px` на mobile;
- заметный `focus-visible`, адаптация до ширины `320px` и поддержка
  `prefers-reduced-motion`;
- нейтральные empty states и нумерация уже ранжированных предложений без
  изменения provider facts.

Новые frontend dependencies, web fonts и внешние visual assets не добавлены.

## Границы данных и API

- Browser обращается только к существующим `/api/v1/assistant/**` и
  `/api/v1/hotel-searches/**`.
- OpenRouter API key, Hotels API и provider DTO в frontend отсутствуют.
- Transcript и идентификатор session хранятся только в памяти страницы.
- Public API, OpenAPI и generated clients не менялись.
- Backend candidate pool остается ограниченным 20 предложениями; frontend
  показывает не более пяти без изменения их порядка.

## Проверки

| Проверка | Результат |
|---|---|
| Frontend unit tests | `11/11` пройдены |
| `npm run lint` | Пройден |
| `npm run build` | Пройден |
| Полный backend test suite | Пройден |
| Browser smoke, `FAKE LLM + FAKE Hotels` | Session создана, сообщение и clarification показаны, offer request не выполнен |
| Diagnostic navigation | `/diagnostic.html` открывается, прежняя форма доступна |
| Desktop visual QA | `1440x900`, layout и composer отображаются полностью |
| Mobile visual QA | `390x844`, horizontal overflow отсутствует |
| Размеры mobile targets | Основная кнопка `48px`, navigation target `44px` |
| Контраст ключевых text/background пар | От `4.96:1` до `16.20:1` |
| Browser errors | Не обнаружены |
| `git diff --check` | Пройден |

Автоматические тесты отдельно подтверждают `show_hotel_results`: frontend
запрашивает offers только при наличии `hotelSearchId` и ограничивает показ
пятью уже ранжированными предложениями.

## Ограничения

Default `FakeLlmClient` во время smoke вернул существующий англоязычный
тестовый текст clarification. Stage 9.22 не меняет backend copy и не скрывает
этот факт на frontend. Перед внутренним пилотом пользовательские тексты нужно
сверить отдельной ограниченной задачей, если пилот будет запускаться в режиме
`FAKE`.

В этап не входят:

- live-вызовы OpenRouter или Hotels API;
- новый ranking, pagination или polling;
- streaming, booking, payment и durable transcript storage;
- новый public contract или frontend framework.

## Verdict

`PASS_CHAT_FIRST_FRONTEND`.

Stage 9.22 завершен в заявленных границах. Следующий разрешенный этап —
Stage 9.23: внутренний пилот chat-first MVP в контролируемой среде. Пилот не
означает production readiness.
