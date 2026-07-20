export function toOfferViewModel(offer) {
  return {
    id: offer.offerId,
    name: offer.hotelName,
    location: [offer.location?.city, offer.location?.country].filter(Boolean).join(", "),
    price: formatPrice(offer.price),
    rating: formatRating(offer.rating),
    availability: availabilityLabel(offer.availability),
    availabilityTone: offer.availability ?? "unknown",
    matchSummary: offer.matchSummary ?? "Причина ранжирования не указана.",
  };
}

export function renderOfferCardMarkup(offer) {
  const view = toOfferViewModel(offer);

  return `
    <div class="offer-card__header">
      <div>
        <p class="offer-card__location">${escapeHtml(view.location)}</p>
        <h3>${escapeHtml(view.name)}</h3>
      </div>
      <span class="availability availability--${escapeHtml(view.availabilityTone)}">
        ${escapeHtml(view.availability)}
      </span>
    </div>
    <div class="offer-card__facts">
      <strong>${escapeHtml(view.price)}</strong>
      <span>${escapeHtml(view.rating)}</span>
    </div>
    <p class="offer-card__summary">${escapeHtml(view.matchSummary)}</p>
  `;
}

export function toErrorMessage(error) {
  const message = error instanceof Error ? error.message.trim() : "";
  return /[А-Яа-яЁё]/u.test(message)
    ? message
    : "Не удалось выполнить запрос. Попробуйте ещё раз.";
}

function formatPrice(price) {
  if (typeof price?.amount !== "number" || !price.currency) {
    return "Цена неизвестна";
  }

  return new Intl.NumberFormat("ru-RU", {
    style: "currency",
    currency: price.currency,
    maximumFractionDigits: 0,
  }).format(price.amount);
}

function formatRating(rating) {
  if (typeof rating?.value !== "number") {
    return "Нет рейтинга";
  }

  return `${rating.value.toFixed(1)} / ${rating.scale ?? 10}`;
}

function availabilityLabel(availability) {
  switch (availability) {
    case "available":
      return "Доступно";
    case "limited":
      return "Мало мест";
    default:
      return "Доступность неизвестна";
  }
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
