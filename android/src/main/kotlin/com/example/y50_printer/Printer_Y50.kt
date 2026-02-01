package com.example.y50_printer

import android.util.Log
import com.print.base.bean.PrinterConstantPool
import com.print.printer.PrintImgHelper

/**
 * Y50 GE230 does not have active reporting
 */
class Printer_Y50 : PrinterHelper() {
    
    // Callbacks are handled in PrinterHelper via listeners
    
    override fun nextPrint() {
        if (sendIndex >= PrintParameter.count) {
            return
        }
        sendIndex++
        Log.e("print", "Printing, sending image $sendIndex")
        
        val isGap = PrintParameter.paperType != PrinterConstantPool.PaperType.CONTINUOUS
        val build = helper.build(printCall)
        
        build.enable()
        
        if (sendIndex == 1 && isGap) {
            build.backoffPaper()
        }
        
        build.paperType(PrintParameter.paperType)
        build.printImg(imgNames.removeAt(0))
        
        if (isGap) {
            build.fixedPoint()
            if (sendIndex == PrintParameter.count) {
                build.forwardPaper()
            }
        } else {
            if (sendIndex == PrintParameter.count) {
                build.printLinedots(20 * PrintParameter.dpi) // Feed paper 5mm
            } else {
                build.printLinedots(5 * PrintParameter.dpi) // Feed paper 5mm
            }
        }
        
        build.disenable()
        helper.run(build)
    }
}
