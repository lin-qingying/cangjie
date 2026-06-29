@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaImplementationDetail::class,
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
)

package org.cangnova.cangjie.analysis.api.impl.base.projectStructure

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaGlobalSearchScopeMerger
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScope
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScopeProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import java.time.Duration

/**
 * `CaBaseResolutionScopeProvider` 对位 Kotlin `KaBaseResolutionScopeProvider`。
 *
 * 它把 Analysis API use-site module 的“可分析模块集”稳定收敛成统一的解析作用域，
 * 供 low-level CFIR、Analysis API 会话装配和后续平台查询复用。
 */
@CaImplementationDetail
class CaBaseResolutionScopeProvider : CaResolutionScopeProvider {
    /**
     * 返回指定 use-site module 对应的解析作用域。
     */
    override fun getResolutionScope(module: CaModule): CaResolutionScope {
        return resolutionScopeCache.get(module) { useSiteModule ->
            val analyzableModules = getAnalyzableModules(useSiteModule)
            val searchScope = buildSearchScope(useSiteModule, analyzableModules)
            CaBaseResolutionScope(useSiteModule, searchScope)
        } ?: error("无法为模块 `${module.moduleDescription}` 构建解析作用域。")
    }

    /**
     * 计算 use-site module 解析时可见的模块集合。
     */
    private fun getAnalyzableModules(module: CaModule): Set<CaModule> =
        buildSet {
            add(module)
            addAll(module.directRegularDependencies)
            addAll(module.directFriendDependencies)
            addAll(module.transitiveDependsOnDependencies)
            if (module is CaLibrarySourceModule) {
                add(module.binaryLibraryModule)
            }
        }

    /**
     * 将可分析模块集合合并为一个全局搜索作用域。
     */
    private fun buildSearchScope(module: CaModule, analyzableModules: Set<CaModule>): GlobalSearchScope {
        val scopes = buildList {
            analyzableModules.mapTo(this) { it.contentScope }
            if (analyzableModules.none { it is CaBuiltinsModule }) {
                /*
                 * `CaBuiltinsModule` 不会自然出现在普通模块的依赖图中，
                 * 但 Analysis API / low-level resolve 仍然要求 builtins 始终可见，
                 * 因而这里与 Kotlin FIR 一样手工并入 builtins scope。
                 */
                add(createBuiltinsScope(module.project))
            }
        }

        return CaGlobalSearchScopeMerger.getInstance(module.project).union(scopes)
    }

    /**
     * 创建当前 project 的 builtins 搜索作用域。
     */
    private fun createBuiltinsScope(project: Project): GlobalSearchScope {
        return BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)
    }

    /**
     * use-site module 到解析作用域的短期弱缓存。
     */
    private val resolutionScopeCache: Cache<CaModule, CaResolutionScope> =
        Caffeine.newBuilder().weakKeys().softValues().expireAfterAccess(Duration.ofSeconds(10)).build()
}

/**
 * `CaBaseResolutionScope` 对位 Kotlin `KaBaseResolutionScope`。
 *
 * 该作用域不是给调用方手工构造的，而是由 [CaResolutionScopeProvider] 统一分发。
 */
@CaImplementationDetail
class CaBaseResolutionScope(
    /**
     * 该解析作用域对应的 use-site module。
     */
    private val useSiteModule: CaModule,
    /**
     * 实际承载文件包含关系判断的搜索作用域。
     */
    private val searchScope: GlobalSearchScope,
) : CaResolutionScope() {
    /**
     * 缓存最近命中的 virtual file id。
     *
     * 这里沿用 Kotlin 的固定槽位缓存策略：只缓存正命中，避免 `contains(PsiElement)` 在
     * Code Analysis 热路径上重复落到复杂 scope 判定。
     */
    private val virtualFileIdCache = IntArray(32) { -1 }

    /**
     * 判断虚文件是否属于解析作用域。
     */
    override fun contains(file: VirtualFile): Boolean {
        return searchScope.contains(file)
    }

    /**
     * 判断 PSI 元素是否属于解析作用域。
     */
    override fun contains(element: PsiElement): Boolean {
        if (element is PsiDirectory) {
            return cachedSearchScopeContains(element.virtualFile)
        }

        /*
         * 与 Kotlin `KaBaseResolutionScope` 一致，这里必须检查 view provider 的 VirtualFile。
         * dangling / in-memory PSI 的模块边界依赖该文件，不能回退到 original physical file。
         */
        val psiFile = element.containingFile
        val virtualFile = psiFile.viewProvider.virtualFile
        return cachedSearchScopeContains(virtualFile)
    }

    /**
     * 使用固定槽位缓存加速虚文件包含关系的正命中。
     */
    private fun cachedSearchScopeContains(virtualFile: VirtualFile): Boolean {
        val virtualFileWithId = virtualFile as? VirtualFileWithId
            ?: return searchScope.contains(virtualFile)

        val id = virtualFileWithId.id
        if (id < 0) {
            return searchScope.contains(virtualFile)
        }

        val cache = virtualFileIdCache
        val index = id % cache.size
        if (cache[index] == id) {
            return true
        }

        val isContained = searchScope.contains(virtualFile)
        if (isContained) {
            cache[index] = id
        }
        return isContained
    }

    /**
     * 返回底层 IntelliJ 搜索作用域。
     */
    override val underlyingSearchScope: GlobalSearchScope
        get() = searchScope

    /**
     * 返回底层搜索作用域关联的 project。
     */
    override fun getProject(): Project? = searchScope.project

    /**
     * 委托底层搜索作用域判断是否搜索指定 IntelliJ module content。
     */
    override fun isSearchInModuleContent(aModule: Module): Boolean = searchScope.isSearchInModuleContent(aModule)

    /**
     * 委托底层搜索作用域判断是否搜索 library。
     */
    override fun isSearchInLibraries(): Boolean = searchScope.isSearchInLibraries

    /**
     * 返回包含 use-site module 和底层 scope 的调试文本。
     */
    override fun toString(): String = "Resolution scope for '$useSiteModule'. Underlying search scope: '$searchScope'"
}
