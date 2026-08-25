#!/usr/bin/env node

import { createHash, createPrivateKey, randomUUID, sign } from "node:crypto";
import { readFileSync, statSync } from "node:fs";
import { createConnection } from "node:net";
import { isAbsolute } from "node:path";
import { createInterface } from "node:readline";
import { fileURLToPath } from "node:url";

const SERVER_NAME = "tbank-hotels-api-mcp";
const SERVER_VERSION = "0.22.0";
const MCP_PROTOCOL_VERSION = "2025-03-26";
const DEFAULT_TIMEOUT_MS = 15_000;
const MAX_TIMEOUT_MS = 60_000;
const JOURNEY_TTL_MS = 60 * 60 * 1_000;
const BOOKING_DRAFT_TTL_MS = 60 * 60 * 1_000;
const CHECKOUT_VALIDATION_TTL_MS = 5 * 60 * 1_000;
const PREPARED_CONFIRMATION_TTL_MS = 5 * 60 * 1_000;
const AUTH_HEADER_NAME = /^[!#$%&'*+.^_`|~0-9A-Za-z-]+$/;
const SERVICE_JWT_REFRESH_MS = 30_000;
const LOCATION_CACHE_TTL_MS = 5 * 60 * 1_000;
const DEFAULT_PLAN_OPTIONS = 20;
const MAX_PLAN_OPTIONS = 50;
const MAX_ROOMS = 8;
const MAX_ACTIVE_JOURNEYS = 100;
const MAX_ACTIVE_BOOKING_DRAFTS = 100;
const MAX_ACTIVE_BOOKING_REFERENCES = 500;
const MAX_TRACKED_MUTATION_EXECUTIONS = 500;
const MAX_LOCATION_CACHES = 20;
const LOCATION_PAGE_SIZE = 100;
const MAX_LOCATION_PAGES = 50;
const LOCATION_COLLECTION_BUDGET_MS = 10_000;
const MAX_PROVIDER_RESPONSE_BYTES = 2 * 1_024 * 1_024;
const MAX_SERVICE_JWT_KEY_BYTES = 64 * 1_024;
const SEARCH_PAGE_SIZE = 50;
const MAX_SEARCH_REQUESTS = 20;
const MAX_SEARCH_LOADING_POLLS = 3;
const SEARCH_LOADING_POLL_DELAY_MS = 200;
const SEARCH_COLLECTION_BUDGET_MS = 11_000;
const MIN_SEARCH_REQUEST_BUDGET_MS = 1_000;
const DEFAULT_MAX_PROVIDER_CONCURRENCY = 2;
const MAX_PROVIDER_CONCURRENCY = 8;
const MAX_PROVIDER_REQUEST_QUEUE = 32;
const SEARCH_CACHE_TTL_MS = 30_000;
const MAX_SEARCH_CACHE_ENTRIES = 50;
const CHECKOUT_REQUEST_BUDGET_MS = 13_000;
const CHECKOUT_FIRST_ATTEMPT_MS = 8_000;
const RATES_REQUEST_BUDGET_MS = 13_000;
const RATES_FIRST_ATTEMPT_MS = 5_000;

const journeysById = new Map();
const bookingDraftsById = new Map();
const bookingReferencesById = new Map();
const mutationExecutionsByHash = new Map();
const hotelSearchCacheByKey = new Map();
const inFlightHotelSearchByKey = new Map();
let cachedServiceJwt;
const locationCatalogByCountry = new Map();
let authBrokerConnector = createConnection;
let activeProviderRequests = 0;
const providerRequestQueue = [];
let searchCacheTransport = globalThis.fetch;

export function setAuthBrokerConnectorForTests(connector) {
  authBrokerConnector = connector ?? createConnection;
}

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

const isoDate = {
  type: "string",
  format: "date",
  pattern: "^\\d{4}-\\d{2}-\\d{2}$",
  description: "Календарная дата в формате YYYY-MM-DD.",
};

const isoTimestamp = {
  type: "string",
  format: "date-time",
  description: "Timestamp from the corresponding prepare response; pass it back unchanged.",
};

const providerGuest = objectSchema({
  adultsCount: { type: "integer", minimum: 1, maximum: 16 },
  childrenAge: { type: "array", maxItems: 16, items: { type: "integer", minimum: 0, maximum: 17 } },
}, ["adultsCount"]);

const SEARCH_FILTER_IDS = [
  "accommodation_types",
  "chains",
  "free_cancellation_allowed",
  "payment_card_not_required",
  "meal_types",
  "payment_places",
  "price",
  "stars",
  "photos_available",
  "districts",
  "hotel_entertainments",
  "hotel_facilities",
  "room_facilities",
  "bed_types",
  "review_rating",
  "special_offer",
];

const searchFilterId = { type: "string", enum: SEARCH_FILTER_IDS };
const arraySearchFilter = objectSchema({
  $objectType: { type: "string", const: "array" },
  filterId: searchFilterId,
  values: { type: "array", items: { type: "string" } },
}, ["$objectType", "filterId", "values"]);
const booleanSearchFilter = objectSchema({
  $objectType: { type: "string", const: "boolean" },
  filterId: searchFilterId,
  value: { type: "boolean" },
}, ["$objectType", "filterId", "value"]);
const radioSearchFilter = objectSchema({
  $objectType: { type: "string", const: "radio" },
  filterId: searchFilterId,
  value: { type: "string", minLength: 1 },
  values: { anyOf: [{ type: "array", items: { type: "string" } }, { type: "null" }] },
}, ["$objectType", "filterId", "value"]);
const rangeSearchFilter = objectSchema({
  $objectType: { type: "string", const: "range" },
  filterId: searchFilterId,
  min: { type: "number" },
  max: { type: "number" },
}, ["$objectType", "filterId", "min", "max"]);
const providerSearchFilter = {
  oneOf: [arraySearchFilter, booleanSearchFilter, radioSearchFilter, rangeSearchFilter],
  description: "Строгий discriminator-контракт Hotels API. Не угадывайте форму фильтра: используйте $objectType и соответствующие поля.",
};

const providerSearchPayload = objectSchema({
  destinationId: { type: "integer", minimum: 1, description: "Provider destinationId, не SEO regionId." },
  checkinDate: isoDate,
  checkoutDate: isoDate,
  guests: { type: "array", minItems: 1, maxItems: MAX_ROOMS, items: providerGuest },
  filters: { type: "array", items: providerSearchFilter },
  sort: { type: "object", additionalProperties: true },
  offset: { type: "integer", minimum: 0 },
  limit: { type: "integer", minimum: 0 },
}, ["destinationId", "checkinDate", "checkoutDate", "guests"]);

const journeyRoom = objectSchema({
  adults: { type: "integer", minimum: 1, maximum: 16, description: "Количество взрослых в комнате." },
  childrenAges: { type: "array", maxItems: 16, items: { type: "integer", minimum: 0, maximum: 17 }, description: "Возраст каждого ребёнка, 0–17." },
}, ["adults"]);

const rankingStrategy = {
  type: "string",
  enum: ["provider_order", "lowest_price", "highest_rating"],
  default: "provider_order",
  description: "provider_order сохраняет порядок Hotels API; остальные стратегии сортируют только по доступным provider facts.",
};

const inheritedRankingStrategy = {
  type: "string",
  enum: ["provider_order", "lowest_price", "highest_rating"],
  description: "Если значение не передано, используется ranking исходного plan_stay.",
};

const planStayInput = {
  type: "object",
  properties: {
    destination: { type: "string", minLength: 1, maxLength: 200, description: "Название города или локации, например Москва. MCP сам разрешит destinationId." },
    destinationId: { type: "integer", minimum: 1, description: "Используйте только после выбора кандидата из resolve_destination или clarification_required." },
    countryName: { type: "string", minLength: 1, maxLength: 120, description: "Необязательное название страны для разрешения одноимённых локаций." },
    checkinDate: isoDate,
    checkoutDate: isoDate,
    rooms: { type: "array", minItems: 1, maxItems: MAX_ROOMS, items: journeyRoom, description: "Один элемент на комнату." },
    hotelName: { type: "string", minLength: 1, maxLength: 250, description: "Необязательное название конкретного отеля внутри выбранной локации. Глобальный поиск без локации не заявлен контрактом." },
    breakfastIncluded: { type: "boolean", default: false, description: "Если true, MCP применяет подтверждённый provider-фильтр meal_types=breakfast до построения journey. Не заменяйте этот параметр низкоуровневым перебором filters." },
    ranking: rankingStrategy,
    maxOptions: { type: "integer", minimum: 1, maximum: MAX_PLAN_OPTIONS, default: DEFAULT_PLAN_OPTIONS, description: "Ограничивает число вариантов в ответе plan_stay, но не размер собираемой MCP выборки." },
    language: languageSchema(),
  },
  required: ["checkinDate", "checkoutDate", "rooms"],
  anyOf: [{ required: ["destination"] }, { required: ["destinationId"] }],
  additionalProperties: false,
};

const seoSearchPayloadV1 = objectSchema({
  destinationId: { type: "integer", minimum: 1 },
  hostelIsNeeded: { type: "boolean" },
  guesthouseIsNeeded: { type: "boolean" },
}, ["destinationId"]);

const seoSearchPayloadV2 = objectSchema({
  locationId: { type: "integer", minimum: 1 },
  offset: { type: "integer", minimum: 0 },
  limit: { type: "integer", minimum: 1 },
  filter: { type: "object", additionalProperties: true },
}, ["locationId"]);

const seoSearchPayloadV3 = objectSchema({
  country: { type: "string", minLength: 1 },
  location: { type: "string", minLength: 1 },
  offset: { type: "integer", minimum: 0 },
  limit: { type: "integer", minimum: 1 },
  filter: { type: "object", additionalProperties: true },
}, ["country", "location"]);

const seoSearchInput = {
  type: "object",
  oneOf: [
    objectSchema({ apiVersion: { type: "string", const: "v1" }, payload: seoSearchPayloadV1 }, ["apiVersion", "payload"]),
    objectSchema({ apiVersion: { type: "string", const: "v2" }, payload: seoSearchPayloadV2 }, ["apiVersion", "payload"]),
    objectSchema({ apiVersion: { type: "string", const: "v3", default: "v3" }, payload: seoSearchPayloadV3 }, ["payload"]),
  ],
};

const nullableString = { anyOf: [{ type: "string" }, { type: "null" }] };
const bookingGuestContact = objectSchema({
  email: { type: "string", minLength: 1 },
  phone: { type: "string", minLength: 1 },
  comment: nullableString,
}, ["email", "phone"]);
const bookingGuest = objectSchema({
  firstName: { type: "string", minLength: 1 },
  lastName: { type: "string", minLength: 1 },
  childAge: { anyOf: [{ type: "integer" }, { type: "null" }] },
}, ["firstName", "lastName"]);
const bookingRoom = objectSchema({
  guests: { type: "array", items: bookingGuest },
}, ["guests"]);
const bookingPaymentData = objectSchema({ creditCardId: nullableString });
const bookingContactData = objectSchema({ firstName: { type: "string" }, lastName: { type: "string" }, email: nullableString }, ["firstName", "lastName"]);
const bookingArrivalTime = objectSchema({ type: nullableString, from: nullableString, to: nullableString });
const bookingExtraServices = objectSchema({
  earlyCheckInId: nullableString,
  lateCheckOutId: nullableString,
  guaranteedRefundSelected: { anyOf: [{ type: "boolean" }, { type: "null" }] },
});
const bookingUserData = objectSchema({ ssoId: nullableString, siebelId: nullableString, phoneNumber: nullableString });
const bookingBaseProperties = {
  bookHash: { type: "string", minLength: 1, description: "Хеш выбранного тарифа." },
  checkOutId: nullableString,
  guestContact: bookingGuestContact,
  rooms: { type: "array", items: bookingRoom },
  paymentData: { anyOf: [bookingPaymentData, { type: "null" }] },
  contactData: { anyOf: [bookingContactData, { type: "null" }] },
  arrivalTime: { anyOf: [bookingArrivalTime, { type: "null" }] },
  paymentMeans: { anyOf: [{ type: "string", enum: ["payment_form", "on_us", "off_us", "dolyame"] }, { type: "null" }] },
  promocode: nullableString,
  extraServices: { anyOf: [bookingExtraServices, { type: "null" }] },
};
const bookingPayload = {
  oneOf: [
    objectSchema(bookingBaseProperties, ["bookHash", "guestContact", "rooms"]),
    objectSchema({ ...bookingBaseProperties, userData: bookingUserData, userIp: { type: "string", minLength: 1 } }, ["bookHash", "guestContact", "rooms", "userData", "userIp"]),
  ],
  description: "CreateBookingsApiRequest или CreateBookingsWithTcsUserDataApiRequest.",
};
const bookingDraftBaseProperties = { ...bookingBaseProperties };
delete bookingDraftBaseProperties.bookHash;
const bookingDraftData = {
  oneOf: [
    objectSchema(bookingDraftBaseProperties, ["guestContact", "rooms"]),
    objectSchema({ ...bookingDraftBaseProperties, userData: bookingUserData, userIp: { type: "string", minLength: 1 } }, ["guestContact", "rooms", "userData", "userIp"]),
  ],
  description: "CreateBookingsApiRequest без bookHash; MCP связывает выбранный тариф самостоятельно.",
};
const lsBookingPayload = objectSchema({
  bookHash: bookingBaseProperties.bookHash,
  checkOutId: nullableString,
  guestContact: bookingGuestContact,
  rooms: { type: "array", items: bookingRoom },
  contactData: { anyOf: [bookingContactData, { type: "null" }] },
  arrivalTime: { anyOf: [bookingArrivalTime, { type: "null" }] },
  promocode: nullableString,
  extraServices: { anyOf: [bookingExtraServices, { type: "null" }] },
}, ["bookHash", "guestContact", "rooms"]);

const bookingsListInput = objectSchema({
  isActiveRequired: { type: "boolean", description: "Вернуть активные бронирования." },
  isCancelledRequired: { type: "boolean", description: "Вернуть отменённые бронирования за поддерживаемый provider период." },
  isCompletedRequired: { type: "boolean", description: "Provider-флаг выгрузки завершённых бронирований; семантика фиксируется Hotels API." },
}, ["isActiveRequired", "isCancelledRequired", "isCompletedRequired"]);

const trancheAccount = objectSchema({
  accountId: { type: "string", minLength: 1 },
  type: { type: "string", minLength: 1 },
  balance: { type: "number" },
}, ["accountId", "type", "balance"]);

const trancheAmountInput = objectSchema({
  accounts: { type: "array", maxItems: 100, items: trancheAccount },
}, ["accounts"]);

const tools = [
  {
    name: "tbank_hotels_connection_status",
    description: "Показывает раздельную готовность read-only поиска, customer reads и booking execution. Секреты, значения заголовков и токены никогда не возвращаются.",
    inputSchema: objectSchema({}),
  },
  {
    name: "tbank_hotels_get_customer",
    description: "Получает персональные данные текущего авторизованного клиента через GET /api/v1/auth/customerdata. При настроенном auth broker использует общую mobile session с live-подтверждённым Bearer-only профилем. Вызывайте только по явному пользовательскому запросу на автозаполнение.",
    inputSchema: objectSchema({}),
  },
  {
    name: "tbank_hotels_search",
    description: "Низкоуровневый поиск по подтверждённому provider destinationId. filters имеют строгий discriminator-контракт; не угадывайте и не перебирайте альтернативные формы. Для обычного запроса по городу или завтраку используйте tbank_hotels_plan_stay. При отказе filtered provider request tool намеренно возвращает структурированный success-результат со status=requirements_unavailable; клиент обязан проверять status, а не только isError.",
    inputSchema: objectSchema({ payload: providerSearchPayload, language: languageSchema() }, ["payload"]),
  },
  {
    name: "tbank_hotels_resolve_destination",
    description: "Разрешает название города или локации в provider destinationId через каталог SEO locations. Не выполняет поиск отелей.",
    inputSchema: objectSchema({
      query: { type: "string", minLength: 1, maxLength: 200, description: "Название, например Москва или Moscow." },
      countryName: { type: "string", minLength: 1, maxLength: 120 },
      maxCandidates: { type: "integer", minimum: 1, maximum: 10, default: 5 },
    }, ["query"]),
  },
  {
    name: "tbank_hotels_plan_stay",
    description: "Основной agent-facing поиск. Принимает название локации, даты, комнаты и semantic breakfastIncluded, сам разрешает destinationId, применяет обязательные условия до поиска, собирает bounded paginated/partial provider results и создаёт short-lived journeyId. Не вызывайте get_search_filters или low-level search для завтрака. Ranking применяется локально: production search пока отклоняет provider sort. Для конкретного отеля передайте hotelName вместе с локацией.",
    inputSchema: planStayInput,
  },
  {
    name: "tbank_hotels_get_stay_options",
    description: "Возвращает нормализованные provider facts вариантов из journey-контекста без provider hotelId. Если ranking не передан, наследует ranking исходного plan_stay.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId из tbank_hotels_plan_stay."), ranking: inheritedRankingStrategy, limit: { type: "integer", minimum: 1, maximum: MAX_PLAN_OPTIONS, default: DEFAULT_PLAN_OPTIONS } }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_compare_stay_options",
    description: "Сравнивает 2–5 вариантов по provider facts внутри той же отфильтрованной journey-выборки и возвращает готовые comparisonRows и comparisonTableMarkdown. В пользовательском ответе покажите comparisonTableMarkdown целиком: не удаляйте колонки, не округляйте и не изменяйте ratingsCount/price/cancellation facts. Не подмешивайте другие journey-варианты без запроса пользователя. Для запроса лучших вариантов передайте ranking и не выбирайте optionIds вручную: ranking применяется ко всей journey-выборке. optionIds предназначены только для явно названных пользователем вариантов. Без ranking наследуется стратегия plan_stay.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId."), optionIds: { type: "array", minItems: 2, maxItems: 5, items: identifierSchema("optionId из journey."), description: "Только явно выбранные пользователем варианты. Не используйте для top/best: передайте ranking." }, ranking: inheritedRankingStrategy, limit: { type: "integer", minimum: 2, maximum: 5, default: 5 } }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_select_stay_option",
    description: "Выбирает один вариант в journey-контексте. Не резервирует номер и не выполняет HTTP write-запрос.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId."), optionId: identifierSchema("optionId из journey.") }, ["journeyId", "optionId"]),
  },
  {
    name: "tbank_hotels_get_selected_stay_rates",
    description: "Загружает бронируемые тарифы выбранного journey-варианта. hotelId, даты и гости берутся из journey. filters — неподтверждённый untyped pass-through rates-контракт: не угадывайте его и не передавайте без точных provider-данных. Один timeout повторяется внутри общего бюджета. После исчерпания бюджета tool возвращает rates_temporarily_unavailable и запрещает автоматический повтор. Если provider вернул пустой rates, tool возвращает no_bookable_rates и запрещает запрашивать guest PII или создавать draft по search-feed цене.",
    inputSchema: objectSchema({
      journeyId: identifierSchema("Непрозрачный journeyId."),
      filters: { type: "array", items: { type: "object", additionalProperties: true }, description: "Неподтверждённый untyped pass-through для rates endpoint. Не угадывайте форму; поле следует опускать без точного provider-контракта." },
      apiVersion: rateApiVersionSchema(),
      language: languageSchema(),
    }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_select_stay_rate",
    description: "Выбирает конкретный тариф ранее выбранного отеля. Не создаёт бронирование.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId."), rateOptionId: identifierSchema("rateOptionId из tbank_hotels_get_selected_stay_rates.") }, ["journeyId", "rateOptionId"]),
  },
  {
    name: "tbank_hotels_create_booking_preview",
    description: "Создаёт безопасный локальный preview выбранного отеля, тарифа и состава гостей без ФИО, email и телефона. Не создаёт booking draft, не вызывает Hotels API и не запрашивает PII. Используйте для preview-only сценария, особенно когда executionAvailable=false.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId с выбранным rateOptionId.") }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_create_booking_draft",
    description: "Создаёт черновик только для явно намеренного реального бронирования и поэтому принимает guest PII. Для безопасного preview без оформления используйте tbank_hotels_create_booking_preview. Достаточность имён всех проживающих OpenAPI не подтверждает, поэтому MCP возвращает guestCoverage без выдумывания требования. HTTP create-запрос не выполняется; executionAvailable учитывает activation, auth profile и обязательные доверенные headers.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId."), bookingData: bookingDraftData }, ["journeyId", "bookingData"]),
  },
  {
    name: "tbank_hotels_validate_checkout",
    description: "Перед бронированием повторно получает checkout rate черновика. Один timeout безопасно повторяется внутри tool; успешная проверка действует 5 минут.",
    inputSchema: objectSchema({ bookingDraftId: identifierSchema("Непрозрачный bookingDraftId.") }, ["bookingDraftId"]),
  },
  {
    name: "tbank_hotels_prepare_draft_booking",
    description: "Подготавливает финальное подтверждение после свежей проверки checkout. Если booking execution profile не готов, возвращает только preview с executionAvailable=false и не выдаёт confirmation/hash — не просите пользователя подтверждать невозможное действие.",
    inputSchema: objectSchema({ bookingDraftId: identifierSchema("Непрозрачный bookingDraftId.") }, ["bookingDraftId"]),
  },
  {
    name: "tbank_hotels_confirm_booking",
    description: "Создаёт бронь черновика только после непосредственного явного подтверждения пользователя и при готовом доверенном execution profile. Данные карты и OTP не принимаются.",
    inputSchema: objectSchema({ bookingDraftId: identifierSchema("Непрозрачный bookingDraftId."), preparedRequestHash: { type: "string", pattern: "^[a-f0-9]{64}$" }, confirmation: { type: "string" }, preparedAt: isoTimestamp, expiresAt: isoTimestamp }, ["bookingDraftId", "preparedRequestHash", "confirmation", "preparedAt", "expiresAt"]),
  },
  {
    name: "tbank_hotels_get_booking_overview",
    description: "Возвращает карточку заказа. PDF voucher никогда не встраивается в MCP-ответ; при includeVoucher=true возвращается только указание использовать отдельный безопасный local handoff.",
    inputSchema: objectSchema({ orderId: identifierSchema("Идентификатор заказа."), includeVoucher: { type: "boolean", default: false }, apiVersion: bookingApiVersionSchema() }, ["orderId"]),
  },
  {
    name: "tbank_hotels_preview_cancellation",
    description: "Загружает текущие provider-данные брони для просмотра условий отмены. Не рассчитывает сумму возврата и не отменяет заказ.",
    inputSchema: objectSchema({ orderId: identifierSchema("Идентификатор заказа."), apiVersion: bookingApiVersionSchema() }, ["orderId"]),
  },
  {
    name: "tbank_hotels_repeat_stay_plan",
    description: "Запускает новый поиск по параметрам текущего journey с новыми датами, не меняя предыдущий контекст.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId."), checkinDate: isoDate, checkoutDate: isoDate }, ["journeyId", "checkinDate", "checkoutDate"]),
  },
  {
    name: "tbank_hotels_get_search_filters",
    description: "Возвращает каталог доступных фильтров Hotels API.",
    inputSchema: objectSchema({ apiVersion: apiVersionSchema() }),
  },
  {
    name: "tbank_hotels_get_filter_availability",
    description: "Возвращает доступность фильтров для точных параметров поиска. filters имеют строгий discriminator-контракт; не угадывайте альтернативные формы. Для semantic breakfast-запроса используйте plan_stay, а не этот low-level tool. При отказе filtered provider request tool намеренно возвращает структурированный success-результат со status=requirements_unavailable; клиент обязан проверять status, а не только isError.",
    inputSchema: objectSchema({ payload: providerSearchPayload, language: languageSchema() }, ["payload"]),
  },
  {
    name: "tbank_hotels_search_map",
    description: "Возвращает поисковые данные для карты отелей. payload соответствует SearchParametersListApiRequest.",
    inputSchema: objectSchema({ payload: providerSearchPayload, language: languageSchema() }, ["payload"]),
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
    description: "Получает существующую бронь. В mobile broker-режиме используйте process-local bookingRef из tbank_hotels_list_bookings: provider orderId не попадает в tool arguments или ответ. Для прямого API-профиля передайте orderId.",
    inputSchema: {
      ...objectSchema({
        bookingRef: { type: "string", pattern: "^booking_[a-f0-9]{24}$", description: "Непрозрачный process-local bookingRef из tbank_hotels_list_bookings." },
        orderId: identifierSchema("Provider orderId только для прямого API-профиля без mobile auth broker."),
        apiVersion: bookingApiVersionSchema(),
        language: languageSchema(),
      }),
      anyOf: [{ required: ["bookingRef"] }, { required: ["orderId"] }],
    },
  },
  {
    name: "tbank_hotels_list_bookings",
    description: "Возвращает детали выбранных категорий броней текущего авторизованного клиента. Ответ содержит личную историю поездок; не используйте этот tool для краткой сводки без деталей — вызывайте tbank_hotels_summarize_bookings. В mobile broker-режиме provider orderId заменяется на process-local bookingRef для последующего tbank_hotels_get_booking; raw orderId не раскрывается модели.",
    inputSchema: bookingsListInput,
  },
  {
    name: "tbank_hotels_summarize_bookings",
    description: "Privacy-first сводка собственных бронирований: возвращает только количества активных, отменённых и завершённых записей. Не возвращает отели, города, даты, стоимость, гостей, bookingRef или provider identifiers. Используйте для просьб «кратко», «без личных данных» или «сколько у меня броней».",
    inputSchema: objectSchema({}),
  },
  {
    name: "tbank_hotels_get_voucher",
    description: "Отключённый legacy-вызов: binary voucher нельзя помещать в MCP JSON или контекст модели. Используйте tbank_hotels_save_voucher с bookingRef.",
    inputSchema: objectSchema({ orderId: identifierSchema("Идентификатор заказа.") }, ["orderId"]),
  },
  {
    name: "tbank_hotels_save_voucher",
    description: "По явному запросу пользователя безопасно сохраняет PDF voucher собственной брони через local auth broker. Принимает только непрозрачный bookingRef, возвращает путь и метаданные; PDF, provider orderId, PII и credentials не попадают в MCP JSON. Файл owner-only и автоматически удаляется по TTL.",
    inputSchema: objectSchema({
      bookingRef: { type: "string", pattern: "^booking_[a-f0-9]{24}$", description: "Непрозрачный process-local bookingRef из tbank_hotels_list_bookings." },
    }, ["bookingRef"]),
  },
  {
    name: "tbank_hotels_create_payment_handoff_preview",
    description: "Создаёт через общий local auth broker одноразовый короткоживущий paymentHandoffRef для собственной брони. Broker выполняет один read booking v1 и связывает наблюдаемые paymentPrice и raw paymentStatus; status не интерпретируется как разрешение оплаты. Capability поглощается при первом Banking preview. Не возвращает provider orderId/paymentToken и не выполняет payment setup или оплату.",
    inputSchema: objectSchema({
      bookingRef: { type: "string", pattern: "^booking_[a-f0-9]{24}$", description: "Непрозрачный process-local bookingRef из tbank_hotels_list_bookings." },
    }, ["bookingRef"]),
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
    inputSchema: objectSchema({ orderId: identifierSchema("Идентификатор заказа."), language: languageSchema() }, ["orderId"]),
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
    description: "Выполняет versioned SEO hotel search: v1 использует destinationId, v2 — locationId, v3 — country и location. Название города в provider ID разрешайте через resolve_destination.",
    inputSchema: seoSearchInput,
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
    description: "Проверяет доступную сумму рассрочки по счетам без знания provider DTO. Не подключает рассрочку и не создаёт платёж.",
    inputSchema: trancheAmountInput,
  },
  {
    name: "tbank_hotels_get_partner_redirect_url",
    description: "Получает redirect URL партнёра по partnerAlias. Сам переход и внешняя покупка не выполняются.",
    inputSchema: objectSchema({ partnerAlias: identifierSchema("Псевдоним партнёра."), payload }, ["partnerAlias", "payload"]),
  },
  mutationTool("tbank_hotels_prepare_booking", "Создаёт stateless preview создания брони. Не делает HTTP-запрос и не резервирует номер.", "booking", bookingPayload),
  mutationTool("tbank_hotels_execute_booking", "Создаёт задачу бронирования только после непосредственного явного подтверждения пользователя.", "booking", bookingPayload, true),
  mutationTool("tbank_hotels_prepare_ls_booking", "Создаёт stateless preview создания LS-брони. Не делает HTTP-запрос и не резервирует номер.", "lsBooking", lsBookingPayload),
  mutationTool("tbank_hotels_execute_ls_booking", "Создаёт LS-задачу бронирования только после непосредственного явного подтверждения пользователя.", "lsBooking", lsBookingPayload, true),
  mutationTool("tbank_hotels_prepare_cancel_booking", "Создаёт stateless preview отмены брони. Не отменяет заказ.", "cancel", null, false, { orderId: identifierSchema("Идентификатор заказа.") }),
  mutationTool("tbank_hotels_execute_cancel_booking", "Отменяет бронь только после непосредственного явного подтверждения пользователя.", "cancel", null, true, { orderId: identifierSchema("Идентификатор заказа.") }),
  mutationTool("tbank_hotels_prepare_payment_setup", "Создаёт stateless preview подготовки оплаты без request body. Не создаёт платёж и не списывает средства.", "paymentSetup", null, false, { orderId: identifierSchema("Идентификатор заказа.") }),
  mutationTool("tbank_hotels_execute_payment_setup", "Запрашивает provider payment token без request body только после непосредственного явного подтверждения пользователя. MCP не принимает данные карты.", "paymentSetup", null, true, { orderId: identifierSchema("Идентификатор заказа.") }),
  mutationTool("tbank_hotels_prepare_apply_promocode", "Создаёт stateless preview применения или удаления промокода у тарифа. Не создаёт бронь.", "applyPromocode", null, false, { bookHash: identifierSchema("Непрозрачный bookHash из ответа Hotels API."), promocode: { anyOf: [{ type: "string" }, { type: "null" }], description: "Промокод или null для снятия промокода, если это поддерживает provider." } }),
  mutationTool("tbank_hotels_execute_apply_promocode", "Применяет или удаляет промокод у тарифа после непосредственного явного подтверждения пользователя.", "applyPromocode", null, true, { bookHash: identifierSchema("Непрозрачный bookHash из ответа Hotels API."), promocode: { anyOf: [{ type: "string" }, { type: "null" }] } }),
  mutationTool("tbank_hotels_prepare_update_extra_services", "Создаёт stateless preview замены списка дополнительных услуг тарифа.", "extraServices", null, false, { bookHash: identifierSchema("Непрозрачный bookHash из ответа Hotels API."), extraServiceIds: { type: "array", items: { type: "string", minLength: 1 } } }),
  mutationTool("tbank_hotels_execute_update_extra_services", "Заменяет список дополнительных услуг тарифа после непосредственного явного подтверждения пользователя.", "extraServices", null, true, { bookHash: identifierSchema("Непрозрачный bookHash из ответа Hotels API."), extraServiceIds: { type: "array", items: { type: "string", minLength: 1 } } }),
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
  };
  const hasPayload = actionPayload !== null;
  if (hasPayload) properties.payload = actionPayload;
  if (execute) {
    properties.preparedRequestHash = { type: "string", pattern: "^[a-f0-9]{64}$", description: "requestHash из соответствующего prepare-вызова." };
    properties.confirmation = { type: "string", description: "Точная фраза подтверждения из соответствующего prepare-вызова после явного согласия пользователя." };
    properties.preparedAt = isoTimestamp;
    properties.expiresAt = isoTimestamp;
  }
  return {
    name,
    description: execute ? `${description} HTTP write требует готового доверенного execution profile.` : `${description} Если execution profile не готов, возвращается preview_only без confirmation/hash.`,
    inputSchema: objectSchema(properties, [...Object.keys(extraProperties), ...(hasPayload ? ["payload"] : []), ...(execute ? ["preparedRequestHash", "confirmation", "preparedAt", "expiresAt"] : [])]),
    _action: action,
    _execute: execute,
    _hasPayload: hasPayload,
  };
}

