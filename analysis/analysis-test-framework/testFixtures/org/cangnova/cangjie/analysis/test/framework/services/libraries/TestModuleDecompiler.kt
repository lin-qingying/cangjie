package org.cangnova.cangjie.analysis.test.framework.services.libraries

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoBinaryFileReader
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import java.nio.file.Path

interface TestModuleDecompiler : TestService {
    fun getAllPsiFilesFromLibrary(artifactRoot: Path, project: Project): List<PsiFile>
}

val TestServices.testModuleDecompiler: TestModuleDecompiler by TestServices.testServiceAccessor()

class TestModuleDecompilerDirectory : TestModuleDecompiler {
    override fun getAllPsiFilesFromLibrary(artifactRoot: Path, project: Project): List<PsiFile> {
        val normalizedRoot = artifactRoot.toAbsolutePath().normalize()
        val normalizedPath = normalizedRoot.toString().replace('\\', '/')
        val localFileSystem = StandardFileSystems.local()
        val root = localFileSystem.findFileByPath(normalizedPath)
            ?: localFileSystem.refreshAndFindFileByPath(normalizedPath)
            ?: VirtualFileManager.getInstance().findFileByNioPath(normalizedRoot)
            ?: error("Cannot find virtual file for compiled library root: $artifactRoot")
        val psiManager = PsiManager.getInstance(project)
        val psiFiles = linkedMapOf<String, PsiFile>()

        if (root.isDirectory) {
            VfsUtilCore.iterateChildrenRecursively(root, null) { file ->
                if (!file.isDirectory && CjoBinaryFileReader.isCjoBinaryFile(file)) {
                    psiManager.findFile(file)?.let { psiFile ->
                        psiFiles.putIfAbsent(file.path, psiFile)
                    }
                }
                true
            }
        } else if (CjoBinaryFileReader.isCjoBinaryFile(root)) {
            psiManager.findFile(root)?.let { psiFile ->
                psiFiles.putIfAbsent(root.path, psiFile)
            }
        }

        return psiFiles.values.toList()
    }
}
