package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedSymbolError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic

/**
 * 查询 unresolved 错误是否被同一文件的导入失败支配。
 *
 * 官方在包查找失败后不再报告缺失包引起的类型/引用错误。错误节点收集和声明检查
 * 共享该判断，导入根诊断仍由 CfirImportsChecker 报告；其它独立语义错误不受影响。
 */
internal fun ConeDiagnostic.isUnresolvedCascadeAfterFailedImport(context: CheckerContext): Boolean {
    when ((this as? ConeUnreportedDuplicateDiagnostic)?.original ?: this) {
        is ConeUnresolvedNameError,
        is ConeUnresolvedReferenceError,
        is ConeUnresolvedSymbolError,
        is ConeUnresolvedTypeQualifierError -> Unit
        else -> return false
    }
    val file = context.containingFileSymbol?.takeIf { it.isBound }?.cfir ?: return false
    val imports = context.session.importBindingStoreOrNull?.getBindings(file)?.imports ?: return false
    return imports.any { binding ->
        binding.targets.isEmpty() &&
                binding.importDirective.source?.kind?.shouldSkipErrorTypeReporting != true
    }
}
