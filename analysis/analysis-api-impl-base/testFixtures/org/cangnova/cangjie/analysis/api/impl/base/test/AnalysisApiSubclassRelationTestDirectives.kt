package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue
import kotlin.text.toBooleanStrict

/**
 * subclass relation 能力族专用指令。
 *
 * 子类名称继续复用公共 `TARGET_CLASS`，这里额外声明：
 * 1. 目标父类名称
 * 2. `isSubClassOf`
 * 3. `isDirectSubClassOf`
 */
object AnalysisApiSubclassRelationTestDirectives : SimpleDirectivesContainer() {
    val SUPER_CLASS_NAME by stringDirective(
        description = "当前 subclass 测试中作为父类目标的类名。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_IS_SUBCLASS by stringDirective(
        description = "isSubClassOf(superClass) 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_IS_DIRECT_SUBCLASS by stringDirective(
        description = "isDirectSubClassOf(superClass) 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )
}

val RegisteredDirectives.subClassName: String
    get() = targetClassName

val RegisteredDirectives.superClassName: String
    get() = singleValue(AnalysisApiSubclassRelationTestDirectives.SUPER_CLASS_NAME)

val RegisteredDirectives.expectedIsSubclass: Boolean
    get() = singleValue(AnalysisApiSubclassRelationTestDirectives.EXPECTED_IS_SUBCLASS).toBooleanStrict()

val RegisteredDirectives.expectedIsDirectSubclass: Boolean
    get() = singleValue(AnalysisApiSubclassRelationTestDirectives.EXPECTED_IS_DIRECT_SUBCLASS).toBooleanStrict()
