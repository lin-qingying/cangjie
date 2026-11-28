package org.cangnova.cangjie.config

import java.nio.file.Path

/**
 * 编译器配置中的 package-path 输入，保留为独立角色供包发现、配置文件选择和哈希阶段消费。
 */
var CompilerConfiguration.packagePaths: List<Path>
    get() = get(CLIConfigurationKeys.PACKAGE_PATHS).orEmpty()
    set(value) {
        put(CLIConfigurationKeys.PACKAGE_PATHS, value.toList())
    }
