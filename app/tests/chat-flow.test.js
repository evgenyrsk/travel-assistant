import test from "node:test";
import assert from "node:assert/strict";
import { createChatFlow, MAX_PRESENTED_OFFERS } from "../src/chat-flow.js";

test("creates a session from the first message and reuses it for the next turn", async () => {
  const calls = [];
  const userMessages = [];
  const assistantMessages = [];
  const flow = createChatFlow({
    api: {
      async createAssistantSession(message) {
        calls.push(["create", message]);
        return assistantResponse("Уточните даты.");
      },
      async sendAssistantMessage(sessionId, message) {
        calls.push(["send", sessionId, message]);
        return assistantResponse("Подтвердите параметры.");
      },
    },
    onUserMessage: userMessages.push.bind(userMessages),
    onAssistantMessage: assistantMessages.push.bind(assistantMessages),
  });

  await flow.submit("  Найди отель в Казани  ");
  await flow.submit("С 10 по 14 августа");

  assert.equal(flow.getSessionId(), "assistant-session-local-000001");
  assert.deepEqual(calls, [
    ["create", "Найди отель в Казани"],
    ["send", "assistant-session-local-000001", "С 10 по 14 августа"],
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
