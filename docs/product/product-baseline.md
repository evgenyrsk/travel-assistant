# Product Baseline

**Роль:** источник истины по текущим продуктовым границам MVP v1 и продуктовым ограничениям. Исторические артефакты `docs/product/stage-*` сохраняют ход решений, но не переопределяют эту основу.

## 1. Назначение документа

Этот документ фиксирует актуальную продуктовую основу Travel Assistant после завершения Stage 0–12. Текущий статус этапов, последний завершенный шаг и следующий разрешенный шаг фиксируются только в `docs/roadmap/roadmap.md`.

Он нужен как компактная точка входа в текущее продуктовое состояние: что входит в MVP v1, что остается за его пределами, какие продуктовые границы уже подтверждены и где искать исходные stage artifacts.

Документ не заменяет исторические артефакты этапов в `docs/product/stage-*`, не переписывает требования и не добавляет новые продуктовые решения. Источником текущего статуса этапов остается `docs/roadmap/roadmap.md`.

## 2. Текущий статус продукта

- Stage 0 - завершен.
- Stage 1 - завершен.
- Stage 2 - завершен.
- Stage 3 - завершен.
- Stage 4 - завершен.
- Stage 4.1 - завершен отчетом `docs/product/stage-4/stage-4-consistency-review.md`.
- Stage 5 - завершен.
- Stage 6 - завершен как этап контрактов и проектирования.
- Stage 7 - завершен в границах ограниченной hotel-only основы; подробности закрытия и перенесенные пункты находятся в `docs/roadmap/roadmap.md`.
- Stage 8 - завершен как backend confirmation lifecycle.
- Stage 9 - завершен после opt-in интеграций Hotels API и OpenRouter, chat-first frontend и ограниченного внутреннего MVP-пилота.
- Stage 10 - завершен после подтверждения платформонезависимого HTTP-контракта и границы между backend-сервисом, локальной demo shell и будущими продуктовыми клиентами.
- Stage 11 - завершен после добавления воспроизводимого launcher и успешной локальной REAL-демонстрации chat-first hotel flow.
- Stage 12 - завершил демонстрационный MVP итеративного уточнения hotel search; подробный статус находится только в roadmap.

Stage 7 сформировал process-local hotel-only основу. Stage 8 добавил явное
подтверждение до поиска. Stage 9 добавил opt-in adapters для Hotels API и
OpenRouter, накопление hotel constraints и chat-first frontend. Оба provider
mode по умолчанию остаются `FAKE`, stores остаются process-local. Завершение
внутреннего пилота не означает готовность к промышленному использованию,
наличие generated clients, booking, durable storage или auth.

Stage 10.0 первоначально выбрал устанавливаемый responsive web/PWA как первый
ограниченный cross-platform срез. Stage 10.4 уточнил роль реализации: текущий
web/PWA является только локальной demo shell MVP, а не будущим продуктовым
клиентом. Web, Android, iOS и другие продуктовые интерфейсы будут создаваться
отдельными командами и интегрироваться с Travel Assistant через HTTP API.

После Stage 9.22 основной frontend-сценарий соответствует chat-first модели:
пользователь начинает с естественного сообщения, Assistant извлекает известные
параметры и задает уточняющие вопросы, а структурированная область результатов
дополняет чат. Форма Stage 7.51 сохранена отдельно только для диагностики API.

Stage 11.0 подтвердил локальную демонстрационную готовность: после явного
confirmation backend получил пул из 20 REAL provider offers, а demo shell
показала первые 5. Stage 12 завершил этот срез: пользователь может уточнить
четыре необязательных preference, увидеть полный повторный confirmation и
запустить новый provider search, сохранив предыдущий результат. Для пустой
выдачи доступен один необязательный совет без автоматического ослабления
условий. Это не означает production readiness или готовность будущих
продуктовых клиентов.

## 3. Границы MVP v1

MVP v1 - это hotel-only travel assistant.

Актуальный MVP включает:

- AI-assisted hotel search and selection;
- естественный пользовательский запрос на подбор отеля;
- уточнение недостающих параметров перед поиском;
- provider-backed hotel facts, когда provider layer возвращает данные;
- первичный поиск без требования заранее заполнить необязательные фильтры;
- итеративное уточнение в чате максимальной общей стоимости, звезд,
  минимального гостевого рейтинга по дискретным порогам `5`, `6`, `7`, `8`,
  `9` и обязательной бесплатной отмены;
- существующее рекомендуемое ранжирование; пользовательские сортировки сначала
  дешевле или сначала с лучшим рейтингом отложены, поскольку Hotels API runtime
  отклоняет поле `sort`;
- повторное подтверждение полного набора критериев перед каждым новым provider
  search;
- ранжирование и краткое объяснение полученных hotel options;
- явное разделение provider facts, user-provided constraints, assistant assumptions и unknown data.

MVP v1 не является полным планировщиком поездки, booking flow или аккаунтной историей путешествий.

## 4. Явно вне границ MVP v1

В MVP v1 явно не входят:

