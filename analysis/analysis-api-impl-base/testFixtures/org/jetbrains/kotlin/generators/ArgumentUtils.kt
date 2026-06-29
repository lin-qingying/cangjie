

package org.jetbrains.kotlin.generators

/**
 * 判断命令行参数是否显式允许在 TeamCity 上写入生成文件。
 *
 * 该开关用于区分本地修复生成文件与 CI 上只报告生成结果不一致的场景。
 */
internal fun Array<String>.allowGenerationOnTeamCity(): Boolean {
    return any { it == "allowGenerationOnTeamCity" }
}

/**
 * 判断命令行参数是否要求跳过 `testAllFilesPresent` 检查方法生成。
 *
 * 该开关主要服务于临时迁移或特殊测试数据目录，避免生成文件覆盖不完整目录结构。
 */
internal fun Array<String>.skipTestAllFilesCheck(): Boolean {
    return any { it == "skipTestAllFilesCheck" }
}
