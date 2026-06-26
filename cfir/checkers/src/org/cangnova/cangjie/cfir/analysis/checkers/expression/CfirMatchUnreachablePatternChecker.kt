package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.expandedPatternEnumType
import org.cangnova.cangjie.cfir.declarations.substitutedPayloadParameterTypes
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.match.CfirMatrix
import org.cangnova.cangjie.cfir.resolve.match.CfirConstantValue
import org.cangnova.cangjie.cfir.resolve.match.CfirConstructor
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern
import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPatternKind
import org.cangnova.cangjie.cfir.resolve.match.calculateMatrix
import org.cangnova.cangjie.cfir.resolve.match.isMatchSubtypeOf
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria.MarangetChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria.Usefulness
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.expandedClassIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.optionElementType
import org.cangnova.cangjie.name.ClassId

/**
 * match 分支可达性检查器。
 *
 * 对齐 C++ DiagKind::sema_unreachable_pattern:
 * 如果某一分支的模式被之前无 guard 分支完全覆盖，则该分支不可达。
 * 覆盖关系复用共享 Maranget usefulness 模型，避免 wildcard、enum、tuple、type pattern
 * 在可达性和穷尽性两套算法中产生不同语义。
 */
object CfirMatchUnreachablePatternChecker : CfirMatchExpressionChecker() {
    /**
     * 检查 selector-based match 分支是否被前序无 guard 分支完全覆盖。
     *
     * pattern 合法性已经失败时跳过，避免在错误 pattern 上继续运行 usefulness 算法。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val subjectType = expression.subject?.coneTypeOrNull ?: return
        if (subjectType is ConeErrorType) return
        if (expression.hasPatternLegalityProblem(context)) return

        val matchContext = MatchExhaustivenessContext.fromSession(context.session)
        val knownConstructor = expression.subject?.knownEnumConstructorOrNull(subjectType, context)
        val knownSubjectRows = expression.subject?.knownSubjectRowsOrNull(subjectType, matchContext)
        val previousRows = mutableListOf<List<CfirMatchPattern>>()

        for (branch in expression.branches) {
            val branchRows = runCatching {
                branch.pattern.calculateMatrix(subjectType, context.session)
            }.getOrElse { emptyList() }

            if (branchRows.isNotEmpty() && branchRows.all { row ->
                    row.isUnreachable(previousRows, matchContext, knownConstructor, knownSubjectRows)
                }) {
                reporter.reportOn(
                    source = branch.pattern.source ?: branch.source,
                    factory = CfirErrors.UNREACHABLE_PATTERN,
                )
            }

            if (branch.guard == null) {
                previousRows += branchRows
            }
        }
    }

    /**
     * 判断一行 pattern 是否不可达。
     *
     * 若 subject 已知为某个 enum 构造器且当前行不可能匹配它，直接不可达；否则委托覆盖算法判断。
     */
    private fun List<CfirMatchPattern>.isUnreachable(
        previousRows: CfirMatrix,
        context: MatchExhaustivenessContext,
        knownConstructor: KnownEnumConstructor?,
        knownSubjectRows: CfirMatrix?,
    ): Boolean {
        if (knownConstructor != null && none { it.mayMatch(knownConstructor) }) return true
        val uncoveredKnownRows = knownSubjectRows?.filter { knownRow ->
            !knownRow.isCoveredBy(previousRows, context)
        }
        if (uncoveredKnownRows != null) {
            if (uncoveredKnownRows.isEmpty()) return true
            if (uncoveredKnownRows.none { knownRow -> knownRow.isCoveredBy(listOf(this), context) }) return true
        }
        return isCoveredBy(previousRows, context)
    }

    /**
     * 判断单个 pattern 是否可能匹配已知 enum 构造器。
     */
    private fun CfirMatchPattern.mayMatch(knownConstructor: KnownEnumConstructor): Boolean =
        when (val patternKind = kind) {
            is CfirMatchPatternKind.Enum ->
                patternKind.enumClassId == knownConstructor.enumClassId &&
                    patternKind.entryName == knownConstructor.entryName

            CfirMatchPatternKind.Error,
            CfirMatchPatternKind.Wild,
            is CfirMatchPatternKind.Binding,
            is CfirMatchPatternKind.Const,
            is CfirMatchPatternKind.Tuple,
            is CfirMatchPatternKind.Type,
            -> true
        }

