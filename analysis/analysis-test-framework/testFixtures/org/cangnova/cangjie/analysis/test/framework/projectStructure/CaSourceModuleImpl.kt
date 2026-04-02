package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.CaSourceModule

/**
 * Analysis API 测试框架使用的源码模块实现。
 *
 * 这不是生产平台模块，而是测试平台对 `CaSourceModule` 的落地实现。
 * 其职责是把 `TestModuleStructure` 中解析出的模块信息转换成 Analysis API 可消费的模块图。
 */
class CaSourceModuleImpl(
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    override val project: Project,
    psiRoots: List<PsiFileSystemItem>,
) : CaSourceModule {
    override val directRegularDependencies: MutableList<CaModule> = mutableListOf()

    override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()

    override val directFriendDependencies: MutableList<CaModule> = mutableListOf()

    override val psiRoots: List<PsiFileSystemItem> = psiRoots.toList()

    override val baseContentScope: GlobalSearchScope = GlobalSearchScope.filesWithoutLibrariesScope(
        project,
        this.psiRoots.mapNotNull { it.virtualFile },
    )

    override fun toString(): String = name
}
