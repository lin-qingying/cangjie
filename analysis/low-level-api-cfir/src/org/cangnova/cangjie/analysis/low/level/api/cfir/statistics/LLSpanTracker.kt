package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Scope

/**
 * 阶段链路 span 的开闭记录器。
 *
 * 每个统计域持有一个实例。埋点接缝在操作开始时调用 [start]（创建 span、设为当前
 * context 中的活动 span），在结束时调用 [end]（补齐结果属性、关闭 scope、结束 span）。
 * 后创建的 span 自动成为当前活动 span 的子 span，因此同线程上的阶段调用会自然形成
 * "分析会话 → 阶段 → 子操作"的链路树；跨线程时以调用发生线程的当前 context 为准。
 *
 * ## 与指标的分工
 *
 * 指标（Prometheus）回答"聚合看哪里慢"，span（Jaeger）回答"这一次调用里时间花在哪"。
 * 每个耗时接缝同时产出两者：`duration` 直方图与同名 span，两个信号可在
 * `cangjie.*` 命名空间下按名字直接对照。
 *
 * ## 配对契约
 *
 * 调用方必须保证同一线程上 [start] 与 [end] 严格成对（各接缝的 `measure` 包装均以
 * `try/finally` 保证）。嵌套的同一 key 按后进先出配对；未找到配对记录的 [end] 是空操作，
 * 未配对结束的 [start] 只会在该线程存活期间保留 span 对象（不会导出）。
 */
internal class LLSpanTracker(private val tracer: Tracer) {
    /**
     * 一个已打开但尚未结束的 span，连同它压入的 context scope。
     */
    private class OpenSpan(val key: Any, val span: Span, val scope: Scope)

    /**
     * 当前线程已打开的 span 栈；按 key 后进先出配对。
     */
    private val openSpans: ThreadLocal<ArrayDeque<OpenSpan>> = ThreadLocal.withInitial { ArrayDeque() }

    /**
     * 打开一个 span 并设为当前活动 span。
     *
     * @param key 配对键；[end] 按它找到最近打开的那一个 span。
     * @param name span 名，命名规则见 [LLStatisticsScopes]。
     * @param configure 在 start 时即可确定的属性。
     */
    fun start(key: Any, name: String, configure: SpanBuilder.() -> Unit = {}) {
        val span = tracer.spanBuilder(name).apply(configure).startSpan()
        val scope = span.makeCurrent()
        openSpans.get().addLast(OpenSpan(key, span, scope))
    }

    /**
     * 结束最近一个以 [key] 打开的 span。
     *
     * @param configure 在结束时才能确定的属性（结果归类、产出计数等）。
     */
    fun end(key: Any, configure: Span.() -> Unit = {}) {
        val stack = openSpans.get()
        val index = stack.indexOfLast { it.key == key }
        if (index < 0) return

        val open = stack.removeAt(index)
        open.span.apply(configure)
        open.scope.close()
        open.span.end()
    }
}

/**
 * 链路 span 的属性名。
 *
 * 与指标名的关系：span 名取对应指标名去掉指标后缀后的前缀（如
 * `cangjie.analysis.resolve.phases.types`），维度信息放在属性里；属性统一用
 * `cangjie.` 前缀，测试与看板（Jaeger 查询、PromQL 的 label）都引用这里的定义。
 */
@LLStatisticsOnlyApi
object LLStatisticsSpanAttributes {
    /**
     * 语义解析阶段（[org.cangnova.cangjie.cfir.declarations.CfirResolvePhase] 的 `name`）。
     */
    val resolvePhase: AttributeKey<String> = AttributeKey.stringKey("cangjie.resolve.phase")

    /**
     * 语义解析阶段覆盖的 CFIR 文件数；仅全量解析路径设置。
     */
    val resolveFiles: AttributeKey<Long> = AttributeKey.longKey("cangjie.resolve.files")

    /**
     * 语义解析阶段推进的声明数；仅按需解析路径设置。
     */
    val resolveDeclarations: AttributeKey<Long> = AttributeKey.longKey("cangjie.resolve.declarations")

    /**
     * raw 构建来源（`psi` / `lightTree`）。
     */
    val rawBuildSource: AttributeKey<String> = AttributeKey.stringKey("cangjie.rawBuild.source")

    /**
     * raw 构建阶段（`parse` / `convert`）。
     */
    val rawBuildStage: AttributeKey<String> = AttributeKey.stringKey("cangjie.rawBuild.stage")

    /**
     * raw 构建的 body 策略；解析阶段不设置。
     */
    val rawBuildBodyBuildingMode: AttributeKey<String> = AttributeKey.stringKey("cangjie.rawBuild.bodyBuildingMode")

    /**
     * 宏构造阶段（`symbolIndex` / `importBinding` / `expansion`）。
     */
    val macroStage: AttributeKey<String> = AttributeKey.stringKey("cangjie.macro.stage")

    /**
     * 宏构造运行模式；仅展开段设置。
     */
    val macroMode: AttributeKey<String> = AttributeKey.stringKey("cangjie.macro.mode")

    /**
     * 宏展开结果归类；仅展开段设置。
     */
    val macroOutcome: AttributeKey<String> = AttributeKey.stringKey("cangjie.macro.outcome")

    /**
     * 宏构造覆盖的 pre-macro 文件数；仅展开段设置。
     */
    val macroFiles: AttributeKey<Long> = AttributeKey.longKey("cangjie.macro.files")

    /**
     * 宏构造覆盖的宏 surface 数；仅展开段设置。
     */
    val macroSurfaces: AttributeKey<Long> = AttributeKey.longKey("cangjie.macro.surfaces")

    /**
     * 诊断遍历阶段（`sema` / `post_sema`）。
     */
    val diagnosticsPhase: AttributeKey<String> = AttributeKey.stringKey("cangjie.diagnostics.phase")

    /**
     * 本次诊断收集产出的诊断数。
     */
    val diagnosticsCount: AttributeKey<Long> = AttributeKey.longKey("cangjie.diagnostics.count")

    /**
     * `.cjo` 反序列化阶段（`packageLoad` / `declaration`）。
     */
    val deserializationStage: AttributeKey<String> = AttributeKey.stringKey("cangjie.deserialization.stage")

    /**
     * `.cjo` 惰性反序列化的声明数；仅 `declaration` 阶段设置。
     */
    val deserializationDeclarations: AttributeKey<Long> = AttributeKey.longKey("cangjie.deserialization.declarations")

    /**
     * PSI 解析入口（`file` / `lambdaExpression` / …）。
     */
    val parseKind: AttributeKey<String> = AttributeKey.stringKey("cangjie.parse.kind")

    /**
     * PSI 解析是否正常返回；`false` 表示以异常结束。
     */
    val parseSucceeded: AttributeKey<Boolean> = AttributeKey.booleanKey("cangjie.parse.succeeded")

    /**
     * session 创建的模块种类（[CaModuleKind.metricSuffix]）。
     */
    val sessionKind: AttributeKey<String> = AttributeKey.stringKey("cangjie.session.kind")
}
