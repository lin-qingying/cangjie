package org.cangnova.cangjie.compiler.plugin

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CompilerConfigurationKey
import org.cangnova.cangjie.extensions.ExtensionPointDescriptor

class ExtensionStorage {
    private val registeredExtensions = linkedMapOf<ExtensionPointDescriptor<*>, MutableList<Any>>()

    operator fun <T : Any> get(descriptor: ExtensionPointDescriptor<T>): List<T> {
        @Suppress("UNCHECKED_CAST")
        return registeredExtensions[descriptor]?.toList() as? List<T> ?: emptyList()
    }

    fun <T : Any> registerExtension(descriptor: ExtensionPointDescriptor<T>, extension: T) {
        @Suppress("UNCHECKED_CAST")
        val list = registeredExtensions.getOrPut(descriptor) { mutableListOf() } as MutableList<T>
        list += extension
    }
}

private object CompilerPluginConfigurationKeys {
    val EXTENSIONS_STORAGE = CompilerConfigurationKey.create<ExtensionStorage>("EXTENSIONS_STORAGE")
}

var CompilerConfiguration.extensionsStorage: ExtensionStorage?
    get() = get(CompilerPluginConfigurationKeys.EXTENSIONS_STORAGE)
    set(value) {
        putIfNotNull(CompilerPluginConfigurationKeys.EXTENSIONS_STORAGE, value)
    }

fun <T : Any> CompilerConfiguration.getCompilerExtensions(descriptor: ExtensionPointDescriptor<T>): List<T> {
    val extensionStorage = extensionsStorage ?: return emptyList()
    return extensionStorage[descriptor]
}

fun <T : Any> CompilerConfiguration.registerCompilerExtension(
    descriptor: ExtensionPointDescriptor<T>,
    extension: T,
) {
    val extensionStorage = extensionsStorage ?: ExtensionStorage().also { extensionsStorage = it }
    extensionStorage.registerExtension(descriptor, extension)
}
