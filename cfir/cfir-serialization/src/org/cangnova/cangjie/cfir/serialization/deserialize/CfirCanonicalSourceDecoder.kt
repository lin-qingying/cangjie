package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.*
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.references.CfirReferenceParentType
import org.cangnova.cangjie.cfir.resolve.createParametersSubstitutor
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.serialization.cjo.wire
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import java.util.Collections
import java.util.IdentityHashMap

/** 声明/type binder 属于正式包 context；decoder 不按名字找声明、不重新读文件或推导类型。 */
internal interface CfirSourceHeaderDecodingContext {
    val pkg: PackageFormat.Package
    fun type(reference: UInt): ConeCangJieType
    fun declaration(reference: UInt): CfirDeclaration
    fun signature(function: CfirCallableDeclaration): CfirSourceCallableSignature
    fun header(function: CfirCallableDeclaration, body: FuncBody): CfirSourceImportedFunctionHeader
}

/** 引用阶段只读取 owning barrier 已完成的 d/F；header-only 入口不拥有正文读取能力。 */
internal interface CfirCanonicalSourceDecodingContext : CfirSourceHeaderDecodingContext {
    val publicationOwner: CfirSourceGraphPublicationOwner
    fun target(reference: FullId): CfirDeclaration
    fun referenceTarget(reference: FullId?): CfirSourceReferenceTarget
    fun savedReference(reference: FullId?): CfirSourceSavedReference
    fun function(identity: CjoSourceFunctionIdentity): CfirCallableDeclaration
    fun declarationContext(value: CjoSourceContext): CfirDeclaration?
    fun owningDeclaration(entry: CjoSourceOwnedDeclaration): CfirDeclaration?
    fun position(expression: Expr): CfirSourceImportedPosition
    fun position(pattern: Pattern): CfirSourceImportedPosition
    fun typeReference(reference: UInt): CfirTypeRef
    fun constants(pattern: Pattern, hostPosition: Int): List<CfirConstantValue>
    fun constant(info: VarInfo, type: UInt, hostPosition: Int): CfirConstantValue?
    fun expressionIdentity(reference: UInt): CfirSourceExpressionIdentity
    fun expressionOrigin(identity: CfirSourceExpressionIdentity): CfirExpression?
    fun expressionSource(reference: UInt): CfirSourceGraph?
    fun borrowedExpressionSource(reference: UInt): CfirSourceGraph
    fun matchCaseSelector(reference: UInt): UInt
    fun patternIdentity(position: Int): CfirSourcePatternIdentity
    fun patternRecord(position: Int, produce: () -> CfirSourcePattern): CfirSourcePattern
    fun patternSource(position: Int): CfirSourceGraph?
    fun registerCompletedRecords(graph: CfirSourceGraph, references: Map<UInt, CfirSourceExpressionIdentity>)
}

/** 拥有阶段完成的实际上下文；metadata祖先和内容集合独立，decoder不能再调度绑定。 */
internal class CfirBoundSourceContexts(
    val selection: CjoSourceContentSelection,
    declarations: Map<UInt, CfirDeclaration>,
    owners: Map<CfirDeclaration, CfirDeclaration?>,
    functions: Map<CjoSourceFunctionIdentity, CfirCallableDeclaration>,
    functionContexts: Map<CfirCallableDeclaration, CfirSourceFunctionContext>,
    headers: Map<CfirCallableDeclaration, CfirSourceImportedFunctionHeader>,
) {
    val declarations = Collections.unmodifiableMap(LinkedHashMap(declarations))
    val owners = Collections.unmodifiableMap(IdentityHashMap(owners))
    val functions = Collections.unmodifiableMap(LinkedHashMap(functions))
    val functionContexts = Collections.unmodifiableMap(IdentityHashMap(functionContexts))
    val headers = Collections.unmodifiableMap(IdentityHashMap(headers))

    init {
        check(this.declarations.keys == selection.borrowedContexts.declarations.keys)
        check(this.owners.keys == this.declarations.values.toSet())
        check(this.functions.keys == selection.borrowedContexts.functions.keys)
        check(this.functionContexts.keys == this.functions.values.toSet() && this.headers.keys == this.functionContexts.keys)
    }
}

