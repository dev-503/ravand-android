package ir.ravand.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ravand.app.data.Dashboard
import ir.ravand.app.data.Employee
import ir.ravand.app.data.EmployeeForm
import ir.ravand.app.data.Settings
import ir.ravand.app.data.SettingsForm
import ir.ravand.app.data.cleanDigits
import ir.ravand.app.viewmodel.MainViewModel
import ir.ravand.app.viewmodel.UiState

private val pages = listOf("پیشخوان", "کارکنان", "گزارش ماهانه", "مدیریت دوره", "تنظیمات")
private val pageIcons = listOf("▦", "👥", "📊", "📅", "⚙️")

@Composable
fun AdminScreen(vm: MainViewModel, state: UiState) {
    val dash = state.dashboard ?: return
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                pages.forEachIndexed { i, page ->
                    NavigationBarItem(
                        selected = state.screen == page,
                        onClick = { vm.navigate(page) },
                        icon = { Text(pageIcons[i]) },
                        label = { Text(page, style = MaterialTheme.typography.labelSmall) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            TopHeader(state, vm)
            Spacer(Modifier.height(16.dp))
            PageHeading(state, vm)
            Spacer(Modifier.height(14.dp))

            when (state.screen) {
                "تنظیمات" -> SettingsPage(vm, dash.settings)
                "مدیریت دوره" -> PeriodsPage(vm, state)
                else -> DashboardBody(vm, state, dash)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "© ۱۴۰۵ رَوَند · ساختهشده برای روزهای بهتر",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9AA4A1),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun TopHeader(state: UiState, vm: MainViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("ر", color = Color.White, style = MaterialTheme.typography.titleLarge) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("رَوَند", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("مدیریت حضور و غیاب", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
            onClick = { vm.logout() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            shape = RoundedCornerShape(10.dp),
        ) { Text("↩ خروج") }
    }
    Spacer(Modifier.height(10.dp))
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (state.dashboard?.settings?.orgName ?: "ن").take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(state.dashboard?.settings?.orgName.orEmpty(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("فضای کاری شما", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PageHeading(state: UiState, vm: MainViewModel) {
    val context = LocalContext.current
    val isDash = state.screen == "پیشخوان"
    Text(nowFmt(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        if (isDash) "روز بخیر، مدیر 👋" else state.screen,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        if (isDash) "نمای کلی وضعیت تیم شما در یک نگاه." else "اطلاعات و فعالیتهای تیم خود را مدیریت کنید.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(10.dp),
            ) { Text(state.selectedMonth, style = MaterialTheme.typography.labelMedium) }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                listOf("آبان ۱۴۰۵", "مهر ۱۴۰۵", "شهریور ۱۴۰۵").forEach { m ->
                    DropdownMenuItem(text = { Text(m) }, onClick = { vm.setMonth(m); expanded = false })
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        OutlineButton("⬇ گزارش", { vm.exportCsv(context) })
    }
    Spacer(Modifier.height(8.dp))
    PrimaryButton("＋ افزودن کارمند", { vm.openEmployeeDialog(null) }, Modifier.fillMaxWidth())
}

@Composable
private fun DashboardBody(vm: MainViewModel, state: UiState, dash: Dashboard) {
    val s = dash.stats
    StatsGrid(s)
    Spacer(Modifier.height(12.dp))
    AttendancePanel(dash)
    Spacer(Modifier.height(12.dp))
    HoursChart(s.workedHours)
    Spacer(Modifier.height(12.dp))
    TeamPanel(vm, state, dash)
}

@Composable
private fun StatsGrid(s: ir.ravand.app.data.Stats) {
    Row {
        Box(Modifier.weight(1f)) {
            StatCard("کل کارکنان", fa(s.total, 0), "نفر", "👥", Color(0xFF8E6BD9))
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            StatCard("حاضر در محل", fa(s.present, 0), "نفر", "◉", MaterialTheme.colorScheme.primary)
        }
    }
    Spacer(Modifier.height(10.dp))
    Row {
        Box(Modifier.weight(1f)) {
            StatCard("ساعت کارکرد", fa(s.workedHours, 0), "ساعت", "⏱", Color(0xFFE39A3B))
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            StatCard("اضافهکاری", fa(s.overtime, 0), "ساعت", "👛", Color(0xFF5179E5))
        }
    }
}

@Composable
private fun AttendancePanel(dash: Dashboard) {
    SectionCard {
        Text("وضعیت امروز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("فعالیت لحظه‌ای کارکنان", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        val total = dash.stats.total.coerceAtLeast(1)
        listOf(
            Triple("حاضر", dash.employees.count { it.status == "حاضر" }, MaterialTheme.colorScheme.primary),
            Triple("در مرخصی", dash.employees.count { it.status == "مرخصی" }, Color(0xFFEDAA63)),
            Triple("خارج از محل", dash.employees.count { it.status == "خارج‌شده" }, Color(0xFFB9C3C0)),
        ).forEach { (label, count, color) ->
            Text("$label  —  ${fa(count, 0)} نفر", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFEDF1EF), RoundedCornerShape(8.dp))) {
                Box(
                    Modifier
                        .fillMaxWidth(count.toFloat() / total)
                        .height(8.dp)
                        .background(color, RoundedCornerShape(8.dp))
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        Text("● بهروزرسانی زنده  ·  ${timeFmt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HoursChart(worked: Double) {
    SectionCard {
        Text("روند ساعت کاری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("مجموع ساعات ثبتشده در ماه جاری", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Text("${fa(worked, 0)} ساعت", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        val heights = listOf(0.42f, 0.58f, 0.5f, 0.72f, 0.64f, 0.86f, 0.78f, 0.95f)
        Row(Modifier.fillMaxWidth().height(110.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            heights.forEach {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(it)
                        .padding(horizontal = 3.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + 0.55f * it), RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("۱ آبان                     ۱۵ آبان                     ۳۰ آبان", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TeamPanel(vm: MainViewModel, state: UiState, dash: Dashboard) {
    val q = state.query.trim()
    val filtered = dash.employees.filter { e ->
        val matchesQuery = q.isEmpty() || "${e.name} ${e.family} ${e.username} ${e.id}".contains(q, ignoreCase = true)
        val matchesStatus = state.statusFilter == "همه" || e.status == state.statusFilter
        matchesQuery && matchesStatus
    }
    SectionCard {
        Text("حضور و غیاب تیم", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("وضعیت و ساعت کارکرد کارکنان در ${state.selectedMonth}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LabeledField("جستجوی نام کارمند…", state.query, vm::setQuery, placeholder = "نام، شناسه یا کد پرسنلی")
        Spacer(Modifier.height(8.dp))
        var filterExpanded by remember { mutableStateOf(false) }
        Box {
            Button(
                onClick = { filterExpanded = true },
                colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
                shape = RoundedCornerShape(10.dp),
            ) { Text("وضعیت: ${state.statusFilter}") }
            DropdownMenu(filterExpanded, onDismissRequest = { filterExpanded = false }) {
                listOf("همه", "حاضر", "مرخصی", "خارج‌شده").forEach { f ->
                    DropdownMenuItem(text = { Text(f) }, onClick = { vm.setStatusFilter(f); filterExpanded = false })
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("نمایش ${fa(filtered.size, 0)} از ${fa(dash.employees.size, 0)} کارمند", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Text("کارمندی با این مشخصات پیدا نشد.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            filtered.forEach { emp ->
                EmployeeCard(vm, emp)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EmployeeCard(vm: MainViewModel, emp: Employee) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFBFA)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(emp.initials, hexColor(emp.color))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${emp.name} ${emp.family}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("${emp.id}  ·  ${emp.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    Text("⋯", modifier = Modifier.clickable { menuOpen = true }, style = MaterialTheme.typography.titleLarge)
                    DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("ثبت ورود") }, onClick = { vm.setStatus(emp.id, "حاضر"); menuOpen = false })
                        DropdownMenuItem(text = { Text("ثبت خروج") }, onClick = { vm.setStatus(emp.id, "خارج‌شده"); menuOpen = false })
                        DropdownMenuItem(text = { Text("ثبت مرخصی") }, onClick = { vm.setStatus(emp.id, "مرخصی"); menuOpen = false })
                        DropdownMenuItem(text = { Text("✎ ویرایش") }, onClick = { vm.openEmployeeDialog(emp); menuOpen = false })
                        DropdownMenuItem(text = { Text("🗑 حذف") }, onClick = { vm.openDelete(emp); menuOpen = false })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(emp.status)
                Spacer(Modifier.width(8.dp))
                AuthBadge(emp.totpEnabled)
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Text("امروز: ${fa(emp.today, 1)} ساعت", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("ماه: ${fa(emp.hours, 1)} ساعت", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (emp.overtime > 0) Text("اضافه: +${fa(emp.overtime, 1)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB2661A))
            }
            Spacer(Modifier.height(10.dp))
            val next = if (emp.status == "حاضر") "↰ ثبت خروج" else "↱ ثبت ورود"
            OutlineButton(
                next,
                { vm.setStatus(emp.id, if (emp.status == "حاضر") "خارج‌شده" else "حاضر") },
                Modifier.fillMaxWidth().height(38.dp),
            )
        }
    }
}

@Composable
private fun SettingsPage(vm: MainViewModel, settings: Settings) {
    var org by remember(settings) { mutableStateOf(settings.orgName) }
    var adminUser by remember(settings) { mutableStateOf(settings.adminUsername) }
    var curPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var theme by remember(settings) { mutableStateOf(settings.theme) }
    var font by remember(settings) { mutableStateOf(settings.font) }

    val themes = listOf(
        "mint" to "سبز و سفید", "dark" to "مشکی و نارنجی", "blue" to "آبی و سفید",
        "brown" to "قهوه‌ای و سفید", "mono" to "مشکی و سفید",
    )
    SectionCard {
        Text("ظاهر پنل مدیریت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("رنگ و حالوهای فضای کاری خود را انتخاب کنید.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        themes.chunked(2).forEach { pair ->
            Row {
                pair.forEach { (id, label) ->
                    val chosen = theme == id
                    Box(
                        Modifier
                            .weight(1f)
                            .clickable { theme = id; vm.setThemePreview(id) }
                            .border(
                                if (chosen) 2.dp else 1.dp,
                                if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(10.dp),
                            )
                            .padding(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(18.dp).background(hexColor(themeHex(id)), CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall)
                            if (chosen) Text(" ✓", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        LabeledField("نام مجموعه", org, { org = it })
        Spacer(Modifier.height(12.dp))
        Text("خانوادهٔ فونت", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row {
            listOf("arad" to "آراد (پیشفرض)", "vazirmatn" to "وزیرمتن").forEach { (id, label) ->
                Button(
                    onClick = { font = id; vm.setFontPreview(id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (font == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (font == id) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                Spacer(Modifier.width(8.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            "ذخیرهٔ تنظیمات ✓",
            {
                vm.saveSettings(
                    SettingsForm(theme, font, org, settings.adminUsername, "", "")
                )
            },
            Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(12.dp))
    SectionCard {
        Text("🛡 امنیت و ورود", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("نام کاربری و رمز عبور مدیر از این بخش تغییر میکند. رمزها هششده ذخیره میشوند.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LabeledField("نام کاربری مدیر", adminUser, { adminUser = it })
        Spacer(Modifier.height(10.dp))
        LabeledField("رمز فعلی مدیر (برای تغییر رمز)", curPass, { curPass = it }, password = true)
        Spacer(Modifier.height(10.dp))
        LabeledField("رمز جدید مدیر (اختیاری)", newPass, { newPass = it }, password = true)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            "ذخیرهٔ تغییرات ✓",
            { vm.saveSettings(SettingsForm(theme, font, org, adminUser, newPass, curPass)) },
            Modifier.fillMaxWidth(),
        )
    }
}

private fun themeHex(id: String): String = when (id) {
    "dark" -> "#EF8C4D"
    "blue" -> "#5179E5"
    "brown" -> "#997857"
    "mono" -> "#424849"
    else -> "#21A87A"
}

@Composable
private fun PeriodsPage(vm: MainViewModel, state: UiState) {
    var title by remember { mutableStateOf("") }
    var starts by remember { mutableStateOf("") }
    var ends by remember { mutableStateOf("") }
    SectionCard {
        Text("دورههای حقوق", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("دورههای باز و بستهٔ حقوق و دستمزد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LabeledField("مثلاً آذر ۱۴۰۵", title, { title = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("تاریخ شروع (YYYY-MM-DD)", starts, { starts = it })
        Spacer(Modifier.height(8.dp))
        LabeledField("تاریخ پایان (YYYY-MM-DD)", ends, { ends = it })
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            "＋ افزودن دوره",
            {
                if (title.isBlank() || starts.isBlank() || ends.isBlank()) vm.toast("عنوان و بازه دوره الزامی است")
                else {
                    vm.addPeriod(title, starts, ends)
                    title = ""; starts = ""; ends = ""
                }
            },
            Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        if (state.periods.isEmpty()) {
            Text("دورهای ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.periods.forEach { p ->
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFFFAFBFA), RoundedCornerShape(10.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${cleanDigits(p.startsOn)} ← ${cleanDigits(p.endsOn)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
