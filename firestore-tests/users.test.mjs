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

console.log("nutrition");
const entry = (id, overrides = {}) => ({ id, day: "2026-11-23", atMs: 1_700_000_000_000, name: "Skyr", kcal: 158, proteinG: 27.5,
  carbsG: 10, fatG: 0.5, grams: 250, foodId: "f1", updatedAt: serverTimestamp(), ...overrides });
await check("owner logs food", assertSucceeds(setDoc(doc(alice, `users/${ALICE}/foodLog/e1`), entry("e1"))));
await check("a quick entry needs no grams or food", assertSucceeds(setDoc(doc(alice, `users/${ALICE}/foodLog/e2`), entry("e2", { grams: null, foodId: null, carbsG: null, fatG: null }))));
await check("day must be a date", assertFails(setDoc(doc(alice, `users/${ALICE}/foodLog/e3`), entry("e3", { day: "monday" }))));
await check("calories must be plausible", assertFails(setDoc(doc(alice, `users/${ALICE}/foodLog/e4`), entry("e4", { kcal: -5 }))));
await check("someone else cannot read the food log", assertFails(getDoc(doc(bob, `users/${ALICE}/foodLog/e1`))));
const food = (id, overrides = {}) => ({ id, name: "Skyr", brand: "Arla", barcode: "5701234567890", kcalPer100: 63, proteinPer100: 11,
  carbsPer100: 4, fatPer100: 0.2, servingG: 150, lastUsedMs: 1_700_000_000_000, updatedAt: serverTimestamp(), ...overrides });
await check("owner saves a food", assertSucceeds(setDoc(doc(alice, `users/${ALICE}/foods/f1`), food("f1"))));
await check("per 100 g values must be plausible", assertFails(setDoc(doc(alice, `users/${ALICE}/foods/f2`), food("f2", { proteinPer100: 150 }))));
await check("unknown food fields are refused", assertFails(setDoc(doc(alice, `users/${ALICE}/foods/f3`), food("f3", { price: 2 }))));
await check("someone else cannot write foods", assertFails(setDoc(doc(bob, `users/${ALICE}/foods/f4`), food("f4"))));

console.log("own exercises and photos");
const custom = (id, overrides = {}) => ({
  id, nameDe: "Landmine Press", nameEn: "Landmine Press", primary: "SHOULDERS", secondary: ["TRICEPS"], equipment: "BARBELL",
  pattern: "VERTICAL_PUSH", isCustom: true, level: "BEGINNER", instructionsEn: [], instructionsDe: [], images: 0,
  note: "", archived: false, updatedAt: serverTimestamp(), ...overrides,
});
await check("owner saves an own exercise (all model fields)", assertSucceeds(setDoc(doc(alice, `users/${ALICE}/customExercises/c1`), custom("c1"))));
await check("own exercise must be marked custom", assertFails(setDoc(doc(alice, `users/${ALICE}/customExercises/c2`), custom("c2", { isCustom: false }))));
await check("note has a length limit", assertFails(setDoc(doc(alice, `users/${ALICE}/customExercises/c3`), custom("c3", { note: "x".repeat(501) }))));
await check("at most three secondary muscles", assertFails(setDoc(doc(alice, `users/${ALICE}/customExercises/c4`), custom("c4", { secondary: ["A", "B", "C", "D"] }))));
await check("no hosted images for own exercises", assertFails(setDoc(doc(alice, `users/${ALICE}/customExercises/c5`), custom("c5", { images: 2 }))));
await check("archiving is allowed", assertSucceeds(setDoc(doc(alice, `users/${ALICE}/customExercises/c1`), custom("c1", { archived: true }))));
await check("someone else cannot read my exercises", assertFails(getDoc(doc(bob, `users/${ALICE}/customExercises/c1`))));
const photo = (overrides = {}) => ({ webp: "UklGRg==", updatedAt: serverTimestamp(), ...overrides });
await check("owner saves a photo for an own exercise", assertSucceeds(setDoc(doc(alice, `users/${ALICE}/exercisePhotos/c1`), photo())));
await check("no photo without the exercise", assertFails(setDoc(doc(alice, `users/${ALICE}/exercisePhotos/nope`), photo())));
await check("photo has a size limit", assertFails(setDoc(doc(alice, `users/${ALICE}/exercisePhotos/c1`), photo({ webp: "x".repeat(200001) }))));
await check("photo takes no other fields", assertFails(setDoc(doc(alice, `users/${ALICE}/exercisePhotos/c1`), photo({ caption: "me" }))));
await check("someone else cannot see my photo", assertFails(getDoc(doc(bob, `users/${ALICE}/exercisePhotos/c1`))));
await check("someone else cannot write my photo", assertFails(setDoc(doc(bob, `users/${ALICE}/exercisePhotos/c1`), photo())));
await check("owner deletes the photo", assertSucceeds(deleteDoc(doc(alice, `users/${ALICE}/exercisePhotos/c1`))));

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