/** 完整 native 正文与保存的 FuncBody header；SOURCE 可读不等于 CFIR DFA/CFG 已重建。 */
class CfirDeserializedSourceGraph internal constructor(
    val graph: CfirSourceGraph,
    headers: Map<CfirCallableDeclaration, CfirSourceImportedFunctionHeader>,
    references: Map<UInt, CfirSourceExpressionIdentity>,
    internal val selection: CjoSourceContentSelection,
) {
    val functionHeaders: Map<CfirCallableDeclaration, CfirSourceImportedFunctionHeader> =
        Collections.unmodifiableMap(IdentityHashMap(headers))
    internal val expressionReferences: Map<UInt, CfirSourceExpressionIdentity> = Collections.unmodifiableMap(HashMap(references))

    internal fun withGraph(value: CfirSourceGraph): CfirDeserializedSourceGraph =
        if (value === graph) this else CfirDeserializedSourceGraph(value, functionHeaders, expressionReferences, selection)

    internal fun withExpressionOrigins(expressions: Map<UInt, CfirExpression>): CfirDeserializedSourceGraph {
        val aliases = IdentityHashMap<org.cangnova.cangjie.cfir.CfirElement, CfirSourceExpressionIdentity>()
        expressionReferences.forEach { (reference, identity) -> expressions[reference]?.let { aliases[it] = identity } }
        val merged = graph.withOriginAliases(aliases)
        return if (merged === graph) this else CfirDeserializedSourceGraph(merged, functionHeaders, expressionReferences, selection)
    }
}

/**
 * 同一包 SOURCE30+3 的统一材料化事务。先 reserve 全部 e 身份，逐项填载荷；
 * d/t 由正式 context 的两阶段 binder 提供，Jump/map/Return 保持实际目标。
 */
internal class CfirCanonicalSourceDecoder(private val context: CfirCanonicalSourceDecodingContext) {
    private val builder = CfirSourceGraph.Builder(CfirSourceGraphProvenance.Published(context.publicationOwner))
    private val expressions = HashMap<UInt, CfirSourceExpressionIdentity>()
    private val borrowedExpressions = HashMap<UInt, CfirSourceExpressionIdentity>()
    private val declarations = HashSet<UInt>()
    private val headers = IdentityHashMap<CfirCallableDeclaration, CfirSourceImportedFunctionHeader>()
    private val functionBodies = HashMap<CjoSourceFunctionIdentity, CfirSourceFunctionBody>()
    private lateinit var owningPlan: CjoSourceOwnershipPlan
    private lateinit var selection: CjoSourceContentSelection

    fun decode(selection: CjoSourceContentSelection, bound: CfirBoundSourceContexts): CfirDeserializedSourceGraph {
        check(bound.selection === selection) { "Bound contexts must belong to this exact SOURCE request" }
        this.selection = selection
        val plan = selection.ownedContent
        check(plan.kind == CjoSourcePlanKind.CONTENT) { "Header-only registry does not authorize canonical content decoding" }
        owningPlan = plan
        // 全部 e 先从合法拥有闭包 reserve；map/Jump/FullId 从此以后只有 lookup，不能增加 work。
        plan.expressions.keys.forEach { reference ->
            val identity = builder.reserveGenerated(context.expressionIdentity(reference))
            expressions[reference] = identity
            // 已完成的普通节点与本次 canonical record 共用原 e 身份；发布前登记全部辅助角色。
            context.expressionOrigin(identity)?.let { builder.alias(it, identity) }
        }
        plan.expressions.values.forEach { entry ->
            if (entry.wire.mapExpr != 0u) borrowedExpressionReference(entry.wire.mapExpr)
            if (entry.wire.kind == ExprKind.MatchCase) {
                builder.matchCaseSelector(expressions.getValue(entry.reference),
                    borrowedExpressionReference(context.matchCaseSelector(entry.reference)))
            }
        }
        bound.owners.forEach { (declaration, owner) -> builder.declarationOwner(declaration, owner) }
        // 保存的const值属于metadata产品，不因此选择变量的initializer或任何F.body。
        bound.declarations.forEach { (reference, declaration) ->
            val wire = selection.borrowedContexts.declarations.getValue(reference).wire
            if (wire.kind == DeclKind.VarDecl && declaration is CfirVariable) {
                context.constant(wire.info(VarInfo()) as VarInfo, wire.type,
                    (wire.info(CjoSourceTable()) as CjoSourceTable).position())?.let { builder.constantValue(declaration, it) }
            }
        }
        bound.functionContexts.forEach { (function, value) -> builder.functionContext(function, value) }
        selection.borrowedContexts.functions.values.forEach { entry ->
            val function = bound.functions.getValue(entry.identity)
            functionBodies[entry.identity] = decodeFunction(function, entry.body, bound.headers.getValue(function), entry.identity in selection.selectedBodies)
        }
        plan.declarations.values.forEach { entry ->
            declaration(entry.reference)
        }
        (selection.selectedInitializers + selection.selectedDefaults + selection.selectedAnnotations + selection.selectedPatternHeaders).forEach(::declaration)
        plan.expressions.values.forEach { entry ->
            val expression = entry.wire
            val owner = checkNotNull(context.declarationContext(entry.scope.expressionContext))
            val completed = context.expressionSource(entry.reference)
            if (completed != null) {
                builder.reuseExpression(expressions.getValue(entry.reference), completed)
            } else {
                val payload = payload(entry, plan, owner)
                val map = expression.mapExpr.takeIf { it != 0u }?.let(::borrowedExpressionReference)
                val record = CfirSourceExpressionRecord(
                context = owner,
                type = context.type(expression.type),
                span = null,
                payload = payload,
                mapExpression = if (payload is CfirSourceExpressionPayload.Jump) null else map,
                overflowStrategy = when (expression.overflowPolicy) {
                    OverflowPolicy.NA -> CangjieOverflowStrategy.NA
                    OverflowPolicy.Throwing -> CangjieOverflowStrategy.THROWING
                    OverflowPolicy.Wrapping -> CangjieOverflowStrategy.WRAPPING
                    OverflowPolicy.Saturating -> CangjieOverflowStrategy.SATURATING
                    OverflowPolicy.Checked -> CangjieOverflowStrategy.CHECKED
                    else -> error("Unknown SOURCE overflow policy")
                },
                importedPosition = context.position(expression),
                )
                builder.fill(expressions.getValue(entry.reference), record)
            }
        }
        check(declarations == plan.declarations.keys + selection.selectedInitializers + selection.selectedDefaults + selection.selectedAnnotations + selection.selectedPatternHeaders &&
            expressions.keys == plan.expressions.keys && headers.keys == bound.headers.keys) {
            "SOURCE materialization did not complete its selected owning plan"
        }
        val graph = builder.finish()
        context.registerCompletedRecords(graph, expressions)
        return CfirDeserializedSourceGraph(graph, headers, expressions, selection)
    }

