package org.cangnova.cangjie.analysis.api.decompiled

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.psi.CjDecompiledFile

interface CaDecompiledTextRenderer {
    fun render(binaryFile: VirtualFile): String?

    fun render(file: CjDecompiledFile): String

    companion object {
        fun getInstance(project: Project): CaDecompiledTextRenderer = project.service()
    }
}
