package org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.isValid
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEventKind
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule

/**
 * `CaSession` 失效行为的抽象测试。
 *
 * 该测试族通过 `CaSessionProvider` 获取公开分析 session，并复用通用 session invalidation 基类
 * 验证不同 modification event 对 session 生命周期的影响。
 */
abstract class AbstractAnalysisSessionInvalidationTest : AbstractSessionInvalidationTest<CaSession>() {
    /**
     * 当前测试输出所在的 golden 子目录名。
     *
     * analysis session 与 CFIR session 共享失效测试框架，但输出目录需要分开保存。
     */
    override val testOutputSubdirectoryName: String
        get() = "analysisSession"

    /**
     * 为指定测试模块创建需要参与失效断言的 Analysis API session。
     *
     * 每个模块通过项目级 `CaSessionProvider` 获取 use-site module 对应的 `CaSession`。
     */
    override fun getSessions(cjTestModule: CjTestModule): List<TestSession<CaSession>> {
        val sessionProvider = CaSessionProvider.getInstance(cjTestModule.caModule.project)
        return listOf(AnalysisTestSession(cjTestModule, sessionProvider.getAnalysisSession(cjTestModule.caModule)))
    }

    /**
     * 判断指定 session 是否需要跳过有效性标记断言。
     *
     * 全局 source 修改不会使 library binary/source use-site session 直接标记为无效，因此这些场景只比较失效集合。
     */
    override fun shouldSkipValidityCheck(session: TestSession<CaSession>): Boolean {
        return when (modificationEventKind) {
            KotlinModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION,
            KotlinModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION -> {
                val useSiteModule = session.underlyingSession.useSiteModule
                useSiteModule is CaLibraryModule || useSiteModule is CaLibrarySourceModule
            }

            else -> false
        }
    }
}

/**
 * Analysis API session 测试中的 session 包装模型。
 *
 * 该模型把仓颉测试模块和底层 `CaSession` 绑定在一起，并提供失效测试可读的描述文本。
 */
internal class AnalysisTestSession(
    /**
     * session 所属的测试模块。
     *
     * 描述和有效性跳过规则会通过该模块访问 use-site module。
     */
    override val cjTestModule: CjTestModule,
    /**
     * 当前测试实际观察的 Analysis API session。
     */
    override val underlyingSession: CaSession,
) : TestSession<CaSession>() {
    /**
     * 当前 Analysis API session 是否仍有效。
     *
     * 返回值来自公开 lifetime API，用于失效事件前后的断言。
     */
    override val isValid: Boolean
        get() = underlyingSession.isValid()

    /**
     * session 在 golden 输出中的稳定描述。
     *
     * library binary 模块额外标记为 resolvable session，方便区分依赖模块与普通 source module。
     */
    override val description: String
        get() = buildString {
            val useSiteModule = cjTestModule.caModule
            append(useSiteModule)
            if (useSiteModule is CaLibraryModule) {
                append(" (resolvable session)")
            }
        }
}

/**
 * 模块状态修改事件对应的 Analysis session 失效测试。
 */
abstract class AbstractModuleStateModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    /**
     * 当前测试发布的修改事件种类。
     */
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.MODULE_STATE_MODIFICATION
}

/**
 * 模块 out-of-block 修改事件对应的 Analysis session 失效测试。
 */
abstract class AbstractModuleOutOfBlockModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    /**
     * 当前测试发布的修改事件种类。
     */
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION
}

/**
 * 全局模块状态修改事件对应的 Analysis session 失效测试。
 */
abstract class AbstractGlobalModuleStateModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    /**
     * 当前测试发布的修改事件种类。
     */
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_MODULE_STATE_MODIFICATION
}

/**
 * 全局 source module 状态修改事件对应的 Analysis session 失效测试。
 */
abstract class AbstractGlobalSourceModuleStateModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    /**
     * 当前测试发布的修改事件种类。
     */
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION
}

/**
 * 全局 source out-of-block 修改事件对应的 Analysis session 失效测试。
 */
abstract class AbstractGlobalSourceOutOfBlockModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    /**
     * 当前测试发布的修改事件种类。
     */
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION
}

/**
 * code fragment context 修改事件对应的 Analysis session 失效测试。
 */
abstract class AbstractCodeFragmentContextModificationAnalysisSessionInvalidationTest : AbstractAnalysisSessionInvalidationTest() {
    /**
     * 当前测试发布的修改事件种类。
     */
    override val modificationEventKind: KotlinModificationEventKind
        get() = KotlinModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION
}
