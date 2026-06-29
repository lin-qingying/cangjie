package org.cangnova.cangjie.test.services

import com.intellij.codeInsight.actions.onSave.FormatOnSaveOptionsBase
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.model.TestModuleStructure

/**
 * 表示 `ModuleStructureTransformer`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
@TestInfrastructureInternals
abstract class ModuleStructureTransformer {
    /**
     * 提供 `transformModuleStructure` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    abstract fun transformModuleStructure(moduleStructure: TestModuleStructure, defaultsProvider: DefaultsProvider): TestModuleStructure
}

/**
 * 表示 `ExceptionFromModuleStructureTransformer`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class ExceptionFromModuleStructureTransformer(
    /**
     * 保存 `cause`，供测试服务在测试执行期间读取或传递。
     */
    override val cause: Throwable,
    /**
     * 保存 `alreadyParsedModuleStructure`，供测试服务在测试执行期间读取或传递。
     */
    val alreadyParsedModuleStructure: TestModuleStructure
) : Exception(cause)
