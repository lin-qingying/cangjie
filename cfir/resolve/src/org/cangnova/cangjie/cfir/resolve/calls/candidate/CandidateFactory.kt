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

package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.calls.ReceiverValue
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.utils.addDefaultBoundIfNecessary
import org.cangnova.cangjie.cfir.diagnostic.HiddenCandidate
import org.cangnova.cangjie.cfir.diagnostic.InferenceConstraintError
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithSingleChild
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.isInstanceExtendMemberCandidate
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.session.inferenceLogger
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * tower 遍历阶段发现 callable 后，通过本工厂统一构造候选。
 *
 * 这里保留 candidate 的 receiver 与 constraint-system 初始化规则，让 tower 层
 * 只负责作用域遍历，不把候选构造细节扩散到各个调用入口。
 */
internal enum class BuiltinArrayConstructorKind {
    EMPTY,
    COLLECTION,
    INIT_FUNCTION,
    REPEAT_ELEMENT,
}

internal data class BuiltinArrayConstructorTypeParameter(
    val name: Name,
    val originalSymbol: CfirTypeParameterSymbol? = null,
)

internal sealed class BuiltinArrayConstructorTarget {
    abstract val typeParameters: List<BuiltinArrayConstructorTypeParameter>
    abstract fun returnType(elementType: ConeCangJieType): ConeCangJieType

    data object Array : BuiltinArrayConstructorTarget() {
        override val typeParameters: List<BuiltinArrayConstructorTypeParameter> =
            listOf(BuiltinArrayConstructorTypeParameter(Name.identifier("T")))

        override fun returnType(elementType: ConeCangJieType): ConeCangJieType =
            ConeClassLikeType(StdlibClassIds.Array.toLookupTag(), typeArguments = listOf(elementType))
    }

    data class VArray(
        val sizeLiteral: String,
        val elementType: ConeCangJieType? = null,
        override val typeParameters: List<BuiltinArrayConstructorTypeParameter> =
            listOf(BuiltinArrayConstructorTypeParameter(Name.identifier("T"))),
    ) : BuiltinArrayConstructorTarget() {
        override fun returnType(elementType: ConeCangJieType): ConeCangJieType {
            val size = CfirIntConstantEvalUtils.parseVArraySizeLiteral(sizeLiteral)
                ?: return ConeErrorType(ConeSimpleDiagnostic("Invalid VArray size: $sizeLiteral"))
            return ConeVArrayType(elementType, size)
        }
    }
}

