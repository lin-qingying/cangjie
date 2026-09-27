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


def replace_all(text, old, new, what, expect):
    c = text.count(old)
    assert c == expect, f"{what}: expected {expect} occurrences, found {c}"
    return text.replace(old, new)


# ---------------------------------------------------------------- P1 新组件
p_cjmp = R + '/cfir/cfir-common/src/org/cangnova/cangjie/cfir/session/CfirCjmpSettings.kt'
content = '''package org.cangnova.cangjie.cfir.session

/**
 * 当前编译调用的 CJMP（common/specific 跨平台声明族）编译模式。
 *
 * 判据对齐官方 `GlobalOptions` 的模式谓词：
 * - `IsCompilingCJMPSpecific() = commonPartChirs 非空` → [SPECIFIC]；
 * - `IsCompilingCJMP() = Specific || outputMode == CHIR` → [COMMON]（CHIR 输出模式即编译 common part）；
 * - 两个谓词皆假 → [NONE]（官方 `CheckCJMPModifiers` 在此报 `parse_unexpected_cjmp_decl`）。
 *
 * 模式是**编译调用**的事实，不是语言版本的事实：1.1+ 的语言表面在 mode=None 下依然不合法，
 * 只是诊断换成 `parse_unexpected_cjmp_decl`（计划 §8.1 三层门的第 ② 层）。
 */
public enum class CfirCjmpMode {
    /** 未开启 CJMP 编译（既非 common part 也非 specific part）。 */
    NONE,

    /** 编译 common part（CHIR 输出模式）。 */
    COMMON,

    /** 编译 specific part（已加载 common part 的 cjo/chir）。 */
    SPECIFIC,
}

/**
 * 源码 session 的 CJMP 编译模式与 common-part 输入。
 *
 * 与 [CfirInteropSettingsComponent] 同构：模式事实由 driver 选项进入 session，
 * checker 不得从源码形状反推。**per-session 语义**（D14）：多模块编译中每个模块 session
 * 持有自己的组件值——源模块由 driver options 决定，测试模块由 `// CJMP_MODE` 指令决定，
 * IDE 由模块 kind 决定（未落地前为 [CfirCjmpMode.NONE]）。
 *
 * @property commonPartCjoPaths common part `.cjo` 输入路径（官方 `--common-part-cjo`）。
 * @property commonPartChirPaths common part `.chir` 输入路径（官方 `--common-part-chir`）；
 *   非空即 specific 编译（官方 `IsCompilingCJMPSpecific()` 判据）。
 * @property isChirOutput 当前调用是否为 CHIR 输出模式（官方 `IsCompilingCJMP()` 的 Common 判据）。
 * @property explicitMode 显式模式覆盖（测试指令 / IDE 模块 kind）；为 null 时由前三项推导。
 */
public open class CfirCjmpSettingsComponent(
    /** 官方 `--common-part-cjo` 输入路径列表。 */
    val commonPartCjoPaths: List<String> = emptyList(),
    /** 官方 `--common-part-chir` 输入路径列表。 */
    val commonPartChirPaths: List<String> = emptyList(),
    /** 当前调用是否为 CHIR 输出模式。 */
    val isChirOutput: Boolean = false,
    /** 显式模式覆盖；null 表示按官方谓词推导。 */
    val explicitMode: CfirCjmpMode? = null,
) : CfirSessionComponent {
    /** 当前 session 的 CJMP 编译模式。 */
    public val mode: CfirCjmpMode
        get() = explicitMode ?: when {
            commonPartChirPaths.isNotEmpty() -> CfirCjmpMode.SPECIFIC
            isChirOutput -> CfirCjmpMode.COMMON
            else -> CfirCjmpMode.NONE
        }

    companion object {
        /**
         * 两个 common-part 选项的数量配对校验（官方
         * `driver_require_common_chir_for_each_common_cjo` 对位）。
         *
         * 官方在 driver 层拒绝"有 common cjo 却没有对应 chir"的调用；本仓库没有独立
         * driver 进程，校验落在配置 → session 组件的唯一转换点上。
         */
        public fun validateCommonPartInputs(
            commonPartCjoPaths: List<String>,
            commonPartChirPaths: List<String>,
        ) {
            require(commonPartCjoPaths.size == commonPartChirPaths.size) {
                "require common chir for each common cjo: ${commonPartCjoPaths.size} common-part " +
                    "cjo path(s) but ${commonPartChirPaths.size} common-part chir path(s)"
            }
        }
    }
}

/** 未配置 CJMP 编译时使用的不可变默认组件（mode = [CfirCjmpMode.NONE]）。 */
public object DefaultCfirCjmpSettingsComponent : CfirCjmpSettingsComponent()

/** 当前 session 的 CJMP 编译模式配置。 */
public val CfirSession.cjmpSettings: CfirCjmpSettingsComponent by
    CfirSession.sessionComponentAccessorWithDefault(DefaultCfirCjmpSettingsComponent)
'''
robust_write(p_cjmp, content)
print('P1 CfirCjmpSettings.kt created')