    private fun expressionReference(reference: UInt): CfirSourceExpressionIdentity =
        checkNotNull(expressions[reference]) { "SOURCE e $reference was not reserved by its owning root" }

    private fun borrowedExpressionReference(reference: UInt): CfirSourceExpressionIdentity =
        if (reference in expressions) expressions.getValue(reference) else borrowedExpressions.getOrPut(reference) {
            val source = context.borrowedExpressionSource(reference)
            context.expressionIdentity(reference).also { builder.borrowExpression(it, source) }
        }

    private fun optional(reference: UInt): CfirSourceExpressionIdentity? =
        reference.takeIf { it != 0u }?.let(::expressionReference)

    private fun declaration(reference: UInt): CfirDeclaration {
        require(reference != 0u && reference <= context.pkg.allDeclsLength.toUInt())
        val declaration = context.declaration(reference)
        if (!declarations.add(reference)) return declaration
        val wire = checkNotNull(context.pkg.allDecls(reference.toInt() - 1))
        when (wire.kind) {
            DeclKind.VarDecl -> if (declaration is CfirVariable) {
                val info = wire.info(VarInfo()) as VarInfo
                if (reference in selection.selectedInitializers) optional(info.initializer)?.let { builder.initializer(declaration, it) }
            }
            DeclKind.VarWithPatternDecl -> {
                val variable = declaration as CfirPatternVariable
                val info = wire.info(VarWithPatternInfo()) as VarWithPatternInfo
                val position = (wire.info(CjoSourceTable()) as CjoSourceTable).tablePosition(8)
                if (position in owningPlan.patterns) {
                    val completed = context.patternSource(position)
                    if (completed != null) builder.reuseDeclarationPattern(variable, completed)
                    else builder.declarationPattern(variable, pattern(position, variable))
                } else if (reference in selection.selectedPatternHeaders) {
                    builder.borrowDeclarationPattern(variable, checkNotNull(context.patternSource(position)) {
                        "A scoped P header requires its explicitly completed original owning pattern product"
                    })
                }
                if (reference in selection.selectedInitializers) optional(info.initializer)?.let { builder.initializer(variable, it) }
            }
            DeclKind.FuncParam -> {
                val parameter = declaration as CfirValueParameter
                when (wire.infoType) {
                    DeclInfo.NONE -> Unit
                    DeclInfo.ParamInfo -> if (reference in selection.selectedDefaults)
                        optional((wire.info(ParamInfo()) as ParamInfo).defaultVal)?.let { builder.default(parameter, it) }
                    else -> error("A function parameter has an invalid additional-info kind")
                }
            }
        }
        if (reference in selection.selectedAnnotations) builder.annotations(declaration, (0 until wire.annotationsLength).map { ordinal ->
            val annotation = checkNotNull(wire.annotations(ordinal))
            CfirSourceAnnotation(annotationKind(annotation.kind), checkNotNull(annotation.identifier),
                (0 until annotation.argsLength).map { index ->
                    val argument = checkNotNull(annotation.args(index))
                    CfirSourceAnnotationArgument(argument.name?.let(Name::identifier), expressionReference(argument.expr))
                }, if (annotation.kind == AnnoKind.Custom) optionalDeclarationTarget(annotation.target) else null,
                    annotation.target?.takeIf { annotation.kind == AnnoKind.Custom && classifySourceReferenceTarget(it) == CjoSourceReferenceTargetKind.DECLARATION }?.let(::referenceKind)
                        ?: CfirSourceReferenceKind.SOURCE)
        })
        return declaration
    }