function configuredHeaders() {
  const rawHeaders = process.env.TBANK_HOTELS_AUTH_HEADERS_JSON;
  const token = process.env.TBANK_HOTELS_AUTH_TOKEN;
  const header = process.env.TBANK_HOTELS_AUTH_HEADER;
  const serviceJwtKey = serviceJwtConfigured();
  if (rawHeaders && (token || header || serviceJwtKey)) throw new Error("Configure exactly one auth profile: TBANK_HOTELS_AUTH_HEADERS_JSON, TBANK_HOTELS_AUTH_TOKEN, or TBANK_HOTELS_JWT_PRIVATE_KEY.");
  if (serviceJwtKey && (token || header)) throw new Error("Configure either TBANK_HOTELS_AUTH_TOKEN or TBANK_HOTELS_JWT_PRIVATE_KEY, not both.");
  if (rawHeaders) {
    let headers;
    try { headers = JSON.parse(rawHeaders); } catch { throw new Error("TBANK_HOTELS_AUTH_HEADERS_JSON must contain a JSON object."); }
    if (!headers || Array.isArray(headers) || typeof headers !== "object") throw new Error("TBANK_HOTELS_AUTH_HEADERS_JSON must contain a JSON object.");
    return validateHeaders(headers);
  }
  if (serviceJwtKey) {
    const resolvedHeader = process.env.TBANK_HOTELS_JWT_AUTH_HEADER ?? "Authorization";
    if (!AUTH_HEADER_NAME.test(resolvedHeader)) throw new Error("TBANK_HOTELS_JWT_AUTH_HEADER contains an invalid header name.");
    // HotelsApiPrivate's Go client concatenates the prefix and JWT directly.
    const prefix = process.env.TBANK_HOTELS_JWT_AUTH_PREFIX ?? "Bearer";
    return { [resolvedHeader]: `${prefix}${serviceJwtSignature()}` };
  }
  if (!token && !header) return {};
  if (!token) throw new Error("TBANK_HOTELS_AUTH_HEADER requires TBANK_HOTELS_AUTH_TOKEN.");
  const resolvedHeader = header ?? "Authorization";
  if (!AUTH_HEADER_NAME.test(resolvedHeader)) throw new Error("TBANK_HOTELS_AUTH_HEADER contains an invalid header name.");
  const prefix = process.env.TBANK_HOTELS_AUTH_PREFIX ?? "Bearer ";
  return { [resolvedHeader]: `${prefix}${token}` };
}