# ---------------------------------------------------------------- P2 配置键
p = R + '/cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/configuration/CfirFrontendConfigurationKeys.kt'
t = read(p)
t = replace_once(
    t,
    'import org.cangnova.cangjie.cfir.session.CfirInteropTarget\n',
    'import org.cangnova.cangjie.cfir.session.CfirInteropTarget\n'
    'import org.cangnova.cangjie.cfir.session.CfirCjmpMode\n',
    'P2 import')
t = replace_once(
    t,
    '''    /** 平台入口显式授权的隐式系统注解 ClassId 集合。 */
    @JvmField
    val IMPLICIT_SYSTEM_ANNOTATIONS =
        CompilerConfigurationKey.create<Set<FqName>>("IMPLICIT_SYSTEM_ANNOTATIONS")
}''',
    '''    /** 平台入口显式授权的隐式系统注解 ClassId 集合。 */
    @JvmField
    val IMPLICIT_SYSTEM_ANNOTATIONS =
        CompilerConfigurationKey.create<Set<FqName>>("IMPLICIT_SYSTEM_ANNOTATIONS")

    /** CJMP common-part `.cjo` 输入路径列表（官方 `--common-part-cjo` 对位）。 */
    @JvmField
    val CJMP_COMMON_PART_CJO_PATHS =
        CompilerConfigurationKey.create<List<String>>("CJMP_COMMON_PART_CJO_PATHS")

    /** CJMP common-part `.chir` 输入路径列表（官方 `--common-part-chir` 对位）。 */
    @JvmField
    val CJMP_COMMON_PART_CHIR_PATHS =
        CompilerConfigurationKey.create<List<String>>("CJMP_COMMON_PART_CHIR_PATHS")

    /** 当前调用是否为 CHIR 输出模式（官方 `outputMode == CHIR` 对位，即编译 common part）。 */
    @JvmField
    val CJMP_CHIR_OUTPUT =
        CompilerConfigurationKey.create<Boolean>("CJMP_CHIR_OUTPUT")

    /** 显式注入的 CJMP 编译模式（测试 `// CJMP_MODE` 与 IDE 模块 kind 用；不设则由选项推导）。 */
    @JvmField
    val CJMP_MODE =
        CompilerConfigurationKey.create<CfirCjmpMode>("CJMP_MODE")
}''',
    'P2 keys')
t = t.rstrip('\n') + '''

/** CJMP common-part `.cjo` 输入路径列表。 */
var CompilerConfiguration.cjmpCommonPartCjoPaths: List<String>
    get() = getList(CfirFrontendConfigurationKeys.CJMP_COMMON_PART_CJO_PATHS)
    set(value) = put(CfirFrontendConfigurationKeys.CJMP_COMMON_PART_CJO_PATHS, value.toList())

/** CJMP common-part `.chir` 输入路径列表。 */
var CompilerConfiguration.cjmpCommonPartChirPaths: List<String>
    get() = getList(CfirFrontendConfigurationKeys.CJMP_COMMON_PART_CHIR_PATHS)
    set(value) = put(CfirFrontendConfigurationKeys.CJMP_COMMON_PART_CHIR_PATHS, value.toList())

/** 当前调用是否为 CHIR 输出模式（编译 common part）。 */
var CompilerConfiguration.cjmpChirOutput: Boolean
    get() = getBoolean(CfirFrontendConfigurationKeys.CJMP_CHIR_OUTPUT)
    set(value) = put(CfirFrontendConfigurationKeys.CJMP_CHIR_OUTPUT, value)

/** 显式注入的 CJMP 编译模式；未设置时由 common-part 选项与输出模式推导。 */
var CompilerConfiguration.cjmpMode: CfirCjmpMode?
    get() = get(CfirFrontendConfigurationKeys.CJMP_MODE)
    set(value) {
        if (value == null) return
        put(CfirFrontendConfigurationKeys.CJMP_MODE, value)
    }
'''
robust_write(p, t)
print('P2 config keys + extensions applied')

