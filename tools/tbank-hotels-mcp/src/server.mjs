#!/usr/bin/env node

import { createHash } from "node:crypto";
import { createInterface } from "node:readline";

const SERVER_NAME = "tbank-hotels-api-mcp";
const SERVER_VERSION = "0.2.0";
const DEFAULT_TIMEOUT_MS = 15_000;
const MAX_TIMEOUT_MS = 60_000;
const AUTH_HEADER_NAME = /^[!#$%&'*+.^_`|~0-9A-Za-z-]+$/;

const text = (value) => ({ type: "text", text: typeof value === "string" ? value : JSON.stringify(value, null, 2) });

const objectSchema = (properties, required = [], additionalProperties = false) => ({
  type: "object",
  properties,
  required,
  additionalProperties,
});

const payload = {
  type: "object",
  description: "Payload в точном формате соответствующего Hotels API контракта.",
  additionalProperties: true,
};

const bookingPayload = {
  type: "object",
  description: "Payload из CreateBookingsApiRequest или CreateBookingsWithTcsUserDataApiRequest.",
  additionalProperties: true,
};

const tools = [
  {
    name: "tbank_hotels_connection_status",
    description: "Показывает, настроен ли API transport и auth profile. Секреты, значения заголовков и токены никогда не возвращаются.",
    inputSchema: objectSchema({}),
  },
  {
    name: "tbank_hotels_get_customer",
    description: "Получает данные авторизованного клиента через GET /api/v1/auth/customerdata. Используйте только когда интегратор имеет законное основание обрабатывать эти данные.",
    inputSchema: objectSchema({}),
  },
  {
    name: "tbank_hotels_search",
    description: "Ищет отели по локации или точному отелю, датам, гостям, фильтрам и сортировке. payload соответствует SearchParametersListApiRequest; exact-hotel и location search задаются provider-полями payload.",
    inputSchema: objectSchema({ payload, language: languageSchema() }, ["payload"]),
  },
  {
    name: "tbank_hotels_get_search_filters",
    description: "Возвращает каталог доступных фильтров Hotels API.",
    inputSchema: objectSchema({ apiVersion: apiVersionSchema() }),
  },
  {
    name: "tbank_hotels_get_filter_availability",
    description: "Возвращает доступность фильтров для точных параметров поиска.",
    inputSchema: objectSchema({ payload, language: languageSchema() }, ["payload"]),
  },
  {
    name: "tbank_hotels_search_map",
    description: "Возвращает поисковые данные для карты отелей. payload соответствует SearchParametersListApiRequest.",
    inputSchema: objectSchema({ payload, language: languageSchema() }, ["payload"]),
  },
  {
    name: "tbank_hotels_get_map_hotels",
    description: "Возвращает отели для области карты. payload соответствует контракту POST /api/v1/hotels/map/hotels.",
    inputSchema: objectSchema({ payload, language: languageSchema() }, ["payload"]),
  },
  {
    name: "tbank_hotels_search_points_of_interest",
    description: "Ищет точки интереса, ориентиры или группы ориентиров. mode: search, landmarks или groups.",
    inputSchema: objectSchema({ mode: { type: "string", enum: ["search", "landmarks", "groups"] }, payload }, ["mode", "payload"]),
  },
  {
    name: "tbank_hotels_get_hotel",
    description: "Получает provider-карточку отеля по hotelId.",
    inputSchema: objectSchema({ hotelId: identifierSchema("Идентификатор отеля из Hotels API."), language: languageSchema() }, ["hotelId"]),
  },
  {
    name: "tbank_hotels_get_hotel_rates",
    description: "Получает номера и тарифы выбранного отеля. По умолчанию используется v3; payload соответствует POST /api/v3/hotels/{hotelId}/rates.",
    inputSchema: objectSchema({ hotelId: identifierSchema("Идентификатор отеля из Hotels API."), payload, apiVersion: rateApiVersionSchema(), language: languageSchema() }, ["hotelId", "payload"]),
  },
  {
    name: "tbank_hotels_get_rate",
    description: "Получает актуальный checkout rate по bookHash. По умолчанию используется v3.",
    inputSchema: objectSchema({ bookHash: identifierSchema("Непрозрачный bookHash из ответа Hotels API."), apiVersion: rateApiVersionSchema(), language: languageSchema() }, ["bookHash"]),
  },
  {
    name: "tbank_hotels_get_cashback_percent",
    description: "Получает процент кэшбэка по bookHash для авторизованного счёта.",
    inputSchema: objectSchema({ bookHash: identifierSchema("Непрозрачный bookHash из ответа Hotels API.") }, ["bookHash"]),
  },
  {
    name: "tbank_hotels_get_max_cashback",
    description: "Получает максимальный доступный процент кэшбэка.",
    inputSchema: objectSchema({}),
  },
  {
    name: "tbank_hotels_validate_promocode",
    description: "Проверяет промокод без создания брони. payload соответствует POST /api/v1/hotels/promocodes/validate.",
    inputSchema: objectSchema({ payload }, ["payload"]),
  },
  {
    name: "tbank_hotels_get_rate_upgrade",
    description: "Получает доступное улучшение тарифа по bookHash. Не применяет изменение.",
    inputSchema: objectSchema({ bookHash: identifierSchema("Непрозрачный bookHash из ответа Hotels API."), payload }, ["bookHash", "payload"]),
  },
  {
    name: "tbank_hotels_get_booking",
    description: "Получает существующую бронь по orderId. По умолчанию используется v3.",
    inputSchema: objectSchema({ orderId: identifierSchema("Идентификатор заказа."), apiVersion: bookingApiVersionSchema(), language: languageSchema() }, ["orderId"]),
  },
  {
    name: "tbank_hotels_list_bookings",
    description: "Возвращает активные, отменённые и завершённые брони. payload соответствует BookingsListApiRequest.",
    inputSchema: objectSchema({ payload }, ["payload"]),
  },
  {
    name: "tbank_hotels_get_voucher",
    description: "Получает voucher существующей брони. Результат может содержать персональные данные и документ бронирования.",
    inputSchema: objectSchema({ orderId: identifierSchema("Идентификатор заказа.") }, ["orderId"]),
  },
  {
    name: "tbank_hotels_get_reservation",
    description: "Получает reservation-данные бронирования. Параметры query должны соответствовать GET /api/v1/hotels/bookings/getReservation.",
    inputSchema: objectSchema({ query: payload }, ["query"]),
  },
  {
    name: "tbank_hotels_get_evo_booking",
    description: "Получает EVO booking-данные по orderId.",
    inputSchema: objectSchema({ orderId: identifierSchema("Идентификатор заказа.") }, ["orderId"]),
  },
  {
    name: "tbank_hotels_get_bnpl_offer",
    description: "Получает предложение рассрочки по существующему заказу. Не подключает рассрочку и не списывает средства.",
    inputSchema: objectSchema({ orderId: identifierSchema("Идентификатор заказа."), payload }, ["orderId", "payload"]),
  },
  {
    name: "tbank_hotels_get_booking_task_status",
    description: "Получает статус асинхронной задачи создания бронирования.",
    inputSchema: objectSchema({ taskId: identifierSchema("Идентификатор задачи.") }, ["taskId"]),
  },
  {
    name: "tbank_hotels_check_ls_order",
    description: "Проверяет статус LS-заказа по orderId.",
    inputSchema: objectSchema({ orderId: identifierSchema("Идентификатор заказа.") }, ["orderId"]),
  },
  {
    name: "tbank_hotels_get_reviews",
    description: "Возвращает ratings, summary, feedback, feedback-filters или order-status для отеля. Запрос не изменяет лайки или отзывы.",
    inputSchema: objectSchema({ hotelId: identifierSchema("Идентификатор отеля."), resource: { type: "string", enum: ["ratings", "summary", "feedback", "feedback-filters"] }, query: payload }, ["hotelId", "resource"]),
  },
  {
    name: "tbank_hotels_get_review_order_status",
    description: "Проверяет, доступно ли действие с отзывом для конкретного заказа. Запрос не изменяет отзыв или лайк.",
    inputSchema: objectSchema({ orderId: identifierSchema("Идентификатор заказа.") }, ["orderId"]),
  },
  {
    name: "tbank_hotels_search_seo",
    description: "Выполняет SEO/location search Hotels API. По умолчанию используется v3.",
    inputSchema: objectSchema({ payload, apiVersion: seoApiVersionSchema() }, ["payload"]),
  },
  {
    name: "tbank_hotels_search_urls",
    description: "Создаёт поисковый URL Hotels API из параметров. По умолчанию используется v3.",
    inputSchema: objectSchema({ payload, apiVersion: seoApiVersionSchema() }, ["payload"]),
  },
  {
    name: "tbank_hotels_get_seo_resource",
    description: "Получает публичные SEO-данные: отель, регион, фильтры, locations, location-by-slug, комнаты или slug отеля.",
    inputSchema: objectSchema({ resource: { type: "string", enum: ["hotel", "region", "available-filters", "locations", "location-by-slug", "rooms", "slug-by-hotel"] }, id: identifierSchema("hotelId, regionId или locationId, если это требуется выбранным resource."), query: payload }, ["resource"]),
  },
  {
    name: "tbank_hotels_get_deeplink_token",
    description: "Получает токен для general или Hotels URL deeplink. Не создаёт бронирование.",
    inputSchema: objectSchema({ kind: { type: "string", enum: ["general", "hotels-urls"] } }, ["kind"]),
  },
  {
    name: "tbank_hotels_get_available_tranche_amount",
    description: "Проверяет доступную сумму рассрочки для параметров checkout. Не подключает рассрочку и не создаёт платёж.",
    inputSchema: objectSchema({ payload }, ["payload"]),
  },
  {
    name: "tbank_hotels_get_partner_redirect_url",
    description: "Получает redirect URL партнёра по partnerAlias. Сам переход и внешняя покупка не выполняются.",
    inputSchema: objectSchema({ partnerAlias: identifierSchema("Псевдоним партнёра."), payload }, ["partnerAlias", "payload"]),
  },
  mutationTool("tbank_hotels_prepare_booking", "Создаёт stateless preview создания брони. Не делает HTTP-запрос и не резервирует номер.", "booking", bookingPayload),
  mutationTool("tbank_hotels_execute_booking", "Создаёт задачу бронирования только после непосредственного явного подтверждения пользователя.", "booking", bookingPayload, true),
  mutationTool("tbank_hotels_prepare_ls_booking", "Создаёт stateless preview создания LS-брони. Не делает HTTP-запрос и не резервирует номер.", "lsBooking", bookingPayload),
  mutationTool("tbank_hotels_execute_ls_booking", "Создаёт LS-задачу бронирования только после непосредственного явного подтверждения пользователя.", "lsBooking", bookingPayload, true),
  mutationTool("tbank_hotels_prepare_cancel_booking", "Создаёт stateless preview отмены брони. Не отменяет заказ.", "cancel", payload),
  mutationTool("tbank_hotels_execute_cancel_booking", "Отменяет бронь только после непосредственного явного подтверждения пользователя.", "cancel", payload, true),
  mutationTool("tbank_hotels_prepare_payment_setup", "Создаёт stateless preview подготовки оплаты. Не создаёт платёж и не списывает средства.", "paymentSetup", payload, false, { orderId: identifierSchema("Идентификатор заказа.") }),
  mutationTool("tbank_hotels_execute_payment_setup", "Подготавливает оплату для заказа только после непосредственного явного подтверждения пользователя. MCP не принимает данные карты.", "paymentSetup", payload, true, { orderId: identifierSchema("Идентификатор заказа.") }),
  mutationTool("tbank_hotels_prepare_apply_promocode", "Создаёт stateless preview применения промокода к тарифу. Не создаёт бронь.", "applyPromocode", payload, false, { bookHash: identifierSchema("Непрозрачный bookHash из ответа Hotels API.") }),
  mutationTool("tbank_hotels_execute_apply_promocode", "Применяет промокод к тарифу после непосредственного явного подтверждения пользователя.", "applyPromocode", payload, true, { bookHash: identifierSchema("Непрозрачный bookHash из ответа Hotels API.") }),
  mutationTool("tbank_hotels_prepare_update_extra_services", "Создаёт stateless preview изменения дополнительных услуг тарифа.", "extraServices", payload, false, { bookHash: identifierSchema("Непрозрачный bookHash из ответа Hotels API.") }),
  mutationTool("tbank_hotels_execute_update_extra_services", "Изменяет дополнительные услуги тарифа после непосредственного явного подтверждения пользователя.", "extraServices", payload, true, { bookHash: identifierSchema("Непрозрачный bookHash из ответа Hotels API.") }),
];

function languageSchema() {
  return { type: "string", minLength: 2, maxLength: 35, description: "Значение заголовка X-User-Language, например ru-RU." };
}

function identifierSchema(description) {
  return { type: "string", minLength: 1, maxLength: 512, description };
}

function apiVersionSchema() {
  return { type: "string", enum: ["v1", "v2"], default: "v1" };
}

function rateApiVersionSchema() {
  return { type: "string", enum: ["v2", "v3"], default: "v3" };
}

function bookingApiVersionSchema() {
  return { type: "string", enum: ["v1", "v2", "v3"], default: "v3" };
}

function seoApiVersionSchema() {
  return { type: "string", enum: ["v1", "v2", "v3"], default: "v3" };
}

function mutationTool(name, description, action, actionPayload, execute = false, extraProperties = {}) {
  const properties = {
    ...extraProperties,
    payload: actionPayload,
  };
  if (execute) {
    properties.preparedRequestHash = { type: "string", pattern: "^[a-f0-9]{64}$", description: "requestHash из соответствующего prepare-вызова." };
    properties.confirmation = { type: "string", description: "Точная фраза подтверждения из соответствующего prepare-вызова после явного согласия пользователя." };
  }
  return {
    name,
    description,
    inputSchema: objectSchema(properties, [...Object.keys(extraProperties), "payload", ...(execute ? ["preparedRequestHash", "confirmation"] : [])]),
    _action: action,
    _execute: execute,
  };
}

function configuredHeaders() {
  const rawHeaders = process.env.TBANK_HOTELS_AUTH_HEADERS_JSON;
  const token = process.env.TBANK_HOTELS_AUTH_TOKEN;
  const header = process.env.TBANK_HOTELS_AUTH_HEADER;
  if (rawHeaders && (token || header)) throw new Error("Configure either TBANK_HOTELS_AUTH_HEADERS_JSON or TBANK_HOTELS_AUTH_TOKEN with TBANK_HOTELS_AUTH_HEADER, not both.");
  if (rawHeaders) {
    let headers;
    try { headers = JSON.parse(rawHeaders); } catch { throw new Error("TBANK_HOTELS_AUTH_HEADERS_JSON must contain a JSON object."); }
    if (!headers || Array.isArray(headers) || typeof headers !== "object") throw new Error("TBANK_HOTELS_AUTH_HEADERS_JSON must contain a JSON object.");
    return validateHeaders(headers);
  }
  if (!token && !header) return {};
  if (!token) throw new Error("TBANK_HOTELS_AUTH_HEADER requires TBANK_HOTELS_AUTH_TOKEN.");
  const resolvedHeader = header ?? "Authorization";
  if (!AUTH_HEADER_NAME.test(resolvedHeader)) throw new Error("TBANK_HOTELS_AUTH_HEADER contains an invalid header name.");
  const prefix = process.env.TBANK_HOTELS_AUTH_PREFIX ?? "Bearer ";
  return { [resolvedHeader]: `${prefix}${token}` };
}

function validateHeaders(headers) {
  const result = {};
  for (const [name, value] of Object.entries(headers)) {
    if (!AUTH_HEADER_NAME.test(name) || typeof value !== "string" || !value) throw new Error("Auth headers must have valid names and non-empty string values.");
    result[name] = value;
  }
  return result;
}

function baseUrl() {
  const configured = process.env.TBANK_HOTELS_API_BASE_URL;
  if (!configured) throw new Error("TBANK_HOTELS_API_BASE_URL is required. The supplied contracts do not declare an absolute server URL.");
  let url;
  try { url = new URL(configured); } catch { throw new Error("TBANK_HOTELS_API_BASE_URL must be an absolute URL."); }
  const localHttp = url.protocol === "http:" && ["localhost", "127.0.0.1", "::1"].includes(url.hostname);
  if (url.protocol !== "https:" && !localHttp) throw new Error("TBANK_HOTELS_API_BASE_URL must use HTTPS outside localhost.");
  return url;
}

function timeoutMs() {
  const configured = process.env.TBANK_HOTELS_TIMEOUT_MS;
  if (!configured) return DEFAULT_TIMEOUT_MS;
  const value = Number(configured);
  if (!Number.isInteger(value) || value < 1_000 || value > MAX_TIMEOUT_MS) throw new Error(`TBANK_HOTELS_TIMEOUT_MS must be an integer from 1000 to ${MAX_TIMEOUT_MS}.`);
  return value;
}

function connectionStatus() {
  const hasBaseUrl = Boolean(process.env.TBANK_HOTELS_API_BASE_URL);
  const hasAuth = Boolean(process.env.TBANK_HOTELS_AUTH_HEADERS_JSON || process.env.TBANK_HOTELS_AUTH_TOKEN);
  return {
    transport: hasBaseUrl ? "configured" : "not_configured",
    authentication: hasAuth ? "configured" : "not_configured",
    browserDependency: false,
    storedUserSession: false,
    note: "Значения URL, токенов и auth-заголовков намеренно не раскрываются.",
  };
}

function value(value, name) {
  if (typeof value !== "string" || !value.trim()) throw new Error(`${name} must be a non-empty string.`);
  if (value.length > 512 || value.includes("/") || value.includes("?")) throw new Error(`${name} contains unsupported path characters.`);
  return encodeURIComponent(value);
}

function requestObject(value, name = "payload") {
  if (!value || Array.isArray(value) || typeof value !== "object") throw new Error(`${name} must be an object.`);
  return value;
}

function requestHash(action, path, args) {
  const material = JSON.stringify({ action, path, payload: args.payload, orderId: args.orderId, bookHash: args.bookHash });
  return createHash("sha256").update(material).digest("hex");
}

function confirmationPhrase(action, hash) {
  return `CONFIRM_TBANK_HOTELS_${action.toUpperCase()}_${hash.slice(0, 12)}`;
}

function mutationPath(action, args) {
  switch (action) {
    case "booking": return "/api/v1/hotels/bookings/tasks/create";
    case "lsBooking": return "/api/v1/hotels/bookings/ls/tasks/create";
    case "cancel": return "/api/v1/hotels/bookings/cancel";
    case "paymentSetup": return `/api/v1/hotels/bookings/shevo/${value(args.orderId, "orderId")}/payment/setup`;
    case "applyPromocode": return `/api/v1/hotels/rates/${value(args.bookHash, "bookHash")}/promocode`;
    case "extraServices": return `/api/v1/hotels/rates/${value(args.bookHash, "bookHash")}/extraServices`;
    default: throw new Error("Unsupported mutation.");
  }
}

function redactPreview(payloadValue) {
  if (Array.isArray(payloadValue)) return payloadValue.map(redactPreview);
  if (!payloadValue || typeof payloadValue !== "object") return payloadValue;
  const secretNames = /password|token|authorization|card|pan|cvv|cvc|phone|email|passport|birth|document/i;
  return Object.fromEntries(Object.entries(payloadValue).map(([key, item]) => [key, secretNames.test(key) ? "[REDACTED]" : redactPreview(item)]));
}

async function apiRequest(method, path, { payload: body, query, language } = {}) {
  const origin = baseUrl();
  const target = new URL(path.replace(/^\//, ""), `${origin.href.replace(/\/$/, "")}/`);
  if (query) {
    requestObject(query, "query");
    for (const [key, item] of Object.entries(query)) {
      if (item === undefined || item === null) continue;
      if (typeof item === "object") throw new Error("query values must be scalar.");
      target.searchParams.set(key, String(item));
    }
  }
  if (body !== undefined) requestObject(body);
  const headers = { Accept: "application/json", ...configuredHeaders() };
  if (language) {
    if (typeof language !== "string" || language.length < 2 || language.length > 35) throw new Error("language must be a string from 2 to 35 characters.");
    headers["X-User-Language"] = language;
  }
  if (body !== undefined) headers["Content-Type"] = "application/json";
  let response;
  try {
    response = await fetch(target, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(timeoutMs()) });
  } catch (error) {
    throw new Error(error.name === "TimeoutError" ? "Hotels API request timed out." : "Unable to reach Hotels API.");
  }
  const responseText = await response.text();
  let responseBody = null;
  if (responseText) {
    try { responseBody = JSON.parse(responseText); } catch { responseBody = responseText; }
  }
  if (!response.ok) throw new Error(`Hotels API returned HTTP ${response.status}.`);
  return { status: response.status, data: responseBody };
}

function version(args, fallback, allowed) {
  const selected = args.apiVersion ?? fallback;
  if (!allowed.includes(selected)) throw new Error(`apiVersion must be one of: ${allowed.join(", ")}.`);
  return selected;
}

async function callTool(name, args = {}) {
  if (!args || typeof args !== "object" || Array.isArray(args)) throw new Error("Tool arguments must be an object.");
  const mutation = tools.find((tool) => tool.name === name && tool._action);
  if (mutation) return callMutation(mutation, args);
  switch (name) {
    case "tbank_hotels_connection_status": return connectionStatus();
    case "tbank_hotels_get_customer": return apiRequest("GET", "/api/v1/auth/customerdata");
    case "tbank_hotels_search": return apiRequest("POST", "/api/v1/hotels/search", args);
    case "tbank_hotels_get_search_filters": return apiRequest("GET", `/api/${version(args, "v1", ["v1", "v2"])}/hotels/search-filters`);
    case "tbank_hotels_get_filter_availability": return apiRequest("POST", "/api/v1/hotels/search-filters-availability", args);
    case "tbank_hotels_search_map": return apiRequest("POST", "/api/v1/hotels/map/search", args);
    case "tbank_hotels_get_map_hotels": return apiRequest("POST", "/api/v1/hotels/map/hotels", args);
    case "tbank_hotels_search_points_of_interest": {
      if (!["search", "landmarks", "groups"].includes(args.mode)) throw new Error("mode must be search, landmarks, or groups.");
      return apiRequest("POST", `/api/v1/points_of_interest/${args.mode}`, args);
    }
    case "tbank_hotels_get_hotel": return apiRequest("GET", `/api/v1/hotels/${value(args.hotelId, "hotelId")}`, args);
    case "tbank_hotels_get_hotel_rates": { const v = version(args, "v3", ["v2", "v3"]); return apiRequest("POST", `/api/${v}/hotels/${value(args.hotelId, "hotelId")}/rates`, args); }
    case "tbank_hotels_get_rate": { const v = version(args, "v3", ["v2", "v3"]); return apiRequest("GET", `/api/${v}/rates/${value(args.bookHash, "bookHash")}`, args); }
    case "tbank_hotels_get_cashback_percent": return apiRequest("GET", `/api/v1/hotels/cashback/percent-by-account/${value(args.bookHash, "bookHash")}`);
    case "tbank_hotels_get_max_cashback": return apiRequest("GET", "/api/v1/hotels/cashback/max-percent");
    case "tbank_hotels_validate_promocode": return apiRequest("POST", "/api/v1/hotels/promocodes/validate", args);
    case "tbank_hotels_get_rate_upgrade": return apiRequest("POST", `/api/v1/hotels/rates/${value(args.bookHash, "bookHash")}/upgrade`, args);
    case "tbank_hotels_get_booking": { const v = version(args, "v3", ["v1", "v2", "v3"]); return apiRequest("GET", `/api/${v}/hotels/bookings/${value(args.orderId, "orderId")}`, args); }
    case "tbank_hotels_list_bookings": return apiRequest("POST", "/api/v1/hotels/bookings/booking_list", args);
    case "tbank_hotels_get_voucher": return apiRequest("GET", `/api/v1/hotels/bookings/voucher/${value(args.orderId, "orderId")}`);
    case "tbank_hotels_get_reservation": return apiRequest("GET", "/api/v1/hotels/bookings/getReservation", args);
    case "tbank_hotels_get_evo_booking": return apiRequest("GET", `/api/v1/hotels/bookings/evo/${value(args.orderId, "orderId")}`);
    case "tbank_hotels_get_bnpl_offer": return apiRequest("POST", `/api/v1/hotels/bookings/evo/${value(args.orderId, "orderId")}/bnpl_offer`, args);
    case "tbank_hotels_get_booking_task_status": return apiRequest("GET", `/api/v1/hotels/bookings/tasks/${value(args.taskId, "taskId")}/status`);
    case "tbank_hotels_check_ls_order": return apiRequest("GET", `/api/v1/hotels/bookings/ls/check_orders/${value(args.orderId, "orderId")}`);
    case "tbank_hotels_get_reviews": {
      if (!["ratings", "summary", "feedback", "feedback-filters"].includes(args.resource)) throw new Error("resource is unsupported.");
      return apiRequest("GET", `/api/v1/review/${value(args.hotelId, "hotelId")}/${args.resource}`, args);
    }
    case "tbank_hotels_get_review_order_status": return apiRequest("GET", `/api/v1/review/order-status/${value(args.orderId, "orderId")}`);
    case "tbank_hotels_search_seo": return apiRequest("POST", `/api/${version(args, "v3", ["v1", "v2", "v3"])}/seo/search`, args);
    case "tbank_hotels_search_urls": return apiRequest("POST", `/api/${version(args, "v3", ["v1", "v2", "v3"])}/hotels/urls/search`, args);
    case "tbank_hotels_get_seo_resource": return seoResource(args);
    case "tbank_hotels_get_deeplink_token": {
      if (args.kind === "general") return apiRequest("GET", "/api/v1/get-link-token");
      if (args.kind === "hotels-urls") return apiRequest("GET", "/api/v1/hotels/urls/link-token");
      throw new Error("kind must be general or hotels-urls.");
    }
    case "tbank_hotels_get_available_tranche_amount": return apiRequest("POST", "/api/v1/tranches/available/amount", args);
    case "tbank_hotels_get_partner_redirect_url": return apiRequest("POST", `/api/v1/partners/${value(args.partnerAlias, "partnerAlias")}/redirectUrl`, args);
    default: throw new Error(`Unknown tool: ${name}`);
  }
}

function seoResource(args) {
  switch (args.resource) {
    case "hotel": return apiRequest("GET", `/api/v1/seo/hotels/${value(args.id, "id")}`, args);
    case "region": return apiRequest("GET", `/api/v1/seo/regions/${value(args.id, "id")}`, args);
    case "available-filters": return args.id ? apiRequest("GET", `/api/v1/seo/available-filters/${value(args.id, "id")}`, args) : apiRequest("GET", "/api/v1/seo/available-filters", args);
    case "locations": return apiRequest("GET", "/api/v1/seo/locations", args);
    case "location-by-slug": return apiRequest("GET", "/api/v1/seo/location-by-slug", args);
    case "rooms": return apiRequest("GET", `/api/v1/seo/rooms/${value(args.id, "id")}`, args);
    case "slug-by-hotel": return apiRequest("GET", `/api/v1/seo/slug-by-hotel/${value(args.id, "id")}`, args);
    default: throw new Error("resource is unsupported.");
  }
}

async function callMutation(tool, args) {
  requestObject(args.payload);
  const path = mutationPath(tool._action, args);
  const hash = requestHash(tool._action, path, args);
  const phrase = confirmationPhrase(tool._action, hash);
  if (!tool._execute) {
    return {
      action: tool._action,
      requestHash: hash,
      confirmation: phrase,
      endpoint: path,
      payloadPreview: redactPreview(args.payload),
      note: "HTTP-запрос не выполнен. Получите явное подтверждение пользователя непосредственно перед execute-вызовом.",
    };
  }
  if (args.preparedRequestHash !== hash) throw new Error("preparedRequestHash does not match this exact request. Prepare and review the action again.");
  if (args.confirmation !== phrase) throw new Error("confirmation must exactly match the phrase returned by the corresponding prepare call.");
  return apiRequest("POST", path, args);
}

function response(id, result) { return { jsonrpc: "2.0", id, result }; }
function error(id, code, message) { return { jsonrpc: "2.0", id, error: { code, message } }; }
function write(message) { process.stdout.write(`${JSON.stringify(message)}\n`); }

async function handle(request) {
  if (request.id === undefined) return;
  if (request.jsonrpc !== "2.0") return write(error(request.id ?? null, -32600, "Invalid JSON-RPC version."));
  if (request.method === "initialize") {
    return write(response(request.id, {
      protocolVersion: request.params?.protocolVersion ?? "2025-03-26",
      capabilities: { tools: { listChanged: false } },
      serverInfo: { name: SERVER_NAME, version: SERVER_VERSION },
      instructions: "API-driven T-Bank Hotels MCP. Configure the base URL and authentication only through environment variables. This server does not use a browser, cookies, local browser state, or stored user sessions. Calls that can create a booking, set up a payment, cancel a booking, apply a promocode, or update extra services require a stateless prepare/execute confirmation protocol.",
    }));
  }
  if (request.method === "tools/list") return write(response(request.id, { tools: tools.map(({ _action, _execute, ...tool }) => tool) }));
  if (request.method === "tools/call") {
    try { return write(response(request.id, { content: [text(await callTool(request.params?.name, request.params?.arguments))], isError: false })); }
    catch (toolError) { return write(response(request.id, { content: [text(toolError.message)], isError: true })); }
  }
  return write(error(request.id, -32601, "Method not found."));
}

const input = createInterface({ input: process.stdin, crlfDelay: Infinity });
input.on("line", (line) => { try { void handle(JSON.parse(line)); } catch { write(error(null, -32700, "Parse error.")); } });
