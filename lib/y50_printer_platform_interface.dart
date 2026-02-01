import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'y50_printer_method_channel.dart';

abstract class Y50PrinterPlatform extends PlatformInterface {
  /// Constructs a Y50PrinterPlatform.
  Y50PrinterPlatform() : super(token: _token);

  static final Object _token = Object();

  static Y50PrinterPlatform _instance = MethodChannelY50Printer();

  /// The default instance of [Y50PrinterPlatform] to use.
  ///
  /// Defaults to [MethodChannelY50Printer].
  static Y50PrinterPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [Y50PrinterPlatform] when
  /// they register themselves.
  static set instance(Y50PrinterPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