    /** resources/future 是 SOURCE 拥有声明；不能仅解析其 FullId 后遗失 initializer/body。 */
    private fun ownedTarget(reference: FullId): CfirDeclaration {
        check(reference.pkgId == org.cangnova.cangjie.cfir.serialization.cjo.PackageIndex.CURRENT.value)
        return declaration(reference.index)
    }

    private fun decodeFunction(function: CfirCallableDeclaration, body: FuncBody, header: CfirSourceImportedFunctionHeader,
        bodySelected: Boolean): CfirSourceFunctionBody {
        val previous = headers.put(function, header)
        check(previous == null || previous == header)
        if (previous == null) builder.functionHeader(function, header)
        val identity = if (bodySelected) body.body.takeIf { it != 0u }?.let(::expressionReference) else null
        if (previous == null && header.bodyAvailable && identity == null) builder.unloadedBody(function)
        if (previous == null && identity != null) builder.body(function, identity)
        return CfirSourceFunctionBody(
            function, header.parameterLists.flatten(), header.signature, identity,
            body.captureKind.toInt(), body.always, header,
        )
    }

    private fun payload(entry: CjoSourceOwnedExpression, plan: CjoSourceOwnershipPlan, owner: CfirDeclaration): CfirSourceExpressionPayload {
        val expr = entry.wire
        fun arity(size: Int) { check(expr.operandsLength == size) { "SOURCE kind ${expr.kind} has wrong operand count" } }
        fun operand(index: Int): CfirSourceExpressionIdentity = expressionReference(expr.operands(index))
        fun operands(start: Int = 0): List<CfirSourceExpressionIdentity> =
            (start until expr.operandsLength).map(::operand)
        fun info(tag: UByte) { check(expr.infoType == tag) { "SOURCE expression kind/info disagree" } }
        fun none() = info(ExprInfo.NONE)
        return when (expr.kind) {
            ExprKind.WildcardExpr -> { none(); arity(0); CfirSourceExpressionPayload.Wildcard }
            ExprKind.PrimitiveTypeExpr -> { none(); arity(0); CfirSourceExpressionPayload.PrimitiveType }
            ExprKind.ReturnExpr -> {
                none(); arity(1)
                CfirSourceExpressionPayload.Return(operand(0), context.function(checkNotNull(entry.scope.returnFunction)))
            }
            ExprKind.JumpExpr -> {
                info(ExprInfo.JumpInfo); arity(0)
                CfirSourceExpressionPayload.Jump((expr.info(JumpInfo()) as JumpInfo).isBreak,
                    borrowedExpressionReference(expr.mapExpr))
            }
            ExprKind.RefExpr -> { info(ExprInfo.ReferenceInfo); arity(0); reference(expr, owner) }
            ExprKind.MemberAccess -> {
                info(ExprInfo.ReferenceInfo); arity(1)
                CfirSourceExpressionPayload.Member(operand(0), reference(expr, owner))
            }
            ExprKind.CallExpr -> {
                info(ExprInfo.CallInfo); check(expr.operandsLength >= 1)
                val value = expr.info(CallInfo()) as CallInfo
                CfirSourceExpressionPayload.Call(operand(0), operands(1), callKind(value.callKind), value.hasSideEffect)
            }
            ExprKind.UnaryExpr -> {
                info(ExprInfo.UnaryInfo); arity(1)
                CfirSourceExpressionPayload.Unary(operand(0), operator((expr.info(UnaryInfo()) as UnaryInfo).op))
            }
            ExprKind.IncOrDecExpr -> {
                info(ExprInfo.IncOrDecInfo); arity(1)
                CfirSourceExpressionPayload.Increment(operand(0), operator((expr.info(IncOrDecInfo()) as IncOrDecInfo).op))
            }
            ExprKind.LitConstExpr -> {
                info(ExprInfo.LitConstInfo); arity(0)
                val value = expr.info(LitConstInfo()) as LitConstInfo
                CfirSourceExpressionPayload.Literal(literalKind(value.constKind), checkNotNull(value.strValue),
                    when (value.strKind) {
                        StringKind.Normal -> CfirSourceStringKind.NORMAL
                        StringKind.JString -> CfirSourceStringKind.JSTRING
                        StringKind.MultiLine -> CfirSourceStringKind.MULTILINE
                        StringKind.MultiLineRaw -> CfirSourceStringKind.MULTILINE_RAW
                        else -> error("Unknown SOURCE string kind")
                    })
            }
            ExprKind.BinaryExpr -> {
                info(ExprInfo.BinaryInfo); arity(2)
                CfirSourceExpressionPayload.Binary(operand(0), operand(1), operator((expr.info(BinaryInfo()) as BinaryInfo).op))
            }
            ExprKind.SubscriptExpr -> {
                info(ExprInfo.SubscriptInfo); check(expr.operandsLength >= 2)
                CfirSourceExpressionPayload.Subscript(operand(0), operands(1), (expr.info(SubscriptInfo()) as SubscriptInfo).isTupleAccess)
            }
            ExprKind.AssignExpr -> {
                info(ExprInfo.AssignInfo); arity(2)
                val value = expr.info(AssignInfo()) as AssignInfo
                CfirSourceExpressionPayload.Assignment(operand(0), operand(1), operator(value.op), value.isCompound)
            }
            ExprKind.ArrayExpr, ExprKind.ArrayLit -> {
                info(ExprInfo.ArrayInfo)
                val value = expr.info(ArrayInfo()) as ArrayInfo
                val isValueArrayType = context.type(expr.type) is ConeVArrayType
                val target = if (isValueArrayType) null else value.initFunc
                val initializer = optionalDeclarationTarget(target)
                if (initializer != null) check(initializer is CfirFunction) { "A native array factory must retain its actual FuncDecl" }
                if (expr.kind == ExprKind.ArrayLit && !isValueArrayType) check(initializer is CfirFunction) { "A native ArrayLit requires its actual factory FuncDecl" }
                val kind = if (initializer != null) referenceKind(checkNotNull(target)) else CfirSourceReferenceKind.SOURCE
                val saved = context.savedReference(value.initFunc)
                if (expr.kind == ExprKind.ArrayLit) CfirSourceExpressionPayload.ArrayLiteral(operands(), initializer, kind, saved, value.isValueArray)
                else CfirSourceExpressionPayload.Array(operands(), initializer, value.isValueArray, kind, saved)
            }
            ExprKind.PointerExpr -> {
                none(); check(expr.operandsLength <= 1)
                CfirSourceExpressionPayload.Pointer(if (expr.operandsLength == 1) operand(0) else null)
            }
            ExprKind.TypeConvExpr -> { none(); arity(1); CfirSourceExpressionPayload.TypeConversion(operand(0)) }
            ExprKind.ThrowExpr -> { none(); arity(1); CfirSourceExpressionPayload.Throw(operand(0)) }
            ExprKind.PerformExpr -> { none(); arity(1); CfirSourceExpressionPayload.Perform(operand(0)) }
            ExprKind.SpawnExpr -> {
                info(ExprInfo.SpawnInfo); check(expr.operandsLength <= 1)
                val future = checkNotNull((expr.info(SpawnInfo()) as SpawnInfo).future)
                CfirSourceExpressionPayload.Spawn(if (expr.operandsLength == 1) operand(0) else null,
                    ownedTarget(future))
            }
            ExprKind.TupleLit -> { none(); CfirSourceExpressionPayload.Tuple(operands()) }
            ExprKind.IfExpr -> {
                none(); arity(3)
                CfirSourceExpressionPayload.Conditional(operand(0), operand(1), optional(expr.operands(2)))
            }
            ExprKind.TryExpr -> {
                info(ExprInfo.TryInfo); check(expr.operandsLength >= 2)
                val value = expr.info(TryInfo()) as TryInfo
                val catches = (1 until expr.operandsLength - 1).map(::operand)
                check(catches.size == value.patternsLength)
                CfirSourceExpressionPayload.Try(operand(0), catches,
                    (0 until value.patternsLength).map { pattern((expr.info(CjoSourceTable()) as CjoSourceTable).tablePosition(6, it), owner) },
                    optional(expr.operands(expr.operandsLength - 1)),
                    (0 until value.resourcesLength).map { ownedTarget(checkNotNull(value.resources(it))) })
            }
            ExprKind.WhileExpr, ExprKind.DoWhileExpr -> {
                none(); arity(2)
                val isDoWhile = expr.kind == ExprKind.DoWhileExpr
                CfirSourceExpressionPayload.Loop(operand(if (isDoWhile) 1 else 0), operand(if (isDoWhile) 0 else 1), isDoWhile)
            }
            ExprKind.LambdaExpr -> {
                info(ExprInfo.LambdaInfo); arity(0)
                val value = expr.info(LambdaInfo()) as LambdaInfo
                val identity = plan.functionByLambda.getValue(entry.reference)
                CfirSourceExpressionPayload.Lambda(functionBodies.getValue(identity), value.supportMock)
            }
            ExprKind.Block -> {
                info(ExprInfo.BlockInfo)
                val value = expr.info(BlockInfo()) as BlockInfo
                check(value.isExprLength == expr.operandsLength + 1)
                CfirSourceExpressionPayload.Block((0 until expr.operandsLength).map {
                    if (value.isExpr(it)) CfirSourceSlot.Expression(operand(it))
                    else CfirSourceSlot.Declaration(declaration(expr.operands(it)))
                }, value.isExpr(expr.operandsLength))
            }
            ExprKind.MatchExpr -> {
                info(ExprInfo.MatchInfo)
                val value = expr.info(MatchInfo()) as MatchInfo
                check(expr.operandsLength > 0)
                CfirSourceExpressionPayload.Match(if (value.matchMode) operand(0) else null,
                    operands(if (value.matchMode) 1 else 0))
            }
            ExprKind.LetPatternDestructor -> {
                info(ExprInfo.LetPatternDestructorInfo); arity(1)
                val value = expr.info(LetPatternDestructorInfo()) as LetPatternDestructorInfo
                CfirSourceExpressionPayload.LetPatternDestructor(operand(0),
                    (0 until value.patternsLength).map { pattern((expr.info(CjoSourceTable()) as CjoSourceTable).tablePosition(4, it), owner) })
            }
            ExprKind.ForInExpr -> {
                info(ExprInfo.ForInInfo); arity(3)
                val value = expr.info(ForInInfo()) as ForInInfo
                CfirSourceExpressionPayload.ForIn(operand(0), optional(expr.operands(1)), operand(2),
                    pattern((expr.info(CjoSourceTable()) as CjoSourceTable).tablePosition(4), owner), when (value.forInKind) {
                        ForInKind.NA -> CfirSourceForInKind.DIRECT
                        ForInKind.Range -> CfirSourceForInKind.RANGE
                        ForInKind.String -> CfirSourceForInKind.STRING
                        ForInKind.Iterator -> CfirSourceForInKind.ITERATOR
                        else -> error("Unknown SOURCE for-in kind")
                    })
            }
            ExprKind.MatchCase -> {
                info(ExprInfo.MatchCaseInfo); arity(2)
                val value = expr.info(MatchCaseInfo()) as MatchCaseInfo
                CfirSourceExpressionPayload.MatchCase(
                    (0 until value.patternsLength).map { pattern((expr.info(CjoSourceTable()) as CjoSourceTable).tablePosition(4, it), owner) },
                    optional(expr.operands(0)), operand(1))
            }
            ExprKind.MatchCaseOther -> { none(); arity(2); CfirSourceExpressionPayload.MatchCaseOther(operand(0), operand(1)) }
            ExprKind.InvalidExpr -> {
                info(ExprInfo.FuncArgInfo)
                val value = expr.info(FuncArgInfo()) as FuncArgInfo
                arity(if (value.isDefaultVal) 0 else 1)
                CfirSourceExpressionPayload.Argument(if (value.isDefaultVal) null else operand(0),
                    value.isDefaultVal, value.withInout)
            }
            else -> error("No completed SOURCE role for expression kind ${expr.kind}")
        }
    }

