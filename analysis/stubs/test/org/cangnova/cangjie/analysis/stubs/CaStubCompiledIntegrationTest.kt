package org.cangnova.cangjie.analysis.stubs

import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.analysis.api.stubs.CaStubIndexFacade
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjTypeStatement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 锁定 compiled `.cjo` 经过 decompiled 链路进入 `analysis:stubs` 后的稳定行为。
 *
 * 这里不伪造二进制数据，而是直接复用仓库内 stdlib fixture，确保：
 * 1. builtins binary index 能定位真实 package；
 * 2. compiled PSI 能携带真实 file stub；
 * 3. `analysis:stubs` 的 summary / facade 查询与 compiled PSI 对齐。
 */
class CaStubCompiledIntegrationTest {
    @Test
    fun builtinsCompiledFileParticipatesInStubIndex() {
        CaStubTestSupport.withEnvironment("CaStubCompiledIntegrationTest") { environment ->
            CaStubTestSupport.withRegisteredStubAndDecompilerServices(environment) {
                CaStubTestSupport.withSlimStdlibFixture(
                    "std.cjo",
                    "std/std.core.cjo",
                    "std/std.objectpool.cjo",
                ) { _ ->
                    val builtinsModule = CaStubTestSupport.installBuiltinsProjectStructure(environment)
                    val packageFqName = FqName("std.objectpool")
                    val binaryFile = CaStubTestSupport.findBuiltinsBinaryFile(environment, builtinsModule, packageFqName)
                    assertNotNull(binaryFile, "builtins binary index should resolve `std.objectpool`")

                    val psiProvider = environment.project.getService(CaDecompiledPsiProvider::class.java)
                    val decompiledFile = requireNotNull(psiProvider.findDecompiledFile(builtinsModule, packageFqName)) {
                        "decompiled PSI provider should restore compiled file for `std.objectpool`"
                    }
                    assertEquals(packageFqName, decompiledFile.packageFqName)
                    assertTrue(decompiledFile.isCompiled, "restored file should stay in compiled mode")

                    val summary = CaStubSummaryBuilder().build(decompiledFile)
                    assertEquals(packageFqName, summary.packageFqName)
                    assertNotNull(summary.stubKind, "compiled file should carry a real file stub kind")
                    assertTrue(
                        summary.topLevelClassifierNames.isNotEmpty() || summary.topLevelCallableNames.isNotEmpty(),
                        "compiled stub summary should expose at least one top-level declaration",
                    )
                    val topLevelTypeStatement = decompiledFile.declarations.filterIsInstance<CjTypeStatement>().single()
                    val classBody = topLevelTypeStatement.body
                    assertNotNull(classBody, "compiled type statement should restore a stub-backed body placeholder")
                    assertTrue(
                        requireNotNull(classBody).declarations.isNotEmpty(),
                        "compiled class body should expose member declarations from stubs without parsing decompiled text",
                    )

                    val facade = CaStubIndexFacade.getInstance(environment.project)
                    val fileClassifiers = facade.fileProvider.getTopLevelClassifierNames(decompiledFile)
                    val fileCallables = facade.fileProvider.getTopLevelCallableNames(decompiledFile)
                    assertEquals(summary.topLevelClassifierNames, fileClassifiers)
                    assertEquals(summary.topLevelCallableNames, fileCallables)
                    assertTrue(
                        facade.packageIndex.getAvailablePackages().contains(packageFqName),
                        "package index should include compiled package `std.objectpool`",
                    )
                    assertEquals(fileClassifiers, facade.packageIndex.getTopLevelClassifierNames(packageFqName))
                    assertEquals(fileCallables, facade.packageIndex.getTopLevelCallableNames(packageFqName))
                    assertFalse(
                        facade.fileProvider.getFileStubKind(decompiledFile).toString().contains("Invalid"),
                        "supported stdlib binary should not degrade to invalid file stub",
                    )
                }
            }
        }
    }
}
