import test from "node:test";
import assert from "node:assert/strict";
import { createChatFlow, MAX_PRESENTED_OFFERS } from "../src/chat-flow.js";

test("creates a session from the first message and reuses it for the next turn", async () => {
  const calls = [];
  const userMessages = [];
  const assistantMessages = [];
  const clientContext = {
    locale: "ru-RU",
    timezone: "Europe/Moscow",
  };
  const flow = createChatFlow({
    api: {
      async createAssistantSession(message, context) {
        calls.push(["create", message, context]);
        return assistantResponse("Уточните даты.");
      },
      async sendAssistantMessage(sessionId, message, context) {
        calls.push(["send", sessionId, message, context]);
        return assistantResponse("Подтвердите параметры.");
      },
    },
    getClientContext: () => clientContext,
    onUserMessage: userMessages.push.bind(userMessages),
    onAssistantMessage: assistantMessages.push.bind(assistantMessages),
  });

  await flow.submit("  Найди отель в Казани  ");
  await flow.submit("С 10 по 14 августа");

  assert.equal(flow.getSessionId(), "assistant-session-local-000001");
  assert.deepEqual(calls, [
    ["create", "Найди отель в Казани", clientContext],
    ["send", "assistant-session-local-000001", "С 10 по 14 августа", clientContext],
  ]);
  assert.deepEqual(userMessages, [
    "Найди отель в Казани",
    "С 10 по 14 августа",
  ]);
  assert.deepEqual(assistantMessages, [
    "Уточните даты.",
    "Подтвердите параметры.",
  ]);
});

test("loads and presents no more than five ranked offers", async () => {
  const offers = Array.from({ length: 8 }, (_, index) => ({
    offerId: `offer-${index + 1}`,
  }));
  const offerViews = [];
  const flow = createChatFlow({
    api: {
      async createAssistantSession() {
        return assistantResponse("Поиск завершён.", {
          nextAction: "show_hotel_results",
          hotelSearchId: "hotel-search-local-000001",
        });
      },
      async getHotelOffers(searchId) {
        assert.equal(searchId, "hotel-search-local-000001");
        return {
          offers,
          appliedPreferences: {
            minimumGuestRating: 8,
          },
        };
      },
    },
    onOffers: offerViews.push.bind(offerViews),
  });

  await flow.submit("Да, ищи");

  assert.equal(offerViews.length, 1);
  assert.equal(offerViews[0].offers.length, MAX_PRESENTED_OFFERS);
  assert.deepEqual(
    offerViews[0].offers.map(({ offerId }) => offerId),
    ["offer-1", "offer-2", "offer-3", "offer-4", "offer-5"],
  );
  assert.equal(offerViews[0].totalCount, 8);
  assert.deepEqual(offerViews[0].appliedPreferences, {
    minimumGuestRating: 8,
  });
  assert.equal(offerViews[0].refinementSuggestion, undefined);
});

test("polls a semantic search with bounded backoff until a terminal result", async () => {
  let currentTime = 0;
  const pollDelays = [];
  const statuses = [];
  const offerViews = [];
  const responses = [
    {
      status: "searching",
      offers: [],
      metadata: { analysis: { status: "searching", pollAfterMillis: 500 } },
    },
    {
      status: "searching",
      offers: [],
      metadata: { analysis: { status: "searching", pollAfterMillis: 1000 } },
    },
    {
      status: "completed_with_offers",
      offers: [{ offerId: "glamping-1" }],
      metadata: {
        analysis: {
          status: "completed",
          analyzedCount: 8,
          deepAnalyzedCount: 3,
          matchCount: 1,
          probableCount: 0,
        },
      },
    },
  ];
  const flow = createChatFlow({
    api: {
      async createAssistantSession() {
        return assistantResponse("Начинаю semantic-поиск.", {
          nextAction: "show_hotel_results",
          hotelSearchId: "semantic-search-1",
        });
      },
      async getHotelOffers() {
        return responses.shift();
      },
    },
    wait: async (milliseconds) => {
      pollDelays.push(milliseconds);
      currentTime += milliseconds;
    },
    now: () => currentTime,
    onStatus: (message) => statuses.push(message),
    onOffers: offerViews.push.bind(offerViews),
  });

  await flow.submit("Да, ищи глемпинг");

  assert.deepEqual(pollDelays, [1000, 1500]);
  assert.equal(statuses.filter((message) => message.includes("Анализирую")).length, 2);
  assert.equal(statuses.at(-1), "Показано предложений: 1.");
  assert.equal(statuses.at(-1).includes("Анализирую"), false);
  assert.equal(offerViews.length, 1);
  assert.equal(offerViews[0].searchStatus, "completed_with_offers");
  assert.equal(offerViews[0].analysis.matchCount, 1);
});

