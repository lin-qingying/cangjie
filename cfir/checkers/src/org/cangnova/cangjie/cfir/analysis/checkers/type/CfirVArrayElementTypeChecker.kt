package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.findUnsupportedVArrayElementType
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangnova.cangjie.cfir.types.ConeVArrayType

/**
 * VArray 元素类型限制检查。
 *
 * 对齐官方 `TypeCheckType.cpp#CheckVArrayType`：
 * - class / interface / enum / generic / 普通函数类型不能直接或间接作为 VArray 元素；
 * - tuple 与 struct 需要递归检查其成员类型；
 * - CPointer / CString 与 CFunc 语义允许，不向内部递归。
 *
 * 按 Kotlin FIR 框架位置，该规则挂在 resolved type-ref checker，而不是
 * 某类声明 checker 上，这样变量、参数、typealias 使用处与构造器类型实参都能统一覆盖。
 */
object CfirVArrayElementTypeChecker : CfirResolvedTypeRefChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: CfirResolvedTypeRef) {
        val sourceTypeRef = typeRef.delegatedTypeRef.originalVArrayTypeRef() ?: return
        val varrayType = typeRef.coneType.fullyExpandedType(context.session) as? ConeVArrayType ?: return
        val unsupportedType = findUnsupportedVArrayElementType(
            type = varrayType.elementType,
            visited = mutableSetOf(),
        ) ?: return

        reporter.reportOn(
            source = sourceTypeRef.elementTypeRef.source ?: sourceTypeRef.source ?: typeRef.source,
            factory = CfirErrors.VARRAY_ARG_TYPE_WITH_REFTYPE,
            a = unsupportedType,
        )
    }

    private fun CfirTypeRef?.originalVArrayTypeRef(): CfirVArrayTypeRef? =
        when (this) {
            is CfirVArrayTypeRef -> this
            is CfirResolvedTypeRef -> delegatedTypeRef.originalVArrayTypeRef()
            else -> null
        }
}
