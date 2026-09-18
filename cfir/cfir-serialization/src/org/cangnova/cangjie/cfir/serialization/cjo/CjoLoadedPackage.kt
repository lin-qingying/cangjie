package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.Package
import java.nio.file.Path

/**
 * 已实际读取的 CJO 快照。header/body/sourcePath 属于同一次读取，不能重新搜索包名推导来源。
 * 此快照及其 manager/provider/session 为同一代；库或 sidecar 变化必须重建整个库图。
 */
interface CjoLoadedPackage {
    val pkg: Package
    val header: CjoPackageHeader
    val sourcePath: Path
}

/** 二进制来源查询边界；只返回管理器实际选择并读取的文件。 */
interface CjoLoadedPackageProvider {
    fun loadPackageSnapshot(fullPkgName: String): CjoLoadedPackage?
}
