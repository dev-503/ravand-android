package ir.ravand.app.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

internal fun Cursor.str(name: String): String = getString(getColumnIndexOrThrow(name))
internal fun Cursor.optStr(name: String): String? = getString(getColumnIndexOrThrow(name))
internal fun Cursor.inte(name: String): Int = getInt(getColumnIndexOrThrow(name))
internal fun Cursor.lng(name: String): Long = getLong(getColumnIndexOrThrow(name))
internal fun Cursor.dbl(name: String): Double = getDouble(getColumnIndexOrThrow(name))
internal fun Cursor.bool(name: String): Boolean = lng(name) == 1L

/** دیتابیس محلی Ravand — همان طرحواره و دادهٔ نمونهٔ نسخهٔ دسکتاپ. */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS profiles (
              id TEXT PRIMARY KEY,
              username TEXT NOT NULL UNIQUE COLLATE NOCASE,
              first_name TEXT NOT NULL,
              last_name TEXT NOT NULL DEFAULT '',
              password_hash TEXT NOT NULL,
              role TEXT NOT NULL DEFAULT 'employee' CHECK (role IN ('admin','assistant','accountant','employee')),
              phone TEXT,
              national_id TEXT,
              hourly_rate INTEGER,
              leave_limit_minutes INTEGER,
              leave_taken_minutes INTEGER NOT NULL DEFAULT 0,
              month_hours REAL NOT NULL DEFAULT 0,
              today_hours REAL NOT NULL DEFAULT 0,
              overtime_hours REAL NOT NULL DEFAULT 0,
              status TEXT NOT NULL DEFAULT 'خارج‌شده' CHECK (status IN ('حاضر','مرخصی','خارج‌شده')),
              initials TEXT NOT NULL DEFAULT '',
              color TEXT NOT NULL DEFAULT '#dff3eb',
              totp_secret TEXT,
              totp_pending TEXT,
              totp_enabled INTEGER NOT NULL DEFAULT 0,
              created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_profiles_username ON profiles(username)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_profiles_role ON profiles(role)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS attendance_events (
              id TEXT PRIMARY KEY,
              employee_id TEXT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
              kind TEXT NOT NULL CHECK (kind IN ('check_in','check_out','leave_start','leave_end','auto_checkout')),
              occurred_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
              recorded_by TEXT REFERENCES profiles(id),
              note TEXT,
              needs_explanation INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attendance_emp_time ON attendance_events(employee_id, occurred_at)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS payroll_periods (
              id TEXT PRIMARY KEY,
              title TEXT NOT NULL,
              starts_on TEXT NOT NULL,
              ends_on TEXT NOT NULL CHECK (ends_on >= starts_on),
              created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_reports (
              id TEXT PRIMARY KEY,
              employee_id TEXT NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
              work_date TEXT NOT NULL,
              body TEXT NOT NULL,
              created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
              UNIQUE(employee_id, work_date)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS app_settings (
              id INTEGER PRIMARY KEY CHECK (id = 1),
              organization_name TEXT NOT NULL DEFAULT 'نوآوران فردا',
              theme TEXT NOT NULL DEFAULT 'mint' CHECK (theme IN ('mint','dark','blue','brown','mono')),
              font_family TEXT NOT NULL DEFAULT 'arad',
              admin_username TEXT NOT NULL DEFAULT 'admin',
              admin_password_hash TEXT NOT NULL DEFAULT '',
              updated_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )
            """.trimIndent()
        )
        db.execSQL("INSERT OR IGNORE INTO app_settings (id) VALUES (1)")
        db.execSQL("UPDATE app_settings SET font_family = 'arad' WHERE font_family = 'vazirmatn'")
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // نسخهٔ ۱: ارتقایی وجود ندارد.
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("PRAGMA foreign_keys=ON")
        // این pragmaها مقداری برمیگردانند، بنابراین باید با rawQuery اجرا شوند؛
        // execSQL در اندروید فقط برای دستوراتی بدون نتیجه مجاز است.
        db.rawQuery("PRAGMA journal_mode=WAL", null).use { it.moveToFirst() }
        db.rawQuery("PRAGMA synchronous=NORMAL", null).use { it.moveToFirst() }
    }

    private fun seed(db: SQLiteDatabase) {
        val adminHash = queryDb(db, "SELECT admin_password_hash FROM app_settings WHERE id=1") {
            it.str("admin_password_hash")
        }.firstOrNull().orEmpty()
        if (adminHash.isEmpty()) {
            exec(
                db,
                "UPDATE app_settings SET admin_password_hash=? WHERE id=1",
                Crypto.hashPassword("admin")
            )
        }

        val count = queryDb(db, "SELECT COUNT(*) AS n FROM profiles") { it.inte("n") }.firstOrNull() ?: 0
        if (count > 0) return

        // شناسه، نام، نام خانوادگی، نام کاربری، موبایل، وضعیت، کارکرد، امروز، اضافهکاری، دستمزد، سقف مرخصی، مرخصی گرفتهشده
        val rows = listOf(
            SeedRow("EMP-024", "آرمان", "رضایی", "arman.r", "0912 345 6789", "حاضر", 148.5, 6.2, 4.5, 180_000, 24, 6.0),
            SeedRow("EMP-018", "نگار", "احمدی", "negar.a", "0935 120 4488", "حاضر", 136.25, 5.8, 0.0, null, null, 0.0),
            SeedRow("EMP-031", "پارسا", "محمدی", "parsa.m", null, "مرخصی", 122.0, 0.0, 2.0, 210_000, null, 0.0),
            SeedRow("EMP-012", "سارا", "کریمی", "sara.k", "0910 443 2190", "حاضر", 158.75, 7.1, 8.0, null, null, 0.0),
            SeedRow("EMP-027", "کیان", "حسینی", "kian.h", null, "خارج‌شده", 104.5, 4.4, 0.0, null, null, 0.0),
        )
        val defaultHash = Crypto.hashPassword("123456")
        rows.forEachIndexed { i, r ->
            exec(
                db,
                """INSERT INTO profiles
                   (id, username, first_name, last_name, password_hash, role, phone, status,
                    month_hours, today_hours, overtime_hours, hourly_rate,
                    leave_limit_minutes, leave_taken_minutes, initials, color)
                   VALUES (?,?,?,?,?,'employee',?,?,?,?,?,?,?,?,?,?)""",
                r.id, r.username, r.name, r.family, defaultHash, r.phone, r.status,
                r.hours, r.today, r.overtime, r.hourly,
                r.leaveLimit?.times(60), (r.leaveTaken * 60).toLong(),
                r.name.firstOrNull()?.toString() ?: "?", PALETTE[i % PALETTE.size]
            )
        }
        exec(
            db,
            "INSERT INTO payroll_periods (id, title, starts_on, ends_on) VALUES (?,'آبان ۱۴۰۵','2026-10-23','2026-11-21')",
            UUID.randomUUID().toString()
        )
    }

    private data class SeedRow(
        val id: String,
        val name: String,
        val family: String,
        val username: String,
        val phone: String?,
        val status: String,
        val hours: Double,
        val today: Double,
        val overtime: Double,
        val hourly: Int?,
        val leaveLimit: Int?,
        val leaveTaken: Double,
    )

    // ---------- ابزارهای کوچک پرسوجو ----------

    fun <T> query(sql: String, args: Array<String>? = null, mapper: (Cursor) -> T): List<T> =
        queryDb(writableDatabase, sql, args, mapper)

    private fun <T> queryDb(
        db: SQLiteDatabase,
        sql: String,
        args: Array<String>? = null,
        mapper: (Cursor) -> T,
    ): List<T> {
        val out = mutableListOf<T>()
        db.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) out.add(mapper(c))
        }
        return out
    }

    fun <T> queryOne(sql: String, args: Array<String>? = null, mapper: (Cursor) -> T): T? =
        query(sql, args, mapper).firstOrNull()

    fun exec(sql: String, vararg args: Any?) = exec(writableDatabase, sql, *args)

    private fun exec(db: SQLiteDatabase, sql: String, vararg args: Any?) {
        db.execSQL(sql, arrayOf<Any?>(*args))
    }

    companion object {
        private const val DB_NAME = "ravand.db"
        private const val DB_VERSION = 1

        val PALETTE = listOf(
            "#dff3eb", "#f3e8ff", "#fff0d9", "#e1edff",
            "#ffe4e6", "#e9e6ff", "#fdeee8", "#e6f7f2",
        )
    }
}
