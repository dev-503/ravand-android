package ir.ravand.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.ravand.app.viewmodel.MainViewModel
import ir.ravand.app.viewmodel.UiState

/** ورود تنها برای مدیر است؛ ثبت ورود/خروج کارکنان توسط همین پنل انجام میشود. */
@Composable
fun LoginScreen(vm: MainViewModel, state: UiState) {
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(30.dp))
            Box(Modifier.size(56.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Text("ر", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text("رَوَند", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text("سامانه حضور و غیاب · Ravand", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(26.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("خوش برگشتی 👋", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "برای ثبت و مدیریت حضور و غیاب کارکنان، وارد پنل مدیریت شوید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    LabeledField("شناسهٔ کاربری", state.adminUser, vm::setAdminUser, placeholder = "مثلاً admin")
                    Spacer(Modifier.height(12.dp))
                    LabeledField("رمز عبور", state.adminPass, vm::setAdminPass, placeholder = "رمز عبور را وارد کنید", password = true)
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton("ورود به پنل ←", vm::adminLogin, Modifier.fillMaxWidth())

                    if (state.loginError.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(state.loginError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "ورود آزمایشی: admin / admin",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Text("رَوَند — نسخهٔ اندروید ۱.۰.۵", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9AA4A1))
        }
    }
}
