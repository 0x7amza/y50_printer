import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'y50_printer_platform_interface.dart';

/// An implementation of [Y50PrinterPlatform] that uses method channels.
class MethodChannelY50Printer extends Y50PrinterPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('y50_printer');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
