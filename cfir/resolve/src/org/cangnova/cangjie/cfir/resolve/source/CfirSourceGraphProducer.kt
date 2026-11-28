package org.cangnova.cangjie.cfir.resolve.source

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirStringLiteralValueNormalizer
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.evaluatedInitializer
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.hasImplicitOrInferredReturnType
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.scopes.*
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.type.AbstractTypeChecker
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 官方 PerformDesugarAfterTypeCheck 的 SOURCE 产品边界。
 *
 * 本生产器只读取已完成 BODY 的类型和绑定，以独立 SOURCE 记录表达规范化，不修改 normal
 * CFIR、重新访问其 resolver 或重跑 CFG。实际 family 根/成员域与正式写出使用同一输入。
 * providers 在此之后冻结来源、声明和类型；CJO encoder 只消费完成图，不执行本生产过程。
 */
object CfirSourceGraphProducer {
    fun produce(files: Collection<CfirFile>, family: CfirCjmpFamilySnapshot? = null): CfirSourceGraph {
        require(files.isNotEmpty())
        return Context(files, family).produce()
    }

    @OptIn(CfirImplementationDetail::class)
    internal class Context(private val files: Collection<CfirFile>, private val family: CfirCjmpFamilySnapshot?) {
        val builder = CfirSourceGraph.Builder()
        private val scopeSessions = IdentityHashMap<CfirSession, ScopeSession>()
        private val declarationContexts = IdentityHashMap<CfirDeclaration, CfirSourceDeclarationContext>()
        private val visitedDeclarations = Collections.newSetFromMap(IdentityHashMap<CfirDeclaration, Boolean>())
        private val normalizedFunctions = Collections.newSetFromMap(IdentityHashMap<CfirFunction, Boolean>())
        private val initializerOwners = Collections.newSetFromMap(IdentityHashMap<CfirVariable, Boolean>())
        private val defaultOwners = Collections.newSetFromMap(IdentityHashMap<CfirValueParameter, Boolean>())
        private val captureKinds = IdentityHashMap<CfirFunction, Int>()
        private val functions = ArrayList<CfirFunction>()
        private val owners = IdentityHashMap<CfirDeclaration, CfirDeclaration?>()
        private val loops = IdentityHashMap<org.cangnova.cangjie.cfir.CfirIterationOwner, CfirSourceExpressionIdentity>()

        fun produce(): CfirSourceGraph {
            files.forEach { file -> file.declarations.forEach { indexDeclaration(it, null, file) } }
            family?.packageDeclarationsForExport()?.forEach { root ->
                if (root.cjoDeclarationOrigin == null && !declarationContexts.containsKey(root)) {
                    indexDeclaration(root, null, sourceFile(root))
                }
            }
            family?.packageDeclarationsForExport()?.forEach { root ->
                if (root is CfirClassLikeDeclaration || root is CfirExtend) family.membersFor(root).forEach { member ->
                    if (member.cjoDeclarationOrigin == null && !declarationContexts.containsKey(member)) {
                        indexDeclaration(member, root, sourceFile(root))
                    }
                }
            }
            functions.forEach { function ->
                val parent = declarationContextOf(owners[function])
                builder.functionContext(function, CfirSourceFunctionContext(parent, outerFunctionOf(owners[function])))
            }
            computeCaptureKinds()
            functions.forEach(::function)
            declarationContexts.keys.toList().filterIsInstance<CfirVariable>().forEach(::variable)
            declarationContexts.keys.toList().forEach(::annotationArguments)
            return builder.finish()
        }

        /** 注解的格式实参与正文共用同一 SOURCE 图；编码器不能另造 literal/call 子池。 */
        private fun annotationArguments(owner: CfirDeclaration) {
            val plans = owner.prepareAnnotationExportPlans(owner.moduleData.session.languageVersionSettings.languageVersion)
            builder.annotationPlans(owner, plans)
            plans.forEach { plan ->
                plan.arguments.forEach { argument ->
                    val value = argument.expression
                    val constant = value.cjmpAnnotationLiteralConstant()
                    if (constant != null && builder.existing(value) == null) {
                        val type = requireType(checkNotNull(value.coneTypeOrNull) {
                            "SOURCE annotation literal requires its completed type"
                        })
                        val id = builder.reserve(value)
                        builder.fill(id, record(owner, type, CfirSourceSpan.capture(value.source),
                            CfirSourceExpressionPayload.Literal(constant.kind,
                                if (constant.kind == CfirLiteralKind.UNIT) "()" else checkNotNull(constant.value),
                                constant.kind == CfirLiteralKind.JSTRING)))
                    } else if (plan.kind == CfirAnnotationExportKind.ANNOTATION) {
                        expression(value, owner)
                    }
                }
            }
            builder.annotations(owner, plans.map { plan ->
                CfirSourceAnnotation(plan.kind, plan.identifier, plan.arguments.map { argument ->
                    CfirSourceAnnotationArgument(argument.name, checkNotNull(builder.existing(argument.expression)))
                }, plan.target)
            })
        }

        /** 词法 owner 由拥有边登记；引用目标不通过此入口冒充新的定义。 */
        private fun indexDeclaration(declaration: CfirDeclaration, owner: CfirDeclaration?, file: CfirFile) {
            if (declaration.cjoDeclarationOrigin != null || !visitedDeclarations.add(declaration)) return
            owners[declaration] = owner
            val owningDeclaration = if (declaration is CfirNamedFunction && declaration.defaultParameterHelperInfo != null) {
                val helper = checkNotNull(declaration.defaultParameterHelperInfo)
                helper.owner.valueParameters[helper.parameterIndex]
            } else owner
            builder.declarationOwner(declaration, owningDeclaration)
            declarationContexts[declaration] = CfirSourceDeclarationContext(owner ?: declaration, file)
            if (declaration is CfirClassLikeDeclaration) {
                if (declaration is CfirClass && declaration.symbol.classId != StdlibClassIds.Object &&
                    declaration.symbol.classId != StdlibClassIds.Any
                ) {
                    check(declaration.superTypeRefs.any {
                        val type = requireType(checkNotNull(it.coneTypeOrNull)).fullyExpandedType(declaration.moduleData.session)
                        type is ConeClassLikeType && !type.isInterface
                    }) { "SOURCE class inheritance requires a superclass published by SUPER_TYPES" }
                }
                builder.inheritedTypes(declaration, declaration.superTypeRefs)
            }
            if (declaration is CfirFunction) functions += declaration
            val seen = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
            declaration.accept(object : CfirVisitorVoid() {
                override fun visitElement(element: CfirElement) {
                    if (element is CfirControlFlowGraphReference || !seen.add(element)) return
                    if (element is CfirDeclaration && element !== declaration) {
                        indexDeclaration(element, declaration, file)
                        return
                    }
                    when (element) {
                        is CfirArgumentList -> element.arguments.forEach { it.accept(this) }
                        is CfirValueParameter -> {
                            element.defaultValue?.accept(this)
                            element.defaultValueMetadata?.function?.let { indexDeclaration(it, owner, file) }
                        }
                        else -> element.acceptSourceChildren(this)
                    }
                }
            })
            if (declaration is CfirValueParameter) {
                declaration.defaultValueMetadata?.function?.let { helper ->
                    val sourceOwner = checkNotNull(helper.defaultParameterHelperInfo).owner
                    indexDeclaration(helper, owners[sourceOwner], file)
                }
            }
        }

