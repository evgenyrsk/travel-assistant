import test from "node:test";
import assert from "node:assert/strict";
import {
  formatAppliedPreferences,
  OFFER_RANKING_EXPLANATION,
  renderOfferCardMarkup,
  toErrorMessage,
  toOfferViewModel,
} from "../src/offer-view.js";

test("keeps the first safe image in the hotel offer view model", () => {
  const view = toOfferViewModel({
    offerId: "fake-offer-rome-001",
    hotelName: "Rome Central Hotel",
    location: {
      city: "Rome",
      country: "Italy",
    },
    price: {
      amount: 420,
      currency: "EUR",
    },
    rating: {
      value: 8.6,
      scale: 10,
    },
    imageUrl: "https://images.example.test/hotel.jpg",
    availability: "available",
    matchSummary: "Доступно; выше размещены варианты с лучшим рейтингом, затем — с меньшей общей ценой за проживание.",
  });

  assert.equal(view.name, "Rome Central Hotel");
  assert.equal(view.location, "Rome, Italy");
  assert.match(view.price, /420/);
  assert.equal(view.availability, "Доступно");
  assert.equal(view.imageUrl, "https://images.example.test/hotel.jpg");
  assert.equal("matchSummary" in view, false);
});

test("renders a compact hotel offer with safe lazy image and without repeated summary", () => {
  const repeatedSummary =
    "Доступно; выше размещены варианты с лучшим рейтингом, затем — с меньшей общей ценой за проживание.";
  const markup = renderOfferCardMarkup({
    offerId: "fake-offer-rome-001",
    hotelName: "Rome Central Hotel",
    location: {
      city: "Rome",
      country: "Italy",
    },
    price: {
      amount: 420,
      currency: "EUR",
    },
    rating: {
      value: 8.6,
      scale: 10,
    },
    starRating: 4,
    freeCancellationUntil: "2026-08-09T18:00:00Z",
    breakfastIncluded: true,
    imageUrl: "https://images.example.test/hotel.jpg",
    availability: "available",
    matchSummary: repeatedSummary,
  });

  assert.match(markup, /Rome Central Hotel/);
  assert.match(markup, /Доступно/);
  assert.match(markup, /4 звезды/);
  assert.match(markup, /Бесплатная отмена до/);
  assert.match(markup, /Завтрак включён/);
  assert.match(markup, /src="https:\/\/images\.example\.test\/hotel\.jpg"/);
  assert.match(markup, /loading="lazy"/);
  assert.match(markup, /decoding="async"/);
  assert.match(markup, /referrerpolicy="no-referrer"/);
  assert.doesNotMatch(markup, new RegExp(repeatedSummary));
  assert.match(markup, /data-action="load-details"/);
  assert.match(markup, /data-offer-id="fake-offer-rome-001"/);
  assert.match(markup, />Подробнее<\/button>/);
  assert.match(markup, /data-role="hotel-details"/);
});

test("renders a neutral placeholder for missing or unsafe images", () => {
  const baseOffer = {
    offerId: "offer-without-safe-image",
    hotelName: "Отель без фото",
    location: { city: "Казань", country: "Россия" },
    price: { amount: 12000, currency: "RUB" },
    availability: "available",
  };

  const missingMarkup = renderOfferCardMarkup(baseOffer);
  const unsafeMarkup = renderOfferCardMarkup({
    ...baseOffer,
    imageUrl: "https://user:password@example.test/hotel.jpg",
  });

  assert.match(missingMarkup, /data-state="placeholder"/);
  assert.match(missingMarkup, /Фото пока нет/);
  assert.doesNotMatch(missingMarkup, /data-role="offer-image"/);
  assert.match(unsafeMarkup, /data-state="placeholder"/);
  assert.doesNotMatch(unsafeMarkup, /user:password/);
});

