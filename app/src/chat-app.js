import { createApiClient } from "./api-client.js";
import { createChatFlow } from "./chat-flow.js";
import {
  formatAppliedPreferences,
  renderOfferCardMarkup,
  toErrorMessage,
} from "./offer-view.js";
import { renderHotelDetailsMarkup } from "./hotel-details-view.js";

const api = createApiClient();
const elements = {
  form: document.querySelector("#chat-form"),
  input: document.querySelector("#chat-input"),
  submit: document.querySelector("#send-message"),
  submitLabel: document.querySelector("#send-message-label"),
  transcript: document.querySelector("#transcript"),
  status: document.querySelector("#chat-status"),
  error: document.querySelector("#chat-error"),
  results: document.querySelector("#results"),
  resultsCount: document.querySelector("#results-count"),
  resultsSummary: document.querySelector("#results-summary"),
  emptyState: document.querySelector("#empty-state"),
};

const chatFlow = createChatFlow({
  api,
  onUserMessage: (message) => appendMessage("user", "Вы", message),
  onAssistantMessage: (message) => appendMessage("assistant", "Ассистент", message),
  onStatus: setStatus,
  onOffers: renderOffers,
});

elements.form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const message = elements.input.value.trim();
  if (!message) {
    return;
  }

  clearError();
  setBusy(true);
  elements.input.value = "";

  try {
    await chatFlow.submit(message);
  } catch (error) {
    showError(error);
  } finally {
    setBusy(false);
    elements.input.focus();
  }
});

elements.results.addEventListener("click", (event) => {
  const button = event.target.closest?.("button[data-action='load-details']");
  if (!button || !elements.results.contains(button)) {
    return;
  }

  void toggleHotelDetails(button);
});

function appendMessage(role, author, content) {
  const article = document.createElement("article");
  article.className = `chat-message chat-message--${role}`;

  const authorElement = document.createElement("p");
  authorElement.className = "chat-message__author";
  authorElement.textContent = author;

  const contentElement = document.createElement("p");
  contentElement.className = "chat-message__content";
  contentElement.textContent = content;

  article.append(authorElement, contentElement);
  elements.transcript.append(article);
  article.scrollIntoView({ block: "nearest" });
}

function renderOffers({ offers, totalCount, appliedPreferences }) {
  elements.results.replaceChildren();
  elements.resultsCount.textContent = String(offers.length);
  const preferenceSummary = formatAppliedPreferences(appliedPreferences);

  if (offers.length === 0) {
    elements.emptyState.hidden = false;
    elements.emptyState.textContent = "По текущим параметрам предложения не найдены.";
    elements.resultsSummary.hidden = !preferenceSummary;
    elements.resultsSummary.textContent = preferenceSummary
      ? `Применённые условия: ${preferenceSummary}.`
      : "";
    return;
  }

  elements.emptyState.hidden = true;
  elements.resultsSummary.hidden = false;
  const countSummary = totalCount > offers.length
    ? `Показаны ${offers.length} лучших предложений из ${totalCount}.`
    : `Показаны все предложения: ${offers.length}.`;
  elements.resultsSummary.textContent = preferenceSummary
    ? `${countSummary} Применённые условия: ${preferenceSummary}.`
    : countSummary;

  offers.forEach((offer, index) => {
    const article = document.createElement("article");
    article.className = "offer-card";
    article.innerHTML = renderOfferCardMarkup(offer);
    const details = article.querySelector("[data-role='hotel-details']");
    const button = article.querySelector("button[data-action='load-details']");
    const detailsId = `hotel-details-${index + 1}`;
    details.id = detailsId;
    button.setAttribute("aria-controls", detailsId);
    elements.results.append(article);
  });
}

async function toggleHotelDetails(button) {
  const card = button.closest(".offer-card");
  const details = card?.querySelector("[data-role='hotel-details']");
  if (!card || !details) {
    return;
  }

  if (button.dataset.loaded === "true") {
    const showDetails = details.hidden;
    details.hidden = !showDetails;
    button.setAttribute("aria-expanded", String(showDetails));
    button.textContent = showDetails ? "Скрыть детали" : "Подробнее";
    if (showDetails) {
      details.focus();
    }
    return;
  }

  const offerId = button.dataset.offerId;
  button.disabled = true;
  button.textContent = "Загружаю...";
  button.setAttribute("aria-expanded", "true");
  details.hidden = false;
  details.dataset.state = "loading";
  details.setAttribute("aria-busy", "true");
  details.textContent = "Загружаю сведения о выбранном отеле...";

  try {
    const response = await chatFlow.loadOfferDetails(offerId);
    if (!card.isConnected) {
      return;
    }
    details.innerHTML = renderHotelDetailsMarkup(response);
    details.dataset.state = "loaded";
    button.dataset.loaded = "true";
    button.textContent = "Скрыть детали";
    details.focus();
  } catch (error) {
    if (!card.isConnected) {
      return;
    }
    details.textContent = toErrorMessage(error);
    details.dataset.state = "error";
    button.textContent = "Попробовать снова";
    details.focus();
  } finally {
    details.removeAttribute("aria-busy");
    button.disabled = false;
  }
}

function setBusy(isBusy) {
  elements.input.disabled = isBusy;
  elements.submit.disabled = isBusy;
  elements.submitLabel.textContent = isBusy ? "Отправляю..." : "Отправить";
  elements.form.setAttribute("aria-busy", String(isBusy));
}

function setStatus(message, tone) {
  elements.status.textContent = message;
  elements.status.dataset.tone = tone;
}

function showError(error) {
  elements.error.textContent = toErrorMessage(error);
  elements.error.hidden = false;
  setStatus("Сообщение не обработано. Попробуйте ещё раз.", "error");
}

function clearError() {
  elements.error.hidden = true;
  elements.error.textContent = "";
}
