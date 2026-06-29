package org.cangnova.cangjie.utils
/**
 * 保存 `DEFAULT_LINE_SEPARATOR`，供测试基础设施在测试执行期间读取或传递。
 */
private const val DEFAULT_LINE_SEPARATOR = "\n"



/**
 * 执行 `convertLineSeparators` 对应的测试基础设施流程，维持测试框架的阶段契约。
 */
fun String.convertLineSeparators(separator: String = DEFAULT_LINE_SEPARATOR): String {
    return replace(Regex.fromLiteral("\r\n|\r|\n"), separator)
}
/**
 * 执行 `trimTrailingWhitespacesAndAddNewlineAtEOF` 对应的测试基础设施流程，维持测试框架的阶段契约。
 */
fun String.trimTrailingWhitespacesAndAddNewlineAtEOF(): String =
    this.trimTrailingWhitespaces().let { result -> if (result.endsWith("\n")) result else result + "\n" }
/**
 * 执行 `trimTrailingWhitespaces` 对应的测试基础设施流程，维持测试框架的阶段契约。
 */
fun String.trimTrailingWhitespaces(): String =
    this.split('\n').joinToString(separator = "\n") { it.trimEnd() }
