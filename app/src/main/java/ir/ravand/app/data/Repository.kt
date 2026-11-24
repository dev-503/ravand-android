package ir.ravand.app.data

import android.content.Context
import java.util.UUID
import kotlin.random.Random

/** لایهٔ عملیات — معادل ravand-core::db + دستورهای Tauri. */
class Repository(context: Context) {
    private val db = DatabaseHelper(context)

    // ---------- خواندن ----------

    fun loadDashboard(): Dashboard {
        val employees = listEmployees()
        return Dashboard(
            employees = employees,
            settings = getSettings(),
            stats = Stats(
                total = employees.size,
                present = employees.count { it.status == "حاضر" },
                workedHours = employees.sumOf { it.hours },
                overtime = employees.sumOf { it.overtime },
            ),
        )
    }

    fun listEmployees(): List<Employee> = db.query(
        """SELECT id, username, first_name, last_name, phone, national_id, status,
                  month_hours, today_hours, overtime_hours, hourly_rate,
                  leave_limit_minutes, leave_taken_minutes, initials, color,
                  totp_enabled, role
           FROM profiles WHERE role='employee' ORDER BY created_at DESC, id"""
    ) { c ->
        val leaveLimitMin = c.optStr("leave_limit_minutes")?.toLongOrNull()
        Employee(
            id = c.str("id"),
            name = c.str("first_name"),
            family = c.str("last_name"),
            username = c.str("username"),
            phone = c.optStr("phone"),
            national = c.optStr("national_id"),
            status = c.str("status"),
            hours = c.dbl("month_hours"),
            today = c.dbl("today_hours"),
            overtime = c.dbl("overtime_hours"),
            hourly = c.optStr("hourly_rate")?.toIntOrNull(),
            leaveLimit = leaveLimitMin?.div(60)?.toInt(),
            leaveTaken = c.lng("leave_taken_minutes") / 60.0,
            initials = c.str("initials"),
            color = c.str("color"),
            totpEnabled = c.bool("totp_enabled"),
            role = c.str("role"),
        )
    }

    fun getSettings(): Settings = db.queryOne(
        "SELECT theme, font_family, organization_name, admin_username FROM app_settings WHERE id=1"
    ) { c ->
        Settings(
            theme = c.str("theme"),
            font = c.str("font_family"),
            orgName = c.str("organization_name"),
            adminUsername = c.str("admin_username"),
        )
    } ?: Settings("mint", "arad", "نوآوران فردا", "admin")

    fun listEvents(employeeId: String, limit: Int = 50): List<AttendanceEvent> = db.query(
        """SELECT id, employee_id, kind, occurred_at, recorded_by, note
           FROM attendance_events WHERE employee_id=? ORDER BY occurred_at DESC LIMIT ?""",
        arrayOf(employeeId, limit.toString())
    ) { c ->
        AttendanceEvent(
            id = c.str("id"),
            employeeId = c.str("employee_id"),
            kind = c.str("kind"),
            occurredAt = c.str("occurred_at"),
            recordedBy = c.optStr("recorded_by"),
            note = c.optStr("note"),
        )
    }

    fun listPeriods(): List<Period> = db.query(
        "SELECT id, title, starts_on, ends_on FROM payroll_periods ORDER BY starts_on DESC"
    ) { c ->
        Period(
            id = c.str("id"),
            title = c.str("title"),
            startsOn = c.str("starts_on"),
            endsOn = c.str("ends_on"),
        )
    }

    // ---------- احراز هویت ----------

    fun adminLogin(username: String, password: String): AuthResult {
        val admin = getAdmin()
        return if (username.trim() == admin.username && Crypto.verifyPassword(password, admin.hash)) {
            AuthResult(true, "خوش آمدید", "admin", null, true)
        } else {
            AuthResult(false, "شناسه یا رمز عبور نادرست است", "", null, false)
        }
    }

    fun employeeLogin(username: String, totpCode: String): AuthResult {
        val uname = username.trim()
        if (uname.isEmpty()) {
            return AuthResult(false, "شناسه کاربری را وارد کنید", "", null, false)
        }
        val code = cleanDigits(totpCode).trim()
        if (code.isEmpty()) {
            return AuthResult(false, "کد ۶ رقمی Google Authenticator را وارد کنید", "", null, false)
        }
        val row = getEmployeeAuth(uname)
            ?: return AuthResult(false, "کارمند با این شناسه یافت نشد", "", null, false)

        var verified = false
        var needActivation = false

        row.totpSecret?.let { if (Crypto.verifyTotp(it, code)) verified = true }
        if (!verified) {
            row.totpPending?.let {
                if (Crypto.verifyTotp(it, code)) {
                    verified = true
                    needActivation = true
                }
            }
        }
        // کد اولیهٔ پیشفرض تا زمانی که Authenticator فعال نشده است
        if (!verified && !row.totpEnabled && code == "123456") verified = true

        if (!verified) {
            val message = if (row.totpEnabled) {
                "کد Google Authenticator نادرست یا منقضی شده است"
            } else {
                "کد واردشده معتبر نیست (کد اولیه: ۱۲۳۴۵۶ یا اسکن بارکد در اپ Authenticator)"
            }
            return AuthResult(false, message, "", null, row.totpEnabled)
        }
        if (needActivation) activatePendingTotp(row.id)
        return AuthResult(true, "خوش آمدید", "employee", row.id, row.totpEnabled || needActivation)
    }

