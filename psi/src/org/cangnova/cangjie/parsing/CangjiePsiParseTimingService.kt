package org.cangnova.cangjie.parsing

import com.intellij.openapi.project.Project
import kotlin.time.TimeSource

/**
 * 仓颉 PSI 解析入口的种类。
 *
 * 分入口而不是只记总量：补全里的 lambda 片段解析与整文件解析成本差一个量级，
 * 混在一起无法回答"是哪种解析慢"。
 *
 * @property metricSuffix 指标名中的入口段。
 */
enum class CangjiePsiParseKind(val metricSuffix: String) {
    /** 整文件解析（`SOURCE` / `DECLARATION` / `MACRO_CALL` 三种 source kind 走同一入口）。 */
    FILE("file"),

    /** lambda 表达式片段。 */
    LAMBDA_EXPRESSION("lambdaExpression"),

    /** 语句块代码片段。 */
    BLOCK_CODE_FRAGMENT("blockCodeFragment"),

    /** 表达式代码片段。 */
    EXPRESSION_CODE_FRAGMENT("expressionCodeFragment"),

    /** 类型代码片段。 */
    TYPE_CODE_FRAGMENT("typeCodeFragment"),

    /** 块表达式。 */
    BLOCK_EXPRESSION("blockExpression"),
}

/**
 * 仓颉 PSI 解析耗时上报服务。
 *
 * ## 为什么是工程级服务而不是 session 组件
 *
 * PSI 解析由 IntelliJ 平台在需要 AST 时回调（[com.intellij.psi.tree.IStubFileElementType.doParseContents]
 * 与 `ICodeFragmentElementType.doParseContents`），此时 CFIR session 尚不存在——session 是解析完成之后
 * 才创建的。因此 `CfirSessionComponent` 这条路在这里拿不到任何东西。
 *
 * 解析入口手里只有 [com.intellij.lang.PsiBuilder]，它暴露 `project`，工程级服务是这里唯一可用的上下文。
 * 与 [org.cangnova.cangjie.psi.CangJieReferenceProvidersService] 的做法一致：接口声明在 `psi`，
 * 实现在 `analysis/low-level-api-cfir`，由描述符以 serviceInterface / serviceImplementation 组合注册。
 *
 * ## 成本
 *
 * 未注册实现时 [getInstance] 返回无操作兜底，解析路径只多一次 service 查找（并发 map 读），
 * 不取单调时钟、不分配统计对象。解析本身是毫秒量级，这一次查找可以忽略。
 */
open class CangjiePsiParseTimingService {
    /**
     * 一次 PSI 解析结束。
     *
     * @param kind 解析入口种类
     * @param elapsedNanos 本次解析耗时（纳秒）
     * @param succeeded 是否正常返回；`false` 表示以异常结束，耗时为已消耗部分
     */
    open fun onParseFinished(kind: CangjiePsiParseKind, elapsedNanos: Long, succeeded: Boolean) {
    }

    /**
     * 一次 PSI 解析开始。
     *
     * 默认空实现：只有需要记录链路 span 的实现才覆写它。开始回调与 [onParseFinished]
     * 在同一线程上严格成对。
     */
    open fun onParseStarted(kind: CangjiePsiParseKind) {
    }

    companion object {
        /**
         * 无统计后端时的兜底实现：所有回调都是空操作。
         */
        private val NO_OP: CangjiePsiParseTimingService = CangjiePsiParseTimingService()

        /**
         * 取 [project] 的解析耗时上报服务；未注册实现时返回无操作兜底。
         */
        @JvmStatic
        fun getInstance(project: Project): CangjiePsiParseTimingService =
            project.getService(CangjiePsiParseTimingService::class.java) ?: NO_OP
    }
}

/**
 * 在 [service] 上计时执行 [action] 并上报解析耗时。
 *
 * 解析抛异常时按未完成上报已消耗耗时后原样重抛：解析失败的耗时同样值得看见。
 */
inline fun <T> CangjiePsiParseTimingService.measure(kind: CangjiePsiParseKind, action: () -> T): T {
    onParseStarted(kind)
    val startedAt = TimeSource.Monotonic.markNow()
    var succeeded = true
    try {
        return action()
    } catch (throwable: Throwable) {
        succeeded = false
        throw throwable
    } finally {
        onParseFinished(kind, startedAt.elapsedNow().inWholeNanoseconds, succeeded)
    }
}