class CandidateFactory(
    private val context: ResolutionContext,
    private val baseSystem: ConstraintStorage  ,
) {
    constructor(context: ResolutionContext, callInfo: CallInfo) : this(context, buildBaseSystem(context, callInfo))
    companion object {
        private fun buildBaseSystemForContainingCallAwareCases(
            context: ResolutionContext,
            containingCall: Candidate,
            // For callable references, there is no call
            callInfo: CallInfo?,
        ): ConstraintStorage {
            val system = context.inferenceComponents.createConstraintSystem()
            system.setBaseSystem(containingCall.system.currentStorage())
            callInfo?.argumentAtoms?.forEach {
                system.addSubsystemFromAtom(it)
            }
            return system.asReadOnlyStorage()
        }
        private fun buildBaseSystem(context: ResolutionContext, callInfo: CallInfo): ConstraintStorage {
            callInfo.containingCandidateForCollectionLiteral?.let {
                return buildBaseSystemForContainingCallAwareCases(context, it, callInfo)
            }
            val system = context.inferenceComponents.createConstraintSystem()
            callInfo.argumentAtoms.forEach {
                system.addSubsystemFromAtom(it)
            }
            context.session.inferenceLogger?.logStage("CandidateFactory.buildBaseSystem()", system)
            return system.asReadOnlyStorage()
        }

        fun createForCallableReferenceCandidate(
            context: ResolutionContext,
            containingCall: Candidate,
        ): CandidateFactory =
            CandidateFactory(context, buildBaseSystemForContainingCallAwareCases(context, containingCall, callInfo = null))
    }

    fun createCallableReferenceCandidate(
        callInfo: CallInfo,
        originalCandidate: Candidate,
    ): Candidate {
        return Candidate(
            symbol = originalCandidate.symbol,
            dispatchReceiver = originalCandidate.dispatchReceiver,
            givenExtensionReceiver = originalCandidate.givenExtensionReceiver,
            explicitReceiverKind = originalCandidate.explicitReceiverKind,
            constraintSystemFactory = context.inferenceComponents.constraintSystemFactory,
            baseSystem = baseSystem,
            callInfo = callInfo,
            originScope = originalCandidate.originScope,
            isFromCompanionObjectTypeScope = originalCandidate.isFromCompanionObjectTypeScope,
            isFromOriginalTypeInPresenceOfSmartCast = originalCandidate.isFromOriginalTypeInPresenceOfSmartCast,
            bodyResolveContext = context.bodyResolveContext,
        )
    }

    fun createCandidate(
        callInfo: CallInfo,
        symbol: CfirCallableSymbol<*>,
        originScope: CfirScope?,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
        dispatchReceiver: ReceiverValue? = null,
        givenExtensionReceiver: ReceiverValue? = null,
        baseSystem: ConstraintStorage = this.baseSystem,
    ): Candidate {
        val useDispatchReceiverAsExtensionReceiver =
            givenExtensionReceiver == null &&
                dispatchReceiver != null &&
                symbol.isInstanceExtendMemberCandidate(context.session)
        val effectiveDispatchReceiver = if (useDispatchReceiverAsExtensionReceiver) null else dispatchReceiver
        val effectiveExtensionReceiver = givenExtensionReceiver ?: dispatchReceiver.takeIf {
            useDispatchReceiverAsExtensionReceiver
        }

        return Candidate(
            symbol = symbol,
            dispatchReceiver = effectiveDispatchReceiver?.receiverExpression?.let(ConeResolutionAtom::createRawAtom),
            givenExtensionReceiver = effectiveExtensionReceiver?.receiverExpression?.let(ConeResolutionAtom::createRawAtom),
            explicitReceiverKind = explicitReceiverKind,
            constraintSystemFactory = context.inferenceComponents.constraintSystemFactory,
            baseSystem = baseSystem,
            callInfo = callInfo,
            originScope = originScope,
            bodyResolveContext = context.bodyResolveContext,
        )
    }

    fun createFunctionTypeInvokeCandidate(
        callInfo: CallInfo,
        functionType: ConeFunctionType,
        receiverExpression: CfirExpression,
        explicitReceiverKind: ExplicitReceiverKind,
        dispatchReceiver: ReceiverValue,
    ): Candidate {
        val symbol = CfirNamedFunctionSymbol(CallableId(callInfo.name))
        val valueParameters = functionType.parameterTypes.mapIndexed { index, parameterType ->
            val parameterName = Name.identifier("functionTypeInvokeArg$index")
            buildValueParameter {
                source = receiverExpression.source
                moduleData = context.session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.FakeFunction
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                dispatchReceiverType = null
                this.symbol = CfirValueParameterSymbol(CallableId(parameterName))
                containingDeclarationSymbol = symbol
                isNamed = false
                status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
                returnTypeRef = buildResolvedTypeRef {
                    source = receiverExpression.source
                    coneType = parameterType
                }
                name = parameterName
                defaultValue = null
            }
        }

        buildNamedFunction {
            source = callInfo.callSite.source
            moduleData = context.session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.FakeFunction
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildResolvedTypeRef {
                source = callInfo.callSite.source
                coneType = functionType.returnType
            }
            this.valueParameters.addAll(valueParameters)
            body = null
            this.symbol = symbol
            name = callInfo.name
            isMut = false
        }

        return createCandidate(
            callInfo = callInfo,
            symbol = symbol,
            originScope = null,
            explicitReceiverKind = explicitReceiverKind,
            dispatchReceiver = dispatchReceiver,
            baseSystem = baseSystem.withSubsystemFromInvokeReceiver(receiverExpression),
        )
    }

    private fun ConstraintStorage.withSubsystemFromInvokeReceiver(receiverExpression: CfirExpression): ConstraintStorage {
        val receiverAtom = ConeResolutionAtom.createRawAtom(receiverExpression)
        val system = context.inferenceComponents.createConstraintSystem()
        system.setBaseSystem(this)
        system.addSubsystemFromAtom(receiverAtom)
        return system.asReadOnlyStorage()
    }

    /**
     * 构造 `std.core.Array` 内建构造表达式候选。
     *
     * 官方编译器把 `Array<T>(...)` 调用解糖为 `ArrayExpr`，再按
     * 空数组、`(size, (Int64)->T)`、`(size, repeat: T)` 三种形状做检查。
     * CFIR 没有独立 ArrayExpr 节点参与解析，因此用 synthetic function 保留
     * 统一 call-resolution 管线中的类型实参映射、参数映射和 lambda 期望类型。
     */
    internal fun createBuiltinArrayConstructorCandidate(
        callInfo: CallInfo,
        kind: BuiltinArrayConstructorKind,
        target: BuiltinArrayConstructorTarget = BuiltinArrayConstructorTarget.Array,
    ): Candidate {
        val symbol = CfirNamedFunctionSymbol(CallableId(callInfo.name))
        val typeParameters = target.typeParameters.map { parameter ->
            buildTypeParameter {
                source = callInfo.callSite.source
                moduleData = context.session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor
                attributes = CfirDeclarationAttributes.EMPTY
                containingDeclarationSymbol = symbol
                this.symbol = CfirTypeParameterSymbol()
                name = parameter.name
                addDefaultBoundIfNecessary()
            }
        }
        val elementType = target.syntheticElementType(typeParameters)
        val int64Type = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val isVArrayTarget = target is BuiltinArrayConstructorTarget.VArray
        val valueParameters = when (kind) {
            BuiltinArrayConstructorKind.EMPTY -> emptyList()
            BuiltinArrayConstructorKind.COLLECTION -> listOf(
                buildSyntheticValueParameter(
                    ownerSymbol = symbol,
                    parameterName = ARRAY_COLLECTION_PARAMETER_NAME,
                    parameterType = ConeClassLikeType(
                        StdlibClassIds.Collection.toLookupTag(),
                        typeArguments = listOf(elementType),
                        isInterface = true,
                    ),
                    isNamed = false,
                    source = callInfo.arguments.getOrNull(0)?.source ?: callInfo.callSite.source,
                ),
            )
            BuiltinArrayConstructorKind.INIT_FUNCTION ->
                if (isVArrayTarget) {
                    listOf(
                        buildSyntheticValueParameter(
                            ownerSymbol = symbol,
                            parameterName = ARRAY_INIT_PARAMETER_NAME,
                            parameterType = ConeFunctionType(parameterTypes = listOf(int64Type), returnType = elementType),
                            isNamed = false,
                            source = callInfo.arguments.getOrNull(0)?.source ?: callInfo.callSite.source,
                        ),
                    )
                } else {
                    listOf(
                        buildSyntheticValueParameter(
                            ownerSymbol = symbol,
                            parameterName = ARRAY_SIZE_PARAMETER_NAME,
                            parameterType = int64Type,
                            isNamed = false,
                            source = callInfo.arguments.getOrNull(0)?.source ?: callInfo.callSite.source,
                        ),
                        buildSyntheticValueParameter(
                            ownerSymbol = symbol,
                            parameterName = ARRAY_INIT_PARAMETER_NAME,
                            parameterType = ConeFunctionType(parameterTypes = listOf(int64Type), returnType = elementType),
                            isNamed = false,
                            source = callInfo.arguments.getOrNull(1)?.source ?: callInfo.callSite.source,
                        ),
                    )
                }
            BuiltinArrayConstructorKind.REPEAT_ELEMENT ->
                if (isVArrayTarget) {
                    listOf(
                        buildSyntheticValueParameter(
                            ownerSymbol = symbol,
                            parameterName = ARRAY_REPEAT_PARAMETER_NAME,
                            parameterType = elementType,
                            isNamed = true,
                            source = callInfo.arguments.getOrNull(0)?.source ?: callInfo.callSite.source,
                        ),
                    )
                } else {
                    listOf(
                        buildSyntheticValueParameter(
                            ownerSymbol = symbol,
                            parameterName = ARRAY_SIZE_PARAMETER_NAME,
                            parameterType = int64Type,
                            isNamed = false,
                            source = callInfo.arguments.getOrNull(0)?.source ?: callInfo.callSite.source,
                        ),
                        buildSyntheticValueParameter(
                            ownerSymbol = symbol,
                            parameterName = ARRAY_REPEAT_PARAMETER_NAME,
                            parameterType = elementType,
                            isNamed = true,
                            source = callInfo.arguments.getOrNull(1)?.source ?: callInfo.callSite.source,
                        ),
                    )
                }
        }

        buildNamedFunction {
            source = callInfo.callSite.source
            moduleData = context.session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            this.typeParameters.addAll(typeParameters)
            returnTypeRef = buildResolvedTypeRef {
                source = callInfo.callSite.source
                coneType = target.returnType(elementType)
            }
            this.valueParameters.addAll(valueParameters)
            body = null
            this.symbol = symbol
            name = callInfo.name
            isMut = false
        }

        return createCandidate(
            callInfo = callInfo,
            symbol = symbol,
            originScope = null,
        )
    }

    private fun BuiltinArrayConstructorTarget.syntheticElementType(
        syntheticTypeParameters: List<CfirTypeParameter>,
    ): ConeCangJieType {
        val declaredElementType = (this as? BuiltinArrayConstructorTarget.VArray)?.elementType
        if (declaredElementType == null) {
            val elementTypeParameter = syntheticTypeParameters.firstOrNull()?.symbol
                ?: return ConeErrorType(ConeSimpleDiagnostic("Missing builtin array element type parameter"))
            return ConeTypeParameterTypeImpl(elementTypeParameter.toLookupTag())
        }

        val originalToSynthetic: Map<TypeConstructorMarker, ConeCangJieType> = typeParameters.zip(syntheticTypeParameters)
            .mapNotNull { (original, synthetic) ->
                original.originalSymbol?.toLookupTag()?.let { originalTag ->
                    originalTag to ConeTypeParameterTypeImpl(synthetic.symbol.toLookupTag())
                }
            }
            .toMap()
        if (originalToSynthetic.isEmpty()) return declaredElementType
        return CfirTypeSubstitutorByMap(originalToSynthetic).substituteOrSelf(declaredElementType)
    }

    private fun buildSyntheticValueParameter(
        ownerSymbol: CfirNamedFunctionSymbol,
        parameterName: Name,
        parameterType: ConeCangJieType,
        isNamed: Boolean,
        source: CjSourceElement?,
    ) = buildValueParameter {
        this.source = source
        moduleData = context.session.moduleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true
        dispatchReceiverType = null
        symbol = CfirValueParameterSymbol(CallableId(parameterName))
        containingDeclarationSymbol = ownerSymbol
        this.isNamed = isNamed
        status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
        returnTypeRef = buildResolvedTypeRef {
            this.source = source
            coneType = parameterType
        }
        name = parameterName
        defaultValue = null
    }

    fun createErrorCandidate(callInfo: CallInfo, diagnostic: ConeDiagnostic): Candidate {
        val errorSymbol = when(callInfo.callKind) {
            is CallKind.Function,
            CallKind.DelegatingConstructorCall,
                ->  createErrorFunctionSymbol(diagnostic)

            CallKind.EnumConstructorCall -> createErrorEnumConstructorSymbol(diagnostic)
            CallKind.NamedValueAccess ->
                createErrorNamedValueSymbol(callInfo, diagnostic)
        }
        val candidate = createCandidate(
            callInfo = callInfo,
            symbol = errorSymbol,
            originScope = null,
        )
        when (diagnostic) {
            is org.cangnova.cangjie.cfir.diagnostic.ConeHiddenCandidateError ->
                candidate.addDiagnostic(HiddenCandidate())
            else ->
                candidate.addDiagnostic(InferenceConstraintError(diagnostic.reason))
        }
        return candidate
    }

    private fun createErrorFunctionSymbol(diagnostic: ConeDiagnostic): CfirErrorFunctionSymbol {
        return CfirErrorFunctionSymbol().also {
            buildErrorFunction {
                source = null
                moduleData = context.session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Error
                attributes = CfirDeclarationAttributes()
                dispatchReceiverType = null
                status = CfirDeclarationStatusImpl()
                valueParameters.clear()
                body = null
                this.diagnostic = diagnostic
                symbol = it
            }
        }
    }

    private fun createErrorEnumConstructorSymbol(diagnostic: ConeDiagnostic): CfirErrorEnumConstructorSymbol {
        val callableId = CallableId(org.cangnova.cangjie.name.Name.special("<error-enum-constructor>"))
        return CfirErrorEnumConstructorSymbol(callableId, diagnostic).also {
            buildErrorNamedValue {
                source = null
                moduleData = context.session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Error
                attributes = CfirDeclarationAttributes()
                status = CfirDeclarationStatusImpl()
                this.diagnostic = diagnostic
                name = callableId.callableName
                symbol = it
            }
        }
    }

    private fun createErrorNamedValueSymbol(
        callInfo: CallInfo,
        diagnostic: ConeDiagnostic,
    ): CfirErrorNamedValueSymbol {
        return CfirErrorNamedValueSymbol(callInfo.name.asErrorNamedValueCallableId(), diagnostic).also {
            buildErrorNamedValue {
                source = callInfo.callSite.source
                moduleData = context.session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Error
                attributes = CfirDeclarationAttributes()
                status = CfirDeclarationStatusImpl()
                this.diagnostic = diagnostic
                name = callInfo.name
                symbol = it
            }
        }
    }
}

