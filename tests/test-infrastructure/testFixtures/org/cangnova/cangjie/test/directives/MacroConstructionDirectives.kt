package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * 单条 macro 定义声明（test-only DTO），由 [MacroConstructionDirectives.MACRO_DEFINITION]
 * 解析得到，由 cfir/analysis-tests 侧的 `MacroConstructionEnvironmentConfigurator`
 * 转成 `MacroDefinitionEntry` 注入 macro construction step。
 *
 * 解析格式（每行一条）：
 *
 * ```
 * // MACRO_DEFINITION: <fq-name>[, source=<Source>][, libPath=<path>]
 *                     [, supportsForcedKind=true|false][, supportsPlainAttrOverload=true|false]
 *                     [, expand=<text>]
 * ```
 *
 * - `source` 默认为 `MACRO_ARTIFACT`。其他可选值与
 *   `MacroDefinitionEntry.Source` 枚举对齐（`SOURCE_PACKAGE` / `LIBRARY` /
 *   `SHARED_BUILTIN` / `MACRO_ARTIFACT` / `BUILTIN_MACRO`）。
 * - `expand=<text>` 在 stub executor 下用作该宏的固定展开文本，方便 testdata
 *   控制宏展开成功路径；不指定时默认走 stub 的默认 `MacroExpansionResult.Failure`。
 */
data class MacroDefinitionSpec(
    val fqName: String,
    val source: String = "MACRO_ARTIFACT",
    val libPath: String? = null,
    val supportsForcedKind: Boolean = false,
    val supportsPlainAttrOverload: Boolean = false,
    val expand: String? = null,
) {
    companion object {
        fun parse(raw: String): MacroDefinitionSpec? {
            val parts = raw.split(",").map(String::trim).filter(String::isNotEmpty)
            if (parts.isEmpty()) return null
            val fqName = parts.first()
            if (fqName.contains('=') || fqName.isEmpty()) return null
            var source = "MACRO_ARTIFACT"
            var libPath: String? = null
            var supportsForcedKind = false
            var supportsPlainAttrOverload = false
            var expand: String? = null
            for (entry in parts.drop(1)) {
                val eq = entry.indexOf('=')
                if (eq < 0) return null
                val key = entry.substring(0, eq).trim()
                val value = entry.substring(eq + 1).trim()
                when (key) {
                    "source" -> source = value
                    "libPath" -> libPath = value.takeIf(String::isNotBlank)
                    "supportsForcedKind" -> supportsForcedKind = value.equals("true", ignoreCase = true)
                    "supportsPlainAttrOverload" -> supportsPlainAttrOverload = value.equals("true", ignoreCase = true)
                    "expand" -> expand = value
                    else -> return null
                }
            }
            return MacroDefinitionSpec(
                fqName = fqName,
                source = source,
                libPath = libPath,
                supportsForcedKind = supportsForcedKind,
                supportsPlainAttrOverload = supportsPlainAttrOverload,
                expand = expand,
            )
        }
    }
}

/**
 * 端到端 macro artifact 测试声明。
 *
 * 解析格式：
 *
 * ```
 * // MACRO_ARTIFACT_PACKAGE: <package>, declarations=A|B[, origin=SDK_STDLIB|EXTERNAL_PATH|ORCHESTRATION]
 *                          [, expands=A=text|B=text]
 * // MACRO_SOURCE_PACKAGE: <package>[, declarations=A|B][, expands=A=text]
 * ```
 *
 * `MACRO_ARTIFACT_PACKAGE` 直接向 `CompilerConfiguration.macroArtifactPackages`
 * 注入已编译 `.cjo + 动态库`；`MACRO_SOURCE_PACKAGE` 先注入
 * `macroSourcePackageCompilationRequests`，再由 test-only orchestrator 返回同样的
 * artifact。省略 `declarations` 时，cfir/analysis-tests 会从同一 testdata 的
 * `macro package` 源文件中抽取 `public macro` 声明，用于验证
 * “宏展开需求 -> 宏包编译 -> artifact resolver -> expand”链路。
 */
