package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue

/**
 * type creator 能力族专用指令。
 *
 * 这组指令把“要构造哪种公开类型”与“期望渲染结果”显式固定下来，
 * 让 generated 测试直接围绕 public `CaTypeCreator` 契约断言，
 * 而不是顺带依赖 renderer preset 的手写回归。
 */
object AnalysisApiTypeCreatorTestDirectives : SimpleDirectivesContainer() {
    val TYPE_CREATION_KIND by stringDirective(
        description = "当前 type creator 用例要走的公开构造入口，例如 CLASS / GENERIC_CLASS / TUPLE / FUNCTION。",
        applicability = DirectiveApplicability.File,
    )

    val SECOND_TARGET_CLASS by stringDirective(
        description = "当前用例中的第二个类名，用于 tuple / intersection / union / function 返回类型等场景。",
        applicability = DirectiveApplicability.File,
    )

    val CONTAINER_CLASS by stringDirective(
        description = "当前用例中的泛型容器类名，用于 GENERIC_CLASS 场景。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_QUALIFIED_TYPE_RENDER by stringDirective(
        description = "以 qualified names 渲染构造后类型时的期望文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_SHORT_TYPE_RENDER by stringDirective(
        description = "以 short names 渲染构造后类型时的期望文本。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.typeCreationKind: String
    get() = singleValue(AnalysisApiTypeCreatorTestDirectives.TYPE_CREATION_KIND)

val RegisteredDirectives.secondTargetClassName: String?
    get() = this[AnalysisApiTypeCreatorTestDirectives.SECOND_TARGET_CLASS].singleOrNull()

val RegisteredDirectives.containerClassName: String?
    get() = this[AnalysisApiTypeCreatorTestDirectives.CONTAINER_CLASS].singleOrNull()

val RegisteredDirectives.expectedQualifiedTypeRender: String
    get() = this[AnalysisApiTypeCreatorTestDirectives.EXPECTED_QUALIFIED_TYPE_RENDER].restoreTypeRenderExpectation()

val RegisteredDirectives.expectedShortTypeRender: String
    get() = this[AnalysisApiTypeCreatorTestDirectives.EXPECTED_SHORT_TYPE_RENDER].restoreTypeRenderExpectation()

/**
 * test directives 在解析时会把逗号分隔和空白分隔都拆成多个 token。
 *
 * type creator 这组用例里既有：
 * - `Box<User>` 这类需要按 `, ` 还原的文本
 * - `A & B`、`(A) -> B` 这类需要按空格还原的文本
 *
 * 因此这里统一在测试基建层恢复期望文本，避免让每个 abstract test 重复发明一套拼接规则。
 */
private fun List<String>.restoreTypeRenderExpectation(): String {
    if (isEmpty()) error("Type render expectation cannot be empty.")
    if (size == 1) return single()

    val containsOperator = any { token ->
        token == "&" || token == "|" || token == "->"
    }
    val startsWithFunctionKind = first() == "cfunc" || first() == "closure"

    return if (containsOperator || startsWithFunctionKind) {
        buildString {
            this@restoreTypeRenderExpectation.forEachIndexed { index, token ->
                if (index == 0) {
                    append(token)
                } else {
                    val previousToken = this@restoreTypeRenderExpectation[index - 1]
                    val separator = if (token.startsWith("...)") && !previousToken.endsWith(",")) ", " else " "
                    append(separator)
                    append(token)
                }
            }
        }
    } else {
        joinToString(", ")
    }
}
