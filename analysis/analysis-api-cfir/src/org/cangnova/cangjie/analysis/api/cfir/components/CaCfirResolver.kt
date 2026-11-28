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

package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.cfir.types.typeContext

import org.cangnova.cangjie.cfir.resolve.substitution.createTypeSubstitutorByTypeConstructor

import org.cangnova.cangjie.cfir.types.asCone

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.references.CaCfirReference
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CJ_DIAGNOSTIC_CONVERTER
import org.cangnova.cangjie.analysis.api.cfir.components.resolver.resolveSymbol
import org.cangnova.cangjie.analysis.api.components.asSignature
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnostic
import org.cangnova.cangjie.analysis.api.diagnostics.CaSeverity
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseResolver
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseEnumConstructorCall
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseFunctionValueCall
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBasePartiallyAppliedSymbol
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseSimpleFunctionCall
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseCallResolutionError
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseCallResolutionSuccess
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.*
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionAttempt
import org.cangnova.cangjie.analysis.api.resolution.CaCallResolutionSuccess
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionSuccess
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.*
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.analysis.diagnostics.toCfirDiagnostics
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithCandidates
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirErrorFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.resolve.substitution.asCone
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.resolvedType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * CFIR resolver 组件。
 *
 * 该组件只负责把公开 Analysis API 的解析请求映射到 session 内部协议，
 * 不再直接接触 low-level facade。
 */
