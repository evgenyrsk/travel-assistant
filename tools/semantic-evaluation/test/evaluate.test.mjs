import test from "node:test";
import assert from "node:assert/strict";
import { evaluate, parseJsonLines } from "../src/evaluate.mjs";

test("passes a rights-safe aggregate dataset that meets every quality gate", () => {
  const records = Array.from({ length: 100 }, (_, index) => {
    const expectedLabel = index < 60 ? "glamping" : "not_glamping";
    let predictedVerdict;
    if (index < 45) predictedVerdict = "match";
    else if (index < 55) predictedVerdict = "probable";
    else if (index < 60) predictedVerdict = "unknown";
    else if (index < 62) predictedVerdict = "probable";
    else predictedVerdict = "no_match";
    return record(index, expectedLabel, predictedVerdict);
  });

  const report = evaluate(records);

  assert.equal(report.status, "passed");
  assert.equal(report.metrics.matchPrecision, 1);
  assert.ok(report.metrics.acceptedPrecision >= 0.8);
  assert.ok(report.metrics.recall >= 0.7);
  assert.ok(report.metrics.falsePositiveRate <= 0.05);
});

test("fails incomplete coverage and rejects raw provider fields", () => {
  const incomplete = evaluate([
    {
      ...record(1, "glamping", "match"),
      hotelName: "must not be stored in the aggregate dataset",
    },
  ]);

  assert.equal(incomplete.status, "invalid_dataset");
  assert.match(incomplete.validationErrors.join(" "), /hotelName is not allowed/);
});

test("requires two independent labels for borderline records", () => {
  const borderline = record(1, "glamping", "probable");
  borderline.borderline = true;
  borderline.reviewerLabels = ["glamping"];

  const report = evaluate([borderline]);

  assert.equal(report.status, "invalid_dataset");
  assert.match(report.validationErrors.join(" "), /requires two labels/);
});

test("parses JSON Lines and reports the failing line", () => {
  assert.equal(parseJsonLines(`${JSON.stringify(record(1, "glamping", "match"))}\n`).length, 1);
  assert.throws(() => parseJsonLines("{}\nnot-json"), /Line 2/);
});

function record(index, expectedLabel, predictedVerdict) {
  return {
    candidateId: `candidate-${index}`,
    expectedLabel,
    predictedVerdict,
    destinationGroup: `destination-group-${index % 3}`,
    borderline: index % 10 === 0,
    reviewerLabels: index % 10 === 0
      ? [expectedLabel, expectedLabel]
      : [expectedLabel],
  };
}
