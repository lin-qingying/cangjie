@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.name.FqName

/** 资源选择与 actual-file 读取分开；读取结果已经带有原 metadata 图的绑定 context。 */
class DecompiledPackageDataFinder(private val project: Project) {
    private val binaryIndex: CaDecompiledBinaryIndex
        get() = project.getService(CaDecompiledBinaryIndex::class.java)

    fun loadPackageData(binaryFile: VirtualFile): LoadedCjoPackage? =
        loadPackageData(binaryFile, binaryFile.contentsToByteArray(false))

    fun loadPackageData(binaryFile: VirtualFile, content: ByteArray): LoadedCjoPackage? =
        loadPackageData(binaryFile, content, null)

    private fun loadPackageData(
        binaryFile: VirtualFile, content: ByteArray,
        owner: org.cangnova.cangjie.analysis.api.projectStructure.CaModule?,
        expectedPackage: FqName? = null,
    ): LoadedCjoPackage? {
        val name = CjoBinaryFileReader.readPackageFqName(binaryFile, content) ?: return null
        check(expectedPackage == null || name == expectedPackage) { "CJO binary index and actual content have different package names" }
        val pkg = PackageFormat.Package.getRootAsPackage(java.nio.ByteBuffer.wrap(content))
        if (!CjoBinaryFileReader.isSupportedVersion(pkg)) return null
        val service = CjoModuleDataProvider.getInstance(project)
        val context = if (owner == null) service.getPackageContext(binaryFile, content, CjoDeserializationMode.SIGNATURE_ONLY)
            else service.getPackageContext(binaryFile, content, CjoDeserializationMode.SIGNATURE_ONLY, owner)
        check(context.header.fullPkgName == name.asString()) { "CJO package changed during actual-file reading" }
        return LoadedCjoPackage(
            binaryFile = binaryFile, packageFqName = name, pkg = context.pkg, header = context.header,
            cjoManager = context.binding.ownManager, isVersionSupported = true,
            sourcePath = context.sourcePath, deserializationContext = context,
        )
    }

    /** 按 module/package 选择实际文件后，进入同一个 reader，不再扩展成 own+SDK 联合 repository。 */
    fun loadPackageData(module: CaLibraryModule, packageFqName: FqName): LoadedCjoPackage? =
        binaryIndex.findBinaryFile(module, packageFqName)?.let { loadPackageData(it, it.contentsToByteArray(false), module, packageFqName) }

    fun loadPackageData(module: CaBuiltinsModule, packageFqName: FqName): LoadedCjoPackage? =
        binaryIndex.findBinaryFile(module, packageFqName)?.let { loadPackageData(it, it.contentsToByteArray(false), module, packageFqName) }
}
