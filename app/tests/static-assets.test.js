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

test("pages expose bounded standalone PWA metadata", async () => {
  const [chatPage, diagnosticPage] = await Promise.all([
    readFile(new URL("index.html", sourceUrl), "utf8"),
    readFile(new URL("diagnostic.html", sourceUrl), "utf8"),
  ]);

  for (const page of [chatPage, diagnosticPage]) {
    assert.match(page, /<link rel="manifest" href="\/manifest\.webmanifest">/);
    assert.match(page, /<meta name="theme-color" content="#0e6253">/);
    assert.match(page, /<meta name="viewport" content="[^"]*viewport-fit=cover">/);
    assert.match(page, /<link rel="apple-touch-icon" href="\/icons\/app-icon-180\.png" sizes="180x180">/);
  }
});

test("manifest defines standalone presentation and local raster icons", async () => {
  const manifest = JSON.parse(await readFile(new URL("manifest.webmanifest", sourceUrl), "utf8"));

  assert.equal(manifest.id, "/");
  assert.equal(manifest.start_url, "/");
  assert.equal(manifest.scope, "/");
  assert.equal(manifest.display, "standalone");
  assert.equal(manifest.background_color, "#f3f6f4");
  assert.equal(manifest.theme_color, "#0e6253");
  assert.equal(manifest.prefer_related_applications, false);
  assert.equal("serviceworker" in manifest, false);
  assert.deepEqual(
    manifest.icons.map(({ src, sizes, type, purpose }) => ({ src, sizes, type, purpose })),
    [
      {
        src: "/icons/app-icon-192.png",
        sizes: "192x192",
        type: "image/png",
        purpose: "any maskable",
      },
      {
        src: "/icons/app-icon-512.png",
        sizes: "512x512",
        type: "image/png",
        purpose: "any maskable",
      },
    ],
  );
});

test("PWA raster icons have the declared dimensions", async () => {
  const icons = [
    ["icons/app-icon-180.png", 180],
    ["icons/app-icon-192.png", 192],
    ["icons/app-icon-512.png", 512],
  ];

  for (const [path, expectedSize] of icons) {
    const icon = await readFile(new URL(path, sourceUrl));
    assert.deepEqual([...icon.subarray(0, 8)], [137, 80, 78, 71, 13, 10, 26, 10]);
    assert.equal(icon.readUInt32BE(16), expectedSize);
    assert.equal(icon.readUInt32BE(20), expectedSize);
  }
});

test("frontend remains online-only without service worker or cache storage", async () => {
  const scriptNames = ["api-client.js", "app.js", "chat-app.js", "chat-flow.js", "offer-view.js"];
  const scripts = await Promise.all(scriptNames.map((name) => readFile(new URL(name, sourceUrl), "utf8")));
  const server = await readFile(new URL("../server.mjs", import.meta.url), "utf8");

  for (const source of scripts) {
    assert.doesNotMatch(source, /serviceWorker|caches\.(?:open|match|put)/);
  }
  assert.match(server, /\["\.png", "image\/png"\]/);
  assert.match(server, /\["\.webmanifest", "application\/manifest\+json; charset=utf-8"\]/);
  assert.match(server, /responseHeaders\["cache-control"\] = "no-store"/);
  assert.match(server, /"cache-control": "no-store"/);
});
