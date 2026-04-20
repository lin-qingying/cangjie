package org.cangnova.cangjie.analysis.test.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructure
import java.util.concurrent.ConcurrentHashMap

/**
 * 测试侧项目结构注册表。
 *
 * `CangJieProjectStructureProvider` 是 IntelliJ project service，而测试模块结构来自 `TestServices`。
 * 两者的生命周期容器不同，因此需要一个受控桥接层把测试结构挂到 Project 上。
 */
object CaTestProjectStructureRegistry {
    private val structures = ConcurrentHashMap<Project, CjTestModuleStructure>()

    fun register(
        project: Project,
        moduleStructure: CjTestModuleStructure,
        disposable: Disposable,
    ) {
        structures[project] = moduleStructure
        Disposer.register(disposable) {
            structures.remove(project)
        }
    }

    fun get(project: Project): CjTestModuleStructure =
        structures[project] ?: error("Project structure for `$project` has not been initialized in Analysis API tests.")
}