test("provides one shared ranking explanation outside individual cards", () => {
  const markup = renderOfferCardMarkup({
    offerId: "offer-1",
    hotelName: "Отель",
    location: { city: "Казань", country: "Россия" },
    price: { amount: 12000, currency: "RUB" },
    availability: "available",
    matchSummary: OFFER_RANKING_EXPLANATION,
  });

  assert.equal(
    OFFER_RANKING_EXPLANATION,
    "Сначала показаны доступные варианты с известным более высоким рейтингом; " +
      "при равном рейтинге — с меньшей общей ценой.",
  );
  assert.doesNotMatch(markup, /Сначала показаны доступные варианты/);
});

test("formats only active applied preferences", () => {
  const summary = formatAppliedPreferences({
    maxTotalPrice: {
      amount: "80000.5",
      currency: "RUB",
    },
    stars: [5, 4, 4],
    minimumGuestRating: 8,
    freeCancellationRequired: true,
    breakfastIncludedRequired: true,
  });

  assert.match(summary, /до.*80[\s\u00a0]?000,5.*₽.*за поездку/u);
  assert.match(summary, /4, 5 звёзд/);
  assert.match(summary, /рейтинг от 8/);
  assert.match(summary, /бесплатная отмена/);
  assert.match(summary, /завтрак включён/);
  assert.equal(formatAppliedPreferences(undefined), "");
});

test("renders an offer without rating as an unknown rating", () => {
  const offer = {
    offerId: "provider-offer-without-review",
    hotelName: "Hotel Without Review",
    location: {
      city: "Kazan",
      country: "Russia",
    },
    price: {
      amount: 12000,
      currency: "RUB",
    },
    availability: "available",
    matchSummary: "Доступно; рейтинг неизвестен, поэтому место определено по общей цене за проживание.",
  };

  const view = toOfferViewModel(offer);
  const markup = renderOfferCardMarkup(offer);

  assert.equal(view.rating, "Нет рейтинга");
  assert.equal(view.starRating, null);
  assert.equal(view.freeCancellation, null);
  assert.equal(view.breakfast, null);
  assert.match(markup, /Нет рейтинга/);
  assert.doesNotMatch(markup, /0\.0 \/ 10/);
  assert.doesNotMatch(markup, /Бесплатная отмена/);
  assert.doesNotMatch(markup, /Завтрак/);
});

test("renders explicit provider no-breakfast fact without inventing meal details", () => {
  const markup = renderOfferCardMarkup({
    offerId: "provider-offer-without-breakfast",
    hotelName: "Отель без завтрака",
    location: { city: "Казань", country: "Россия" },
    price: { amount: 12000, currency: "RUB" },
    breakfastIncluded: false,
    availability: "available",
  });

  assert.match(markup, /Завтрак не включён/);
});

test("does not render zero star category as zero stars", () => {
  const offer = {
    offerId: "provider-offer-zero-stars",
    hotelName: "Hotel Without Category",
    location: { city: "Kazan", country: "Russia" },
    price: { amount: 12000, currency: "RUB" },
    starRating: 0,
    availability: "available",
  };

  const view = toOfferViewModel(offer);
  const markup = renderOfferCardMarkup(offer);

  assert.equal(view.starRating, null);
  assert.doesNotMatch(markup, /0 звёзд/);
});

test("preserves a short provider hotel name instead of inventing a replacement", () => {
  const offer = {
    offerId: "provider-short-name",
    hotelName: "МА",
    location: {
      city: "Казань",
      country: "Россия",
    },
    price: {
      amount: 32810,
      currency: "RUB",
    },
    availability: "available",
    matchSummary: "Доступно; рейтинг неизвестен, поэтому место определено по общей цене за проживание.",
  };

  const view = toOfferViewModel(offer);
  const markup = renderOfferCardMarkup(offer);

  assert.equal(view.name, "МА");
  assert.match(markup, />МА<\/h3>/);
  assert.doesNotMatch(markup, /без названия/i);
});

test("provides a visible error-state message", () => {
  assert.equal(
    toErrorMessage(new Error("Assistant session was not found.")),
    "Не удалось выполнить запрос. Попробуйте ещё раз.",
  );
  assert.equal(
    toErrorMessage(new Error("Сессия ассистента не найдена.")),
    "Сессия ассистента не найдена.",
  );
  assert.equal(toErrorMessage(null), "Не удалось выполнить запрос. Попробуйте ещё раз.");
});