- flights;
- combined itinerary;
- combined hotel + flight search;
- booking;
- payment;
- account history;
- loyalty/profile system;
- account-level saved trips;
- cross-device sync как обязательная функция;
- production integrations за пределами явно запланированных provider abstractions;
- любые будущие возможности, не активированные отдельной задачей roadmap.

Flight search остается следующим расширением после hotel flow. Combined itinerary возвращается только после появления flight flow и отдельного product decision.

## 5. Пользовательский и бизнес-контекст

Travel Assistant помогает пользователю перейти от естественного, неполного или противоречивого запроса к понятному выбору отеля.

Основные пользователи MVP-контекста:

- casual travellers, которым нужно быстрее выбрать отель;
- budget travellers, которым важны цена и trade-offs;
- базовые business travellers, которым важны удобство, локация и понятные ограничения;
- пары, семьи и небольшие группы, которым нужно согласовать требования.

Проблема продукта: обычный поиск требует заранее заполнить форму и знать точные фильтры, а реальный запрос часто начинается словами вроде "недорого у моря", "удобно с ребенком", "ближе к центру" или "без переплаты".

AI важен не как источник фактов, а как слой понимания: он помогает распознать intent, уточнить параметры, объяснить trade-offs, сравнить варианты и сохранить прозрачность reasoning.

## 6. Основной продуктовый flow

Основной MVP flow:

1. Пользователь формулирует hotel request на естественном языке.
2. Ассистент определяет intent и извлекает известные параметры.
3. Если не хватает required fields, ассистент задает короткое уточнение.
4. Когда обязательных данных достаточно, ассистент показывает полный набор
   критериев и запрашивает подтверждение.
5. После подтверждения application/provider layer выполняет один ограниченный
   поиск и получает hotel facts.
6. Пользователь видит hotel options, rationale, ограничения и unknown data.
7. Следующая реплика может установить, изменить или снять несколько явно
   названных необязательных предпочтений.
8. Ассистент снова показывает полный обновленный набор критериев и не вызывает
   provider до нового подтверждения.
9. После подтверждения выполняется новый provider search и создается новый
   `hotelSearchId`; предыдущий поиск остается доступен в process-local storage.

Первичный provider-backed поиск получает один ограниченный пул до 20 уникальных
кандидатов. Ассистентская подача 2-5 вариантов является отдельной задачей
presentation-слоя. Автоматическая пагинация и сценарий «показать еще» не входят
в текущий MVP flow и требуют отдельного продуктового решения.

Assumptions и unknowns должны оставаться видимыми. Provider facts нельзя заменять уверенными догадками ассистента.

Stage 7 реализовал техническую основу потока. Stage 8 и Stage 9 добавили
confirmation lifecycle, opt-in provider runtime, накопление hotel constraints и
chat-first frontend. Ограниченный внутренний MVP-пилот завершен; production
hardening остается отдельным будущим решением.

## 7. Продуктовые границы и guardrails

Ключевые границы продукта:

- LLM не создает provider facts.
- Provider facts должны приходить от provider layer/source data.
- Assistant assumptions должны быть явно обозначены.
- Unknown data не нужно превращать в уверенные факты.
- Будущие функции не входят в текущие границы.
- `shownPrice` отображается без перерасчета как provider total за выбранный
  период; включение taxes/fees не утверждается без provider contract.
- Бюджет без явно указанной валюты трактуется как RUB; автоматическая
  конвертация валют не выполняется.
- Фильтры применяются через новый provider search после подтверждения, а
  не только к сохраненному пулу предложений.
- Успешный поиск без предложений сохраняется отдельно от provider failure.
  Ассистент может предложить снять одно активное условие, но не меняет его и не
  запускает новый поиск без явной реплики пользователя и подтверждения.
- Порядок предложения ослабления: минимальный гостевой рейтинг, звёзды,
  бесплатная отмена, максимальная общая стоимость. Бюджет предлагается снять
  последним.
- Неизвестный или неподдерживаемый фильтр требует уточнения, а не догадки.
- Минимальный гостевой рейтинг как условие поиска принимает только пороги `5`,
  `6`, `7`, `8`, `9`. Другие числа и ненумерические оценки требуют уточнения;
  автоматическое округление запрещено.
- Дискретность filter input не изменяет provider fact: фактический рейтинг
  предложения сохраняется без округления, если provider его вернул.
- Статус `LIMITED` нельзя выводить из количества комнат или другого
  эвристического threshold без подтвержденного provider fact.
- Локальная demo shell остается online-only и не обещает resume или
  cross-device sync; transcript и provider results не становятся durable data.

Подробные roadmap guardrails остаются в `docs/roadmap/roadmap.md` и `docs/guides/documentation-style-guide.md`.

## 8. Связь со stage artifacts

Stage 0-4.1 documents сохраняются как historical stage artifacts и audit trail. Они важны для понимания эволюции продукта, но не все их ранние формулировки являются active MVP v1 baseline.

Если старый исследовательский контекст шире текущего MVP boundary, приоритет имеют:

