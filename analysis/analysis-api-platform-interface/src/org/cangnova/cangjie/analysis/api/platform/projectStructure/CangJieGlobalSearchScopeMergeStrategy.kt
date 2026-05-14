package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaPlatformComponent
import kotlin.reflect.KClass

/**
 * `CangJieGlobalSearchScopeMergeStrategy` 对位 Kotlin `KotlinGlobalSearchScopeMergeStrategy`。
 *
 * 策略按注册顺序把一组同类 [GlobalSearchScope] 合并成更扁平的搜索范围，
 * 供 [CaGlobalSearchScopeMerger] 统一应用。
 */
@CaPlatformInterface
@CaExperimentalApi
interface CangJieGlobalSearchScopeMergeStrategy<T : Any> : CaPlatformComponent {
    /**
     * 当前策略处理的目标 scope 类型。
     */
    val targetType: KClass<T>

    /**
     * 合并同类型 scope。
     *
     * 如果无法优化，返回原 scope 列表；如果可合并为空范围，返回空列表。
     */
    fun uniteScopes(scopes: List<T>): List<GlobalSearchScope>

    @CaPlatformInterface
    companion object {
        val EP_NAME: ExtensionPointName<CangJieGlobalSearchScopeMergeStrategy<*>> =
            ExtensionPointName<CangJieGlobalSearchScopeMergeStrategy<*>>(
                "org.cangnova.cangjie.cangjieGlobalSearchScopeMergeStrategy",
            )

        fun getMergeStrategies(project: Project): List<CangJieGlobalSearchScopeMergeStrategy<*>> =
            EP_NAME.getExtensionList(project)
    }
}

/**
 * 标记可从 intersection scope 中按分配律提取的 [GlobalSearchScope]。
 */
@CaPlatformInterface
@CaExperimentalApi
interface CangJieIntersectionScopeMergeTarget