function serviceJwtSignature() {
  const now = Date.now();
  const issuer = requiredAuthSetting("TBANK_HOTELS_JWT_ISSUER");
  const audience = requiredAuthSetting("TBANK_HOTELS_JWT_AUDIENCE");
  const audiences = audience.split(",").map((value) => value.trim()).filter(Boolean);
  if (!audiences.length) throw new Error("TBANK_HOTELS_JWT_AUDIENCE must contain at least one audience.");
  const pem = normalizedServiceJwtPrivateKey(serviceJwtPrivateKeyMaterial());
  const fingerprint = createHash("sha256").update(`${issuer}\u0000${audiences.join(",")}\u0000${pem}`).digest("hex");
  if (cachedServiceJwt && cachedServiceJwt.fingerprint === fingerprint && cachedServiceJwt.expiresAt > now) return cachedServiceJwt.value;
  const header = base64UrlJson({ alg: "RS384", typ: "JWT" });
  const claims = base64UrlJson({
    iss: issuer,
    aud: audiences,
    iat: Math.floor(now / 1_000),
  });
  const signingInput = `${header}.${claims}`;
  let signature;
  try {
    signature = sign("RSA-SHA384", Buffer.from(signingInput), createPrivateKey(pem)).toString("base64url");
  } catch {
    throw new Error("Unable to create Hotels service JWT from the configured private key.");
  }
  const value = `${signingInput}.${signature}`;
  cachedServiceJwt = { value, fingerprint, expiresAt: now + SERVICE_JWT_REFRESH_MS };
  return value;
}

function requiredAuthSetting(name) {
  const value = process.env[name];
  if (!value || !value.trim()) throw new Error(`${name} is required when service JWT authentication is configured.`);
  return value.trim();
}

function serviceJwtConfigured() {
  return Boolean(process.env.TBANK_HOTELS_JWT_PRIVATE_KEY || process.env.TBANK_HOTELS_JWT_PRIVATE_KEY_FILE);
}

function serviceJwtPrivateKeyMaterial() {
  const inlineKey = process.env.TBANK_HOTELS_JWT_PRIVATE_KEY;
  const keyFile = process.env.TBANK_HOTELS_JWT_PRIVATE_KEY_FILE;
  if (inlineKey && keyFile) throw new Error("Configure either TBANK_HOTELS_JWT_PRIVATE_KEY or TBANK_HOTELS_JWT_PRIVATE_KEY_FILE, not both.");
  if (inlineKey) return inlineKey;
  if (!keyFile || !keyFile.trim()) throw new Error("A service JWT private key is required.");
  const resolvedPath = keyFile.trim();
  if (!isAbsolute(resolvedPath)) throw new Error("TBANK_HOTELS_JWT_PRIVATE_KEY_FILE must be an absolute path.");
  let metadata;
  try { metadata = statSync(resolvedPath); } catch { throw new Error("Unable to read the configured service JWT private key file."); }
  if (!metadata.isFile() || metadata.size < 1 || metadata.size > MAX_SERVICE_JWT_KEY_BYTES) throw new Error("The configured service JWT private key file is invalid or too large.");
  if (process.platform !== "win32" && (metadata.mode & 0o077) !== 0) {
    throw new Error("The configured service JWT private key file must be owner-only (mode 0600 or stricter).");
  }
  try { return readFileSync(resolvedPath, "utf8"); } catch { throw new Error("Unable to read the configured service JWT private key file."); }
}

function normalizedServiceJwtPrivateKey(material) {
  const key = String(material ?? "").replace(/\\n/g, "\n").trim();
  if (!key) throw new Error("The configured service JWT private key is empty.");
  if (key.includes("-----BEGIN")) return key;
  return `-----BEGIN RSA PRIVATE KEY-----\n${key}\n-----END RSA PRIVATE KEY-----`;
}

function base64UrlJson(value) {
  return Buffer.from(JSON.stringify(value)).toString("base64url");
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

function maxProviderConcurrency() {
  const configured = process.env.TBANK_HOTELS_MAX_CONCURRENT_REQUESTS;
  if (!configured) return DEFAULT_MAX_PROVIDER_CONCURRENCY;
  const value = Number(configured);
  if (!Number.isInteger(value) || value < 1 || value > MAX_PROVIDER_CONCURRENCY) {
    throw new Error(`TBANK_HOTELS_MAX_CONCURRENT_REQUESTS must be an integer from 1 to ${MAX_PROVIDER_CONCURRENCY}.`);
  }
  return value;
}

async function withProviderRequestSlot(operation) {
  const concurrency = maxProviderConcurrency();
  if (activeProviderRequests >= concurrency) {
    if (providerRequestQueue.length >= MAX_PROVIDER_REQUEST_QUEUE) {
      const error = new Error("Hotels provider request queue is full. Retry later instead of starting parallel tool calls.");
      error.code = "HOTELS_API_LOCAL_OVERLOAD";
      throw error;
    }
    await new Promise((resolve) => providerRequestQueue.push(resolve));
  }
  activeProviderRequests += 1;
  try {
    return await operation();
  } finally {
    activeProviderRequests -= 1;
    providerRequestQueue.shift()?.();
  }
}

function configuredAuthMode() {
  if (serviceJwtConfigured()) return "service_jwt";
  if (process.env.TBANK_HOTELS_AUTH_HEADERS_JSON) return "static_headers";
  if (process.env.TBANK_HOTELS_AUTH_TOKEN) return "static_token";
  return "not_configured";
}

function authBrokerSocket() {
  const configured = process.env.TBANK_AUTH_BROKER_SOCKET;
  return configured && configured.trim() ? configured.trim() : null;
}

function authBrokerTimeoutMs() {
  const configured = process.env.TBANK_AUTH_BROKER_TIMEOUT_MS;
  if (!configured) return 45_000;
  const parsed = Number(configured);
  if (!Number.isInteger(parsed) || parsed < 1_000 || parsed > 120_000) {
    throw new Error("TBANK_AUTH_BROKER_TIMEOUT_MS must be an integer from 1000 to 120000.");
  }
  return parsed;
}

function authBrokerRequest(method, params = {}, requestTimeoutMs = authBrokerTimeoutMs()) {
  const socketPath = authBrokerSocket();
  if (!socketPath) throw new Error("T-Bank auth broker is not configured.");
  return new Promise((resolve, reject) => {
    const client = authBrokerConnector(socketPath);
    let response = "";
    let settled = false;
    const finish = (callback, value) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      client.destroy();
      callback(value);
    };
    const timer = setTimeout(() => finish(reject, new Error("T-Bank auth broker timed out.")), requestTimeoutMs);
    client.setEncoding("utf8");
    client.on("connect", () => client.write(`${JSON.stringify({ version: 2, client: "hotels", method, params })}\n`));
    client.on("data", (chunk) => {
      response += chunk;
      if (Buffer.byteLength(response) > MAX_PROVIDER_RESPONSE_BYTES) {
        finish(reject, new Error("T-Bank auth broker response exceeded the safe size limit."));
        return;
      }
      const newline = response.indexOf("\n");
      if (newline < 0) return;
      try {
        const parsed = JSON.parse(response.slice(0, newline));
        if (!parsed || parsed.ok !== true || !parsed.result || typeof parsed.result !== "object") {
          throw new Error(typeof parsed?.error === "string" ? parsed.error : "T-Bank auth broker request failed.");
        }
        finish(resolve, parsed.result);
      } catch (error) {
        finish(reject, new Error(String(error.message || "T-Bank auth broker returned an invalid response.").slice(0, 240)));
      }
    });
    client.on("error", () => finish(reject, new Error("T-Bank auth broker is unavailable.")));
    client.on("end", () => {
      if (!settled) finish(reject, new Error("T-Bank auth broker closed the connection without a response."));
    });
  });
}

function mutationRequiredHeaders(action) {
  return action === "booking" || action === "lsBooking" ? ["x-real-ip"] : [];
}

function mutationExecutionReadiness(action) {
  if (!mutationsEnabled()) {
    return { available: false, status: "not_activated", missingRequiredHeaders: mutationRequiredHeaders(action) };
  }
  try {
    baseUrl();
    const headers = configuredHeaders();
    if (configuredAuthMode() === "not_configured") {
      return { available: false, status: "authentication_not_configured", missingRequiredHeaders: mutationRequiredHeaders(action) };
    }
    const configuredNames = new Set(Object.keys(headers).map((name) => name.toLowerCase()));
    const missingRequiredHeaders = mutationRequiredHeaders(action).filter((name) => !configuredNames.has(name));
    if (missingRequiredHeaders.length) {
      return { available: false, status: "required_trusted_headers_not_configured", missingRequiredHeaders };
    }
    return { available: true, status: "configured_unverified", missingRequiredHeaders: [] };
  } catch {
    return { available: false, status: "invalid_configuration", missingRequiredHeaders: mutationRequiredHeaders(action) };
  }
}

async function connectionStatus() {
  const hasBaseUrl = Boolean(process.env.TBANK_HOTELS_API_BASE_URL);
  const authMode = configuredAuthMode();
  let transportError = null;
  let authenticationError = null;
  if (hasBaseUrl) {
    try { baseUrl(); } catch (error) { transportError = error.message; }
  }
  if (authMode !== "not_configured") {
    try { configuredHeaders(); } catch (error) { authenticationError = error.message; }
  }
  const transport = !hasBaseUrl ? "not_configured" : transportError ? "invalid_configuration" : "configured";
  const authentication = authMode === "not_configured" ? "not_configured" : authenticationError ? "invalid_configuration" : "configured";
  let configuredProviderConcurrency = null;
  let loadProtectionError = null;
  try { configuredProviderConcurrency = maxProviderConcurrency(); }
  catch (error) { loadProtectionError = error.message; }
  const searchReady = transport === "configured" && authentication === "configured" && !loadProtectionError;
  const sharedMobileAuth = Boolean(authBrokerSocket());
  let brokerProbe = null;
  let brokerError = null;
  if (sharedMobileAuth) {
    try { brokerProbe = await authBrokerRequest("status", {}, 1_500); }
    catch (error) { brokerError = error.message; }
  }
  const brokerReachable = Boolean(brokerProbe);
  const brokerSessionConfigured = brokerProbe?.sessionConfigured === true;
  const brokerVerifiedOperations = new Set(Array.isArray(brokerProbe?.verifiedOperations) ? brokerProbe.verifiedOperations : brokerProbe?.supportedOperations ?? []);
  const brokerSupportedOperations = new Set(Array.isArray(brokerProbe?.supportedOperations) ? brokerProbe.supportedOperations : []);
  const brokerCanReadCustomer = brokerReachable && brokerSessionConfigured && brokerVerifiedOperations.has("hotels.get_customer");
  const brokerCanListBookings = brokerReachable && brokerSessionConfigured && brokerVerifiedOperations.has("hotels.list_bookings");
  const brokerCanReadBookingV1 = brokerReachable && brokerSessionConfigured && brokerVerifiedOperations.has("hotels.get_booking_v1");
  const brokerCanSaveVoucherV1 = brokerReachable && brokerSessionConfigured && brokerVerifiedOperations.has("hotels.save_voucher_v1");
  const brokerCanCreatePaymentHandoff = brokerReachable && brokerSessionConfigured && brokerSupportedOperations.has("hotels.create_payment_handoff");
  const directCustomerAuth = authMode === "static_headers" || authMode === "static_token";
  const customerReadiness = sharedMobileAuth
    ? !brokerReachable ? "broker_unavailable"
      : !brokerSessionConfigured ? "mobile_login_required"
      : brokerCanReadCustomer && brokerCanListBookings ? "mobile_read_only_ready"
      : "partial_read_only_unverified"
    : directCustomerAuth ? "unverified" : "not_configured";
  const bookingExecution = mutationExecutionReadiness("booking");
  return {
    serverVersion: SERVER_VERSION,
    ready: searchReady,
    searchReady,
    transport,
    authentication,
    authMode,
    customerContext: customerReadiness,
    customerReadiness,
    canReadCustomer: brokerCanReadCustomer || directCustomerAuth,
    canListBookings: brokerCanListBookings || directCustomerAuth,
    canReadBookingV1: brokerCanReadBookingV1 || directCustomerAuth,
    canSaveVoucher: brokerCanSaveVoucherV1,
    canCreatePaymentHandoff: brokerCanCreatePaymentHandoff,
    paymentHandoffPreview: {
      available: brokerCanCreatePaymentHandoff,
      bookingBindingSupported: brokerCanCreatePaymentHandoff,
      amountBindingVerified: false,
      paymentStatusObservation: brokerCanCreatePaymentHandoff ? "available_at_handoff" : "not_available",
      providerRequestsPerformed: false,
      amountBindingAvailableAtHandoff: brokerCanCreatePaymentHandoff,
      rawPaymentStatusAvailableAtHandoff: brokerCanCreatePaymentHandoff,
      providerReadOnCreate: brokerCanCreatePaymentHandoff,
      singleUse: true,
    },
    mobileAuth: {
      configured: sharedMobileAuth,
      provider: sharedMobileAuth ? "local_auth_broker" : "none",
      reachable: brokerReachable,
      sessionConfigured: brokerSessionConfigured,
      sessionOwnerOnly: brokerProbe?.sessionOwnerOnly ?? null,
      verified: brokerCanReadCustomer && brokerCanListBookings && brokerCanReadBookingV1,
      verifiedOperations: [...brokerVerifiedOperations].filter((operation) => operation === "hotels.get_customer" || operation === "hotels.list_bookings" || operation === "hotels.get_booking_v1" || operation === "hotels.save_voucher_v1"),
      supportedOperations: brokerReachable ? (brokerProbe.supportedOperations ?? []) : [],
    },
    bookingExecution,
    loadProtection: {
      status: loadProtectionError ? "invalid_configuration" : "configured",
      maxConcurrentProviderRequests: configuredProviderConcurrency,
      maxQueuedProviderRequests: MAX_PROVIDER_REQUEST_QUEUE,
      activeProviderRequests,
      queuedProviderRequests: providerRequestQueue.length,
      identicalSearchCacheTtlMs: SEARCH_CACHE_TTL_MS,
      cachedSearches: hotelSearchCacheByKey.size,
      inFlightSearches: inFlightHotelSearchByKey.size,
    },
    mutationsEnabled: mutationsEnabled(),
    diagnostics: {
      transport: transportError,
      authentication: authenticationError,
      authBroker: brokerError,
      loadProtection: loadProtectionError,
    },
    browserDependency: false,
    storedUserSession: false,
    sharedMobileSessionConfigured: brokerSessionConfigured,
    note: "Значения URL, токенов и auth-заголовков намеренно не раскрываются.",
  };
}

async function getCustomer() {
  if (authBrokerSocket()) {
    const result = await authBrokerRequest("hotels.get_customer");
    return result.customer;
  }
  if (configuredAuthMode() === "service_jwt") {
    throw new Error("Customer context is not configured. service_jwt authenticates the MCP service and cannot autofill booking guest data.");
  }
  if (configuredAuthMode() === "not_configured") {
    throw new Error("Customer context is not configured. Complete local mobile login and configure the auth broker, or provide an approved static customer auth profile.");
  }
  return apiRequest("GET", "/api/v1/auth/customerdata");
}

function mutationsEnabled() {
  return process.env.TBANK_HOTELS_ENABLE_MUTATIONS === "true";
}

function requireMutationsEnabled() {
  if (!mutationsEnabled()) throw new Error("Hotels API mutation execution is not available in this MCP configuration. Activation requires an integration-owner decision outside the model conversation.");
}

function requireMutationExecutionReady(action) {
  requireMutationsEnabled();
  const readiness = mutationExecutionReadiness(action);
  if (!readiness.available) {
    throw new Error(`Hotels API ${action} execution profile is not ready (${readiness.status}). Required trusted request headers and customer authorization must be configured outside the model conversation.`);
  }
}

function cleanupJourneys() {
  const now = Date.now();
  for (const [journeyId, journey] of journeysById.entries()) {
    if (journey.expiresAt <= now) journeysById.delete(journeyId);
  }
  for (const [draftId, draft] of bookingDraftsById.entries()) {
    if (draft.expiresAt <= now) bookingDraftsById.delete(draftId);
  }
  for (const [bookingRef, reference] of bookingReferencesById.entries()) {
    if (reference.expiresAt <= now) bookingReferencesById.delete(bookingRef);
  }
  for (const [requestHashValue, execution] of mutationExecutionsByHash.entries()) {
    if (execution.expiresAt <= now) mutationExecutionsByHash.delete(requestHashValue);
  }
}

function storeBounded(map, key, item, maximum) {
  cleanupJourneys();
  if (map.size >= maximum) {
    let oldestKey = null;
    let oldestExpiry = Number.POSITIVE_INFINITY;
    for (const [candidateKey, candidate] of map.entries()) {
      const expiry = Number(candidate?.expiresAt ?? 0);
      if (expiry < oldestExpiry) {
        oldestKey = candidateKey;
        oldestExpiry = expiry;
      }
    }
    if (oldestKey !== null) map.delete(oldestKey);
  }
  map.set(key, item);
}

function startTrackedMutationExecution(hash, expiresAt) {
  cleanupJourneys();
  const existing = mutationExecutionsByHash.get(hash);
  if (existing?.state === "in_flight") throw new Error("This mutation execution is already in progress.");
  if (existing?.state === "outcome_unknown") throw new Error("The previous mutation execution outcome is unknown. Do not retry it automatically; reconcile provider state first.");
  if (existing?.state === "completed") throw new Error("This prepared mutation has already completed and cannot be replayed.");
  storeBounded(mutationExecutionsByHash, hash, { state: "in_flight", expiresAt }, MAX_TRACKED_MUTATION_EXECUTIONS);
}

function finishTrackedMutationExecution(hash, state) {
  const execution = mutationExecutionsByHash.get(hash);
  if (execution) execution.state = state;
}

