package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.*
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.references.CfirReferenceParentType
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.OperatorNameConventions

/** 同一包编码上下文拥有d/e/t表和来源；SOURCE编码器不能另建索引或查询活动resolver。 */
internal interface CjoCanonicalSourceEncodingContext {
    val sourceEncodingContract: CjoSourceEncodingContract
    fun reserveExpression(identity: CfirSourceExpressionIdentity): UInt
    fun expressionIndex(identity: CfirSourceExpressionIdentity): UInt
    fun beginExpression(identity: CfirSourceExpressionIdentity): Boolean
    fun fillExpression(index: UInt, metadata: CjoExpressionMetadata)
    fun declarationReference(declaration: CfirDeclaration): UInt
    fun declarationTarget(declaration: CfirDeclaration, kind: CfirSourceReferenceKind = CfirSourceReferenceKind.SOURCE): CjoExpressionTargetMetadata
    fun ownedDeclarationTarget(declaration: CfirDeclaration): CjoExpressionTargetMetadata
    fun savedReferenceTarget(reference: CfirSourceSavedReference): CjoExpressionTargetMetadata?
    fun typeReference(type: ConeCangJieType, context: CfirDeclaration): UInt
    fun typeReference(ref: CfirTypeRef, context: CfirDeclaration): UInt
    /** 包含lambda的FuncBody.retType只消费同代TypeUse，不借signature补齐缺少的节点。 */
    fun functionReturnTypeReference(function: CfirCallableDeclaration): UInt
    fun invalidTypeReference(): UInt
    fun parameterDesugar(parameter: CfirValueParameter): UInt
    fun positions(context: CfirDeclaration, span: CfirSourceSpan?, imported: CfirSourceImportedPosition? = null): Pair<CjoPositionMetadata?, CjoPositionMetadata?>
    fun constantValues(pattern: CfirSourcePattern, owner: CfirDeclaration): List<CjoConstValueInfo>
    fun annotationArgumentName(name: org.cangnova.cangjie.name.Name?, owner: CfirDeclaration): String?
}

/**
 * 全部SOURCE30及三个辅助角色共用的图encoder。输入是完成、同代捕获的canonical产品，
 * 只做原生格式投影：先reserve，再递归fill；不生成Return/helper/callee或猜测d/e槽。
 */
