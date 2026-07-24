# Stage 9.18a — детерминированный выбор location candidate

## Роль документа

Это отчет о реализации Stage 9.18a. Документ фиксирует реализованную policy
уровня application, проверки и результат одного разрешенного runtime smoke.
Текущий статус проекта задает [`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Устранить блокирующее ограничение Stage 9.18: разрешать location автоматически только при
единственном безопасно определяемом candidate и не выбирать первый результат
autocomplete по порядку provider.

## Реализация

Добавлены внутренние application-контракты:

- `HotelLocationCandidateSelectionPolicy`;
- `HotelLocationCandidateSelectionResult` с вариантами `Selected`, `NotFound` и
  `SelectionRequired`;
- `ExactMatchHotelLocationCandidateSelectionPolicy` как стандартная реализация.

`HotelsApiSearchOrchestrator` получает policy через конструктор.
`HotelOfferProviderFactory` явно подключает стандартную реализацию только для
`REAL` runtime-композиции. Transport, autocomplete/search DTO, mapper и public API не
изменены.

## Правила выбора

Policy выполняет следующие шаги:

1. дедуплицирует candidates по `destinationId`, сохраняя первое вхождение;
2. возвращает `NotFound` для пустого списка;
3. выбирает единственный уникальный candidate;
4. среди нескольких candidates ищет точное совпадение запроса с `name` или
   `signature`;
5. выбирает candidate только при одном точном совпадении;
6. при нуле или нескольких совпадениях требует явного уточнения.

Нормализация включает Unicode NFKC, удаление пробелов по краям, сведение
повторяющихся пробельных символов к одному пробелу, lowercase через
`Locale.ROOT` и эквивалентность `ё`/`е`.

Поиск по подстроке, нечеткое сравнение, приоритет `type`, порядок provider и fallback на
первый candidate не используются. Hotel suggestions не участвуют в выборе
`destinationId`.

## Автоматизированные проверки

Точечные тесты покрывают:

- пустой и единственный candidate;
- точное совпадение по `name` и `signature`;
- Unicode, регистр, пробелы и `ё`/`е`;
- дедупликацию с сохранением первого вхождения;
- неоднозначность двух точных совпадений с разными ID;
- запрет выбора первого элемента при частичном совпадении или отсутствии exact match;
- отсутствие search HTTP-вызова при неоднозначности;
- ровно один search request при единственном точном совпадении.

Полный набор backend-тестов прошел. Последующие outcomes и публичные response
schemas остались прежними.

## Контролируемый runtime smoke

18 июля 2026 года после локальных проверок выполнен один разрешенный opt-in
smoke через production runtime composition:

- режим: `REAL`;
- public base URL: `https://hotels.tbank.ru/`;
- синтетическое направление: `Иннополис`;
- синтетические будущие даты и два взрослых;
- без `Authorization`, cookies и пользовательских данных.

Результат: search создан с HTTP `202`, состояние
`completed_with_offers`, endpoint предложений вернул семь вариантов. Полные
provider responses и headers не сохранялись и не публиковались. Повторный
live-вызов не выполнялся.

## Границы этапа

Не изменялись:

- `HotelOfferRanker` и политика показа 2–5 вариантов;
- поведение pagination, polling и retry;
- routes, public API, OpenAPI, frontend и generated clients;
- provider DTO, mapper и transport;
- `FAKE` как режим по умолчанию;
- auth, secrets, storage и booking flow.

Policy остается заменяемой через application interface; изменение будущей
стратегии выбора не требует изменения transport или public contract.

## Verdict

`PASS_LOCATION_SELECTION_BLOCKER_CLOSED`.

Stage 9.18 и Stage 9.18a закрыты: regression пройден, единственный повторный
runtime smoke создал реальный search. Следующий разрешенный этап — Stage 9.19a,
накопление канонического контекста hotel constraints по assistant session.