    /** 可选声明槽保留合法无目标；package 不可被当作函数或注解声明。 */
    private fun optionalDeclarationTarget(reference: FullId?): CfirDeclaration? = when (val target = context.referenceTarget(reference)) {
        is CfirSourceReferenceTarget.Declaration -> target.declaration
        is CfirSourceReferenceTarget.Absent -> null
        is CfirSourceReferenceTarget.Package -> error("Declaration target slot cannot refer to a package")
    }

    private fun reference(expr: Expr, owner: CfirDeclaration): CfirSourceExpressionPayload.Reference {
        val value = expr.info(ReferenceInfo()) as ReferenceInfo
        val spelling = checkNotNull(value.reference)
        val name = OperatorNameConventions.TOKENS_BY_OPERATOR_NAME.entries.firstOrNull { it.value == spelling }?.key
            ?: Name.identifier(spelling)
        val parent = if (value.matchedParentTy == 0u) CfirReferenceParentType.Absent
            else if (checkNotNull(context.pkg.allTypes(value.matchedParentTy.toInt() - 1)).kind == TypeKind.Invalid)
                CfirReferenceParentType.Invalid
            else CfirReferenceParentType.Instantiated(context.type(value.matchedParentTy))
        val target = value.target
        val boundTarget = context.referenceTarget(target)
        // native super() 的 RefExpr 具有函数类型及真实 constructor 目标；同名父类型值才是 receiver。
        // MemberAccess 自身是成员引用，其拥有的 receiver 由独立 RefExpr 保存，不能按成员拼写重标角色。
        val role = when {
            expr.kind != ExprKind.RefExpr || (spelling != "this" && spelling != "super") -> CfirSourceReferenceRole.ORDINARY
            context.type(expr.type) is ConeFunctionType -> {
                // 反引号标识符及别名的函数值借用原声明；Func 类型本身不授予 Package/Absent callee 资格。
                val declaration = (boundTarget as? CfirSourceReferenceTarget.Declaration)?.declaration
                val callable = when (declaration) {
                    is CfirConstructor, is CfirNamedFunction -> true
                    is CfirEnumConstructor -> declaration.exportFacts.enumConstructorHasParameterList
                    is CfirVariable -> declaration.returnTypeRef.coneTypeOrNull?.let(::isCompletedFunctionValueType) == true
                    else -> false
                }
                check(callable) { "A function-valued SOURCE $spelling reference requires its completed callable declaration target" }
                CfirSourceReferenceRole.ORDINARY
            }
            spelling == "this" -> CfirSourceReferenceRole.THIS
            else -> CfirSourceReferenceRole.SUPER
        }
        val kind = when (classifySourceReferenceTarget(target)) {
            CjoSourceReferenceTargetKind.DECLARATION -> referenceKind(checkNotNull(target))
            else -> CfirSourceReferenceKind.SOURCE
        }
        return CfirSourceExpressionPayload.Reference(name, boundTarget,
            (0 until value.instTysLength).map { context.type(value.instTys(it)) }, parent,
            kind, role)
    }