# ---------------------------------------------------------------- P3 session factory
p = R + '/cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirDefaultSessionFactory.kt'
t = read(p)
t = replace_once(
    t,
    'import org.cangnova.cangjie.cfir.session.CfirInteropSettingsComponent\n',
    'import org.cangnova.cangjie.cfir.session.CfirInteropSettingsComponent\n'
    'import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent\n',
    'P3 import')
t = replace_once(
    t,
    '''        /** 当前 session 使用的互操作/CJMapping 配置。 */
         val interopSettings: CfirInteropSettingsComponent = CfirInteropSettingsComponent(),''',
    '''        /** 当前 session 使用的互操作/CJMapping 配置。 */
         val interopSettings: CfirInteropSettingsComponent = CfirInteropSettingsComponent(),
        /** 当前 session 使用的 CJMP 编译模式（D14：模式 = session 组件）。 */
        val cjmpSettings: CfirCjmpSettingsComponent = CfirCjmpSettingsComponent(),''',
    'P3 context field')
t = replace_all(
    t,
    '        register(CfirInteropSettingsComponent::class, c.interopSettings)\n',
    '        register(CfirInteropSettingsComponent::class, c.interopSettings)\n'
    '        register(CfirCjmpSettingsComponent::class, c.cjmpSettings)\n',
    'P3 registers', 2)
robust_write(p, t)
print('P3 factory wired')

# ---------------------------------------------------------------- P4 context utils
p = R + '/cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirSessionFactoryContextUtils.kt'
t = read(p)
t = replace_once(
    t,
    'import org.cangnova.cangjie.cfir.entrypoint.configuration.enableInteropCJMapping\n',
    'import org.cangnova.cangjie.cfir.entrypoint.configuration.enableInteropCJMapping\n'
    'import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartCjoPaths\n'
    'import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartChirPaths\n'
    'import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpChirOutput\n'
    'import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode\n',
    'P4 imports')
t = replace_once(
    t,
    'import org.cangnova.cangjie.cfir.session.CfirInteropSettingsComponent\n',
    'import org.cangnova.cangjie.cfir.session.CfirInteropSettingsComponent\n'
    'import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent\n',
    'P4 session import')
t = replace_once(
    t,
    '''        mockSettings = CfirMockSettingsComponent(''',
    '''        cjmpSettings = createCfirCjmpSettingsComponent(configuration),
        mockSettings = CfirMockSettingsComponent(''',
    'P4 wiring')
t = replace_once(
    t,
    '''/**
 * 从 frontend 配置恢复 API level/syscap 语义。''',
    '''/**
 * 从 frontend 配置构造 CJMP 编译模式组件（D14 四段式样板：配置键 → session 组件 → 门禁消费）。
 *
 * 判定与校验都收敛在这一处，保证生产 pipeline 与测试 facade 使用同一份模式事实：
 * - 两个 common-part 选项数量必须一一配对（官方
 *   `driver_require_common_chir_for_each_common_cjo` 对位）；
 * - 模式判据对齐官方：Specific ⇔ chir 输入非空，Common ⇔ CHIR 输出模式；
 * - 测试/IDE 可通过 `cjmpMode` 显式覆盖（`// CJMP_MODE` 指令 / 模块 kind）。
 */
fun createCfirCjmpSettingsComponent(configuration: CompilerConfiguration): CfirCjmpSettingsComponent {
    val commonPartCjoPaths = configuration.cjmpCommonPartCjoPaths
    val commonPartChirPaths = configuration.cjmpCommonPartChirPaths
    CfirCjmpSettingsComponent.validateCommonPartInputs(commonPartCjoPaths, commonPartChirPaths)
    return CfirCjmpSettingsComponent(
        commonPartCjoPaths = commonPartCjoPaths,
        commonPartChirPaths = commonPartChirPaths,
        isChirOutput = configuration.cjmpChirOutput,
        explicitMode = configuration.cjmpMode,
    )
}

/**
 * 从 frontend 配置恢复 API level/syscap 语义。''',
    'P4 factory fn')
