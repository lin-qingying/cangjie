package org.cangnova.cangjie.analysis.decompiler.stub

import PackageFormat.Package as CjoPackage
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.name.FqName
import java.io.File

/**
 * `.cjo` 反编译前已经完成读取的 package 数据集合。
 *
 * 该结构把原始虚拟文件、flatbuffer package、package header、搜索根和版本兼容性统一传递给
 * stub 构建层，避免不同反编译入口重复读取或重新推导同一批元数据。
 */
data class LoadedCjoPackage(
    /** 触发反编译的原始 `.cjo` 虚拟文件。 */
    val binaryFile: VirtualFile,

    /** 当前 package 的包全限定名。 */
    val packageFqName: FqName,

    /** 从 `.cjo` body 中读取出的 flatbuffer package 对象。 */
    val pkg: CjoPackage,

    /** 与 package body 对应的 package header，用于定位声明索引和 facade 信息。 */
    val header: CjoPackageHeader,

    /** 反序列化过程中可交给 [org.cangnova.cangjie.cfir.serialization.cjo.CjoManager] 搜索的物理根目录。 */
    val searchRoots: List<File>,

    /** 当前 `.cjo` 版本是否可由本反编译器安全读取。 */
    val isVersionSupported: Boolean,
)
