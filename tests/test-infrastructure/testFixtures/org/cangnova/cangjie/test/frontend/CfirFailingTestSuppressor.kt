package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.test.WrappedException
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.model.TestArtifactKind
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.moduleStructure
import java.io.File

abstract class AbstractFailingFacadeSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {

    protected abstract fun testFile(): File

    protected abstract val facadeKind: TestArtifactKind<*>

    override val order: Order
        get() = Order.P5

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val failFile = testFile().parentFile.resolve("${testFile().nameWithoutExtension}.fail").takeIf { it.exists() }
            ?: return failedAssertions
        val (suppressible, notSuppressible) = failedAssertions.partition {
            when (it) {
                is WrappedException.FromFacade -> it.facade.outputKind == facadeKind
                is WrappedException.FromHandler -> it.handler.artifactKind == facadeKind
                is WrappedException.FromMetaInfoHandler -> true
                else -> false
            }
        }

        return when {
            suppressible.isNotEmpty() -> notSuppressible

            // LL FIR is a Kotlin-specific concept and is not used in this project.
            testServices.moduleStructure.originalTestDataFiles.first().isLLCfirTestData -> failedAssertions
            else -> failedAssertions + AssertionError("Fail file exists but no exceptions was thrown. Please remove ${failFile.name}").wrap()
        }
    }
}

class CfirFailingTestSuppressor(testServices: TestServices) : AbstractFailingFacadeSuppressor(testServices) {

    override fun testFile(): File {
        return testServices.moduleStructure.originalTestDataFiles.first().cfirTestDataFile
    }

    override val facadeKind: TestArtifactKind<*>
        get() = FrontendKinds.CFIR
}

private const val CFIR_PREFIX = ".cfir"

private val File.isLLCfirTestData: Boolean
    get() = false

private val File.cfirTestDataFile: File
    get() {
        val extensionWithDot = ".$extension"
        if (name.endsWith("$CFIR_PREFIX$extensionWithDot")) return this

        val candidate = parentFile.resolve("${nameWithoutExtension}$CFIR_PREFIX$extensionWithDot")
        return if (candidate.exists()) candidate else this
    }
