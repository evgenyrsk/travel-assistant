# T-Bank Hotels API MCP

Независимый от браузера MCP-сервер для Hotels API Т-Банка. Это stdio-сервер на
Node.js 20+, поэтому его можно подключить к Codex, Claude Code, OpenCode и
любому другому MCP-клиенту без расширения браузера, cookie и сохранённой
браузерной сессии.

Сервер использует только явную конфигурацию API transport. Токены остаются в
переменных окружения процесса: MCP не принимает их как аргументы инструментов,
не записывает на диск и не выводит в ответах.

## Подключение

Установите Node.js 20+ и задайте endpoint и один auth-profile. Абсолютный
server URL отсутствует в переданных OpenAPI-контрактах, поэтому его обязан
предоставить владелец интеграции.

```bash
export TBANK_HOTELS_API_BASE_URL='https://<approved-hotels-api-origin>/'
export TBANK_HOTELS_AUTH_TOKEN='<token>'
```

По умолчанию MCP передаёт токен как `Authorization: Bearer <token>`. Для
нестандартной схемы можно явно задать `TBANK_HOTELS_AUTH_HEADER` и
`TBANK_HOTELS_AUTH_PREFIX`. Если нужны несколько заголовков, вместо token-based
переменных используйте ровно одну:

```bash
export TBANK_HOTELS_AUTH_HEADERS_JSON='{"X-Integration-Token":"<token>","X-Client-Id":"<client-id>"}'
```

Cookie, SSO-заголовки и обновление токена не используются. Если production API
потребует несколько заголовков, их можно передать в JSON без изменения MCP
tools.

Необязательная настройка таймаута:

```bash
export TBANK_HOTELS_TIMEOUT_MS=15000
```

Пример stdio-конфигурации для любого MCP-клиента:

```json
{
  "command": "node",
  "args": ["/absolute/path/to/travel-assistant/tools/tbank-hotels-mcp/src/server.mjs"],
  "env": {
    "TBANK_HOTELS_API_BASE_URL": "https://<approved-hotels-api-origin>/",
    "TBANK_HOTELS_AUTH_TOKEN": "<token>"
  }
}
```

Пакет также объявляет bin-команду `tbank-hotels-mcp`, поэтому после публикации
или локальной установки его можно запускать этой командой вместо пути к файлу.

## Контракт MCP

Инструменты возвращают ответ provider как `{ status, data }`. Их `payload`
передаётся без преобразования в соответствующий OpenAPI request body, поэтому
потребитель должен использовать типы из официальных v1–v3-контрактов. Это
исключает догадки о полях, которые не подтверждены спецификацией.

| Группа | Инструменты |
| --- | --- |
| Проверка transport/auth | `tbank_hotels_connection_status`, `tbank_hotels_get_customer` |
| Поиск по локации и точному отелю | `tbank_hotels_search`, `tbank_hotels_get_search_filters`, `tbank_hotels_get_filter_availability`, `tbank_hotels_search_map`, `tbank_hotels_get_map_hotels`, `tbank_hotels_search_points_of_interest` |
| Карточка, номера и тарифы | `tbank_hotels_get_hotel`, `tbank_hotels_get_hotel_rates`, `tbank_hotels_get_rate`, `tbank_hotels_get_cashback_percent`, `tbank_hotels_get_max_cashback`, `tbank_hotels_validate_promocode`, `tbank_hotels_get_rate_upgrade` |
| Отзывы, SEO и deeplink | `tbank_hotels_get_reviews`, `tbank_hotels_get_review_order_status`, `tbank_hotels_search_seo`, `tbank_hotels_search_urls`, `tbank_hotels_get_seo_resource`, `tbank_hotels_get_deeplink_token`, `tbank_hotels_get_partner_redirect_url` |
| Рассрочка | `tbank_hotels_get_available_tranche_amount`, `tbank_hotels_get_bnpl_offer` |
| Авторизованные заказы | `tbank_hotels_get_booking`, `tbank_hotels_list_bookings`, `tbank_hotels_get_voucher`, `tbank_hotels_get_reservation`, `tbank_hotels_get_evo_booking`, `tbank_hotels_get_bnpl_offer`, `tbank_hotels_get_booking_task_status`, `tbank_hotels_check_ls_order` |
| Изменяющие операции | `tbank_hotels_prepare_*` и `tbank_hotels_execute_*` для обычной и LS-брони, отмены, setup оплаты, промокода и дополнительных услуг |

По умолчанию для чтения заказа, тарифов, SEO и URL выбран v3; аргумент
`apiVersion` позволяет явно выбрать поддерживаемую версию (`v1`, `v2`, `v3`) в
тех случаях, где она есть в полученных контрактах.

Не включены browser-cookie endpoint, internal endpoints, callbacks и операции
изменения отзывов: они не являются переносимой клиентской Hotels-функцией либо
требуют отдельного официального контракта и security review.

## Бронь, отмена и оплата

Для каждой изменяющей операции есть пара `prepare` → `execute`:

1. `prepare` не вызывает API, ничего не бронирует и не списывает. Он возвращает
   endpoint, безопасный preview payload, `requestHash` и точную фразу
   подтверждения.
2. Покажите пользователю цену, условия, последствия и preview. Только после
   непосредственного явного согласия вызовите соответствующий `execute` с тем
   же payload, `preparedRequestHash` и фразой подтверждения.

Сервер повторно вычисляет хэш полного запроса. Изменение даже одного поля делает
подтверждение недействительным. Pending action, пользовательская сессия и
платёжное состояние в памяти сервера не хранятся. MCP не принимает номер карты,
CVV, пароль или одноразовый код; если официальный payment contract требует их,
нужен отдельный защищённый payment-hand-off.

## Проверка

```bash
cd tools/tbank-hotels-mcp
npm test
```

Тесты проверяют MCP-протокол, конфигурационные границы и stateless confirmation
без сетевых вызовов к Т-Банку.
