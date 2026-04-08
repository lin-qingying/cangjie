package org.cangnova.cangjie.analysis.stubs

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.stubs.CaStubFileProvider
import org.cangnova.cangjie.analysis.api.stubs.CaStubIndexFacade
import org.cangnova.cangjie.analysis.api.stubs.CaStubPackageIndex
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind

/**
 * 单文件 stub 读取入口实现。
 */
internal class CaStubFileProviderImpl(
    private val project: Project,
) : CaStubFileProvider {
    private val state: CaStubPlatformState
        get() = project.getService(CaStubPlatformState::class.java)

    override fun getFileStubKind(file: CjFile): CangJieFileStubKind? {
        return state.snapshot().findFileSummary(file)?.stubKind
    }

    override fun getTopLevelClassifierNames(file: CjFile): Set<Name> {
        return state.snapshot().findFileSummary(file)?.topLevelClassifierNames.orEmpty()
    }

    override fun getTopLevelCallableNames(file: CjFile): Set<Name> {
        return state.snapshot().findFileSummary(file)?.topLevelCallableNames.orEmpty()
    }
}

/**
 * 包级 stub 索引实现。
 */
internal class CaStubPackageIndexImpl(
    private val project: Project,
) : CaStubPackageIndex {
    private val state: CaStubPlatformState
        get() = project.getService(CaStubPlatformState::class.java)

    override fun getAvailablePackages(): Set<FqName> {
        val snapshot = state.snapshot()
        return snapshot.packageClassifierNames.keys + snapshot.packageCallableNames.keys
    }

    override fun getTopLevelClassifierNames(packageFqName: FqName): Set<Name> {
        return state.snapshot().packageClassifierNames[packageFqName].orEmpty()
    }

    override fun getTopLevelCallableNames(packageFqName: FqName): Set<Name> {
        return state.snapshot().packageCallableNames[packageFqName].orEmpty()
    }
}

/**
 * analysis 层统一 stub facade。
 */
internal class CaStubIndexFacadeImpl(
    private val project: Project,
) : CaStubIndexFacade {
    override val fileProvider: CaStubFileProvider
        get() = project.getService(CaStubFileProvider::class.java)

    override val packageIndex: CaStubPackageIndex
        get() = project.getService(CaStubPackageIndex::class.java)

    private val state: CaStubPlatformState
        get() = project.getService(CaStubPlatformState::class.java)

    override fun getClassMemberNames(classId: ClassId): Set<Name> {
        return state.snapshot().classMemberNames[classId].orEmpty()
    }
}

private fun CaStubSnapshot.findFileSummary(file: CjFile): CaStubFileSummary? {
    val fileKey = file.virtualFile?.url ?: file.name
    return fileSummaries[fileKey]
}
