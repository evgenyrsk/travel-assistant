import { existsSync, statSync } from "node:fs";
import { spawn } from "node:child_process";
import { randomUUID } from "node:crypto";
import { createInterface } from "node:readline";

const BANK_ORIGIN = "https://www.tbank.ru";
const HOTELS_HOME = `${BANK_ORIGIN}/travel/hotels/new/`;
const DEFAULT_SESSION = "tbank-hotels-mcp";
const SENSITIVE_ACTION = /(оплат|заброниров|оформ|подтверд|купить|отменить|pay\b|book\b|confirm\b|purchase\b|cancel\b)/i;
const STATE_CHANGING_ACTION = /(?:добавить|удалить).*(?:избранн|favorite)|(?:избранн|favorite).*(?:добавить|удалить)|скачать|download\b/i;
const ARMED_ACTION_TTL_MS = 5 * 60 * 1000;

const snapshotsBySession = new Map();
const armedActionsById = new Map();

const text = (value) => ({ type: "text", text: value });

const schema = (properties, required = []) => ({
  type: "object",
  properties,
  required,
  additionalProperties: false,
});

const tools = [
  {
    name: "tbank_hotels_open",
    description: "Открывает главную страницу отелей Т-Банка в изолированной браузерной сессии.",
    inputSchema: schema({ session: { type: "string", description: "Необязательное имя изолированной сессии." } }),
  },
  {
    name: "tbank_hotels_start_login",
    description: "Открывает ручной вход в Интернет-банк в видимом окне браузера. Не передавайте пароль, одноразовый код или данные карты через MCP.",
    inputSchema: schema({ session: { type: "string", description: "Необязательное имя изолированной сессии." } }),
  },
  {
    name: "tbank_hotels_import_auth_cookies",
    description: "Импортирует авторизованную cookie-сессию из локального файла, созданного пользователем. Содержимое файла не читается и не возвращается моделью.",
    inputSchema: schema({
      cookieFile: { type: "string", description: "Абсолютный путь к файлу c Cookie header или Copy as cURL." },
      session: { type: "string", description: "Необязательное имя изолированной сессии." },
    }, ["cookieFile"]),
  },
  {
    name: "tbank_hotels_open_favorites",
    description: "Открывает раздел «Избранное». Его содержимое доступно после авторизации.",
    inputSchema: schema({ session: { type: "string" } }),
  },
  {
    name: "tbank_hotels_open_orders",
    description: "Открывает раздел заказов Т-Путешествий. Его содержимое доступно после авторизации.",
    inputSchema: schema({ session: { type: "string" } }),
  },
  {
    name: "tbank_hotels_open_order",
    description: "Открывает карточку существующего заказа отеля по числовому номеру. Позволяет просмотреть статус, условия, документы и доступные действия, но не отменяет бронирование и не скачивает документы.",
    inputSchema: schema({
      orderId: { type: "string", description: "Только цифры, например 443021782873." },
      session: { type: "string" },
    }, ["orderId"]),
  },
  {
    name: "tbank_hotels_open_city",
    description: "Открывает публичную подборку отелей города по SEO slug, например russia/moscow.",
    inputSchema: schema({
      countrySlug: { type: "string", description: "Например: russia." },
      citySlug: { type: "string", description: "Например: moscow." },
      session: { type: "string" },
    }, ["countrySlug", "citySlug"]),
  },
  {
    name: "tbank_hotels_open_search_results",
    description: "Открывает выдачу отелей для города, дат и числа гостей. Не создаёт бронь и не фиксирует цену.",
    inputSchema: schema({
      countrySlug: { type: "string", description: "Например: russia." },
      citySlug: { type: "string", description: "Например: moscow." },
      checkIn: { type: "string", description: "Дата заезда в формате YYYY-MM-DD." },
      checkOut: { type: "string", description: "Дата выезда в формате YYYY-MM-DD." },
      guests: { type: "integer", minimum: 1, maximum: 8, description: "Число взрослых гостей от 1 до 8." },
      session: { type: "string" },
    }, ["countrySlug", "citySlug", "checkIn", "checkOut", "guests"]),
  },
  {
    name: "tbank_hotels_open_hotel",
    description: "Открывает карточку отеля по числовому идентификатору из URL Т-Банка.",
    inputSchema: schema({
      hotelId: { type: "string", description: "Только цифры, например 1441391." },
      session: { type: "string" },
    }, ["hotelId"]),
  },
  {
    name: "tbank_hotels_open_booking_preview",
    description: "Открывает карточку отеля с выбранными датами и гостями, чтобы просмотреть номера, тарифы, условия отмены и предельный шаг перед оформлением. Бронь и оплата не создаются.",
    inputSchema: schema({
      hotelId: { type: "string", description: "Только цифры, например 1441391." },
      checkIn: { type: "string", description: "Дата заезда в формате YYYY-MM-DD." },
      checkOut: { type: "string", description: "Дата выезда в формате YYYY-MM-DD." },
      guests: { type: "integer", minimum: 1, maximum: 8, description: "Число взрослых гостей от 1 до 8." },
      session: { type: "string" },
    }, ["hotelId", "checkIn", "checkOut", "guests"]),
  },
  {
    name: "tbank_hotels_fill_destination",
    description: "Вводит название отеля, города, страны или другую локацию в поисковую строку и показывает актуальные подсказки. Затем выберите подходящий вариант через snapshot и click.",
    inputSchema: schema({
      destination: { type: "string", description: "Название города, страны или отеля." },
      session: { type: "string" },
    }, ["destination"]),
  },
  {
    name: "tbank_hotels_arm_user_action",
    description: "Подготавливает действие с внешним эффектом из последнего snapshot: бронь, оплату, подтверждение, отмену, изменение избранного или скачивание. Само действие не выполняет; для выполнения требуется отдельное одноразовое подтверждение пользователя.",
    inputSchema: schema({
      ref: { type: "string", description: "Ref финальной или изменяющей кнопки из последнего snapshot." },
      session: { type: "string" },
    }, ["ref"]),
  },
  {
    name: "tbank_hotels_execute_armed_action",
    description: "Выполняет ранее подготовленное действие только после явного подтверждения пользователя непосредственно перед вызовом. Используйте исключительно для конкретной показанной пользователю брони, оплаты, отмены или другого подготовленного действия.",
    inputSchema: schema({
      actionId: { type: "string", description: "Одноразовый actionId из tbank_hotels_arm_user_action." },
      confirmation: { type: "string", description: "Точная строка подтверждения из ответа подготовки действия." },
    }, ["actionId", "confirmation"]),
  },
  {
    name: "tbank_hotels_snapshot",
    description: "Возвращает интерактивные элементы текущего экрана. Используйте полученные refs в click; после каждого изменения экрана вызовите snapshot снова.",
    inputSchema: schema({ session: { type: "string" } }),
  },
  {
    name: "tbank_hotels_click",
    description: "Нажимает интерактивный элемент из последнего snapshot. Бронирование, оплата, покупка, подтверждение, отмена, изменение избранного и скачивание намеренно заблокированы.",
    inputSchema: schema({
      ref: { type: "string", description: "Ref из последнего snapshot, например @e42." },
      session: { type: "string" },
    }, ["ref"]),
  },
  {
    name: "tbank_hotels_press",
    description: "Отправляет безопасную клавишу текущему элементу: Enter, Escape, ArrowDown, ArrowUp или Tab.",
    inputSchema: schema({
      key: { type: "string", enum: ["Enter", "Escape", "ArrowDown", "ArrowUp", "Tab"] },
      session: { type: "string" },
    }, ["key"]),
  },
  {
    name: "tbank_hotels_current_url",
    description: "Возвращает URL текущего экрана изолированной браузерной сессии.",
    inputSchema: schema({ session: { type: "string" } }),
  },
  {
    name: "tbank_hotels_close",
    description: "Закрывает изолированную браузерную сессию отелей.",
    inputSchema: schema({ session: { type: "string" } }),
  },
];

