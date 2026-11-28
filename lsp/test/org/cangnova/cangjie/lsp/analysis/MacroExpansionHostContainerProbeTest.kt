package org.cangnova.cangjie.lsp.analysis

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.util.concurrency.AppExecutorUtil
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionConfigurator
import org.cangnova.cangjie.analysis.macro.expansion.FakeMacroSemanticExpansionEnvironment
import org.cangnova.cangjie.analysis.macro.expansion.MacroSemanticExpansionEnvironment
import org.cangnova.cangjie.analysis.macro.expansion.MacroSemanticExpansionService
import org.cangnova.cangjie.analysis.macro.expansion.MacroSemanticSessionConfigurator
import org.cangnova.cangjie.lsp.testkit.ServiceLoadedLspTestEnvironment
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

/**
 * Phase 2 前置探针（已固化为回归用例）：LSP 的 MockApplication / MockProject 容器能否承载宏语义展开编排。
 *
 * 实测结论（Windows / 平台 253）：
 * - `WriteAction.compute` 直接跑在后台线程会触发 EDT 断言；
 * - 后台线程的 `invokeAndWait { WriteAction.compute { ... } }` **可用**，写访问在 runnable 内成立；
 * - 后台线程直接 `Application.runWriteAction { ... }` 也可用。
 *
 * 因此 LSP 可以复用 IDE 的 `MacroExpansionBarrier` 形态（`invokeAndWait + WriteAction`），
 * 不需要为 headless 容器另造一套发布原语。
 *
 * 同时验证 `analysis:macro-expansion` 的描述符能被 LSP 的服务注册链路加载：
 * 服务可解析、扩展点已声明、environment 可由宿主注册。
 */
class MacroExpansionHostContainerProbeTest {
    @Test
    fun `macro expansion host contracts are available in headless lsp container`() {
        val environment = ServiceLoadedLspTestEnvironment.create(mode = CangJieCoreEnvironmentMode.UnitTest)
        try {
            val application = ApplicationManager.getApplication()
            assertTrue(application is MockApplication, "LSP 测试容器必须是 MockApplication")
            val project = environment.project
            assertTrue(project is MockProject, "LSP 测试容器必须是 MockProject")

            // 1. 描述符加载：编排服务必须是可解析的 project service。
            assertNotNull(
                MacroSemanticExpansionService.getInstance(project),
                "cangjie-analysis-api-cfir.xml 应加载宏展开服务",
            )

            // 2. 扩展点已声明，且没有 environment 时不触发任何构造。
            assertNull(
                MacroSemanticExpansionEnvironment.getInstance(project),
                "未注册 environment 时 LSP 容器必须保持无宏展开",
            )

            // 2.1 触发器由 analysis 描述符注册，并经 headless 扩展点白名单放行。
            val configurators = LLCfirSessionConfigurator.extensionPointName.getExtensionList(project)
            assertTrue(
                configurators.any { it.javaClass.name == MacroSemanticSessionConfigurator::class.java.name },
                "cangjie-macro-expansion.xml 注册的 MacroSemanticSessionConfigurator 必须被 headless 容器装配",
            )

            // 3. 宿主注册 environment 后立即可见（dynamic 扩展点）。
            val fakeEnvironment = FakeMacroSemanticExpansionEnvironment()
            fakeEnvironment.register(project)
            assertSame(
                fakeEnvironment,
                MacroSemanticExpansionEnvironment.getInstance(project),
                "macroExpansionEnvironment 动态扩展点必须支持运行时注册",
            )
        } finally {
            environment.close()
        }
    }

    @Test
    fun `macro expansion barrier shape works on background thread`() {
        val environment = ServiceLoadedLspTestEnvironment.create(mode = CangJieCoreEnvironmentMode.UnitTest)
        try {
            // IDE 形态：后台线程 invokeAndWait，runnable 内执行写动作。
            val writeInsideInvokeAndWait = runOnPool {
                var writeAccessAllowed = false
                ApplicationManager.getApplication().invokeAndWait {
                    writeAccessAllowed = WriteAction.compute<Boolean, RuntimeException> {
                        ApplicationManager.getApplication().isWriteAccessAllowed
                    }
                }
                writeAccessAllowed
            }
            assertTrue(writeInsideInvokeAndWait, "barrier 内必须持有写访问")

            // 备选形态：后台线程直接 runWriteAction（MockApplication 不要求 EDT）。
            val writeInsideRunWriteAction = runOnPool {
                ApplicationManager.getApplication().runWriteAction<Boolean> {
                    ApplicationManager.getApplication().isWriteAccessAllowed
                }
            }
            assertTrue(writeInsideRunWriteAction, "runWriteAction 在 headless 容器内可用")
        } finally {
            environment.close()
        }
    }

    private fun <T> runOnPool(action: () -> T): T =
        AppExecutorUtil.getAppExecutorService().submit(Callable(action)).get(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

    private companion object {
        private const val PROBE_TIMEOUT_SECONDS = 30L
    }
}