        private fun sourceFile(declaration: CfirDeclaration): CfirFile = when (val symbol = declaration.symbol) {
            is CfirClassLikeSymbol<*> -> declaration.moduleData.session.cfirProvider.getCfirClassifierContainerFile(symbol)
            is CfirCallableSymbol<*> -> checkNotNull(declaration.moduleData.session.cfirProvider.getCfirCallableContainerFile(symbol))
            else -> error("SOURCE root has no registered file: $symbol")
        }

        fun context(declaration: CfirDeclaration): CfirSourceDeclarationContext = declarationContexts.getValue(declaration)

        /** initializer/参数不会建立新的函数语境；沿真实声明拥有索引取得词法容器。 */
        private fun declarationContextOf(owner: CfirDeclaration?): CfirDeclaration? {
            var current = owner
            while (current != null) {
                if (current is CfirFunction || current is CfirClassLikeDeclaration || current is CfirExtend || current is CfirProperty) return current
                check(owners.containsKey(current)) { "SOURCE declaration has no indexed lexical owner" }
                current = owners[current]
            }
            return null
        }

        private fun outerFunctionOf(owner: CfirDeclaration?): CfirFunction? {
            var current = owner
            while (current != null) {
                if (current is CfirFunction) return current
                check(owners.containsKey(current)) { "SOURCE function has no indexed lexical owner" }
                current = owners[current]
            }
            return null
        }

        fun function(function: CfirFunction): CfirSourceExpressionIdentity? {
            if (function.cjoDeclarationOrigin != null || function.body == null) return null
            if (!normalizedFunctions.add(function)) return builder.existing(checkNotNull(function.body))
            val body = checkNotNull(function.body)
            check(body !is CfirLazyBlock && function.returnTypeRef is CfirResolvedTypeRef) {
                "SOURCE function requires its completed signature and materialized body: ${function.symbol}"
            }
            requireType(checkNotNull(body.coneTypeOrNull))
            function.valueParameters.forEach { parameter ->
                requireType(checkNotNull(parameter.returnTypeRef.coneTypeOrNull))
                parameter.defaultValue?.let { value ->
                    if (defaultOwners.add(parameter)) builder.default(parameter, expression(value, parameter))
                }
                parameter.defaultValueMetadata?.function?.let(::function)
            }
            val originalTail = body.statements.lastOrNull()
            val insertUnit = originalTail == null || originalTail is CfirDeclaration || function is CfirConstructor ||
                (!function.hasImplicitOrInferredReturnType() && function !is CfirAnonymousFunction &&
                    function.returnTypeRef.coneTypeOrNull == ConePrimitiveType.UNIT &&
                    originalTail !is CfirReturnExpression &&
                    !(originalTail is CfirLiteralExpression && originalTail.kind == CfirLiteralKind.UNIT))
            val wrapTail = !insertUnit && originalTail is CfirExpression && originalTail !is CfirReturnExpression
            val id = block(body, function, insertUnit = insertUnit, wrapTail = wrapTail)
            builder.body(function, id)
            builder.alias(function, id)
            return id
        }

        fun functionBody(function: CfirFunction): CfirSourceFunctionBody = CfirSourceFunctionBody(
            owner = function,
            parameters = function.valueParameters,
            signature = CfirSourceCallableSignature.Resolved(requireType(checkNotNull(function.returnTypeRef.coneTypeOrNull))),
            body = checkNotNull(function(function)),
            captureKind = captureKinds.getValue(function),
            always = false, // 固定 v113 ASTWriter::SaveFuncBody 的源码产品 always=false。
        )

        fun variable(variable: CfirVariable) {
            if (variable.cjoDeclarationOrigin != null || !initializerOwners.add(variable)) return
            if (variable is CfirPatternVariable && variable.pattern.simpleVariableBindingOrNull() == null) {
                builder.declarationPattern(variable, pattern(variable.pattern, variable))
            }
            val initializer = variable.initializer ?: return
            val evaluated = variable.evaluatedInitializer as? org.cangnova.cangjie.cfir.CfirEvaluatorResult.Evaluated
            (evaluated?.result as? CfirConstantValueExpression)?.constantValue?.let { builder.constantValue(variable, it) }
            builder.initializer(variable, expression(initializer, variable))
            if (variable is CfirPatternVariable) variable.pattern.simpleVariableBindingOrNull()?.let { binding ->
                if (initializerOwners.add(binding)) builder.initializer(binding, expression(initializer, variable))
            }
        }

        fun block(
            block: CfirBlock,
            owner: CfirDeclaration,
            insertUnit: Boolean = false,
            wrapTail: Boolean = false,
            unsafe: Boolean = false,
        ): CfirSourceExpressionIdentity {
            builder.existing(block)?.let { return it }
            val id = builder.reserve(block)
            val statements = block.statements.mapIndexed { index, statement ->
                when (statement) {
                    is CfirDeclaration -> {
                        if (statement is CfirFunction) function(statement)
                        if (statement is CfirVariable) variable(statement)
                        CfirSourceSlot.Declaration(if (statement is CfirPatternVariable) statement.pattern.simpleVariableBindingOrNull() ?: statement else statement)
                    }
                    is CfirExpression -> {
                        val value = expression(statement, owner)
                        CfirSourceSlot.Expression(if (wrapTail && index == block.statements.lastIndex) {
                            check(owner is CfirFunction)
                            generated(owner, ConePrimitiveType.NOTHING, CfirSourceExpressionPayload.Return(value, owner),
                                CfirSourceSpan.capture(statement.source))
                        } else value)
                    }
                    else -> error("SOURCE block has a non-statement owned node")
                }
            }.toMutableList()
            if (insertUnit) {
                check(owner is CfirFunction)
                val unit = unit(owner)
                val returnSpan = block.source?.let {
                    val offset = (it.endOffset - 1).coerceAtLeast(it.startOffset)
                    CfirSourceSpan(offset, offset, it.kind)
                }
                statements += CfirSourceSlot.Expression(generated(owner, ConePrimitiveType.NOTHING,
                    CfirSourceExpressionPayload.Return(unit, owner), returnSpan))
            }
            // CFIR lambda的body结果类型可保留返回值类型；SOURCE的实际Return尾部属于Nothing。
            val type = if (wrapTail || !insertUnit && owner is CfirFunction && block.statements.lastOrNull() is CfirReturnExpression)
                ConePrimitiveType.NOTHING else requireType(checkNotNull(block.coneTypeOrNull))
            builder.fill(id, record(owner, type, CfirSourceSpan.capture(block.source),
                CfirSourceExpressionPayload.Block(statements, unsafe)))
            return id
        }