    private fun getAdmin(): AdminRow = db.queryOne(
        "SELECT admin_username, admin_password_hash FROM app_settings WHERE id=1"
    ) { c ->
        AdminRow(c.str("admin_username"), c.str("admin_password_hash"))
    } ?: AdminRow("admin", "")

    private fun getEmployeeAuth(username: String): EmpAuthRow? = db.queryOne(
        """SELECT id, password_hash, totp_secret, totp_pending, totp_enabled
           FROM profiles WHERE (username=? OR id=?) AND role='employee' LIMIT 1""",
        arrayOf(username.trim(), username.trim())
    ) { c ->
        EmpAuthRow(
            id = c.str("id"),
            hash = c.str("password_hash"),
            totpSecret = c.optStr("totp_secret"),
            totpPending = c.optStr("totp_pending"),
            totpEnabled = c.bool("totp_enabled"),
        )
    }

    // ---------- نوشتن: کارکنان ----------

    fun addEmployee(f: EmployeeForm): String {
        require(f.name.isNotBlank() && f.username.isNotBlank()) { "نام و شناسهٔ کاربری الزامی است" }
        val password = f.password.ifBlank { "123456" }
        var id: String
        do {
            id = "EMP-%03d".format(Random.nextInt(100, 1000))
        } while (db.queryOne("SELECT COUNT(*) AS n FROM profiles WHERE id=?", arrayOf(id)) { it.inte("n") } != 0)
        val hash = Crypto.hashPassword(password)
        val leaveMin = f.leaveLimit?.times(60)
        db.exec(
            """INSERT INTO profiles
               (id, username, first_name, last_name, password_hash, role, phone, national_id,
                hourly_rate, leave_limit_minutes, initials, color)
               VALUES (?,?,?,?,?,'employee',?,?,?,?,?,?)""",
            id, f.username.trim(), f.name.trim(), f.family.trim(), hash,
            nz(f.phone), nz(f.national), f.hourly, leaveMin,
            f.name.trim().firstOrNull()?.toString() ?: "?",
            DatabaseHelper.PALETTE[Random.nextInt(DatabaseHelper.PALETTE.size)]
        )
        return id
    }

    fun updateEmployee(id: String, f: EmployeeForm) {
        require(f.name.isNotBlank() && f.username.isNotBlank()) { "نام و شناسهٔ کاربری الزامی است" }
        val initials = f.name.trim().firstOrNull()?.toString() ?: "?"
        val leaveMin = f.leaveLimit?.times(60)
        if (f.password.isBlank()) {
            db.exec(
                """UPDATE profiles SET username=?, first_name=?, last_name=?, phone=?, national_id=?,
                   hourly_rate=?, leave_limit_minutes=?, initials=? WHERE id=?""",
                f.username.trim(), f.name.trim(), f.family.trim(), nz(f.phone), nz(f.national),
                f.hourly, leaveMin, initials, id
            )
        } else {
            db.exec(
                """UPDATE profiles SET username=?, first_name=?, last_name=?, password_hash=?, phone=?,
                   national_id=?, hourly_rate=?, leave_limit_minutes=?, initials=? WHERE id=?""",
                f.username.trim(), f.name.trim(), f.family.trim(), Crypto.hashPassword(f.password),
                nz(f.phone), nz(f.national), f.hourly, leaveMin, initials, id
            )
        }
    }

    fun deleteEmployee(id: String) {
        db.exec("DELETE FROM profiles WHERE id=? AND role='employee'", id)
    }

    fun setStatus(employeeId: String, status: String) {
        require(status in listOf("حاضر", "مرخصی", "خارج‌شده")) { "status نامعتبر" }
        db.exec("UPDATE profiles SET status=? WHERE id=?", status, employeeId)
        val kind = when (status) {
            "حاضر" -> "check_in"
            "مرخصی" -> "leave_start"
            else -> "check_out"
        }
        db.exec(
            "INSERT INTO attendance_events (id, employee_id, kind) VALUES (?,?,?)",
            UUID.randomUUID().toString(), employeeId, kind
        )
    }

