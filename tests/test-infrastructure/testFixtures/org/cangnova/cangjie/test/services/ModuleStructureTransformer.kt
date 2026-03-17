package org.cangnova.cangjie.test.services

import com.intellij.codeInsight.actions.onSave.FormatOnSaveOptionsBase
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.model.TestModuleStructure

@TestInfrastructureInternals
abstract class ModuleStructureTransformer {
    abstract fun transformModuleStructure(moduleStructure: TestModuleStructure, defaultsProvider: DefaultsProvider): TestModuleStructure
}

class ExceptionFromModuleStructureTransformer(
    override val cause: Throwable,
    val alreadyParsedModuleStructure: TestModuleStructure
) : Exception(cause)
