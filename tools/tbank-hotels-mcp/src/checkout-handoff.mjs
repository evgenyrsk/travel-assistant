const DEFAULT_HOTEL_PAGE_URL_TEMPLATE = "https://www.tbank.ru/travel/hotels/new/hotels/{hotelId}/";

function dateOnly(value) {
  return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : null;
}

function simpleAdultsOnlyOccupancy(occupancy) {
  if (!Array.isArray(occupancy) || occupancy.length !== 1) return null;
  const room = occupancy[0];
  if (!Number.isInteger(room?.adults) || room.adults < 1) return null;
  if (!Array.isArray(room.childrenAges) || room.childrenAges.length !== 0) return null;
  return room.adults;
}

function validatedHttpsTarget(rawValue, variableName) {
  let target;
  try {
    target = new URL(rawValue);
  } catch {
    throw new Error(`${variableName} must resolve to an absolute HTTPS URL without credentials, query parameters, or a fragment.`);
  }
  if (
    target.protocol !== "https:"
    || target.username
    || target.password
    || target.search
    || target.hash
  ) {
    throw new Error(`${variableName} must resolve to an absolute HTTPS URL without credentials, query parameters, or a fragment.`);
  }
  return target.toString();
}

function withVerifiedSearchContext(baseUrl, searchContext) {
  const target = new URL(baseUrl);
  const checkinDate = dateOnly(searchContext?.checkinDate);
  const checkoutDate = dateOnly(searchContext?.checkoutDate);
  const adults = simpleAdultsOnlyOccupancy(searchContext?.occupancy);
  const datesPreserved = checkinDate !== null && checkoutDate !== null;

  if (datesPreserved) {
    target.searchParams.set("dateFrom", checkinDate);
    target.searchParams.set("dateTo", checkoutDate);
  }
  if (adults !== null) target.searchParams.set("guests", String(adults));

  const fullSimpleSearchPreserved = datesPreserved && adults !== null;
  return {
    url: target.toString(),
    datesPreserved,
    guestCountPreserved: adults !== null,
    roomCompositionPreserved: adults !== null,
    childrenAgesPreserved: adults !== null,
    searchCriteriaPreserved: fullSimpleSearchPreserved,
    searchCriteriaPreservationScope: fullSimpleSearchPreserved
      ? "dates_and_single_room_adults"
      : datesPreserved
        ? "dates_only"
        : "none",
  };
}

export function hostedCheckoutTarget(hotelId, searchContext = {}, environment = process.env) {
  const normalizedHotelId = typeof hotelId === "string" || typeof hotelId === "number" ? String(hotelId).trim() : "";
  if (!normalizedHotelId || normalizedHotelId.length > 128) throw new Error("Selected hotel does not contain a safe public hotel identifier.");

  const configuredTemplate = environment.TBANK_HOTELS_HOTEL_PAGE_URL_TEMPLATE?.trim();
  if (configuredTemplate) {
    if ((configuredTemplate.match(/\{hotelId\}/g) ?? []).length !== 1) {
      throw new Error("TBANK_HOTELS_HOTEL_PAGE_URL_TEMPLATE must contain exactly one {hotelId} placeholder.");
    }
    return {
      ...withVerifiedSearchContext(
        validatedHttpsTarget(configuredTemplate.replace("{hotelId}", encodeURIComponent(normalizedHotelId)), "TBANK_HOTELS_HOTEL_PAGE_URL_TEMPLATE"),
        searchContext,
      ),
      source: "operator_configured_hotel_page",
      selectionPreserved: true,
    };
  }
  return {
    ...withVerifiedSearchContext(
      validatedHttpsTarget(DEFAULT_HOTEL_PAGE_URL_TEMPLATE.replace("{hotelId}", encodeURIComponent(normalizedHotelId)), "official hotel page template"),
      searchContext,
    ),
    source: "official_selected_hotel_page",
    selectionPreserved: true,
  };
}