function bookingDraftById(bookingDraftId) {
  cleanupJourneys();
  if (typeof bookingDraftId !== "string" || !bookingDraftId) throw new Error("bookingDraftId must be a non-empty string.");
  const draft = bookingDraftsById.get(bookingDraftId);
  if (!draft) throw new Error("Unknown or expired bookingDraftId. Create a new booking draft.");
  return draft;
}

function bookingReferenceForOrderId(orderId) {
  cleanupJourneys();
  for (const [bookingRef, reference] of bookingReferencesById.entries()) {
    if (reference.orderId === orderId) return bookingRef;
  }
  const bookingRef = `booking_${randomUUID().replaceAll("-", "").slice(0, 24)}`;
  storeBounded(bookingReferencesById, bookingRef, {
    orderId,
    expiresAt: Date.now() + JOURNEY_TTL_MS,
  }, MAX_ACTIVE_BOOKING_REFERENCES);
  return bookingRef;
}

function orderIdForBookingReference(bookingRef) {
  cleanupJourneys();
  if (typeof bookingRef !== "string" || !/^booking_[a-f0-9]{24}$/.test(bookingRef)) {
    throw new Error("bookingRef must be an opaque reference returned by tbank_hotels_list_bookings.");
  }
  const reference = bookingReferencesById.get(bookingRef);
  if (!reference) throw new Error("Unknown or expired bookingRef. Call tbank_hotels_list_bookings again.");
  return reference.orderId;
}

function withoutProviderBookingIdentifiers(value) {
  if (Array.isArray(value)) return value.map(withoutProviderBookingIdentifiers);
  if (!value || typeof value !== "object") return value;
  return Object.fromEntries(Object.entries(value)
    .filter(([key]) => !providerBookingIdentifierKey(key))
    .map(([key, nested]) => [key, withoutProviderBookingIdentifiers(nested)]));
}

function providerBookingIdentifierKey(key) {
  const normalized = String(key).replace(/[^a-z0-9]/gi, "").toLowerCase();
  return [
    "orderid", "bookingid", "reservationid", "taskid", "paymentid",
    "transactionid", "customerid", "userid", "sessionid", "ssoid", "siebelid",
    "ordernumber", "bookingnumber", "reservationnumber", "confirmationnumber",
    "orderref", "bookingref", "reservationref", "providerref", "bookhash",
  ].some((suffix) => normalized.endsWith(suffix)) || normalized.includes("token");
}

function bookingListWithReferences(bookings) {
  const root = bookings?.payload && typeof bookings.payload === "object" && !Array.isArray(bookings.payload)
    ? bookings.payload
    : bookings;
  if (!root || typeof root !== "object" || Array.isArray(root)) {
    throw new Error("Hotels booking list response has an unsupported shape.");
  }
  const normalized = { ...root };
  for (const listName of ["activeList", "cancelledList", "completedList"]) {
    const list = root[listName];
    if (!Array.isArray(list)) throw new Error(`Hotels booking list response does not contain ${listName}.`);
    normalized[listName] = list.map((item) => {
      if (!item || typeof item !== "object" || Array.isArray(item)) throw new Error(`Hotels booking list ${listName} contains an invalid item.`);
      const orderId = brokerIdentifier(item.orderId, "provider orderId");
      return {
        ...withoutProviderBookingIdentifiers(item),
        bookingRef: bookingReferenceForOrderId(orderId),
      };
    });
  }
  return root === bookings ? normalized : { ...withoutProviderBookingIdentifiers(bookings), payload: normalized };
}

function bookingListSummary(bookings) {
  const root = bookings?.payload && typeof bookings.payload === "object" && !Array.isArray(bookings.payload)
    ? bookings.payload
    : bookings;
  if (!root || typeof root !== "object" || Array.isArray(root)) {
    throw new Error("Hotels booking list response has an unsupported shape.");
  }
  const counts = {};
  for (const [outputName, listName] of [["activeCount", "activeList"], ["cancelledCount", "cancelledList"], ["completedCount", "completedList"]]) {
    if (!Array.isArray(root[listName])) throw new Error(`Hotels booking list response does not contain ${listName}.`);
    counts[outputName] = root[listName].length;
  }
  return {
    status: "ready",
    ...counts,
    detailsIncluded: false,
    personalTravelFactsIncluded: false,
    bookingReferencesIncluded: false,
  };
}

function bookingWithReference(booking, bookingRef) {
  const sanitized = withoutProviderBookingIdentifiers(booking);
  if (!sanitized || typeof sanitized !== "object" || Array.isArray(sanitized)) {
    throw new Error("Hotels booking response has an unsupported shape.");
  }
  return { ...sanitized, bookingRef };
}

function journeyById(journeyId) {
  cleanupJourneys();
  if (typeof journeyId !== "string" || !journeyId) throw new Error("journeyId must be a non-empty string.");
  const journey = journeysById.get(journeyId);
  if (!journey) throw new Error("Unknown or expired journeyId. Start a new hotel stay plan.");
  return journey;
}

function optional(object, key) {
  return object && typeof object === "object" ? object[key] ?? null : null;
}

function displayedPriceBreakfastEvidence(mealName) {
  if (typeof mealName !== "string" || !mealName.trim()) return "not_confirmed_for_displayed_price";
  const normalized = normalizedText(mealName);
  const explicitlyExcluded = normalized.includes("breakfast not included")
    || normalized.includes("without breakfast")
    || normalized.includes("no breakfast")
    || normalized.includes("meal not included")
    || normalized.includes("meals not included")
    || normalized.includes("without meals")
    || normalized.includes("no meals")
    || normalized === "room only"
    || normalized.includes("завтрак не включен")
    || normalized.includes("без завтрака")
    || normalized.includes("питание не включено")
    || normalized.includes("без питания");
  if (explicitlyExcluded) return "excluded_by_meal_name";
  const explicitlyIncluded = normalized === "breakfast"
    || normalized === "завтрак"
    || normalized.includes("breakfast included")
    || normalized.includes("includes breakfast")
    || normalized.includes("завтрак включен")
    || normalized.includes("завтрак входит")
    || normalized.includes("с завтраком");
  return explicitlyIncluded ? "confirmed_by_meal_name" : "not_confirmed_for_displayed_price";
}

function stayOption(option) {
  const hotel = option.hotel;
  const rate = optional(hotel, "rateForHotelsFeed") ?? {};
  const review = optional(hotel, "review") ?? null;
  return {
    optionId: option.optionId,
    hotelName: optional(hotel, "hotelName"),
    hotelChain: optional(hotel, "hotelChain"),
    starRating: optional(hotel, "starRating"),
    destination: optional(optional(hotel, "areaLocation"), "destinationName"),
    address: optional(optional(hotel, "hotelLocation"), "address"),
    price: optional(rate, "shownPrice"),
    freeCancellationUntil: optional(rate, "freeCancellationUntil"),
    mealName: optional(rate, "mealName"),
    displayedPriceBreakfastEvidence: displayedPriceBreakfastEvidence(optional(rate, "mealName")),
    paymentPlace: optional(rate, "paymentPlace"),
    availableRoomsCount: optional(rate, "availableRoomsCount"),
    review: review ? { rating: optional(review, "rating"), ratingsCount: optional(review, "ratingsCount") } : null,
    cashback: optional(hotel, "cashback"),
  };
}

function stayRateOption(rateOption) {
  const rate = rateOption.rate;
  return {
    rateOptionId: rateOption.rateOptionId,
    shownPrice: optional(rate, "shownPrice"),
    paymentPrice: optional(rate, "paymentPrice"),
    paymentPlace: optional(rate, "paymentPlace"),
    mealName: optional(rate, "mealName"),
    displayedPriceBreakfastEvidence: displayedPriceBreakfastEvidence(optional(rate, "mealName")),
    availableRoomsCount: optional(rate, "availableRoomsCount"),
    isNonRefundable: optional(rate, "isNonRefundable"),
    isCreditCardDataRequired: optional(rate, "isCreditCardDataRequired"),
    cancellationPolicyRules: optional(rate, "cancellationPolicyRules"),
    cashback: optional(rate, "cashback"),
  };
}

function stayComparisonRow(option) {
  const facts = stayOption(option);
  return {
    hotelName: facts.hotelName,
    destination: facts.destination,
    starRating: facts.starRating,
    reviewRating: facts.review?.rating ?? null,
    ratingsCount: facts.review?.ratingsCount ?? null,
    priceAmount: numericProviderFact(facts.price),
    priceCurrency: providerCurrency(facts.price),
    freeCancellationUntil: facts.freeCancellationUntil,
    mealName: facts.mealName,
    displayedPriceBreakfastEvidence: facts.displayedPriceBreakfastEvidence,
  };
}

function markdownCell(value) {
  if (value === null || value === undefined || value === "") return "—";
  return String(value).replaceAll("|", "\\|").replaceAll("\n", " ");
}

function comparisonTableMarkdown(rows) {
  const header = "| Отель | Локация | Звёзды | Рейтинг | Отзывов | Цена | Бесплатная отмена | Питание |";
  const divider = "| --- | --- | ---: | ---: | ---: | ---: | --- | --- |";
  const body = rows.map((row) => {
    const price = row.priceAmount === null
      ? null
      : `${row.priceAmount}${row.priceCurrency ? ` ${row.priceCurrency}` : ""}`;
    return `| ${markdownCell(row.hotelName)} | ${markdownCell(row.destination)} | ${markdownCell(row.starRating)} | ${markdownCell(row.reviewRating)} | ${markdownCell(row.ratingsCount)} | ${markdownCell(price)} | ${markdownCell(row.freeCancellationUntil)} | ${markdownCell(row.mealName)} |`;
  });
  return [header, divider, ...body].join("\n");
}

function normalizedText(value) {
  return String(value ?? "")
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase("ru-RU")
    .replace(/ё/g, "е")
    .replace(/[^\p{L}\p{N}]+/gu, " ")
    .trim()
    .replace(/\s+/g, " ");
}

function boundedInteger(value, name, fallback, minimum, maximum) {
  if (value === undefined) return fallback;
  if (!Number.isInteger(value) || value < minimum || value > maximum) throw new Error(`${name} must be an integer from ${minimum} to ${maximum}.`);
  return value;
}

function dateOnly(value, name) {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) throw new Error(`${name} must use YYYY-MM-DD format.`);
  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) throw new Error(`${name} is not a valid calendar date.`);
  return date;
}

function validatedPlanInput(args) {
  requestObject(args, "arguments");
  assertOnlyKeys(args, ["destination", "destinationId", "countryName", "checkinDate", "checkoutDate", "rooms", "hotelName", "breakfastIncluded", "ranking", "maxOptions", "language"], "arguments");
  const destination = typeof args.destination === "string" ? args.destination.trim() : "";
  const destinationId = args.destinationId;
  if (!destination && (!Number.isInteger(destinationId) || destinationId <= 0)) throw new Error("Provide destination as a location name or a positive destinationId from destination resolution.");
  if (destination && destination.length > 200) throw new Error("destination must not exceed 200 characters.");
  if (destinationId !== undefined && (!Number.isInteger(destinationId) || destinationId <= 0)) throw new Error("destinationId must be a positive integer.");
  const checkin = dateOnly(args.checkinDate, "checkinDate");
  const checkout = dateOnly(args.checkoutDate, "checkoutDate");
  if (checkout <= checkin) throw new Error("checkoutDate must be after checkinDate.");
  const today = new Date();
  const todayUtc = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()));
  if (checkin < todayUtc) throw new Error("checkinDate must not be in the past.");
  if (!Array.isArray(args.rooms) || args.rooms.length < 1 || args.rooms.length > MAX_ROOMS) throw new Error(`rooms must contain 1 to ${MAX_ROOMS} rooms.`);
  const rooms = args.rooms.map((room, index) => {
    requestObject(room, `rooms[${index}]`);
    if (!Number.isInteger(room.adults) || room.adults < 1 || room.adults > 16) throw new Error(`rooms[${index}].adults must be an integer from 1 to 16.`);
    const childrenAges = room.childrenAges ?? [];
    if (!Array.isArray(childrenAges) || childrenAges.length > 16 || childrenAges.some((age) => !Number.isInteger(age) || age < 0 || age > 17)) {
      throw new Error(`rooms[${index}].childrenAges must contain at most 16 integer ages from 0 to 17.`);
    }
    return { adults: room.adults, childrenAges: [...childrenAges] };
  });
  const hotelName = args.hotelName == null ? null : String(args.hotelName).trim();
  if (args.hotelName != null && (!hotelName || hotelName.length > 250)) throw new Error("hotelName must contain 1 to 250 characters.");
  const countryName = args.countryName == null ? null : String(args.countryName).trim();
  if (args.countryName != null && (!countryName || countryName.length > 120)) throw new Error("countryName must contain 1 to 120 characters.");
  const ranking = args.ranking ?? "provider_order";
  if (!["provider_order", "lowest_price", "highest_rating"].includes(ranking)) throw new Error("ranking must be provider_order, lowest_price, or highest_rating.");
  if (args.breakfastIncluded !== undefined && typeof args.breakfastIncluded !== "boolean") throw new Error("breakfastIncluded must be a boolean.");
  return {
    destination,
    destinationId: destinationId ?? null,
    countryName,
    checkinDate: args.checkinDate,
    checkoutDate: args.checkoutDate,
    rooms,
    hotelName,
    breakfastIncluded: args.breakfastIncluded === true,
    ranking,
    maxOptions: boundedInteger(args.maxOptions, "maxOptions", DEFAULT_PLAN_OPTIONS, 1, MAX_PLAN_OPTIONS),
    language: args.language,
  };
}

function validatedSearchFilters(filtersValue, name = "payload.filters") {
  if (filtersValue === undefined) return undefined;
  if (!Array.isArray(filtersValue)) throw new Error(`${name} must be an array.`);
  return filtersValue.map((filterValue, index) => {
    const itemName = `${name}[${index}]`;
    const filter = requestObject(filterValue, itemName);
    if (!SEARCH_FILTER_IDS.includes(filter.filterId)) throw new Error(`${itemName}.filterId is unsupported.`);
    switch (filter.$objectType) {
      case "array":
        assertOnlyKeys(filter, ["$objectType", "filterId", "values"], itemName);
        if (!Array.isArray(filter.values) || filter.values.some((value) => typeof value !== "string")) throw new Error(`${itemName}.values must be an array of strings.`);
        break;
      case "boolean":
        assertOnlyKeys(filter, ["$objectType", "filterId", "value"], itemName);
        if (typeof filter.value !== "boolean") throw new Error(`${itemName}.value must be a boolean.`);
        break;
      case "radio":
        assertOnlyKeys(filter, ["$objectType", "filterId", "value", "values"], itemName);
        if (typeof filter.value !== "string" || !filter.value) throw new Error(`${itemName}.value must be a non-empty string.`);
        if (filter.values !== undefined && filter.values !== null && (!Array.isArray(filter.values) || filter.values.some((value) => typeof value !== "string"))) {
          throw new Error(`${itemName}.values must be an array of strings or null.`);
        }
        break;
      case "range":
        assertOnlyKeys(filter, ["$objectType", "filterId", "min", "max"], itemName);
        if (typeof filter.min !== "number" || !Number.isFinite(filter.min) || typeof filter.max !== "number" || !Number.isFinite(filter.max)) {
          throw new Error(`${itemName}.min and ${itemName}.max must be finite numbers.`);
        }
        break;
      default:
        throw new Error(`${itemName}.$objectType must be array, boolean, radio, or range.`);
    }
    return structuredClone(filter);
  });
}

function validatedProviderSearchRequest(payloadValue) {
  const body = requestObject(payloadValue);
  assertOnlyKeys(body, ["destinationId", "checkinDate", "checkoutDate", "guests", "filters", "sort", "offset", "limit"], "payload");
  if (!Number.isInteger(body.destinationId) || body.destinationId <= 0) throw new Error("payload.destinationId must be a positive integer.");
  const checkin = dateOnly(body.checkinDate, "payload.checkinDate");
  const checkout = dateOnly(body.checkoutDate, "payload.checkoutDate");
  if (checkout <= checkin) throw new Error("payload.checkoutDate must be after payload.checkinDate.");
  if (!Array.isArray(body.guests) || body.guests.length < 1 || body.guests.length > MAX_ROOMS) throw new Error(`payload.guests must contain 1 to ${MAX_ROOMS} room guest groups.`);
  body.guests.forEach((guest, index) => {
    requestObject(guest, `payload.guests[${index}]`);
    assertOnlyKeys(guest, ["adultsCount", "childrenAge"], `payload.guests[${index}]`);
    if (!Number.isInteger(guest.adultsCount) || guest.adultsCount < 1 || guest.adultsCount > 16) throw new Error(`payload.guests[${index}].adultsCount must be an integer from 1 to 16.`);
    if (guest.childrenAge !== undefined && (!Array.isArray(guest.childrenAge) || guest.childrenAge.length > 16 || guest.childrenAge.some((age) => !Number.isInteger(age) || age < 0 || age > 17))) {
      throw new Error(`payload.guests[${index}].childrenAge must contain integer ages from 0 to 17.`);
    }
  });
  const filters = validatedSearchFilters(body.filters);
  if (body.sort !== undefined) requestObject(body.sort, "payload.sort");
  optionalNonNegativeInteger(body.offset, "payload.offset");
  optionalNonNegativeInteger(body.limit, "payload.limit");
  return { ...body, ...(filters === undefined ? {} : { filters }) };
}

