import os, time

R = 'D:/code/intellij/cangjie'


def read(p):
    with open(p, 'rb') as f:
        return f.read().decode('utf-8')


def robust_write(path, text):
    data = text.encode('utf-8')
    tmp = path + '.cjmp.tmp'
    last = None
    for _ in range(20):
        try:
            with open(tmp, 'wb') as f:
                f.write(data)
            os.replace(tmp, path)
            return
        except OSError as e:
            last = e
            time.sleep(0.5)
    raise last


def replace_once(text, old, new, what):
    if text.count(old) == 1:
        return text.replace(old, new, 1)
    nl = '\r\n' if '\r\n' in text else '\n'
    old2, new2 = old.replace('\n', nl), new.replace('\n', nl)
    c = text.count(old2)
    assert c == 1, f"{what}: anchor count {c} (lf={text.count(old)})"
    return text.replace(old2, new2, 1)


# ---------------------------------------------------------------- P4.3a-1 组件：模块选项
p = R + '/cfir/cfir-common/src/org/cangnova/cangjie/cfir/session/CfirCjmpSettings.kt'
t = read(p)
t = replace_once(
    t,
    ''' * @property isChirOutput 当前调用是否为 CHIR 输出模式（官方 `IsCompilingCJMP()` 的 Common 判据）。
 * @property explicitMode 显式模式覆盖（测试指令 / IDE 模块 kind）；为 null 时由前三项推导。
 */''',
    ''' * @property isChirOutput 当前调用是否为 CHIR 输出模式（官方 `IsCompilingCJMP()` 的 Common 判据）。
 * @property explicitMode 显式模式覆盖（测试指令 / IDE 模块 kind）；为 null 时由前三项推导。
 * @property moduleDebug 写出/加载 common part cjo 时的 debug 选项（官方 `Option::debug`）。
 * @property moduleOptLevel 优化级别字符串（官方 `Option` 的优化级别，`O0`/`O1`/…）。
 */''',
    'component kdoc')
t = replace_once(
    t,
    '''    /** 显式模式覆盖；null 表示按官方谓词推导。 */
    val explicitMode: CfirCjmpMode? = null,
) : CfirSessionComponent {''',
    '''    /** 显式模式覆盖；null 表示按官方谓词推导。 */
    val explicitMode: CfirCjmpMode? = null,
    /** 写出/加载 common part cjo 时的 debug 选项（官方 `Option::debug`）。 */
    val moduleDebug: Boolean = false,
    /** 优化级别字符串（`O0`..`Oz`），对齐官方 `module_common_cjo_opt_mismatch` 诊断文案。 */
    val moduleOptLevel: String = DEFAULT_OPT_LEVEL,
) : CfirSessionComponent {''',
    'component fields')
t = replace_once(
    t,
    '''    companion object {
        /**
         * 两个 common-part 选项的数量配对校验''',
    '''    companion object {
        /** 官方 `OptimizationLevel::O0` 的字符串形；未显式配置时的默认优化级别。 */
        public const val DEFAULT_OPT_LEVEL: String = "O0"

        /**
         * 两个 common-part 选项的数量配对校验''',
    'component companion')
robust_write(p, t)
print('a1 component module options')

# ---------------------------------------------------------------- P4.3a-2 配置键
p = R + '/cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/configuration/CfirFrontendConfigurationKeys.kt'
t = read(p)
t = replace_once(
    t,
    '''    /** 显式注入的 CJMP 编译模式（测试 `// CJMP_MODE` 与 IDE 模块 kind 用；不设则由选项推导）。 */
    @JvmField
    val CJMP_MODE =
        CompilerConfigurationKey.create<CfirCjmpMode>("CJMP_MODE")
}''',
    '''    /** 显式注入的 CJMP 编译模式（测试 `// CJMP_MODE` 与 IDE 模块 kind 用；不设则由选项推导）。 */
    @JvmField
    val CJMP_MODE =
        CompilerConfigurationKey.create<CfirCjmpMode>("CJMP_MODE")

    /** 写出 common part cjo 时内嵌的 debug 选项（官方 `Option::debug` 对位）。 */
    @JvmField
    val CJMP_MODULE_DEBUG =
        CompilerConfigurationKey.create<Boolean>("CJMP_MODULE_DEBUG")

    /** 写出 common part cjo 时内嵌的优化级别（官方 `Option` 优化级别对位）。 */
    @JvmField
    val CJMP_MODULE_OPT_LEVEL =
        CompilerConfigurationKey.create<String>("CJMP_MODULE_OPT_LEVEL")
}''',
    'config keys')
