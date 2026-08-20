package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.accessContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.expandedPatternEnumType
import org.cangnova.cangjie.cfir.declarations.substitutedPayloadParameterTypes
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExhaustivenessStatus
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
import org.cangnova.cangjie.cfir.resolve.match.isNonExhaustiveEnum
import org.cangnova.cangjie.cfir.resolve.match.isMatchSubtypeOf
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.MatchExhaustivenessContext
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria.MarangetChecker
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.inria.Usefulness
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeAnyType
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

        val matchContext = MatchExhaustivenessContext.fromSession(
            session = context.session,
            accessContext = context.accessContext(CfirAccessKind.EXTEND),
        )
        /*
         * subject value-domain 来自官方 CHIR 常量分析阶段。Sema 已判定 match 非穷尽时，
         * 官方编译流程会在进入 CHIR 前终止；此时仍保留 Sema usefulness 的分支覆盖检查，
         * 但不能额外释放基于运行值域的 chir_unreachable_pattern。
         */
        val valueDomainAvailable =
            expression.exhaustiveness !is CfirMatchExhaustivenessStatus.NonExhaustive &&
                !subjectType.containsNonExhaustiveEnum(matchContext)
        val knownConstructor = if (!valueDomainAvailable) null else {
            /*
             * 官方 Sema reachability 只消费 selector 类型和已类型检查 pattern 矩阵。
             * 函数级 initializer enum-tag 属于后续 CHIR DCE 信息，不能在这里收窄
             * `match (local)` 的候选构造器，否则会把仍然合法的 Sema pattern 判成不可达。
             */
            expression.subject?.knownEnumConstructorOrNull(subjectType, context)
        }
        val knownSubjectRows = if (!valueDomainAvailable) null else {
            expression.subject?.knownSubjectRowsOrNull(subjectType, matchContext)
        }
        val previousRows = mutableListOf<List<CfirMatchPattern>>()
        var knownConstructorConsumed = false

        for (branch in expression.branches) {
            val branchRows = runCatching {
                branch.pattern.calculateMatrix(subjectType, context.session)
            }.getOrElse { emptyList() }

            val matchesKnownConstructor = knownConstructor != null && branchRows.any { row ->
                row.any { pattern -> pattern.mayMatch(knownConstructor) }
            }
            val unreachableByKnownTag = knownConstructor != null &&
                (knownConstructorConsumed || !matchesKnownConstructor)
            val unreachableByPatterns = branchRows.isNotEmpty() && branchRows.all { row ->
                row.isUnreachable(previousRows, matchContext, knownConstructor, knownSubjectRows)
            }
            if (unreachableByKnownTag || unreachableByPatterns) {
                reporter.reportOn(
                    source = branch.pattern.source ?: branch.source,
                    factory = CfirErrors.UNREACHABLE_PATTERN,
                )
            }

            if (!unreachableByKnownTag && !unreachableByPatterns && matchesKnownConstructor && branch.guard == null) {
                knownConstructorConsumed = true
            }

            // 官方 usefulness 只把有 witness 的可达分支加入覆盖矩阵；不可达 type pattern
            // 既不能消费已知 enum tag，也不能遮蔽后续 wildcard。
            if (branch.guard == null && !unreachableByKnownTag && !unreachableByPatterns) {
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
                    patternKind.entryName == knownConstructor.entryName &&
                    patternKind.subPatterns.size == knownConstructor.payloadArity

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
     * 只识别 subject 表达式自身的构造器访问。局部变量在当前位置的值域必须由
     * CFG/DFA 事实提供，checker 不能回溯声明 initializer 代替流分析。
     */
    private fun CfirExpression.knownEnumConstructorOrNull(
        subjectType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        context: CheckerContext,
    ): KnownEnumConstructor? {
        val enumType = subjectType.expandedPatternEnumType(context.session) ?: return null
        val enumConstructor = knownEnumConstructorDeclarationOrNull() ?: return null
        val enumDeclaration = context.session.symbolProvider
            .getClassLikeSymbolByClassId(enumType.classId)
            ?.takeIf { symbol -> symbol.isBound }
            ?.cfir as? CfirEnum
            ?: return null
        if (enumDeclaration.declarations
                .filterIsInstance<CfirEnumConstructor>()
                .none { constructor -> constructor.valueParameters.isNotEmpty() }
        ) return null
        return KnownEnumConstructor(
            enumClassId = enumType.classId,
            entryName = enumConstructor.name.asString(),
            payloadArity = enumConstructor.valueParameters.size,
        )
    }

    /** 把 DFA 记录的 enum constructor symbol 转为 usefulness 使用的最小 tag。 */
    private fun org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol.toKnownEnumConstructor(
        context: CheckerContext,
    ): KnownEnumConstructor? {
        val owner = context.session.cfirProvider.getContainingClass(this)?.cfir as? CfirEnum ?: return null
        val constructor = takeIf { isBound }?.cfir ?: return null
        return KnownEnumConstructor(
            enumClassId = owner.symbol.classId,
            entryName = name.asString(),
            payloadArity = constructor.valueParameters.size,
        )
    }

    /**
     * 提取表达式已知 enum 构造器声明。
     */
    private fun CfirExpression.knownEnumConstructorDeclarationOrNull(): CfirEnumConstructor? {
        unwrapSingleExpressionBlock().takeIf { it !== this }
            ?.knownEnumConstructorDeclarationOrNull()
            ?.let { return it }
        return directEnumConstructorDeclarationOrNull()
    }

    /**
     * 从直接 enum constructor 调用或命名访问中提取已解析声明。
     */
    private fun CfirExpression.directEnumConstructorDeclarationOrNull(): CfirEnumConstructor? {
        val access = when (this) {
            is CfirFunctionCall -> this
            is CfirNamedAccessExpression -> this
            is CfirQualifiedAccessExpression -> this
            else -> return null
        }
        return access.calleeReference.resolvedSymbolOrNull()
            ?.takeIf { it.isBound }
            ?.cfir as? CfirEnumConstructor
    }

    /**
     * 去掉只包含单个表达式的 block 包裹。
     */
    private fun CfirExpression.unwrapSingleExpressionBlock(): CfirExpression =
        (this as? CfirBlock)?.statements?.singleOrNull() as? CfirExpression ?: this

    /**
     * 从 match subject 表达式自身的静态形态构造“当前实际只可能是这些值”的模式行。
     *
     * 官方 unreachable-pattern 诊断来自 CHIR 的不可达分支分析；这里把 CFIR 中已经
     * 可见的 enum 构造、tuple 字面量和 Option autobox 信息降到同一个
     * Maranget pattern 模型中，避免另起一套与穷尽性不同的覆盖语义。
     */
    private fun CfirExpression.knownSubjectRowsOrNull(
        subjectType: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): CfirMatrix? {
        if (subjectType.isAnyType()) return null
        val pattern = knownSubjectPatternOrNull(
            expectedType = subjectType,
            context = context,
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
        allowNarrowTypePattern: Boolean,
    ): CfirMatchPattern? {
        unwrapSingleExpressionBlock().takeIf { it !== this }
            ?.knownSubjectPatternOrNull(expectedType, context, allowNarrowTypePattern)
            ?.let { return it }

        if (expectedType.isStdlibOptionType()) {
            knownStdlibOptionSomePatternOrNull(expectedType, context)?.let { return it }
        }

        if (allowNarrowTypePattern) {
            knownConstructedTypePatternOrNull(expectedType, context)?.let { return it }
        }
        (this as? CfirTupleLiteral)?.knownTuplePatternOrNull(expectedType, context)?.let { return it }
        knownEnumConstructorPatternOrNull(expectedType, context)?.let { return it }
        knownLiteralPatternOrNull(expectedType)?.let { return it }
        return if (allowNarrowTypePattern) knownNarrowTypePatternOrNull(expectedType, context) else null
    }

    /**
     * tuple 字面量保留每个分量的已知 pattern；未知分量退化为对应分量通配。
     */
    private fun CfirTupleLiteral.knownTuplePatternOrNull(
        expectedType: ConeCangJieType,
        context: MatchExhaustivenessContext,
    ): CfirMatchPattern? {
        val tupleType = expectedType as? ConeTupleType ?: coneTypeOrNull as? ConeTupleType ?: return null
        val subPatterns = elements.mapIndexed { index, element ->
            val elementType = tupleType.elementTypes.getOrNull(index) ?: return@mapIndexed CfirMatchPattern.wild()
            element.knownSubjectPatternOrNull(
                expectedType = elementType,
                context = context,
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
    ): CfirMatchPattern? {
        val payloadType = expectedType.optionElementType ?: return null
        val actualType = coneTypeOrNull ?: return null
        if (actualType.isStdlibOptionType()) return null
        if (!actualType.isSubtypeOf(payloadType, context)) return null
        val payloadPattern = knownSubjectPatternOrNull(
            expectedType = payloadType,
            context = context,
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
    ): Boolean = isMatchSubtypeOf(superType, context.session, context.accessContext)

    /**
     * 判断当前类型是否为标准库 Option 类型。
     */
    private fun ConeCangJieType.isStdlibOptionType(): Boolean =
        optionElementType != null

    /**
     * `Any` 的显式声明类型保留运行期类型测试空间，不能被初始化表达式的静态值域收窄。
     */
    private fun ConeCangJieType.isAnyType(): Boolean =
        this === ConeAnyType || expandedClassIdOrPrimitiveClassId == StdlibClassIds.Any

    /**
     * 非穷尽 enum 带有未知未来构造器，不能再用当前 subject 初始化值域收窄可达性。
     */
    private fun ConeCangJieType.containsNonExhaustiveEnum(context: MatchExhaustivenessContext): Boolean =
        when (this) {
            is ConeTupleType -> elementTypes.any { it.containsNonExhaustiveEnum(context) }
            else -> isNonExhaustiveEnum(context.session)
        }

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
     * @property payloadArity constructor payload 参数数量。
     */
    private data class KnownEnumConstructor(
        /** enum 类的 ClassId。 */
        val enumClassId: ClassId,
        /** enum entry 的名称文本。 */
        val entryName: String,
        /** constructor payload 参数数量。 */
        val payloadArity: Int,
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
