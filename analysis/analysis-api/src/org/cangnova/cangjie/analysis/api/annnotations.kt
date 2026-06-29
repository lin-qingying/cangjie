package org.cangnova.cangjie.analysis.api

/**
 * 标记 Analysis API 的实现细节声明。
 *
 * 该注解对齐 Kotlin `KaImplementationDetail`，用于把仍然暴露在 API 面上的实现内部契约
 * 显式标成仅供 Analysis API 实现模块使用的非兼容接口。
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Internal API which should not be used outside the Analysis API implementation modules as it does not have any compatibility guarantees")
/**
 * 需要显式 opt-in 的 Analysis API 实现细节标记。
 */
annotation class CaImplementationDetail

/**
 * 标记 Analysis API 的非公开接口。
 *
 * 对齐 Kotlin `KaNonPublicApi`，用于表达仅供 IDE 与框架内部使用、
 * 不面向外部调用方承诺兼容性的 API。
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Internal API which is intended for IDE and Analysis API internal use only and does not provide compatibility guarantees")
/**
 * 需要显式 opt-in 的非公开 Analysis API 标记。
 */
annotation class CaNonPublicApi

/**
 * 标记仅供 IntelliJ 仓颉插件内部使用的 Analysis API。
 *
 * 该注解对齐 Kotlin `KaIdeApi`，用于承载更适合放在 Analysis API/low-level API 中、
 * 但并不面向外部用户开放的 IDE 专用契约。
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Internal API which is used only from the IntelliJ Cangjie plugin. Such an API should not be used in other places since it has no compatibility guarantees")
/**
 * 需要显式 opt-in 的 IDE 专用 Analysis API 标记。
 */
annotation class CaIdeApi

/**
 * 标记实验性 Analysis API。
 *
 * 这里对位 Kotlin `KaExperimentalApi`，供与 Kotlin low-level API 对齐的新声明直接复用。
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("Experimental API with no compatibility guarantees")
/**
 * 需要显式 opt-in 的实验性 Analysis API 标记。
 */
annotation class CaExperimentalApi

/**
 * 标记 Analysis API 实现和平台 SPI 的契约。
 *
 * 对齐 Kotlin `KaPlatformInterface`,用于声明仅供 Analysis API 实现模块和平台层
 * 协作使用的扩展点;调用方需要显式 opt-in,且不享有跨版本兼容承诺。
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@RequiresOptIn("An API intended for Analysis API implementations and platforms. The API is neither stable nor intended for user consumption.")
/**
 * 需要显式 opt-in 的 Analysis API 平台 SPI 标记。
 */
annotation class CaPlatformInterface

/**
 * 显式声明允许在 write action 中执行 analyze 块。
 *
 * 默认禁止在 write action 中分析,以避免在写入期间触发 resolve 导致 IDE 卡顿或状态不一致;
 * 仅当确认调用上下文已经持有合适的解析快照、不会引发递归读写时使用。
 */
@RequiresOptIn("Analysis should not be allowed to be run from a write action, as otherwise it may cause incorrect behavior and IDE freezes.")
annotation class CaAllowAnalysisFromWriteAction

/**
 * 显式声明允许在 EDT 上执行 analyze 块。
 *
 * 默认禁止在 EDT 中分析,以避免阻塞 UI 线程;
 * 仅当确认调用上下文已被严格限速(例如 read action + 超时控制)时使用。
 */
@RequiresOptIn("Analysis should not be allowed to be run from the EDT, as otherwise it may cause IDE freezes.")
annotation class CaAllowAnalysisOnEdt