function sessionName(value) {
  if (value === undefined || value === "") return DEFAULT_SESSION;
  if (typeof value !== "string" || !/^[a-zA-Z0-9_-]{1,64}$/.test(value)) {
    throw new Error("session must contain only letters, digits, underscores, or hyphens.");
  }
  return `tbank-hotels-${value}`;
}

function slug(value, name) {
  if (typeof value !== "string" || !/^[a-z0-9-]+$/.test(value)) {
    throw new Error(`${name} must be a lowercase URL slug.`);
  }
  return value;
}

function numericId(value, name) {
  if (typeof value !== "string" || !/^\d+$/.test(value)) {
    throw new Error(`${name} must contain digits only.`);
  }
  return value;
}

function dateRange(checkIn, checkOut) {
  const datePattern = /^\d{4}-\d{2}-\d{2}$/;
  if (typeof checkIn !== "string" || typeof checkOut !== "string" || !datePattern.test(checkIn) || !datePattern.test(checkOut)) {
    throw new Error("checkIn and checkOut must use YYYY-MM-DD format.");
  }
  const start = new Date(`${checkIn}T00:00:00Z`);
  const end = new Date(`${checkOut}T00:00:00Z`);
  if (Number.isNaN(start.valueOf()) || Number.isNaN(end.valueOf()) || start.toISOString().slice(0, 10) !== checkIn || end.toISOString().slice(0, 10) !== checkOut || end <= start) {
    throw new Error("checkOut must be later than a valid checkIn date.");
  }
  return { checkIn, checkOut };
}

