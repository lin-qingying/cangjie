package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.calls.ReceiverValue
import org.cangnova.cangjie.cfir.diagnostic.HiddenCandidate
import org.cangnova.cangjie.cfir.diagnostic.InferenceConstraintError
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.cangnova.cangjie.cfir.declarations.builder.buildErrorNamedValue
import org.cangnova.cangjie.cfir.declarations.builder.buildErrorFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.utils.addDefaultBoundIfNecessary
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithSingleChild
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.session.inferenceLogger
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirErrorEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirErrorNamedValueSymbol
import org.cangnova.cangjie.cfir.symbols.CfirErrorFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind

/**
 * Small construction seam for candidates discovered during tower traversal.
 *
 * This keeps tower traversal focused on scope walking while preserving the current
 * receiver/base-system defaults used by local call resolution.
 */
internal enum class BuiltinArrayConstructorKind {
    EMPTY,
    COLLECTION,
    INIT_FUNCTION,
    REPEAT_ELEMENT,
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
    }
    fun createCandidate(
        callInfo: CallInfo,
        symbol: CfirCallableSymbol<*>,
        originScope: CfirScope?,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
        dispatchReceiver: ReceiverValue? = null,
        givenExtensionReceiver: ReceiverValue? = null,
    ): Candidate {
        return Candidate(
            symbol = symbol,
            dispatchReceiver = dispatchReceiver?.receiverExpression?.let(ConeResolutionAtom::createRawAtom),
            givenExtensionReceiver = givenExtensionReceiver?.receiverExpression?.let(ConeResolutionAtom::createRawAtom),
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
        )
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
    ): Candidate {
        val symbol = CfirNamedFunctionSymbol(CallableId(callInfo.name))
        val elementTypeParameterSymbol = CfirTypeParameterSymbol()
        val elementTypeParameterName = Name.identifier("T")
        val elementTypeParameter = buildTypeParameter {
            source = callInfo.callSite.source
            moduleData = context.session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor
            attributes = CfirDeclarationAttributes.EMPTY
            containingDeclarationSymbol = symbol
            this.symbol = elementTypeParameterSymbol
            name = elementTypeParameterName
            addDefaultBoundIfNecessary()
        }
        val elementType = ConeTypeParameterTypeImpl(elementTypeParameterSymbol.toLookupTag())
        val int64Type = ConePrimitiveType(PrimitiveTypeKind.INT64)
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
            BuiltinArrayConstructorKind.INIT_FUNCTION -> listOf(
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
            BuiltinArrayConstructorKind.REPEAT_ELEMENT -> listOf(
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

        buildNamedFunction {
            source = callInfo.callSite.source
            moduleData = context.session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            typeParameters.add(elementTypeParameter)
            returnTypeRef = buildResolvedTypeRef {
                source = callInfo.callSite.source
                coneType = ConeClassLikeType(StdlibClassIds.Array.toLookupTag(), typeArguments = listOf(elementType))
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

    /**
     * 构造仓颉原始类型转换表达式的合成候选。
     *
     * 官方 `TypeConvExpr` 对 `Int32(x)` 这类写法独立执行 `SynNumTypeConvExpr`，
     * 语义上不是用户声明的构造器。CFIR 仍复用现有 call candidate 管线承载
     * 参数映射、参数检查和调用完成，因此这里用 synthetic fake function 表达
     * “一个参数转换为目标原始类型”的调用形状。
     */
    fun createPrimitiveTypeConversionCandidate(
        callInfo: CallInfo,
        targetKind: PrimitiveTypeKind,
        sourceType: ConePrimitiveType,
    ): Candidate {
        val symbol = CfirNamedFunctionSymbol(CallableId(callInfo.name))
        val parameterName = Name.identifier("primitiveTypeConversionArg")
        val parameter = buildValueParameter {
            source = callInfo.arguments.singleOrNull()?.source ?: callInfo.callSite.source
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
                source = callInfo.arguments.singleOrNull()?.source ?: callInfo.callSite.source
                coneType = sourceType
            }
            name = parameterName
            defaultValue = null
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
                coneType = ConePrimitiveType(targetKind)
            }
            valueParameters.add(parameter)
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