    /**
     * 尝试从 match subject 的静态形态推导已知 enum 构造器。
     *
     * 直接构造器访问和由局部变量初始化转发的构造器访问都会被识别。
     */
    private fun CfirExpression.knownEnumConstructorOrNull(
        subjectType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        context: CheckerContext,
    ): KnownEnumConstructor? {
        val enumType = subjectType.expandedPatternEnumType(context.session) ?: return null
        val entryName = knownEnumConstructorEntryNameOrNull() ?: return null
        return KnownEnumConstructor(enumType.classId, entryName)
    }

    /**
     * 提取表达式已知 enum 构造器的 entry 名称。
     */
    private fun CfirExpression.knownEnumConstructorEntryNameOrNull(): String? {
        unwrapSingleExpressionBlock().takeIf { it !== this }?.knownEnumConstructorEntryNameOrNull()?.let { return it }
        directEnumConstructorEntryNameOrNull()?.let { return it }

        val variable = (this as? CfirQualifiedAccessExpression)
            ?.takeIf { it.explicitReceiver == null && it.dispatchReceiver == null }
            ?.resolvedVariableOrNull()
            ?: return null

        return variable.initializer?.unwrapSingleExpressionBlock()?.directEnumConstructorEntryNameOrNull()
    }

    /**
     * 从直接 enum constructor 调用或命名访问中提取 entry 名称。
     */
    private fun CfirExpression.directEnumConstructorEntryNameOrNull(): String? {
        val access = when (this) {
            is CfirFunctionCall -> this
            is CfirNamedAccessExpression -> this
            is CfirQualifiedAccessExpression -> this
            else -> return null
        }
        val enumConstructor = access.calleeReference.resolvedSymbolOrNull()
            ?.takeIf { it.isBound }
            ?.cfir as? CfirEnumConstructor
            ?: return null
        return enumConstructor.name.asString()
    }

    /**
     * 去掉只包含单个表达式的 block 包裹。
     */
    private fun CfirExpression.unwrapSingleExpressionBlock(): CfirExpression =
        (this as? CfirBlock)?.statements?.singleOrNull() as? CfirExpression ?: this

    /**
     * 从 qualified access 中解析变量声明。
     */
    private fun CfirQualifiedAccessExpression.resolvedVariableOrNull(): CfirVariable? =
        (calleeReference.resolvedSymbolOrNull() as? CfirVariableSymbol<*>)
            ?.takeIf { it.isBound }
            ?.cfir

    /**
     * 从 match subject 的静态初始化形态构造“当前实际只可能是这些值”的模式行。
     *
     * 官方 unreachable-pattern 诊断来自 CHIR 的不可达分支分析；这里把 CFIR 中已经
     * 可见的局部初始化、enum 构造、tuple 字面量和 Option autobox 信息降到同一个
     * Maranget pattern 模型中，避免另起一套与穷尽性不同的覆盖语义。
     */
    private fun CfirExpression.knownSubjectRowsOrNull(
        subjectType: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): CfirMatrix? {
        val pattern = knownSubjectPatternOrNull(
            expectedType = subjectType,
            context = context,
            visitedVariables = mutableSetOf(),
            allowNarrowTypePattern = false,
        )
            ?: return null
        return listOf(listOf(pattern))
    }

    /**
     * 将表达式归约为单个已知 subject pattern。
     */
    private fun CfirExpression.knownSubjectPatternOrNull(
        expectedType: ConeCangJieType,
        context: MatchExhaustivenessContext,
        visitedVariables: MutableSet<CfirVariableSymbol<*>>,
        allowNarrowTypePattern: Boolean,
    ): CfirMatchPattern? {
        unwrapSingleExpressionBlock().takeIf { it !== this }
            ?.knownSubjectPatternOrNull(expectedType, context, visitedVariables, allowNarrowTypePattern)
            ?.let { return it }

        resolvedVariableOrNullForKnownSubject()
            ?.takeIf { visitedVariables.add(it.symbol) }
            ?.initializer
            ?.knownSubjectPatternOrNull(expectedType, context, visitedVariables, allowNarrowTypePattern)
            ?.let { return it }

        if (expectedType.isStdlibOptionType()) {
            knownStdlibOptionSomePatternOrNull(expectedType, context, visitedVariables)?.let { return it }
        }

        if (allowNarrowTypePattern) {
            knownConstructedTypePatternOrNull(expectedType, context)?.let { return it }
        }
        (this as? CfirTupleLiteral)?.knownTuplePatternOrNull(expectedType, context, visitedVariables)?.let { return it }
        knownEnumConstructorPatternOrNull(expectedType, context, visitedVariables)?.let { return it }
        knownLiteralPatternOrNull(expectedType)?.let { return it }
        return if (allowNarrowTypePattern) knownNarrowTypePatternOrNull(expectedType, context) else null
    }

