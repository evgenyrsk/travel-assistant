export function readBrowserClientContext({
  intl = globalThis.Intl,
  navigator = globalThis.navigator,
} = {}) {
  const locale = normalizeHint(navigator?.language);
  const timezone = readTimeZone(intl);

  if (!locale && !timezone) {
    return undefined;
  }

  return {
    ...(locale ? { locale } : {}),
    ...(timezone ? { timezone } : {}),
  };
}

function readTimeZone(intl) {
  try {
    return normalizeHint(intl?.DateTimeFormat?.().resolvedOptions?.().timeZone);
  } catch {
    return undefined;
  }
}

function normalizeHint(value) {
  return typeof value === "string" && value.trim() ? value.trim() : undefined;
}
