package com.example.y50_printer

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.print.base.bean.DeviceItem
import com.print.base.bean.ImgData
import com.print.base.bean.PrinterConstantPool
import com.print.base.bean.TaskCallBean
import com.print.base.listen.ConnectListener
import com.print.base.listen.TaskCallback
import com.print.base.utils.SDKUtils
import com.print.printer.Command
import com.print.printer.PrintImgHelper
import com.print.printer.Printer
import com.print.printer.PrinterManage

abstract class PrinterHelper {

    protected val imgNames: MutableList<String> = ArrayList()

    protected var printIndex: Int = 0 // Current printing index
    protected var sendIndex: Int = 0 // Current sending index

    protected var context: Context? = null

    protected val printer: Printer = PrinterManage.getInstance().getPrinter(PrinterConstantPool.SocketType.SPP)

    protected lateinit var helper: PrintImgHelper
    
    // Listeners for Plugin
    var onConnectionSuccess: (() -> Unit)? = null
    var onConnectionFailed: ((String) -> Unit)? = null
    var onLog: ((String) -> Unit)? = null
    var onPrintResult: ((String) -> Unit)? = null // Success or Error message

    companion object {
        /**
         * Bluetooth device reference object
         */
        var item: DeviceItem? = null

        fun setSDKKEY(application: Application) {
            SDKUtils.init(application, "d2fnGqzf2Rs=")
        }

        fun getInstance(): PrinterHelper? {
            if (item == null || item?.name == null) return null
            item?.modelKey = "Z212"
            return Printer_Y50()
        }
    }

    fun init(context: Context) {
        printer.setListener(connectListener)
        printer.setTaskCallback(taskCallback)
        helper = printer.helper
        this.context = context.applicationContext
    }

    /**
     * @param isConnect true to connect, false to disconnect
     */
    fun connect(isConnect: Boolean) {
        if (isConnect) {
            Log.d("PrinterHelper", "Connecting to device: ${PrinterHelper.item?.name}, ${PrinterHelper.item?.address}")
            printer.connect(PrinterHelper.item)
        } else {
            printer.disconnect()
        }
    }

    // _____________________________________________ Common methods start ____________________________________________________

    fun getInfo() {
        if (!check()) {
            return
        }
        addTask(Command.get_deviceName())
        addTask(Command.get_btName())
        addTask(Command.get_version())
        addTask(Command.get_DENSITY())
        addTask(Command.get_shutTime())
        addTask(Command.get_battervol())
        addTask(Command.get_btMAC())
        addTask(Command.get_SN())
        addTask(Command.get_status())
    }

    fun release() {
        printer.release()
    }

    /**
     * Stop printing
     */
    fun stopPrint() {
        log("stopPrint")
        helper.stopPrint()
        printIndex = 0
        sendIndex = 0
        PrintParameter.count = 0
    }

    fun testPage() {
        if (check()) {
            printer.addTask(Command.print_SELFTEST(), false)
        }
    }

    fun addTask(command: Command) {
        printer.addTask(command, "", false, callback)
    }

    private val callback = object : TaskCallback() {
        override fun readCall(bean: TaskCallBean) {
            val result = printer.data2Str(bean)
            val s = "type = ${bean.type}, hex: ${CommonUtil.byte2Hex(bean.data)}, data = $result"
            log(s)
        }
    }

    fun getPrinterStatus() {
        log("Query printer status")
        printer.addTask(Command.get_status(), null, false, statusCall)
    }

    fun check(): Boolean {
        return printer.isConnect
    }

    protected fun showPrintTips(msg: String?) {
        printIndex++
        val m = "Printing, processing image ${printIndex}"
        log(m)
        // No UI update here, just log
    }

    // _____________________________________________ Common methods end ____________________________________________________


    // _________________________________________ Print Start ___________________________________________________________

    fun printImg() {
        if (!check()) {
            return
        }
        helper.stopPrint()
        printInit()
        start()
    }

    private fun printInit() {
        if (PrintParameter.customImage != null) {
            var bitmap = PrintParameter.customImage!!
            bitmap = CommonUtil.zoomImg(
                bitmap,
                PrintParameter.imgW * PrintParameter.dpi,
                PrintParameter.imgH * PrintParameter.dpi
            )
            helper.setImgData(PrintParameter.threshold, ImgData("custom_image", bitmap))
            
            printIndex = 0
            sendIndex = 0
            imgNames.clear()
            for (i in 0 until PrintParameter.count) {
                imgNames.add("custom_image")
            }
        } else {
             // Fallback logic for resources if needed, but in Plugin usually we pass image
             // Assuming maps is empty if valid image passed
             printIndex = 0
             sendIndex = 0
             imgNames.clear()
        }

        showPrintTips(null)
    }

    protected open fun start() {
        getPrinterStatus()
    }

    open fun print() {
        nextPrint()
    }

    abstract fun nextPrint()

    // _____________________________________________ Callbacks ____________________________________________________

    private val statusCall = object : TaskCallback() {
        override fun sendStatus(bean: TaskCallBean) {
            if (bean.status != PrinterConstantPool.Status.OK) {
                log("Printer status callback: Send exception >>> ${bean.msg}")
            }
        }

        override fun readCall(bean: TaskCallBean) {
            try {
                log("Printer status callback>> type: ${bean.type}, msg: ${bean.msg}")
                val s = printer.data2Str(bean) 
                log("Printer status callback = $s")
                if (s == null || (s != "0" && s != "-4" && s != "-1")) {
                    log("Get printer status exception")
                    stopPrint()
                    return
                }
                print()
            } catch (e: Exception) {
                log(e.message ?: "Unknown error")
                stopPrint()
            }
        }

        override fun timeOut(): Int {
            return 1000
        }
    }

    protected val printCall = object : TaskCallback() {
        override fun sendStatus(bean: TaskCallBean) {
            if (bean.status != PrinterConstantPool.Status.OK) {
                log("Printing callback: Send failed")
            }
        }

        override fun readCall(bean: TaskCallBean) {
             if (!CommonUtil.equals(bean.type, PrinterConstantPool.Command.PRINT_IMG)) {
                return
            }

            if (bean.status == PrinterConstantPool.Status.TIMEOUT) {
                log("Printing timeout")
                stopPrint()
                return
            }

            if (printIndex >= PrintParameter.count) {
                log("Printing complete")
                onPrintResult?.invoke("Printing complete")
                stopPrint()
                return
            }

            // Simplified check logic to proceed
            if (sendIndex < PrintParameter.count && check()) {
                print()
            }
        }

        override fun timeOut(): Int {
            return maxOf(PrintParameter.paperH * PrintParameter.dpi * 30, 10 * 1000)
        }
    }

    private val connectListener = object : ConnectListener {
        override fun onConneted() {
            log("Connection callback: onConneted")
            onConnectionSuccess?.invoke()
        }

        override fun onConnetFailed(s: String) {
            log("Connection callback: onConnetFailed : $s")
            onConnectionFailed?.invoke(s)
        }

        override fun closed() {
            log("Connection callback: closed")
            // Can notify disconnect if needed
            onConnectionFailed?.invoke("Disconnected") 
        }
    }

    private val taskCallback = object : TaskCallback() {
        override fun readCall(bean: TaskCallBean) {
            if (bean.data == null) return
            // Proactive reporting logic
            val s = printer.data2Str(bean)
            log("Global read: $s")
        }
    }

    fun log(s: String) {
        Log.d("PrinterHelper", s)
        onLog?.invoke(s)
    }
}
