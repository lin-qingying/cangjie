package org.cangnova.cangjie.analysis.api.impl.base.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleBase
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.platform.TargetPlatform

/**
 * Analysis API 主干使用的 builtins 模块实现。
 *
 * 该实现对位 Kotlin `KaBuiltinsModuleImpl`，作为 low-level CFIR/session 架构中的
 * 内建模块实体，统一承载 builtins scope 与目标平台身份。
 */
@CaPlatformInterface
@CaImplementationDetail
class CaBuiltinsModuleImpl(
    override val targetPlatform: TargetPlatform,
    override val project: Project,
) : CaModuleBase(), CaBuiltinsModule {
    override val baseContentScope: GlobalSearchScope
        get() = BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)

    override fun equals(other: Any?): Boolean =
        other is CaBuiltinsModule && targetPlatform == other.targetPlatform

    override fun hashCode(): Int = targetPlatform.hashCode()
}
