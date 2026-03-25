package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemUtilContext
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.FixVariableConstraintPosition
import org.cangnova.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker

object ConeConstraintSystemUtilContext : ConstraintSystemUtilContext {
    override fun CangJieTypeMarker.refineType(): CangJieTypeMarker {
        TODO("Not yet implemented")
    }

    override fun createArgumentConstraintPosition(argument: PostponedAtomWithRevisableExpectedType): ConstraintPosition {
        TODO("Not yet implemented")
    }

    override fun <T> createFixVariableConstraintPosition(
        variable: TypeVariableMarker,
        atom: T
    ): FixVariableConstraintPosition<T> {
        TODO("Not yet implemented")
    }

    override fun extractLambdaParameterTypesFromDeclaration(declaration: PostponedAtomWithRevisableExpectedType): List<CangJieTypeMarker?>? {
        TODO("Not yet implemented")
    }

    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpression(): Boolean {
        TODO("Not yet implemented")
    }

    override fun PostponedAtomWithRevisableExpectedType.contextParameterCountOfFunctionExpression(): Int {
        TODO("Not yet implemented")
    }

    override fun PostponedAtomWithRevisableExpectedType.isLambda(): Boolean {
        TODO("Not yet implemented")
    }

    override fun createTypeVariableForLambdaReturnType(): TypeVariableMarker {
        TODO("Not yet implemented")
    }

    override fun createTypeVariableForLambdaParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker {
        TODO("Not yet implemented")
    }

    override fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker {
        return ConeTypeVariableForPostponedAtom(PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE)

    }

    override fun createTypeVariableForCallableReferenceParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker {
        TODO("Not yet implemented")
    }

}