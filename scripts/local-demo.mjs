#!/usr/bin/env node

import { spawn, spawnSync } from "node:child_process";
import {
  chmodSync,
  closeSync,
  existsSync,
  mkdirSync,
  openSync,
  readFileSync,
} from "node:fs";
import net from "node:net";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(scriptDirectory, "..");
const defaultEnvFile = path.join(repositoryRoot, ".env");
const defaultBackendPort = 8080;
const defaultFrontendPort = 4173;
const defaultStartupTimeoutMillis = 120_000;
const localDemoHost = "127.0.0.1";
const demoEnvFileKeys = new Set([
  "JAVA_HOME",
  "LLM_PROVIDER_MODE",
  "HOTEL_PROVIDER_MODE",
  "OPENROUTER_API_KEY",
  "OPENROUTER_MODEL",
  "OPENROUTER_BASE_URL",
  "OPENROUTER_TIMEOUT_MS",
  "HOTELS_API_PUBLIC_BASE_URL",
  "HOTELS_API_PUBLIC_TIMEOUT_MS",
  "HOTELS_API_USER_LANGUAGE",
  "DEMO_BACKEND_PORT",
  "DEMO_FRONTEND_PORT",
  "DEMO_STARTUP_TIMEOUT_MS",
]);

export function parseArguments(argv) {
  let profile;
  let checkOnly = false;
  let envFile;
  let help = false;

  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index];
    if (argument === "--real" || argument === "--fake") {
      const selected = argument.slice(2);
      if (profile && profile !== selected) {
        throw new Error("Нужно выбрать только один профиль: --real или --fake.");
      }
      profile = selected;
    } else if (argument === "--check-only") {
      checkOnly = true;
    } else if (argument === "--env-file") {
      envFile = argv[index + 1];
      if (!envFile) {
        throw new Error("После --env-file требуется путь к файлу.");
      }
      index += 1;
    } else if (argument === "--help" || argument === "-h") {
      help = true;
    } else {
      throw new Error(`Неизвестный аргумент: ${argument}`);
    }
  }

  if (!help && !profile) {
    throw new Error("Укажите явный профиль --real или --fake.");
  }

  return { profile, checkOnly, envFile, help };
}

export function parseEnvContent(content) {
  const values = {};
  const lines = content.replaceAll("\r\n", "\n").split("\n");

  for (let index = 0; index < lines.length; index += 1) {
    const trimmed = lines[index].trim();
    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }

    const declaration = trimmed.startsWith("export ")
      ? trimmed.slice("export ".length).trim()
      : trimmed;
    const match = declaration.match(/^([A-Za-z_][A-Za-z0-9_]*)=(.*)$/);
    if (!match) {
      throw new Error(`Некорректная строка ${index + 1} в env-файле.`);
    }

    const [, key, rawValue] = match;
    values[key] = unwrapEnvValue(rawValue.trim(), index + 1);
  }

  return values;
}

export function selectDemoFileValues(values) {
  return Object.fromEntries(
    Object.entries(values).filter(([key]) => demoEnvFileKeys.has(key)),
  );
}

function unwrapEnvValue(rawValue, lineNumber) {
  if (!rawValue) {
    return "";
  }

  const quote = rawValue[0];
  if (quote !== '"' && quote !== "'") {
    return rawValue;
  }
  if (rawValue.at(-1) !== quote) {
    throw new Error(`Незакрытая кавычка в строке ${lineNumber} env-файла.`);
  }
  return rawValue.slice(1, -1);
}

export function buildProfileEnvironment(profile, inputEnvironment) {
  const environment = { ...inputEnvironment };
  environment.OPENROUTER_RUNTIME_QA_ENABLED = "false";
  environment.HOTELS_API_RUNTIME_QA_ENABLED = "false";

  if (profile === "fake") {
    environment.LLM_PROVIDER_MODE = "FAKE";
    environment.HOTEL_PROVIDER_MODE = "FAKE";
    delete environment.OPENROUTER_API_KEY;
    return environment;
  }

  if (profile !== "real") {
    throw new Error("Неподдерживаемый demo-профиль.");
  }

  requireNonBlank(environment, "OPENROUTER_API_KEY");
  requireNonBlank(environment, "OPENROUTER_MODEL");
  validateOptionalHttpsUrl(environment, "OPENROUTER_BASE_URL");
  validateOptionalHttpsUrl(environment, "HOTELS_API_PUBLIC_BASE_URL");
  validateOptionalPositiveInteger(environment, "OPENROUTER_TIMEOUT_MS");
  validateOptionalPositiveInteger(environment, "HOTELS_API_PUBLIC_TIMEOUT_MS");

  environment.LLM_PROVIDER_MODE = "OPENROUTER";
  environment.HOTEL_PROVIDER_MODE = "REAL";
  return environment;
}

function requireNonBlank(environment, key) {
  if (!environment[key]?.trim()) {
    throw new Error(`Для профиля --real требуется ${key}.`);
  }
}

