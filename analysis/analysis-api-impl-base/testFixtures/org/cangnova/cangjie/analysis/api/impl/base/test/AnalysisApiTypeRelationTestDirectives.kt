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
    val LEFT_TYPE_KIND by stringDirective(
        description = "左侧类型的构造种类，例如 CLASS / GENERIC_CLASS / FUNCTION。",
        applicability = DirectiveApplicability.File,
    )

    val LEFT_TARGET_CLASS by stringDirective(
        description = "左侧类型的主类名。",
        applicability = DirectiveApplicability.File,
    )

    val LEFT_SECOND_TARGET_CLASS by stringDirective(
        description = "左侧类型的第二个类名，用于 tuple / function / union 等场景。",
        applicability = DirectiveApplicability.File,
    )

    val LEFT_CONTAINER_CLASS by stringDirective(
        description = "左侧泛型 class-like 类型的容器类名。",
        applicability = DirectiveApplicability.File,
    )

    val RIGHT_TYPE_KIND by stringDirective(
        description = "右侧类型的构造种类，例如 CLASS / GENERIC_CLASS / FUNCTION。",
        applicability = DirectiveApplicability.File,
    )

    val RIGHT_TARGET_CLASS by stringDirective(
        description = "右侧类型的主类名。",
        applicability = DirectiveApplicability.File,
    )

    val RIGHT_SECOND_TARGET_CLASS by stringDirective(
        description = "右侧类型的第二个类名，用于 tuple / function / union 等场景。",
        applicability = DirectiveApplicability.File,
    )

    val RIGHT_CONTAINER_CLASS by stringDirective(
        description = "右侧泛型 class-like 类型的容器类名。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_IS_SUBTYPE by stringDirective(
        description = "left.isSubTypeOf(right) 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_SEMANTICALLY_EQUAL by stringDirective(
        description = "left.semanticallyEquals(right) 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.leftTypeKind: String
    get() = singleValue(AnalysisApiTypeRelationTestDirectives.LEFT_TYPE_KIND)

val RegisteredDirectives.leftTargetClassName: String
    get() = singleValue(AnalysisApiTypeRelationTestDirectives.LEFT_TARGET_CLASS)

val RegisteredDirectives.leftSecondTargetClassName: String?
    get() = this[AnalysisApiTypeRelationTestDirectives.LEFT_SECOND_TARGET_CLASS].singleOrNull()

val RegisteredDirectives.leftContainerClassName: String?
    get() = this[AnalysisApiTypeRelationTestDirectives.LEFT_CONTAINER_CLASS].singleOrNull()

val RegisteredDirectives.rightTypeKind: String
    get() = singleValue(AnalysisApiTypeRelationTestDirectives.RIGHT_TYPE_KIND)

val RegisteredDirectives.rightTargetClassName: String
    get() = singleValue(AnalysisApiTypeRelationTestDirectives.RIGHT_TARGET_CLASS)

val RegisteredDirectives.rightSecondTargetClassName: String?
    get() = this[AnalysisApiTypeRelationTestDirectives.RIGHT_SECOND_TARGET_CLASS].singleOrNull()

val RegisteredDirectives.rightContainerClassName: String?
    get() = this[AnalysisApiTypeRelationTestDirectives.RIGHT_CONTAINER_CLASS].singleOrNull()

val RegisteredDirectives.expectedIsSubtype: Boolean
    get() = singleValue(AnalysisApiTypeRelationTestDirectives.EXPECTED_IS_SUBTYPE).toBooleanStrict()

val RegisteredDirectives.expectedSemanticallyEqual: Boolean
    get() = singleValue(AnalysisApiTypeRelationTestDirectives.EXPECTED_SEMANTICALLY_EQUAL).toBooleanStrict()
