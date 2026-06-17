# Шаблон задачи на ревью

Используй этот шаблон, когда просишь Codex/opencode проверить изменение, PR или patch.

Canonical repository governance находится в `AGENTS.md`. Этот шаблон задает review-specific focus areas и не заменяет глобальные правила `AGENTS.md`.

## Контекст

- Изменение или PR для ревью:
- Релевантный этап roadmap:
- Релевантная веха:
- Документы, которые нужно прочитать перед началом:
- Canonical repository governance: `AGENTS.md`
- Primary roadmap и source of truth по статусам: `docs/roadmap/roadmap.md`
- Product baseline, если применимо: `docs/product/product-baseline.md`
- Architecture baseline, если применимо: `docs/architecture/architecture-baseline.md`
- Documentation style guide, если review касается документации: `docs/guides/documentation-style-guide.md`
- Document role для каждого затронутого документа: source-of-truth, navigation/index, guide/rules, review/audit artifact или historical artifact.
- ADR, которые нужно проверить, если есть:

## Фокус ревью

Проверить:

- отклонение от roadmap;
- разрастание границ задачи;
- согласованность с ADR;
- случайную реализацию будущих этапов;
- изменения unrelated files;
- сломанные или устаревшие ссылки в документации;
- несоответствие README/index-документов;
- неясную роль документа или смешение source-of-truth, navigation, guide/rules и audit trail;
- обычную английскую prose в Russian-first active documentation без технической необходимости;
- длинные status paragraphs там, где нужна таблица, status matrix или checklist;
- checklist items, которые нельзя проверить;
- конфликт между задачей, roadmap и ADR;
- превращение future/reference documents в active implementation backlog;
- превращение ADR candidates или decision inventory в accepted ADR;
- создание нового source-of-truth документа, когда можно обновить существующий source-of-truth;
- beautification без проверяемой цели;
- конфликт backend implementation с подтвержденным stack Kotlin + Ktor;
- попытку продолжить Java/Spring Boot backend без явного ADR и согласованной с roadmap задачи;
- изменения публичных контрактов без документации;
- захардкоженные детали провайдеров, учетные данные или допущения о внешних API;
- отсутствующие шаги проверки.

Если задача только на ревью, фиксируй findings и риски, а не выполняй исправления самостоятельно.

## Обязательные проверки

Не дублируй полный global governance из `AGENTS.md`. Используй список ниже как review-specific checklist.

### Отклонение от roadmap

- Переопределяет ли изменение roadmap?
- Меняет ли оно порядок этапов?
- Начинает ли оно следующий этап без явного запроса?
- Соответствует ли изменение текущему этапу roadmap и вехе?
- Не трактует ли оно `docs/development/*` как active implementation backlog?
- Не подменяет ли `docs/roadmap/roadmap.md` документом `docs/ROADMAP.md` или development roadmap?

### Разрастание границ задачи

- Реализует ли изменение работу за пределами задачи?
- Были ли рекомендации выполнены вместо того, чтобы быть только задокументированными?
- Были ли изменены unrelated files?
- Добавлены ли новые файлы, директории, зависимости или tooling без явного запроса?
- Смешаны ли governance cleanup, roadmap refactor, language normalization и historical labeling без явного combined scope?

### Согласованность с ADR

- Соблюдены ли существующие ADR?
- Требует ли изменение нового ADR или обновления существующего?
- Есть ли конфликт между задачей, roadmap и ADR?
- Не превращены ли ADR candidates, drafts или decision inventory в accepted ADR?

### Ссылки в документации

- Корректны ли ссылки на README, roadmap, prompt и релевантные документы?
- Явно ли отмечены отсутствующие referenced docs?
- Соответствуют ли README и индексные документы фактической структуре и рабочему процессу?
- Не дублируют ли README или `docs/ROADMAP.md` подробный roadmap/status вместо ссылки на `docs/roadmap/roadmap.md`?
- Использованы ли checklist/table formats для scope/gate/status-heavy sections там, где это повышает читаемость?
- Сохранены ли historical artifacts как audit trail без ретроспективной language/style normalization?

### Реализация будущих этапов

- Добавляет ли изменение реальные интеграции с travel API слишком рано?
- Использует ли backend implementation подтвержденный stack Kotlin + Ktor?
- Не продолжает ли изменение Java/Spring Boot skeleton без явного ADR и согласованной с roadmap задачи?
- Не игнорирует ли изменение архитектурное расхождение между файлами реализации и current architecture baseline?
- Вносит ли оно поведение LLM, специфичное для провайдера, в доменную логику?
- Добавляет ли оно infrastructure до соответствующего этапа roadmap?
- Создает ли backend/frontend skeleton, если задача относится к продуктовой или аналитической проработке?
- Использует ли технические ориентиры как разрешение расширить границы задачи?
- Создает ли изменение API/OpenAPI contracts, endpoint specs, DB schema/storage model, auth/security/DevOps/testing backlog или production code до явной roadmap activation?

## Формат финального отчета

Если задача не задает другой формат, используй обязательный формат из `AGENTS.md`. Для review-only задач findings должны идти первыми, если это требуется средой выполнения Codex.

1. Созданные файлы
2. Изменённые файлы
3. Краткое описание изменений
4. Принятые решения
5. Открытые вопросы
6. Рекомендации, не выполнены

Для documentation tasks также укажи:

- роль каждого измененного документа;
- причину изменения каждого файла;
- что явно осталось out of scope;
- были ли затронуты source-of-truth documents и почему это допустимо.
