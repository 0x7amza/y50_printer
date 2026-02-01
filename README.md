# y50_printer

A Flutter plugin for the Y50 Bluetooth Thermal Printer. This plugin provides a simple interface to connect to the printer, configure print settings, and print labels (including images) via Bluetooth.

## Features

- **Bluetooth Connectivity**: Scan for bonded devices and connect/disconnect easily.
- **Status Monitoring**: Real-time streams for connection status and logs.
- **Print Configuration**: Customizable print parameters (paper size, density, speed, gap/continuous, etc.).
- **Image Printing**: Print labels from standard image data (PNG via `Uint8List`).
- **Test Page**: built-in command to print a test page.

## Getting Started

### 1. Add Dependency

Add `y50_printer` to your `pubspec.yaml`:

```yaml
dependencies:
  y50_printer:
    path: ../ # Or your git/pub path
```

### 2. Android Setup

Add the following permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

Note: For Android 12+ (API 31+), ensure you handle runtime permissions for Bluetooth Scan and Connect.

## Usage

### Initialization & Permissions

Before using the printer, ensure you have requested the necessary permissions and initialized the SDK.

```dart
import 'package:y50_printer/y50_printer.dart';
import 'package:permission_handler/permission_handler.dart';

// ... inside your init function
await [
  Permission.bluetooth,
  Permission.bluetoothScan,
  Permission.bluetoothConnect,
  Permission.location,
].request();

await Y50Printer.initSDK();
```

### connecting to a Printer

You can get a list of bonded (paired) devices and connect to one.

```dart
// Get paired devices
List<BluetoothDevice> devices = await Y50Printer.getBondedDevices();

// Connect to a specific device
await Y50Printer.connectToDevice(devices.first);
```

### Monitoring Connection

Listen to connection state changes:

```dart
Y50Printer.onConnectionStateChanged.listen((state) {
  print("Connection State: $state"); // CONNECTED, DISCONNECTED, FAILED: ...
});
```

### Printing a Label

To print a label, you first configure the specific parameters for your label roll (e.g., 50x30mm) and then send the image data.

```dart
// 1. Configure Printer Settings (e.g., for 50x30mm Gap Label)
await Y50Printer.setPrintParameters(
  paperW: 50,       // Paper width in mm
  paperH: 30,       // Paper height in mm
  imgW: 50,         // Image width to print
  imgH: 30,         // Image height to print
  dpi: 8,           // Print resolution (usually 8)
  run: 1,           // Number of copies
  paperType: PaperType.GAP, // GAP or CONTINUOUS
);

// 2. Print Image (Uint8List)
// You can capture this from a RepaintBoundary or load from assets/file
await Y50Printer.printLabel(imageBytes: myPngBytes);
```

### API Reference

| Method | Description |
|Args|
| --- | --- |
| `initSDK()` | Initializes the native printer SDK. Required before other operations. |
| `getBondedDevices()` | Returns a list of paired Bluetooth devices. |
| `connectToDevice(device)` | Connects to the specified `BluetoothDevice`. |
| `disconnect()` | Disconnects current session. |
| `setPrintParameters(...)` | Sets paper dimensions, density, speed, etc. |
| `printLabel({imageBytes})` | Prints the provided image data as a label. |
| `testPage()` | Prints a built-in test page. |
| `onConnectionStateChanged` | Stream<String> of connection events. |
| `onLog` | Stream<String> of debug logs from native SDK. |

## Troubleshooting

- **Connection Failed**: Ensure the printer is paired in Android Bluetooth settings first.
- **Blank Prints**: Check `paperW` and `paperH` match your physical label size. Ensure `imageBytes` is valid PNG data.
- **Permissions**: On Android 12+, Scan/Connect permissions are critical. Ensure Location is also granted for discovery to work on some versions.
