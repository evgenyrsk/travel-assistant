import { buildStructureOnlyReport } from "./fixture-shape.mjs";

const CATEGORY_LISTS = {
  active: "activeList",
  cancelled: "cancelledList",
  completed: "completedList",
};

function bookingListRoot(bookings) {
  const root = bookings?.payload && typeof bookings.payload === "object" && !Array.isArray(bookings.payload)
    ? bookings.payload
    : bookings;
  if (!root || typeof root !== "object" || Array.isArray(root)) {
    throw new Error("Hotels booking list response has an unsupported shape.");
  }
  return root;
}

export async function captureOwnBookingStructure({ category = "active", brokerCall }) {
  const listName = CATEGORY_LISTS[category];
  if (!listName) throw new Error("--category must be active, completed, or cancelled.");
  if (typeof brokerCall !== "function") throw new Error("A local auth broker connection is required.");

  const listed = await brokerCall("hotels.list_bookings", {
    isActiveRequired: category === "active",
    isCancelledRequired: category === "cancelled",
    isCompletedRequired: category === "completed",
  });
  const bookings = bookingListRoot(listed?.bookings);
  const selected = bookings[listName]?.[0];
  if (!selected || typeof selected !== "object" || Array.isArray(selected)) {
    throw new Error(`No own ${category} booking is available for structure-only inspection.`);
  }
  const bookingId = selected.orderId;
  if (typeof bookingId !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(bookingId)) {
    throw new Error("Selected booking does not contain a supported provider identifier.");
  }

  const details = await brokerCall("hotels.get_booking_v1", { bookingId });
  if (!details?.booking || typeof details.booking !== "object" || Array.isArray(details.booking)) {
    throw new Error("Hotels booking response has an unsupported shape.");
  }
  return {
    reportVersion: "1.1",
    mode: "structure_only",
    selection: { category, providerOrder: "first" },
    providerRequestsPerformed: true,
    providerRequestScope: {
      hotelReadRequests: 2,
      ownDataOnly: true,
      mobileSessionRefreshMayOccur: true,
    },
    sourceValuesIncluded: false,
    sourceIdentifiersIncluded: false,
    rawPayloadPersisted: false,
    limitations: {
      observedShapeOnly: true,
      requiredContractFieldsNotProven: true,
      paymentSemanticsNotInferred: true,
    },
    shapes: {
      bookingListItem: buildStructureOnlyReport(selected).shape,
      bookingDetails: buildStructureOnlyReport(details.booking).shape,
    },
  };
}
