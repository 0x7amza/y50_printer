package com.example.y50_printer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.print.base.bean.DeviceItem
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** Y50PrinterPlugin */
class Y50PrinterPlugin : FlutterPlugin, MethodCallHandler {
    
    private lateinit var channel: MethodChannel
    private var printerHelper: PrinterHelper? = null
    private var context: Context? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Callbacks for asynchronous connection
    private var connectionCallback: Result? = null
    private var connectionTimeout: Runnable? = null
    private var connectionAccepted = false

    private fun clearConnectionTimeout() {
        connectionTimeout?.let { handler.removeCallbacks(it) }
        connectionTimeout = null
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "y50_printer")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
        
        // Initialize Utils
        CommonUtil.init(flutterPluginBinding.applicationContext)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        // Ensure initialization
        if (call.method == "initSDK") {
             try {
                if (context != null) {
                    PrinterHelper.setSDKKEY(context as android.app.Application)
                    result.success("SDK initialized")
                } else {
                    result.error("INIT_ERROR", "Context is null", null)
                }
            } catch (e: Exception) {
                result.error("INIT_ERROR", e.message, null)
            }
            return
        }

        when (call.method) {
            "setDevice" -> {
                try {
                    val name = call.argument<String>("name")
                    val address = call.argument<String>("address")
                    
                    if (name != null && address != null) {
                        val device = DeviceItem.build(address)
                        device.name = name
                        PrinterHelper.item = device
                        
                        printerHelper = PrinterHelper.getInstance()
                        
                        if (printerHelper == null) {
                            result.error("HELPER_NULL", "Failed to create printer helper instance", null)
                            return
                        }
                        
                        // Setup Listeners
                        setupPrinterListeners()
                        
                        printerHelper?.init(context!!)
                        result.success("Device set: $name")
                    } else {
                        result.error("INVALID_ARGS", "Name and address required", null)
                    }
                } catch (e: Exception) {
                    result.error("SET_DEVICE_ERROR", e.message, null)
                }
            }

            "connect" -> {
                if (printerHelper == null) {
                    result.error("NOT_INITIALIZED", "Call setDevice first", null)
                    return
                }
                
                if (connectionCallback != null) {
                    result.error("CONNECTION_BUSY", "A connection is already pending", null)
                    return
                }
                clearConnectionTimeout()
                connectionAccepted = false
                connectionCallback = result
                
                // Background thread for connection
                Thread {
                    try {
                        printerHelper?.connect(true)
                    } catch (e: Exception) {
                        handler.post {
                            if (connectionCallback === result) {
                                clearConnectionTimeout()
                                connectionCallback = null
                                result.error("CONNECT_ERROR", e.message, null)
                            }
                        }
                    }
                }.start()
                
                // Timeout
                val timeout = Runnable {
                    if (connectionCallback === result) {
                         val cb = connectionCallback
                         connectionCallback = null
                         connectionTimeout = null
                         connectionAccepted = false
                         runCatching { printerHelper?.connect(false) }
                         cb?.error("TIMEOUT", "Connection timeout after 15 seconds", null)
                    }
                }
                connectionTimeout = timeout
                handler.postDelayed(timeout, 15000)
            }

            "disconnect" -> {
                try {
                    clearConnectionTimeout()
                    connectionAccepted = false
                    connectionCallback?.error("CANCELLED", "Connection cancelled", null)
                    connectionCallback = null
                    printerHelper?.connect(false)
                    result.success("Disconnected")
                } catch (e: Exception) {
                    result.error("DISCONNECT_ERROR", e.message, null)
                }
            }

            "setPrintParameters" -> {
                try {
                    call.argument<Int>("paperW")?.let { PrintParameter.paperW = it }
                    call.argument<Int>("paperH")?.let { PrintParameter.paperH = it }
                    call.argument<Int>("imgW")?.let { PrintParameter.imgW = it }
                    call.argument<Int>("imgH")?.let { PrintParameter.imgH = it }
                    call.argument<Int>("dpi")?.let { PrintParameter.dpi = it }
                    call.argument<Int>("threshold")?.let { PrintParameter.threshold = it }
                    call.argument<Int>("count")?.let { PrintParameter.count = it }
                    call.argument<Int>("density")?.let { PrintParameter.density = it }
                    call.argument<Int>("speed")?.let { PrintParameter.speed = it }
                    call.argument<Int>("paperType")?.let { PrintParameter.paperType = it }
                    result.success("Parameters set")
                } catch (e: Exception) {
                    result.error("SET_PARAMS_ERROR", e.message, null)
                }
            }

            "print" -> {
                 try {
                    if (printerHelper == null) {
                        result.error("NOT_INITIALIZED", "Printer not initialized", null)
                        return
                    }
                    if (printerHelper?.check() != true) {
                        result.error("NOT_CONNECTED", "Printer is not connected", null)
                        return
                    }

                    val imageData = call.argument<ByteArray>("image")
                    if (imageData != null) {
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                        if (bitmap != null) {
                            PrintParameter.customImage = bitmap
                            PrintParameter.maps.clear()
                        } else {
                            result.error("DECODE_ERROR", "Failed to decode image", null)
                            return
                        }
                    } else {
                         PrintParameter.customImage = null
                         // Assuming resource map fallback is not used in plugin or handled differently
                    }
                    
                    printerHelper?.printImg()
                    result.success("Printing...")
                } catch (e: Exception) {
                    result.error("PRINT_ERROR", e.message, null)
                }
            }

            "stopPrint" -> {
                try {
                    printerHelper?.stopPrint()
                    result.success("Print stopped")
                } catch (e: Exception) {
                    result.error("STOP_ERROR", e.message, null)
                }
            }

            "getPrinterStatus" -> {
                 try {
                    printerHelper?.getPrinterStatus()
                    result.success("Getting status...")
                } catch (e: Exception) {
                    result.error("STATUS_ERROR", e.message, null)
                }
            }
            
            "getInfo" -> {
                 try {
                    printerHelper?.getInfo()
                    result.success("Getting info...")
                } catch (e: Exception) {
                    result.error("INFO_ERROR", e.message, null)
                }
            }

            "testPage" -> {
                 try {
                    printerHelper?.testPage()
                    result.success("Printing test page...")
                } catch (e: Exception) {
                    result.error("TEST_ERROR", e.message, null)
                }
            }

            "isConnected" -> {
                 try {
                    val isConnected = printerHelper?.check() ?: false
                    result.success(isConnected)
                } catch (e: Exception) {
                    result.error("CHECK_ERROR", e.message, null)
                }
            }

            else -> {
                result.notImplemented()
            }
        }
    }
    
    private fun setupPrinterListeners() {
        val source = printerHelper
        printerHelper?.onConnectionSuccess = {
            handler.post {
                if (printerHelper !== source) return@post
                if (connectionCallback == null) {
                    if (!connectionAccepted) runCatching { printerHelper?.connect(false) }
                    return@post
                }
                clearConnectionTimeout()
                connectionAccepted = true
                connectionCallback?.success("Connected successfully")
                connectionCallback = null
                channel.invokeMethod("onConnected", null)
                channel.invokeMethod("onConnectionStatusChanged", true)
            }
        }
        
        printerHelper?.onConnectionFailed = { error ->
            handler.post {
                if (printerHelper !== source) return@post
                clearConnectionTimeout()
                connectionAccepted = false
                if (connectionCallback != null) {
                     connectionCallback?.error("CONNECTION_FAILED", error, null)
                     connectionCallback = null
                }
                channel.invokeMethod("onConnectFailed", error)
                channel.invokeMethod("onConnectionStatusChanged", false)
            }
        }
        
        printerHelper?.onLog = { msg ->
            handler.post {
                if (printerHelper !== source) return@post
                channel.invokeMethod("onLog", msg)
            }
        }
        
        // You can add more specific callbacks if needed
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        clearConnectionTimeout()
        connectionAccepted = false
        connectionCallback?.error("CANCELLED", "Application closed", null)
        connectionCallback = null
        printerHelper?.onConnectionSuccess = null
        printerHelper?.onConnectionFailed = null
        printerHelper?.onLog = null
        channel.setMethodCallHandler(null)
        printerHelper?.release()
    }
}
