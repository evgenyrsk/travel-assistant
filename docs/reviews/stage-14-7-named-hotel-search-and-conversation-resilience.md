# Stage 14.7 — поиск конкретного отеля и устойчивость уточнений

## Цель

Исправить воспроизведённый диалог с запросом конкретного отеля: сохранить
длительность проживания между сообщениями, задавать вопросы только по реально
недостающим данным и после подтверждения проверить наличие и цену именно
выбранного отеля.

## Подтверждённый provider contract

23 июля 2026 года выполнены два ограниченных анонимных запроса без
`Authorization`, cookies, redirects и автоматических повторов:

- autocomplete для `Cosmos ВДНХ` вернул `200`, ноль locations и один hotel;
- hotel candidate содержит строковый opaque `id`, название, подпись и объект
  `type`;
- `POST /api/v3/hotels/{hotelId}/rates` вернул `200`, 24 тарифа и 6 комнат;
- rate содержит общую `shownPrice`, currency, `availableRoomsCount`,
  `mealType`, cancellation fact и внутреннюю связь с room;
- room содержит отображаемое название и массив image objects.

Raw body и headers использовались только во временном каталоге. В репозиторий
добавлены обезличенные fixtures. `bookHash`, provider search ID и room IDs не
входят в production-модель.

## Изменения диалога

- явная длительность `7 ночей` или `1 неделя` сохраняется как вспомогательное
  session-bound значение;
- после появления даты заезда дата выезда вычисляется детерминированно;
- явно названная дата выезда имеет приоритет и завершает использование
  вспомогательной длительности;
- вопросы формируются из фактического накопленного состояния, а не из
  произвольной формулировки LLM;
- если валидный LLM candidate пропустил явно названный отель, консервативный
  application parser может дополнить только достаточно явное название; обычное
  требование к отелю не принимается за destination;
- дети и их возраста не запрашиваются, если пользователь их не упоминал;
- если первый ответ LLM был семантически невалидным, последующий пустой ответ
  единственного retry не маскирует исходную категорию как временный сбой.

## Exact-hotel orchestration

Autocomplete locations и hotels остаются разными внутренними типами:

- числовой location ID может стать `destinationId` обычного поиска;
- строковый hotel reference никогда не становится `destinationId`;
- точное совпадение hotel name/signature выбирается независимо от provider
  order;
- единственный hotel candidate допускается как fallback только при отсутствии
  location candidates;
- несколько hotel candidates требуют уточнения; первый элемент автоматически
  не выбирается.

После отдельного пользовательского подтверждения exact-hotel ветка выполняет:

1. один `GET /api/v1/hotels/{hotelId}` для безопасных hotel/location facts;
2. один `POST /api/v3/hotels/{hotelId}/rates` для availability и total price;
3. выбор самого дешёвого подходящего rate в пределах одного отеля.

Проверяются максимальная общая стоимость, звёзды, бесплатная отмена и
включённый завтрак. Guest rating отсутствует в подтверждённом exact-rates
контракте, поэтому активный порог рейтинга завершается fail-closed, а не
получает выдуманное значение.

## Безопасность

- public API не меняется;
- `hotelSearchId` и `offerId` остаются opaque application IDs;
- provider hotel/room/search IDs и `bookHash` не попадают в ответы;
- browser не обращается к Hotels API напрямую;
- malformed provider data преобразуются в существующие typed safe outcomes;
- cancellation не перехватывается как provider failure.

## REAL-проверка и оставшийся gate

Первый контролируемый REAL browser smoke дошёл до валидного OpenRouter
candidate, но candidate пропустил `Cosmos ВДНХ` в `destination`. Backend не
запустил provider search и безопасно запросил направление. Автоматический
повтор не выполнялся.

Ручная перепроверка подтвердила exact-hotel поиск, уточнение состава гостей и
получение пустой выдачи при активном требовании завтрака. Она также выявила
позднюю валидацию запроса двух номеров: агрегированные критерии допускали
`rooms=2` до confirmation, хотя оба provider mapper-а поддерживают только одну
guest group. После «Да» это превращалось в общий `RequestRejected`, а
оставшееся активным confirmation делало фразу «попробуй ещё раз» неочевидной.

Стабилизационное исправление перенесло границу раньше:

- два и более номера очищаются из session constraints как неподдерживаемые;
- assistant объясняет ограничение одного номера до confirmation;
- pending confirmation и `hotelSearchId` для такого запроса не создаются;
- распределение «двое в одном, один во втором» не объединяется и не теряется
  молча;
- исправление на один номер использует сохранённые destination, даты и состав
  гостей и снова формирует confirmation;
- внутренний инвариант `rooms=1` больше не показывается в обычном confirmation
  и удалён из diagnostic demo control;
- пустой успешный поиск сообщает, что варианты не найдены, вместо фразы
  «Результат готов».

В безопасную диагностику добавлена только фиксированная категория
`UNSUPPORTED_ROOM_COUNT`; количество номеров и пользовательский текст не
логируются. Повторная REAL-проверка после этого исправления остаётся ручной.

После этого добавлены две независимые защиты:

- OpenRouter-инструкция явно определяет destination как город, район или
  конкретный отель и требует сохранять явно названный отель;
- application-owned parser дополняет отсутствующий destination только для
  явного hotel marker либо достаточно характерного собственного названия.

Дополнение фиксируется только событием `DESTINATION_ENRICHED`; пользовательский
текст и извлечённое название в лог не попадают. Targeted и полный backend
suite после исправления прошли. Повторная REAL-проверка оставлена ручной, чтобы
не нарушать ограничение одного автоматизированного live-сценария без retry.

## Проверки

- unit tests для длительности, накопления контекста и deterministic
  clarification;
- regression test исходного диалога с `Cosmos ВДНХ` и семью ночами;
- contract tests выбора hotel candidate и обезличенных autocomplete/rates
  fixtures;
- `MockEngine` test: exact-hotel flow выполняет только details + rates и не
  вызывает destination search;
- mapper tests для price, currency, stars, breakfast, cancellation, image и
  отсутствующего review;
- regression tests ранней блокировки нескольких номеров, отсутствия pending
  confirmation и восстановления confirmation после исправления на один номер;
- отдельная проверка понятного сообщения для `COMPLETED_NO_OFFERS`.

Итоговые browser gates фиксируются после ручной повторной проверки актуального
REAL demo.

## Не входит в этап

- публичный каталог rates, room selection и передача `bookHash`;
- booking, payment, deeplink и cancellation operations;
- fuzzy hotel matching и автоматический выбор из нескольких вариантов;
- pagination, auth, durable storage, deployment и generated clients;
- изменение demo UI или публичных schemas.

## Verdict

`IMPLEMENTED_PENDING_MANUAL_REAL_RECHECK`.
