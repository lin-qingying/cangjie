package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.name.FqName
import java.io.File

internal data class RepositoryKey(
    val moduleKey: String,
    val roots: List<File>,
)

internal class DecompiledCjoRepository(
    roots: List<File>,
) {
    private val rootPathString = roots.joinToString(File.pathSeparator) { it.absolutePath }
    private val cjoManager = CjoManager(
        CjoSearchPath { key ->
            when (key) {
                "CANGJIE_LIBRARY", "CANGJIE_STDLIB_MODULE" -> rootPathString
                else -> null
            }
        },
    )

    fun loadPackageData(
        packageFqName: FqName,
        binaryFile: VirtualFile,
        searchRoots: List<File>,
    ): LoadedCjoPackage? {
        val fullPkgName = packageFqName.asString()
        val pkg = cjoManager.loadPackage(fullPkgName) ?: return null
        val header = cjoManager.loadPackageHeader(fullPkgName) ?: return null
        return LoadedCjoPackage(
            binaryFile = binaryFile,
            packageFqName = packageFqName,
            pkg = pkg,
            header = header,
            searchRoots = searchRoots,
            isVersionSupported = CjoBinaryFileReader.isSupportedVersion(pkg),
        )
    }
}
