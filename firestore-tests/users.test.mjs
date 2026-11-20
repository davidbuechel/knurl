//
//  users.test.mjs — ownership and shapes for everything under users/{uid}.
//

import { assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { deleteDoc, doc, getDoc, serverTimestamp, setDoc } from "firebase/firestore";
import { ALICE, BOB, check, makeEnv, report, routine, workout } from "./harness.mjs";

const env = await makeEnv();
await env.clearFirestore();

const alice = env.authenticatedContext(ALICE).firestore();
const bob = env.authenticatedContext(BOB).firestore();
const nobody = env.unauthenticatedContext().firestore();

console.log("profile");
await check("owner creates profile", assertSucceeds(setDoc(doc(alice, `users/${ALICE}`), {
  settings: { barKg: 20.0, defaultRestSec: 120, weeklyGoal: 3 }, schemaVersion: 1, createdAt: serverTimestamp(),
})));
await check("owner reads profile", assertSucceeds(getDoc(doc(alice, `users/${ALICE}`))));
await check("someone else cannot read it", assertFails(getDoc(doc(bob, `users/${ALICE}`))));
await check("signed-out cannot read it", assertFails(getDoc(doc(nobody, `users/${ALICE}`))));
await check("someone else cannot write it", assertFails(setDoc(doc(bob, `users/${ALICE}`), { settings: {} })));
await check("unknown profile fields are refused", assertFails(setDoc(doc(alice, `users/${ALICE}`), { settings: {}, isAdmin: true })));

console.log("workouts");
await check("owner saves a workout", assertSucceeds(setDoc(doc(alice, `users/${ALICE}/workouts/w1`), workout("w1"))));
await check("id must match the document", assertFails(setDoc(doc(alice, `users/${ALICE}/workouts/w2`), workout("other"))));
await check("name may not be empty", assertFails(setDoc(doc(alice, `users/${ALICE}/workouts/w3`), workout("w3", { name: "" }))));
await check("updatedAt must be the server time", assertFails(setDoc(doc(alice, `users/${ALICE}/workouts/w4`), workout("w4", { updatedAt: 5 }))));
await check("extra fields are refused", assertFails(setDoc(doc(alice, `users/${ALICE}/workouts/w5`), workout("w5", { shared: true }))));
await check("someone else cannot read a workout", assertFails(getDoc(doc(bob, `users/${ALICE}/workouts/w1`))));
await check("someone else cannot write into my workouts", assertFails(setDoc(doc(bob, `users/${ALICE}/workouts/w9`), workout("w9"))));
await check("owner deletes a workout", assertSucceeds(deleteDoc(doc(alice, `users/${ALICE}/workouts/w1`))));

console.log("running workout");
await check("owner saves the running workout", assertSucceeds(setDoc(doc(alice, `users/${ALICE}/state/active`), workout("live", { endedAtMs: null }))));
await check("no other state documents", assertFails(setDoc(doc(alice, `users/${ALICE}/state/other`), workout("live"))));
await check("someone else cannot see it", assertFails(getDoc(doc(bob, `users/${ALICE}/state/active`))));
await check("owner clears it", assertSucceeds(deleteDoc(doc(alice, `users/${ALICE}/state/active`))));

console.log("routines");
await check("owner saves a routine", assertSucceeds(setDoc(doc(alice, `users/${ALICE}/routines/r1`), routine("r1"))));
await check("position must be a number", assertFails(setDoc(doc(alice, `users/${ALICE}/routines/r2`), routine("r2", { position: "first" }))));
await check("someone else cannot read routines", assertFails(getDoc(doc(bob, `users/${ALICE}/routines/r1`))));

console.log("notes and body weight");
await check("a workout may carry a note", assertSucceeds(setDoc(doc(alice, `users/${ALICE}/workouts/n1`), workout("n1", { note: "Felt strong" }))));
await check("notes have a length limit", assertFails(setDoc(doc(alice, `users/${ALICE}/workouts/n2`), workout("n2", { note: "x".repeat(2001) }))));
const weight = (id, overrides = {}) => ({ id, atMs: 1_700_000_000_000, weightKg: 82.5, updatedAt: serverTimestamp(), ...overrides });
await check("owner logs body weight", assertSucceeds(setDoc(doc(alice, `users/${ALICE}/bodyWeights/b1`), weight("b1"))));
await check("body weight must be plausible", assertFails(setDoc(doc(alice, `users/${ALICE}/bodyWeights/b2`), weight("b2", { weightKg: -1 }))));
await check("someone else cannot read body weight", assertFails(getDoc(doc(bob, `users/${ALICE}/bodyWeights/b1`))));

console.log("devices");
const device = (overrides = {}) => ({ token: "fcm-token", platform: "android", locale: "de", updatedAt: serverTimestamp(), ...overrides });
await check("owner registers a device", assertSucceeds(setDoc(doc(alice, `users/${ALICE}/devices/install1`), device())));
await check("unknown platform is refused", assertFails(setDoc(doc(alice, `users/${ALICE}/devices/install2`), device({ platform: "web" }))));
await check("someone else cannot read my tokens", assertFails(getDoc(doc(bob, `users/${ALICE}/devices/install1`))));

console.log("everything else");
await check("no top-level collections", assertFails(setDoc(doc(alice, "public/x"), { a: 1 })));
await check("no unknown subcollections", assertFails(setDoc(doc(alice, `users/${ALICE}/secrets/x`), { a: 1 })));

await env.cleanup();
report();
