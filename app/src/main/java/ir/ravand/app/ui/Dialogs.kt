package ir.ravand.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import ir.ravand.app.data.Employee
import ir.ravand.app.data.EmployeeForm
import ir.ravand.app.data.cleanDigits
import ir.ravand.app.viewmodel.MainViewModel
import ir.ravand.app.viewmodel.UiState

@Composable
fun AppDialogs(vm: MainViewModel, state: UiState) {
    state.employeeDialog?.let { d ->
        EmployeeFormDialog(d.employee, vm)
    }
    state.deleteTarget?.let { emp ->
        AlertDialog(
            onDismissRequest = { vm.closeDelete() },
            title = { Text("حذف کارمند") },
            text = { Text("آیا از حذف ${emp.name} ${emp.family} مطمئن هستید؟ این عمل قابل بازگشت نیست.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteEmployee() }) { Text("🗑 حذف کارمند", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { vm.closeDelete() }) { Text("انصراف") } },
        )
    }
    state.totp?.let { t ->
        TotpDialog(vm, state)
    }
    state.codePrompt?.let { p ->
        CodePromptDialog(p.actionName, vm)
    }
}

@Composable
private fun EmployeeFormDialog(employee: Employee?, vm: MainViewModel) {
    var name by remember(employee) { mutableStateOf(employee?.name.orEmpty()) }
    var family by remember(employee) { mutableStateOf(employee?.family.orEmpty()) }
    var username by remember(employee) { mutableStateOf(employee?.username.orEmpty()) }
    var national by remember(employee) { mutableStateOf(employee?.national.orEmpty()) }
    var phone by remember(employee) { mutableStateOf(employee?.phone.orEmpty()) }
    var hourly by remember(employee) { mutableStateOf(employee?.hourly?.toString().orEmpty()) }
    var leave by remember(employee) { mutableStateOf(employee?.leaveLimit?.toString().orEmpty()) }
    var password by remember { mutableStateOf("") }

    val isEdit = employee != null
    AlertDialog(
        onDismissRequest = { vm.closeEmployeeDialog() },
        title = { Text(if (isEdit) "ویرایش مشخصات کارمند" else "افزودن کارمند جدید") },
        text = {
            Column {
                if (isEdit) Text("شناسهٔ پرسنلی ${employee!!.id} ثابت است.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LabeledField("نام *", name, { name = it }, "مثلاً علی")
                Spacer(Modifier.height(8.dp))
                LabeledField("نام خانوادگی", family, { family = it }, "مثلاً رضایی")
                Spacer(Modifier.height(8.dp))
                LabeledField("شناسه کاربری *", username, { username = it }, "مثلاً ali.r")
                Spacer(Modifier.height(8.dp))
                LabeledField("کد ملی", national, { national = it }, "۱۰ رقمی", numeric = true)
                Spacer(Modifier.height(8.dp))
                LabeledField("شماره همراه", phone, { phone = it }, "09xx xxx xxxx", numeric = true)
                Spacer(Modifier.height(8.dp))
                LabeledField("دستمزد ساعتی (تومان)", hourly, { hourly = it }, numeric = true)
                Spacer(Modifier.height(8.dp))
                LabeledField("سقف مرخصی (ساعت/ماه)", leave, { leave = it }, numeric = true)
                Spacer(Modifier.height(8.dp))
                LabeledField(
                    if (isEdit) "رمز جدید (اختیاری)" else "رمز عبور (پیشفرض ۱۲۳۴۵۶)",
                    password, { password = it }, password = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val form = EmployeeForm(
                    name = name.trim(),
                    family = family.trim(),
                    username = username.trim().lowercase(),
                    password = password,
                    phone = cleanDigits(phone),
                    national = cleanDigits(national),
                    hourly = cleanDigits(hourly).toIntOrNull(),
                    leaveLimit = cleanDigits(leave).toIntOrNull(),
                )
                if (form.name.isEmpty() || form.username.isEmpty()) vm.toast("نام و شناسهٔ کاربری الزامی است")
                else vm.saveEmployee(employee?.id, form)
            }) { Text(if (isEdit) "✓ ذخیره" else "＋ افزودن") }
        },
        dismissButton = { TextButton(onClick = { vm.closeEmployeeDialog() }) { Text("انصراف") } },
    )
}

@Composable
private fun TotpDialog(vm: MainViewModel, state: UiState) {
    val t = state.totp ?: return
    val clipboard = LocalClipboardManager.current
    var code by remember { mutableStateOf("") }
    val qr = remember(t.enrollment?.otpauthUri) {
        t.enrollment?.otpauthUri?.let { qrBitmap(it) }
    }
    AlertDialog(
        onDismissRequest = { vm.closeTotp() },
        title = { Text("اتصال Google Authenticator") },
        text = {
            Column {
                Text(t.targetName, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Text(
                    "۱) برنامه Google Authenticator را باز کنید.\n۲) علامت ＋ و سپس اسکن کد QR را بزنید.\n۳) کد ۶ رقمی ایجادشده را وارد کنید:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (qr != null) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = "QR",
                            modifier = Modifier.size(180.dp),
                        )
                    }
                } else {
                    Text("QR ساخته نشد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                Text(t.enrollment?.secret.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                RowOfButtons(
                    "کپی کلید" to {
                        clipboard.setText(AnnotatedString(t.enrollment?.secret.orEmpty()))
                        vm.toast("کپی شد")
                    },
                    "کپی لینک" to {
                        clipboard.setText(AnnotatedString(t.enrollment?.otpauthUri.orEmpty()))
                        vm.toast("کپی شد")
                    },
                )
                Spacer(Modifier.height(12.dp))
                LabeledField("تأیید و تست کد ۶ رقمی", code, { code = it }, "------", numeric = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (code.isBlank()) vm.toast("کد ۶ رقمی را وارد کنید") else vm.confirmEnrollment(code) }) {
                Text("✓ تأیید و فعالسازی")
            }
        },
        dismissButton = {
            RowOfButtons(
                "بستن" to { vm.closeTotp() },
                "ریست" to { vm.resetEnrollment() },
            )
        },
    )
}

@Composable
private fun CodePromptDialog(actionName: String, vm: MainViewModel) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { vm.closeCodePrompt() },
        title = { Text("تأیید $actionName با کد گوگل") },
        text = {
            Column {
                Text("لطفاً کد ۶ رقمی Google Authenticator خود را وارد کنید.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                LabeledField("کد ۶ رقمی", code, { code = it }, "------", numeric = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (code.isBlank()) vm.toast("لطفاً کد ۶ رقمی را وارد کنید")
                else vm.recordAttendance(code)
            }) { Text("✓ تأیید و $actionName") }
        },
        dismissButton = { TextButton(onClick = { vm.closeCodePrompt() }) { Text("انصراف") } },
    )
}

@Composable
private fun RowOfButtons(vararg items: Pair<String, () -> Unit>) {
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
        items.forEach { (label, action) ->
            TextButton(onClick = action, modifier = Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.labelSmall) }
        }
    }
}
