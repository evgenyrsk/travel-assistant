import assert from "node:assert/strict";
import { createServer } from "node:net";
import test from "node:test";

import {
  buildBackendProcessEnvironment,
  buildProfileEnvironment,
  isPortAvailable,
  parseArguments,
  parseEnvContent,
  selectDemoFileValues,
  validatePort,
} from "./local-demo.mjs";

test("binds the demo backend to loopback even when the shell host is broader", () => {
  assert.deepEqual(
    buildBackendProcessEnvironment(
      {
        HOST: "0.0.0.0",
        LLM_PROVIDER_MODE: "OPENROUTER",
      },
      8080,
    ),
    {
      HOST: "127.0.0.1",
      PORT: "8080",
      LLM_PROVIDER_MODE: "OPENROUTER",
    },
  );
});

test("requires one explicit demo profile", () => {
  assert.deepEqual(parseArguments(["--fake", "--check-only"]), {
    profile: "fake",
    checkOnly: true,
    envFile: undefined,
    help: false,
  });
  assert.throws(() => parseArguments([]), /--real или --fake/);
  assert.throws(() => parseArguments(["--real", "--fake"]), /только один профиль/);
});

test("reads env values as data without shell evaluation", () => {
  assert.deepEqual(
    parseEnvContent(`
      # local values
      export OPENROUTER_MODEL="provider/model"
      OPENROUTER_API_KEY='local-value'
      HOTELS_API_USER_LANGUAGE=RU
    `),
    {
      OPENROUTER_MODEL: "provider/model",
      OPENROUTER_API_KEY: "local-value",
      HOTELS_API_USER_LANGUAGE: "RU",
    },
  );
  assert.throws(
    () => parseEnvContent("touch /tmp/should-not-run"),
    /Некорректная строка 1/,
  );
});

test("imports only launcher configuration from the env file", () => {
  assert.deepEqual(
    selectDemoFileValues({
      OPENROUTER_MODEL: "provider/model",
      NODE_OPTIONS: "--require=/tmp/untrusted.js",
      BASH_ENV: "/tmp/untrusted.sh",
    }),
    { OPENROUTER_MODEL: "provider/model" },
  );
});

test("fake profile overrides modes and removes OpenRouter key", () => {
  const environment = buildProfileEnvironment("fake", {
    LLM_PROVIDER_MODE: "OPENROUTER",
    HOTEL_PROVIDER_MODE: "REAL",
    OPENROUTER_API_KEY: "must-not-propagate",
  });

  assert.equal(environment.LLM_PROVIDER_MODE, "FAKE");
  assert.equal(environment.HOTEL_PROVIDER_MODE, "FAKE");
  assert.equal(environment.OPENROUTER_API_KEY, undefined);
  assert.equal(environment.OPENROUTER_RUNTIME_QA_ENABLED, "false");
  assert.equal(environment.HOTELS_API_RUNTIME_QA_ENABLED, "false");
});

test("real profile fails closed without credentials and never reveals a key", () => {
  assert.throws(
    () => buildProfileEnvironment("real", {}),
    /OPENROUTER_API_KEY/,
  );

  const secret = "local-super-secret";
  let message = "";
  try {
    buildProfileEnvironment("real", {
      OPENROUTER_API_KEY: secret,
      OPENROUTER_MODEL: "provider/model",
      OPENROUTER_BASE_URL: "http://not-allowed.example",
    });
  } catch (error) {
    message = error.message;
  }
  assert.match(message, /OPENROUTER_BASE_URL/);
  assert.equal(message.includes(secret), false);
});

test("validates ports and detects an occupied local port", async () => {
  assert.equal(validatePort("4173", "PORT"), 4173);
  assert.throws(() => validatePort("0", "PORT"), /от 1 до 65535/);

  const server = createServer();
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const { port } = server.address();

  assert.equal(await isPortAvailable(port), false);
  await new Promise((resolve, reject) => server.close((error) => {
    if (error) reject(error);
    else resolve();
  }));
  assert.equal(await isPortAvailable(port), true);
});
