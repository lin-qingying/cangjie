package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.cfir.ConeTypeRegistry
import org.cangnova.cangjie.util.ArrayMapAccessor
import org.cangnova.cangjie.util.ComponentArrayOwner
import org.cangnova.cangjie.util.NullableArrayMapAccessor
import org.cangnova.cangjie.util.TypeRegistry
import kotlin.reflect.KClass

/**
 * 测试服务标记接口（对齐 Kotlin 的 TestService）。
 *
 * 所有可注册到 [TestServices] 中的测试服务都需要实现此接口。
 */
interface TestService

data class ServiceRegistrationData(
    val kClass: KClass<out  TestService>,
    val serviceConstructor: (TestServices) ->  TestService
)

inline fun <reified T :  TestService> service(
    noinline serviceConstructor: () -> T
): ServiceRegistrationData {
    return ServiceRegistrationData(T::class) { serviceConstructor() }
}

inline fun <reified T :  TestService> service(
    noinline serviceConstructor: (TestServices) -> T
): ServiceRegistrationData {
    return ServiceRegistrationData(T::class, serviceConstructor)
}

class TestServices : ComponentArrayOwner<TestService, TestService>(){
    override val typeRegistry: TypeRegistry<TestService, TestService>
        get() = Companion

    companion object : ConeTypeRegistry<TestService, TestService>() {
        inline fun <reified T :  TestService> testServiceAccessor(): ArrayMapAccessor<TestService,  TestService, T> {
            return generateAccessor(T::class)
        }

        inline fun <reified T : TestService> nullableTestServiceAccessor(): NullableArrayMapAccessor<TestService, TestService, T> {
            return generateNullableAccessor(T::class)
        }
    }

    fun register(data: ServiceRegistrationData, skipAlreadyRegistered: Boolean) {
        if (skipAlreadyRegistered && getOrNull(data.kClass) != null) {
            return
        }
        registerComponent(data.kClass, data.serviceConstructor(this))
    }

    fun register(kClass: KClass<out TestService>, service: TestService) {
        registerComponent(kClass, service)
    }

    fun register(data: List<ServiceRegistrationData>, skipAlreadyRegistered: Boolean) {
        data.forEach { register(it, skipAlreadyRegistered) }
    }
}

fun TestServices.registerArtifactsProvider(artifactsProvider: ArtifactsProvider) {
    register(ArtifactsProvider::class, artifactsProvider)
}

