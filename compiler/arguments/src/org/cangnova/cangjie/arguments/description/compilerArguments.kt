package org.cangnova.cangjie.arguments.description

import org.cangnova.cangjie.arguments.dsl.base.*
import org.cangnova.cangjie.arguments.dsl.types.*

/**
 * 仓颉编译器参数定义
 */
@OptIn(ExperimentalArgumentApi::class)
val cangjieCompilerArguments = compilerArguments {
    topLevel(CompilerArgumentsLevelNames.commonToolArguments) {
        subLevel(CompilerArgumentsLevelNames.commonCompilerArguments) {
            compilerArgument {
                name = "language-version"
                description = "Language version".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "verbose"
                shortName = "V"
                description = "Enable verbose".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                // Official v1.0.0 Options.inc already exposes --verbose.
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "report-perf"
                description = "Report performance statistics".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_5)
            }

            compilerArgument {
                name = "dump-perf"
                description = "Dump performance statistics to file".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_5)
            }

            compilerArgument {
                name = "d"
                // 显式指定生成属性名，避免生成 `var d`（可读性差且与配置侧命名不一致）
                compilerName = "compileCjd"
                description = "Compile declaration file(s) (.cj.d)".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                // 官方 v1.0.0 Options.inc 已声明 -d；1.0.5 不是该能力的引入版本。
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "output"
                shortName = "o"
                compilerName = "outputFile"
                description = "Specify product name or output directory when compiling a package".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "output-dir"
                compilerName = "outputDirectory"
                description = "Specify output directory (it affects '--output' option)".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            // ------------------------------------------------------------------
            // 输入与模块结构（官方 GLOBAL 组对位）
            // ------------------------------------------------------------------

            compilerArgument {
                name = "import-path"
                compilerName = "importPath"
                description = "Add .cjo search path".asReleaseDependent()
                argumentType = StringArrayType()
                valueType = StringArrayType()
                delimiter = CangJieCompilerArgument.Delimiter.PathSeparator
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "module-name"
                compilerName = "moduleName"
                description = "Tell compiler name of the module".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "package"
                shortName = "p"
                compilerName = "compilePackage"
                description = "Specify package directory to be compiled".asReleaseDependent()
                argumentType = StringArrayType()
                valueType = StringArrayType()
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "no-sub-pkg"
                compilerName = "noSubPackage"
                description = "The package doesn't have sub-packages".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "no-prelude"
                compilerName = "noPrelude"
                description =
                    "Not import cangjie std.core package by default, only applicable when compiling std.core".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            // ------------------------------------------------------------------
            // 条件编译（官方 --cfg 对位）
            // ------------------------------------------------------------------

            compilerArgument {
                name = "cfg"
                compilerName = "conditionalCompilationConfig"
                description = "User defined condition to compile".asReleaseDependent()
                argumentType = StringArrayType()
                valueType = StringArrayType()
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            // ------------------------------------------------------------------
            // 测试编译模式（官方 CJNATIVE GLOBAL 组对位）
            // ------------------------------------------------------------------

            compilerArgument {
                name = "test"
                compilerName = "compileTest"
                description = "Enable compile test".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "test-only"
                compilerName = "compileTestsOnly"
                description =
                    "Compile test files only (`<filename>_test.cj`), production parts should be added as a dependency separately".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "export-for-test"
                compilerName = "exportForTest"
                description =
                    "Export some additional kinds of declaration specifically for using them in tests".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "mock"
                compilerName = "mock"
                description =
                    "Specify whether mock features are enabled, or disabled, or a runtime exception is thrown when trying to use mock features".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            // ------------------------------------------------------------------
            // CHIR → JVM codegen（核心管线可选 CFIR2CHIR → CODEGEN 阶段）
            // ------------------------------------------------------------------

            compilerArgument {
                name = "emit-jvm"
                compilerName = "emitJvm"
                description = "Emit JVM class files through the CHIR backend after frontend analysis".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "jvm-output-dir"
                compilerName = "jvmOutputDirectory"
                description = "Directory for emitted JVM class files (implies --emit-jvm)".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            // ------------------------------------------------------------------
            // 宏编译（官方 GLOBAL/FRONTEND 组对位）
            // ------------------------------------------------------------------

            compilerArgument {
                name = "compile-macro"
                compilerName = "compileMacro"
                description = "Options to compile the macro define package".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "debug-macro"
                compilerName = "debugMacro"
                description = "Enable debug macro".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "parallel-macro-expansion"
                compilerName = "parallelMacroExpansion"
                description = "Enable parallel macro expansion".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "dump-macro"
                compilerName = "dumpMacro"
                description = "Dump tokens after macro expansion".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            // ------------------------------------------------------------------
            // 诊断控制（官方 GLOBAL 组对位）
            // ------------------------------------------------------------------

            compilerArgument {
                name = "diagnostic-format"
                compilerName = "diagnosticFormat"
                description = "Diagnostic format. Candidate modes: json, txt".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "warn-on"
                compilerName = "warnOn"
                description = "Report a specific group of warning".asReleaseDependent()
                argumentType = StringArrayType()
                valueType = StringArrayType()
                delimiter = CangJieCompilerArgument.Delimiter.Semicolon
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "warn-off"
                compilerName = "warnOff"
                description = "Suppress a specific group of warning".asReleaseDependent()
                argumentType = StringArrayType()
                valueType = StringArrayType()
                delimiter = CangJieCompilerArgument.Delimiter.Semicolon
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "error-count-limit"
                compilerName = "errorCountLimit"
                description =
                    "Emit specified <number> of errors only. Available options: all, <number> (8 by default)".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            // ------------------------------------------------------------------
            // dump/调试族（官方 FRONTEND 组对位）
            // ------------------------------------------------------------------

            compilerArgument {
                name = "dump-tokens"
                compilerName = "dumpTokens"
                description = "Dump tokens".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "dump-parse"
                compilerName = "dumpParse"
                description = "Dump parser tree".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "dump-ast"
                compilerName = "dumpAst"
                description = "Dump abstract syntax tree".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "typecheck"
                compilerName = "typecheck"
                description = "Parse and check type of input file(s)".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            // ------------------------------------------------------------------
            // CLI 与 CJMP（保留既有条目）
            // ------------------------------------------------------------------

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

            compilerArgument {
                // 官方 `--output-type=chir`（`IsCompilingCJMP()` 的 Common 判据）：把输入源码按 CJMP common part 编译。
                // 本仓库前端没有输出类型选项，故以独立开关表达该模式事实。
                name = "Xcjmp-compile-common"
                description = "Compile the sources as a CJMP common part (official --output-type=chir)"
                    .asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_1_0)
            }
        }
    }
}
