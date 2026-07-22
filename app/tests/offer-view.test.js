import test from "node:test";
import assert from "node:assert/strict";
import {
  formatAppliedPreferences,
  renderOfferCardMarkup,
  toErrorMessage,
  toOfferViewModel,
} from "../src/offer-view.js";

test("keeps matchSummary visible in the hotel offer view model", () => {
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
    availability: "available",
    matchSummary: "Доступно; выше размещены варианты с лучшим рейтингом, затем — с меньшей общей ценой за проживание.",
  });

  assert.equal(view.name, "Rome Central Hotel");
  assert.equal(view.location, "Rome, Italy");
  assert.match(view.price, /420/);
  assert.equal(view.availability, "Доступно");
  assert.equal(
    view.matchSummary,
    "Доступно; выше размещены варианты с лучшим рейтингом, затем — с меньшей общей ценой за проживание.",
  );
});

test("renders a hotel offer with its matchSummary", () => {
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
    availability: "available",
    matchSummary: "Доступно; выше размещены варианты с лучшим рейтингом, затем — с меньшей общей ценой за проживание.",
  });

  assert.match(markup, /Rome Central Hotel/);
  assert.match(markup, /Доступно/);
  assert.match(markup, /4 звезды/);
  assert.match(markup, /Бесплатная отмена до/);
  assert.match(markup, /Доступно; выше размещены варианты с лучшим рейтингом/);
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
  });

  assert.match(summary, /до.*80[\s\u00a0]?000,5.*₽.*за поездку/u);
  assert.match(summary, /4, 5 звёзд/);
  assert.match(summary, /рейтинг от 8/);
  assert.match(summary, /бесплатная отмена/);
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
  assert.match(markup, /Нет рейтинга/);
  assert.doesNotMatch(markup, /0\.0 \/ 10/);
  assert.doesNotMatch(markup, /Бесплатная отмена/);
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
