package org.cangnova.cangjie.test.model

import org.cangnova.cangjie.test.directives.model.ComposedRegisteredDirectives
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import java.io.File

/**
 * 表示 `TestModuleStructureImpl`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
class TestModuleStructureImpl(
    /**
     * 保存 `modules`，供测试模型在测试执行期间读取或传递。
     */
    override val modules: List<TestModule>,
    /**
     * 保存 `originalTestDataFiles`，供测试模型在测试执行期间读取或传递。
     */
    override val originalTestDataFiles: List<File>
) : TestModuleStructure() {
    /**
     * 保存 `allDirectives`，供测试模型在测试执行期间读取或传递。
     */
    override val allDirectives: RegisteredDirectives = ComposedRegisteredDirectives(modules.map { it.directives })

    /**
     * 执行 `toString` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    override fun toString(): String {
        return buildString {
            modules.forEach {
                appendLine(it)
                appendLine()
            }
        }
    }


}
