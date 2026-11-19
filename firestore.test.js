const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const STAFF_A = "staff_alice";
const STAFF_B = "staff_bob";
const ADMIN_UID = "admin_owner";
const BOOTSTRAP_ADMIN_UID = "bootstrap_owner";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("items").where("uid", "==", STAFF_A).get());
  await assertFails(unauth.collection("entries").where("uid", "==", STAFF_A).get());
  await assertFails(
    unauth.collection("items").doc("item_1").set({
      uid: STAFF_A,
      cat: "coffee",
      name: "Espresso",
      price: 12.0,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Staff can create, read, update photo, and delete their own items; unfiltered queries fail", async () => {
  const aliceDb = testEnv.authenticatedContext(STAFF_A).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("items").doc("item_1").set({
      uid: STAFF_A,
      cat: "coffee",
      name: "Spanish Latte",
      price: 18.5,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("items").where("uid", "==", STAFF_A).get()
  );

  // Unfiltered list query by non-admin must fail
  await assertFails(aliceDb.collection("items").get());

  // Photo update succeeds while uid and createdAt remain unchanged
  await assertSucceeds(
    aliceDb.collection("items").doc("item_1").update({
      photo: "base64sample",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Full item update (category, name, price, updatedAt) succeeds
  await assertSucceeds(
    aliceDb.collection("items").doc("item_1").update({
      cat: "cake",
      name: "Chocolate Cake",
      price: 22.0,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Ghost field / immutable createdAt rejected
  await assertFails(
    aliceDb.collection("items").doc("item_1").update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(
    aliceDb.collection("items").doc("item_2").set({
      id: "item_2",
      uid: STAFF_A,
      cat: "coffee",
      name: "Bad ID",
      price: 10,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("items").doc("item_1").delete());
});

test("Cross-user isolation: Staff B cannot read or delete Staff A records", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("entries").doc("entry_a").set({
      uid: STAFF_A,
      item: "Pasta (Chicken)",
      base: "Pasta",
      cat: "food",
      price: 25.0,
      qty: 2,
      total: 50.0,
      person: "Tariq",
      ts: Date.now() - 1000,
      createdAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(STAFF_B).firestore();
  await assertFails(bobDb.collection("entries").doc("entry_a").get());
  await assertFails(bobDb.collection("entries").where("uid", "==", STAFF_A).get());
  await assertFails(bobDb.collection("entries").doc("entry_a").delete());
});

test("Entries, people, notes, dlItems, and dlEntries enforce schema and uid", async () => {
  const aliceDb = testEnv.authenticatedContext(STAFF_A).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("people").doc("p1").set({
      uid: STAFF_A,
      name: "Omar",
      createdAt: now,
    })
  );

  // Updating person name succeeds while extra keys fail
  await assertSucceeds(
    aliceDb.collection("people").doc("p1").update({
      name: "Omar Hassan",
    })
  );
  await assertFails(
    aliceDb.collection("people").doc("p1").update({
      name: "Omar Hassan",
      extraField: "disallowed",
    })
  );

  await assertSucceeds(
    aliceDb.collection("notes").doc("n1").set({
      uid: STAFF_A,
      scope: "free",
      itemId: "item_1",
      text: "Extra cheese",
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("entries").doc("e1").set({
      uid: STAFF_A,
      item: "Pasta (Extra cheese)",
      base: "Pasta",
      cat: "food",
      price: 25.0,
      qty: 1,
      total: 25.0,
      person: "Omar",
      ts: Date.now() - 1000,
      note: "Counter 1",
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("dlItems").doc("d1").set({
      uid: STAFF_A,
      name: "Croissant Box",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("dlEntries").doc("de1").set({
      uid: STAFF_A,
      item: "Croissant Box",
      base: "Croissant Box",
      qty: 4,
      ts: Date.now() - 1000,
      createdAt: now,
    })
  );

  // Invalid category rejected
  await assertFails(
    aliceDb.collection("items").doc("bad_cat").set({
      uid: STAFF_A,
      cat: "invalid_cat",
      name: "Test",
      price: 10,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Admin via admins/{uid} or verified bootstrap email can read all staff collections; users cannot self-promote to admin", async () => {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({ role: "admin" });
    await db.collection("entries").doc("e_staff").set({
      uid: STAFF_A,
      item: "Latte",
      base: "Latte",
      cat: "coffee",
      price: 15.0,
      qty: 1,
      total: 15.0,
      person: "Ali",
      ts: Date.now() - 1000,
      createdAt: now,
    });
    await db.collection("dlEntries").doc("de_staff").set({
      uid: STAFF_A,
      item: "Cake",
      base: "Cake",
      qty: 3,
      ts: Date.now() - 1000,
      createdAt: now,
    });
  });

  const staffDb = testEnv.authenticatedContext(STAFF_A).firestore();
  await assertFails(staffDb.collection("admins").doc(STAFF_A).set({ role: "admin" }));
  await assertFails(staffDb.collection("admins").get());

  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(adminDb.collection("admins").doc(ADMIN_UID).get());
  await assertSucceeds(adminDb.collection("entries").get());
  await assertSucceeds(adminDb.collection("dlEntries").get());

  // Unverified spoofed admin email must fail
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", { email: "sojibahmed186@gmail.com", email_verified: false })
    .firestore();
  await assertFails(spoofDb.collection("entries").get());

  // Verified bootstrap owner email succeeds
  const ownerDb = testEnv
    .authenticatedContext(BOOTSTRAP_ADMIN_UID, { email: "sojibahmed186@gmail.com", email_verified: true })
    .firestore();
  await assertSucceeds(ownerDb.collection("entries").get());
});
