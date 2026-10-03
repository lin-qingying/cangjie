package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

/**
 * `.cjo` 读侧埋点的端到端接线测试。
 *
 * 这一层此前完全没有验证，而 PSI 解析那次正是同一形状的漏洞：观察者接口和统计域两端都存在、
 * 单测都绿，可没有任何东西真正到达接缝，端到端指标恒为 0。所以这里用**真实的 SDK cjo
 * fixture + 真实的 [CfirDeserializedSymbolProvider]**，在真实 session 上注册记录观察者，然后做
 * 一次真实的库查询，断言观察者确实收到了两个阶段的回调。
 *
 * 断言的是**观察者回调而不是查询结果**：查的包在 fixture 里、遍历就被包裹，样本必然产生；这样
 * 用例不会因 fixture 内容变化而脆。用什么查询命中同样无关紧要。
 */
class CjoDeserializationTimingSeamTest {
    /**
     * 真实库查询经接缝上报包加载与按声明两个阶段；重复查询命中缓存后不再产生样本。
     */
    @Test
    fun realLibraryQueryReachesTheObserverThroughBothStages() {
        val tempDir = Files.createTempDirectory("cjo-timer-seam-")
        try {
            val target = tempDir.resolve(CjoConstants.packageNameToPath("std.core"))
            target.parent?.createDirectories()
            target.outputStream().use { it.write(resourceBytes(STD_CORE_FIXTURE)) }

            val manager = CjoManager(CjoSearchPath { key -> tempDir.toString() })
            val provider = CfirDeserializedSymbolProvider(
                session = ProbeSession,
                cjoManager = manager,
                cangjieScopeProvider = CfirCangJieScopeProvider(),
                libraryModuleData = ProbeModuleData,
            )
            val recorder = RecordingObserver()
            ProbeSession.registerCjoDeserializationTimingObserver(recorder)

            // 首次查询：包上下文未加载 + 声明未反序列化，两个阶段都应各报一次。
            provider.getTopLevelExtendDeclarations(FqName("std.core"))

            assertEquals(
                1,
                recorder.stages.count { it == CfirCjoDeserializationStage.PACKAGE_LOAD },
                "首次查询必须上报一次包加载，实际：${recorder.stages}",
            )
            assertTrue(
                recorder.stages.contains(CfirCjoDeserializationStage.DECLARATION),
                "首次查询必须上报按声明反序列化，实际：${recorder.stages}",
            )
            val declarationSamples = recorder.declarationCounts
                .filterIndexed { index, _ -> recorder.stages[index] == CfirCjoDeserializationStage.DECLARATION }
            assertTrue(
                declarationSamples.isNotEmpty() && declarationSamples.all { it > 0 },
                "按声明阶段的样本必须带正的声明数，实际：$declarationSamples",
            )
            assertTrue(recorder.elapsedNanos.all { it >= 0L }, recorder.elapsedNanos.toString())

            // 命中缓存：extendCache 与 contextCache 都已填充，不应再产生任何样本。
            // 这是"只在缓存未命中时计时"这条口径的守卫——记进去会把"包加载耗时"
            // 稀释成"符号查询耗时"。
            val samplesAfterFirstQuery = recorder.stages.size
            provider.getTopLevelExtendDeclarations(FqName("std.core"))

            assertEquals(
                samplesAfterFirstQuery,
                recorder.stages.size,
                "缓存命中的重复查询不得再上报，实际新增：${recorder.stages.drop(samplesAfterFirstQuery)}",
            )
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    /**
     * 未注册观察者时查询照常返回结果，且不产生任何上报。
     *
     * session 组件接缝的全部意义就在这里：宿主没开统计时这条路径必须零行为变化，
     * 否则关掉统计开关会顺带关掉反序列化功能本身。
     */
    @Test
    fun queryWorksWithoutAnObserver() {
        val tempDir = Files.createTempDirectory("cjo-timer-noseam-")
        try {
            val target = tempDir.resolve(CjoConstants.packageNameToPath("std.core"))
            target.parent?.createDirectories()
            target.outputStream().use { it.write(resourceBytes(STD_CORE_FIXTURE)) }

            val manager = CjoManager(CjoSearchPath { key -> tempDir.toString() })
            val provider = CfirDeserializedSymbolProvider(
                session = UnobservedSession,
                cjoManager = manager,
                cangjieScopeProvider = CfirCangJieScopeProvider(),
                libraryModuleData = UnobservedModuleData,
            )

            assertTrue(
                UnobservedSession.cjoDeserializationTimingObserverOrNull == null,
                "本用例的前提就是没有注册观察者",
            )
            // 结果内容不在断言范围内：这里只关心"没注册观察者时这条路径不会抛、也不上报"。
            provider.getTopLevelExtendDeclarations(FqName("std.core"))
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    /**
     * 记录上报内容的观察者替身。
     */
    private class RecordingObserver : CfirCjoDeserializationTimingObserver {
        val stages = mutableListOf<CfirCjoDeserializationStage>()
        val elapsedNanos = mutableListOf<Long>()
        val declarationCounts = mutableListOf<Int>()

        override fun onCjoDeserializationFinished(
            stage: CfirCjoDeserializationStage,
            elapsedNanos: Long,
            declarationCount: Int,
        ) {
            stages += stage
            this.elapsedNanos += elapsedNanos
            declarationCounts += declarationCount
        }
    }

    /**
     * 注册了观察者的库模块数据。
     */
    private object ProbeModuleData : TestModuleData("cjo_timer_probe") {
        override val session: CfirSession
            get() = ProbeSession
    }

    /**
     * 未注册观察者的库模块数据；与 [UnobservedSession] 一一绑定。
     */
    private object UnobservedModuleData : TestModuleData("cjo_timer_unobserved") {
        override val session: CfirSession
            get() = UnobservedSession
    }

    /**
     * 测试用库模块数据的公共部分：模块数据与 session 必须一一绑定，所以两个用例各持一份。
     */
    private abstract class TestModuleData(moduleName: String) : CfirModuleData() {
        override val name: Name = Name.identifier(moduleName)
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = false
        override val stableModuleName: String = moduleName

        init {
            bindSession(session)
        }
    }

    /**
     * 注册了观察者的库 session。
     */
    private object ProbeSession : CfirSession(CfirSession.Kind.Library) {
        init {
            register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
        }
    }

    /**
     * 未注册观察者的库 session。
     */
    private object UnobservedSession : CfirSession(CfirSession.Kind.Library) {
        init {
            register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
        }
    }

    /**
     * 读取测试资源里的 `.cjo` fixture。
     */
    private fun resourceBytes(path: String): ByteArray =
        javaClass.classLoader.getResourceAsStream(path)?.use { it.readBytes() }
            ?: error("fixture not found in test resources: $path")

    private companion object {
        /**
         * 随测试资源携带的真实 SDK `std.core` 包。
         */
        private const val STD_CORE_FIXTURE = "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo"
    }
}