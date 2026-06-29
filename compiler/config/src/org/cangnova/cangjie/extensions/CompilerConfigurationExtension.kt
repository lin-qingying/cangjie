package org.cangnova.cangjie.extensions

import org.cangnova.cangjie.config.CompilerConfiguration

/**
 * 允许项目级插件在编译开始前调整 `CompilerConfiguration` 的扩展接口。
 */
interface CompilerConfigurationExtension {
    /**
     * 项目级编译配置扩展点描述符。
     */
    companion object : ProjectExtensionDescriptor<CompilerConfigurationExtension>(
        "org.cangnova.cangjie.compilerConfigurationExtension",
        CompilerConfigurationExtension::class.java,
    )

    /**
     * 根据扩展自身配置修改编译器配置。
     */
    fun updateConfiguration(configuration: CompilerConfiguration)

    /**
     * 更新文件注册表相关状态；不需要文件级注册的扩展可保持默认空实现。
     */
    fun updateFileRegistry() {}
}
