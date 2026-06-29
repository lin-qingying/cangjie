package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.test.model.DependencyDescription
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices

/**
 * 表示 `CfirModuleInfoProvider`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirModuleInfoProvider(private val testServices: TestServices) : TestService {
    /**
     * 保存 `cfirModuleDataByModule`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val cfirModuleDataByModule: MutableMap<TestModule, CfirModuleData> = mutableMapOf()

    /**
     * 执行 `registerModuleData` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    fun registerModuleData(module: TestModule, moduleData: CfirModuleData) {
        cfirModuleDataByModule[module] = moduleData
    }

    /**
     * 执行 `getCorrespondingModuleData` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    fun getCorrespondingModuleData(module: TestModule): CfirModuleData {
        return cfirModuleDataByModule[module] ?: error("module data for module $module is not registered")
    }

    /**
     * 执行 `getRegularDependentSourceModules` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    fun getRegularDependentSourceModules(module: TestModule): List<CfirModuleData> {
        return getDependentModulesImpl(module.regularDependencies)
    }

    /**
     * 执行 `getDependentFriendSourceModules` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    fun getDependentFriendSourceModules(module: TestModule): List<CfirModuleData> {
        return getDependentModulesImpl(module.friendDependencies)
    }

    /**
     * 执行 `getDependentDependsOnSourceModules` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    fun getDependentDependsOnSourceModules(@Suppress("UNUSED_PARAMETER") module: TestModule): List<CfirModuleData> {
        return emptyList()
    }

    /**
     * 提供 `getDependentModulesImpl` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun getDependentModulesImpl(dependencies: List<DependencyDescription>): List<CfirModuleData> {
        return dependencies.filter { it.kind == DependencyKind.Source }.map {
            getCorrespondingModuleData(it.dependencyModule ?: error("Dependency module is unresolved for $it"))
        }
    }
}

/**
 * 保存 `TestServices.cfirModuleInfoProvider`，供CFIR 前端测试在测试执行期间读取或传递。
 */
val TestServices.cfirModuleInfoProvider: CfirModuleInfoProvider by TestServices.testServiceAccessor()
