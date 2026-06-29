/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.psi.packgae

import com.intellij.lang.Language
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Queryable
import com.intellij.psi.*
import com.intellij.psi.impl.file.PsiPackageBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.Processors
import com.intellij.util.containers.ContainerUtil
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.CangJiePsiFacade

/**
 * Represents a CangJie package.
 */
interface CangJiePackage :
    PsiCheckedRenameElement,
    NavigationItem,

    PsiDirectoryContainer,
    PsiQualifiedNamedElement


/**
 * 表示 `AbstractCangJiePackage`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
abstract class AbstractCangJiePackage(

    manager: PsiManager,
    qualifiedName: String
) : PsiPackageBase(
    manager, qualifiedName
), CangJiePackage, Queryable {
    /**
     * 实现 `getLanguage` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getLanguage(): Language = CangJieLanguage
    /**
     * 保存 `facade` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val facade: CangJiePsiFacade
        get() {
            return CangJiePsiFacade.getInstance(project)
        }

    /**
     * 保存 `myDirectoriesWithLibSources`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @Volatile
    private lateinit var myDirectoriesWithLibSources: CachedValue<Collection<PsiDirectory>>

    /**
     * 保存 `myDirectories`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @Volatile
    private lateinit var myDirectories: CachedValue<Collection<PsiDirectory>>
    /**
     * 执行 `createCachedDirectories` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun createCachedDirectories(includeLibrarySources: Boolean): CachedValue<Collection<PsiDirectory>> {
        return CachedValuesManager.getManager(project).createCachedValue({
            val result: Collection<PsiDirectory> = ArrayList()
            val processor = Processors.cancelableCollectProcessor(result)
            facade.processPackageDirectories(this, allScope(), processor, includeLibrarySources)
            CachedValueProvider.Result.create(
                result,
                PsiModificationTracker.MODIFICATION_COUNT,
                ProjectRootManager.getInstance(project)
            )
        }, false)
    }

    /**
     * 实现 `navigate` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun navigate(requestFocus: Boolean) {

    }


    /**
     * 提供 `allScope` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    protected fun allScope(): GlobalSearchScope {
        return GlobalSearchScope.allScope(
            project
        )

    }

    /**
     * 实现 `getAllDirectories` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getAllDirectories(scope: GlobalSearchScope): MutableCollection<PsiDirectory> {
        if (scope.isForceSearchingInLibrarySources) {
            if (!::myDirectoriesWithLibSources.isInitialized) {
                myDirectoriesWithLibSources = createCachedDirectories(true)
            }
            return ContainerUtil.filter(
                myDirectoriesWithLibSources.value
            ) { d: PsiDirectory -> scope.contains(d.virtualFile) }
        } else {
            if (!::myDirectories.isInitialized) {
                myDirectories = createCachedDirectories(false)
            }
            return ContainerUtil.filter(
                myDirectories.value
            ) { d: PsiDirectory -> scope.contains(d.virtualFile) }
        }
    }

    /**
     * 实现 `findPackage` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun findPackage(qName: String): AbstractCangJiePackage? {

        return facade.findPackage(qName) as? AbstractCangJiePackage


    }
}
