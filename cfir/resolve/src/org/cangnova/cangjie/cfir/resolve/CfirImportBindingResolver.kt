package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.ClassId

/** 将 raw import 指令解析为结构化 import binding 的解析器。 */
internal class CfirImportBindingResolver(
    /** 当前解析 session，用于查询包、class-like 与顶层 callable。 */
    private val session: CfirSession,
) {
    /**
     * 解析单条 [CfirImport] 的有效导入名和候选目标集合。
     *
     * 目标可能同时包含 package、class-like 与 callable；all-under import 不在这里展开具体成员。
     */
    fun resolveImportBinding(importDirective: CfirImport): CfirResolvedImportBinding {
        val importedFqName = importDirective.importedFqName
        val aliasName = importDirective.aliasName
        val effectiveName = when {
            aliasName != null -> aliasName
            importedFqName != null -> importedFqName.shortNameAsIdentifier()
            else -> org.cangnova.cangjie.name.Name.identifier("")
        }
        val targets = mutableListOf<CfirResolvedImportTarget>()

        if (importedFqName != null && session.symbolProvider.hasPackage(importedFqName)) {
            targets += CfirResolvedImportTarget.Package(importedFqName)
        }

        if (importedFqName != null) {
            val memberName = importedFqName.shortNameAsIdentifier()
            val packageFqName = importedFqName.parentOrRoot()

            if (!importDirective.isAllUnder) {
                val classId = ClassId(packageFqName, memberName)
                session.symbolProvider.getClassLikeSymbolByClassId(classId)?.let { symbol ->
                    targets += CfirResolvedImportTarget.ClassLike(
                        classId = classId,
                        symbol = symbol,
                    )
                }

                val callableSymbols = session.symbolProvider.getTopLevelCallableSymbols(packageFqName, memberName)
                if (callableSymbols.isNotEmpty()) {
                    targets += CfirResolvedImportTarget.Callable(
                        packageFqName = packageFqName,
                        name = memberName,
                        symbols = callableSymbols,
                    )
                }
            }
        }

        return CfirResolvedImportBinding(
            importDirective = importDirective,
            effectiveName = effectiveName,
            targets = targets,
        )
    }
}
