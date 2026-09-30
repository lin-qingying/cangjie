package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * 只读 `.cjo` 头部与 flatbuffer 的 class-like 符号查找入口。
 *
 * 反序列化期间的符号解析不得依赖 PSI：`.cjo` 的 stub 树由 `StubUpdatingIndex` 在索引期构建，
 * 此时向 stub 索引索取其他未索引文件的 green stub 会被平台直接拒绝。这里只暴露反序列化器
 * 实际需要的一个操作，不暴露 `CfirSymbolProvider` 上会落到 PSI 的其他查询，从而使新的调用点无法重新引入同类越界。
 *
 * Kotlin 对应：`AbstractFirDeserializedSymbolProvider.getClassLikeSymbolByClassId` 只用元数据，`fir-deserialization` 全模块零 `com.intellij` 导入。
 */
interface CfirClassLikeSymbolLookup {
    /**
     * 返回给定 class id 的 class-like 符号；不存在或无法从头部解析时返回 null。
     */
    fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>?
}
