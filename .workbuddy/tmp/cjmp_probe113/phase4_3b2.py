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


# =====================================================================
# 3) 包头数据类 + 加载门（新文件）
# =====================================================================
p = R + '/cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoPackageHeader.kt'
t = read(p)
t = t.rstrip('\n') + '''

/** `.cjo` 包内记录的格式版本（官方 `Package.cjoVersion`）。 */
data class CjoModuleVersion(
    /** 主版本。 */
    val major: UByte,
    /** 次版本。 */
    val minor: UByte,
    /** 修订版本。 */
    val patch: UByte,
) {
    override fun toString(): String = "${'$'}major.${'$'}minor.${'$'}patch"
}

/** `.cjo` 包内记录的编译选项（官方 `Package.options`）。 */
data class CjoModuleOptionInfo(
    /** 官方 `Option::debug`。 */
    val debug: Boolean,
    /** 官方优化级别序号（O0=0, O1=1, O2=2, O3=3, Os=4, Oz=5）。 */
    val optLevel: UByte,
)
'''
robust_write(p, t)
print('3 header data classes')

p = R + '/cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjmpCommonPartLoadGate.kt'
content = '''package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnostic
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnosticKind

/**
 * CJMP common-part cjo 加载门（官方 `ASTLoader::PreloadCommonPartOfPackage` 对位，计划 §8.3）。
 *
 * 门序列与官方一致：
 * 1. cjo 格式版本（官方 `CheckCjoVersion`，**缺版本即拒**）；
 * 2. 包名一致（官方 `module_common_cjo_wrong_package`）；
 * 3. features 子集 `common ⊆ specific`（官方 `feature_is_not_subset_of_child_set`）；
 * 4. 编译选项匹配（官方 `module_common_cjo_no_options` / `_debug_mismatch` / `_opt_mismatch`）。
 *
 * 诊断不在此上报（加载期 `DiagnosticReporter` 尚不可用），按 G17 约定返回结构化诊断，
 * 由加载器收进 session 的 [org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnosticsComponent]，
 * 再由装配层外显。**属性位装填保持无版本判断**（D15）：兼容性完全由格式版本门承担。
 */
object CjmpCommonPartLoadGate {
    /** 当前支持的 cjo 格式版本（= [CjoConstants]，官方 `ModuleFormat.CjoVersion` 0.1.0）。 */
    val supportedVersion: CjoModuleVersion = CjoModuleVersion(
        CjoConstants.VERSION_MAJOR.toUByte(),
        CjoConstants.VERSION_MINOR.toUByte(),
        CjoConstants.VERSION_PATCH.toUByte(),
    )

    /** 官方 `OptimizationLevel` 的字符串→序号映射（O0..Oz，与 `OptimizationLevelToString` 逆序对位）。 */
    private val OPT_LEVEL_ORDINALS: Map<String, UByte> = mapOf(
        "O0" to 0u,
        "O1" to 1u,
        "O2" to 2u,
        "O3" to 3u,
        "Os" to 4u,
        "Oz" to 5u,
    )

    /** 优化级别字符串 → 官方序号；未知级别按 `O0` 处理（调用方负责合法性）。 */
    fun optLevelOrdinal(level: String): UByte = OPT_LEVEL_ORDINALS[level] ?: 0u

    /**
     * ① cjo 格式版本门。
     *
     * 官方判据：major 相等 ∧ minor ≤ 当前；**缺失版本同样拒绝**（官方 v1.0.0 教训：
     * 写版本不校验 = 无防御，缺版本静默消费更危险）。
     */
    fun checkFormatVersion(packageName: String, version: CjoModuleVersion?): CfirCjmpLoadDiagnostic? = when {
        version == null -> CfirCjmpLoadDiagnostic(
            kind = CfirCjmpLoadDiagnosticKind.CJO_VERSION,
            packageName = packageName,
            message = "cjo of '$packageName' has no format version; refuse to consume it",
        )

        version.major != supportedVersion.major || version.minor > supportedVersion.minor ->
            CfirCjmpLoadDiagnostic(
                kind = CfirCjmpLoadDiagnosticKind.CJO_VERSION,
                packageName = packageName,
                message = "cjo format version $version of '$packageName' is not supported " +
                    "(supported major ${supportedVersion.major}, minor <= ${supportedVersion.minor})",
            )

        else -> null
    }

    /** ② 包名一致门（官方 `module_common_cjo_wrong_package`）。 */
    fun checkPackageName(expected: String, actual: String): CfirCjmpLoadDiagnostic? =
        if (expected == actual) {
            null
        } else {
            CfirCjmpLoadDiagnostic(
                kind = CfirCjmpLoadDiagnosticKind.WRONG_PACKAGE,
                packageName = actual,
                message = "common cjo '$actual' does not match the expected package '$expected'",
            )
        }

    /**
     * ③ features 子集门：common part 文件的每个 feature 必须出现在 specific 侧 features 中
     *（官方 `feature_is_not_subset_of_child_set`）。
     */
    fun checkFeaturesSubset(
        packageName: String,
        commonFeatures: Collection<List<String>>,
        specificFeatures: Set<String>,
    ): CfirCjmpLoadDiagnostic? {
        val missing = commonFeatures
            .map { identifiers -> identifiers.joinToString(".") }
            .filter { feature -> feature.isNotEmpty() && feature !in specificFeatures }
        if (missing.isEmpty()) return null
        return CfirCjmpLoadDiagnostic(
            kind = CfirCjmpLoadDiagnosticKind.FEATURE_NOT_SUBSET,
            packageName = packageName,
            message = "feature from common part is not a subset of the child feature set: " +
                missing.joinToString(", ") { "'$it'" },
        )
    }

    /**
     * ④ 编译选项匹配门（官方 `module_common_cjo_no_options` / `_debug_mismatch` / `_opt_mismatch`）。
     *
     * 选项缺失只诊断不拒绝（官方 `ValidateOptions` 在 `options == nullptr` 时记诊断后继续）；
     * debug/优化级别不一致必须拒绝——否则后续 desugar/CHIR 差异会崩溃。
     */
    fun checkOptions(
        packageName: String,
        options: CjoModuleOptionInfo?,
        debug: Boolean,
        optLevel: UByte,
    ): List<CfirCjmpLoadDiagnostic> = buildList {
        if (options == null) {
            add(
                CfirCjmpLoadDiagnostic(
                    kind = CfirCjmpLoadDiagnosticKind.OPTIONS_MISMATCH,
                    packageName = packageName,
                    message = "common cjo '$packageName' has no embedded compile options",
                ),
            )
            return@buildList
        }
        if (options.debug != debug) {
            add(
                CfirCjmpLoadDiagnostic(
                    kind = CfirCjmpLoadDiagnosticKind.OPTIONS_MISMATCH,
                    packageName = packageName,
                    message = "common cjo debug option is ${if (options.debug) "enabled" else "disabled"} " +
                        "but the current compile is ${if (debug) "enabled" else "disabled"}",
                ),
            )
        }
        if (options.optLevel != optLevel) {
            add(
                CfirCjmpLoadDiagnostic(
                    kind = CfirCjmpLoadDiagnosticKind.OPTIONS_MISMATCH,
                    packageName = packageName,
                    message = "common cjo optimization level is ${options.optLevel} " +
                        "but the current compile is $optLevel",
                ),
            )
        }
    }
}
'''
robust_write(p, content)
print('3 load gate file')

