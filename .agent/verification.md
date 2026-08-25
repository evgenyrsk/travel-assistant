# Verification

| Check | Required | Latest result |
|---|---|---|
| Workflow YAML syntax and expected structure | yes | passed with existing `yaml` parser and assertions |
| Locked dependency install | yes | `npm ci --ignore-scripts --prefix tools/openapi-conformance` passed; 0 vulnerabilities |
| `./scripts/verify.sh docs` | yes | passed |
| `./scripts/verify.sh core` | yes | passed three times; final run after independent-review repair |
| Acceptance criteria review | yes | passed; all seven criteria checked individually |
| Final staged, unstaged, and untracked scope review | yes | passed; task changes are CI workflow, focused docs, and persistent state only |
| Secret and REAL-provider activation review | yes | passed for workflow candidate; no secret expressions or provider modes |
| Independent read-only review | yes | accepted after Node.js 20 → 22 repair |
| First GitHub-hosted `Verify core` | yes | failed in run `32822469845`; clean-checkout docs gate returned exit code 1 |
| Clean-checkout repair | yes | shell syntax, `docs`, and full local `core` passed; hosted rerun pending |

## Recovery history

- Independent review выявил EOL Node.js 20 в initial workflow. Runtime обновлён до поддерживаемой Node.js 22 LTS; documentation и persistent state синхронизированы; повторные docs, YAML и `core` checks прошли.
- Первый hosted run выявил POSIX shell edge case: при отсутствии untracked files bare `return` сохранял status `1` от `[ -n "" ]`. Root cause локально воспроизведён; применяется explicit `return 0`.

## Unresolved failures

- Hosted CI остаётся red до push repair и успешного повторного run.

## Environment note

GitHub-hosted execution будет доступен после публикации workflow в repository. Локально `core` прошёл на доступном Node.js v25.8.1; CI явно provision Node.js 22 LTS, которая удовлетворяет package constraint `>=20`.
