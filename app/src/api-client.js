export function createApiClient({
  fetchImpl = globalThis.fetch,
  baseUrl = "/api/v1",
} = {}) {
  return {
    createAssistantSession() {
      return requestJson(fetchImpl, `${baseUrl}/assistant/sessions`, {
        method: "POST",
      });
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
