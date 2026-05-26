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

@CaPlatformInterface
abstract class CangJieProjectStructureProviderBase : CangJieProjectStructureProvider {
    protected abstract fun getNotUnderContentRootModule(project: Project): CaNotUnderContentRootModule

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

    protected open fun computeDefaultDanglingFileResolutionMode(file: CjFile): CaDanglingFileResolutionMode {
        // 对齐 Kotlin：IDE/Analysis 临时副本文件应默认忽略自身的非局部声明，
        // 继续复用原始文件已经建立好的上下文与跨文件解析结果。
        if (!file.isPhysical && !file.viewProvider.isEventSystemEnabled && file.copyOrigin != null) {
            return CaDanglingFileResolutionMode.IGNORE_SELF
        }
        return CaDanglingFileResolutionMode.PREFER_SELF
    }

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

    protected open fun isDangling(file: CjFile): Boolean {
        return file.isCodeFragment || file is CjCodeFragment || !file.isPhysical || file.copyOrigin != null || file.elementContext != null
    }

    private fun isSupportedContextElement(context: PsiElement): Boolean {
        return context.language == CangJieLanguage || context is PsiDirectory
    }
}

private val PsiFile.copyOrigin: PsiFile?
    get() {
        return originalFile.takeUnless { it == this }
            ?: getUserData(PsiFileFactory.ORIGINAL_FILE)?.takeUnless { it == this }
    }
