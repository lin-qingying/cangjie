package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * containing declaration 能力族的专用测试指令。
 *
 * 目标引用名称复用公共 `TARGET_NAME`，这里只描述容器链本身的期望输出，
 * 让 containing-declaration 测试与其它按引用定位目标的测试族共享统一入口。
 */
object AnalysisApiContainingDeclarationTestDirectives : SimpleDirectivesContainer() {
    /**
     * 记录从目标符号向外追踪 containingDeclaration 链时应得到的稳定文本。
     *
     * 测试用例通过该指令把期望容器链写入 testData，抽象测试基类再用公开 Analysis API
     * 恢复符号并逐级渲染 containing declaration，确保不同前端模式输出同一语义结构。
     */
    val EXPECTED_CONTAINING_DECLARATION by stringDirective(
        description = "从当前 symbol 向外追踪 containingDeclaration 链时应得到的稳定文本。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取 containing declaration 测试要定位的目标声明或引用名称。
 *
 * 该访问器复用公共组件测试中的 `TARGET_NAME`，使 containing-declaration 用例不需要额外声明
 * 一套同义指令，也避免测试数据在多个目标名称字段之间产生歧义。
 */
val RegisteredDirectives.containingDeclarationTargetName: String
    get() = targetNameText
