package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.model.TestModuleStructure

/**
 * 预分析处理器
 *
 * 对应 Kotlin K2 的 PreAnalysisHandler
 */
abstract class PreAnalysisHandler(protected val testServices: TestServices) {
    abstract fun preprocessModuleStructure(moduleStructure: TestModuleStructure)

    open fun prepareSealedClassInheritors(moduleStructure: TestModuleStructure) {}
}