    /** 仅查询已绑定的 Alias 身份与完成 RHS；非 Alias classifier 不触发 provider 查询。 */
    private fun isCompletedFunctionValueType(type: ConeCangJieType): Boolean {
        var current = type
        val checkingDefinitions = HashSet<CfirTypeAliasSymbol>()
        val completedDefinitions = HashSet<CfirTypeAliasSymbol>()
        while (current !is ConeFunctionType) {
            val tag = (current as? ConeClassLikeType)?.lookupTag as? ConeClassLikeLookupTagWithFixedSymbol ?: return false
            val symbol = tag.symbol as? CfirTypeAliasSymbol ?: return false
            validateCompletedAliasDefinition(symbol, checkingDefinitions, completedDefinitions)
            val alias = symbol.cfir
            val rhs = checkNotNull(alias.expandedTypeRef.coneTypeOrNull) { "A SOURCE function value requires its completed alias RHS" }
            check(alias.typeParameters.size == current.typeArguments.size) { "A SOURCE function value alias requires its complete type arguments" }
            current = alias.createParametersSubstitutor(current, alias.moduleData.session).substituteOrSelf(rhs)
        }
        return true
    }

    /** 校验原 RHS 的定义依赖；Identity<Identity<Func>> 的实参复用不属于定义递归。 */
    private fun validateCompletedAliasDefinition(
        symbol: CfirTypeAliasSymbol,
        checking: MutableSet<CfirTypeAliasSymbol>,
        completed: MutableSet<CfirTypeAliasSymbol>,
    ) {
        if (symbol in completed) return
        check(symbol.isBound && checking.add(symbol)) { "A SOURCE function value alias requires a bound, acyclic header" }
        val rhs = checkNotNull(symbol.cfir.expandedTypeRef.coneTypeOrNull) {
            "A SOURCE function value requires its completed alias RHS"
        }
        rhs.forEachType { component ->
            val tag = (component as? ConeClassLikeType)?.lookupTag as? ConeClassLikeLookupTagWithFixedSymbol
            val dependency = tag?.symbol as? CfirTypeAliasSymbol
            if (dependency != null) validateCompletedAliasDefinition(dependency, checking, completed)
        }
        check(checking.remove(symbol))
        completed.add(symbol)
    }

