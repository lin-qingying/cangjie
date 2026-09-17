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

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.calls.ReceiverValue
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
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
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirPCLAInferenceSession
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.session.inferenceLogger
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaParameterType
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForPostponedAtom
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.UnstableSystemMergeMode
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * tower 遍历阶段发现 callable 后，通过本工厂统一构造候选。
 *
 * 这里保留 candidate 的 receiver 与 constraint-system 初始化规则，让 tower 层
 * 只负责作用域遍历，不把候选构造细节扩散到各个调用入口。
 */
/**
 * 内建 Array/VArray 构造器形态。
 */
internal enum class BuiltinArrayConstructorKind {
    /**
     * 空数组构造。
     */
    EMPTY,
    /**
     * 从集合构造。
     */
    COLLECTION,
    /**
     * 使用初始化函数构造。
     */
    INIT_FUNCTION,
    /**
     * 使用重复元素构造。
     */
    REPEAT_ELEMENT,
}

/**
 * 内建构造器合成类型参数描述。
 */
internal data class BuiltinConstructorTypeParameter(
    /**
     * 合成类型参数名称。
     */
    val name: Name,
    /**
     * 可选的原始类型参数符号，用于把已声明类型代入合成参数。
     */
    val originalSymbol: CfirTypeParameterSymbol? = null,
    /** 合成签名自身要求的上界，与 typealias 的原始约束共同应用。 */
    val additionalBounds: List<ConeCangJieType> = emptyList(),
)

/**
 * 内建数组构造器目标。
 */
internal sealed class BuiltinArrayConstructorTarget {
    /**
     * 构造器目标暴露的类型参数。
     */
    abstract val typeParameters: List<BuiltinConstructorTypeParameter>
    /**
     * 根据元素类型构造返回类型。
     */
    abstract fun returnType(elementType: ConeCangJieType): ConeCangJieType

    /**
     * 标准库 `Array<T>` 构造目标。
     */
    data object Array : BuiltinArrayConstructorTarget() {
        /**
         * `Array<T>` 的元素类型参数。
         */
        override val typeParameters: List<BuiltinConstructorTypeParameter> =
            listOf(BuiltinConstructorTypeParameter(Name.identifier("T")))

        /**
         * 构造 `Array<elementType>` 返回类型。
         */
        override fun returnType(elementType: ConeCangJieType): ConeCangJieType =
            ConeClassLikeType(StdlibClassIds.Array.toLookupTag(), typeArguments = listOf(elementType))
    }

    /**
     * `VArray<size, T>` 构造目标。
     */
    data class VArray(
        /**
         * VArray 大小字面量。
         */
        val sizeLiteral: String,
        /**
         * 已声明的元素类型；为空时由合成类型参数推断。
         */
        val elementType: ConeCangJieType? = null,
        /**
         * VArray 构造目标的类型参数。
         */
        override val typeParameters: List<BuiltinConstructorTypeParameter> =
            listOf(BuiltinConstructorTypeParameter(Name.identifier("T"))),
    ) : BuiltinArrayConstructorTarget() {
        /**
         * 构造 VArray 返回类型。
         */
        override fun returnType(elementType: ConeCangJieType): ConeCangJieType {
            val size = CfirIntConstantEvalUtils.parseVArraySizeLiteral(sizeLiteral)
                ?: return ConeErrorType(ConeSimpleDiagnostic("Invalid VArray size: $sizeLiteral"))
            return ConeVArrayType(elementType, size)
        }
    }
}

/**
 * 内建指针构造器形态。
 */
internal enum class BuiltinPointerConstructorKind {
    /**
     * 空指针构造。
     */
    EMPTY,
    /**
     * 指针转换构造。
     */
    CONVERT_POINTER,
}

/**
 * 内建指针构造器目标。
 */
internal data class BuiltinPointerConstructorTarget(
    /**
     * 指针指向类型；为空时由合成类型参数推断。
     */
    val pointeeType: ConeCangJieType? = null,
    /**
     * 指针构造目标的类型参数。
     */
    val typeParameters: List<BuiltinConstructorTypeParameter> =
        listOf(
            BuiltinConstructorTypeParameter(
                name = Name.identifier("T"),
            ),
        ),
)

