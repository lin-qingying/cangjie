package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.ClassId

internal class CfirImportBindingResolver(
    private val session: CfirSession,
) {
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

