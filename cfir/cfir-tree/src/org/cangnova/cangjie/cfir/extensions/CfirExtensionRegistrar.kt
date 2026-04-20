package org.cangnova.cangjie.cfir.extensions

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.extensions.ExtensionPointDescriptor
import org.cangnova.cangjie.name.Name
import kotlin.reflect.KClass

typealias AnnotationFqn = org.cangnova.cangjie.name.FqName

/**
 * 编译器扩展基类。
 *
 * 这里保持仓颉当前“扩展对象直接注册到 session 扩展服务”的语义，
 * 但把 low-level / predicate / plugin-annotation 所需的主干抽象补齐到 Kotlin FIR 对位结构。
 */
abstract class CfirExtension(
    val session: CfirSession,
) {
    abstract val name: CfirExtensionPointName

    abstract val extensionType: KClass<out CfirExtension>

    fun interface Factory<out P : CfirExtension> {
        fun create(session: CfirSession): P
    }

    open fun CfirDeclarationPredicateRegistrar.registerPredicates() {}
}

data class CfirExtensionPointName(val name: Name) {
    constructor(name: String) : this(Name.identifier(name))
}

abstract class CfirDeclarationPredicateRegistrar {
    abstract fun register(vararg predicates: org.cangnova.cangjie.cfir.extensions.predicate.AbstractPredicate<*>)
    abstract fun register(predicates: Collection<org.cangnova.cangjie.cfir.extensions.predicate.AbstractPredicate<*>>)
}

@RequiresOptIn
annotation class CfirExtensionApiInternals

@RequiresOptIn
annotation class PluginServicesInitialization

/**
 * 对位 Kotlin `FirExtensionRegistrarAdapter`。
 *
 * 这里只负责 IntelliJ 扩展点注册边界，不改变仓颉主干的扩展注册语义。
 */
abstract class CfirExtensionRegistrarAdapter {
    companion object : ExtensionPointDescriptor<CfirExtensionRegistrarAdapter>(
        name = "org.cangnova.cangjie.cfir.extensions.cfirExtensionRegistrar",
        extensionClass = CfirExtensionRegistrarAdapter::class.java,
    )
}

/**
 * 仓颉主干扩展注册器。
 *
 * 与 Kotlin 不同，这里仍保留 `registerExtensions(service)` 作为真实注册入口。
 */
abstract class CfirExtensionRegistrar : CfirExtensionRegistrarAdapter() {
    abstract fun registerExtensions(service: CfirExtensionService)
}

/**
 * 扩展管理服务，作为 session 组件。
 */
class CfirExtensionService : CfirSessionComponent {
    private val extensions = linkedMapOf<CfirExtensionPointName, MutableList<CfirExtension>>()

    fun registerExtension(pointName: CfirExtensionPointName, extension: CfirExtension) {
        extensions.getOrPut(pointName) { mutableListOf() }.add(extension)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : CfirExtension> getExtensions(pointName: CfirExtensionPointName): List<T> =
        extensions[pointName].orEmpty() as List<T>

    @PluginServicesInitialization
    fun getAllExtensions(): List<CfirExtension> {
        return extensions.values.flatten()
    }

    fun registerAll(registrars: List<CfirExtensionRegistrar>) {
        for (registrar in registrars) {
            registrar.registerExtensions(this)
        }
    }
}

private val CfirSession.extensionServiceOrNull: CfirExtensionService? by CfirSession.nullableSessionComponentAccessor()

val CfirSession.extensionService: CfirExtensionService
    get() = extensionServiceOrNull
        ?: error("Expected `${CfirExtensionService::class}` to be registered in current session.")