private fun org.cangnova.cangjie.name.Name.asErrorNamedValueCallableId() =
    org.cangnova.cangjie.name.CallableId(this)

private fun processConstraintStorageFromAtom(
    atom: ConeResolutionAtom,
    processor: (ConstraintStorage) -> Unit,
): Boolean {
    return when (atom) {
        is ConeAtomWithCandidate -> {
            processor(atom.candidate.system.asReadOnlyStorage())
            true
        }
        is ConeResolutionAtomWithSingleChild -> {
            processConstraintStorageFromAtom(atom.subAtom ?: return false, processor)
        }
        else -> false
    }
}
fun PostponedArgumentsAnalyzerContext.addSubsystemFromAtom(atom: ConeResolutionAtom): Boolean {
    return processConstraintStorageFromAtom(atom) {
        // If a call inside a lambda uses outer CS,
        // it's already integrated into inference session via FirPCLAInferenceSession.processPartiallyResolvedCall
        if (!it.usesOuterCs) {
            addOtherSystem(it)
        }
    }
}

private val ARRAY_SIZE_PARAMETER_NAME = Name.identifier("size")
private val ARRAY_COLLECTION_PARAMETER_NAME = Name.identifier("elements")
private val ARRAY_INIT_PARAMETER_NAME = Name.identifier("arrayInit")
private val ARRAY_REPEAT_PARAMETER_NAME = Name.identifier("repeat")
