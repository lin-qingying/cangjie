@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.util.withPsiEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.getNonLocalContainingOrThisElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.state.*
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.*
import org.cangnova.cangjie.analysis.utils.errors.requireIsInstance
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

/**
 * An entry point for a CFIR Low Level API resolution. Represents a project view from a use-site [CaModule].
 */
class LLResolutionFacade internal constructor(
    val moduleProvider: LLModuleProvider,
    val resolutionStrategyProvider: LLModuleResolutionStrategyProvider,
    val sessionProvider: LLSessionProvider,
    val diagnosticProvider: LLDiagnosticProvider,
) {
    val useSiteModule: CaModule
        get() = moduleProvider.useSiteModule

    val project: Project
        get() = useSiteModule.project

    val useSiteCfirSession: LLCfirSession
        get() = sessionProvider.useSiteSession

    fun getSessionFor(module: CaModule): LLCfirSession {
        return sessionProvider.getSession(module)
    }

    /**
     * @see LLSessionProvider.getDependencySession
     */
    fun getDependencySessionFor(module: CaModule): LLCfirSession? =
        sessionProvider.getDependencySession(module)

    fun getScopeSessionFor(cfirSession: CfirSession): ScopeSession {
        requireIsInstance<LLCfirSession>(cfirSession)
        return LLDefaultScopeSessionProvider.getScopeSession(cfirSession)
    }

    /**
     * Build [CfirElement] node in its final resolved state for a requested element.
     *
     * Note: that it isn't always [BODY_RESOLVE][CfirResolvePhase.BODY_RESOLVE]
     * as not all declarations have types/bodies/etc. to resolve.
     *
     * This operation could be time-consuming because it creates
     * [FileStructureElement][org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructureElement]
     * and may resolve non-local declarations into [BODY_RESOLVE][CfirResolvePhase.BODY_RESOLVE] phase.
     *
     * Please use [getOrBuildCfirFile] to get [CfirFile] in undefined phase.
     *
     * @return associated [CfirElement] in final resolved state if it exists.
     *
     * @see getOrBuildCfirFile
     * @see org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.CfirElementBuilder.getOrBuildCfirFor
     */
    internal fun getOrBuildCfirFor(element: CjElement): CfirElement? {
        val moduleComponents = getModuleComponentsForElement(element)
        return moduleComponents.elementsBuilder.getOrBuildCfirFor(element)
    }

    /**
     * Get or build or get cached [CfirFile] for the requested file in undefined phase
     */
    internal fun getOrBuildCfirFile(cjFile: CjFile): CfirFile {
        val moduleComponents = getModuleComponentsForElement(cjFile)
        return moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(cjFile)
    }

    private fun getModuleComponentsForElement(element: CjElement): LLCfirModuleResolveComponents {
        val module = getModule(element)
        return sessionProvider.getResolvableSession(module).moduleComponents
    }

    /**
     * @see LLDiagnosticProvider.getDiagnostics
     */
    internal fun getDiagnostics(element: CjElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        return diagnosticProvider.getDiagnostics(element, filter)
    }

    /**
     * @see LLDiagnosticProvider.collectDiagnostics
     */
    internal fun collectDiagnosticsForFile(cjFile: CjFile, filter: DiagnosticCheckerFilter): Collection<CjPsiDiagnostic> {
        return diagnosticProvider.collectDiagnostics(cjFile, filter)
    }

    internal fun resolveToCfirSymbol(cjDeclaration: CjDeclaration, phase: CfirResolvePhase): CfirBasedSymbol<*> {
        val containingCjFile = cjDeclaration.containingCjFile
        val module = getModule(containingCjFile)

        return when (getModuleResolutionStrategy(module)) {
            LLModuleResolutionStrategy.LAZY -> findSourceCfirSymbol(cjDeclaration).also { it.cfir.lazyResolveToPhase(phase) }
            LLModuleResolutionStrategy.STATIC -> findCompiledCfirSymbol(cjDeclaration, module)
        }
    }

    private fun getModuleResolutionStrategy(module: CaModule): LLModuleResolutionStrategy {
        return resolutionStrategyProvider.getKind(module)
    }

    private fun findSourceCfirSymbol(cjDeclaration: CjDeclaration): CfirBasedSymbol<*> {
        val targetModule = getModule(cjDeclaration)

        require(getModuleResolutionStrategy(targetModule) == LLModuleResolutionStrategy.LAZY) {
            "Declaration should be resolvable module, instead it had ${targetModule::class}"
        }

        // All elements inside a code fragment are local
        val nonLocalContainer = cjDeclaration.containingCjFile as? CjCodeFragment
            ?: cjDeclaration.getNonLocalContainingOrThisElement()
            ?: errorWithAttachment("Declaration should have non-local container") {
                withPsiEntry("ktDeclaration", cjDeclaration, ::getModule)
                withEntry("module", targetModule) { it.moduleDescription }
            }

        val cfirDeclaration = if ((nonLocalContainer as? CjDeclaration) == cjDeclaration) {
            val session = sessionProvider.getResolvableSession(targetModule)
            nonLocalContainer.findSourceNonLocalCfirDeclaration(
                cfirFileBuilder = session.moduleComponents.cfirFileBuilder,
                provider = session.cfirProvider,
            )
        } else {
            findSourceCfirDeclarationViaResolve(cjDeclaration)
        }

        return cfirDeclaration.symbol
    }

    private fun findSourceCfirDeclarationViaResolve(cjDeclaration: CjExpression): CfirDeclaration {
        val targetExpression = cjDeclaration.unwrapForCfirLookup()
        return when (val cfir = getOrBuildCfirFor(targetExpression)) {
            is CfirDeclaration -> cfir
            is CfirAnonymousFunctionExpression -> cfir.anonymousFunction
            else -> errorWithCfirSpecificEntries(
                "CfirDeclaration was not found for declaration ${cjDeclaration::class}, cfir is ${cfir?.let { it::class }}",
                cfir = cfir,
                psi = cjDeclaration,
            )
        }
    }

    /**
     * 仓颉 lambda 的语法节点是 `CjFunctionLiteral`，但对应 CFIR 锚点是外层 `CjLambdaExpression`。
     *
     * 因此在“声明经由表达式回找 CFIR”这条 low-level 路径上，需要先把 function-literal 提升到
     * 真正承载 `CfirAnonymousFunctionExpression` 的 PSI，再复用和 Kotlin 相同的
     * `AnonymousFunctionExpression -> anonymousFunction` 恢复链。
     */
    private fun CjExpression.unwrapForCfirLookup(): CjExpression {
        return when (this) {
            is CjFunctionLiteral -> (parent as? CjLambdaExpression)?.unwrapForCfirLookup() ?: this
            else -> this
        }
    }

    private fun findCompiledCfirSymbol(cjDeclaration: CjDeclaration, module: CaModule): CfirBasedSymbol<*> {
        requireWithAttachment(
            cjDeclaration.containingCjFile.isCompiled,
            { "`findCfirCompiledSymbol` only works on compiled declarations, but the given declaration is not compiled." },
        ) {
            withPsiEntry("declaration", cjDeclaration, module)
        }

        val session = getSessionFor(module)
        val searcher = CfirDeclarationForCompiledElementSearcher(session)
        val cfirDeclaration = searcher.findNonLocalDeclaration(cjDeclaration)
        return cfirDeclaration.symbol
    }

}

fun LLResolutionFacade.getModule(element: PsiElement): CaModule {
    return CangJieProjectStructureProvider.getModule(project, element, useSiteModule)
}
