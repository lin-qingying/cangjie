package org.cangnova.cangjie.chir.core.serializer

import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirConstantExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirFieldExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirTupleExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeRelations
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirConstantKind
import org.cangnova.cangjie.chir.core.value.ChirValue

/**
 * CHIR 包序列化 round-trip 的语义断言。
 *
 * 这里比较的是后端消费所需的结构化语义，而不是打印文本；
 * operation、操作数、控制流目标和值类型都必须保持一致。
 */
object ChirRoundTripAssert {
    /**
     * 断言两个 CHIR 包在后端语义上等价。
     */
    fun assertSemanticallyEquivalent(expected: ChirPackage, actual: ChirPackage) {
        require(expected.semanticId == actual.semanticId) { "package id mismatch" }
        require(expected.name == actual.name) { "package name mismatch" }
        require(expected.accessLevel == actual.accessLevel) { "package access mismatch" }
        require(expected.packageInitFunctionId == actual.packageInitFunctionId) { "package init mismatch" }
        require(expected.packageLiteralInitFunctionId == actual.packageLiteralInitFunctionId) { "package literal init mismatch" }
        require(expected.members.globalVariables.size == actual.members.globalVariables.size) { "global variable count mismatch" }
        require(expected.members.globalFunctions.size == actual.members.globalFunctions.size) { "global function count mismatch" }
        require(expected.members.importedVariables.size == actual.members.importedVariables.size) { "imported variable count mismatch" }
        require(expected.members.importedFunctions.size == actual.members.importedFunctions.size) { "imported function count mismatch" }
        require(expected.typeDefinitions.size == actual.typeDefinitions.size) { "type definition count mismatch" }
        require(expected.importedTypeDefinitions.size == actual.importedTypeDefinitions.size) { "imported type definition count mismatch" }
        require(expected.modules.size == actual.modules.size) { "module count mismatch" }

        expected.modules.zip(actual.modules).forEach { (leftModule, rightModule) ->
            require(leftModule.semanticId == rightModule.semanticId) { "module id mismatch" }
            require(leftModule.declarations.size == rightModule.declarations.size) { "declaration count mismatch in ${leftModule.name}" }

            leftModule.declarations.zip(rightModule.declarations).forEach { (leftDecl, rightDecl) ->
                val leftFunction = leftDecl as? ChirFunctionDeclaration
                val rightFunction = rightDecl as? ChirFunctionDeclaration
                require(leftFunction != null && rightFunction != null) { "only function declarations supported for round-trip assert" }
                require(leftFunction.semanticId == rightFunction.semanticId) { "function id mismatch" }
                require(leftFunction.name == rightFunction.name) { "function name mismatch" }
                require(ChirTypeRelations.areEquivalent(leftFunction.returnType, rightFunction.returnType)) { "function return type mismatch" }
                require(leftFunction.hasBody == rightFunction.hasBody) { "function body state mismatch" }
                require(leftFunction.isStatic == rightFunction.isStatic) { "function static-member state mismatch" }
                require(leftFunction.entryBlockId == rightFunction.entryBlockId) { "entry block mismatch" }
                require(leftFunction.blocks.size == rightFunction.blocks.size) { "block count mismatch" }

                leftFunction.blocks.zip(rightFunction.blocks).forEach { (leftBlock, rightBlock) ->
                    require(leftBlock.semanticId == rightBlock.semanticId) { "block id mismatch" }
                    require(leftBlock.name == rightBlock.name) { "block name mismatch in ${leftBlock.semanticId.value}" }
                    assertTerminatorEquivalent(leftBlock.terminator, rightBlock.terminator)
                    require(leftBlock.expressions.size == rightBlock.expressions.size) { "expression count mismatch" }
                    leftBlock.expressions.zip(rightBlock.expressions).forEach { (leftExpr, rightExpr) ->
                        assertExpressionEquivalent(leftExpr, rightExpr)
                    }
                }
            }
        }
    }

