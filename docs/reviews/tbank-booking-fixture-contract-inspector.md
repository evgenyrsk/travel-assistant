# Structure-only booking fixture inspector

## Scope

Реализован structure-only intake в двух режимах: полностью офлайн по уже
имеющемуся JSON-ответу собственной брони и отдельный явно подтверждаемый bounded
read через auth broker, когда fixture заранее отсутствует. Hotels MCP, Banking
MCP, payment setup и денежные операции не вызываются.

## Результат

- Local toolkit обновлён до `0.3.0`.
- Команда `inspect-booking-fixture` принимает абсолютный путь к JSON fixture.
- В отчёт попадают только имена полей, JSON-типы и наблюдаемая вложенность.
- Значения строк, чисел и boolean не сохраняются.
- Похожие на identifiers динамические object keys заменяются.
- Размер, глубина и число JSON nodes ограничены; превышение завершает обработку
  без частичного отчёта.
- При `--output` исходный fixture нельзя перезаписать, а новый файл получает
  права `0600`.
- `capture-booking-shape` требует `--acknowledge-read-own-data`, выбирает первую
  собственную бронь только из одной категории и выполняет два Hotels reads.
- Raw list/detail payload при capture остаётся только в памяти и не печатается.

## Проверяемые границы

- [x] Синтетические `orderId`, email, имя, hotel name и payment token отсутствуют
  в отчёте.
- [x] UUID object key заменён на `<dynamic-key>`.
- [x] Наблюдаемая optional-структура элементов массива объединяется без значений.
- [x] Относительный input path и перезапись исходного файла отклоняются.
- [x] `providerRequestsPerformed=false` включён в отчёт.
- [x] Инструмент не добавлен в MCP tool surface и недоступен модели.
- [x] Live capture без явного acknowledgement останавливается до broker access.
- [x] Fake broker подтверждает ровно `list_bookings → get_booking_v1`, отсутствие
  raw values в отчёте и отсутствие raw persistence.

## Проверки

- Local toolkit: `10` tests, passed, включая owner-only broker lifecycle.
- Hotels MCP: `48` tests, passed.
- Banking MCP: `42` tests, passed.
- Contract manifests и offline conformance: passed.
- Provider requests во время release gate не выполнялись.

## Ограничения

Structure-only отчёт не является OpenAPI-схемой: он не доказывает обязательность
полей, допустимые enum, форматы строк, диапазоны чисел и payment semantics.
Исходный fixture остаётся чувствительным файлом и должен храниться вне
репозитория.

## Следующий gate

Выполнить один явно подтверждённый bounded read или получить от владельца уже
созданный и просмотренный `booking-shape.json`, затем сопоставить его
с имеющимися Hotels contracts и определить, существует ли подтверждённый
read-only источник amount, currency и payment state. Реальные API-вызовы и
мутации до отдельного разрешения не выполнять.
