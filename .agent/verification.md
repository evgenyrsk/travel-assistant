# Verification

| Check | Required | Latest result |
| --- | --- | --- |
| Supplied Swagger parse and checkout schema review | yes | passed, offline |
| Node syntax | yes | passed |
| Targeted Hotels tests | yes | 74/74 passed after Qwen P3 follow-up |
| Banking tests | yes | 52/52 passed |
| Toolkit tests | yes | 21/21 passed outside sandbox |
| Toolkit contract manifest/check | yes | passed |
| MCP protocol conformance and clean restart | yes | passed for both MCP |
| npm artifact install outside checkout | yes | passed for Hotels/toolkit |
| Documentation gate | yes | `./scripts/verify.sh docs` passed |
| `git diff --check` | yes | passed |
| Provider requests in automated gates | must be 0 | 0 |
| Bounded checkout live smoke | yes | passed after v3 shape fix; no writes |
| Independent Qwen 3.8 Max review | yes | `CONDITIONAL READY`, 0 P0-P2; all 5 P3 fixed locally |
| npm publication | yes | Hotels `0.30.0` and toolkit `0.16.0` published as `latest`; Banking `0.17.0` present on PyPI |
| Fresh registry install | yes | exact versions installed; Hotels MCP initialize returned `0.30.0` |
