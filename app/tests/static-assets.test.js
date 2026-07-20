import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const sourceUrl = new URL("../src/", import.meta.url);

test("chat and diagnostic pages reference the existing SVG favicon", async () => {
  const [chatPage, diagnosticPage, favicon] = await Promise.all([
    readFile(new URL("index.html", sourceUrl), "utf8"),
    readFile(new URL("diagnostic.html", sourceUrl), "utf8"),
    readFile(new URL("favicon.svg", sourceUrl), "utf8"),
  ]);

  for (const page of [chatPage, diagnosticPage]) {
    assert.match(page, /<link rel="icon" href="\/favicon\.svg" type="image\/svg\+xml">/);
  }
  assert.match(favicon, /^<svg[\s\S]*<\/svg>\s*$/);
});
