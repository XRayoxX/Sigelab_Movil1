package gt.edu.sigelab.mobile.ui.modules

import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import java.io.ByteArrayOutputStream

/** Exporta las páginas elegidas en el diálogo de impresión de Android. */
class ReportPrintAdapter(private val title: String, private val columns: List<String>, private val rows: List<List<String>>) : PrintDocumentAdapter() {
    override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?,
                          callback: LayoutResultCallback, extras: Bundle?) {
        Thread {
            try {
                if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return@Thread }
                val count = exportPdf(ByteArrayOutputStream(), title, columns, rows)
                if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return@Thread }
                callback.onLayoutFinished(PrintDocumentInfo.Builder("$title.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(count).build(), oldAttributes != newAttributes)
            } catch (ex: Exception) { callback.onLayoutFailed("No fue posible preparar el reporte.") }
        }.start()
    }
    override fun onWrite(pages: Array<out PageRange>?, destination: ParcelFileDescriptor?, cancellationSignal: CancellationSignal?, callback: WriteResultCallback) {
        Thread {
            if (cancellationSignal?.isCanceled == true) { destination?.close(); callback.onWriteCancelled(); return@Thread }
            try {
                requireNotNull(destination)
                ParcelFileDescriptor.AutoCloseOutputStream(destination).use { exportPdf(it, title, columns, rows, pages) }
                if (cancellationSignal?.isCanceled == true) callback.onWriteCancelled()
                else callback.onWriteFinished(pages ?: arrayOf(PageRange.ALL_PAGES))
            } catch (ex: Exception) { callback.onWriteFailed("No fue posible preparar el reporte para impresión.") }
        }.start()
    }
}
