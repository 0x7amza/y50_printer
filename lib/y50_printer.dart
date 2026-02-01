import 'package:flutter/services.dart';
import 'dart:async';
import 'package:blue_thermal_printer/blue_thermal_printer.dart';

/// Y50 Printer Plugin Class
class Y50Printer {
  static const MethodChannel _channel = MethodChannel('y50_printer');

  // Stream controller for connection status
  static final StreamController<String> _connectionStatusController =
      StreamController<String>.broadcast();

  // Stream controller for logs
  static final StreamController<String> _logController =
      StreamController<String>.broadcast();

  // Expose streams
  static Stream<String> get onConnectionStateChanged =>
      _connectionStatusController.stream;

  static Stream<String> get onLog => _logController.stream;

  /// Initialize the printer SDK
  static Future<String?> initSDK() async {
    // Set up method call handler
    _channel.setMethodCallHandler(_methodCallHandler);

    try {
      final result = await _channel.invokeMethod('initSDK');
      return result as String?;
    } on PlatformException catch (e) {
      _log("Failed to initialize SDK: ${e.message}");
      return null;
    }
  }

  // Handle callbacks from native code
  static Future<dynamic> _methodCallHandler(MethodCall call) async {
    switch (call.method) {
      case 'onConnected':
        _log("Received onConnected from native");
        _connectionStatusController.add("CONNECTED");
        break;
      case 'onConnectionStatusChanged':
        // Native sends boolean: true = connected, false = disconnected
        final isConnected = call.arguments as bool;
        _log("Received onConnectionStatusChanged: $isConnected");
        _connectionStatusController.add(
          isConnected ? "CONNECTED" : "DISCONNECTED",
        );
        break;
      case 'onConnectFailed':
        final error = call.arguments as String?;
        _log("Received onConnectFailed: $error");
        _connectionStatusController.add("FAILED: $error");
        break;
      case 'onLog':
        final msg = call.arguments as String?;
        if (msg != null) _log(msg);
        break;
      default:
        _log("Unknown method called from native: ${call.method}");
    }
  }

  /// Set the printer device
  static Future<String?> setDevice({
    required String name,
    required String address,
  }) async {
    try {
      final result = await _channel.invokeMethod('setDevice', {
        'name': name,
        'address': address,
      });
      return result as String?;
    } on PlatformException catch (e) {
      _log('Failed to set device: ${e.message}');
      return null;
    }
  }

  /// Connect to the printer
  static Future<String?> connect() async {
    try {
      final result = await _channel.invokeMethod('connect');
      return result as String?;
    } on PlatformException catch (e) {
      _log('Failed to connect: ${e.message}');
      return null;
    }
  }

  /// Connect to a specific device (Convenience method)
  static Future<String?> connectToDevice(BluetoothDevice device) async {
    await setDevice(
      name: device.name ?? "Unknown",
      address: device.address ?? "",
    );
    return await connect();
  }

  /// Disconnect from the printer
  static Future<String?> disconnect() async {
    try {
      final result = await _channel.invokeMethod('disconnect');
      return result as String?;
    } on PlatformException catch (e) {
      _log('Failed to disconnect: ${e.message}');
      return null;
    }
  }

  /// Set print parameters
  static Future<String?> setPrintParameters({
    int? paperW,
    int? paperH,
    int? imgW,
    int? imgH,
    int? dpi,
    int? threshold,
    int? count,
    int? density,
    int? speed,
    int? paperType,
  }) async {
    try {
      final Map<String, dynamic> params = {};
      if (paperW != null) params['paperW'] = paperW;
      if (paperH != null) params['paperH'] = paperH;
      if (imgW != null) params['imgW'] = imgW;
      if (imgH != null) params['imgH'] = imgH;
      if (dpi != null) params['dpi'] = dpi;
      if (threshold != null) params['threshold'] = threshold;
      if (count != null) params['count'] = count;
      if (density != null) params['density'] = density;
      if (speed != null) params['speed'] = speed;
      if (paperType != null) params['paperType'] = paperType;

      final result = await _channel.invokeMethod('setPrintParameters', params);
      return result as String?;
    } on PlatformException catch (e) {
      _log('Failed to set parameters: ${e.message}');
      return null;
    }
  }

  /// Start printing
  static Future<String?> printLabel({Uint8List? imageBytes}) async {
    try {
      final Map<String, dynamic> args = {};
      if (imageBytes != null) {
        args['image'] = imageBytes;
      }
      final result = await _channel.invokeMethod('print', args);
      return result as String?;
    } on PlatformException catch (e) {
      _log('Failed to print: ${e.message}');
      return null;
    }
  }

  /// Stop printing
  static Future<String?> stopPrint() async {
    try {
      final result = await _channel.invokeMethod('stopPrint');
      return result as String?;
    } on PlatformException catch (e) {
      _log('Failed to stop print: ${e.message}');
      return null;
    }
  }

  /// Get printer status
  static Future<String?> getPrinterStatus() async {
    try {
      final result = await _channel.invokeMethod('getPrinterStatus');
      return result as String?;
    } on PlatformException catch (e) {
      _log('Failed to get status: ${e.message}');
      return null;
    }
  }

  /// Get printer info
  static Future<String?> getInfo() async {
    try {
      final result = await _channel.invokeMethod('getInfo');
      return result as String?;
    } on PlatformException catch (e) {
      _log('Failed to get info: ${e.message}');
      return null;
    }
  }

  /// Print test page
  static Future<String?> testPage() async {
    try {
      final result = await _channel.invokeMethod('testPage');
      return result as String?;
    } on PlatformException catch (e) {
      _log('Failed to print test page: ${e.message}');
      return null;
    }
  }

  /// Check if printer is connected
  static Future<bool> isConnected() async {
    try {
      final result = await _channel.invokeMethod('isConnected');
      return result as bool? ?? false;
    } on PlatformException catch (e) {
      _log('Failed to check connection: ${e.message}');
      return false;
    }
  }

  /// Scan for devices using BlueThermalPrinter
  static Future<List<BluetoothDevice>> getBondedDevices() async {
    try {
      return await BlueThermalPrinter.instance.getBondedDevices();
    } catch (e) {
      _log("Error getting bonded devices: $e");
      return [];
    }
  }

  static void _log(String message) {
    if (!_logController.isClosed) {
      _logController.add(message);
    }
    // print("[Y50Printer] $message");
  }
}

/// Paper type constants
class PaperType {
  static const int GAP = 0; // Gap paper
  static const int CONTINUOUS = 1; // Continuous paper
}
