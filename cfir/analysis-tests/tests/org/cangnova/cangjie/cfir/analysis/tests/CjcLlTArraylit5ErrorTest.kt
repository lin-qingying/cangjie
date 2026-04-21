package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCjcLlTDiagnosticsConsistencyTest
import org.cangnova.cangjie.cfir.session.noPrelude
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.test.WrappedException
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.artifactsProvider
import org.cangnova.cangjie.test.services.moduleStructure
import org.junit.jupiter.api.Test

/**
 * 回归验证 `--no-prelude` 下数组字面量场景不能在 body resolve 提前崩溃，
 * 必须稳定进入 CFIR vs CJC 诊断对比。
 */
class CjcLlTArraylit5ErrorTest : AbstractCjcLlTDiagnosticsConsistencyTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useAfterAnalysisCheckers(::Arraylit5DebugChecker, insertAtFirst = true)
    }

    @Test
    fun testArraylit5Error() {
        runTest("cfir/analysis-tests/testData/llt/array/arraylit5_error.cj")
    }
}

class Arraylit5DebugChecker(
    testServices: TestServices,
) : AfterAnalysisChecker(testServices) {
    override fun check(failedAssertions: List<WrappedException>) {
        val module = testServices.moduleStructure.modules.single()
        val artifact = testServices.artifactsProvider.getArtifactSafe(module, FrontendKinds.CFIR) ?: error("Missing CFIR artifact")
        val session = artifact.partsForDependsOnModules.single().session
        val stdCoreFqName = FqName("std.core")
        val stdCoreObjectId = ClassId(stdCoreFqName, Name.identifier("Object"))
        error(
            "DEBUG noPrelude=${session.noPrelude}, " +
                "hasStdCore=${session.symbolProvider.hasPackage(stdCoreFqName)}, " +
                "hasObject=${runCatching { session.symbolProvider.getClassLikeSymbolByClassId(stdCoreObjectId) }.getOrNull() != null}"
        )
    }
}
