import { accessSync, chmodSync, constants, existsSync, mkdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import { delimiter, dirname, isAbsolute, resolve } from "node:path";
import { spawn } from "node:child_process";

export const publicPackageVersions = Object.freeze({
  hotels: "0.30.0",
  banking: "0.17.0",
  toolkit: "0.16.0",
});

function argumentValue(args, name) {
  const index = args.indexOf(name);
  return index < 0 ? undefined : args[index + 1];
}

function executableOnPath(commandName, environment = process.env) {
  for (const directory of String(environment.PATH ?? "").split(delimiter)) {
    if (!directory) continue;
    const candidate = resolve(directory, commandName);
    if (!existsSync(candidate) || !statSync(candidate).isFile()) continue;
    try { accessSync(candidate, constants.X_OK); } catch { continue; }
    return candidate;
  }
  return null;
}

function checkedExecutable(path, name) {
  if (!path || !isAbsolute(path) || !existsSync(path) || !statSync(path).isFile()) {
    throw new Error(`${name} is missing after installation.`);
  }
  try { accessSync(path, constants.X_OK); } catch { throw new Error(`${name} is not executable after installation.`); }
  return path;
}

function readExistingConfig(configPath) {
  if (!existsSync(configPath)) return {};
  const parsed = JSON.parse(readFileSync(configPath, "utf8"));
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) throw new Error("Local MCP config must contain a JSON object.");
  return parsed;
}

