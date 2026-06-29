package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * `CaModule` 的平台实现基类。
 */
@CaPlatformInterface
abstract class CaModuleBase : CaModule {
    /**
     * 平台修正后的模块内容搜索范围。
     */
    override val contentScope: GlobalSearchScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaContentScopeProvider.getInstance(project).getRefinedContentScope(this)
    }
}
