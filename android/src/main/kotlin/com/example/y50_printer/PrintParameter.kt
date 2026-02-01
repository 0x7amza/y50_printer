package com.example.y50_printer

import com.print.base.bean.PrinterConstantPool

/**
 * Print Parameters
 */
object PrintParameter {
    var paperW: Int = 0
    var paperH: Int = 0
    var imgW: Int = 0
    var imgH: Int = 0
    var dpi: Int = 8

    var threshold: Int = 160
    var count: Int = 1

    var density: Int = 2
    var speed: Int = 5

    val maps: MutableMap<String, Int> = HashMap()
    var customImage: android.graphics.Bitmap? = null

    var paperType: Int = PrinterConstantPool.PaperType.GAP // Gap paper
    // var paperType: Int = PrinterConstantPool.PaperType.CONTINUOUS // Continuous paper
}
