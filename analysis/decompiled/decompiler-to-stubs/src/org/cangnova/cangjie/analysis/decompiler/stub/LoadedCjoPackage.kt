package org.cangnova.cangjie.analysis.decompiler.stub

import PackageFormat.Package as CjoPackage
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.name.FqName
import java.io.File

data class LoadedCjoPackage(
    val binaryFile: VirtualFile,
    val packageFqName: FqName,
    val pkg: CjoPackage,
    val header: CjoPackageHeader,
    val searchRoots: List<File>,
    val isVersionSupported: Boolean,
)
