package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.type

import org.cangnova.cangjie.name.Name

/**
 * 泛型深层检查器（GenericDeep 分组）
 *
 * 对齐 C++ TypeCheckGeneric.cpp:
 * - 泛型参数直接递归（上界引用自身）
 * - 泛型参数间接递归（上界通过与类无关的路径递归引用）
 *
 * 注意：参数个数匹配、约束宽松性、实例化歧义等检查依赖 resolve 阶段的完整信息，
 * 将在 resolve 管线就绪后补充。
 */
object CfirGenericDeepChecker : CfirTypeParameterChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirTypeParameter) {
        checkDirectRecursiveBound(declaration)
        checkIndirectRecursiveBound(declaration)
    }

    /**
     * 泛型参数的上界不能直接引用自身。
     *
     * 对齐 C++ DiagKind::sema_generic_param_directly_recursive:
     * `class Foo<T> where T <: T` 是非法的。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDirectRecursiveBound(typeParam: CfirTypeParameter) {
        val paramName = typeParam.name
        for (boundRef in typeParam.symbol.resolvedBounds) {
            val boundType = boundRef.coneType
            if (boundType is ConeErrorType) continue
            if (boundType is ConeTypeParameterType && boundType.lookupTag.name == paramName) {
                reporter.reportOn(
                    source = boundRef.source ?: typeParam.source,
                    factory = CfirErrors.GENERIC_PARAM_DIRECTLY_RECURSIVE,
                    a = paramName,
                    b = paramName,
                )
            }
        }
    }

    /**
     * 泛型参数的上界不能通过间接路径递归引用自身。
     *
     * 对齐 C++ DiagKind::sema_generic_param_exist_in_class_irrelevant_upperbound_recursively:
     * 检查上界类型的类型参数中是否间接引用了当前泛型参数。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkIndirectRecursiveBound(typeParam: CfirTypeParameter) {
        val paramName = typeParam.name
        for (boundRef in typeParam.symbol.resolvedBounds) {
            val boundType = boundRef.coneType
            if (boundType is ConeErrorType) continue
            if (boundType is ConeTypeParameterType) continue // 直接引用已经由上面检查

            // 检查上界类型的类型参数中是否间接包含当前泛型参数
            if (containsTypeParameterInArgs(boundType, paramName)) {
                reporter.reportOn(
                    source = boundRef.source ?: typeParam.source,
                    factory = CfirErrors.GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY,
                    a = paramName,
                    b = boundType,
                )
            }
        }
    }

    /**
     * 检查类型的类型参数中是否包含指定名称的类型参数引用。
     */
    private fun containsTypeParameterInArgs(
        type: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        name: Name,
    ): Boolean {
        for (arg in type.typeArguments) {
            val argType = arg.type ?: continue
            if (argType is ConeTypeParameterType && argType.lookupTag.name == name) return true
            if (containsTypeParameterInArgs(argType, name)) return true
        }
        return false
    }

}
