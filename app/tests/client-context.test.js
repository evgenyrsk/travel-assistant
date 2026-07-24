import test from "node:test";
import assert from "node:assert/strict";
import { readBrowserClientContext } from "../src/client-context.js";

test("reads locale and IANA timezone from the client device", () => {
  const context = readBrowserClientContext({
    intl: {
      DateTimeFormat() {
        return {
          resolvedOptions() {
            return { timeZone: "Europe/Moscow" };
          },
        };
      },
    },
    navigator: { language: "ru-RU" },
  });

  assert.deepEqual(context, {
    locale: "ru-RU",
    timezone: "Europe/Moscow",
  });
});

test("omits unavailable device hints instead of inventing a timezone", () => {
  const context = readBrowserClientContext({
    intl: {
      DateTimeFormat() {
        throw new Error("timezone unavailable");
      },
    },
    navigator: null,
  });

  assert.equal(context, undefined);
});
