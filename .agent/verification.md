# Verification

| Check | Required | Latest result |
|---|---|---|
| Swagger JSON parse | yes | passed for both supplied files |
| Local Swagger refs resolve | yes | passed for both supplied files |
| Targeted Hotels protocol tests | yes | 60/60 passed, including anonymous search without Authorization |
| Banking tests if handoff changes | conditional | 52/52 passed, including runtime/package metadata consistency |
| Toolkit manifests/conformance | yes | 20/20, manifests match, both MCP conformance passed |
| Full offline release gate | yes | 20 toolkit + 60 Hotels + 52 Banking passed outside restricted sandbox; 0 skipped, 0 provider requests |
| Portable toolkit artifact | yes | exact allowlist, npm install outside checkout and client config passed |
| Portable Banking login | yes | wheel contains entry point; installed logout passed outside checkout |
| Documentation gate | yes | `./scripts/verify.sh docs` passed |
| Repository-wide gate | yes | `./scripts/verify.sh all` passed outside restricted sandbox |
| `git diff --check` | yes | passed |
| npm publish dry-run | yes | Hotels 0.28.0 and toolkit 0.12.0 passed; 8 allowlisted files each |
| Public npm registry | yes | `tbank-hotels-mcp@0.28.0` and `tbank-mcp-local@0.12.0` published and visible via registry metadata |
| Fresh npm install | yes | both packages installed from registry outside checkout; Hotels initialize and toolkit local readiness command passed; 0 provider requests |
| One-command combined connect | yes | hermetic OpenCode registration test passed with managed runtime, no login/network/provider calls |
| Public toolkit 0.13.1 | yes | npm registry metadata and isolated Hotels-only connect/start passed; broker not required |
| Banking wheel / PyPI metadata | yes | 0.17.0 wheel built, `twine check` passed, uploaded to PyPI and fresh-installed |
| Public combined OpenCode | yes | isolated HOME/config, both MCP connected from npm/PyPI, mobile login skipped, provider requests 0 |
| Public combined Codex | yes | isolated CODEX_HOME, both MCP registrations enabled from npm/PyPI, mobile login skipped, provider requests 0 |
| Provider requests | must be 0 | 0; verify и docs gate полностью offline |