function guests(value) {
  if (!Number.isInteger(value) || value < 1 || value > 8) {
    throw new Error("guests must be an integer from 1 to 8.");
  }
  return value;
}

function hotelPreviewUrl(hotelId, checkIn, checkOut, guestCount) {
  const params = new URLSearchParams({ dateFrom: checkIn, dateTo: checkOut, guests: String(guestCount) });
  return `${HOTELS_HOME}hotels/${hotelId}/?${params}`;
}

function refLabel(session, ref) {
  const escaped = ref.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const snapshotText = snapshotsBySession.get(session) ?? "";
  const match = snapshotText.match(new RegExp(`^.*${escaped}.*$`, "m"));
  return match?.[0] ?? "";
}

function actionRequiresConfirmation(label) {
  return SENSITIVE_ACTION.test(label) || STATE_CHANGING_ACTION.test(label);
}

function requireSafeRef(session, ref) {
  if (typeof ref !== "string" || !/^@e\d+$/.test(ref)) {
    throw new Error("ref must be a value from the latest tbank_hotels_snapshot response.");
  }
  if (!snapshotsBySession.has(session)) {
    throw new Error("Call tbank_hotels_snapshot before clicking an element.");
  }
  const label = refLabel(session, ref);
  if (!label) {
    throw new Error("ref is not present in the latest tbank_hotels_snapshot response.");
  }
  if (actionRequiresConfirmation(label)) {
    throw new Error("This action requires explicit user confirmation. Call tbank_hotels_arm_user_action first.");
  }
}

function armAction(session, ref) {
  if (typeof ref !== "string" || !/^@e\d+$/.test(ref) || !snapshotsBySession.has(session)) {
    throw new Error("Call tbank_hotels_snapshot and provide a ref from it before preparing an action.");
  }
  const label = refLabel(session, ref);
  if (!label) throw new Error("ref is not present in the latest tbank_hotels_snapshot response.");
  if (!actionRequiresConfirmation(label)) {
    throw new Error("This is a non-final action. Use tbank_hotels_click instead.");
  }
  const actionId = randomUUID();
  const confirmation = `CONFIRM ${actionId}`;
  armedActionsById.set(actionId, { session, ref, label, confirmation, expiresAt: Date.now() + ARMED_ACTION_TTL_MS });
  return { actionId, confirmation, label };
}

async function executeArmedAction(actionId, confirmation) {
  const armed = armedActionsById.get(actionId);
  if (!armed) throw new Error("Unknown or already used actionId. Prepare the action again.");
  if (Date.now() > armed.expiresAt) {
    armedActionsById.delete(actionId);
    throw new Error("The action confirmation expired. Prepare the action again.");
  }
  if (confirmation !== armed.confirmation) {
    throw new Error("confirmation must exactly match the confirmation string returned when the action was prepared.");
  }
  await snapshot(armed.session);
  const currentLabel = refLabel(armed.session, armed.ref);
  if (!currentLabel || currentLabel !== armed.label) {
    armedActionsById.delete(actionId);
    throw new Error("The page changed after preparation. Review the latest snapshot and prepare the action again.");
  }
  await runAgentBrowser(armed.session, ["click", armed.ref]);
  armedActionsById.delete(actionId);
  return snapshot(armed.session);
}

