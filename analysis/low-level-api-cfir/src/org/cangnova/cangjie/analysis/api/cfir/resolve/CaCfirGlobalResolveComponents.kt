package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.CaLibraryModule
import org.cangnova.cangjie.analysis.api.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.CaSourceModule
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * low-level CFIR 全局解析组件。
 *
 * 这一层对应 Kotlin `LLFirGlobalResolveComponents` 的项目级职责：
 * 1. 汇聚平台 project structure 服务；
 * 2. 统一管理 low-level session cache；
 * 3. 为 module-level resolve components 提供稳定的模块名、源码文件集合与语言版本配置。
 *
 * 这些信息如果继续散落在 facade service 和上层 Analysis API 组件里，模块图与失效边界会再次分裂。
 */
internal class CaCfirGlobalResolveComponents(
    val project: Project,
) {
    private val moduleProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaModuleProvider.getInstance(project)
    }

    val sessionCache: CaCfirSessionCache by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaCfirSessionCache(this)
    }

    /**
     * 统一生成 low-level session 绑定的模块名。
     *
     * 优先使用 `stableModuleName`，保证 session cache、模块边界与符号身份的一致性；
     * 若平台模块未提供稳定名，再显式退回 `moduleDescription`，但仍在这里集中完成 `Name` 建模。
     */
    fun getModuleName(module: CaModule): Name {
        val stableName = module.stableModuleName ?: module.moduleDescription
        return Name.identifierIfValid(stableName) ?: Name.special("<$stableName>")
    }

    /**
     * 统一提取模块可参与 Raw CFIR 构建的源码文件。
     *
     * 对显式暴露 roots 的模块，优先使用结构化 roots；
     * 对 not-under-content-root 这类由内容作用域决定的模块，则从项目结构暴露的全量文件中按 contentScope 过滤。
     */
    fun getSourceFiles(module: CaModule): List<CjFile> {
        val directRoots = when (module) {
            is CaSourceModule -> module.psiRoots
            is CaLibrarySourceModule -> module.sourceRoots
            else -> emptyList()
        }
        if (directRoots.isNotEmpty()) {
            return directRoots.filterIsInstance<CjFile>()
        }

        return moduleProvider.snapshot.allSourceFiles
            .asSequence()
            .filterIsInstance<CjFile>()
            .filter { file -> file.virtualFile?.let(module.contentScope::contains) == true }
            .toList()
    }

    /**
     * 抽取 low-level session 构建使用的语言版本配置。
     *
     * 只有源码形态模块在 Analysis API 层显式携带语言版本；其余模块当前仍归一到默认配置。
     */
    fun getLanguageVersionSettings(module: CaModule): LanguageVersionSettings {
        return when (module) {
            is CaSourceModule -> module.languageVersionSettings
            else -> LanguageVersionSettings.DEFAULT
        }
    }

    /**
     * 判断该模块是否可以直接作为 use-site 进入 low-level 解析。
     *
     * builtins / binary library / script dependency 这类模块只承载依赖边界，不应被误当成可构建 Raw CFIR 的 use-site 模块。
     */
    fun isResolvableSourceLikeModule(module: CaModule): Boolean {
        return when (module) {
            is CaLibraryModule,
            is CaBuiltinsModule,
            is CaSourceModule,
            is CaLibrarySourceModule,
            is CaNotUnderContentRootModule -> true

            else -> module.isResolvable && getSourceFiles(module).isNotEmpty()
        }
    }

    /**
     * 查询当前平台快照中允许直接作为 use-site 的模块。
     */
    fun getResolvableModules(): List<CaModule> {
        return moduleProvider.snapshot.allResolvableModules
    }

    /**
     * 为当前 use-site 模块创建统一的 low-level 解析策略提供器。
     *
     * 这一步把“模块类型 -> 解析方式”的分支逻辑从 session cache 和 facade service 中抽离出来，
     * 后续 lazy declaration resolve、scope cache、diagnostic provider 都应复用同一策略对象。
     */
    fun createResolutionStrategyProvider(useSiteModule: CaModule): CaCfirModuleResolutionStrategyProvider {
        return when (useSiteModule) {
            is CaDanglingFileModule -> {
                val contextProvider = useSiteModule.contextModule?.let(::createResolutionStrategyProvider)
                    ?: CaCfirSourceModuleResolutionStrategyProvider(useSiteModule)
                CaCfirDanglingFileResolutionStrategyProvider(contextProvider)
            }
            is CaSourceModule -> CaCfirSourceModuleResolutionStrategyProvider(useSiteModule)
            is CaLibraryModule,
            is CaBuiltinsModule,
            is CaLibrarySourceModule -> CaCfirBinaryModuleResolutionStrategyProvider(useSiteModule)
            is CaNotUnderContentRootModule -> CaCfirSimpleResolutionStrategyProvider(useSiteModule)
            else -> CaCfirSimpleResolutionStrategyProvider(useSiteModule)
        }
    }

    /**
     * 暴露模块在 low-level 视角下的根文件集合，便于 module resolve components 做统一建模与调试。
     */
    fun getVisibleRoots(module: CaModule): List<PsiFileSystemItem> {
        return when (module) {
            is CaSourceModule -> module.psiRoots
            is CaLibraryModule -> module.binaryRoots
            is CaLibrarySourceModule -> module.sourceRoots
            else -> getSourceFiles(module)
        }
    }
}
