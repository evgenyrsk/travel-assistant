export const MAX_PRESENTED_OFFERS = 5;

export function createChatFlow({
  api,
  onUserMessage = () => {},
  onAssistantMessage = () => {},
  onStatus = () => {},
  onOffers = () => {},
}) {
  let sessionId;

  return {
    async submit(rawMessage) {
      const message = String(rawMessage ?? "").trim();
      if (!message) {
        throw new Error("Введите сообщение.");
      }

      onUserMessage(message);
      onStatus("Ассистент обрабатывает сообщение...", "loading");

      const response = sessionId
        ? await api.sendAssistantMessage(sessionId, message)
        : await api.createAssistantSession(message);

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
    const result = await api.getHotelOffers(hotelSearchId);
    const allOffers = Array.isArray(result?.offers) ? result.offers : [];
    const offers = allOffers.slice(0, MAX_PRESENTED_OFFERS);

    onOffers({
      offers,
      totalCount: allOffers.length,
      hotelSearchId,
      appliedPreferences: result?.appliedPreferences,
    });
    onStatus(
      allOffers.length === 0
        ? "Поиск завершён без предложений."
        : `Показано предложений: ${offers.length}.`,
      allOffers.length === 0 ? "idle" : "success",
    );
  }
}
