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
                name = "api-version"
                description = "API version".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "verbose"
                description = "Enable verbose logging".asReleaseDependent()
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
                compilerName = "outputFile"
                description = "Product name or output directory".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }

            compilerArgument {
                name = "output-dir"
                compilerName = "outputDirectory"
                description = "Output directory".asReleaseDependent()
                argumentType = StringType(defaultValue = ReleaseDependent(null))
                valueType = StringType(defaultValue = ReleaseDependent(null))
                lifecycle(CangJieReleaseVersion.V_1_0_0)
            }
        }
    }
}
