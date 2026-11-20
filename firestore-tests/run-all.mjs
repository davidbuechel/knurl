//
//  run-all.mjs — runs every *.test.mjs in this folder, one after another,
//  so a new test file is picked up without touching package.json.
//

import { readdirSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const files = readdirSync(here).filter((f) => f.endsWith(".test.mjs")).sort();
let failed = 0;

for (const file of files) {
  console.log(`\n▶ ${file}`);
  const result = spawnSync(process.execPath, [join(here, file)], { stdio: "inherit" });
  if (result.status !== 0) failed++;
}

console.log(`\n${files.length - failed}/${files.length} test files passed`);
process.exit(failed === 0 ? 0 : 1);