        fun expression(expression: CfirExpression, owner: CfirDeclaration): CfirSourceExpressionIdentity {
            builder.existing(expression)?.let { return it }
            if (expression is CfirBlock) return block(expression, owner)
            if (expression is CfirUnsafeExpression) {
                val block = expression.body as? CfirBlock ?: error("SOURCE unsafe requires its real block")
                return block(block, owner, unsafe = true).also { builder.alias(expression, it) }
            }
            if (expression is CfirNamedArgumentExpression) {
                return expression(expression.expression, owner).also { builder.alias(expression, it) }
            }
            if (expression is CfirInoutArgumentExpression) {
                return expression(expression.expression, owner).also { builder.alias(expression, it) }
            }
            val id = builder.reserve(expression)
            if (expression is CfirLoopExpression) bindLoop(expression, id)
            val type = requireType(checkNotNull(expression.coneTypeOrNull) {
                "SOURCE expression has no completed type: ${expression::class.simpleName}"
            })
            val payload = when (expression) {
                is CfirLiteralExpression -> literalPayload(expression)
                is CfirFunctionCall -> call(expression, owner)
                is CfirResolvedQualifier -> {
                    val target = expression.qualifierSymbol?.let { CfirSourceReferenceTarget.Declaration(it.cfir) }
                        ?: CfirSourceReferenceTarget.Package(expression.packageFqName)
                    val reference = CfirSourceExpressionPayload.Reference(expression.referenceName, target,
                        expression.typeArguments.map(::typeOf), CfirReferenceParentType.Absent)
                    expression.explicitParent?.let { CfirSourceExpressionPayload.Member(expression(it, owner), reference) } ?: reference
                }
                is CfirSuperReceiverExpression -> {
                    val parentType = typeOf(expression.calleeReference.superTypeRef)
                    val target = checkNotNull((parentType as? ConeLookupTagBasedType)?.lookupTag?.toSymbol(owner.moduleData.session))
                    CfirSourceExpressionPayload.Reference(Name.identifier("super"), CfirSourceReferenceTarget.Declaration(target.cfir),
                        expression.typeArguments.map(::typeOf), CfirReferenceParentType.Absent, role = CfirSourceReferenceRole.SUPER)
                }
                is CfirQualifiedAccessExpression -> access(expression, owner)
                is CfirThisReceiverExpression -> {
                    val receiverOwner = checkNotNull(expression.calleeReference.boundSymbol).cfir
                    // Native InferTypeOfThis：词法 receiver owner 与 FullId 目标正交。
                    // 名义 extend 的目标是完成 receiver type 的真实类型声明；builtin 不设置目标。
                    val target = when (receiverOwner) {
                        is CfirExtend -> if (type.isBuiltin) {
                            CfirSourceReferenceTarget.Absent(CfirSourceReferenceAbsence.INVALID_SENTINEL)
                        } else {
                            val receiverType = type as? ConeLookupTagBasedType
                                ?: error("Nominal SOURCE this requires its completed classifier type")
                            val classifier = checkNotNull(receiverType.lookupTag.toSymbol(receiverOwner.moduleData.session))
                            check(classifier.cfir is CfirClassLikeDeclaration)
                            CfirSourceReferenceTarget.Declaration(classifier.cfir)
                        }
                        is CfirClassLikeDeclaration -> CfirSourceReferenceTarget.Declaration(receiverOwner)
                        else -> error("SOURCE this has no class-like or extend receiver owner")
                    }
                    CfirSourceExpressionPayload.Reference(Name.identifier("this"), target, emptyList(),
                        CfirReferenceParentType.Absent, role = CfirSourceReferenceRole.THIS)
                }
                is CfirReturnExpression -> CfirSourceExpressionPayload.Return(expression(expression.result, owner),
                    expression.target.let {
                        check(it.isBound)
                        val function = it.labeledElement
                        check(function is CfirCallableDeclaration) { "SOURCE Return requires its actual callable F owner" }
                        function
                    })
                is CfirBreakExpression -> CfirSourceExpressionPayload.Jump(true,
                    loopIdentity(expression.target.let { check(it.isBound); it.labeledElement }, owner))
                is CfirContinueExpression -> CfirSourceExpressionPayload.Jump(false,
                    loopIdentity(expression.target.let { check(it.isBound); it.labeledElement }, owner))
                is CfirForInExpression -> CfirSourceForInCanonicalizer(this).produce(expression, owner, id)
                is CfirLoopExpression -> CfirSourceExpressionPayload.Loop(expression(expression.condition, owner),
                    block(expression.body, owner), expression.isDoWhile)
                is CfirIfExpression -> CfirSourceExpressionPayload.Conditional(expression(expression.condition, owner),
                    block(expression.thenBranch, owner), expression.elseBranch?.let { expression(it, owner) })
                is CfirAssignment -> assignment(expression, owner)
                is CfirIncrementDecrementExpression -> {
                    check(!expression.isPrefix && type == ConePrimitiveType.UNIT)
                    val operandType = requireType(checkNotNull(expression.expression.coneTypeOrNull))
                    check((operandType as? ConePrimitiveType)?.kind?.isInteger == true)
                    CfirSourceExpressionPayload.Assignment(expression(expression.expression, owner),
                        literal(owner, operandType, CfirLiteralKind.INT, "1"),
                        when (expression.operationName) {
                            OperatorNameConventions.INC -> CfirSourceOperator.ADD_ASSIGN
                            OperatorNameConventions.DEC -> CfirSourceOperator.SUBTRACT_ASSIGN
                            else -> error("Invalid resolved increment operation")
                        }, true)
                }
                is CfirBinaryOp -> CfirSourceExpressionPayload.Binary(expression(expression.left, owner),
                    expression(expression.right, owner), sourceOperator(expression.kind.symbol))
                is CfirComparisonExpression -> CfirSourceExpressionPayload.Binary(expression(expression.left, owner),
                    expression(expression.right, owner), sourceOperator(expression.operation.symbol))
                is CfirTupleLiteral -> CfirSourceExpressionPayload.Tuple(expression.elements.map { expression(it, owner) })
                is CfirArrayLiteral -> CfirSourceExpressionPayload.ArrayLiteral(
                    expression.elements.map { expression(it, owner) }, if (type is ConeVArrayType) null else arrayLiteralConstructor(type, owner))
                is CfirSubscriptExpression -> {
                    val receiverType = typeOf(expression.receiver)
                    if (receiverType is ConeTupleType) CfirSourceExpressionPayload.Subscript(
                        expression(expression.receiver, owner), expression.indices.map { expression(it, owner) }, true)
                    else if (receiverType is ConeVArrayType || receiverType.classId == StdlibClassIds.RawArray) {
                        CfirSourceExpressionPayload.Subscript(expression(expression.receiver, owner), expression.indices.map { expression(it, owner) }, false)
                    } else checkNotNull(expression.resolvedGetCall) { "SOURCE subscript requires its completed get call" }
                        .let { call(it, owner) }
                }
                is CfirTypeConversion -> CfirSourceExpressionPayload.TypeConversion(expression(expression.argument, owner))
                is CfirTypeOperator -> CfirSourceTypeOperationCanonicalizer(this).produce(expression, owner)
                is CfirRangeExpression -> CfirSourceForInCanonicalizer(this).range(expression, owner)
                is CfirAnonymousFunctionExpression -> CfirSourceExpressionPayload.Lambda(
                    functionBody(expression.anonymousFunction), expression.anonymousFunction.annotationInfo?.isMockSupported == true)
                is CfirMatchExpression -> match(expression, owner)
                is CfirLetPatternExpression -> CfirSourceExpressionPayload.LetPatternDestructor(
                    expression(expression.initializer, owner), alternatives(expression.pattern, owner))
                is CfirTryExpression -> CfirSourceTryCanonicalizer(this).produce(expression, owner)
                is CfirThrowExpression -> CfirSourceExpressionPayload.Throw(expression(expression.exception, owner))
                is CfirPerformExpression -> CfirSourceExpressionPayload.Perform(expression(expression.expression, owner))
                is CfirSpawnExpression -> CfirSourceSpawnCanonicalizer(this).produce(expression, owner)
                is CfirSmartCastExpression -> {
                    val original = expression(expression.originalExpression, owner)
                    builder.record(original).payload
                }
                is CfirConstantValueExpression -> constant(expression, owner)
                is CfirOptionalExpression, is CfirOptionalChainExpression,
                is CfirStringInterpolation, is CfirSynchronizedExpression ->
                    CfirSourceAdditionalCanonicalizer(this).produce(expression, owner)
                is CfirLazyExpression, is CfirLazyBlock, is CfirErrorExpression ->
                    error("SOURCE production requires a valid completed expression: ${expression::class.simpleName}")
                else -> error("CFIR expression has no native SOURCE production: ${expression::class.qualifiedName}")
            }
            builder.fill(id, record(owner, type, CfirSourceSpan.capture(expression.source), payload))
            return id
        }

