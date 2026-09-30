package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import java.util.concurrent.ConcurrentHashMap

/**
 * 按模块数据共享的反序列化符号查找入口。
 *
 * `CfirDeserializationContext` 每个 `.cjo` 一份，若每份资源自己构造 provider，SDK 的 N 个包会把同一个跨包类物化 N 次。
 * 本持有者由 `CjoManager` 持有；IDE 反编译路径下 `CjoManager` 按模块 binary roots 创建（`DecompiledCjoRepository`），
 * BINARIES 路径下是 session 级单例，两种路径都符合「每模块每类一份物化缓存」的契约。
 *
 * 生命周期与 [CjoManager] 的包快照一致：`.cjo`、sidecar 或搜索根变化后调用方必须重建 `CjoManager`，此时一并丢弃本持有者。
 */
class CjoDeserializedSymbolLookupHolder(
    private val cjoManager: CjoManager,
) {
    private val byModuleData = ConcurrentHashMap<CfirModuleData, CfirDeserializedSymbolProvider>()

    /**
     * 返回该模块数据对应的查找入口，第一次调用时创建。
     */
    fun forModuleData(moduleData: CfirModuleData): CfirClassLikeSymbolLookup =
        byModuleData.computeIfAbsent(moduleData) {
            CfirDeserializedSymbolProvider(
                session = moduleData.session,
                cjoManager = cjoManager,
                cangjieScopeProvider = CfirCangJieScopeProvider(),
                libraryModuleData = moduleData,
            )
        }
}
