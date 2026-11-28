package org.cangnova.cangjie.chir.core.transform

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirExtendDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirPropertyDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirConstantExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirFieldExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirTupleExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeRelations
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirBlockGroupValue
import org.cangnova.cangjie.chir.core.value.ChirBlockValue
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirGlobalValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedVariableValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.chir.core.value.ChirValue

/**
 * 在 CHIR 声明及其可执行图中按泛型语义身份替换类型。
 *
 * 该变换保留节点身份和控制流关系，只改写所有承载类型事实的位置，避免签名、值类型与调用点
 * 在包合并或实例化后引用不同的泛型参数身份。
 */
object ChirTypeSubstitutor {
    /** 替换声明签名、成员和函数体内的所有类型引用。 */
    fun substitute(declaration: ChirDeclaration, substitutions: Map<String, ChirTypeRef>): ChirDeclaration =
        when (declaration) {
            is DefaultChirTypeDeclaration -> declaration.copy(
                typeParameterTypes = declaration.typeParameterTypes.map { substitute(it, substitutions) },
            )

            is DefaultChirClassDeclaration -> declaration.copy(
                superTypes = declaration.superTypes.map { substitute(it, substitutions) },
                implementedTypes = declaration.implementedTypes.map { substitute(it, substitutions) },
                memberDeclarations = declaration.memberDeclarations.map { substitute(it, substitutions) },
                typeParameterTypes = declaration.typeParameterTypes.map { substitute(it, substitutions) },
            )

            is DefaultChirStructDeclaration -> declaration.copy(
                fieldDeclarations = declaration.fieldDeclarations.map { substitute(it, substitutions) },
                memberDeclarations = declaration.memberDeclarations.map { substitute(it, substitutions) },
                typeParameterTypes = declaration.typeParameterTypes.map { substitute(it, substitutions) },
            )

            is DefaultChirEnumDeclaration -> declaration.copy(
                memberDeclarations = declaration.memberDeclarations.map { substitute(it, substitutions) },
                typeParameterTypes = declaration.typeParameterTypes.map { substitute(it, substitutions) },
                caseTypes = declaration.caseTypes.map { case ->
                    case.copy(payloadTypes = case.payloadTypes.map { substitute(it, substitutions) })
                },
            )

            is DefaultChirExtendDeclaration -> declaration.copy(
                targetType = substitute(declaration.targetType, substitutions),
                extendedTypes = declaration.extendedTypes.map { substitute(it, substitutions) },
                memberDeclarations = declaration.memberDeclarations.map { substitute(it, substitutions) },
                typeParameterTypes = declaration.typeParameterTypes.map { substitute(it, substitutions) },
            )

            is DefaultChirVariableDeclaration -> declaration.copy(type = substitute(declaration.type, substitutions))
            is DefaultChirPropertyDeclaration -> declaration.copy(type = substitute(declaration.type, substitutions))
            is DefaultChirFunctionDeclaration -> declaration.copy(
                returnType = substitute(declaration.returnType, substitutions),
                parameters = declaration.parameters.map { substitute(it, substitutions) },
                blocks = substitute(declaration.blocks, substitutions),
                genericTypeParameters = declaration.genericTypeParameters.map { substitute(it, substitutions) },
            )

            else -> unsupported("declaration", declaration)
        }

    /** 替换控制流图中表达式、值、终结指令和虚调用签名里的类型引用。 */
    fun substitute(blocks: List<ChirBlock>, substitutions: Map<String, ChirTypeRef>): List<ChirBlock> =
        blocks.map { block ->
            block.copy(
                expressions = block.expressions.map { substitute(it, substitutions) },
                terminator = block.terminator?.let { terminator ->
                    when (terminator) {
                        is ChirReturnTerminator -> terminator.copy(
                            returnValue = terminator.returnValue?.let { substitute(it, substitutions) },
                        )

                        is ChirBranchTerminator -> terminator
                        is ChirConditionalBranchTerminator -> terminator.copy(
                            condition = substitute(terminator.condition, substitutions),
                        )

                        is ChirThrowTerminator -> terminator.copy(
                            exceptionValue = substitute(terminator.exceptionValue, substitutions),
                        )

                        is ChirUnwindTerminator -> terminator
                        else -> unsupported("terminator", terminator)
                    }
                },
            )
        }