function writePrivateJson(path, value) {
  mkdirSync(dirname(path), { recursive: true, mode: 0o700 });
  writeFileSync(path, `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 });
  chmodSync(path, 0o600);
}

function run(command, args, { env = process.env, stdio = "inherit", allowFailure = false } = {}) {
  return new Promise((resolvePromise, rejectPromise) => {
    const child = spawn(command, args, { env, stdio });
    child.on("error", rejectPromise);
    child.on("exit", (code, signal) => {
      if (signal) return rejectPromise(new Error(`${command} was interrupted by ${signal}.`));
      if (code !== 0 && !allowFailure) return rejectPromise(new Error(`${command} exited with code ${code}.`));
      resolvePromise(code ?? 1);
    });
  });
}

export function managedRuntimePaths(runtimeDirectory) {
  const root = resolve(runtimeDirectory);
  const npmBin = resolve(root, "npm/node_modules/.bin");
  const pythonBin = resolve(root, "python/bin");
  return {
    root,
    npmPrefix: resolve(root, "npm"),
    pythonEnvironment: resolve(root, "python"),
    toolkit: resolve(npmBin, "tbank-mcp-local"),
    hotels: resolve(npmBin, "tbank-hotels-mcp"),
    banking: resolve(pythonBin, "tbank-banking-mcp"),
    broker: resolve(pythonBin, "tbank-auth-broker"),
    login: resolve(pythonBin, "tbank-banking-login"),
    python: resolve(pythonBin, "python"),
  };
}

export function clientRegistrationCommands(client, clientExecutable, toolkitExecutable, profile = "combined") {
  const components = profile === "combined" ? ["hotels", "banking"] : profile === "hotels" ? ["hotels"] : profile === "banking" ? ["banking"] : null;
  if (!components) throw new Error("profile must be hotels, banking, or combined.");
  if (!['opencode', 'codex', 'cursor'].includes(client)) throw new Error("connect currently supports cursor, codex, or opencode.");
  if (client === "cursor") return [];
  return components.flatMap((component) => {
    const name = `tbank-${component}`;
    const launcher = [toolkitExecutable, "run", component, ...(profile === "hotels" ? [] : ["--ensure-broker"])];
    if (client === "opencode") return [{ command: clientExecutable, args: ["mcp", "add", name, "--", ...launcher], allowFailure: false }];
    return [
      { command: clientExecutable, args: ["mcp", "remove", name], allowFailure: true },
      { command: clientExecutable, args: ["mcp", "add", name, "--", ...launcher], allowFailure: false },
    ];
  });
}

export function writeCursorRegistration(configPath, toolkitExecutable, profile = "combined") {
  const components = profile === "combined" ? ["hotels", "banking"] : profile === "hotels" ? ["hotels"] : profile === "banking" ? ["banking"] : null;
  if (!components) throw new Error("profile must be hotels, banking, or combined.");
  const existing = readExistingConfig(configPath);
  const mcpServers = { ...(existing.mcpServers ?? {}) };
  for (const component of components) {
    mcpServers[`tbank-${component}`] = {
      type: "stdio",
      command: toolkitExecutable,
      args: ["run", component, ...(profile === "hotels" ? [] : ["--ensure-broker"])],
    };
  }
  writePrivateJson(configPath, { ...existing, mcpServers });
  return configPath;
}

export async function connect(args, environment = process.env) {
  if (process.platform === "win32") throw new Error("Automatic combined setup currently requires macOS or Linux because the shared auth broker uses a Unix socket.");
  const positionalClient = args[0] && !args[0].startsWith("-") ? args[0] : undefined;
  const client = argumentValue(args, "--client") ?? positionalClient ?? "opencode";
  const profile = argumentValue(args, "--profile") ?? "combined";
  if (!['cursor', 'codex', 'opencode'].includes(client)) throw new Error("client must be cursor, codex, or opencode.");
  if (!['combined', 'hotels', 'banking'].includes(profile)) throw new Error("profile must be hotels, banking, or combined.");
  const userHome = environment.HOME ? resolve(environment.HOME) : homedir();
  const configPath = resolve(argumentValue(args, "--config") ?? environment.TBANK_MCP_LOCAL_CONFIG ?? resolve(userHome, ".config/tbank-mcp/config.json"));
  const runtimeDirectory = resolve(argumentValue(args, "--runtime-dir") ?? resolve(userHome, ".local/share/tbank-mcp/runtime"));
  const paths = managedRuntimePaths(runtimeDirectory);
  const skipInstall = args.includes("--skip-install");
  const skipLogin = args.includes("--skip-login") || profile === "hotels";
  const clientArgument = argumentValue(args, "--client-executable");
  const clientExecutable = client === "cursor" ? null : clientArgument
    ? checkedExecutable(resolve(clientArgument), "--client-executable")
    : executableOnPath(client, environment);
  if (client !== "cursor" && !clientExecutable) throw new Error(`${client} CLI is not installed or is not available in PATH.`);

  mkdirSync(paths.root, { recursive: true, mode: 0o700 });
  chmodSync(paths.root, 0o700);
  if (!skipInstall) {
    const npm = executableOnPath("npm", environment);
    if (!npm) throw new Error("Node.js 20+ with npm is required for automatic installation.");
    const npmPackages = [`tbank-mcp-local@${publicPackageVersions.toolkit}`];
    if (profile !== "banking") npmPackages.push(`tbank-hotels-mcp@${publicPackageVersions.hotels}`);
    await run(npm, [
      "install", "--prefix", paths.npmPrefix, "--ignore-scripts", "--no-audit", "--no-fund",
      ...npmPackages,
    ], { env: environment });
    if (profile !== "hotels") {
      const python = executableOnPath("python3", environment);
      if (!python) throw new Error("Python 3.11+ is required for automatic Banking MCP installation.");
      await run(python, ["-m", "venv", paths.pythonEnvironment], { env: environment });
      await run(paths.python, [
        "-m", "pip", "install", "--disable-pip-version-check", "--upgrade",
        `travel-assistant-tbank-banking-mcp==${publicPackageVersions.banking}`,
      ], { env: environment });
    }
  }

  const requiredComponents = profile === "combined" ? ["toolkit", "hotels", "banking", "broker", "login"] : profile === "hotels" ? ["toolkit", "hotels"] : ["toolkit", "banking", "broker", "login"];
  for (const component of requiredComponents) checkedExecutable(paths[component], component);

  const existing = readExistingConfig(configPath);
  const next = {
    ...existing,
    version: 1,
    runtimeExecutables: {
      ...(existing.runtimeExecutables ?? {}),
      ...(profile !== "banking" ? { hotels: paths.hotels } : {}),
      ...(profile !== "hotels" ? { banking: paths.banking, broker: paths.broker, login: paths.login } : {}),
    },
  };
  if (profile !== "banking") next.hotels = {
    ...(existing.hotels ?? {}),
    apiBaseUrl: argumentValue(args, "--hotels-api-base-url") ?? existing.hotels?.apiBaseUrl ?? "https://hotels.tbank.ru/api",
    maxConcurrentRequests: existing.hotels?.maxConcurrentRequests ?? 2,
  };
  if (profile !== "hotels") next.banking = {
    ...(existing.banking ?? {}),
    sessionFile: existing.banking?.sessionFile ?? resolve(userHome, ".local/share/tbank-banking-mcp/session.json"),
    brokerSocket: existing.banking?.brokerSocket ?? resolve(userHome, ".local/share/tbank-auth-broker/auth.sock"),
  };
  writePrivateJson(configPath, next);

  if (client === "cursor") {
    const cursorConfigPath = resolve(argumentValue(args, "--cursor-config") ?? resolve(userHome, ".cursor/mcp.json"));
    writeCursorRegistration(cursorConfigPath, paths.toolkit, profile);
  } else {
    for (const registration of clientRegistrationCommands(client, clientExecutable, paths.toolkit, profile)) {
      await run(registration.command, registration.args, { env: environment, stdio: "inherit", allowFailure: registration.allowFailure });
    }
  }

  if (!skipLogin) {
    const loginEnvironment = Object.fromEntries(Object.entries(environment).filter(([name]) => !name.startsWith("TBANK_")));
    loginEnvironment.TBANK_BANKING_SESSION = next.banking.sessionFile;
    loginEnvironment.TBANK_AUTH_BROKER_SOCKET = next.banking.brokerSocket;
    await run(paths.login, [], { env: loginEnvironment, stdio: "inherit" });
  }

  const report = {
    connectVersion: "1.0",
    client,
    profile,
    installed: !skipInstall,
    registeredComponents: profile === "combined" ? ["hotels", "banking"] : [profile],
    mobileLoginCompleted: !skipLogin,
    mobileLoginMayContactProvider: !skipLogin,
    configContainsCredentials: false,
    providerRequestsPerformedByInstaller: false,
    nextStep: `Restart ${client === "cursor" ? "Cursor" : client} and ask a natural-language hotel or spending-profile question.`,
  };
  process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
  return report;
}
