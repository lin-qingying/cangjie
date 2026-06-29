@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractSessionInvalidationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.TestSession
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEventKind
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator

/**
 * 对齐 Kotlin `AbstractLLFirSessionInvalidationTest` 的 low-level session 失效测试。
 *
 * 这里覆盖的是 `LLCfirSessionCache` 持有的 session 实例，而不是上层 analysis session。
 */
abstract class AbstractLLCfirSessionInvalidationTest : AbstractSessionInvalidationTest<LLCfirSession>() {
    /**
     * Golden 输出目录名，用于把 low-level CFIR session 失效结果与上层 analysis session 区分开。
     */
    override val testOutputSubdirectoryName: String
        get() = "cfirSession"

    /**
     * 从当前测试模块及其依赖模块中取得所有需要观察失效状态的 low-level CFIR session。
     */
    override fun getSessions(cjTestModule: CjTestModule): List<TestSession<LLCfirSession>> {
        val cache = LLCfirSessionCache.getInstance(cjTestModule.caModule.project)
        return cjTestModule.allCaModules.map { module ->
            LLCfirTestSession(
                cjTestModule = cjTestModule,
                underlyingSession = cache.getSession(module, preferBinary = module is CaLibraryModule),
            )
        }
    }

    /**
     * 使用源码 low-level CFIR 配置驱动 session 失效测试。
     */
    override val configurator: AnalysisApiTestConfigurator =
        analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}

/**
 * 将 `LLCfirSession` 适配为通用 session 失效测试框架可观察的测试对象。
 */
internal class LLCfirTestSession(
    /**
     * 创建该 session 的测试模块上下文。
     */
    override val cjTestModule: CjTestModule,
    /**
     * 被失效测试直接观察的 low-level CFIR session 实例。
     */
    override val underlyingSession: LLCfirSession,
) : TestSession<LLCfirSession>() {
    /**
     * 当前底层 session 是否仍被 cache 认为有效。
     */
    override val isValid: Boolean
        get() = underlyingSession.isValid

    /**
     * 输出到 golden 文件的 session 描述，库模块会显式标记二进制 session。
     */
    override val description: String
        get() = buildString {
            append(underlyingSession.caModule)
            if (underlyingSession.caModule is CaLibraryModule) {
                append(" (binary session)")
            }
    }
}

/**
 * 验证模块状态修改事件会正确失效相关 low-level CFIR session。
 */
abstract class AbstractModuleStateModificationLLCfirSessionInvalidationTest : AbstractLLCfirSessionInvalidationTest() {
    /**
     * 当前测试发送的模块状态修改事件类型。
     */
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.MODULE_STATE_MODIFICATION
}

/**
 * 验证模块级 out-of-block 修改事件对 low-level CFIR session 的失效影响。
 */
abstract class AbstractModuleOutOfBlockModificationLLCfirSessionInvalidationTest : AbstractLLCfirSessionInvalidationTest() {
    /**
     * 当前测试发送的模块 out-of-block 修改事件类型。
     */
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION
}

/**
 * 验证全局模块状态修改事件会传播到所有受影响的 low-level CFIR session。
 */
abstract class AbstractGlobalModuleStateModificationLLCfirSessionInvalidationTest : AbstractLLCfirSessionInvalidationTest() {
    /**
     * 当前测试发送的全局模块状态修改事件类型。
     */
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_MODULE_STATE_MODIFICATION
}

/**
 * 验证全局源码模块状态修改事件对源码相关 low-level CFIR session 的失效语义。
 */
abstract class AbstractGlobalSourceModuleStateModificationLLCfirSessionInvalidationTest : AbstractLLCfirSessionInvalidationTest() {
    /**
     * 当前测试发送的全局源码模块状态修改事件类型。
     */
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION
}

/**
 * 验证全局源码 out-of-block 修改事件对 low-level CFIR session cache 的失效语义。
 */
abstract class AbstractGlobalSourceOutOfBlockModificationLLCfirSessionInvalidationTest : AbstractLLCfirSessionInvalidationTest() {
    /**
     * 当前测试发送的全局源码 out-of-block 修改事件类型。
     */
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION
}

/**
 * 验证代码片段上下文修改事件不会破坏 low-level CFIR session 的失效契约。
 */
abstract class AbstractCodeFragmentContextModificationLLCfirSessionInvalidationTest : AbstractLLCfirSessionInvalidationTest() {
    /**
     * 当前测试发送的代码片段上下文修改事件类型。
     */
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION
}