    fun employeeRecordAttendance(employeeId: String, status: String, totpCode: String) {
        val code = cleanDigits(totpCode).trim()
        if (code.isEmpty()) throw IllegalArgumentException("کد ۶ رقمی Google Authenticator را وارد کنید")
        val row = getEmployeeAuth(employeeId) ?: throw IllegalArgumentException("کارمند یافت نشد")

        var verified = false
        var needActivation = false
        row.totpSecret?.let { if (Crypto.verifyTotp(it, code)) verified = true }
        if (!verified) {
            row.totpPending?.let {
                if (Crypto.verifyTotp(it, code)) {
                    verified = true
                    needActivation = true
                }
            }
        }
        if (!verified && !row.totpEnabled && code == "123456") verified = true
        if (!verified) throw IllegalArgumentException("کد Google Authenticator نادرست یا منقضی شده است")
        if (needActivation) activatePendingTotp(row.id)
        setStatus(row.id, status)
    }

    // ---------- نوشتن: تنظیمات و دورهها ----------

    fun updateSettings(f: SettingsForm) {
        db.exec(
            """UPDATE app_settings SET theme=?, font_family=?, organization_name=?, admin_username=?,
               updated_at=datetime('now','localtime') WHERE id=1""",
            f.theme, f.font, f.orgName.trim(), f.adminUsername.trim()
        )
        if (f.adminPassword.isNotBlank()) {
            val currentHash = db.queryOne(
                "SELECT admin_password_hash FROM app_settings WHERE id=1"
            ) { it.str("admin_password_hash") }.orEmpty()
            if (!Crypto.verifyPassword(f.currentAdminPassword, currentHash)) {
                throw IllegalArgumentException("رمز فعلی مدیر نادرست است")
            }
            db.exec(
                "UPDATE app_settings SET admin_password_hash=? WHERE id=1",
                Crypto.hashPassword(f.adminPassword)
            )
        }
    }

    fun addPeriod(title: String, starts: String, ends: String) {
        db.exec(
            "INSERT INTO payroll_periods (id, title, starts_on, ends_on) VALUES (?,?,?,?)",
            UUID.randomUUID().toString(), title.trim(), starts, ends
        )
    }

    fun exportCsv(): String {
        val lines = mutableListOf(
            "نام,نام خانوادگی,نام کاربری,کد ملی,کد پرسنلی,شماره همراه,وضعیت,ساعت کارکرد,اضافهکاری,دستمزد ساعتی (تومان)"
        )
        listEmployees().forEach { e ->
            val cells = listOf(
                e.name, e.family, e.username, e.national.orEmpty(), e.id,
                e.phone.orEmpty(), e.status, e.hours.toString(), e.overtime.toString(),
                e.hourly?.toString().orEmpty()
            )
            lines.add(cells.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
        }
        return "\uFEFF" + lines.joinToString("\r\n")
    }

    // ---------- TOTP ----------

    fun totpStartEnrollment(employeeIdOrUsername: String): Enrollment {
        val key = employeeIdOrUsername.trim()
        val row = db.queryOne(
            "SELECT id, username, totp_secret FROM profiles WHERE id=? OR username=? LIMIT 1",
            arrayOf(key, key)
        ) { c -> Triple(c.str("id"), c.str("username"), c.optStr("totp_secret")) }
            ?: throw IllegalArgumentException("کارمند با این شناسه یافت نشد")

        val secret = row.third?.takeIf { it.isNotBlank() } ?: Crypto.generateSecret().also { newSecret ->
            db.exec("UPDATE profiles SET totp_pending=? WHERE id=?", newSecret, row.first)
        }
        return Enrollment(otpauthUri = Crypto.otpauthUri(row.second, secret), secret = secret)
    }

    fun totpConfirmEnrollment(employeeIdOrUsername: String, code: String): Boolean {
        val key = employeeIdOrUsername.trim()
        val row = db.queryOne(
            "SELECT id, totp_pending, totp_secret FROM profiles WHERE id=? OR username=? LIMIT 1",
            arrayOf(key, key)
        ) { c -> Triple(c.str("id"), c.optStr("totp_pending"), c.optStr("totp_secret")) }
            ?: return false
        val candidate = row.second?.takeIf { it.isNotBlank() } ?: row.third
        val secret = candidate ?: return false
        if (!Crypto.verifyTotp(secret, code)) return false
        db.exec(
            "UPDATE profiles SET totp_secret=?, totp_pending=NULL, totp_enabled=1 WHERE id=?",
            secret, row.first
        )
        return true
    }

    fun totpReset(employeeIdOrUsername: String) {
        db.exec(
            "UPDATE profiles SET totp_secret=NULL, totp_pending=NULL, totp_enabled=0 WHERE id=? OR username=?",
            employeeIdOrUsername.trim(), employeeIdOrUsername.trim()
        )
    }

    private fun activatePendingTotp(id: String) {
        db.exec(
            "UPDATE profiles SET totp_secret=totp_pending, totp_pending=NULL, totp_enabled=1 WHERE id=?",
            id
        )
    }

    private fun nz(s: String): String? = s.trim().takeIf { it.isNotEmpty() }
}
