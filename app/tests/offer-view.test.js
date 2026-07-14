import test from "node:test";
import assert from "node:assert/strict";
import {
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
    matchSummary: "Available; ranked by rating, total stay price, then offer ID.",
  });

  assert.equal(view.name, "Rome Central Hotel");
  assert.equal(view.location, "Rome, Italy");
  assert.match(view.price, /420/);
  assert.equal(view.availability, "Доступно");
  assert.equal(view.matchSummary, "Available; ranked by rating, total stay price, then offer ID.");
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
    availability: "available",
    matchSummary: "Available; ranked by rating, total stay price, then offer ID.",
  });

  assert.match(markup, /Rome Central Hotel/);
  assert.match(markup, /Доступно/);
  assert.match(markup, /Available; ranked by rating, total stay price, then offer ID\./);
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
    matchSummary: "Available; rating unavailable, ranked by total stay price, then offer ID.",
  };

  const view = toOfferViewModel(offer);
  const markup = renderOfferCardMarkup(offer);

  assert.equal(view.rating, "Нет рейтинга");
  assert.match(markup, /Нет рейтинга/);
  assert.doesNotMatch(markup, /0\.0 \/ 10/);
});

test("provides a visible error-state message", () => {
  assert.equal(
    toErrorMessage(new Error("Assistant session was not found.")),
    "Assistant session was not found.",
  );
  assert.equal(toErrorMessage(null), "Не удалось выполнить запрос.");
});
