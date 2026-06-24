package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaParameterType
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForPostponedAtom
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.resolve.calls.ConeLambdaWithTypeVariableAsExpectedTypeAtom
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.cangnova.cangjie.cfir.types.ConeFunctionType
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

/**
 * CFIR 约束系统工具上下文。
 *
 * 该对象把通用调用推断框架需要的函数类型、延迟实参、lambda 和 callable reference
 * 操作映射到 CFIR/Cone 类型模型。
 */
object ConeConstraintSystemUtilContext : ConstraintSystemUtilContext {
    /**
     * 构造指定参数个数的内建函数类型构造器。
     */
    override fun getBuiltinFunctionTypeConstructor(parametersNumber: Int): TypeConstructorMarker {
        return ConeFunctionType(
            parameterTypes = List(parametersNumber) { ConePrimitiveType.NOTHING },
            returnType = ConePrimitiveType.NOTHING,
        )
    }

    /**
     * 从函数类型中提取参数类型列表。
     */
    override fun CangJieTypeMarker.extractBuiltinFunctionArgumentTypes(): List<CangJieTypeMarker> {
        return (this as? ConeFunctionType)?.parameterTypes.orEmpty()
    }

    /**
     * CFIR 当前没有 captured type 解包需求，直接返回自身。
     */
    override fun CangJieTypeMarker.unCapture(): CangJieTypeMarker = this

    /**
     * CFIR 当前不在该入口执行类型精化，直接返回自身。
     */
    override fun CangJieTypeMarker.refineType(): CangJieTypeMarker = this

    /**
     * 判断类型变量是否带有 only-input-types 语义。
     */
    override fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean {
        if (this !is ConeTypeParameterBasedTypeVariable) return false
        return typeParameterSymbol.cfir.hasOnlyInputTypesAnnotation()
    }

    /**
     * 为延迟实参创建约束位置。
     */
    override fun createArgumentConstraintPosition(argument: PostponedAtomWithRevisableExpectedType): ConstraintPosition {
        require(argument is ConeLambdaWithTypeVariableAsExpectedTypeAtom || argument is org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedCallableReferenceAtom) {
            "${argument::class}"
        }
        val atom = argument as? ConeLambdaWithTypeVariableAsExpectedTypeAtom
            ?: return ConeArgumentConstraintPosition((argument as org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedCallableReferenceAtom).expression)
        return ConeArgumentConstraintPosition(atom.expression)
    }

    /**
     * 为固定类型变量创建约束位置。
     */
    override fun <T> createFixVariableConstraintPosition(
        variable: TypeVariableMarker,
        atom: T,
    ): FixVariableConstraintPosition<T> {
        @Suppress("UNCHECKED_CAST")
        return ConeFixVariableConstraintPosition(variable) as FixVariableConstraintPosition<T>
    }

    /**
     * 从函数表达式声明中提取显式参数类型。
     */
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

    /**
     * 判断延迟实参是否为匿名函数表达式。
     */
    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpression(): Boolean {
        return this is ConeLambdaWithTypeVariableAsExpectedTypeAtom && !anonymousFunction.isLambda
    }

    /**
     * 仓颉函数表达式当前没有 context parameter。
     */
    override fun PostponedAtomWithRevisableExpectedType.contextParameterCountOfFunctionExpression(): Int = 0

    /**
     * 判断延迟实参是否为 lambda。
     */
    override fun PostponedAtomWithRevisableExpectedType.isLambda(): Boolean {
        return this is ConeLambdaWithTypeVariableAsExpectedTypeAtom && anonymousFunction.isLambda
    }

    /**
     * 创建 lambda 返回类型的临时类型变量。
     */
    override fun createTypeVariableForLambdaReturnType(): TypeVariableMarker {
        return ConeTypeVariableForPostponedAtom(PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE)
    }

    /**
     * 创建 lambda 指定参数位置的临时类型变量。
     */
    override fun createTypeVariableForLambdaParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int,
    ): TypeVariableMarker {
        return ConeTypeVariableForLambdaParameterType(
            PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + index,
        )
    }

    /**
     * 创建 callable reference 返回类型的临时类型变量。
     */
    override fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker {
        return ConeTypeVariableForPostponedAtom(PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE)
    }

    /**
     * 创建 callable reference 指定参数位置的临时类型变量。
     */
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
