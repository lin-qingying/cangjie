package org.cangnova.cangjie.cfir

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes

/** 注册表沿用真实 ArrayMap 槽位；不通过 value 类型猜 key，不使用反射读取 private object。 */
object CfirRawDeclarationDataRegistry {
    private class Registration(
        val keyObject: CfirDeclarationDataKey,
    ) {
        val key: KClass<out CfirDeclarationDataKey> = keyObject::class
        /** key facade 与 policy carrier 可形成静态初始化环；收集图前才读取完整策略。 */
        @Suppress("UNCHECKED_CAST")
        val policy: CfirRawDeclarationDataPolicy<Any> by lazy(LazyThreadSafetyMode.PUBLICATION) {
            (keyObject.rawCopyPolicy ?: error("Non-empty declaration-data key has no RAW policy")) as CfirRawDeclarationDataPolicy<Any>
        }
        fun collect(value: Any, context: CfirRawCopyContext, visit: (CfirElement) -> Unit) =
            policy.collectOwned(policy.decode(value), context, visit)

        fun copy(value: Any, target: CfirDeclarationAttributes, context: CfirRawCopyContext) {
            policy.copy(policy.decode(value), context) { copied -> target[key] = copied }
        }
    }

    private val registrations = ConcurrentHashMap<Int, Registration>()

    /** 必须在正常 data/symbol/attributes accessor 创建时调用；未提供策略的 key 不会获得通行证。 */
    fun register(key: CfirDeclarationDataKey, slot: Int) {
        val registration = Registration(key)
        val previous = registrations.putIfAbsent(slot, registration)
        check(previous == null || previous.keyObject === key) { "Conflicting RAW declaration-data registration" }
    }

    fun collectOwned(source: CfirDeclarationAttributes, context: CfirRawCopyContext, visit: (CfirElement) -> Unit) {
        source.rawCopyEntries().forEach { (slot, value) ->
            val registration = registrations[slot] ?: error("Non-empty declaration-data slot $slot has no RAW policy")
            registration.collect(value, context, visit)
        }
    }

    fun copy(source: CfirDeclarationAttributes, target: CfirDeclarationAttributes, context: CfirRawCopyContext) {
        // 先检查完整非空槽集合，禁止复制一半后才遇到未分类 key。
        val entries = source.rawCopyEntries()
        entries.forEach { (slot, _) -> check(registrations.containsKey(slot)) { "Non-empty declaration-data slot $slot has no RAW policy" } }
        entries.forEach { (slot, value) -> registrations.getValue(slot).copy(value, target, context) }
    }
}