internal class CjoCanonicalSourceEncoder(
    graphs: Collection<CfirSourceGraph>,
    private val context: CjoCanonicalSourceEncodingContext,
) {
    private val graphs = graphs.distinct()
    private val expressionGraphs = java.util.IdentityHashMap<CfirSourceExpressionIdentity, CfirSourceGraph>().apply {
        this@CjoCanonicalSourceEncoder.graphs.forEach { graph -> graph.expressions.keys.forEach { identity ->
            check(put(identity, graph).let { it == null || it === graph }) { "A canonical expression belongs to multiple graphs" }
        } }
    }

    fun graphOf(identity: CfirSourceExpressionIdentity): CfirSourceGraph = expressionGraphs.getValue(identity)

    fun expression(identity: CfirSourceExpressionIdentity): UInt {
        val index = context.expressionIndex(identity)
        if (!context.beginExpression(identity)) return index
        val record = graphOf(identity).expression(identity)
        val nodePayload = record.payload
        val metadata = payload(nodePayload, record.context)
        val (begin, end) = context.positions(record.context, record.span, record.importedPosition)
        context.fillExpression(index, metadata.copy(
            type = context.typeReference(record.type, record.context),
            begin = begin, end = end,
            mapExpression = if (nodePayload is CfirSourceExpressionPayload.Jump) context.expressionIndex(nodePayload.loop)
                else record.mapExpression?.let(context::expressionIndex) ?: 0u,
            overflowPolicy = when (record.overflowStrategy) {
                org.cangnova.cangjie.annotations.CangjieOverflowStrategy.THROWING -> OverflowPolicy.Throwing
                org.cangnova.cangjie.annotations.CangjieOverflowStrategy.WRAPPING -> OverflowPolicy.Wrapping
                org.cangnova.cangjie.annotations.CangjieOverflowStrategy.SATURATING -> OverflowPolicy.Saturating
                org.cangnova.cangjie.annotations.CangjieOverflowStrategy.CHECKED -> OverflowPolicy.Checked
                null, org.cangnova.cangjie.annotations.CangjieOverflowStrategy.NA -> OverflowPolicy.NA
            },
        ))
        return index
    }

    /** loaded F 消费原 header 槽；SOURCE F 消费创建 owner 保存的 TypeUse，不以 signature 补 0。 */
    fun functionBody(body: CfirSourceFunctionBody): CjoFunctionBodyInfo {
        val header = body.importedHeader
        if (body.signature == CfirSourceCallableSignature.Unexported) {
            check(header != null && header.signature == body.signature && header.parameterLists.isEmpty() &&
                header.desugaredParameterLists.isEmpty() && header.returnType == null && !header.bodyAvailable &&
                !header.always && header.captureKind == 0 && body.parameters.isEmpty() && body.body == null) {
                "A signature omitted by Native must retain its exact empty F header"
            }
        }
        return CjoFunctionBodyInfo(
            parameterLists = if (header == null) listOf(body.parameters.map(context::declarationReference))
                else header.parameterLists.map { it.map(context::declarationReference) },
            desugaredParameterLists = if (header == null) listOf(body.parameters.map(context::parameterDesugar))
                else header.desugaredParameterLists.map { group -> group.map { it?.let(context::declarationReference) ?: 0u } },
            returnType = if (header == null) context.functionReturnTypeReference(body.owner)
                else header.returnType?.let { context.typeReference(it, body.owner) } ?: 0u,
            body = body.body?.let(::expression) ?: 0u,
            always = context.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA && body.always,
            captureKind = if (body.body != null || context.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA)
                body.captureKind.toUByte() else 0u,
        )
    }

    fun declarationPattern(variable: CfirPatternVariable): CjoPatternMetadata =
        pattern(graphs.single { it.declarationPatterns.containsKey(variable) }.declarationPatterns.getValue(variable), variable)

    fun annotations(owner: CfirDeclaration, values: List<CfirSourceAnnotation>): List<CjoAnnotationMetadata> = values.map { value ->
        CjoAnnotationMetadata(kind = when (value.kind) {
            org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportKind.DEPRECATED -> AnnoKind.Deprecated
            org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportKind.FROZEN -> AnnoKind.Frozen
            org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportKind.TEST_REGISTRATION -> AnnoKind.TestRegistration
            org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportKind.ANNOTATION -> AnnoKind.Annotation
            org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportKind.CUSTOM -> AnnoKind.Custom
            org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportKind.JAVA_MIRROR -> AnnoKind.JavaMirror
            org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportKind.JAVA_IMPL -> AnnoKind.JavaImpl
            org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportKind.JAVA_HAS_DEFAULT -> AnnoKind.JavaHasDefault
            org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportKind.OBJ_C_MIRROR -> AnnoKind.ObjCMirror
            org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportKind.OBJ_C_IMPL -> AnnoKind.ObjCImpl
            org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportKind.FOREIGN_NAME -> AnnoKind.ForeignName
        }, identifier = value.identifier, arguments = value.arguments.map { argument ->
            CjoAnnotationArgumentMetadata(context.annotationArgumentName(argument.name, owner), expression(argument.expression))
        }, target = value.target?.let { declaration ->
            context.declarationTarget(declaration, value.targetKind).let { CjoAnnotationTargetMetadata(it.pkgId, it.decl, it.index) }
        })
    }

    private fun payload(value: CfirSourceExpressionPayload, owner: CfirDeclaration): CjoExpressionMetadata = when (value) {
        CfirSourceExpressionPayload.PrimitiveType -> CjoExpressionMetadata(kind = ExprKind.PrimitiveTypeExpr)
        CfirSourceExpressionPayload.Wildcard -> CjoExpressionMetadata(kind = ExprKind.WildcardExpr)
        is CfirSourceExpressionPayload.Return -> CjoExpressionMetadata(kind = ExprKind.ReturnExpr, operands = listOf(expression(value.value)))
        is CfirSourceExpressionPayload.Jump -> CjoExpressionMetadata(kind = ExprKind.JumpExpr, additionalInfo = CjoAdditionalExpressionInfoMetadata.Jump(value.isBreak))
        is CfirSourceExpressionPayload.Reference -> CjoExpressionMetadata(kind = ExprKind.RefExpr, reference = reference(value, owner))
        is CfirSourceExpressionPayload.Member -> CjoExpressionMetadata(kind = ExprKind.MemberAccess,
            operands = listOf(expression(value.receiver)), reference = reference(value.reference, owner))
        is CfirSourceExpressionPayload.Call -> CjoExpressionMetadata(kind = ExprKind.CallExpr,
            operands = listOf(expression(value.base)) + value.arguments.map(::expression),
            call = CjoCallExpressionMetadata(value.hasSideEffect, when (value.kind) {
                CfirSourceCallKind.DECLARED -> CallKind.CallDeclaredFunction
                CfirSourceCallKind.INTRINSIC -> CallKind.CallIntrinsicFunction
                CfirSourceCallKind.SUPER -> CallKind.CallSuperFunction
                CfirSourceCallKind.OBJECT_CREATION -> CallKind.CallObjectCreation
                CfirSourceCallKind.STRUCT_CREATION -> CallKind.CallStructCreation
                CfirSourceCallKind.VARIADIC -> CallKind.CallVariadicFunction
                CfirSourceCallKind.FUNCTION_POINTER -> CallKind.CallFunctionPtr
                CfirSourceCallKind.ANNOTATION -> CallKind.CallAnnotation
                CfirSourceCallKind.BUILTIN -> CallKind.CallBuiltinFunction
            }))
        is CfirSourceExpressionPayload.Unary -> CjoExpressionMetadata(kind = ExprKind.UnaryExpr,
            operands = listOf(expression(value.operand)), additionalInfo = CjoAdditionalExpressionInfoMetadata.Unary(value.operator.wire()))
        is CfirSourceExpressionPayload.Increment -> CjoExpressionMetadata(kind = ExprKind.IncOrDecExpr,
            operands = listOf(expression(value.operand)), additionalInfo = CjoAdditionalExpressionInfoMetadata.IncOrDec(value.operator.wire()))
        is CfirSourceExpressionPayload.Literal -> CjoExpressionMetadata(kind = ExprKind.LitConstExpr,
            literal = CjoLiteralExpressionMetadata(value.text, when (value.kind) {
                CfirLiteralKind.INT -> LitConstKind.Integer
                CfirLiteralKind.FLOAT -> LitConstKind.Float
                CfirLiteralKind.BOOLEAN -> LitConstKind.Bool
                CfirLiteralKind.RUNE -> LitConstKind.Rune
                CfirLiteralKind.BYTE -> LitConstKind.RuneByte
                CfirLiteralKind.STRING -> LitConstKind.String
                CfirLiteralKind.JSTRING -> LitConstKind.JString
                CfirLiteralKind.UNIT -> LitConstKind.Unit
            }, when (value.stringKind) {
                CfirSourceStringKind.NORMAL -> StringKind.Normal
                CfirSourceStringKind.JSTRING -> StringKind.JString
                CfirSourceStringKind.MULTILINE -> StringKind.MultiLine
                CfirSourceStringKind.MULTILINE_RAW -> StringKind.MultiLineRaw
            }))
        is CfirSourceExpressionPayload.Binary -> CjoExpressionMetadata(kind = ExprKind.BinaryExpr,
            operands = listOf(expression(value.left), expression(value.right)), additionalInfo = CjoAdditionalExpressionInfoMetadata.Binary(value.operator.wire()))
        is CfirSourceExpressionPayload.Subscript -> CjoExpressionMetadata(kind = ExprKind.SubscriptExpr,
            operands = listOf(expression(value.receiver)) + value.indices.map(::expression), additionalInfo = CjoAdditionalExpressionInfoMetadata.Subscript(value.isTupleAccess))
        is CfirSourceExpressionPayload.Assignment -> CjoExpressionMetadata(kind = ExprKind.AssignExpr,
            operands = listOf(expression(value.left), expression(value.right)), additionalInfo = CjoAdditionalExpressionInfoMetadata.Assign(value.isCompound, value.operator.wire()))
        is CfirSourceExpressionPayload.Array -> CjoExpressionMetadata(kind = ExprKind.ArrayExpr, operands = value.arguments.map(::expression),
            additionalInfo = CjoAdditionalExpressionInfoMetadata.Array(arrayInitializer(value.savedInitializer, value.initializer, value.initializerKind), value.isValueArray))
        is CfirSourceExpressionPayload.Pointer -> CjoExpressionMetadata(kind = ExprKind.PointerExpr, operands = listOfNotNull(value.argument?.let(::expression)))
        is CfirSourceExpressionPayload.TypeConversion -> CjoExpressionMetadata(kind = ExprKind.TypeConvExpr, operands = listOf(expression(value.value)))
        is CfirSourceExpressionPayload.Throw -> CjoExpressionMetadata(kind = ExprKind.ThrowExpr, operands = listOf(expression(value.exception)))
        is CfirSourceExpressionPayload.Perform -> CjoExpressionMetadata(kind = ExprKind.PerformExpr, operands = listOf(expression(value.value)))
        is CfirSourceExpressionPayload.Spawn -> CjoExpressionMetadata(kind = ExprKind.SpawnExpr,
            operands = listOfNotNull(value.argument?.let(::expression)), additionalInfo = CjoAdditionalExpressionInfoMetadata.Spawn(context.ownedDeclarationTarget(value.future)))
        is CfirSourceExpressionPayload.ArrayLiteral -> CjoExpressionMetadata(kind = ExprKind.ArrayLit, operands = value.elements.map(::expression),
            additionalInfo = CjoAdditionalExpressionInfoMetadata.Array(arrayInitializer(value.savedInitializer, value.initializer, value.initializerKind),
                context.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA && value.isValueArray))
        is CfirSourceExpressionPayload.Tuple -> CjoExpressionMetadata(kind = ExprKind.TupleLit, operands = value.elements.map(::expression))
        is CfirSourceExpressionPayload.Conditional -> CjoExpressionMetadata(kind = ExprKind.IfExpr,
            operands = listOf(expression(value.condition), expression(value.thenBranch), value.elseBranch?.let(::expression) ?: 0u))
        is CfirSourceExpressionPayload.Try -> CjoExpressionMetadata(kind = ExprKind.TryExpr,
            operands = listOf(expression(value.body)) + value.catches.map(::expression) + (value.finallyBlock?.let(::expression) ?: 0u),
            additionalInfo = CjoAdditionalExpressionInfoMetadata.Try(value.resources.map(context::ownedDeclarationTarget), value.patterns.map { pattern(it, owner) }))
        is CfirSourceExpressionPayload.Loop -> CjoExpressionMetadata(kind = if (value.isDoWhile) ExprKind.DoWhileExpr else ExprKind.WhileExpr,
            operands = if (value.isDoWhile) listOf(expression(value.body), expression(value.condition)) else listOf(expression(value.condition), expression(value.body)))
        is CfirSourceExpressionPayload.Lambda -> CjoExpressionMetadata(kind = ExprKind.LambdaExpr,
            additionalInfo = CjoAdditionalExpressionInfoMetadata.Lambda(functionBody(value.function), value.supportMock))
        is CfirSourceExpressionPayload.Block -> CjoExpressionMetadata(kind = ExprKind.Block,
            operands = value.statements.map { slot(it) }, additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(
                value.statements.map { it is CfirSourceSlot.Expression } + value.isUnsafe))
        is CfirSourceExpressionPayload.Match -> CjoExpressionMetadata(kind = ExprKind.MatchExpr,
            operands = listOfNotNull(value.selector?.let(::expression)) + value.cases.map(::expression),
            additionalInfo = CjoAdditionalExpressionInfoMetadata.Match(value.selector != null))
        is CfirSourceExpressionPayload.LetPatternDestructor -> CjoExpressionMetadata(kind = ExprKind.LetPatternDestructor,
            operands = listOf(expression(value.initializer)), additionalInfo = CjoAdditionalExpressionInfoMetadata.LetPatternDestructor(value.patterns.map { pattern(it, owner) }))
        is CfirSourceExpressionPayload.ForIn -> CjoExpressionMetadata(kind = ExprKind.ForInExpr,
            operands = listOf(expression(value.iterable), value.guard?.let(::expression) ?: 0u, expression(value.body)),
            additionalInfo = CjoAdditionalExpressionInfoMetadata.ForIn(pattern(value.pattern, owner), when (value.kind) {
                CfirSourceForInKind.DIRECT -> ForInKind.NA
                CfirSourceForInKind.RANGE -> ForInKind.Range
                CfirSourceForInKind.STRING -> ForInKind.String
                CfirSourceForInKind.ITERATOR -> ForInKind.Iterator
            }))
        is CfirSourceExpressionPayload.MatchCase -> CjoExpressionMetadata(kind = ExprKind.MatchCase,
            operands = listOf(value.guard?.let(::expression) ?: 0u, expression(value.body)),
            additionalInfo = CjoAdditionalExpressionInfoMetadata.MatchCase(value.patterns.map { pattern(it, owner) }))
        is CfirSourceExpressionPayload.MatchCaseOther -> CjoExpressionMetadata(kind = ExprKind.MatchCaseOther,
            operands = listOf(expression(value.condition), expression(value.body)))
        is CfirSourceExpressionPayload.Argument -> CjoExpressionMetadata(kind = ExprKind.InvalidExpr,
            operands = listOfNotNull(value.value?.let(::expression)), functionArgument = CjoFunctionArgumentExpressionMetadata(value.withInout, value.isDefaultValue))
    }

    /** schema 保留原字段；native AST 重写消费语义 factory，空目标写真实 INVALID FullId。 */
    private fun arrayInitializer(saved: CfirSourceSavedReference?, initializer: CfirDeclaration?, kind: CfirSourceReferenceKind): CjoExpressionTargetMetadata? =
        if (context.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA && saved != null)
            context.savedReferenceTarget(saved)
        else initializer?.let { context.declarationTarget(it, kind) }
            ?: if (context.sourceEncodingContract == CjoSourceEncodingContract.NATIVE_SOURCE)
                CjoExpressionTargetMetadata(PackageIndex.INVALID.value, decl = "") else null

    private fun reference(value: CfirSourceExpressionPayload.Reference, owner: CfirDeclaration): CjoReferenceExpressionMetadata =
        CjoReferenceExpressionMetadata(OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[value.name] ?: value.name.asString(), when (val target = value.target) {
            is CfirSourceReferenceTarget.Declaration -> context.declarationTarget(target.declaration, value.targetKind)
            is CfirSourceReferenceTarget.Package -> CjoExpressionTargetMetadata(PackageIndex.PACKAGE_REFERENCE.value, target.packageName.asString())
            is CfirSourceReferenceTarget.Absent -> when (target.reason) {
                CfirSourceReferenceAbsence.FIELD_MISSING -> null
                CfirSourceReferenceAbsence.INVALID_SENTINEL -> CjoExpressionTargetMetadata(PackageIndex.INVALID.value, "")
            }
        },
            value.instantiatedTypes.map { context.typeReference(it, owner) }, when (val parent = value.matchedParentType) {
                CfirReferenceParentType.Absent -> 0u
                CfirReferenceParentType.Invalid -> context.invalidTypeReference()
                is CfirReferenceParentType.Instantiated -> context.typeReference(parent.type, owner)
            })

    private fun slot(slot: CfirSourceSlot): UInt = when (slot) {
        is CfirSourceSlot.Declaration -> context.declarationReference(slot.declaration)
        is CfirSourceSlot.Expression -> expression(slot.identity)
    }

    private fun pattern(value: CfirSourcePattern, owner: CfirDeclaration): CjoPatternMetadata = CjoPatternMetadata(
        kind = when (value.kind) {
            CfirSourcePatternKind.CONSTANT -> PatternKind.ConstPattern
            CfirSourcePatternKind.WILDCARD -> PatternKind.WildcardPattern
            CfirSourcePatternKind.VARIABLE -> PatternKind.VarPattern
            CfirSourcePatternKind.TUPLE -> PatternKind.TuplePattern
            CfirSourcePatternKind.TYPE -> PatternKind.TypePattern
            CfirSourcePatternKind.ENUM -> PatternKind.EnumPattern
            CfirSourcePatternKind.EXCEPTION_TYPE -> PatternKind.ExceptTypePattern
            CfirSourcePatternKind.COMMAND_TYPE -> PatternKind.CommandTypePattern
        },
        types = value.types.map { context.typeReference(it, owner) } + value.typeReferences.map { context.typeReference(it, owner) },
        exprs = value.slots.map(::slot),
        patterns = value.patterns.map { pattern(it, owner) },
        matchBeforeRuntime = value.matchBeforeRuntime, needRuntimeTypeCheck = value.needRuntimeTypeCheck,
        values = context.constantValues(value, owner),
        begin = context.positions(owner, value.span, value.importedPosition).first,
        end = context.positions(owner, value.span, value.importedPosition).second,
    )
}

