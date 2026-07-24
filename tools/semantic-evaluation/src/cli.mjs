import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { evaluate, parseJsonLines } from "./evaluate.mjs";

const datasetPath = process.argv[2];
if (!datasetPath) {
  console.error("Usage: npm run evaluate -- /approved/path/evaluation.jsonl");
  process.exitCode = 2;
} else {
  try {
    const content = await readFile(resolve(datasetPath), "utf8");
    const report = evaluate(parseJsonLines(content));
    console.log(JSON.stringify(report, null, 2));
    process.exitCode = report.status === "passed" ? 0 : 1;
  } catch (error) {
    console.error(error instanceof Error ? error.message : "Evaluation failed.");
    process.exitCode = 2;
  }
}
