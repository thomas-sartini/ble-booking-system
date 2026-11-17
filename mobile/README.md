This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/sharedLogic](./sharedLogic/src) is for the code that will be shared between app targets in the project.
  The most important subfolder is [commonMain](./sharedLogic/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/sharedUI](./sharedUI/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./sharedUI/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./sharedUI/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./sharedUI/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :sharedUI:testAndroidHostTest :sharedLogic:testAndroidHostTest`
- iOS tests: `./gradlew :sharedLogic:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
### Simulated BLE runtime tests (Android JVM)

From the `mobile` directory on Windows:

```powershell
.\gradlew.bat :androidApp:testDebugUnitTest --tests "ch.hslu.blebooking.mobile.ble.*"
```

Alternatively, run `TerminalManagerRuntimeTest` and `BleMessageCodecTest` using Android Studio's test gutter.

The runtime tests execute the real `TerminalManager`, `BleGattClient` and session with a simulated `BleGattTransport`. The simulator controls asynchronous subscription acknowledgements, read/write callbacks, status notifications and disconnection. Coroutine virtual time checks the ten-second timeout without waiting ten real seconds.

Covered scenarios include OPEN/CLOSE, notification ordering, unrelated/pending results, malformed messages, GATT failures, concurrent managers, cancellation, timeout and connection loss. The simulator exists only in test sources; the app continues to use `AndroidBleGattTransport`.

These are JVM integration tests of the session logic, not radio or UI tests. They do not exercise scanning, Android permissions, connection establishment/service discovery, native callback forwarding, or the Android transport implementation. Those still require Android/device integration testing with a BLE peripheral.
