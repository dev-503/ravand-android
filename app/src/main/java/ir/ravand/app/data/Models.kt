package ir.ravand.app.data

// ساختارهای داده — معادل DTOهای ravand-core در نسخهٔ دسکتاپ

data class Employee(
    val id: String,
    val name: String,
    val family: String,
    val username: String,
    val phone: String?,
    val national: String?,
    val status: String,          // حاضر | مرخصی | خارج‌شده
    val hours: Double,
    val today: Double,
    val overtime: Double,
    val hourly: Int?,
    val leaveLimit: Int?,        // ساعت
    val leaveTaken: Double,
    val initials: String,
    val color: String,
    val totpEnabled: Boolean,
    val role: String,
)

data class Settings(
    val theme: String,           // mint | dark | blue | brown | mono
    val font: String,            // arad | vazirmatn
    val orgName: String,
    val adminUsername: String,
)

data class AttendanceEvent(
    val id: String,
    val employeeId: String,
    val kind: String,            // check_in | check_out | leave_start | leave_end | auto_checkout
    val occurredAt: String,
    val recordedBy: String?,
    val note: String?,
)

data class Period(
    val id: String,
    val title: String,
    val startsOn: String,
    val endsOn: String,
)

data class AuthResult(
    val ok: Boolean,
    val message: String,
    val role: String,            // admin | employee | ""
    val employeeId: String?,
    val totpEnrolled: Boolean,
)

data class Enrollment(
    val otpauthUri: String,
    val secret: String,
)

data class Stats(
    val total: Int,
    val present: Int,
    val workedHours: Double,
    val overtime: Double,
)

data class Dashboard(
    val employees: List<Employee>,
    val settings: Settings,
    val stats: Stats,
)

data class EmployeeForm(
    val name: String,
    val family: String,
    val username: String,
    val password: String,
    val phone: String,
    val national: String,
    val hourly: Int?,
    val leaveLimit: Int?,
)

data class SettingsForm(
    val theme: String,
    val font: String,
    val orgName: String,
    val adminUsername: String,
    val adminPassword: String,          // خالی = بدون تغییر
    val currentAdminPassword: String,   // برای احراز هویت هنگام تغییر رمز
)

internal data class AdminRow(val username: String, val hash: String)

internal data class EmpAuthRow(
    val id: String,
    val hash: String,
    val totpSecret: String?,
    val totpPending: String?,
    val totpEnabled: Boolean,
)
