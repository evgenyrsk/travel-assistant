import { createApiClient } from "./api-client.js";
import { createChatFlow } from "./chat-flow.js";
import {
  formatAppliedPreferences,
  renderOfferCardMarkup,
  toErrorMessage,
} from "./offer-view.js";

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

  for (const offer of offers) {
    const article = document.createElement("article");
    article.className = "offer-card";
    article.innerHTML = renderOfferCardMarkup(offer);
    elements.results.append(article);
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
