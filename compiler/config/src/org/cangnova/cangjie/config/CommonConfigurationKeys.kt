package org.cangnova.cangjie.config

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.diagnostics.impl.BaseDiagnosticsCollector
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.config.CommonConfigurationKeys.DIAGNOSTICS_COLLECTOR
import org.cangnova.cangjie.incremental.components.EnumMatchTracker
import org.cangnova.cangjie.incremental.components.ICFileMappingTracker
import org.cangnova.cangjie.incremental.components.ImportTracker
import org.cangnova.cangjie.incremental.components.LookupTracker
import org.cangnova.cangjie.platform.TargetPlatform

/**
 * 通用编译配置键集合。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.config.CommonConfigurationKeys`。
 */
object CommonConfigurationKeys {
    /**
     * 编译消息收集器配置键。
     */
    @JvmField
    val MESSAGE_COLLECTOR_KEY = CompilerConfigurationKey.create<MessageCollector>("MESSAGE_COLLECTOR_KEY")

    /** 语言版本设置。对齐 Kotlin 键：`LANGUAGE_VERSION_SETTINGS`。 */
    @JvmField
    val LANGUAGE_VERSION_SETTINGS =
        CompilerConfigurationKey.create<LanguageVersionSettings>("LANGUAGE_VERSION_SETTINGS")

    /** 模块名。对齐 Kotlin 键：`MODULE_NAME`。 */
    @JvmField
    val MODULE_NAME = CompilerConfigurationKey.create<String>("MODULE_NAME")

    /** 名称查找跟踪器。对齐 Kotlin 键：`LOOKUP_TRACKER`。 */
    @JvmField
    val LOOKUP_TRACKER = CompilerConfigurationKey.create<LookupTracker>("LOOKUP_TRACKER")

    /** 文件映射跟踪器。对齐 Kotlin 键：`FILE_MAPPING_TRACKER`。 */
    @JvmField
    val FILE_MAPPING_TRACKER = CompilerConfigurationKey.create<ICFileMappingTracker>("FILE_MAPPING_TRACKER")

    /**
     * CFIR 诊断收集器配置键。
     */
    @JvmField
    val DIAGNOSTICS_COLLECTOR = CompilerConfigurationKey.create<BaseDiagnosticsCollector>("DIAGNOSTICS_COLLECTOR")

    /** `when` 枚举跟踪器。对齐 Kotlin 键：`ENUM_WHEN_TRACKER`。 */
    @JvmField
    val ENUM_WHEN_TRACKER = CompilerConfigurationKey.create<EnumMatchTracker>("ENUM_WHEN_TRACKER")

    /** import 跟踪器。对齐 Kotlin 键：`IMPORT_TRACKER`。 */
    @JvmField
    val IMPORT_TRACKER = CompilerConfigurationKey.create<ImportTracker>("IMPORT_TRACKER")

    /** 是否启用 FIR。对齐 Kotlin 键：`USE_FIR`。 */
    @JvmField
    val USE_FIR = CompilerConfigurationKey.create<Boolean>("USE_FIR")

    /** 是否启用 Light Tree。对齐 Kotlin 键：`USE_LIGHT_TREE`。 */
    @JvmField
    val USE_LIGHT_TREE = CompilerConfigurationKey.create<Boolean>("USE_LIGHT_TREE")

    /** 是否启用额外检查器（lint 级别的可选诊断，如冗余代码、代码风格等）。 */
    @JvmField
    val USE_FIR_EXTRA_CHECKERS = CompilerConfigurationKey.create<Boolean>("USE_FIR_EXTRA_CHECKERS")

    /** 是否输出推断日志。对齐 Kotlin 键：`DUMP_INFERENCE_LOGS`。 */
    @JvmField
    val DUMP_INFERENCE_LOGS = CompilerConfigurationKey.create<Boolean>("DUMP_INFERENCE_LOGS")

    /** 是否增量编译。对齐 Kotlin 键：`INCREMENTAL_COMPILATION`。 */
    @JvmField
    val INCREMENTAL_COMPILATION = CompilerConfigurationKey.create<Boolean>("INCREMENTAL_COMPILATION")

    /** 是否允许任意脚本出现在源码根。对齐 Kotlin 键：`ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS`。 */
    @JvmField
    val ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS =
        CompilerConfigurationKey.create<Boolean>("ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS")

    /** 是否对源码文件禁用排序。对齐 Kotlin 键：`DONT_SORT_SOURCE_FILES`。 */
    @JvmField
    val DONT_SORT_SOURCE_FILES = CompilerConfigurationKey.create<Boolean>("DONT_SORT_SOURCE_FILES")

    /** 目标平台。对齐 Kotlin 键：`TARGET_PLATFORM`。 */
    @JvmField
    val TARGET_PLATFORM = CompilerConfigurationKey.create<TargetPlatform>("TARGET_PLATFORM")
}

/**
 * 语言版本设置扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.languageVersionSettings`。
 */
var CompilerConfiguration.languageVersionSettings: LanguageVersionSettings
    get() = get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettings.DEFAULT)
    set(value) {
        put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, value)
    }

/**
 * 模块名扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.moduleName`。
 */
