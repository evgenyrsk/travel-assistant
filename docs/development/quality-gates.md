# Quality Gates

Run only checks that are relevant to the task and supported by the repository.

## Required for Documentation-Only Changes

```bash
git diff --check
```

Repository-native equivalent:

```bash
./scripts/verify.sh docs
```

The repository-native gate checks staged and unstaged diffs and scans untracked text files for trailing whitespace. Untracked files still require explicit `git status` and scope review before completion.

Also manually check:

- new or changed links point to existing files;
- README and index navigation matches the actual repository structure;
- no roadmap/status wording changed unless the task explicitly changed project status;
- historical docs were not rewritten without need.

## Backend Checks

For backend code, build, test, or behavior changes in `services/backend`, run:

```bash
cd services/backend && ./gradlew test
```

If the local environment requires an explicit JDK, use the project convention:

```bash
cd services/backend && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test
```

Run backend tests when changing:

- Kotlin source files;
- Gradle files;
- backend README commands that should match actual behavior;
- API behavior, validation, or response mapping;
- application/domain behavior;
- test fixtures or test utilities.

## Stable Cross-Module Gate

For substantial changes that span the stable backend, demo shell, local launcher, OpenAPI conformance tool, or semantic evaluation harness, run:

```bash
./scripts/verify.sh core
```

The command composes existing repository checks; it does not install dependencies, access external providers, start production-like services, or claim OpenAPI/generated-client readiness. Use targeted module commands for narrower tasks instead of running an unnecessarily broad gate.

For an explicitly repository-wide task that also changes experimental tools, run:

```bash
./scripts/verify.sh all
```

The `all` profile additionally runs checks for locally present MCP tools. It does not invoke REAL providers or credentials. Native build tooling may still need its ordinary dependency cache or request network access to resolve missing build dependencies; that access remains controlled by the active harness permissions.

## CI Gate

`.github/workflows/core-verification.yml` запускает `./scripts/verify.sh core` для pull request, push в `main` и ручного `workflow_dispatch`. Workflow использует Java 17 и поддерживаемую Node.js 22 LTS, устанавливает locked dependencies из `tools/openapi-conformance/package-lock.json` и применяет встроенные Gradle/npm caches официальных setup actions.

Workflow имеет только `contents: read`, не требует repository secrets и не задаёт REAL provider modes. Сетевой доступ используется только обычными dependency/toolchain setup steps; сам `core` profile не вызывает внешние provider integrations. Более широкий `all` profile остаётся task-specific локальной проверкой и не входит в минимальный CI gate.

## Diff Hygiene

Before finishing:

- inspect `git status --short`;
- inspect the diff for unrelated changes;
- avoid formatting unrelated sections;
- verify no generated or local-only files were added.
- inspect both staged and unstaged changes so pre-existing user work is not attributed to the task.

## Review Output Expectations

Review findings should be ordered by severity:

- Critical - breaks roadmap, architecture, security, or core behavior.
- Major - likely behavioral regression, broken contract, missing required tests, or significant boundary violation.
- Minor - local maintainability, wording, navigation, or low-risk consistency issue.
- Note - observation without required action.

Review-only tasks must not edit files unless the prompt explicitly asks for fixes.

## Risk Reporting

Final reports must state:

- checks that passed;
- checks that failed and why;
- checks not run and why;
- known residual risks;
- follow-up recommendations that were intentionally not implemented.