test("stops semantic polling after 120 seconds", async () => {
  let currentTime = 0;
  let offerRequests = 0;
  let renderedResults = 0;
  const flow = createChatFlow({
    api: {
      async createAssistantSession() {
        return assistantResponse("Начинаю semantic-поиск.", {
          nextAction: "show_hotel_results",
          hotelSearchId: "semantic-search-timeout",
        });
      },
      async getHotelOffers() {
        offerRequests += 1;
        return {
          status: "searching",
          offers: [],
          metadata: { analysis: { status: "searching", pollAfterMillis: 3000 } },
        };
      },
    },
    wait: async (milliseconds) => {
      currentTime += milliseconds;
    },
    now: () => currentTime,
    onOffers: () => {
      renderedResults += 1;
    },
  });

  await assert.rejects(
    () => flow.submit("Да, ищи глемпинг"),
    /не завершился за 120 секунд/,
  );
  assert.ok(offerRequests > 1);
  assert.equal(renderedResults, 0);
});

test("renders semantic failure without offering ordinary hotel fallback", async () => {
  const offerViews = [];
  const statuses = [];
  const flow = createChatFlow({
    api: {
      async createAssistantSession() {
        return assistantResponse("Начинаю semantic-поиск.", {
          nextAction: "show_hotel_results",
          hotelSearchId: "semantic-search-failed",
        });
      },
      async getHotelOffers() {
        return {
          status: "failed",
          offers: [],
          metadata: { analysis: { status: "failed" } },
        };
      },
    },
    onStatus: (message, tone) => statuses.push([message, tone]),
    onOffers: offerViews.push.bind(offerViews),
  });

  await flow.submit("Да, ищи глемпинг");

  assert.equal(offerViews[0].searchStatus, "failed");
  assert.deepEqual(offerViews[0].offers, []);
  assert.deepEqual(statuses.at(-1), [
    "Semantic-анализ недоступен. Обычные отели не показаны.",
    "error",
  ]);
});

test("replaces semantic loading status with terminal no-match presentation", async () => {
  const statuses = [];
  const offerViews = [];
  const responses = [
    {
      status: "searching",
      offers: [],
      metadata: { analysis: { status: "searching", pollAfterMillis: 1000 } },
    },
    {
      status: "completed_no_semantic_matches",
      offers: [],
      metadata: { analysis: { status: "completed" } },
    },
  ];
  const flow = createChatFlow({
    api: {
      async createAssistantSession() {
        return assistantResponse("Проверка типа размещения запущена.", {
          nextAction: "show_hotel_results",
          hotelSearchId: "semantic-search-no-matches",
        });
      },
      async getHotelOffers() {
        return responses.shift();
      },
    },
    wait: async () => {},
    onStatus: (message, tone) => statuses.push([message, tone]),
    onOffers: offerViews.push.bind(offerViews),
  });

  await flow.submit("Да, ищи глемпинг");

  assert.equal(offerViews[0].searchStatus, "completed_no_semantic_matches");
  assert.deepEqual(offerViews[0].offers, []);
  assert.deepEqual(statuses.at(-1), [
    "Поиск завершён без подтверждённых semantic-совпадений.",
    "idle",
  ]);
  assert.equal(statuses.at(-1)[0].includes("Анализирую"), false);
});