    /**
     * 断言两个表达式节点语义等价。
     */
    private fun assertExpressionEquivalent(expected: ChirExpression, actual: ChirExpression) {
        require(expected::class == actual::class) { "expression kind mismatch" }
        require(expected.semanticId == actual.semanticId) { "expression id mismatch" }
        require(expected.domain == actual.domain) { "expression domain mismatch" }
        require(
            (expected.resultType == null && actual.resultType == null) ||
                (expected.resultType != null && actual.resultType != null &&
                    ChirTypeRelations.areEquivalent(expected.resultType!!, actual.resultType!!)),
        ) { "expression result type mismatch" }
        require(
            (expected.resultLocalValue == null && actual.resultLocalValue == null) ||
                (expected.resultLocalValue != null && actual.resultLocalValue != null &&
                    expected.resultLocalValue!!.semanticId == actual.resultLocalValue!!.semanticId &&
                    (expected.resultLocalValue!!.name.isBlank() || actual.resultLocalValue!!.name.isBlank() ||
                        expected.resultLocalValue!!.name == actual.resultLocalValue!!.name) &&
                    expected.resultLocalValue!!.associatedExpressionId == actual.resultLocalValue!!.associatedExpressionId &&
                    expected.resultLocalValue!!.isReturnValue == actual.resultLocalValue!!.isReturnValue &&
                    ChirTypeRelations.areEquivalent(expected.resultLocalValue!!.type, actual.resultLocalValue!!.type)),
        ) { "expression result local value mismatch" }
        when (expected) {
            is ChirUnaryExpression -> {
                actual as ChirUnaryExpression
                require(expected.operator == actual.operator) { "unary operator mismatch in ${expected.semanticId.value}" }
                require(expected.overflowStrategy == actual.overflowStrategy) {
                    "unary overflow strategy mismatch in ${expected.semanticId.value}"
                }
                assertValueEquivalent(expected.operand, actual.operand, "unary operand")
            }
            is ChirBinaryExpression -> {
                actual as ChirBinaryExpression
                require(expected.operator == actual.operator) { "binary operator mismatch in ${expected.semanticId.value}" }
                require(expected.overflowStrategy == actual.overflowStrategy) {
                    "binary overflow strategy mismatch in ${expected.semanticId.value}"
                }
                assertValueEquivalent(expected.left, actual.left, "binary left")
                assertValueEquivalent(expected.right, actual.right, "binary right")
            }
            is ChirMemoryExpression -> {
                actual as ChirMemoryExpression
                require(expected.operation == actual.operation) { "memory operation mismatch in ${expected.semanticId.value}" }
                require(expected.memberNames == actual.memberNames) { "memory member path mismatch in ${expected.semanticId.value}" }
                assertValueEquivalent(expected.address, actual.address, "memory address")
                assertNullableValueEquivalent(expected.value, actual.value, "memory value")
            }
            is ChirCallExpression -> {
                actual as ChirCallExpression
                require(expected.isSuperCall == actual.isSuperCall) { "call dispatch kind mismatch in ${expected.semanticId.value}" }
                require(expected.instantiatedTypeArguments.size == actual.instantiatedTypeArguments.size &&
                    expected.instantiatedTypeArguments.zip(actual.instantiatedTypeArguments).all { (left, right) ->
                        ChirTypeRelations.areEquivalent(left, right)
                    }
                ) {
                    "call instantiated type arguments mismatch in ${expected.semanticId.value}"
                }
                require(
                    (expected.thisType == null && actual.thisType == null) ||
                        (expected.thisType != null && actual.thisType != null &&
                            ChirTypeRelations.areEquivalent(expected.thisType!!, actual.thisType!!)),
                ) {
                    "call this type mismatch in ${expected.semanticId.value}"
                }
                val expectedContext = expected.virtualMethodContext
                val actualContext = actual.virtualMethodContext
                require((expectedContext == null) == (actualContext == null)) {
                    "call kind mismatch in ${expected.semanticId.value}"
                }
                if (expectedContext == null) {
                    assertValueEquivalent(expected.callee, actual.callee, "call callee")
                } else {
                    val decodedContext = checkNotNull(actualContext)
                    require(expectedContext.sourceCodeIdentifier == decodedContext.sourceCodeIdentifier) {
                        "virtual method identifier mismatch in ${expected.semanticId.value}"
                    }
                    require(ChirTypeRelations.areEquivalent(
                        ChirResolvedTypeRef(expectedContext.originalFunctionType),
                        ChirResolvedTypeRef(decodedContext.originalFunctionType),
                    )) {
                        "virtual method signature mismatch in ${expected.semanticId.value}"
                    }
                    require(expectedContext.genericTypeParameters.size == decodedContext.genericTypeParameters.size &&
                        expectedContext.genericTypeParameters.zip(decodedContext.genericTypeParameters).all { (left, right) ->
                            ChirTypeRelations.areEquivalent(left, right)
                        }
                    ) {
                        "virtual method generic parameters mismatch in ${expected.semanticId.value}"
                    }
                    assertNullableValueEquivalent(expected.dispatchReceiver, actual.dispatchReceiver, "call dispatch receiver")
                }
                require(expected.arguments.size == actual.arguments.size) { "call argument count mismatch in ${expected.semanticId.value}" }
                expected.arguments.zip(actual.arguments).forEachIndexed { index, (left, right) ->
                    assertValueEquivalent(left, right, "call argument #$index")
                }
            }
            is ChirTupleExpression -> {
                actual as ChirTupleExpression
                require(expected.elements.size == actual.elements.size) {
                    "tuple element count mismatch in ${expected.semanticId.value}"
                }
                expected.elements.zip(actual.elements).forEachIndexed { index, (left, right) ->
                    assertValueEquivalent(left, right, "tuple element #$index")
                }
            }
            is ChirFieldExpression -> {
                actual as ChirFieldExpression
                require(expected.path == actual.path) { "field path mismatch in ${expected.semanticId.value}" }
                assertValueEquivalent(expected.base, actual.base, "field base")
            }
            is ChirConstantExpression -> {
                actual as ChirConstantExpression
                assertValueEquivalent(expected.value, actual.value, "constant literal")
            }
            is ChirOtherExpression -> {
                actual as ChirOtherExpression
                require(expected.operation == actual.operation) { "other operation mismatch in ${expected.semanticId.value}" }
                require(expected.intrinsicKind == actual.intrinsicKind) {
                    "intrinsic kind mismatch in ${expected.semanticId.value}"
                }
                require(expected.intrinsicTypeArguments.size == actual.intrinsicTypeArguments.size) {
                    "intrinsic type argument count mismatch in ${expected.semanticId.value}"
                }
                expected.intrinsicTypeArguments.zip(actual.intrinsicTypeArguments).forEachIndexed { index, (left, right) ->
                    require(ChirTypeRelations.areEquivalent(left, right)) {
                        "intrinsic type argument #$index mismatch in ${expected.semanticId.value}"
                    }
                }
                require(expected.operands.size == actual.operands.size) { "other operand count mismatch in ${expected.semanticId.value}" }
                expected.operands.zip(actual.operands).forEachIndexed { index, (left, right) ->
                    assertValueEquivalent(left, right, "other operand #$index")
                }
                require(expected.debugMetadata == actual.debugMetadata) {
                    "debug metadata mismatch in ${expected.semanticId.value}"
                }
            }
        }
    }