function validateCookieFile(cookieFile) {
  if (typeof cookieFile !== "string" || !cookieFile.startsWith("/")) {
    throw new Error("cookieFile must be an absolute path to a local file.");
  }
  if (!existsSync(cookieFile) || !statSync(cookieFile).isFile()) {
    throw new Error("cookieFile must point to an existing regular file.");
  }
}

function runAgentBrowser(session, args, { headed = false } = {}) {
  return new Promise((resolve, reject) => {
    const commandArgs = ["--session", session];
    if (headed) commandArgs.push("--headed");
    commandArgs.push(...args);
    const child = spawn("agent-browser", commandArgs, { stdio: ["ignore", "pipe", "pipe"] });
    let stdout = "";
    let stderr = "";
    child.stdout.on("data", (chunk) => { stdout += chunk; });
    child.stderr.on("data", (chunk) => { stderr += chunk; });
    child.on("error", (spawnError) => reject(new Error(`Unable to start agent-browser: ${spawnError.message}`)));
    child.on("close", (code) => {
      if (code === 0) resolve(stdout.trim());
      else reject(new Error((stderr || stdout || `agent-browser exited with code ${code}`).trim()));
    });
  });
}

async function snapshot(session) {
  const output = await runAgentBrowser(session, ["snapshot", "-i", "-c"]);
  snapshotsBySession.set(session, output);
  return output;
}

async function openAndSnapshot(session, url, options) {
  await runAgentBrowser(session, ["open", url], options);
  await runAgentBrowser(session, ["wait", "--load", "networkidle"]);
  return snapshot(session);
}

