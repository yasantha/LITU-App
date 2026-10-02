// Firestore security rules tests (spec section 16), run with the Firebase Emulator Suite:
//   cd firebase && npm ci && npm test
import { after, before, beforeEach, test } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { addDoc, collection, deleteDoc, doc, getDoc, setDoc } from 'firebase/firestore';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-litu',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => env.cleanup());
beforeEach(async () => env.clearFirestore());

const backup = { schemaVersion: 1, settings: { dailyGoal: 20 }, reviewState: {}, mocks: [], dailyStats: {} };
const report = (uid, extra = {}) => ({
  uid, questionId: 'Q-CH3-TUD-001', reason: 'unclear', comment: 'Two answers look right',
  contentVersion: 1, appVersion: '1.0.0', createdAt: new Date(), ...extra,
});

test('a user can write, read and delete only their own backup', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  await assertSucceeds(setDoc(doc(alice, 'users/alice'), backup));
  await assertSucceeds(getDoc(doc(alice, 'users/alice')));
  await assertFails(getDoc(doc(alice, 'users/bob')));
  await assertFails(setDoc(doc(alice, 'users/bob'), backup));
  await assertSucceeds(deleteDoc(doc(alice, 'users/alice')));
});

test('signed-out users cannot touch backups', async () => {
  const anon = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(anon, 'users/alice')));
  await assertFails(setDoc(doc(anon, 'users/alice'), backup));
});

test('backups are bounded: known fields and capped collections only', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(alice, 'users/alice'), { ...backup, padding: 'x'.repeat(950 * 1024) }));
  const many = (n) => Object.fromEntries(Array.from({ length: n }, (_, i) => [`Q-${i}`, [2.5, 1, 0, 0, 0, 0, 0]]));
  await assertSucceeds(setDoc(doc(alice, 'users/alice'), { ...backup, reviewState: many(3000) }));
  await assertFails(setDoc(doc(alice, 'users/alice'), { ...backup, reviewState: many(3001) }));
  await assertFails(setDoc(doc(alice, 'users/alice'), { ...backup, mocks: Array.from({ length: 31 }, (_, i) => ({ id: `${i}` })) }));
});

test('reports are create-only, own uid, known reason, short comment', async () => {
  const alice = env.authenticatedContext('alice').firestore();
  const ref = await assertSucceeds(addDoc(collection(alice, 'reports'), report('alice')));
  await assertFails(getDoc(ref));
  await assertFails(deleteDoc(ref));
  await assertFails(addDoc(collection(alice, 'reports'), report('bob')));
  await assertFails(addDoc(collection(alice, 'reports'), report('alice', { reason: 'spam' })));
  await assertFails(addDoc(collection(alice, 'reports'), report('alice', { comment: 'x'.repeat(501) })));
  await assertFails(addDoc(collection(env.unauthenticatedContext().firestore(), 'reports'), report('alice')));
});
