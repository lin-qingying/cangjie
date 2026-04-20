package org.cangnova.cangjie.analysis.api.platform.declarations

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaPlatformComponent
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjClassLikeDeclaration

/**
 * `CangJieDirectInheritorsProvider` 对位 Kotlin `KotlinDirectInheritorsProvider`。
 *
 * 平台实现负责在给定作用域内查找某个物理源码类的直接仓颉继承者。
 */
@CaPlatformInterface
interface CangJieDirectInheritorsProvider : CaPlatformComponent {
    /**
     * 返回 [cjClass] 在 [scope] 中的所有直接仓颉继承者。
     *
     * [cjClass] 必须来自物理源码，而不是 dangling file。
     */
    fun getDirectCangJieInheritors(
        cjClass: CjClass,
        scope: GlobalSearchScope,
        includeLocalInheritors: Boolean = true,
    ): Iterable<CjClassLikeDeclaration>

    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CangJieDirectInheritorsProvider = project.service()
    }
}
