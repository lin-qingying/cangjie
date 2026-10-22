package org.cangnova.cangjie.config

/**
 * 命令行编译调用选项的强类型配置键。
 *
 * 参数解析与前端阶段通过 [CompilerConfiguration] 交换这些调用事实，避免前端直接依赖
 * CLI 参数对象，也避免把 invocation 状态散落在阶段实现中。
 */
object CompilerInvocationConfigurationKeys {
    @JvmField
    val COMPILE_MACRO = CompilerConfigurationKey.create<Boolean>("COMPILE_MACRO")

    @JvmField
    val DEBUG_MACRO = CompilerConfigurationKey.create<Boolean>("DEBUG_MACRO")

    @JvmField
    val PARALLEL_MACRO_EXPANSION = CompilerConfigurationKey.create<Boolean>("PARALLEL_MACRO_EXPANSION")

    @JvmField
    val EXPORT_FOR_TEST = CompilerConfigurationKey.create<Boolean>("EXPORT_FOR_TEST")

    @JvmField
    val COMPILE_TESTS_ONLY = CompilerConfigurationKey.create<Boolean>("COMPILE_TESTS_ONLY")

    @JvmField
    val DIAGNOSTIC_FORMAT = CompilerConfigurationKey.create<String>("DIAGNOSTIC_FORMAT")

    @JvmField
    val ERROR_COUNT_LIMIT = CompilerConfigurationKey.create<String>("ERROR_COUNT_LIMIT")

    @JvmField
    val WARNING_GROUPS_ENABLED = CompilerConfigurationKey.create<Set<String>>("WARNING_GROUPS_ENABLED")

    @JvmField
    val DUMP_TOKENS = CompilerConfigurationKey.create<Boolean>("DUMP_TOKENS")

    @JvmField
    val DUMP_PARSE = CompilerConfigurationKey.create<Boolean>("DUMP_PARSE")

    @JvmField
    val DUMP_AST = CompilerConfigurationKey.create<Boolean>("DUMP_AST")

    @JvmField
    val DUMP_MACRO = CompilerConfigurationKey.create<Boolean>("DUMP_MACRO")

    @JvmField
    val TYPECHECK_ONLY = CompilerConfigurationKey.create<Boolean>("TYPECHECK_ONLY")
}

/** 是否为宏源码包编译调用。 */
var CompilerConfiguration.compileMacro: Boolean
    get() = getBoolean(CompilerInvocationConfigurationKeys.COMPILE_MACRO)
    set(value) = put(CompilerInvocationConfigurationKeys.COMPILE_MACRO, value)

/** 是否启用宏调试语义。 */
var CompilerConfiguration.debugMacro: Boolean
    get() = getBoolean(CompilerInvocationConfigurationKeys.DEBUG_MACRO)
    set(value) = put(CompilerInvocationConfigurationKeys.DEBUG_MACRO, value)

/** 是否允许宏展开并行执行。 */
var CompilerConfiguration.parallelMacroExpansion: Boolean
    get() = getBoolean(CompilerInvocationConfigurationKeys.PARALLEL_MACRO_EXPANSION)
    set(value) = put(CompilerInvocationConfigurationKeys.PARALLEL_MACRO_EXPANSION, value)

/** 是否为 test compilation 导出声明。 */
var CompilerConfiguration.exportForTest: Boolean
    get() = getBoolean(CompilerInvocationConfigurationKeys.EXPORT_FOR_TEST)
    set(value) = put(CompilerInvocationConfigurationKeys.EXPORT_FOR_TEST, value)

/** 是否只编译 test 源码。 */
var CompilerConfiguration.compileTestsOnly: Boolean
    get() = getBoolean(CompilerInvocationConfigurationKeys.COMPILE_TESTS_ONLY)
    set(value) = put(CompilerInvocationConfigurationKeys.COMPILE_TESTS_ONLY, value)

/** 诊断输出格式；未设置时由诊断报告入口选择默认格式。 */
var CompilerConfiguration.diagnosticFormat: String?
    get() = get(CompilerInvocationConfigurationKeys.DIAGNOSTIC_FORMAT)
    set(value) {
        if (value == null) {
            remove(CompilerInvocationConfigurationKeys.DIAGNOSTIC_FORMAT)
        } else {
            put(CompilerInvocationConfigurationKeys.DIAGNOSTIC_FORMAT, value)
        }
    }

/** 编译错误数量上限的原始调用参数。 */
var CompilerConfiguration.errorCountLimit: String?
    get() = get(CompilerInvocationConfigurationKeys.ERROR_COUNT_LIMIT)
    set(value) {
        if (value == null) {
            remove(CompilerInvocationConfigurationKeys.ERROR_COUNT_LIMIT)
        } else {
            put(CompilerInvocationConfigurationKeys.ERROR_COUNT_LIMIT, value)
        }
    }

/** 当前编译调用启用的告警组。 */
var CompilerConfiguration.warningGroupsEnabled: Set<String>
    get() = get(CompilerInvocationConfigurationKeys.WARNING_GROUPS_ENABLED, emptySet())
    set(value) = put(CompilerInvocationConfigurationKeys.WARNING_GROUPS_ENABLED, value.toSet())

/** 是否输出词法 token。 */
var CompilerConfiguration.dumpTokens: Boolean
    get() = getBoolean(CompilerInvocationConfigurationKeys.DUMP_TOKENS)
    set(value) = put(CompilerInvocationConfigurationKeys.DUMP_TOKENS, value)

/** 是否输出解析结果。 */
var CompilerConfiguration.dumpParse: Boolean
    get() = getBoolean(CompilerInvocationConfigurationKeys.DUMP_PARSE)
    set(value) = put(CompilerInvocationConfigurationKeys.DUMP_PARSE, value)

/** 是否输出语法树。 */
var CompilerConfiguration.dumpAst: Boolean
    get() = getBoolean(CompilerInvocationConfigurationKeys.DUMP_AST)
    set(value) = put(CompilerInvocationConfigurationKeys.DUMP_AST, value)

/** 是否输出宏展开信息。 */
var CompilerConfiguration.dumpMacro: Boolean
    get() = getBoolean(CompilerInvocationConfigurationKeys.DUMP_MACRO)
    set(value) = put(CompilerInvocationConfigurationKeys.DUMP_MACRO, value)

/** 是否仅执行类型检查阶段。 */
var CompilerConfiguration.typecheckOnly: Boolean
    get() = getBoolean(CompilerInvocationConfigurationKeys.TYPECHECK_ONLY)
    set(value) = put(CompilerInvocationConfigurationKeys.TYPECHECK_ONLY, value)