    private fun pattern(position: Int, owner: CfirDeclaration): CfirSourcePattern = context.patternRecord(position) {
        val value = Pattern().__assign(position, context.pkg.getByteBuffer())
        val table = CjoSourceTable().at(position, context.pkg.getByteBuffer())
        val kind = when (value.kind) {
            PatternKind.ConstPattern -> CfirSourcePatternKind.CONSTANT
            PatternKind.WildcardPattern -> CfirSourcePatternKind.WILDCARD
            PatternKind.VarPattern -> CfirSourcePatternKind.VARIABLE
            PatternKind.TuplePattern -> CfirSourcePatternKind.TUPLE
            PatternKind.TypePattern -> CfirSourcePatternKind.TYPE
            PatternKind.EnumPattern -> CfirSourcePatternKind.ENUM
            PatternKind.ExceptTypePattern -> CfirSourcePatternKind.EXCEPTION_TYPE
            PatternKind.CommandTypePattern -> CfirSourcePatternKind.COMMAND_TYPE
            else -> error("Unknown SOURCE pattern kind")
        }
        val hasTypeTail = kind == CfirSourcePatternKind.EXCEPTION_TYPE || kind == CfirSourcePatternKind.COMMAND_TYPE
        check(!hasTypeTail || value.typesLength >= 1)
        val types = (0 until if (hasTypeTail) 1 else value.typesLength).map { context.type(value.types(it)) }
        CfirSourcePattern(kind, types,
            (0 until value.exprsLength).map {
                if (kind == CfirSourcePatternKind.VARIABLE) CfirSourceSlot.Declaration(declaration(value.exprs(it)))
                else CfirSourceSlot.Expression(expressionReference(value.exprs(it)))
            },
            (0 until value.patternsLength).map { pattern(table.tablePosition(10, it), owner) },
            value.needRuntimeTypeCheck, value.matchBeforeRuntime,
            typeReferences = if (hasTypeTail) (1 until value.typesLength).map { context.typeReference(value.types(it)) } else emptyList(),
            importedPosition = context.position(value),
            constantValues = context.constants(value, position),
            identity = context.patternIdentity(position))
    }

