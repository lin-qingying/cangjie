package org.cangnova.cangjie.test.model

import org.cangnova.cangjie.test.directives.model.ComposedRegisteredDirectives
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import java.io.File

class TestModuleStructureImpl(
    override val modules: List<TestModule>,
    override val originalTestDataFiles: List<File>
) : TestModuleStructure() {
    override val allDirectives: RegisteredDirectives = ComposedRegisteredDirectives(modules.map { it.directives })

    override fun toString(): String {
        return buildString {
            modules.forEach {
                appendLine(it)
                appendLine()
            }
        }
    }


}

