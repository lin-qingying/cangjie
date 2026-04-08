package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 官方 C++ Sema 的 `sema_invalid_cfunc_return_type` 落在 CFFI / signature legality 子域。
 *
 * 在当前 first-party 前端里，可稳定进入这条语义链的源定义入口是 `foreign func`，
 * 因此这里放在 declaration checker 层，而不是塞进通用 type mismatch：
 * - 它描述的是“foreign 函数签名本身是否合法”；
 * - 不是某个调用点或表达式上下文里的局部类型不匹配。
 */
object CfirForeignFunctionReturnTypeChecker : CfirFunctionChecker() {
    private val cTypeClassId = ClassId.topLevel(StandardNames.FqNames.ctypeFqName)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        if (!declaration.status.isForeign) return

        val returnTypeRef = declaration.returnTypeRef as? CfirResolvedTypeRef ?: return
        val returnType = returnTypeRef.coneType
        if (returnType is ConeErrorType) return

        val cTypeSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(cTypeClassId) ?: return
        val cType = cTypeSymbol.constructType()
        val expandedReturnType = returnType.fullyExpandedType(context.session)
        if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, expandedReturnType, cType)) return

        reporter.reportOn(
            source = returnTypeRef.source,
            factory = CfirErrors.INVALID_CFUNC_RETURN_TYPE,
            a = returnType,
        )
    }
}
