package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.model.TestModuleStructure

/**
 * 预分析处理器
 *
 * 对应 Kotlin K2 的 PreAnalysisHandler
 */
abstract class PreAnalysisHandler(protected val testServices: TestServices) {
    /**
     * 提供 `preprocessModuleStructure` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    abstract fun preprocessModuleStructure(moduleStructure: TestModuleStructure)

    /**
     * 提供 `prepareSealedClassInheritors` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    open fun prepareSealedClassInheritors(moduleStructure: TestModuleStructure) {}
}
