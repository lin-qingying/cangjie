package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl

/**
 * Source [CfirProviderImpl] 的唯一注册入口。
 *
 * 约束（baseline 第 5 节"Provider 状态机"）：
 * - 在 construction 前 provider 必须处于 `EMPTY` 状态；
 * - 本函数将 provider 从 `EMPTY` 单调推进至 `FINALIZED`；
 * - finalized 后再次调用将抛出 `IllegalStateException`。
 *
 * `recordExpandedRawFilesOnce` 是 source CFIR 文件进入 ordinary resolve 的唯一桥梁。
 * 任何旁路写入（例如绕过 [MacroConstructionService] 直接拿到 `List<CfirFile>` 灌 provider）
 * 都被视为对 baseline 硬性边界的违反。
 *
 * @param provider 当前 session 的 source provider
 * @param files 经 [MacroConstructionService] 产出的可注册文件
 * @param registry 构造期 registry；当前 batch 用于将来与 provider 关联，
 *                 Batch 9 起将持久化到 session 上
 */
fun recordExpandedRawFilesOnce(
    provider: CfirProviderImpl,
    files: RecordableRawCfirFiles,
    @Suppress("UNUSED_PARAMETER") registry: MacroExpansionRegistry,
) {
    provider.recordExpandedFilesOnce(files)
}