        fun bindLoop(loop: org.cangnova.cangjie.cfir.CfirIterationOwner, identity: CfirSourceExpressionIdentity) {
            check(!loops.containsKey(loop))
            loops[loop] = identity
        }

        private fun loopIdentity(loop: org.cangnova.cangjie.cfir.CfirIterationOwner, owner: CfirDeclaration): CfirSourceExpressionIdentity {
            loops[loop]?.let { return it }
            expression(loop as CfirExpression, owner)
            return loops.getValue(loop)
        }

        private fun assignment(assignment: CfirAssignment, owner: CfirDeclaration): CfirSourceExpressionPayload {
            // 写下标的 canonical 根是正常完成的 set call；写目标不因此变为一次 get 求值。
            (assignment.lValue as? CfirSubscriptExpression)?.resolvedSetCall?.let { return call(it, owner) }
            val right = assignment.rValue
            val builtin = (right as? CfirFunctionCall)?.calleeReference as? CfirResolvedBuiltinOperatorReference
            val operation = assignment.augmentedOperation
            val value = if (operation != null && builtin != null) {
                check(builtin.signature.operation.nameInCfir == operation)
                check(right.argumentList.arguments.size == 1)
                right.argumentList.arguments.single()
            } else right
            return CfirSourceExpressionPayload.Assignment(expression(assignment.lValue, owner), expression(value, owner),
                operation?.let(::compoundOperator) ?: CfirSourceOperator.ASSIGN, operation != null)
        }

        private fun access(access: CfirQualifiedAccessExpression, owner: CfirDeclaration): CfirSourceExpressionPayload {
            val reference = resolvedReference(access.calleeReference, access.typeArguments.map { typeOf(it) })
            val receiver = referenceSyntaxReceiver(access)
            return if (receiver == null) reference else CfirSourceExpressionPayload.Member(expression(receiver, owner), reference)
        }

        /** 原生裸 RefExpr 不写隐式 this operand；物理对象由正式拥有/词法环境保存与恢复。 */
        private fun referenceSyntaxReceiver(access: CfirQualifiedAccessExpression): CfirExpression? {
            access.explicitReceiver?.let { return it }
            val implicit = access.dispatchReceiver?.unwrapSmartcastExpression()
            check(implicit == null || implicit is CfirThisReceiverExpression && implicit.calleeReference.isImplicit) {
                "SOURCE bare reference requires a pure implicit instance or a non-instance role; an independent receiver needs explicit syntax"
            }
            return null
        }