data class MacroArtifactPackageSpec(
    val packageFqName: String,
    val declarations: List<String>,
    val expands: Map<String, String> = emptyMap(),
    val origin: String = "EXTERNAL_PATH",
) {
    companion object {
        fun parse(raw: String): MacroArtifactPackageSpec? {
            val parts = raw.split(",").map(String::trim).filter(String::isNotEmpty)
            if (parts.isEmpty()) return null
            val packageFqName = parts.first()
            if (packageFqName.contains('=') || packageFqName.isEmpty()) return null
            var declarations: List<String> = emptyList()
            var expands: Map<String, String> = emptyMap()
            var origin = "EXTERNAL_PATH"
            for (entry in parts.drop(1)) {
                val eq = entry.indexOf('=')
                if (eq < 0) return null
                val key = entry.substring(0, eq).trim()
                val value = entry.substring(eq + 1).trim()
                when (key) {
                    "declarations" -> declarations = value.split("|").map(String::trim).filter(String::isNotEmpty)
                    "expands" -> expands = parseExpansions(value) ?: return null
                    "origin" -> origin = value
                    else -> return null
                }
            }
            return MacroArtifactPackageSpec(
                packageFqName = packageFqName,
                declarations = declarations,
                expands = expands,
                origin = origin,
            )
        }

        private fun parseExpansions(raw: String): Map<String, String>? {
            if (raw.isBlank()) return emptyMap()
            val result = linkedMapOf<String, String>()
            for (entry in raw.split("|").map(String::trim).filter(String::isNotEmpty)) {
                val eq = entry.indexOf('=')
                if (eq <= 0) return null
                val name = entry.substring(0, eq).trim()
                val text = entry.substring(eq + 1)
                if (name.isEmpty()) return null
                result[name] = text
            }
            return result
        }
    }
}

/**
 * Macro construction step 测试 directive 容器（baseline 第 11 节）。
 *
 * 这些 directive 控制 `.cj` testdata 在 macro construction step 阶段的行为：
 *
 * - [MACRO_EXECUTOR] 指定 macro executor 实现：
 *     * `none`  —— 不注入 executor；CLI strict 模式应当产 `MACRO_EXECUTOR_UNAVAILABLE`。
 *     * `stub`  —— 注入 `:macro:macro-stub` 的桩 executor；多数 IDE / analysis 测试默认值。
 *     * `real`  —— 调用 `:macro:macro-process` 的真实外部进程；CI 上才使用。
 *
 * - [EXPECT_DEGRADED] 标记本 testdata 期望进入 `MacroConstructionResult.Degraded`：
 *     * `true`  —— 应产 typed error placeholder + `MACRO_NOT_EXPANDED` /
 *                  `MACRO_EXPANSION_FAILED` 诊断，且 ordinary resolve 仍运行。
 *     * `false` —— 默认值；STRICT 模式，未展开即失败。
 *
 * - [MACRO_DEFINITION] 声明 macro 定义入口，注入 macro construction symbol index。
 *   每行一条 [MacroDefinitionSpec]，由 testdata 自描述宏来源、libPath、支持的调用形态等。
 *   详细字段见 [MacroDefinitionSpec.parse]。
 *
 * Baseline 第 11 节中提到的 cache key 与 ABI 入口由
 * `CompilerConfiguration` 控制，不在此处暴露 directive。
 */
object MacroConstructionDirectives : SimpleDirectivesContainer() {

    val MACRO_EXECUTOR by enumDirective<MacroExecutorMode>(
        description = """
            Macro executor 实现选择：none / stub / real
            参考 baseline 第 11 节 "测试 directive"。
        """.trimIndent()
    )

    val EXPECT_DEGRADED by directive(
        description = """
            标记本 testdata 期望进入 MacroConstructionResult.Degraded
            （typed error placeholder + MACRO_NOT_EXPANDED 等诊断）。
            参考 baseline 第 11 节 "测试 directive"。
        """.trimIndent()
    )

    val MACRO_DEFINITION by valueDirective<MacroDefinitionSpec>(
        description = """
            声明一条 macro 定义入口（fqName + source + libPath + ...）。
            每行一条，由 cfir/analysis-tests 侧 configurator 转换成
            MacroDefinitionEntry 注入 macro construction symbol index。
        """.trimIndent(),
        parser = MacroDefinitionSpec::parse,
    )

    val MACRO_ARTIFACT_PACKAGE by valueDirective<MacroArtifactPackageSpec>(
        description = """
            声明一包已编译 macro artifact，由 cfir/analysis-tests 侧生成临时
            .cjo + 动态库并注入 CompilerConfiguration.macroArtifactPackages。
        """.trimIndent(),
        parser = MacroArtifactPackageSpec::parse,
    )

    val MACRO_SOURCE_PACKAGE by valueDirective<MacroArtifactPackageSpec>(
        description = """
            声明一包同项目 macro source package，由 cfir/analysis-tests 侧注入
            macroSourcePackageCompilationRequests，并通过 test-only orchestrator 产出 artifact。
        """.trimIndent(),
        parser = MacroArtifactPackageSpec::parse,
    )

    enum class MacroExecutorMode { none, stub, real }
}
