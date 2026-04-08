package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaParameterType
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForPostponedAtom
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.resolve.calls.ConeLambdaWithTypeVariableAsExpectedTypeAtom
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemUtilContext
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.FixVariableConstraintPosition
import org.cangnova.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker

object ConeConstraintSystemUtilContext : ConstraintSystemUtilContext {
    override fun getBuiltinFunctionTypeConstructor(parametersNumber: Int): TypeConstructorMarker {
        return ConeFuncType(
            parameterTypes = List(parametersNumber) { ConePrimitiveType.NOTHING },
            returnType = ConePrimitiveType.NOTHING,
        )
    }

    override fun CangJieTypeMarker.extractBuiltinFunctionArgumentTypes(): List<CangJieTypeMarker> {
        return (this as? ConeFuncType)?.parameterTypes.orEmpty()
    }

    override fun CangJieTypeMarker.unCapture(): CangJieTypeMarker = this

    override fun CangJieTypeMarker.refineType(): CangJieTypeMarker = this

    override fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean {
        if (this !is ConeTypeParameterBasedTypeVariable) return false
        return typeParameterSymbol.cfir.hasOnlyInputTypesAnnotation()
    }

    override fun createArgumentConstraintPosition(argument: PostponedAtomWithRevisableExpectedType): ConstraintPosition {
        require(argument is ConeLambdaWithTypeVariableAsExpectedTypeAtom || argument is org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedCallableReferenceAtom) {
            "${argument::class}"
        }
        val atom = argument as? ConeLambdaWithTypeVariableAsExpectedTypeAtom
            ?: return ConeArgumentConstraintPosition((argument as org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedCallableReferenceAtom).expression)
        return ConeArgumentConstraintPosition(atom.expression)
    }

    override fun <T> createFixVariableConstraintPosition(
        variable: TypeVariableMarker,
        atom: T,
    ): FixVariableConstraintPosition<T> {
        @Suppress("UNCHECKED_CAST")
        return ConeFixVariableConstraintPosition(variable) as FixVariableConstraintPosition<T>
    }

    override fun extractLambdaParameterTypesFromDeclaration(
        declaration: PostponedAtomWithRevisableExpectedType,
    ): List<CangJieTypeMarker?>? {
        declaration as? ConeLambdaWithTypeVariableAsExpectedTypeAtom ?: return null
        val anonymousFunction = declaration.anonymousFunction
        if (anonymousFunction.isLambda && !anonymousFunction.hasExplicitParameterList) {
            return null
        }
        return anonymousFunction.valueParameters.map { it.returnTypeRef.coneTypeOrNull }
    }

    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpression(): Boolean {
        return this is ConeLambdaWithTypeVariableAsExpectedTypeAtom && !anonymousFunction.isLambda
    }

    override fun PostponedAtomWithRevisableExpectedType.contextParameterCountOfFunctionExpression(): Int = 0

    override fun PostponedAtomWithRevisableExpectedType.isLambda(): Boolean {
        return this is ConeLambdaWithTypeVariableAsExpectedTypeAtom && anonymousFunction.isLambda
    }

    override fun createTypeVariableForLambdaReturnType(): TypeVariableMarker {
        return ConeTypeVariableForPostponedAtom(PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE)
    }

    override fun createTypeVariableForLambdaParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int,
    ): TypeVariableMarker {
        return ConeTypeVariableForLambdaParameterType(
            PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + index,
        )
    }

    override fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker {
        return ConeTypeVariableForPostponedAtom(PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE)
    }

    override fun createTypeVariableForCallableReferenceParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int,
    ): TypeVariableMarker {
        return ConeTypeVariableForPostponedAtom(
            PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE + index,
        )
    }
}

/**
 * `OnlyInputTypes` 属于类型参数声明元数据，不应该散落在约束系统主流程里到处判断。
 *
 * 当前 first-party 前端的“类型参数注解 -> CFIR annotations”入口还没完全对齐；
 * 这里先把语义查询集中起来，等入口补齐后即可直接生效。
 */
private fun CfirTypeParameter.hasOnlyInputTypesAnnotation(): Boolean {
    return annotations.any { annotation ->
        val annotationText = annotation.typeRef.source?.text?.toString().orEmpty()
        annotationText == "OnlyInputTypes" || annotationText.endsWith(".OnlyInputTypes")
    }
}
