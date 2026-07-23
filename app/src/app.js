import { createApiClient } from "./api-client.js";
import { renderOfferCardMarkup, toErrorMessage } from "./offer-view.js";

const api = createApiClient();
const elements = {
  form: document.querySelector("#search-form"),
  destination: document.querySelector("#destination"),
  checkInDate: document.querySelector("#check-in-date"),
  checkOutDate: document.querySelector("#check-out-date"),
  adults: document.querySelector("#adults"),
  submit: document.querySelector("#submit-search"),
  status: document.querySelector("#status"),
  error: document.querySelector("#error-message"),
  results: document.querySelector("#results"),
  emptyState: document.querySelector("#empty-state"),
};

let sessionId;

elements.form.addEventListener("submit", async (event) => {
  event.preventDefault();
  setBusy(true, "Создаю локальную сессию и запускаю поиск...");
  clearError();
  clearResults();

  try {
    sessionId ??= (await api.createAssistantSession()).session.sessionId;
    const search = await api.createHotelSearch(sessionId, {
      destination: elements.destination.value.trim(),
      checkInDate: elements.checkInDate.value,
      checkOutDate: elements.checkOutDate.value,
      guests: {
        adults: Number.parseInt(elements.adults.value, 10),
        children: 0,
      },
      rooms: 1,
    });
    const offers = await api.getHotelOffers(search.searchId);

    renderOffers(offers.offers ?? []);
    setStatus(`Найдено предложений: ${offers.offers?.length ?? 0}.`, "success");
  } catch (error) {
    showError(error);
  } finally {
    setBusy(false);
  }
});

function renderOffers(offers) {
  elements.results.replaceChildren();

  if (offers.length === 0) {
    elements.emptyState.hidden = false;
    elements.emptyState.textContent = "Поиск завершён без предложений.";
    return;
  }

  elements.emptyState.hidden = true;

  for (const offer of offers) {
    const article = document.createElement("article");
    article.className = "offer-card";
    article.innerHTML = renderOfferCardMarkup(offer);
    elements.results.append(article);
  }
}

function setBusy(isBusy, message) {
  for (const control of elements.form.elements) {
    control.disabled = isBusy;
  }

  if (message) {
    setStatus(message, "loading");
  }
}

function setStatus(message, tone) {
  elements.status.textContent = message;
  elements.status.dataset.tone = tone;
}

function showError(error) {
  elements.error.textContent = toErrorMessage(error);
  elements.error.hidden = false;
  setStatus("Запрос завершился ошибкой.", "error");
}

function clearError() {
  elements.error.hidden = true;
  elements.error.textContent = "";
}

function clearResults() {
  elements.results.replaceChildren();
  elements.emptyState.hidden = false;
  elements.emptyState.textContent = "Поиск выполняется...";
}
