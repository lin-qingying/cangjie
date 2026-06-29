import java.util.Locale

/**
 * 使用 ROOT locale 将字符串首字符转换为标题大小写。
 */
fun String.capitalize(): String = capitalize(Locale.ROOT)

/**
 * 使用指定 locale 将字符串首字符转换为标题大小写。
 */
fun String.capitalize(locale: Locale): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
