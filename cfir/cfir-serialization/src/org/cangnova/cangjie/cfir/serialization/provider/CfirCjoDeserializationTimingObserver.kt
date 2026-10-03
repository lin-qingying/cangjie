package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import kotlin.time.TimeSource

/**
 * `.cjo` 反序列化阶段。
 *
 * 两个阶段对应"IDE 打开一个 cjo 库时的两笔开销"，优化方向不同：读不出问题是包文件读与
 * FlatBuffers 解析慢（`PACKAGE_LOAD`），读得出但符号查询卡是惰性逐声明反序列化慢
 * （`DECLARATION`）。合成一个耗时会让人把力气用错地方。
 */
enum class CfirCjoDeserializationStage(val metricSuffix: String) {
    /**
     * 首次加载某个包：读 `.cjo` 字节、解析 FlatBuffers 根、建反序列化上下文并初始化包 scope。
     */
    PACKAGE_LOAD("packageLoad"),

    /**
     * 符号查询未命中后按声明索引惰性反序列化。
     */
    DECLARATION("declaration"),
}

/**
 * `.cjo` 反序列化耗时观察者。
 *
 * 挂 session 上、宿主注册实现后才计时，未注册时反序列化路径零开销——`.cjo` 加载发生在
 * 用户动作中间（IDE 打开工程、补全一个库符号），是"IDE 卡住"的主要来源之一。
 *
 * 与 stub 反序列化（`deserialization.classLike`）是两条独立通道：前者读 IntelliJ stub 树，
 * 两者在同一进程里都会发生（`.cjo` 库与源码库混用），必须能分开看。
 */
interface CfirCjoDeserializationTimingObserver : CfirSessionComponent {
    /**
     * 一次 `.cjo` 反序列化阶段结束。
     *
     * @param stage 本次执行的阶段。
     * @param elapsedNanos 本阶段的单调时钟耗时（纳秒）。
     * @param declarationCount 本次反序列化涉及的声明数；仅 [CfirCjoDeserializationStage.DECLARATION]
     *   有意义，包加载阶段传 0——它加载的是整个包，没有"声明数"这个观测点。
     */
    fun onCjoDeserializationFinished(
        stage: CfirCjoDeserializationStage,
        elapsedNanos: Long,
        declarationCount: Int,
    )
}

/**
 * 在 [observer] 存在时对包加载计时并上报；未注册观察者时直接执行 [action]。
 */
inline fun <T> CfirCjoDeserializationTimingObserver?.measureCjoPackageLoad(action: () -> T): T {
    if (this == null) return action()

    val startedAt = TimeSource.Monotonic.markNow()
    try {
        return action()
    } finally {
        onCjoDeserializationFinished(
            CfirCjoDeserializationStage.PACKAGE_LOAD,
            startedAt.elapsedNow().inWholeNanoseconds,
            0,
        )
    }
}

/**
 * 在 [observer] 存在时对按声明惰性反序列化计时并上报；未注册观察者时直接执行 [action]。
 *
 * [action] 抛出异常时仍上报已消耗的时长：加载失败的包同样是耗时，值得看见。
 */
inline fun <T> CfirCjoDeserializationTimingObserver?.measureCjoDeclaration(
    declarationCount: Int,
    action: () -> T,
): T {
    if (this == null) return action()

    val startedAt = TimeSource.Monotonic.markNow()
    try {
        return action()
    } finally {
        onCjoDeserializationFinished(
            CfirCjoDeserializationStage.DECLARATION,
            startedAt.elapsedNow().inWholeNanoseconds,
            declarationCount,
        )
    }
}

/**
 * `.cjo` 反序列化耗时观察者；宿主未注册时为 `null`，反序列化路径据此跳过计时。
 */
val CfirSession.cjoDeserializationTimingObserverOrNull: CfirCjoDeserializationTimingObserver? by
    CfirSession.nullableSessionComponentAccessor()

/**
 * 注册 `.cjo` 反序列化耗时观察者。
 */
fun CfirSession.registerCjoDeserializationTimingObserver(observer: CfirCjoDeserializationTimingObserver) {
    register(CfirCjoDeserializationTimingObserver::class, observer)
}
