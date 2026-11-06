const { onCall, HttpsError } = require("firebase-functions/v2/https");

const { initializeApp } = require("firebase-admin/app");
const {
  getFirestore,
  FieldValue,
  Timestamp
} = require("firebase-admin/firestore");

const crypto = require("crypto");

initializeApp();

const db = getFirestore();

// ===================================
// 1. Create Pairing Code (Shawon/Admin)
// ===================================

exports.createPairCode = onCall(async (request) => {

  if (!request.auth) {
    throw new HttpsError(
      "unauthenticated",
      "Please sign in first."
    );
  }

  const code = String(
    crypto.randomInt(100000, 1000000)
  );

  const codeHash = crypto
    .createHash("sha256")
    .update(code)
    .digest("hex");

  const expiresAt = Timestamp.fromMillis(
    Date.now() + 10 * 60 * 1000
  );

  await db.collection("pairCodes").doc(codeHash).set({
    adminUid: request.auth.uid,
    expiresAt: expiresAt,
    used: false,
    createdAt: FieldValue.serverTimestamp()
  });

  return {
    code: code,
    expiresInSeconds: 600
  };
});


// ===================================
// 2. Redeem Pairing Code (Fatema/Member)
// ===================================

exports.redeemPairCode = onCall(async (request) => {

  if (!request.auth) {
    throw new HttpsError(
      "unauthenticated",
      "Please sign in first."
    );
  }

  const code = String(request.data?.code || "").trim();

  if (!/^\d{6}$/.test(code)) {
    throw new HttpsError(
      "invalid-argument",
      "Enter a valid 6-digit code."
    );
  }

  const hash = crypto
    .createHash("sha256")
    .update(code)
    .digest("hex");

  const ref = db.collection("pairCodes").doc(hash);

  const pairId = crypto.randomUUID();

  await db.runTransaction(async (tx) => {

    const snap = await tx.get(ref);

    if (!snap.exists) {
      throw new HttpsError(
        "not-found",
        "Invalid pairing code."
      );
    }

    const data = snap.data();

    if (
      data.used ||
      !data.expiresAt ||
      data.expiresAt.toMillis() <= Date.now()
    ) {
      throw new HttpsError(
        "deadline-exceeded",
        "Pairing code expired or already used."
      );
    }

    if (data.adminUid === request.auth.uid) {
      throw new HttpsError(
        "failed-precondition",
        "Use the other phone."
      );
    }

    // Consume the code only once
    tx.update(ref, {
      used: true,
      redeemedAt: FieldValue.serverTimestamp()
    });

    // Create the pairing
    tx.set(db.collection("pairs").doc(pairId), {
      adminUid: data.adminUid,
      memberUid: request.auth.uid,
      createdAt: FieldValue.serverTimestamp(),
      active: true
    });

  });

  return {
    success: true,
    pairId: pairId,
    message: "Phone paired successfully."
  };

});