test("shows one safe refinement suggestion for a completed empty search", async () => {
  const calls = [];
  const assistantMessages = [];
  const offerViews = [];
  const flow = createChatFlow({
    api: {
      async createAssistantSession() {
        calls.push("create-session");
        return assistantResponse("Поиск завершён. Результат готов.", {
          nextAction: "show_hotel_results",
          hotelSearchId: "hotel-search-local-empty",
        });
      },
      async getHotelOffers(searchId) {
        calls.push(["get-offers", searchId]);
        return {
          status: "completed_no_offers",
          offers: [],
          appliedPreferences: {
            minimumGuestRating: 8,
          },
          refinementSuggestion: {
            type: "relax_preference",
            preference: "minimumGuestRating",
            message: "Можно убрать ограничение по рейтингу и подтвердить новый поиск.",
          },
        };
      },
    },
    onAssistantMessage: assistantMessages.push.bind(assistantMessages),
    onOffers: offerViews.push.bind(offerViews),
  });

  await flow.submit("Да, ищи");

  assert.deepEqual(calls, [
    "create-session",
    ["get-offers", "hotel-search-local-empty"],
  ]);
  assert.deepEqual(assistantMessages, [
    "Поиск завершён. Результат готов.",
    "Можно убрать ограничение по рейтингу и подтвердить новый поиск.",
  ]);
  assert.equal(offerViews[0].offers.length, 0);
  assert.deepEqual(offerViews[0].refinementSuggestion, {
    type: "relax_preference",
    preference: "minimumGuestRating",
    message: "Можно убрать ограничение по рейтингу и подтвердить новый поиск.",
  });
});

test("does not load offers for clarification or boundary responses", async () => {
  let offerRequests = 0;
  const responses = [
    assistantResponse("Уточните состав гостей."),
    assistantResponse("Доступен только поиск отелей.", {
      nextAction: "show_boundary_message",
    }),
  ];
  const flow = createChatFlow({
    api: {
      async createAssistantSession() {
        return responses.shift();
      },
      async sendAssistantMessage() {
        return responses.shift();
      },
      async getHotelOffers() {
        offerRequests += 1;
      },
    },
  });

  await flow.submit("Нужен отель");
  await flow.submit("Найди авиабилеты");

  assert.equal(offerRequests, 0);
});

test("rejects results action without a hotel search id", async () => {
  const flow = createChatFlow({
    api: {
      async createAssistantSession() {
        return assistantResponse("Предложения готовы.", {
          nextAction: "show_hotel_results",
        });
      },
    },
  });

  await assert.rejects(
    () => flow.submit("Да, ищи"),
    /Не удалось загрузить предложения/,
  );
});

test("loads details only after explicit selection and only for that offer", async () => {
  const calls = [];
  const flow = createChatFlow({
    api: {
      async createAssistantSession() {
        return assistantResponse("Предложения готовы.", {
          nextAction: "show_hotel_results",
          hotelSearchId: "hotel-search-local-000001",
        });
      },
      async getHotelOffers(searchId) {
        calls.push(["offers", searchId]);
        return {
          offers: [{ offerId: "offer-1" }, { offerId: "offer-2" }],
        };
      },
      async getHotelOfferDetails(searchId, offerId) {
        calls.push(["details", searchId, offerId]);
        return { hotelName: "Выбранный отель" };
      },
    },
  });

  await flow.submit("Да, ищи");
  assert.deepEqual(calls, [["offers", "hotel-search-local-000001"]]);

  const details = await flow.loadOfferDetails("offer-2");

  assert.equal(details.hotelName, "Выбранный отель");
  assert.deepEqual(calls, [
    ["offers", "hotel-search-local-000001"],
    ["details", "hotel-search-local-000001", "offer-2"],
  ]);
});

test("does not load details before a result search exists", async () => {
  const flow = createChatFlow({ api: {} });

  await assert.rejects(
    () => flow.loadOfferDetails("offer-1"),
    /Не удалось определить выбранное предложение/,
  );
});

function assistantResponse(content, overrides = {}) {
  return {
    session: {
      sessionId: "assistant-session-local-000001",
    },
    assistantMessage: {
      role: "assistant",
      content,
    },
    nextAction: overrides.nextAction ?? "ask_clarification",
    hotelSearchId: overrides.hotelSearchId,
  };
}
