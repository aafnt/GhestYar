package ir.ghestyar.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import ir.ghestyar.app.BuildConfig
import ir.ghestyar.app.data.database.AppDatabase
import ir.ghestyar.app.data.entity.AlertEntity
import ir.ghestyar.app.data.entity.InstallmentEntity
import ir.ghestyar.app.data.entity.LoanEntity
import ir.ghestyar.app.data.entity.SettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * پشتیبان‌گیری و بازیابی کامل اطلاعات برنامه (بند ۳۸-۳۹ سند طراحی).
 *
 * فرمت خروجی: یک فایل JSON شامل schemaVersion (برای پشتیبانی از Migration در آینده اگر
 * ساختار دیتابیس تغییر کرد - پیشنهاد اصلاحی ابتدای گفتگو)، نسخه اپلیکیشن، زمان تهیه، و
 * تمام جدول‌های وام، اقساط، هشدارها و تنظیمات.
 *
 * توجه: تصاویر وام‌ها (فایل‌های داخلی) در این نسخه ساده در فایل Backup گنجانده نمی‌شوند -
 * فقط مسیر ذخیره می‌شود؛ اگر گوشی عوض شود کاربر باید تصویر بانک را دوباره انتخاب کند. این
 * ساده‌سازی عمدی است تا حجم فایل Backup کوچک و اپ ساده بماند (بند ۶۰: چیزی اضافه نکن).
 */
object BackupManager {

    private const val SCHEMA_VERSION = 1

    data class RestoreResult(val success: Boolean, val message: String)

    suspend fun export(context: Context, db: AppDatabase, destinationUri: Uri): RestoreResult =
        withContext(Dispatchers.IO) {
            try {
                val root = JSONObject()
                root.put("schemaVersion", SCHEMA_VERSION)
                root.put("appVersion", BuildConfig.VERSION_NAME)
                root.put("exportedAt", Instant.now().toString())

                val loans = db.loanDao().getAllOnce()
                val installments = db.installmentDao().getAllOnce()
                val alerts = db.alertDao().getAllOnce()
                val settings = db.settingsDao().get() ?: SettingsEntity()

                root.put("loans", JSONArray(loans.map { it.toJson() }))
                root.put("installments", JSONArray(installments.map { it.toJson() }))
                root.put("alerts", JSONArray(alerts.map { it.toJson() }))
                root.put("settings", settings.toJson())

                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    out.write(root.toString(2).toByteArray(Charsets.UTF_8))
                } ?: return@withContext RestoreResult(false, "امکان نوشتن فایل وجود ندارد")

                RestoreResult(true, "پشتیبان‌گیری با موفقیت انجام شد")
            } catch (e: Exception) {
                RestoreResult(false, "خطا در تهیه پشتیبان: ${e.message}")
            }
        }

    /** بازیابی از فایل - تمام اطلاعات فعلی جایگزین می‌شود (کاربر قبلاً تأیید کرده است) */
    suspend fun restore(context: Context, db: AppDatabase, sourceUri: Uri): RestoreResult =
        withContext(Dispatchers.IO) {
            try {
                val text = context.contentResolver.openInputStream(sourceUri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: return@withContext RestoreResult(false, "امکان خواندن فایل وجود ندارد")

                val root = JSONObject(text)
                val schemaVersion = root.optInt("schemaVersion", 1)
                if (schemaVersion > SCHEMA_VERSION) {
                    return@withContext RestoreResult(
                        false,
                        "این فایل پشتیبان مربوط به نسخه جدیدتری از برنامه است"
                    )
                }

                val loans = root.getJSONArray("loans").toEntityList { LoanEntity.fromJson(it) }
                val installments = root.getJSONArray("installments").toEntityList { InstallmentEntity.fromJson(it) }
                val alerts = root.optJSONArray("alerts")?.toEntityList { AlertEntity.fromJson(it) } ?: emptyList()
                val settings = root.optJSONObject("settings")?.let { SettingsEntity.fromJson(it) } ?: SettingsEntity()

                db.withTransaction {
                    db.installmentDao().deleteAll()
                    db.alertDao().deleteAll()
                    db.loanDao().deleteAll()

                    // چون id ها autoGenerate هستند ولی روابط FK را باید حفظ کنیم، از insert مستقیم
                    // با id اصلی استفاده می‌کنیم (Room با @PrimaryKey ثابت هم insert را می‌پذیرد)
                    loans.forEach { db.loanDao().insert(it) }
                    installments.forEach { db.installmentDao().insert(it) }
                    if (alerts.isNotEmpty()) db.alertDao().upsertAll(alerts)
                    db.settingsDao().upsert(settings)
                }

                RestoreResult(true, "بازیابی اطلاعات با موفقیت انجام شد")
            } catch (e: Exception) {
                RestoreResult(false, "خطا در بازیابی: ${e.message}")
            }
        }

    private inline fun <T> JSONArray.toEntityList(mapper: (JSONObject) -> T): List<T> =
        (0 until length()).map { mapper(getJSONObject(it)) }
}