internal class CaCfirResolver(
    /**
     * 延迟取得当前 CFIR Analysis session，解析请求通过该 session 访问引用、符号和类型构建器。
     */
    analysisSessionProvider: () -> CaCfirSession,
) : CaBaseResolver<CaCfirSession>(analysisSessionProvider), CaCfirSessionComponent {
    /**
     * 将仓颉引用表达式解析为公开符号集合。
     */
    override fun CjReferenceExpression.resolveToSymbols(): Collection<CaSymbol> = withValidityAssertion {
        if (this is org.cangnova.cangjie.psi.CjCallExpression) {
            resolveCallExpressionToSymbol(this)?.let { return@withValidityAssertion listOf(it) }
        }

        return@withValidityAssertion doResolveToSymbols(this)
    }

    /**
     * 对齐 Kotlin `KtCallExpression.resolveSymbol()` 的语义来源：
     * 先做调用解析，再从成功调用里抽出 callable symbol。
    */
    private fun resolveCallExpressionToSymbol(callExpression: org.cangnova.cangjie.psi.CjCallExpression): CaSymbol? {
        val successfulCall = with(this) { callExpression.resolveToCall() }?.successfulCallOrNull<CaCall>() ?: return null
        return (successfulCall as? CaCallableMemberCall<*, *>)?.partiallyAppliedSymbol?.signature?.symbol
    }

    /**
     * 通过 main reference 的 CFIR 引用实现执行符号解析。
     */
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

    /**
     * 将调用 PSI 解析为 success/error attempt。
     *
     * 只缓存成功结果：冷启动/索引未就绪时的失败尝试一旦写入缓存，会话内不会自动失效，
     * 该元素会一直解析失败，直到用户修改它（新 PSI 元素）或发生块内修改。
     */
    override fun CjCallExpression.tryResolveCall(): CaCallResolutionAttempt? = withValidityAssertion {
        val cache = analysisSession.cacheStorage.resolveCallCache.value
        cache.getIfPresent(this@tryResolveCall)?.let { return it }
        val attempt = computeCallResolutionAttempt(this@tryResolveCall) ?: return null
        if (attempt !is CaCallResolutionSuccess) return attempt
        cache.put(this@tryResolveCall, attempt)
        attempt
    }

    /** 通过 CFIR 中的 operator call 构造与普通调用一致的成功/失败结果。 */
    override fun CjBinaryExpression.tryResolveCall(): CaCallResolutionAttempt? = withValidityAssertion {
        val cfirElement = getOrBuildCfir(analysisSession.resolutionFacade)
        val cfirCall = cfirElement as? CfirFunctionCall ?: return null
        computeCallResolutionAttempt(cfirCall)
    }

    /**
     * 把任意 PSI 元素解析为元素级符号解析尝试。
     *
     * 对齐 Kotlin `KaFirResolver.performSymbolResolution`：元素先经 CFIR 取出对应树节点，
     * 再按节点种类投影为成功/失败尝试；失败时保留诊断与候选符号。
     *
     * 与 [tryResolveCall] 一样，失败尝试**不进入缓存**，避免冷启动阶段的失败被永久固化。
     */
    override fun performSymbolResolution(element: CjElement): CaSymbolResolutionAttempt? = withValidityAssertion {
        val cache = analysisSession.cacheStorage.resolveSymbolCache.value
        cache.getIfPresent(element)?.let { return it }
        val attempt = analysisSession.resolveSymbol(element) ?: return null
        if (attempt !is CaSymbolResolutionSuccess) return attempt
        cache.put(element, attempt)
        attempt
    }

    /**
     * 从 CFIR 调用节点构造成功或错误的公开解析尝试。
     */
    private fun computeCallResolutionAttempt(callExpression: CjCallExpression): CaCallResolutionAttempt? {
        val resolutionTarget = callExpression.getQualifiedExpressionForSelectorOrThis()
        val cfirCall = resolutionTarget.getOrBuildCfir(analysisSession.resolutionFacade) as? CfirFunctionCall
            ?: return null
        return computeCallResolutionAttempt(cfirCall)
    }

    /** 将已完成 body resolve 的 CFIR 调用投影为成功/失败的 Analysis API attempt。 */
    private fun computeCallResolutionAttempt(cfirCall: CfirFunctionCall): CaCallResolutionAttempt? {
        val diagnostic = cfirCall.calleeReference.callResolutionErrorDiagnosticOrNull()
            ?.takeUnless { it is ConeUnreportedDuplicateDiagnostic }
        if (diagnostic != null) {
            val callDiagnostic = buildCallResolutionDiagnostic(cfirCall, diagnostic)
            val candidates = when (diagnostic) {
                is ConeDiagnosticWithCandidates -> diagnostic.candidates.filterIsInstance<Candidate>()
                else -> (cfirCall.calleeReference as? CfirNamedReferenceWithCandidate)
                    ?.candidate
                    ?.takeIf { candidate -> candidate.symbol !is CfirErrorFunctionSymbol }
                    ?.let(::listOf)
                    .orEmpty()
            }.mapNotNull(::buildCandidateCall)
            return CaBaseCallResolutionError(candidates, callDiagnostic)
        }

        val successfulCall = buildSuccessfulCall(cfirCall) ?: return null
        return CaBaseCallResolutionSuccess(successfulCall)
    }

    /**
     * 将 CFIR call diagnostic 映射为 Analysis API typed diagnostic。
     */
    private fun buildCallResolutionDiagnostic(
        functionCall: CfirFunctionCall,
        diagnostic: ConeDiagnostic,
    ): CaDiagnostic {
        val cfirDiagnostic = diagnostic.toCfirDiagnostics(
            session = analysisSession.cfirSession,
            source = functionCall.calleeReference.source ?: functionCall.source,
            callOrAssignmentSource = functionCall.source,
        ).firstOrNull()
        return cfirDiagnostic?.let { CJ_DIAGNOSTIC_CONVERTER.convert(analysisSession, it) }
            ?: CaCfirCallResolutionDiagnostic(diagnostic, token)
    }

    /**
     * 对齐 Kotlin resolver 的“成功调用 -> public call model”主链。
     *
     * 当前仓颉 public API 的 call 面仍比 Kotlin 收窄：
     * 只公开函数调用、dispatch receiver、type argument mapping 与 value argument mapping。
     * 因此这里先把已经稳定存在于 public API 中的语义完整落地。
     */
    private fun buildSuccessfulCall(functionCall: CfirFunctionCall): CaCall? {
        functionCall.calleeReference.resolvedEnumConstructorSymbol()?.let { enumConstructorSymbol ->
            return buildSuccessfulEnumConstructorCall(functionCall, enumConstructorSymbol)
        }

        functionCall.calleeReference.resolvedFunctionValueSymbol()?.let { variableSymbol ->
            return buildFunctionValueCall(variableSymbol)
        }

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

        return CaBaseSimpleFunctionCall(
            backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
            backingValueArgumentMapping = valueArgumentMapping,
            backingTypeArgumentsMapping = typeArguments.publicMapping,
        )
    }

    /** 将成功解析到的函数类型变量调用投影为公开函数值调用。 */
    private fun buildFunctionValueCall(symbol: CfirCallableSymbol<*>): CaCall? {
        val variableSymbol = analysisSession.cfirSymbolBuilder.variableBuilder.buildVariableSymbol(symbol)
        val functionType = variableSymbol.returnType as? CaFunctionType ?: return null
        return CaBaseFunctionValueCall(variableSymbol, functionType)
    }

    /** 构造成功解析的枚举构造器调用及其 payload 映射。 */
    private fun buildSuccessfulEnumConstructorCall(
        functionCall: CfirFunctionCall,
        resolvedSymbol: CfirEnumConstructorSymbol,
    ): CaCall {
        val publicSymbol = analysisSession.cfirSymbolBuilder.buildSymbol(resolvedSymbol) as CaEnumConstructorSymbol
        val typeArguments = buildEnumConstructorTypeArguments(resolvedSymbol, functionCall.resolvedType)
        val signature = with(analysisSession) { publicSymbol.asSignature() }.let { baseSignature ->
            typeArguments.publicSubstitutor?.let(baseSignature::substitute) ?: baseSignature
        }
        val partiallyAppliedSymbol = CaBasePartiallyAppliedSymbol(
            backingSignature = signature,
            dispatchReceiver = functionCall.dispatchReceiver?.toPublicReceiverValue(),
        )
        val argumentList = functionCall.argumentList as? CfirResolvedArgumentList
        val payloadArgumentMapping = argumentList?.let { resolvedArguments ->
            buildEnumPayloadArgumentMapping(
                resolvedArguments.mapping.map { (argument, parameter) -> argument to parameter },
                resolvedSymbol.cfir.valueParameters,
                signature.payloadTypes,
            )
        }.orEmpty()
        return CaBaseEnumConstructorCall(
            backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
            payloadArgumentMapping = payloadArgumentMapping,
            typeArgumentsMapping = typeArguments.publicMapping,
        )
    }

    /**
     * 从不同 CFIR 引用实现中提取已解析的函数符号。
     */
    private fun CfirReference.resolvedFunctionSymbol(): CfirFunctionSymbol<*>? {
        return when (this) {
            is CfirResolvedAppliedCallableReference -> resolvedSymbol as? CfirFunctionSymbol<*>
            is CfirResolvedNamedReference -> resolvedSymbol as? CfirFunctionSymbol<*>
            is CfirNamedReferenceWithCandidate -> candidateSymbol as? CfirFunctionSymbol<*>
            else -> null
        }
    }

    /** 从已解析引用中恢复函数值调用的变量符号。 */
    private fun CfirReference.resolvedFunctionValueSymbol(): CfirCallableSymbol<*>? {
        val callableSymbol = when (this) {
            is CfirResolvedAppliedCallableReference -> {
                if (!isFunctionValue) return null
                resolvedSymbol as? CfirCallableSymbol<*>
            }

            is CfirResolvedNamedReference -> resolvedSymbol as? CfirCallableSymbol<*>
            else -> null
        } ?: return null

        return callableSymbol.takeIf { it.cfir is CfirVariable }
    }

    /** 从解析后的 CFIR 引用中提取枚举构造器符号。 */
    private fun CfirReference.resolvedEnumConstructorSymbol(): CfirEnumConstructorSymbol? = when (this) {
        is CfirResolvedAppliedCallableReference -> resolvedSymbol as? CfirEnumConstructorSymbol
        is CfirResolvedNamedReference -> resolvedSymbol as? CfirEnumConstructorSymbol
        is CfirNamedReferenceWithCandidate -> candidateSymbol as? CfirEnumConstructorSymbol
        else -> null
    }

    /** 成功候选引用可能携带非终结诊断；只有明确的错误引用才表示 call-resolution failure。 */
    private fun CfirReference.callResolutionErrorDiagnosticOrNull(): ConeDiagnostic? = when (this) {
        is CfirResolvedErrorReference -> diagnostic
        is CfirErrorNamedReference -> diagnostic
        is CfirNamedReferenceWithCandidate ->
            if (isError) (this as? CfirDiagnosticHolder)?.diagnostic else null
        else -> null
    }

    /**
     * 调用类型实参在公开 API 中的映射和可选 substitutor。
     */
    private data class CallTypeArguments(
        /**
         * 公开类型参数符号到实际类型实参的映射。
         */
        val publicMapping: Map<CaTypeParameterSymbol, CaType>,
        /**
         * 可直接作用于公开签名的类型替换器。
         */
        val publicSubstitutor: org.cangnova.cangjie.analysis.api.types.CaSubstitutor?,
    )

    /**
     * 从 CFIR 调用类型实参构建公开类型实参映射和签名替换器。
     */
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

        val cfirMappings =
            buildMap<TypeConstructorMarker, org.cangnova.cangjie.cfir.types.ConeCangJieType>(typeArguments.size) {
            typeParameters.zip(typeArguments).forEach { (typeParameter, typeArgument) ->
                put(typeParameter.symbol.toLookupTag(), typeArgument.coneType)
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
                createTypeSubstitutorByTypeConstructor(cfirMappings, analysisSession.cfirSession.typeContext, false)
            ),
        )
    }

    /**
     * 从 CFIR 重载候选的显式类型实参与约束解中构建公开类型实参映射。
     */
    private fun buildCandidateTypeArguments(
        candidate: Candidate,
        resolvedSymbol: CfirFunctionSymbol<*>,
        publicFunctionSymbol: CaFunctionSymbol,
    ): CallTypeArguments {
        val typeParameters = resolvedSymbol.cfir.typeParameters
        if (typeParameters.isEmpty() || typeParameters.size != publicFunctionSymbol.typeParameters.size) {
            return CallTypeArguments(emptyMap(), null)
        }

        val cfirMappings = candidateTypeArgumentMappings(candidate, typeParameters)
        if (cfirMappings.isEmpty()) return CallTypeArguments(emptyMap(), null)

        val publicMapping = buildMap(cfirMappings.size) {
            typeParameters.forEachIndexed { index, typeParameter ->
                val type = cfirMappings[typeParameter.symbol.toLookupTag()] ?: return@forEachIndexed
                put(
                    publicFunctionSymbol.typeParameters[index],
                    analysisSession.cfirSymbolBuilder.typeBuilder.buildType(type),
                )
            }
        }
        val publicSubstitutor = analysisSession.cfirSymbolBuilder.typeBuilder.buildSubstitutor(
            createTypeSubstitutorByTypeConstructor(cfirMappings, analysisSession.cfirSession.typeContext, false),
        )
        return CallTypeArguments(publicMapping, publicSubstitutor)
    }

    /** 从通用候选约束状态恢复声明类型参数的最终 CFIR 类型实参。 */
    private fun candidateTypeArgumentMappings(
        candidate: Candidate,
        // 只读取 symbol：构造器/enum 构造项的 constructed-class 引用与自有声明同序参与实参映射。
        typeParameters: List<CfirTypeParameterRef>,
    ): Map<TypeConstructorMarker, ConeCangJieType> {
        if (typeParameters.isEmpty()) return emptyMap()
        val systemSubstitutor = candidate.system.buildCurrentSubstitutor().asCone()
        return buildMap(typeParameters.size) {
            typeParameters.forEachIndexed { index, typeParameter ->
                val type = candidate.typeArgumentMapping.sourceTypeRef(index)?.coneType
                    ?: run {
                        val typeParameterType = ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
                        val candidateType = candidate.substitutor.substituteOrNull(typeParameterType)
                            ?: return@forEachIndexed
                        // 显式 owner 的替换可以已经确定，不需要约束系统再固定一次。
                        systemSubstitutor.substituteOrNull(candidateType)
                            ?: candidateType.takeUnless { type -> type.contains { it is ConeTypeVariableType || it is ConeStubType } }
                            ?: return@forEachIndexed
                    }
                put(typeParameter.symbol.toLookupTag(), type)
            }
        }
    }

    /** 枚举构造器不公开命名类型参数，但其调用签名仍应用候选推断得到的替换。 */
    private fun buildCandidateEnumConstructorTypeArguments(
        candidate: Candidate,
        resolvedSymbol: CfirEnumConstructorSymbol,
    ): CallTypeArguments {
        val cfirMappings = candidateTypeArgumentMappings(candidate, resolvedSymbol.cfir.typeParameters)
        val publicSubstitutor = cfirMappings.takeIf { it.isNotEmpty() }?.let { mappings ->
            analysisSession.cfirSymbolBuilder.typeBuilder.buildSubstitutor(createTypeSubstitutorByTypeConstructor(mappings, analysisSession.cfirSession.typeContext, false))
        }
        return CallTypeArguments(emptyMap(), publicSubstitutor)
    }

    /** 完成后的枚举实例承载有效的 owner 替换，成员上的非法源码实参不参与替换。 */
    private fun buildEnumConstructorTypeArguments(
        resolvedSymbol: CfirEnumConstructorSymbol,
        resultType: ConeCangJieType,
    ): CallTypeArguments {
        val typeParameters = resolvedSymbol.cfir.typeParameters
        val enumType = resultType.fullyExpandedType(analysisSession.cfirSession) as? ConeClassLikeType
            ?: return CallTypeArguments(emptyMap(), null)
        if (enumType.lookupTag.classId != resolvedSymbol.callableId.classId) {
            return CallTypeArguments(emptyMap(), null)
        }
        val typeArguments = enumType.typeArguments.map { it.type ?: return CallTypeArguments(emptyMap(), null) }
        if (typeParameters.isEmpty() || typeParameters.size != typeArguments.size) {
            return CallTypeArguments(emptyMap(), null)
        }
        val cfirMappings = buildMap<TypeConstructorMarker, ConeCangJieType>(typeArguments.size) {
            typeParameters.zip(typeArguments).forEach { (typeParameter, typeArgument) ->
                put(typeParameter.symbol.toLookupTag(), typeArgument)
            }
        }
        return CallTypeArguments(
            emptyMap(),
            analysisSession.cfirSymbolBuilder.typeBuilder.buildSubstitutor(createTypeSubstitutorByTypeConstructor(cfirMappings, analysisSession.cfirSession.typeContext, false)),
        )
    }

    /**
     * 将一个真实 CFIR overload candidate 投影为 Analysis API 函数调用。
     */
    private fun buildCandidateFunctionCall(candidate: Candidate): CaCall? {
        val resolvedSymbol = candidate.symbol as? CfirFunctionSymbol<*> ?: return null
        if (resolvedSymbol is CfirErrorFunctionSymbol) return null

        val publicFunctionSymbol = analysisSession.cfirSymbolBuilder.functionBuilder.buildFunctionSymbol(resolvedSymbol)
        val typeArguments = buildCandidateTypeArguments(candidate, resolvedSymbol, publicFunctionSymbol)
        val signature = analysisSession.cfirSymbolBuilder.functionBuilder.buildFunctionSignature(resolvedSymbol).let { baseSignature ->
            typeArguments.publicSubstitutor?.let(baseSignature::substitute) ?: baseSignature
        }
        val partiallyAppliedSymbol = CaBasePartiallyAppliedSymbol(
            backingSignature = signature,
            dispatchReceiver = candidate.dispatchReceiverExpression()?.toPublicReceiverValue(),
        )
        val argumentMapping = if (candidate.argumentMappingInitialized) {
            candidate.argumentMapping.entries.map { (argument, parameter) -> argument.expression to parameter }
        } else {
            emptyList()
        }
        return CaBaseSimpleFunctionCall(
            backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
            backingValueArgumentMapping = buildValueArgumentMapping(
                argumentMapping,
                resolvedSymbol.cfir.valueParameters,
                signature,
            ),
            backingTypeArgumentsMapping = typeArguments.publicMapping,
        )
    }

    /**
     * 将枚举构造器重载候选构造成公开 payload 调用。
     */
    private fun buildCandidateEnumConstructorCall(candidate: Candidate): CaCall? {
        val resolvedSymbol = candidate.symbol as? CfirEnumConstructorSymbol ?: return null
        val publicSymbol = analysisSession.cfirSymbolBuilder.buildSymbol(resolvedSymbol) as? CaEnumConstructorSymbol ?: return null
        val typeArguments = buildCandidateEnumConstructorTypeArguments(candidate, resolvedSymbol)
        val signature = with(analysisSession) { publicSymbol.asSignature() }.let { baseSignature ->
            typeArguments.publicSubstitutor?.let(baseSignature::substitute) ?: baseSignature
        }
        val partiallyAppliedSymbol = CaBasePartiallyAppliedSymbol(
            backingSignature = signature,
            dispatchReceiver = candidate.dispatchReceiverExpression()?.toPublicReceiverValue(),
        )
        val argumentMapping = if (candidate.argumentMappingInitialized) {
            candidate.argumentMapping.entries.map { (argument, parameter) -> argument.expression to parameter }
        } else {
            emptyList()
        }
        return CaBaseEnumConstructorCall(
            backingPartiallyAppliedSymbol = partiallyAppliedSymbol,
            payloadArgumentMapping = buildEnumPayloadArgumentMapping(
                argumentMapping,
                resolvedSymbol.cfir.valueParameters,
                signature.payloadTypes,
            ),
            typeArgumentsMapping = typeArguments.publicMapping,
        )
    }

    /** 依据 enum constructor payload 位置建立 PSI 实参与公开 payload 类型映射。 */
    private fun buildEnumPayloadArgumentMapping(
        argumentMapping: Iterable<Pair<CfirExpression, CfirValueParameter>>,
        valueParameters: List<CfirValueParameter>,
        payloadTypes: List<CaType>,
    ): Map<CjExpression, CaType> {
        val parameterIndexes = valueParameters.withIndex().associate { (index, parameter) -> parameter.symbol to index }
        return buildMap {
            for ((argument, parameter) in argumentMapping) {
                val psiExpression = (argument.realPsi ?: argument.psi) as? CjExpression ?: continue
                val index = parameterIndexes[parameter.symbol] ?: continue
                val payloadType = payloadTypes.getOrNull(index) ?: continue
                put(psiExpression, payloadType)
            }
        }
    }

    /** 构造错误 attempt 中一个可公开表示的真实候选调用。 */
    private fun buildCandidateCall(candidate: Candidate): CaCall? {
        if (candidate.callInfo.isImplicitInvoke && candidate.symbol.cfir is CfirVariable) {
            val callableSymbol = candidate.symbol as? CfirCallableSymbol<*> ?: return null
            return buildFunctionValueCall(callableSymbol)
        }

        return when (candidate.symbol) {
            is CfirEnumConstructorSymbol -> buildCandidateEnumConstructorCall(candidate)
            is CfirFunctionSymbol<*> -> buildCandidateFunctionCall(candidate)
            else -> null
        }
    }

    /**
     * 把解析后的实参到形参关系转换为公开 PSI 表达式到参数签名的映射。
     */
    private fun buildValueArgumentMapping(
        functionCall: CfirFunctionCall,
        valueParameters: List<CfirValueParameter>,
        signature: CaFunctionSignature<CaFunctionSymbol>,
    ): Map<CjExpression, CaVariableSignature<CaValueParameterSymbol>> {
        val argumentList = functionCall.argumentList as? CfirResolvedArgumentList ?: return emptyMap()
        return buildValueArgumentMapping(
            argumentList.mapping.map { (argument, parameter) -> argument to parameter },
            valueParameters,
            signature,
        )
    }

    /**
     * 将底层实参表达式与值形参映射投影为 Analysis API 签名映射。
     */
    private fun buildValueArgumentMapping(
        argumentMapping: Iterable<Pair<CfirExpression, CfirValueParameter>>,
        valueParameters: List<CfirValueParameter>,
        signature: CaFunctionSignature<CaFunctionSymbol>,
    ): Map<CjExpression, CaVariableSignature<CaValueParameterSymbol>> {
        val valueParametersBySymbol = valueParameters.withIndex().associate { (index, parameter) -> parameter.symbol to index }

        return buildMap {
            for ((argument, parameter) in argumentMapping) {
                // 命名实参在 CFIR 里是 `name: value` 节点，其 PSI 是 `CjValueArgument` 而不是表达式；
                // 公开映射按**值表达式**取键（期望类型与补全都按值表达式查），因此先拆到值表达式。
                // 不拆的后果是命名实参被整段静默跳过——实测：只剩命名实参的调用映射为空。
                val valueArgument = (argument as? CfirNamedArgumentExpression)?.expression ?: argument
                val psiExpression = (valueArgument.realPsi ?: valueArgument.psi) as? CjExpression ?: continue
                val parameterIndex = valueParametersBySymbol[parameter.symbol] ?: continue
                val parameterSignature = signature.valueParameters.getOrNull(parameterIndex) ?: continue
                put(psiExpression, parameterSignature)
            }
        }
    }

    /**
     * 将 CFIR receiver 表达式转换为公开 receiver value。
     */
    private fun CfirExpression.toPublicReceiverValue(): CaReceiverValue {
        return CaBaseReceiverValue(analysisSession.cfirSymbolBuilder.typeBuilder.buildType(resolvedType))
    }

    /**
     * 使用当前 session 的符号构建器将底层 CFIR 符号提升为公开符号。
     */
    private fun buildPublicSymbol(symbol: CfirBasedSymbol<*>): CaSymbol {
        return analysisSession.cfirSymbolBuilder.buildSymbol(symbol)
    }
}

/**
 * 不具备源码诊断映射的 CFIR call error 在 Analysis API 中仍保留稳定诊断信息。
 */
private class CaCfirCallResolutionDiagnostic(
    private val coneDiagnostic: ConeDiagnostic,
    override val token: org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken,
) : CaDiagnostic {
    override val diagnosticClass: kotlin.reflect.KClass<*> get() = CaDiagnostic::class
    override val factoryName: String get() = "CALL_RESOLUTION_ERROR"
    override val severity: CaSeverity get() = CaSeverity.ERROR
    override val defaultMessage: String get() = withValidityAssertion { coneDiagnostic.reason }
}