    private fun substitute(expression: ChirExpression, substitutions: Map<String, ChirTypeRef>): ChirExpression =
        when (expression) {
            is ChirUnaryExpression -> expression.copy(
                operand = substitute(expression.operand, substitutions),
                resultType = substitute(expression.resultType, substitutions),
                resultLocalValue = expression.resultLocalValue?.let { substitute(it, substitutions) as ChirLocalValue },
            )

            is ChirBinaryExpression -> expression.copy(
                left = substitute(expression.left, substitutions),
                right = substitute(expression.right, substitutions),
                resultType = substitute(expression.resultType, substitutions),
                resultLocalValue = expression.resultLocalValue?.let { substitute(it, substitutions) as ChirLocalValue },
            )

            is ChirMemoryExpression -> expression.copy(
                address = substitute(expression.address, substitutions),
                value = expression.value?.let { substitute(it, substitutions) },
                resultType = expression.resultType?.let { substitute(it, substitutions) },
                resultLocalValue = expression.resultLocalValue?.let { substitute(it, substitutions) as ChirLocalValue },
            )

            is ChirCallExpression -> expression.copy(
                callee = substitute(expression.callee, substitutions),
                arguments = expression.arguments.map { substitute(it, substitutions) },
                resultType = substitute(expression.resultType, substitutions),
                instantiatedTypeArguments = expression.instantiatedTypeArguments.map { substitute(it, substitutions) },
                thisType = expression.thisType?.let { substitute(it, substitutions) },
                dispatchReceiver = expression.dispatchReceiver?.let { substitute(it, substitutions) },
                virtualMethodContext = expression.virtualMethodContext?.let { context ->
                    context.copy(
                        originalFunctionType = substitute(context.originalFunctionType, substitutions),
                        genericTypeParameters = context.genericTypeParameters.map { substitute(it, substitutions) },
                    )
                },
                resultLocalValue = expression.resultLocalValue?.let { substitute(it, substitutions) as ChirLocalValue },
            )

            is ChirTupleExpression -> expression.copy(
                elements = expression.elements.map { substitute(it, substitutions) },
                resultType = substitute(expression.resultType, substitutions),
                resultLocalValue = expression.resultLocalValue?.let { substitute(it, substitutions) as ChirLocalValue },
            )

            is ChirFieldExpression -> expression.copy(
                base = substitute(expression.base, substitutions),
                resultType = substitute(expression.resultType, substitutions),
                resultLocalValue = expression.resultLocalValue?.let { substitute(it, substitutions) as ChirLocalValue },
            )

            is ChirConstantExpression -> expression.copy(
                value = substitute(expression.value, substitutions) as ChirConstantValue,
                resultType = substitute(expression.resultType, substitutions),
                resultLocalValue = substitute(expression.resultLocalValue, substitutions) as ChirLocalValue,
            )

            is ChirOtherExpression -> expression.copy(
                operands = expression.operands.map { substitute(it, substitutions) },
                resultType = expression.resultType?.let { substitute(it, substitutions) },
                intrinsicTypeArguments = expression.intrinsicTypeArguments.map { substitute(it, substitutions) },
                debugMetadata = expression.debugMetadata?.copy(
                    accurateEnvironmentObjectType = expression.debugMetadata.accurateEnvironmentObjectType?.let {
                        substitute(it, substitutions)
                    },
                ),
                resultLocalValue = expression.resultLocalValue?.let { substitute(it, substitutions) as ChirLocalValue },
            )

            else -> unsupported("expression", expression)
        }

    private fun substitute(value: ChirValue, substitutions: Map<String, ChirTypeRef>): ChirValue =
        when (value) {
            is ChirLocalValue -> value.copy(type = substitute(value.type, substitutions))
            is ChirConstantValue -> value.copy(type = substitute(value.type, substitutions))
            is ChirParameterValue -> value.copy(type = substitute(value.type, substitutions))
            is ChirGlobalValue -> value.copy(type = substitute(value.type, substitutions))
            is ChirImportedFunctionValue -> value.copy(type = substitute(value.type, substitutions))
            is ChirImportedVariableValue -> value.copy(type = substitute(value.type, substitutions))
            is ChirFunctionValue -> value.copy(type = substitute(value.type, substitutions))
            is ChirBlockValue -> value.copy(type = substitute(value.type, substitutions))
            is ChirBlockGroupValue -> value.copy(type = substitute(value.type, substitutions))
            else -> unsupported("value", value)
        }

    private fun substitute(
        declaration: ChirVariableDeclaration,
        substitutions: Map<String, ChirTypeRef>,
    ): ChirVariableDeclaration = when (declaration) {
        is DefaultChirVariableDeclaration -> declaration.copy(type = substitute(declaration.type, substitutions))
        is DefaultChirPropertyDeclaration -> declaration.copy(type = substitute(declaration.type, substitutions))
        else -> unsupported("variable declaration", declaration)
    }

    private fun substitute(type: ChirTypeRef, substitutions: Map<String, ChirTypeRef>): ChirTypeRef =
        ChirTypeRelations.substitute(type, substitutions)

    private fun substitute(type: ChirFunctionType, substitutions: Map<String, ChirTypeRef>): ChirFunctionType =
        (ChirTypeRelations.substitute(ChirResolvedTypeRef(type), substitutions) as ChirResolvedTypeRef)
            .type as ChirFunctionType

    private fun <T : Any> unsupported(nodeKind: String, node: T): Nothing =
        error("Unsupported CHIR $nodeKind implementation for type substitution: ${node.javaClass.name}")
}
