package org.cangnova.cangjie.cfir.session

import java.util.concurrent.CopyOnWriteArraySet

/**
 * CJMP common-part cjo 加载门诊断类别。
 *
 * 对位官方 `ASTLoader::PreloadCommonPartOfPackage` 的门序列与诊断名（计划 §8.3）：
 * - [CJO_VERSION]：格式版本门（官方 `CheckCjoVersion`，缺版本即拒）；
 * - [WRONG_PACKAGE]：包名一致门（官方 `module_common_cjo_wrong_package`）；
 * - [FEATURE_NOT_SUBSET]：features 子集门（官方 `feature_is_not_subset_of_child_set`）；
 * - [OPTIONS_MISMATCH]：编译选项匹配门（官方 `module_common_cjo_{debug,opt}_mismatch`，含
 *   缺失选项的 `module_common_cjo_no_options`）。
 */
public enum class CfirCjmpLoadDiagnosticKind {
    CJO_VERSION,
    WRONG_PACKAGE,
    FEATURE_NOT_SUBSET,
    OPTIONS_MISMATCH,
}

/**
 * 加载期（session/checker 之前）产生的 CJMP 诊断。
 *
 * 通道约定（计划 G17）：加载门发生在 `DiagnosticReporter` 可用之前，因此按 cjd 家族的
 * collector-list 先例——加载器把结构化诊断放进 session 组件，装配层（编译输出 / LLT 断言）
 * 读取该列表外显，而不是在加载层直接上报。
 *
 * @property kind 诊断类别。
 * @property packageName 出错包的完整包名。
 * @property message 面向用户的消息文本（对位官方诊断文案）。
 * @property isError 是否为错误级（官方 `module_common_cjo_no_options` 为 WARNING，其余加载门均为 ERROR）。
 */
public data class CfirCjmpLoadDiagnostic(
    val kind: CfirCjmpLoadDiagnosticKind,
    val packageName: String,
    val message: String,
    val isError: Boolean = true,
)

/**
 * 当前编译调用收集到的 CJMP 加载门诊断。
 *
 * 可变组件：一次编译内可累积多个包的加载结果；同一 provider 的重复加载由加载缓存保证只记一次。
 * 生命周期 = 一次编译调用：由 session 工厂上下文创建一个实例，同时注册到库会话（记录方，
 * 反序列化 provider 所在）与源码会话（外显方，装配层从这里取）。
 */
public open class CfirCjmpLoadDiagnosticsComponent : CfirSessionComponent {
    private val recorded = CopyOnWriteArraySet<CfirCjmpLoadDiagnostic>()

    /** 已收集诊断的只读快照。 */
    public val diagnostics: List<CfirCjmpLoadDiagnostic> get() = recorded.toList()

    /** 记录一条加载门诊断（同一诊断重复记录只保留一次：缺失包可能被多次查询）。 */
    public fun record(diagnostic: CfirCjmpLoadDiagnostic) {
        recorded += diagnostic
    }
}

/**
 * 当前 session 的 CJMP 加载门诊断收集器；未注册（LL/测试会话等无外显通道的装配）时为 null。
 *
 * 刻意不提供共享默认实例：可变收集器若以进程级默认值兜底，会在无关会话之间串扰。
 */
public val CfirSession.cjmpLoadDiagnostics: CfirCjmpLoadDiagnosticsComponent? by
    CfirSession.nullableSessionComponentAccessor()
