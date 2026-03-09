package org.cangjie.cfir.extensions

import org.cangjie.cfir.session.CfirSessionComponent

/** 编译器扩展基接口 */
interface CfirExtension

/** 扩展点名称标识符 */
@JvmInline
value class CfirExtensionPointName(val name: String) {
    override fun toString(): String = name
}

/** 扩展注册器接口 */
interface CfirExtensionRegistrar {
    fun registerExtensions(service: CfirExtensionService)
}

/**
 * 扩展管理服务，作为 session 组件。
 */
class CfirExtensionService : CfirSessionComponent {

    private val extensions = mutableMapOf<CfirExtensionPointName, MutableList<CfirExtension>>()

    fun registerExtension(pointName: CfirExtensionPointName, extension: CfirExtension) {
        extensions.getOrPut(pointName) { mutableListOf() }.add(extension)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : CfirExtension> getExtensions(pointName: CfirExtensionPointName): List<T> =
        (extensions[pointName] as? List<T>) ?: emptyList()

    fun registerAll(registrars: List<CfirExtensionRegistrar>) {
        for (registrar in registrars) registrar.registerExtensions(this)
    }
}