function providerSearchRequest(method, path, args) {
  return apiRequest(method, path, { ...args, payload: validatedProviderSearchRequest(args.payload) });
}

function requiredStayConditions(input) {
  return {
    breakfastIncluded: input.breakfastIncluded,
  };
}

function appliedStayConditions(input) {
  return {
    breakfastIncluded: input.breakfastIncluded
      ? { required: true, applied: true, source: "provider_search_filter", filterId: "meal_types", value: "breakfast" }
      : { required: false, applied: false, source: "not_requested" },
  };
}

function providerConditionFailure(error) {
  return error?.code === "HOTELS_API_TIMEOUT" || error?.code === "HOTELS_API_NETWORK" || error?.code === "HOTELS_API_HTTP";
}

function providerConditionUnavailable(error, requiredConditions) {
  const providerHttpStatus = Number.isInteger(error?.httpStatus) ? error.httpStatus : null;
  const reason = error?.code === "HOTELS_API_TIMEOUT"
    ? "provider_timeout"
    : error?.code === "HOTELS_API_NETWORK"
      ? "provider_unreachable"
      : providerHttpStatus === 401 || providerHttpStatus === 403
        ? "provider_auth_rejected"
        : (providerHttpStatus ?? 0) >= 500
          ? "provider_unavailable"
          : "provider_rejected_required_request";
  return {
    status: "requirements_unavailable",
    reason,
    requiredConditions,
    retryAllowed: false,
    lowLevelFallbackAllowed: false,
    providerHttpStatus,
    nextStep: reason === "provider_auth_rejected"
      ? "Do not retry or weaken the required condition. Check the configured Hotels search authentication profile outside the model conversation, then start a new search after readiness is restored."
      : "Do not retry by guessing low-level filter payloads and do not present unfiltered hotels as satisfying the requirement. Report that the required filtered search is temporarily unavailable.",
  };
}

async function guardedProviderSearchRequest(method, path, args) {
  const body = validatedProviderSearchRequest(args.payload);
  try {
    return await apiRequest(method, path, { ...args, payload: body });
  } catch (error) {
    if (body.filters?.length && providerConditionFailure(error)) {
      return providerConditionUnavailable(error, { providerFilters: body.filters });
    }
    throw error;
  }
}

function locationCandidate(location) {
  const destinationId = optional(location, "locationId");
  if (!Number.isInteger(destinationId) || destinationId <= 0) return null;
  return {
    destinationId,
    name: optional(location, "locationNameRu") || optional(location, "locationName"),
    internationalName: optional(location, "locationName"),
    countryName: optional(location, "countryNameRu") || optional(location, "countryName"),
    internationalCountryName: optional(location, "countryName"),
    hotelsCount: optional(location, "hotelsCount"),
  };
}

async function locationCatalog(countryName) {
  const cacheKey = normalizedText(countryName || "*");
  const cached = locationCatalogByCountry.get(cacheKey);
  if (cached && cached.expiresAt > Date.now()) return cached.locations;
  const locationsById = new Map();
  const startedAt = Date.now();
  for (let page = 0; page < MAX_LOCATION_PAGES; page += 1) {
    const remainingBudgetMs = LOCATION_COLLECTION_BUDGET_MS - (Date.now() - startedAt);
    if (page > 0 && remainingBudgetMs < MIN_SEARCH_REQUEST_BUDGET_MS) {
      throw new Error("Hotels API locations catalog could not be loaded completely within the safe time budget. Retry destination resolution.");
    }
    const query = { Sort: "name", Offset: page * LOCATION_PAGE_SIZE, Limit: LOCATION_PAGE_SIZE };
    if (countryName) query.CountryName = countryName;
    let result;
    try {
      result = await apiRequest("GET", "/api/v1/seo/locations", {
        query,
        requestTimeoutMs: Math.max(MIN_SEARCH_REQUEST_BUDGET_MS, remainingBudgetMs),
      });
    } catch (error) {
      if (error.code === "HOTELS_API_TIMEOUT") {
        throw new Error("Hotels API locations catalog could not be loaded completely within the safe time budget. Retry destination resolution.");
      }
      throw error;
    }
    const rawLocations = result.data?.payload?.locations;
    if (!Array.isArray(rawLocations)) throw new Error("Hotels API locations response does not contain the expected payload.locations array.");
    let added = 0;
    for (const candidate of rawLocations.map(locationCandidate).filter(Boolean)) {
      if (!locationsById.has(candidate.destinationId)) added += 1;
      locationsById.set(candidate.destinationId, candidate);
    }
    if (rawLocations.length < LOCATION_PAGE_SIZE || added === 0) break;
    if (page === MAX_LOCATION_PAGES - 1) throw new Error("Hotels API locations catalog exceeded the safe pagination limit.");
  }
  const locations = [...locationsById.values()];
  if (locationCatalogByCountry.size >= MAX_LOCATION_CACHES) {
    const oldest = [...locationCatalogByCountry.entries()].sort((left, right) => left[1].expiresAt - right[1].expiresAt)[0];
    if (oldest) locationCatalogByCountry.delete(oldest[0]);
  }
  locationCatalogByCountry.set(cacheKey, { expiresAt: Date.now() + LOCATION_CACHE_TTL_MS, locations });
  return locations;
}

function locationMatchScore(candidate, query) {
  const names = [candidate.name, candidate.internationalName].map(normalizedText).filter(Boolean);
  const countries = [candidate.countryName, candidate.internationalCountryName].map(normalizedText).filter(Boolean);
  if (names.includes(query)) return 0;
  if (names.some((name) => `${name} ${countries[0] ?? ""}`.trim() === query)) return 1;
  if (names.some((name) => name.startsWith(query))) return 2;
  if (names.some((name) => name.includes(query))) return 3;
  const tokens = query.split(" ").filter(Boolean);
  if (tokens.length > 1 && names.some((name) => tokens.every((token) => name.includes(token)))) return 4;
  return null;
}

async function resolveDestination(args) {
  if (typeof args.query !== "string" || !args.query.trim() || args.query.length > 200) throw new Error("query must contain 1 to 200 characters.");
  if (args.countryName != null && (typeof args.countryName !== "string" || !args.countryName.trim() || args.countryName.length > 120)) {
    throw new Error("countryName must contain 1 to 120 characters.");
  }
  const query = normalizedText(args.query);
  const maxCandidates = boundedInteger(args.maxCandidates, "maxCandidates", 5, 1, 10);
  const locations = await locationCatalog(args.countryName);
  const matches = locations
    .map((candidate) => ({ candidate, score: locationMatchScore(candidate, query) }))
    .filter(({ score }) => score !== null)
    .sort((left, right) => left.score - right.score || (right.candidate.hotelsCount ?? 0) - (left.candidate.hotelsCount ?? 0) || String(left.candidate.name).localeCompare(String(right.candidate.name), "ru"));
  const candidates = matches.slice(0, maxCandidates).map(({ candidate }) => candidate);
  if (!matches.length) return { status: "not_found", query: args.query, candidates: [], catalogSize: locations.length };
  const top = matches[0];
  const next = matches[1];
  const uniquelyResolved = !next || (top.score <= 2 && top.score < next.score);
  return {
    status: uniquelyResolved ? "resolved" : "ambiguous",
    query: args.query,
    destination: uniquelyResolved ? top.candidate : null,
    candidates,
  };
}

function numericProviderFact(value) {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (!value || typeof value !== "object") return null;
  for (const key of ["amount", "value", "price"]) {
    if (typeof value[key] === "number" && Number.isFinite(value[key])) return value[key];
  }
  return null;
}

function providerCurrency(value) {
  if (!value || typeof value !== "object") return null;
  for (const key of ["currency", "currencyCode", "currencyType"]) {
    if (typeof value[key] === "string" && value[key].trim()) return value[key].trim().toUpperCase();
  }
  return null;
}

function rankedOptions(options, strategy = "provider_order") {
  if (!["provider_order", "lowest_price", "highest_rating"].includes(strategy)) throw new Error("ranking must be provider_order, lowest_price, or highest_rating.");
  const ranked = [...options];
  if (strategy === "lowest_price") {
    const currencies = new Set(ranked
      .map((option) => stayOption(option).price)
      .filter((price) => numericProviderFact(price) !== null)
      .map((price) => providerCurrency(price) ?? "UNKNOWN"));
    if (currencies.size > 1 || currencies.has("UNKNOWN")) throw new Error("Cannot rank hotel prices across different or unknown currencies. Use provider_order and compare currency fields explicitly.");
    ranked.sort((left, right) => (numericProviderFact(stayOption(left).price) ?? Number.POSITIVE_INFINITY) - (numericProviderFact(stayOption(right).price) ?? Number.POSITIVE_INFINITY));
  } else if (strategy === "highest_rating") {
    ranked.sort((left, right) => (numericProviderFact(stayOption(right).review?.rating) ?? Number.NEGATIVE_INFINITY) - (numericProviderFact(stayOption(left).review?.rating) ?? Number.NEGATIVE_INFINITY));
  }
  return ranked;
}

function editDistance(left, right) {
  const previous = Array.from({ length: right.length + 1 }, (_, index) => index);
  for (let i = 1; i <= left.length; i += 1) {
    let diagonal = previous[0];
    previous[0] = i;
    for (let j = 1; j <= right.length; j += 1) {
      const old = previous[j];
      previous[j] = Math.min(previous[j] + 1, previous[j - 1] + 1, diagonal + (left[i - 1] === right[j - 1] ? 0 : 1));
      diagonal = old;
    }
  }
  return previous[right.length];
}

function hotelsByName(hotels, hotelName) {
  if (!hotelName) return { matchMode: null, hotels, suggestions: [] };
  const query = normalizedText(hotelName);
  const exact = hotels.filter((hotel) => normalizedText(optional(hotel, "hotelName")) === query);
  if (exact.length) return { matchMode: "exact", hotels: exact, suggestions: [] };
  const partial = hotels.filter((hotel) => normalizedText(optional(hotel, "hotelName")).includes(query));
  if (partial.length) return { matchMode: "partial", hotels: partial, suggestions: [] };
  const suggestions = [...hotels]
    .filter((hotel) => optional(hotel, "hotelName"))
    .sort((left, right) => editDistance(normalizedText(left.hotelName), query) - editDistance(normalizedText(right.hotelName), query))
    .slice(0, 5)
    .map((hotel) => ({ hotelName: hotel.hotelName, destination: optional(optional(hotel, "areaLocation"), "destinationName") }));
  return { matchMode: "not_found", hotels: [], suggestions };
}

function hotelDeduplicationKey(hotel) {
  const hotelId = optional(hotel, "hotelId");
  if (typeof hotelId === "string" && hotelId) return `id:${hotelId}`;
  return `content:${createHash("sha256").update(JSON.stringify(hotel)).digest("hex")}`;
}

function optionalCount(value) {
  return Number.isInteger(value) && value >= 0 ? value : null;
}

async function collectHotelSearchUncached(searchRequest, ranking, language) {
  const hotelsByKey = new Map();
  const visitedOffsets = new Set([0]);
  const startedAt = Date.now();
  let offset = 0;
  let requestCount = 0;
  let loadingPolls = 0;
  let isLoadingCompleted = false;
  let hotelsTotalCount = null;
  let filteredHotelsCount = null;
  let stoppedReason = null;

  while (requestCount < MAX_SEARCH_REQUESTS) {
    const remainingBudgetMs = SEARCH_COLLECTION_BUDGET_MS - (Date.now() - startedAt);
    if (requestCount > 0 && remainingBudgetMs < MIN_SEARCH_REQUEST_BUDGET_MS) {
      stoppedReason = "time_budget";
      break;
    }
    const payload = {
      ...searchRequest,
      offset,
      limit: SEARCH_PAGE_SIZE,
    };
    requestCount += 1;
    let search;
    try {
      search = await apiRequest("POST", "/api/v1/hotels/search", {
        payload,
        language,
        requestTimeoutMs: Math.max(MIN_SEARCH_REQUEST_BUDGET_MS, remainingBudgetMs),
      });
    } catch (error) {
      if (error.code === "HOTELS_API_TIMEOUT" && hotelsByKey.size > 0) {
        stoppedReason = "time_budget";
        break;
      }
      throw error;
    }
    const page = search.data?.payload;
    const hotels = page?.hotels;
    if (!Array.isArray(hotels)) throw new Error("Hotels API search response does not contain the expected payload.hotels array.");
    for (const hotel of hotels) hotelsByKey.set(hotelDeduplicationKey(hotel), hotel);
    hotelsTotalCount = optionalCount(page.hotelsTotalCount) ?? hotelsTotalCount;
    filteredHotelsCount = optionalCount(page.filteredHotelsCount) ?? filteredHotelsCount;
    isLoadingCompleted = page.isLoadingCompleted !== false;
    const nextOffset = optionalCount(page.nextOffset);

    if (nextOffset !== null && nextOffset > offset) {
      if (visitedOffsets.has(nextOffset)) {
        stoppedReason = "repeated_next_offset";
        break;
      }
      if (requestCount >= MAX_SEARCH_REQUESTS) {
        stoppedReason = "request_limit";
        break;
      }
      visitedOffsets.add(nextOffset);
      offset = nextOffset;
      loadingPolls = 0;
      continue;
    }
    if (isLoadingCompleted) break;
    if (nextOffset !== null && nextOffset < offset) {
      stoppedReason = "repeated_next_offset";
      break;
    }
    if (loadingPolls >= MAX_SEARCH_LOADING_POLLS) {
      stoppedReason = "loading_poll_limit";
      break;
    }
    loadingPolls += 1;
    if (SEARCH_COLLECTION_BUDGET_MS - (Date.now() - startedAt) <= SEARCH_LOADING_POLL_DELAY_MS + MIN_SEARCH_REQUEST_BUDGET_MS) {
      stoppedReason = "time_budget";
      break;
    }
    await new Promise((resolve) => setTimeout(resolve, SEARCH_LOADING_POLL_DELAY_MS));
  }

  if (requestCount >= MAX_SEARCH_REQUESTS && !isLoadingCompleted && stoppedReason === null) stoppedReason = "request_limit";
  const hotels = [...hotelsByKey.values()];
  const expectedCount = filteredHotelsCount ?? hotelsTotalCount;
  const truncated = stoppedReason !== null || !isLoadingCompleted || (expectedCount !== null && hotels.length < expectedCount);
  return {
    hotels,
    metadata: {
      fetchedHotelsCount: hotels.length,
      hotelsTotalCount,
      filteredHotelsCount,
      isLoadingCompleted,
      truncated,
      requestCount,
      providerSort: null,
      rankingAppliedLocally: ranking,
      stoppedReason,
    },
  };
}

function resetSearchCacheForChangedTransport() {
  if (searchCacheTransport === globalThis.fetch) return;
  hotelSearchCacheByKey.clear();
  inFlightHotelSearchByKey.clear();
  searchCacheTransport = globalThis.fetch;
}

function hotelSearchCacheKey(searchRequest, ranking, language) {
  const origin = baseUrl().href;
  return createHash("sha256")
    .update(JSON.stringify({ origin, authMode: configuredAuthMode(), searchRequest, ranking, language: language ?? null }))
    .digest("hex");
}

function collectedSearchCopy(search, cacheStatus) {
  return {
    hotels: structuredClone(search.hotels),
    metadata: { ...search.metadata, cacheStatus },
  };
}

async function collectHotelSearch(searchRequest, ranking, language) {
  resetSearchCacheForChangedTransport();
  const key = hotelSearchCacheKey(searchRequest, ranking, language);
  const now = Date.now();
  const cached = hotelSearchCacheByKey.get(key);
  if (cached?.expiresAt > now) return collectedSearchCopy(cached.search, "hit");
  if (cached) hotelSearchCacheByKey.delete(key);

  const inFlight = inFlightHotelSearchByKey.get(key);
  if (inFlight) return collectedSearchCopy(await inFlight, "coalesced");

  const pending = collectHotelSearchUncached(searchRequest, ranking, language);
  inFlightHotelSearchByKey.set(key, pending);
  try {
    const search = await pending;
    storeBounded(hotelSearchCacheByKey, key, {
      expiresAt: Date.now() + SEARCH_CACHE_TTL_MS,
      search: collectedSearchCopy(search, "stored"),
    }, MAX_SEARCH_CACHE_ENTRIES);
    return collectedSearchCopy(search, "miss");
  } finally {
    inFlightHotelSearchByKey.delete(key);
  }
}

