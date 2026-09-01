# Development Governance

Эта директория содержит активные инженерные правила для задач реализации, проверки, тестирования, документации и сопровождения репозитория.

Эти документы не задают статус roadmap или продуктовые границы. Для статуса используй `../roadmap/roadmap.md`, для границ MVP — `../product/product-baseline.md`, для архитектурной основы и принятого стека backend — `../architecture/architecture-baseline.md`.

## Активные правила

- [Coding standards](coding-standards.md) - общие правила написания кода, независимые от конкретной технологии.
- [Kotlin backend style guide](kotlin-backend-style-guide.md) - стиль реализации backend на Kotlin + Ktor.
- [Testing strategy](testing-strategy.md) - ожидания по тестам и правила покрытия.
- [Documentation guidelines](documentation-guidelines.md) - правила источников истины, навигации и языка документации.
- [Definition of Done](definition-of-done.md) - критерии завершения задач в репозитории.
- [Quality gates](quality-gates.md) - команды проверки, проверка diff и отчетность о рисках.
- [Autonomous engineering](autonomous-engineering.md) - политика автономности, устойчивое состояние длительных задач, recovery loop, независимое review и границы harness adapters.
- [Backend layering rules](../architecture/backend-layering-rules.md) - допустимые зависимости и границы слоев.
- [Codex task template](../prompts/codex-task-template.md) - шаблон запроса для задач реализации.
- [Codex review template](../prompts/codex-review-template.md) - шаблон запроса для задач только на проверку.

## Справочный контекст разработки

- [Development roadmap reference](roadmap.md) - справочная терминология будущих направлений, не активный список задач.
- [Implementation strategy reference](implementation-strategy.md) - справочный подход к будущей реализации, не активация задачи.

Если активное инженерное правило конфликтует с `AGENTS.md`, основным roadmap, принятым ADR или продуктовыми/архитектурными основами, следуй источнику с более высоким приоритетом и сообщи о конфликте.