robust_write(p, t)
print('P4 context utils wired')

# ---------------------------------------------------------------- P5 CjmpGate
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CjmpGate.kt'
t = read(p)
t = replace_once(
    t,
    '''import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.source.AbstractCjSourceElement''',
    '''import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.source.AbstractCjSourceElement''',
    'P5 imports')
t = replace_once(
    t,
    ''' * 2. **模式门**（`CjmpSettingsComponent.mode == None` 时使用修饰符报
 *    `parse_unexpected_cjmp_decl` 对位诊断）：driver 编译模式基建落地于 Phase 4，
 *    当前恒放行（[modeGatePasses] 桩）。''',
    ''' * 2. **模式门**（`CfirCjmpSettingsComponent.mode == None` 时使用修饰符报
 *    `parse_unexpected_cjmp_decl` 对位诊断）：模式经 session 组件
 *    [org.cangnova.cangjie.cfir.session.cjmpSettings] 进入（D14 四段式样板），
 *    判定与上报由 [requireModeSupport] 统一负责。''',
    'P5 kdoc')
t = replace_once(
    t,
    '''    /**
     * 版本门（非上报型）：仅返回判定结果，不产生诊断。
     *
     * 供"整族静默跳过"的既有检查器消费（它们本就不产出版本诊断；族级版本诊断由
     * `CfirCjmpParseRulesChecker` 按声明统一负责，避免重复上报）。
     */
    fun isEnabled(context: CheckerContext): Boolean =
        context.languageVersionSettings.supportsFeature(LanguageFeature.CommonSpecificDeclarations)''',
    '''    /**
     * 组合门（非上报型）：版本门 ∧ 模式门，仅返回判定结果，不产生诊断。
     *
     * 供"整族静默跳过"的既有检查器消费（它们本就不产出门诊断；族级门诊断由
     * `CfirCjmpParseRulesChecker`（版本门，按声明）与 `CfirCjmpFilePartChecker`
     *（模式错位，按文件）统一负责，避免重复上报）。1.0.x 或 mode=None 下返回 false。
     */
    fun isEnabled(context: CheckerContext): Boolean =
        context.languageVersionSettings.supportsFeature(LanguageFeature.CommonSpecificDeclarations) &&
            isModeEnabled(context)

    /** 模式门（非上报型）：当前 session 是否处于 CJMP 编译模式。 */
    fun isModeEnabled(context: CheckerContext): Boolean =
        context.session.cjmpSettings.mode != CfirCjmpMode.NONE''',
    'P5 isEnabled')
t = replace_once(
    t,
    '''    /**
     * 模式门（D10 判定序第 ② 层）：`mode == None` 时报 `parse_unexpected_cjmp_decl`
     * 对位诊断（[org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.PARSE_UNEXPECTED_CJMP_DECL]）。
     *
     * Phase 4 接线 `CjmpSettingsComponent` 之前恒返回 true（模式基建未落地）；
     * 接线时在此读取 `context.session.cjmpSettings.mode`，保持"门禁单入口"承诺。
     */
    fun modeGatePasses(context: CheckerContext): Boolean = true''',
    '''    /**
     * 模式门（D10 判定序第 ② 层）：mode=None 时报
     * [org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.PARSE_UNEXPECTED_CJMP_DECL]
     * 并返回 false（调用方据此整族短路）。
     *
     * 官方对位：`MPParserImpl::CheckCJMPModifiers` 在 `!CompilePlatform() && !CompileCommon()`
     * 时对携带修饰符的声明报 `parse_unexpected_cjmp_decl`。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun requireModeSupport(source: AbstractCjSourceElement?): Boolean {
        if (isModeEnabled(context)) return true
        reporter.reportOn(
            source = source,
            factory = CfirErrors.PARSE_UNEXPECTED_CJMP_DECL,
        )
        return false
    }''',
    'P5 mode gate')
robust_write(p, t)
print('P5 CjmpGate wired')