var CompilerConfiguration.moduleName: String?
    get() = get(CommonConfigurationKeys.MODULE_NAME)
    set(value) {
        put(CommonConfigurationKeys.MODULE_NAME, requireNotNull(value) { "nullable values are not allowed" })
    }

/**
 * 目标平台扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.targetPlatform`。
 */
var CompilerConfiguration.targetPlatform: TargetPlatform?
    get() = get(CommonConfigurationKeys.TARGET_PLATFORM)
    set(value) {
        put(CommonConfigurationKeys.TARGET_PLATFORM, requireNotNull(value) { "nullable values are not allowed" })
    }

/**
 * 名称查找跟踪器扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.lookupTracker`。
 */
var CompilerConfiguration.lookupTracker: LookupTracker?
    get() = get(CommonConfigurationKeys.LOOKUP_TRACKER)
    set(value) {
        putIfNotNull(CommonConfigurationKeys.LOOKUP_TRACKER, value)
    }

/**
 * 文件映射跟踪器扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.fileMappingTracker`。
 */
var CompilerConfiguration.fileMappingTracker: ICFileMappingTracker?
    get() = get(CommonConfigurationKeys.FILE_MAPPING_TRACKER)
    set(value) {
        putIfNotNull(CommonConfigurationKeys.FILE_MAPPING_TRACKER, value)
    }

/**
 * CFIR 诊断收集器扩展属性。
 */
var CompilerConfiguration.diagnosticsCollector: BaseDiagnosticsCollector
    get() = getOrDefault( DIAGNOSTICS_COLLECTOR) { error("diagnostic collector is not initialized") }
    set(value) { put( DIAGNOSTICS_COLLECTOR, value) }

/**
 * `when` 枚举跟踪器扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.enumMatchTracker`。
 */
var CompilerConfiguration.enumMatchTracker: EnumMatchTracker?
    get() = get(CommonConfigurationKeys.ENUM_WHEN_TRACKER)
    set(value) {
        putIfNotNull(CommonConfigurationKeys.ENUM_WHEN_TRACKER, value)
    }

/**
 * import 跟踪器扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.importTracker`。
 */
var CompilerConfiguration.importTracker: ImportTracker?
    get() = get(CommonConfigurationKeys.IMPORT_TRACKER)
    set(value) {
        putIfNotNull(CommonConfigurationKeys.IMPORT_TRACKER, value)
    }

/**
 * 是否启用 FIR 扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.useFir`。
 */
var CompilerConfiguration.useFir: Boolean
    get() = getBoolean(CommonConfigurationKeys.USE_FIR)
    set(value) {
        put(CommonConfigurationKeys.USE_FIR, value)
    }

/**
 * 是否启用 Light Tree 扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.useLightTree`。
 */
var CompilerConfiguration.useLightTree: Boolean
    get() = getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)
    set(value) {
        put(CommonConfigurationKeys.USE_LIGHT_TREE, value)
    }

/**
 * 是否启用额外检查器。
 *
 * 额外检查器（extra checkers）执行可选的 lint 级别诊断，包括但不限于：
 * - 冗余修饰符检测（如多余的 public/open）
 * - 未使用表达式/参数检测
 * - 代码风格建议
 *
 * 这些检查不影响编译正确性，仅提供代码质量提示。
 * 官方仓颉编译器（C++）未做此分层，但在 IDE 场景下按需开启额外诊断是合理的。
 */
var CompilerConfiguration.useCfirExtraCheckers: Boolean
    get() = getBoolean(CommonConfigurationKeys.USE_FIR_EXTRA_CHECKERS)
    set(value) {
        put(CommonConfigurationKeys.USE_FIR_EXTRA_CHECKERS, value)
    }

/**
 * 是否输出推断日志扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.dumpInferenceLogs`。
 */
var CompilerConfiguration.dumpInferenceLogs: Boolean
    get() = getBoolean(CommonConfigurationKeys.DUMP_INFERENCE_LOGS)
    set(value) {
        put(CommonConfigurationKeys.DUMP_INFERENCE_LOGS, value)
    }

/**
 * 是否增量编译扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.incrementalCompilation`。
 */
var CompilerConfiguration.incrementalCompilation: Boolean
    get() = getBoolean(CommonConfigurationKeys.INCREMENTAL_COMPILATION)
    set(value) {
        put(CommonConfigurationKeys.INCREMENTAL_COMPILATION, value)
    }

/**
 * 是否允许任意脚本扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.allowAnyScriptsInSourceRoots`。
 */
var CompilerConfiguration.allowAnyScriptsInSourceRoots: Boolean
    get() = getBoolean(CommonConfigurationKeys.ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS)
    set(value) {
        put(CommonConfigurationKeys.ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS, value)
    }

/**
 * 是否禁用源码排序扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.dontSortSourceFiles`。
 */
var CompilerConfiguration.dontSortSourceFiles: Boolean
    get() = getBoolean(CommonConfigurationKeys.DONT_SORT_SOURCE_FILES)
    set(value) {
        put(CommonConfigurationKeys.DONT_SORT_SOURCE_FILES, value)
    }

/**
 * 编译消息收集器扩展属性，缺省为不会输出消息的 `MessageCollector.NONE`。
 */
var CompilerConfiguration.messageCollector: MessageCollector
    get() = get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    set(value) {
        put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, value)
    }
