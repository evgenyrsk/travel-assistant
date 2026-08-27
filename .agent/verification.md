# Verification

| Check | Required | Latest result |
|---|---|---|
| Swagger JSON parse | yes | passed for both supplied files |
| Local Swagger refs resolve | yes | passed for both supplied files |
| Targeted Hotels protocol tests | yes | 60/60 passed, including anonymous search without Authorization |
| Banking tests if handoff changes | conditional | 52/52 passed, including runtime/package metadata consistency |
| Toolkit manifests/conformance | yes | 17/17, manifests match, both MCP conformance passed |
| Full offline release gate | yes | 17 toolkit + 60 Hotels + 52 Banking passed outside restricted sandbox; 0 skipped, 0 provider requests |
| Portable toolkit artifact | yes | exact allowlist, npm install outside checkout and client config passed |
| Portable Banking login | yes | wheel contains entry point; installed logout passed outside checkout |
| Documentation gate | yes | `./scripts/verify.sh docs` passed |
| Repository-wide gate | yes | `./scripts/verify.sh all` passed outside restricted sandbox |
| `git diff --check` | yes | passed |
| npm publish dry-run | yes | Hotels 0.28.0 and toolkit 0.12.0 passed; 8 allowlisted files each |
| Provider requests | must be 0 | 0; verify и docs gate полностью offline |
