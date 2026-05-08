package org.cangnova.cangjie.analysis.api.decompiled

import com.intellij.openapi.vfs.VirtualFile

/**
 * 暴露 builtins 根目录，供二进制仓库恢复真实的 `.cjo` 搜索根。
 *
 * 这个契约属于 Analysis API 公共 decompiled surface：
 * - low-level CFIR 的 builtins session 需要它恢复 `CjoManager` 搜索路径；
 * - IDE / CLI / test 三种 builtins provider 都需要以同一方式暴露根目录。
 */
interface CaBuiltinsRootAware {
    fun getBuiltinRootVirtualFiles(): Set<VirtualFile>
}