    /**
     * 断言两个终结指令语义等价。
     */
    private fun assertTerminatorEquivalent(expected: ChirTerminator?, actual: ChirTerminator?) {
        require(expected == null || actual != null) { "terminator mismatch: actual is missing" }
        require(actual == null || expected != null) { "terminator mismatch: expected is missing" }
        if (expected == null || actual == null) return
        require(expected::class == actual::class) { "terminator kind mismatch" }
        require(expected.semanticId == actual.semanticId) { "terminator id mismatch" }
        when (expected) {
            is ChirReturnTerminator -> {
                actual as ChirReturnTerminator
                assertNullableValueEquivalent(expected.returnValue, actual.returnValue, "return value")
            }
            is ChirBranchTerminator -> {
                actual as ChirBranchTerminator
                require(expected.targetBlockId == actual.targetBlockId) { "branch target mismatch in ${expected.semanticId.value}" }
            }
            is ChirConditionalBranchTerminator -> {
                actual as ChirConditionalBranchTerminator
                assertValueEquivalent(expected.condition, actual.condition, "branch condition")
                require(expected.trueTargetBlockId == actual.trueTargetBlockId) { "branch true target mismatch in ${expected.semanticId.value}" }
                require(expected.falseTargetBlockId == actual.falseTargetBlockId) { "branch false target mismatch in ${expected.semanticId.value}" }
            }
            is ChirThrowTerminator -> {
                actual as ChirThrowTerminator
                assertValueEquivalent(expected.exceptionValue, actual.exceptionValue, "throw exception")
                require(expected.unwindTargetBlockId == actual.unwindTargetBlockId) { "throw unwind target mismatch in ${expected.semanticId.value}" }
            }
            is ChirUnwindTerminator -> {
                actual as ChirUnwindTerminator
                require(expected.targetBlockId == actual.targetBlockId) { "unwind target mismatch in ${expected.semanticId.value}" }
            }
        }
    }

