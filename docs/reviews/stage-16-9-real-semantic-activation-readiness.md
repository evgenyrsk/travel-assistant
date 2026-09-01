# Stage 16.9 — REAL semantic activation readiness

## Статус

`BLOCKED` для REAL activation. Configuration readiness реализуется без
provider content и без semantic model calls. Controlled probe, evaluation и
rollout требуют отдельных разрешений после закрытия rights gate.

## Scope

- [x] OpenRouter сохранён как provider-neutral gateway существующего adapter.
- [x] Model и provider endpoint разделены в явной конфигурации без defaults.
- [x] Exact endpoint передаётся singleton `provider.only`; fallback запрещён.
- [x] Сохранены `require_parameters=true`, `data_collection=deny`, `zdr=true`.
- [x] Зафиксированы EU-only shortlist, порядок проверки и правило выбора.
- [x] Определены rights checklist и спецификация evaluation dataset.

## Out of scope

- REAL semantic/model call и передача provider content;
- controlled probe, model bake-off и quality evaluation;
- включение `OPENROUTER` в local demo launcher или изменение `FAKE` default;
- taxonomy, ranking, provider mapping, public API/OpenAPI и frontend contract;
- durable storage, deployment manifests, booking, payment и Stage 16 expansion.

## Runtime safety

`ACCOMMODATION_ANALYSIS_MODE=OPENROUTER` требует одновременно:

- `ACCOMMODATION_ANALYSIS_EXTERNAL_CONTENT_APPROVED=true`;
- явный immutable `ACCOMMODATION_ANALYSIS_MODEL`;
- exact `ACCOMMODATION_ANALYSIS_PROVIDER_ENDPOINT`;
- exact-host `ACCOMMODATION_ANALYSIS_IMAGE_HOSTS`;
- существующий `OPENROUTER_API_KEY`.

Endpoint допускает только lowercase segments без wildcard и передаётся одним
элементом `provider.only`. Request содержит `allow_fallbacks=false`, поэтому
OpenRouter не может автоматически сменить downstream provider. Автоматический
retry в adapter отсутствует. Правила соответствуют
[OpenRouter provider routing](https://openrouter.ai/docs/guides/routing/provider-selection).

## EU-only shortlist

Публичные OpenRouter metadata и ZDR inventory проверены 28 июля 2026 года без
model calls. Они подтверждают image input, `response_format`/structured outputs
и EU/ZDR endpoint как предварительную совместимость, но не заменяют controlled
multiple-image probe.

| Порядок | Immutable model | Exact endpoint | Роль |
|---:|---|---|---|
| 1 | `openai/gpt-5.6-luna-20260709` | `azure/eu` | основной balanced candidate |
| 2 | `google/gemini-2.5-flash` | `google-vertex/eu` | cost/latency baseline |
| 3 | `google/gemini-2.5-pro` | `google-vertex/eu` | quality baseline |

Multiple images поддерживаются общим OpenRouter wire format, но фактический
лимит зависит от model/provider endpoint. Совместимость strict schema и трёх
image inputs проверяется отдельно согласно документации по
[image inputs](https://openrouter.ai/docs/guides/overview/multimodal/image-understanding),
[structured outputs](https://openrouter.ai/docs/guides/features/structured-outputs)
и [ZDR](https://openrouter.ai/docs/guides/features/zdr).

## Rights gate

- [ ] Получено письменное разрешение владельца/provider content на передачу
      descriptions, amenities и images внешнему processor.
- [ ] В разрешении явно указаны OpenRouter и выбранный downstream model/provider
      endpoint.
- [ ] Подтверждены EU processing region, retention, logging, training и ZDR
      conditions конкретного endpoint.
- [ ] Согласованы внутренние dataset location, access, retention и deletion
      rules.
- [ ] Exact image host `extranet-cdn.tinkoff.ru` письменно разрешён для external
      forwarding и только после этого добавлен в runtime allowlist.
- [ ] Выдано отдельное разрешение на три controlled probes без retry/fallback.

Технический flag `ACCOMMODATION_ANALYSIS_EXTERNAL_CONTENT_APPROVED=true` не
является юридическим подтверждением и не закрывает checklist.

## Evaluation gate

Dataset создаётся до просмотра outputs и хранится вне repository в одобренном
контуре. Минимум 100 candidates из не менее чем трёх opaque destination groups
должны включать обычные отели, подтверждённые glamping-объекты и borderline
cases. Borderline subset размечают два reviewer независимо.

Каждая model/endpoint pair сначала получает один probe с русским текстом,
strict schema и тремя изображениями. После успешных probes все пары оцениваются
на одном dataset. Обязательные thresholds:

- `MATCH precision >= 90%`;
- `MATCH + PROBABLE precision >= 80%`;
- recall `>= 70%`;
- false-positive rate обычных отелей `<= 5%`.

Сначала исключаются пары, не прошедшие любой quality threshold. Среди прошедших
выбирается наименее дорогая по измеренному workload; при сопоставимой стоимости
выигрывает меньшая latency. Model и endpoint фиксируются совместно.

## Readiness verdict

Adapter готов к безопасному controlled probe после закрытия rights checklist и
отдельного разрешения. REAL activation и rollout остаются `BLOCKED`; demo
profiles продолжают использовать semantic `FAKE`.

## Проверки

- [x] Backend: `./gradlew test --rerun-tasks` с Java 17.
- [x] Frontend: 47 tests, lint и build.
- [x] Launcher: 8 tests; `--fake --check-only` и synthetic
      `--real --check-only` показали semantic `FAKE` без network calls.
- [x] Semantic evaluation tool: 4 tests.
- [x] OpenAPI conformance tool: 12 tests и read-only check со
      `status=not_ready`, `readinessClaim=false`, без blocking findings.
- [x] REAL semantic call и provider-content forwarding не выполнялись.
