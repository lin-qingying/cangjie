package org.cangnova.cangjie.config

import org.cangnova.cangjie.cfir.diagnostics.impl.BaseDiagnosticsCollector
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.compiler.plugin.ExtensionStorage
import org.cangnova.cangjie.compiler.plugin.extensionsStorage
import org.cangnova.cangjie.messages.MessageCollector
import java.util.Collections

/**
 * 编译配置容器。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.config.CompilerConfiguration`。
 */
class CompilerConfiguration {
    companion object {
        /** 将集合类型包装为不可变视图，其它类型原样返回。 */
        private fun <T> T.unmodifiable(): T {
            @Suppress("UNCHECKED_CAST")
            return when (this) {
                is List<*> -> Collections.unmodifiableList(this)
                is Map<*, *> -> Collections.unmodifiableMap(this)
                is Set<*> -> Collections.unmodifiableSet(this)
                is Collection<*> -> Collections.unmodifiableCollection(this)
                else -> this
            } as T
        }
    }

    /** 内部构造器提示：优先通过上层工厂创建配置。 */
    @Internals("Prefer to create CompilerConfiguration through dedicated configuration phases.")
    constructor()

    /** 底层键值存储。 */
    private val map: MutableMap<com.intellij.openapi.util.Key<*>, Any> = LinkedHashMap()

    /** 只读标记。 */
    var isReadOnly: Boolean = false

    /** 读取可空配置值。 */
    operator fun <T : Any> get(key: CompilerConfigurationKey<T>): T? {
        return getValue(key)?.unmodifiable()
    }

    /** 读取配置值，缺失时返回默认值。 */
    operator fun <T : Any> get(key: CompilerConfigurationKey<T>, defaultValue: T): T {
        return getValue(key) ?: defaultValue
    }

    /** 读取配置值，缺失时由回调提供默认值。 */
    fun <T : Any> getOrDefault(key: CompilerConfigurationKey<T>, defaultValue: () -> T): T {
        return getValue(key) ?: defaultValue()
    }

    /** 读取非空配置值，缺失时报错。 */
    fun <T : Any> getNotNull(key: CompilerConfigurationKey<T>): T {
        return getValue(key) ?: error("No value for configuration key: $key")
    }

    /** 读取布尔配置，缺省为 `false`。 */
    fun getBoolean(key: CompilerConfigurationKey<Boolean>): Boolean {
        return get(key, defaultValue = false)
    }

    /** 读取列表配置，缺省为空列表。 */
    fun <T> getList(key: CompilerConfigurationKey<List<T>>): List<T> {
        return get(key, defaultValue = emptyList())
    }

    /** 读取映射配置，缺省为空映射。 */
    fun <K, V> getMap(key: CompilerConfigurationKey<Map<K, V>>): Map<K, V> {
        return get(key, defaultValue = emptyMap())
    }

    /** 写入配置值。 */
    fun <T : Any> put(key: CompilerConfigurationKey<T>, value: T) {
        checkReadOnly()
        map[key.ideaKey] = value
    }

    /** 缺失时写入配置值，返回最终值。 */
    fun <T : Any> putIfAbsent(key: CompilerConfigurationKey<T>, value: T): T {
        getValue(key)?.let { return it }
        checkReadOnly()
        put(key, value)
        return value
    }

    /** 非空时写入配置值。 */
    fun <T : Any> putIfNotNull(key: CompilerConfigurationKey<T>, value: T?) {
        if (value != null) {
            put(key, value)
        }
    }

    /** 向列表配置追加单项。 */
    fun <T> add(key: CompilerConfigurationKey<List<T>>, value: T) {
        checkReadOnly()

        @Suppress("UNCHECKED_CAST")
        val list = map.getOrPut(key.ideaKey) { mutableListOf<T>() } as MutableList<T>
        list += value
    }

    /** 向映射配置写入单项。 */
    fun <K, V> put(configurationKey: CompilerConfigurationKey<Map<K, V>>, key: K, value: V) {
        checkReadOnly()

        @Suppress("UNCHECKED_CAST")
        val map = map.getOrPut(configurationKey.ideaKey) { mutableMapOf<K, V>() } as MutableMap<K, V>
        map[key] = value
    }

    /** 向列表配置追加集合。 */
    fun <T : Any> addAll(key: CompilerConfigurationKey<List<T>>, values: Collection<T>?) {
        if (values != null) {
            addAll(key, getList(key).size, values)
        }
    }

    /** 向列表配置指定位置插入集合。 */
    fun <T : Any> addAll(key: CompilerConfigurationKey<List<T>>, index: Int, values: Collection<T>) {
        checkReadOnly()

        @Suppress("UNCHECKED_CAST")
        val list = map.getOrPut(key.ideaKey) { mutableListOf<T>() } as MutableList<T>
        list.addAll(index, values)
    }

    /** 复制配置。 */
    fun copy(): CompilerConfiguration {
        @OptIn(Internals::class)
        return CompilerConfiguration().also { it.map.putAll(map) }
    }

    /** 读取底层值。 */
    private fun <T : Any> getValue(key: CompilerConfigurationKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return map[key.ideaKey] as T?
    }

    /**
     * 将当前配置中的所有键值渲染为便于调试的多行文本。
     */
    override fun toString(): String {
        return buildString {
            for ((key, value) in map) {
                append(key).append(":")
                when (value) {
                    is Collection<*> -> {
                        appendLine()
                        for (v in value) {
                            append("  ").appendLine(v)
                        }
                    }

                    is Map<*, *> -> {
                        appendLine()
                        for ((k, v) in value) {
                            append("  ").append(k).append("=").appendLine(v)
                        }
                    }

                    else -> append(" ").appendLine(value)
                }
            }
        }.trim()
    }

    /** 只读保护。 */
    private fun checkReadOnly() {
        check(!isReadOnly) { "CompilerConfiguration is read-only" }
    }

    /** 内部 API 标记。 */
    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    annotation class Internals(val message: String)
}

/**
 * 创建 CLI/standalone 编译流程使用的默认编译配置。
 */
fun CompilerConfiguration.Companion.create(
    diagnosticsCollector: BaseDiagnosticsCollector? = null,
    messageCollector: MessageCollector? = null,
): CompilerConfiguration {
    @OptIn(CompilerConfiguration.Internals::class)
    return CompilerConfiguration().apply {
        initializeDiagnosticFactoriesStorageForCli()
        this.diagnosticsCollector = diagnosticsCollector ?: DiagnosticsCollectorImpl()
        this.extensionsStorage = ExtensionStorage()
        messageCollector?.let { this.messageCollector = it }
    }
}

/**
 * TODO: 实现 CLI 诊断工厂存储的初始化
 * 当前此方法为空，诊断工厂注册逻辑尚未接入。
 * 应完成以下步骤：
 * 1. 创建 [CjRegisteredDiagnosticFactoriesStorage] 实例
 * 2. 调用 storage.registerDiagnosticContainers(CliDiagnostics) 注册 CLI 专属诊断（如参数错误、插件错误等）
 * 3. 将 storage 写入 this.diagnosticFactoriesStorage
 * 待 CliDiagnostics 和 diagnosticFactoriesStorage 机制实现后解除注释。
 */
private fun CompilerConfiguration.initializeDiagnosticFactoriesStorageForCli() {
    // TODO: 实现 CLI 诊断工厂存储初始化，参考 Kotlin 的 CliDiagnostics 注册方式
    // val storage = CjRegisteredDiagnosticFactoriesStorage()
    // storage.registerDiagnosticContainers(CliDiagnostics)
    // this.diagnosticFactoriesStorage = storage
}