private fun LoanEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("imagePath", imagePath ?: JSONObject.NULL)
    put("totalAmount", totalAmount)
    put("receivedDate", receivedDate)
    put("installmentCount", installmentCount)
    put("periodType", periodType)
    put("firstDueDate", firstDueDate)
    put("firstInstallmentAmount", firstInstallmentAmount)
    put("otherInstallmentAmount", otherInstallmentAmount)
    put("createdAt", createdAt)
}

private fun LoanEntity.Companion.fromJson(json: JSONObject): LoanEntity = LoanEntity(
    id = json.getLong("id"),
    name = json.getString("name"),
    imagePath = json.optString("imagePath", null).takeUnless { it.isNullOrEmpty() },
    totalAmount = json.getLong("totalAmount"),
    receivedDate = json.getString("receivedDate"),
    installmentCount = json.getInt("installmentCount"),
    periodType = json.getString("periodType"),
    firstDueDate = json.getString("firstDueDate"),
    firstInstallmentAmount = json.getLong("firstInstallmentAmount"),
    otherInstallmentAmount = json.getLong("otherInstallmentAmount"),
    createdAt = json.optLong("createdAt", System.currentTimeMillis())
)

private fun InstallmentEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("loanId", loanId)
    put("installmentNumber", installmentNumber)
    put("amount", amount)
    put("dueDate", dueDate)
    put("paidDate", paidDate ?: JSONObject.NULL)
    put("note", note ?: JSONObject.NULL)
}

private fun InstallmentEntity.Companion.fromJson(json: JSONObject): InstallmentEntity = InstallmentEntity(
    id = json.getLong("id"),
    loanId = json.getLong("loanId"),
    installmentNumber = json.getInt("installmentNumber"),
    amount = json.getLong("amount"),
    dueDate = json.getString("dueDate"),
    paidDate = json.optString("paidDate", null).takeUnless { it.isNullOrEmpty() },
    note = json.optString("note", null).takeUnless { it.isNullOrEmpty() }
)

private fun AlertEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("loanId", loanId)
    put("alertIndex", alertIndex)
    put("enabled", enabled)
    put("daysBefore", daysBefore)
    put("hour", hour)
    put("minute", minute)
}

private fun AlertEntity.Companion.fromJson(json: JSONObject): AlertEntity = AlertEntity(
    id = json.getLong("id"),
    loanId = json.getLong("loanId"),
    alertIndex = json.getInt("alertIndex"),
    enabled = json.getBoolean("enabled"),
    daysBefore = json.getInt("daysBefore"),
    hour = json.getInt("hour"),
    minute = json.getInt("minute")
)

private fun SettingsEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("themeMode", themeMode)
    put("notificationsEnabled", notificationsEnabled)
}

private fun SettingsEntity.Companion.fromJson(json: JSONObject): SettingsEntity = SettingsEntity(
    id = json.optInt("id", 1),
    themeMode = json.optString("themeMode", "SYSTEM"),
    notificationsEnabled = json.optBoolean("notificationsEnabled", true)
)
