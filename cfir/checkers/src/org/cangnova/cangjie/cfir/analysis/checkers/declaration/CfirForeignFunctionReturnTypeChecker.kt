package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/**
 * 官方 C++ Sema 的 `sema_invalid_cfunc_return_type` 落在 CFFI / signature legality 子域。
 *
 * 在当前 first-party 前端里，可稳定进入这条语义链的源定义入口是 `foreign func`，
 * 因此这里放在 declaration checker 层，而不是塞进通用 type mismatch：
 * - 它描述的是“foreign 函数签名本身是否合法”；
 * - 不是某个调用点或表达式上下文里的局部类型不匹配。
 */
object CfirForeignFunctionReturnTypeChecker : CfirFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        if (!declaration.status.isForeign) return

        val returnTypeRef = declaration.returnTypeRef as? CfirResolvedTypeRef ?: return
        val returnType = returnTypeRef.coneType
        if (returnType is ConeErrorType) return

        val expandedReturnType = returnType.fullyExpandedType(context.session)
        if (isMetCType(expandedReturnType)) return

        reporter.reportOn(
            source = returnTypeRef.source,
            factory = CfirErrors.INVALID_CFUNC_RETURN_TYPE,
            a = returnType,
        )
    }

    /**
     * 对齐官方 `Ty::IsMetCType`，判断类型是否已经实例化为可用于 C 互操作的具体 CType。
     */
    context(context: CheckerContext)
    private fun isMetCType(type: ConeCangJieType): Boolean = when (type) {
        is ConeVArrayType -> isMetCType(type.elementType)
        is ConePrimitiveType -> type.kind in primitiveCTypes
        is ConePointerType,
        is ConeCStringType,
        is ConeQuestType,
            -> true
        is ConeFunctionType -> type.isCFunc
        is ConeStructType -> isCStructType(type)
        else -> false
    }

    /**
     * 官方 `IsCStructType` 只接受带 `@C` 互操作边界的 struct。
     */
    context(context: CheckerContext)
    private fun isCStructType(type: ConeStructType): Boolean {
        val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(type.classId) ?: return false
        val declaration = symbol.cfir as? CfirStruct ?: return false
        return CfirExtendSemantics.isForeignInteropBoundary(declaration)
    }

    private val primitiveCTypes: Set<PrimitiveTypeKind> = setOf(
        PrimitiveTypeKind.UNIT,
        PrimitiveTypeKind.BOOLEAN,
        PrimitiveTypeKind.INT8,
        PrimitiveTypeKind.UINT8,
        PrimitiveTypeKind.INT16,
        PrimitiveTypeKind.UINT16,
        PrimitiveTypeKind.INT32,
        PrimitiveTypeKind.UINT32,
        PrimitiveTypeKind.INT64,
        PrimitiveTypeKind.UINT64,
        PrimitiveTypeKind.INT_NATIVE,
        PrimitiveTypeKind.UINT_NATIVE,
        PrimitiveTypeKind.FLOAT32,
        PrimitiveTypeKind.FLOAT64,
    )
}
