package com.jothivel.chits.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.jothivel.chits.data.local.SavedCollection
import java.io.File
import java.text.NumberFormat
import java.util.Locale

object ReceiptPdfHelper {
    fun share(context: Context, customer: String, chit: String, receipt: SavedCollection): Result<Unit> = runCatching {
        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, "Receipt-${receipt.receiptNo}.pdf")
        val pdf = PdfDocument()
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(420, 595, 1).create())
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(118,31,41); canvas.drawRect(0f,0f,420f,92f,paint)
        paint.color=Color.WHITE;paint.textSize=24f;paint.isFakeBoldText=true;canvas.drawText("JOTHI VEL CHITS",28f,42f,paint)
        paint.textSize=11f;paint.isFakeBoldText=false;canvas.drawText("OFFICIAL PAYMENT RECEIPT",28f,66f,paint)
        paint.color=Color.rgb(35,35,35);paint.textSize=12f
        var y=125f
        fun row(label:String,value:String){paint.color=Color.GRAY;paint.isFakeBoldText=false;canvas.drawText(label,28f,y,paint);paint.color=Color.rgb(30,30,30);paint.isFakeBoldText=true;canvas.drawText(value,160f,y,paint);y+=30f}
        row("Receipt No",receipt.receiptNo);row("Date",receipt.businessDate);row("Customer",customer);row("Chit",chit);row("Payment Mode",receipt.mode);row("Reference",receipt.referenceNo ?: "-")
        paint.color=Color.rgb(250,242,225);canvas.drawRoundRect(24f,y+5f,396f,y+82f,12f,12f,paint)
        paint.color=Color.rgb(90,22,30);paint.textSize=12f;paint.isFakeBoldText=false;canvas.drawText("AMOUNT RECEIVED",42f,y+34f,paint)
        paint.textSize=25f;paint.isFakeBoldText=true;canvas.drawText("₹"+NumberFormat.getNumberInstance(Locale("en","IN")).format(receipt.amountPaise/100),42f,y+66f,paint)
        paint.color=Color.DKGRAY;paint.textSize=10f;paint.isFakeBoldText=false;canvas.drawText("This is a system-generated receipt.",28f,535f,paint);canvas.drawText("Thank you for your payment.",28f,552f,paint)
        pdf.finishPage(page);file.outputStream().use(pdf::writeTo);pdf.close()
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);putExtra(Intent.EXTRA_SUBJECT,"Jothi Vel Chits receipt ${receipt.receiptNo}");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) },"Send receipt invoice"))
    }
}