function validateOptionalHttpsUrl(environment, key) {
  const rawValue = environment[key]?.trim();
  if (!rawValue) {
    return;
  }

  let url;
  try {
    url = new URL(rawValue);
  } catch {
    throw new Error(`${key} должен быть корректным HTTPS URL.`);
  }
  if (url.protocol !== "https:" || url.username || url.password) {
    throw new Error(`${key} должен быть HTTPS URL без credentials.`);
  }
}

function validateOptionalPositiveInteger(environment, key) {
  const rawValue = environment[key]?.trim();
  if (!rawValue) {
    return;
  }
  if (!/^\d+$/.test(rawValue) || Number(rawValue) <= 0) {
    throw new Error(`${key} должен быть положительным целым числом.`);
  }
}

export function validatePort(rawValue, name) {
  const port = Number(rawValue);
  if (!Number.isInteger(port) || port < 1 || port > 65_535) {
    throw new Error(`${name} должен быть целым числом от 1 до 65535.`);
  }
  return port;
}

export function buildBackendProcessEnvironment(environment, backendPort) {
  return {
    ...environment,
    HOST: localDemoHost,
    PORT: String(backendPort),
  };
}

export async function isPortAvailable(port, host = "127.0.0.1") {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.unref();
    server.once("error", (error) => {
      if (error.code === "EADDRINUSE" || error.code === "EACCES") {
        resolve(false);
      } else {
        reject(error);
      }
    });
    server.listen({ host, port, exclusive: true }, () => {
      server.close((error) => {
        if (error) reject(error);
        else resolve(true);
      });
    });
  });
}

function loadEnvironment(envFile, explicitlySelected) {
  if (!existsSync(envFile)) {
    if (explicitlySelected) {
      throw new Error("Указанный env-файл не найден.");
    }
    return { ...process.env };
  }

  const fileValues = selectDemoFileValues(parseEnvContent(readFileSync(envFile, "utf8")));
  return { ...fileValues, ...process.env };
}

async function runPreflight(environment) {
  const nodeMajor = Number(process.versions.node.split(".")[0]);
  if (!Number.isInteger(nodeMajor) || nodeMajor < 18) {
    throw new Error("Для demo launcher требуется Node.js 18 или новее.");
  }

  const javaExecutable = environment.JAVA_HOME
    ? path.join(environment.JAVA_HOME, "bin", "java")
    : "java";
  const javaResult = spawnSync(javaExecutable, ["-version"], {
    encoding: "utf8",
    env: environment,
  });
  const javaVersionText = `${javaResult.stdout ?? ""}\n${javaResult.stderr ?? ""}`;
  const javaMajor = Number(javaVersionText.match(/version\s+"?(\d+)/)?.[1]);
  if (javaResult.status !== 0 || javaMajor !== 17) {
    throw new Error("Для backend требуется Java 17; настройте JAVA_HOME или PATH.");
  }

  const npmResult = spawnSync("npm", ["--version"], {
    encoding: "utf8",
    env: environment,
  });
  if (npmResult.status !== 0) {
    throw new Error("Команда npm недоступна в PATH.");
  }

  const gradleWrapper = path.join(repositoryRoot, "services", "backend", "gradlew");
  if (!existsSync(gradleWrapper)) {
    throw new Error("Gradle wrapper backend не найден.");
  }

  const backendPort = validatePort(
    environment.DEMO_BACKEND_PORT ?? defaultBackendPort,
    "DEMO_BACKEND_PORT",
  );
  const frontendPort = validatePort(
    environment.DEMO_FRONTEND_PORT ?? defaultFrontendPort,
    "DEMO_FRONTEND_PORT",
  );
  if (backendPort === frontendPort) {
    throw new Error("Backend и demo shell должны использовать разные порты.");
  }

  const [backendAvailable, frontendAvailable] = await Promise.all([
    isPortAvailable(backendPort),
    isPortAvailable(frontendPort),
  ]);
  if (!backendAvailable) {
    throw new Error(`Порт backend ${backendPort} уже занят.`);
  }
  if (!frontendAvailable) {
    throw new Error(`Порт demo shell ${frontendPort} уже занят.`);
  }

  return {
    backendPort,
    frontendPort,
    startupTimeoutMillis: validatePositiveInteger(
      environment.DEMO_STARTUP_TIMEOUT_MS ?? defaultStartupTimeoutMillis,
      "DEMO_STARTUP_TIMEOUT_MS",
    ),
  };
}

function validatePositiveInteger(rawValue, name) {
  const value = Number(rawValue);
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${name} должен быть положительным целым числом.`);
  }
  return value;
}

async function startDemo(environment, profile, preflight) {
  const logDirectory = path.join(repositoryRoot, ".tmp", "local-demo");
  mkdirSync(logDirectory, { recursive: true, mode: 0o700 });
  chmodSync(logDirectory, 0o700);

  const backendLogPath = path.join(logDirectory, "backend.log");
  const frontendLogPath = path.join(logDirectory, "frontend.log");
  const backendLog = openSync(backendLogPath, "w", 0o600);
  const frontendLog = openSync(frontendLogPath, "w", 0o600);
  const children = [];

  try {
    const backend = spawn("./gradlew", ["--no-daemon", "run"], {
      cwd: path.join(repositoryRoot, "services", "backend"),
      detached: process.platform !== "win32",
      env: buildBackendProcessEnvironment(environment, preflight.backendPort),
      stdio: ["ignore", backendLog, backendLog],
    });
    children.push({ name: "Backend", process: backend });

    await waitForHttp(
      `http://127.0.0.1:${preflight.backendPort}/api/v1/health`,
      backend,
      preflight.startupTimeoutMillis,
      "Backend",
    );

    const frontend = spawn("npm", ["run", "dev"], {
      cwd: path.join(repositoryRoot, "app"),
      detached: process.platform !== "win32",
      env: {
        ...environment,
        BACKEND_URL: `http://127.0.0.1:${preflight.backendPort}`,
        PORT: String(preflight.frontendPort),
      },
      stdio: ["ignore", frontendLog, frontendLog],
    });
    children.push({ name: "Demo shell", process: frontend });

    await waitForHttp(
      `http://127.0.0.1:${preflight.frontendPort}/`,
      frontend,
      preflight.startupTimeoutMillis,
      "Demo shell",
    );

    console.log(`Demo profile: ${profile.toUpperCase()}`);
    console.log(`Backend: http://127.0.0.1:${preflight.backendPort}`);
    console.log(`Demo shell: http://127.0.0.1:${preflight.frontendPort}`);
    console.log(`Логи: ${path.relative(repositoryRoot, logDirectory)}/`);
    console.log("Для завершения нажмите Ctrl+C.");

    await waitForTermination(children);
  } finally {
    await stopChildren(children);
    closeSync(backendLog);
    closeSync(frontendLog);
  }
}

