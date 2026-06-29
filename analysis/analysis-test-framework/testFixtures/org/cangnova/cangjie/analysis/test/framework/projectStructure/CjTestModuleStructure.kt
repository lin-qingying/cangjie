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
    /**
     * 测试基础设施中的原始模块结构。
     */
    val testModuleStructure: TestModuleStructure,
    /**
     * 当前测试用例的主测试模块列表。
     */
    val mainModules: List<CjTestModule>,
) : TestService {
    /**
     * 当前模块结构绑定的 IntelliJ project。
     */
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

    /**
     * 所有可承载源码文件的 Analysis API 模块。
     */
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

    /**
     * 当前测试模块结构中的全部仓颉源码 PSI 文件。
     */
    val allCjFiles: List<CjFile> by lazy(LazyThreadSafetyMode.NONE) {
        allSourceFiles.filterIsInstance<CjFile>()
    }

    /**
     * 当前测试模块结构暴露的全部 Analysis API 模块视图。
     */
    val allCaModules: List<CaModule> by lazy(LazyThreadSafetyMode.NONE) {
        mainModules.flatMap(CjTestModule::allCaModules).distinct()
    }

    /**
     * 当前测试模块结构中的全部 library binary 模块。
     */
    val binaryModules: List<CaLibraryModule> by lazy(LazyThreadSafetyMode.NONE) {
        allCaModules.filterIsInstance<CaLibraryModule>()
    }

    /**
     * 按测试模块名查找主模块。
     */
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

    /**
     * PSI 文件到测试模块的双索引。
     */
    private data class ModuleIndex(
        /**
         * 以 PSI 文件实例为 key 的索引。
         */
        val byPsiFile: Map<PsiFile, CjTestModule>,
        /**
         * 以 VirtualFile URL 为 key 的索引。
         */
        val byVirtualFileUrl: Map<String, CjTestModule>,
    )
}

/**
 * 判断模块是否可以包含源码 PSI 文件。
 */
private val CaModule.canContainSourceFiles: Boolean
    get() = when (this) {
        is CaSourceModule,
        is CaLibrarySourceModule,
        is CaDanglingFileModule,
        is CaNotUnderContentRootModule,
        -> true
        else -> false
    }

/**
 * 测试服务容器中的 Analysis API 测试模块结构 provider。
 */
abstract class CjTestModuleStructureProvider : TestService {
    /**
     * 当前 provider 可访问的测试服务容器。
     */
    protected abstract val testServices: TestServices

    /**
     * 注册当前测试用例构建出的模块结构。
     */
    abstract fun registerModuleStructure(moduleStructure: CjTestModuleStructure)

    /**
     * 返回当前测试用例已注册的模块结构。
     */
    abstract fun getModuleStructure(): CjTestModuleStructure
}

/**
 * 基于单次写入缓存的测试模块结构 provider 实现。
 */
class CjTestModuleStructureProviderImpl(
    /**
     * 当前 provider 可访问的测试服务容器。
     */
    override val testServices: TestServices,
) : CjTestModuleStructureProvider() {
    /**
     * 当前测试用例注册的模块结构。
     */
    private lateinit var moduleStructure: CjTestModuleStructure

    /**
     * 注册模块结构，并禁止同一测试用例重复覆盖。
     */
    override fun registerModuleStructure(moduleStructure: CjTestModuleStructure) {
        require(!this::moduleStructure.isInitialized) {
            "CjTestModuleStructure 已经注册，测试框架不允许重复覆盖。"
        }
        this.moduleStructure = moduleStructure
    }

    /**
     * 返回已注册的模块结构。
     */
    override fun getModuleStructure(): CjTestModuleStructure = moduleStructure
}

/**
 * 当前测试服务容器中的测试模块结构 provider。
 */
val TestServices.cjTestModuleStructureProvider: CjTestModuleStructureProvider
    by TestServices.testServiceAccessor()

/**
 * 当前测试服务容器中的测试模块结构。
 */
val TestServices.cjTestModuleStructure: CjTestModuleStructure
    get() = cjTestModuleStructureProvider.getModuleStructure()