# ---------------------------------------------------------------- P6 文件级 part checker
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCjmpFilePartChecker.kt'
content = '''/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CjmpGate
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.cjmpSettings

/**
 * CJMP 文件级 part 规则检查器（计划 §8.1 修饰符 × 模式合法性矩阵的 file-part 两条）。
 *
 * 官方 1.1.3 语义：`common` 声明只能出现在 common part 源文件，`specific` 声明只能出现在
 * specific part 源文件——判据是编译模式（不是语言版本）。cjc 1.1.3 实测：
 * - common part 编译（CHIR 输出）中写 `specific` → `parse_specific_in_non_specific_file`；
 * - specific part 编译（`--common-part-chir` 非空）中写 `common` → `parse_common_in_non_common_file`
 *  （specific part 的 common 声明来自 common part cjo，不在源码里）。
 *
 * 多 parent 链中"中间层文件同时含两种声明"的场景由文件级 part 标记承载而非模式枚举
 *（计划 §8.1 尾注，首版 fixture 不覆盖），因此本检查器只做模式与修饰符的错位判定。
 *
 * 判定序（D10/D16）：版本门 → 模式门；mode=None 时由 [CfirCjmpParseRulesChecker] 逐声明报
 * `parse_unexpected_cjmp_decl`，本检查器不重复报告。两条诊断官方都是文件级的，这里按文件
 * 报告一次，锚点取文件内首个错位声明。
 */
object CfirCjmpFilePartChecker : CfirFileChecker() {
    /** 文件级 part 判定消费解析完成的声明状态。 */
    override val requiresImplementation: Boolean get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        // 版本门 ∧ 模式门：整族静默时不做任何判定
        if (!CjmpGate.isEnabled(context)) return
        when (context.session.cjmpSettings.mode) {
            CfirCjmpMode.COMMON -> {
                val offending = declaration.allDeclarations()
                    .firstOrNull { it.cjmpStatus()?.isSpecific == true } ?: return
                reporter.reportOn(
                    source = offending.source,
                    factory = CfirErrors.PARSE_SPECIFIC_IN_NON_SPECIFIC_FILE,
                )
            }

            CfirCjmpMode.SPECIFIC -> {
                val offending = declaration.allDeclarations()
                    .firstOrNull { it.cjmpStatus()?.isCommon == true } ?: return
                reporter.reportOn(
                    source = offending.source,
                    factory = CfirErrors.PARSE_COMMON_IN_NON_COMMON_FILE,
                )
            }

            CfirCjmpMode.NONE -> Unit
        }
    }

    /** 文件内全部声明（含成员，深度优先、保持源码顺序）。 */
    private fun CfirFile.allDeclarations(): Sequence<CfirDeclaration> =
        declarations.asSequence().flatMap { it.selfAndMembers() }

    /** 声明自身与其成员声明。 */
    private fun CfirDeclaration.selfAndMembers(): Sequence<CfirDeclaration> = sequence {
        yield(this@selfAndMembers)
        val members: List<CfirDeclaration> = when (val self = this@selfAndMembers) {
            is CfirClassLikeDeclaration -> self.declarations
            is CfirExtend -> self.declarations
            else -> emptyList()
        }
        for (member in members) {
            yieldAll(member.selfAndMembers())
        }
    }

    /** 可修饰声明的状态载体。 */
    private fun CfirDeclaration.cjmpStatus() = (this as? CfirMemberDeclaration)?.status
}
'''
robust_write(p, content)
print('P6 CfirCjmpFilePartChecker.kt created')

# ---------------------------------------------------------------- P7 注册
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CommonDeclarationCheckers.kt'
t = read(p)
t = replace_once(
    t,
    '''            CfirCommonPackageMainChecker,
            CfirMacroRedefinitionChecker,''',
    '''            CfirCommonPackageMainChecker,
            CfirCjmpFilePartChecker,
            CfirMacroRedefinitionChecker,''',
    'P7 register')
robust_write(p, t)
print('P7 file checker registered')

# ---------------------------------------------------------------- P8 parse rules checker 模式门
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCjmpParseRulesChecker.kt'
t = read(p)
t = replace_once(
    t,
    '''        // ② 模式门：Phase 4 接线前恒放行
        if (!CjmpGate.modeGatePasses(context)) return''',
    '''        // ② 模式门：mode=None 时只报 parse_unexpected_cjmp_decl 并整族短路
        //（官方 CheckCJMPModifiers：!CompilePlatform() && !CompileCommon() 即报）
        if (!CjmpGate.requireModeSupport(declaration.source)) return''',
    'P8 mode gate call')