# =====================================================================
# 4) CjoManager：版本门 + 包名门
# =====================================================================
p = R + '/cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoManager.kt'
t = read(p)
t = replace_one = replace_once(
    t,
    '''import PackageFormat.Package
import org.cangnova.cangjie.name.FqName''',
    '''import PackageFormat.Package
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnostic
import org.cangnova.cangjie.name.FqName''',
    'manager import')
t = replace_once(
    t,
    '''    private val snapshots = ConcurrentHashMap<String, CjoLoadedPackage>()
    private val missingPackages = ConcurrentHashMap.newKeySet<String>()''',
    '''    private val snapshots = ConcurrentHashMap<String, CjoLoadedPackage>()
    private val missingPackages = ConcurrentHashMap.newKeySet<String>()

    /**
     * 加载门诊断（版本 / 包名）按包名记录，供 session 层的收集器取走。
     *
     * 通道（计划 G17）：加载期没有 `DiagnosticReporter`，诊断随加载结果上浮，
     * 由 [org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider]
     * 转入 `CfirCjmpLoadDiagnosticsComponent`。
     */
    private val loadDiagnostics = ConcurrentHashMap<String, List<CfirCjmpLoadDiagnostic>>()

    /** 读取指定包的加载门诊断（无诊断时为空列表）。 */
    fun loadDiagnostics(fullPkgName: String): List<CfirCjmpLoadDiagnostic> =
        loadDiagnostics[fullPkgName].orEmpty()''',
    'manager diagnostics field')