    /**
     * 断言两个可空值节点语义等价。
     */
    private fun assertNullableValueEquivalent(expected: ChirValue?, actual: ChirValue?, location: String) {
        require(expected != null || actual == null) { "$location value mismatch: expected null" }
        require(expected == null || actual != null) { "$location value mismatch: actual null" }
        if (expected != null && actual != null) {
            assertValueEquivalent(expected, actual, location)
        }
    }

    /**
     * 断言两个值节点语义等价。
     */
    private fun assertValueEquivalent(expected: ChirValue, actual: ChirValue, location: String) {
        require(expected::class == actual::class) { "$location value kind mismatch" }
        require(expected.semanticId == actual.semanticId) { "$location value id mismatch" }
        require(expected.kind == actual.kind) { "$location value kind enum mismatch" }
        require(ChirTypeRelations.areEquivalent(expected.type, actual.type)) { "$location value type mismatch" }
        if (expected is ChirConstantValue && actual is ChirConstantValue) {
            if (expected.constantKind != ChirConstantKind.UNSPECIFIED) {
                require(expected.constantKind == actual.constantKind) { "$location constant kind mismatch" }
            }
            when ((expected.type as? ChirResolvedTypeRef)?.type) {
                ChirPrimitiveType.BOOL ->
                    require(booleanValue(expected.literal) == booleanValue(actual.literal)) {
                        "$location Bool literal mismatch"
                    }
                ChirPrimitiveType.FLOAT16,
                ChirPrimitiveType.FLOAT32,
                ChirPrimitiveType.FLOAT64,
                -> require(floatingValueBits(expected.literal) == floatingValueBits(actual.literal)) {
                    "$location floating-point literal mismatch"
                }
                ChirPrimitiveType.RUNE ->
                    require(expected.literal.toUIntOrNull() == actual.literal.toUIntOrNull()) {
                        "$location Rune literal mismatch"
                    }
                else -> require(expected.displayName == actual.displayName) {
                    "$location value display name mismatch"
                }
            }
        } else {
            require(expected.displayName == actual.displayName) { "$location value display name mismatch" }
        }
    }

    private fun floatingValueBits(literal: String): Long = literal.toDoubleOrNull()?.toBits()
        ?: throw IllegalArgumentException("invalid floating-point literal '$literal'")

    private fun booleanValue(literal: String): Boolean? = when (literal) {
        "0", "false" -> false
        "1", "true" -> true
        else -> null
    }
}
