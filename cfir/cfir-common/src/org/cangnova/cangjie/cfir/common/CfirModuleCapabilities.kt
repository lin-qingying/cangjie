package org.cangnova.cangjie.cfir.common

import org.cangnova.cangjie.util.ComponentArrayOwner
import org.cangnova.cangjie.util.TypeRegistry
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * 模块级能力的基类。
 *
 * capability 通过 [key] 声明自身在 [CfirModuleCapabilities] 中的唯一注册键，
 * 用于为模块数据附加可选行为或元信息，而不扩展 [CfirModuleData] 的核心抽象。
 */
abstract class CfirModuleCapability {
    /**
     * 当前能力在模块能力表中的类型键。
     */
    abstract val key: KClass<out CfirModuleCapability>
}

/**
 * 模块能力集合。
 *
 * 该集合以 [ComponentArrayOwner] 存储能力实例，读取侧通过 [TypeRegistry] 生成的
 * 下标访问具体 capability，避免在模块解析热路径中遍历列表或依赖字符串映射。
 */
class CfirModuleCapabilities private constructor(
    capabilities: List<CfirModuleCapability>,
) : ComponentArrayOwner<CfirModuleCapability, CfirModuleCapability>() {

    /**
     * [CfirModuleCapability] 的全局注册表。
     *
     * 能力键按类名分配稳定下标；由于能力集合会在不同模块数据之间共享，
     * 这里使用同步的惰性计算避免并发创建重复下标。
     */
    companion object : TypeRegistry<CfirModuleCapability, CfirModuleCapability>() {
        /**
         * 空能力集合的共享实例。
         */
        val Empty: CfirModuleCapabilities = CfirModuleCapabilities(emptyList())

        /**
         * 根据 [capabilities] 创建能力集合。
         *
         * 空列表直接返回 [Empty]，非空列表会注册每个能力到数组拥有者中。
         */
        fun create(capabilities: List<CfirModuleCapability>): CfirModuleCapabilities {
            return if (capabilities.isEmpty()) {
                Empty
            } else {
                CfirModuleCapabilities(capabilities)
            }
        }

        /**
         * 为能力类型键分配稳定下标。
         *
         * 这里保留显式同步，是为了兼容 [TypeRegistry] 调用点中先查后算的访问模式，
         * 确保同一个能力类不会在并发初始化时得到多个数组位置。
         */
        override fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(
            key: String,
            compute: (String) -> Int
        ): Int {
            return this[key] ?: synchronized(this) {
                this[key] ?: compute(key).also { this.putIfAbsent(key, it) }
            }
        }
    }

    /**
     * 当前能力集合使用的注册表。
     */
    override val typeRegistry: TypeRegistry<CfirModuleCapability, CfirModuleCapability> = Companion

    init {
        for (capability in capabilities) {
            registerComponent(capability.key, capability)
        }
    }
}
