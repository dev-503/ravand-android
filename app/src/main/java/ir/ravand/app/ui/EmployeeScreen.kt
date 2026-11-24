package ir.ravand.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ravand.app.viewmodel.MainViewModel
import ir.ravand.app.viewmodel.UiState

@Composable
fun EmployeeScreen(vm: MainViewModel, state: UiState) {
    val me = state.dashboard?.employees?.find { it.id == state.employeeId }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("ر", color = Color.White, style = MaterialTheme.typography.titleLarge) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("رَوَند", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("پنل پرسنل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Spacer(Modifier.height(16.dp))
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(me?.initials ?: "پ", hexColor(me?.color ?: "#dff3eb"))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("${me?.name.orEmpty()} ${me?.family.orEmpty()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(me?.username.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (me?.totpEnabled == true) "Authenticator فعال ✓" else "Authenticator غیرفعال",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (me?.totpEnabled == true) MaterialTheme.colorScheme.primary else Color(0xFFB2661A),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(nowFmt(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("سلام ${me?.name.orEmpty()} 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("وضعیت حضور امروز شما", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))

            Row {
                Box(Modifier.weight(1f)) { StatCard("وضعیت فعلی", me?.status ?: "—", icon = "◉", tint = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) { StatCard("ساعت امروز", fa(me?.today ?: 0.0, 1), "ساعت", "⏱", Color(0xFFE39A3B)) }
            }
            Spacer(Modifier.height(10.dp))
            Row {
                Box(Modifier.weight(1f)) { StatCard("کارکرد ماه", fa(me?.hours ?: 0.0, 1), "ساعت", "📊", Color(0xFF8E6BD9)) }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) { StatCard("اضافهکاری", fa(me?.overtime ?: 0.0, 1), "ساعت", "👛", Color(0xFF5179E5)) }
            }
            Spacer(Modifier.height(12.dp))

            SectionCard {
                Text("ثبت حضور و غیاب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                PrimaryButton("↱ ثبت ورود", { vm.openCodePrompt("حاضر", "ثبت ورود") }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlineButton("↰ ثبت خروج", { vm.openCodePrompt("خارج‌شده", "ثبت خروج") }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlineButton("🌴 ثبت مرخصی", { vm.openCodePrompt("مرخصی", "ثبت مرخصی") }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlineButton("📱 فعالسازی Authenticator", { state.employeeId?.let { vm.startEnrollment(it, "${me?.name.orEmpty()} ${me?.family.orEmpty()}") } }, Modifier.fillMaxWidth())
                if (me?.totpEnabled != true) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "هنوز Google Authenticator فعال نیست. از مدیر بخواهید QR را نمایش دهد یا از دکمهٔ بالا استفاده کنید.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB2661A),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("آخرین رویدادها", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (state.events.isEmpty()) {
                    Text("رویدادی ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.events.forEach { ev ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(eventKindLabel(ev.kind), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(ev.occurredAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("© ۱۴۰۵ رَوَند · Ravand", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9AA4A1))
        }
    }
}