async function callTool(name, args = {}) {
  const session = sessionName(args.session);
  switch (name) {
    case "tbank_hotels_open":
      return text(await openAndSnapshot(session, HOTELS_HOME));
    case "tbank_hotels_start_login": {
      await runAgentBrowser(session, ["open", HOTELS_HOME], { headed: true });
      await runAgentBrowser(session, ["wait", "--load", "networkidle"]);
      await runAgentBrowser(session, ["find", "role", "button", "click", "--name", "Личный кабинет"]);
      await runAgentBrowser(session, ["find", "role", "link", "click", "--name", "Интернет-банк"]);
      return text(`Окно входа открыто. Завершите вход вручную в видимом браузере, затем вызовите tbank_hotels_snapshot.\n\n${await snapshot(session)}`);
    }
    case "tbank_hotels_import_auth_cookies":
      validateCookieFile(args.cookieFile);
      await runAgentBrowser(session, ["cookies", "set", "--curl", args.cookieFile]);
      return text(`Cookie-сессия импортирована.\n\n${await openAndSnapshot(session, HOTELS_HOME)}`);
    case "tbank_hotels_open_favorites":
      return text(await openAndSnapshot(session, `${HOTELS_HOME}favorite/`));
    case "tbank_hotels_open_orders":
      return text(await openAndSnapshot(session, `${BANK_ORIGIN}/mybank/gorod/orders/?previousPageUrl=/travel/hotels/new/`));
    case "tbank_hotels_open_order":
      return text(await openAndSnapshot(session, `${HOTELS_HOME}orders/${numericId(args.orderId, "orderId")}/`));
    case "tbank_hotels_open_city": {
      const country = slug(args.countrySlug, "countrySlug");
      const city = slug(args.citySlug, "citySlug");
      return text(await openAndSnapshot(session, `${HOTELS_HOME}countries/${country}/${city}/`));
    }
    case "tbank_hotels_open_search_results": {
      const country = slug(args.countrySlug, "countrySlug");
      const city = slug(args.citySlug, "citySlug");
      const range = dateRange(args.checkIn, args.checkOut);
      const guestCount = guests(args.guests);
      const params = new URLSearchParams({ dateFrom: range.checkIn, dateTo: range.checkOut, guests: String(guestCount) });
      return text(await openAndSnapshot(session, `${HOTELS_HOME}countries/${country}/${city}/?${params}`));
    }
    case "tbank_hotels_open_hotel":
      return text(await openAndSnapshot(session, `${HOTELS_HOME}hotels/${numericId(args.hotelId, "hotelId")}/`));
    case "tbank_hotels_open_booking_preview": {
      const hotelId = numericId(args.hotelId, "hotelId");
      const range = dateRange(args.checkIn, args.checkOut);
      const guestCount = guests(args.guests);
      return text(await openAndSnapshot(session, hotelPreviewUrl(hotelId, range.checkIn, range.checkOut, guestCount)));
    }
    case "tbank_hotels_fill_destination":
      if (typeof args.destination !== "string" || args.destination.trim().length === 0 || args.destination.length > 160) {
        throw new Error("destination must be a non-empty string of at most 160 characters.");
      }
      await openAndSnapshot(session, HOTELS_HOME);
      await runAgentBrowser(session, ["find", "first", "input", "fill", args.destination.trim()]);
      await runAgentBrowser(session, ["wait", "750"]);
      return text(await snapshot(session));
    case "tbank_hotels_arm_user_action": {
      const armed = armAction(session, args.ref);
      return text(`Action prepared but not performed: ${armed.label}\n\nShow this exact action to the user and obtain an explicit confirmation immediately before execution. Then call tbank_hotels_execute_armed_action with actionId ${armed.actionId} and confirmation ${armed.confirmation}. The confirmation expires in 5 minutes.`);
    }
    case "tbank_hotels_execute_armed_action":
      return text(await executeArmedAction(args.actionId, args.confirmation));
    case "tbank_hotels_snapshot":
      return text(await snapshot(session));
    case "tbank_hotels_click":
      requireSafeRef(session, args.ref);
      await runAgentBrowser(session, ["click", args.ref]);
      return text(await snapshot(session));
    case "tbank_hotels_press":
      await runAgentBrowser(session, ["press", args.key]);
      return text(await snapshot(session));
    case "tbank_hotels_current_url":
      return text(await runAgentBrowser(session, ["get", "url"]));
    case "tbank_hotels_close":
      await runAgentBrowser(session, ["close"]);
      snapshotsBySession.delete(session);
      for (const [actionId, armed] of armedActionsById.entries()) if (armed.session === session) armedActionsById.delete(actionId);
      return text("Browser session closed.");
    default:
      throw new Error(`Unknown tool: ${name}`);
  }
}

function response(id, result) {
  return { jsonrpc: "2.0", id, result };
}

function error(id, code, message) {
  return { jsonrpc: "2.0", id, error: { code, message } };
}

function write(message) {
  process.stdout.write(`${JSON.stringify(message)}\n`);
}

async function handle(request) {
  if (request.id === undefined) return;
  if (request.jsonrpc !== "2.0") {
    write(error(request.id ?? null, -32600, "Invalid JSON-RPC version."));
    return;
  }
  if (request.method === "initialize") {
    write(response(request.id, {
      protocolVersion: request.params?.protocolVersion ?? "2025-03-26",
      capabilities: { tools: { listChanged: false } },
      serverInfo: { name: "tbank-hotels-browser-mcp", version: "0.1.0" },
      instructions: "Use the browser only for T-Bank Hotels. Complete login manually or import a local cookie file. For booking, payment, purchase, confirmation, cancellation, favorites, or downloads, first prepare the exact visible action, then obtain explicit user confirmation immediately before executing it through the one-time confirmation protocol.",
    }));
    return;
  }
  if (request.method === "tools/list") {
    write(response(request.id, { tools }));
    return;
  }
  if (request.method === "tools/call") {
    try {
      const content = await callTool(request.params?.name, request.params?.arguments);
      write(response(request.id, { content: [content], isError: false }));
    } catch (toolError) {
      write(response(request.id, { content: [text(toolError.message)], isError: true }));
    }
    return;
  }
  write(error(request.id, -32601, "Method not found."));
}

const input = createInterface({ input: process.stdin, crlfDelay: Infinity });
input.on("line", (line) => {
  try {
    void handle(JSON.parse(line));
  } catch {
    write(error(null, -32700, "Parse error."));
  }
});
