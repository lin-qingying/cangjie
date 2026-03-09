package org.cangjie.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import org.cangjie.analysis.api.CaModule
import org.cangjie.test.services.TestService
import org.cangjie.test.services.TestServices
import org.cangnova.cangjie.psi.CjFile

/**
 * 测试模块结构（对齐 Kotlin 的 KtTestModuleStructure）。
 *
 * 持有测试中所有模块的信息，作为 [TestService] 注册到 [TestServices]。
 */
class CjTestModuleStructure(
    val mainModules: List<CjTestModule>,
) : TestService {
    val project: Project
        get() = mainModules.first().caModule.project

    val allCjFiles: List<CjFile>
        get() = mainModules.flatMap { it.cjFiles }

    val allCaModules: List<CaModule>
        get() = mainModules.map { it.caModule }

    fun getModule(moduleName: String): CjTestModule =
        mainModules.first { it.name == moduleName }
}

val TestServices.cjTestModuleStructure: CjTestModuleStructure
    by TestServices.testServiceAccessor()