/**
 * 候选工厂。
 *
 * 负责构造普通 callable 候选、函数类型 invoke 候选、内建构造器候选和错误候选。
 */
class CandidateFactory(
    /**
     * 当前调用解析上下文。
     */
    private val context: ResolutionContext,
    /**
     * 新候选使用的基础约束系统。
     */
    private val baseSystem: ConstraintStorage  ,
) {
    /**
     * 以调用信息构造候选工厂。
     */
    constructor(context: ResolutionContext, callInfo: CallInfo) : this(context, buildBaseSystem(context, callInfo))
    /**
     * 候选工厂静态构造辅助。
     */
    companion object {
        /**
         * 为 collection literal 或 callable reference 等含外层调用的场景构造基础系统。
         */
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
        /**
         * 构造普通调用候选的基础约束系统。
         */
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

        /**
         * 创建 callable reference 候选工厂。
         */
        fun createForCallableReferenceCandidate(
            context: ResolutionContext,
            containingCall: Candidate,
        ): CandidateFactory =
            CandidateFactory(context, buildBaseSystemForContainingCallAwareCases(context, containingCall, callInfo = null))
    }

    /**
     * 基于已有候选创建 callable reference 候选。
     */
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
            discoveryAccessibilityResult = originalCandidate.discoveryAccessibilityResult,
            lookupProvenance = originalCandidate.lookupProvenance,
            isFromCompanionObjectTypeScope = originalCandidate.isFromCompanionObjectTypeScope,
            isFromOriginalTypeInPresenceOfSmartCast = originalCandidate.isFromOriginalTypeInPresenceOfSmartCast,
            bodyResolveContext = context.bodyResolveContext,
        )
    }

    /**
     * 仅从 tower discovery 的不可变字段重建普通 callable 候选。
     *
     * expected-return 细化不能复用旧候选的约束系统、stage 进度或诊断；receiver 也必须
     * 重新包装为 raw atom，使新调用在独立的 [CallInfo] 与基础约束系统上完整执行 stages。
     */
    fun createCandidateFromDiscovery(
        callInfo: CallInfo,
        discovery: CfirCallableCandidateDiscovery,
    ): Candidate = Candidate(
        symbol = discovery.symbol,
        dispatchReceiver = discovery.dispatchReceiverExpression?.let(ConeResolutionAtom::createRawAtom),
        givenExtensionReceiver = discovery.givenExtensionReceiverExpression?.let(ConeResolutionAtom::createRawAtom),
        explicitReceiverKind = discovery.explicitReceiverKind,
        constraintSystemFactory = context.inferenceComponents.constraintSystemFactory,
        baseSystem = baseSystem,
        callInfo = callInfo,
        originScope = discovery.originScope,
        discoveryAccessibilityResult = discovery.accessibilityResult,
        lookupProvenance = discovery.lookupProvenance,
        isFromCompanionObjectTypeScope = discovery.isFromCompanionObjectTypeScope,
        isFromOriginalTypeInPresenceOfSmartCast = discovery.isFromOriginalTypeInPresenceOfSmartCast,
        bodyResolveContext = context.bodyResolveContext,
    )

    /**
     * 基于已解析的函数值变量创建调用候选。
     *
     * `f(x)` 这种形态先要把 `f` 解析成普通值，再把该值的函数类型参数映射到
     * 源码实参。lambda 初始化器已在定义点完成，调用只消费已经发布的函数类型。
     */
    fun createCallableValueInvokeCandidate(
        callInfo: CallInfo,
        callableValueCandidate: Candidate,
        callableValueReceiver: CfirExpression? = null,
    ): Candidate {
        val variable = (callableValueCandidate.symbol as? CfirVariableSymbol<*>)?.cfir
        val freshValueParameterInvokeShape = buildFreshValueParameterInvokeShape(
            callInfo = callInfo,
            callableValueCandidate = callableValueCandidate,
            variable = variable,
        )
        val candidate = Candidate(
            symbol = callableValueCandidate.symbol,
            // 隐式 invoke 的最终调用没有源码显式 receiver；仍需把函数值本身
            // 保留为 dispatch receiver，供 completion、checker 和诊断消费其完整
            // ConeFunctionType（尤其是 CFunc ABI 标记）。
            dispatchReceiver = callableValueCandidate.dispatchReceiver
                ?: callableValueReceiver?.let(ConeResolutionAtom::createRawAtom),
            givenExtensionReceiver = callableValueCandidate.givenExtensionReceiver,
            explicitReceiverKind = callableValueCandidate.explicitReceiverKind,
            constraintSystemFactory = context.inferenceComponents.constraintSystemFactory,
            baseSystem = baseSystem.withCallableValueReceiverSystems(
                callableValueCandidate.system.currentStorage(),
            ),
            callInfo = callInfo,
            originScope = callableValueCandidate.originScope,
            discoveryAccessibilityResult = callableValueCandidate.discoveryAccessibilityResult,
            isFromCompanionObjectTypeScope = callableValueCandidate.isFromCompanionObjectTypeScope,
            isFromOriginalTypeInPresenceOfSmartCast = callableValueCandidate.isFromOriginalTypeInPresenceOfSmartCast,
            bodyResolveContext = context.bodyResolveContext,
        )

        if (freshValueParameterInvokeShape != null) {
            candidate.registerFreshValueParameterInvokeShape(freshValueParameterInvokeShape)
        }
        return candidate
    }

    /**
     * 为 fresh lambda 形参的直接调用构造函数形状。
     *
     * `g(0)` 中 `g` 初始只有 lambda 参数占位类型；这里把调用语法转成
     * `g <: (Int64) -> R` 形式的约束，并让后续实参映射按同一个函数形状执行。
     */
    private fun buildFreshValueParameterInvokeShape(
        callInfo: CallInfo,
        callableValueCandidate: Candidate,
        variable: CfirVariable?,
    ): FreshValueParameterInvokeShape? {
        if (variable !is CfirValueParameter) return null
        val receiverType = variable.returnTypeRef.coneTypeOrNull as? ConeTypeVariableType ?: return null
        if (receiverType.typeConstructor.originalTypeParameter != null) return null
        if (receiverType.typeConstructor !in callableValueCandidate.system.currentStorage().allTypeVariables) return null
        val pclaSession = context.bodyResolveContext.inferenceSession as? CfirPCLAInferenceSession ?: return null
        val createdVariables = mutableListOf<ConeTypeVariable>()
        val parameterTypes = callInfo.arguments.mapIndexed { index, argument ->
            argument.coneTypeOrNull
                ?.let(IdealTypeResolver::resolveIfIdeal)
                ?: ConeTypeVariableForLambdaParameterType("InvokeParameter$index")
                    .also { freshVariable ->
                        createdVariables += freshVariable
                        pclaSession.registerInferenceVariable(freshVariable)
                    }
                    .defaultType
        }
        val returnVariable = ConeTypeVariableForPostponedAtom("InvokeReturn")
            .also { freshVariable ->
                createdVariables += freshVariable
                pclaSession.registerInferenceVariable(freshVariable)
            }
        val functionType = ConeFunctionType(
            parameterTypes = parameterTypes,
            returnType = returnVariable.defaultType,
        )
        pclaSession.addSubtypeConstraintIfCompatible(receiverType, functionType)
        return FreshValueParameterInvokeShape(
            receiverType = receiverType,
            functionType = functionType,
            createdVariables = createdVariables,
        )
    }

    /** fresh lambda 形参调用语法反推出的函数值形状。 */
    private data class FreshValueParameterInvokeShape(
        /** 被调用形参的原始占位类型。 */
        val receiverType: ConeTypeVariableType,
        /** 当前调用使用的函数类型形状。 */
        val functionType: ConeFunctionType,
        /** 该函数形状内部新建的推断变量。 */
        val createdVariables: List<ConeTypeVariable>,
    )

    /** 将 fresh 形参函数形状接入新建的函数值调用候选。 */
    private fun Candidate.registerFreshValueParameterInvokeShape(shape: FreshValueParameterInvokeShape) {
        registerCallableValueInvokeFunctionShape(shape.functionType)
        additionalCompletionVariables += shape.createdVariables.map { it.typeConstructor }
        val builder = system.getBuilder()
        for (createdVariable in shape.createdVariables) {
            if (createdVariable.typeConstructor !in builder.currentStorage().allTypeVariables) {
                builder.registerVariable(createdVariable)
            }
        }
        builder.addSubtypeConstraint(
            shape.receiverType,
            shape.functionType,
            ConeExpectedTypeConstraintPosition,
        )
    }

    /**
     * 创建普通 callable 候选。
     */
    fun createCandidate(
        callInfo: CallInfo,
        symbol: CfirCallableSymbol<*>,
        originScope: CfirScope?,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
        dispatchReceiver: ReceiverValue? = null,
        givenExtensionReceiver: ReceiverValue? = null,
        baseSystem: ConstraintStorage = this.baseSystem,
        accessibilityResult: CfirAccessibilityResult? = null,
        lookupProvenance: org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance =
            org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance.None,
    ): Candidate {
        val useDispatchReceiverAsExtensionReceiver =
            givenExtensionReceiver == null &&
                dispatchReceiver != null &&
                symbol.isInstanceExtendMemberCandidate(
                    context.session,
                    CfirAccessContext(
                        useSiteFile = callInfo.containingFile,
                        containingDeclarations = callInfo.containingDeclarations,
                        receiverType = dispatchReceiver.type,
                        kind = CfirAccessKind.EXTEND,
                    ),
                )
        val effectiveDispatchReceiver = if (useDispatchReceiverAsExtensionReceiver) null else dispatchReceiver
        val effectiveExtensionReceiver = givenExtensionReceiver ?: dispatchReceiver.takeIf {
            useDispatchReceiverAsExtensionReceiver
        }
        val candidate = Candidate(
            symbol = symbol,
            dispatchReceiver = effectiveDispatchReceiver?.receiverExpression?.let(ConeResolutionAtom::createRawAtom),
            givenExtensionReceiver = effectiveExtensionReceiver?.receiverExpression?.let(ConeResolutionAtom::createRawAtom),
            explicitReceiverKind = explicitReceiverKind,
            constraintSystemFactory = context.inferenceComponents.constraintSystemFactory,
            baseSystem = baseSystem,
            callInfo = callInfo,
            originScope = originScope,
            discoveryAccessibilityResult = accessibilityResult,
            lookupProvenance = lookupProvenance,
            bodyResolveContext = context.bodyResolveContext,
        )
        return candidate
    }

    /**
     * 为函数类型接收者创建 synthetic invoke 候选。
     */
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

        val candidate = createCandidate(
            callInfo = callInfo,
            symbol = symbol,
            originScope = null,
            explicitReceiverKind = explicitReceiverKind,
            dispatchReceiver = dispatchReceiver,
            baseSystem = baseSystem.withSubsystemFromInvokeReceiver(
                receiverExpression,
            ),
        )
        candidate.registerFunctionTypeInvokeCompletionVariables(functionType)
        return candidate
    }

    /**
     * 将函数类型 invoke 接收者的子系统并入基础系统。
     */
    private fun ConstraintStorage.withSubsystemFromInvokeReceiver(
        receiverExpression: CfirExpression,
    ): ConstraintStorage {
        val receiverAtom = ConeResolutionAtom.createRawAtom(receiverExpression)
        val system = context.inferenceComponents.createConstraintSystem()
        system.setBaseSystem(this)
        system.addSubsystemFromAtom(receiverAtom)
        return system.asReadOnlyStorage()
    }

    /**
     * 函数类型 `invoke` 的 receiver 可能携带上游调用留下的推断变量。
     *
     * 这些变量不属于 synthetic invoke 函数声明本身；若不显式暴露给 completion，
     * invoke 实参和返回 expected type 产生的约束只会留在系统里，变量不会进入固定队列。
     */
    private fun Candidate.registerFunctionTypeInvokeCompletionVariables(functionType: ConeFunctionType) {
        additionalCompletionVariables += functionType.completionBoundaryTypeVariablesIn(system.currentStorage().allTypeVariables.keys)
    }

    /** 收集函数类型边界上当前约束系统可见的推断变量。 */
    private fun ConeCangJieType.completionBoundaryTypeVariablesIn(
        availableConstructors: Set<TypeConstructorMarker>,
    ): Set<TypeConstructorMarker> {
        val result = linkedSetOf<TypeConstructorMarker>()

        fun ConeCangJieType.collect() {
            when (this) {
                is ConeTypeVariableType -> {
                    if (typeConstructor in availableConstructors) {
                        result += typeConstructor
                    }
                }
                is ConeLookupTagBasedType -> typeArguments.forEach { it.type.collect() }
                is ConeFunctionType -> {
                    parameterTypes.forEach { it.collect() }
                    returnType.collect()
                }
                is ConeTupleType -> elementTypes.forEach { it.collect() }
                is ConeVArrayType -> elementType.collect()
                is ConePointerType -> pointeeType.collect()
                is ConeTypeAliasType -> {
                    typeArguments.forEach { it.type.collect() }
                    expandedType?.collect()
                }
                else -> Unit
            }
        }

        collect()
        return result
    }

    /**
     * 将函数值 receiver 候选的约束接入当前调用，保留上游 PCLA 的系统边界。
     */
    private fun ConstraintStorage.withCallableValueReceiverSystems(
        callableValueReceiverStorage: ConstraintStorage,
    ): ConstraintStorage {
        val system = context.inferenceComponents.createConstraintSystem()
        if (callableValueReceiverStorage.usesOuterCs && !usesOuterCs) {
            /*
             * 函数值 receiver 本身可能是 postponed PCLA 调用，必须作为 invoke 的外层
             * 系统导入；把它作为根系统的普通同级子系统会破坏嵌套约束系统的不变量。
             */
            system.addOuterSystem(callableValueReceiverStorage)
            system.addOtherSystem(this)
        } else {
            system.setBaseSystem(this)
            system.addOtherSystem(callableValueReceiverStorage)
        }
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
        val typeParameters = buildSyntheticTypeParameters(
            ownerSymbol = symbol,
            parameters = target.typeParameters,
            source = callInfo.callSite.source,
            origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor,
        )
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
                    origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor,
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
                            origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor,
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
                            origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor,
                        ),
                        buildSyntheticValueParameter(
                            ownerSymbol = symbol,
                            parameterName = ARRAY_INIT_PARAMETER_NAME,
                            parameterType = ConeFunctionType(parameterTypes = listOf(int64Type), returnType = elementType),
                            isNamed = false,
                            source = callInfo.arguments.getOrNull(1)?.source ?: callInfo.callSite.source,
                            origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor,
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
                            origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor,
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
                            origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor,
                        ),
                        buildSyntheticValueParameter(
                            ownerSymbol = symbol,
                            parameterName = ARRAY_REPEAT_PARAMETER_NAME,
                            parameterType = elementType,
                            isNamed = true,
                            source = callInfo.arguments.getOrNull(1)?.source ?: callInfo.callSite.source,
                            origin = CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor,
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

    /**
     * 构造 `CPointer<T>(...)` 内建构造表达式候选。
     *
     * 官方前端将该调用解糖为 `PointerExpr`，并允许零参空指针构造或一个
     * `CPointer<T>` 参数的指针转换；CFIR 在 synthetic function 中保留同一
     * 参数形状，让普通参数映射与约束系统决定显式/隐式 `T`。
     */
    internal fun createBuiltinPointerConstructorCandidate(
        callInfo: CallInfo,
        kind: BuiltinPointerConstructorKind,
        target: BuiltinPointerConstructorTarget = BuiltinPointerConstructorTarget(),
    ): Candidate {
        val symbol = CfirNamedFunctionSymbol(CallableId(callInfo.name))
        val typeParameters = buildSyntheticTypeParameters(
            ownerSymbol = symbol,
            parameters = target.typeParameters.map { parameter ->
                if (target.pointeeType == null ||
                    (target.pointeeType as? ConeTypeParameterType)?.lookupTag == parameter.originalSymbol?.toLookupTag()
                ) parameter.copy(additionalBounds = listOf(ConeClassLikeType(StdlibClassIds.CType.toLookupTag(), isInterface = true)))
                else parameter
            },
            source = callInfo.callSite.source,
            origin = CfirDeclarationOrigin.Synthetic.BuiltinPointerConstructor,
        )
        val pointeeType = target.syntheticPointeeType(typeParameters)
        val pointerType = ConePointerType(pointeeType)
        val valueParameters = when (kind) {
            BuiltinPointerConstructorKind.EMPTY -> emptyList()
            BuiltinPointerConstructorKind.CONVERT_POINTER -> listOf(
                buildSyntheticValueParameter(
                    ownerSymbol = symbol,
                    parameterName = POINTER_VALUE_PARAMETER_NAME,
                    parameterType = pointerType,
                    isNamed = false,
                    source = callInfo.arguments.getOrNull(0)?.source ?: callInfo.callSite.source,
                    origin = CfirDeclarationOrigin.Synthetic.BuiltinPointerConstructor,
                ),
            )
        }

        buildNamedFunction {
            source = callInfo.callSite.source
            moduleData = context.session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.BuiltinPointerConstructor
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            this.typeParameters.addAll(typeParameters)
            returnTypeRef = buildResolvedTypeRef {
                source = callInfo.callSite.source
                coneType = pointerType
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
     * 构造 `CFunc<(CType...) -> CType>(CPointer)` 内建构造表达式候选。
     *
     * 官方 `SynCFuncCall` 的唯一值参数只要求其结果是 CPointer；它不会把
     * 指针的指向类型与 CFunc 的函数签名建立泛型约束。因此这里使用无约束的
     * `CPointer<Any>` 作为 synthetic 参数形状，并由 CFunc 专用参数检查阶段
     * 只接受已解析的 [ConePointerType]。CFunc 的源码类型实参已经被调用方
     * 转换为返回值签名，不作为 synthetic callable 的显式类型实参重复映射。
     */
    internal fun createBuiltinCFuncConstructorCandidate(
        callInfo: CallInfo,
        functionType: ConeFunctionType,
    ): Candidate {
        val symbol = CfirNamedFunctionSymbol(CallableId(callInfo.name))
        val cFuncType = ConeFunctionType(
            parameterTypes = functionType.parameterTypes,
            returnType = functionType.returnType,
            isCFunc = true,
            isClosureType = functionType.isClosureType,
            hasVariableLenArg = functionType.hasVariableLenArg,
            attributes = functionType.attributes,
        )
        val valueParameter = buildSyntheticValueParameter(
            ownerSymbol = symbol,
            parameterName = CFUNC_POINTER_PARAMETER_NAME,
            parameterType = ConePointerType(ConeAnyType),
            isNamed = false,
            source = callInfo.arguments.getOrNull(0)?.source ?: callInfo.callSite.source,
            origin = CfirDeclarationOrigin.Synthetic.BuiltinCFuncConstructor,
        )

        buildNamedFunction {
            source = callInfo.callSite.source
            moduleData = context.session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.BuiltinCFuncConstructor
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildResolvedTypeRef {
                source = callInfo.callSite.source
                coneType = cFuncType
            }
            valueParameters.add(valueParameter)
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
     * 构造 `CString(CPointer<UInt8>)` 内建构造表达式候选。
     */
    internal fun createBuiltinCStringConstructorCandidate(callInfo: CallInfo): Candidate {
        val symbol = CfirNamedFunctionSymbol(CallableId(callInfo.name))
        val uint8Pointer = ConePointerType(ConePrimitiveType(PrimitiveTypeKind.UINT8))
        val valueParameter = buildSyntheticValueParameter(
            ownerSymbol = symbol,
            parameterName = CSTRING_POINTER_PARAMETER_NAME,
            parameterType = uint8Pointer,
            isNamed = false,
            source = callInfo.arguments.getOrNull(0)?.source ?: callInfo.callSite.source,
            origin = CfirDeclarationOrigin.Synthetic.BuiltinCStringConstructor,
        )

        buildNamedFunction {
            source = callInfo.callSite.source
            moduleData = context.session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.BuiltinCStringConstructor
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildResolvedTypeRef {
                source = callInfo.callSite.source
                coneType = ConeCStringType()
            }
            valueParameters.add(valueParameter)
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
     * 构造 compiler-core `composition<T1, T2, T3>` 候选。
     *
     * 官方 `DesugarCompositionExpr` 把 `f ~> g` 固定解糖为
     * `std.core.composition(f, g)`，其签名为
     * `(T1 -> T2, T2 -> T3) -> T1 -> T3`。此符号是编译器内部引用，
     * 不能因为测试 stdlib cjo 未导出该实现就退化为用户作用域里的同名函数。
     */
    internal fun createCompilerCoreCompositionCandidate(callInfo: CallInfo): Candidate {
        val symbol = CfirNamedFunctionSymbol(CallableId(StandardNames.FqNames.core, callInfo.name))
        val typeParameters = buildSyntheticTypeParameters(
            ownerSymbol = symbol,
            parameters = listOf(
                BuiltinConstructorTypeParameter(Name.identifier("T1")),
                BuiltinConstructorTypeParameter(Name.identifier("T2")),
                BuiltinConstructorTypeParameter(Name.identifier("T3")),
            ),
            source = callInfo.callSite.source,
            origin = CfirDeclarationOrigin.Synthetic.Default,
        )
        val (inputType, intermediateType, outputType) = typeParameters.map { typeParameter ->
            ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
        }
        val firstFunctionType = ConeFunctionType(
            parameterTypes = listOf(inputType),
            returnType = intermediateType,
        )
        val secondFunctionType = ConeFunctionType(
            parameterTypes = listOf(intermediateType),
            returnType = outputType,
        )
        val valueParameters = listOf(
            buildSyntheticValueParameter(
                ownerSymbol = symbol,
                parameterName = Name.identifier("f"),
                parameterType = firstFunctionType,
                isNamed = false,
                source = callInfo.arguments.getOrNull(0)?.source ?: callInfo.callSite.source,
                origin = CfirDeclarationOrigin.Synthetic.Default,
            ),
            buildSyntheticValueParameter(
                ownerSymbol = symbol,
                parameterName = Name.identifier("g"),
                parameterType = secondFunctionType,
                isNamed = false,
                source = callInfo.arguments.getOrNull(1)?.source ?: callInfo.callSite.source,
                origin = CfirDeclarationOrigin.Synthetic.Default,
            ),
        )

        buildNamedFunction {
            source = callInfo.callSite.source
            moduleData = context.session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            this.typeParameters.addAll(typeParameters)
            returnTypeRef = buildResolvedTypeRef {
                source = callInfo.callSite.source
                coneType = ConeFunctionType(parameterTypes = listOf(inputType), returnType = outputType)
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
     * 计算内建数组构造器使用的合成元素类型。
     */
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

    /**
     * 计算内建指针构造器使用的合成 pointee 类型。
     */
    private fun BuiltinPointerConstructorTarget.syntheticPointeeType(
        syntheticTypeParameters: List<CfirTypeParameter>,
    ): ConeCangJieType {
        if (pointeeType == null) {
            val pointeeTypeParameter = syntheticTypeParameters.firstOrNull()?.symbol
                ?: return ConeErrorType(ConeSimpleDiagnostic("Missing builtin pointer type parameter"))
            return ConeTypeParameterTypeImpl(pointeeTypeParameter.toLookupTag())
        }

        val originalToSynthetic: Map<TypeConstructorMarker, ConeCangJieType> = typeParameters.zip(syntheticTypeParameters)
            .mapNotNull { (original, synthetic) ->
                original.originalSymbol?.toLookupTag()?.let { originalTag ->
                    originalTag to ConeTypeParameterTypeImpl(synthetic.symbol.toLookupTag())
                }
            }
            .toMap()
        if (originalToSynthetic.isEmpty()) return pointeeType
        return CfirTypeSubstitutorByMap(originalToSynthetic).substituteOrSelf(pointeeType)
    }

    /**
     * 为 synthetic builtin 构造器创建类型参数声明。
     */
    private fun buildSyntheticTypeParameters(
        ownerSymbol: CfirNamedFunctionSymbol,
        parameters: List<BuiltinConstructorTypeParameter>,
        source: CjSourceElement?,
        origin: CfirDeclarationOrigin.Synthetic,
    ): List<CfirTypeParameter> {
        val symbols = parameters.map { CfirTypeParameterSymbol() }
        val substitution: Map<TypeConstructorMarker, ConeCangJieType> = parameters.zip(symbols).mapNotNull { (parameter, symbol) ->
            parameter.originalSymbol?.toLookupTag()?.let { it to ConeTypeParameterTypeImpl(symbol.toLookupTag()) }
        }.toMap()
        val substitutor = CfirTypeSubstitutorByMap(substitution)
        return parameters.mapIndexed { index, parameter ->
            buildTypeParameter {
                this.source = source
                moduleData = context.session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                this.origin = origin
                attributes = CfirDeclarationAttributes.EMPTY
                containingDeclarationSymbol = ownerSymbol
                symbol = symbols[index]
                name = parameter.name
                // 先建立全部 fresh symbols，再同时替换跨参数上界，避免别名丢失 where 约束。
                parameter.originalSymbol?.cfir?.bounds?.forEach { bound ->
                    bounds += buildResolvedTypeRef { this.source = bound.source; coneType = substitutor.substituteOrSelf(bound.coneType) }
                }
                parameter.additionalBounds.forEach { bound ->
                    bounds += buildResolvedTypeRef { this.source = source; coneType = bound }
                }
                addDefaultBoundIfNecessary()
            }
        }
    }

    /**
     * 为 synthetic builtin 构造器创建值参数声明。
     */
    private fun buildSyntheticValueParameter(
        ownerSymbol: CfirNamedFunctionSymbol,
        parameterName: Name,
        parameterType: ConeCangJieType,
        isNamed: Boolean,
        source: CjSourceElement?,
        origin: CfirDeclarationOrigin.Synthetic,
    ) = buildValueParameter {
        this.source = source
        moduleData = context.session.moduleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        this.origin = origin
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

    /**
     * 创建携带诊断的错误候选。
     */
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

    /**
     * 创建错误函数符号。
     */
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

    /**
     * 创建错误 enum constructor 符号。
     */
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

    /**
     * 创建错误命名值符号。
     */
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

/**
 * 将名称转换为错误命名值 callableId。
 */
private fun org.cangnova.cangjie.name.Name.asErrorNamedValueCallableId() =
    org.cangnova.cangjie.name.CallableId(this)

/**
 * 从 resolution atom 中提取并处理约束系统。
 */
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

/**
 * 将 atom 中已有子系统并入 postponed arguments analyzer 上下文。
 */
fun PostponedArgumentsAnalyzerContext.addSubsystemFromAtom(atom: ConeResolutionAtom): Boolean {
    return processConstraintStorageFromAtom(atom) {
        // If a call inside a lambda uses outer CS,
        // it's already integrated into inference session via FirPCLAInferenceSession.processPartiallyResolvedCall
        if (!it.usesOuterCs) {
            addSubsystemStorage(it)
        }
    }
}

/**
 * 将子表达式候选约束系统接入当前完成系统。
 *
 * 同一个子候选可能先作为实参被加入基础系统，随后在实参检查或 PCLA 重算中继续产生约束。
 * 当两个系统共享同一未固定类型变量时，直接追加会用后来的快照覆盖已有约束；这里改为合并，
 * 保留嵌套调用 payload、lambda 参数形状和外层 expected type 共同产生的约束。
 */
@OptIn(UnstableSystemMergeMode::class)
private fun PostponedArgumentsAnalyzerContext.addSubsystemStorage(storage: ConstraintStorage) {
    val hasSharedNotFixedVariable = storage.notFixedTypeVariables.keys.any { it in notFixedTypeVariables }
    if (hasSharedNotFixedVariable) {
        mergeOtherSystem(storage)
    } else {
        addOtherSystem(storage)
    }
}

/**
 * 内建 Array size 参数名。
 */
private val ARRAY_SIZE_PARAMETER_NAME = Name.identifier("size")
/**
 * 内建 Array collection 参数名。
 */
private val ARRAY_COLLECTION_PARAMETER_NAME = Name.identifier("elements")
/**
 * 内建 Array 初始化函数参数名。
 */
private val ARRAY_INIT_PARAMETER_NAME = Name.identifier("arrayInit")
/**
 * 内建 Array 重复元素参数名。
 */
private val ARRAY_REPEAT_PARAMETER_NAME = Name.identifier("repeat")
/**
 * 内建指针值参数名。
 */
private val POINTER_VALUE_PARAMETER_NAME = Name.identifier("value")
/**
 * 内建 CString 指针参数名。
 */
private val CSTRING_POINTER_PARAMETER_NAME = Name.identifier("pointer")

/**
 * 内建 CFunc 构造器的唯一指针参数名。
 */
private val CFUNC_POINTER_PARAMETER_NAME = Name.identifier("pointer")
