package ir.ravand.app.data

/** تبدیل ارقام فارسی/عربی به انگلیسی و حذف فاصله — معادل cleanDigits در فرانت دسکتاپ. */
fun cleanDigits(value: String?): String {
    if (value == null) return ""
    val fa = "۰۱۲۳۴۵۶۷۸۹"
    val ar = "٠١٢٣٤٥٦٧٨٩"
    val out = StringBuilder()
    for (ch in value) {
        when {
            ch in fa -> out.append(fa.indexOf(ch))
            ch in ar -> out.append(ar.indexOf(ch))
            !ch.isWhitespace() -> out.append(ch)
        }
    }
    return out.toString()
}
