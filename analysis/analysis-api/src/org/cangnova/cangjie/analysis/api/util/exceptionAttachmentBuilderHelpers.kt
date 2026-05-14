package org.cangnova.cangjie.analysis.api.util

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder

/**
 * 把 PSI 元素以"含上下文文本"形式附加到异常报告中,便于离线分析定位。
 */
fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?) {
    withEntry(name, psi) { psiElement ->
        getElementTextWithContext(psiElement)
    }
}

/**
 * 把 [VirtualFile] 的路径、类型、所在文件系统附加到异常报告中。
 */
fun ExceptionAttachmentBuilder.withVirtualFileEntry(name: String, virtualFile: VirtualFile?) {
    withEntry(name, virtualFile) { file ->
        "path: ${file.path}, filetype: ${file.fileType} ,filesystem,${file.fileSystem}"
    }
}

/**
 * 在附加 PSI 信息的同时,通过 [moduleFactory] 计算其所属模块并一并附加。
 *
 * 适用于诊断流程已可由 PSI 反查模块的场景。
 */
fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?, moduleFactory: (PsiElement) -> CaModule) {
    withPsiEntry(name, psi, psi?.let(moduleFactory))
}


/**
 * 同时附加 PSI 元素和已经计算好的 [CaModule] 信息。
 *
 * 适用于上下文已经持有模块对象,无需再次推断的场景。
 */
fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?, module: CaModule?) {
    withPsiEntry(name, psi)
    withCaModuleEntry("${name}Module", module)
}


/**
 * 把 [CaModule] 摘要(包含 dangling file 的 context module)附加到异常报告中。
 */
fun ExceptionAttachmentBuilder.withCaModuleEntry(name: String, module: CaModule?) {
    withEntry(name, module) { module -> module.moduleDescription }
    if (module is CaDanglingFileModule) {
        withCaModuleEntry("${name}contextModule", module.contextModule)
    }
}


/**
 * 附加任意对象的 Java 类名,便于在异常日志中识别实际实现类型。
 */
fun ExceptionAttachmentBuilder.withClassEntry(name: String, element: Any?) {
    withEntry(name, element) { it::class.java.name }
}
