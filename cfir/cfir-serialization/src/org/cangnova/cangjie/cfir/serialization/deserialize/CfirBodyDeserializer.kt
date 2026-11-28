@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.deserialize

import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.patterns.builder.*
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.references.builder.buildSuperReference
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.symbols.CfirThisOwnerSymbol
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import java.util.IdentityHashMap

/**
 * 正式普通正文读取器。annotation、default、initializer 和 F 共用 SOURCE33 的完成载荷，
 * 声明及类型仍由同一包 binder 登记；不恢复表面语法、不再运行 lowering。
 *
 * 对位 Kotlin IrBodyDeserializer 的 operation/statement 分派与 loop header 先登记协议。
 * Native 的 F/P、map、FuncArg 和四种字符串形态是已考证的格式差异。
 */
internal class CfirBodyDeserializer(private val context: CfirDeserializationContext, private val declarations: CfirDeclDeserializer) {
    fun deserializeExpression(reference: UInt, root: CjoSourceContentRoot): CfirExpression = context.materialize {
        check(reference != 0u && reference <= context.pkg.allExprsLength.toUInt()) { "Required CJO expression has an invalid saved reference: $reference" }
        // 缓存只能复用完成产品，不能替代此次原保存槽的授权。
        val selected = declarations.withSourceDeclarationHeaders {
            CfirSourceGraphLoader(context, declarations).content(listOf(root))
        }
        check(reference in selected.expressionReferences) { "The requested e does not belong to its explicit content root" }
        requireBorrowedOrdinary(selected)
        Reader(selected.graph).read(selected.expressionReferences.getValue(reference))
    }

    /** 分组域由调用方明确提供，map环等绑定一次完成；返回值仅包含该域真实拥有的e。 */
    fun deserializeExpressions(roots: List<CjoSourceContentRoot>): Map<UInt, CfirExpression> = context.materialize {
        val selected = declarations.withSourceDeclarationHeaders { CfirSourceGraphLoader(context, declarations).content(roots) }
        requireBorrowedOrdinary(selected)
        val reader = Reader(selected.graph)
        selected.expressionReferences.mapValues { (_, identity) -> reader.read(identity) }
    }

    /** canonical完成不等于ordinary完成；借用节点、loop target只能消费调用方先前明确完成的产品。 */
    private fun requireBorrowedOrdinary(selected: CfirDeserializedSourceGraph) {
        selected.selection.borrowedBindings.forEach { binding ->
            val identity = context.bodyState.identities.getValue(binding.target)
            check(identity in context.bodyState.expressions) { "Borrowed e ${binding.target} requires its completed ordinary product" }
            if (binding.role == CjoSourceExpressionBindingRole.REF_LOOP)
                check(context.bodyState.loops.getValue(identity).isBound) { "Borrowed loop requires its actual completed target" }
        }
    }

    /** 真正保存了body槽才读取；原F身份、metadata和body选择分别验证。 */
    fun deserializeBody(function: CfirCallableDeclaration): CfirBlock? = context.materialize {
        val declaration = context.declarations.entries.singleOrNull { it.value === function }?.key
        if (function is CfirEnumConstructor && declaration != null &&
            checkNotNull(context.pkg.allDecls(declaration)).kind == PackageFormat.DeclKind.VarDecl) return@materialize null
        val headers = context.sourceHeaderRegistry
        val identity = if (declaration != null) headers.functionByDeclaration.getValue((declaration + 1).toUInt())
            else context.sourceFunctions.entries.single { it.value === function }.key
        val reference = headers.functions.getValue(identity).body.body
        if (reference == 0u) return@materialize null
        val body = block(deserializeExpression(reference, CjoSourceContentRoot.FunctionBody(identity)))
        context.publishFunctionBody(function, body)
        body
    }

    /** initializer由原d保存槽取得，不能误把e下标传作声明根。 */
    fun deserializeInitializer(variable: CfirVariable): CfirExpression? = context.materialize {
        val index = context.declarations.entries.single { it.value === variable }.key
        val wire = checkNotNull(context.pkg.allDecls(index))
        val reference = when (wire.kind) {
            PackageFormat.DeclKind.VarDecl -> (wire.info(PackageFormat.VarInfo()) as PackageFormat.VarInfo).initializer
            PackageFormat.DeclKind.VarWithPatternDecl -> (wire.info(PackageFormat.VarWithPatternInfo()) as PackageFormat.VarWithPatternInfo).initializer
            else -> error("SOURCE initializer requires its actual variable declaration")
        }
        if (reference == 0u) null else deserializeExpression(reference,
            CjoSourceContentRoot.Initializer((index + 1).toUInt())).also { context.publishInitializer(variable, it) }
    }

