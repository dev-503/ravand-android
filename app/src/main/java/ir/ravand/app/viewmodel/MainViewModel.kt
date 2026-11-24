package ir.ravand.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.ravand.app.data.AttendanceEvent
import ir.ravand.app.data.Dashboard
import ir.ravand.app.data.Employee
import ir.ravand.app.data.EmployeeForm
import ir.ravand.app.data.Enrollment
import ir.ravand.app.data.Period
import ir.ravand.app.data.Repository
import ir.ravand.app.data.SettingsForm
import ir.ravand.app.data.cleanDigits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class EmployeeDialogState(val employee: Employee?)

data class TotpState(
    val targetId: String,
    val targetName: String,
    val enrollment: Enrollment? = null,
)

data class CodePromptState(val status: String, val actionName: String)

data class UiState(
    val loading: Boolean = true,
    val error: String? = null,
    val dashboard: Dashboard? = null,
    val periods: List<Period> = emptyList(),
    val events: List<AttendanceEvent> = emptyList(),

    val logged: Boolean = false,
    val role: String = "",
    val employeeId: String? = null,
    val screen: String = "پیشخوان",

    val loginMode: String = "admin",
    val loginError: String = "",
    val adminUser: String = "admin",
    val adminPass: String = "",
    val empUser: String = "",
    val empCode: String = "",

    val query: String = "",
    val statusFilter: String = "همه",
    val selectedMonth: String = "آبان ۱۴۰۵",

    // پیشنمایش فوری تم/فونت قبل از ذخیره
    val themePreview: String? = null,
    val fontPreview: String? = null,

    val toast: String? = null,
    val employeeDialog: EmployeeDialogState? = null,
    val deleteTarget: Employee? = null,
    val totp: TotpState? = null,
    val codePrompt: CodePromptState? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app.applicationContext)
    private var _state = UiState()
    var state: UiState = _state
        private set

    private fun update(block: (UiState) -> UiState) {
        _state = block(_state)
        state = _state
        // انتشار وضعیت جدید به رابط Compose از طریق StateFlow
        _stateFlow.value = _state
    }

    private val _stateFlow = kotlinx.coroutines.flow.MutableStateFlow(_state)
    val stateFlow: kotlinx.coroutines.flow.StateFlow<UiState> = _stateFlow

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val dash = io { repo.loadDashboard() }
                val periods = io { repo.listPeriods() }
                update { it.copy(dashboard = dash, periods = periods, loading = false, error = null) }
            } catch (e: Exception) {
                update { it.copy(loading = false, error = e.message ?: "خطا در باز کردن دیتابیس رَوَند") }
            }
        }
    }

    fun toast(message: String) = update { it.copy(toast = message) }
    fun clearToast() = update { it.copy(toast = null) }

    // ---------- ورود ----------

    fun setLoginMode(mode: String) = update { it.copy(loginMode = mode, loginError = "") }
    fun setAdminUser(v: String) = update { it.copy(adminUser = v) }
    fun setAdminPass(v: String) = update { it.copy(adminPass = v) }
    fun setEmpUser(v: String) = update { it.copy(empUser = v) }
    fun setEmpCode(v: String) = update { it.copy(empCode = v) }

    fun adminLogin() {
        viewModelScope.launch {
            try {
                val r = io { repo.adminLogin(state.adminUser, state.adminPass) }
                if (r.ok) {
                    update { it.copy(logged = true, role = "admin", loginError = "", screen = "پیشخوان") }
                    toast("خوش آمدید")
                } else {
                    update { it.copy(loginError = r.message) }
                }
            } catch (e: Exception) {
                update { it.copy(loginError = e.message ?: "خطای ورود") }
            }
        }
    }

    fun employeeLogin() {
        viewModelScope.launch {
            try {
                val code = cleanDigits(state.empCode)
                val r = io { repo.employeeLogin(state.empUser, code) }
                if (r.ok) {
                    val events = io { repo.listEvents(r.employeeId.orEmpty()) }
                    update {
                        it.copy(
                            logged = true,
                            role = "employee",
                            employeeId = r.employeeId,
                            events = events,
                            loginError = "",
                            empCode = code,
                        )
                    }
                    toast("خوش آمدید")
                } else {
                    update { it.copy(loginError = r.message) }
                }
            } catch (e: Exception) {
                update { it.copy(loginError = e.message ?: "خطای ورود") }
            }
        }
    }

    fun logout() = update {
        it.copy(logged = false, role = "", employeeId = null, loginError = "", screen = "پیشخوان")
    }

    // ---------- ناوبری و فیلتر ----------

    fun navigate(screen: String) = update { it.copy(screen = screen) }
    fun setQuery(v: String) = update { it.copy(query = v) }
    fun setStatusFilter(v: String) = update { it.copy(statusFilter = v) }
    fun setMonth(v: String) = update { it.copy(selectedMonth = v) }

    fun setThemePreview(id: String) = update { it.copy(themePreview = id) }
    fun setFontPreview(id: String) = update { it.copy(fontPreview = id) }

    // ---------- عملیات کارکنان ----------

    fun setStatus(employeeId: String, status: String) {
        viewModelScope.launch {
            try {
                io { repo.setStatus(employeeId, status) }
                refresh()
                toast(
                    when (status) {
                        "حاضر" -> "ورود ثبت شد"
                        "مرخصی" -> "مرخصی ثبت شد"
                        else -> "خروج ثبت شد"
                    }
                )
            } catch (e: Exception) {
                toast(e.message ?: "خطا در ثبت وضعیت")
            }
        }
    }

    fun openEmployeeDialog(employee: Employee?) =
        update { it.copy(employeeDialog = EmployeeDialogState(employee)) }

    fun closeEmployeeDialog() = update { it.copy(employeeDialog = null) }

    fun saveEmployee(id: String?, form: EmployeeForm) {
        viewModelScope.launch {
            try {
                io { if (id == null) repo.addEmployee(form) else repo.updateEmployee(id, form) }
                update { it.copy(employeeDialog = null) }
                refresh()
                toast(if (id == null) "کارمند جدید با موفقیت افزوده شد" else "اطلاعات کارمند بهروزرسانی شد")
            } catch (e: Exception) {
                toast(e.message ?: "خطا در ذخیرهٔ کارمند")
            }
        }
    }

    fun openDelete(employee: Employee) = update { it.copy(deleteTarget = employee) }
    fun closeDelete() = update { it.copy(deleteTarget = null) }

    fun deleteEmployee() {
        val target = state.deleteTarget ?: return
        viewModelScope.launch {
            try {
                io { repo.deleteEmployee(target.id) }
                update { it.copy(deleteTarget = null) }
                refresh()
                toast("کارمند حذف شد")
            } catch (e: Exception) {
                toast(e.message ?: "خطا در حذف کارمند")
            }
        }
    }

    // ---------- تنظیمات و دورهها ----------

    fun saveSettings(form: SettingsForm) {
        viewModelScope.launch {
            try {
                io { repo.updateSettings(form) }
                update { it.copy(themePreview = null, fontPreview = null) }
                refresh()
                toast("تنظیمات ذخیره شد")
            } catch (e: Exception) {
                toast(e.message ?: "خطا در ذخیرهٔ تنظیمات")
            }
        }
    }

    fun addPeriod(title: String, starts: String, ends: String) {
        viewModelScope.launch {
            try {
                io { repo.addPeriod(title, starts, ends) }
                val periods = io { repo.listPeriods() }
                update { it.copy(periods = periods) }
                toast("دوره جدید افزوده شد")
            } catch (e: Exception) {
                toast(e.message ?: "خطا در افزودن دوره")
            }
        }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            try {
                val csv = io { repo.exportCsv() }
                val file = withContext(Dispatchers.IO) {
                    val dir = File(context.filesDir, "exports").apply { mkdirs() }
                    val out = File(dir, "ravand-attendance-${state.selectedMonth}.csv")
                    out.writeText(csv, Charsets.UTF_8)
                    out
                }
                val uri = FileProvider.getUriForFile(
                    context.applicationContext,
                    "${context.packageName}.fileprovider",
                    file
                )
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "گزارش حضور و غیاب رَوَند")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(send, "ذخیره یا اشتراکگذاری گزارش")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.applicationContext.startActivity(chooser)
                toast("گزارش ماهانه دریافت شد")
            } catch (e: Exception) {
                toast(e.message ?: "خطا در ایجاد گزارش")
            }
        }
    }

    // ---------- TOTP ----------

    fun startEnrollment(targetId: String, targetName: String) {
        viewModelScope.launch {
            try {
                val enr = io { repo.totpStartEnrollment(targetId) }
                update { it.copy(totp = TotpState(targetId.trim(), targetName, enr)) }
            } catch (e: Exception) {
                toast(e.message ?: "کارمند با این شناسه یافت نشد")
            }
        }
    }

    fun closeTotp() = update { it.copy(totp = null) }

    fun confirmEnrollment(code: String) {
        val t = state.totp ?: return
        viewModelScope.launch {
            try {
                val ok = io { repo.totpConfirmEnrollment(t.targetId, cleanDigits(code)) }
                if (!ok) {
                    toast("کد واردشده معتبر نیست. لطفاً مجدداً امتحان کنید.")
                    return@launch
                }
                if (state.logged) {
                    update { it.copy(totp = null) }
                    refresh()
                } else {
                    update { it.copy(totp = null, empCode = cleanDigits(code), loginError = "") }
                }
                toast("Google Authenticator با موفقیت فعال شد ✓")
            } catch (e: Exception) {
                toast(e.message ?: "خطا در تأیید کد")
            }
        }
    }

    fun resetEnrollment() {
        val t = state.totp ?: return
        viewModelScope.launch {
            try {
                io { repo.totpReset(t.targetId) }
                update { it.copy(totp = null) }
                refresh()
                toast("Authenticator ریست شد")
            } catch (e: Exception) {
                toast(e.message ?: "خطا در ریست Authenticator")
            }
        }
    }

    // ---------- پنل کارمند ----------

    fun openCodePrompt(status: String, actionName: String) =
        update { it.copy(codePrompt = CodePromptState(status, actionName)) }

    fun closeCodePrompt() = update { it.copy(codePrompt = null) }

    fun recordAttendance(code: String) {
        val prompt = state.codePrompt ?: return
        val empId = state.employeeId ?: return
        viewModelScope.launch {
            try {
                io { repo.employeeRecordAttendance(empId, prompt.status, cleanDigits(code)) }
                update { it.copy(codePrompt = null) }
                val events = io { repo.listEvents(empId) }
                update { it.copy(events = events) }
                refresh()
                toast("${prompt.actionName} با موفقیت ثبت شد ✓")
            } catch (e: Exception) {
                toast(e.message ?: "خطا در ثبت حضور")
            }
        }
    }
}
