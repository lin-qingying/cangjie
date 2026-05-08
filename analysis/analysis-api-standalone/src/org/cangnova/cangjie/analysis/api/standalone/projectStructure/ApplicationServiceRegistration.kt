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
    private val lock = ReentrantReadWriteLock()

    fun <DATA> register(application: MockApplication, registrars: List<AnalysisApiServiceRegistrar<DATA>>, data: DATA) {
        registerWithCustomRegistration(application, registrars) {
            registerApplicationServices(application, data)
        }
    }

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

    private fun <DATA> Application.isRegistrarRegistered(registrar: AnalysisApiServiceRegistrar<DATA>): Boolean =
        serviceRegistered[registrar.id] == true

    private val <DATA> AnalysisApiServiceRegistrar<DATA>.id: String
        get() = this::class.qualifiedName ?: error("A service registrar should have a qualified name.")

    private val Application.serviceRegistered
            by UserDataPropertyWithDefault<Application, MutableMap<String, Boolean>>(
                KeyWithDefaultValue.create("ApplicationServiceRegistration.serviceRegistered") { mutableMapOf() },
            )

    private class UserDataPropertyWithDefault<in R : UserDataHolder, T>(val key: KeyWithDefaultValue<T>) {
        operator fun getValue(thisRef: R, desc: KProperty<*>): T =
            thisRef.getUserData(key) ?: error("A user data key with a default value should guarantee a non-null value.")

        operator fun setValue(thisRef: R, desc: KProperty<*>, value: T) {
            thisRef.putUserData(key, value)
        }
    }
}
