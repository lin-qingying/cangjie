package org.cangnova.cangjie.analysis.stubs

import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * compiled stub 的 golden 基线。
 *
 * 这里固定选取体量较小的 `std.objectpool`，避免把测试变成大规模 builtins 扫描；
 * 同时它仍然走真实 `.cjo -> compiled PSI -> stub summary` 链路。
 */
class CaStubCompiledGoldenTest {
    @Test
    fun builtinsObjectPoolSummary() {
        CaStubTestSupport.withEnvironment("CaStubCompiledGoldenTest") { environment ->
            CaStubTestSupport.withRegisteredStubAndDecompilerServices(environment) {
                CaStubTestSupport.withSlimStdlibFixture(
                    "std.cjo",
                    "std/std.core.cjo",
                    "std/std.objectpool.cjo",
                ) { _ ->
                    val builtinsModule = CaStubTestSupport.installBuiltinsProjectStructure(environment)
                    val packageFqName = FqName("std.objectpool")
                    val psiProvider = environment.project.getService(CaDecompiledPsiProvider::class.java)
                    val decompiledFile = psiProvider.findDecompiledFile(builtinsModule, packageFqName)
                    assertNotNull(decompiledFile, "decompiled PSI provider should restore compiled file for `std.objectpool`")

                    val summary = CaStubSummaryBuilder().build(decompiledFile!!)
                    val actual = CaStubTestSupport.renderSummary(summary)
                    val expectedFile = CaStubTestSupport.locateRepositoryRoot()
                        .resolve("analysis")
                        .resolve("stubs")
                        .resolve("testData")
                        .resolve("compiled")
                        .resolve("std.objectpool.compiled.stubs.txt")
                    CaStubTestSupport.assertMatchesGolden(actual, expectedFile)
                }
            }
        }
    }
}