    /**
     * 局部引用的 initializer 承接到当前 subject。
     */
    private fun CfirExpression.resolvedVariableOrNullForKnownSubject(): CfirVariable? =
        (this as? CfirQualifiedAccessExpression)
            ?.takeIf { it.explicitReceiver == null && it.dispatchReceiver == null }
            ?.resolvedVariableOrNull()

    /**
     * tuple 字面量保留每个分量的已知 pattern；未知分量退化为对应分量通配。
     */
    private fun CfirTupleLiteral.knownTuplePatternOrNull(
        expectedType: ConeCangJieType,
        context: MatchExhaustivenessContext,
        visitedVariables: MutableSet<CfirVariableSymbol<*>>,
    ): CfirMatchPattern? {
        val tupleType = expectedType as? ConeTupleType ?: coneTypeOrNull as? ConeTupleType ?: return null
        val subPatterns = elements.mapIndexed { index, element ->
            val elementType = tupleType.elementTypes.getOrNull(index) ?: return@mapIndexed CfirMatchPattern.wild()
            element.knownSubjectPatternOrNull(
                expectedType = elementType,
                context = context,
                visitedVariables = visitedVariables,
                allowNarrowTypePattern = true,
            )
                ?: CfirMatchPattern.wild(elementType)
        }
        return CfirMatchPattern(expectedType, CfirMatchPatternKind.Tuple(subPatterns))
    }

    /**
     * enum constructor 调用携带 constructor 名称和 payload 的已知 pattern。
     */
    private fun CfirExpression.knownEnumConstructorPatternOrNull(
        expectedType: ConeCangJieType,
        context: MatchExhaustivenessContext,
        visitedVariables: MutableSet<CfirVariableSymbol<*>>,
    ): CfirMatchPattern? {
        val enumType = expectedType.expandedPatternEnumType(context.session) ?: return null
        val call = this as? CfirFunctionCall ?: return null
        val enumConstructor = call.calleeReference.resolvedSymbolOrNull()
            ?.takeIf { it.isBound }
            ?.cfir as? CfirEnumConstructor
            ?: return null
        val entryName = enumConstructor.name.asString()
        val payloadTypes = (enumType as? ConeEnumType)?.let { type ->
            val constructor = CfirConstructor.Enum(
                enumClassId = type.classId,
                entryName = entryName,
                arityHint = call.argumentList.arguments.size,
                payloadTypes = enumConstructor.substitutedPayloadTypes(type, context),
            )
            constructor.subTypes(type)
        }.orEmpty()
        val subPatterns = call.argumentList.arguments.mapIndexed { index, argument ->
            val payloadType = payloadTypes.getOrNull(index)
                ?.takeUnless { it is ConeErrorType }
                ?: argument.coneTypeOrNull
            payloadType?.let {
                argument.knownSubjectPatternOrNull(
                    expectedType = it,
                    context = context,
                    visitedVariables = visitedVariables,
                    allowNarrowTypePattern = true,
                )
            }
                ?: CfirMatchPattern.wild(payloadType ?: ConeErrorType(org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic("enum payload")))
        }
        return CfirMatchPattern(
            expectedType,
            CfirMatchPatternKind.Enum(enumType.classId, entryName, subPatterns),
        )
    }

    /**
     * `Option<T> = valueOfT` 在 pattern 可达性中等价于已知 `Some(valueOfT)`。
     */
    private fun CfirExpression.knownStdlibOptionSomePatternOrNull(
        expectedType: ConeCangJieType,
        context: MatchExhaustivenessContext,
        visitedVariables: MutableSet<CfirVariableSymbol<*>>,
    ): CfirMatchPattern? {
        val payloadType = expectedType.optionElementType ?: return null
        val actualType = coneTypeOrNull ?: return null
        if (actualType.isStdlibOptionType()) return null
        if (!actualType.isSubtypeOf(payloadType, context)) return null
        val payloadPattern = knownSubjectPatternOrNull(
            expectedType = payloadType,
            context = context,
            visitedVariables = visitedVariables,
            allowNarrowTypePattern = true,
        )
            ?: CfirMatchPattern(payloadType, CfirMatchPatternKind.Type(actualType, null))
        return CfirMatchPattern(
            expectedType,
            CfirMatchPatternKind.Enum(StdlibClassIds.Option, "Some", listOf(payloadPattern)),
        )
    }