    private fun referenceKind(reference: FullId): CfirSourceReferenceKind = when (reference.pkgId) {
        org.cangnova.cangjie.cfir.serialization.cjo.PackageIndex.CURRENT.value -> CfirSourceReferenceKind.CURRENT
        else -> CfirSourceReferenceKind.IMPORTED
    }

    private fun literalKind(kind: UByte): CfirLiteralKind = when (kind) {
        LitConstKind.Integer -> CfirLiteralKind.INT
        LitConstKind.Float -> CfirLiteralKind.FLOAT
        LitConstKind.Bool -> CfirLiteralKind.BOOLEAN
        LitConstKind.Rune -> CfirLiteralKind.RUNE
        LitConstKind.RuneByte -> CfirLiteralKind.BYTE
        LitConstKind.String -> CfirLiteralKind.STRING
        LitConstKind.JString -> CfirLiteralKind.JSTRING
        LitConstKind.Unit -> CfirLiteralKind.UNIT
        else -> error("Unknown SOURCE literal kind")
    }

    private fun annotationKind(kind: UShort): CfirAnnotationExportKind = when (kind) {
        AnnoKind.Deprecated -> CfirAnnotationExportKind.DEPRECATED
        AnnoKind.Frozen -> CfirAnnotationExportKind.FROZEN
        AnnoKind.TestRegistration -> CfirAnnotationExportKind.TEST_REGISTRATION
        AnnoKind.Annotation -> CfirAnnotationExportKind.ANNOTATION
        AnnoKind.Custom -> CfirAnnotationExportKind.CUSTOM
        AnnoKind.JavaMirror -> CfirAnnotationExportKind.JAVA_MIRROR
        AnnoKind.JavaImpl -> CfirAnnotationExportKind.JAVA_IMPL
        AnnoKind.JavaHasDefault -> CfirAnnotationExportKind.JAVA_HAS_DEFAULT
        AnnoKind.ObjCMirror -> CfirAnnotationExportKind.OBJ_C_MIRROR
        AnnoKind.ObjCImpl -> CfirAnnotationExportKind.OBJ_C_IMPL
        AnnoKind.ForeignName -> CfirAnnotationExportKind.FOREIGN_NAME
        else -> error("Unknown SOURCE annotation kind")
    }

    private fun callKind(kind: UByte): CfirSourceCallKind = when (kind) {
        CallKind.CallDeclaredFunction -> CfirSourceCallKind.DECLARED
        CallKind.CallIntrinsicFunction -> CfirSourceCallKind.INTRINSIC
        CallKind.CallSuperFunction -> CfirSourceCallKind.SUPER
        CallKind.CallObjectCreation -> CfirSourceCallKind.OBJECT_CREATION
        CallKind.CallStructCreation -> CfirSourceCallKind.STRUCT_CREATION
        CallKind.CallVariadicFunction -> CfirSourceCallKind.VARIADIC
        CallKind.CallFunctionPtr -> CfirSourceCallKind.FUNCTION_POINTER
        CallKind.CallAnnotation -> CfirSourceCallKind.ANNOTATION
        CallKind.CallBuiltinFunction -> CfirSourceCallKind.BUILTIN
        else -> error("Unknown SOURCE call kind")
    }

    private fun operator(kind: UByte): CfirSourceOperator =
        CfirSourceOperator.entries.single { it.wire() == kind }
}

/** Header-only与SOURCE复用同一binder/读取器；不通过此入口加载body或解析表达式。 */
internal fun readSourceFunctionHeader(
    context: CfirSourceHeaderDecodingContext,
    function: CfirCallableDeclaration,
    body: FuncBody,
): CfirSourceImportedFunctionHeader {
        val signature = context.signature(function)
        if (signature == CfirSourceCallableSignature.Unexported) {
            check(body.paramListsLength == 0 && body.retType == 0u && body.body == 0u && !body.always && body.captureKind == 0.toUByte()) {
                "An unexported signature requires the actual native partial empty FuncBody"
            }
        }
        val parameters = (0 until body.paramListsLength).map { ordinal ->
            val list = checkNotNull(body.paramLists(ordinal))
            (0 until list.paramsLength).map {
                (context.declaration(list.params(it)) as CfirValueParameter).also { parameter ->
                    check(parameter.containingDeclarationSymbol === function.symbol)
                }
            }
        }
        val desugars = (0 until body.paramListsLength).map { ordinal ->
            val list = checkNotNull(body.paramLists(ordinal))
            check(list.desugarsLength == 0 || list.desugarsLength == list.paramsLength)
            (0 until list.desugarsLength).map {
                list.desugars(it).takeIf { reference -> reference != 0u }
                    ?.let { reference -> context.declaration(reference) as CfirNamedFunction }
            }
        }
        return CfirSourceImportedFunctionHeader(
            parameters,
            desugars,
            body.retType.takeIf { it != 0u }?.let(context::type),
            body.captureKind.toInt(), body.always, body.body != 0u,
            signature,
        )
}
