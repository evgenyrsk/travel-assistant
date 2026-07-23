export const OFFER_RANKING_EXPLANATION =
  "Сначала показаны доступные варианты с известным более высоким рейтингом; " +
  "при равном рейтинге — с меньшей общей ценой.";

export function toOfferViewModel(offer) {
  return {
    id: offer.offerId,
    name: offer.hotelName,
    location: [offer.location?.city, offer.location?.country].filter(Boolean).join(", "),
    imageUrl: safeImageUrl(offer.imageUrl),
    price: formatPrice(offer.price),
    rating: formatRating(offer.rating),
    starRating: formatStarRating(offer.starRating),
    freeCancellation: formatFreeCancellation(offer.freeCancellationUntil),
    availability: availabilityLabel(offer.availability),
    availabilityTone: offer.availability ?? "unknown",
  };
}

export function renderOfferCardMarkup(offer) {
  const view = toOfferViewModel(offer);
  const optionalFacts = [view.starRating, view.freeCancellation].filter(Boolean);
  const optionalFactsMarkup = optionalFacts.length === 0
    ? ""
    : `<div class="offer-card__tags">${optionalFacts
      .map((fact) => `<span class="offer-card__tag">${escapeHtml(fact)}</span>`)
      .join("")}</div>`;
  const hasImage = Boolean(view.imageUrl);
  const imageMarkup = hasImage
    ? `<img
        class="offer-card__image"
        data-role="offer-image"
        src="${escapeHtml(view.imageUrl)}"
        alt="Фото отеля ${escapeHtml(view.name)}"
        loading="lazy"
        decoding="async"
        referrerpolicy="no-referrer"
      >`
    : "";

  return `
    <div
      class="offer-card__media"
      data-role="offer-media"
      data-state="${hasImage ? "image" : "placeholder"}"
    >
      ${imageMarkup}
      <div
        class="offer-card__placeholder"
        data-role="offer-image-placeholder"
        aria-hidden="true"
        ${hasImage ? "hidden" : ""}
      >
        <span class="offer-card__placeholder-icon" aria-hidden="true">✦</span>
        <span>Фото пока нет</span>
      </div>
    </div>
    <div class="offer-card__content">
      <div class="offer-card__header">
        <div>
          <p class="offer-card__location">${escapeHtml(view.location)}</p>
          <h3>${escapeHtml(view.name)}</h3>
        </div>
      </div>
      <div class="offer-card__facts">
        <strong>${escapeHtml(view.price)}</strong>
        <span class="offer-card__rating">${escapeHtml(view.rating)}</span>
      </div>
      <div class="offer-card__tags">
        <span class="availability availability--${escapeHtml(view.availabilityTone)}">
          ${escapeHtml(view.availability)}
        </span>
      </div>
      ${optionalFactsMarkup}
      <div class="offer-card__actions">
        <button
          class="offer-card__details-button"
          type="button"
          data-action="load-details"
          data-offer-id="${escapeHtml(view.id)}"
          aria-expanded="false"
          aria-label="Показать подробности: ${escapeHtml(view.name)}"
        >Подробнее</button>
      </div>
    </div>
    <section
      class="hotel-details"
      data-role="hotel-details"
      aria-live="polite"
      tabindex="-1"
      hidden
    ></section>
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
  if (!Number.isInteger(starRating) || starRating < 1 || starRating > 5) {
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

function safeImageUrl(value) {
  if (typeof value !== "string" || !value.trim()) {
    return null;
  }

  try {
    const url = new URL(value.trim());
    if (
      url.protocol !== "https:" ||
      !url.hostname ||
      url.username ||
      url.password ||
      url.hash
    ) {
      return null;
    }
    return value.trim();
  } catch {
    return null;
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
