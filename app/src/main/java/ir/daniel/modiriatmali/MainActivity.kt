package ir.daniel.modiriatmali
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

import android.content.ContentValues
import kotlinx.coroutines.launch
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.net.Uri
import kotlinx.coroutines.delay
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.pdf.PdfDocument
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
private val LocalCurrency = compositionLocalOf { "تومان" }
private val LocalDateFormat = compositionLocalOf { "شمسی" }

private fun formatDisplayDate(
    value: String,
    dateFormat: String
): String {
    return try {
        val parser = SimpleDateFormat(
            "yyyy/MM/dd HH:mm",
            Locale("fa", "IR")
        )
        val date = parser.parse(value) ?: return value

        val locale = if (dateFormat == "شمسی") {
            android.icu.util.ULocale("fa_IR@calendar=persian")
        } else {
            android.icu.util.ULocale("fa_IR@calendar=gregorian")
        }

        val formatter = android.icu.text.SimpleDateFormat(
            "yyyy/MM/dd HH:mm",
            locale
        )
        formatter.format(date)
    } catch (e: Exception) {
        value
    }
}

@Composable
private fun displayDate(value: String): String {
    return formatDisplayDate(value, LocalDateFormat.current)
}

private val Night = Color(0xFF080D18)
private val Panel = Color(0xFF111B2D)
private val Cyan = Color(0xFF27D7F2)
private val Purple = Color(0xFF9B7BFF)
private val Green = Color(0xFF36D399)
private val Red = Color(0xFFFF647C)

data class AppThemePreset(
    val id: String,
    val name: String,
    val description: String,
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color
)

private val appThemePresets = listOf(
    AppThemePreset(
        "neon", "نئونی دیجیتال", "قالب فعلی برنامه",
        Color(0xFF080D18), Color(0xFF111B2D),
        Color(0xFF27D7F2), Color(0xFF9B7BFF)
    ),
    AppThemePreset(
        "royal", "سلطنتی", "مشکی و طلایی لوکس",
        Color(0xFF100D0A), Color(0xFF211A12),
        Color(0xFFFFC857), Color(0xFFD49B42)
    ),
    AppThemePreset(
        "ocean", "اقیانوسی", "آبی عمیق و آرام",
        Color(0xFF071522), Color(0xFF102A40),
        Color(0xFF45B7E8), Color(0xFF58D6C5)
    ),
    AppThemePreset(
        "forest", "جنگلی", "سبز تیره و طبیعی",
        Color(0xFF0B1711), Color(0xFF17291E),
        Color(0xFF65D68A), Color(0xFFB2D96C)
    ),
    AppThemePreset(
        "purple", "بنفش مدرن", "بنفش و سرمه‌ای",
        Color(0xFF100D20), Color(0xFF211A38),
        Color(0xFFB69AFF), Color(0xFFEC91D8)
    ),
    AppThemePreset(
        "light", "روشن مینیمال", "سفید و آبی ملایم",
        Color(0xFFF2F5FA), Color(0xFFFFFFFF),
        Color(0xFF2878D0), Color(0xFF7356C7)
    )
)


data class MoneyTransaction(
    val id: Long,
    val type: String,
    val amount: Long,
    val category: String,
    val note: String,
    val date: String
)

data class BudgetAlert(
    val id: Long,
    val percentage: Int,
    val enabled: Boolean,
    val alertType: String,
    val soundUri: String,
    val durationSeconds: Int,
    val vibration: Boolean
)

data class SavingsGoal(
    val id: Long,
    val title: String,
    val target: Long,
    val saved: Long
)
data class DebtItem(
    val id: Long,
    val person: String,
    val type: String,
    val amount: Long,
    val paid: Long,
    val note: String,
    val date: String
) {
    val remaining: Long
        get() = (amount - paid).coerceAtLeast(0L)
}
data class BankItem(
    val id: Long,
    val name: String,
    val accountNumber: String,
    val balance: Long
)
class FinanceDb(context: Context) :
    SQLiteOpenHelper(context, "finance.db", null, 7) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT NOT NULL, amount INTEGER NOT NULL, " +
                "category TEXT NOT NULL, note TEXT NOT NULL, date TEXT NOT NULL)"
        )
        createCategoriesTable(db)
        createBudgetTables(db)
        createSavingsTable(db)
        createDebtsTable(db)
        createBanksTable(db)
    }

 override fun onUpgrade(
    db: SQLiteDatabase,
    oldVersion: Int,
    newVersion: Int
) {
    if (oldVersion < 2) {
        createCategoriesTable(db)
    }

    if (oldVersion < 3) {
        createBudgetTables(db)
    }

    if (oldVersion < 4) {
        db.execSQL(
            "INSERT INTO budget_alerts (percentage) " +
            "SELECT 50 WHERE NOT EXISTS " +
            "(SELECT 1 FROM budget_alerts WHERE percentage = 50)"
        )

        db.execSQL(
            "INSERT INTO budget_alerts (percentage) " +
            "SELECT 75 WHERE NOT EXISTS " +
            "(SELECT 1 FROM budget_alerts WHERE percentage = 75)"
        )
    }

    if (oldVersion < 5) {
        createSavingsTable(db)
    }
    if (oldVersion < 6) {
    createDebtsTable(db)
    }
    if (oldVersion < 7) {
    createBanksTable(db)
    }
 }
 
    
private fun createBudgetTables(db: SQLiteDatabase) {
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS budget_settings (" +
            "id INTEGER PRIMARY KEY CHECK(id = 1), " +
            "daily_limit INTEGER NOT NULL DEFAULT 0)"
    )

    db.execSQL(
        "INSERT OR IGNORE INTO budget_settings (id, daily_limit) VALUES (1, 0)"
    )

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS budget_alerts (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "percentage INTEGER NOT NULL, " +
            "enabled INTEGER NOT NULL DEFAULT 1, " +
            "alert_type TEXT NOT NULL DEFAULT 'صدا', " +
            "sound_uri TEXT NOT NULL DEFAULT '', " +
            "duration_seconds INTEGER NOT NULL DEFAULT 5, " +
            "vibration INTEGER NOT NULL DEFAULT 1, " +
            "last_triggered_date TEXT NOT NULL DEFAULT '')"
    )
db.execSQL(
    "INSERT OR IGNORE INTO budget_alerts (percentage) VALUES (50)"
)

db.execSQL(
    "INSERT OR IGNORE INTO budget_alerts (percentage) VALUES (75)"
)
    db.execSQL(
        "INSERT OR IGNORE INTO budget_alerts (percentage) VALUES (85)"
    )

    db.execSQL(
        "INSERT OR IGNORE INTO budget_alerts (percentage) VALUES (100)"
    )
}
private fun createDebtsTable(db: SQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS debts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            person TEXT NOT NULL,
            type TEXT NOT NULL,
            amount INTEGER NOT NULL,
            paid INTEGER NOT NULL DEFAULT 0,
            note TEXT NOT NULL DEFAULT '',
            date TEXT NOT NULL
        )
        """.trimIndent()
    )
}
private fun createBanksTable(db: SQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS banks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            account_number TEXT NOT NULL DEFAULT '',
            balance INTEGER NOT NULL DEFAULT 0
        )
        """.trimIndent()
    )
}
    private fun createCategoriesTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "UNIQUE(type, name))"
        )

        val incomeCategories = listOf(
            "حقوق", "پاداش", "فروش", "هدیه", "سود سرمایه‌گذاری", "سایر"
        )

        val expenseCategories = listOf(
            "خوراک", "حمل‌ونقل", "خرید", "قبض و خدمات",
            "درمان", "پوشاک", "تفریح", "آموزش",
            "اجاره و مسکن", "قسط و بدهی", "سایر"
        )

        incomeCategories.forEach {
            insertCategory(db, "درآمد", it)
        }

        expenseCategories.forEach {
            insertCategory(db, "هزینه", it)
        }
    }

    private fun insertCategory(
        db: SQLiteDatabase,
        type: String,
        name: String
    ) {
        val values = ContentValues().apply {
            put("type", type)
            put("name", name)
        }

        db.insertWithOnConflict(
            "categories",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }
        fun getBudgetAlerts(): List<BudgetAlert> {
        val result = mutableListOf<BudgetAlert>()

        readableDatabase.rawQuery(
            "SELECT id, percentage, enabled, alert_type, sound_uri, duration_seconds, vibration FROM budget_alerts ORDER BY percentage ASC, id ASC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    BudgetAlert(
                        id = cursor.getLong(0),
                        percentage = cursor.getInt(1),
                        enabled = cursor.getInt(2) == 1,
                        alertType = cursor.getString(3),
                        soundUri = cursor.getString(4),
                        durationSeconds = cursor.getInt(5),
                        vibration = cursor.getInt(6) == 1
                    )
                )
            }
        }
        return result
    }
fun addBudgetAlert(
    percentage: Int,
    alertType: String = "صدا",
    durationSeconds: Int = 5,
    vibration: Boolean = true,
    soundUri: String = ""
): Boolean {
    if (percentage !in 1..100) return false

    val values = ContentValues().apply {
        put("percentage", percentage)
        put("enabled", 1)
        put("alert_type", alertType)
        put("sound_uri", soundUri)
        put("duration_seconds", durationSeconds.coerceAtLeast(1))
        put("vibration", if (vibration) 1 else 0)
    }

    return writableDatabase.insert(
        "budget_alerts",
        null,
        values
    ) != -1L
}
    

    fun updateBudgetAlert(alert: BudgetAlert): Boolean {
        if (alert.percentage !in 1..100) return false

        val values = ContentValues().apply {
            put("percentage", alert.percentage)
            put("enabled", if (alert.enabled) 1 else 0)
            put("alert_type", alert.alertType)
            put("sound_uri", alert.soundUri)
            put("duration_seconds", alert.durationSeconds.coerceAtLeast(1))
            put("vibration", if (alert.vibration) 1 else 0)
        }

        return writableDatabase.update(
            "budget_alerts", values, "id = ?",
            arrayOf(alert.id.toString())
        ) > 0
    }

    fun deleteBudgetAlert(id: Long): Boolean {
        return writableDatabase.delete(
            "budget_alerts", "id = ?",
            arrayOf(id.toString())
        ) > 0
    }

    fun setBudgetAlertEnabled(id: Long, enabled: Boolean): Boolean {
        val values = ContentValues().apply {
            put("enabled", if (enabled) 1 else 0)
        }

        return writableDatabase.update(
            "budget_alerts", values, "id = ?",
            arrayOf(id.toString())
        ) > 0
    }
    fun markBudgetAlertTriggered(id: Long, date: String): Boolean {
    val values = ContentValues().apply {
        put("last_triggered_date", date)
    }

    return writableDatabase.update(
        "budget_alerts",
        values,
        "id = ?",
        arrayOf(id.toString())
    ) > 0
}

fun getBudgetAlertsNotTriggeredToday(date: String): List<BudgetAlert> {
    val result = mutableListOf<BudgetAlert>()

    readableDatabase.rawQuery(
        """
        SELECT id, percentage, enabled, alert_type,
               sound_uri, duration_seconds, vibration
        FROM budget_alerts
        WHERE enabled = 1 AND last_triggered_date != ?
        ORDER BY percentage ASC
        """.trimIndent(),
        arrayOf(date)
    ).use { cursor ->
        while (cursor.moveToNext()) {
            result.add(
                BudgetAlert(
                    id = cursor.getLong(0),
                    percentage = cursor.getInt(1),
                    enabled = cursor.getInt(2) == 1,
                    alertType = cursor.getString(3),
                    soundUri = cursor.getString(4),
                    durationSeconds = cursor.getInt(5),
                    vibration = cursor.getInt(6) == 1
                )
            )
        }
    }

    return result
}
fun getBudgetAlertsForPercentage(
    currentPercentage: Int,
    today: String
): List<BudgetAlert> {
    val result = mutableListOf<BudgetAlert>()

    readableDatabase.rawQuery(
        """
        SELECT id, percentage, enabled, alert_type,
               sound_uri, duration_seconds, vibration
        FROM budget_alerts
        WHERE enabled = 1
          AND percentage <= ?
          AND last_triggered_date != ?
        ORDER BY percentage ASC
        """.trimIndent(),
        arrayOf(currentPercentage.toString(), today)
    ).use { cursor ->
        while (cursor.moveToNext()) {
            result.add(
                BudgetAlert(
                    id = cursor.getLong(0),
                    percentage = cursor.getInt(1),
                    enabled = cursor.getInt(2) == 1,
                    alertType = cursor.getString(3),
                    soundUri = cursor.getString(4),
                    durationSeconds = cursor.getInt(5),
                    vibration = cursor.getInt(6) == 1
                )
            )
        }
    }

    return result
}
fun getDailyLimit(): Long {
    readableDatabase.rawQuery(
        "SELECT daily_limit FROM budget_settings WHERE id = 1",
        null
    ).use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getLong(0)
        }
    }
    return 0L
}

