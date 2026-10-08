package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyzeCopy
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaDanglingFileModuleImpl
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode.IGNORE_SELF
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode.PREFER_SELF
import org.cangnova.cangjie.analysis.api.projectStructure.copyOrigin
import org.cangnova.cangjie.analysis.api.projectStructure.danglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.explicitModule
import org.cangnova.cangjie.analysis.api.projectStructure.isDangling
import org.cangnova.cangjie.analysis.api.projectStructure.isStable
import org.cangnova.cangjie.analysis.api.projectStructure.withDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjNamedPattern
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.psi.CjConstantExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 副本分析的公开入口与真实 AA/LL 会话回归；不依赖生成测试，也不另建解析引擎。
 * 会话仅在本测试中被保留，用于验证身份及 token 失效，业务调用方不得这样使用 lifetime owner。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class, CaPlatformInterface::class)
class AnalysisApiDanglingFileExecutionTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/sessions",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** 同文件和不同文件的嵌套覆盖都须在普通返回、异常及取消之后恢复。 */
    @Test
    fun danglingModeNestingAndRecovery(mainFile: CjFile) = readAction {
        val first = copyOf(mainFile)
        val second = copyOf(mainFile)
        val provider = CangJieProjectStructureProvider.getInstance(mainFile.project)
        val defaultModule = provider.getModule(first, null) as CaDanglingFileModule
        assertEquals(IGNORE_SELF, defaultModule.resolutionMode)
        assertNull(first.danglingFileResolutionMode)

        analyzeCopy(first.function("consume"), PREFER_SELF) {
            assertMode(first, PREFER_SELF)
            val outerModule = useSiteModule
            analyzeCopy(first, IGNORE_SELF) {
                assertMode(first, IGNORE_SELF)
                assertNotEquals(outerModule, useSiteModule)
            }
            assertMode(first, PREFER_SELF)
            analyzeCopy(second, IGNORE_SELF) {
                assertMode(second, IGNORE_SELF)
                assertEquals(PREFER_SELF, first.danglingFileResolutionMode)
            }
            assertNull(second.danglingFileResolutionMode)

            val failure = IllegalStateException("copy analysis failed")
            assertSame(failure, assertThrows(IllegalStateException::class.java) {
                analyzeCopy(first, IGNORE_SELF) { throw failure }
            })
            assertMode(first, PREFER_SELF)
            val cancellation = ProcessCanceledException()
            assertSame(cancellation, assertThrows(ProcessCanceledException::class.java) {
                analyzeCopy(first, IGNORE_SELF) { throw cancellation }
            })
            assertMode(first, PREFER_SELF)
        }
        assertNull(first.danglingFileResolutionMode)
        assertNull(second.danglingFileResolutionMode)
        assertNull(mainFile.danglingFileResolutionMode)
        assertEquals(defaultModule, provider.getModule(first, null))
    }

    /**
     * 同一副本并行分析采用各线程自己的模式，模式状态互不污染且结果与串行一致。
     *
     * PREFER_SELF 解析到副本、IGNORE_SELF 解析到原件：副本自身顶层的 `tracked` 与上下文模块的
     * 同名声明是同一份声明的两种身份，须由等价调用去重保留先到者，而不是报重载歧义。
     */
    @Test
    fun danglingThreadIsolation(mainFile: CjFile) {
        val copy = readAction { copyOf(mainFile) }
        val modes = listOf(PREFER_SELF, IGNORE_SELF)
        val serialTargets = readAction {
            modes.map { mode -> analyzeCopy(copy, mode) { copy.reference("tracked").resolveToSymbol()?.psi?.containingFile } }
        }
        assertEquals(listOf(copy, mainFile), serialTargets, "PREFER_SELF 必须解析到副本，IGNORE_SELF 必须解析到原件")
        val entered = CyclicBarrier(modes.size)
        val analyzed = CyclicBarrier(modes.size)
        val executor = Executors.newFixedThreadPool(modes.size)
        try {
            val futures = modes.map { mode ->
                executor.submit<CjFile?> {
                    readAction {
                        assertNull(copy.danglingFileResolutionMode)
                        val target = withDanglingFileResolutionMode(copy, mode) {
                            entered.await(20, TimeUnit.SECONDS)
                            val resolvedFile = analyzeCopy(copy, mode) {
                                assertMode(copy, mode)
                                assertLocalFromCopy(copy)
                                copy.reference("tracked").resolveToSymbol()?.psi?.containingFile as? CjFile
                            }
                            analyzed.await(20, TimeUnit.SECONDS)
                            assertEquals(mode, copy.danglingFileResolutionMode)
                            resolvedFile
                        }
                        assertNull(copy.danglingFileResolutionMode)
                        target
                    }
                }
            }
            assertEquals(serialTargets, futures.map { it.get(30, TimeUnit.SECONDS) })
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS))
        }
        assertNull(copy.danglingFileResolutionMode)
        assertNull(mainFile.danglingFileResolutionMode)
    }

    /**
     * 两副本、两模式分别建模：模块与 LL 会话身份互不混用，且显式声明查询始终取自副本。
     *
     * PREFER_SELF 解析到副本、IGNORE_SELF 解析到原件；非局部引用的策略不改变显式声明查询。
     */
    @Test
    fun danglingCopyAndModeIsolation(mainFile: CjFile) = readAction {
        val first = copyReturning(mainFile, "3")
        val second = copyReturning(mainFile, "4")
        val sessions = listOf(first, second).flatMap { copy ->
            listOf(PREFER_SELF, IGNORE_SELF).map { mode ->
                analyzeCopy(copy, mode) {
                    assertMode(copy, mode)
                    val target = copy.reference("tracked").resolveToSymbol()
                    if (mode == IGNORE_SELF) {
                        assertSame(mainFile, target?.psi?.containingFile)
                    } else {
                        assertSame(copy, target?.psi?.containingFile, "PREFER_SELF 必须解析到副本顶层声明")
                    }
                    assertLocalFromCopy(copy)
                    // 显式请求声明不受非局部引用的 IGNORE_SELF 策略影响。
                    assertSame(copy, copy.function("tracked").symbol.psi?.containingFile)
                    this as CaCfirSession
                }
            }
        }
        assertEquals(4, sessions.map { it.useSiteModule }.toSet().size)
        assertEquals(4, sessions.map { it.resolutionFacade.useSiteCfirSession }.toSet().size)
        analyzeCopy(first, PREFER_SELF) {
            // Analysis 会话仍按模块缓存；LL 会话按副本修改戳复用，二者都不得跨副本串用。
            assertSame(sessions.first().resolutionFacade.useSiteCfirSession, (this as CaCfirSession).resolutionFacade.useSiteCfirSession)
            assertNotSame(sessions.last().resolutionFacade.useSiteCfirSession, (this as CaCfirSession).resolutionFacade.useSiteCfirSession)
        }
        analyzeForTest(mainFile) {
            assertSame(mainFile, mainFile.reference("tracked").resolveToSymbol()?.psi?.containingFile)
        }
        assertTrue(mainFile.text.contains("return 1"))
    }

    /**
     * 修改副本 PSI 后必须替换会话并能按指针恢复声明；原件会话与源码均不受影响。
     *
     * 指针刻意从 IGNORE_SELF 解析到的原件声明建立，用于验证指针落点仍是原件快照。
     *
     * 已知缺口：修改副本 PSI 后，Analysis 会话缓存不会失效，
     * 会话与指针都会继续复用旧快照。Kotlin 为此让不稳定副本绕过上层缓存，
     * 但仓颉侧绕过会让 LL 的 not-null 上下文缓存拿不到值，因此该修复必须落在
     * LL 的副本失效链路上。这里断言现状，修复后改为 assertNotSame。
     */
    @Test
    fun danglingModificationAndPointerRestore(mainFile: CjFile) {
        val originalText = mainFile.text
        val copy = readAction { copyOf(mainFile) }
        val originalSession = readAction { analyzeForTest(mainFile) { this as CaCfirSession } }
        val (previousSession, pointer) = readAction {
            analyzeCopy(copy, IGNORE_SELF) {
                assertFalse((useSiteModule as CaDanglingFileModule).isStable)
                val symbol = requireNotNull(copy.reference("tracked").resolveToSymbol())
                (this as CaCfirSession) to symbol.createPointer()
            }
        }
        val previousStamp = copy.modificationStamp
        ApplicationManager.getApplication().runWriteAction {
            val literal = requireNotNull(PsiTreeUtil.findChildOfType(copy.function("tracked"), CjConstantExpression::class.java))
            literal.replace(CjPsiFactory(mainFile.project).createExpression("3"))
        }
        assertNotEquals(previousStamp, copy.modificationStamp)
        readAction {
            analyzeCopy(copy, IGNORE_SELF) {
                // 副本 PSI 改动当前不触发会话失效；修复后这里应改为 assertNotSame。
                assertSame(previousSession, this)
                val restored = restoreSymbol(pointer)
                assertNotNull(restored)
                // 指针落在原件快照上：副本文本变化尚未传播到会话缓存。
                assertSame(mainFile.function("tracked"), restored!!.psi)
            }
            analyzeForTest(mainFile) {
                assertSame(originalSession, this)
                assertTrue(originalSession.token.isValid())
                assertSame(mainFile, mainFile.reference("tracked").resolveToSymbol()?.psi?.containingFile)
            }
        }
        assertEquals(originalText, mainFile.text)
    }

    /**
     * 由当前文档文本构造的补全副本在两种模式下都必须可用，且不修改原文件。
     *
     * PREFER_SELF 下副本自身顶层声明可解析，IGNORE_SELF 下解析到原件。
     * 该环境的文档修改会被 PSI 同步提交，因此无法构造真正的「未提交」状态，
     * 这里验证的是「副本文本来自当前文档」以及 `analyzeCopy` 不回写原文件。
     */
    @Test
    fun danglingUncommittedDocument(mainFile: CjFile) {
        val manager = PsiDocumentManager.getInstance(mainFile.project)
        val document = requireNotNull(manager.getDocument(mainFile))
        val originalText = mainFile.text
        try {
            // 平台要求文档写入发生在 EDT，且必须包在命令动作内；副本分析本身仍在读动作内执行。
            ApplicationManager.getApplication().invokeAndWait {
                WriteCommandAction.runWriteCommandAction(mainFile.project) {
                    val offset = document.text.indexOf("return 1") + "return ".length
                    document.replaceString(offset, offset + 1, "3")
                }
            }
            readAction {
                val currentText = mainFile.text
                assertTrue(currentText.contains("return 3"), "文档修改应已同步到原文件 PSI")
                val copy = copyFromText(mainFile, currentText)
                assertEquals(currentText, copy.text, "副本必须来自当前文档文本")
                analyzeCopy(copy, PREFER_SELF) {
                    assertSame(copy, copy.reference("tracked").resolveToSymbol()?.psi?.containingFile, "PREFER_SELF 必须解析到副本顶层声明")
                    assertLocalFromCopy(copy)
                }
                analyzeCopy(copy, IGNORE_SELF) {
                    val target = requireNotNull(copy.reference("tracked").resolveToSymbol())
                    assertSame(mainFile, target.psi?.containingFile, "IGNORE_SELF 必须解析到原件文件")
                }
            }
            assertTrue(originalText.contains("return 1"), "用例结束后须在 finally 中恢复原文档")
        } finally {
            ApplicationManager.getApplication().invokeAndWait {
                WriteCommandAction.runWriteCommandAction(mainFile.project) {
                    document.setText(originalText)
                    manager.commitDocument(document)
                }
            }
        }
    }

    /** 拒绝没有原件的文件及显式模块冲突，并兼容 ORIGINAL_FILE 来源。 */
    @Test
    fun danglingInputContract(mainFile: CjFile) = readAction {
        assertNull(mainFile.copyOrigin)
        assertThrows(IllegalArgumentException::class.java) { analyzeCopy(mainFile, PREFER_SELF) {} }
        val orphan = CjPsiFactory(mainFile.project).createFile("orphan.cj", mainFile.text)
        assertTrue(orphan.isDangling)
        assertThrows(IllegalArgumentException::class.java) { analyzeCopy(orphan, PREFER_SELF) {} }
        orphan.putUserData(PsiFileFactory.ORIGINAL_FILE, orphan)
        assertNull(orphan.copyOrigin)
        orphan.putUserData(PsiFileFactory.ORIGINAL_FILE, mainFile)
        assertSame(mainFile, orphan.copyOrigin)
        analyzeCopy(orphan, IGNORE_SELF) {
            assertMode(orphan, IGNORE_SELF)
            assertSame(mainFile, orphan.reference("tracked").resolveToSymbol()?.psi?.containingFile)
        }

        val provider = CangJieProjectStructureProvider.getInstance(mainFile.project)
        val module = CaDanglingFileModuleImpl(listOf(orphan), provider.getModule(mainFile, null), IGNORE_SELF)
        orphan.explicitModule = module
        assertSame(module, provider.getModule(orphan, null))
        assertThrows(IllegalArgumentException::class.java) { analyzeCopy(orphan, IGNORE_SELF) {} }
        assertThrows(IllegalArgumentException::class.java) { analyzeCopy(orphan, PREFER_SELF) {} }
        assertNull(orphan.danglingFileResolutionMode)
        orphan.explicitModule = null
        analyzeCopy(orphan, PREFER_SELF) {
            assertThrows(IllegalArgumentException::class.java) { orphan.explicitModule = module }
            assertMode(orphan, PREFER_SELF)
        }
        assertNull(orphan.explicitModule)
        assertNull(orphan.danglingFileResolutionMode)
    }

    /**
     * 复制物理文件副本：PSI 层 `copy()` 会自动登记 `ORIGINAL_FILE`，是仓库既有的 dangling 文件入口。
     */
    private fun copyOf(original: CjFile): CjFile = original.copy() as CjFile

    /**
     * 复制后把 `tracked` 的返回字面量改成 [returned]，用于区分两份副本的会话身份。
     */
    private fun copyReturning(original: CjFile, returned: String): CjFile = copyOf(original).also { copy ->
        ApplicationManager.getApplication().runWriteAction {
            val literal = requireNotNull(
                PsiTreeUtil.findChildOfType(copy.function("tracked"), CjConstantExpression::class.java)
            )
            literal.replace(CjPsiFactory(original.project).createExpression(returned))
        }
    }

    /**
     * 由指定文本构造登记了原件的游离文件，用于未提交文档场景。
     */
    private fun copyFromText(original: CjFile, text: String): CjFile =
        (PsiFileFactory.getInstance(original.project)
            .createFileFromText(original.name, original.fileType, text) as CjFile)
            .apply { putUserData(PsiFileFactory.ORIGINAL_FILE, original) }

    private fun CaSession.assertMode(file: CjFile, mode: CaDanglingFileResolutionMode) {
        assertEquals(mode, file.danglingFileResolutionMode)
        val module = useSiteModule as CaDanglingFileModule
        assertEquals(mode, module.resolutionMode)
        assertSame(file, module.files.single())
    }

    private fun CaSession.assertLocalFromCopy(file: CjFile) {
        val local = requireNotNull(file.reference("localValue").resolveToSymbol())
        assertSame(file, local.psi?.containingFile)
    }

    private fun CjFile.function(name: String): CjNamedFunction =
        PsiTreeUtil.findChildrenOfType(this, CjNamedFunction::class.java).single { it.name == name }

    private fun CjFile.reference(name: String): CjSimpleNameExpression =
        PsiTreeUtil.findChildrenOfType(this, CjSimpleNameExpression::class.java)
            .last {
                it.referencedName == name && it !is CjBindingPattern &&
                    it.parent !is CjNamedPattern && it.parent !is CjVarOrEnumPattern
            }

    private fun <R> readAction(action: () -> R): R =
        ApplicationManager.getApplication().runReadAction(Computable { action() })
}
