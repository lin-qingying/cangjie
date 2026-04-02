package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices

/**
 * Analysis API 测试模块结构。
 *
 * 对齐 Kotlin `KtTestModuleStructure` 的职责，保存原始 `TestModuleStructure`
 * 与 Analysis API 层模块对象之间的映射关系。
 */
class CjTestModuleStructure(
    val testModuleStructure: TestModuleStructure,
    val mainModules: List<CjTestModule>,
) : TestService {
    val project: Project
        get() = mainModules.first().caModule.project

    val allCjFiles: List<CjFile>
        get() = mainModules.flatMap(CjTestModule::cjFiles)

    val allCaModules: List<CaModule>
        get() = mainModules.map(CjTestModule::caModule)

    fun getModule(moduleName: String): CjTestModule =
        mainModules.first { it.name == moduleName }
}

abstract class CjTestModuleStructureProvider : TestService {
    protected abstract val testServices: TestServices

    abstract fun registerModuleStructure(moduleStructure: CjTestModuleStructure)

    abstract fun getModuleStructure(): CjTestModuleStructure
}

class CjTestModuleStructureProviderImpl(
    override val testServices: TestServices,
) : CjTestModuleStructureProvider() {
    private lateinit var moduleStructure: CjTestModuleStructure

    override fun registerModuleStructure(moduleStructure: CjTestModuleStructure) {
        require(!this::moduleStructure.isInitialized) {
            "CjTestModuleStructure 已经注册，测试框架不允许重复覆盖。"
        }
        this.moduleStructure = moduleStructure
    }

    override fun getModuleStructure(): CjTestModuleStructure = moduleStructure
}

val TestServices.cjTestModuleStructureProvider: CjTestModuleStructureProvider
    by TestServices.testServiceAccessor()

val TestServices.cjTestModuleStructure: CjTestModuleStructure
    get() = cjTestModuleStructureProvider.getModuleStructure()
