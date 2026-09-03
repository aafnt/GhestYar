package ir.ghestyar.app.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * URI برگشتی از Photo Picker همیشه دسترسی دائمی تضمین‌شده ندارد (ممکن است بعد از ری‌استارت
 * گوشی از بین برود). به همین دلیل تصویر انتخابی بلافاصله به فضای داخلی اپ کپی می‌شود و فقط
 * مسیر فایل داخلی در دیتابیس ذخیره می‌گردد (پیشنهاد اصلاحی که در ابتدای گفتگو مطرح شد).
 */
object ImageStorageManager {

    private const val FOLDER_NAME = "loan_images"

    private fun imagesDir(context: Context): File =
        File(context.filesDir, FOLDER_NAME).apply { if (!exists()) mkdirs() }

    /** کپی تصویر انتخاب‌شده به فضای داخلی اپ و بازگرداندن مسیر فایل ذخیره‌شده */
    fun copyToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val fileName = "loan_${UUID.randomUUID()}.jpg"
            val destFile = File(imagesDir(context), fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** حذف تصویر قدیمی وام هنگام تغییر یا حذف وام */
    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            File(path).takeIf { it.exists() }?.delete()
        } catch (_: Exception) {
            // نادیده گرفتن خطای حذف فایل - غیرحیاتی است
        }
    }
}
