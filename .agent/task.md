# Active task

**Статус:** completed

## Goal

Добавить минимальный GitHub Actions workflow, который без secrets и REAL provider integrations запускает `./scripts/verify.sh core` с Java 17 и поддерживаемой совместимой версией Node.js.

## Acceptance criteria

- [x] Workflow запускается для pull request, push в primary branch и вручную.
- [x] Workflow использует Java 17 и поддерживаемую Node.js 22 LTS, соответствующие repository requirements.
- [x] Безопасные Gradle и npm caches настроены через официальные setup actions.
- [x] Locked OpenAPI conformance dependencies устанавливаются до единственного repository gate `./scripts/verify.sh core`.
- [x] Workflow имеет read-only repository permissions, не использует secrets и не активирует REAL provider modes.
- [x] Применимая development documentation описывает CI gate без создания нового source of truth.
- [x] Локальная syntax/behavior verification и independent read-only review завершены; findings исправлены.

## Constraints

- Сохранить действующие governance, architecture, roadmap и product scope.
- Не менять application behavior, dependencies, contracts, provider implementations или production configuration.
- Сохранить unrelated existing worktree changes.

## Out of scope

- Product feature work и изменение roadmap status.
- REAL provider execution, deployment, releases и production mutations.
- Замена существующего `scripts/verify.sh` или расширение профиля `core`.
- Полная переработка CI или добавление matrix/release/security workflows.

## Definition of Done

- Все acceptance criteria проверены отдельно.
- Выполнены применимые criteria из `docs/development/definition-of-done.md`.
- Workflow YAML синтаксически корректен; `./scripts/verify.sh docs` и `./scripts/verify.sh core` проходят.
- Final diff и отсутствие secret/REAL-provider activation проверены.
- Independent reviewer проверил итоговый diff; findings возвращены в repair loop.

## Task-specific escalation triggers

- Для CI необходим secret, платный ресурс, production access или REAL provider call.
- Требуется изменить product, public contract или architecture baseline.
- Primary branch или toolchain невозможно определить из repository evidence.
- Блокер сохраняется после нескольких существенно разных попыток.
