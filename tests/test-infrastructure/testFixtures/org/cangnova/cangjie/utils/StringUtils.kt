package org.cangnova.cangjie.utils
private const val DEFAULT_LINE_SEPARATOR = "\n"



fun String.convertLineSeparators(separator: String = DEFAULT_LINE_SEPARATOR): String {
    return replace(Regex.fromLiteral("\r\n|\r|\n"), separator)
}
fun String.trimTrailingWhitespacesAndAddNewlineAtEOF(): String =
    this.trimTrailingWhitespaces().let { result -> if (result.endsWith("\n")) result else result + "\n" }
fun String.trimTrailingWhitespaces(): String =
    this.split('\n').joinToString(separator = "\n") { it.trimEnd() }
