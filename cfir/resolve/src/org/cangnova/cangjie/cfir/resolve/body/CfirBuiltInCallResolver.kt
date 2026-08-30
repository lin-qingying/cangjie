package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInTypeKind
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.expandedBuiltInKind
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedClass
import org.cangnova.cangjie.cfir.resolve.calls.CandidateProcessingMode
import org.cangnova.cangjie.cfir.resolve.calls.candidate.BuiltinArrayConstructorKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.BuiltinArrayConstructorTarget
import org.cangnova.cangjie.cfir.resolve.calls.candidate.BuiltinConstructorTypeParameter
import org.cangnova.cangjie.cfir.resolve.calls.candidate.BuiltinPointerConstructorKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.BuiltinPointerConstructorTarget
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CandidateFactory
import org.cangnova.cangjie.cfir.resolve.calls.overloads.ConeCallConflictResolver
import org.cangnova.cangjie.cfir.resolve.calls.stages.fullyProcessCandidate
import org.cangnova.cangjie.cfir.symbols.CfirBuiltInTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.text

/**
 * 内建调用解析完成后的候选结果。
 *
 * 内建调用与普通 tower 调用共享 CandidateFactory 和 stage pipeline，区别只在于
 * 候选不是从声明 scope 发现，而是由官方 `ChkBuiltinCall` 对应的固定形状合成。
 */
internal data class CfirBuiltInCallResolution(
    /** 合成调用使用的完整调用信息。 */
    val info: CallInfo,
    /** 候选集合整体适用性。 */
    val applicability: CandidateApplicability,
    /** 已完成 stage 处理并规约的候选。 */
    val candidates: Collection<Candidate>,
)

/**
 * 仓颉内建调用的统一 resolver。
 *
 * 官方 `ChkBuiltinCall` 按 Pointer、CString、Array、VArray、CFunc 顺序尝试内建目标。
 * 这里只负责 classifier 识别、typealias 展开和内建候选构造；参数映射、约束求解、
 * expected type 与 lambda body 规约仍然进入现有统一调用解析流水线。
 */
