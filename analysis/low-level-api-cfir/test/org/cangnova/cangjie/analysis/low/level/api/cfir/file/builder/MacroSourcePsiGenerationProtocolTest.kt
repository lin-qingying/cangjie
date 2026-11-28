@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.impl.FileManagerImpl
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieFileBasedDeclarationProvider
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals as expectEquals
import org.junit.jupiter.api.Assertions.assertFalse as expectFalse
import org.junit.jupiter.api.Assertions.assertThrows as expectThrows
import org.junit.jupiter.api.Assertions.assertNotSame as expectNotSame
import org.junit.jupiter.api.Assertions.assertSame as expectSame
import org.junit.jupiter.api.Assertions.assertTrue as expectTrue

/** 直接使用平台 PSI/file manager 的 headless 协议，不以 IDE fixture 或合成 valid 标记代替 provider 退役。 */
class MacroSourcePsiGenerationProtocolTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun droppingCachesDoesNotInventANewPsiGeneration() {
        val file = source("unchanged", 42)
        val virtualFile = requireNotNull(file.virtualFile)
        val bytes = virtualFile.contentsToByteArray()
        val provider = file.viewProvider
        val lease = capture(file)
        ApplicationManager.getApplication().runWriteAction { PsiManager.getInstance(project).dropPsiCaches() }
        ReadAction.run<RuntimeException> {
            expectSame(file, current(virtualFile))
            expectSame(provider, file.viewProvider)
            expectTrue(bytes.contentEquals(virtualFile.contentsToByteArray()))
            lease.requireCurrent()
        }
    }

    @Test
    fun realProviderReloadRevokesTheLeaseBeforeOldIdentifierAccess() {
        val file = source("reload", 42)
        val virtualFile = requireNotNull(file.virtualFile)
        val provider = file.viewProvider
        val bytes = virtualFile.contentsToByteArray()
        val leaf = requireNotNull((file.declarations.single() as CjNamedFunction).nameIdentifier)
        val declarationProvider = CangJieFileBasedDeclarationProvider(file)
        val lease = capture(file)
        reload(virtualFile)
        ReadAction.run<RuntimeException> {
            val fresh = current(virtualFile)
            expectNotSame(file, fresh)
            expectNotSame(provider, fresh.viewProvider)
            expectSame(fresh, declarationProvider.cangjieFile)
            expectTrue(fresh.isValid)
            expectTrue(bytes.contentEquals(virtualFile.contentsToByteArray()))
            var leafWasRead = false
            expectThrows(LLCfirSourcePsiInvalidatedException::class.java) {
                lease.requireCurrent()
                leafWasRead = true
                leaf.text
            }
            expectFalse(leafWasRead)
            expectTrue(capture(fresh).isCurrent())
        }
    }

    @Test
    fun reloadingOnlyTheSecondFileRevokesTheAlreadyReadCohort() {
        val first = source("first", 40)
        val second = source("second", 2)
        val firstFile = requireNotNull(first.virtualFile)
        val secondFile = requireNotNull(second.virtualFile)
        val firstProvider = first.viewProvider
        val secondProvider = second.viewProvider
        val firstBytes = firstFile.contentsToByteArray()
        val secondBytes = secondFile.contentsToByteArray()
        val secondLeaf = requireNotNull((second.declarations.single() as CjNamedFunction).nameIdentifier)
        val cohort = capture(first, second)
        ReadAction.run<RuntimeException> {
            cohort.requireCurrent()
            expectTrue(first.text.contains("return 40"))
        }
        reload(secondFile)
        ReadAction.run<RuntimeException> {
            expectSame(first, current(firstFile))
            expectSame(firstProvider, first.viewProvider)
            val freshSecond = current(secondFile)
            expectNotSame(second, freshSecond)
            expectNotSame(secondProvider, freshSecond.viewProvider)
            expectTrue(firstBytes.contentEquals(firstFile.contentsToByteArray()))
            expectTrue(secondBytes.contentEquals(secondFile.contentsToByteArray()))
            var leafWasRead = false
            expectThrows(LLCfirSourcePsiInvalidatedException::class.java) {
                cohort.requireCurrent()
                leafWasRead = true
                secondLeaf.text
            }
            expectFalse(leafWasRead)
            expectTrue(capture(first, freshSecond).isCurrent())
        }
    }

    @Test
    fun identicalNonPhysicalCopiesKeepDistinctPsiOwners() {
        val physical = source("copies", 42)
        val first = physical.copy() as CjFile
        val second = physical.copy() as CjFile
        expectFalse(first.isPhysical || second.isPhysical)
        expectNotSame(first, second)
        expectEquals(first.text, second.text)
        val firstProvider = CangJieFileBasedDeclarationProvider(first)
        val secondProvider = CangJieFileBasedDeclarationProvider(second)
        val cohort = capture(first, second)
        ReadAction.run<RuntimeException> {
            expectSame(first, firstProvider.cangjieFile)
            expectSame(second, secondProvider.cangjieFile)
            expectSame(first, cohort.files[0])
            expectSame(second, cohort.files[1])
            cohort.requireCurrent()
        }
    }

    private fun source(name: String, value: Int): CjFile {
        val file = createPsiFile(name, "package sourcegeneration\nfunc target(): Int64 { return $value }") as CjFile
        expectTrue(file.isPhysical)
        ReadAction.run<RuntimeException> { expectSame(file, current(requireNotNull(file.virtualFile))) }
        return file
    }

    private fun current(file: VirtualFile): CjFile = requireNotNull(PsiManager.getInstance(project).findFile(file)) as CjFile

    private fun capture(vararg files: CjFile): LLCfirSourcePsiGeneration =
        ReadAction.compute<LLCfirSourcePsiGeneration, RuntimeException> {
            LLCfirSourcePsiGeneration.capture(files.toList())
        }

    /** 使用实际平台 file manager 撤销 provider，保留 VirtualFile 和源码 bytes。 */
    private fun reload(file: VirtualFile) {
        ApplicationManager.getApplication().runWriteAction {
            val fileManager = (PsiManager.getInstance(project) as PsiManagerImpl).fileManager as FileManagerImpl
            fileManager.forceReload(file)
        }
    }
}
