package org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols

import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

abstract class AbstractPackageSymbolTest : org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val allDirectives = testServices.cjTestModuleStructure.testModuleStructure.allDirectives

        analyze(mainFile) {
            for (packageName in allDirectives[Directives.HAS_PACKAGE]) {
                val packageSymbol = getPackageSymbol(packageName)
                testServices.assertions.assertNotNull(packageSymbol) {
                    "Package '$packageName' should exist"
                }
            }

            for (packageName in allDirectives[Directives.NO_PACKAGE]) {
                val packageSymbol = getPackageSymbol(packageName)
                testServices.assertions.assertEquals(null, packageSymbol) {
                    "Package '$packageName' should not exist"
                }
            }
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val HAS_PACKAGE by valueDirective(
            description = "Check whether the specified package exists",
            applicability = DirectiveApplicability.File,
            parser = ::parsePackageName,
        )

        val NO_PACKAGE by valueDirective(
            description = "Check whether the specified package does not exist",
            applicability = DirectiveApplicability.File,
            parser = ::parsePackageName,
        )

        private fun parsePackageName(value: String): FqName {
            return if (value == "<root>") FqName.ROOT else FqName(value)
        }
    }
}
