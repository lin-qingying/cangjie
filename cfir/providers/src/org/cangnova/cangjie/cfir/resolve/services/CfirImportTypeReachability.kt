package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.importBindingStore
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.ClassId

/**
 * 判断 [classId] 是否通过当前包、显式 import 或语言默认 import 在 [CfirFile] 中名字可达。
 *
 * 本服务只读取 IMPORTS 阶段建立的结构 binding。缺失 binding 是阶段契约错误，不能退回
 * `DefaultImportsProvider` 或 symbol provider 现场回放，否则 extend、类型与调用解析会形成
 * 不同的导入语义。
 */
fun CfirFile.isClassIdReachableByImports(
    session: CfirSession,
    classId: ClassId,
): Boolean {
    if (classId.packageFqName == packageDirective.packageFqName) return true

    val store = session.importBindingStore
    return sequenceOf(
        store.requireBindings(this).imports,
        store.requireDefaultImportBindings(CfirDefaultImportPriority.HIGH),
        store.requireDefaultImportBindings(CfirDefaultImportPriority.LOW),
    ).flatten().any { binding -> binding.reaches(session, classId) }
}

/** 判断单条已解析 binding 是否把 [classId] 暴露给当前文件。 */
private fun CfirResolvedImportBinding.reaches(session: CfirSession, classId: ClassId): Boolean = targets.any { target ->
    when (target) {
        is CfirResolvedImportTarget.ClassLike -> target.classId == classId
        is CfirResolvedImportTarget.Package -> {
            target.fqName == classId.packageFqName ||
                session.symbolProvider
                    .getClassLikeSymbolByClassId(ClassId(target.fqName, classId.shortClassName))
                    ?.classId == classId
        }
        is CfirResolvedImportTarget.Callable -> false
    }
}
