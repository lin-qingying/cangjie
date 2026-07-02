package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.specificTypeMismatchDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory3
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 统一检查声明初始化器类型是否兼容期望类型。
 *
 * 该工具会先处理专门的类型不匹配诊断，再把 ideal 类型按期望类型消解，并对类型变量做稳定化后
 * 交给类型系统子类型判断。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkTypeMismatch(
    expectedType: ConeCangJieType,
    actualType: ConeCangJieType,
    source: AbstractCjSourceElement,
    preferredSpecializedSource: AbstractCjSourceElement? = null,
    diagnosticFactory: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean>,
) {
    if (actualType is ConeErrorType || expectedType is ConeErrorType) return
    val effectiveActualType = IdealTypeResolver.resolveIfIdeal(actualType, expectedType)
    val diagnosticSource = preferredSpecializedSource ?: source
    specificTypeMismatchDiagnostic(
        source = diagnosticSource,
        expectedType = expectedType,
        actualType = effectiveActualType,
        session = context.session,
    )?.let { diagnostic ->
        reporter.report(diagnostic, context)
        return
    }

    val normalizedActualType = effectiveActualType.normalizeForSubtypeCheck()
    val normalizedExpectedType = expectedType.normalizeForSubtypeCheck()
    if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, normalizedActualType, normalizedExpectedType) == true) return
    if (expectedType.isCompatibleWithInitializerActualTypeModuloInferencePlaceholders(effectiveActualType)) return
    reporter.reportOn(
        diagnosticSource,
        diagnosticFactory,
        expectedType,
        actualType,
        false,
    )
}

/**
 * 把约束系统产生的类型变量类型规范化为原始类型参数类型。
 *
 * 这样初始化器检查能复用普通类型参数的子类型规则，而不是把临时类型变量构造器当作不同类型。
 */
private fun ConeCangJieType.normalizeForSubtypeCheck(): ConeCangJieType {
    return when (this) {
        is ConeTypeVariableType -> {
            val originalTypeParameter = typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
            if (originalTypeParameter != null) {
                ConeTypeParameterTypeImpl(originalTypeParameter, attributes)
            } else {
                this
            }
        }

        else -> this
    }
}

/**
 * 判断 initializer 类型是否只是在无上下文 lambda placeholder 上比声明缓存更具体。
 *
 * `let f = { x => ... }` 会先以 synthetic accept 调用建立函数类型占位符，随后真实
 * `f(arg)` 调用再把实参约束导回 lambda 参数。声明 initializer mismatch checker
 * 不能把这类无源码类型参数的内部 type variable 当成最终 expected type；只要结构相同
 * 且差异可由 placeholder 解释，就应交给后续 completion 写回真实函数类型。
 */
private fun ConeCangJieType.isCompatibleWithInitializerActualTypeModuloInferencePlaceholders(
    actualType: ConeCangJieType,
): Boolean {
    if (!containsUnownedInferencePlaceholder()) return false
    return matchesModuloUnownedInferencePlaceholders(actualType)
}

private fun ConeCangJieType.matchesModuloUnownedInferencePlaceholders(
    actualType: ConeCangJieType,
): Boolean {
    if (this == actualType) return true
    if (isUnownedInferencePlaceholder() || actualType.isUnownedInferencePlaceholder()) return true

    return when {
        this is ConeFunctionType && actualType is ConeFunctionType ->
            parameterTypes.size == actualType.parameterTypes.size &&
                    parameterTypes.zip(actualType.parameterTypes).all { (expectedParameter, actualParameter) ->
                        expectedParameter.matchesModuloUnownedInferencePlaceholders(actualParameter)
                    } &&
                    returnType.matchesModuloUnownedInferencePlaceholders(actualType.returnType)

        this is ConeTupleType && actualType is ConeTupleType ->
            elementTypes.size == actualType.elementTypes.size &&
                    elementTypes.zip(actualType.elementTypes).all { (expectedElement, actualElement) ->
                        expectedElement.matchesModuloUnownedInferencePlaceholders(actualElement)
                    }

        this is ConeVArrayType && actualType is ConeVArrayType ->
            size == actualType.size &&
                    elementType.matchesModuloUnownedInferencePlaceholders(actualType.elementType)

        this is ConePointerType && actualType is ConePointerType ->
            pointeeType.matchesModuloUnownedInferencePlaceholders(actualType.pointeeType)

        this is ConeLookupTagBasedType && actualType is ConeLookupTagBasedType ->
            lookupTag == actualType.lookupTag &&
                    typeArguments.size == actualType.typeArguments.size &&
                    typeArguments.zip(actualType.typeArguments).all { (expectedArgument, actualArgument) ->
                        expectedArgument.type.matchesModuloUnownedInferencePlaceholders(actualArgument.type)
                    }

        this is ConeTypeAliasType && actualType is ConeTypeAliasType ->
            classId == actualType.classId &&
                    typeArguments.size == actualType.typeArguments.size &&
                    typeArguments.zip(actualType.typeArguments).all { (expectedArgument, actualArgument) ->
                        expectedArgument.type.matchesModuloUnownedInferencePlaceholders(actualArgument.type)
                    } &&
                    (expandedType?.matchesModuloUnownedInferencePlaceholders(actualType.expandedType ?: return false)
                        ?: (actualType.expandedType == null))

        this is ConeIntersectionType && actualType is ConeIntersectionType ->
            intersectedTypes.size == actualType.intersectedTypes.size &&
                    intersectedTypes.zip(actualType.intersectedTypes).all { (expectedType, actualType) ->
                        expectedType.matchesModuloUnownedInferencePlaceholders(actualType)
                    }

        this is ConeUnionType && actualType is ConeUnionType ->
            unionTypes.size == actualType.unionTypes.size &&
                    unionTypes.zip(actualType.unionTypes).all { (expectedType, actualType) ->
                        expectedType.matchesModuloUnownedInferencePlaceholders(actualType)
                    }

        else -> false
    }
}

private fun ConeCangJieType.containsUnownedInferencePlaceholder(): Boolean =
    contains { it.isUnownedInferencePlaceholder() }

private fun ConeCangJieType.isUnownedInferencePlaceholder(): Boolean =
    this is ConeTypeVariableType && typeConstructor.originalTypeParameter == null
