export function toOfferViewModel(offer) {
  return {
    id: offer.offerId,
    name: offer.hotelName,
    location: [offer.location?.city, offer.location?.country].filter(Boolean).join(", "),
    price: formatPrice(offer.price),
    rating: formatRating(offer.rating),
    starRating: formatStarRating(offer.starRating),
    freeCancellation: formatFreeCancellation(offer.freeCancellationUntil),
    availability: availabilityLabel(offer.availability),
    availabilityTone: offer.availability ?? "unknown",
    matchSummary: offer.matchSummary ?? "Причина ранжирования не указана.",
  };
}

export function renderOfferCardMarkup(offer) {
  const view = toOfferViewModel(offer);
  const optionalFacts = [view.starRating, view.freeCancellation].filter(Boolean);
  const optionalFactsMarkup = optionalFacts.length === 0
    ? ""
    : `<p class="offer-card__details">${optionalFacts.map(escapeHtml).join(" · ")}</p>`;

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
    ${optionalFactsMarkup}
    <p class="offer-card__summary">${escapeHtml(view.matchSummary)}</p>
  `;
}

export function formatAppliedPreferences(preferences) {
  if (!preferences || typeof preferences !== "object") {
    return "";
  }

  return [
    formatMaximumTotalPrice(preferences.maxTotalPrice),
    formatStarPreference(preferences.stars),
    Number.isInteger(preferences.minimumGuestRating)
      ? `рейтинг от ${preferences.minimumGuestRating}`
      : null,
    preferences.freeCancellationRequired === true
      ? "бесплатная отмена"
      : null,
  ].filter(Boolean).join(" · ");
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

function formatStarRating(starRating) {
  if (!Number.isInteger(starRating) || starRating < 0 || starRating > 5) {
    return null;
  }

  return `${starRating} ${starWord(starRating)}`;
}

function formatFreeCancellation(value) {
  if (typeof value !== "string") {
    return null;
  }

  const deadline = new Date(value);
  if (Number.isNaN(deadline.getTime())) {
    return null;
  }

  const formatted = new Intl.DateTimeFormat("ru-RU", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(deadline);
  return `Бесплатная отмена до ${formatted}`;
}

function formatMaximumTotalPrice(price) {
  if (typeof price?.amount !== "string" || !price.currency) {
    return null;
  }

  const amount = Number(price.amount);
  if (!Number.isFinite(amount) || amount <= 0) {
    return null;
  }

  return `до ${new Intl.NumberFormat("ru-RU", {
    style: "currency",
    currency: price.currency,
    maximumFractionDigits: 2,
  }).format(amount)} за поездку`;
}

function formatStarPreference(stars) {
  if (!Array.isArray(stars)) {
    return null;
  }

  const normalized = [...new Set(stars)]
    .filter((star) => Number.isInteger(star) && star >= 0 && star <= 5)
    .sort((left, right) => left - right);
  if (normalized.length === 0) {
    return null;
  }

  const label = normalized.length === 1
    ? `${normalized[0]} ${starWord(normalized[0])}`
    : `${normalized.join(", ")} звёзд`;
  return label;
}

function starWord(value) {
  if (value === 1) {
    return "звезда";
  }
  if (value >= 2 && value <= 4) {
    return "звезды";
  }
  return "звёзд";
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
