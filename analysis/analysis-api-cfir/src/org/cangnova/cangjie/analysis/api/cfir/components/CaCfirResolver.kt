package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.references.CaCfirReference
import org.cangnova.cangjie.analysis.api.cfir.symbols.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.components.CaResolver
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBasePartiallyAppliedSymbol
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseSimpleFunctionCall
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseSuccessCallInfo
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaBaseReceiverValue
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaReceiverValue
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.resolution.symbol
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.resolvedType

/**
 * CFIR resolver 组件。
 *
 * 该组件只负责把公开 Analysis API 的解析请求映射到 session 内部协议，
 * 不再直接接触 low-level facade。
 */
internal class CaCfirResolver(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaResolver, CaCfirSessionComponent {
    override fun CjReferenceExpression.resolveToSymbols(): Collection<CaSymbol> = withValidityAssertion {
        if (this is org.cangnova.cangjie.psi.CjCallExpression) {
            resolveCallExpressionToSymbol(this)?.let { return@withValidityAssertion listOf(it) }
        }

        val directSymbols = doResolveToSymbols(this)
        if (directSymbols.isNotEmpty()) {
            return@withValidityAssertion directSymbols
        }

        val branchBindings = restoreMatchBranchPatternBindings(this)
        if (branchBindings.isNotEmpty()) {
            return@withValidityAssertion branchBindings.distinctSymbols()
        }

        return@withValidityAssertion emptyList()
    }

    /**
     * 对齐 Kotlin `KtCallExpression.resolveSymbol()` 的语义来源：
     * 先做调用解析，再从成功调用里抽出 callable symbol。
     */
    private fun resolveCallExpressionToSymbol(callExpression: org.cangnova.cangjie.psi.CjCallExpression): CaSymbol? {
        val successfulCall = with(this) { callExpression.resolveToCall() }?.successfulFunctionCallOrNull() ?: return null
        return successfulCall.symbol
    }

    private fun doResolveToSymbols(referenceExpression: CjReferenceExpression): Collection<CaSymbol> {
        val reference = referenceExpression.mainReference ?: return emptyList()
        checkWithAttachment(
            reference is CaCfirReference,
            { "${reference::class.simpleName} is not extends ${CaCfirReference::class.simpleName}" },
        ) {
            withPsiEntry("reference", reference.element)
        }

        with(reference) {
            return analysisSession.resolveToSymbols()
        }
    }

    override fun CjElement.resolveToCall(): CaCallInfo? = withValidityAssertion {
        val callExpression = this as? org.cangnova.cangjie.psi.CjCallExpression ?: return@withValidityAssertion null
        val resolutionTarget = callExpression.getQualifiedExpressionForSelectorOrThis()
        val cfirCall = resolutionTarget.getOrBuildCfir(analysisSession.resolutionFacade) as? CfirFunctionCall
            ?: return@withValidityAssertion null

        buildSuccessfulFunctionCallInfo(cfirCall)
    }

    /**
     * 对齐 Kotlin resolver 的“成功调用 -> public call model”主链。
     *
     * 当前仓颉 public API 的 call 面仍比 Kotlin 收窄：
     * 只公开函数调用、dispatch receiver、type argument mapping 与 value argument mapping。
     * 因此这里先把已经稳定存在于 public API 中的语义完整落地。
     */
    private fun buildSuccessfulFunctionCallInfo(functionCall: CfirFunctionCall): CaCallInfo? {
        val resolvedSymbol = functionCall.calleeReference.resolvedFunctionSymbol() ?: return null
        val publicFunctionSymbol = analysisSession.cfirSymbolBuilder.functionBuilder.buildFunctionSymbol(resolvedSymbol)
        val typeArguments = buildCallTypeArguments(resolvedSymbol, publicFunctionSymbol, functionCall.typeArguments)
        val signature = analysisSession.cfirSymbolBuilder.functionBuilder.buildFunctionSignature(resolvedSymbol).let { baseSignature ->
            typeArguments.publicSubstitutor?.let(baseSignature::substitute) ?: baseSignature
        }
        val partiallyAppliedSymbol = CaBasePartiallyAppliedSymbol(
            backingSignature = signature,
            dispatchReceiver = functionCall.dispatchReceiver?.toPublicReceiverValue(),
        )
        val valueArgumentMapping = buildValueArgumentMapping(functionCall, resolvedSymbol.cfir.valueParameters, signature)

        return CaBaseSuccessCallInfo(
            CaBaseSimpleFunctionCall(
                backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
                backingValueArgumentMapping = valueArgumentMapping,
                backingTypeArgumentsMapping = typeArguments.publicMapping,
            )
        )
    }

    private fun CfirReference.resolvedFunctionSymbol(): CfirFunctionSymbol<*>? {
        return when (this) {
            is CfirResolvedAppliedCallableReference -> resolvedSymbol as? CfirFunctionSymbol<*>
            is CfirResolvedNamedReference -> resolvedSymbol as? CfirFunctionSymbol<*>
            is CfirNamedReferenceWithCandidate -> candidateSymbol as? CfirFunctionSymbol<*>
            else -> null
        }
    }

    private data class CallTypeArguments(
        val publicMapping: Map<CaTypeParameterSymbol, CaType>,
        val publicSubstitutor: org.cangnova.cangjie.analysis.api.types.CaSubstitutor?,
    )

    private fun buildCallTypeArguments(
        resolvedSymbol: CfirFunctionSymbol<*>,
        publicFunctionSymbol: CaFunctionSymbol,
        typeArguments: List<CfirTypeRef>,
    ): CallTypeArguments {
        if (typeArguments.isEmpty()) {
            return CallTypeArguments(emptyMap(), null)
        }

        val typeParameters = resolvedSymbol.cfir.typeParameters
        if (typeParameters.size != typeArguments.size || publicFunctionSymbol.typeParameters.size != typeArguments.size) {
            return CallTypeArguments(emptyMap(), null)
        }

        val cfirMappings = buildMap<String, org.cangnova.cangjie.cfir.types.ConeCangJieType>(typeArguments.size) {
            typeParameters.zip(typeArguments).forEach { (typeParameter, typeArgument) ->
                put(typeParameter.symbol.name.asString(), typeArgument.coneType)
            }
        }
        val publicMapping = buildMap(typeArguments.size) {
            publicFunctionSymbol.typeParameters.zip(typeArguments).forEach { (typeParameter, typeArgument) ->
                put(typeParameter, analysisSession.cfirSymbolBuilder.typeBuilder.buildType(typeArgument))
            }
        }

        return CallTypeArguments(
            publicMapping = publicMapping,
            publicSubstitutor = analysisSession.cfirSymbolBuilder.typeBuilder.buildSubstitutor(
                CfirTypeSubstitutorByMap(cfirMappings)
            ),
        )
    }

    private fun buildValueArgumentMapping(
        functionCall: CfirFunctionCall,
        valueParameters: List<CfirValueParameter>,
        signature: CaFunctionSignature<CaFunctionSymbol>,
    ): Map<CjExpression, CaVariableSignature<CaValueParameterSymbol>> {
        val argumentList = functionCall.argumentList as? CfirResolvedArgumentList ?: return emptyMap()
        val valueParametersBySymbol = valueParameters.withIndex().associate { (index, parameter) -> parameter.symbol to index }

        return buildMap(argumentList.mapping.size) {
            for ((argument, parameter) in argumentList.mapping) {
                val psiExpression = (argument.realPsi ?: argument.psi) as? CjExpression ?: continue
                val parameterIndex = valueParametersBySymbol[parameter.symbol] ?: continue
                val parameterSignature = signature.valueParameters.getOrNull(parameterIndex) ?: continue
                put(psiExpression, parameterSignature)
            }
        }
    }

    private fun CfirExpression.toPublicReceiverValue(): CaReceiverValue {
        return CaBaseReceiverValue(analysisSession.cfirSymbolBuilder.typeBuilder.buildType(resolvedType))
    }

    /**
     * `match` 分支中的模式绑定属于源码局部声明。
     *
     * 它们在当前仓库里还没有完全通过 low-level reference 索引稳定暴露，
     * 但其语义边界在 PSI 上是明确的：只能解析到当前分支条件侧声明的具名绑定。
     * 因此这里直接基于 `CjMatchEntry.conditions` 恢复同分支 binding symbol，
     * 保证不同分支的同名绑定不会混淆。
     */
    private fun restoreMatchBranchPatternBindings(reference: CjReferenceExpression): Collection<CaSymbol> {
        val simpleName = reference as? CjSimpleNameExpression ?: return emptyList()
        val matchEntry = simpleName.getStrictParentOfType<CjMatchEntry>() ?: return emptyList()
        val arrow = matchEntry.arrow ?: return emptyList()
        if (simpleName.textOffset <= arrow.textOffset) {
            return emptyList()
        }

        return matchEntry.conditions.asSequence()
            .flatMap { condition ->
                sequence {
                    yieldAll(condition.getAllBindings().asSequence())
                    yieldAll(com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(condition, CjVarOrEnumPattern::class.java).asSequence())
                }
            }
            .filter { declaration -> declaration.name == simpleName.referencedName }
            .mapNotNull { declaration ->
                resolvePatternBindingSymbolByPsi(declaration)
                    ?: (declaration as? CjVarOrEnumPattern)?.reference?.let(::resolvePatternBindingSymbolByPsi)
            }
            .toList()
    }

    private fun Collection<CaSymbol>.distinctSymbols(): List<CaSymbol> {
        return distinctBy { symbol ->
            symbol.publicSymbolCacheKeyOrNull() ?: "${symbol::class.qualifiedName}@${System.identityHashCode(symbol)}"
        }
    }

    private fun resolvePatternBindingSymbolByPsi(psi: com.intellij.psi.PsiElement): CaPatternBindingSymbol? {
        val cfirDeclaration = (psi as? CjElement)
            ?.getOrBuildCfir(analysisSession.resolutionFacade) as? CfirDeclaration
            ?: return null
        return listOf(buildPublicSymbol(cfirDeclaration.symbol))
            .filterIsInstance<CaPatternBindingSymbol>()
            .firstOrNull()
    }

    private fun buildPublicSymbol(symbol: CfirBasedSymbol<*>): CaSymbol {
        return analysisSession.cfirSymbolBuilder.buildSymbol(symbol)
    }
}
