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

/**
 * 表示 `ServiceRegistrationData`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
data class ServiceRegistrationData(
    /**
     * 保存 `kClass`，供测试服务在测试执行期间读取或传递。
     */
    val kClass: KClass<out  TestService>,
    /**
     * 保存 `serviceConstructor`，供测试服务在测试执行期间读取或传递。
     */
    val serviceConstructor: (TestServices) ->  TestService
)

/**
 * 提供 `service` 对应的测试服务流程，维持测试框架的阶段契约。
 */
inline fun <reified T :  TestService> service(
    noinline serviceConstructor: () -> T
): ServiceRegistrationData {
    return ServiceRegistrationData(T::class) { serviceConstructor() }
}

/**
 * 提供 `service` 对应的测试服务流程，维持测试框架的阶段契约。
 */
inline fun <reified T :  TestService> service(
    noinline serviceConstructor: (TestServices) -> T
): ServiceRegistrationData {
    return ServiceRegistrationData(T::class, serviceConstructor)
}

/**
 * 表示 `TestServices`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class TestServices : ComponentArrayOwner<TestService, TestService>(){
    /**
     * 保存 `typeRegistry`，供测试服务在测试执行期间读取或传递。
     */
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

    /**
     * 执行 `register` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun register(data: ServiceRegistrationData, skipAlreadyRegistered: Boolean) {
        if (skipAlreadyRegistered && getOrNull(data.kClass) != null) {
            return
        }
        registerComponent(data.kClass, data.serviceConstructor(this))
    }

    /**
     * 执行 `register` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun register(kClass: KClass<out TestService>, service: TestService) {
        registerComponent(kClass, service)
    }

    /**
     * 执行 `register` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun register(data: List<ServiceRegistrationData>, skipAlreadyRegistered: Boolean) {
        data.forEach { register(it, skipAlreadyRegistered) }
    }
}

/**
 * 执行 `registerArtifactsProvider` 对应的测试服务流程，维持测试框架的阶段契约。
 */
fun TestServices.registerArtifactsProvider(artifactsProvider: ArtifactsProvider) {
    register(ArtifactsProvider::class, artifactsProvider)
}
