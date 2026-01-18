# Volume Bubble 🫧

<p align="center">
  <img src="assets/demo.gif" width="280" alt="Volume Bubble Demo">
</p>

A minimalist, high-performance floating volume controller for Android. This tool allows users to manage system audio via a draggable overlay, eliminating the need for physical volume buttons.

---

## 🚀 Features
- **Edge Snapping**: The bubble intelligently snaps to the nearest screen edge to stay out of the user's way.
- **Instant Control**: A single tap triggers the system volume slider for quick adjustments.
- **Intuitive Dismiss**: Drag the bubble into the "Trash Zone" at the bottom of the screen to stop the service.
- **Adaptive UI**: Full support for System Light and Dark modes.

## 🛡 Permissions Required
To provide a seamless overlay experience on modern Android versions (API 26 to API 36+), the following permissions are required:

* **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)**: Required to render the floating bubble globally.
* **Foreground Service (`FOREGROUND_SERVICE`)**: Ensures the volume controller remains active and responsive in the background.
* **Special Use Type**: Implements `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` for compliance with Android 14+ security standards.

## 🏗 How to Build
1. **Clone the repository**:
   ```bash
   git clone https://github.com/vijaydhakal/volume_bubble.git