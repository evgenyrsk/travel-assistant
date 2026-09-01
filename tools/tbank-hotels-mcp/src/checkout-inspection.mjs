function record(value) {
  return value && typeof value === "object" && !Array.isArray(value) ? value : null;
}

function finiteNumber(value) {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  const object = record(value);
  if (!object) return null;
  for (const key of ["amount", "value", "price"]) {
    if (typeof object[key] === "number" && Number.isFinite(object[key])) return object[key];
  }
  return null;
}

function currency(value) {
  const object = record(value);
  if (!object) return null;
  for (const key of ["currency", "currencyCode", "currencyType"]) {
    if (typeof object[key] === "string" && object[key].trim()) return object[key].trim().toUpperCase();
  }
  return null;
}

function money(value, formatMoney) {
  const amount = finiteNumber(value);
  const resolvedCurrency = currency(value);
  return {
    amount,
    currency: resolvedCurrency,
    display: formatMoney(amount, resolvedCurrency),
  };
}

function nullableString(value) {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function checkoutPayload(providerResponse, expectedBookHash) {
  const response = record(providerResponse);
  if (!response) throw new Error("Hotels API checkout response must be an object.");
  const payload = record(response.payload) ?? response;
  const directCandidate = record(payload.rate) ?? (nullableString(payload.bookHash) ? payload : null);
  const directBookHash = nullableString(directCandidate?.bookHash);
  const directRate = directCandidate && (!expectedBookHash || !directBookHash || directBookHash === expectedBookHash)
    ? directCandidate
    : null;
  const rooms = Array.isArray(record(payload.roomsForBooking)?.rooms) ? payload.roomsForBooking.rooms : [];
  const roomRates = rooms.flatMap((room) => Array.isArray(record(room)?.rates) ? room.rates : []).map(record).filter(Boolean);
  const rate = directRate
    ?? (expectedBookHash
      ? roomRates.find((candidate) => nullableString(candidate.bookHash) === expectedBookHash)
      : roomRates[0])
    ?? null;
  if (!rate) throw new Error("Hotels API checkout response does not contain a checkout rate.");
  return { payload, rate };
}

function cancellationSnapshot(rate, formatTimestamp, formatMoney) {
  const rules = record(rate.cancellationPolicyRules);
  const policies = Array.isArray(rules?.policies) ? rules.policies.map((policy) => {
    const item = record(policy) ?? {};
    return {
      startAt: nullableString(item.startAt),
      startAtDisplay: formatTimestamp(item.startAt),
      endAt: nullableString(item.endAt),
      endAtDisplay: formatTimestamp(item.endAt),
      shownPrice: money(item.shownPrice, formatMoney),
      paymentPrice: money(item.paymentPrice, formatMoney),
    };
  }) : [];
  const freeCancellationUntil = nullableString(rules?.freeCancellationUntil ?? rate.freeCancellationUntil);
  return {
    isNonRefundable: typeof rate.isNonRefundable === "boolean" ? rate.isNonRefundable : null,
    freeCancellationUntil,
    freeCancellationUntilDisplay: formatTimestamp(freeCancellationUntil),
    policies,
  };
}

function extraServiceOptions(rate, serviceReference, formatMoney) {
  const services = record(rate.extraServices);
  const normalize = (kind, values) => Array.isArray(values) ? values.map((value) => {
    const option = record(value) ?? {};
    const providerId = nullableString(option.id);
    if (!providerId) return null;
    return {
      extraServiceOptionRef: serviceReference(kind, providerId),
      kind,
      time: nullableString(option.time),
      price: money(option.price, formatMoney),
    };
  }).filter(Boolean) : [];
  return {
    earlyCheckIn: normalize("early_check_in", services?.earlyCheckIn),
    lateCheckOut: normalize("late_check_out", services?.lateCheckOut),
  };
}

function promocodeSnapshot(payload, formatMoney) {
  const info = record(payload.promocodeInfo);
  if (!info) return { present: false, status: null, validationErrorCode: null, discount: money(null, formatMoney) };
  return {
    present: true,
    status: nullableString(info.status),
    validationErrorCode: nullableString(info.validationErrorCode),
    discount: money(info.value, formatMoney),
  };
}

function cashbackSnapshot(payload) {
  const info = record(payload.cashbackInfo);
  if (!info) return { available: false, serviceName: null, options: [] };
  const accounts = Array.isArray(info.accounts) ? info.accounts : [];
  return {
    available: accounts.length > 0,
    serviceName: nullableString(info.cbServiceName),
    options: accounts.map((account) => {
      const item = record(account) ?? {};
      return {
        loyaltyProgram: nullableString(item.loyaltyProgram),
        loyaltyProgramCurrency: nullableString(item.loyaltyProgramCurrency),
        cashbackPercent: finiteNumber(item.cashbackPercent),
        cashbackAmount: finiteNumber(item.cashbackAmount),
        cashbackCorrectionAmount: finiteNumber(item.cashbackCorrectionAmount),
        topBorder: finiteNumber(item.topBorder),
      };
    }),
  };
}

export function normalizeCheckoutInspection(providerResponse, { expectedBookHash, serviceReference, formatMoney, formatTimestamp }) {
  const { payload, rate } = checkoutPayload(providerResponse, expectedBookHash);
  return {
    prices: {
      shown: money(rate.shownPrice, formatMoney),
      payment: money(rate.paymentPrice, formatMoney),
      standard: money(record(rate.discount)?.standardRatePrice, formatMoney),
      basis: "provider_total_for_stay",
    },
    cancellation: cancellationSnapshot(rate, formatTimestamp, formatMoney),
    extraServices: extraServiceOptions(rate, serviceReference, formatMoney),
    promocode: promocodeSnapshot(payload, formatMoney),
    cashback: cashbackSnapshot(payload),
  };
}

export function normalizePromocodeValidation(providerResponse, formatMoney) {
  const response = record(providerResponse);
  const payload = record(response?.payload) ?? response;
  const info = record(payload?.promocodeInfo);
  if (!info) throw new Error("Hotels API promocode validation response does not contain promocodeInfo.");
  return {
    status: "valid",
    discount: money(info.value, formatMoney),
  };
}

export function normalizeRateUpgrade(providerResponse, formatMoney) {
  const response = record(providerResponse);
  const payload = record(response?.payload) ?? response;
  const wrapper = record(payload?.rate);
  const upgrade = record(wrapper?.rateForUpgrade);
  if (!upgrade) return { available: false, upgradeType: null, additionalCost: money(null, formatMoney), room: null };
  const room = record(upgrade.room);
  const difference = record(room?.facilitiesDifference);
  return {
    available: true,
    upgradeType: nullableString(upgrade.upgradeType),
    additionalCost: money(upgrade.additionalCost, formatMoney),
    room: room ? {
      roomName: nullableString(room.roomName),
      bedName: nullableString(room.bedName),
      roomSize: finiteNumber(room.roomSize),
      upgradedFacilityCodes: Array.isArray(difference?.upgradedFacilityCodes)
        ? difference.upgradedFacilityCodes.filter((value) => typeof value === "string")
        : [],
    } : null,
  };
}