    /**
     * 普通构造调用的实际值域是被构造的 class-like 类型，而不是上下文期望类型。
     */
    private fun CfirExpression.knownConstructedTypePatternOrNull(
        expectedType: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): CfirMatchPattern? {
        val call = this as? CfirFunctionCall ?: return null
        val constructorSymbol = call.calleeReference.resolvedSymbolOrNull() as? CfirConstructorSymbol ?: return null
        val ownerSymbol = context.session.cfirProvider.getContainingClass(constructorSymbol) ?: return null
        val constructedType = listOfNotNull(
            call.coneTypeOrNull,
            constructorSymbol.resolvedReturnType,
            ownerSymbol.constructTypeForKnownConstructorOrNull(),
        ).firstOrNull { type ->
            type.expandedClassIdOrPrimitiveClassId == ownerSymbol.classId
        } ?: return null

        if (!constructedType.isSubtypeOf(expectedType, context)) return null
        return CfirMatchPattern(expectedType, CfirMatchPatternKind.Type(constructedType, null))
    }

    /**
     * 没有具体实参可用时，只为非泛型 owner 合成构造类型。
     */
    private fun org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol<*>.constructTypeForKnownConstructorOrNull(): ConeCangJieType? {
        val typeParameterOwner = cfir as? CfirTypeParameterRefsOwner
        if (!typeParameterOwner?.typeParameters.isNullOrEmpty()) return null
        return constructType()
    }

    /**
     * 字面量 subject 直接进入常量构造器。
     */
    private fun CfirExpression.knownLiteralPatternOrNull(expectedType: ConeCangJieType): CfirMatchPattern? {
        val literal = this as? CfirLiteralExpression ?: return null
        val constant = CfirConstantValue.fromLiteral(literal, expectedType) ?: return null
        return CfirMatchPattern(expectedType, CfirMatchPatternKind.Const(constant))
    }

    /**
     * 静态类型已经比 expected type 更窄时，用该类型约束 subject 的实际值域。
     */
    private fun CfirExpression.knownNarrowTypePatternOrNull(
        expectedType: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): CfirMatchPattern? {
        val actualType = coneTypeOrNull ?: return null
        if (actualType == expectedType) return null
        if (!actualType.isSubtypeOf(expectedType, context)) return null
        return CfirMatchPattern(expectedType, CfirMatchPatternKind.Type(actualType, null))
    }

    /**
     * 基于当前穷尽性上下文判断 this 是否为 [superType] 的子类型。
     */
    private fun ConeCangJieType.isSubtypeOf(
        superType: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): Boolean = isMatchSubtypeOf(superType, context.session)

    /**
     * 判断当前类型是否为标准库 Option 类型。
     */
    private fun ConeCangJieType.isStdlibOptionType(): Boolean =
        optionElementType != null

    /**
     * 读取 enum constructor 在当前 enum use-site 类型下的真实 payload 类型。
     */
    private fun CfirEnumConstructor.substitutedPayloadTypes(
        enumType: ConeEnumType,
        context: MatchExhaustivenessContext,
    ): List<ConeCangJieType> {
        val enumDeclaration = context.session.symbolProvider
            .getClassLikeSymbolByClassId(enumType.classId)
            ?.takeIf { it.isBound }
            ?.cfir as? CfirEnum
            ?: return emptyList()
        return substitutedPayloadParameterTypes(enumDeclaration, enumType)
    }

    /**
     * 从引用节点提取已解析或候选符号。
     */
    private fun org.cangnova.cangjie.cfir.references.CfirReference.resolvedSymbolOrNull(): CfirBasedSymbol<*>? =
        when (this) {
            is CfirResolvedNamedReference -> resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> candidateSymbol
            else -> null
        }

    /**
     * 已知 enum 构造器的最小表示。
     *
     * @property enumClassId enum 类的 ClassId。
     * @property entryName enum entry 的名称文本。
     */
    private data class KnownEnumConstructor(
        /** enum 类的 ClassId。 */
        val enumClassId: ClassId,
        /** enum entry 的名称文本。 */
        val entryName: String,
    )

    /**
     * 使用 Maranget usefulness 算法判断当前 pattern 行是否已被前序矩阵覆盖。
     */
    private fun List<CfirMatchPattern>.isCoveredBy(
        previousRows: CfirMatrix,
        context: MatchExhaustivenessContext,
    ): Boolean {
        return MarangetChecker.INSTANCE.isUseful(
            matrix = previousRows,
            patterns = this,
            withWitness = false,
            context = context,
            isTopLevel = true,
        ) is Usefulness.Useless
    }
}
