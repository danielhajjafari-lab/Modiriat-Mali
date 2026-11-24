package ir.daniel.modiriatmali

import android.content.ContentValues
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
    var menuOpen by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
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
        Scaffold(
            containerColor = Night,
            topBar = {
                TopAppBar(
                    title = { Text("مدیریت مالی", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.Menu, "منو", tint = Cyan)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Night)
                )
            }
        ) { padding ->
            if (menuOpen) {
                AlertDialog(
                    onDismissRequest = { menuOpen = false },
                    title = { Text("منوی برنامه", color = Cyan) },
                    text = {
                        Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                            pages.forEach { item ->
                                Text(
                                    item,
                                    Modifier.fillMaxWidth().clickable {
                                        page = item
                                        menuOpen = false
                                    }.padding(vertical = 12.dp),
                                    color = Color.White
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { menuOpen = false }) { Text("بستن") }
                    },
                    containerColor = Panel
                )
            }

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
                        transactions.take(10).forEach { item -> TransactionRow(item) }
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
                            else transactions.forEach { TransactionRow(it) }
                        }
                        else -> EmptyCard("این بخش هنوز در دست ساخت است. اطلاعات مالی شما در این نسخه در بخش ثبت تراکنش‌ها ذخیره می‌شود.")
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionForm(page: String, onSave: (Long, String, String) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = toEnglishDigits(it).filter(Char::isDigit); error = "" },
        label = { Text("مبلغ به تومان") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    OutlinedTextField(
        value = category,
        onValueChange = { category = it; error = "" },
        label = { Text(if (page == "ثبت درآمد") "دلیل دریافت (مثلاً حقوق)" else "دسته هزینه (مثلاً خوراک)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
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
            when {
                amount == null || amount <= 0 -> error = "مبلغ معتبر وارد کن."
                category.isBlank() -> error = "لطفاً دسته یا دلیل تراکنش را بنویس."
                else -> onSave(amount, category.trim(), note.trim())
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("ذخیره تراکنش") }
    Text("مبلغ‌ها به تومان ثبت می‌شوند و روی همین گوشی ذخیره می‌مانند.", color = Color.LightGray, fontSize = 12.sp)
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
private fun TransactionRow(item: MoneyTransaction) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                (if (item.type == "درآمد") "+" else "−") + money(item.amount),
                color = if (item.type == "درآمد") Green else Red,
                fontWeight = FontWeight.Bold
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(item.category, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(item.date, color = Color.LightGray, fontSize = 11.sp)
                if (item.note.isNotBlank()) Text(item.note, color = Color.LightGray, fontSize = 12.sp)
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
