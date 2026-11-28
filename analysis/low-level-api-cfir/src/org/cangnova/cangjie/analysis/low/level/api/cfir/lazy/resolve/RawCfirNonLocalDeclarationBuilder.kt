/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.CfirFunctionBodyOwner
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.parsing.CjMemberParsingContext
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi

/**
 * 对位 Kotlin RawFirNonLocalDeclarationBuilder：沿实际 designation 恢复已有 owner，
 * 只重建指定 PSI 子树。仓颉使用真实 symbol/self 帧，不生成不存在的 inner/nested ClassId。
 */
internal class RawCfirNonLocalDeclarationBuilder private constructor(
    session: CfirSession,
    baseScopeProvider: CfirScopeProvider,
    private val originalDeclaration: CfirDeclaration,
    private val declarationToBuild: CjElement,
    private val functionsToRebind: Set<CfirFunctionBodyOwner>,
    annotationMetadata: CfirAnnotationMetadataBuilder,
) : PsiRawCfirBuilder(
    session, baseScopeProvider, BodyBuildingMode.NORMAL, annotationMetadata,
) {
    companion object {
        fun buildWithFunctionSymbolRebind(
            session: CfirSession,
            scopeProvider: CfirScopeProvider,
            designation: CfirDesignation,
            rootNonLocalDeclaration: CjElement,
            annotationMetadata: CfirAnnotationMetadataBuilder,
            macroSurfaces: MutableList<MacroSurface>,
        ): CfirDeclaration {
            val original = designation.target as CfirDeclaration
            val functionsToRebind = when (original) {
                is CfirFunctionBodyOwner -> setOf(original)
                is CfirProperty -> setOfNotNull(original.getter, original.setter)
                else -> emptySet()
            }
            return build(session, scopeProvider, designation, rootNonLocalDeclaration, functionsToRebind, annotationMetadata, macroSurfaces)
        }

        private fun build(
            session: CfirSession,
            scopeProvider: CfirScopeProvider,
            designation: CfirDesignation,
            rootNonLocalDeclaration: CjElement,
            functionsToRebind: Set<CfirFunctionBodyOwner>,
            annotationMetadata: CfirAnnotationMetadataBuilder,
            macroSurfaces: MutableList<MacroSurface>,
        ): CfirDeclaration {
            check(rootNonLocalDeclaration is CjDeclaration || rootNonLocalDeclaration is CjCodeFragment)
            val original = designation.target as CfirDeclaration
            val builder = RawCfirNonLocalDeclarationBuilder(
                session, scopeProvider, original, rootNonLocalDeclaration, functionsToRebind, annotationMetadata,
            )
            val file = rootNonLocalDeclaration.containingCjFile
            builder.context.packageFqName = file.packageFqName
            builder.context.forcedContainerSymbol = original.symbol
            val rebuilt = builder.withSourceModuleContext(rootNonLocalDeclaration) {
                builder.moveNext(designation.path.iterator(), null)
            }
            // 私有重建只交付原始图和surface；Calculator采用真实槽位后才封闭和发布。
            macroSurfaces += builder.consumeCollectedMacroSurfaces()
            return rebuilt
        }
    }

    override fun bindFunctionTarget(target: CfirFunctionTarget, function: CfirFunctionBodyOwner) {
        super.bindFunctionTarget(target, computeRebindTarget(function) ?: function)
    }

    /** 保留原 target session 的函数身份；新局部函数和 lambda 仍绑定自身。 */
    private fun computeRebindTarget(function: CfirFunctionBodyOwner): CfirFunctionBodyOwner? {
        if (function is CfirPropertyAccessor) {
            val accessorPsi = function.realPsi ?: function.psi ?: return null
            return originalAccessor(accessorPsi, function.isGetter)
        }
        val realPsi = function.realPsi
        if (realPsi != null) return functionsToRebind.firstOrNull { it.realPsi === realPsi }
        return null
    }

    /** 词法 owner 与 return target 共用同一 PSI/角色身份匹配，不读取未绑定的新 symbol。 */
    private fun originalAccessor(psi: PsiElement, isGetter: Boolean): CfirPropertyAccessor? {
        val candidates = functionsToRebind.filterIsInstance<CfirPropertyAccessor>().filter {
            it.isGetter == isGetter && (it.realPsi === psi || it.psi === psi)
        }
        check(candidates.size <= 1) { "An accessor PSI and role must identify at most one original function." }
        return candidates.singleOrNull()
    }

    private inner class VisitorWithReplacement : Visitor() {
        override fun accessorContainerSymbol(
            psi: CjPropertyAccessor,
            declarationSymbol: CfirPropertyAccessorSymbol,
        ): CfirPropertyAccessorSymbol = originalAccessor(psi, psi.isGetter)?.symbol
            ?: super.accessorContainerSymbol(psi, declarationSymbol)

        override fun visitCjFile(file: CjFile, data: CfirElement?): CfirElement {
            check(file is CjCodeFragment) { "A designated file target must be an actual native code fragment." }
            return convertCodeFragment(file)
        }

        fun processPrimaryConstructor(owner: CjClassLikeDeclaration): CfirConstructor =
            buildImplicitPrimaryConstructor(owner)
    }

    /** 对位 Kotlin：指定 target 捕获保留 header 的 symbol，副本 header 始终新建声明。 */
    override fun addCapturedTypeParameters(
        status: Boolean,
        declarationSource: CjSourceElement?,
        currentCfirTypeParameters: List<CfirTypeParameterRef>,
    ) {
        val parameters = if (originalDeclaration is CfirTypeParameterRefsOwner &&
            declarationSource?.psi === originalDeclaration.psi
        ) originalDeclaration.typeParameters else currentCfirTypeParameters
        super.addCapturedTypeParameters(status, declarationSource, parameters)
    }

    private fun moveNext(iterator: Iterator<CfirDeclaration>, containingDeclaration: CfirDeclaration?): CfirDeclaration {
        if (!iterator.hasNext()) {
            val visitor = VisitorWithReplacement()
            val rebuilt = when {
                declarationToBuild is CjClassLikeDeclaration && originalDeclaration is CfirConstructor ->
                    visitor.processPrimaryConstructor(declarationToBuild)
                else -> visitor.convertElement(declarationToBuild, originalDeclaration) as CfirDeclaration
            }
            return rebuilt.bindMacroFragmentOwner()
        }
        val parent = iterator.next()
        return when (parent) {
            is CfirFile -> withEnclosingContainerSymbol(parent.symbol) { moveNext(iterator, parent) }
            is CfirClassLikeDeclaration -> {
                val psi = parent.psi as? CjClassLikeDeclaration
                    ?: errorWithCfirSpecificEntries("The designation class must retain its actual PSI owner", cfir = parent, psi = parent.psi)
                val kind = when (psi) {
                    is CjClass -> CjMemberParsingContext.Kind.CLASS
                    is CjStruct -> CjMemberParsingContext.Kind.STRUCT
                    is CjInterface -> CjMemberParsingContext.Kind.INTERFACE
                    is CjEnum -> CjMemberParsingContext.Kind.ENUM
                    else -> errorWithCfirSpecificEntries("Unexpected Cangjie nominal owner", cfir = parent, psi = psi)
                }
                context.withNominalSyntax({ CjMemberParsingContext(kind, psi.nameIdentifier?.text) }) {
                    withEnclosingContainerSymbol(parent.symbol) {
                        // 祖先恢复与整文件构建共用同一成员环境，构造器由此引用保留祖先的真实参数。
                        withClassLikeMembers(
                            parent.symbol.rawDispatchReceiverType(parent.typeParameters), null, parent.typeParameters,
                            isConstructible = psi !is CjInterface,
                        ) {
                            moveNext(iterator, parent)
                        }
                    }
                }
            }
            is CfirExtend -> {
                check(parent.psi is CjExtend) { "The designation extend must retain its actual PSI owner." }
                val self = parent.extendedTypeRef.coneType as ConeSimpleCangJieType
                context.withNominalSyntax({ CjMemberParsingContext(CjMemberParsingContext.Kind.EXTEND, null) }) {
                    withEnclosingContainerSymbol(parent.symbol) {
                        withCapturedTypeParameters(false, currentCfirTypeParameters = parent.typeParameters) {
                            withDispatchReceiverType(self) { moveNext(iterator, parent) }
                        }
                    }
                }
            }
            else -> errorWithCfirSpecificEntries("Unexpected designation owner", cfir = parent, psi = parent.psi)
        }
    }
}
