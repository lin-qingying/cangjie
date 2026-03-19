package org.cangnova.cangjie.extensions

import org.cangnova.cangjie.config.CompilerConfiguration

interface CompilerConfigurationExtension {
    companion object : ProjectExtensionDescriptor<CompilerConfigurationExtension>(
        "org.cangnova.cangjie.compilerConfigurationExtension",
        CompilerConfigurationExtension::class.java,
    )

    fun updateConfiguration(configuration: CompilerConfiguration)

    fun updateFileRegistry() {}
}
