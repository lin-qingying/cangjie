package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile

@CaPlatformInterface
abstract class CangJieProjectStructureProviderBase : CangJieProjectStructureProvider {
    protected abstract fun getNotUnderContentRootModule(project: Project): CaNotUnderContentRootModule

    protected fun computeSpecialModule(file: PsiFile): CaModule? {
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
        return CaDanglingFileResolutionMode.PREFER_SELF
    }

    protected open fun computeContextModule(file: CjFile): CaModule {
        val contextElement =
            (file as? CjCodeFragment)?.context?.takeIf(::isSupportedContextElement)
                ?: file.originalFile.takeUnless { it == file }?.takeIf(::isSupportedContextElement)

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
        return file.isCodeFragment || file is CjCodeFragment || !file.isPhysical
    }

    private fun isSupportedContextElement(context: PsiElement): Boolean {
        return context is CjFile || context is PsiDirectory
    }
}
