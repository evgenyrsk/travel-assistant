# Internal semantic gateway contract v1

## Роль

Технический contract для `INTERNAL_GATEWAY` adapter Travel Assistant. Он не
является публичным product API и не входит в OpenAPI client subset.

Архитектурная граница и rationale зафиксированы в
[`ADR-0002`](../../decisions/adr-0002-provider-neutral-semantic-gateway-boundary.md).

## Transport

- Method: `POST`.
- Exact path: `/v1/accommodation-analysis`.
- Scheme: HTTPS.
- Request/response content type: `application/json`.
- Redirect: запрещён.
- Текущий adapter: `Authorization: Bearer <secret>`.
- Retry и automatic fallback: отсутствуют.

Будущая workload identity или mTLS меняет только infrastructure composition и
не меняет body contract.

## Request

```json
{
  "schema_version": "1",
  "deployment_id": "vision-balanced-v1",
  "concept": "glamping",
  "candidates": [
    {
      "candidate_id": "candidate-01",
      "hotel_name": "Synthetic Glamping",
      "descriptions": ["Synthetic description"],
      "amenities": ["Synthetic amenity"],
      "image_urls": ["https://images.internal.test/one.jpg"]
    }
  ]
}
```

Ограничения:

- один adapter batch содержит 1–6 candidates;
- `candidate_id` является ephemeral и не содержит provider identity;
- `hotel_name` — максимум 200 символов;
- максимум 8 descriptions по 2000 символов;
- максимум 50 amenities по 200 символов;
- максимум 3 уникальных HTTPS image URL из exact-host allowlist;
- user message, session/search/offer/provider IDs не передаются;
- единственный текущий managed concept — `glamping`.

## Response

```json
{
  "schema_version": "1",
  "deployment_id": "vision-balanced-v1",
  "results": [
    {
      "candidate_id": "candidate-01",
      "verdict": "match",
      "evidence": [
        {
          "source": "name",
          "signal": "explicit_glamping_label"
        }
      ]
    }
  ]
}
```

Для каждого request candidate возвращается ровно один result с тем же
`candidate_id`. `schema_version` и `deployment_id` должны точно совпасть с
request.

Допустимые verdict:

- `match`;
- `probable`;
- `no_match`;
- `unknown`.

Допустимые evidence source:

- `name`;
- `description`;
- `amenities`;
- `image`.

Допустимые signal:

- `explicit_glamping_label`;
- `glamping_structure`;
- `nature_setting`;
- `glamping_amenity`;
- `image_glamping_structure`;
- `standard_hotel_format`;
- `apartment_block_format`;
- `empty_camping_pitch`;
- `ordinary_cottage`.

Free-form rationale и дополнительные response fields запрещены. Application
validation дополнительно проверяет completeness, duplicate IDs, evidence
consistency и semantic verdict policy.

## Failure mapping

| HTTP / failure | Application reason |
|---|---|
| `400`, `422` | `REQUEST_REJECTED` |
| `401`, `403` | `AUTHENTICATION_FAILED` |
| `408`, `504`, client timeout | `TIMEOUT` |
| `429` | `RATE_LIMITED` |
| network / other non-2xx | `UNAVAILABLE` |
| content type, schema, version, deployment или enum drift | `INVALID_RESPONSE` |

Любой failure завершает текущий batch fail-closed; adapter не повторяет запрос и
не выбирает другой deployment.
