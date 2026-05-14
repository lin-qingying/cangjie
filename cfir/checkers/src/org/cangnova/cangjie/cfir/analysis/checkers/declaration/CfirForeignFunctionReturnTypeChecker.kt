package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.Name

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
        if (with(context) { with(reporter) { CfirCFuncTypeLegalityReporter.reportNestedDiagnosticsIfCFunc(returnTypeRef) } }) {
            return
        }
        if (isTypeAliasToCFunc(returnTypeRef)) return
        val returnType = returnTypeRef.coneType
        if (returnType is ConeErrorType) return

        if (CfirForeignFunctionCTypeSemantics.run { with(context) { isMetCType(returnType) } }) return

        reporter.reportOn(
            source = returnTypeRef.source,
            factory = CfirErrors.INVALID_CFUNC_RETURN_TYPE,
            a = returnType,
        )
    }
}

/**
 * foreign 函数参数同样要满足官方 `Ty::IsMetCType`。
 *
 * 这里与返回类型共享同一套 CType 语义：
 * - `CType` / `type Alias = CType` 合法；
 * - 非 CType 的 class / enum / tuple / string 非法；
 * - `CFunc` 还要递归检查参数/返回类型是否满足 CType。
 */
object CfirForeignFunctionParameterTypeChecker : CfirFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        if (!declaration.status.isForeign) return

        for (parameter in declaration.valueParameters) {
            val parameterTypeRef = parameter.returnTypeRef as? CfirResolvedTypeRef ?: continue
            if (with(context) { with(reporter) { CfirCFuncTypeLegalityReporter.reportNestedDiagnosticsIfCFunc(parameterTypeRef) } }) {
                continue
            }
            if (isTypeAliasToCFunc(parameterTypeRef)) continue
            val parameterType = parameterTypeRef.coneType
            if (parameterType is ConeErrorType) continue
            if (CfirForeignFunctionCTypeSemantics.run { with(context) { isMetCType(parameterType) } }) continue

            reporter.reportOn(
                source = parameterTypeRef.source ?: parameter.source ?: declaration.source,
                factory = CfirErrors.INVALID_CFUNC_PARAMETER_TYPE,
                a = parameterType,
            )
        }
    }
}

context(context: CheckerContext)
private fun isTypeAliasToCFunc(typeRef: CfirResolvedTypeRef): Boolean {
    val originalTypeRef = typeRef.delegatedTypeRef as? CfirUserTypeRef ?: return false
    if (originalTypeRef.qualifier.isEmpty()) return false
    val shortName = originalTypeRef.qualifier.last().name
    val packageName = originalTypeRef.qualifier.dropLast(1).joinToString(".") { it.name.asString() }
    val classId = org.cangnova.cangjie.name.ClassId(
        if (packageName.isEmpty()) org.cangnova.cangjie.name.FqName.ROOT else org.cangnova.cangjie.name.FqName(packageName),
        shortName,
    )
    val typeAlias = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirTypeAlias ?: return false
    return CfirCFuncTypeLegalityReporter.run { with(context) { isCFuncSyntax(typeAlias.expandedTypeRef) } }
}

/**
 * foreign C interop 的 CType 判定集中在这里，避免返回类型、参数类型、CFunc 递归约束各写一份。
 */
internal object CfirForeignFunctionCTypeSemantics {
    /**
     * 官方 `IsCStructType` 只接受带 `@C` 互操作边界的 struct。
     */
    val primitiveCTypes: Set<PrimitiveTypeKind> = setOf(
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

    context(context: CheckerContext)
    fun isMetCType(type: ConeCangJieType): Boolean {
        return when (val expandedType = type.fullyExpandedType(context.session)) {
            is ConeVArrayType -> isMetCType(expandedType.elementType)
            is ConePrimitiveType -> expandedType.kind in primitiveCTypes
            is ConePointerType,
            is ConeCStringType,
            is ConeQuestType,
                -> true
            is ConeFunctionType -> expandedType.isCFunc &&
                expandedType.parameterTypes.all { isMetCType(it) } &&
                isMetCType(expandedType.returnType)
            is ConeStructType -> isCStructType(expandedType)
            else -> CfirExtendSemantics.isCType(expandedType.classIdOrPrimitiveClassId)
        }
    }

    context(context: CheckerContext)
    private fun isCStructType(type: ConeStructType): Boolean {
        val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(type.classId) ?: return false
        val declaration = symbol.cfir as? CfirStruct ?: return false
        return CfirExtendSemantics.isForeignInteropBoundary(declaration)
    }
}

/**
 * `CFunc<...>` 的非法参数/返回类型应落在其内部具体类型节点上。
 *
 * 这条规则同时服务于：
 * - `foreign func f(cb: CFunc<(String) -> CType>)`
 * - `type Bad = CFunc<(String) -> CType>`
 */
internal object CfirCFuncTypeLegalityReporter {
    private val cFuncName = Name.identifier("CFunc")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun reportNestedDiagnosticsIfCFunc(typeRef: CfirTypeRef): Boolean {
        val originalTypeRef = when (typeRef) {
            is CfirResolvedTypeRef -> typeRef.delegatedTypeRef ?: return false
            else -> typeRef
        }
        return reportNestedDiagnosticsIfCFuncSyntax(originalTypeRef)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportNestedDiagnosticsIfCFuncSyntax(typeRef: CfirTypeRef): Boolean {
        val cFuncFunctionType = extractCFuncFunctionType(typeRef)
            ?: return false

        checkFunctionType(cFuncFunctionType)
        return true
    }

    context(context: CheckerContext)
    fun isCFuncSyntax(typeRef: CfirTypeRef): Boolean = extractCFuncFunctionType(
        (typeRef as? CfirResolvedTypeRef)?.delegatedTypeRef ?: typeRef
    ) != null

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkFunctionType(functionTypeRef: CfirFunctionTypeRef) {
        for (parameterTypeRef in functionTypeRef.parameterTypeRefs) {
            if (reportNestedDiagnosticsIfCFunc(parameterTypeRef)) continue

            val parameterType = (parameterTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (parameterType is ConeErrorType) continue
            if (CfirForeignFunctionCTypeSemantics.run { with(context) { isMetCType(parameterType) } }) continue

            reporter.reportOn(
                source = parameterTypeRef.source,
                factory = CfirErrors.INVALID_CFUNC_PARAMETER_TYPE,
                a = parameterType,
            )
        }

        val returnTypeRef = functionTypeRef.returnTypeRef
        if (reportNestedDiagnosticsIfCFunc(returnTypeRef)) return

        val returnType = (returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        if (returnType is ConeErrorType) return
        if (CfirForeignFunctionCTypeSemantics.run { with(context) { isMetCType(returnType) } }) return

        reporter.reportOn(
            source = returnTypeRef.source,
            factory = CfirErrors.INVALID_CFUNC_RETURN_TYPE,
            a = returnType,
        )
    }

    private fun CfirTypeRef?.toResolvedFunctionTypeRef(): CfirFunctionTypeRef? {
        return when (this) {
            is CfirFunctionTypeRef -> this
            is CfirResolvedTypeRef -> delegatedTypeRef as? CfirFunctionTypeRef
            else -> null
        }
    }

    private fun extractCFuncFunctionType(typeRef: CfirTypeRef): CfirFunctionTypeRef? {
        return (typeRef as? CfirUserTypeRef)
            ?.qualifier
            ?.singleOrNull()
            ?.takeIf { it.name == cFuncName }
            ?.typeArguments
            ?.singleOrNull()
            .toResolvedFunctionTypeRef()
    }
}
