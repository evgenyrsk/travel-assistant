import fs from "node:fs";
import path from "node:path";
import { DEFAULT_SUBSET_MANIFEST } from "./paths.js";

export interface SubsetManifestState {
  path: string;
  exists: boolean;
  status: "missing_not_created" | "present_not_evaluated";
  requiredForSkeleton: false;
}

export function inspectSubsetManifest(
  repositoryRoot: string,
  manifestPath = DEFAULT_SUBSET_MANIFEST,
): SubsetManifestState {
  const exists = fs.existsSync(path.resolve(repositoryRoot, manifestPath));

  return {
    path: manifestPath,
    exists,
    status: exists ? "present_not_evaluated" : "missing_not_created",
    requiredForSkeleton: false,
  };
}
