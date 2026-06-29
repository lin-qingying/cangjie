package org.cangnova.cangjie.compiler.plugin

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CompilerConfigurationKey
import org.cangnova.cangjie.extensions.ExtensionPointDescriptor

/**
 * 编译器配置中的插件扩展实例存储。
 */
class ExtensionStorage {
    /**
     * 按扩展点描述符保存的扩展实例列表，保持注册顺序。
     */
    private val registeredExtensions = linkedMapOf<ExtensionPointDescriptor<*>, MutableList<Any>>()

    /**
     * 读取指定扩展点下已经注册的扩展实例。
     */
    operator fun <T : Any> get(descriptor: ExtensionPointDescriptor<T>): List<T> {
        @Suppress("UNCHECKED_CAST")
        return registeredExtensions[descriptor]?.toList() as? List<T> ?: emptyList()
    }

    /**
     * 向指定扩展点追加一个编译器插件扩展实例。
     */
    fun <T : Any> registerExtension(descriptor: ExtensionPointDescriptor<T>, extension: T) {
        @Suppress("UNCHECKED_CAST")
        val list = registeredExtensions.getOrPut(descriptor) { mutableListOf() } as MutableList<T>
        list += extension
    }
}

/**
 * 编译器插件相关的配置键集合。
 */
private object CompilerPluginConfigurationKeys {
    /**
     * 保存编译器插件扩展实例存储的配置键。
     */
    val EXTENSIONS_STORAGE = CompilerConfigurationKey.create<ExtensionStorage>("EXTENSIONS_STORAGE")
}

/**
 * 当前编译配置关联的插件扩展存储。
 */
var CompilerConfiguration.extensionsStorage: ExtensionStorage?
    get() = get(CompilerPluginConfigurationKeys.EXTENSIONS_STORAGE)
    set(value) {
        putIfNotNull(CompilerPluginConfigurationKeys.EXTENSIONS_STORAGE, value)
    }

/**
 * 读取指定扩展点在当前编译配置中注册的所有扩展实例。
 */
fun <T : Any> CompilerConfiguration.getCompilerExtensions(descriptor: ExtensionPointDescriptor<T>): List<T> {
    val extensionStorage = extensionsStorage ?: return emptyList()
    return extensionStorage[descriptor]
}

/**
 * 在当前编译配置中注册指定扩展点的扩展实例。
 */
fun <T : Any> CompilerConfiguration.registerCompilerExtension(
    descriptor: ExtensionPointDescriptor<T>,
    extension: T,
) {
    val extensionStorage = extensionsStorage ?: ExtensionStorage().also { extensionsStorage = it }
    extensionStorage.registerExtension(descriptor, extension)
}
