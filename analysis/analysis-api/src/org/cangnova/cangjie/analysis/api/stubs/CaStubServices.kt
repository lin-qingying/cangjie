package org.cangnova.cangjie.analysis.api.stubs

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind

/**
 * 单文件 stub 读取入口。
 *
 * 面向上层暴露稳定的 stub 视图摘要，不直接泄漏 IntelliJ stub API。
 */
interface CaStubFileProvider {
    fun getFileStubKind(file: CjFile): CangJieFileStubKind?

    fun getTopLevelClassifierNames(file: CjFile): Set<Name>

    fun getTopLevelCallableNames(file: CjFile): Set<Name>
}

/**
 * 包级 stub 索引。
 */
interface CaStubPackageIndex {
    fun getAvailablePackages(): Set<FqName>

    fun getTopLevelClassifierNames(packageFqName: FqName): Set<Name>

    fun getTopLevelCallableNames(packageFqName: FqName): Set<Name>
}

/**
 * Analysis 侧统一 stub facade。
 */
interface CaStubIndexFacade {
    val fileProvider: CaStubFileProvider

    val packageIndex: CaStubPackageIndex

    fun getClassMemberNames(classId: ClassId): Set<Name>

    companion object {
        fun getInstance(project: Project): CaStubIndexFacade = project.service()
    }
}
