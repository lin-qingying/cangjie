package org.cangnova.cangjie.analysis.api.interop

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

/**
 * 仓颉互操作后端类型。
 *
 * 这里只描述声明与哪一类外部生态建立边界，不把 `ForeignName`、`CallingConv`
 * 这类附属注解混入“后端类型”枚举。
 */
enum class CaInteropBackend {
    C,
    JAVA,
    JAVA_MIRROR,
    JAVA_IMPL,
    OBJC_MIRROR,
    OBJC_IMPL,
}

/**
 * Analysis API 对外公开的调用约定视图。
 *
 * 该枚举独立于 PSI 层的枚举定义，避免平台与工具层直接依赖 PSI 内部类型。
 */
enum class CaInteropCallingConvention {
    CDECL,
    STDCALL,
}

/**
 * 互操作语义快照。
 *
 * 该快照统一承载：
 * 1. 声明是否处于 `foreign` 边界；
 * 2. 声明映射到哪些外部后端；
 * 3. 外部符号名与调用约定；
 * 4. 快速本地执行提示等附属语义。
 *
 * 上层 IDE、LSP、重构和文档层都应共享这份稳定快照，
 * 而不是各自重新扫描注解、修饰符与参数文本。
 */
interface CaInteropInfo : CaLifetimeOwner {
    /**
     * 当前声明直接声明的互操作后端。
     *
     * 保留源代码中的声明顺序，不做额外重排。
     */
    val backends: List<CaInteropBackend>

    /**
     * 是否显式使用了 `foreign` 声明边界。
     */
    val isForeignDeclaration: Boolean

    /**
     * 是否显式声明了快速本地执行提示。
     */
    val isFastNative: Boolean

    /**
     * `@ForeignName` 中声明的外部符号名。
     */
    val externalName: String?

    /**
     * `@CallingConv` 中声明的调用约定。
     */
    val callingConvention: CaInteropCallingConvention?

    /**
     * 当前互操作快照涉及的全部 FFI 注解名。
     *
     * 这里保留的是稳定的注解短名视图，供渲染和工具层直接消费。
     */
    val ffiAnnotationNames: List<String>
}
