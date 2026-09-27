"use strict";

// OPTIONAL background-delivery function for the command channel.
//
// The child app already executes commands in real time via a Firestore listener while its
// monitor service is alive. This function is the wake-up path for when the child app is
// backgrounded or killed: on each new command it sends a high-priority data push to the
// device's FCM token, which starts the monitor service so it can drain the queue.
//
// Deploy is optional: `firebase deploy --only functions`. Without it, commands still work
// whenever the child's service is running.

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.notifyCommand = onDocumentCreated(
  "devices/{deviceId}/commands/{commandId}",
  async (event) => {
    const deviceId = event.params.deviceId;

    const statusSnap = await getFirestore()
      .doc(`devices/${deviceId}/status/current`)
      .get();
    const token = statusSnap.get("fcmToken");
    if (!token) {
      console.log(`No FCM token for device ${deviceId}; skipping wake-up.`);
      return;
    }

    try {
      await getMessaging().send({
        token,
        // Data-only, high priority so Android allows the service start while backgrounded.
        data: { type: "COMMAND", deviceId },
        android: { priority: "high" },
      });
    } catch (err) {
      // A stale token is expected occasionally; the child refreshes it on next launch.
      console.warn(`FCM send failed for device ${deviceId}: ${err.message}`);
    }
  }
);