fun setDailyLimit(amount: Long) {
    val values = ContentValues().apply {
        put("daily_limit", amount.coerceAtLeast(0L))
    }

    writableDatabase.update(
        "budget_settings",
        values,
        "id = 1",
        null
    )
}
    fun getCategories(type: String): List<String> {
        val result = mutableListOf<String>()

        readableDatabase.rawQuery(
            "SELECT name FROM categories WHERE type = ? ORDER BY id ASC",
            arrayOf(type)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0))
            }
        }

        return result
    }

    fun addCategory(type: String, name: String): Boolean {
        if (name.isBlank()) return false

        val values = ContentValues().apply {
            put("type", type)
            put("name", name.trim())
        }

        return writableDatabase.insertWithOnConflict(
            "categories",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }

    fun renameCategory(
        type: String,
        oldName: String,
        newName: String
    ): Boolean {
        if (newName.isBlank()) return false

        val values = ContentValues().apply {
            put("name", newName.trim())
        }

        return writableDatabase.update(
            "categories",
            values,
            "type = ? AND name = ?",
            arrayOf(type, oldName)
        ) > 0
    }

    fun deleteCategory(type: String, name: String): Boolean {
        if (name == "سایر") return false

        return writableDatabase.delete(
            "categories",
            "type = ? AND name = ?",
            arrayOf(type, name)
        ) > 0
    }

    fun add(
        type: String,
        amount: Long,
        category: String,
        note: String
    ) {
        val values = ContentValues().apply {
            put("type", type)
            put("amount", amount)
            put("category", category)
            put("note", note)
            put(
                "date",
                SimpleDateFormat(
                    "yyyy/MM/dd HH:mm",
                    Locale("fa", "IR")
                ).format(Date())
            )
        }

        writableDatabase.insertOrThrow(
            "transactions",
            null,
            values
        )
    }

    fun update(item: MoneyTransaction) {
        val values = ContentValues().apply {
            put("amount", item.amount)
            put("category", item.category)
            put("note", item.note)
        }

        writableDatabase.update(
            "transactions",
            values,
            "id = ?",
            arrayOf(item.id.toString())
        )
    }

    fun delete(id: Long) {
        writableDatabase.delete(
            "transactions",
            "id = ?",
            arrayOf(id.toString())
        )
    }
fun createSavingsTable(db: SQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS savings_goals (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            target INTEGER NOT NULL,
            saved INTEGER NOT NULL DEFAULT 0
        )
        """.trimIndent()
    )
}

fun getSavingsGoals(): List<SavingsGoal> {
    val result = mutableListOf<SavingsGoal>()

    readableDatabase.rawQuery(
        "SELECT id, title, target, saved FROM savings_goals ORDER BY id DESC",
        null
    ).use { cursor ->
        while (cursor.moveToNext()) {
            result.add(
                SavingsGoal(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getLong(2),
                    cursor.getLong(3)
                )
            )
        }
    }

    return result
}

fun addSavingsGoal(title: String, target: Long): Boolean {
    if (title.isBlank() || target <= 0) return false

    val values = ContentValues().apply {
        put("title", title.trim())
        put("target", target)
        put("saved", 0L)
    }

    return writableDatabase.insert(
        "savings_goals", null, values
    ) != -1L
}

fun updateSavingsAmount(id: Long, amount: Long): Boolean {
    val values = ContentValues().apply {
        put("saved", amount.coerceAtLeast(0L))
    }

    return writableDatabase.update(
        "savings_goals",
        values,
        "id = ?",
        arrayOf(id.toString())
    ) > 0
}

fun deleteSavingsGoal(id: Long): Boolean {
    return writableDatabase.delete(
        "savings_goals",
        "id = ?",
        arrayOf(id.toString())
    ) > 0
}
fun addDebt(
    person: String,
    type: String,
    amount: Long,
    note: String
): Boolean {
    if (person.isBlank() || amount <= 0) return false

    val values = ContentValues().apply {
        put("person", person.trim())
        put("type", type)
        put("amount", amount)
        put("paid", 0L)
        put("note", note.trim())
        put(
            "date",
            SimpleDateFormat(
                "yyyy/MM/dd HH:mm",
                Locale("fa", "IR")
            ).format(Date())
        )
    }

    return writableDatabase.insert("debts", null, values) != -1L
}

fun getDebts(): List<DebtItem> {
    val result = mutableListOf<DebtItem>()

    readableDatabase.rawQuery(
        "SELECT id, person, type, amount, paid, note, date FROM debts ORDER BY id DESC",
        null
    ).use { cursor ->
        while (cursor.moveToNext()) {
            result.add(
                DebtItem(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getLong(3),
                    cursor.getLong(4),
                    cursor.getString(5),
                    cursor.getString(6)
                )
            )
        }
    }

    return result
}

fun updateDebtPaid(id: Long, paid: Long): Boolean {
    val values = ContentValues().apply {
        put("paid", paid.coerceAtLeast(0L))
    }

    return writableDatabase.update(
        "debts",
        values,
        "id = ? AND ? <= amount",
        arrayOf(id.toString(), paid.toString())
    ) > 0
}

fun deleteDebt(id: Long): Boolean {
    return writableDatabase.delete(
        "debts",
        "id = ?",
        arrayOf(id.toString())
    ) > 0
}
fun addBank(
    name: String,
    accountNumber: String,
    balance: Long
): Boolean {
    if (name.isBlank() || balance < 0) return false

    val values = ContentValues().apply {
        put("name", name.trim())
        put("account_number", accountNumber.trim())
        put("balance", balance)
    }

    return writableDatabase.insert("banks", null, values) != -1L
}

fun getBanks(): List<BankItem> {
    val result = mutableListOf<BankItem>()

    readableDatabase.rawQuery(
        "SELECT id, name, account_number, balance FROM banks ORDER BY id DESC",
        null
    ).use { cursor ->
        while (cursor.moveToNext()) {
            result.add(
                BankItem(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getLong(3)
                )
            )
        }
    }

    return result
}

fun updateBankBalance(id: Long, balance: Long): Boolean {
    if (balance < 0) return false

    val values = ContentValues().apply {
        put("balance", balance)
    }

    return writableDatabase.update(
        "banks",
        values,
        "id = ?",
        arrayOf(id.toString())
    ) > 0
}

fun deleteBank(id: Long): Boolean {
    return writableDatabase.delete(
        "banks",
        "id = ?",
        arrayOf(id.toString())
    ) > 0
}
    fun all(): List<MoneyTransaction> {
        val result = mutableListOf<MoneyTransaction>()

        readableDatabase.rawQuery(
            "SELECT id,type,amount,category,note,date " +
                "FROM transactions ORDER BY id DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    MoneyTransaction(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getLong(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5)
                    )
                )
            }
        }

        return result
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = FinanceDb(this)
        setContent { FinanceApp(db) }
    }
}
private suspend fun playBudgetAlert(
    context: Context,
    alert: BudgetAlert
) {
    val duration = alert.durationSeconds.coerceIn(1, 60)

    var ringtone: android.media.Ringtone? = null
    var vibrator: Vibrator? = null

    try {
        if (alert.alertType == "صدا") {
            val soundUri = if (alert.soundUri.isNotBlank()) {
                Uri.parse(alert.soundUri)
            } else {
                RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_NOTIFICATION
                )
            }

            ringtone = RingtoneManager.getRingtone(
                context,
                soundUri
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            }

            ringtone?.play()
        }

        if (alert.vibration) {
            vibrator = if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(
                    VibratorManager::class.java
                ).defaultVibrator
            } else {
                context.getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 500, 300, 500),
                        0
                    )
                )
            }
        }

        delay(duration * 1000L)

    } finally {
        ringtone?.stop()
        vibrator?.cancel()
    }
}
class BudgetAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val alertId = inputData.getLong("alert_id", -1L)

        if (alertId <= 0L) {
            return Result.failure()
        }

        val alert = BudgetAlert(
            id = alertId,
            percentage = inputData.getInt("percentage", 0),
            enabled = true,
            alertType = inputData.getString("alert_type") ?: "صدا",
            soundUri = inputData.getString("sound_uri") ?: "",
            durationSeconds = inputData.getInt("duration", 5),
            vibration = inputData.getBoolean("vibration", true)
        )

        return try {
            playBudgetAlert(applicationContext, alert)

            val today = SimpleDateFormat(
                "yyyy/MM/dd",
                Locale("fa", "IR")
            ).format(Date())

            FinanceDb(applicationContext)
                .markBudgetAlertTriggered(alertId, today)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
private fun money(value: Long, currency: String = "تومان"): String =
    NumberFormat.getInstance(Locale("fa", "IR")).format(value) + " " + currency

@Composable
private fun displayMoney(value: Long): String {
    return money(value, LocalCurrency.current)
}
private fun toEnglishDigits(value: String): String {
    val persian = "۰۱۲۳۴۵۶۷۸۹"
    val arabic = "٠١٢٣٤٥٦٧٨٩"
    return value.map { ch ->
        when {
            persian.indexOf(ch) >= 0 -> ('0'.code + persian.indexOf(ch)).toChar()
            arabic.indexOf(ch) >= 0 -> ('0'.code + arabic.indexOf(ch)).toChar()
            else -> ch
        }
    }.joinToString("")
}
private fun applyLauncherIcon(context: Context, iconId: String) {
    val aliases = mapOf(
    "wallet" to ".WalletIcon",
    "chart" to ".ChartIcon",
    "coin" to ".CoinIcon",
    "bank" to ".BankIcon",
    "savings" to ".SavingsIcon",
    "receipt" to ".ReceiptIcon",
    "calculator" to ".CalculatorIcon",
    "card" to ".CardIcon"
)
    

    val selectedAlias = aliases[iconId] ?: ".WalletIcon"

    val packageManager = context.packageManager

    packageManager.setComponentEnabledSetting(
        android.content.ComponentName(
            context.packageName,
            context.packageName + selectedAlias
        ),
        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        android.content.pm.PackageManager.DONT_KILL_APP
    )

    aliases.values
        .filter { it != selectedAlias }
        .forEach { alias ->
            packageManager.setComponentEnabledSetting(
                android.content.ComponentName(
                    context.packageName,
                    context.packageName + alias
                ),
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
        }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceApp(db: FinanceDb) {
    
    var transactionFilter by remember { mutableStateOf("همه") }
var refresh by remember { mutableIntStateOf(0) }
val context = LocalContext.current
    
val themePreferences = remember {
    context.getSharedPreferences(
        "app_settings",
        Context.MODE_PRIVATE
    )
}
val currency = themePreferences.getString("currency", "تومان") ?: "تومان"
    val dateFormat = themePreferences.getString("date_format", "شمسی") ?: "شمسی"
var page by remember {
    mutableStateOf("خانه")
}
var pageHistory by remember {
    mutableStateOf(listOf<String>())
}

val navigateTo: (String) -> Unit = { target ->
    if (target == "خانه") {
        pageHistory = emptyList()
        page = "خانه"
    } else if (target != page) {
        pageHistory = pageHistory + page
        page = target
    }
}

BackHandler(enabled = pageHistory.isNotEmpty()) {
    page = pageHistory.last()
    pageHistory = pageHistory.dropLast(1)
}
var pendingPrivatePage by remember {
    mutableStateOf<String?>(null)
}

var privatePinInput by remember {
    mutableStateOf("")
}

var privatePinError by remember {
    mutableStateOf("")
}
LaunchedEffect(Unit) {
    val startPage = themePreferences.getString(
        "start_page",
        "خانه"
    ) ?: "خانه"

    val startLockKey = when (startPage) {
        "گزارش‌ها و PDF" -> "lock_reports"
        "قرض و بدهی" -> "lock_debts"
        "جستجو و تاریخچه" -> "lock_history"
        "پس‌انداز" -> "lock_savings"
        "مدیریت بانک‌ها" -> "lock_banks"
        "ماشین‌حساب" -> "lock_calculator"
        else -> null
    }

    if (
        startLockKey != null &&
        themePreferences.getBoolean(startLockKey, false)
    ) {
        pendingPrivatePage = startPage
    } else {
    pageHistory = emptyList()
    page = startPage
    }
}

val confirmDelete = themePreferences.getBoolean("confirm_delete", true)
var selectedThemeId by remember {
    mutableStateOf(
        themePreferences.getString("theme_id", "neon") ?: "neon"
    )
}

val activeTheme = appThemePresets.firstOrNull {
    it.id == selectedThemeId
} ?: appThemePresets.first()
var selectedIconId by remember {
    mutableStateOf(
        themePreferences.getString("icon_id", "wallet") ?: "wallet"
    )
}

LaunchedEffect(selectedIconId) {
    applyLauncherIcon(context, selectedIconId)
}


val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
val scope = rememberCoroutineScope() 
BackHandler(enabled = drawerState.isOpen) {
    scope.launch {
        drawerState.close()
    }
}

var expandedDrawerGroups by remember {
    mutableStateOf(setOf<String>())
}


    var editing by remember { mutableStateOf<MoneyTransaction?>(null) }
    var deleting by remember { mutableStateOf<MoneyTransaction?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf("") }

    val transactions = remember(refresh) { db.all() }
    val income = transactions.filter { it.type == "درآمد" }.sumOf { it.amount }
    val expenses = transactions.filter { it.type == "هزینه" }.sumOf { it.amount }
    val currentMonth = SimpleDateFormat(
    "yyyy/MM",
    Locale("fa", "IR")
).format(Date())

val monthlyTransactions = transactions.filter {
    it.date.startsWith(currentMonth)
}

val monthlyIncome = monthlyTransactions
    .filter { it.type == "درآمد" }
    .sumOf { it.amount }

val monthlyExpenses = monthlyTransactions
    .filter { it.type == "هزینه" }
    .sumOf { it.amount }

val monthlyBalance = monthlyIncome - monthlyExpenses
    val pdfLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/pdf")
) { uri ->
    if (uri != null) {
                createMonthlyPdf(
            context,
            uri,
            monthlyTransactions,
            currentMonth,
            currency,
            dateFormat
        )
    }
    }
    
var backupMessage by remember { mutableStateOf("") }

val backupLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("application/zip")
) { uri ->
    if (uri != null) {
        try {
            createFinanceBackup(context, db, themePreferences, uri)
            backupMessage = "پشتیبان‌گیری با موفقیت انجام شد."
        } catch (e: Exception) {
            backupMessage = "خطا در پشتیبان‌گیری: ${e.message}"
        }
    }
}

val restoreLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
) { uri ->
    if (uri != null) {
        try {
            restoreFinanceBackup(context, db, themePreferences, uri)
            backupMessage = "بازیابی اطلاعات با موفقیت انجام شد."
            refresh++
        } catch (e: Exception) {
            backupMessage = "خطا در بازیابی: ${e.message}"
        }
    }
}

    
val drawerGroups = listOf(
    "خانه" to emptyList(),

    "تراکنش‌ها" to listOf(
        "ثبت درآمد",
        "ثبت هزینه",
        "دسته‌بندی‌ها",
        "جستجو و تاریخچه",
        "قرض و بدهی",
        "پس‌انداز"
    ),

    "حساب‌ها و بانک‌ها" to listOf(
        "مدیریت بانک‌ها"
    ),

    "بودجه و هشدارها" to listOf(
        "بودجه‌بندی",
        "تنظیم هشدارها"
    ),

    "ابزارها" to listOf(
        "گزارش‌ها و PDF",
        "ماشین‌حساب",
        "پشتیبانی هوشمند"
    ),

    "تنظیمات" to listOf(
        "قفل بخش‌های خصوصی",
        "شخصی‌سازی",
        "پشتیبان‌گیری",
        "تنظیمات"
    )
)

val todayDate = SimpleDateFormat(
    "yyyy/MM/dd",
    Locale("fa", "IR")
).format(Date())

val dailyBudgetLimit = db.getDailyLimit()

val todayExpenses = transactions
    .filter {
        it.type == "هزینه" &&
        it.date.startsWith(todayDate)
    }
    .sumOf { it.amount }

val budgetPercentage = if (dailyBudgetLimit > 0L) {
    (todayExpenses.toDouble() / dailyBudgetLimit * 100).toInt()
} else {
    0
}

val budgetProgress = if (dailyBudgetLimit > 0L) {
    (todayExpenses.toFloat() / dailyBudgetLimit.toFloat())
        .coerceIn(0f, 1f)
} else {
    0f
}

val budgetColor = when {
    budgetPercentage >= 100 -> Red
    budgetPercentage >= 85 -> Color(0xFFFF9800)
    else -> Green
}
    

val appColorScheme = if (selectedThemeId == "light") {
    lightColorScheme(
        primary = activeTheme.primary,
        onPrimary = Color.White,
        secondary = activeTheme.secondary,
        onSecondary = Color.White,
        tertiary = Green,
        background = activeTheme.background,
        onBackground = Color(0xFF1A2230),
        surface = activeTheme.surface,
        onSurface = Color(0xFF1A2230),
        surfaceVariant = Color(0xFFE5EAF2),
        onSurfaceVariant = Color(0xFF465365),
        outline = Color(0xFF8290A3),
        error = Red,
        onError = Color.White
    )
} else {
    darkColorScheme(
        primary = activeTheme.primary,
        onPrimary = Night,
        primaryContainer = Color(0xFF123746),
        onPrimaryContainer = Color(0xFFB9F5FF),
        secondary = activeTheme.secondary,
        onSecondary = Night,
        secondaryContainer = Color(0xFF30264F),
        onSecondaryContainer = Color(0xFFE5DAFF),
        tertiary = Green,
        onTertiary = Night,
        background = activeTheme.background,
        onBackground = Color.White,
        surface = activeTheme.surface,
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1B2940),
        onSurfaceVariant = Color(0xFFC3D0E2),
        outline = Color(0xFF52647C),
        error = Red,
        onError = Night
    )
}

CompositionLocalProvider(
    LocalCurrency provides currency,
    LocalDateFormat provides dateFormat
) {
    MaterialTheme(colorScheme = appColorScheme) {

if (pendingPrivatePage != null) {
    AlertDialog(
        onDismissRequest = {
            pendingPrivatePage = null
            privatePinInput = ""
            privatePinError = ""
        },
        title = {
            Text("قفل بخش خصوصی")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("برای ورود، رمز چهاررقمی را وارد کن.")

                OutlinedTextField(
                    value = privatePinInput,
                    onValueChange = {
                        if (
                            it.length <= 4 &&
                            it.all(Char::isDigit)
                        ) {
                            privatePinInput = it
                            privatePinError = ""
                        }
                    },
                    label = { Text("رمز ورود") },
                    singleLine = true,
                    visualTransformation =
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType =
                                androidx.compose.ui.text.input.KeyboardType.Number
                        )
                )

                if (privatePinError.isNotEmpty()) {
                    Text(
                        privatePinError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val salt = themePreferences.getString(
                        "private_pin_salt",
                        ""
                    ) ?: ""

                    val savedHash = themePreferences.getString(
                        "private_pin_hash",
                        ""
                    ) ?: ""

                    val enteredHash = if (
                        privatePinInput.length == 4 &&
                        salt.isNotBlank()
                    ) {
                        hashPrivatePin(privatePinInput, salt)
                    } else {
                        ""
                    }

                    if (
                        enteredHash.isNotEmpty() &&
                        enteredHash == savedHash
                    ) {
                        val targetPage = pendingPrivatePage

                        if (targetPage != null) {
    navigateTo(targetPage)
                        }

                        pendingPrivatePage = null
                        privatePinInput = ""
                        privatePinError = ""
                    } else {
                        privatePinInput = ""
                        privatePinError = "رمز واردشده اشتباه است."
                    }
                }
            ) {
                Text("ورود")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    pendingPrivatePage = null
                    privatePinInput = ""
                    privatePinError = ""
                }
            ) {
                Text("انصراف")
            }
        }
    )
}



                ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
    modifier = Modifier
        .fillMaxHeight()
        .width(200.dp),
    drawerContainerColor = activeTheme.surface,
    drawerContentColor = MaterialTheme.colorScheme.onSurface
) {
                    

                    Column(
                        Modifier.fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
drawerGroups.forEach { (groupTitle, groupPages) ->

    val isExpanded = groupTitle != "خانه" && groupTitle in expandedDrawerGroups

    Row(
        modifier = Modifier
    .fillMaxWidth()
    .clickable {
        if (groupTitle == "خانه") {
            navigateTo("خانه")
            scope.launch {
                drawerState.close()
            }
        } else {
            expandedDrawerGroups =
                if (isExpanded) {
                    expandedDrawerGroups - groupTitle
                } else {
                    expandedDrawerGroups + groupTitle
                }
        }
    }
    .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (groupTitle) {
                "خانه" -> Icons.Default.Home
                "تراکنش‌ها" -> Icons.Default.SwapHoriz
                "حساب‌ها و بانک‌ها" -> Icons.Default.AccountBalance
                "قرض و بدهی" -> Icons.Default.Favorite
                "پس‌انداز" -> Icons.Default.Savings
                "بودجه و هشدارها" -> Icons.Default.PieChart
                "گزارش‌ها" -> Icons.Default.Assessment
                "ابزارها" -> Icons.Default.Calculate
                else -> Icons.Default.Settings
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = groupTitle,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

                if (groupTitle != "خانه") {
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.ExpandLess
                } else {
                    Icons.Default.ExpandMore
                },
                contentDescription = null
            )
                }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant
    )

    if (isExpanded) {
        groupPages.forEach { item ->

            NavigationDrawerItem(
                label = {
                    Text(item)
                },
                selected = page == item,
                onClick = {
                    val lockKey = when (item) {
                        "گزارش‌ها و PDF" -> "lock_reports"
                        "قرض و بدهی" -> "lock_debts"
                        "جستجو و تاریخچه" -> "lock_history"
                        "پس‌انداز" -> "lock_savings"
                        "مدیریت بانک‌ها" -> "lock_banks"
                        "ماشین‌حساب" -> "lock_calculator"
                        else -> null
                    }

                    if (
                        lockKey != null &&
                        themePreferences.getBoolean(lockKey, false)
                    ) {
                        pendingPrivatePage = item
                        privatePinInput = ""
                        privatePinError = ""
                    } else {
                        navigateTo(item)
                    }

                    scope.launch {
                        drawerState.close()
                    }
                },
                icon = {
    Icon(
        imageVector = Icons.Default.Circle,
        contentDescription = null,
        modifier = Modifier.size(5.dp),
tint = MaterialTheme.colorScheme.primary
    )
},
                modifier = Modifier.padding(
                    start = 24.dp,
                    end = 10.dp
                )
            )
        }
    }
}

                }
            }
            }
        ) {
        Scaffold(
            containerColor = activeTheme.background,
            topBar = {
                TopAppBar(
                    title = {
    if (page == "خانه") {
        Text(
            "مدیریت مالی",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
},
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                                        Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Box(
                                    Modifier
                                        .width(24.dp)
                                        .height(3.dp)
                                        .background(activeTheme.primary, RoundedCornerShape(2.dp))
                                )
                                Box(
                                    Modifier
                                        .width(17.dp)
                                        .height(3.dp)
                                        .background(activeTheme.primary, RoundedCornerShape(2.dp))
                                )
                                Box(
                                    Modifier
                                        .width(10.dp)
                                        .height(3.dp)
                                        .background(activeTheme.primary, RoundedCornerShape(2.dp))
                                )
                                                        }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
    containerColor = activeTheme.background
)
                    )
            }
        ) { padding ->
            

            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (page == "خانه") {Text(
    "داشبورد مالی",
    color = MaterialTheme.colorScheme.primary,
    fontSize = 22.sp,
    fontWeight = FontWeight.Bold
)
                    if (dailyBudgetLimit > 0L) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "بودجه روزانه",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
) {
    Text(
        "مصرف‌شده: ${budgetPercentage}٪",
        color = budgetColor,
        fontWeight = FontWeight.Bold
    )

    Text(
        "سقف: ${displayMoney(dailyBudgetLimit)}",
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
            }

            LinearProgressIndicator(
                progress = { budgetProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = budgetColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                if (todayExpenses >= dailyBudgetLimit) {
                    "بودجه امروز به پایان رسیده است."
                } else {
                    "مانده بودجه: ${displayMoney(dailyBudgetLimit - todayExpenses)}"
                },color = if (budgetPercentage >= 100)
    Red
else
    MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
    "موجودی محاسبه‌شده",
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize = 14.sp
)
                            Text(
    displayMoney(income - expenses),
    color = MaterialTheme.colorScheme.onSurface,
    fontSize = 27.sp,
    fontWeight = FontWeight.Bold
)
                            Text(
    "بر اساس تراکنش‌های ثبت‌شده در این دستگاه",
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize = 11.sp
)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryCard("مجموع درآمد", displayMoney(income), Green, Modifier.weight(1f))
                        SummaryCard("مجموع هزینه", displayMoney(expenses), Red, Modifier.weight(1f))
                    }
                    Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    shape = RoundedCornerShape(20.dp)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "عملکرد این ماه",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("درآمد ماه", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(displayMoney(monthlyIncome), color = Green, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("هزینه ماه", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(displayMoney(monthlyIncome), color = Red, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            "خالص ماه: ${displayMoney(monthlyExpenses)}",
            color = if (monthlyBalance >= 0) Green else Red,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
                    }
                    Text("دسترسی سریع", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction("ثبت درآمد", Icons.Default.AddCircle, Green, Modifier.weight(1f)) { navigateTo("ثبت درآمد") }
QuickAction("ثبت هزینه", Icons.Default.RemoveCircle, Red, Modifier.weight(1f)) { navigateTo("ثبت هزینه") }
                    }
                    Text("تراکنش‌های اخیر", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    listOf("همه", "درآمد", "هزینه").forEach { type ->
        FilterChip(
            selected = transactionFilter == type,
            onClick = { transactionFilter = type },
            label = { Text(type) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
labelColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
                    }
                    if (transactions.isEmpty()) {
                        EmptyCard("هنوز تراکنشی ثبت نشده است.")
                    } else {
                        transactions
    .filter { transactionFilter == "همه" || it.type == transactionFilter }
    .take(10)
    .forEach { item ->
                            TransactionRow(
                                item = item,
                                onEdit = {
                                    editing = item
                                    editAmount = item.amount.toString()
                                    editCategory = item.category
                                    editNote = item.note
                                    editError = ""
                                },
                                onDelete = {
    if (confirmDelete) {
        deleting = item
    } else {
        db.delete(item.id)
        refresh++
    }
                                }
                            )
                        }
                    }
                } else if (page == "ثبت درآمد" || page == "ثبت هزینه") {
                    Text(page, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    TransactionForm(
    page = page,
    categories = db.getCategories(
        if (page == "ثبت درآمد") "درآمد" else "هزینه"
    ),
    onSave = { amount, category, note ->
    val transactionType =
        if (page == "ثبت درآمد") "درآمد" else "هزینه"

    db.add(transactionType, amount, category, note)

    if (transactionType == "هزینه") {
        val today = SimpleDateFormat(
            "yyyy/MM/dd",
            Locale("fa", "IR")
        ).format(Date())

        val dailyLimit = db.getDailyLimit()

        if (dailyLimit > 0L) {
            val todayExpenses = db.all()
                .filter {
                    it.type == "هزینه" &&
                    it.date.startsWith(today)
                }
                .sumOf { it.amount }

            val percentage = (
                todayExpenses.toDouble() /
                dailyLimit.toDouble() * 100
            ).toInt().coerceAtLeast(0)

            
val alerts = db.getBudgetAlertsForPercentage(
    percentage,
    today
)

alerts.forEach { alert ->
    val data = Data.Builder()
        .putLong("alert_id", alert.id)
        .putInt("percentage", alert.percentage)
        .putString("alert_type", alert.alertType)
        .putString("sound_uri", alert.soundUri)
        .putInt("duration", alert.durationSeconds)
        .putBoolean("vibration", alert.vibration)
        .build()

    val request = OneTimeWorkRequestBuilder<BudgetAlertWorker>()
        .setInputData(data)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "budget-alert-${alert.id}-$today",
        ExistingWorkPolicy.KEEP,
        request
    )
}

        }
    }

        
    navigateTo("خانه")
})
                } else {
                    Text(page, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    when (page) {
                        "گزارش‌ها و PDF" -> {
    ReportScreen(
        transactions = monthlyTransactions,
        month = currentMonth,
        onExport = {
            pdfLauncher.launch("گزارش مالی $currentMonth.pdf")
        }
    )
                        }
                        "پس‌انداز" -> {
    SavingsScreen(db, refresh) {
        refresh++
    }
                        }
                        "قرض و بدهی" -> {
    DebtScreen(db, refresh) {
        refresh++
    }
                        }
                        "مدیریت بانک‌ها" -> {
    BankManagementScreen(db, refresh) {
        refresh++
    }
                        }
                        "ماشین‌حساب" -> {
    FinanceCalculator()
                        }
                                                "جستجو و تاریخچه" -> {
                            AdvancedTransactionSearch(
                                transactions = transactions,
                                onEdit = { item ->
                                    editing = item
                                    editAmount = item.amount.toString()
                                    editCategory = item.category
                                    editNote = item.note
                                    editError = ""
                                },
                                onDelete = { item ->
                                    if (confirmDelete) {
                                        deleting = item
                                    } else {
                                        db.delete(item.id)
                                        refresh++
                                    }
                                }
                            )
                                                }
                        "دسته‌بندی‌ها" -> {
    CategoryManager(db) {
        refresh++
    }
}

"بودجه‌بندی" -> {
    BudgetScreen(db, transactions)
}

"تنظیم هشدارها" -> {
    BudgetAlertsScreen(db)
}
"قفل بخش‌های خصوصی" -> {
    PrivateSectionLockScreen(themePreferences)
}

"تنظیمات" -> {
    SettingsScreen(themePreferences)
}

"شخصی‌سازی" -> {
    AppThemeScreen(
        selectedThemeId = selectedThemeId,
        onThemeSelected = { themeId ->
            selectedThemeId = themeId
            themePreferences.edit()
                .putString("theme_id", themeId)
                .apply()
        },
        selectedIconId = selectedIconId,
        onIconSelected = { iconId ->
            selectedIconId = iconId
            themePreferences.edit()
                .putString("icon_id", iconId)
                .apply()
            applyLauncherIcon(context, iconId)
        }
    )
}
"پشتیبانی هوشمند" -> {
    SmartSupportScreen()
}
"پشتیبان‌گیری" -> {
    BackupScreen(
        message = backupMessage,
        onCreateBackup = {
            backupMessage = ""
            backupLauncher.launch("ModiriatMali_Backup.zip")
        },
        onRestore = {
            backupMessage = ""
            restoreLauncher.launch(
    arrayOf("*/*")
)
        }
    )
}

else -> EmptyCard("این بخش هنوز در دست ساخت است. اطلاعات مالی شما در این نسخه در بخش ثبت تراکنش‌ها ذخیره می‌شود.")
                        
    
                    }
                }
            }

            editing?.let { item ->
                AlertDialog(
                    onDismissRequest = { editing = null },
                    title = { Text("ویرایش تراکنش", color = MaterialTheme.colorScheme.primary) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
    if (item.type == "درآمد") "نوع: درآمد" else "نوع: هزینه",
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
                            OutlinedTextField(
                                value = editAmount,
                                onValueChange = { editAmount = toEnglishDigits(it).filter(Char::isDigit); editError = "" },
                                label = { Text("مبلغ به تومان") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editCategory,
                                onValueChange = { editCategory = it; editError = "" },
                                label = { Text("دسته یا دلیل") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editNote,
                                onValueChange = { editNote = it },
                                label = { Text("توضیحات") }
                            )
                            if (editError.isNotBlank()) Text(editError, color = Red)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val amount = editAmount.toLongOrNull()
                            when {
                                amount == null || amount <= 0 -> editError = "مبلغ معتبر وارد کن."
                                editCategory.isBlank() -> editError = "دسته یا دلیل را وارد کن."
                                else -> {
                                    db.update(item.copy(
                                        amount = amount,
                                        category = editCategory.trim(),
                                        note = editNote.trim()
                                    ))
                                    refresh++
                                    editing = null
                                }
                            }
                        }) { Text("ذخیره تغییرات") }
                    },
                    dismissButton = {
                        TextButton(onClick = { editing = null }) { Text("انصراف") }
                    },
                    containerColor = Color(0xFF17263D),
tonalElevation = 8.dp,
shape = RoundedCornerShape(24.dp)
                )
            }

            deleting?.let { item ->
                AlertDialog(
                    onDismissRequest = { deleting = null },
                    title = { Text("حذف تراکنش", color = Red) },
                    text = {
                        Text(
                            "از حذف این ${item.type} به مبلغ ${displayMoney(item.amount)
                            } مطمئنی؟ این کار قابل بازگشت نیست.",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            db.delete(item.id)
                            refresh++
                            deleting = null
                        }) { Text("بله، حذف شود", color = Red) }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleting = null }) { Text("انصراف") }
                    },
                    containerColor = Color(0xFF17263D),
tonalElevation = 8.dp,
shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}
}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionForm(
    page: String,
    categories: List<String>,
    onSave: (Long, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }



    OutlinedTextField(
        value = amountText,
        onValueChange = {
            amountText = toEnglishDigits(it).filter(Char::isDigit)
            error = ""
        },
        label = {
    Text(
    "مبلغ به تومان",
    color = MaterialTheme.colorScheme.primary
)
},
modifier = Modifier.fillMaxWidth(),
shape = RoundedCornerShape(16.dp),
colors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
unfocusedTextColor = MaterialTheme.colorScheme.onSurface
),
singleLine = true
    )

    

Box(Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = category,
        onValueChange = {},
        readOnly = true,
        label = {
            Text(
                if (page == "ثبت درآمد") "دسته درآمد" else "دسته هزینه"
            )
        },
        trailingIcon = {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "انتخاب دسته",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        categories.forEach { item ->
            DropdownMenuItem(
                text = { Text(item) },
                onClick = {
                    category = item
                    customCategory = ""
                    expanded = false
                    error = ""
                }
            )
        }
    }
}



    if (category == "سایر") {
        OutlinedTextField(
            value = customCategory,
            onValueChange = {
                customCategory = it
                error = ""
            },
            label = { Text("نام دسته دلخواه") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }

    
OutlinedTextField(
    value = note,
    onValueChange = { note = it },
    label = {
        Text("توضیحات (اختیاری)")
    },
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )
)


    if (error.isNotBlank()) Text(error, color = Red)

    Button(
        onClick = {
            val amount = amountText.toLongOrNull()
            val finalCategory =
                if (category == "سایر") customCategory.trim() else category

            when {
                amount == null || amount <= 0 ->
                    error = "مبلغ معتبر وارد کن."
                category.isBlank() ->
                    error = "لطفاً یک دسته انتخاب کن."
                finalCategory.isBlank() ->
                    error = "نام دسته دلخواه را وارد کن."
                else -> onSave(amount, finalCategory, note.trim())
            }
        },
        modifier = Modifier
    .fillMaxWidth(),
shape = RoundedCornerShape(18.dp),
colors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
contentColor = MaterialTheme.colorScheme.onPrimary
),
elevation = ButtonDefaults.buttonElevation(
    defaultElevation = 5.dp
)
) {
    Text(
        "ذخیره تراکنش",
        fontWeight = FontWeight.Bold
    )
    }
    

    Text(
        "مبلغ‌ها به تومان ثبت می‌شوند و روی همین گوشی ذخیره می‌مانند.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
}

@Composable
private fun CategoryManager(
    db: FinanceDb,
    onChanged: () -> Unit
) {
    var type by remember { mutableStateOf("هزینه") }
    var refresh by remember { mutableIntStateOf(0) }
    var newName by remember { mutableStateOf("") }
    var editingName by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val categories = remember(type, refresh) {
        db.getCategories(type)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { type = "هزینه"; message = "" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "هزینه") MaterialTheme.colorScheme.primary
else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("دسته‌های هزینه")
            }

            Button(
                onClick = { type = "درآمد"; message = "" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "درآمد") MaterialTheme.colorScheme.primary
else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("دسته‌های درآمد")
            }
        }

        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it; message = "" },
            label = { Text("نام دسته جدید") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                if (newName.isBlank()) {
                    message = "نام دسته را وارد کن."
                } else if (db.addCategory(type, newName.trim())) {
                    newName = ""
                    refresh++
                    onChanged()
                    message = "دسته جدید اضافه شد."
                } else {
                    message = "این دسته قبلاً وجود دارد."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("افزودن دسته")
        }

        if (message.isNotBlank()) {
            Text(message, color = MaterialTheme.colorScheme.primary)
        }

        Text(
            "فهرست دسته‌های ${type}",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        categories.forEach { name ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        IconButton(
                            onClick = {
                                editingName = name
                                editText = name
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                "ویرایش",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                if (db.deleteCategory(type, name)) {
                                    refresh++
                                    onChanged()
                                    message = "دسته حذف شد."
                                } else {
                                    message = "دسته «سایر» قابل حذف نیست."
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                "حذف",
                                tint = Red
                            )
                        }
                    }

                    Text(name, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    editingName?.let { oldName ->
        AlertDialog(
            onDismissRequest = { editingName = null },
            title = { Text("تغییر نام دسته") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("نام جدید") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editText.isNotBlank()) {
                            if (db.renameCategory(
                                    type,
                                    oldName,
                                    editText.trim()
                                )
                            ) {
                                refresh++
                                onChanged()
                                message = "نام دسته تغییر کرد."
                                editingName = null
                            } else {
                                message = "تغییر نام انجام نشد؛ شاید نام تکراری باشد."
                            }
                        }
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingName = null }) {
                    Text("انصراف")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun AdvancedTransactionSearch(
    transactions: List<MoneyTransaction>,
    onEdit: (MoneyTransaction) -> Unit,
    onDelete: (MoneyTransaction) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var minAmount by remember { mutableStateOf("") }
    var maxAmount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("همه") }

    val filtered = transactions.filter { item ->
        val query = searchText.trim()
        val categoryQuery = categoryText.trim()
        val dateQuery = dateText.trim()

        val min = toEnglishDigits(minAmount).toLongOrNull()
        val max = toEnglishDigits(maxAmount).toLongOrNull()

        val matchesType =
            selectedType == "همه" || item.type == selectedType

        val matchesSearch =
            query.isBlank() ||
            item.category.contains(query, ignoreCase = true) ||
            item.note.contains(query, ignoreCase = true) ||
            item.amount.toString().contains(toEnglishDigits(query)) ||
            item.date.contains(query)

        val matchesCategory =
            categoryQuery.isBlank() ||
            item.category.contains(categoryQuery, ignoreCase = true)

        val matchesDate =
            dateQuery.isBlank() || item.date.contains(dateQuery)

        val matchesMin = min == null || item.amount >= min
        val matchesMax = max == null || item.amount <= max

        matchesType && matchesSearch &&
            matchesCategory && matchesDate &&
            matchesMin && matchesMax
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("جست‌وجو در مبلغ، توضیحات و دسته") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("نوع تراکنش", color = MaterialTheme.colorScheme.onSurface)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("همه", "درآمد", "هزینه").forEach { type ->
                Button(
                    onClick = { selectedType = type },
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (selectedType == type) MaterialTheme.colorScheme.primary
else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(type)
                }
            }
        }

        OutlinedTextField(
            value = categoryText,
            onValueChange = { categoryText = it },
            label = { Text("فیلتر دسته‌بندی") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            label = { Text("تاریخ (مثلاً ۱۴۰۵/۰۸)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = minAmount,
                onValueChange = {
                    minAmount = toEnglishDigits(it).filter(Char::isDigit)
                },
                label = { Text("حداقل مبلغ") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = maxAmount,
                onValueChange = {
                    maxAmount = toEnglishDigits(it).filter(Char::isDigit)
                },
                label = { Text("حداکثر مبلغ") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Text(
            "تعداد نتایج: ${filtered.size}",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        if (filtered.isEmpty()) {
            EmptyCard("تراکنشی با این مشخصات پیدا نشد.")
        } else {
            filtered.forEach { item ->
                TransactionRow(
                    item = item,
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: String,
    color: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            color.copy(alpha = 0.35f)
        )
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
color = MaterialTheme.colorScheme.onSurfaceVariant,
fontSize = 13.sp
            )
            Text(
                amount,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
        }
    }
}



@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            tint.copy(alpha = 0.4f)
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(34.dp)
            )

            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}



@Composable
private fun TransactionRow(
    item: MoneyTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = if (item.type == "درآمد") Green else Red

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = 0.25f)
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        "ویرایش",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "حذف",
                        tint = Red
                    )
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    item.category,
color = MaterialTheme.colorScheme.onSurface,
fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Text(
                    displayDate(item.date),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                if (item.note.isNotBlank()) {
                    Text(
                        item.note,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Text(
                    (if (item.type == "درآمد") "+" else "−") +
                        displayMoney(item.amount),
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}


@Composable
private fun BudgetScreen(
    db: FinanceDb,
    transactions: List<MoneyTransaction>
) {
    var limitText by remember {
        mutableStateOf(db.getDailyLimit().takeIf { it > 0 }?.toString() ?: "")
    }
    var message by remember { mutableStateOf("") }

    val today = SimpleDateFormat(
        "yyyy/MM/dd",
        Locale("fa", "IR")
    ).format(Date())

    val todayExpenses = transactions
        .filter {
            it.type == "هزینه" &&
            it.date.startsWith(today)
        }
        .sumOf { it.amount }

    val dailyLimit = limitText.toLongOrNull() ?: 0L
    val remaining = (dailyLimit - todayExpenses).coerceAtLeast(0L)

    val progress = if (dailyLimit > 0) {
        (todayExpenses.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surface
),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "سقف هزینه روزانه",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = limitText,
                    onValueChange = {
                        limitText = toEnglishDigits(it).filter(Char::isDigit)
                        message = ""
                    },
                    label = { Text("مبلغ به تومان") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val amount = limitText.toLongOrNull()
                        if (amount == null || amount <= 0L) {
                            message = "یک مبلغ معتبر وارد کن."
                        } else {
                            db.setDailyLimit(amount)
                            message = "سقف هزینه روزانه ذخیره شد."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ذخیره سقف روزانه")
                }

                if (message.isNotBlank()) {
                    Text(
    message,
    color = MaterialTheme.colorScheme.primary
)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surface
),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "وضعیت هزینه‌های امروز",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
Text(
    "مجموع هزینه امروز",
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
                Text(
                    displayMoney(todayExpenses),
                    color = Red,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
Text(
    "باقی‌مانده بودجه",
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
                Text(
                    if (dailyLimit > 0) displayMoney(remaining)
                
            else "سقف بودجه تعیین نشده",
                    color = Green,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    if (dailyLimit > 0)
                        "درصد مصرف: ${(todayExpenses.toDouble() / dailyLimit * 100).toInt()}٪"
                    else "درصد مصرف: نامشخص",
                    color = MaterialTheme.colorScheme.onSurface
                )

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (todayExpenses >= dailyLimit && dailyLimit > 0)
    Red
else
    MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                if (dailyLimit > 0 && todayExpenses > dailyLimit) {
                    Text(
                        "هشدار: از سقف هزینه روزانه عبور کرده‌ای!",
                        color = Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
@Composable
private fun BudgetAlertsScreen(db: FinanceDb) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf(db.getBudgetAlerts()) }
    var percentage by remember { mutableStateOf("85") }
    var alertType by remember { mutableStateOf("صدا") }
    var duration by remember { mutableStateOf("5") }
    var editingAlert by remember {
    mutableStateOf<BudgetAlert?>(null)
    }
var soundUri by remember {
    mutableStateOf("")
}

val soundPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    if (uri != null) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            soundUri = uri.toString()
        } catch (e: Exception) {
            soundUri = uri.toString()
        }
    }
}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "تنظیم هشدارهای بودجه",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "برای هر درصد مصرف بودجه می‌توانی هشدار جداگانه بسازی.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = percentage,
            onValueChange = {
                percentage = it.filter(Char::isDigit).take(3)
            },
            label = { Text("درصد هشدار (۱ تا ۱۰۰)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = duration,
            onValueChange = {
                duration = it.filter(Char::isDigit).take(2)
            },
            label = { Text("مدت هشدار به ثانیه") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
    "نوع هشدار",
    color = MaterialTheme.colorScheme.onSurface
)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("صدا", "فقط لرزش", "بی‌صدا").forEach { type ->
                FilterChip(
                    selected = alertType == type,
                    onClick = { alertType = type },
                    label = { Text(type) }
                )
            }
        }
        if (alertType == "صدا") {
            OutlinedButton(
                onClick = {
                    soundPicker.launch(arrayOf("audio/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (soundUri.isBlank()) {
                        "انتخاب صدای دلخواه"
                    } else {
                        "تغییر صدای انتخاب‌شده"
                    }
                )
            }

            if (soundUri.isNotBlank()) {
                Text(
                    "صدای دلخواه انتخاب شده است",
                    color = Green
                )

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            playBudgetAlert(
                                context,
                                BudgetAlert(
                                    id = 0,
                                    percentage = 0,
                                    enabled = true,
                                    alertType = "صدا",
                                    soundUri = soundUri,
                                    durationSeconds = 3,
                                    vibration = false
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("پخش آزمایشی صدا 🔊")
                }
            }
        }

        Button(
            onClick = {
                val p = percentage.toIntOrNull()
                val d = duration.toIntOrNull()

                if (p != null && p in 1..100 && d != null && d in 1..60) {
                    val currentAlert = editingAlert

                    if (currentAlert != null) {
                        db.updateBudgetAlert(
                            currentAlert.copy(
                                percentage = p,
                                alertType = alertType,
                                soundUri = soundUri,
                                durationSeconds = d,
                                vibration = alertType != "بی‌صدا"
                            )
                        )
                        editingAlert = null
                    } else {
                        db.addBudgetAlert(
                            percentage = p,
                            alertType = alertType,
                            durationSeconds = d,
                            vibration = alertType != "بی‌صدا",
                            soundUri = soundUri
                        )
                    }

                    alerts = db.getBudgetAlerts()
                    percentage = "85"
                    duration = "5"
                    alertType = "صدا"
                    soundUri = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (editingAlert != null) "ذخیره ویرایش" else "افزودن هشدار")
        }

if (editingAlert != null) {
    OutlinedButton(
        onClick = {
            editingAlert = null
            percentage = "85"
            duration = "5"
            alertType = "صدا"
            soundUri = ""
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("لغو ویرایش")
    }
}
        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
    "هشدارهای ثبت‌شده",
    color = MaterialTheme.colorScheme.primary,
    fontSize = 18.sp
)

        if (alerts.isEmpty()) {
            Text(
    "هنوز هشداری ثبت نشده است.",
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
        }

        alerts.forEach { alert ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surface
)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${alert.percentage}٪ مصرف بودجه",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "نوع: ${alert.alertType} | مدت: ${alert.durationSeconds} ثانیه",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Switch(
                            checked = alert.enabled,
                            onCheckedChange = {
                                db.setBudgetAlertEnabled(alert.id, it)
                                alerts = db.getBudgetAlerts()
                            }
                        )

                        Text(
                            if (alert.enabled) "فعال" else "غیرفعال",
                            color = if (alert.enabled) Green else Color.Gray
                        )

                        Spacer(modifier = Modifier.weight(1f))
TextButton(
    onClick = {
        editingAlert = alert
        percentage = alert.percentage.toString()
        alertType = alert.alertType
        duration = alert.durationSeconds.toString()
        soundUri = alert.soundUri
    }
) {
    Text(
    "ویرایش",
    color = MaterialTheme.colorScheme.primary
)
}
                        TextButton(
                            onClick = {
                                db.deleteBudgetAlert(alert.id)
                                alerts = db.getBudgetAlerts()
                            }
                        ) {
                            Text(
    "حذف",
    color = Red
)
                        }
                    }
                }
            }
        }
    }
}

private fun createPrivatePinSalt(): String {
    val bytes = ByteArray(16)
    java.security.SecureRandom().nextBytes(bytes)
    return android.util.Base64.encodeToString(
        bytes,
        android.util.Base64.NO_WRAP
    )
}

private fun hashPrivatePin(pin: String, salt: String): String {
    val saltBytes = android.util.Base64.decode(
        salt,
        android.util.Base64.NO_WRAP
    )

    val spec = javax.crypto.spec.PBEKeySpec(
        pin.toCharArray(),
        saltBytes,
        120000,
        256
    )

    val hash = javax.crypto.SecretKeyFactory
        .getInstance("PBKDF2WithHmacSHA1")
        .generateSecret(spec)
        .encoded

    spec.clearPassword()

    return android.util.Base64.encodeToString(
        hash,
        android.util.Base64.NO_WRAP
    )
}

@Composable
private fun PrivateSectionLockScreen(
    preferences: android.content.SharedPreferences
) {
    val sections = listOf(
        "گزارش‌ها و PDF" to "lock_reports",
        "قرض و بدهی" to "lock_debts",
        "جستجو و تاریخچه" to "lock_history",
        "پس‌انداز" to "lock_savings",
        "مدیریت بانک‌ها" to "lock_banks",
        "ماشین‌حساب" to "lock_calculator"
    )

    var pinConfigured by remember {
        mutableStateOf(
            !preferences.getString("private_pin_hash", "").isNullOrBlank() &&
            !preferences.getString("private_pin_salt", "").isNullOrBlank()
        )
    }

    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var messageIsSuccess by remember { mutableStateOf(false) }

    var lockStates by remember {
        mutableStateOf(
            sections.associate { (name, key) ->
                key to preferences.getBoolean(key, false)
            }
        )
    }

    Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "قفل بخش‌های خصوصی",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            if (pinConfigured)
                "رمز چهاررقمی شما تنظیم شده است."
            else
                "برای فعال‌کردن قفل، ابتدا یک رمز چهاررقمی بسازید.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (pinConfigured) {
            OutlinedTextField(
                value = currentPin,
                onValueChange = {
                    if (it.length <= 4 && it.all(Char::isDigit)) {
                        currentPin = it
                        message = ""
                    }
                },
                label = { Text("رمز فعلی") },
                singleLine = true,
                visualTransformation =
                    androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions =
                    androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType =
                            androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = newPin,
            onValueChange = {
                if (it.length <= 4 && it.all(Char::isDigit)) {
                    newPin = it
                    message = ""
                }
            },
            label = {
                Text(if (pinConfigured) "رمز جدید" else "رمز چهاررقمی")
            },
            singleLine = true,
            visualTransformation =
                androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType =
                        androidx.compose.ui.text.input.KeyboardType.Number
                ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = confirmPin,
            onValueChange = {
                if (it.length <= 4 && it.all(Char::isDigit)) {
                    confirmPin = it
                    message = ""
                }
            },
            label = { Text("تکرار رمز") },
            singleLine = true,
            visualTransformation =
                androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType =
                        androidx.compose.ui.text.input.KeyboardType.Number
                ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val savedSalt = preferences.getString(
                    "private_pin_salt", ""
                ) ?: ""

                val savedHash = preferences.getString(
                    "private_pin_hash", ""
                ) ?: ""

                when {
                    pinConfigured && currentPin.length != 4 -> {
                        message = "رمز فعلی باید چهار رقم باشد."
                        messageIsSuccess = false
                    }

                    pinConfigured &&
                        hashPrivatePin(currentPin, savedSalt) != savedHash -> {
                        message = "رمز فعلی اشتباه است."
                        messageIsSuccess = false
                    }

                    newPin.length != 4 -> {
                        message = "رمز جدید باید دقیقاً چهار رقم باشد."
                        messageIsSuccess = false
                    }

                    newPin != confirmPin -> {
                        message = "تکرار رمز با رمز جدید مطابقت ندارد."
                        messageIsSuccess = false
                    }

                    else -> {
                        val salt = createPrivatePinSalt()

                        preferences.edit()
                            .putString("private_pin_salt", salt)
                            .putString(
                                "private_pin_hash",
                                hashPrivatePin(newPin, salt)
                            )
                            .apply()

                        pinConfigured = true
                        currentPin = ""
                        newPin = ""
                        confirmPin = ""
                        message = "رمز با موفقیت ذخیره شد."
                        messageIsSuccess = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (pinConfigured) "تغییر رمز" else "ذخیره رمز")
        }

        if (message.isNotBlank()) {
            Text(
                message,
                color = if (messageIsSuccess)
                    Green
                else
                    MaterialTheme.colorScheme.error
            )
        }

        HorizontalDivider()

        Text(
            "انتخاب بخش‌های قفل‌شونده",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        sections.forEach { (name, key) ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name)

                    Switch(
                        checked = lockStates[key] == true,
                        enabled = pinConfigured,
                        onCheckedChange = { enabled ->
                            preferences.edit()
                                .putBoolean(key, enabled)
                                .apply()

                            lockStates = lockStates.toMutableMap().apply {
                                put(key, enabled)
                            }
                        }
                    )
                }
            }
        }

        if (!pinConfigured) {
            Text(
                "ابتدا رمز چهاررقمی بساز تا کلیدهای قفل فعال شوند.",
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    shape = RoundedCornerShape(16.dp)
) {
        Text(
    message,
    Modifier.fillMaxWidth().padding(22.dp),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center
)
    }
}
@Composable
private fun SettingsScreen(
    preferences: android.content.SharedPreferences
) {
    var currency by remember {
        mutableStateOf(preferences.getString("currency", "تومان") ?: "تومان")
    }

    var startPage by remember {
        mutableStateOf(preferences.getString("start_page", "خانه") ?: "خانه")
    }

    var confirmDelete by remember {
        mutableStateOf(preferences.getBoolean("confirm_delete", true))
    }

    var dateFormat by remember {
        mutableStateOf(preferences.getString("date_format", "شمسی") ?: "شمسی")
    }

    var showResetDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val startPages = listOf(
        "خانه",
        "ثبت درآمد",
        "ثبت هزینه",
        "مدیریت بانک‌ها",
        "قرض و بدهی",
        "پس‌انداز"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "تنظیمات عمومی",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("واحد پول", fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currency == "تومان",
                        onClick = {
                            currency = "تومان"
                            preferences.edit().putString("currency", currency).apply()
                        },
                        label = { Text("تومان") }
                    )

                    FilterChip(
                        selected = currency == "ریال",
                        onClick = {
                            currency = "ریال"
                            preferences.edit().putString("currency", currency).apply()
                        },
                        label = { Text("ریال") }
                    )
                }

                HorizontalDivider()

                Text("صفحه شروع برنامه", fontWeight = FontWeight.Bold)

                startPages.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = startPage == item,
                            onClick = {
                                startPage = item
                                preferences.edit().putString("start_page", item).apply()
                            }
                        )
                        Text(item)
                    }
                }

                HorizontalDivider()

                Text("تأیید قبل از حذف اطلاعات", fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (confirmDelete) "فعال" else "غیرفعال")
                    Switch(
                        checked = confirmDelete,
                        onCheckedChange = {
                            confirmDelete = it
                            preferences.edit().putBoolean("confirm_delete", it).apply()
                        }
                    )
                }

                HorizontalDivider()

                Text("فرمت تاریخ", fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = dateFormat == "شمسی",
                        onClick = {
                            dateFormat = "شمسی"
                            preferences.edit().putString("date_format", dateFormat).apply()
                        },
                        label = { Text("شمسی") }
                    )

                    FilterChip(
                        selected = dateFormat == "میلادی",
                        onClick = {
                            dateFormat = "میلادی"
                            preferences.edit().putString("date_format", dateFormat).apply()
                        },
                        label = { Text("میلادی") }
                    )
                }

                HorizontalDivider()

                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("بازنشانی تنظیمات")
                }

                if (message.isNotBlank()) {
                    Text(message, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("بازنشانی تنظیمات") },
            text = {
                Text("تنظیمات عمومی به حالت پیش‌فرض برگردند؟ اطلاعات مالی حذف نمی‌شوند.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferences.edit()
                            .remove("currency")
                            .remove("start_page")
                            .remove("confirm_delete")
                            .remove("date_format")
                            .apply()

                        currency = "تومان"
                        startPage = "خانه"
                        confirmDelete = true
                        dateFormat = "شمسی"
                        message = "تنظیمات بازنشانی شدند."
                        showResetDialog = false
                    }
                ) {
                    Text("بله، بازنشانی کن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
@Composable
private fun AppThemeScreen(
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    selectedIconId: String,
    onIconSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "شخصی‌سازی ظاهر برنامه",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "قالب مورد علاقه‌ات رو انتخاب کن.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
Text(
    "انتخاب آیکون برنامه",
    color = MaterialTheme.colorScheme.primary,
    fontSize = 19.sp,
    fontWeight = FontWeight.Bold
)


val launcherIcons = listOf(
    Triple("wallet", "کیف پول", Icons.Default.AccountBalanceWallet),
    Triple("chart", "نمودار", Icons.Default.BarChart),
    Triple("coin", "سکه", Icons.Default.Paid),
    Triple("bank", "بانک", Icons.Default.AccountBalance),
    Triple("savings", "پس‌انداز", Icons.Default.Savings),
    Triple("receipt", "رسید مالی", Icons.Default.ReceiptLong),
    Triple("calculator", "ماشین‌حساب", Icons.Default.Calculate),
    Triple("card", "کارت بانکی", Icons.Default.CreditCard)

)


launcherIcons.forEach { item ->
    val selected = selectedIconId == item.first

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onIconSelected(item.first)
            },
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
    item.third,
    contentDescription = null,
    tint = MaterialTheme.colorScheme.primary,
    modifier = Modifier.size(40.dp)
)
                
                Text(item.second)
            }

            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
        appThemePresets.forEach { preset ->
            val selected = selectedThemeId == preset.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onThemeSelected(preset.id)
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = preset.surface
                ),
                border = if (selected) {
                    androidx.compose.foundation.BorderStroke(
                        2.dp,
                        preset.primary
                    )
                } else null
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            preset.name,
                            color = preset.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        if (selected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = preset.primary
                            )
                        }
                    }

                    Text(
                        preset.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                            
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            preset.background,
                            preset.surface,
                            preset.primary,
                            preset.secondary
                        ).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color,
                                        RoundedCornerShape(10.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun FinanceCalculator() {
    val calculatorPrefs = LocalContext.current.getSharedPreferences(
    "calculator_history",
    Context.MODE_PRIVATE
)

var calculationHistory by remember {
    mutableStateOf(
        runCatching {
            val saved = calculatorPrefs.getString("history", "[]") ?: "[]"
            val array = org.json.JSONArray(saved)

            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                item.getString("expression") to item.getString("result")
            }
        }.getOrDefault(emptyList())
    )
}
var historyExpanded by remember { mutableStateOf(false) }
    var display by remember { mutableStateOf("0") }
    var storedValue by remember { mutableStateOf<Double?>(null) }
    var pendingOperator by remember { mutableStateOf<String?>(null) }
    var startNewNumber by remember { mutableStateOf(false) }

    fun formatNumber(value: Double): String {
        if (!value.isFinite()) return "خطا"
        if (kotlin.math.abs(value) < 0.00000001) return "0"

        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.8f", value)
                .trimEnd('0')
                .trimEnd('.')
        }
    }

    fun calculate(a: Double, b: Double, op: String): Double? {
        return when (op) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            "÷" -> if (b == 0.0) null else a / b
            else -> b
        }
    }

    fun press(key: String) {
        when (key) {
            "AC" -> {
                display = "0"
                storedValue = null
                pendingOperator = null
                startNewNumber = false
            }

            "⌫" -> {
                if (!startNewNumber) {
                    display = if (display.length <= 1) "0"
                    else display.dropLast(1)
                }
            }

            "+/−" -> {
                if (display != "0" && display != "خطا") {
                    display = if (display.startsWith("-")) {
                        display.drop(1)
                    } else {
                        "-$display"
                    }
                }
            }

            "%" -> {
                val value = display.toDoubleOrNull() ?: 0.0
                display = formatNumber(value / 100.0)
            }

            "+", "−", "×", "÷" -> {
                val current = display.toDoubleOrNull() ?: return

                if (storedValue != null &&
                    pendingOperator != null &&
                    !startNewNumber
                ) {
                    val result = calculate(
                        storedValue!!,
                        current,
                        pendingOperator!!
                    )

                    if (result == null) {
                        display = "خطا"
                        storedValue = null
                        pendingOperator = null
                        return
                    }

                    storedValue = result
                    display = formatNumber(result)
                } else {
                    storedValue = current
                }

                pendingOperator = key
                startNewNumber = true
            }
"=" -> {
    val first = storedValue
    val op = pendingOperator
    val second = display.toDoubleOrNull()

    if (first != null && op != null && second != null) {
        val result = calculate(first, second, op)

        if (result != null) {
            val expression =
                "${formatNumber(first)} $op ${formatNumber(second)}"
            val resultText = formatNumber(result)

            calculationHistory =
                (listOf(expression to resultText) + calculationHistory).take(100)

            val jsonArray = org.json.JSONArray()
            calculationHistory.forEach { item ->
                jsonArray.put(
                    org.json.JSONObject()
                        .put("expression", item.first)
                        .put("result", item.second)
                )
            }

            calculatorPrefs.edit()
                .putString("history", jsonArray.toString())
                .apply()

            display = resultText
        } else {
            display = "خطا"
        }

        storedValue = null
        pendingOperator = null
        startNewNumber = true
    }
}
            
            "." -> {
                if (startNewNumber) {
                    display = "0."
                    startNewNumber = false
                } else if (!display.contains(".")) {
                    display += "."
                }
            }

            else -> {
                if (key.all { it.isDigit() }) {
                    if (startNewNumber || display == "0" || display == "خطا") {
                        display = key
                        startNewNumber = false
                    } else if (display.length < 15) {
                        display += key
                    }
                }
            }
        }
    }

    val rows = listOf(
        listOf("AC", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("+/−", "0", ".", "=")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.End
            ) {
                                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            calculationHistory = emptyList()
                            calculatorPrefs.edit()
                                .putString("history", "[]")
                                .apply()
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "پاک کردن حافظه",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
Text(
    if (storedValue != null && pendingOperator != null) {
        "${formatNumber(storedValue!!)}  $pendingOperator  ${if (startNewNumber) "" else display}"
    } else {
        ""
    },
    fontSize = 20.sp,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.End,
    modifier = Modifier.fillMaxWidth()
)

Spacer(modifier = Modifier.height(8.dp))
                Text(
                    display,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                
            }
                if (calculationHistory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(28.dp)
                            .clickable {
                                historyExpanded = !historyExpanded
                            }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount > 2f) {
                                        historyExpanded = true
                                    } else if (dragAmount < -2f) {
                                        historyExpanded = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(5.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                    RoundedCornerShape(50)
                                )
                        )
                    }

                    if (historyExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "حافظه محاسبات",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        calculationHistory.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(item.first, fontSize = 14.sp)
                                    Text(
                                        "= ${item.second}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        calculationHistory =
                                            calculationHistory.filterIndexed { i, _ ->
                                                i != index
                                            }

                                        val jsonArray = org.json.JSONArray()
                                        calculationHistory.forEach { historyItem ->
                                            jsonArray.put(
                                                org.json.JSONObject()
                                                    .put("expression", historyItem.first)
                                                    .put("result", historyItem.second)
                                            )
                                        }

                                        calculatorPrefs.edit()
                                            .putString("history", jsonArray.toString())
                                            .apply()
                                    }
                                ) {
                                    Text("×", fontSize = 22.sp)
                                }
                            }

                            if (index < calculationHistory.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
                }
                rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { key ->
                    Button(
                        onClick = { press(key) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.25f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (key) {
                                "+", "−", "×", "÷", "=" ->
                                    MaterialTheme.colorScheme.primary

                                "AC" ->
                                    MaterialTheme.colorScheme.errorContainer

                                else ->
                                    MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = when (key) {
                                "+", "−", "×", "÷", "=" ->
                                    MaterialTheme.colorScheme.onPrimary

                                "AC" ->
                                    MaterialTheme.colorScheme.onErrorContainer

                                else ->
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    ) {
                        Text(
                            key,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    }
@Composable
private fun DebtScreen(
    db: FinanceDb,
    refresh: Int,
    onChanged: () -> Unit
) {
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("طلب از دیگران") }
    var message by remember { mutableStateOf("") }

    val debts = remember(refresh) {
        db.getDebts()
    }

    var paidInputs by remember(refresh) {
        mutableStateOf(
            debts.associate { it.id to it.paid.toString() }
        )
    }

    val totalRemaining = debts.sumOf { it.remaining }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "ثبت قرض و بدهی",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text("نوع حساب")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "طلب از دیگران",
                        onClick = { type = "طلب از دیگران" },
                        label = { Text("طلب از دیگران") }
                    )

                    FilterChip(
                        selected = type == "بدهی به دیگران",
                        onClick = { type = "بدهی به دیگران" },
                        label = { Text("بدهی به دیگران") }
                    )
                }

                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("نام شخص") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = toEnglishDigits(it).filter(Char::isDigit)
                    },
                    label = { Text("مبلغ به تومان") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val value = amount.toLongOrNull()

                        if (person.isBlank() || value == null || value <= 0L) {
                            message = "نام شخص و مبلغ معتبر وارد کن."
                        } else if (db.addDebt(person, type, value, note)) {
                            person = ""
                            amount = ""
                            note = ""
                            message = "با موفقیت ثبت شد."
                            onChanged()
                        } else {
                            message = "ثبت اطلاعات انجام نشد."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ثبت اطلاعات")
                }

                if (message.isNotBlank()) {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "مجموع مانده حساب‌ها",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    displayMoney(totalRemaining),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text("تعداد حساب‌ها: ${debts.size}")
            }
        }

        Text(
            "فهرست قرض و بدهی‌ها",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        if (debts.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "هنوز هیچ قرض یا بدهی ثبت نکرده‌ای.",
                    modifier = Modifier.padding(18.dp)
                )
            }
        }

        debts.forEach { debt ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        debt.person,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        debt.type,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text("مبلغ اولیه: ${displayMoney(debt.amount)}")
                    Text("پرداخت‌شده: ${displayMoney(debt.remaining)}")

                    Text(
                        "مانده: ${displayMoney(debt.amount)}",
                        fontWeight = FontWeight.Bold,
                        color = if (debt.remaining > 0L) Red else Green
                    )

                    if (debt.note.isNotBlank()) {
                        Text("توضیحات: ${debt.note}")
                    }

                    Text(
                        "تاریخ ثبت: ${debt.date}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    OutlinedTextField(
                        value = paidInputs[debt.id] ?: debt.paid.toString(),
                        onValueChange = {
                            paidInputs = paidInputs.toMutableMap().apply {
                                put(
                                    debt.id,
                                    toEnglishDigits(it).filter(Char::isDigit)
                                )
                            }
                        },
                        label = { Text("مجموع پرداخت یا دریافت‌شده") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val paid = paidInputs[debt.id]?.toLongOrNull()

                            if (paid == null || paid < 0L || paid > debt.amount) {
                                message = "مبلغ باید بین صفر و مبلغ اولیه باشد."
                            } else if (db.updateDebtPaid(debt.id, paid)) {
                                message = "مبلغ به‌روزرسانی شد."
                                onChanged()
                            } else {
                                message = "به‌روزرسانی انجام نشد."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ذخیره پرداخت")
                    }

                    TextButton(
                        onClick = {
                            db.deleteDebt(debt.id)
                            message = "حساب حذف شد."
                            onChanged()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "حذف این حساب",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun BankManagementScreen(
    db: FinanceDb,
    refresh: Int,
    onChanged: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val banks = remember(refresh) {
        db.getBanks()
    }

    var balanceInputs by remember(refresh) {
        mutableStateOf(
            banks.associate { it.id to it.balance.toString() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "افزودن حساب بانکی",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام بانک") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("شماره حساب یا کارت (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = balance,
                    onValueChange = {
                        balance = toEnglishDigits(it).filter(Char::isDigit)
                    },
                    label = { Text("موجودی اولیه به تومان") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val amount = balance.toLongOrNull() ?: 0L

                        if (name.isBlank()) {
                            message = "نام بانک را وارد کن."
                        } else if (db.addBank(name, accountNumber, amount)) {
                            name = ""
                            accountNumber = ""
                            balance = ""
                            message = "حساب بانکی اضافه شد."
                            onChanged()
                        } else {
                            message = "ثبت حساب انجام نشد."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("افزودن بانک")
                }

                if (message.isNotBlank()) {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Text(
            "حساب‌های بانکی",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        if (banks.isEmpty()) {
            Text("هنوز حساب بانکی ثبت نشده است.")
        }

        banks.forEach { bank ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        bank.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (bank.accountNumber.isNotBlank()) {
                        Text("شماره حساب: ${bank.accountNumber}")
                    }

                    OutlinedTextField(
                        value = balanceInputs[bank.id] ?: bank.balance.toString(),
                        onValueChange = {
                            balanceInputs = balanceInputs.toMutableMap().apply {
                                put(
                                    bank.id,
                                    toEnglishDigits(it).filter(Char::isDigit)
                                )
                            }
                        },
                        label = { Text("موجودی به تومان") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val amount = balanceInputs[bank.id]?.toLongOrNull()

                            if (amount == null) {
                                message = "موجودی معتبر وارد کن."
                            } else if (db.updateBankBalance(bank.id, amount)) {
                                message = "موجودی به‌روزرسانی شد."
                                onChanged()
                            } else {
                                message = "به‌روزرسانی انجام نشد."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ذخیره موجودی")
                    }

                    TextButton(
                        onClick = {
                            db.deleteBank(bank.id)
                            message = "حساب بانکی حذف شد."
                            onChanged()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "حذف حساب بانکی",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun SavingsScreen(
    db: FinanceDb,
    refresh: Int,
    onChanged: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var savedAmount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val goals = remember(refresh) {
        db.getSavingsGoals()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "ایجاد هدف پس‌انداز",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان هدف؛ مثلاً خرید گوشی") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = target,
                    onValueChange = {
                        target = toEnglishDigits(it).filter(Char::isDigit)
                    },
                    label = { Text("مبلغ هدف به تومان") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val amount = target.toLongOrNull()

                        if (title.isBlank() || amount == null || amount <= 0L) {
                            message = "عنوان و مبلغ معتبر وارد کن."
                        } else if (db.addSavingsGoal(title, amount)) {
                            title = ""
                            target = ""
                            message = "هدف پس‌انداز اضافه شد."
                            onChanged()
                        } else {
                            message = "ثبت هدف انجام نشد."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("افزودن هدف")
                }

                if (message.isNotBlank()) {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Text(
            "هدف‌های پس‌انداز من",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        if (goals.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "هنوز هدفی ثبت نکرده‌ای.",
                    modifier = Modifier.padding(18.dp)
                )
            }
        }

        goals.forEach { goal ->
            val progress = (
                goal.saved.toFloat() / goal.target.toFloat()
            ).coerceIn(0f, 1f)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        goal.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text("مبلغ هدف: ${displayMoney(goal.target)}")
                    Text("پس‌انداز شده: ${displayMoney(goal.saved)}")
                    Text(
                        "مانده تا هدف: ${displayMoney((goal.target - goal.saved).coerceAtLeast(0L))}"
                    )

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "پیشرفت: ${(progress * 100).toInt()}٪",
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = if (editingId == goal.id) savedAmount else "",
                        onValueChange = {
                            savedAmount = toEnglishDigits(it).filter(Char::isDigit)
                        },
                        label = { Text("مبلغ پس‌انداز فعلی به تومان") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                editingId = goal.id
                                val amount = savedAmount.toLongOrNull()
                                if (amount == null) {
                                    savedAmount = goal.saved.toString()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ویرایش مبلغ")
                        }

                        Button(
                            onClick = {
                                val amount = savedAmount.toLongOrNull()
                                if (editingId != goal.id || amount == null) {
                                    message = "ابتدا مبلغ را وارد کن و ویرایش را بزن."
                                } else if (db.updateSavingsAmount(goal.id, amount)) {
                                    editingId = null
                                    savedAmount = ""
                                    message = "مبلغ پس‌انداز به‌روزرسانی شد."
                                    onChanged()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ذخیره مبلغ")
                        }
                    }

                    TextButton(
                        onClick = {
                            db.deleteSavingsGoal(goal.id)
                            editingId = null
                            savedAmount = ""
                            onChanged()
                        }
                    ) {
                        Text("حذف این هدف", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
@Composable
private fun ReportScreen(
    transactions: List<MoneyTransaction>,
    month: String,
    onExport: () -> Unit
) {
    val income = transactions
        .filter { it.type == "درآمد" }
        .sumOf { it.amount }

    val expenses = transactions
        .filter { it.type == "هزینه" }
        .sumOf { it.amount }

    val balance = income - expenses

    Column(
        modifier = Modifier
    .fillMaxWidth()
    .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "گزارش مالی ماه $month",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("خلاصه عملکرد ماه", fontWeight = FontWeight.Bold)

                Text("مجموع درآمد: ${displayMoney(income)}", color = Green)
                Text("مجموع هزینه: ${displayMoney(expenses)}", color = Red)

                HorizontalDivider()

                Text(
                    "مانده نهایی: ${displayMoney(balance)}",
                    fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) Green else Red
                )

                Text("تعداد تراکنش‌ها: ${transactions.size}")
            }
        }

        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ذخیره گزارش به صورت PDF")
        }

        Text(
            "جزئیات تراکنش‌های این ماه",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        if (transactions.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "هنوز تراکنشی در این ماه ثبت نشده است.",
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            transactions.sortedByDescending { it.date }.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            item.type,
                            fontWeight = FontWeight.Bold,
                            color = if (item.type == "درآمد") Green else Red
                        )

                        Text("مبلغ: ${displayMoney(item.amount)}")
                        Text("دسته‌بندی: ${item.category}")

                        if (item.note.isNotBlank()) {
                            Text("توضیحات: ${item.note}")
                        }

                        Text(
                            "تاریخ: ${displayDate(item.date)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun buildPdfLayout(
    text: String,
    width: Int,
    textSize: Float,
    bold: Boolean
): StaticLayout {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        this.textSize = textSize
        typeface = Typeface.create(
            "sans-serif",
            if (bold) Typeface.BOLD else Typeface.NORMAL
        )
    }

    return StaticLayout.Builder.obtain(
        text,
        0,
        text.length,
        paint,
        width
    )
        .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
        .setTextDirection(TextDirectionHeuristics.RTL)
        .setIncludePad(false)
        .build()
}

private fun createMonthlyPdf(
    context: Context,
    uri: Uri,
    transactions: List<MoneyTransaction>,
    month: String,
    currency: String,
    dateFormat: String
) {
    val document = PdfDocument()

    var pageNumber = 0
    var currentPage: PdfDocument.Page? = null
    var canvas: Canvas? = null
    var y = 0f

    val income = transactions
        .filter { it.type == "درآمد" }
        .sumOf { it.amount }

    val expenses = transactions
        .filter { it.type == "هزینه" }
        .sumOf { it.amount }

    val balance = income - expenses

    fun startPage() {
        currentPage?.let { document.finishPage(it) }

        pageNumber++

        val info = PdfDocument.PageInfo.Builder(
            595,
            842,
            pageNumber
        ).create()

        currentPage = document.startPage(info)
        canvas = currentPage!!.canvas
        y = 45f

        val titleLayout = buildPdfLayout(
            "گزارش مالی ماه $month",
            515,
            21f,
            true
        )

        canvas!!.save()
        canvas!!.translate(40f, y)
        titleLayout.draw(canvas!!)
        canvas!!.restore()

        y += titleLayout.height + 25f
    }

    fun writeLine(
        text: String,
        size: Float = 14f,
        bold: Boolean = false
    ) {
        val layout = buildPdfLayout(text, 515, size, bold)

        if (y + layout.height > 790f) {
            startPage()
        }

        canvas!!.save()
        canvas!!.translate(40f, y)
        layout.draw(canvas!!)
        canvas!!.restore()

        y += layout.height + 12f
    }

    try {
        startPage()

        writeLine("خلاصه عملکرد ماه", 17f, true)
        writeLine("مجموع درآمد: ${money(income, currency)}")
        writeLine("مجموع هزینه: ${money(expenses, currency)}")
        writeLine("مانده نهایی: ${money(balance, currency)}", 15f, true)
        writeLine("تعداد تراکنش‌ها: ${transactions.size}")
        writeLine("----------------------------------------")

        writeLine("جزئیات تراکنش‌ها", 17f, true)

        if (transactions.isEmpty()) {
            writeLine("در این ماه هیچ تراکنشی ثبت نشده است.")
        } else {
            transactions.sortedByDescending { it.date }.forEach { item ->
                writeLine(
                    "${item.type} - ${money(item.amount, currency)}",
                    14f,
                    true
                )
                writeLine("دسته‌بندی: ${item.category}")

                if (item.note.isNotBlank()) {
                    writeLine("توضیحات: ${item.note}")
                }

                writeLine("تاریخ: ${formatDisplayDate(item.date, dateFormat)}")
                writeLine("----------------------------------------")
            }
        }

        currentPage?.let {
            document.finishPage(it)
            currentPage = null
        }

        val output = context.contentResolver.openOutputStream(uri)
            ?: throw Exception("امکان باز کردن فایل وجود ندارد.")

        output.use {
            document.writeTo(it)
        }

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        currentPage?.let {
            document.finishPage(it)
        }
        document.close()
    }
}
@Composable
private fun BackupScreen(
    message: String,
    onCreateBackup: () -> Unit,
    onRestore: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "پشتیبان‌گیری اطلاعات",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "از اطلاعات مالی و تنظیمات برنامه یک فایل پشتیبان تهیه کن.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onCreateBackup,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ایجاد فایل پشتیبان")
        }

        OutlinedButton(
            onClick = { showConfirm = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("بازیابی اطلاعات از فایل")
        }

        if (message.isNotBlank()) {
            Text(
                message,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            "توجه: بازیابی، اطلاعات فعلی برنامه را با اطلاعات فایل پشتیبان جایگزین می‌کند.",
            color = MaterialTheme.colorScheme.error
        )
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("تأیید بازیابی") },
            text = {
                Text("با بازیابی، اطلاعات مالی فعلی جایگزین می‌شوند. ادامه می‌دهی؟")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onRestore()
                    }
                ) {
                    Text("ادامه")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirm = false }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
}


private fun createFinanceBackup(
    context: Context,
    db: FinanceDb,
    preferences: android.content.SharedPreferences,
    uri: Uri
) {
    val databaseFile = context.getDatabasePath("finance.db")

    db.writableDatabase.rawQuery(
    "PRAGMA wal_checkpoint(FULL)",
    null
).use { cursor ->
    cursor.moveToFirst()
    }
    db.close()

    try {
        if (!databaseFile.exists() || databaseFile.length() == 0L) {
            throw Exception("فایل اطلاعات برنامه پیدا نشد.")
        }

        val settings = Properties()

        preferences.all.forEach { (key, value) ->
            when (value) {
                is String -> settings.setProperty(key, "S|$value")
                is Boolean -> settings.setProperty(key, "B|$value")
                is Int -> settings.setProperty(key, "I|$value")
                is Long -> settings.setProperty(key, "L|$value")
                is Float -> settings.setProperty(key, "F|$value")
            }
        }

        val output = context.contentResolver.openOutputStream(uri)
            ?: throw Exception("فایل قابل ذخیره نیست.")

        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("finance.db"))

            FileInputStream(databaseFile).use { input ->
                input.copyTo(zip)
            }

            zip.closeEntry()

            zip.putNextEntry(ZipEntry("settings.properties"))
            settings.store(zip, "Finance App Backup")
            zip.closeEntry()
        }
    } finally {
        db.writableDatabase
    }
}

private fun restoreFinanceBackup(
    context: Context,
    db: FinanceDb,
    preferences: android.content.SharedPreferences,
    uri: Uri
) {
    val tempDatabase = File(context.cacheDir, "restore_finance.db")
    val settings = Properties()
    var databaseFound = false

    val input = context.contentResolver.openInputStream(uri)
        ?: throw Exception("فایل قابل خواندن نیست.")

    ZipInputStream(input).use { zip ->
        var entry = zip.nextEntry

        while (entry != null) {
            when (entry.name) {
                "finance.db" -> {
                    FileOutputStream(tempDatabase).use { output ->
                        zip.copyTo(output)
                    }
                    databaseFound = true
                }

                "settings.properties" -> {
                    settings.load(zip)
                }
            }

            zip.closeEntry()
            entry = zip.nextEntry
        }
    }

    if (!databaseFound || tempDatabase.length() == 0L) {
    tempDatabase.delete()
    throw Exception("فایل پشتیبان معتبر نیست.")
}

val checkDatabase = SQLiteDatabase.openDatabase(
    tempDatabase.absolutePath,
    null,
    SQLiteDatabase.OPEN_READONLY
)
val databaseIsHealthy = checkDatabase.rawQuery(
    "PRAGMA quick_check",
    null
).use { cursor ->
    cursor.moveToFirst() && cursor.getString(0) == "ok"
}

if (!databaseIsHealthy) {
    checkDatabase.close()
    tempDatabase.delete()
    throw Exception("فایل پشتیبان خراب است و قابل بازیابی نیست.")
}
val hasTransactionsTable = checkDatabase.rawQuery(
    "SELECT name FROM sqlite_master WHERE type='table' AND name='transactions'",
    null
).use { cursor ->
    cursor.moveToFirst()
}

checkDatabase.close()

if (!hasTransactionsTable) {
    tempDatabase.delete()
    throw Exception("ساختار فایل پشتیبان معتبر نیست.")
}

if (settings.isEmpty) {
    tempDatabase.delete()
    throw Exception("تنظیمات داخل فایل پشتیبان پیدا نشد.")
}

db.close()

    val databaseFile = context.getDatabasePath("finance.db")
databaseFile.parentFile?.mkdirs()

val newDatabaseFile = File(databaseFile.parentFile, "finance_restore.db")

FileInputStream(tempDatabase).use { inputStream ->
    FileOutputStream(newDatabaseFile).use { output ->
        inputStream.copyTo(output)
        output.fd.sync()
    }
}

File(databaseFile.path + "-wal").delete()
File(databaseFile.path + "-shm").delete()

val oldDatabaseFile = File(
    databaseFile.parentFile,
    "finance_before_restore.db"
)

oldDatabaseFile.delete()

if (databaseFile.exists() && !databaseFile.renameTo(oldDatabaseFile)) {
    newDatabaseFile.delete()
    throw Exception("ذخیره اطلاعات قبلی ناموفق بود.")
}

if (!newDatabaseFile.renameTo(databaseFile)) {
    if (oldDatabaseFile.exists()) {
        oldDatabaseFile.renameTo(databaseFile)
    }
    newDatabaseFile.delete()
    throw Exception("بازیابی انجام نشد؛ اطلاعات قبلی حفظ شدند.")
}

oldDatabaseFile.delete()

tempDatabase.delete()

    db.writableDatabase

    val editor = preferences.edit().clear()

    settings.stringPropertyNames().forEach { key ->
        val value = settings.getProperty(key) ?: return@forEach
        val separator = value.indexOf('|')

        if (separator < 0) return@forEach

        val type = value.substring(0, separator)
        val content = value.substring(separator + 1)

        when (type) {
            "S" -> editor.putString(key, content)
            "B" -> editor.putBoolean(key, content.toBoolean())
            "I" -> editor.putInt(key, content.toInt())
            "L" -> editor.putLong(key, content.toLong())
            "F" -> editor.putFloat(key, content.toFloat())
        }
    }

    editor.apply()
}


@Composable
private fun SmartSupportScreen() {
    var searchText by remember { mutableStateOf("") }
    var selectedQuestion by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("همه") }

    data class HelpItem(
        val category: String,
        val title: String,
        val answer: String
    )

    val categories = listOf(
        "همه",
        "تراکنش‌ها",
        "حساب‌ها",
        "برنامه‌ریزی",
        "امنیت",
        "ابزارها"
    )

    val helpItems = listOf(
        HelpItem("تراکنش‌ها", "ثبت درآمد",
            "۱. وارد ثبت درآمد شو.\n۲. مبلغ درآمد را وارد کن.\n۳. دسته‌بندی و تاریخ را انتخاب کن.\n۴. اطلاعات را ذخیره کن."),
        HelpItem("تراکنش‌ها", "ثبت هزینه",
            "۱. وارد ثبت هزینه شو.\n۲. مبلغ هزینه را وارد کن.\n۳. دسته‌بندی مناسب را انتخاب کن.\n۴. تراکنش را ذخیره کن."),
        HelpItem("تراکنش‌ها", "جستجو و تاریخچه",
            "از بخش جستجو و تاریخچه می‌توانی تراکنش‌ها را بر اساس اطلاعات موجود جستجو و بررسی کنی."),
        HelpItem("حساب‌ها", "مدیریت بانک‌ها",
            "در این بخش حساب‌های بانکی را مدیریت کن و اطلاعات حساب‌هایت را بررسی کن."),
        HelpItem("حساب‌ها", "قرض و بدهی",
            "برای ثبت قرض، دریافت یا پرداخت بدهی از بخش قرض و بدهی استفاده کن."),
        HelpItem("برنامه‌ریزی", "پس‌انداز",
            "۱. یک هدف پس‌انداز بساز.\n۲. نام و مبلغ هدف را مشخص کن.\n۳. واریزها و برداشت‌ها را ثبت کن.\n۴. پیشرفت هدفت را بررسی کن."),
        HelpItem("برنامه‌ریزی", "بودجه‌بندی",
            "برای دسته‌های هزینه سقف بودجه تعیین کن و میزان مصرف بودجه را بررسی کن."),
        HelpItem("برنامه‌ریزی", "تنظیم هشدارها",
            "در این قسمت درصد و نوع هشدارهای مالی را مطابق نیازت تنظیم کن."),
        HelpItem("ابزارها", "گزارش‌ها و PDF",
            "گزارش‌های مالی را مشاهده کن و در صورت نیاز خروجی PDF بگیر."),
        HelpItem("امنیت", "قفل بخش‌های خصوصی",
            "ابتدا رمز چهاررقمی تعیین کن. سپس بخش‌هایی را که می‌خواهی قفل شوند فعال کن."),
        HelpItem("امنیت", "پشتیبان‌گیری",
            "برای تهیه نسخه پشتیبان، فایل ZIP بساز و در محل امن نگهداری کن. برای بازیابی، فایل ZIP را از داخل برنامه انتخاب کن."),
        HelpItem("ابزارها", "شخصی‌سازی",
            "از این بخش می‌توانی قالب ظاهری و آیکون برنامه را تغییر بدهی."),
        HelpItem("ابزارها", "ماشین‌حساب",
            "از ماشین‌حساب برای محاسبات مالی روزمره استفاده کن.")
    )

    val filteredItems = helpItems.filter { item ->
        (selectedCategory == "همه" || item.category == selectedCategory) &&
        (
            item.title.contains(searchText, ignoreCase = true) ||
            item.answer.contains(searchText, ignoreCase = true) ||
            item.category.contains(searchText, ignoreCase = true)
        )
    }

    Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
        Text(
            "پشتیبانی هوشمند",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "راهنمای استفاده از مدیریت مالی",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                selectedQuestion = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("جستجو در راهنما") },
            singleLine = true
        )

        Text(
            "دسته‌بندی آموزش‌ها",
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        selectedCategory = category
                        selectedQuestion = null
                    },
                    label = { Text(category) }
                )
            }
        }

        Text(
            "راهنمای آفلاین",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        filteredItems.forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedQuestion =
                            if (selectedQuestion == item.title) null
                            else item.title
                    }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        item.category,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        item.title,
                        fontWeight = FontWeight.Bold
                    )

                    if (selectedQuestion == item.title) {
                        Divider()
                        Text(item.answer)
                    }
                }
            }
        }

        if (filteredItems.isEmpty()) {
            Text("موردی پیدا نشد.")
        }

        Divider()

        Text(
            "سؤالات پرتکرار",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text("چطور از اطلاعاتم نسخه پشتیبان بگیرم؟")
        Text("از بخش پشتیبان‌گیری گزینه ایجاد فایل پشتیبان را انتخاب کن.")

        Text("اگر رمز بخش خصوصی را فراموش کنم چه کنم؟")
        Text("برای جلوگیری از ازدست‌رفتن دسترسی، قبل از فعال‌کردن قفل‌ها رمز خود را در جای امن نگهداری کن.")

        Divider()

        Text(
            "پشتیبانی آنلاین هوشمند",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            "اتصال دستیار آنلاین در مرحله بعد اضافه می‌شود."
        )

        Text(
            "تماس با سازنده",
            fontWeight = FontWeight.Bold
        )

        Text(
            "اطلاعات تماس سازنده در نسخه بعدی اضافه می‌شود."
        )
    }
}


