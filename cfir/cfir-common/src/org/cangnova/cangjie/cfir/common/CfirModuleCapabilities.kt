package org.cangnova.cangjie.cfir.common

import org.cangnova.cangjie.util.ComponentArrayOwner
import org.cangnova.cangjie.util.TypeRegistry
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

abstract class CfirModuleCapability {
    abstract val key: KClass<out CfirModuleCapability>
}

class CfirModuleCapabilities private constructor(
    capabilities: List<CfirModuleCapability>,
) : ComponentArrayOwner<CfirModuleCapability, CfirModuleCapability>() {

    companion object : TypeRegistry<CfirModuleCapability, CfirModuleCapability>() {
        val Empty: CfirModuleCapabilities = CfirModuleCapabilities(emptyList())

        fun create(capabilities: List<CfirModuleCapability>): CfirModuleCapabilities {
            return if (capabilities.isEmpty()) {
                Empty
            } else {
                CfirModuleCapabilities(capabilities)
            }
        }

        override fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(
            key: String,
            compute: (String) -> Int
        ): Int {
            return this[key] ?: synchronized(this) {
                this[key] ?: compute(key).also { this.putIfAbsent(key, it) }
            }
        }
    }

    override val typeRegistry: TypeRegistry<CfirModuleCapability, CfirModuleCapability> = Companion

    init {
        for (capability in capabilities) {
            registerComponent(capability.key, capability)
        }
    }
}