t = replace_once(
    t,
    '''        return Snapshot(ByteBuffer.wrap(file.readBytes()), file.toPath().toAbsolutePath().normalize()).also {
            snapshots[fullPkgName] = it
        }''',
    '''        val snapshot = Snapshot(
            ByteBuffer.wrap(file.readBytes()),
            file.toPath().toAbsolutePath().normalize(),
        )
        // 加载门①版本 / ②包名：违反即拒绝装载（官方 PreloadCommonPartOfPackage 的 return false 对位），
        // 但诊断仍记录，供装配层外显——静默地把包当"不存在"会让调用方难以定位问题。
        val diagnostics = buildList {
            CjmpCommonPartLoadGate.checkFormatVersion(fullPkgName, snapshot.header.cjoVersion)?.let(::add)
            CjmpCommonPartLoadGate.checkPackageName(fullPkgName, snapshot.header.fullPkgName)?.let(::add)
        }
        if (diagnostics.isNotEmpty()) {
            loadDiagnostics[fullPkgName] = diagnostics
            missingPackages += fullPkgName
            return null
        }
        return snapshot.also {
            snapshots[fullPkgName] = it
        }''',
    'manager gates')
robust_write(p, t)
print('4 CjoManager gates')

# =====================================================================
# 5) provider：features / 选项门 + 诊断转交 session 收集器
# =====================================================================
p = R + '/cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/provider/CfirDeserializedSymbolProvider.kt'
t = read(p)
t = replace_once(
    t,
    '''import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager''',
    '''import org.cangnova.cangjie.cfir.serialization.cjo.CjmpCommonPartLoadGate
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader''',
    'provider imports')
t = replace_once(
    t,
    '''        return PackageDeserializers(header, context)
    }
}''',
    '''        recordLoadGateDiagnostics(header)
        return PackageDeserializers(header, context)
    }

    /**
     * 收集本包触发的 CJMP 加载门诊断（G17 通道）。
     *
     * - 版本门 / 包名门由 [cjoManager] 在快照装载时记录；
     * - features 子集门与编译选项门只在 **specific 模式加载带 CJMP 内容的 common part cjo** 时判定
     *  （官方 `PreloadCommonPartOfPackage` 只在 specific 编译加载 common part 时跑这些门）——
     *  普通库加载（含 stdlib）不做 features/选项比对。
     */
    private fun recordLoadGateDiagnostics(header: CjoPackageHeader) {
        val settings = session.cjmpSettings
        val collector = session.cjmpLoadDiagnostics
        cjoManager.loadDiagnostics(header.fullPkgName).forEach(collector::record)
        if (settings.mode != org.cangnova.cangjie.cfir.session.CfirCjmpMode.SPECIFIC) return
        if (!header.isCjmpCommonPart) return
        CjmpCommonPartLoadGate.checkFeaturesSubset(
            packageName = header.fullPkgName,
            commonFeatures = header.fileFeatures.values.flatten(),
            specificFeatures = settings.packageFeatures,
        )?.let(collector::record)
        CjmpCommonPartLoadGate.checkOptions(
            packageName = header.fullPkgName,
            options = header.options,
            debug = settings.moduleDebug,
            optLevel = CjmpCommonPartLoadGate.optLevelOrdinal(settings.moduleOptLevel),
        ).forEach(collector::record)
    }
}''',
    'provider gates')
