/*
 * Copyright 2010-2026 Cangnova contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.platform.modification.CaElementModificationType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.services.TestServices

/**
 * 对齐 Kotlin `AbstractInBlockModificationTest` 的 low-level 块内修改测试。
 *
 * 测试流程（对齐 `LLCfirDeclarationModificationService` 的块内失效契约）：
 * 1. 将标记函数解析到 [CfirResolvePhase.BODY_RESOLVE]，此时函数体不是 [CfirLazyBlock]；
 * 2. 在写动作中替换函数体内标记的表达式，并把修改交给
 *    [LLCfirDeclarationModificationService.handleElementModification] 判定局部性；
 * 3. 断言块内修改使函数体回退为 [CfirLazyBlock]、解析阶段回退到 BODY_RESOLVE 之前；
 * 4. 重新解析到 BODY_RESOLVE，断言函数体恢复为已解析状态。
 */
abstract class AbstractInBlockModificationTest : AbstractAnalysisApiBasedTest() {
    /**
     * 使用源码 low-level CFIR 测试配置。
     */
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    /**
     * 块内修改后用于替换标记表达式的文本。
     */
    private val replacementExpressionText: String = "42"

    /**
     * 对标记表达式所在函数执行块内修改失效与恢复验证。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val targetElement = testServices.expressionMarkerProvider.getBottommostElementOfTypeByDirective(
            file = mainFile,
            module = mainModule.testModule,
            defaultType = PsiElement::class,
        ) as? CjElement ?: error("No <expr> / <caret> marker found in ${mainFile.name}")

        val function = findContainingNamedFunction(targetElement)

        val resolutionFacade = mainFile.getResolutionFacadeForTest()

        // 1. 完整解析函数，确保 body 已解析且被标记为可块内失效（hasCfirBody）。
        val symbol = function.resolveToCfirSymbol(resolutionFacade, CfirResolvePhase.BODY_RESOLVE)
        val cfirFunction = symbol.cfir as? CfirNamedFunction
            ?: error("The marked element must be inside a named function, but got `${symbol.cfir::class.simpleName}`")
        val bodyBeforeModification = requireNotNull(cfirFunction.body) {
            "Precondition failed: `${function.name}` must have a body for the in-block modification test."
        }
        check(bodyBeforeModification !is CfirLazyBlock) {
            "Precondition failed: the function body must be resolved (non-lazy) before the in-block modification."
        }
        check(cfirFunction.resolvePhase == CfirResolvePhase.BODY_RESOLVE) {
            "Precondition failed: expected BODY_RESOLVE, but got ${cfirFunction.resolvePhase}."
        }

        // 2. 写命令内替换标记表达式并触发块内修改处理。
        val modificationService = LLCfirDeclarationModificationService.getInstance(mainFile.project)
        WriteCommandAction.runWriteCommandAction(mainFile.project) {
            val replacement = CjPsiFactory(mainFile.project).createExpression(replacementExpressionText)
            val newElement = targetElement.replace(replacement)
            modificationService.handleElementModification(newElement, CaElementModificationType.Unknown)
            modificationService.flushDeferredModifications()
        }

        // 3. 断言块内修改后的失效状态：body 回退为惰性块、阶段回退。
        val bodyAfterModification = cfirFunction.body
        check(bodyAfterModification is CfirLazyBlock) {
            "After an in-block modification the function body must be reset to a lazy block, " +
                "but got `${bodyAfterModification?.let { it::class.simpleName }}`."
        }
        check(cfirFunction.resolvePhase < CfirResolvePhase.BODY_RESOLVE) {
            "After an in-block modification the resolve phase must decrease below BODY_RESOLVE, " +
                "but got ${cfirFunction.resolvePhase}."
        }

        // 4. 重新解析到 BODY_RESOLVE，断言函数体恢复。
        val reResolvedCfir = function.resolveToCfirSymbol(resolutionFacade, CfirResolvePhase.BODY_RESOLVE).cfir
            as? CfirNamedFunction
            ?: error("Re-resolution must return a CfirNamedFunction again.")
        val recoveredBody = requireNotNull(reResolvedCfir.body) {
            "After re-resolution the function body must exist again."
        }
        check(recoveredBody !is CfirLazyBlock) {
            "After re-resolution the function body must be resolved again instead of staying lazy."
        }
        check(reResolvedCfir.resolvePhase == CfirResolvePhase.BODY_RESOLVE) {
            "After re-resolution the resolve phase must return to BODY_RESOLVE, but got ${reResolvedCfir.resolvePhase}."
        }
    }

    /**
     * 从标记元素向上查找包含它的命名函数声明。
     */
    private fun findContainingNamedFunction(element: CjElement): CjNamedFunction {
        var current: PsiElement? = element
        while (current != null) {
            if (current is CjNamedFunction) return current
            current = current.parent
        }
        error("The marked element `${element.text}` must be located inside a named function body.")
    }
}
