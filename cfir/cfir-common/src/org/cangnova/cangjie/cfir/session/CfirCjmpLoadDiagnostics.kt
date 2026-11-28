package org.cangnova.cangjie.cfir.session

import java.util.concurrent.CopyOnWriteArraySet

/** CJMP common-part CJO 加载阶段产生的语义诊断类别。 */
public enum class CfirCjmpLoadDiagnosticKind {
    CJO_VERSION,
    WRONG_PACKAGE,
    FEATURE_NOT_SUBSET,
    OPTIONS_MISSING,
    DEBUG_MISMATCH,
    OPT_LEVEL_MISMATCH,
    INPUT_FILE_INVALID,
}

/**
 * 加载期产生、由反序列化符号 provider 暴露给共享 CFIR checker 的 CJMP 事实。
 *
 * requestedPackageName 标识触发本次加载请求的包；packageName 是诊断所指向的包。包名门失败时，
 * 两者有意不同，以便 checker 仍能把诊断定位回消费侧的 import。
 */
public sealed interface CfirCjmpLoadDiagnostic {
    public val requestedPackageName: String
    public val packageName: String
    public val kind: CfirCjmpLoadDiagnosticKind
    public val isError: Boolean

    /** common CJO 的格式版本不受当前 reader 支持。 */
    public data class CjoVersion(
        override val requestedPackageName: String,
        val actualVersion: String?,
        val supportedMajor: Int,
        val supportedMinor: Int,
    ) : CfirCjmpLoadDiagnostic {
        override val packageName: String get() = requestedPackageName
        override val kind: CfirCjmpLoadDiagnosticKind get() = CfirCjmpLoadDiagnosticKind.CJO_VERSION
        override val isError: Boolean get() = true
    }

    /** 显式 common CJO 的包名与请求包不一致。 */
    public data class WrongPackage(
        override val requestedPackageName: String,
        val actualPackageName: String,
    ) : CfirCjmpLoadDiagnostic {
        override val packageName: String get() = actualPackageName
        override val kind: CfirCjmpLoadDiagnosticKind get() = CfirCjmpLoadDiagnosticKind.WRONG_PACKAGE
        override val isError: Boolean get() = true
    }

    /** common CJO 中有 specific 侧未启用的语言 feature。 */
    public data class FeaturesNotSubset(
        override val requestedPackageName: String,
        val commonFileName: String,
        val extraFeatures: List<String>,
    ) : CfirCjmpLoadDiagnostic {
        override val packageName: String get() = requestedPackageName
        override val kind: CfirCjmpLoadDiagnosticKind get() = CfirCjmpLoadDiagnosticKind.FEATURE_NOT_SUBSET
        override val isError: Boolean get() = true
    }

    /** 旧 common CJO 没有序列化 options；官方语义为告警并继续加载。 */
    public data class OptionsMissing(
        override val requestedPackageName: String,
    ) : CfirCjmpLoadDiagnostic {
        override val packageName: String get() = requestedPackageName
        override val kind: CfirCjmpLoadDiagnosticKind get() = CfirCjmpLoadDiagnosticKind.OPTIONS_MISSING
        override val isError: Boolean get() = false
    }

    /** common 与当前编译的 debug 模式不一致。 */
    public data class DebugMismatch(
        override val requestedPackageName: String,
        val commonDebug: Boolean,
        val currentDebug: Boolean,
    ) : CfirCjmpLoadDiagnostic {
        override val packageName: String get() = requestedPackageName
        override val kind: CfirCjmpLoadDiagnosticKind get() = CfirCjmpLoadDiagnosticKind.DEBUG_MISMATCH
        override val isError: Boolean get() = true
    }

    /** common 与当前编译的优化级别不一致。 */
    public data class OptLevelMismatch(
        override val requestedPackageName: String,
        val commonOptLevel: String,
        val currentOptLevel: String,
    ) : CfirCjmpLoadDiagnostic {
        override val packageName: String get() = requestedPackageName
        override val kind: CfirCjmpLoadDiagnosticKind get() = CfirCjmpLoadDiagnosticKind.OPT_LEVEL_MISMATCH
        override val isError: Boolean get() = true
    }

    /** 显式 common CJO 输入路径不可用。 */
    public data class InputFileInvalid(
        override val requestedPackageName: String,
        val path: String,
    ) : CfirCjmpLoadDiagnostic {
        override val packageName: String get() = requestedPackageName
        override val kind: CfirCjmpLoadDiagnosticKind get() = CfirCjmpLoadDiagnosticKind.INPUT_FILE_INVALID
        override val isError: Boolean get() = true
    }
}

/**
 * 单个反序列化 provider 生命周期内累积的 CJMP 加载事实。
 *
 * 加载结果随 provider 公开，而不依赖 CLI 配置或进程级共享收集器，因此 IDE、LLT 与 CLI
 * 可以由同一个 checker 读取；同一包因重复符号查询而重新观察到的事实只保留一份。
 */
public class CfirCjmpLoadDiagnosticsCollector {
    private val recorded = CopyOnWriteArraySet<CfirCjmpLoadDiagnostic>()

    /** 已收集诊断的只读快照。 */
    public val diagnostics: List<CfirCjmpLoadDiagnostic> get() = recorded.toList()

    /** 指定加载请求包的诊断快照。 */
    public fun diagnosticsForPackage(requestedPackageName: String): List<CfirCjmpLoadDiagnostic> =
        recorded.filter { it.requestedPackageName == requestedPackageName }

    /** 记录一条加载事实。 */
    public fun record(diagnostic: CfirCjmpLoadDiagnostic) {
        recorded += diagnostic
    }
}