t = replace_once(
    t,
    '''import org.cangnova.cangjie.cfir.session.CfirSession''',
    '''import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cjmpLoadDiagnostics
import org.cangnova.cangjie.cfir.session.cjmpSettings''',
    'provider session imports')
robust_write(p, t)
print('5 provider gates')

# =====================================================================
# 6) 组件 packageFeatures + 配置键
# =====================================================================
p = R + '/cfir/cfir-common/src/org/cangnova/cangjie/cfir/session/CfirCjmpSettings.kt'
t = read(p)
t = replace_once(
    t,
    ''' * @property moduleDebug 写出/加载 common part cjo 时的 debug 选项（官方 `Option::debug`）。''',
    ''' * @property packageFeatures 当前编译包的 features 集合（官方 `CollectFeaturesFromPackage`：
 *   来自源文件的 `features { ... }` 指令）；features 子集门用它比对 common part。
 * @property moduleDebug 写出/加载 common part cjo 时的 debug 选项（官方 `Option::debug`）。''',
    'component features kdoc')
t = replace_once(
    t,
    '''    /** 写出/加载 common part cjo 时的 debug 选项（官方 `Option::debug`）。 */
    val moduleDebug: Boolean = false,''',
    '''    /** 当前编译包的 features 集合（官方 `File::GetFeatures()` 的包级并集）。 */
    val packageFeatures: Set<String> = emptySet(),
    /** 写出/加载 common part cjo 时的 debug 选项（官方 `Option::debug`）。 */
    val moduleDebug: Boolean = false,''',
    'component features field')
robust_write(p, t)
print('6a component packageFeatures')

p = R + '/cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/configuration/CfirFrontendConfigurationKeys.kt'
t = read(p)
t = replace_once(
    t,
    '''    /** 写出 common part cjo 时内嵌的 debug 选项（官方 `Option::debug` 对位）。 */''',
    '''    /** 当前编译包的 features 集合（官方 `features { ... }` 指令的包级并集）。 */
    @JvmField
    val CJMP_PACKAGE_FEATURES =
        CompilerConfigurationKey.create<Set<String>>("CJMP_PACKAGE_FEATURES")

    /** 写出 common part cjo 时内嵌的 debug 选项（官方 `Option::debug` 对位）。 */''',
    'config features key')
t = t.rstrip('\n') + '''

/** 当前编译包的 features 集合（features 子集门的 specific 侧输入）。 */
var CompilerConfiguration.cjmpPackageFeatures: Set<String>
    get() = get(CfirFrontendConfigurationKeys.CJMP_PACKAGE_FEATURES) ?: emptySet()
    set(value) = put(CfirFrontendConfigurationKeys.CJMP_PACKAGE_FEATURES, value.toSet())
'''
robust_write(p, t)
print('6b config features extension')

p = R + '/cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirSessionFactoryContextUtils.kt'
t = read(p)
t = replace_once(
    t,
    '''import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpModuleDebug\n''',
    '''import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpModuleDebug\n'''
    '''import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpPackageFeatures\n''',
    'ctx imports')
t = replace_once(
    t,
    '''        moduleDebug = configuration.cjmpModuleDebug,''',
    '''        packageFeatures = configuration.cjmpPackageFeatures,
        moduleDebug = configuration.cjmpModuleDebug,''',
    'ctx wiring')
robust_write(p, t)
print('6c context utils features')

print('phase4_3b-part2 complete')