async function planStay(args) {
  const input = validatedPlanInput(args);
  let destination = input.destinationId ? { destinationId: input.destinationId, name: input.destination || null, countryName: input.countryName } : null;
  if (!destination) {
    const resolution = await resolveDestination({ query: input.destination, countryName: input.countryName, maxCandidates: 5 });
    if (resolution.status !== "resolved") {
      return {
        status: "clarification_required",
        reason: resolution.status === "ambiguous" ? "destination_ambiguous" : "destination_not_found",
        destinationQuery: input.destination,
        destinationCandidates: resolution.candidates,
        nextStep: resolution.status === "ambiguous" ? "Ask the user to choose a candidate, then call plan_stay again with destinationId." : "Ask the user to clarify the city or country.",
      };
    }
    destination = resolution.destination;
  }
  const searchRequest = {
    destinationId: destination.destinationId,
    checkinDate: input.checkinDate,
    checkoutDate: input.checkoutDate,
    guests: input.rooms.map((room) => ({ adultsCount: room.adults, childrenAge: room.childrenAges })),
    filters: input.breakfastIncluded
      ? [{ $objectType: "array", filterId: "meal_types", values: ["breakfast"] }]
      : [],
  };
  let search;
  try {
    search = await collectHotelSearch(searchRequest, input.ranking, input.language);
  } catch (error) {
    if (input.breakfastIncluded && providerConditionFailure(error)) {
      return providerConditionUnavailable(error, requiredStayConditions(input));
    }
    throw error;
  }
  const hotels = search.hotels;
  if (input.breakfastIncluded && hotels.length === 0) {
    return {
      status: "no_matching_stays",
      reason: "no_hotels_matched_required_conditions",
      resolvedDestination: destination,
      requiredConditions: requiredStayConditions(input),
      conditionsApplied: appliedStayConditions(input),
      searchCoverage: search.metadata,
      retryAllowed: false,
      lowLevelFallbackAllowed: false,
      nextStep: "Report that no matching hotels were returned. Do not remove the breakfast requirement or run low-level filter experiments unless the user explicitly changes the request.",
    };
  }
  const nameMatch = hotelsByName(hotels, input.hotelName);
  if (nameMatch.matchMode === "not_found") {
    return {
      status: "no_matching_hotel",
      resolvedDestination: destination,
      hotelNameQuery: input.hotelName,
      searchedHotelsCount: hotels.length,
      searchCoverage: search.metadata,
      requiredConditions: requiredStayConditions(input),
      conditionsApplied: appliedStayConditions(input),
      suggestions: nameMatch.suggestions,
      nextStep: "Ask the user to confirm one suggested provider hotel name or search the location without hotelName.",
    };
  }
  const journeyId = randomUUID();
  const expiresAt = Date.now() + JOURNEY_TTL_MS;
  const options = nameMatch.hotels.map((hotel) => ({ optionId: randomUUID(), hotel }));
  storeBounded(journeysById, journeyId, { expiresAt, searchRequest, searchMetadata: search.metadata, planInput: input, destination, language: input.language, options, selectedOptionId: null, rateOptions: [], selectedRateOptionId: null }, MAX_ACTIVE_JOURNEYS);
  const displayedOptions = rankedOptions(options, input.ranking).slice(0, input.maxOptions);
  return {
    status: "ready",
    journeyId,
    expiresAt: new Date(expiresAt).toISOString(),
    resolvedDestination: destination,
    hotelNameMatch: nameMatch.matchMode,
    ranking: input.ranking,
    requiredConditions: requiredStayConditions(input),
    conditionsApplied: appliedStayConditions(input),
    totalOptions: options.length,
    returnedOptions: displayedOptions.length,
    searchCoverage: search.metadata,
    options: displayedOptions.map(stayOption),
    note: "Контекст хранится только в текущем MCP-процессе до expiresAt и не содержит токен или auth headers. conditionsApplied подтверждает применение provider-фильтра ко всей journey; утверждайте включение завтрака в показанную цену только при displayedPriceBreakfastEvidence=confirmed_by_meal_name.",
  };
}

function getStayOptions(args) {
  const journey = journeyById(args.journeyId);
  const ranking = args.ranking ?? journey.planInput.ranking ?? "provider_order";
  const limit = boundedInteger(args.limit, "limit", DEFAULT_PLAN_OPTIONS, 1, MAX_PLAN_OPTIONS);
  const options = rankedOptions(journey.options, ranking).slice(0, limit);
  return {
    journeyId: args.journeyId,
    expiresAt: new Date(journey.expiresAt).toISOString(),
    selectedOptionId: journey.selectedOptionId,
    ranking,
    totalOptions: journey.options.length,
    returnedOptions: options.length,
    searchCoverage: journey.searchMetadata,
    requiredConditions: requiredStayConditions(journey.planInput),
    conditionsApplied: appliedStayConditions(journey.planInput),
    options: options.map(stayOption),
  };
}

function selectedJourneyOption(journey, optionId) {
  const option = journey.options.find((candidate) => candidate.optionId === optionId);
  if (!option) throw new Error("optionId is not part of this journey.");
  return option;
}

function compareStayOptions(args) {
  const journey = journeyById(args.journeyId);
  let selected;
  let selectionStrategy = "explicit";
  if (args.optionIds !== undefined && args.ranking === undefined) {
    if (!Array.isArray(args.optionIds) || args.optionIds.length < 2 || args.optionIds.length > 5 || new Set(args.optionIds).size !== args.optionIds.length) {
      throw new Error("optionIds must contain 2 to 5 distinct optionIds.");
    }
    selected = args.optionIds.map((optionId) => selectedJourneyOption(journey, optionId));
  } else {
    const ranking = args.ranking ?? journey.planInput.ranking ?? "provider_order";
    const limit = boundedInteger(args.limit, "limit", 5, 2, 5);
    selected = rankedOptions(journey.options, ranking).slice(0, limit);
    selectionStrategy = ranking;
    if (selected.length < 2) throw new Error("At least two stay options are required for comparison.");
  }
  const comparisonRows = selected.map(stayComparisonRow);
  return {
    journeyId: args.journeyId,
    selectionStrategy,
    selectionScope: selectionStrategy === "explicit" ? "explicit_options" : "all_journey_options",
    searchCoverage: journey.searchMetadata,
    requiredConditions: requiredStayConditions(journey.planInput),
    conditionsApplied: appliedStayConditions(journey.planInput),
    comparison: selected.map(stayOption),
    comparisonRows,
    comparisonTableMarkdown: comparisonTableMarkdown(comparisonRows),
    presentationGuidance: {
      source: "Copy comparisonTableMarkdown into the user-facing answer and explain it from comparisonRows.",
      scope: "Use only hotels in comparisonRows unless the user explicitly asks for alternatives.",
      fields: ["hotelName", "destination", "starRating", "reviewRating", "ratingsCount", "priceAmount", "priceCurrency", "freeCancellationUntil", "mealName", "displayedPriceBreakfastEvidence"],
      breakfastFacts: "confirmed_by_meal_name means included in the displayed price; excluded_by_meal_name means explicitly excluded; not_confirmed_for_displayed_price means unknown for that price.",
      factIntegrity: "Do not round, reinterpret, or replace ratingsCount, priceAmount, freeCancellationUntil, mealName, or evidence.",
    },
    note: "null означает, что соответствующий provider fact отсутствует или не был однозначно извлечён из search response. Используйте displayedPriceBreakfastEvidence как трёхсостоянийное доказательство для показанной цены; conditionsApplied относится только к отбору journey.",
  };
}

function selectStayOption(args) {
  const journey = journeyById(args.journeyId);
  const option = selectedJourneyOption(journey, args.optionId);
  journey.selectedOptionId = option.optionId;
  return {
    journeyId: args.journeyId,
    selectedOption: stayOption(option),
    nextStep: "Use tbank_hotels_get_selected_stay_rates to inspect current rooms and rates before creating a booking draft.",
  };
}

async function selectedStayRates(args) {
  const journey = journeyById(args.journeyId);
  if (!journey.selectedOptionId) throw new Error("Select one stay option before requesting rates.");
  const option = selectedJourneyOption(journey, journey.selectedOptionId);
  const hotelId = optional(option.hotel, "hotelId");
  if (typeof hotelId !== "string" || !hotelId) throw new Error("Selected option does not contain a usable provider hotelId.");
  const apiVersion = version(args, "v3", ["v2", "v3"]);
  if (args.filters !== undefined && !Array.isArray(args.filters)) throw new Error("filters must be an array when provided.");
  const ratesRequest = {
    checkInDate: journey.searchRequest.checkinDate,
    checkOutDate: journey.searchRequest.checkoutDate,
    guests: journey.searchRequest.guests,
    ...(args.filters === undefined ? {} : { filters: args.filters }),
  };
  const path = `/api/${apiVersion}/hotels/${value(hotelId, "hotelId")}/rates`;
  const startedAt = Date.now();
  const timeoutResult = (attempts) => {
    journey.rateOptions = [];
    journey.selectedRateOptionId = null;
    return {
      status: "rates_temporarily_unavailable",
      journeyId: args.journeyId,
      selectedOption: stayOption(option),
      rateOptions: [],
      canCreateBookingDraft: false,
      otherRatesCount: null,
      roomsCount: null,
      sourceStatus: null,
      attempts,
      durationMs: Math.max(0, Date.now() - startedAt),
      failureKind: "timeout",
      nextStep: "The internal timeout retry budget is exhausted. Do not repeat the same tool call automatically; tell the user rates are temporarily unavailable and offer another hotel or a later explicit retry.",
      note: "No booking rate was selected and the search-feed price cannot be used for a booking preview or draft.",
    };
  };
  let attempts = 1;
  let result;
  try {
    result = await apiRequest("POST", path, {
      payload: ratesRequest,
      language: args.language,
      requestTimeoutMs: RATES_FIRST_ATTEMPT_MS,
    });
  } catch (error) {
    const remainingMs = RATES_REQUEST_BUDGET_MS - (Date.now() - startedAt);
    if (error.code !== "HOTELS_API_TIMEOUT") throw error;
    if (remainingMs < MIN_SEARCH_REQUEST_BUDGET_MS) return timeoutResult(attempts);
    attempts = 2;
    try {
      result = await apiRequest("POST", path, {
        payload: ratesRequest,
        language: args.language,
        requestTimeoutMs: remainingMs,
      });
    } catch (retryError) {
      if (retryError.code === "HOTELS_API_TIMEOUT") return timeoutResult(attempts);
      throw retryError;
    }
  }
  const rates = result.data?.payload?.rates;
  if (!Array.isArray(rates)) throw new Error("Hotels API rates response does not contain the expected payload.rates array.");
  journey.rateOptions = rates.map((rate) => ({ rateOptionId: randomUUID(), rate }));
  journey.selectedRateOptionId = null;
  const canCreateBookingDraft = journey.rateOptions.length > 0;
  const otherRates = result.data?.payload?.otherRates;
  const rooms = result.data?.payload?.rooms;
  return {
    status: canCreateBookingDraft ? "ready" : "no_bookable_rates",
    journeyId: args.journeyId,
    selectedOption: stayOption(option),
    rateOptions: journey.rateOptions.map(stayRateOption),
    canCreateBookingDraft,
    otherRatesCount: Array.isArray(otherRates) ? otherRates.length : null,
    roomsCount: Array.isArray(rooms) ? rooms.length : null,
    sourceStatus: result.status,
    attempts,
    durationMs: Math.max(0, Date.now() - startedAt),
    failureKind: null,
    nextStep: canCreateBookingDraft
      ? "Ask the user to choose one returned rateOption before requesting booking guest data."
      : "No selectable rateOption/bookHash is available. Do not request guest personal data and do not create a booking draft; choose another hotel or retry rates later.",
    note: canCreateBookingDraft
      ? "Only returned rateOptions are selectable for booking."
      : "The search-feed price is informational and cannot be used as a booking rate without a provider rateOption/bookHash.",
  };
}

function selectedStayRate(args) {
  const journey = journeyById(args.journeyId);
  if (!journey.selectedOptionId) throw new Error("Select one stay option before selecting a rate.");
  const rateOption = journey.rateOptions.find((candidate) => candidate.rateOptionId === args.rateOptionId);
  if (!rateOption) throw new Error("rateOptionId is not part of this journey. Load current rates again.");
  journey.selectedRateOptionId = rateOption.rateOptionId;
  const executionReadiness = mutationExecutionReadiness("booking");
  return {
    journeyId: args.journeyId,
    selectedRate: stayRateOption(rateOption),
    executionAvailable: executionReadiness.available,
    executionReadiness,
    nextStep: executionReadiness.available
      ? "For a preview without personal data, call tbank_hotels_create_booking_preview. Collect guest PII and call tbank_hotels_create_booking_draft only after the user explicitly chooses real booking."
      : "Call tbank_hotels_create_booking_preview without requesting guest PII. Execution is unavailable, so do not create a booking draft or ask for final confirmation.",
  };
}

function createBookingPreview(args) {
  const journey = journeyById(args.journeyId);
  if (!journey.selectedOptionId) throw new Error("Select one stay option before creating a booking preview.");
  if (!journey.selectedRateOptionId) throw new Error("Select one current rate before creating a booking preview.");
  const rateOption = journey.rateOptions.find((candidate) => candidate.rateOptionId === journey.selectedRateOptionId);
  if (!rateOption) throw new Error("Selected rate is no longer part of this journey. Load current rates again.");
  const bookHash = optional(rateOption.rate, "bookHash");
  if (typeof bookHash !== "string" || !bookHash) throw new Error("Selected rate does not contain a usable bookHash.");
  const executionReadiness = mutationExecutionReadiness("booking");
  return {
    status: "preview_only",
    journeyId: args.journeyId,
    executionAvailable: executionReadiness.available,
    executionReadiness,
    selectedStay: stayOption(selectedJourneyOption(journey, journey.selectedOptionId)),
    selectedRate: stayRateOption(rateOption),
    occupancy: journey.searchRequest.guests.map((guest, roomIndex) => ({
      roomIndex,
      adults: guest.adultsCount,
      childrenAges: guest.childrenAge ?? [],
    })),
    personalDataCollected: false,
    httpRequestPerformed: false,
    bookingDataRequiredForExecution: ["guestContact.email", "guestContact.phone", "rooms[].guests[].firstName", "rooms[].guests[].lastName"],
    nextStep: executionReadiness.available
      ? "Show this preview without requesting personal data. Only if the user explicitly asks to make a real booking, collect required guest data and create a booking draft."
      : "Show this preview and stop. Do not request guest personal data or final confirmation while booking execution is unavailable.",
    note: "Локальный preview не резервирует номер, не проверяет checkout и не выполняет HTTP-запрос к Hotels API.",
  };
}

function bookingGuestCoverage(journey, bookingPayload) {
  const rooms = journey.searchRequest.guests.map((requested, index) => {
    const namedGuests = bookingPayload.rooms[index]?.guests ?? [];
    const namedChildrenAges = namedGuests.filter((guest) => Number.isInteger(guest.childAge)).map((guest) => guest.childAge);
    return {
      roomIndex: index,
      requestedAdults: requested.adultsCount,
      requestedChildrenAges: requested.childrenAge ?? [],
      namedAdults: namedGuests.filter((guest) => !Number.isInteger(guest.childAge)).length,
      namedChildrenAges,
    };
  });
  const roomCountMatches = bookingPayload.rooms.length === journey.searchRequest.guests.length;
  const namedGuestCountMatches = roomCountMatches && rooms.every((room) => (
    room.namedAdults === room.requestedAdults
    && JSON.stringify([...room.namedChildrenAges].sort((a, b) => a - b)) === JSON.stringify([...room.requestedChildrenAges].sort((a, b) => a - b))
  ));
  return {
    roomCountMatches,
    namedGuestCountMatches,
    rooms,
    note: namedGuestCountMatches
      ? "Named guests match the searched occupancy."
      : "Named guests do not match the searched occupancy. OpenAPI describes rooms.guests as residents but does not declare an exact cross-field count constraint; review before execution.",
  };
}

function createBookingDraft(args) {
  const journey = journeyById(args.journeyId);
  if (!journey.selectedRateOptionId) throw new Error("Select one current rate before creating a booking draft.");
  requestObject(args.bookingData, "bookingData");
  if (Object.hasOwn(args.bookingData, "bookHash")) throw new Error("bookingData must not contain bookHash; it is bound to the selected rate.");
  validatedBookingPayload(args.bookingData, { requireBookHash: false, name: "bookingData" });
  const rateOption = journey.rateOptions.find((candidate) => candidate.rateOptionId === journey.selectedRateOptionId);
  const bookHash = optional(rateOption?.rate, "bookHash");
  if (typeof bookHash !== "string" || !bookHash) throw new Error("Selected rate does not contain a usable bookHash.");
  const bookingDraftId = randomUUID();
  const bookingPayload = { ...args.bookingData, bookHash };
  const expiresAt = Date.now() + BOOKING_DRAFT_TTL_MS;
  const executionReadiness = mutationExecutionReadiness("booking");
  const executionAvailable = executionReadiness.available;
  storeBounded(bookingDraftsById, bookingDraftId, {
    expiresAt,
    bookingPayload,
    journeyId: args.journeyId,
    rateOption,
    confirmationState: "ready",
  }, MAX_ACTIVE_BOOKING_DRAFTS);
  return {
    bookingDraftId,
    expiresAt: new Date(expiresAt).toISOString(),
    executionAvailable,
    executionReadiness,
    selectedStay: stayOption(selectedJourneyOption(journey, journey.selectedOptionId)),
    selectedRate: stayRateOption(rateOption),
    guestCoverage: bookingGuestCoverage(journey, bookingPayload),
    bookingPreview: redactPreview(bookingPayload),
    nextStep: executionAvailable
      ? "Call tbank_hotels_validate_checkout immediately before asking for the final booking confirmation."
      : "Preview only. If current provider terms are needed, call tbank_hotels_validate_checkout and then tbank_hotels_prepare_draft_booking. Do not ask the user for final execution confirmation.",
  };
}

async function validateCheckout(args) {
  const draft = bookingDraftById(args.bookingDraftId);
  const bookHash = value(draft.bookingPayload.bookHash, "bookHash");
  const path = `/api/v3/rates/${bookHash}`;
  const startedAt = Date.now();
  let attempts = 1;
  let result;
  try {
    result = await apiRequest("GET", path, { requestTimeoutMs: CHECKOUT_FIRST_ATTEMPT_MS });
  } catch (error) {
    const remainingMs = CHECKOUT_REQUEST_BUDGET_MS - (Date.now() - startedAt);
    if (error.code !== "HOTELS_API_TIMEOUT" || remainingMs < MIN_SEARCH_REQUEST_BUDGET_MS) throw error;
    attempts = 2;
    result = await apiRequest("GET", path, { requestTimeoutMs: remainingMs });
  }
  draft.validationExpiresAt = Date.now() + CHECKOUT_VALIDATION_TTL_MS;
  return {
    bookingDraftId: args.bookingDraftId,
    attempts,
    validatedUntil: new Date(draft.validationExpiresAt).toISOString(),
    selectedRate: stayRateOption(draft.rateOption),
    checkout: result,
    note: "Provider checkout response is authoritative. Review it with the user before preparing final confirmation.",
  };
}

