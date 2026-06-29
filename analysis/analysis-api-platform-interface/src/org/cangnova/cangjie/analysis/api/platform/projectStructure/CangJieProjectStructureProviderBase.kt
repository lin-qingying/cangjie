package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.projectStructure.explicitModule
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
                return explicitModule
            }
        }

        if (file is CjFile && isDangling(file)) {
            val contextModule = computeContextModule(file)
            return CaDanglingFileModuleImpl(
                files = listOf(file),
                contextModule = contextModule,
                resolutionMode = computeDefaultDanglingFileResolutionMode(file),
            )
        }

        return null
    }

    /**
     * 计算 dangling 文件默认解析模式。
     */
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
    protected open fun computeContextModule(file: CjFile): CaModule {
        val originalFile = file.copyOrigin
        val contextElement =
            (file as? CjCodeFragment)?.context?.takeIf(::isSupportedContextElement)
                ?: file.elementContext?.takeIf(::isSupportedContextElement)
                ?: originalFile?.takeIf(::isSupportedContextElement)
                ?: file.context?.takeIf(::isSupportedContextElement)

        if (contextElement != null) {
            val contextModule = getModule(contextElement, useSiteModule = null)
            if (contextModule is CaDanglingFileModule && file !is CjCodeFragment) {
                return contextModule.contextModule
            }
            return contextModule
        }

        return getNotUnderContentRootModule(file.project)
    }

    /**
     * 判断文件是否需要建模为 dangling file module。
     */
    protected open fun isDangling(file: CjFile): Boolean {
        return file.isCodeFragment || file is CjCodeFragment || !file.isPhysical || file.copyOrigin != null || file.elementContext != null
    }

    /**
     * 判断元素是否可以作为 dangling 文件上下文。
     */
    private fun isSupportedContextElement(context: PsiElement): Boolean {
        return context.language == CangJieLanguage || context is PsiDirectory
    }
}

/**
 * PSI 文件的原始文件来源。
 */
private val PsiFile.copyOrigin: PsiFile?
    get() {
        return originalFile.takeUnless { it == this }
            ?: getUserData(PsiFileFactory.ORIGINAL_FILE)?.takeUnless { it == this }
    }
