# Stage 2 — Edge Cases

> MVP v1 scope update: этот документ сохраняется как historical traceability. Edge cases про flight search и combined search относятся к future scope после hotel-only MVP v1.

## Цель документа

Зафиксировать edge cases для Travel Assistant без технической реализации. Edge cases помогают Stage 3 уточнить MVP boundaries и acceptance criteria.

## Таблица edge cases

| ID | Категория | Описание | Пример пользовательского запроса | Ожидаемое поведение ассистента | Задавать уточнение | Вызывать provider | Fallback-поведение | Связанные use cases | MVP status |
|---|---|---|---|---|---|---|---|---|---|
| EC-001 | Missing data | Нет дат для hotel search. | "Найди отель в Барселоне." | Уточнить даты или допустимую гибкость. | yes | no | Попросить даты/период. | UC-01, UC-04 | In MVP |
| EC-002 | Missing data | Нет города отправления для flight search. | "Хочу в Рим на выходные." | Future-scope для MVP v1; предложить hotel search, если релевантно. | yes | no | Сохранить destination и период как known parameters for future. | UC-02, UC-04 | Future scope after MVP v1 |
| EC-003 | Missing data | Неясно, нужен отель, перелет или оба. | "Хочу поездку в Париж." | В MVP v1 уточнить, нужен ли hotel search; flight/combined обозначить как future scope. | yes | no | Продолжить с hotel search после выбора intent. | UC-03, UC-04 | In MVP v1 для hotel; future scope для flight/combined |
| EC-004 | Missing data | Нет бюджета. | "Хочу хороший отель в центре." | Уточнить budget или предложить price tiers. | yes | open | Можно искать только если бюджет не критичен и это явно обозначено. | UC-01, UC-04 | In MVP |
| EC-005 | Missing data | Нет количества гостей/пассажиров. | "Нужен отель на неделю." | Уточнить guests/passengers, если влияет на цену/availability. | yes | no | Сохранить остальные параметры. | UC-01, UC-02, UC-04 | In MVP |
| EC-006 | Missing data | Нет длительности или return date. | "Хочу слетать в Лиссабон в июне." | Уточнить длительность или даты возврата. | yes | no | Не обещать цены без дат. | UC-02, UC-04 | In MVP |
| EC-007 | Ambiguous request | Открытое направление. | "Куда-нибудь в теплое место." | Уточнить origin, период, бюджет, тип отдыха и допустимую географию. | yes | open | Если provider discovery недоступен, предложить сузить критерии. | UC-03, UC-11 | Open |
| EC-008 | Ambiguous request | Расплывчатое качество. | "Нормальный отель недорого." | Уточнить, что значит "нормальный": рейтинг, отзывы, район, удобства. | yes | open | Предложить несколько интерпретаций. | UC-01, UC-11 | In MVP |
| EC-009 | Contradictory constraints | Luxury + strict budget. | "5 звезд в центре Парижа на неделю до 200 евро." | Обозначить конфликт как вероятный; уточнить готовность ослабить бюджет/класс/район. | yes | open | Можно искать после предупреждения. | UC-10 | In MVP |
| EC-010 | Contradictory constraints | Слишком низкий бюджет для hotel constraints. | "Чистый отель у моря в августе до 20 евро." | Объяснить риск нереалистичности и предложить изменить constraints. | yes | open | Не выдавать случайные варианты как хорошие. | UC-01, UC-10 | In MVP |
| EC-011 | Contradictory constraints | Прямой рейс туда, где прямых рейсов может не быть. | "Только прямой рейс на маленький остров." | Проверить provider facts, если route/dates достаточны; иначе уточнить. | open | open | Предложить пересадки, если прямой рейс недоступен. | UC-02, UC-10 | In MVP |
| EC-012 | Contradictory constraints | Очень короткая пересадка. | "Пересадка максимум 10 минут." | Объяснить вероятный operational conflict и предложить реалистичный минимум. | yes | open | Не обещать невозможную стыковку. | UC-02, UC-03, UC-10 | In MVP |
| EC-013 | Provider/data problems | API-контракт еще не предоставлен на продуктовом этапе. | N/A | Зафиксировать как Open input для будущих технических этапов, не переносить real API integration в Post-MVP. | no | no | Использовать placeholders только для разработки. | UC-15 | Open |
| EC-014 | Provider/data problems | Provider недоступен. | Любой search. | Отличить provider error от empty result. | no | yes | Сообщить временную проблему и предложить повторить/изменить параметры. | UC-09, UC-15 | In MVP |
| EC-015 | Provider/data problems | Provider вернул пустой список. | Любой search. | Объяснить, что offers не найдены по текущим constraints. | no | yes | Предложить ослабить 1-3 ограничения. | UC-09 | In MVP |
| EC-016 | Provider/data problems | Provider вернул неполные данные. | N/A | Показать known facts и unknown fields. | no | yes | Не использовать missing fields в уверенной рекомендации. | UC-13, UC-15 | In MVP |
| EC-017 | Provider/data problems | Provider data недостаточно для сравнения. | "Сравни эти два." | Сравнить доступные поля и явно назвать gaps. | no | open | Предложить перепроверить или выбрать критерий. | UC-05, UC-13 | In MVP |
| EC-018 | Provider/data problems | Нет freshness-информации для цены/availability. | N/A | Пометить цену/availability как потенциально устаревшую или с unknown freshness. | no | yes | Не обещать актуальность. | UC-06, UC-13, UC-15 | In MVP |
| EC-019 | Provider/data problems | Provider вернул данные без обязательного поля. | N/A | Не показывать offer как полноценный, если поле критично. | no | yes | Пометить как incomplete или исключить с объяснением. | UC-02, UC-13, UC-15 | In MVP |
| EC-020 | Provider/data problems | Provider result отличается от пользовательских constraints. | "Только без пересадок", provider вернул рейс с пересадками. | Не ранжировать нарушающий offer как лучший без предупреждения. | no | yes | Объяснить mismatch и предложить изменить constraints. | UC-03, UC-09 | In MVP |
| EC-021 | Provider/data problems | Provider вернул противоречивые данные. | N/A | Не скрывать conflict; не делать уверенный вывод. | no | yes | Пометить как provider limitation. | UC-01, UC-13, UC-15 | In MVP |
| EC-022 | Provider/data problems | Provider latency слишком высокая. | Любой search. | Сообщить ожидание или fallback без выдуманных offers. | no | yes | Предложить повторить позже или уточнить запрос. | UC-02, UC-09 | Open |
| EC-023 | LLM/assistant risks | Ассистент пытается угадать факты. | "Сколько стоит?" без provider data. | Отказаться выдумывать цену; запросить/использовать provider data. | no | open | Пометить как unknown data. | UC-06, UC-11 | In MVP |
| EC-024 | LLM/assistant risks | Смешаны provider facts и assumptions. | N/A | Разделить verified facts, assumptions и unknown data. | no | open | Исправить объяснение. | UC-06, UC-09, UC-13, UC-15 | In MVP |
| EC-025 | LLM/assistant risks | Поиск начат без обязательных параметров. | "Найди билет" | Сначала уточнить required fields. | yes | no | Не запускать ненадежный search. | UC-03, UC-10 | In MVP |
| EC-026 | LLM/assistant risks | Слишком уверенная рекомендация без данных. | "Какой точно лучший?" | Объяснить пределы уверенности и known gaps. | no | open | Рекомендовать только в пределах доступных facts. | UC-05, UC-06 | In MVP |
| EC-027 | Session data | Сохраненный offer может устареть. | "Вернись к сохраненному." | Показать saved facts и freshness warning. | no | open | Предложить refresh через provider. | UC-07, UC-08 | In MVP |
| EC-028 | Session data | Пользователь просит долгосрочную историю. | "Покажи поездку из прошлого месяца." | Объяснить ограничение MVP, если history unavailable. | no | no | Работать с текущей session only. | UC-07, UC-08 | Post-MVP |
| EC-029 | Session data | Изменение constraints делает старые offers нерелевантными. | "Теперь только без пересадок." | Обновить constraints и пометить affected offers. | no | open | Повторить search при достаточных данных. | UC-08, UC-12 | In MVP |
| EC-030 | Session data | Пользователь меняет сразу несколько параметров. | "Сделай дешевле, но ближе к центру и на день дольше." | Подтвердить изменения и проверить conflicts. | yes | open | Уточнить приоритет, если constraints конфликтуют. | UC-12 | In MVP |
| EC-031 | Unsupported actions | Booking request. | "Забронируй этот отель." | Не бронировать; объяснить, что booking вне MVP. | no | no | Предложить сохранить или показать детали offer. | UC-14 | In MVP fallback; booking Post-MVP |
| EC-032 | Unsupported actions | Payment request. | "Оплати билеты." | Не принимать платежи и не обещать покупку. | no | no | Предложить продолжить выбор/сохранить. | UC-14 | In MVP fallback; payment Post-MVP |
| EC-033 | Unsupported actions | Guaranteed availability. | "Гарантируй, что место есть." | Не гарантировать без provider confirmation. | no | open | Указать freshness и limits. | UC-14, UC-15 | In MVP |
| EC-034 | Unsupported actions | Visa/legal advice. | "Нужна ли мне виза?" | Не давать юридически значимое заключение. | no | no | Посоветовать проверить официальные источники; можно отметить как outside MVP. | UC-14 | Post-MVP/Open |
| EC-035 | Unsupported actions | Интерпретация refund policy без provider data. | "Точно вернут деньги?" | Не интерпретировать policy без provider facts. | no | open | Пометить как unknown data и предложить проверить provider terms. | UC-14 | In MVP fallback |

## Общие правила

- Отсутствие API-контракта на Stage 2 является Open input для будущих технических этапов, а не основанием переносить реальную интеграцию в Post-MVP.
- Booking и payment не входят в MVP; fallback на такие запросы входит в MVP как безопасное поведение.
- Provider facts имеют приоритет над assistant assumptions.
