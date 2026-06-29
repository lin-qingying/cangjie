package org.cangnova.cangjie.analysis.api.standalone.projectStructure

import com.intellij.mock.MockApplication
import com.intellij.openapi.application.Application
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.openapi.util.UserDataHolder
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.reflect.KProperty

/**
 * 对齐 Kotlin `ApplicationServiceRegistration`。
 */
object ApplicationServiceRegistration {
    /**
     * 防止同一个 application 容器内并发重复执行同一个注册器的读写锁。
     */
    private val lock = ReentrantReadWriteLock()

    /**
     * 按 registrar 类型去重后注册 application 级服务。
     */
    fun <DATA> register(application: MockApplication, registrars: List<AnalysisApiServiceRegistrar<DATA>>, data: DATA) {
        registerWithCustomRegistration(application, registrars) {
            registerApplicationServices(application, data)
        }
    }

    /**
     * 使用调用方提供的注册动作完成 application 服务注册，并保证同一 registrar 只执行一次。
     */
    fun <DATA> registerWithCustomRegistration(
        application: MockApplication,
        registrars: List<AnalysisApiServiceRegistrar<DATA>>,
        register: AnalysisApiServiceRegistrar<DATA>.() -> Unit,
    ) {
        for (registrar in registrars) {
            if (lock.readLock().withLock { application.isRegistrarRegistered(registrar) }) {
                continue
            }

            lock.writeLock().withLock {
                if (application.isRegistrarRegistered(registrar)) return@withLock
                registrar.register()
                application.serviceRegistered[registrar.id] = true
            }
        }
    }

    /**
     * 判断指定 registrar 是否已经在该 application 容器中成功执行过。
     */
    private fun <DATA> Application.isRegistrarRegistered(registrar: AnalysisApiServiceRegistrar<DATA>): Boolean =
        serviceRegistered[registrar.id] == true

    /**
     * registrar 的稳定去重标识，使用实现类全限定名避免不同注册器互相覆盖。
     */
    private val <DATA> AnalysisApiServiceRegistrar<DATA>.id: String
        get() = this::class.qualifiedName ?: error("A service registrar should have a qualified name.")

    /**
     * 存放 application 已执行 registrar 标记的 user-data 属性。
     */
    private val Application.serviceRegistered
            by UserDataPropertyWithDefault<Application, MutableMap<String, Boolean>>(
                KeyWithDefaultValue.create("ApplicationServiceRegistration.serviceRegistered") { mutableMapOf() },
            )

    /**
     * 以 [KeyWithDefaultValue] 为后端的 user-data 委托。
     */
    private class UserDataPropertyWithDefault<in R : UserDataHolder, T>(
        /**
         * 存取 user data 时使用的带默认值 key。
         */
        val key: KeyWithDefaultValue<T>,
    ) {
        /**
         * 读取 user data 中的值，依赖 [KeyWithDefaultValue] 保证默认值非空。
         */
        operator fun getValue(thisRef: R, desc: KProperty<*>): T =
            thisRef.getUserData(key) ?: error("A user data key with a default value should guarantee a non-null value.")

        /**
         * 将新值写回 user data。
         */
        operator fun setValue(thisRef: R, desc: KProperty<*>, value: T) {
            thisRef.putUserData(key, value)
        }
    }
}
