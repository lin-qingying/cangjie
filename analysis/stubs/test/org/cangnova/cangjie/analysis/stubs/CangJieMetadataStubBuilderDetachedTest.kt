package org.cangnova.cangjie.analysis.stubs

import com.intellij.openapi.vfs.VirtualFileManager
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.decompiler.stub.file.CangJieMetadataStubBuilder
import org.cangnova.cangjie.analysis.decompiled.psi.CangJieBuiltInMetadataStubBuilder
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.common.DetachedCjoModuleData
import org.cangnova.cangjie.cfir.session.DetachedCjoSession
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 结构外 `.cjo` 的 owner 降级验收点。
 *
 * 复现 `S9` 索引洞的触发条件：`.cjo` 位于 builtins 根之外、不属于任何 library module，
 * `findOwningModule` 返回 null。此时 `readFile` 必须给出 detached owner 而不是 null，
 * 并且 `DetachedCjoModuleData.session` 读到的就是构造时绑定的 `DetachedCjoSession`。
 */
class CangJieMetadataStubBuilderDetachedTest : AbstractAnalysisApiBasedTest() {
    /**
     * 使用 standalone CFIR 分析 API 测试配置执行结构外 `.cjo` 流程。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 注册 compiled `.cjo` 测试所需的 builtins provider、binary index 和 module data 服务。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> = listOf(
        CjoCompiledStubsTestServiceRegistrar,
    )

    /**
     * 结构外 `.cjo` 的 `readFile` 返回 detached owner，且 owner 的 session 就是绑定的 detached session。
     */
    @Test
    fun structuralForeignCjoGetsDetachedOwner() {
        CjoCompiledTestEnvironment.withSlimStdlibFixture(
            "std.cjo",
            "std/std.core.cjo",
            "std/std.objectpool.cjo",
        ) { stdlibRoot ->
            val detachedRoot = Files.createTempDirectory("cangjie-detached-cjo")
            val detachedBinary = detachedRoot.resolve("std.objectpool.cjo")
            Files.copy(
                stdlibRoot.resolve("std").resolve("std.objectpool.cjo"),
                detachedBinary,
                StandardCopyOption.REPLACE_EXISTING,
            )

            val testDataFile = CjoCompiledTestEnvironment.locateRepositoryRoot()
                .resolve("analysis")
                .resolve("stubs")
                .resolve("testData")
                .resolve("compiled")
                .resolve("std.objectpool.cj")
            runTest(testDataFile.toString()) { testServices ->
                val project = testServices.cjTestModuleStructure.project
                CjoCompiledTestEnvironment.installBuiltinsProjectStructure(project)

                val virtualFile = requireNotNull(
                    VirtualFileManager.getInstance().findFileByNioPath(detachedBinary),
                ) { "detached `.cjo` fixture should be visible in VFS" }

                // 前置事实：该文件不在 builtins 根内，owner 解析必须走 detached 分支。
                val owningModule = project.getService(CaDecompiledBinaryIndex::class.java).findOwningModule(virtualFile)
                assertNull(owningModule, "fixture outside builtins roots must have no owning module")

                val metadata = requireNotNull(
                    CangJieBuiltInMetadataStubBuilder.readFileSafely(virtualFile, null, project),
                ) { "structural-foreign `.cjo` must not fall back to null metadata" }
                val compatible = metadata as CangJieMetadataStubBuilder.FileWithMetadata.Compatible

                val moduleData = compatible.moduleData
                assertTrue(moduleData is DetachedCjoModuleData, "owner must be detached module data")
                assertTrue(moduleData.session is DetachedCjoSession, "detached module data must expose the bound detached session")
                assertEquals("std.objectpool", (moduleData as DetachedCjoModuleData).packageFqName.asString())
            }
        }
    }
}
