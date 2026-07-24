const PREDICTED_VERDICTS = new Set(["match", "probable", "no_match", "unknown"]);
const EXPECTED_LABELS = new Set(["glamping", "not_glamping"]);

export const QUALITY_THRESHOLDS = Object.freeze({
  minimumCandidates: 100,
  minimumDestinationGroups: 3,
  matchPrecision: 0.90,
  acceptedPrecision: 0.80,
  recall: 0.70,
  falsePositiveRate: 0.05,
});

export function evaluate(records) {
  const validationErrors = validateRecords(records);
  if (validationErrors.length > 0) {
    return {
      schemaVersion: "1",
      status: "invalid_dataset",
      validationErrors,
    };
  }

  const positive = records.filter((record) => record.expectedLabel === "glamping");
  const negative = records.filter((record) => record.expectedLabel === "not_glamping");
  const matches = records.filter((record) => record.predictedVerdict === "match");
  const accepted = records.filter((record) =>
    record.predictedVerdict === "match" || record.predictedVerdict === "probable");
  const trueMatches = matches.filter((record) => record.expectedLabel === "glamping");
  const trueAccepted = accepted.filter((record) => record.expectedLabel === "glamping");
  const falseAccepted = accepted.filter((record) => record.expectedLabel === "not_glamping");
  const destinationGroups = new Set(records.map((record) => record.destinationGroup));
  const metrics = {
    matchPrecision: ratio(trueMatches.length, matches.length),
    acceptedPrecision: ratio(trueAccepted.length, accepted.length),
    recall: ratio(trueAccepted.length, positive.length),
    falsePositiveRate: ratio(falseAccepted.length, negative.length),
  };
  const gates = {
    candidateCount: records.length >= QUALITY_THRESHOLDS.minimumCandidates,
    destinationCoverage:
      destinationGroups.size >= QUALITY_THRESHOLDS.minimumDestinationGroups,
    borderlineDoubleReview: records
      .filter((record) => record.borderline)
      .every((record) => record.reviewerLabels.length >= 2),
    matchPrecision:
      metrics.matchPrecision !== null &&
      metrics.matchPrecision >= QUALITY_THRESHOLDS.matchPrecision,
    acceptedPrecision:
      metrics.acceptedPrecision !== null &&
      metrics.acceptedPrecision >= QUALITY_THRESHOLDS.acceptedPrecision,
    recall: metrics.recall !== null && metrics.recall >= QUALITY_THRESHOLDS.recall,
    falsePositiveRate:
      metrics.falsePositiveRate !== null &&
      metrics.falsePositiveRate <= QUALITY_THRESHOLDS.falsePositiveRate,
  };

  return {
    schemaVersion: "1",
    status: Object.values(gates).every(Boolean) ? "passed" : "failed",
    counts: {
      candidates: records.length,
      positive: positive.length,
      negative: negative.length,
      match: matches.length,
      accepted: accepted.length,
      destinationGroups: destinationGroups.size,
      borderline: records.filter((record) => record.borderline).length,
    },
    metrics,
    thresholds: QUALITY_THRESHOLDS,
    gates,
  };
}

export function parseJsonLines(content) {
  return content
    .split(/\r?\n/u)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      try {
        return JSON.parse(line);
      } catch {
        throw new Error(`Line ${index + 1} is not valid JSON.`);
      }
    });
}

function validateRecords(records) {
  if (!Array.isArray(records)) {
    return ["Dataset must be an array of JSON Lines records."];
  }

  const errors = [];
  const candidateIds = new Set();
  records.forEach((record, index) => {
    const prefix = `record[${index}]`;
    if (!record || typeof record !== "object" || Array.isArray(record)) {
      errors.push(`${prefix} must be an object.`);
      return;
    }
    if (typeof record.candidateId !== "string" || !record.candidateId.trim()) {
      errors.push(`${prefix}.candidateId must be a non-empty opaque string.`);
    } else if (candidateIds.has(record.candidateId)) {
      errors.push(`${prefix}.candidateId must be unique.`);
    } else {
      candidateIds.add(record.candidateId);
    }
    if (!EXPECTED_LABELS.has(record.expectedLabel)) {
      errors.push(`${prefix}.expectedLabel is invalid.`);
    }
    if (!PREDICTED_VERDICTS.has(record.predictedVerdict)) {
      errors.push(`${prefix}.predictedVerdict is invalid.`);
    }
    if (
      typeof record.destinationGroup !== "string" ||
      !/^destination-group-[a-z0-9_-]+$/u.test(record.destinationGroup)
    ) {
      errors.push(`${prefix}.destinationGroup must be an opaque destination-group-* value.`);
    }
    if (typeof record.borderline !== "boolean") {
      errors.push(`${prefix}.borderline must be boolean.`);
    }
    if (!Array.isArray(record.reviewerLabels)) {
      errors.push(`${prefix}.reviewerLabels must be an array.`);
    } else {
      if (record.borderline && record.reviewerLabels.length < 2) {
        errors.push(`${prefix}.reviewerLabels requires two labels for borderline records.`);
      }
      if (record.reviewerLabels.some((label) => !EXPECTED_LABELS.has(label))) {
        errors.push(`${prefix}.reviewerLabels contains an invalid label.`);
      }
    }
    const allowedFields = new Set([
      "candidateId",
      "expectedLabel",
      "predictedVerdict",
      "destinationGroup",
      "borderline",
      "reviewerLabels",
    ]);
    Object.keys(record)
      .filter((field) => !allowedFields.has(field))
      .forEach((field) => errors.push(`${prefix}.${field} is not allowed.`));
  });
  return errors;
}

function ratio(numerator, denominator) {
  return denominator === 0 ? null : Number((numerator / denominator).toFixed(6));
}
