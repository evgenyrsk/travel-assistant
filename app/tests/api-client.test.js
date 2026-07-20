import test from "node:test";
import assert from "node:assert/strict";
import { createApiClient } from "../src/api-client.js";

test("creates a hotel search and loads hotel offers", async () => {
  const calls = [];
  const responses = [
    jsonResponse({
      session: {
        sessionId: "assistant-session-local-000001",
      },
    }),
    jsonResponse({
      searchId: "hotel-search-local-000001",
    }),
    jsonResponse({
      offers: [
        {
          offerId: "fake-offer-rome-001",
          hotelName: "Rome Central Hotel",
          matchSummary: "Доступно; выше размещены варианты с лучшим рейтингом, затем — с меньшей общей ценой за проживание.",
        },
      ],
    }),
  ];
  const api = createApiClient({
    fetchImpl: async (url, options = {}) => {
      calls.push({ url, options });
      return responses.shift();
    },
  });

  const session = await api.createAssistantSession();
  const search = await api.createHotelSearch(
    session.session.sessionId,
    {
      destination: "Rome",
      checkInDate: "2026-07-01",
      checkOutDate: "2026-07-04",
      guests: {
        adults: 2,
        children: 0,
      },
      rooms: 1,
    },
  );
  const offers = await api.getHotelOffers(search.searchId);

  assert.equal(
    offers.offers[0].matchSummary,
    "Доступно; выше размещены варианты с лучшим рейтингом, затем — с меньшей общей ценой за проживание.",
  );
  assert.deepEqual(
    calls.map(({ url }) => url),
    [
      "/api/v1/assistant/sessions",
      "/api/v1/hotel-searches",
      "/api/v1/hotel-searches/hotel-search-local-000001/offers",
    ],
  );
  assert.equal(JSON.parse(calls[1].options.body).criteria.destination, "Rome");
});

test("surfaces backend error messages", async () => {
  const api = createApiClient({
    fetchImpl: async () => jsonResponse(
      {
        message: "Assistant session was not found.",
      },
      {
        ok: false,
        status: 404,
      },
    ),
  });

  await assert.rejects(
    () => api.createHotelSearch("missing-session", {
      destination: "Rome",
    }),
    /Assistant session was not found/,
  );
});

test("sends initial and subsequent chat messages through assistant routes", async () => {
  const calls = [];
  const api = createApiClient({
    fetchImpl: async (url, options = {}) => {
      calls.push({ url, options });
      return jsonResponse({
        session: {
          sessionId: "assistant-session-local-000001",
        },
        assistantMessage: {
          role: "assistant",
          content: "Уточните даты.",
        },
        nextAction: "ask_clarification",
      });
    },
  });

  await api.createAssistantSession("Найди отель в Казани");
  await api.sendAssistantMessage(
    "assistant-session-local-000001",
    "С 10 по 14 августа",
  );

  assert.deepEqual(
    calls.map(({ url }) => url),
    [
      "/api/v1/assistant/sessions",
      "/api/v1/assistant/sessions/assistant-session-local-000001/messages",
    ],
  );
  assert.equal(
    JSON.parse(calls[0].options.body).message,
    "Найди отель в Казани",
  );
  assert.equal(
    JSON.parse(calls[1].options.body).message,
    "С 10 по 14 августа",
  );
});

function jsonResponse(body, overrides = {}) {
  return {
    ok: overrides.ok ?? true,
    status: overrides.status ?? 200,
    async json() {
      return body;
    },
  };
}
