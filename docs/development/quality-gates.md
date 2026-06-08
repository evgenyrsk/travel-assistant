# Quality Gates

Run only checks that are relevant to the task and supported by the repository.

## Required for Documentation-Only Changes

```bash
git diff --check
```

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

## Diff Hygiene

Before finishing:

- inspect `git status --short`;
- inspect the diff for unrelated changes;
- avoid formatting unrelated sections;
- verify no generated or local-only files were added.

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
