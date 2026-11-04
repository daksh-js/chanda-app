const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_admin_123";
const BOB_UID = "bob_admin_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function validDonorPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    donorCode: "DONOR-0001",
    fullName: "Choudhary Surender Sangwan",
    fatherName: "Choudhary Ramphal Sangwan",
    mobileNumber: "9812345678",
    whatsappNumber: "9812345678",
    address: "Main Chopal Road, Chhara",
    village: "Chhara",
    city: "Jhajjar",
    district: "Jhajjar",
    state: "Haryana",
    pinCode: "124504",
    addedByAdmin: "Vikramaditya Sengupta",
    lifetimeDonation: 5100,
    donationCount: 1,
    isFavorite: true,
    consentGiven: true,
    createdBy: uid,
    updatedBy: uid,
    createdAt: now,
    updatedAt: now,
  };
}

function validDonationPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    donorId: "donor_1",
    donorCode: "DONOR-0001",
    donorName: "Choudhary Surender Sangwan",
    donorMobile: "9812345678",
    amount: 5100,
    amountInWords: "Five Thousand One Hundred Rupees Only",
    donationDate: "2026-11-04",
    purpose: "Kushti Peeth Diet Fund",
    paymentMethod: "CASH",
    receiptNumber: "AKD-2026-0001",
    receivedByAdmin: "Vikramaditya Sengupta",
    status: "CONFIRMED",
    createdBy: uid,
    updatedBy: uid,
    createdAt: now,
    updatedAt: now,
  };
}

test("1. Unauthenticated user: cannot read donors or donations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("donors").get());
  await assertFails(unauthDb.collection("donations").get());
});

test("2. Authenticated user: can create valid donor and CASH donation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("donors").doc("donor_1").set(validDonorPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb
      .collection("donations")
      .doc("donation_1")
      .set(validDonationPayload(ALICE_UID))
  );
});

test("3. CASH-only rule: rejects non-CASH paymentMethod (UPI/BANK_TRANSFER)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badPayload = {
    ...validDonationPayload(ALICE_UID),
    paymentMethod: "UPI",
  };
  await assertFails(
    aliceDb.collection("donations").doc("donation_upi").set(badPayload)
  );
});

test("4. Cross-user isolation: Bob cannot read Alice's donor or donation", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("donors")
      .doc("alice_donor")
      .set(validDonorPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("donors").doc("alice_donor").get());
});

test("5. Query alignment: Alice can query her own donors with userId filter, fails without filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("donors")
      .doc("alice_donor")
      .set(validDonorPayload(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("donors").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("donors").get());
});

test("6. Financial immutability: donations cannot be deleted, only corrected/reversed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("donations")
      .doc("donation_1")
      .set(validDonationPayload(ALICE_UID))
  );
  await assertFails(aliceDb.collection("donations").doc("donation_1").delete());
});

test("7. Shadow field rejection: rejects unexpected ghost fields on donor creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const shadowPayload = {
    ...validDonorPayload(ALICE_UID),
    isVerifiedSuperUser: true,
  };
  await assertFails(
    aliceDb.collection("donors").doc("donor_shadow").set(shadowPayload)
  );
});
