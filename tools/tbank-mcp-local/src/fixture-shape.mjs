import { chmodSync, lstatSync, mkdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { dirname, isAbsolute, resolve } from "node:path";

const MAX_INPUT_BYTES = 2 * 1024 * 1024;
const MAX_DEPTH = 24;
const MAX_NODES = 50_000;

function safePropertyName(name) {
  if (/^[0-9]{4,}$/.test(name)) return "<dynamic-key>";
  if (/^\+?\d[\d ()-]{6,}$/.test(name)) return "<dynamic-key>";
  if (/^[0-9a-f]{8}-[0-9a-f-]{27,}$/i.test(name)) return "<dynamic-key>";
  if (/^[0-9a-f]{32,}$/i.test(name)) return "<dynamic-key>";
  if (/^[A-Za-z0-9_-]{24,}={0,2}$/.test(name)) return "<dynamic-key>";
  if (/^(?=[A-Za-z0-9_-]{12,23}$)(?=(?:.*[A-Za-z]){4})(?=(?:.*\d){4})/.test(name)) return "<dynamic-key>";
  if (/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(name)) return "<dynamic-key>";
  if (/\s|:\/\//.test(name)) return "<dynamic-key>";
  if (name.length > 96) return "<dynamic-key>";
  return name;
}

function schemaKind(schema) {
  return schema.type ?? "union";
}

function schemaFingerprint(schema) {
  return JSON.stringify(schema);
}

function mergeSchemas(left, right) {
  if (!left) return right;
  if (!right) return left;
  if (schemaFingerprint(left) === schemaFingerprint(right)) return left;

  if (left.type === "object" && right.type === "object") {
    const propertyNames = [...new Set([
      ...Object.keys(left.properties),
      ...Object.keys(right.properties),
    ])].sort();
    const properties = Object.fromEntries(propertyNames.map((name) => [
      name,
      left.properties[name] && right.properties[name]
        ? mergeSchemas(left.properties[name], right.properties[name])
        : left.properties[name] ?? right.properties[name],
    ]));
    const rightObserved = new Set(right.observedInEveryObject);
    return {
      type: "object",
      properties,
      observedInEveryObject: left.observedInEveryObject.filter((name) => rightObserved.has(name)),
    };
  }

  if (left.type === "array" && right.type === "array") {
    return { type: "array", items: mergeSchemas(left.items, right.items) };
  }

  const alternatives = [
    ...(left.anyOf ?? [left]),
    ...(right.anyOf ?? [right]),
  ];
  const unique = [...new Map(alternatives.map((item) => [schemaFingerprint(item), item])).values()];
  unique.sort((a, b) => schemaKind(a).localeCompare(schemaKind(b)));
  return { anyOf: unique };
}

function inferShape(value, state, depth = 0) {
  state.nodes += 1;
  if (state.nodes > MAX_NODES) throw new Error(`Fixture exceeds the ${MAX_NODES} node safety limit.`);
  if (depth > MAX_DEPTH) throw new Error(`Fixture exceeds the ${MAX_DEPTH} level depth safety limit.`);

  if (value === null) return { type: "null" };
  if (Array.isArray(value)) {
    const items = value.reduce(
      (shape, item) => mergeSchemas(shape, inferShape(item, state, depth + 1)),
      undefined,
    );
    return { type: "array", items: items ?? { type: "unknown" } };
  }
  if (typeof value === "object") {
    const properties = {};
    for (const [originalName, child] of Object.entries(value)) {
      const name = safePropertyName(originalName);
      const childShape = inferShape(child, state, depth + 1);
      properties[name] = mergeSchemas(properties[name], childShape);
    }
    const names = Object.keys(properties).sort();
    return {
      type: "object",
      properties: Object.fromEntries(names.map((name) => [name, properties[name]])),
      observedInEveryObject: names,
    };
  }
  if (typeof value === "string") return { type: "string" };
  if (typeof value === "number") return { type: Number.isInteger(value) ? "integer" : "number" };
  if (typeof value === "boolean") return { type: "boolean" };
  throw new Error("Fixture contains a value that JSON cannot represent.");
}

export function buildStructureOnlyReport(value) {
  const state = { nodes: 0 };
  return {
    reportVersion: "1.0",
    mode: "structure_only",
    providerRequestsPerformed: false,
    sourceValuesIncluded: false,
    sourceIdentifiersIncluded: false,
    limitations: {
      observedShapeOnly: true,
      requiredContractFieldsNotProven: true,
      paymentSemanticsNotInferred: true,
      dynamicKeyMaskingIsHeuristic: true,
    },
    shape: inferShape(value, state),
  };
}

export function writeStructureOnlyReport(report, outputPath, protectedSourceMetadata = null) {
  if (!outputPath || !isAbsolute(outputPath)) throw new Error("--output must be an absolute path.");
  const output = resolve(outputPath);
  let outputLinkMetadata;
  try {
    outputLinkMetadata = lstatSync(output);
  } catch (error) {
    if (error?.code !== "ENOENT") throw error;
  }
  if (outputLinkMetadata) {
    if (outputLinkMetadata.isSymbolicLink()) throw new Error("--output must not be a symbolic link.");
    const outputMetadata = statSync(output);
    if (protectedSourceMetadata && outputMetadata.dev === protectedSourceMetadata.dev && outputMetadata.ino === protectedSourceMetadata.ino) {
      throw new Error("--output must not overwrite the source fixture.");
    }
  }
  mkdirSync(dirname(output), { recursive: true, mode: 0o700 });
  writeFileSync(output, `${JSON.stringify(report, null, 2)}\n`, { mode: 0o600 });
  chmodSync(output, 0o600);
  return `${JSON.stringify({
    reportVersion: report.reportVersion,
    status: "created",
    providerRequestsPerformed: report.providerRequestsPerformed,
    sourceValuesIncluded: false,
  }, null, 2)}\n`;
}

export function inspectBookingFixture({ inputPath, outputPath }) {
  if (!inputPath || !isAbsolute(inputPath)) throw new Error("--input must be an absolute path to a JSON fixture.");
  const input = resolve(inputPath);
  const metadata = statSync(input);
  if (!metadata.isFile()) throw new Error("--input must point to a regular file.");
  if (metadata.size > MAX_INPUT_BYTES) throw new Error(`Fixture exceeds the ${MAX_INPUT_BYTES} byte safety limit.`);

  let parsed;
  try {
    parsed = JSON.parse(readFileSync(input, "utf8"));
  } catch {
    throw new Error("Fixture must contain valid JSON.");
  }
  const report = buildStructureOnlyReport(parsed);
  if (!outputPath) return `${JSON.stringify(report, null, 2)}\n`;
  if (!isAbsolute(outputPath)) throw new Error("--output must be an absolute path.");
  const output = resolve(outputPath);
  if (output === input) throw new Error("--output must not overwrite the source fixture.");
  return writeStructureOnlyReport(report, output, metadata);
}
