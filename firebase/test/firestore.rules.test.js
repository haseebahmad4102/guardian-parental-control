"use strict";

// Firestore security-rules tests for the Guardian schema.
// Run from firebase/test:  npm install && npm test
// (firebase.json points the emulator at ../firestore.rules)

const fs = require("fs");
const path = require("path");
const assert = require("assert");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");

let testEnv;

const PARENT = "parent-uid";
const OTHER_PARENT = "other-parent-uid";
const CHILD = "child-anon-uid";
const DEVICE = "device-1";
const CODE = "123456";

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "guardian-test",
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, "../firestore.rules"), "utf8"),
      host: "127.0.0.1",
      port: 8080,
    },
  });
});

after(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
  // Seed a device owned by PARENT and a pending pairing doc, bypassing rules.
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc(`devices/${DEVICE}`).set({
      deviceId: DEVICE, parentUid: PARENT, childUid: "", childName: "Amir",
      paired: false, pairedAt: 0, ownerMode: "STANDARD",
    });
    await db.doc(`pairing/${CODE}`).set({
      code: CODE, parentUid: PARENT, deviceId: DEVICE, childName: "Amir",
      status: "PENDING", childUid: "", createdAt: Date.now(), expiresAt: Date.now() + 600000,
    });
  });
});

function parentDb() { return testEnv.authenticatedContext(PARENT).firestore(); }
function otherParentDb() { return testEnv.authenticatedContext(OTHER_PARENT).firestore(); }
function childDb() { return testEnv.authenticatedContext(CHILD).firestore(); }

describe("users", () => {
  it("a user can write only their own profile", async () => {
    await assertSucceeds(parentDb().doc(`users/${PARENT}`).set({ email: "p@x.com" }));
    await assertFails(parentDb().doc(`users/${OTHER_PARENT}`).set({ email: "hack" }));
  });
});

describe("devices", () => {
  it("owner reads their device; others cannot", async () => {
    await assertSucceeds(parentDb().doc(`devices/${DEVICE}`).get());
    await assertFails(otherParentDb().doc(`devices/${DEVICE}`).get());
  });

  it("a parent can only create a device naming themselves as owner", async () => {
    await assertSucceeds(
      parentDb().doc("devices/new").set({ parentUid: PARENT, paired: false, childUid: "" })
    );
    await assertFails(
      parentDb().doc("devices/bad").set({ parentUid: OTHER_PARENT, paired: false, childUid: "" })
    );
  });
});

describe("pairing claim", () => {
  it("a child can claim an unclaimed device and pending code", async () => {
    await assertSucceeds(
      childDb().doc(`devices/${DEVICE}`).update({ childUid: CHILD, paired: true, pairedAt: Date.now() })
    );
    await assertSucceeds(
      childDb().doc(`pairing/${CODE}`).update({ status: "CLAIMED", childUid: CHILD })
    );
  });

  it("a child cannot claim a device for a different uid", async () => {
    await assertFails(
      childDb().doc(`devices/${DEVICE}`).update({ childUid: "someone-else", paired: true })
    );
  });

  it("a child cannot re-point the device to another parent", async () => {
    await assertFails(
      childDb().doc(`devices/${DEVICE}`).update({ childUid: CHILD, paired: true, parentUid: CHILD })
    );
  });
});

describe("device sub-collections", () => {
  it("the bound child can write usage/notifications; a stranger cannot", async () => {
    // Bind the child first.
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await ctx.firestore().doc(`devices/${DEVICE}`).update({ childUid: CHILD, paired: true });
    });
    await assertSucceeds(
      childDb().collection(`devices/${DEVICE}/notifications`).add({ pkg: "com.x", ts: Date.now() })
    );
    await assertFails(
      otherParentDb().collection(`devices/${DEVICE}/notifications`).add({ pkg: "com.x", ts: 1 })
    );
  });

  it("the owning parent can read the notification feed", async () => {
    await assertSucceeds(parentDb().collection(`devices/${DEVICE}/notifications`).get());
  });
});