robust_write(p, t)
print('P8 parse rules checker wired')

# ---------------------------------------------------------------- P9 arguments DSL
p = R + '/compiler/arguments/src/org/cangnova/cangjie/arguments/description/compilerArguments.kt'
t = read(p)
t = replace_once(
    t,
    '''            compilerArgument {
                name = "output-dir"
                compilerName = "outputDirectory"
                description = "Output directory".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }
        }''',
    '''            compilerArgument {
                name = "output-dir"
                compilerName = "outputDirectory"
                description = "Output directory".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                // 官方 `--common-part-cjo`：CJMP specific 编译加载的 common part cjo 列表。
                name = "Xcjmp-common-part"
                description = "Path list of common part .cjo inputs for CJMP specific compilation"
                    .asReleaseDependent()
                argumentType = StringArrayType()
                valueType = StringArrayType()
                delimiter = CangJieCompilerArgument.Delimiter.PathSeparator
                lifecycle(CangJieReleaseVersion.V_1_1_0)
            }

            compilerArgument {
                // 官方 `--common-part-chir`：与 common cjo 一一配对的 chir 输入列表。
                name = "Xcjmp-common-part-chir"
                description = "Path list of common part .chir inputs for CJMP specific compilation"
                    .asReleaseDependent()
                argumentType = StringArrayType()
                valueType = StringArrayType()
                delimiter = CangJieCompilerArgument.Delimiter.PathSeparator
                lifecycle(CangJieReleaseVersion.V_1_1_0)
            }
        }''',
    'P9 arguments')
robust_write(p, t)
print('P9 arguments DSL updated')

# ---------------------------------------------------------------- P10 测试指令
p = R + '/tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/directives/CfirDiagnosticsDirectives.kt'
t = read(p)
t = replace_once(
    t,
    'import org.cangnova.cangjie.cfir.session.CfirInteropTarget\n',
    'import org.cangnova.cangjie.cfir.session.CfirInteropTarget\n'
    'import org.cangnova.cangjie.cfir.session.CfirCjmpMode\n',
    'P10 import')
t = replace_once(
    t,
    '''    /**
     * 保存 `RENDER_DIAGNOSTIC_ARGUMENTS`，供测试指令在测试执行期间读取或传递。
     */
    val RENDER_DIAGNOSTIC_ARGUMENTS by directive(
        description = "Forces rendering diagnostic arguments in test metadata.",
    )
}''',
    '''    /**
     * 保存 `RENDER_DIAGNOSTIC_ARGUMENTS`，供测试指令在测试执行期间读取或传递。
     */
    val RENDER_DIAGNOSTIC_ARGUMENTS by directive(
        description = "Forces rendering diagnostic arguments in test metadata.",
    )

    /**
     * 注入 CJMP 编译模式，例如 `// CJMP_MODE: SPECIFIC`。
     *
     * 官方模式下模式来自 driver 选项；测试基建没有 driver，用该指令把同一份事实
     * （`CfirCjmpSettingsComponent.explicitMode`）注入 session，语义与 CLI 一致。
     */
    val CJMP_MODE by enumDirective<CfirCjmpMode>(
        description = "Selects the CJMP compilation mode (NONE/COMMON/SPECIFIC).",
        additionalParser = { value ->
            CfirCjmpMode.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        },
    )
}''',
    'P10 directive')
robust_write(p, t)
print('P10 CJMP_MODE directive added')

# ---------------------------------------------------------------- P11 测试配置映射
p = R + '/tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/services/EnvironmentConfigurator.kt'
t = read(p)
t = replace_once(
    t,
    '''        register(
            CfirDiagnosticsDirectives.TARGET_INTEROP_LANGUAGE,
            CfirFrontendConfigurationKeys.TARGET_INTEROP_LANGUAGE,
        )
    }''',
    '''        register(
            CfirDiagnosticsDirectives.TARGET_INTEROP_LANGUAGE,
            CfirFrontendConfigurationKeys.TARGET_INTEROP_LANGUAGE,
        )
        register(
            CfirDiagnosticsDirectives.CJMP_MODE,
            CfirFrontendConfigurationKeys.CJMP_MODE,
        )
    }''',
    'P11 register')
robust_write(p, t)
print('P11 test config wiring done')
print('phase4_1 script complete')
