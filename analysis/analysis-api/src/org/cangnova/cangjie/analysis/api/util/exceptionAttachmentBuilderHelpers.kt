package org.cangnova.cangjie.analysis.api.util

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder

fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?) {
    withEntry(name, psi) { psiElement ->
        getElementTextWithContext(psiElement)
    }
}

fun ExceptionAttachmentBuilder.withVirtualFileEntry(name: String, virtualFile: VirtualFile?) {
    withEntry(name, virtualFile) { file ->
        "path: ${file.path}, filetype: ${file.fileType} ,filesystem,${file.fileSystem}"
    }
}
public fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?, moduleFactory: (PsiElement) -> CaModule) {
    withPsiEntry(name, psi, psi?.let(moduleFactory))
}


public fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?, module: CaModule?) {
    withPsiEntry(name, psi)
    withCaModuleEntry("${name}Module", module)
}


public fun ExceptionAttachmentBuilder.withCaModuleEntry(name: String, module: CaModule?) {
    withEntry(name, module) { module -> module.moduleDescription }
    if (module is CaDanglingFileModule) {
        withCaModuleEntry("${name}contextModule", module.contextModule)
    }
}


public fun ExceptionAttachmentBuilder.withClassEntry(name: String, element: Any?) {
    withEntry(name, element) { it::class.java.name }
}