async function waitForHttp(url, child, timeoutMillis, name) {
  const deadline = Date.now() + timeoutMillis;
  while (Date.now() < deadline) {
    if (child.exitCode !== null) {
      throw new Error(`${name} завершился до готовности. Проверьте локальный log-файл.`);
    }
    try {
      const response = await fetch(url, {
        redirect: "error",
        signal: AbortSignal.timeout(2_000),
      });
      if (response.ok) {
        return;
      }
    } catch {
      // Ожидаем готовность локального процесса без вывода response body.
    }
    await delay(300);
  }
  throw new Error(`${name} не стал доступен за отведенное время.`);
}

function waitForTermination(children) {
  return new Promise((resolve, reject) => {
    const listeners = [];
    const finish = (callback) => {
      for (const [emitter, event, listener] of listeners) {
        emitter.off(event, listener);
      }
      callback();
    };

    for (const signal of ["SIGINT", "SIGTERM"]) {
      const listener = () => finish(resolve);
      process.once(signal, listener);
      listeners.push([process, signal, listener]);
    }

    for (const child of children) {
      const listener = (code, signal) => finish(() => reject(
        new Error(
          `${child.name} неожиданно завершился ` +
          `(code=${code ?? "none"}, signal=${signal ?? "none"}).`,
        ),
      ));
      child.process.once("exit", listener);
      listeners.push([child.process, "exit", listener]);
    }
  });
}

async function stopChildren(children) {
  for (const child of [...children].reverse()) {
    signalChild(child.process, "SIGTERM");
  }
  await Promise.race([
    Promise.all(children.map(({ process: child }) => waitForExit(child))),
    delay(5_000),
  ]);
  for (const child of [...children].reverse()) {
    if (child.process.exitCode === null) {
      signalChild(child.process, "SIGKILL");
    }
  }
}

function signalChild(child, signal) {
  if (!child?.pid || child.exitCode !== null) {
    return;
  }
  try {
    if (process.platform === "win32") child.kill(signal);
    else process.kill(-child.pid, signal);
  } catch {
    // Процесс уже завершен.
  }
}

function waitForExit(child) {
  if (child.exitCode !== null) {
    return Promise.resolve();
  }
  return new Promise((resolve) => child.once("exit", resolve));
}

function delay(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

function printUsage() {
  console.log(`Использование:
  node scripts/local-demo.mjs --fake [--check-only] [--env-file PATH]
  node scripts/local-demo.mjs --real [--check-only] [--env-file PATH]

Профиль обязателен. Содержимое env-файла читается как данные и не исполняется.`);
}

async function main() {
  const arguments_ = parseArguments(process.argv.slice(2));
  if (arguments_.help) {
    printUsage();
    return;
  }

  const envFile = path.resolve(
    process.cwd(),
    arguments_.envFile ?? process.env.DEMO_ENV_FILE ?? defaultEnvFile,
  );
  const loadedEnvironment = loadEnvironment(envFile, Boolean(arguments_.envFile));
  const environment = buildProfileEnvironment(arguments_.profile, loadedEnvironment);
  const preflight = await runPreflight(environment);

  console.log(`Preflight ${arguments_.profile.toUpperCase()}: OK`);
  if (!arguments_.checkOnly) {
    await startDemo(environment, arguments_.profile, preflight);
  }
}

const isEntrypoint = process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url;
if (isEntrypoint) {
  main().catch((error) => {
    console.error(`Demo launcher: ${error instanceof Error ? error.message : "неизвестная ошибка"}`);
    process.exitCode = 1;
  });
}
