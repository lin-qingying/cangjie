package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.name.ClassId

/**
 * 判断 [classId] 是否通过当前包、显式导入或语言默认导入在 [CfirFile] 中名字可达。
 *
 * 返回 `null` 表示显式 import binding 尚未建立。调用方可根据所处阶段决定是否延迟判定；
 * 已知的同包和默认导入事实不依赖 IMPORTS 阶段，因此始终返回确定结果。
 */
fun CfirFile.isClassIdReachableByImports(
    session: CfirSession,
    classId: ClassId,
): Boolean? {
    if (classId.packageFqName == packageDirective.packageFqName) return true

    val classFqName = classId.asSingleFqName()
    val defaultImportsProvider = session.defaultImportsProvider
    if (classFqName !in defaultImportsProvider.excludedImports) {
        val reachableFromDefaultImports = defaultImportsProvider
            .getDefaultImports(includeLowPriorityImports = true)
            .any { importPath ->
                if (importPath.isAllUnder) {
                    importPath.fqName == classId.packageFqName
                } else {
                    importPath.fqName == classFqName
                }
            }
        if (reachableFromDefaultImports) return true
    }

    val bindings = session.importBindingStoreOrNull?.getBindings(this) ?: return null
    return bindings.imports.any { binding ->
        binding.targets.any { target ->
            when (target) {
                is CfirResolvedImportTarget.ClassLike -> target.classId == classId
                is CfirResolvedImportTarget.Package -> target.fqName == classId.packageFqName
                is CfirResolvedImportTarget.Callable -> false
            }
        }
    }
}