function preparedDraftBooking(args) {
  const draft = bookingDraftById(args.bookingDraftId);
  if (draft.confirmationState === "confirming") throw new Error("Booking confirmation is already in progress for this draft.");
  if (draft.confirmationState === "outcome_unknown") throw new Error("The previous booking confirmation outcome is unknown. Do not retry creation; reconcile the provider task or order status outside this draft.");
  if (!draft.validationExpiresAt || draft.validationExpiresAt <= Date.now()) throw new Error("Checkout validation is required and must be fresh before booking confirmation.");
  const path = "/api/v1/hotels/bookings/tasks/create";
  const executionReadiness = mutationExecutionReadiness("booking");
  if (!executionReadiness.available) {
    return {
      status: "preview_only",
      bookingDraftId: args.bookingDraftId,
      executionAvailable: false,
      executionReadiness,
      endpoint: path,
      payloadPreview: redactPreview(draft.bookingPayload),
      nextStep: "Execution is unavailable in this configuration. Do not ask the user for final execution confirmation; an integration owner must configure and approve the execution profile outside the model conversation.",
      note: "HTTP-запрос не выполнен; confirmation и requestHash намеренно не выданы.",
    };
  }
  const window = preparationWindow();
  const hash = requestHash("booking", path, { payload: draft.bookingPayload, ...window });
  return {
    bookingDraftId: args.bookingDraftId,
    executionAvailable: true,
    executionReadiness,
    requestHash: hash,
    confirmation: confirmationPhrase("booking", hash),
    ...window,
    endpoint: path,
    payloadPreview: redactPreview(draft.bookingPayload),
    note: "HTTP-запрос не выполнен. Получите явное подтверждение пользователя непосредственно перед confirm_booking.",
  };
}

async function confirmBooking(args) {
  requireMutationExecutionReady("booking");
  const draft = bookingDraftById(args.bookingDraftId);
  if (draft.confirmationState === "confirming") throw new Error("Booking confirmation is already in progress for this draft.");
  if (draft.confirmationState === "outcome_unknown") throw new Error("The previous booking confirmation outcome is unknown. Do not retry creation; reconcile the provider task or order status outside this draft.");
  if (!draft.validationExpiresAt || draft.validationExpiresAt <= Date.now()) throw new Error("Checkout validation is required and must be fresh before booking confirmation.");
  validatePreparationWindow(args);
  const path = "/api/v1/hotels/bookings/tasks/create";
  const hash = requestHash("booking", path, { payload: draft.bookingPayload, preparedAt: args.preparedAt, expiresAt: args.expiresAt });
  if (args.preparedRequestHash !== hash) throw new Error("preparedRequestHash does not match this booking draft.");
  if (args.confirmation !== confirmationPhrase("booking", hash)) throw new Error("confirmation must exactly match the phrase returned by tbank_hotels_prepare_draft_booking.");
  draft.confirmationState = "confirming";
  try {
    const result = await apiRequest("POST", path, { payload: draft.bookingPayload });
    bookingDraftsById.delete(args.bookingDraftId);
    return result;
  } catch (error) {
    if (error.code === "HOTELS_API_TIMEOUT" || error.code === "HOTELS_API_NETWORK" || (error.httpStatus ?? 0) >= 500) {
      draft.confirmationState = "outcome_unknown";
    } else {
      draft.confirmationState = "ready";
    }
    throw error;
  }
}

async function bookingOverview(args) {
  const apiVersion = version(args, "v3", ["v1", "v2", "v3"]);
  const orderId = value(args.orderId, "orderId");
  const booking = await apiRequest("GET", `/api/${apiVersion}/hotels/bookings/${orderId}`);
  if (args.includeVoucher === false) return { booking, voucher: { requested: false } };
  return {
    booking,
    voucher: {
      requested: true,
      documentContentIncluded: false,
      separateHandoffRequired: true,
      availableViaTool: authBrokerSocket() ? "tbank_hotels_save_voucher" : null,
      note: "Binary voucher is never fetched or embedded by booking_overview. Use the local broker handoff only after an explicit user request.",
    },
  };
}

async function previewCancellation(args) {
  const apiVersion = version(args, "v3", ["v1", "v2", "v3"]);
  const booking = await apiRequest("GET", `/api/${apiVersion}/hotels/bookings/${value(args.orderId, "orderId")}`);
  return { booking, note: "MCP не вычисляет сумму возврата. Используйте только фактически возвращённые provider условия, затем tbank_hotels_prepare_cancel_booking при явном намерении отменить." };
}

async function repeatStayPlan(args) {
  const journey = journeyById(args.journeyId);
  return planStay({ ...journey.planInput, destinationId: journey.destination.destinationId, checkinDate: args.checkinDate, checkoutDate: args.checkoutDate });
}

function value(value, name) {
  if (typeof value !== "string" || !value.trim()) throw new Error(`${name} must be a non-empty string.`);
  if (value.length > 512 || value.includes("/") || value.includes("?")) throw new Error(`${name} contains unsupported path characters.`);
  return encodeURIComponent(value);
}

function brokerIdentifier(identifier, name) {
  if (typeof identifier !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(identifier)) {
    throw new Error(`${name} contains unsupported characters.`);
  }
  return identifier;
}

function requestObject(value, name = "payload") {
  if (!value || Array.isArray(value) || typeof value !== "object") throw new Error(`${name} must be an object.`);
  return value;
}

function requireNonEmptyString(value, name) {
  if (typeof value !== "string" || !value.trim()) throw new Error(`${name} must be a non-empty string.`);
}

function optionalNullableString(value, name) {
  if (value !== undefined && value !== null && typeof value !== "string") throw new Error(`${name} must be a string or null.`);
}

function validatedBookingPayload(payloadValue, { requireBookHash = true, ls = false, name = "payload" } = {}) {
  const body = requestObject(payloadValue, name);
  const commonKeys = ["checkOutId", "guestContact", "rooms", "contactData", "arrivalTime", "promocode", "extraServices"];
  const allowed = [...(requireBookHash ? ["bookHash"] : []), ...commonKeys, ...(ls ? [] : ["paymentData", "paymentMeans", "userData", "userIp"] )];
  assertOnlyKeys(body, allowed, name);
  if (requireBookHash) requireNonEmptyString(body.bookHash, `${name}.bookHash`);
  optionalNullableString(body.checkOutId, `${name}.checkOutId`);

  const guestContact = requestObject(body.guestContact, `${name}.guestContact`);
  assertOnlyKeys(guestContact, ["email", "phone", "comment"], `${name}.guestContact`);
  requireNonEmptyString(guestContact.email, `${name}.guestContact.email`);
  requireNonEmptyString(guestContact.phone, `${name}.guestContact.phone`);
  optionalNullableString(guestContact.comment, `${name}.guestContact.comment`);

  if (!Array.isArray(body.rooms)) throw new Error(`${name}.rooms must be an array.`);
  body.rooms.forEach((room, roomIndex) => {
    requestObject(room, `${name}.rooms[${roomIndex}]`);
    assertOnlyKeys(room, ["guests"], `${name}.rooms[${roomIndex}]`);
    if (!Array.isArray(room.guests)) throw new Error(`${name}.rooms[${roomIndex}].guests must be an array.`);
    room.guests.forEach((guest, guestIndex) => {
      const guestName = `${name}.rooms[${roomIndex}].guests[${guestIndex}]`;
      requestObject(guest, guestName);
      assertOnlyKeys(guest, ["firstName", "lastName", "childAge"], guestName);
      requireNonEmptyString(guest.firstName, `${guestName}.firstName`);
      requireNonEmptyString(guest.lastName, `${guestName}.lastName`);
      if (guest.childAge !== undefined && guest.childAge !== null && !Number.isInteger(guest.childAge)) throw new Error(`${guestName}.childAge must be an integer or null.`);
    });
  });

  if (body.contactData !== undefined && body.contactData !== null) {
    const contact = requestObject(body.contactData, `${name}.contactData`);
    assertOnlyKeys(contact, ["firstName", "lastName", "email"], `${name}.contactData`);
    if (typeof contact.firstName !== "string" || typeof contact.lastName !== "string") throw new Error(`${name}.contactData requires string firstName and lastName.`);
    optionalNullableString(contact.email, `${name}.contactData.email`);
  }
  if (body.arrivalTime !== undefined && body.arrivalTime !== null) {
    const arrival = requestObject(body.arrivalTime, `${name}.arrivalTime`);
    assertOnlyKeys(arrival, ["type", "from", "to"], `${name}.arrivalTime`);
    for (const key of ["type", "from", "to"]) optionalNullableString(arrival[key], `${name}.arrivalTime.${key}`);
  }
  optionalNullableString(body.promocode, `${name}.promocode`);
  if (body.extraServices !== undefined && body.extraServices !== null) {
    const extras = requestObject(body.extraServices, `${name}.extraServices`);
    assertOnlyKeys(extras, ["earlyCheckInId", "lateCheckOutId", "guaranteedRefundSelected"], `${name}.extraServices`);
    optionalNullableString(extras.earlyCheckInId, `${name}.extraServices.earlyCheckInId`);
    optionalNullableString(extras.lateCheckOutId, `${name}.extraServices.lateCheckOutId`);
    if (extras.guaranteedRefundSelected !== undefined && extras.guaranteedRefundSelected !== null && typeof extras.guaranteedRefundSelected !== "boolean") {
      throw new Error(`${name}.extraServices.guaranteedRefundSelected must be a boolean or null.`);
    }
  }

  if (!ls) {
    if (body.paymentData !== undefined && body.paymentData !== null) {
      const paymentData = requestObject(body.paymentData, `${name}.paymentData`);
      assertOnlyKeys(paymentData, ["creditCardId"], `${name}.paymentData`);
      optionalNullableString(paymentData.creditCardId, `${name}.paymentData.creditCardId`);
    }
    if (body.paymentMeans !== undefined && body.paymentMeans !== null && !["payment_form", "on_us", "off_us", "dolyame"].includes(body.paymentMeans)) {
      throw new Error(`${name}.paymentMeans is unsupported.`);
    }
    const hasUserData = Object.hasOwn(body, "userData");
    const hasUserIp = Object.hasOwn(body, "userIp");
    if (hasUserData !== hasUserIp) throw new Error(`${name}.userData and ${name}.userIp must be provided together.`);
    if (hasUserData) {
      const userData = requestObject(body.userData, `${name}.userData`);
      assertOnlyKeys(userData, ["ssoId", "siebelId", "phoneNumber"], `${name}.userData`);
      for (const key of ["ssoId", "siebelId", "phoneNumber"]) optionalNullableString(userData[key], `${name}.userData.${key}`);
      requireNonEmptyString(body.userIp, `${name}.userIp`);
    }
  }
  return body;
}

function validatedBookingsListArgs(args) {
  requestObject(args, "arguments");
  assertOnlyKeys(args, ["isActiveRequired", "isCancelledRequired", "isCompletedRequired"], "arguments");
  for (const name of ["isActiveRequired", "isCancelledRequired", "isCompletedRequired"]) {
    if (typeof args[name] !== "boolean") throw new Error(`${name} must be a boolean.`);
  }
  return {
    isActiveRequired: args.isActiveRequired,
    isCancelledRequired: args.isCancelledRequired,
    isCompletedRequired: args.isCompletedRequired,
  };
}

function validatedTrancheAmountArgs(args) {
  requestObject(args, "arguments");
  assertOnlyKeys(args, ["accounts"], "arguments");
  if (!Array.isArray(args.accounts) || args.accounts.length > 100) throw new Error("accounts must be an array with at most 100 items.");
  return {
    accounts: args.accounts.map((account, index) => {
      requestObject(account, `accounts[${index}]`);
      assertOnlyKeys(account, ["accountId", "type", "balance"], `accounts[${index}]`);
      if (typeof account.accountId !== "string" || !account.accountId.trim()) throw new Error(`accounts[${index}].accountId must be a non-empty string.`);
      if (typeof account.type !== "string" || !account.type.trim()) throw new Error(`accounts[${index}].type must be a non-empty string.`);
      if (typeof account.balance !== "number" || !Number.isFinite(account.balance)) throw new Error(`accounts[${index}].balance must be a finite number.`);
      return { accountId: account.accountId, type: account.type, balance: account.balance };
    }),
  };
}

function requestHash(action, path, args) {
  const material = JSON.stringify({ action, path, payload: args.payload, orderId: args.orderId, bookHash: args.bookHash, preparedAt: args.preparedAt, expiresAt: args.expiresAt });
  return createHash("sha256").update(material).digest("hex");
}

function preparationWindow() {
  const preparedAt = Date.now();
  return {
    preparedAt: new Date(preparedAt).toISOString(),
    expiresAt: new Date(preparedAt + PREPARED_CONFIRMATION_TTL_MS).toISOString(),
  };
}

function validatePreparationWindow(args) {
  const preparedAt = Date.parse(args.preparedAt);
  const expiresAt = Date.parse(args.expiresAt);
  if (!Number.isFinite(preparedAt) || !Number.isFinite(expiresAt)) throw new Error("preparedAt and expiresAt must be valid timestamps from the prepare response.");
  if (expiresAt <= preparedAt || expiresAt - preparedAt > PREPARED_CONFIRMATION_TTL_MS) throw new Error("Prepared confirmation window is invalid.");
  const now = Date.now();
  if (preparedAt > now + 30_000) throw new Error("Prepared confirmation timestamp is in the future.");
  if (expiresAt <= now) throw new Error("Prepared confirmation has expired. Prepare and review the action again.");
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

function mutationRequestBody(tool, args) {
  switch (tool._action) {
    case "booking":
      return validatedBookingPayload(args.payload);
    case "lsBooking":
      return validatedBookingPayload(args.payload, { ls: true });
    case "cancel":
      assertOnlyKeys(args, tool._execute
        ? ["orderId", "preparedRequestHash", "confirmation", "preparedAt", "expiresAt"]
        : ["orderId"], "arguments");
      if (typeof args.orderId !== "string" || !args.orderId.trim()) throw new Error("orderId must be a non-empty string.");
      return { orderId: args.orderId };
    case "paymentSetup":
      assertOnlyKeys(args, tool._execute
        ? ["orderId", "preparedRequestHash", "confirmation", "preparedAt", "expiresAt"]
        : ["orderId"], "arguments");
      return undefined;
    case "applyPromocode":
      assertOnlyKeys(args, tool._execute
        ? ["bookHash", "promocode", "preparedRequestHash", "confirmation", "preparedAt", "expiresAt"]
        : ["bookHash", "promocode"], "arguments");
      if (args.promocode !== null && typeof args.promocode !== "string") throw new Error("promocode must be a string or null.");
      return { promocode: args.promocode };
    case "extraServices":
      assertOnlyKeys(args, tool._execute
        ? ["bookHash", "extraServiceIds", "preparedRequestHash", "confirmation", "preparedAt", "expiresAt"]
        : ["bookHash", "extraServiceIds"], "arguments");
      if (!Array.isArray(args.extraServiceIds) || !args.extraServiceIds.every((item) => typeof item === "string" && item.length > 0)) {
        throw new Error("extraServiceIds must be an array of non-empty strings.");
      }
      return { extraServiceIds: args.extraServiceIds };
    default:
      throw new Error("Unsupported mutation.");
  }
}

function redactPreview(payloadValue) {
  if (Array.isArray(payloadValue)) return payloadValue.map(redactPreview);
  if (!payloadValue || typeof payloadValue !== "object") return payloadValue;
  const secretNames = /password|token|authorization|card|pan|cvv|cvc|phone|email|passport|birth|document/i;
  const guestNameKeys = new Set(["firstname", "lastname", "middlename", "surname", "givenname", "familyname", "patronymic", "fullname", "guestname", "ssoid", "siebelid", "userip", "ipaddress"]);
  return Object.fromEntries(Object.entries(payloadValue).map(([key, item]) => {
    const normalizedKey = key.replace(/[^A-Za-z]/g, "").toLowerCase();
    return [key, secretNames.test(key) || guestNameKeys.has(normalizedKey) ? "[REDACTED]" : redactPreview(item)];
  }));
}

function assertOnlyKeys(object, allowed, name) {
  const unexpected = Object.keys(object).filter((key) => !allowed.includes(key));
  if (unexpected.length) throw new Error(`${name} contains unsupported fields: ${unexpected.join(", ")}.`);
}

function optionalNonNegativeInteger(value, name, minimum = 0) {
  if (value !== undefined && (!Number.isInteger(value) || value < minimum)) throw new Error(`${name} must be an integer greater than or equal to ${minimum}.`);
}

function validatedSeoSearchArgs(args) {
  const apiVersion = version(args, "v3", ["v1", "v2", "v3"]);
  const body = requestObject(args.payload);
  if (apiVersion === "v1") {
    if (!Number.isInteger(body.destinationId) || body.destinationId <= 0) throw new Error("payload.destinationId must be a positive integer for SEO v1.");
    assertOnlyKeys(body, ["destinationId", "hostelIsNeeded", "guesthouseIsNeeded"], "payload");
    for (const key of ["hostelIsNeeded", "guesthouseIsNeeded"]) {
      if (body[key] !== undefined && typeof body[key] !== "boolean") throw new Error(`payload.${key} must be boolean.`);
    }
  } else if (apiVersion === "v2") {
    if (!Number.isInteger(body.locationId) || body.locationId <= 0) throw new Error("payload.locationId must be a positive integer for SEO v2.");
    assertOnlyKeys(body, ["locationId", "offset", "limit", "filter"], "payload");
    optionalNonNegativeInteger(body.offset, "payload.offset");
    optionalNonNegativeInteger(body.limit, "payload.limit", 1);
    if (body.filter !== undefined) requestObject(body.filter, "payload.filter");
  } else {
    if (typeof body.country !== "string" || !body.country.trim()) throw new Error("payload.country must be a non-empty string for SEO v3.");
    if (typeof body.location !== "string" || !body.location.trim()) throw new Error("payload.location must be a non-empty string for SEO v3.");
    assertOnlyKeys(body, ["country", "location", "offset", "limit", "filter"], "payload");
    optionalNonNegativeInteger(body.offset, "payload.offset");
    optionalNonNegativeInteger(body.limit, "payload.limit", 1);
    if (body.filter !== undefined) requestObject(body.filter, "payload.filter");
  }
  return { apiVersion, payload: body };
}

function safeDiagnosticToken(value) {
  return typeof value === "string" && /^[A-Za-z0-9_.:-]{1,128}$/.test(value) ? value : null;
}

function providerErrorCode(responseBody) {
  if (!responseBody || typeof responseBody !== "object") return null;
  const queue = [{ value: responseBody, depth: 0 }];
  while (queue.length) {
    const { value, depth } = queue.shift();
    if (!value || typeof value !== "object" || depth > 3) continue;
    for (const [key, item] of Object.entries(value)) {
      if (/^(errorCode|code|type)$/i.test(key)) {
        const safe = safeDiagnosticToken(item);
        if (safe) return safe;
      }
      if (item && typeof item === "object") queue.push({ value: item, depth: depth + 1 });
    }
  }
  return null;
}

async function boundedResponseText(response) {
  const declaredLength = Number(response.headers.get("content-length"));
  if (Number.isFinite(declaredLength) && declaredLength > MAX_PROVIDER_RESPONSE_BYTES) {
    await response.body?.cancel();
    throw new Error("Hotels API response exceeded the safe size limit.");
  }
  if (!response.body) return "";
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let size = 0;
  let result = "";
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    size += value.byteLength;
    if (size > MAX_PROVIDER_RESPONSE_BYTES) {
      await reader.cancel();
      throw new Error("Hotels API response exceeded the safe size limit.");
    }
    result += decoder.decode(value, { stream: true });
  }
  return result + decoder.decode();
}

