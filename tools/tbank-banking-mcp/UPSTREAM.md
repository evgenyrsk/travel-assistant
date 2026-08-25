# Upstream provenance

Часть transport/auth-кода получена из проекта
[`icyberdeveloper/tbank-mcp`](https://github.com/icyberdeveloper/tbank-mcp):

- upstream revision: `6e2f577daf118d95def41a738573c3030fc83bb2`;
- license: MIT, копия находится в `LICENSE.upstream`;
- импортированы `client.py`, `endpoints.py`, `observability.py`, `tls.py` и
  закреплённый Russian Trusted Root CA;
- исходные copyright и license terms сохранены.

Локальные изменения ограничивают публичную MCP-поверхность. В этот пакет не
перенесён исходный FastMCP server с полным набором банковских, grocery,
messenger и денежных tools. Реальные payment methods присутствуют внутри
MIT-derived transport-клиента как неэкспортируемая реализация, но новый MCP их
не вызывает и не объявляет.

Upstream использует неофициальный capture-driven mobile API. Наличие исходного
кода не является официальным контрактом Т-Банка. Перед production use нужны
approval владельца API, security review и проверка актуальных auth/payment
контрактов.