t = t.rstrip('\n') + '''

/** 写出 common part cjo 时内嵌的 debug 选项。 */
var CompilerConfiguration.cjmpModuleDebug: Boolean
    get() = getBoolean(CfirFrontendConfigurationKeys.CJMP_MODULE_DEBUG)
    set(value) = put(CfirFrontendConfigurationKeys.CJMP_MODULE_DEBUG, value)

/** 写出 common part cjo 时内嵌的优化级别。 */
var CompilerConfiguration.cjmpModuleOptLevel: String
    get() = get(CfirFrontendConfigurationKeys.CJMP_MODULE_OPT_LEVEL)
        ?: org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent.DEFAULT_OPT_LEVEL
    set(value) = put(CfirFrontendConfigurationKeys.CJMP_MODULE_OPT_LEVEL, value)
'''
robust_write(p, t)
print('a2 config keys + extensions')

# ---------------------------------------------------------------- P4.3a-3 context utils 接线
p = R + '/cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirSessionFactoryContextUtils.kt'
t = read(p)
t = replace_once(
    t,
    '''import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode\n''',
    '''import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode\n'''
    '''import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpModuleDebug\n'''
    '''import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpModuleOptLevel\n''',
    'a3 imports')
t = replace_once(
    t,
    '''        isChirOutput = configuration.cjmpChirOutput,
        explicitMode = configuration.cjmpMode,
    )''',
    '''        isChirOutput = configuration.cjmpChirOutput,
        explicitMode = configuration.cjmpMode,
        moduleDebug = configuration.cjmpModuleDebug,
        moduleOptLevel = configuration.cjmpModuleOptLevel,
    )''',
    'a3 wiring')
robust_write(p, t)
print('a3 context utils')

# ---------------------------------------------------------------- P4.3a-4 加载期诊断收集组件
p = R + '/cfir/cfir-common/src/org/cangnova/cangjie/cfir/session/CfirCjmpLoadDiagnostics.kt'
content = '''package org.cangnova.cangjie.cfir.session

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
 */
public data class CfirCjmpLoadDiagnostic(
    val kind: CfirCjmpLoadDiagnosticKind,
    val packageName: String,
    val message: String,
)

/**
 * 当前 session 收集到的 CJMP 加载门诊断。
 *
 * 可变组件：一次编译内可累积多个包的加载结果；同一 provider 的重复加载由加载缓存保证只记一次。
 */
public open class CfirCjmpLoadDiagnosticsComponent : CfirSessionComponent {
    private val recorded = mutableListOf<CfirCjmpLoadDiagnostic>()

    /** 已收集诊断的只读快照。 */
    public val diagnostics: List<CfirCjmpLoadDiagnostic> get() = recorded.toList()

    /** 记录一条加载门诊断。 */
    public fun record(diagnostic: CfirCjmpLoadDiagnostic) {
        recorded += diagnostic
    }
}

/** 当前 session 的 CJMP 加载门诊断收集器。 */
public val CfirSession.cjmpLoadDiagnostics: CfirCjmpLoadDiagnosticsComponent by
    CfirSession.sessionComponentAccessorWithDefault(CfirCjmpLoadDiagnosticsComponent())
'''
robust_write(p, content)
print('a4 load diagnostics component')

print('phase4_3a complete')
