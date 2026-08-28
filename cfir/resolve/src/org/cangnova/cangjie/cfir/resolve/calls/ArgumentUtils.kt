package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirInoutArgumentExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.collectUpperBounds
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.hasSupertypeWithGivenClassId
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind

/**
 * 将实参类型规整为参数兼容性检查可直接使用的形态。
 *
 * 当前阶段主要展开类型别名，避免调用检查在别名外壳上做构造器比较。
 */
internal fun prepareArgumentType(argumentType: ConeCangJieType, session: CfirSession): ConeCangJieType {
    return argumentType.fullyExpandedType(session)
}

/**
 * 取得调用实参中 `inout` 修饰的真实表达式。
 *
 * 命名实参只改变参数映射，不改变 inout 的目标类型；因此这里递归剥离命名包装，
 * 让候选检查和完成阶段共享同一套参数投影规则。
 */
internal fun CfirExpression.inoutArgumentTargetOrNull(): CfirExpression? = when (this) {
    is CfirInoutArgumentExpression -> expression
    is CfirNamedArgumentExpression -> expression.inoutArgumentTargetOrNull()
    else -> null
}

/**
 * 计算 `inout` 实参对应的候选期望类型。
 *
 * 官方 `ChkFuncArgWithInout` 将 `CPointer<T>` 形参投影为 `T`；当实参是 VArray
 * 时，指针形参保留实参的固定长度，投影为同长度的 `VArray<T, N>`，从而只在
 * 左值检查阶段报告不可修改的属性访问，而不会先产生无关的参数类型不匹配。
 */
internal fun CfirExpression.inoutExpectedTypeOrNull(
    expectedType: ConeCangJieType,
    session: CfirSession,
): ConeCangJieType? {
    val targetExpression = inoutArgumentTargetOrNull() ?: return null
    val expandedExpectedType = expectedType.fullyExpandedType(session)
    if (expandedExpectedType !is ConePointerType) return expectedType

    val pointeeType = expandedExpectedType.pointeeType
    val expandedArgumentType = targetExpression.coneTypeOrNull?.fullyExpandedType(session)
    return if (expandedArgumentType is ConeVArrayType) {
        ConeVArrayType(pointeeType, expandedArgumentType.size)
    } else {
        pointeeType
    }
}


/**
 * 在仓颉调用检查里，如果实参本身是类型参数，而该类型参数的某个上界已经具备期望类型的构造器，
 * 则可以先把实参视为该上界参与约束，而不是直接拿类型参数本身做 subtype 检查。
 *
 * 例如：
 * interface Inv<T>
 * fun <Y> bar(l: Inv<Y>): Y = ...
 *
 * fun <X : Inv<Int64>> foo(x: X) {
 *     val xr = bar(x)
 * }
 *
 * 这里会把 `x` 临时视为其上界 `Inv<Int64>`。
 * 仓颉当前没有柔性类型，也没有 Kotlin 式显式 in/out 变型，
 * 因而这里只做上界替换，不再引入 captured type 中间表示。
 *
 * 这等价于：
 * fun <X : Inv<Int64>> foo(x: X) {
 *     val inv: Inv<Int64> = x
 *     val xr = bar(inv)
 * }
 */
internal fun substituteTypeParameterUpperBoundIfNeeded(
    argumentType: ConeCangJieType,
    expectedType: ConeCangJieType,
    session: CfirSession
): ConeCangJieType {
    val expectedTypeClassId = expectedType.classIdOrPrimitiveClassId ?: return argumentType
    val context = session.typeContext
    val chosenSupertype = when (argumentType) {
        is ConeTypeParameterType -> argumentType.collectUpperBounds(context)
            .singleOrNull { it.hasSupertypeWithGivenClassId(expectedTypeClassId, context) }

        is ConeTypeVariableType -> {
            val originalTypeParameter = argumentType.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
                ?: return argumentType
            ConeTypeParameterTypeImpl(originalTypeParameter, argumentType.attributes)
                .collectUpperBounds(context)
                .singleOrNull { it.hasSupertypeWithGivenClassId(expectedTypeClassId, context) }
                ?: ConeTypeParameterTypeImpl(originalTypeParameter, argumentType.attributes)
        }

        else -> null
    } ?: return argumentType
    return chosenSupertype
}

/**
 * 将约束系统中的临时类型变量规整回原始类型参数类型。
 *
 * 参数兼容性检查只关心声明层面的类型参数关系，不能把候选求解过程中创建的
 * [ConeTypeVariableType] 当作独立源码类型参与最终 subtype 比较。
 */
internal fun normalizeTypeForCompatibilityCheck(type: ConeCangJieType): ConeCangJieType {
    return when (type) {
        is ConeTypeVariableType -> {
            val originalTypeParameter = type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
            if (originalTypeParameter != null) {
                ConeTypeParameterTypeImpl(originalTypeParameter, type.attributes)
            } else {
                type
            }
        }

        else -> type
    }
}

/**
 * 将调用中显式类型实参形成的等式约束应用到参数 expected type。
 *
 * 仓颉 enum constructor 的 owner 泛型、普通泛型调用的显式实参都会先进入候选约束系统；
 * 参数检查和完成写回必须看到同一个已替换的 expected type，lambda / 字面量才能按目标类型定型。
 */
internal fun Candidate.substituteExplicitTypeArgumentConstraints(expectedType: ConeCangJieType): ConeCangJieType {
    val replacements = system.currentStorage().notFixedTypeVariables
        .mapNotNull { (typeConstructor, variable) ->
            val explicitType = variable.constraints
                .firstOrNull { constraint ->
                    constraint.kind == ConstraintKind.EQUALITY &&
                            constraint.position.from is ConeExplicitTypeParameterConstraintPosition
                }
                ?.type as? ConeCangJieType
                ?: return@mapNotNull null
            typeConstructor to explicitType
        }
        .toMap()
    if (replacements.isEmpty()) return expectedType
    return CfirTypeSubstitutorByMap(replacements).substituteOrSelf(expectedType)
}

/**
 * 计算表达式在当前参数位置上的期望类型。
 *
 * 普通参数直接使用参数返回类型；仓颉变长参数在需要按元素检查时会展开为元素类型。
 * [session] 保留在签名中，以便函数类型服务恢复启用时继续沿用同一扩展点。
 */
fun CfirExpression.getExpectedType(
    session: CfirSession,
    parameter: CfirValueParameter,
    unwrapCangjieVariadicParameter: Boolean = false,
): ConeCangJieType {
    val expectedType = if (unwrapCangjieVariadicParameter) {
        parameter.cangjieVariadicElementTypeOrNull() ?: parameter.returnTypeRef.coneType
    } else {
        parameter.returnTypeRef.coneType
    }
    return expectedType
//    if (!session.functionTypeService.hasExtensionKinds()) return expectedType
//    return FunctionTypeKindSubstitutor(session).substituteOrSelf(expectedType)
}
