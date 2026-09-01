import { DEFAULT_PLAN_OPTIONS, MAX_PLAN_OPTIONS, MAX_ROOMS } from "./config.mjs";

const objectSchema = (properties, required = [], additionalProperties = false) => ({
  type: "object",
  properties,
  required,
  additionalProperties,
});

const externalSideEffectAnnotations = Object.freeze({
  readOnlyHint: false,
  destructiveHint: false,
  idempotentHint: false,
  openWorldHint: true,
});

const destructiveAnnotations = Object.freeze({
  readOnlyHint: false,
  destructiveHint: true,
  idempotentHint: false,
  openWorldHint: true,
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

export const SEARCH_FILTER_IDS = [
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

const journeyRoom = {
  ...objectSchema({
    adults: { type: "integer", minimum: 1, maximum: 16, description: "Количество взрослых в комнате." },
    adultsCount: { type: "integer", minimum: 1, maximum: 16, description: "Совместимый алиас adults для LLM-клиентов; внутри MCP нормализуется в adults." },
    childrenAges: { type: "array", maxItems: 16, items: { type: "integer", minimum: 0, maximum: 17 }, description: "Возраст каждого ребёнка, 0–17." },
    childrenAge: { type: "array", maxItems: 16, items: { type: "integer", minimum: 0, maximum: 17 }, description: "Совместимый алиас childrenAges; внутри MCP нормализуется в childrenAges." },
  }),
  anyOf: [{ required: ["adults"] }, { required: ["adultsCount"] }],
};

const pricePerNightPreference = objectSchema({
  min: { type: "number", minimum: 0, description: "Мягкая нижняя граница цены за ночь." },
  max: { type: "number", exclusiveMinimum: 0, description: "Мягкая верхняя граница цены за ночь." },
  currency: { type: "string", pattern: "^[A-Za-z]{3}$", description: "ISO-like код валюты диапазона, например RUB." },
}, ["min", "max", "currency"]);

const hotelPreferences = objectSchema({
  pricePerNight: pricePerNightPreference,
  ranking: { type: "string", enum: ["best_value"], description: "Privacy-safe ranking из Banking travel profile." },
  showAlternativesOutsideBand: { type: "boolean", const: true, description: "Всегда true: варианты вне мягкого диапазона сохраняются как явно помеченные альтернативы." },
}, ["pricePerNight", "ranking", "showAlternativesOutsideBand"]);

const rankingStrategy = {
  type: "string",
  enum: ["provider_order", "lowest_price", "highest_rating", "best_value"],
  description: "Если значение опущено, используется hotelPreferences.ranking либо provider_order. lowest_price/highest_rating используют provider facts; best_value — прозрачный локальный score рейтинга, числа отзывов и цены с необязательным мягким hotelPreferences диапазоном.",
};

const inheritedRankingStrategy = {
  type: "string",
  enum: ["provider_order", "lowest_price", "highest_rating", "best_value"],
  description: "Если значение не передано, используется ranking исходного plan_stay.",
};

const planStayInput = {
  type: "object",
  properties: {
    destination: { type: "string", minLength: 1, maxLength: 200, description: "Название города или локации, например Москва. MCP сам разрешит destinationId." },
    location: { type: "string", minLength: 1, maxLength: 200, description: "Совместимый алиас destination для LLM-клиентов; внутри MCP нормализуется в destination." },
    destinationId: { type: "integer", minimum: 1, description: "Используйте только после выбора кандидата из resolve_destination или clarification_required." },
    countryName: { type: "string", minLength: 1, maxLength: 120, description: "Необязательное название страны для разрешения одноимённых локаций." },
    checkinDate: isoDate,
    checkoutDate: isoDate,
    rooms: { type: "array", minItems: 1, maxItems: MAX_ROOMS, items: journeyRoom, description: "Один элемент на комнату." },
    guests: { type: "array", minItems: 1, maxItems: MAX_ROOMS, items: journeyRoom, description: "Совместимый алиас rooms для LLM-клиентов; один элемент на комнату, внутри MCP нормализуется в rooms." },
    hotelName: { type: "string", minLength: 1, maxLength: 250, description: "Необязательное название конкретного отеля внутри выбранной локации. Глобальный поиск без локации не заявлен контрактом." },
    breakfastIncluded: { type: "boolean", default: false, description: "Если true, MCP применяет подтверждённый provider-фильтр meal_types=breakfast до построения journey. Не заменяйте этот параметр низкоуровневым перебором filters." },
    hotelPreferences: { ...hotelPreferences, description: "Необязательные privacy-safe hotelDefaults из tbank_banking_build_portfolio_travel_profile. Это мягкие локальные предпочтения: они не отправляются provider и не скрывают варианты вне диапазона." },
    ranking: rankingStrategy,
    maxOptions: { type: "integer", minimum: 1, maximum: MAX_PLAN_OPTIONS, default: DEFAULT_PLAN_OPTIONS, description: "Ограничивает число вариантов в ответе plan_stay, но не размер собираемой MCP выборки." },
    limit: { type: "integer", minimum: 1, maximum: MAX_PLAN_OPTIONS, description: "Совместимый алиас maxOptions для LLM-клиентов; не является provider pagination limit." },
    language: languageSchema(),
  },
  required: ["checkinDate", "checkoutDate"],
  allOf: [
    { anyOf: [{ required: ["destination"] }, { required: ["location"] }, { required: ["destinationId"] }] },
    { anyOf: [{ required: ["rooms"] }, { required: ["guests"] }] },
  ],
  additionalProperties: false,
};

const personalizedPlanStayInput = {
  ...planStayInput,
  required: [...planStayInput.required, "hotelPreferences"],
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
const bookingPaymentData = objectSchema({
  creditCardId: {
    anyOf: [
      { type: "string", format: "uuid", pattern: "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$" },
      { type: "null" },
    ],
    description: "Provider card UUID. Raw card number, CVV/CVC, PIN and OTP are never accepted by this MCP.",
  },
});
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
  paymentMeans: { anyOf: [{ type: "string", enum: ["payment_form", "on_us", "off_us", "dolyame", "pos"] }, { type: "null" }] },
  promocode: nullableString,
  extraServices: { anyOf: [bookingExtraServices, { type: "null" }] },
  isBusinessTrip: { anyOf: [{ type: "boolean" }, { type: "null" }] },
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
  isBusinessTrip: { anyOf: [{ type: "boolean" }, { type: "null" }] },
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

export const tools = [
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
    description: "Каноническая форма обычного вызова: destination, checkinDate, checkoutDate, rooms:[{adults, childrenAges?}], необязательные breakfastIncluded, ranking, maxOptions и language. Для устойчивости LLM-клиентов MCP также принимает location→destination, guests→rooms, limit→maxOptions и adultsCount/childrenAge внутри комнаты; не смешивайте каноническое поле с его алиасом и не перебирайте формы после ошибки. Основной agent-facing поиск принимает название локации, даты, комнаты, semantic breakfastIncluded и необязательные privacy-safe hotelPreferences из Banking portfolio profile. Если пользователь просит применить профиль, передайте объект hotelPreferences из tbank_banking_build_portfolio_travel_profile без преобразований: ranking=best_value без hotelPreferences не считается применением профиля, и это нельзя утверждать. Сам разрешает destinationId, включая безопасный внутренний fallback при несовпадении локализованного countryName; после clarification_required или search_unavailable не вызывайте resolve_destination с альтернативными написаниями и не перебирайте локации автоматически. Применяет обязательные условия до поиска, собирает bounded paginated provider results и создаёт short-lived journeyId. Если searchCoverage.continuationRecommended=true, перед сравнением пяти лучших вызовите tbank_hotels_continue_stay_search ровно один раз; не перезапускайте plan_stay. При substantial coverage сравнивайте текущую выборку с честной оговоркой, а дополнительное продолжение выполняйте только по явному запросу пользователя. hotelPreferences — мягкое локальное ранжирование: диапазон цены за ночь не отправляется provider и не скрывает альтернативы; provider shownPrice трактуется как полная цена поездки, а MCP вычисляет отдельную цену за ночь. При provider network/DNS/timeout/HTTP failure возвращает terminal search_unavailable с retryAllowed=false: не повторяйте поиск, не обходите MCP и не перебирайте low-level tools. Для конкретного отеля передайте hotelName вместе с локацией.",
    inputSchema: planStayInput,
  },
  {
    name: "tbank_hotels_plan_personalized_stay",
    description: "Основной tool для естественных запросов «используй мой обезличенный профиль». Полностью повторяет semantic hotel journey, но обязательно требует hotelPreferences из tbank_banking_build_portfolio_travel_profile и поэтому не позволяет спутать generic best_value с применённым пользовательским профилем. Передавайте hotelPreferences без преобразований; применение можно утверждать только при preferencesApplied.applied=true. Если searchCoverage.continuationRecommended=true, один раз продолжите тот же journey через tbank_hotels_continue_stay_search перед сравнением.",
    inputSchema: personalizedPlanStayInput,
  },
  {
    name: "tbank_hotels_continue_stay_search",
    description: "Безопасно продолжает неполную provider-выборку существующего journey без повторной загрузки уже пройденных страниц и без смены optionId ранее найденных отелей. Вызывайте автоматически не более одного раза и только когда plan_stay вернул searchCoverage.continuationRecommended=true. После первого продолжения MCP механически возвращает continuationRecommended=false, даже если явное дополнительное продолжение ещё доступно. Если покрытие остаётся partial/substantial, сравните текущую выборку и честно укажите coverage; повторное продолжение допустимо только по явному запросу пользователя на более полный поиск. Общий лимит initial+continuation остаётся 20 provider search requests. Не используйте этот tool после terminal search_unavailable и не заменяйте им low-level search.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId из plan_stay.") }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_get_stay_options",
    description: "Возвращает нормализованные provider facts вариантов из journey-контекста без provider hotelId. Если ranking не передан, наследует ranking исходного plan_stay.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId из tbank_hotels_plan_stay."), ranking: inheritedRankingStrategy, limit: { type: "integer", minimum: 1, maximum: MAX_PLAN_OPTIONS, default: DEFAULT_PLAN_OPTIONS } }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_compare_stay_options",
    description: "Сравнивает 2–5 вариантов и возвращает готовые comparisonRows и comparisonTableMarkdown. Первый вызов ранжирует всю journey. Следующий вызов по умолчанию ранжирует только предыдущую показанную comparison-пятёрку, поэтому фразы «среди этих/ранее найденных вариантов» не вводят невидимый отель. Для намеренного пересчёта всей выборки передайте scope=all_journey_options; scope=previous_comparison требует предыдущего сравнения. optionIds задают явное подмножество, а ranking применяется внутри него. При применённом ценовом профиле preferenceAlternatives отдельно возвращает лучшие варианты ниже и выше мягкого диапазона.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId."), optionIds: { type: "array", minItems: 2, maxItems: 5, items: identifierSchema("optionId из journey."), description: "Явное подмножество вариантов; ranking, если передан, применяется только внутри него." }, scope: { type: "string", enum: ["previous_comparison", "all_journey_options"], description: "Опустите для естественного continuation: после первого сравнения используется previous_comparison. Укажите all_journey_options только если пользователь явно просит пересчитать всю найденную выборку." }, ranking: inheritedRankingStrategy, limit: { type: "integer", minimum: 2, maximum: 5, default: 5 } }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_select_stay_option",
    description: "Выбирает один вариант в journey-контексте. Не резервирует номер и не выполняет HTTP write-запрос.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId."), optionId: identifierSchema("optionId из journey.") }, ["journeyId", "optionId"]),
  },
  {
    name: "tbank_hotels_get_selected_stay_rates",
    description: "Загружает бронируемые тарифы выбранного journey-варианта. hotelId, даты и гости берутся из journey. rateNumber стабилен во всех следующих ответах journey: показывайте ratesTableMarkdown ровно один раз и не перенумеровывайте отфильтрованное подмножество. Если запрос пользователя уже задаёт критерий выбора, завершите select_stay_rate → create_booking_preview и дайте один итоговый ответ вместо предварительной и повторной таблиц; не вызывайте rates и preview параллельно. filters — неподтверждённый untyped pass-through rates-контракт: не угадывайте его и не передавайте без точных provider-данных. Один timeout повторяется внутри общего бюджета. После исчерпания бюджета tool возвращает rates_temporarily_unavailable и запрещает автоматический повтор. Если provider вернул пустой rates, tool возвращает no_bookable_rates и запрещает запрашивать guest PII или создавать draft по search-feed цене.",
    inputSchema: objectSchema({
      journeyId: identifierSchema("Непрозрачный journeyId."),
      filters: { type: "array", items: { type: "object", additionalProperties: true }, description: "Неподтверждённый untyped pass-through для rates endpoint. Не угадывайте форму; поле следует опускать без точного provider-контракта." },
      apiVersion: rateApiVersionSchema(),
      language: languageSchema(),
    }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_select_stay_rate",
    description: "Выбирает конкретный тариф ранее выбранного отеля и возвращает его исходный стабильный rateNumber. Не создаёт бронирование и не перенумеровывает тарифы.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId."), rateOptionId: identifierSchema("rateOptionId из tbank_hotels_get_selected_stay_rates.") }, ["journeyId", "rateOptionId"]),
  },
  {
    name: "tbank_hotels_create_booking_preview",
    description: "Создаёт безопасный локальный preview выбранного отеля, тарифа и состава гостей без ФИО, email и телефона. Используйте как финальный шаг только когда пользователь просит preview или сводку без продолжения оформления. Если пользователь просит оформить, забронировать, продолжить или перейти к оформлению, вызывайте tbank_hotels_create_checkout_handoff вместо остановки на preview: handoff сам включает безопасную сводку и остаётся доступен при выключенном direct execution. Требует уже завершённый tbank_hotels_get_selected_stay_rates и отдельный tbank_hotels_select_stay_rate; не вызывайте preview одновременно с rates или до выбора rateOptionId. Не создаёт booking draft, не вызывает Hotels API и не запрашивает PII. В обычном пользовательском ответе не раскрывайте внутренние имена заголовков или конфигурационные blockers.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId с выбранным rateOptionId.") }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_inspect_checkout",
    description: "Получает актуальный provider checkout для уже выбранного journey-тарифа, нормализует итоговые цены, отмену, cashback и доступные ранний заезд/поздний выезд. Опционально проверяет один промокод без применения и запрашивает одно доступное upgrade-предложение. Используйте этот tool для естественных запросов об окончательной цене, промокоде, дополнительных услугах или upgrade; не вызывайте low-level get_rate/validate_promocode/get_rate_upgrade и не передавайте bookHash/provider DTO. Не принимает PII, не создаёт бронь, не изменяет checkout и не запускает оплату.",
    inputSchema: objectSchema({
      journeyId: identifierSchema("Непрозрачный journeyId с выбранным rateOptionId."),
      promocode: { type: "string", minLength: 1, maxLength: 128, description: "Промокод для безопасной проверки без применения." },
      includeUpgradeOffer: { type: "boolean", default: false, description: "Если true, выполняет один read-like provider запрос доступного upgrade без применения." },
      language: languageSchema(),
    }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_preview_checkout_changes",
    description: "Создаёт локальный preview желаемого изменения checkout после tbank_hotels_inspect_checkout. Принимает только opaque extraServiceOptionRef из последней инспекции и действие с уже проверенным промокодом; не принимает bookHash, checkOutId или provider IDs. Не применяет промокод, не заменяет допуслуги, не вычисляет новую итоговую цену, не создаёт бронь и не запускает оплату. Для фактического оформления после preview используйте tbank_hotels_create_checkout_handoff.",
    inputSchema: objectSchema({
      journeyId: identifierSchema("Непрозрачный journeyId с актуальной checkout-инспекцией."),
      promocodeAction: { type: "string", enum: ["unchanged", "apply_validated"], default: "unchanged", description: "Удаление уже применённого промокода не поддерживается: read-контракт checkout не подтверждает источник текущего promo-состояния." },
      extraServiceOptionRefs: {
        type: "array",
        uniqueItems: true,
        maxItems: 2,
        items: { type: "string", pattern: "^checkout_extra_[a-f0-9]{24}$" },
        description: "Не более одного раннего заезда и одного позднего выезда из последней checkout-инспекции.",
      },
    }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_create_payment_form_preview",
    description: "Создаёт локальный preview безопасного hosted payment form flow для выбранного тарифа. Не принимает ФИО, email, телефон, PAN, CVV/CVC, PIN, OTP, browser fingerprint или redirect URL; не вызывает Hotels API, не создаёт бронь и не запускает оплату. Возвращает подтверждённые состояния payment task без внутренних имён headers или blockers; полная диагностика доступна только в connection_status. Пока executionAvailable=false, покажите preview и остановитесь без запроса финального подтверждения.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId с выбранным rateOptionId.") }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_create_checkout_handoff",
    description: "Финальный безопасный шаг для естественных запросов «оформить», «забронировать», «продолжить» или «перейти к оформлению» после выбора тарифа. Вызывайте этот tool даже когда direct booking execution недоступен: он включает безопасную preview-сводку и возвращает hostedCheckoutUrl на публичную страницу конкретного выбранного отеля. Для одной комнаты без детей URL сохраняет выбранный отель, даты и число взрослых через подтверждённые public-параметры dateFrom/dateTo/guests; сложный состав гостей не угадывается. Покажите ссылку пользователю. Не принимает и не передаёт ФИО, контакты, bookHash, токены, данные карты, OTP или 3-D Secure; не создаёт бронь и не запускает оплату. Точный тариф не переносится и не резервируется: пользователь выбирает актуальный тариф во внешнем интерфейсе.",
    inputSchema: objectSchema({ journeyId: identifierSchema("Непрозрачный journeyId с выбранным rateOptionId.") }, ["journeyId"]),
  },
  {
    name: "tbank_hotels_create_booking_draft",
    description: "Создаёт черновик только для явно намеренного реального бронирования и поэтому принимает guest PII. До разбора и сохранения PII проверяет готовность booking execution; при executionAvailable=false возвращает безопасный отказ без bookingDraftId и без сохранения данных. Для preview без оформления используйте tbank_hotels_create_booking_preview. Достаточность имён всех проживающих OpenAPI не подтверждает, поэтому MCP возвращает guestCoverage без выдумывания требования. HTTP create-запрос не выполняется.",
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
    _annotations: destructiveAnnotations,
  },
  {
    name: "tbank_hotels_get_booking_overview",
    description: "Возвращает карточку собственной брони. В mobile broker-режиме принимает только process-local bookingRef из tbank_hotels_list_bookings и читает booking v1 без раскрытия provider orderId. Для прямого API-профиля принимает orderId. PDF voucher никогда не встраивается в MCP-ответ; при includeVoucher=true возвращается только указание использовать отдельный безопасный local handoff.",
    inputSchema: {
      ...objectSchema({
        bookingRef: { type: "string", pattern: "^booking_[a-f0-9]{24}$", description: "Непрозрачный process-local bookingRef из tbank_hotels_list_bookings." },
        orderId: identifierSchema("Provider orderId только для прямого API-профиля без mobile auth broker."),
        includeVoucher: { type: "boolean", default: false },
        apiVersion: bookingApiVersionSchema(),
      }),
      anyOf: [{ required: ["bookingRef"] }, { required: ["orderId"] }],
    },
  },
  {
    name: "tbank_hotels_preview_cancellation",
    description: "Загружает текущие provider-данные собственной брони для просмотра условий отмены. В mobile broker-режиме принимает только bookingRef и читает booking v1; для прямого API-профиля принимает orderId. Не рассчитывает сумму возврата и не отменяет заказ.",
    inputSchema: {
      ...objectSchema({
        bookingRef: { type: "string", pattern: "^booking_[a-f0-9]{24}$", description: "Непрозрачный process-local bookingRef из tbank_hotels_list_bookings." },
        orderId: identifierSchema("Provider orderId только для прямого API-профиля без mobile auth broker."),
        apiVersion: bookingApiVersionSchema(),
      }),
      anyOf: [{ required: ["bookingRef"] }, { required: ["orderId"] }],
    },
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
    _annotations: externalSideEffectAnnotations,
  },
  {
    name: "tbank_hotels_create_payment_handoff_preview",
    description: "Создаёт через общий local auth broker одноразовый короткоживущий paymentHandoffRef для собственной брони. Broker выполняет один read booking v1 и связывает наблюдаемые paymentPrice и raw paymentStatus; status не интерпретируется как разрешение оплаты. Capability поглощается при первом Banking preview. Не возвращает provider orderId/paymentToken и не выполняет payment setup или оплату.",
    inputSchema: objectSchema({
      bookingRef: { type: "string", pattern: "^booking_[a-f0-9]{24}$", description: "Непрозрачный process-local bookingRef из tbank_hotels_list_bookings." },
    }, ["bookingRef"]),
    _annotations: externalSideEffectAnnotations,
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
    _annotations: execute ? destructiveAnnotations : undefined,
  };
}
