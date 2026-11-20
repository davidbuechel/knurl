//
//  harness.mjs — common ground for every Knurl rules test.
//
//  Talks to the Firestore emulator only (project id "demo-…", so the SDK refuses
//  to reach a real project). The emulator reads the repo's own ../firestore.rules,
//  so a green run speaks for what we deploy.
//

import { initializeTestEnvironment } from "@firebase/rules-unit-testing";
import { serverTimestamp, setLogLevel } from "firebase/firestore";

// Every expected denial would otherwise print a PERMISSION_DENIED stack.
setLogLevel("silent");

export const ALICE = "alice";
export const BOB = "bob";

export async function makeEnv() {
  return initializeTestEnvironment({
    projectId: "demo-knurl-rules",
    firestore: { host: "127.0.0.1", port: 8282 },
  });
}

let pass = 0;
let fail = 0;

/** Awaits one assertSucceeds/assertFails and prints ok or FAIL with the name. */
export async function check(name, promise) {
  try {
    await promise;
    console.log(`  ok   ${name}`);
    pass++;
  } catch (error) {
    console.log(`  FAIL ${name}\n       ${error.message?.split("\n")[0]}`);
    fail++;
  }
}

export function report() {
  console.log(`\n${pass} passed, ${fail} failed`);
  process.exit(fail === 0 ? 0 : 1);
}

/** The documents exactly as the app writes them (data/firebase/FirestoreTrainingStore.kt). */
export function workout(id, overrides = {}) {
  return {
    id,
    name: "Push",
    routineId: "starter_push",
    startedAtMs: 1_700_000_000_000,
    endedAtMs: 1_700_000_360_000,
    exercises: [{ exerciseId: "bench_press", sets: [{ kind: "WORKING", weightKg: 100.0, reps: 5, rir: null, done: true }], repRange: { min: 5, max: 8 }, restSec: 180, note: "" }],
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

export function routine(id, overrides = {}) {
  return {
    id,
    name: "Push",
    items: [{ exerciseId: "bench_press", sets: 3, repRange: { min: 5, max: 8 }, restSec: 180 }],
    position: 0,
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}
