package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.test.model.DependencyDescription
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices

class CfirModuleInfoProvider(private val testServices: TestServices) : TestService {
    private val cfirModuleDataByModule: MutableMap<TestModule, CfirModuleData> = mutableMapOf()

    fun registerModuleData(module: TestModule, moduleData: CfirModuleData) {
        cfirModuleDataByModule[module] = moduleData
    }

    fun getCorrespondingModuleData(module: TestModule): CfirModuleData {
        return cfirModuleDataByModule[module] ?: error("module data for module $module is not registered")
    }

    fun getRegularDependentSourceModules(module: TestModule): List<CfirModuleData> {
        return getDependentModulesImpl(module.regularDependencies)
    }

    fun getDependentFriendSourceModules(module: TestModule): List<CfirModuleData> {
        return getDependentModulesImpl(module.friendDependencies)
    }

    fun getDependentDependsOnSourceModules(@Suppress("UNUSED_PARAMETER") module: TestModule): List<CfirModuleData> {
        return emptyList()
    }

    private fun getDependentModulesImpl(dependencies: List<DependencyDescription>): List<CfirModuleData> {
        return dependencies.filter { it.kind == DependencyKind.Source }.map {
            getCorrespondingModuleData(it.dependencyModule ?: error("Dependency module is unresolved for $it"))
        }
    }
}

val TestServices.cfirModuleInfoProvider: CfirModuleInfoProvider by TestServices.testServiceAccessor()
