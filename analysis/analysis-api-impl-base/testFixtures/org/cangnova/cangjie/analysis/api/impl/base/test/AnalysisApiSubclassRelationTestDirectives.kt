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
    /**
     * 指定当前 subclass relation 测试中的父类目标名称。
     *
     * 子类名称由公共 `TARGET_CLASS` 提供，该字段只描述关系右侧，使测试数据能够明确表达
     * “哪个类被检查为哪个父类的子类”。
     */
    val SUPER_CLASS_NAME by stringDirective(
        description = "当前 subclass 测试中作为父类目标的类名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 描述 `isSubClassOf(superClass)` 的期望布尔结果。
     *
     * 该指令覆盖完整继承链语义，包括间接继承，因此与 direct-subclass 断言分开声明。
     */
    val EXPECTED_IS_SUBCLASS by stringDirective(
        description = "isSubClassOf(superClass) 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 描述 `isDirectSubClassOf(superClass)` 的期望布尔结果。
     *
     * 该字段只约束直接父类关系，用于和完整继承链查询的结果形成成对断言。
     */
    val EXPECTED_IS_DIRECT_SUBCLASS by stringDirective(
        description = "isDirectSubClassOf(superClass) 的期望结果，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取 subclass relation 测试中的子类名称。
 *
 * 该名称复用公共组件测试的 `TARGET_CLASS`，保证所有按类声明定位的用例共享同一入口。
 */
val RegisteredDirectives.subClassName: String
    get() = targetClassName

/**
 * 读取 subclass relation 测试中的父类名称。
 *
 * 返回值会被测试基类用于恢复父类 symbol，再传入公开的继承关系查询 API。
 */
val RegisteredDirectives.superClassName: String
    get() = singleValue(AnalysisApiSubclassRelationTestDirectives.SUPER_CLASS_NAME)

/**
 * 读取完整继承链查询的期望结果。
 *
 * 访问器使用严格布尔解析，确保 testData 中非 `true` / `false` 的值立即暴露为测试配置错误。
 */
val RegisteredDirectives.expectedIsSubclass: Boolean
    get() = singleValue(AnalysisApiSubclassRelationTestDirectives.EXPECTED_IS_SUBCLASS).toBooleanStrict()

/**
 * 读取直接继承关系查询的期望结果。
 *
 * 该值用于断言 `isDirectSubClassOf`，与 `expectedIsSubclass` 一起区分直接和间接继承行为。
 */
val RegisteredDirectives.expectedIsDirectSubclass: Boolean
    get() = singleValue(AnalysisApiSubclassRelationTestDirectives.EXPECTED_IS_DIRECT_SUBCLASS).toBooleanStrict()
