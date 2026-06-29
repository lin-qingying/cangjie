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
    /**
     * 指定当前 type creator 用例要调用的公开类型构造入口。
     *
     * 测试基类根据该字段选择 class、generic class、tuple、function、union、intersection 等
     * 不同 `CaTypeCreator` 路径，保证构造行为由 testData 明确驱动。
     */
    val TYPE_CREATION_KIND by stringDirective(
        description = "当前 type creator 用例要走的公开构造入口，例如 CLASS / GENERIC_CLASS / TUPLE / FUNCTION。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定当前用例中的第二个 class-like 目标。
     *
     * 该字段服务 tuple、intersection、union 以及 function 返回类型等需要两个输入类型的构造场景。
     */
    val SECOND_TARGET_CLASS by stringDirective(
        description = "当前用例中的第二个类名，用于 tuple / intersection / union / function 返回类型等场景。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定泛型类型构造场景中的容器 class-like 名称。
     *
     * `GENERIC_CLASS` 用例通过该字段恢复容器 symbol，并把主目标类型作为 type argument 注入。
     */
    val CONTAINER_CLASS by stringDirective(
        description = "当前用例中的泛型容器类名，用于 GENERIC_CLASS 场景。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定持有目标类型参数的 class-like 声明名称。
     *
     * `TYPE_PARAMETER` 用例通过该字段先恢复 owner symbol，再在 owner 的类型参数列表中查找目标参数。
     */
    val TYPE_PARAMETER_OWNER_CLASS by stringDirective(
        description = "当前用例中持有目标类型参数的 class-like 声明名，用于 TYPE_PARAMETER 场景。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定要恢复并构造成公开类型的类型参数名称。
     *
     * 该字段与 owner class 配对使用，避免同名类型参数在不同声明作用域之间被混淆。
     */
    val TARGET_TYPE_PARAMETER by stringDirective(
        description = "当前用例中要恢复并构造成 public type 的类型参数名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录构造后类型使用 qualified names 渲染时的期望文本。
     *
     * 该期望用于校验类型构造本身的完整符号身份，避免 short-name 渲染掩盖包名或容器差异。
     */
    val EXPECTED_QUALIFIED_TYPE_RENDER by stringDirective(
        description = "以 qualified names 渲染构造后类型时的期望文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录构造后类型使用 short names 渲染时的期望文本。
     *
     * 该期望用于校验用户可见的简短类型展示是否与构造结果一致。
     */
    val EXPECTED_SHORT_TYPE_RENDER by stringDirective(
        description = "以 short names 渲染构造后类型时的期望文本。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取 type creator 用例的构造种类。
 *
 * 返回值驱动 `AnalysisApiTypeTestSupport.buildType` 选择具体公开构造入口。
 */
val RegisteredDirectives.typeCreationKind: String
    get() = singleValue(AnalysisApiTypeCreatorTestDirectives.TYPE_CREATION_KIND)

/**
 * 读取第二个目标类名的可选值。
 *
 * 当当前构造种类不需要第二类型输入时返回 `null`。
 */
val RegisteredDirectives.secondTargetClassName: String?
    get() = this[AnalysisApiTypeCreatorTestDirectives.SECOND_TARGET_CLASS].singleOrNull()

/**
 * 读取泛型容器类名的可选值。
 *
 * 该值只在 `GENERIC_CLASS` 等需要容器 symbol 的场景中存在。
 */
val RegisteredDirectives.containerClassName: String?
    get() = this[AnalysisApiTypeCreatorTestDirectives.CONTAINER_CLASS].singleOrNull()

/**
 * 读取类型参数 owner 类名的可选值。
 *
 * 该值与 `targetTypeParameterName` 配合，定位 TYPE_PARAMETER 场景中的具体类型参数 symbol。
 */
val RegisteredDirectives.typeParameterOwnerClassName: String?
    get() = this[AnalysisApiTypeCreatorTestDirectives.TYPE_PARAMETER_OWNER_CLASS].singleOrNull()

/**
 * 读取目标类型参数名称的可选值。
 *
 * 当构造种类不是 `TYPE_PARAMETER` 时返回 `null`。
 */
val RegisteredDirectives.targetTypeParameterName: String?
    get() = this[AnalysisApiTypeCreatorTestDirectives.TARGET_TYPE_PARAMETER].singleOrNull()

/**
 * 读取 qualified type renderer 的期望输出。
 *
 * 访问器负责把 directive token 恢复为完整类型文本，避免具体测试重复处理逗号、函数箭头和交并类型符号。
 */
val RegisteredDirectives.expectedQualifiedTypeRender: String
    get() = this[AnalysisApiTypeCreatorTestDirectives.EXPECTED_QUALIFIED_TYPE_RENDER].restoreTypeRenderExpectation()

/**
 * 读取 short type renderer 的期望输出。
 *
 * 返回值用于断言构造出的公开类型在短名称渲染 preset 下的用户可见文本。
 */
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