async function apiRequest(method, path, { payload: body, query, language, requestTimeoutMs } = {}) {
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
  return withProviderRequestSlot(async () => {
    let response;
    try {
      const effectiveTimeoutMs = requestTimeoutMs === undefined ? timeoutMs() : Math.min(timeoutMs(), Math.max(1, Math.floor(requestTimeoutMs)));
      response = await fetch(target, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), redirect: "error", signal: AbortSignal.timeout(effectiveTimeoutMs) });
    } catch (error) {
      if (error.name === "TimeoutError") {
        const timeoutError = new Error("Hotels API request timed out.");
        timeoutError.code = "HOTELS_API_TIMEOUT";
        throw timeoutError;
      }
      const networkCode = safeDiagnosticToken(error?.cause?.code);
      const networkError = new Error(`Unable to reach Hotels API${networkCode ? ` (${networkCode})` : ""}.`);
      networkError.code = "HOTELS_API_NETWORK";
      throw networkError;
    }
    const responseText = await boundedResponseText(response);
    let responseBody = null;
    if (responseText) {
      try { responseBody = JSON.parse(responseText); } catch { responseBody = responseText; }
    }
    if (!response.ok) {
      const code = providerErrorCode(responseBody);
      const requestId = safeDiagnosticToken(response.headers.get("x-request-id")) || safeDiagnosticToken(response.headers.get("x-correlation-id"));
      const details = [code ? `code: ${code}` : null, requestId ? `requestId: ${requestId}` : null].filter(Boolean);
      const providerError = new Error(`Hotels API returned HTTP ${response.status}${details.length ? ` (${details.join(", ")})` : ""}.`);
      providerError.code = "HOTELS_API_HTTP";
      providerError.httpStatus = response.status;
      throw providerError;
    }
    return { status: response.status, data: responseBody };
  });
}

function version(args, fallback, allowed) {
  const selected = args.apiVersion ?? fallback;
  if (!allowed.includes(selected)) throw new Error(`apiVersion must be one of: ${allowed.join(", ")}.`);
  return selected;
}

export async function callTool(name, args = {}) {
  if (!args || typeof args !== "object" || Array.isArray(args)) throw new Error("Tool arguments must be an object.");
  const mutation = tools.find((tool) => tool.name === name && tool._action);
  if (mutation) return callMutation(mutation, args);
  switch (name) {
    case "tbank_hotels_connection_status": return connectionStatus();
    case "tbank_hotels_get_customer": return getCustomer();
    case "tbank_hotels_search": return guardedProviderSearchRequest("POST", "/api/v1/hotels/search", args);
    case "tbank_hotels_resolve_destination": return resolveDestination(args);
    case "tbank_hotels_plan_stay": return planStay(args);
    case "tbank_hotels_get_stay_options": return getStayOptions(args);
    case "tbank_hotels_compare_stay_options": return compareStayOptions(args);
    case "tbank_hotels_select_stay_option": return selectStayOption(args);
    case "tbank_hotels_get_selected_stay_rates": return selectedStayRates(args);
    case "tbank_hotels_select_stay_rate": return selectedStayRate(args);
    case "tbank_hotels_create_booking_preview": return createBookingPreview(args);
    case "tbank_hotels_create_booking_draft": return createBookingDraft(args);
    case "tbank_hotels_validate_checkout": return validateCheckout(args);
    case "tbank_hotels_prepare_draft_booking": return preparedDraftBooking(args);
    case "tbank_hotels_confirm_booking": return confirmBooking(args);
    case "tbank_hotels_get_booking_overview": return bookingOverview(args);
    case "tbank_hotels_preview_cancellation": return previewCancellation(args);
    case "tbank_hotels_repeat_stay_plan": return repeatStayPlan(args);
    case "tbank_hotels_get_search_filters": return apiRequest("GET", `/api/${version(args, "v1", ["v1", "v2"])}/hotels/search-filters`);
    case "tbank_hotels_get_filter_availability": return guardedProviderSearchRequest("POST", "/api/v1/hotels/search-filters-availability", args);
    case "tbank_hotels_search_map": return providerSearchRequest("POST", "/api/v1/hotels/map/search", args);
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
    case "tbank_hotels_get_booking": {
      const v = version(args, authBrokerSocket() ? "v1" : "v3", ["v1", "v2", "v3"]);
      if (authBrokerSocket()) {
        if (v !== "v1") throw new Error("Mobile auth broker supports tbank_hotels_get_booking only with apiVersion=v1.");
        if (args.orderId !== undefined) throw new Error("Mobile auth broker mode does not accept provider orderId. Use bookingRef from tbank_hotels_list_bookings.");
        const bookingRef = args.bookingRef;
        const orderId = orderIdForBookingReference(bookingRef);
        const result = await authBrokerRequest("hotels.get_booking_v1", { bookingId: orderId });
        return bookingWithReference(result.booking, bookingRef);
      }
      if (args.bookingRef !== undefined) throw new Error("bookingRef is available only with the mobile auth broker. Direct API profiles require orderId.");
      return apiRequest("GET", `/api/${v}/hotels/bookings/${value(args.orderId, "orderId")}`, args);
    }
    case "tbank_hotels_list_bookings": {
      const payload = validatedBookingsListArgs(args);
      if (authBrokerSocket()) {
        const result = await authBrokerRequest("hotels.list_bookings", payload);
        return bookingListWithReferences(result.bookings);
      }
      return apiRequest("POST", "/api/v1/hotels/bookings/booking_list", { payload });
    }
    case "tbank_hotels_summarize_bookings": {
      const payload = { isActiveRequired: true, isCancelledRequired: true, isCompletedRequired: true };
      if (authBrokerSocket()) {
        const result = await authBrokerRequest("hotels.list_bookings", payload);
        return bookingListSummary(result.bookings);
      }
      return bookingListSummary(await apiRequest("POST", "/api/v1/hotels/bookings/booking_list", { payload }));
    }
    case "tbank_hotels_get_voucher": throw new Error("Inline voucher delivery is disabled because PDF content must not enter MCP JSON. Use tbank_hotels_save_voucher with bookingRef and the local auth broker.");
    case "tbank_hotels_save_voucher": {
      if (!authBrokerSocket()) throw new Error("Safe voucher handoff requires the local mobile auth broker.");
      if (args.orderId !== undefined) throw new Error("Provider orderId is not accepted. Use bookingRef from tbank_hotels_list_bookings.");
      const bookingRef = args.bookingRef;
      const orderId = orderIdForBookingReference(bookingRef);
      const result = await authBrokerRequest("hotels.save_voucher_v1", { bookingId: orderId });
      if (!result?.voucher || result.voucher.documentContentIncluded !== false) {
        throw new Error("Auth broker returned an unsafe voucher response.");
      }
      return { status: "saved_locally", bookingRef, ...result };
    }
    case "tbank_hotels_create_payment_handoff_preview": {
      if (!authBrokerSocket()) throw new Error("Hotel payment handoff preview requires the shared local auth broker.");
      if (args.orderId !== undefined) throw new Error("Provider orderId is not accepted. Use bookingRef from tbank_hotels_list_bookings.");
      const bookingRef = args.bookingRef;
      const orderId = orderIdForBookingReference(bookingRef);
      const result = await authBrokerRequest("hotels.create_payment_handoff", { bookingId: orderId });
      if (!result || result.bookingBindingVerified !== true || typeof result.paymentHandoffRef !== "string") {
        throw new Error("Auth broker returned an invalid hotel payment handoff.");
      }
      return {
        status: "preview_ready",
        bookingRef,
        ...result,
        paymentSetupPerformed: false,
        paymentExecutionPerformed: false,
      };
    }
    case "tbank_hotels_get_reservation": return apiRequest("GET", "/api/v1/hotels/bookings/getReservation", args);
    case "tbank_hotels_get_evo_booking": return apiRequest("GET", `/api/v1/hotels/bookings/evo/${value(args.orderId, "orderId")}`);
    case "tbank_hotels_get_bnpl_offer": return apiRequest("POST", `/api/v1/hotels/bookings/evo/${value(args.orderId, "orderId")}/bnpl_offer`, { language: args.language });
    case "tbank_hotels_get_booking_task_status": return apiRequest("GET", `/api/v1/hotels/bookings/tasks/${value(args.taskId, "taskId")}/status`);
    case "tbank_hotels_check_ls_order": return apiRequest("GET", `/api/v1/hotels/bookings/ls/check_orders/${value(args.orderId, "orderId")}`);
    case "tbank_hotels_get_reviews": {
      if (!["ratings", "summary", "feedback", "feedback-filters"].includes(args.resource)) throw new Error("resource is unsupported.");
      return apiRequest("GET", `/api/v1/review/${value(args.hotelId, "hotelId")}/${args.resource}`, args);
    }
    case "tbank_hotels_get_review_order_status": return apiRequest("GET", `/api/v1/review/order-status/${value(args.orderId, "orderId")}`);
    case "tbank_hotels_search_seo": {
      const validated = validatedSeoSearchArgs(args);
      return apiRequest("POST", `/api/${validated.apiVersion}/seo/search`, { payload: validated.payload });
    }
    case "tbank_hotels_search_urls": return apiRequest("POST", `/api/${version(args, "v3", ["v1", "v2", "v3"])}/hotels/urls/search`, args);
    case "tbank_hotels_get_seo_resource": return seoResource(args);
    case "tbank_hotels_get_deeplink_token": {
      if (args.kind === "general") return apiRequest("GET", "/api/v1/get-link-token");
      if (args.kind === "hotels-urls") return apiRequest("GET", "/api/v1/hotels/urls/link-token");
      throw new Error("kind must be general or hotels-urls.");
    }
    case "tbank_hotels_get_available_tranche_amount": return apiRequest("POST", "/api/v1/tranches/available/amount", { payload: validatedTrancheAmountArgs(args) });
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
  const path = mutationPath(tool._action, args);
  const body = mutationRequestBody(tool, args);
  if (!tool._execute) {
    const executionReadiness = mutationExecutionReadiness(tool._action);
    if (!executionReadiness.available) {
      return {
        status: "preview_only",
        action: tool._action,
        executionAvailable: false,
        executionReadiness,
        endpoint: path,
        payloadPreview: redactPreview(body),
        nextStep: "Execution is unavailable in this configuration. Do not ask the user for confirmation; configuration and approval belong outside the model conversation.",
        note: "HTTP-запрос не выполнен; confirmation и requestHash намеренно не выданы.",
      };
    }
    const window = preparationWindow();
    const hash = requestHash(tool._action, path, { payload: body, orderId: args.orderId, bookHash: args.bookHash, ...window });
    return {
      action: tool._action,
      executionAvailable: true,
      executionReadiness,
      requestHash: hash,
      confirmation: confirmationPhrase(tool._action, hash),
      ...window,
      endpoint: path,
      payloadPreview: redactPreview(body),
      note: "HTTP-запрос не выполнен. Получите явное подтверждение пользователя непосредственно перед execute-вызовом.",
    };
  }
  requireMutationExecutionReady(tool._action);
  validatePreparationWindow(args);
  const hash = requestHash(tool._action, path, { payload: body, orderId: args.orderId, bookHash: args.bookHash, preparedAt: args.preparedAt, expiresAt: args.expiresAt });
  const phrase = confirmationPhrase(tool._action, hash);
  if (args.preparedRequestHash !== hash) throw new Error("preparedRequestHash does not match this exact request. Prepare and review the action again.");
  if (args.confirmation !== phrase) throw new Error("confirmation must exactly match the phrase returned by the corresponding prepare call.");
  startTrackedMutationExecution(hash, Date.parse(args.expiresAt) + PREPARED_CONFIRMATION_TTL_MS);
  try {
    const result = await apiRequest("POST", path, body === undefined ? {} : { payload: body });
    finishTrackedMutationExecution(hash, "completed");
    return result;
  } catch (error) {
    if (error.code === "HOTELS_API_TIMEOUT" || error.code === "HOTELS_API_NETWORK" || (error.httpStatus ?? 0) >= 500) {
      finishTrackedMutationExecution(hash, "outcome_unknown");
    } else {
      mutationExecutionsByHash.delete(hash);
    }
    throw error;
  }
}

function response(id, result) { return { jsonrpc: "2.0", id, result }; }
function error(id, code, message) { return { jsonrpc: "2.0", id, error: { code, message } }; }
function write(message) { process.stdout.write(`${JSON.stringify(message)}\n`); }

function toolAnnotations(tool) {
  if (tool.name === "tbank_hotels_save_voucher" || tool.name === "tbank_hotels_create_payment_handoff_preview") {
    return {
      readOnlyHint: false,
      destructiveHint: false,
      idempotentHint: false,
      openWorldHint: true,
    };
  }
  const mutating = tool._execute === true || tool.name === "tbank_hotels_confirm_booking";
  return {
    readOnlyHint: !mutating,
    destructiveHint: mutating,
    idempotentHint: !mutating,
    openWorldHint: true,
  };
}

async function handle(request) {
  if (request.id === undefined) return;
  if (request.jsonrpc !== "2.0") return write(error(request.id ?? null, -32600, "Invalid JSON-RPC version."));
  if (request.method === "initialize") {
    return write(response(request.id, {
      protocolVersion: MCP_PROTOCOL_VERSION,
      capabilities: { tools: { listChanged: false } },
      serverInfo: { name: SERVER_NAME, version: SERVER_VERSION },
      instructions: "API-driven T-Bank Hotels MCP. Configure the base URL and authentication only through environment variables. This server does not use a browser, cookies, local browser state, or stored user sessions. Calls that can create a booking, set up a payment, cancel a booking, apply a promocode, or update extra services are disabled by default and require both an explicit runtime activation and a time-limited prepare/execute confirmation protocol.",
    }));
  }
  if (request.method === "ping") return write(response(request.id, {}));
  if (request.method === "tools/list") return write(response(request.id, { tools: tools.map(({ _action, _execute, _hasPayload, ...tool }) => ({ ...tool, annotations: toolAnnotations({ ...tool, _action, _execute, _hasPayload }) })) }));
  if (request.method === "tools/call") {
    try { return write(response(request.id, { content: [text(await callTool(request.params?.name, request.params?.arguments))], isError: false })); }
    catch (toolError) { return write(response(request.id, { content: [text(toolError.message)], isError: true })); }
  }
  return write(error(request.id, -32601, "Method not found."));
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const input = createInterface({ input: process.stdin, crlfDelay: Infinity });
  input.on("line", (line) => {
    try {
      const request = JSON.parse(line);
      if (!request || typeof request !== "object" || Array.isArray(request)) return write(error(null, -32600, "JSON-RPC batch requests are not supported."));
      void handle(request);
    } catch {
      write(error(null, -32700, "Parse error."));
    }
  });
}
