package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedPattern
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `ContextCollector` 的低层回归测试。
 *
 * 这组用例直接验证 match pattern 中的 binding variable 会进入分支的局部 tower context，
 * 避免 `CfirPatternVariable` 被误存入 `CfirLocalScope` 后再次触发框架级崩溃。
 */
class ContextCollectorTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/contextCollector",
) {
    /**
     * 使用源码分析配置，确保 ContextCollector 在 source session 中执行。
     */
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    /**
     * 验证物理文件中的 match 分支 pattern binding 会分别进入对应分支作用域。
     */
    @Test
    fun matchPatternBindingBranchScopes(mainFile: CjFile) {
        assertDistinctPatternBindings(mainFile, mainFile.findBranchUsageReferences())
    }

    /**
     * 验证复制文件中的 match 分支 pattern binding 仍能映射到独立分支作用域。
     */
    @Test
    fun matchPatternBindingBranchScopesInCopiedFile(mainFile: CjFile) {
        val copiedFile = mainFile.copy() as CjFile
        val copiedReferences = mainFile.findBranchUsageReferences().map { originalReference ->
            PsiTreeUtil.findSameElementInCopy(originalReference, copiedFile)
                ?: error("Cannot map branch reference `${originalReference.text}` into copied file `${copiedFile.name}`.")
        }

        assertNotSame(mainFile, copiedFile)
        assertFalse(copiedFile.isPhysical)

        assertDistinctPatternBindings(copiedFile, copiedReferences)
    }
}

/**
 * 统一校验 `match` 分支里的 pattern binding 会进入当前引用点可见的 tower context。
 *
 * 该断言同时用于 physical file 与 copied/dangling file，
 * 确保 `ContextCollector` 在两条路径上都不会把分支 binding 丢失或串到同一个符号实例。
 */
private fun assertDistinctPatternBindings(
    file: CjFile,
    branchReferences: List<CjSimpleNameExpression>,
) {
    assertEquals(
        2,
        branchReferences.size,
        "Test fixture must provide exactly two branch-result references to `y`.",
    )

    val resolutionFacade = file.getResolutionFacadeForTest()
    val cfirFile = file.getOrBuildCfirFile(resolutionFacade)

    val firstContext = ContextCollector.process(resolutionFacade, cfirFile, branchReferences[0])
        ?: error("The first match-branch reference must have a collected context.")
    val secondContext = ContextCollector.process(resolutionFacade, cfirFile, branchReferences[1])
        ?: error("The second match-branch reference must have a collected context.")

    val firstBinding = firstContext.singleVisibleLocalVariable("y")
    val secondBinding = secondContext.singleVisibleLocalVariable("y")

    assertTrue(firstBinding is CfirPatternBindingSymbol, firstBinding::class.qualifiedName)
    assertTrue(secondBinding is CfirPatternBindingSymbol, secondBinding::class.qualifiedName)
    assertNotSame(
        firstBinding,
        secondBinding,
        "Different match branches must expose distinct pattern-binding symbols in collected tower context.",
    )
}

/**
 * 返回测试文件中两个分支结果对 `y` 的引用点。
 */
private fun CjFile.findBranchUsageReferences(): List<CjSimpleNameExpression> {
    return PsiTreeUtil.findChildrenOfType(this, CjSimpleNameExpression::class.java)
        .filter { expression -> expression.referencedName == "y" && expression.isUsageReference() }
        .sortedBy { expression -> expression.textOffset }
}

/**
 * 直接从 `ContextCollector` 快照中读取局部 scope。
 *
 * 这里要求结果唯一，确保测试锁定的是当前分支真实可见的 pattern binding，
 * 而不是偶然从其它 scope 链里捞到的同名声明。
 */
private fun ContextCollector.Context.singleVisibleLocalVariable(name: String): CfirVariableSymbol<*> {
    val targetName = Name.identifier(name)
    val visibleBindings = buildList {
        for (scope in towerDataContext.localScopes.asReversed()) {
            scope.processVariablesByName(targetName) { symbol -> add(symbol) }
        }
    }

    assertEquals(
        1,
        visibleBindings.size,
        "Collected tower context must expose exactly one visible local variable named `$name`.",
    )
    return visibleBindings.single()
}

/**
 * 过滤声明侧 simple-name，只保留真正的引用点。
 */
private fun CjSimpleNameExpression.isUsageReference(): Boolean {
    return this !is CjBindingPattern &&
        parent !is CjNamedPattern &&
        parent !is CjVarOrEnumPattern
}
