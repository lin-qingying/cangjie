package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.pipeline.CfirSessionConstructionUtils
import org.cangnova.cangjie.cfir.pipeline.CfirSessionProducer
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.targetPlatform
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

@OptIn(CompilerConfiguration.Internals::class)
/**
 * 覆盖 CFIR session 构建时 target platform 传递行为的测试。
 */
class CfirSessionConstructionUtilsTargetPlatformTest {
    /**
     * 验证 cjvm 平台占位对象会原样写入 source module data。
     */
    @Test
    fun `prepareSessions carries cjvm placeholder through source module data`() {
        val configuration = CompilerConfiguration().apply {
            targetPlatform = CangJiePlatforms.cjvm
        }
        val dependencyList = DependencyListForCliModule.build(Name.identifier("target-platform-test")) { }

        val sessions = CfirSessionConstructionUtils.prepareSessions(
            files = listOf("sample.cj"),
            configuration = configuration,
            rootModuleName = Name.identifier("target-platform-test"),
            dependencyList = dependencyList,
            createSharedLibrarySession = { object : CfirSession(CfirSession.Kind.Library) {} },
            createLibrarySession = { object : CfirSession(CfirSession.Kind.Library) {} },
            createSourceSession = CfirSessionProducer { _, moduleData, _, _ ->
                object : CfirSession(CfirSession.Kind.Source) {}.also { session ->
                    moduleData.bindSession(session)
                    session.register(CfirModuleData::class, moduleData)
                }
            },
        )

        val sourceModuleData = sessions.single().session.moduleData as CfirSourceModuleData
        assertSame(CangJiePlatforms.cjvm, sourceModuleData.targetPlatform)
        assertEquals(CfirPlatform.DEFAULT, sourceModuleData.platform)
    }
}
