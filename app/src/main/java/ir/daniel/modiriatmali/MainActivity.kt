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
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
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


class FinanceDb(context: Context) :
    SQLiteOpenHelper(context, "finance.db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT NOT NULL, amount INTEGER NOT NULL, " +
                "category TEXT NOT NULL, note TEXT NOT NULL, date TEXT NOT NULL)"
        )
        createCategoriesTable(db)
        createBudgetTables(db)
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
private fun money(value: Long): String =
    NumberFormat.getInstance(Locale("fa", "IR")).format(value) + " تومان"

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceApp(db: FinanceDb) {
    var page by remember { mutableStateOf("خانه") }
    var transactionFilter by remember { mutableStateOf("همه") }
var refresh by remember { mutableIntStateOf(0) }
val context = LocalContext.current
    
val themePreferences = remember {
    context.getSharedPreferences(
        "app_settings",
        Context.MODE_PRIVATE
    )
}

var selectedThemeId by remember {
    mutableStateOf(
        themePreferences.getString("theme_id", "neon") ?: "neon"
    )
}

val activeTheme = appThemePresets.firstOrNull {
    it.id == selectedThemeId
} ?: appThemePresets.first()



val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
val scope = rememberCoroutineScope() 

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
    val pages = listOf(
        "خانه", "ثبت درآمد", "ثبت هزینه", "قرض و بدهی", "پس‌انداز",
        "گزارش‌ها و PDF", "دسته‌بندی‌ها", "مدیریت بانک‌ها", "ماشین‌حساب",
        "جستجو و تاریخچه", "بودجه‌بندی", "تنظیم هشدارها", "قفل بخش‌های خصوصی",
        "شخصی‌سازی", "پشتیبانی هوشمند", "پشتیبان‌گیری", "تنظیمات"
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

MaterialTheme(colorScheme = appColorScheme) {



                ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = activeTheme.surface,
drawerContentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Text(
    "مدیریت مالی",
    modifier = Modifier.fillMaxWidth().padding(20.dp),
    color = MaterialTheme.colorScheme.primary,
    fontSize = 22.sp,
    fontWeight = FontWeight.Bold

                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(
                        Modifier.fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        pages.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item) },
                                selected = page == item,
                                onClick = {
                                    page = item
                                    scope.launch { drawerState.close() }
                                },
                                icon = {
                                    Icon(Icons.Default.Menu, contentDescription = null)
                                },
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
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
    Text(
        "مدیریت مالی",
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
},
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
    Icons.Default.Menu,
    "منو",
    tint = activeTheme.primary
)
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
        "سقف: ${money(dailyBudgetLimit)}",
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
                    "مانده بودجه: ${money(dailyBudgetLimit - todayExpenses)}"
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
    money(income - expenses),
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
                        SummaryCard("مجموع درآمد", money(income), Green, Modifier.weight(1f))
                        SummaryCard("مجموع هزینه", money(expenses), Red, Modifier.weight(1f))
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
                Text(money(monthlyIncome), color = Green, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("هزینه ماه", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(money(monthlyExpenses), color = Red, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            "خالص ماه: ${money(monthlyBalance)}",
            color = if (monthlyBalance >= 0) Green else Red,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
                    }
                    Text("دسترسی سریع", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction("ثبت درآمد", Icons.Default.AddCircle, Green, Modifier.weight(1f)) { page = "ثبت درآمد" }
                        QuickAction("ثبت هزینه", Icons.Default.RemoveCircle, Red, Modifier.weight(1f)) { page = "ثبت هزینه" }
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
                                onDelete = { deleting = item }
                            )
                        }
                    }
                } else if (page == "ثبت درآمد" || page == "ثبت هزینه") {
                    Text(page, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    TransactionForm(
    page,
    categories = db.getCategories(if (page == "ثبت درآمد") "درآمد" else "هزینه"),
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

    refresh++
    page = "خانه"
})
                } else {
                    Text(page, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    when (page) {
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
            deleting = item
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

"شخصی‌سازی" -> {
    AppThemeScreen(
        selectedThemeId = selectedThemeId,
        onThemeSelected = { themeId ->
            selectedThemeId = themeId
            themePreferences.edit()
                .putString("theme_id", themeId)
                .apply()
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
                            "از حذف این ${item.type} به مبلغ ${money(item.amount)} مطمئنی؟ این کار قابل بازگشت نیست.",
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
    unfocusedBorderColor = Color(0xFF52647C),
    focusedLabelColor = Cyan,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = Cyan,
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
            focusedBorderColor = Cyan,
            unfocusedBorderColor = Color(0xFF52647C),
            focusedLabelColor = Cyan,
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
        focusedBorderColor = Cyan,
        unfocusedBorderColor = Color(0xFF52647C),
        focusedLabelColor = Cyan,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = Cyan,
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
    containerColor = Cyan,
    contentColor = Night
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
                    item.date,
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
                        money(item.amount),
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
                    money(todayExpenses),
                    color = Red,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
Text(
    "باقی‌مانده بودجه",
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
                Text(
                    if (dailyLimit > 0) money(remaining) else "سقف بودجه تعیین نشده",
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
private fun AppThemeScreen(
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit
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
