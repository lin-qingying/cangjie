/*
 * Copyright 2010-2026 Cangnova contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkDeprecationProviderIsResolved
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.test.services.TestServices

/**
 * 对齐 Kotlin low-level `checkDeprecationProviderIsResolved` 契约的 deprecation 懒解析测试。
 *
 * 契约（对齐 `LLCfirLazyDeclarationResolver` 的阶段可见性要求）：
 * 1. 声明被懒解析到 [CfirResolvePhase.STATUS] 后，其 `deprecationsProvider`
 *    不得再是 [UnresolvedDeprecationProvider][org.cangnova.cangjie.cfir.declarations.UnresolvedDeprecationProvider]
 *    占位——解析管线必须已经依据注解解析结果替换为真实（或明确为空）的 provider；
 * 2. 源码中标注 `@Deprecated` 的声明，其 provider 还必须真正携带弃用信息，
 *    即不得回退为 [EmptyDeprecationsProvider]。
 */
abstract class AbstractDeprecationsResolveTest : AbstractAnalysisApiBasedTest() {
    /**
     * 使用源码 low-level CFIR 测试配置。
     */
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    /**
     * 将主文件内所有顶层命名函数解析到 STATUS，并校验各自的 deprecation provider 契约。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val resolutionFacade = mainFile.getResolutionFacadeForTest()
        val functions = mainFile.declarations.filterIsInstance<CjNamedFunction>()
        check(functions.isNotEmpty()) {
            "The deprecationsResolve testdata must declare at least one named function in ${mainFile.name}"
        }

        for (function in functions) {
            val cfir = function.resolveToCfirSymbol(resolutionFacade, CfirResolvePhase.STATUS).cfir as CfirNamedFunction

            check(cfir.resolvePhase >= CfirResolvePhase.STATUS) {
                "Precondition failed: `${function.name}` must be resolved to STATUS, " +
                    "but the resolve phase is ${cfir.resolvePhase}."
            }

            // 契约 1：STATUS 之后 provider 不得是未解析占位。
            checkDeprecationProviderIsResolved(cfir, cfir.deprecationsProvider)

            // 契约 2：@Deprecated 声明的 provider 必须携带真实弃用信息。
            if (function.isMarkedDeprecated()) {
                check(cfir.deprecationsProvider !is EmptyDeprecationsProvider) {
                    "`${function.name}` is annotated with @Deprecated, so its deprecations provider " +
                        "must carry deprecation info instead of being empty."
                }
            }
        }
    }

    /**
     * 判断 PSI 声明上是否标注了内置 `@Deprecated` 注解。
     */
    private fun CjNamedFunction.isMarkedDeprecated(): Boolean =
        annotationEntries.any { it.shortName?.asString() == "Deprecated" }
}
