package org.cangnova.cangjie.cfir.extensions

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.extensions.ExtensionPointDescriptor
import org.cangnova.cangjie.name.Name
import kotlin.reflect.KClass

/**
 * 注解全限定名别名。
 */
typealias AnnotationFqn = org.cangnova.cangjie.name.FqName

/**
 * 编译器扩展基类。
 *
 * 这里保持仓颉当前“扩展对象直接注册到 session 扩展服务”的语义，
 * 但把 low-level / predicate / plugin-annotation 所需的主干抽象补齐到 Kotlin FIR 对位结构。
 */
abstract class CfirExtension(
    /**
     * 当前扩展绑定的 session。
     */
    val session: CfirSession,
) {
    /**
     * 扩展点名称。
     */
    abstract val name: CfirExtensionPointName

    /**
     * 扩展实现类型。
     */
    abstract val extensionType: KClass<out CfirExtension>

    /**
     * 扩展实例工厂。
     */
    fun interface Factory<out P : CfirExtension> {
        /**
         * 为指定 session 创建扩展实例。
         */
        fun create(session: CfirSession): P
    }

    /**
     * 注册当前扩展需要的声明谓词。
     */
    open fun CfirDeclarationPredicateRegistrar.registerPredicates() {}
}

/**
 * CFIR 扩展点名称。
 *
 * @property name 扩展点短名称。
 */
data class CfirExtensionPointName(val name: Name) {
    /**
     * 通过字符串构造扩展点名称。
     */
    constructor(name: String) : this(Name.identifier(name))
}

/**
 * 声明谓词注册器。
 */
abstract class CfirDeclarationPredicateRegistrar {
    /**
     * 注册若干谓词。
     */
    abstract fun register(vararg predicates: org.cangnova.cangjie.cfir.extensions.predicate.AbstractPredicate<*>)

    /**
     * 注册谓词集合。
     */
    abstract fun register(predicates: Collection<org.cangnova.cangjie.cfir.extensions.predicate.AbstractPredicate<*>>)
}

/**
 * CFIR 扩展内部 API 标记。
 */
@RequiresOptIn
annotation class CfirExtensionApiInternals

/**
 * 插件服务初始化阶段 API 标记。
 */
@RequiresOptIn
annotation class PluginServicesInitialization

/**
 * 对位 Kotlin `FirExtensionRegistrarAdapter`。
 *
 * 这里只负责 IntelliJ 扩展点注册边界，不改变仓颉主干的扩展注册语义。
 */
abstract class CfirExtensionRegistrarAdapter {
    /**
     * IntelliJ 扩展点描述符。
     */
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
    /**
     * 向 [service] 注册扩展实例。
     */
    abstract fun registerExtensions(service: CfirExtensionService)
}

/**
 * 扩展管理服务，作为 session 组件。
 */
class CfirExtensionService : CfirSessionComponent {
    /**
     * 按扩展点名称分组的扩展实例。
     */
    private val extensions = linkedMapOf<CfirExtensionPointName, MutableList<CfirExtension>>()

    /**
     * 注册单个扩展实例。
     */
    fun registerExtension(pointName: CfirExtensionPointName, extension: CfirExtension) {
        extensions.getOrPut(pointName) { mutableListOf() }.add(extension)
    }

    /**
     * 获取指定扩展点下的扩展实例列表。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : CfirExtension> getExtensions(pointName: CfirExtensionPointName): List<T> =
        extensions[pointName].orEmpty() as List<T>

    /**
     * 获取当前 session 中所有扩展实例。
     */
    @PluginServicesInitialization
    fun getAllExtensions(): List<CfirExtension> {
        return extensions.values.flatten()
    }

    /**
     * 执行所有扩展注册器。
     */
    fun registerAll(registrars: List<CfirExtensionRegistrar>) {
        for (registrar in registrars) {
            registrar.registerExtensions(this)
        }
    }
}

/**
 * 当前 session 中可选的扩展服务组件。
 */
private val CfirSession.extensionServiceOrNull: CfirExtensionService? by CfirSession.nullableSessionComponentAccessor()

/**
 * 当前 session 中的扩展服务组件。
 */
val CfirSession.extensionService: CfirExtensionService
    get() = extensionServiceOrNull
        ?: error("Expected `${CfirExtensionService::class}` to be registered in current session.")
