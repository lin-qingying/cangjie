package org.cangnova.cangjie.analysis.decompiler.stub

import PackageFormat.Package as CjoPackage
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.name.FqName

/**
 * `.cjo` 反编译前已经完成读取的 package 数据集合。
 *
 * 该结构把原始虚拟文件、flatbuffer package、package header、repository manager 和版本兼容性统一传递给
 * stub 构建层，避免不同反编译入口重复读取或重新推导同一批元数据。manager 标识 package 所属的仓库快照，
 * 因此也属于该数据值的身份。
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

    /**
     * 由当前 `.cjo` repository 持有的 package manager。
     *
     * 声明反序列化必须复用读取当前 package 的 manager，使包头索引和已加载 package 快照跨文件 stub 共用。
     */
    val cjoManager: CjoManager,

    /** 当前 `.cjo` 版本是否可由本反编译器安全读取。 */
    val isVersionSupported: Boolean,

    /** body 真正来源；可能不同于触发反编译的 binaryFile。旧的内存调用方默认不启用 sidecar。 */
    val sourcePath: java.nio.file.Path? = null,
)
