# Stage 8.0 — Проверка точки входа в AI/LLM-оркестрацию

## 1. Цель

Stage 8.0 — шаг только для проверки и планирования перед Stage 8. Он проверяет границы закрытого Stage 7, классифицирует перенесенные пункты и подтверждает первый небольшой безопасный шаг в направлении AI/LLM-оркестрации.

Этот отчет сохраняет результаты проверки. Он не заменяет `docs/roadmap/roadmap.md`, не меняет статус Stage 8 и не является планом реализации.

## 2. Текущая точка входа

- Stage 7 завершен через Stage 7.53 в границах ограниченной основы hotel-only MVP.
- Stage 8 остается запланированным и не начатым.
- Целевое направление продукта остается chat-first; форма Stage 7.51 является временной diagnostic/demo shell.
- Следующий шаг, указанный в roadmap: `Stage 8.1 — LLM Orchestration Boundary and Safety Plan`, только как отдельная planning-only задача.
- Принятые ADR для LLM-оркестрации отсутствуют.

## 3. Граница завершения Stage 7

Stage 7 завершил:

- минимальную Kotlin + Ktor backend-основу;
- process-local границы сессии/сообщений Assistant и основы уточнений;
- process-local поиск отелей через `FakeHotelOfferProvider`;
- детерминированное ранжирование и короткий `matchSummary`;
- строгую передачу от Assistant к поиску отелей;
- минимальную frontend-оболочку прямого поиска;
- ограниченные вспомогательные средства OpenAPI/conformance без заявления готовности.

Stage 7 не завершил:

- извлечение intent и параметров из естественного сообщения;
- динамический поток уточнений;
- LLM-оркестрацию;
- целевой Assistant chat UI;
- интеграцию real provider;
- durable storage, auth или production hardening.

Закрытие Stage 7 не означает готовность к промышленному использованию, завершение OpenAPI, готовность generated clients или готовность к выпуску.

## 4. Классификация carryover Stage 7

| Категория | Пункты | Решение для Stage 8.0 |
|---|---|---|
| Прямой вход в Stage 8 | Provider-independent LLM boundary, извлечение параметров из естественного запроса, уточнения, объяснения, сравнения, устойчивость к неполным и противоречивым запросам | Учесть при определении последовательности, не реализовывать |
| Защитные ограничения Stage 8 | Hotel-only scope, chat-first UX, provider facts как источник данных, разделение facts/assumptions/unknowns, process-local foundation | Сохранить как обязательные границы |
| Возможная более поздняя часть Stage 8 | Расширенный UI Assistant и переход от diagnostic form к chat-first flow | Не включать в Stage 8.0 или первый шаг реализации |
| Отдельная работа по интеграции и инструментам | Live browser-to-backend E2E, generated clients, расширение manifest, CI/Gradle gate, runtime conformance | Не включать в Stage 8.0; не считать зависимостью Stage 8.1 |
| Stage 9 | Интеграция real provider, provider-specific ошибки, повторы запросов и reliability hardening | Не включать в Stage 8 |
| Отдельные будущие решения | Durable storage, Redis/cache, auth/account flows, security, observability, deployment и production UI hardening | Не включать без отдельной roadmap-задачи |
| Вне hotel-only MVP v1 | Booking, payment, flights и combined itinerary | Не включать |

## 5. Предлагаемая последовательность Stage 8

Последовательность ниже является рекомендацией для дальнейшей декомпозиции, а не новым roadmap:

1. **Stage 8.1 — LLM Orchestration Boundary and Safety Plan.** Определить границу, допустимые данные, результат интерпретации, правила валидации, fallback и проверку через fake LLM.
2. **Первый шаг реализации после отдельной проверки.** Добавить минимальную provider-independent границу LLM-оркестрации с fake LLM и детерминированными тестами, не меняя public API без необходимости.
3. **Извлечение параметров и уточнения.** Подключить ограниченное извлечение hotel-search параметров и короткие уточнения к существующему process-local flow.
4. **Укрепление объяснений и сравнений.** Улучшить объяснения, сравнения и обработку конфликтующих ограничений, сохраняя provider facts и unknowns.
5. **Интеграция chat-first frontend.** Рассматривать только после стабилизации backend-границы оркестрации и отдельного решения о границах frontend.

Каждый пункт после Stage 8.1 требует отдельной явной задачи и проверки фактического состояния.

## 6. Первый рекомендуемый шаг Stage 8.1

Stage 8.1 должен остаться planning-only и зафиксировать:

- ответственность provider-independent `LlmClient` boundary без выбора SDK, модели или поставщика;
- минимальные входы и результаты для извлечения intent/параметров, уточнений и объяснений;
- какие данные пользователя, provider facts, assumptions и unknowns разрешено передавать в LLM;
- правила валидации ответа LLM до изменения состояния session/search или вызова provider boundary;
- условия, при которых вызов provider разрешен, запрещен или требует уточнения;
- fallback для timeout, ошибки, невалидного или небезопасного ответа;
- стратегию тестирования через fake LLM без сети и внешних вызовов;
- минимальный объем одного последующего шага реализации.

Stage 8.1 не должен создавать код, библиотеку prompt-шаблонов, OpenAPI contract, endpoint, provider adapter или ADR без отдельного основания.

## 7. Что не входит в Stage 8.0

- production code и изменения поведения приложения;
- подключение реальной LLM, SDK, выбор model, API keys или secrets;
- real travel provider и его contract;
- frontend/UI изменения;
- OpenAPI и сгенерированные файлы;
- generated clients, расширение manifest, CI/Gradle gate или live E2E;
- storage, auth, booking, payment, flights и combined itinerary;
- изменение статуса roadmap или заявление готовности к промышленному использованию.

## 8. Риски преждевременной реализации

- Привязка потока application/domain к конкретному LLM SDK до определения provider-independent boundary.
- Передача лишних пользовательских или служебных данных без правила минимизации данных.
- Принятие невалидного ответа LLM как подтвержденных ограничений пользователя или provider facts.
- Вызов provider boundary до заполнения обязательных hotel-search параметров.
- Смешение Stage 8 orchestration с интеграцией real provider из Stage 9.
- Закрепление временной формы Stage 7.51 как целевого UI.
- Расширение public API, storage или infrastructure до появления проверяемой необходимости.

## 9. Verdict

Passed — точка входа в Stage 8 согласована с roadmap и текущими baseline-документами.

Первым безопасным шагом остается отдельный planning-only `Stage 8.1 — LLM Orchestration Boundary and Safety Plan`. До его завершения реализация, реальные внешние вызовы, изменения UI/API и заявления о готовности к промышленному использованию преждевременны.
