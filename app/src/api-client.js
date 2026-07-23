export function createApiClient({
  fetchImpl = globalThis.fetch,
  baseUrl = "/api/v1",
} = {}) {
  return {
    createAssistantSession(initialMessage) {
      const options = {
        method: "POST",
      };

      if (typeof initialMessage === "string") {
        options.headers = {
          "content-type": "application/json",
        };
        options.body = JSON.stringify({
          message: initialMessage,
        });
      }

      return requestJson(fetchImpl, `${baseUrl}/assistant/sessions`, options);
    },

    sendAssistantMessage(sessionId, message) {
      return requestJson(
        fetchImpl,
        `${baseUrl}/assistant/sessions/${encodeURIComponent(sessionId)}/messages`,
        {
          method: "POST",
          headers: {
            "content-type": "application/json",
          },
          body: JSON.stringify({ message }),
        },
      );
    },

    createHotelSearch(sessionId, criteria) {
      return requestJson(fetchImpl, `${baseUrl}/hotel-searches`, {
        method: "POST",
        headers: {
          "content-type": "application/json",
        },
        body: JSON.stringify({
          sessionId,
          criteria,
        }),
      });
    },

    getHotelOffers(searchId) {
      return requestJson(fetchImpl, `${baseUrl}/hotel-searches/${encodeURIComponent(searchId)}/offers`);
    },

    getHotelOfferDetails(searchId, offerId) {
      return requestJson(
        fetchImpl,
        `${baseUrl}/hotel-searches/${encodeURIComponent(searchId)}/offers/${encodeURIComponent(offerId)}/details`,
      );
    },
  };
}

async function requestJson(fetchImpl, url, options = {}) {
  const response = await fetchImpl(url, options);
  const body = await readJson(response);

  if (!response.ok) {
    throw new Error(body?.message ?? `Backend request failed with status ${response.status}.`);
  }

  return body;
}

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
