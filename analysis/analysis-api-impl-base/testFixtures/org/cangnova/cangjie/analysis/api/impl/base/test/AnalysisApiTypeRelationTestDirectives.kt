package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue
import kotlin.text.toBooleanStrict

/**
 * type relation 能力族专用指令。
 *
 * 左右两侧类型都通过公开 `CaTypeCreator` 构造，
 * 再由 generated 测试统一校验：
 * - `isSubTypeOf`
 * - `semanticallyEquals`
 */
object AnalysisApiTypeRelationTestDirectives : SimpleDirectivesContainer() {
    /**
     * 指定左侧类型的构造种类。
     *
     * 测试会通过公共 type builder 构造左侧 `CaType`，再将其作为类型关系查询的 receiver。
     */
    val LEFT_TYPE_KIND by stringDirective(
        description = "左侧类型的构造种类，例如 CLASS / GENERIC_CLASS / FUNCTION。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定左侧类型的主 class-like 名称。
     *
     * 该名称用于恢复左侧类型构造所需的主要 class symbol。
     */
    val LEFT_TARGET_CLASS by stringDirective(
        description = "左侧类型的主类名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定左侧类型构造需要的第二个 class-like 名称。
     *
     * 该字段服务 tuple、function、union、intersection 等需要多个输入类型的关系测试。
     */
    val LEFT_SECOND_TARGET_CLASS by stringDirective(
        description = "左侧类型的第二个类名，用于 tuple / function / union 等场景。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定左侧泛型 class-like 类型的容器类名。
     *
     * `GENERIC_CLASS` 左侧类型会用该字段恢复容器 symbol 并填入主类型实参。
     */
    val LEFT_CONTAINER_CLASS by stringDirective(
        description = "左侧泛型 class-like 类型的容器类名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定右侧类型的构造种类。
     *
     * 测试会通过同一公共 type builder 构造右侧 `CaType`，作为类型关系查询的 argument。
     */
    val RIGHT_TYPE_KIND by stringDirective(
        description = "右侧类型的构造种类，例如 CLASS / GENERIC_CLASS / FUNCTION。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定右侧类型的主 class-like 名称。
     *
     * 该名称用于恢复右侧类型构造所需的主要 class symbol。
     */
    val RIGHT_TARGET_CLASS by stringDirective(
        description = "右侧类型的主类名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定右侧类型构造需要的第二个 class-like 名称。
     *
     * 该字段服务 tuple、function、union、intersection 等需要多个输入类型的关系测试。
     */
    val RIGHT_SECOND_TARGET_CLASS by stringDirective(
        description = "右侧类型的第二个类名，用于 tuple / function / union 等场景。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定右侧泛型 class-like 类型的容器类名。
     *
     * `GENERIC_CLASS` 右侧类型会用该字段恢复容器 symbol 并填入主类型实参。
     */
    val RIGHT_CONTAINER_CLASS by stringDirective(
        description = "右侧泛型 class-like 类型的容器类名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 `left.isSubTypeOf(right)` 的期望布尔结果。
     *
     * 该字段用于断言公开类型系统的子类型关系查询。
     */
    val EXPECTED_IS_SUBTYPE by stringDirective(
        description = "left.isSubTypeOf(right) 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 `left.semanticallyEquals(right)` 的期望布尔结果。
     *
     * 该字段用于断言类型语义等价关系，和子类型关系分开检查。
     */
    val EXPECTED_SEMANTICALLY_EQUAL by stringDirective(
        description = "left.semanticallyEquals(right) 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取左侧类型构造种类。
 *
 * 返回值会传入公共 type builder，决定左侧 `CaType` 的构造路径。
 */
val RegisteredDirectives.leftTypeKind: String
    get() = singleValue(AnalysisApiTypeRelationTestDirectives.LEFT_TYPE_KIND)

/**
 * 读取左侧类型的主 class-like 名称。
 *
 * 该名称用于恢复构造左侧 `CaType` 所需的主要 symbol。
 */
val RegisteredDirectives.leftTargetClassName: String
    get() = singleValue(AnalysisApiTypeRelationTestDirectives.LEFT_TARGET_CLASS)

/**
 * 读取左侧类型的第二个 class-like 名称。
 *
 * 当左侧类型构造不需要第二输入类型时返回 `null`。
 */
val RegisteredDirectives.leftSecondTargetClassName: String?
    get() = this[AnalysisApiTypeRelationTestDirectives.LEFT_SECOND_TARGET_CLASS].singleOrNull()

/**
 * 读取左侧泛型容器类名。
 *
 * 当左侧类型不是泛型容器构造时返回 `null`。
 */
val RegisteredDirectives.leftContainerClassName: String?
    get() = this[AnalysisApiTypeRelationTestDirectives.LEFT_CONTAINER_CLASS].singleOrNull()

/**
 * 读取右侧类型构造种类。
 *
 * 返回值会传入公共 type builder，决定右侧 `CaType` 的构造路径。
 */
val RegisteredDirectives.rightTypeKind: String
    get() = singleValue(AnalysisApiTypeRelationTestDirectives.RIGHT_TYPE_KIND)

/**
 * 读取右侧类型的主 class-like 名称。
 *
 * 该名称用于恢复构造右侧 `CaType` 所需的主要 symbol。
 */
val RegisteredDirectives.rightTargetClassName: String
    get() = singleValue(AnalysisApiTypeRelationTestDirectives.RIGHT_TARGET_CLASS)

/**
 * 读取右侧类型的第二个 class-like 名称。
 *
 * 当右侧类型构造不需要第二输入类型时返回 `null`。
 */
val RegisteredDirectives.rightSecondTargetClassName: String?
    get() = this[AnalysisApiTypeRelationTestDirectives.RIGHT_SECOND_TARGET_CLASS].singleOrNull()

/**
 * 读取右侧泛型容器类名。
 *
 * 当右侧类型不是泛型容器构造时返回 `null`。
 */
val RegisteredDirectives.rightContainerClassName: String?
    get() = this[AnalysisApiTypeRelationTestDirectives.RIGHT_CONTAINER_CLASS].singleOrNull()

/**
 * 读取子类型关系的期望结果。
 *
 * 访问器使用严格布尔解析，保证非法 testData 值不会被宽松转换。
 */
val RegisteredDirectives.expectedIsSubtype: Boolean
    get() = singleValue(AnalysisApiTypeRelationTestDirectives.EXPECTED_IS_SUBTYPE).toBooleanStrict()

/**
 * 读取语义等价关系的期望结果。
 *
 * 返回值用于断言 `semanticallyEquals`，并与 subtype 断言分开报告。
 */
val RegisteredDirectives.expectedSemanticallyEqual: Boolean
    get() = singleValue(AnalysisApiTypeRelationTestDirectives.EXPECTED_SEMANTICALLY_EQUAL).toBooleanStrict()
