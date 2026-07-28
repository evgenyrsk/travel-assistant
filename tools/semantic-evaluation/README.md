# Semantic accommodation quality evaluation

Инструмент считает Stage 16 quality gates по агрегированному JSON Lines dataset.
Он не принимает hotel names, descriptions, amenities, image URL или raw model
output и поэтому может храниться отдельно от provider content.

```bash
cd tools/semantic-evaluation
npm test
npm run evaluate -- /approved/internal/path/glamping-evaluation.jsonl
```

Каждая строка должна соответствовать `evaluation-record.schema.json`.
`destinationGroup` — непрозрачная группа, а не название направления. Для
`borderline=true` обязательны как минимум две независимые reviewer labels.

Exit code `0` означает прохождение всех gates, `1` — валидный dataset не прошёл
quality thresholds, `2` — ошибка запуска или schema. Dataset минимум из 100
кандидатов и provider images/descriptions остаётся в одобренном внутреннем
контуре, пока права на его хранение и передачу явно не подтверждены.

Rights-approved dataset должен быть сформирован до просмотра model outputs и
содержать минимум три непрозрачные destination groups, обычные отели,
подтверждённые glamping-объекты и borderline cases. Borderline subset размечают
два reviewer независимо. Repository получает только opaque labels и
агрегированный report; исходные descriptions, amenities, images и model output
остаются в согласованном внутреннем контуре с зафиксированными retention,
access и deletion rules.
