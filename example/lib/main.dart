import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:ui' as ui;
import 'package:flutter/rendering.dart';

import 'package:flutter/services.dart';
import 'package:y50_printer/y50_printer.dart';
import 'package:blue_thermal_printer/blue_thermal_printer.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List<BluetoothDevice> _devices = [];
  BluetoothDevice? _selectedDevice;
  bool _isConnected = false;
  String _log = "Initializing...";
  final GlobalKey _printKey = GlobalKey();

  @override
  void initState() {
    super.initState();
    _initPrinter();
  }

  Future<void> _initPrinter() async {
    // Request permissions
    await [
      Permission.bluetooth,
      Permission.bluetoothScan,
      Permission.bluetoothConnect,
      Permission.location,
    ].request();

    // 1. Initialize SDK
    await Y50Printer.initSDK();

    // 2. Get Paired Devices
    List<BluetoothDevice> devices = await Y50Printer.getBondedDevices();
    setState(() {
      _devices = devices;
      _log = "Found ${devices.length} paired devices";
    });

    // Listen to connection state
    Y50Printer.onConnectionStateChanged.listen((state) {
      setState(() {
        if (state == "CONNECTED") {
          _isConnected = true;
          _log = "Connected!";
        } else if (state == "DISCONNECTED") {
          _isConnected = false;
          _log = "Disconnected";
        } else {
          _log = state;
        }
      });
    });

    // Listen to logs
    Y50Printer.onLog.listen((msg) {
      debugPrint("[Y50_LOG] $msg");
    });
  }

  Future<void> _connect() async {
    if (_selectedDevice == null) return;
    setState(() => _log = "Connecting to ${_selectedDevice!.name}...");

    // Set Device
    await Y50Printer.setDevice(
      name: _selectedDevice!.name ?? "Unknown",
      address: _selectedDevice!.address ?? "",
    );

    // Connect
    await Y50Printer.connect();
  }

  Future<Uint8List?> _capturePng() async {
    try {
      RenderRepaintBoundary boundary =
          _printKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
      // Pixel Ratio 3.0 for better quality @ 203dpi
      ui.Image image = await boundary.toImage(pixelRatio: 3.0);
      ByteData? byteData = await image.toByteData(
        format: ui.ImageByteFormat.png,
      );
      return byteData?.buffer.asUint8List();
    } catch (e) {
      debugPrint("Error capturing image: $e");
      return null;
    }
  }

  Future<void> _printLabel() async {
    if (!_isConnected) return;

    setState(() => _log = "Capturing image...");

    final imageBytes = await _capturePng();
    if (imageBytes == null) {
      setState(() => _log = "Failed to capture image");
      return;
    }

    setState(() => _log = "Printing Label...");

    // Set Params
    await Y50Printer.setPrintParameters(
      paperW: 50,
      paperH: 30,
      imgW: 50,
      imgH: 30,
      dpi: 8,
      count: 1,
      paperType: PaperType.GAP,
    );

    // Print Label with Image
    await Y50Printer.printLabel(imageBytes: imageBytes);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Y50 Printer Plugin Example')),
        body: Column(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              color: Colors.grey[200],
              width: double.infinity,
              child: Text(_log, textAlign: TextAlign.center),
            ),

            // Preview Area (Hidden or Visible)
            Padding(
              padding: const EdgeInsets.all(20),
              child: RepaintBoundary(
                key: _printKey,
                child: Container(
                  width: 200, // 50mm approx relation
                  height: 120, // 30mm approx relation
                  decoration: BoxDecoration(
                    color: Colors.white,
                    border: Border.all(color: Colors.black),
                  ),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.qr_code, size: 50),
                      Text(
                        "Y50 PLUGIN TEST",
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      Text("1234567890"),
                    ],
                  ),
                ),
              ),
            ),

            if (!_isConnected)
              Expanded(
                child: ListView.builder(
                  itemCount: _devices.length,
                  itemBuilder: (context, index) {
                    final device = _devices[index];
                    return ListTile(
                      title: Text(device.name ?? "Unknown"),
                      subtitle: Text(device.address ?? "No Address"),
                      onTap: () {
                        setState(() => _selectedDevice = device);
                        _connect();
                      },
                      selected: _selectedDevice?.address == device.address,
                    );
                  },
                ),
              )
            else
              Expanded(
                child: Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text("Connected to ${_selectedDevice?.name}"),
                      SizedBox(height: 20),
                      ElevatedButton(
                        onPressed: () {
                          Y50Printer.testPage();
                        },
                        child: Text("Print Test Page"),
                      ),
                      SizedBox(height: 20),
                      ElevatedButton.icon(
                        onPressed: _printLabel,
                        icon: Icon(Icons.print),
                        label: Text("Print Label (50x30)"),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.deepPurple,
                          foregroundColor: Colors.white,
                          padding: EdgeInsets.symmetric(
                            horizontal: 30,
                            vertical: 15,
                          ),
                        ),
                      ),
                      SizedBox(height: 20),
                      ElevatedButton(
                        onPressed: () {
                          Y50Printer.disconnect();
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.red,
                        ),
                        child: Text("Disconnect"),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