        private fun call(call: CfirFunctionCall, owner: CfirDeclaration): CfirSourceExpressionPayload {
            val builtin = call.calleeReference as? CfirResolvedBuiltinOperatorReference
            if (builtin != null) {
                val operands = listOf(checkNotNull(call.explicitReceiver ?: call.dispatchReceiver)) + call.argumentList.arguments
                check(operands.size == builtin.signature.parameterKinds.size + 1)
                val operation = builtin.signature.operation
                return if (operation.parameterCount == 0) CfirSourceExpressionPayload.Unary(
                    expression(operands.single(), owner), builtinOperator(operation))
                else CfirSourceExpressionPayload.Binary(expression(operands[0], owner), expression(operands[1], owner), builtinOperator(operation))
            }
            val reference = call.calleeReference as? CfirResolvedNamedReference
                ?: error("SOURCE call requires its completed callee binding")
            val target = reference.resolvedSymbol.cfir
            val arguments = call.argumentList.arguments.map { argument(it, owner) }
            when (target.origin) {
                CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor -> return CfirSourceExpressionPayload.Array(
                    arguments, CfirSourceAdditionalCanonicalizer(this).arrayInitializer(call, owner), typeOf(call) is ConeVArrayType)
                CfirDeclarationOrigin.Synthetic.BuiltinPointerConstructor -> {
                    check(arguments.size <= 1)
                    return CfirSourceExpressionPayload.Pointer(arguments.singleOrNull())
                }
                else -> Unit
            }
            val signature = appliedSignature(call, target)
            val sourceReference = resolvedReference(reference, call.typeArguments.map { typeOf(it) })
            val receiver = call.explicitReceiver ?: call.dispatchReceiver
            // implicit invoke 的 receiver 是函数值本身，不能再造 Member(value, value)。
            // 有真实 property receiver 时沿该完成 access 保留原 Member/Reference 角色。
            val receiverAccess = receiver as? CfirQualifiedAccessExpression
            val valueReceiver = target is CfirVariable &&
                (receiverAccess?.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol === reference.resolvedSymbol
            val syntaxReceiver = if (valueReceiver) null else referenceSyntaxReceiver(call)
            val basePayload = when {
                valueReceiver -> access(checkNotNull(receiverAccess), owner)
                syntaxReceiver == null -> sourceReference
                else -> CfirSourceExpressionPayload.Member(expression(syntaxReceiver, owner), sourceReference)
            }
            val base = generated(owner, signature, basePayload, CfirSourceSpan.capture(reference.source))
            val kind = when {
                (call.argumentList as? CfirResolvedArgumentList)?.usesCangjieVariadicCall == true -> CfirSourceCallKind.VARIADIC
                target is CfirVariable && signature.isCFunc -> CfirSourceCallKind.FUNCTION_POINTER
                target is CfirVariable -> CfirSourceCallKind.STRUCT_CREATION
                call.origin.isSuperConstructorDelegation -> CfirSourceCallKind.SUPER
                target is CfirConstructor -> CfirSourceCallKind.OBJECT_CREATION
                call.origin == CfirFunctionCallOrigin.CompilerCoreIntrinsic || call.origin == CfirFunctionCallOrigin.MockIntrinsic -> CfirSourceCallKind.INTRINSIC
                else -> CfirSourceCallKind.DECLARED
            }
            return CfirSourceExpressionPayload.Call(base, arguments, kind, call.sourceCallHasSideEffect)
        }

        private fun appliedSignature(call: CfirFunctionCall, target: CfirDeclaration): ConeFunctionType {
            val applied = call.calleeReference as? CfirResolvedAppliedCallableReference
            val functionValueType = if (target is CfirVariable) {
                checkNotNull((call.dispatchReceiver ?: call.explicitReceiver)?.coneTypeOrNull as? ConeFunctionType) {
                    "SOURCE function-value invocation requires its completed value receiver type"
                }
            } else null
            if (applied?.isFunctionValue == true) {
                return checkNotNull((target as? CfirVariable)?.returnTypeRef?.coneTypeOrNull as? ConeFunctionType)
            }
            if (applied?.substitutedReturnType != null) return ConeFunctionType(applied.substitutedParameterTypes,
                checkNotNull(applied.substitutedReturnType), isCFunc = functionValueType?.isCFunc == true || (target as? CfirFunction)?.interopInfo?.resolvedAbi?.isCFunction == true,
                isClosureType = false, hasVariableLenArg = functionValueType?.hasVariableLenArg == true || (target as? CfirFunction)?.hasVariableLenArg == true)
            return when (target) {
                is CfirFunction -> ConeFunctionType(target.valueParameters.map { typeOf(it.returnTypeRef) }, typeOf(target.returnTypeRef),
                    isCFunc = target.interopInfo?.resolvedAbi?.isCFunction == true, isClosureType = false, hasVariableLenArg = target.hasVariableLenArg)
                is CfirEnumConstructor -> ConeFunctionType(target.valueParameters.map { typeOf(it.returnTypeRef) }, typeOf(target.returnTypeRef))
                is CfirVariable -> checkNotNull(typeOf(target.returnTypeRef) as? ConeFunctionType)
                else -> error("SOURCE call has no completed callable signature: ${target.symbol}")
            }
        }

        fun resolvedReference(reference: CfirReference, instantiatedTypes: List<ConeCangJieType>): CfirSourceExpressionPayload.Reference {
            val resolved = reference as? CfirResolvedNamedReference ?: error("SOURCE reference is not resolved")
            check(resolved.resolvedSymbol.isBound)
            val parent = (resolved as? CfirResolvedAppliedCallableReference)?.matchedParentType ?: CfirReferenceParentType.Absent
            if (parent is CfirReferenceParentType.Instantiated) requireType(parent.type)
            return CfirSourceExpressionPayload.Reference(resolved.name, CfirSourceReferenceTarget.Declaration(resolved.resolvedSymbol.cfir), instantiatedTypes,
                parent)
        }

        fun argument(argument: CfirExpression, owner: CfirDeclaration): CfirSourceExpressionIdentity {
            var value = argument
            var inout = false
            while (true) when (value) {
                is CfirNamedArgumentExpression -> value = value.expression
                is CfirInoutArgumentExpression -> { inout = true; value = value.expression }
                else -> break
            }
            return generated(owner, typeOf(value), CfirSourceExpressionPayload.Argument(expression(value, owner), false, inout),
                CfirSourceSpan.capture(argument.source))
        }

        private fun match(match: CfirMatchExpression, owner: CfirDeclaration): CfirSourceExpressionPayload.Match {
            val selector = match.subject
            val cases = match.branches.map { branch ->
                val payload = if (selector != null) CfirSourceExpressionPayload.MatchCase(
                    alternatives(branch.pattern, owner), branch.guard?.let { expression(it, owner) }, block(branch.body, owner))
                else {
                    val condition = when (val pattern = branch.pattern) {
                        is CfirExpressionPattern -> expression(pattern.expression, owner)
                        is CfirWildcardPattern -> generated(owner, ConePrimitiveType.BOOLEAN, CfirSourceExpressionPayload.Wildcard,
                            CfirSourceSpan.capture(pattern.source))
                        else -> error("Selectorless SOURCE match requires its real condition pattern")
                    }
                    CfirSourceExpressionPayload.MatchCaseOther(condition, block(branch.body, owner))
                }
                val id = builder.reserve(branch)
                builder.fill(id, record(owner, typeOf(branch.body), CfirSourceSpan.capture(branch.source), payload))
                id
            }
            return CfirSourceExpressionPayload.Match(selector?.let { expression(it, owner) }, cases)
        }

        fun alternatives(pattern: CfirPattern, owner: CfirDeclaration): List<CfirSourcePattern> =
            if (pattern is CfirOrPattern) pattern.alternatives.map { pattern(it, owner) } else listOf(pattern(pattern, owner))

        /** 每个 P 只读取正常检查发布的自身类型；外层声明、selector 和父 P 不替代这个保存槽。 */
        fun pattern(pattern: CfirPattern, owner: CfirDeclaration): CfirSourcePattern {
            val ownType = typeOf(checkNotNull(pattern.semanticTypeRef) { "SOURCE requires the pattern's completed semantic type" })
            return when (pattern) {
            is CfirWildcardPattern -> wildcardPattern(ownType)
            is CfirBindingPattern -> {
                val binding = checkNotNull(pattern.bindingVariable)
                CfirSourcePattern(CfirSourcePatternKind.VARIABLE, listOf(ownType),
                    listOf(CfirSourceSlot.Declaration(binding)), pattern.nestedPattern?.let { listOf(pattern(it, owner)) }.orEmpty())
            }
            is CfirVarOrEnumPattern -> {
                val binding = checkNotNull(pattern.bindingVariable) { "SOURCE enum pattern must complete its enum production" }
                variablePattern(binding, ownType)
            }
            is CfirTypePattern -> {
                CfirSourcePattern(CfirSourcePatternKind.TYPE, listOf(ownType), emptyList(),
                    listOf(pattern(pattern.nestedPattern, owner)),
                    needRuntimeTypeCheck = pattern.needRuntimeTypeCheck,
                    matchBeforeRuntime = pattern.matchBeforeRuntime)
            }
            is CfirConstPattern -> CfirSourcePattern(CfirSourcePatternKind.CONSTANT, listOf(ownType),
                listOf(CfirSourceSlot.Expression(expression(pattern.expression, owner))) +
                    pattern.operatorCall?.let { listOf(CfirSourceSlot.Expression(expression(it, owner))) }.orEmpty(), emptyList())
            is CfirExpressionPattern -> CfirSourcePattern(CfirSourcePatternKind.CONSTANT, listOf(ownType),
                listOf(CfirSourceSlot.Expression(expression(pattern.expression, owner))), emptyList())
            is CfirTuplePattern -> {
                val tuple = ownType.fullyExpandedType(owner.moduleData.session) as? ConeTupleType ?: error("SOURCE tuple pattern requires its completed tuple type")
                check(pattern.elements.size == tuple.elementTypes.size)
                CfirSourcePattern(CfirSourcePatternKind.TUPLE, listOf(ownType), emptyList(), pattern.elements.map { pattern(it, owner) })
            }
            is CfirEnumPattern -> {
                val constructor = (pattern.constructorReference as? CfirResolvedNamedReference)?.resolvedSymbol?.cfir as? CfirEnumConstructor
                    ?: error("SOURCE enum pattern requires its completed constructor")
                val signature = enumConstructorSignature(constructor, ownType)
                val ref = expression(pattern.constructor, owner)
                check(pattern.arguments.size == signature.parameterTypes.size)
                CfirSourcePattern(CfirSourcePatternKind.ENUM, listOf(ownType), listOf(CfirSourceSlot.Expression(ref)),
                    pattern.arguments.map { pattern(it, owner) })
            }
            is CfirOrPattern -> error("SOURCE alternatives must retain their individual pattern records")
            is CfirCatchPattern -> CfirSourcePattern(CfirSourcePatternKind.EXCEPTION_TYPE, listOf(ownType),
                emptyList(), listOf(pattern(pattern.nestedPattern, owner)), typeReferences = pattern.typeRefs)
            is CfirCommandTypePattern -> CfirSourcePattern(CfirSourcePatternKind.COMMAND_TYPE, listOf(ownType),
                emptyList(), listOf(pattern(pattern.nestedPattern, owner)), typeReferences = pattern.typeRefs)
            }.copy(span = CfirSourceSpan.capture(pattern.source), constantValues = pattern.constantValues,
                contextExpression = pattern.contextExpression?.let { expression(it, owner) })
        }

        fun catchPattern(pattern: CfirPattern, owner: CfirDeclaration): CfirSourcePattern =
            pattern(pattern, owner)

        fun wildcardPattern(type: ConeCangJieType): CfirSourcePattern =
            CfirSourcePattern(CfirSourcePatternKind.WILDCARD, listOf(type), emptyList(), emptyList())

        fun variablePattern(variable: CfirDeclaration, type: ConeCangJieType): CfirSourcePattern =
            CfirSourcePattern(CfirSourcePatternKind.VARIABLE, listOf(type), listOf(CfirSourceSlot.Declaration(variable)), emptyList())

        fun typePattern(sourceType: ConeCangJieType, targetType: ConeCangJieType, inner: CfirSourcePattern, owner: CfirDeclaration): CfirSourcePattern {
            val session = owner.moduleData.session
            val before = AbstractTypeChecker.isSubtypeOf(session.typeContext, sourceType, targetType) == true
            return CfirSourcePattern(CfirSourcePatternKind.TYPE, listOf(targetType), emptyList(), listOf(inner),
                needRuntimeTypeCheck = !before && needsRuntimeTypeCheck(sourceType, targetType, session), matchBeforeRuntime = before)
        }

        private fun needsRuntimeTypeCheck(source: ConeCangJieType, target: ConeCangJieType, session: CfirSession): Boolean {
            fun final(type: ConeCangJieType): Boolean = when (type) {
                is ConePrimitiveType, is ConeStructType, is ConeEnumType, is ConePointerType, is ConeCStringType, is ConeVArrayType -> true
                is ConeClassLikeType -> (type.toSymbol(session)?.cfir as? CfirClass)?.status?.let { !it.isAbstract && !it.isOpen } == true ||
                    type.classId == StdlibClassIds.RawArray
                else -> false
            }
            val sourceDeclaration = source.toSymbol(session)?.cfir
            val targetDeclaration = target.toSymbol(session)?.cfir
            if (final(source) && final(target) && (sourceDeclaration != null || targetDeclaration != null)) return sourceDeclaration === targetDeclaration
            return source is ConeClassLikeType && target is ConeClassLikeType ||
                source.contains { it is ConeTypeParameterType } || target.contains { it is ConeTypeParameterType } ||
                AbstractTypeChecker.isSubtypeOf(session.typeContext, source, target) == true ||
                AbstractTypeChecker.isSubtypeOf(session.typeContext, target, source) == true
        }

        fun enumConstructor(type: ConeCangJieType, name: String, owner: CfirDeclaration): CfirEnumConstructor {
            val declaration = type.toSymbol(owner.moduleData.session)?.cfir as? CfirEnum
                ?: error("SOURCE constructor requires its actual enum type")
            return declaration.declarations.filterIsInstance<CfirEnumConstructor>().single { it.name.asString() == name }
        }

        fun enumConstructorSignature(constructor: CfirEnumConstructor, type: ConeCangJieType): ConeFunctionType {
            val session = constructor.moduleData.session
            val enum = type.toSymbol(session)?.cfir as? CfirEnum ?: error("SOURCE enum has no declaration")
            val arguments = (type as ConeLookupTagBasedType).typeArguments.map { it.type }
            val substitutor = substitutorByMap(enum.typeParameters.zip(arguments).associate { (parameter, argument) -> parameter.symbol to argument }, session)
            return ConeFunctionType(constructor.valueParameters.map { substitutor.substituteOrSelf(typeOf(it.returnTypeRef)) }, type)
        }

        fun enumReference(type: ConeCangJieType, name: String, owner: CfirDeclaration): CfirSourceExpressionIdentity {
            val constructor = enumConstructor(type, name, owner)
            val arguments = (type as ConeLookupTagBasedType).typeArguments.map { it.type }
            return generated(owner, if (constructor.valueParameters.isEmpty()) type else enumConstructorSignature(constructor, type),
                CfirSourceExpressionPayload.Reference(constructor.name, CfirSourceReferenceTarget.Declaration(constructor), arguments, CfirReferenceParentType.Absent))
        }

        fun enumCall(type: ConeCangJieType, name: String, values: List<CfirSourceExpressionIdentity>, owner: CfirDeclaration): CfirSourceExpressionIdentity {
            val constructor = enumConstructor(type, name, owner)
            val signature = enumConstructorSignature(constructor, type)
            check(values.size == signature.parameterTypes.size)
            val arguments = values.zip(signature.parameterTypes).map { (value, parameterType) ->
                generated(owner, parameterType, CfirSourceExpressionPayload.Argument(value, false, false))
            }
            return generated(owner, type, CfirSourceExpressionPayload.Call(enumReference(type, name, owner), arguments, CfirSourceCallKind.DECLARED, false))
        }

        fun memberCall(receiver: CfirSourceExpressionIdentity, receiverType: ConeCangJieType, name: String,
            arguments: List<CfirSourceExpressionIdentity>, owner: CfirDeclaration): CfirSourceExpressionIdentity {
            val function = member(receiverType, Name.identifier(name), arguments.size, owner, arguments.map { builder.record(it).type })
            val parameterTypes = function.valueParameters.map { typeOf(it.returnTypeRef) }
            val returnType = typeOf(function.returnTypeRef)
            val reference = CfirSourceExpressionPayload.Reference(function.name, CfirSourceReferenceTarget.Declaration(function), emptyList(), CfirReferenceParentType.Absent)
            val base = generated(owner, ConeFunctionType(parameterTypes, returnType), CfirSourceExpressionPayload.Member(receiver, reference))
            val wrapped = arguments.zip(parameterTypes).map { (value, parameterType) ->
                generated(owner, parameterType, CfirSourceExpressionPayload.Argument(value, false, false))
            }
            return generated(owner, returnType, CfirSourceExpressionPayload.Call(base, wrapped, CfirSourceCallKind.DECLARED, false))
        }

        fun member(type: ConeCangJieType, name: Name, parameterCount: Int, owner: CfirDeclaration,
            argumentTypes: List<ConeCangJieType>): CfirNamedFunction {
            val session = owner.moduleData.session
            val symbol = type.toSymbol(session) as? CfirClassLikeSymbol<*> ?: error("SOURCE member has no actual receiver declaration")
            val scope = session.cangjieScopeProvider.getUseSiteMemberScope(classSymbol = symbol, useSiteSession = session,
                scopeSession = scopeSessions.getOrPut(session) { ScopeSession() }, ownerType = type, dispatchReceiverType = type,
                scopeKind = CfirClassMemberScopeKind.USE_SITE, memberRequiredPhase = CfirResolvePhase.STATUS)
            val substituted = CfirClassSubstitutionScope(session, scope, type)
            val candidates = buildList { substituted.processFunctionsByName(name) { add(it.cfir) } }
            return candidates.single { function ->
                function.valueParameters.size == parameterCount && function.valueParameters.zip(argumentTypes).all { (parameter, argument) ->
                    AbstractTypeChecker.isSubtypeOf(session.typeContext, argument, typeOf(parameter.returnTypeRef)) == true
                }
            }
        }

        fun constructor(type: ConeCangJieType, parameterCount: Int, owner: CfirDeclaration,
            accepts: (CfirConstructor) -> Boolean = { true }): CfirConstructor {
            val session = owner.moduleData.session
            val symbol = type.toSymbol(session) as? CfirClassLikeSymbol<*> ?: error("SOURCE constructor has no actual class declaration")
            val scope = session.cangjieScopeProvider.getUseSiteMemberScope(classSymbol = symbol, useSiteSession = session,
                scopeSession = scopeSessions.getOrPut(session) { ScopeSession() }, ownerType = type, dispatchReceiverType = type,
                scopeKind = CfirClassMemberScopeKind.USE_SITE, memberRequiredPhase = CfirResolvePhase.STATUS)
            val substituted = CfirClassSubstitutionScope(session, scope, type)
            return buildList { substituted.processDeclaredConstructors { add(it.cfir) } }
                .single { it.valueParameters.size == parameterCount && accepts(it) }
        }

        private fun arrayLiteralConstructor(type: ConeCangJieType, owner: CfirDeclaration): CfirConstructor = constructor(type, 3, owner) {
            val first = typeOf(it.valueParameters.first().returnTypeRef)
            first is ConeClassLikeType && first.classId == StdlibClassIds.RawArray
        }

        fun constructorCall(type: ConeCangJieType, values: List<CfirSourceExpressionIdentity>, owner: CfirDeclaration): CfirSourceExpressionPayload.Call {
            val constructor = constructor(type, values.size, owner)
            val parameters = constructor.valueParameters.map { typeOf(it.returnTypeRef) }
            val signature = ConeFunctionType(parameters, type)
            val typeArguments = (type as? ConeLookupTagBasedType)?.typeArguments?.map { it.type }.orEmpty()
            val base = generated(owner, signature, CfirSourceExpressionPayload.Reference(Name.identifier("<init>"), CfirSourceReferenceTarget.Declaration(constructor), typeArguments, CfirReferenceParentType.Absent))
            val arguments = values.zip(parameters).map { (value, parameterType) ->
                generated(owner, parameterType, CfirSourceExpressionPayload.Argument(value, false, false))
            }
            return CfirSourceExpressionPayload.Call(base, arguments, CfirSourceCallKind.OBJECT_CREATION, false)
        }

        fun freshVariable(name: String, type: ConeCangJieType, isVar: Boolean, owner: CfirDeclaration, spanSource: CfirElement? = null): CfirFieldVariable {
            val lexicalOwner = owner as? CfirFunction ?: nearestFunction(owner) ?: owner
            val nameValue = Name.identifier(name)
            val declaration = buildFieldVariable {
                source = spanSource?.source
                moduleData = owner.moduleData
                resolvePhase = CfirResolvePhase.RAW_CFIR
                origin = CfirDeclarationOrigin.Synthetic.CanonicalSource
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                this.isVar = isVar
                isTypeImplicit = true
                this.name = nameValue
                symbol = CfirFieldVariableSymbol(CallableId(context(owner).file.packageDirective.packageFqName, nameValue))
                returnTypeRef = requireType(type).toCfirResolvedTypeRef(source)
                status = buildResolvedDeclarationStatus {
                    visibility = Visibilities.Private; modality = Modality.FINAL
                    isVisibilityExplicit = false; isModalityExplicit = false; isAbstractExplicit = false
                    isOverride = false; isOperator = false; isStatic = false; isConst = false; isMut = false
                    isUnsafe = false; isForeign = false; isC = false; isCommon = false; isSpecific = false
                    isCommonWithDefault = false; isRedef = false; isDefault = false; isAbstract = false; isOpen = false; isSealed = false
                }
            }
            declaration.exportFacts = CfirDeclarationExportFacts(isImplicitlyAdded = true, sourceFactsPublished = true)
            declaration.interopInfo = CfirInteropInfo.NONE
            declarationContexts[declaration] = CfirSourceDeclarationContext(lexicalOwner, context(owner).file)
            owners[declaration] = lexicalOwner
            builder.generatedDeclaration(declaration, context(declaration))
            return declaration
        }

        fun reference(variable: CfirVariable, owner: CfirDeclaration): CfirSourceExpressionIdentity = generated(owner,
            typeOf(variable.returnTypeRef), CfirSourceExpressionPayload.Reference(variable.symbol.name, CfirSourceReferenceTarget.Declaration(variable), emptyList(), CfirReferenceParentType.Absent))

        fun generated(owner: CfirDeclaration, type: ConeCangJieType, payload: CfirSourceExpressionPayload,
            span: CfirSourceSpan? = null): CfirSourceExpressionIdentity = builder.reserveGenerated().also {
            builder.fill(it, record(owner, requireType(type), span, payload))
        }

        private fun record(owner: CfirDeclaration, type: ConeCangJieType, span: CfirSourceSpan?, payload: CfirSourceExpressionPayload): CfirSourceExpressionRecord =
            CfirSourceExpressionRecord(owner, type, span, payload, overflowStrategy = owner.annotationInfo?.overflowStrategy ?: owner.interopInfo?.overflowStrategy)

        fun literal(owner: CfirDeclaration, type: ConeCangJieType, kind: CfirLiteralKind, text: String): CfirSourceExpressionIdentity =
            generated(owner, type, CfirSourceExpressionPayload.Literal(kind, text, false))

        fun unit(owner: CfirDeclaration): CfirSourceExpressionIdentity = literal(owner, ConePrimitiveType.UNIT, CfirLiteralKind.UNIT, "()")
        fun generatedBlock(owner: CfirDeclaration, statements: List<CfirSourceSlot>, type: ConeCangJieType = ConePrimitiveType.UNIT): CfirSourceExpressionIdentity =
            generated(owner, type, CfirSourceExpressionPayload.Block(statements, false))
        fun expressionBlock(owner: CfirDeclaration, expression: CfirSourceExpressionIdentity): CfirSourceExpressionIdentity =
            generatedBlock(owner, listOf(CfirSourceSlot.Expression(expression)), builder.record(expression).type)
        fun optionType(element: ConeCangJieType, owner: CfirDeclaration): ConeEnumType {
            val symbol = owner.moduleData.session.symbolProvider.getClassLikeSymbolByClassId(StdlibClassIds.Option) as? CfirEnumSymbol
                ?: error("SOURCE requires the actual std.core.Option declaration")
            return ConeEnumType(symbol.toLookupTag(), listOf(element))
        }
        fun boolean(owner: CfirDeclaration, value: Boolean): CfirSourceExpressionIdentity = literal(owner, ConePrimitiveType.BOOLEAN, CfirLiteralKind.BOOLEAN, value.toString())
        fun typeOf(expression: CfirExpression): ConeCangJieType = requireType(checkNotNull(expression.coneTypeOrNull))
        fun typeOf(ref: CfirTypeRef): ConeCangJieType = requireType(checkNotNull(ref.coneTypeOrNull))

        private fun requireType(type: ConeCangJieType): ConeCangJieType {
            check(!type.contains { it is ConeErrorType || it is ConeTypeVariableType || it is ConeStubType || it is ConePrimitiveType && it.kind.isIdeal }) {
                "SOURCE product requires complete valid normal types: $type"
            }
            return type
        }

        private fun literalPayload(literal: CfirLiteralExpression): CfirSourceExpressionPayload.Literal {
            val source = literal.source?.text?.toString()
            val text = when (literal.kind) {
                CfirLiteralKind.STRING, CfirLiteralKind.JSTRING -> if (source != null) {
                    checkNotNull(CfirStringLiteralValueNormalizer.normalizeSourceLiteral(source))
                } else literal.value as String
                CfirLiteralKind.RUNE -> if (source != null) {
                    checkNotNull(CfirStringLiteralValueNormalizer.normalizeRuneSourceLiteral(source))
                } else (literal.value as Char).toString()
                CfirLiteralKind.BYTE -> checkNotNull(source)
                CfirLiteralKind.INT -> when (val value = literal.value) {
                    // 两条 raw builder 的数字 payload 都保留字面量拼写；BODY 已证明其类型和值合法。
                    is String -> value
                    is BigInteger -> value.toString(); is Long -> value.toString(); is Int -> value.toString()
                    is Short -> value.toString(); is Byte -> value.toString(); is ULong -> value.toString(); is UInt -> value.toString()
                    else -> error("SOURCE integer literal has no integer value")
                }
                CfirLiteralKind.FLOAT -> when (val value = literal.value) {
                    is String -> value
                    is BigDecimal -> value.toString(); is Double -> value.toString(); is Float -> value.toString()
                    else -> error("SOURCE float literal has no floating value")
                }
                CfirLiteralKind.BOOLEAN -> (literal.value as Boolean).toString()
                CfirLiteralKind.UNIT -> "()"
            }
            val stringKind = when {
                literal.kind == CfirLiteralKind.JSTRING -> CfirSourceStringKind.JSTRING
                literal.kind != CfirLiteralKind.STRING -> CfirSourceStringKind.NORMAL
                source == null -> CfirSourceStringKind.NORMAL
                source.startsWith("#") -> CfirSourceStringKind.MULTILINE_RAW
                source.startsWith("\"\"\"") || source.startsWith("'''") -> CfirSourceStringKind.MULTILINE
                else -> CfirSourceStringKind.NORMAL
            }
            return CfirSourceExpressionPayload.Literal(literal.kind, text, stringKind)
        }

        private fun constant(expression: CfirConstantValueExpression, owner: CfirDeclaration): CfirSourceExpressionPayload =
            CfirSourceAdditionalCanonicalizer(this).constant(expression.constantValue, owner)

        /** CaptureKind 按真实 mutable binding/call 图求固定点，不能以 lambda 有引用就标 CAPTURE_VAR。 */
        private fun computeCaptureKinds() {
            val direct = IdentityHashMap<CfirFunction, MutableSet<CfirVariable>>()
            val mutableOwned = IdentityHashMap<CfirFunction, MutableSet<CfirVariable>>()
            val calls = IdentityHashMap<CfirFunction, MutableSet<CfirFunction>>()
            functions.forEach { function ->
                val captures = Collections.newSetFromMap(IdentityHashMap<CfirVariable, Boolean>())
                val own = Collections.newSetFromMap(IdentityHashMap<CfirVariable, Boolean>())
                val called = Collections.newSetFromMap(IdentityHashMap<CfirFunction, Boolean>())
                val seen = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
                function.body?.accept(object : CfirVisitorVoid() {
                    override fun visitElement(element: CfirElement) {
                        if (element is CfirControlFlowGraphReference || !seen.add(element)) return
                        if (element is CfirFunction) return
                        if (element is CfirVariable && element.isVar) own += element
                        if (element is CfirQualifiedAccessExpression) {
                            val target = (element.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol?.cfir
                            if (target is CfirVariable && target.isVar && nearestFunction(target) !== function && nearestFunction(target) != null) captures += target
                            if (element is CfirFunctionCall && target is CfirFunction) called += target
                        }
                        if (element is CfirArgumentList) element.arguments.forEach { it.accept(this) }
                        else element.acceptChildren(this)
                    }
                })
                direct[function] = captures
                mutableOwned[function] = own
                calls[function] = called
                captureKinds[function] = if (captures.isEmpty()) 0 else 1
            }
            var changed: Boolean
            do {
                changed = false
                functions.forEach { function ->
                    calls.getValue(function).forEach { target ->
                        direct[target]?.filter { it !in mutableOwned.getValue(function) }?.forEach { variable ->
                            if (direct.getValue(function).add(variable)) {
                                captureKinds[function] = 2
                                changed = true
                            }
                        }
                    }
                }
            } while (changed)
        }

        private fun nearestFunction(declaration: CfirDeclaration): CfirFunction? {
            var current = owners[declaration]
            while (current != null && current !is CfirFunction) current = owners[current]
            return current as? CfirFunction
        }
    }
}

internal fun sourceOperator(symbol: String): CfirSourceOperator = when (symbol) {
    "+" -> CfirSourceOperator.ADD; "-" -> CfirSourceOperator.SUBTRACT; "*" -> CfirSourceOperator.MULTIPLY
    "/" -> CfirSourceOperator.DIVIDE; "%" -> CfirSourceOperator.REMAINDER; "**" -> CfirSourceOperator.POWER
    "!" -> CfirSourceOperator.NOT; "<" -> CfirSourceOperator.LESS; "<=" -> CfirSourceOperator.LESS_EQUAL
    ">" -> CfirSourceOperator.GREATER; ">=" -> CfirSourceOperator.GREATER_EQUAL; "==" -> CfirSourceOperator.EQUAL; "!=" -> CfirSourceOperator.NOT_EQUAL
    "&" -> CfirSourceOperator.BIT_AND; "|" -> CfirSourceOperator.BIT_OR; "^" -> CfirSourceOperator.BIT_XOR
    "<<" -> CfirSourceOperator.SHIFT_LEFT; ">>" -> CfirSourceOperator.SHIFT_RIGHT
    "&&" -> CfirSourceOperator.LOGICAL_AND; "||" -> CfirSourceOperator.LOGICAL_OR; "??" -> CfirSourceOperator.COALESCING
    "|>" -> CfirSourceOperator.PIPELINE; "~>" -> CfirSourceOperator.COMPOSITION
    else -> error("No native SOURCE operation for '$symbol'")
}

internal fun compoundOperator(operation: Name): CfirSourceOperator = when (checkNotNull(OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[operation])) {
    "+" -> CfirSourceOperator.ADD_ASSIGN; "-" -> CfirSourceOperator.SUBTRACT_ASSIGN
    "*" -> CfirSourceOperator.MULTIPLY_ASSIGN; "/" -> CfirSourceOperator.DIVIDE_ASSIGN
    "%" -> CfirSourceOperator.REMAINDER_ASSIGN; "**" -> CfirSourceOperator.POWER_ASSIGN
    "<<" -> CfirSourceOperator.SHIFT_LEFT_ASSIGN; ">>" -> CfirSourceOperator.SHIFT_RIGHT_ASSIGN
    "&" -> CfirSourceOperator.BIT_AND_ASSIGN; "|" -> CfirSourceOperator.BIT_OR_ASSIGN
    "^" -> CfirSourceOperator.BIT_XOR_ASSIGN
    "&&" -> CfirSourceOperator.LOGICAL_AND_ASSIGN; "||" -> CfirSourceOperator.LOGICAL_OR_ASSIGN
    else -> error("No native SOURCE compound operation for '$operation'")
}

internal fun builtinOperator(operation: BuiltinPrimitiveOperatorOperation): CfirSourceOperator = when (operation) {
    BuiltinPrimitiveOperatorOperation.NEGATE -> CfirSourceOperator.SUBTRACT
    else -> sourceOperator(checkNotNull(OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[operation.nameInCfir]))
}
