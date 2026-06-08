package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirValue
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol

internal class Cfir2ChirFunctionBodyConverter(
    private val components: Cfir2ChirComponents,
    private val packageName: String,
    private val function: CfirFunction,
    private val header: ChirFunctionHeader,
) {
    private val storage: Cfir2ChirDeclarationStorage = components.declarationStorage
    private val typeMapper: Cfir2ChirTypeMapper = components.typeMapper
    private val builder = FunctionBodyBuilder(header.semanticId)

    fun convert(): DefaultChirFunctionDeclaration {
        val entry = builder.enterNewBlock("entry")
        val body = function.body
        if (body == null) {
            if (!header.returnType.isUnit()) {
                throw Cfir2ChirConversionException("non-unit function ${header.name} has no CFIR body", function)
            }
            builder.terminate(ChirReturnTerminator(builder.nextId("return")))
        } else {
            val result = lowerBlock(body)
            if (builder.hasOpenCurrentBlock) {
                val returnValue = if (header.returnType.isUnit()) {
                    null
                } else {
                    result ?: throw Cfir2ChirConversionException(
                        "function ${header.name} body does not produce return value for ${header.returnType.renderName}",
                        body,
                    )
                }
                builder.terminate(ChirReturnTerminator(builder.nextId("return"), returnValue))
            }
        }

        return DefaultChirFunctionDeclaration(
            semanticId = header.semanticId,
            name = header.name,
            returnType = header.returnType,
            parameters = header.parameters,
            blocks = builder.freeze(),
            entryBlockId = entry.semanticId,
        )
    }

    private fun lowerBlock(block: CfirBlock): ChirValue? {
        var lastValue: ChirValue? = null
        block.statements.forEach { statement ->
            if (!builder.hasOpenCurrentBlock) {
                throw Cfir2ChirConversionException("unreachable CFIR statement after terminated CHIR block", statement)
            }
            lastValue = lowerStatement(statement)
        }
        return lastValue
    }

    private fun lowerStatement(statement: CfirStatement): ChirValue? {
        return when (statement) {
            is CfirReturnExpression -> {
                lowerReturnExpression(statement)
                null
            }
            is CfirVariable -> lowerLocalVariable(statement)
            is CfirExpression -> lowerExpression(statement)
            is CfirDeclaration -> throw Cfir2ChirConversionException(
                "local CFIR declaration lowering is not yet represented in CHIR body converter: ${statement::class.qualifiedName}",
                statement,
            )
            else -> throw Cfir2ChirConversionException(
                "unsupported CFIR statement for CHIR body lowering: ${statement::class.qualifiedName}",
                statement,
            )
        }
    }

    private fun lowerReturnExpression(returnExpression: CfirReturnExpression) {
        val result = lowerExpression(returnExpression.result)
        val returnValue = if (header.returnType.isUnit()) {
            null
        } else {
            result ?: throw Cfir2ChirConversionException("non-unit return expression produced no CHIR value", returnExpression)
        }
        builder.terminate(ChirReturnTerminator(builder.nextId("return"), returnValue))
    }

    private fun lowerLocalVariable(variable: CfirVariable): ChirValue? {
        if (variable is CfirPatternVariable) {
            return lowerPatternVariable(variable)
        }
        return lowerLocalBindingVariable(variable, variable.initializer)
    }

    private fun lowerPatternVariable(variable: CfirPatternVariable): ChirValue? {
        val bindingVariables = variable.pattern.bindingVariables()
        if (bindingVariables.size != 1) {
            throw Cfir2ChirConversionException(
                "pattern variable with ${bindingVariables.size} bindings requires destructuring CHIR lowering",
                variable,
            )
        }
        val initializer = variable.initializer
            ?: throw Cfir2ChirConversionException("pattern local variable requires initializer before CHIR lowering", variable)
        return lowerLocalBindingVariable(bindingVariables.single(), initializer)
    }

    /**
     * 将真实进入作用域的 CFIR 局部绑定降为 CHIR 地址。
     *
     * 外层模式声明只负责组织 pattern；实际读写必须绑定到 pattern 中的 binding variable symbol，
     * 否则后续名称解析引用无法与 CHIR local slot 对齐。
     */
    private fun lowerLocalBindingVariable(
        variable: CfirVariable,
        initializer: CfirExpression?,
    ): ChirValue? {
        val declaration = DefaultChirVariableDeclaration(
            semanticId = Cfir2ChirIds.declarationId(variable.symbol),
            name = variable.nameForChir(),
            type = typeMapper.mapTypeRef(variable.returnTypeRef),
            mutable = variable.isVar,
        )
        val localVariable = ChirLocalVariableHeader(
            declaration = declaration,
            ownerFunctionId = header.semanticId,
        )
        storage.registerLocalVariable(variable.symbol, localVariable)
        builder.emit(
            ChirMemoryExpression(
                semanticId = builder.nextId("alloca"),
                operation = "alloca",
                address = localVariable.asAddressValue(),
            ),
        )

        initializer ?: return null
        val initializerValue = lowerExpression(initializer)
            ?: throw Cfir2ChirConversionException("local variable initializer does not produce a CHIR value", initializer)
        emitStore(
            targetAddress = localVariable.asAddressValue(),
            value = initializerValue,
            source = initializer,
            allowImmutableInitialization = true,
        )
        return null
    }

    private fun lowerAssignmentExpression(expression: CfirAssignment) {
        val targetAddress = assignmentAddress(expression.lValue)
        val value = lowerExpression(expression.rValue)
            ?: throw Cfir2ChirConversionException("assignment right hand side does not produce a CHIR value", expression.rValue)
        emitStore(
            targetAddress = targetAddress,
            value = value,
            source = expression,
            allowImmutableInitialization = false,
        )
    }

    private fun lowerExpression(expression: CfirExpression): ChirValue? {
        return when (expression) {
            is CfirLiteralExpression -> lowerLiteralExpression(expression)
            is CfirNamedAccessExpression -> lowerNamedAccessExpression(expression)
            is CfirFunctionCall -> lowerFunctionCall(expression)
            is CfirComparisonExpression -> lowerComparisonExpression(expression)
            is CfirAssignment -> {
                lowerAssignmentExpression(expression)
                null
            }
            is CfirIfExpression -> {
                lowerIfExpression(expression)
                null
            }
            is CfirBlock -> lowerBlock(expression)
            is CfirBinaryOp -> throw Cfir2ChirConversionException(
                "short-circuit CFIR binary op ${expression.kind} requires dedicated CFG lowering before CHIR value production",
                expression,
            )
            else -> throw Cfir2ChirConversionException(
                "unsupported CFIR expression for CHIR body lowering: ${expression::class.qualifiedName}",
                expression,
            )
        }
    }

    private fun lowerLiteralExpression(expression: CfirLiteralExpression): ChirValue? {
        if (expression.kind == CfirLiteralKind.UNIT) {
            return null
        }
        val type = expression.coneTypeOrNull?.let(typeMapper::mapConeTypeRef)
            ?: throw Cfir2ChirConversionException("literal ${expression.kind} must carry resolved Cone type before CHIR lowering", expression)
        return ChirConstantValue(
            semanticId = builder.nextElementId("literal", expression),
            type = type,
            literal = when (expression.kind) {
                CfirLiteralKind.STRING -> expression.value?.toString() ?: ""
                CfirLiteralKind.BOOLEAN -> expression.value.toString()
                CfirLiteralKind.INT,
                CfirLiteralKind.FLOAT,
                CfirLiteralKind.RUNE,
                -> expression.value?.toString()
                    ?: throw Cfir2ChirConversionException("literal ${expression.kind} has null value", expression)
                CfirLiteralKind.UNIT -> error("unit literal is handled before value materialization")
            },
        )
    }

    private fun lowerNamedAccessExpression(expression: CfirNamedAccessExpression): ChirValue {
        val reference = expression.calleeReference as? CfirResolvedNamedReference
            ?: throw Cfir2ChirConversionException("named access must be resolved before CHIR lowering", expression)
        return valueForResolvedReference(reference, expression)
    }

    private fun lowerLocalVariableAccess(
        symbol: CfirVariableSymbol<*>,
        expression: CfirExpression,
    ): ChirValue {
        val localVariable = storage.getLocalVariable(symbol)
        val loadId = builder.nextElementId("load", expression)
        builder.emit(
            ChirMemoryExpression(
                semanticId = loadId,
                operation = "load",
                address = localVariable.asAddressValue(),
                resultType = localVariable.declaration.type,
            ),
        )
        return localVariable.declaration.type.valueOrNull(loadId)
            ?: throw Cfir2ChirConversionException("local variable ${localVariable.declaration.name} cannot be loaded as unit value", expression)
    }

    private fun lowerFunctionCall(functionCall: CfirFunctionCall): ChirValue? {
        if (functionCall.dispatchReceiver != null || functionCall.explicitReceiver != null) {
            throw Cfir2ChirConversionException("member/receiver calls require object lowering before JVM CHIR backend", functionCall)
        }
        val reference = functionCall.calleeReference as? CfirResolvedNamedReference
            ?: throw Cfir2ChirConversionException("function call must be resolved before CHIR lowering", functionCall)
        val callee = valueForResolvedReference(reference, functionCall)
        val arguments = functionCall.argumentList.arguments.map { argument ->
            lowerExpression(argument)
                ?: throw Cfir2ChirConversionException("function call argument does not produce a CHIR value", argument)
        }
        val resultType = functionCall.coneTypeOrNull?.let(typeMapper::mapConeTypeRef)
            ?: when (reference.resolvedSymbol) {
                is CfirNamedFunctionSymbol,
                is CfirMainFunctionSymbol,
                -> storage.getFunctionHeader(reference.resolvedSymbol).returnType
                else -> throw Cfir2ChirConversionException("function call result type is missing", functionCall)
            }
        val expressionId = builder.nextElementId("call", functionCall)
        builder.emit(
            ChirCallExpression(
                semanticId = expressionId,
                callee = callee,
                arguments = arguments,
                resultType = resultType,
            ),
        )
        return resultType.valueOrNull(expressionId)
    }

    private fun lowerComparisonExpression(expression: CfirComparisonExpression): ChirValue {
        val left = lowerExpression(expression.left)
            ?: throw Cfir2ChirConversionException("comparison left operand does not produce a CHIR value", expression.left)
        val right = lowerExpression(expression.right)
            ?: throw Cfir2ChirConversionException("comparison right operand does not produce a CHIR value", expression.right)
        val resultType = expression.coneTypeOrNull?.let(typeMapper::mapConeTypeRef)
            ?: ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val expressionId = builder.nextElementId("comparison", expression)
        builder.emit(
            ChirBinaryExpression(
                semanticId = expressionId,
                operator = expression.operation.symbol,
                left = left,
                right = right,
                resultType = resultType,
            ),
        )
        return resultType.valueOrNull(expressionId)
            ?: throw Cfir2ChirConversionException("comparison result cannot be unit", expression)
    }

    private fun lowerIfExpression(expression: CfirIfExpression) {
        val condition = lowerExpression(expression.condition)
            ?: throw Cfir2ChirConversionException("if condition does not produce a CHIR value", expression.condition)
        val thenBlockId = builder.nextId("then")
        val elseBlockId = builder.nextId("else")
        val continuationBlockId = builder.nextId("if_cont")
        builder.terminate(
            ChirConditionalBranchTerminator(
                semanticId = builder.nextId("if"),
                condition = condition,
                trueTargetBlockId = thenBlockId,
                falseTargetBlockId = elseBlockId,
            ),
        )

        builder.enterBlock(thenBlockId, "then")
        lowerBlock(expression.thenBranch)
        val thenFallsThrough = builder.hasOpenCurrentBlock
        if (thenFallsThrough) {
            builder.terminate(ChirBranchTerminator(builder.nextId("branch"), continuationBlockId))
        }

        builder.enterBlock(elseBlockId, "else")
        val elseBranch = expression.elseBranch
        if (elseBranch != null) {
            lowerExpression(elseBranch)
        }
        val elseFallsThrough = builder.hasOpenCurrentBlock
        if (elseFallsThrough) {
            builder.terminate(ChirBranchTerminator(builder.nextId("branch"), continuationBlockId))
        }

        if (thenFallsThrough || elseFallsThrough) {
            builder.enterBlock(continuationBlockId, "if_cont")
        } else {
            builder.clearCurrentBlock()
        }
    }

    private fun valueForResolvedReference(
        reference: CfirResolvedNamedReference,
        expression: CfirExpression,
    ): ChirValue {
        return when (val symbol = reference.resolvedSymbol) {
            is CfirValueParameterSymbol -> storage.getParameter(symbol).asValue()
            is CfirVariableSymbol<*> -> lowerLocalVariableAccess(symbol, expression)
            is CfirNamedFunctionSymbol,
            is CfirMainFunctionSymbol,
            -> storage.getFunctionHeader(symbol).asValue()
            else -> throw Cfir2ChirConversionException(
                "resolved CFIR reference ${reference.name} targets unsupported symbol ${symbol::class.qualifiedName}",
                expression,
            )
        }
    }

    private fun assignmentAddress(lValue: CfirExpression): ChirValue {
        val namedAccess = lValue as? CfirNamedAccessExpression
            ?: throw Cfir2ChirConversionException(
                "assignment target ${lValue::class.qualifiedName} requires dedicated address lowering",
                lValue,
            )
        if (namedAccess.dispatchReceiver != null || namedAccess.explicitReceiver != null) {
            throw Cfir2ChirConversionException("member assignment requires object address lowering before JVM CHIR backend", lValue)
        }
        val reference = namedAccess.calleeReference as? CfirResolvedNamedReference
            ?: throw Cfir2ChirConversionException("assignment target must be resolved before CHIR lowering", lValue)
        return when (val symbol = reference.resolvedSymbol) {
            is CfirValueParameterSymbol -> storage.getParameter(symbol).asAddressValue()
            is CfirVariableSymbol<*> -> storage.getLocalVariable(symbol).asAddressValue()
            else -> throw Cfir2ChirConversionException(
                "assignment target ${reference.name} resolves to unsupported symbol ${symbol::class.qualifiedName}",
                lValue,
            )
        }
    }

    private fun emitStore(
        targetAddress: ChirValue,
        value: ChirValue,
        source: org.cangnova.cangjie.cfir.CfirElement,
        allowImmutableInitialization: Boolean,
    ) {
        val referenceType = (targetAddress.type as? ChirResolvedTypeRef)?.type as? ChirRefType
            ?: throw Cfir2ChirConversionException(
                "assignment target address must have CHIR ref type, actual ${targetAddress.type.renderName}",
                source,
            )
        if (!referenceType.mutable && !allowImmutableInitialization) {
            throw Cfir2ChirConversionException("immutable CHIR address ${targetAddress.displayName} cannot be assigned", source)
        }
        if (value.type != referenceType.referencedType) {
            throw Cfir2ChirConversionException(
                "assignment value type ${value.type.renderName} does not match target ${referenceType.referencedType.renderName}",
                source,
            )
        }
        builder.emit(
            ChirMemoryExpression(
                semanticId = builder.nextId("store"),
                operation = "store",
                address = targetAddress,
                value = value,
            ),
        )
    }

    private fun CfirVariable.nameForChir(): String =
        when (this) {
            is CfirFieldVariable -> name.asString()
            is CfirPatternBindingVariable -> name.asString()
            is CfirPatternVariable -> throw Cfir2ChirConversionException(
                "pattern variable container requires pattern binding lowering before CHIR local allocation",
                this,
            )
            else -> throw Cfir2ChirConversionException(
                "unsupported CFIR variable kind for local CHIR lowering: ${this::class.qualifiedName}",
                this,
            )
        }

    private fun ChirTypeRef.valueOrNull(expressionId: ChirSemanticId): ChirValue? {
        if (isUnit()) return null
        return ChirLocalValue(
            semanticId = expressionId,
            type = this,
            name = expressionId.value,
        )
    }

    private fun ChirTypeRef.isUnit(): Boolean =
        this == ChirResolvedTypeRef(ChirPrimitiveType.UNIT) || this == ChirResolvedTypeRef(ChirPrimitiveType.VOID)

    private class FunctionBodyBuilder(
        private val ownerId: ChirSemanticId,
    ) {
        private val blocks = mutableListOf<MutableBlock>()
        private var currentBlock: MutableBlock? = null
        private var nextIndex = 0

        val hasOpenCurrentBlock: Boolean
            get() = currentBlock?.terminator == null

        fun enterNewBlock(name: String): MutableBlock =
            enterBlock(nextId(name), name)

        fun enterBlock(id: ChirSemanticId, name: String): MutableBlock {
            val block = MutableBlock(id, name)
            blocks += block
            currentBlock = block
            return block
        }

        fun clearCurrentBlock() {
            currentBlock = null
        }

        fun emit(expression: ChirExpression) {
            val block = openCurrentBlock()
            block.expressions += expression
        }

        fun terminate(terminator: ChirTerminator) {
            val block = openCurrentBlock()
            check(block.terminator == null) { "CHIR block ${block.name} is already terminated" }
            block.terminator = terminator
        }

        fun freeze(): List<ChirBlock> {
            return blocks.map { block ->
                ChirBlock(
                    semanticId = block.semanticId,
                    name = block.name,
                    expressions = block.expressions,
                    terminator = block.terminator
                        ?: throw Cfir2ChirConversionException("CHIR block ${block.name} has no terminator"),
                )
            }
        }

        fun nextId(kind: String): ChirSemanticId =
            Cfir2ChirIds.generatedId(kind, ownerId, nextIndex++)

        fun nextElementId(kind: String, expression: CfirExpression): ChirSemanticId =
            Cfir2ChirIds.elementId(kind, expression, ownerId, nextIndex++)

        private fun openCurrentBlock(): MutableBlock =
            currentBlock ?: throw Cfir2ChirConversionException("no open CHIR block in function body converter")
    }

    private data class MutableBlock(
        val semanticId: ChirSemanticId,
        val name: String,
        val expressions: MutableList<ChirExpression> = mutableListOf(),
        var terminator: ChirTerminator? = null,
    )
}
