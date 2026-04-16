package org.cangnova.cangjie.analysis.api.decompiled

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.name.FqName

interface CaDecompiledBinaryIndex {
    fun getBinaryFiles(module: CaLibraryModule): List<VirtualFile>

    fun getBinaryFiles(module: CaBuiltinsModule): List<VirtualFile>

    fun findBinaryFile(module: CaLibraryModule, packageFqName: FqName): VirtualFile?

    fun findBinaryFile(module: CaBuiltinsModule, packageFqName: FqName): VirtualFile?

    fun findOwningModule(binaryFile: VirtualFile): CaModule?

    companion object {
        fun getInstance(project: Project): CaDecompiledBinaryIndex = project.service()
    }
}