@OptIn(ApplicabilityDetail::class)
internal class CfirBuiltInCallResolver(
    /** 当前 body resolve 共享组件。 */
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    /** 当前 body resolve 的表达式 transformer，用于解析上下文与外层声明。 */
    private val transformer: CfirExpressionsResolveTransformer,
    /** 内建候选规约使用的冲突解析器。 */
    private val conflictResolver: ConeCallConflictResolver,
) {
    /** 当前 resolver 使用的 CFIR session。 */
    private val session get() = components.session

    /**
     * 按官方顺序尝试源码直接写出的内建构造调用。
     *
     * classifier lookup 由调用方提供，使局部同名 classifier 可以自然遮蔽 builtin；
     * 只有实际 classifier 是对应 BuiltInDecl（或缺失而没有可遮蔽目标）时才进入固定
     * 的内建候选路径。
     */
    fun tryResolveDirect(
        functionCall: CfirFunctionCall,
        name: Name,
        resolutionMode: ResolutionMode,
        hasNoCallableCandidates: Boolean,
        findClassifier: (CfirFunctionCall, Name) -> CfirClassLikeSymbol<*>?,
    ): CfirBuiltInCallResolution? {
        if (!hasNoCallableCandidates) return null

        val classifier = findClassifier(functionCall, name)
        if (name == StandardNames.CPOINTER && classifier.isBuiltin(CfirBuiltInTypeKind.CPOINTER)) {
            return collectBuiltinPointerConstructorCandidates(
                functionCall = functionCall,
                name = name,
                target = BuiltinPointerConstructorTarget(),
                resolutionMode = resolutionMode,
            )
        }
        if (name == StandardNames.CSTRING && classifier.isBuiltin(CfirBuiltInTypeKind.CSTRING)) {
            return collectBuiltinCStringConstructorCandidates(
                functionCall = functionCall,
                name = name,
                resolutionMode = resolutionMode,
            )
        }
        if (name == StandardNames.ARRAY && classifier.isStdlibArrayClassifier()) {
            return collectBuiltinArrayConstructorCandidates(
                functionCall = functionCall,
                name = name,
                target = BuiltinArrayConstructorTarget.Array,
                resolutionMode = resolutionMode,
            )
        }
        if (name == StandardNames.VARRAY && classifier.isBuiltin(CfirBuiltInTypeKind.VARRAY)) {
            val sizeLiteral = functionCall.varraySizeLiteral ?: return null
            return collectBuiltinArrayConstructorCandidates(
                functionCall = functionCall,
                name = name,
                target = BuiltinArrayConstructorTarget.VArray(sizeLiteral = sizeLiteral),
                resolutionMode = resolutionMode,
            )
        }
        // CFunc is a type-level builtin. It is handled by classifier → ConeType construction,
        // and intentionally does not synthesize a value-level call candidate here.
        return null
    }

    /**
     * 尝试解析 classifier 作为构造调用目标的内建语义。
     *
     * typealias 必须先按其展开后的 BuiltInType 判定，随后才读取应用到别名的实参；
     * 这样 `typealias P<T> = CPointer<T>` 与直接 `CPointer<T>` 使用完全相同的候选形状。
     */
    fun tryResolveClassConstructor(
        functionCall: CfirFunctionCall,
        classifier: CfirClassLikeSymbol<*>,
        resolutionMode: ResolutionMode,
    ): CfirBuiltInCallResolution? {
        val typeAliasSymbol = classifier as? CfirTypeAliasSymbol
        if (typeAliasSymbol != null) {
            when (typeAliasSymbol.cfir.expandedBuiltInKind(session)) {
                CfirBuiltInTypeKind.VARRAY ->
                    typeAliasVArrayConstructorTarget(typeAliasSymbol, functionCall.typeArguments)?.let { target ->
                        return collectBuiltinArrayConstructorCandidates(
                            functionCall = functionCall,
                            name = classifier.name,
                            target = target,
                            resolutionMode = resolutionMode,
                        )
                    }

                CfirBuiltInTypeKind.CPOINTER ->
                    typeAliasPointerConstructorTarget(typeAliasSymbol, functionCall.typeArguments)?.let { target ->
                        return collectBuiltinPointerConstructorCandidates(
                            functionCall = functionCall,
                            name = classifier.name,
                            target = target,
                            resolutionMode = resolutionMode,
                        )
                    }

                CfirBuiltInTypeKind.CSTRING ->
                    return collectBuiltinCStringConstructorCandidates(
                        functionCall = functionCall,
                        name = classifier.name,
                        resolutionMode = resolutionMode,
                    )

                else -> Unit
            }
        }

        val actualClassifier = typeAliasSymbol?.fullyExpandedClass(session) ?: classifier
        val actualKind = actualClassifier.builtinKindOrNull()
        when (actualKind) {
            CfirBuiltInTypeKind.CPOINTER ->
                return collectBuiltinPointerConstructorCandidates(
                    functionCall = functionCall,
                    name = classifier.name,
                    target = BuiltinPointerConstructorTarget(),
                    resolutionMode = resolutionMode,
                )

            CfirBuiltInTypeKind.CSTRING ->
                return collectBuiltinCStringConstructorCandidates(
                    functionCall = functionCall,
                    name = classifier.name,
                    resolutionMode = resolutionMode,
                )

            else -> Unit
        }

        if (actualClassifier.isStdlibArrayClassifier()) {
            return collectBuiltinArrayConstructorCandidates(
                functionCall = functionCall,
                name = classifier.name,
                target = BuiltinArrayConstructorTarget.Array,
                resolutionMode = resolutionMode,
            )
        }
        return null
    }

    /** 返回 classifier 或 typealias 展开后的声明层 builtin kind。 */
    private fun CfirClassLikeSymbol<*>.builtinKindOrNull(): CfirBuiltInTypeKind? = when (this) {
        is CfirBuiltInTypeSymbol -> kind
        is CfirTypeAliasSymbol -> cfir.expandedBuiltInKind(session)
        else -> null
    }

    /** 判断 classifier 是否为指定的声明层 builtin。 */
    private fun CfirClassLikeSymbol<*>?.isBuiltin(kind: CfirBuiltInTypeKind): Boolean =
        this?.builtinKindOrNull() == kind

    /** 判断 classifier 或 typealias 展开结果是否为标准库 `Array`。 */
    private fun CfirClassLikeSymbol<*>?.isStdlibArrayClassifier(): Boolean {
        val actualClassifier = (this as? CfirTypeAliasSymbol)?.fullyExpandedClass(session) ?: this
        return actualClassifier?.classId == StdlibClassIds.Array
    }

    /** 将展开到 `VArray` 的 typealias 调用转换为内建数组构造目标。 */
    private fun typeAliasVArrayConstructorTarget(
        symbol: CfirTypeAliasSymbol,
        typeArgumentRefs: List<CfirTypeRef>,
    ): BuiltinArrayConstructorTarget.VArray? {
        val alias = symbol.cfir
        val appliedArguments = typeArgumentRefs.mapNotNull { it.coneTypeOrNull }
        val aliasType = ConeTypeAliasType(
            classId = symbol.classId,
            // 即使没有显式类型实参，也必须保留 alias RHS，才能恢复其真实 builtin 形状。
            expandedType = alias.expandedTypeRef.coneTypeOrNull,
            typeArguments = appliedArguments,
        )
        val expandedType = aliasType.fullyExpandedType(session) as? ConeVArrayType ?: return null
        return BuiltinArrayConstructorTarget.VArray(
            sizeLiteral = "$${expandedType.size}",
            elementType = expandedType.elementType,
            typeParameters = alias.typeParameters.map { typeParameter ->
                BuiltinConstructorTypeParameter(
                    name = typeParameter.name,
                    originalSymbol = typeParameter.symbol,
                )
            },
        )
    }

    /** 将展开到 `CPointer` 的 typealias 调用转换为内建 pointer 构造目标。 */
    private fun typeAliasPointerConstructorTarget(
        symbol: CfirTypeAliasSymbol,
        typeArgumentRefs: List<CfirTypeRef>,
    ): BuiltinPointerConstructorTarget? {
        val alias = symbol.cfir
        val appliedArguments = typeArgumentRefs.mapNotNull { it.coneTypeOrNull }
        val aliasType = ConeTypeAliasType(
            classId = symbol.classId,
            // 无显式类型实参时仍必须携带 alias RHS，才能把 C() 展开为 CPointer<T>，
            // 再由合成候选的 fresh type parameter 接收 expected type 约束。
            expandedType = alias.expandedTypeRef.coneTypeOrNull,
            typeArguments = appliedArguments,
        )
        val expandedType = aliasType.fullyExpandedType(session) as? ConePointerType ?: return null
        return BuiltinPointerConstructorTarget(
            pointeeType = expandedType.pointeeType,
            typeParameters = alias.typeParameters.map { typeParameter ->
                BuiltinConstructorTypeParameter(
                    name = typeParameter.name,
                    originalSymbol = typeParameter.symbol,
                )
            },
        )
    }

    /** 为 Array/VArray 构造候选。 */
    private fun collectBuiltinArrayConstructorCandidates(
        functionCall: CfirFunctionCall,
        name: Name,
        target: BuiltinArrayConstructorTarget,
        resolutionMode: ResolutionMode,
    ): CfirBuiltInCallResolution {
        val callInfo = createBuiltinArrayConstructorCallInfo(functionCall, name, target, resolutionMode)
        val candidateFactory = CandidateFactory(transformer.resolutionContext, callInfo)
        val candidates = builtinArrayConstructorKinds(
            functionCall = functionCall,
            target = target,
            argumentCount = callInfo.arguments.size,
        ).map { kind ->
            candidateFactory.createBuiltinArrayConstructorCandidate(
                callInfo = callInfo,
                kind = kind,
                target = target,
            )
        }
        return reduceBuiltinCandidates(callInfo, candidates)
    }

    /** 为 CPointer 构造候选。 */
    private fun collectBuiltinPointerConstructorCandidates(
        functionCall: CfirFunctionCall,
        name: Name,
        target: BuiltinPointerConstructorTarget,
        resolutionMode: ResolutionMode,
    ): CfirBuiltInCallResolution {
        val callInfo = createBuiltinConstructorCallInfo(functionCall, name, resolutionMode)
        val kind = if (callInfo.arguments.isEmpty()) {
            BuiltinPointerConstructorKind.EMPTY
        } else {
            BuiltinPointerConstructorKind.CONVERT_POINTER
        }
        val candidate = CandidateFactory(transformer.resolutionContext, callInfo)
            .createBuiltinPointerConstructorCandidate(callInfo, kind, target)
        return reduceBuiltinCandidates(callInfo, listOf(candidate))
    }

    /** 为 CString 构造候选。 */
    private fun collectBuiltinCStringConstructorCandidates(
        functionCall: CfirFunctionCall,
        name: Name,
        resolutionMode: ResolutionMode,
    ): CfirBuiltInCallResolution {
        val callInfo = createBuiltinConstructorCallInfo(functionCall, name, resolutionMode)
        val candidate = CandidateFactory(transformer.resolutionContext, callInfo)
            .createBuiltinCStringConstructorCandidate(callInfo)
        return reduceBuiltinCandidates(callInfo, listOf(candidate))
    }

    /** 对合成 builtin 候选执行与普通候选相同的 stage 与最具体规约。 */
    private fun reduceBuiltinCandidates(
        callInfo: CallInfo,
        candidates: Collection<Candidate>,
    ): CfirBuiltInCallResolution {
        val (reducedCandidates, applicability) = reduceCollectedCandidates(
            candidates = candidates,
            collectorApplicability = CandidateApplicability.HIDDEN,
            isCandidateSuccessful = Candidate::isSuccessful,
            candidateApplicability = Candidate::lowestApplicability,
            fullyProcessCandidate = { candidate ->
                components.resolutionStageRunner.fullyProcessCandidate(candidate, transformer.resolutionContext)
            },
            chooseMostSpecific = { currentCandidates ->
                if (transformer.resolutionContext.candidateProcessingMode == CandidateProcessingMode.ARGUMENT_SHAPE) {
                    return@reduceCollectedCandidates currentCandidates
                }
                currentCandidates.singleOrNull()?.let(::setOf)
                    ?: conflictResolver.chooseMaximallySpecificCandidates(currentCandidates)
            },
        )
        return CfirBuiltInCallResolution(
            info = callInfo,
            applicability = applicability,
            candidates = reducedCandidates,
        )
    }

    /** 根据 builtin 数组目标和实参数量选择唯一的候选形状集合。 */
    private fun builtinArrayConstructorKinds(
        functionCall: CfirFunctionCall,
        target: BuiltinArrayConstructorTarget,
        argumentCount: Int,
    ): List<BuiltinArrayConstructorKind> {
        if (argumentCount > BUILTIN_ARRAY_CONSTRUCTOR_MAX_ARITY) {
            return listOf(BuiltinArrayConstructorKind.INIT_FUNCTION)
        }
        return when (target) {
            BuiltinArrayConstructorTarget.Array -> when (argumentCount) {
                0 -> listOf(BuiltinArrayConstructorKind.EMPTY)
                1 -> if (functionCall.hasTrailingLambda) {
                    listOf(BuiltinArrayConstructorKind.INIT_FUNCTION)
                } else {
                    listOf(BuiltinArrayConstructorKind.COLLECTION)
                }

                else -> if (functionCall.argumentList.arguments.getOrNull(1)?.hasExplicitArgumentName() == true) {
                    listOf(BuiltinArrayConstructorKind.REPEAT_ELEMENT)
                } else {
                    listOf(BuiltinArrayConstructorKind.INIT_FUNCTION)
                }
            }

            is BuiltinArrayConstructorTarget.VArray -> when (argumentCount) {
                0 -> listOf(BuiltinArrayConstructorKind.EMPTY)
                1 -> if (functionCall.argumentList.arguments.singleOrNull()?.hasExplicitArgumentName() == true) {
                    listOf(BuiltinArrayConstructorKind.REPEAT_ELEMENT)
                } else {
                    listOf(BuiltinArrayConstructorKind.INIT_FUNCTION)
                }

                else -> listOf(BuiltinArrayConstructorKind.REPEAT_ELEMENT)
            }
        }
    }

    /** 判断表达式对应的源码实参是否显式写了参数名。 */
    private fun CfirExpression.hasExplicitArgumentName(): Boolean {
        if (this is CfirNamedArgumentExpression) return true
        val source = when (this) {
            is CfirBlock -> source?.takeIf { statements.size == 1 }
            else -> source
        } ?: return false

        val psiArgument = source.psi as? CjValueArgument
        if (psiArgument != null) return psiArgument.getArgumentName() != null

        val rawText = source.text?.toString()?.trim().orEmpty()
        val separatorIndex = rawText.indexOf(':')
        return separatorIndex > 0 && Name.identifierIfValid(rawText.substring(0, separatorIndex).trim()) != null
    }

    /** 创建 Array/VArray builtin 调用信息，并剔除直写 VArray 的尺寸类型实参。 */
    private fun createBuiltinArrayConstructorCallInfo(
        functionCall: CfirFunctionCall,
        name: Name,
        target: BuiltinArrayConstructorTarget,
        resolutionMode: ResolutionMode,
    ): CallInfo = createBuiltinCallInfo(
        functionCall = functionCall,
        name = name,
        resolutionMode = resolutionMode,
        typeArguments = functionCall.builtinArrayConstructorTypeArguments(target),
    )

    /** 创建 CPointer/CString builtin 调用信息。 */
    private fun createBuiltinConstructorCallInfo(
        functionCall: CfirFunctionCall,
        name: Name,
        resolutionMode: ResolutionMode,
    ): CallInfo = createBuiltinCallInfo(
        functionCall = functionCall,
        name = name,
        resolutionMode = resolutionMode,
        typeArguments = functionCall.typeArguments,
    )

    /** 创建所有 builtin 构造调用共享的调用上下文。 */
    private fun createBuiltinCallInfo(
        functionCall: CfirFunctionCall,
        name: Name,
        resolutionMode: ResolutionMode,
        typeArguments: List<CfirTypeRef>,
    ): CallInfo = CallInfo(
        callSite = functionCall,
        callKind = CallKind.Function,
        name = name,
        origin = functionCall.origin,
        explicitReceiver = functionCall.explicitReceiver,
        arguments = functionCall.argumentList.arguments,
        isUsedAsGetClassReceiver = false,
        typeArguments = typeArguments,
        session = session,
        containingFile = components.file,
        containingDeclarations = transformer.components.containingDeclarations,
        resolutionMode = resolutionMode,
    )

    /** 直写 VArray 的尺寸参数不是合成构造器的类型参数。 */
    private fun CfirFunctionCall.builtinArrayConstructorTypeArguments(
        target: BuiltinArrayConstructorTarget,
    ): List<CfirTypeRef> = if (
        target is BuiltinArrayConstructorTarget.VArray && target.elementType == null
    ) {
        typeArguments.take(target.typeParameters.size)
    } else {
        typeArguments
    }

    /** builtin 数组构造形状中最大的有限实参数量。 */
    private companion object {
        const val BUILTIN_ARRAY_CONSTRUCTOR_MAX_ARITY = 2
    }
}
