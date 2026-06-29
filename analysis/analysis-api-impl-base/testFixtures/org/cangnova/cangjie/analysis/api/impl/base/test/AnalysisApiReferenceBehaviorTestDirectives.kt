package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue

/**
 * reference 行为测试族专用指令。
 *
 * `TARGET_NAME` 已经由公共 component 指令骨架统一声明，这里只保留
 * reference 行为族独有的期望字段，避免同名 directive 在测试框架中发生解析冲突。
 */
object AnalysisApiReferenceBehaviorTestDirectives : SimpleDirectivesContainer() {
    /**
     * 指定 reference 行为测试中目标声明的种类。
     *
     * 测试实现根据该值选择函数、类、导入别名等不同定位路径，避免仅凭名字匹配时
     * 把同名声明误判为当前引用应解析到的目标。
     */
    val TARGET_KIND by stringDirective(
        description = "reference 行为测试目标声明的种类。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录引用解析后目标 PSI 期望具有的简单类名。
     *
     * 该字段用于断言引用行为返回的是正确 PSI 层级的元素，而不只是名称相同的任意声明。
     */
    val EXPECTED_TARGET_CLASS by stringDirective(
        description = "reference 行为测试目标 PSI 的简单类名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 import alias 场景下引用应暴露或命中的别名名称。
     *
     * 该指令把别名行为从普通名称解析中分离出来，使 alias 解析、alias 名称保存和目标恢复
     * 可以在同一个测试族中被独立断言。
     */
    val EXPECTED_ALIAS_NAME by stringDirective(
        description = "import alias 测试中目标 alias 的名字。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取 reference 行为测试的目标声明种类。
 *
 * 访问器集中处理 `TARGET_KIND` 的单值约束，供不同 reference 行为测试基类选择对应定位策略。
 */
val RegisteredDirectives.referenceBehaviorTargetKind: String
    get() = singleValue(AnalysisApiReferenceBehaviorTestDirectives.TARGET_KIND)

/**
 * 读取 reference 行为测试的目标名称。
 *
 * 该名称复用公共 `TARGET_NAME`，保证引用测试与其它按名称定位的 Analysis API 测试共享同一字段。
 */
val RegisteredDirectives.referenceBehaviorTargetName: String
    get() = targetNameText

/**
 * 读取引用目标 PSI 简单类名的期望值。
 *
 * 测试断言使用该值确认引用解析结果的元素类型，防止只比较文本名称而遗漏 PSI 结构差异。
 */
val RegisteredDirectives.expectedReferenceTargetClass: String
    get() = singleValue(AnalysisApiReferenceBehaviorTestDirectives.EXPECTED_TARGET_CLASS)

/**
 * 读取 import alias 场景下期望出现的 alias 名称。
 *
 * 返回值用于校验引用行为是否保留 alias 层语义，而不是直接折叠到被导入声明的真实名称。
 */
val RegisteredDirectives.expectedAliasName: String
    get() = singleValue(AnalysisApiReferenceBehaviorTestDirectives.EXPECTED_ALIAS_NAME)
