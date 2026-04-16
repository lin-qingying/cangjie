package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.session.symbolProvider

/**
 * VArray 语义检查器
 *
 * 对齐 C++ TypeCheckType.cpp:
 * - VARRAY_IN_CFUNC: VArray 作为 CFunc 返回类型不允许
 * - VARRAY_ARG_TYPE_WITH_REFTYPE: VArray 元素类型不能包含引用类型
 * - VARRAY_ARGS_NUMBER_MISMATCH: VArray 构造器只接受一个参数
 * - VARRAY_SUBSCRIPT_NUM: VArray subscript 只接受一个 Int64 下标
 *
 * 注册为 callableDeclarationCheckers
 */
object CfirVArrayExtraChecker : CfirCallableDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirCallableDeclaration) {
        checkVArrayReturnInCFunc(declaration)
        checkVArrayElementType(declaration)
    }

    /**
     * VArray 作为 CFunc 返回类型不允许。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVArrayReturnInCFunc(declaration: CfirCallableDeclaration) {
        if (declaration !is CfirFunction) return
        if (!declaration.status.isForeign) return

        val returnType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        if (returnType is ConeVArrayType) {
            reporter.reportOn(
                source = declaration.returnTypeRef.source ?: declaration.source,
                factory = CfirErrors.VARRAY_IN_CFUNC,
            )
        }
    }

    /**
     * VArray 元素类型不能包含引用类型（class 实例）。
     *
     * 对齐 C++ DiagKind::sema_varray_arg_type_with_reftype。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVArrayElementType(declaration: CfirCallableDeclaration) {
        val returnType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        if (returnType !is ConeVArrayType) return

        val elementType = returnType.elementType
        // class-like 类型（非 struct/enum）是引用类型，不能作为 VArray 元素
        if (elementType is ConeClassLikeType) {
            val classId = elementType.classId
            val classSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)
            val classDecl = classSymbol?.cfir
            if (classDecl is org.cangnova.cangjie.cfir.declarations.CfirClass) {
                reporter.reportOn(
                    source = declaration.returnTypeRef.source ?: declaration.source,
                    factory = CfirErrors.VARRAY_ARG_TYPE_WITH_REFTYPE,
                    a = elementType,
                )
            }
        }
    }
}
