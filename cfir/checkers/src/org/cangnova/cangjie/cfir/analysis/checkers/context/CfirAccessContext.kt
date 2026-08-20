package org.cangnova.cangjie.cfir.analysis.checkers.context

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind

/**
 * 将 checker 已维护的文件与声明栈转换为 providers 层的显式访问上下文。
 *
 * 诊断阶段不能再借助解析阶段的 ThreadLocal；缺少 file symbol 时保留 `null`，
 * 由统一 checker 保守拒绝非 public 导出面。
 */
fun CheckerContext.accessContext(kind: CfirAccessKind): CfirAccessContext = CfirAccessContext(
    useSiteFile = containingFileSymbol?.cfir,
    containingDeclarations = containingDeclarations.mapNotNull { it.cfir as? CfirDeclaration },
    kind = kind,
)
