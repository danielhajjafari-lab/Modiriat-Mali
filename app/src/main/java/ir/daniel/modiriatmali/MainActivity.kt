package ir.daniel.modiriatmali

import android.content.ContentValues
import kotlinx.coroutines.launch
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

data class MoneyTransaction(
    val id: Long,
    val type: String,
    val amount: Long,
    val category: String,
    val note: String,
    val date: String
)



class FinanceDb(context: Context) :
    SQLiteOpenHelper(context, "finance.db", null, 3) {

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
            if (oldVersion < 3) {
    createBudgetTables(db)
            }
            createCategoriesTable(db)
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

    
MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Cyan,
        onPrimary = Night,
        primaryContainer = Color(0xFF123746),
        onPrimaryContainer = Color(0xFFB9F5FF),

        secondary = Purple,
        onSecondary = Night,
        secondaryContainer = Color(0xFF30264F),
        onSecondaryContainer = Color(0xFFE5DAFF),

        tertiary = Green,
        onTertiary = Night,

        background = Night,
        onBackground = Color.White,

        surface = Panel,
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1B2940),
        onSurfaceVariant = Color(0xFFC3D0E2),

        outline = Color(0xFF52647C),
        error = Red,
        onError = Night
    )
) {


                ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Panel,
                    drawerContentColor = Color.White
                ) {
                    Text(
                        "مدیریت مالی",
                        Modifier.fillMaxWidth().padding(20.dp),
                        color = Cyan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(color = Color(0xFF26354D))

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
            containerColor = Night,
            topBar = {
                TopAppBar(
                    title = { Text("مدیریت مالی", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "منو", tint = Cyan)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Night)
                )
            }
        ) { padding ->
            

            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (page == "خانه") {
                    Text("داشبورد مالی", color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("موجودی محاسبه‌شده", color = Color.LightGray, fontSize = 14.sp)
                            Text(money(income - expenses), color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text("بر اساس تراکنش‌های ثبت‌شده در این دستگاه", color = Cyan, fontSize = 11.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryCard("مجموع درآمد", money(income), Green, Modifier.weight(1f))
                        SummaryCard("مجموع هزینه", money(expenses), Red, Modifier.weight(1f))
                    }
                    Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = Panel
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
            color = Cyan,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("درآمد ماه", color = Color.LightGray, fontSize = 12.sp)
                Text(money(monthlyIncome), color = Green, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("هزینه ماه", color = Color.LightGray, fontSize = 12.sp)
                Text(money(monthlyExpenses), color = Red, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = Color(0xFF26354D))

        Text(
            "خالص ماه: ${money(monthlyBalance)}",
            color = if (monthlyBalance >= 0) Green else Red,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
                    }
                    Text("دسترسی سریع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction("ثبت درآمد", Icons.Default.AddCircle, Green, Modifier.weight(1f)) { page = "ثبت درآمد" }
                        QuickAction("ثبت هزینه", Icons.Default.RemoveCircle, Red, Modifier.weight(1f)) { page = "ثبت هزینه" }
                    }
                    Text("تراکنش‌های اخیر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    listOf("همه", "درآمد", "هزینه").forEach { type ->
        FilterChip(
            selected = transactionFilter == type,
            onClick = { transactionFilter = type },
            label = { Text(type) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Cyan,
                selectedLabelColor = Night,
                labelColor = Color.White
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
                    Text(page, color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    TransactionForm(
    page,
    categories = db.getCategories(if (page == "ثبت درآمد") "درآمد" else "هزینه"),
    onSave = { amount, category, note ->
                        db.add(if (page == "ثبت درآمد") "درآمد" else "هزینه", amount, category, note)
                        refresh++
                        page = "خانه"
                    })
                } else {
                    Text(page, color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
                        else -> EmptyCard("این بخش هنوز در دست ساخت است. اطلاعات مالی شما در این نسخه در بخش ثبت تراکنش‌ها ذخیره می‌شود.")
                    }
                }
            }

            editing?.let { item ->
                AlertDialog(
                    onDismissRequest = { editing = null },
                    title = { Text("ویرایش تراکنش", color = Cyan) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (item.type == "درآمد") "نوع: درآمد" else "نوع: هزینه", color = Color.LightGray)
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
                            color = Color.White
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
        color = Cyan
    )
},
modifier = Modifier.fillMaxWidth(),
shape = RoundedCornerShape(16.dp),
colors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Cyan,
    unfocusedBorderColor = Color(0xFF52647C),
    focusedLabelColor = Cyan,
    unfocusedLabelColor = Color.LightGray,
    cursorColor = Cyan,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
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
                    tint = Cyan
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
            unfocusedLabelColor = Color.LightGray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
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
        unfocusedLabelColor = Color.LightGray,
        cursorColor = Cyan,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
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
        color = Color.LightGray,
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
                    containerColor = if (type == "هزینه") Cyan else Panel
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("دسته‌های هزینه")
            }

            Button(
                onClick = { type = "درآمد"; message = "" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "درآمد") Cyan else Panel
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
            Text(message, color = Cyan)
        }

        Text(
            "فهرست دسته‌های ${type}",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        categories.forEach { name ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Panel
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
                                tint = Cyan
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

                    Text(name, color = Color.White)
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
            containerColor = Panel
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

        Text("نوع تراکنش", color = Color.White)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("همه", "درآمد", "هزینه").forEach { type ->
                Button(
                    onClick = { selectedType = type },
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (selectedType == type) Cyan else Panel
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
            color = Cyan,
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
            containerColor = Color(0xFF142238)
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
                color = Color(0xFFB8C8DA),
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
            containerColor = Color(0xFF142238)
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
                color = Color.White,
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
            containerColor = Color(0xFF142238)
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
                        tint = Cyan
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Text(
                    item.date,
                    color = Color(0xFF9EADC2),
                    fontSize = 11.sp
                )

                if (item.note.isNotBlank()) {
                    Text(
                        item.note,
                        color = Color.LightGray,
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
private fun EmptyCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Text(message, Modifier.fillMaxWidth().padding(22.dp), color = Color.LightGray, textAlign = TextAlign.Center)
    }
}
