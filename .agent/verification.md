# Verification

- `node --test tools/tbank-mcp-local/test/cli.test.mjs` вне sandbox: 11/11 pass.
- `npm --prefix tools/tbank-mcp-local run verify`: pass; toolkit 11, Hotels 49,
  Banking 48, manifests/conformance pass, providerRequestsPerformed=false.
- `./scripts/verify.sh docs`: pass после исправления trailing whitespace.
- `git diff --check`: pass.

| Check | Required | Latest result |
|---|---|---|
| Hotels-first lazy startup | yes | passed offline Unix-socket test |
| Banking-first lazy startup | yes | passed offline Unix-socket test |
| Shared broker survives individual MCP EOF | yes | passed |
| Explicit stop removes socket | yes | passed |
| Toolkit/Hotels/Banking tests | yes | 11 + 49 + 48 passed |
| Contract manifests and conformance | yes | passed |
| `git diff --check` | yes | passed |
| Provider requests | yes | 0 during implementation checks |

## Live acceptance

- Hotels-first `tbank_hotels_summarize_bookings`: passed после restart клиента,
  один tool, privacy-safe response, writes 0.
- Banking-second `tbank_banking_build_portfolio_travel_profile(days=90)`: passed,
  один tool, агрегированный профиль без account/amount/category disclosure,
  payments/bookings 0.
