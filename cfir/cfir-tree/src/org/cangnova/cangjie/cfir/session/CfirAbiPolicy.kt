package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.annotations.CangjieCallingConvention
import org.cangnova.cangjie.cfir.declarations.CfirAbiRequest
import org.cangnova.cangjie.cfir.declarations.CfirAbiKind
import org.cangnova.cangjie.cfir.declarations.CfirResolvedAbi

/**
 * 当前 CFIR session 的 ABI 选择策略。
 *
 * `foreign`/`@C`/`@CallingConv` 只构成源级请求，不能在 resolve 层直接固化
 * 某个后端的最终 ABI。具体 backend 在创建 session 时注入策略；CFIR 只消费
 * 平台中立的 [CfirResolvedAbi]，从而避免 common/CFIR 依赖 JVM/LLVM 枚举。
 */
public interface CfirAbiPolicy : CfirSessionComponent {
    /** 根据源级请求计算当前 session 的 ABI 结果。 */
    public fun resolve(request: CfirAbiRequest): CfirResolvedAbi
}

/**
 * 未指定具体 backend 时使用的语言层策略。
 *
 * 该策略只解析仓颉源级事实：`foreign` 或显式 `@C` 表示 C function ABI，
 * 其它声明保持仓颉 ABI；调用约定只在源级显式给出时进入 request，不能在
 * Analysis API 的显式语义视图中把 backend 默认值伪装成源码 `@CallingConv`。
 */
public object CfirLanguageAbiPolicy : CfirAbiPolicy {
    override fun resolve(request: CfirAbiRequest): CfirResolvedAbi {
        val hasCAbi = request.isForeign || request.hasExplicitC
        return CfirResolvedAbi(
            kind = if (hasCAbi) CfirAbiKind.C else CfirAbiKind.CANGJIE,
            isCFunction = hasCAbi && request.isFunction,
            effectiveCallingConvention = request.callingConvention,
        )
    }
}

/** 当前 session 的唯一 ABI policy 组件。 */
public val CfirSession.cfirAbiPolicy: CfirAbiPolicy by CfirSession.sessionComponentAccessorWithDefault(
    CfirLanguageAbiPolicy,
)
