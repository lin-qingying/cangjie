package org.cangnova.cangjie.test.model

import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import java.io.File

class TestModuleStructureImpl(
    override val modules: List<TestModule>,
    override val allDirectives: RegisteredDirectives,
    override val originalTestDataFiles: List<File>,
) : TestModuleStructure()

