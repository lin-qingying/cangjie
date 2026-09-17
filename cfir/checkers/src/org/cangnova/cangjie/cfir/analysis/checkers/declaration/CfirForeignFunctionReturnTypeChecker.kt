package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement

/** C/foreign 的返回类型检查只消费有效 C ABI；CFunc 内部签名由类型 checker 统一检查。 */
object CfirForeignFunctionReturnTypeChecker : CfirFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        if (!declaration.status.isC) return
        val ref = declaration.returnTypeRef
        val type = ref.coneTypeOrNull?.fullyExpandedType(context.session) ?: return
        if (type is ConeErrorType) return
        if (!CfirCTypeSemantics.isMetCType(context.session, type)) {
            reporter.reportOn(ref.source, CfirErrors.INVALID_CFUNC_RETURN_TYPE, type)
        }
    }
}

/** named 参数、Unit 和 CType 是三个独立规则，与官方 CheckCFuncParam 顺序一致。 */
object CfirForeignFunctionParameterTypeChecker : CfirFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        if (!declaration.status.isC) return
        for (parameter in declaration.valueParameters) {
            val ref = parameter.returnTypeRef
            val type = ref.coneTypeOrNull?.fullyExpandedType(context.session) ?: continue
            if (type is ConeErrorType) continue
            if (parameter.isNamed) reporter.reportOn(parameter.source, CfirErrors.CFUNC_CANNOT_HAVE_NAMED_ARGS)
            if (type.isUnit) {
                val nameSource = parameter.valueParameterNameDiagnosticSource()
                val typeSource = ref.source
                val source = if (nameSource != null && typeSource != null) {
                    CjOffsetsOnlySourceElement(nameSource.startOffset, typeSource.endOffset)
                } else parameter.source
                reporter.reportOn(source, CfirErrors.CFUNC_CANNOT_HAVE_UNIT_ARGS)
            } else if (!CfirCTypeSemantics.isMetCType(context.session, type)) {
                reporter.reportOn(ref.source, CfirErrors.INVALID_CFUNC_PARAMETER_TYPE, type)
            }
        }
    }
}

/** 从 CFIR 保留的类型语法定位 CFunc 内部类型；不重新解析 PSI 或短名查找 typealias。 */
internal object CfirCFuncTypeLegalityReporter {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun reportNestedDiagnosticsIfCFunc(typeRef: CfirTypeRef): Boolean {
        val resolvedTypeRef = typeRef as? CfirResolvedTypeRef ?: return false
        val cfuncArgument = extractCFuncArgument(resolvedTypeRef) ?: return false
        val function = when (cfuncArgument) {
            is CfirFunctionTypeRef -> cfuncArgument
            is CfirResolvedTypeRef -> cfuncArgument.delegatedTypeRef as? CfirFunctionTypeRef
            else -> null
        }
        if (function == null) {
            reporter.reportOn(
                cfuncArgument.source ?: resolvedTypeRef.source,
                CfirErrors.CFUNC_TYPE,
            )
            return true
        }
        for (ref in function.parameterTypeRefs) {
            val type = ref.coneTypeOrNull?.fullyExpandedType(context.session) ?: continue
            if (type is ConeErrorType) continue
            when {
                type.isUnit -> reporter.reportOn(ref.source, CfirErrors.CFUNC_CANNOT_HAVE_UNIT_ARGS)
                !CfirCTypeSemantics.isMetCType(context.session, type) ->
                    reporter.reportOn(ref.source, CfirErrors.INVALID_CFUNC_PARAMETER_TYPE, type)
            }
        }
        val ref = function.returnTypeRef
        val type = ref.coneTypeOrNull?.fullyExpandedType(context.session)
        if (type != null && type !is ConeErrorType) {
            when {
                !CfirCTypeSemantics.isMetCType(context.session, type) ->
                    reporter.reportOn(ref.source, CfirErrors.INVALID_CFUNC_RETURN_TYPE, type)
                type is ConeVArrayType -> reporter.reportOn(ref.source, CfirErrors.VARRAY_IN_CFUNC)
            }
        }
        return true
    }

    context(context: CheckerContext)
    fun isCFuncSyntax(typeRef: CfirTypeRef): Boolean =
        (typeRef as? CfirResolvedTypeRef)?.let(::extractCFuncArgument) != null

    /**
     * 从已经解析的 CFunc wrapper 恢复其唯一函数类型实参。
     *
     * CFunc 的源码名称不是语义身份：用户声明的同名类型必须走普通 class-like
     * 类型规则。只有 TYPES 阶段已将 ref 构造成 `ConeFunctionType(isCFunc = true)`
     * 时，这里才进入 CFunc 签名检查；嵌套参数/返回类型仍从同一 resolved ref
     * 的 delegated 语法树取得，以保留精确诊断范围。
     */
    private fun extractCFuncArgument(resolved: CfirResolvedTypeRef): CfirTypeRef? {
        val isCFunc = when (val coneType = resolved.coneType) {
            is ConeFunctionType -> coneType.isCFunc
            // Invalid CFunc applications retain the resolved classifier identity
            // in the error lookup tag so their dedicated CFUNC_TYPE diagnostic can
            // still inspect the original function-type argument.
            is ConeErrorType -> coneType.lookupTag.classId == StdlibClassIds.CFunc
            else -> false
        }
        if (!isCFunc) return null
        return (resolved.delegatedTypeRef as? CfirUserTypeRef)
            ?.qualifier
            ?.singleOrNull()
            ?.typeArguments
            ?.singleOrNull()
    }
}
