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
    /**
     * 提供 [CaStubPlatformState] 服务的 IntelliJ project。
     */
    private val project: Project,
) : CaStubFileProvider {
    /**
     * 当前项目的 analysis:stubs 快照状态服务。
     */
    private val state: CaStubPlatformState
        get() = project.getService(CaStubPlatformState::class.java)

    /**
     * 返回指定文件的 stub kind。
     */
    override fun getFileStubKind(file: CjFile): CangJieFileStubKind? {
        return state.snapshot().findFileSummary(file)?.stubKind
    }

    /**
     * 返回指定文件内的顶层 classifier 名称集合。
     */
    override fun getTopLevelClassifierNames(file: CjFile): Set<Name> {
        return state.snapshot().findFileSummary(file)?.topLevelClassifierNames.orEmpty()
    }

    /**
     * 返回指定文件内的顶层 callable 名称集合。
     */
    override fun getTopLevelCallableNames(file: CjFile): Set<Name> {
        return state.snapshot().findFileSummary(file)?.topLevelCallableNames.orEmpty()
    }
}

/**
 * 包级 stub 索引实现。
 */
internal class CaStubPackageIndexImpl(
    /**
     * 提供 [CaStubPlatformState] 服务的 IntelliJ project。
     */
    private val project: Project,
) : CaStubPackageIndex {
    /**
     * 当前项目的 analysis:stubs 快照状态服务。
     */
    private val state: CaStubPlatformState
        get() = project.getService(CaStubPlatformState::class.java)

    /**
     * 返回当前快照中存在 classifier 或 callable 的包名集合。
     */
    override fun getAvailablePackages(): Set<FqName> {
        val snapshot = state.snapshot()
        return snapshot.packageClassifierNames.keys + snapshot.packageCallableNames.keys
    }

    /**
     * 返回指定包中的顶层 classifier 名称集合。
     */
    override fun getTopLevelClassifierNames(packageFqName: FqName): Set<Name> {
        return state.snapshot().packageClassifierNames[packageFqName].orEmpty()
    }

    /**
     * 返回指定包中的顶层 callable 名称集合。
     */
    override fun getTopLevelCallableNames(packageFqName: FqName): Set<Name> {
        return state.snapshot().packageCallableNames[packageFqName].orEmpty()
    }
}

/**
 * analysis 层统一 stub facade。
 */
internal class CaStubIndexFacadeImpl(
    /**
     * 提供各 stub service 实例的 IntelliJ project。
     */
    private val project: Project,
) : CaStubIndexFacade {
    /**
     * 单文件 stub 查询入口。
     */
    override val fileProvider: CaStubFileProvider
        get() = project.getService(CaStubFileProvider::class.java)

    /**
     * 包级 stub 查询入口。
     */
    override val packageIndex: CaStubPackageIndex
        get() = project.getService(CaStubPackageIndex::class.java)

    /**
     * 当前项目的 analysis:stubs 快照状态服务。
     */
    private val state: CaStubPlatformState
        get() = project.getService(CaStubPlatformState::class.java)

    /**
     * 返回指定 class-like 声明的成员名称集合。
     */
    override fun getClassMemberNames(classId: ClassId): Set<Name> {
        return state.snapshot().classMemberNames[classId].orEmpty()
    }
}

/**
 * 在快照中查找指定 PSI 文件对应的单文件摘要。
 */
private fun CaStubSnapshot.findFileSummary(file: CjFile): CaStubFileSummary? {
    val fileKey = file.virtualFile?.url ?: file.name
    return fileSummaries[fileKey]
}
