package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices

/**
 * Analysis API 测试模块结构。
 *
 * 对齐 Kotlin `KtTestModuleStructure` 的职责，保存原始 `TestModuleStructure`
 * 与 Analysis API 模块图之间的映射关系。
 */
class CjTestModuleStructure(
    val testModuleStructure: TestModuleStructure,
    val mainModules: List<CjTestModule>,
) : TestService {
    val project: Project
        get() = mainModules.first().caModule.project

    /**
     * 测试框架内部使用的文件到模块索引。
     *
     * Analysis API 测试数据在单个用例生命周期内是静态的，
     * 因此这里显式缓存：
     * 1. 物理/内存 PSI 文件实例
     * 2. VirtualFile URL
     *
     * 后续所有“PSI -> 测试模块”恢复都应复用该索引，
     * 而不是在 provider、base test、configurator 里各自遍历 `mainModules`。
     */
    private val moduleIndex: ModuleIndex by lazy(LazyThreadSafetyMode.NONE) {
        val byPsiFile = linkedMapOf<PsiFile, CjTestModule>()
        val byVirtualFileUrl = linkedMapOf<String, CjTestModule>()
        mainModules.forEach { testModule ->
            testModule.psiFiles.forEach { psiFile ->
                byPsiFile[psiFile] = testModule
                psiFile.virtualFile?.url?.let { fileUrl ->
                    val previous = byVirtualFileUrl.put(fileUrl, testModule)
                    check(previous == null || previous === testModule) {
                        "Analysis API 测试模块结构中出现重复文件映射 `$fileUrl`。"
                    }
                }
            }
        }
        ModuleIndex(
            byPsiFile = byPsiFile,
            byVirtualFileUrl = byVirtualFileUrl,
        )
    }

    val allSourceLikeModules: List<CaModule> by lazy(LazyThreadSafetyMode.NONE) {
        mainModules.mapNotNull { testModule ->
            testModule.caModule.takeIf(CaModule::canContainSourceFiles)
        }
    }

    /**
     * 对齐 Kotlin `KtTestModuleStructure.allSourceFiles`：
     * 这里只暴露真正 source-like 模块的文件，不能把 `LibraryBinaryDecompiled`
     * 的反编译库文件混进 source providers，否则会在建包/声明 provider 时递归触发 library session。
     */
    val allSourceFiles: List<PsiFileSystemItem> by lazy(LazyThreadSafetyMode.NONE) {
        mainModules
            .filter { it.caModule.canContainSourceFiles }
            .flatMap(CjTestModule::psiFiles)
            .filterIsInstance<PsiFileSystemItem>()
    }

    val allCjFiles: List<CjFile> by lazy(LazyThreadSafetyMode.NONE) {
        allSourceFiles.filterIsInstance<CjFile>()
    }

    val allCaModules: List<CaModule> by lazy(LazyThreadSafetyMode.NONE) {
        mainModules.flatMap(CjTestModule::allCaModules).distinct()
    }

    val binaryModules: List<CaLibraryModule> by lazy(LazyThreadSafetyMode.NONE) {
        allCaModules.filterIsInstance<CaLibraryModule>()
    }

    fun getModule(moduleName: String): CjTestModule =
        mainModules.first { it.name == moduleName }

    /**
     * 按 PSI 文件恢复测试模块。
     */
    fun findModuleByFile(file: PsiFile): CjTestModule? {
        return moduleIndex.byPsiFile[file]
            ?: file.virtualFile?.url?.let(moduleIndex.byVirtualFileUrl::get)
    }

    /**
     * 按 PSI 文件恢复测试模块；不存在时直接报错。
     */
    fun requireModuleByFile(file: PsiFile): CjTestModule {
        return findModuleByFile(file)
            ?: error("Cannot find CjTestModule for `${file.name}` in Analysis API test module structure.")
    }

    private data class ModuleIndex(
        val byPsiFile: Map<PsiFile, CjTestModule>,
        val byVirtualFileUrl: Map<String, CjTestModule>,
    )
}

private val CaModule.canContainSourceFiles: Boolean
    get() = when (this) {
        is CaSourceModule,
        is CaLibrarySourceModule,
        is CaDanglingFileModule,
        is CaNotUnderContentRootModule,
        -> true
        else -> false
    }

abstract class CjTestModuleStructureProvider : TestService {
    protected abstract val testServices: TestServices

    abstract fun registerModuleStructure(moduleStructure: CjTestModuleStructure)

    abstract fun getModuleStructure(): CjTestModuleStructure
}

class CjTestModuleStructureProviderImpl(
    override val testServices: TestServices,
) : CjTestModuleStructureProvider() {
    private lateinit var moduleStructure: CjTestModuleStructure

    override fun registerModuleStructure(moduleStructure: CjTestModuleStructure) {
        require(!this::moduleStructure.isInitialized) {
            "CjTestModuleStructure 已经注册，测试框架不允许重复覆盖。"
        }
        this.moduleStructure = moduleStructure
    }

    override fun getModuleStructure(): CjTestModuleStructure = moduleStructure
}

val TestServices.cjTestModuleStructureProvider: CjTestModuleStructureProvider
    by TestServices.testServiceAccessor()

val TestServices.cjTestModuleStructure: CjTestModuleStructure
    get() = cjTestModuleStructureProvider.getModuleStructure()
