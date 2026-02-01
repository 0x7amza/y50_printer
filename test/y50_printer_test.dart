import 'package:flutter_test/flutter_test.dart';
import 'package:y50_printer/y50_printer.dart';
import 'package:y50_printer/y50_printer_platform_interface.dart';
import 'package:y50_printer/y50_printer_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockY50PrinterPlatform
    with MockPlatformInterfaceMixin
    implements Y50PrinterPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final Y50PrinterPlatform initialPlatform = Y50PrinterPlatform.instance;

  test('$MethodChannelY50Printer is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelY50Printer>());
  });

  test('getPlatformVersion', () async {
    Y50Printer y50PrinterPlugin = Y50Printer();
    MockY50PrinterPlatform fakePlatform = MockY50PrinterPlatform();
    Y50PrinterPlatform.instance = fakePlatform;

    expect(await y50PrinterPlugin.getPlatformVersion(), '42');
  });
}
