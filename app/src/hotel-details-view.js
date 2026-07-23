export function toHotelDetailsViewModel(details) {
  const hotelName = text(details?.hotelName);
  const hotelChain = text(details?.hotelChain);
  const starRating = Number.isInteger(details?.starRating) &&
      details.starRating >= 0 && details.starRating <= 5
    ? `${details.starRating} ${starWord(details.starRating)}`
    : null;
  const address = text(details?.location?.address);
  const images = uniqueHttpsUrls(details?.imageUrls).slice(0, 10);
  const descriptionSections = normalizeDescriptionSections(details?.descriptionSections);
  const amenityGroups = normalizeAmenityGroups(details?.amenityGroups);
  const checkInTime = normalizeTime(details?.checkInTime);
  const checkOutTime = normalizeTime(details?.checkOutTime);
  const paymentMethods = normalizePaymentMethods(details?.paymentMethods);
  const hasFacts = Boolean(
    hotelChain ||
    starRating ||
    address ||
    images.length ||
    descriptionSections.length ||
    amenityGroups.length ||
    checkInTime ||
    checkOutTime ||
    paymentMethods.length,
  );

  return {
    hotelName,
    hotelChain,
    starRating,
    address,
    images,
    descriptionSections,
    amenityGroups,
    checkInTime,
    checkOutTime,
    paymentMethods,
    hasFacts,
  };
}

export function renderHotelDetailsMarkup(details) {
  const view = toHotelDetailsViewModel(details);
  const identity = [view.hotelChain, view.starRating].filter(Boolean);
  const identityMarkup = identity.length
    ? `<p class="hotel-details__identity">${identity.map(escapeHtml).join(" · ")}</p>`
    : "";
  const imagesMarkup = view.images.length
    ? `
      <div class="hotel-details__images" aria-label="Фотографии отеля">
        ${view.images.map((url, index) => `
          <img
            src="${escapeHtml(url)}"
            alt="${escapeHtml(`${view.hotelName || "Отель"}: фото ${index + 1}`)}"
            loading="lazy"
            decoding="async"
            referrerpolicy="no-referrer"
          >
        `).join("")}
      </div>
    `
    : "";
  const addressMarkup = view.address
    ? `<p class="hotel-details__address"><strong>Адрес:</strong> ${escapeHtml(view.address)}</p>`
    : "";
  const descriptionMarkup = view.descriptionSections.map((section) => `
    <section class="hotel-details__section">
      ${section.title ? `<h5>${escapeHtml(section.title)}</h5>` : ""}
      ${section.paragraphs.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("")}
    </section>
  `).join("");
  const amenitiesMarkup = view.amenityGroups.map((group) => `
    <section class="hotel-details__section">
      <h5>${escapeHtml(group.name || "Удобства")}</h5>
      <ul class="hotel-details__amenities">
        ${group.amenities.map((amenity) => `<li>${escapeHtml(amenity)}</li>`).join("")}
      </ul>
    </section>
  `).join("");
  const stayFacts = [
    view.checkInTime ? ["Заезд", view.checkInTime] : null,
    view.checkOutTime ? ["Выезд", view.checkOutTime] : null,
    view.paymentMethods.length ? ["Оплата", view.paymentMethods.join(", ")] : null,
  ].filter(Boolean);
  const stayFactsMarkup = stayFacts.length
    ? `
      <dl class="hotel-details__facts">
        ${stayFacts.map(([label, value]) => `
          <div><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value)}</dd></div>
        `).join("")}
      </dl>
    `
    : "";
  const emptyMarkup = view.hasFacts
    ? ""
    : `<p class="hotel-details__empty">Дополнительные сведения об отеле не предоставлены.</p>`;

  return `
    <div class="hotel-details__heading">
      <p class="hotel-details__kicker">Выбранный вариант</p>
      <h4 class="hotel-details__title">Подробнее об отеле</h4>
      ${identityMarkup}
    </div>
    ${imagesMarkup}
    ${addressMarkup}
    ${descriptionMarkup}
    ${amenitiesMarkup}
    ${stayFactsMarkup}
    ${emptyMarkup}
  `;
}

function normalizeDescriptionSections(value) {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.flatMap((section) => {
    const paragraphs = Array.isArray(section?.paragraphs)
      ? section.paragraphs.map(text).filter(Boolean)
      : [];
    if (paragraphs.length === 0) {
      return [];
    }
    return [{ title: text(section?.title), paragraphs }];
  });
}

function normalizeAmenityGroups(value) {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.flatMap((group) => {
    const amenities = Array.isArray(group?.amenities)
      ? [...new Set(group.amenities.map(text).filter(Boolean))]
      : [];
    if (amenities.length === 0) {
      return [];
    }
    return [{ name: text(group?.name), amenities }];
  });
}

function normalizePaymentMethods(value) {
  if (!Array.isArray(value)) {
    return [];
  }

  const labels = {
    cash: "наличные",
    card: "карта",
  };
  return [...new Set(value.map((method) => labels[method]).filter(Boolean))];
}

function uniqueHttpsUrls(value) {
  if (!Array.isArray(value)) {
    return [];
  }

  return [...new Set(value.filter(isHttpsUrl))];
}

function isHttpsUrl(value) {
  if (typeof value !== "string") {
    return false;
  }
  try {
    return new URL(value).protocol === "https:";
  } catch {
    return false;
  }
}

function normalizeTime(value) {
  return typeof value === "string" && /^(?:[01][0-9]|2[0-3]):[0-5][0-9]$/.test(value)
    ? value
    : null;
}

function text(value) {
  return typeof value === "string" ? value.trim() : "";
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

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