1. явный запрос текущей задачи;
2. `docs/roadmap/roadmap.md` для статусов и progression;
3. этот product baseline для compact product state;
4. Stage 3/4 summary and carryover documents для UX/product details;
5. historical stage artifacts для traceability.

Ключевые product artifacts:

- `docs/product/README.md` - индекс product-документов.
- `docs/product/stage-0/product-framing.md` - исходная продуктовая рамка.
- `docs/product/stage-0/mvp-boundaries.md` - ранняя рамка MVP, сохраненная как historical traceability.
- `docs/product/stage-1/stage-1-summary.md` - итог Stage 1 по аудитории, scenarios, requirements и open questions.
- `docs/product/stage-2/stage-2-summary.md` - итог Stage 2 по use cases, edge cases, behaviour rules и data requirements.
- `docs/product/stage-2/assistant-behaviour-rules.md` - правила поведения ассистента на продуктовом уровне.
- `docs/product/stage-2/data-requirements.md` - продуктовые требования к данным без API/DB schema.
- `docs/product/stage-3/screen-map.md` - screen map и navigation model для hotel-only MVP v1.
- `docs/product/stage-3/required-fields-and-acceptance-criteria.md` - required fields и acceptance criteria для hotel search flow.
- `docs/product/stage-3/mvp-search-flow-details.md` - подробный hotel search flow.
- `docs/product/stage-3/stage-3-summary-and-carryover.md` - основной UX/product baseline Stage 3.
- `docs/product/stage-3/stage-3-hotel-only-consistency-review.md` - review hotel-only refocus.
- `docs/product/stage-4/visual-design-direction.md` - visual/UX direction для Stage 4.
- `docs/product/stage-4/interaction-patterns.md` - interaction patterns для AI-assisted hotel search.
- `docs/product/stage-4/stage-4-summary-and-carryover.md` - итог Stage 4.
- `docs/product/stage-4/stage-4-consistency-review.md` - Stage 4.1 consistency review.

## 9. Принятые policies и внешние ограничения

Stage 10.0–10.4 закрыли вопросы, для которых достаточно внутреннего product
decision:

- текущий responsive web/PWA используется только как локальная demo shell;
- будущие web, Android, iOS и другие продуктовые клиенты принадлежат отдельным
  командам и используют общий HTTP API сервиса;
- `shownPrice` переносится и показывается без перерасчета как total за период,
  при этом taxes/fees inclusion остается неизвестным;
- `LIMITED` не производится эвристически;
- process-local/browser-local состояние допустимо для локального demo-среза без
  обещаний resume, account history и cross-device sync.

Stage 12.1a закрепил отдельную продуктовую политику для `review_rating`:
пользователь может выбрать минимальный порог `5`, `6`, `7`, `8` или `9`.
Значения вроде `8.5`, `10` или «высокий рейтинг» не преобразуются автоматически
и требуют короткого уточнения. Снятие ограничения удаляет preference полностью.

Stage 12.1b подтвердил runtime-ограничение сортировки: Hotels API вернул
`sorting_is_not_allowed_yet`. Текущий MVP не имитирует глобальную сортировку на
ограниченном пуле из 20 предложений. Пользовательские sort preferences отложены,
а существующее рекомендуемое ранжирование остается default.

Внешние ограничения не заменяются предположениями. Они не блокируют локальную
демонстрацию MVP, но должны быть закрыты до публичного rollout:

- официальный server-to-server статус, долгосрочная стабильность, SLA и rate limits публичных Hotels endpoints;
- дальнейшее отображение source/freshness и partial provider data;
- расширенная accessibility-проверка перед внешним пользовательским запуском;
- production security, observability и deployment boundaries.

Hotel details, current-session shortlist и отдельный интерактивный
comparison flow остаются направлениями hotel-only расширения. Они не являются
обязательными для текущего демонстрационного MVP и не блокируют итеративное
уточнение поиска. Booking, payment и cancellation execution остаются отдельным
будущим scope.

Ограничения не являются активным списком задач. Любой следующий шаг выбирается
отдельной задачей и остается согласованным с основным roadmap.

## 10. Связанные документы

- `docs/roadmap/roadmap.md` - primary roadmap и source of truth по статусам этапов.
- `docs/product/README.md` - индекс product-документов.
- `docs/guides/documentation-style-guide.md` - правила языка, структуры и безопасного documentation refactoring.
- `docs/reviews/documentation-refactoring-plan.md` - план controlled documentation refactoring.
- `docs/product/stage-0/product-framing.md` - исходная продуктовая рамка.
- `docs/product/stage-0/mvp-boundaries.md` - historical MVP boundary artifact.
- `docs/product/stage-1/stage-1-summary.md` - Stage 1 summary.
- `docs/product/stage-2/stage-2-summary.md` - Stage 2 summary.
- `docs/product/stage-3/stage-3-summary-and-carryover.md` - Hotel-Only MVP v1 UX baseline.
- `docs/product/stage-4/stage-4-summary-and-carryover.md` - Stage 4 visual/UX summary.
- `docs/product/stage-4/stage-4-consistency-review.md` - Stage 4.1 consistency review.
