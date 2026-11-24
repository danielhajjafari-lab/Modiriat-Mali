package ir.daniel.modiriatmali

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

private val Night = Color(0xFF080D18)
private val Panel = Color(0xFF111B2D)
private val Cyan = Color(0xFF27D7F2)
private val Purple = Color(0xFF9B7BFF)
private val Green = Color(0xFF36D399)
private val Red = Color(0xFFFF647C)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FinanceApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceApp() {
    var page by remember { mutableStateOf("خانه") }
    var menuOpen by remember { mutableStateOf(false) }
    val pages = listOf("خانه", "ثبت درآمد", "ثبت هزینه", "قرض و بدهی", "پس‌انداز",
        "گزارش‌ها و PDF", "دسته‌بندی‌ها", "مدیریت بانک‌ها", "ماشین‌حساب",
        "جستجو و تاریخچه", "بودجه‌بندی", "تنظیم هشدارها", "قفل بخش‌های خصوصی",
        "شخصی‌سازی", "پشتیبانی هوشمند", "پشتیبان‌گیری", "تنظیمات")
    MaterialTheme(colorScheme = darkColorScheme(
        background = Night, surface = Panel, primary = Cyan, secondary = Purple,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(containerColor = Night, topBar = {
            TopAppBar(title = { Text("مدیریت مالی", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = { IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.Menu, "منو", tint = Cyan) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Night))
        }) { padding ->
            if (menuOpen) {
                AlertDialog(onDismissRequest = { menuOpen = false },
                    title = { Text("منوی برنامه", color = Cyan) },
                    text = {
                        Column(Modifier.heightIn(max = 440.dp)) {
                            pages.forEach { item ->
                                Text(item, Modifier.fillMaxWidth().clickable {
                                    page = item
                                    menuOpen = false
                                }.padding(vertical = 9.dp), color = Color.White)
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { menuOpen = false }) { Text("بستن") } },
                    containerColor = Panel)
            }
            Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (page == "خانه") {
                    Text("داشبورد مالی", color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    BalanceCard()
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryCard("درآمد ماه", "۰ تومان", Green, Modifier.weight(1f))
                        SummaryCard("هزینه ماه", "۰ تومان", Red, Modifier.weight(1f))
                    }
                    Text("دسترسی سریع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction("ثبت درآمد", Icons.Default.AddCircle, Green) { page = "ثبت درآمد" }
                        QuickAction("ثبت هزینه", Icons.Default.RemoveCircle, Red) { page = "ثبت هزینه" }
                        QuickAction("ماشین‌حساب", Icons.Default.Calculate, Purple) { page = "ماشین‌حساب" }
                    }
                    Text("تراکنش‌های اخیر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    EmptyCard("هنوز تراکنشی ثبت نشده است.")
                } else {
                    Text(page, color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    PageContent(page)
                }
            }
        }
    }
}

@Composable
private fun BalanceCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.End) {
            Text("موجودی کل", color = Color.LightGray, fontSize = 14.sp)
            Text("۰ تومان", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("اطلاعات مالی روی دستگاه نگهداری می‌شود", color = Cyan, fontSize = 11.sp)
        }
    }
}
@Composable
private fun SummaryCard(title: String, amount: String, color: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Text(title, color = Color.LightGray, fontSize = 12.sp)
            Text(amount, color = color, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}
@Composable
private fun RowScope.QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Card(Modifier.weight(1f).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}
@Composable
private fun EmptyCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Text(message, Modifier.fillMaxWidth().padding(22.dp), color = Color.LightGray, textAlign = TextAlign.Center)
    }
}
@Composable
private fun PageContent(page: String) {
    when (page) {
        "ثبت درآمد", "ثبت هزینه" -> {
            var amount by remember { mutableStateOf("") }
            var category by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("مبلغ به تومان") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(category, { category = it }, label = { Text(if (page == "ثبت درآمد") "نوع / دلیل دریافت" else "دسته هزینه") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it }, label = { Text("توضیحات (اختیاری)") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("ثبت تراکنش") }
            Text("فرم نمایشی اولیه است؛ ذخیره‌سازی در مرحله بعد اضافه می‌شود.", color = Color.LightGray, fontSize = 12.sp)
        }
        "قفل بخش‌های خصوصی" -> {
            Text("انتخاب بخش‌های موردنظر برای قفل", color = Color.White)
            listOf("گزارش‌ها و PDFها", "قرض و بدهی", "تاریخچه تراکنش‌ها", "پس‌انداز", "مدیریت حساب‌ها و بانک‌ها", "ماشین‌حساب").forEach { item ->
                var checked by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked, { checked = it }); Text(item, color = Color.White)
                }
            }
            OutlinedButton(onClick = {}) { Text("＋ افزودن بخش...") }
            Text("رمز شخصی هنگام فعال‌سازی این قابلیت تنظیم خواهد شد.", color = Color.LightGray, fontSize = 12.sp)
        }
        "شخصی‌سازی" -> {
            Text("تم پیش‌فرض: دیجیتال و تکنولوژیک", color = Cyan)
            Text("انتخاب از میان ۸ پوسته، تغییر نام و آیکون در بخش اختصاصی سازنده.", color = Color.White)
        }
        else -> EmptyCard("صفحه «$page» در اسکلت اولیه ایجاد شده است. امکانات عملیاتی این بخش در مراحل بعد تکمیل می‌شود.")
    }
}
