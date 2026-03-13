package org.cangnova.cangjie.test.services

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * 测试服务容器（对齐 Kotlin 的 TestServices）。
 *
 * 提供类型安全的服务注册和查找。
 * 使用 [testServiceAccessor] 委托属性方便地从扩展属性访问服务。
 *
 * 示例：
 * ```
 * val TestServices.myService: MyService by TestServices.testServiceAccessor()
 * ```
 */
class TestServices {
    private val services = mutableMapOf<KClass<*>, TestService>()

    fun <T : TestService> register(kClass: KClass<T>, service: T) {
        services[kClass] = service
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : TestService> getOrNull(kClass: KClass<T>): T? = services[kClass] as? T

    fun <T : TestService> get(kClass: KClass<T>): T =
        getOrNull(kClass) ?: error("Service ${kClass.simpleName} is not registered in TestServices.")

    companion object {
        inline fun <reified T : TestService> testServiceAccessor(): TestServiceAccessor<T> =
            TestServiceAccessor(T::class)
    }
}

class TestServiceAccessor<T : TestService>(private val kClass: KClass<T>) {
    operator fun getValue(thisRef: TestServices, property: KProperty<*>): T = thisRef.get(kClass)
}
