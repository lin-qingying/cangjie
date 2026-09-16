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
                lifecycle(CangJieReleaseVersion.V_1_0_5)
            }

            compilerArgument {
                name = "verbose"
                description = "Enable verbose logging".asReleaseDependent()
                argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
                lifecycle(CangJieReleaseVersion.V_1_0_5)
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
                lifecycle(CangJieReleaseVersion.V_1_0_5)
            }
        }
    }
}