    fun deserializeDefault(parameter: CfirValueParameter): CfirExpression? = context.materialize {
        val index = context.declarations.entries.single { it.value === parameter }.key
        val wire = checkNotNull(context.pkg.allDecls(index))
        check(wire.kind == PackageFormat.DeclKind.FuncParam)
        val reference = when (wire.infoType) {
            PackageFormat.DeclInfo.NONE -> 0u
            PackageFormat.DeclInfo.ParamInfo -> (wire.info(PackageFormat.ParamInfo()) as PackageFormat.ParamInfo).defaultVal
            else -> error("A default requires its actual parameter metadata")
        }
        if (reference == 0u) null else deserializeExpression(reference,
            CjoSourceContentRoot.ParameterDefault((index + 1).toUInt())).also { context.publishDefaultValue(parameter, it) }
    }

    private inner class Reader(private val graph: CfirSourceGraph) {
        private val state get() = context.bodyState
        private val createdPatterns = IdentityHashMap<CfirSourcePatternIdentity, CfirSourcePattern>()
        private val createdExpressions = ArrayList<CfirSourceExpressionIdentity>()

        fun read(root: CfirSourceExpressionIdentity): CfirExpression {
            // 与 Kotlin loop header 协议一致：jump 的 target 从此只能查询，不能认领新 loop。
            graph.expressions.forEach { (identity, record) ->
                when (val value = record.payload) {
                    is CfirSourceExpressionPayload.Loop -> state.loops.getOrPut(identity) {
                        CfirLoopTarget()
                    }
                    is CfirSourceExpressionPayload.ForIn -> state.loops.getOrPut(identity) {
                        CfirLoopTarget()
                    }
                    is CfirSourceExpressionPayload.Return -> functionTarget(value.function)
                    else -> Unit
                }
            }
            graph.expressions.keys.forEach(::expression)
            createdExpressions.forEach { identity ->
                state.expressions.getValue(identity).replaceMappedExpression(
                    graph.expression(identity).mapExpression?.let { state.expressions.getValue(it) })
            }
            createdPatterns.forEach { (identity, record) ->
                state.patterns.getValue(identity).replaceContextExpression(
                    record.contextExpression?.let { state.expressions.getValue(it) })
            }
            graph.initializers.forEach { (variable, identity) ->
                val initializer = state.expressions.getValue(identity)
                context.publishInitializer(variable, initializer)
            }
            graph.functionBodies.forEach { (function, identity) ->
                val body = block(state.expressions.getValue(identity))
                context.publishFunctionBody(function, body)
            }
            return state.expressions.getValue(root)
        }

        private fun expression(identity: CfirSourceExpressionIdentity): CfirExpression {
            state.expressions[identity]?.let { return it }
            check(state.expressionsInProgress.add(identity)) { "SOURCE ordinary owning expression cycle" }
            try {
                val record = graph.expression(identity)
                val source = source(record.importedPosition, identity)
                val result = when (val value = record.payload) {
                    CfirSourceExpressionPayload.PrimitiveType -> buildPrimitiveTypeExpression {}
                    CfirSourceExpressionPayload.Wildcard -> buildWildcardExpression {}
                    is CfirSourceExpressionPayload.Literal -> buildLiteralExpression {
                        kind = value.kind
                        stringKind = value.stringKind
                        this.value = when (value.kind) {
                            CfirLiteralKind.UNIT -> null
                            CfirLiteralKind.BOOLEAN -> value.text.toBooleanStrict()
                            else -> value.text
                        }
                    }
                    is CfirSourceExpressionPayload.Reference -> reference(value, source, record.type, record.context)
                    is CfirSourceExpressionPayload.Member -> reference(value.reference, source, record.type, record.context).also { access ->
                        val receiver = expression(value.receiver)
                        if (access is CfirQualifiedAccessExpression) access.replaceExplicitReceiver(receiver)
                        else {
                            check(access is CfirResolvedQualifier && receiver is CfirResolvedQualifier)
                            access.replaceExplicitParent(receiver)
                        }
                    }
                    is CfirSourceExpressionPayload.Call -> buildCallExpression {
                        base = expression(value.base)
                        arguments += value.arguments.map { argument(it) }
                        callKind = value.kind
                        hasSideEffect = value.hasSideEffect
                    }
                    is CfirSourceExpressionPayload.Unary -> buildUnaryExpression {
                        operand = expression(value.operand)
                        operation = value.operator
                    }
                    is CfirSourceExpressionPayload.Increment -> buildIncrementDecrementExpression {
                        expression = expression(value.operand)
                        isPrefix = false
                        operationName = when (value.operator) {
                            CfirSourceOperator.POST_INCREMENT -> OperatorNameConventions.INC
                            CfirSourceOperator.POST_DECREMENT -> OperatorNameConventions.DEC
                            else -> error("SOURCE increment has an invalid operator")
                        }
                    }
                    is CfirSourceExpressionPayload.Binary -> buildBinaryExpression {
                        left = expression(value.left); right = expression(value.right); operation = value.operator
                    }
                    is CfirSourceExpressionPayload.Subscript -> buildSubscriptExpression {
                        receiver = expression(value.receiver)
                        indices += value.indices.map(::expression)
                        isTupleAccess = value.isTupleAccess
                    }
                    is CfirSourceExpressionPayload.Assignment -> buildAssignment {
                        lValue = expression(value.left); rValue = expression(value.right)
                        operation = value.operator; isCompound = value.isCompound
                    }
                    is CfirSourceExpressionPayload.Array -> buildArrayCreationExpression {
                        arguments += value.arguments.map { argument(it) }
                        initializerSymbol = value.initializer?.let { functionSymbol(it) }
                        isValueArray = value.isValueArray
                    }
                    is CfirSourceExpressionPayload.ArrayLiteral -> buildArrayLiteral {
                        elements += value.elements.map(::expression)
                        initializerSymbol = value.initializer?.let { functionSymbol(it) }
                    }
                    is CfirSourceExpressionPayload.Pointer -> buildPointerExpression {
                        argument = value.argument?.let { argument(it) }
                    }
                    is CfirSourceExpressionPayload.TypeConversion -> buildTypeConversion {
                        argument = expression(value.value)
                        targetTypeRef = typeRef(record.type)
                    }
                    is CfirSourceExpressionPayload.Throw -> buildThrowExpression { exception = expression(value.exception) }
                    is CfirSourceExpressionPayload.Perform -> buildPerformExpression { expression = expression(value.value) }
                    is CfirSourceExpressionPayload.Spawn -> buildResolvedSpawnExpression {
                        argument = value.argument?.let { argument(it) }
                        future = value.future as CfirFieldVariable
                    }
                    is CfirSourceExpressionPayload.Tuple -> buildTupleLiteral { elements += value.elements.map(::expression) }
                    is CfirSourceExpressionPayload.Conditional -> buildIfExpression {
                        condition = expression(value.condition)
                        thenBranch = block(expression(value.thenBranch))
                        elseBranch = value.elseBranch?.let(::expression)
                    }
                    is CfirSourceExpressionPayload.Try -> buildNativeTryExpression {
                        resources += value.resources.map { it as CfirFieldVariable }
                        tryBlock = block(expression(value.body))
                        catches += value.patterns.zip(value.catches).map { (p, b) ->
                            buildNativeCatch { pattern = pattern(p); body = block(expression(b)) }
                        }
                        finallyBlock = value.finallyBlock?.let { block(expression(it)) }
                    }
                    is CfirSourceExpressionPayload.Loop -> buildLoopExpression {
                        condition = expression(value.condition); body = block(expression(value.body)); isDoWhile = value.isDoWhile
                    }.also { state.loops.getValue(identity).bind(it) }
                    is CfirSourceExpressionPayload.ForIn -> buildForInExpression {
                        iterable = expression(value.iterable)
                        patternGuard = value.guard?.let(::expression)
                        pattern = pattern(value.pattern)
                        body = block(expression(value.body))
                        iterationKind = value.kind
                    }.also { state.loops.getValue(identity).bind(it) }
                    is CfirSourceExpressionPayload.Return -> buildReturnExpression {
                        target = functionTarget(value.function); result = expression(value.value)
                    }
                    is CfirSourceExpressionPayload.Jump -> {
                        val target = state.loops.getValue(value.loop)
                        if (value.isBreak) buildBreakExpression { this.target = target }
                        else buildContinueExpression { this.target = target }
                    }
                    is CfirSourceExpressionPayload.Lambda -> buildAnonymousFunctionExpression {
                        anonymousFunction = value.function.owner as CfirAnonymousFunction
                        isTrailingLambda = false
                        supportMock = value.supportMock
                    }
                    is CfirSourceExpressionPayload.Block -> buildBlock {
                        isUnsafe = value.isUnsafe
                        statements += value.statements.map { slot ->
                            when (slot) {
                                is CfirSourceSlot.Expression -> expression(slot.identity)
                                is CfirSourceSlot.Declaration -> slot.declaration as CfirStatement
                            }
                        }
                    }
                    is CfirSourceExpressionPayload.Match -> {
                        val selector = value.selector
                        if (selector != null) buildNativeMatchExpression {
                            this.selector = expression(selector)
                            cases += value.cases.map { expression(it) as CfirNativeMatchCase }
                        } else buildNativeConditionMatchExpression {
                            cases += value.cases.map { expression(it) as CfirNativeConditionMatchCase }
                        }
                    }
                    is CfirSourceExpressionPayload.MatchCase -> buildNativeMatchCase {
                        patterns += value.patterns.map(::pattern)
                        guard = value.guard?.let(::expression); body = block(expression(value.body))
                    }
                    is CfirSourceExpressionPayload.MatchCaseOther -> buildNativeConditionMatchCase {
                        condition = expression(value.condition); body = block(expression(value.body))
                    }
                    is CfirSourceExpressionPayload.LetPatternDestructor -> buildLetPatternDestructorExpression {
                        initializer = expression(value.initializer); patterns += value.patterns.map(::pattern)
                    }
                    is CfirSourceExpressionPayload.Argument -> buildFunctionArgument {
                        this.value = value.value?.let(::expression)
                        isDefaultValue = value.isDefaultValue; withInout = value.withInout
                    }
                }
                result.replaceSource(source)
                result.replaceConeTypeOrNull(record.type)
                result.replaceOverflowStrategy(record.overflowStrategy)
                prepareInstanceDispatch(result, record.context, source)
                check(state.expressions.put(identity, result) == null)
                createdExpressions += identity
                return result
            } finally { check(state.expressionsInProgress.remove(identity)) }
        }

        /** 从本图已绑定 F/声明拥有边恢复实例环境；super selector 的目标不能用来选择物理对象。 */
        private fun sourceInstanceReceiver(owner: CfirDeclaration, source: CjSourceElement?, implicit: Boolean = true): CfirExpression {
            val barrier = sourceInstanceBarrier(owner)
            val visited = java.util.Collections.newSetFromMap(IdentityHashMap<CfirDeclaration, Boolean>())
            var declaration: CfirDeclaration? = owner
            while (declaration != null) {
                check(visited.add(declaration)) { "SOURCE receiver environment has cyclic ownership" }
                if (declaration is CfirClassLikeDeclaration) {
                    check(declaration !is CfirTypeAlias) { "A type alias cannot own an instance receiver" }
                    val symbol = declaration.symbol
                    val receiverType = symbol.constructType(declaration.typeParameters.map { it.symbol.constructType() })
                    return sourceReceiverExpression(symbol, receiverType, source, barrier, implicit)
                }
                if (declaration is CfirExtend) {
                    val symbol = declaration.symbol
                    val receiverType = checkNotNull(declaration.extendedTypeRef.coneTypeOrNull)
                    return sourceReceiverExpression(symbol, receiverType, source, barrier, implicit)
                }
                declaration = if (declaration is CfirFunction || declaration is CfirEnumConstructor) {
                    val lexical = graph.functionContexts.getValue(declaration as CfirCallableDeclaration)
                    when {
                        lexical.outerFunction != null -> lexical.outerFunction
                        lexical.declarationContext != null -> lexical.declarationContext
                        else -> graph.declarationOwners.getValue(declaration)
                    }
                } else graph.declarationOwners.getValue(declaration)
            }
            error("SOURCE super has no bound instance receiver environment")
        }

        /** 先核对完整 owning 链的限制，再走 lexical outer；静态字段不能被 lexical class 捷径跨过。 */
        private fun sourceInstanceBarrier(owner: CfirDeclaration): InaccessibleReceiverKind? {
            val seen = java.util.Collections.newSetFromMap(IdentityHashMap<CfirDeclaration, Boolean>())
            var declaration: CfirDeclaration? = owner
            while (declaration != null && declaration !is CfirClassLikeDeclaration && declaration !is CfirExtend) {
                check(seen.add(declaration)) { "SOURCE instance barrier has cyclic ownership" }
                when {
                    declaration is CfirFinalizer -> return InaccessibleReceiverKind.FINALIZER
                    declaration is CfirCallableDeclaration && declaration.status.isStatic -> return InaccessibleReceiverKind.STATIC_MEMBER
                    declaration is CfirValueParameter && declaration.containingDeclarationSymbol.cfir is CfirConstructor -> return InaccessibleReceiverKind.CLASS_HEADER
                }
                declaration = graph.declarationOwners.getValue(declaration)
            }
            return null
        }

        private fun sourceReceiverExpression(symbol: CfirThisOwnerSymbol<*>, type: ConeCangJieType, source: CjSourceElement?,
            barrier: InaccessibleReceiverKind?, implicit: Boolean): CfirExpression {
            val reference = buildThisReference { this.source = source; boundSymbol = symbol; isImplicit = implicit }
            return if (barrier == null) buildThisReceiverExpression {
                this.source = source; coneTypeOrNull = type; calleeReference = reference
            } else buildInaccessibleReceiverExpression {
                this.source = source; coneTypeOrNull = type; calleeReference = reference; kind = barrier
            }
        }

        /** Member 的显式对象和 native bare RefExpr 的隐式对象在同一发布点准备。 */
        private fun prepareInstanceDispatch(expression: CfirExpression, owner: CfirDeclaration, source: CjSourceElement?) {
            val access = expression as? CfirQualifiedAccessExpression ?: return
            val symbol = (access.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol ?: return
            val declaration = symbol.cfir as? CfirCallableDeclaration ?: return
            // 函数形参即使记录类内 callableId，仍是词法值，不是实例成员。
            if (declaration !is CfirFieldVariable && declaration !is CfirProperty &&
                declaration !is CfirNamedFunction && declaration !is CfirPropertyAccessor) return
            if (declaration is CfirConstructor || declaration is CfirEnumConstructor || declaration.status.isStatic || declaration.isLocal ||
                declaration.symbol.callableId.classId == null) return
            if (access.dispatchReceiver != null) return
            access.replaceDispatchReceiver(access.explicitReceiver ?: sourceInstanceReceiver(owner, source))
        }

        private fun argument(identity: CfirSourceExpressionIdentity): CfirFunctionArgument = expression(identity) as CfirFunctionArgument

        /** 正常正文按引用角色和真实目标种类分派，不在这里重做任何目标发现。 */
        private fun reference(value: CfirSourceExpressionPayload.Reference, source: CjSourceElement?,
            type: ConeCangJieType, owner: CfirDeclaration): CfirExpression {
            val target = value.target
            if (value.role == CfirSourceReferenceRole.THIS) {
                check(target !is CfirSourceReferenceTarget.Package) { "A package cannot own a this receiver" }
                return sourceInstanceReceiver(owner, source, implicit = false).also { it.replaceConeTypeOrNull(type) }
            }
            if (value.role == CfirSourceReferenceRole.SUPER && target is CfirSourceReferenceTarget.Declaration) {
                check(target.declaration is CfirClassLikeDeclaration)
                return buildSuperReceiverExpression {
                    this.source = source
                    coneTypeOrNull = type
                    dispatchReceiver = sourceInstanceReceiver(owner, source)
                    calleeReference = buildSuperReference { this.source = source; superTypeRef = typeRef(type) }
                }
            }
            return when (target) {
                is CfirSourceReferenceTarget.Declaration -> if (target.declaration is CfirClassLikeDeclaration) buildResolvedQualifier {
                    this.source = source
                    val symbol = (target.declaration as CfirClassLikeDeclaration).symbol
                    packageFqName = symbol.classId.packageFqName
                    relativeClassFqName = symbol.classId.relativeClassName
                    qualifierSymbol = symbol
                    referenceName = value.name
                    coneTypeOrNull = type
                    typeArguments += value.instantiatedTypes.map(::typeRef)
                } else buildNamedAccessExpression {
                    this.source = source
                    calleeReference = buildResolvedNamedReference { this.source = source; name = value.name; resolvedSymbol = target.declaration.symbol }
                    typeArguments += value.instantiatedTypes.map(::typeRef)
                }
                is CfirSourceReferenceTarget.Package -> buildResolvedQualifier {
                    this.source = source
                    packageFqName = target.packageName
                    referenceName = value.name
                    coneTypeOrNull = type
                }
                is CfirSourceReferenceTarget.Absent -> buildNamedAccessExpression {
                    this.source = source
                    calleeReference = buildErrorNamedReference {
                        this.source = source; name = value.name
                        diagnostic = ConeSimpleDiagnostic("Serialized reference has no declaration target: ${target.reason}", DiagnosticKind.DeserializationError)
                    }
                    typeArguments += value.instantiatedTypes.map(::typeRef)
                }
            }
        }

        private fun pattern(value: CfirSourcePattern): CfirPattern {
            state.patterns[value.identity]?.let { return it }
            check(state.patternsInProgress.add(value.identity)) { "SOURCE ordinary owning pattern cycle" }
            try {
                val type = typeRef(value.types.first())
                fun expressionSlot(ordinal: Int) = expression((value.slots[ordinal] as CfirSourceSlot.Expression).identity)
                val result = when (value.kind) {
                    CfirSourcePatternKind.WILDCARD -> buildWildcardPattern { semanticTypeRef = type }
                    CfirSourcePatternKind.VARIABLE -> buildBindingPattern {
                        semanticTypeRef = type
                        bindingVariable = (value.slots.single() as CfirSourceSlot.Declaration).declaration as CfirPatternBindingVariable
                        name = bindingVariable!!.name
                    }
                    CfirSourcePatternKind.CONSTANT -> buildConstPattern {
                        semanticTypeRef = type; expression = expressionSlot(0)
                        operatorCall = if (value.slots.size == 2) expressionSlot(1) as CfirCallExpression else null
                    }
                    CfirSourcePatternKind.TUPLE -> buildTuplePattern {
                        semanticTypeRef = type; elements += value.patterns.map(::pattern)
                    }
                    CfirSourcePatternKind.TYPE -> buildTypePattern {
                        semanticTypeRef = type; typeRef = type; nestedPattern = pattern(value.patterns.single())
                        needRuntimeTypeCheck = value.needRuntimeTypeCheck; matchBeforeRuntime = value.matchBeforeRuntime
                    }
                    CfirSourcePatternKind.ENUM -> buildEnumPattern {
                        semanticTypeRef = type; constructor = expressionSlot(0); arguments += value.patterns.map(::pattern)
                    }
                    CfirSourcePatternKind.EXCEPTION_TYPE -> buildCatchPattern {
                        semanticTypeRef = type; nestedPattern = pattern(value.patterns.single()); typeRefs += value.typeReferences
                    }
                    CfirSourcePatternKind.COMMAND_TYPE -> buildCommandTypePattern {
                        semanticTypeRef = type; nestedPattern = pattern(value.patterns.single()); typeRefs += value.typeReferences
                    }
                }
                result.replaceSource(source(value.importedPosition, value.identity))
                result.replaceConstantValues(value.constantValues)
                check(state.patterns.put(value.identity, result) == null)
                createdPatterns[value.identity] = value
                return result
            } finally { check(state.patternsInProgress.remove(value.identity)) }
        }

        private fun functionTarget(function: CfirCallableDeclaration): CfirFunctionTarget {
            val owner = function as CfirFunctionBodyOwner
            return state.functions.getOrPut(owner) {
                CfirFunctionTarget(isLambda = function is CfirAnonymousFunction && function.isLambda).also { it.bind(owner) }
            }
        }
    }

    private fun functionSymbol(declaration: CfirDeclaration): CfirFunctionSymbol<*> {
        check(declaration is CfirFunction) { "Array initializer must retain its actual function declaration" }
        return declaration.symbol
    }

    private fun block(expression: CfirExpression): CfirBlock = expression as CfirBlock
    private fun typeRef(type: ConeCangJieType): CfirTypeRef = buildResolvedTypeRef { coneType = type }

    private fun source(position: CfirSourceImportedPosition?, identity: Any): CjSourceElement? = position?.begin?.let {
        object : CjBinarySourceElement("CJO expression or pattern", it.filePath, identity, it) {}
    }
}
