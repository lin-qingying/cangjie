@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.base.projectStructure

import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProviderBase
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule

/**
 * 带静态模块图的 [CangJieProjectStructureProvider]。
 *
 * 与 Kotlin `KotlinStaticProjectStructureProvider` 对齐：
 * 预注册模块图本身是静态的，但 provider 仍可按需创建 [CaNotUnderContentRootModule]。
 */
abstract class CangJieStaticProjectStructureProvider : CangJieProjectStructureProviderBase() {
    /**
     * 当前 standalone project structure 中的全部模块。
     */
    abstract val allModules: List<CaModule>

    /**
     * 当前 standalone project structure 中的全部源码文件系统项。
     */
    abstract val allSourceFiles: List<PsiFileSystemItem>
}
