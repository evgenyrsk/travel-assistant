# Stage 16.7 — Semantic accommodation quality evaluation

## Статус

`NOT_RUN` — REAL rollout gate не пройден.

## Причина

В репозитории и доступном внутреннем контуре нет одобренного вручную
размеченного dataset минимум из 100 кандидатов. Права на хранение и передачу
provider descriptions/images внешней модели не подтверждены. Поэтому
изображения/provider content не коммитились, model probe и REAL evaluation не
выполнялись.

## Gate summary

| Gate | Threshold | Result |
|---|---:|---|
| Candidates | >= 100 | `NOT_RUN` |
| Destination groups | несколько, harness minimum 3 | `NOT_RUN` |
| `MATCH` precision | >= 90% | `NOT_RUN` |
| `MATCH + PROBABLE` precision | >= 80% | `NOT_RUN` |
| Recall широкого определения | >= 70% | `NOT_RUN` |
| False-positive rate обычных отелей | <= 5% | `NOT_RUN` |
| Borderline independent review | >= 2 labels | `NOT_RUN` |

## Подготовленный evaluation contract

- `tools/semantic-evaluation/evaluation-record.schema.json` принимает только
  opaque candidate/destination-group identifiers, expected label, predicted
  verdict и reviewer labels.
- Hotel names, descriptions, amenities, image URL и raw model output
  запрещены schema и runtime validator.
- CLI вычисляет все thresholds и возвращает non-zero exit code при invalid или
  failed dataset.
- Synthetic unit dataset проверяет математику harness, но не является quality
  evidence и не заменяет ручную разметку.

Команда после размещения dataset в одобренном внутреннем контуре:

```bash
cd tools/semantic-evaluation
npm run evaluate -- /approved/internal/path/glamping-evaluation.jsonl
```

## Решение

Deterministic `FAKE` mode разрешён. `OPENROUTER`/REAL vision остаётся
заблокированным до отдельного approval, controlled probe без retry и отчёта со
статусом `passed`.