internal fun CfirSourceOperator.wire(): UByte = when (this) {
    CfirSourceOperator.INDEX -> OperatorKind.Index; CfirSourceOperator.CALL -> OperatorKind.Call
    CfirSourceOperator.NOT -> OperatorKind.Not; CfirSourceOperator.POWER -> OperatorKind.Power
    CfirSourceOperator.MULTIPLY -> OperatorKind.Multiply; CfirSourceOperator.DIVIDE -> OperatorKind.Divide
    CfirSourceOperator.REMAINDER -> OperatorKind.Remainder; CfirSourceOperator.ADD -> OperatorKind.Add; CfirSourceOperator.SUBTRACT -> OperatorKind.Subtract
    CfirSourceOperator.SHIFT_LEFT -> OperatorKind.BitLeftShift; CfirSourceOperator.SHIFT_RIGHT -> OperatorKind.BitRightShift
    CfirSourceOperator.LESS -> OperatorKind.LT; CfirSourceOperator.LESS_EQUAL -> OperatorKind.LE
    CfirSourceOperator.GREATER -> OperatorKind.GT; CfirSourceOperator.GREATER_EQUAL -> OperatorKind.GE
    CfirSourceOperator.EQUAL -> OperatorKind.Equal; CfirSourceOperator.NOT_EQUAL -> OperatorKind.NotEqual
    CfirSourceOperator.BIT_AND -> OperatorKind.BitAnd; CfirSourceOperator.BIT_XOR -> OperatorKind.BitXor; CfirSourceOperator.BIT_OR -> OperatorKind.BitOr
    CfirSourceOperator.POST_INCREMENT -> OperatorKind.PostInc; CfirSourceOperator.POST_DECREMENT -> OperatorKind.PostDec
    CfirSourceOperator.IS -> OperatorKind.Is; CfirSourceOperator.AS -> OperatorKind.As
    CfirSourceOperator.LOGICAL_AND -> OperatorKind.LogicAnd; CfirSourceOperator.LOGICAL_OR -> OperatorKind.LogicOr
    CfirSourceOperator.COALESCING -> OperatorKind.Coalescing; CfirSourceOperator.PIPELINE -> OperatorKind.Pipeline; CfirSourceOperator.COMPOSITION -> OperatorKind.Composition
    CfirSourceOperator.ASSIGN -> OperatorKind.Assign; CfirSourceOperator.POWER_ASSIGN -> OperatorKind.PowerAssign
    CfirSourceOperator.MULTIPLY_ASSIGN -> OperatorKind.MultiplyAssign; CfirSourceOperator.DIVIDE_ASSIGN -> OperatorKind.DivideAssign
    CfirSourceOperator.REMAINDER_ASSIGN -> OperatorKind.RemainderAssign; CfirSourceOperator.ADD_ASSIGN -> OperatorKind.AddAssign; CfirSourceOperator.SUBTRACT_ASSIGN -> OperatorKind.SubtractAssign
    CfirSourceOperator.SHIFT_LEFT_ASSIGN -> OperatorKind.LeftShiftAssign; CfirSourceOperator.SHIFT_RIGHT_ASSIGN -> OperatorKind.RightShiftAssign
    CfirSourceOperator.BIT_AND_ASSIGN -> OperatorKind.BitAndAssign; CfirSourceOperator.BIT_XOR_ASSIGN -> OperatorKind.BitXorAssign; CfirSourceOperator.BIT_OR_ASSIGN -> OperatorKind.BitOrAssign
    CfirSourceOperator.LOGICAL_AND_ASSIGN -> OperatorKind.LogicAndAssign; CfirSourceOperator.LOGICAL_OR_ASSIGN -> OperatorKind.LogicOrAssign
}
