package org.cangjie.test.model

import org.cangjie.test.directives.model.RegisteredDirectives
import java.io.File

class TestModuleStructureImpl(
    override val modules: List<TestModule>,
    override val allDirectives: RegisteredDirectives,
    override val originalTestDataFiles: List<File>,
) : TestModuleStructure()

