export const MAX_PRESENTED_OFFERS = 5;
export const SEMANTIC_POLL_MIN_MILLIS = 1_000;
export const SEMANTIC_POLL_MAX_MILLIS = 3_000;
export const SEMANTIC_POLL_TIMEOUT_MILLIS = 120_000;

export function createChatFlow({
  api,
  getClientContext = () => undefined,
  onUserMessage = () => {},
  onAssistantMessage = () => {},
  onStatus = () => {},
  onOffers = () => {},
  wait = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds)),
  now = () => Date.now(),
}) {
  let sessionId;
  let activeHotelSearchId;

  return {
    async submit(rawMessage) {
      const message = String(rawMessage ?? "").trim();
      if (!message) {
        throw new Error("Введите сообщение.");
      }

      onUserMessage(message);
      onStatus("Ассистент обрабатывает сообщение...", "loading");
      const clientContext = getClientContext();

      const response = sessionId
        ? await api.sendAssistantMessage(sessionId, message, clientContext)
        : await api.createAssistantSession(message, clientContext);

      sessionId ??= response?.session?.sessionId;
      if (!sessionId) {
        throw new Error("Не удалось создать сессию ассистента.");
      }

      const assistantMessage = response?.assistantMessage?.content?.trim();
      if (!assistantMessage) {
        throw new Error("Не удалось получить ответ ассистента.");
      }

      onAssistantMessage(assistantMessage);
      await handleNextAction(response);
      return response;
    },

    getSessionId() {
      return sessionId;
    },

    async loadOfferDetails(offerId) {
      if (!activeHotelSearchId || !offerId) {
        throw new Error("Не удалось определить выбранное предложение.");
      }

      return api.getHotelOfferDetails(activeHotelSearchId, offerId);
    },
  };

  async function handleNextAction(response) {
    switch (response.nextAction) {
      case "show_hotel_results":
        await loadOffers(response.hotelSearchId);
        break;
      case "ask_clarification":
        onStatus("Продолжите диалог или подтвердите параметры обычным сообщением.", "idle");
        break;
      case "show_boundary_message":
        onStatus("Ассистент не запустил поиск. Можно уточнить запрос на поиск отелей.", "idle");
        break;
      default:
        onStatus("Ответ ассистента получен.", "success");
    }
  }

  async function loadOffers(hotelSearchId) {
    if (!hotelSearchId) {
      throw new Error("Не удалось загрузить предложения.");
    }

    onStatus("Загружаю предложения отелей...", "loading");
    const pollingStartedAt = now();
    let result = await api.getHotelOffers(hotelSearchId);
    let pollDelay = toPollDelay(result?.metadata?.analysis?.pollAfterMillis);

    while (result?.status === "searching") {
      const elapsed = now() - pollingStartedAt;
      if (elapsed >= SEMANTIC_POLL_TIMEOUT_MILLIS) {
        throw new Error("Semantic-анализ не завершился за 120 секунд. Попробуйте новый поиск.");
      }

      onStatus("Анализирую найденные варианты размещения...", "loading");
      await wait(Math.min(pollDelay, SEMANTIC_POLL_TIMEOUT_MILLIS - elapsed));
      result = await api.getHotelOffers(hotelSearchId);
      pollDelay = Math.min(
        SEMANTIC_POLL_MAX_MILLIS,
        Math.max(SEMANTIC_POLL_MIN_MILLIS, Math.round(pollDelay * 1.5)),
      );
    }

    const allOffers = Array.isArray(result?.offers) ? result.offers : [];
    const offers = allOffers.slice(0, MAX_PRESENTED_OFFERS);
    const refinementSuggestion = toRefinementSuggestion(result?.refinementSuggestion);
    activeHotelSearchId = hotelSearchId;

    onOffers({
      offers,
      totalCount: allOffers.length,
      hotelSearchId,
      appliedPreferences: result?.appliedPreferences,
      refinementSuggestion,
      searchStatus: result?.status,
      analysis: result?.metadata?.analysis,
    });
    if (result?.status === "completed_no_offers" && refinementSuggestion) {
      onAssistantMessage(refinementSuggestion.message);
    }

    if (result?.status === "failed") {
      onStatus(
        "Semantic-анализ недоступен. Обычные отели не показаны.",
        "error",
      );
      return;
    }

    onStatus(
      result?.status === "completed_no_semantic_matches"
        ? "Поиск завершён без подтверждённых semantic-совпадений."
        : allOffers.length === 0
          ? "Поиск завершён без предложений."
          : result?.metadata?.analysis?.status === "partial"
            ? `Показано предложений: ${offers.length}. Анализ выполнен частично.`
            : `Показано предложений: ${offers.length}.`,
      allOffers.length === 0 ? "idle" : "success",
    );
  }
}

function toPollDelay(value) {
  if (!Number.isFinite(value)) {
    return SEMANTIC_POLL_MIN_MILLIS;
  }

  return Math.min(
    SEMANTIC_POLL_MAX_MILLIS,
    Math.max(SEMANTIC_POLL_MIN_MILLIS, Math.round(value)),
  );
}

function toRefinementSuggestion(value) {
  const supportedPreferences = new Set([
    "minimumGuestRating",
    "stars",
    "freeCancellationRequired",
    "breakfastIncludedRequired",
    "maxTotalPrice",
  ]);
  const message = typeof value?.message === "string" ? value.message.trim() : "";

  if (
    value?.type !== "relax_preference" ||
    !supportedPreferences.has(value?.preference) ||
    !message
  ) {
    return undefined;
  }

  return {
    type: value.type,
    preference: value.preference,
    message,
  };
}
