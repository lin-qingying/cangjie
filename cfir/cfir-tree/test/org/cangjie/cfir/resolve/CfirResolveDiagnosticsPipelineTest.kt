package org.cangjie.cfir.resolve

import org.cangjie.cfir.resolve.framework.AbstractCfirResolveDiagnosticsTest
import org.cangjie.test.TestMetadata
import org.junit.jupiter.api.Test

@TestMetadata("testData/resolveDiagnostics")
class CfirResolveDiagnosticsPipelineTest : AbstractCfirResolveDiagnosticsTest() {
    @Test
    @TestMetadata("smoke.cj")
    fun smoke() {
        runResolveDiagnosticsTest("testData/resolveDiagnostics/smoke.cj")
    }

    @Test
    @TestMetadata("invalidDeclaration.cj")
    fun invalidDeclaration() {
        runResolveDiagnosticsTest("testData/resolveDiagnostics/invalidDeclaration.cj")
    }

    @Test
    @TestMetadata("superDuplicate.cj")
    fun superDuplicate() {
        runResolveDiagnosticsTest("testData/resolveDiagnostics/superDuplicate.cj")
    }

    @Test
    @TestMetadata("superSelf.cj")
    fun superSelf() {
        runResolveDiagnosticsTest("testData/resolveDiagnostics/superSelf.cj")
    }

    @Test
    @TestMetadata("extendDuplicate.cj")
    fun extendDuplicate() {
        runResolveDiagnosticsTest("testData/resolveDiagnostics/extendDuplicate.cj")
    }

    @Test
    @TestMetadata("extendNotInterface.cj")
    fun extendNotInterface() {
        runResolveDiagnosticsTest("testData/resolveDiagnostics/extendNotInterface.cj")
    }
}
