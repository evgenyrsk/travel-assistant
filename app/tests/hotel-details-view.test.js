import test from "node:test";
import assert from "node:assert/strict";
import {
  renderHotelDetailsMarkup,
  toHotelDetailsViewModel,
} from "../src/hotel-details-view.js";

test("renders available provider-neutral details without unknown placeholders", () => {
  const details = {
    hotelName: "Отель Пример",
    hotelChain: "Сеть Пример",
    starRating: 4,
    location: {
      address: "Тестовая улица, 1",
      coordinates: { latitude: 55.1, longitude: 49.1 },
    },
    descriptionSections: [
      { title: "Об отеле", paragraphs: ["Тихий отель в центре."] },
    ],
    imageUrls: [
      "https://images.example.test/one.jpg",
      "http://images.example.test/insecure.jpg",
      "https://images.example.test/one.jpg",
    ],
    amenityGroups: [
      { name: "Основные", amenities: ["Wi-Fi", "Завтрак", "Wi-Fi"] },
    ],
    checkInTime: "15:00",
    checkOutTime: "12:00",
    paymentMethods: ["cash", "card", "unknown"],
  };

  const view = toHotelDetailsViewModel(details);
  const markup = renderHotelDetailsMarkup(details);

  assert.deepEqual(view.images, ["https://images.example.test/one.jpg"]);
  assert.deepEqual(view.paymentMethods, ["наличные", "карта"]);
  assert.deepEqual(view.amenityGroups[0].amenities, ["Wi-Fi", "Завтрак"]);
  assert.equal(view.hasFacts, true);
  assert.match(markup, /Сеть Пример · 4 звезды/);
  assert.match(markup, /Тестовая улица, 1/);
  assert.match(markup, /Тихий отель в центре/);
  assert.match(markup, /Wi-Fi/);
  assert.match(markup, /Заезд/);
  assert.match(markup, /наличные, карта/);
  assert.match(markup, /referrerpolicy="no-referrer"/);
  assert.doesNotMatch(markup, /insecure\.jpg|unknown/);
});

test("shows a bounded empty state when optional details are absent", () => {
  const view = toHotelDetailsViewModel({ hotelName: "Минимальный отель" });
  const markup = renderHotelDetailsMarkup({ hotelName: "Минимальный отель" });

  assert.equal(view.hasFacts, false);
  assert.match(markup, /Дополнительные сведения об отеле не предоставлены/);
  assert.doesNotMatch(markup, /Адрес:|Заезд|Выезд|Оплата/);
});

test("escapes provider text and image attributes", () => {
  const markup = renderHotelDetailsMarkup({
    hotelName: "<script>alert(1)</script>",
    hotelChain: "Сеть & партнёры",
    location: { address: "<img src=x onerror=alert(1)>" },
    imageUrls: ["https://images.example.test/photo.jpg?x=1&y=2"],
  });

  assert.doesNotMatch(markup, /<script>|<img src=x/);
  assert.match(markup, /Сеть &amp; партнёры/);
  assert.match(markup, /&lt;img src=x onerror=alert\(1\)&gt;/);
  assert.match(markup, /photo\.jpg\?x=1&amp;y=2/);
});
