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

class FinanceDb(context: Context) : SQLiteOpenHelper(context, "finance.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT NOT NULL, amount INTEGER NOT NULL, " +
                "category TEXT NOT NULL, note TEXT NOT NULL, date TEXT NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun add(type: String, amount: Long, category: String, note: String) {
        val values = ContentValues().apply {
            put("type", type)
            put("amount", amount)
            put("category", category)
            put("note", note)
            put("date", SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa", "IR")).format(Date()))
        }
        writableDatabase.insertOrThrow("transactions", null, values)
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
            "SELECT id,type,amount,category,note,date FROM transactions ORDER BY id DESC", null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    MoneyTransaction(
                        cursor.getLong(0), cursor.getString(1), cursor.getLong(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5)
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
    val pages = listOf(
        "خانه", "ثبت درآمد", "ثبت هزینه", "قرض و بدهی", "پس‌انداز",
        "گزارش‌ها و PDF", "دسته‌بندی‌ها", "مدیریت بانک‌ها", "ماشین‌حساب",
        "جستجو و تاریخچه", "بودجه‌بندی", "تنظیم هشدارها", "قفل بخش‌های خصوصی",
        "شخصی‌سازی", "پشتیبانی هوشمند", "پشتیبان‌گیری", "تنظیمات"
    )

    MaterialTheme(colorScheme = darkColorScheme(
        background = Night, surface = Panel, primary = Cyan, secondary = Purple,
        onBackground = Color.White, onSurface = Color.White
    )) {
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
                    Text("دسترسی سریع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction("ثبت درآمد", Icons.Default.AddCircle, Green, Modifier.weight(1f)) { page = "ثبت درآمد" }
                        QuickAction("ثبت هزینه", Icons.Default.RemoveCircle, Red, Modifier.weight(1f)) { page = "ثبت هزینه" }
                    }
                    Text("تراکنش‌های اخیر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (transactions.isEmpty()) {
                        EmptyCard("هنوز تراکنشی ثبت نشده است.")
                    } else {
                        transactions.take(10).forEach { item ->
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
                    TransactionForm(page, onSave = { amount, category, note ->
                        db.add(if (page == "ثبت درآمد") "درآمد" else "هزینه", amount, category, note)
                        refresh++
                        page = "خانه"
                    })
                } else {
                    Text(page, color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    when (page) {
                        "جستجو و تاریخچه" -> {
                            if (transactions.isEmpty()) EmptyCard("هنوز تراکنشی ثبت نشده است.")
                            else transactions.forEach { item ->
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
                    containerColor = Panel
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
                    containerColor = Panel
                )
            }
        }
    }
}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionForm(page: String, onSave: (Long, String, String) -> Unit) {
private fun TransactionForm(page: String, onSave: (Long, String, String) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val categories = if (page == "ثبت درآمد") {
        listOf("حقوق", "پاداش", "فروش", "هدیه", "سود سرمایه‌گذاری", "سایر")
    } else {
        listOf("خوراک", "حمل‌ونقل", "خرید", "قبض و خدمات", "درمان", "پوشاک", "تفریح", "آموزش", "اجاره و مسکن", "قسط و بدهی", "سایر")
    }

    OutlinedTextField(
        value = amountText,
        onValueChange = {
            amountText = toEnglishDigits(it).filter(Char::isDigit)
            error = ""
        },
        label = { Text("مبلغ به تومان") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = category,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(if (page == "ثبت درآمد") "دسته درآمد" else "دسته هزینه")
            },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "انتخاب دسته")
            },
            modifier = Modifier.fillMaxWidth().clickable {
                expanded = true
            },
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
        label = { Text("توضیحات (اختیاری)") },
        modifier = Modifier.fillMaxWidth()
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("ذخیره تراکنش")
    }

    Text(
        "مبلغ‌ها به تومان ثبت می‌شوند و روی همین گوشی ذخیره می‌مانند.",
        color = Color.LightGray,
        fontSize = 12.sp
    )
}


@Composable
private fun SummaryCard(title: String, amount: String, color: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Text(title, color = Color.LightGray, fontSize = 12.sp)
            Text(amount, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
    Card(modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TransactionRow(
    item: MoneyTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "ویرایش", tint = Cyan)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "حذف", tint = Red)
                }
            }
            Column(
                Modifier.weight(1f).padding(end = 6.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(item.category, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(item.date, color = Color.LightGray, fontSize = 11.sp)
                if (item.note.isNotBlank()) Text(item.note, color = Color.LightGray, fontSize = 12.sp)
                Text(
                    (if (item.type == "درآمد") "+" else "−") + money(item.amount),
                    color = if (item.type == "درآمد") Green else Red,
                    fontWeight = FontWeight.Bold
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
