# Decisions

## Reuse the repository-native core gate

- **Decision:** CI устанавливает только locked OpenAPI conformance dependencies и затем запускает `./scripts/verify.sh core` без дублирования внутренних команд.
- **Alternatives:** перечислить backend/frontend/tool checks прямо в workflow или создать новый CI-only script.
- **Reason:** `scripts/verify.sh` уже является repository source of truth для verification order и provider-safe profiles.
- **Consequences:** изменения состава `core` автоматически применяются локально и в CI; workflow остаётся минимальным.

## Supported fixed toolchain majors

- **Decision:** использовать Java 17 и Node.js 22 LTS.
- **Alternatives:** latest Node.js или CI matrix нескольких версий.
- **Reason:** Gradle toolchain фиксирует Java 17, Node.js packages требуют `>=20`, а Node.js 22 — минимальная поддерживаемая LTS-линия на текущую дату; Node.js 20 уже EOL.
- **Consequences:** изменение repository minimum потребует синхронно обновить workflow.

## Built-in setup action caches

- **Decision:** использовать Gradle cache в Java setup action и npm cache, привязанный к `tools/openapi-conformance/package-lock.json`, в Node setup action.
- **Alternatives:** отсутствие cache или отдельные broad `actions/cache` paths.
- **Reason:** официальные setup actions дают узкие dependency caches без сохранения workspaces, build outputs или secrets.
- **Consequences:** cache miss влияет только на скорость; correctness остаётся за locked install и verification gate.
