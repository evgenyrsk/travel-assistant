# Stage 13.7 — детали выбранного отеля в demo shell

## Цель

Добавить в локальную demo shell явный on-demand сценарий просмотра деталей
одного выбранного hotel offer без N+1-загрузки, раскрытия provider identity и
прямых обращений браузера к Hotels API.

## Реализация

- API client вызывает только platform-neutral endpoint по паре opaque
  `hotelSearchId + offerId`.
- Каждая карточка получила кнопку «Подробнее» и отдельную область с
  `aria-live`.
- Details не загружаются при получении списка offers. Первый запрос выполняется
  только после выбора кнопки; повторное скрытие и раскрытие использует уже
  загруженные данные текущей страницы.
- Отображаются только доступные provider-neutral facts: HTTPS-изображения,
  описание, адрес, удобства, время заезда/выезда и способы оплаты.
- Неизвестные значения пропускаются. Для отсутствующих дополнительных facts,
  загрузки и ошибки предусмотрены отдельные безопасные состояния.
- Provider text экранируется; изображения ограничены десятью уникальными HTTPS
  URL.

## Изменённые production-файлы

- `app/src/api-client.js`;
- `app/src/chat-app.js`;
- `app/src/chat-flow.js`;
- `app/src/hotel-details-view.js`;
- `app/src/offer-view.js`;
- `app/src/styles.css`;
- `app/package.json`.

Backend production code, runtime composition и public API не менялись.

## Тесты

- API path кодирует оба opaque идентификатора.
- Offers загружаются без details request; запрос details появляется только
  после явного выбора конкретного offer.
- До активного search details недоступны.
- View renderer проверен для полного, пустого и небезопасного входа.
- Карточка содержит доступную кнопку и скрытую live-region.
- Frontend tests, lint и build пройдены.
- Локальный browser QA с безопасными mock responses подтвердил один details
  request после клика, перенос focus в раскрытую область, отсутствие
  горизонтального overflow на 320×568 и 390×844 и высоту кнопки 44 CSS px.

## Границы

- Demo shell не знает provider `hotelId` и не вызывает Hotels API напрямую.
- Автоматическая загрузка деталей остальных карточек, chat-команды выбора,
  rates, deeplink, shortlist, comparison, booking и payment не добавлены.
- Browser QA не выполнял live provider/LLM calls.
- Состояние details остаётся в памяти текущей страницы.

## Verdict

`PASS_STAGE_13_7_SELECTED_HOTEL_DETAILS_DEMO_FLOW`.

Stage 13 завершён. Следующий этап — Stage 14.0: полная acceptance-проверка,
актуализация активной документации и один разрешённый REAL browser smoke без
автоматического повтора.
