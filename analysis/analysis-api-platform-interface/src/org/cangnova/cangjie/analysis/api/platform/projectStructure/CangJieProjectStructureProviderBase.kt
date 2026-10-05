package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.projectStructure.contextModule
import org.cangnova.cangjie.analysis.api.projectStructure.copyOrigin
import org.cangnova.cangjie.analysis.api.projectStructure.danglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.explicitModule
import org.cangnova.cangjie.analysis.api.projectStructure.isDangling
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.elementContext

/**
 * Project-structure provider 的基础实现。
 */
@CaPlatformInterface
abstract class CangJieProjectStructureProviderBase : CangJieProjectStructureProvider {
    /**
     * 返回项目中“不属于内容根”的兜底模块。
     */
    protected abstract fun getNotUnderContentRootModule(project: Project): CaNotUnderContentRootModule

    /**
     * 针对显式模块和 dangling 文件计算特殊模块。
     */
    @OptIn(CaExperimentalApi::class)
    protected fun computeSpecialModule(file: PsiFile): CaModule? {
        if (file is CjFile) {
            val explicitModule = file.explicitModule
            if (explicitModule != null) {
                require(file.danglingFileResolutionMode == null) {
                    "Resolution mode cannot be overridden for a file with an explicit module"
                }
                return explicitModule
            }
        }

        if (file is CjFile && isDangling(file)) {
            val contextModule = computeContextModule(file)
            return CaDanglingFileModuleImpl(
                files = listOf(file),
                contextModule = contextModule,
                resolutionMode = file.danglingFileResolutionMode ?: computeDefaultDanglingFileResolutionMode(file),
            )
        }

        return null
    }

    /**
     * 计算 dangling 文件默认解析模式。
     */
    @OptIn(CaExperimentalApi::class)
    protected open fun computeDefaultDanglingFileResolutionMode(file: CjFile): CaDanglingFileResolutionMode {
        // 对齐 Kotlin：IDE/Analysis 临时副本文件应默认忽略自身的非局部声明，
        // 继续复用原始文件已经建立好的上下文与跨文件解析结果。
        if (!file.isPhysical && !file.viewProvider.isEventSystemEnabled && file.copyOrigin != null) {
            return CaDanglingFileResolutionMode.IGNORE_SELF
        }
        return CaDanglingFileResolutionMode.PREFER_SELF
    }

    /**
     * 根据 code fragment、copy origin 或 PSI context 计算 dangling 文件上下文模块。
     */
    @OptIn(CaExperimentalApi::class)
    protected open fun computeContextModule(file: CjFile): CaModule {
        // 显式上下文模块优先：对齐 Kotlin `KtFile.contextModule`。
        // 没有它时，`createFile` 造出的副本既无 copyOrigin 也无 elementContext，
        // 会落到「不在任何内容根下」的模块上——那不是调用方要的上下文。
        file.contextModule?.let { return it }

        val originalFile = file.copyOrigin
        val contextElement =
            (file as? CjCodeFragment)?.context?.takeIf(::isSupportedContextElement)
                ?: file.elementContext?.takeIf(::isSupportedContextElement)
                ?: originalFile?.takeIf(::isSupportedContextElement)
                ?: file.context?.takeIf(::isSupportedContextElement)

        if (contextElement != null) {
            var contextModule = getModule(contextElement, useSiteModule = null)
            if (file !is CjCodeFragment) {
                while (contextModule is CaDanglingFileModule) {
                    contextModule = contextModule.contextModule
                }
            }
            return contextModule
        }

        return getNotUnderContentRootModule(file.project)
    }

    /**
     * 判断文件是否需要建模为 dangling file module。
     */
    protected open fun isDangling(file: CjFile): Boolean {
        return file.isDangling
    }

    /**
     * 判断元素是否可以作为 dangling 文件上下文。
     */
    private fun isSupportedContextElement(context: PsiElement): Boolean {
        return context.language == CangJieLanguage || context is PsiDirectory
    }
}
