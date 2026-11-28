package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.platform.modification.publishGlobalModuleStateModificationEvent
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionSuccess
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.CjNamedPattern
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTypePattern
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.psi.CjWildcardPattern
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * source pointer 的显式失效验收：先建立 pointer，再失效真实 LL 会话，最后只查询当前会话。
 *
 * 使用正式 IDE-mode registrar 的 read-action token，直接追踪 LL validity；
 * 这是无界面 Analysis API 测试宿主，不连接或启动 IDEA。
 * 规范节点由 fixture marker 或保留的 PSI 引用确定，不通过名字或 offset 重找身份。
 */
class PatternBindingSourcePointerInvalidationTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/patternBindingPointerInvalidation",
) {
    override val configurator = CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(
            frontend = FrontendKind.Cfir,
            moduleKind = TestModuleKind.Source,
            analysisSessionMode = AnalysisSessionMode.Normal,
            analysisApiMode = AnalysisApiMode.Ide,
        ),
    )

    /** 全局嵌套 tuple 的每个叶子均恢复当前 CFIR，且各叶子身份仍彼此独立。 */
    @Test
    fun globalTupleAfterClearCaches(mainFile: CjFile, testServices: TestServices) {
        val patterns = listOf("number", "flag", "tail").map {
            marked<CjBindingPattern>(mainFile, testServices, it)
        }
        val uses = listOf("numberUse", "flagUse", "tailUse").map {
            marked<CjSimpleNameExpression>(mainFile, testServices, it)
        }
        val kinds = listOf(PrimitiveTypeKind.INT64, PrimitiveTypeKind.BOOLEAN, PrimitiveTypeKind.INT64)
        val owner = requireNotNull(PsiTreeUtil.getParentOfType(patterns.first(), CjPatternVariable::class.java))
        val previous = readAction {
            analyzeForTest(mainFile) {
                val entries = owner.symbol.entries
                assertEquals(3, entries.size)
                val symbols = patterns.mapIndexed { index, pattern ->
                    currentPrimitiveBinding(pattern, uses[index], kinds[index], CaSymbolLocation.TOP_LEVEL).also {
                        assertSame(it.cfirIdentity(), entries[index].cfirIdentity())
                        assertSame(pattern, entries[index].psi)
                    }
                }
                assertDistinctCfir(symbols)
                (this as CaCfirSession).snapshot(symbols)
            }
        }

        clearCaches(mainFile.project)

        readAction {
            analyzeForTest(mainFile) {
                assertFresh(previous)
                val entries = owner.symbol.entries
                assertEquals(3, entries.size)
                val restored = patterns.mapIndexed { index, pattern ->
                    val current = currentPrimitiveBinding(pattern, uses[index], kinds[index], CaSymbolLocation.TOP_LEVEL)
                    val result = restoreCurrent(previous, index, pattern, current)
                    assertSame(result.cfirIdentity(), entries[index].cfirIdentity())
                    assertSame(pattern, entries[index].psi)
                    result
                }
                assertDistinctCfir(restored)
            }
        }
    }

    /** 泛型 enum 的嵌套 payload 逐叶投影；全局容器、使用处与 fresh pointer 必须是同一当前 CFIR。 */
    @Test
    fun globalEnumPayloadAfterClearCaches(mainFile: CjFile, testServices: TestServices) {
        val patterns = listOf("serial", "flag", "tail").map {
            marked<CjBindingPattern>(mainFile, testServices, it)
        }
        val uses = listOf("serialUse", "flagUse", "tailUse").map {
            marked<CjSimpleNameExpression>(mainFile, testServices, it)
        }
        val kinds = listOf(PrimitiveTypeKind.INT64, PrimitiveTypeKind.BOOLEAN, PrimitiveTypeKind.INT64)
        val owner = requireNotNull(PsiTreeUtil.getParentOfType(patterns.first(), CjPatternVariable::class.java))
        val previous = readAction {
            analyzeForTest(mainFile) {
                val entries = owner.symbol.entries
                assertEquals(3, entries.size)
                val symbols = patterns.mapIndexed { index, pattern ->
                    currentPrimitiveBinding(pattern, uses[index], kinds[index], CaSymbolLocation.TOP_LEVEL).also {
                        assertSame(it.cfirIdentity(), entries[index].cfirIdentity())
                        assertSame(pattern, entries[index].psi)
                    }
                }
                assertDistinctCfir(symbols)
                (this as CaCfirSession).snapshot(symbols)
            }
        }
        clearCaches(mainFile.project)
        readAction {
            analyzeForTest(mainFile) {
                assertFresh(previous)
                val entries = owner.symbol.entries
                assertEquals(3, entries.size)
                val restored = patterns.mapIndexed { index, pattern ->
                    val current = currentPrimitiveBinding(pattern, uses[index], kinds[index], CaSymbolLocation.TOP_LEVEL)
                    restoreCurrent(previous, index, pattern, current).also {
                        assertSame(it.cfirIdentity(), entries[index].cfirIdentity())
                        assertSame(pattern, entries[index].psi)
                    }
                }
                assertDistinctCfir(restored)
            }
        }
    }

    /** case 中的 payload 是语义确认的 VarOrEnum 绑定；同一 enum 的 Empty 仍只引用 constructor。 */
    @Test
    fun caseEnumPayloadAfterClearCaches(mainFile: CjFile, testServices: TestServices) {
        val patterns = listOf("serial", "flag", "tail").map {
            marked<CjVarOrEnumPattern>(mainFile, testServices, it)
        }
        val uses = listOf("serialUse", "flagUse", "tailUse").map {
            marked<CjSimpleNameExpression>(mainFile, testServices, it)
        }
        val kinds = listOf(PrimitiveTypeKind.INT64, PrimitiveTypeKind.BOOLEAN, PrimitiveTypeKind.INT64)
        val empty = marked<CjVarOrEnumPattern>(mainFile, testServices, "emptyPattern")
        val constructor = marked<CjEnumConstructor>(mainFile, testServices, "emptyDeclaration")
        val reference = requireNotNull(empty.reference)
        val previous = readAction {
            analyzeForTest(mainFile) {
                val symbols = patterns.mapIndexed { index, pattern ->
                    currentPrimitiveBinding(pattern, uses[index], kinds[index], CaSymbolLocation.LOCAL).also {
                        assertNull(it.callableId)
                    }
                }
                assertDistinctCfir(symbols)
                assertNull(empty.bindingSymbol)
                val resolved = requireNotNull(reference.resolveToSymbol())
                assertTrue(resolved is CaEnumConstructorSymbol)
                assertSame(constructor, resolved.psi)
                assertSame(constructor.symbol.cfirIdentity(), resolved.cfirIdentity())
                (this as CaCfirSession).snapshot(symbols)
            }
        }
        clearCaches(mainFile.project)
        readAction {
            analyzeForTest(mainFile) {
                assertFresh(previous)
                val restored = patterns.mapIndexed { index, pattern ->
                    val current = currentPrimitiveBinding(pattern, uses[index], kinds[index], CaSymbolLocation.LOCAL)
                    restoreCurrent(previous, index, pattern, current).also { assertNull(it.callableId) }
                }
                assertDistinctCfir(restored)
                assertNull(empty.bindingSymbol)
                val resolved = requireNotNull(reference.resolveToSymbol())
                assertTrue(resolved is CaEnumConstructorSymbol)
                assertSame(token, resolved.token)
                assertSame(constructor, resolved.psi)
                assertSame(constructor.symbol.cfirIdentity(), resolved.cfirIdentity())
            }
        }
    }

    /** TypePattern pointer 恢复完整规范节点及当前显式类型，旧 token 只用于验证失效。 */
    @Test
    fun typeAfterClearCaches(mainFile: CjFile, testServices: TestServices) {
        val pattern = marked<CjTypePattern>(mainFile, testServices, "typed")
        val use = marked<CjSimpleNameExpression>(mainFile, testServices, "typedUse")
        val previous = readAction {
            analyzeForTest(mainFile) {
                val current = currentPrimitiveBinding(pattern, use, PrimitiveTypeKind.INT64, CaSymbolLocation.LOCAL)
                assertTrue(current.returnType.semanticallyEquals(requireNotNull(pattern.typeReference).type))
                (this as CaCfirSession).snapshot(listOf(current))
            }
        }

        clearCaches(mainFile.project)

        readAction {
            analyzeForTest(mainFile) {
                assertFresh(previous)
                val current = currentPrimitiveBinding(pattern, use, PrimitiveTypeKind.INT64, CaSymbolLocation.LOCAL)
                val restored = restoreCurrent(previous, 0, pattern, current)
                assertEquals("typed: Int64", restored.psi!!.text)
                assertTrue(restored.returnType.semanticallyEquals(requireNotNull(pattern.typeReference).type))
            }
        }
    }

    /** Int64 selector 的 bare VarOrEnum 经过语义确认后仍是 binder，失效恢复必须取得新 CFIR。 */
    @Test
    fun varOrEnumAfterClearCaches(mainFile: CjFile, testServices: TestServices) {
        val pattern = marked<CjVarOrEnumPattern>(mainFile, testServices, "captured")
        val use = marked<CjSimpleNameExpression>(mainFile, testServices, "capturedUse")
        val previous = readAction {
            analyzeForTest(mainFile) {
                (this as CaCfirSession).snapshot(listOf(
                    currentPrimitiveBinding(pattern, use, PrimitiveTypeKind.INT64, CaSymbolLocation.LOCAL),
                ))
            }
        }

        clearCaches(mainFile.project)

        readAction {
            analyzeForTest(mainFile) {
                assertFresh(previous)
                val current = currentPrimitiveBinding(pattern, use, PrimitiveTypeKind.INT64, CaSymbolLocation.LOCAL)
                restoreCurrent(previous, 0, pattern, current)
            }
        }
    }

    /**
     * 只在 selector 对应的 enum 中增加同名 constructor：pattern 和引用 PSI 本身保持有效且为原节点。
     * pointer 不能把仍存在的旧规范 PSI 当作仍存在的 binder，也不能恢复成 constructor。
     */
    @Test
    fun bareBinderBecomesConstructor(mainFile: CjFile, testServices: TestServices) {
        val pattern = marked<CjVarOrEnumPattern>(mainFile, testServices, "flip")
        val reference = requireNotNull(pattern.reference)
        val enumPsi = PsiTreeUtil.findChildrenOfType(mainFile, CjEnum::class.java).single()
        val originalPatternText = pattern.text
        val psiPointer = readAction { SmartPointerManager.getInstance(mainFile.project).createSmartPsiElementPointer(pattern) }
        val previous = readAction {
            analyzeForTest(mainFile) {
                val binding = requireNotNull(pattern.bindingSymbol)
                assertSame(pattern, binding.psi)
                assertSame(token, binding.token)
                assertEquals(CaSymbolLocation.LOCAL, binding.location)
                assertTrue(binding.isLet)
                assertFalse(binding.isConst)
                val type = binding.returnType
                assertTrue(type is CaUsualClassType)
                assertSame(enumPsi.symbol.cfirIdentity(), requireNotNull((type as CaUsualClassType).symbol).cfirIdentity())
                val declarationResolution = pattern.tryResolveSymbols()
                assertTrue(declarationResolution is CaSymbolResolutionSuccess)
                assertSame(binding.cfirIdentity(), (declarationResolution as CaSymbolResolutionSuccess).symbols.single().cfirIdentity())
                (this as CaCfirSession).snapshot(listOf(binding))
            }
        }

        editAndInvalidate(mainFile.project) {
            // 只替换枚举 body，新增项在同一 enum 内；match 所在函数和 pattern 均未改动。
            val replacement = CjPsiFactory(mainFile.project).createDeclaration<CjEnum>(
                "enum Choice { | Seed | Captured }",
            )
            requireNotNull(enumPsi.body).replace(requireNotNull(replacement.body))
        }

        readAction {
            assertTrue(pattern.isValid)
            assertTrue(reference.isValid)
            assertSame(pattern, psiPointer.element)
            assertSame(reference, pattern.reference)
            assertEquals(originalPatternText, pattern.text)
            analyzeForTest(mainFile) {
                assertFresh(previous)
                assertNull(pattern.bindingSymbol, "加入同名 entry 后，原 VarOrEnum 节点应为 constructor 使用。")
                val declaration = enumPsi.constructor.last()
                assertEquals("Captured", declaration.name)
                val resolved = requireNotNull(reference.resolveToSymbol())
                assertTrue(resolved is CaEnumConstructorSymbol)
                assertSame(token, resolved.token)
                assertSame(declaration, resolved.psi)
                assertSame(declaration.symbol.cfirIdentity(), resolved.cfirIdentity())
                assertNull(restoreSymbol(previous.pointers.single()), "旧 binder pointer 不得跨符号族恢复为 constructor。")
                assertNull(pattern.bindingSymbol)
            }
        }
    }

    /** bare binder 改为 wildcard；保留原 case 宿主，当前通配符没有可发布的绑定。 */
    @Test
    fun bareBinderBecomesWildcard(mainFile: CjFile, testServices: TestServices) {
        val pattern = marked<CjVarOrEnumPattern>(mainFile, testServices, "erased")
        val entry = requireNotNull(PsiTreeUtil.getParentOfType(pattern, CjMatchEntry::class.java))
        val previous = readAction {
            analyzeForTest(mainFile) {
                val binding = requireNotNull(pattern.bindingSymbol)
                assertPrimitive(binding, PrimitiveTypeKind.INT64)
                assertSame(pattern, binding.psi)
                (this as CaCfirSession).snapshot(listOf(binding))
            }
        }

        editAndInvalidate(mainFile.project) {
            val expression = CjPsiFactory(mainFile.project).createExpression("match (1) { case _ => 1 }")
            val wildcard = PsiTreeUtil.findChildrenOfType(expression, CjWildcardPattern::class.java).single()
            pattern.replace(wildcard)
        }

        readAction {
            assertTrue(entry.isValid)
            val wildcard = PsiTreeUtil.findChildrenOfType(entry, CjWildcardPattern::class.java).single()
            assertEquals("_", wildcard.text)
            assertTrue(PsiTreeUtil.findChildrenOfType(entry, CjNamedPattern::class.java).isEmpty())
            analyzeForTest(mainFile) {
                assertFresh(previous)
                assertNull(wildcard.tryResolveSymbols())
                assertNull(restoreSymbol(previous.pointers.single()))
            }
        }
    }

    /** bare VarOrEnum binder 改成 _: Int64，当前 TypePattern 可查询但无绑定。 */
    @Test
    fun bareBinderBecomesTypedWildcard(mainFile: CjFile, testServices: TestServices) {
        val pattern = marked<CjVarOrEnumPattern>(mainFile, testServices, "erased")
        val entry = requireNotNull(PsiTreeUtil.getParentOfType(pattern, CjMatchEntry::class.java))
        val previous = readAction {
            analyzeForTest(mainFile) {
                val binding = requireNotNull(pattern.bindingSymbol)
                assertPrimitive(binding, PrimitiveTypeKind.INT64)
                assertSame(pattern, binding.psi)
                (this as CaCfirSession).snapshot(listOf(binding))
            }
        }

        editAndInvalidate(mainFile.project) {
            val expression = CjPsiFactory(mainFile.project).createExpression("match (1) { case _: Int64 => 1 }")
            val replacement = PsiTreeUtil.findChildrenOfType(expression, CjTypePattern::class.java).single()
            pattern.replace(replacement)
        }

        readAction {
            assertTrue(entry.isValid)
            val current = PsiTreeUtil.findChildrenOfType(entry, CjTypePattern::class.java).single()
            assertEquals("_: Int64", current.text)
            analyzeForTest(mainFile) {
                assertFresh(previous)
                assertNull(current.bindingSymbol)
                val type = requireNotNull(current.typeReference).type
                assertTrue(type is CaPrimitiveType)
                assertEquals(PrimitiveTypeKind.INT64, (type as CaPrimitiveType).kind)
                assertNull(restoreSymbol(previous.pointers.single()))
                assertNull(current.bindingSymbol)
            }
        }
    }

    /** TypePattern 内把 binder 子节点替换为下划线；完整规范 TypePattern 本身必须保留。 */
    @Test
    fun typeBinderBecomesTypedWildcard(mainFile: CjFile, testServices: TestServices) {
        val pattern = marked<CjTypePattern>(mainFile, testServices, "erasedType")
        val previous = readAction {
            analyzeForTest(mainFile) {
                val binding = requireNotNull(pattern.bindingSymbol)
                assertPrimitive(binding, PrimitiveTypeKind.INT64)
                assertSame(pattern, binding.psi)
                (this as CaCfirSession).snapshot(listOf(binding))
            }
        }
        val psiPointer = readAction { SmartPointerManager.getInstance(mainFile.project).createSmartPsiElementPointer(pattern) }

        editAndInvalidate(mainFile.project) {
            val expression = CjPsiFactory(mainFile.project).createExpression("match (1) { case _: Int64 => 1 }")
            val replacement = PsiTreeUtil.findChildrenOfType(expression, CjTypePattern::class.java).single()
            requireNotNull(pattern.reference).replace(requireNotNull(replacement.identifierElement))
        }

        readAction {
            assertTrue(pattern.isValid)
            assertSame(pattern, psiPointer.element)
            assertEquals("_: Int64", pattern.text)
            analyzeForTest(mainFile) {
                assertFresh(previous)
                assertNull(pattern.bindingSymbol, "保留的 TypePattern 已经是 _: Int64，不能发布 binder。")
                val type = requireNotNull(pattern.typeReference).type
                assertTrue(type is CaPrimitiveType)
                assertEquals(PrimitiveTypeKind.INT64, (type as CaPrimitiveType).kind)
                assertNull(restoreSymbol(previous.pointers.single()))
                assertNull(pattern.bindingSymbol)
            }
        }
    }

    /** 删除整条全局 tuple 声明后，两叶子 pointer 均为 null；同名局部叶子不能替代被删除身份。 */
    @Test
    fun deletedDeclarationDoesNotRestore(mainFile: CjFile, testServices: TestServices) {
        val deleted = listOf("doomed", "spare").map { marked<CjBindingPattern>(mainFile, testServices, it) }
        val survivors = listOf("shadowDoomed", "shadowSpare").map { marked<CjBindingPattern>(mainFile, testServices, it) }
        val owner = requireNotNull(PsiTreeUtil.getParentOfType(deleted.first(), CjPatternVariable::class.java))
        val survivorOwner = requireNotNull(PsiTreeUtil.getParentOfType(survivors.first(), CjPatternVariable::class.java))
        val previous = readAction {
            analyzeForTest(mainFile) {
                val bindings = deleted.map { pattern ->
                    requireNotNull(pattern.bindingSymbol).also {
                        assertSame(pattern, it.psi)
                        assertPrimitive(it, PrimitiveTypeKind.INT64)
                        assertEquals(CaSymbolLocation.TOP_LEVEL, it.location)
                    }
                }
                (this as CaCfirSession).snapshot(bindings)
            }
        }

        editAndInvalidate(mainFile.project) { owner.delete() }

        readAction {
            assertFalse(owner.isValid)
            deleted.forEach { assertFalse(it.isValid) }
            assertSame(survivorOwner, PsiTreeUtil.findChildrenOfType(mainFile, CjPatternVariable::class.java).single())
            analyzeForTest(mainFile) {
                assertFresh(previous)
                survivors.forEach { pattern ->
                    val current = requireNotNull(pattern.bindingSymbol)
                    assertSame(pattern, current.psi)
                    assertSame(token, current.token)
                    assertPrimitive(current, PrimitiveTypeKind.INT64)
                    assertEquals(CaSymbolLocation.LOCAL, current.location)
                    previous.cfirSymbols.forEach { assertNotSame(it, current.cfirIdentity()) }
                }
                previous.pointers.forEach { assertNull(restoreSymbol(it)) }
            }
        }
    }

    /** 只冻结失效后可以比较的引用与 pointer；不把旧公开 symbol/type 带入当前断言。 */
    private data class Snapshot(
        val session: CaCfirSession,
        val llSession: LLCfirSession,
        val module: CaModule,
        val token: CaLifetimeToken,
        val cfirSymbols: List<CfirBasedSymbol<*>>,
        val pointers: List<CaSymbolPointer<CaCallableSymbol>>,
    )

    private fun CaCfirSession.snapshot(symbols: List<CaPatternBindingSymbol>): Snapshot {
        assertTrue(token.isValid())
        val llSession = resolutionFacade.useSiteCfirSession
        assertTrue(llSession.isValid)
        symbols.forEach { assertSame(token, it.token) }
        return Snapshot(this, llSession, useSiteModule, token, symbols.map { it.cfirIdentity() }, symbols.map { it.createPointer() })
    }

    /** 验证 API session、LL session 和 token 都已更新，且旧 LL/token 已失效。 */
    private fun CaSession.assertFresh(previous: Snapshot) {
        val current = this as CaCfirSession
        assertFalse(previous.llSession.isValid)
        assertFalse(previous.token.isValid())
        assertNotSame(previous.session, current)
        assertNotSame(previous.llSession, current.resolutionFacade.useSiteCfirSession)
        assertTrue(current.resolutionFacade.useSiteCfirSession.isValid)
        assertSame(previous.module, useSiteModule)
        assertNotSame(previous.token, token)
        assertTrue(token.isValid())
    }

    /** 直接比底层符号引用，防止 PSI-based wrapper equality 遮蔽旧 CFIR 或错误叶子。 */
    private fun CaSession.restoreCurrent(
        previous: Snapshot,
        index: Int,
        pattern: CjNamedPattern,
        current: CaPatternBindingSymbol,
    ): CaPatternBindingSymbol {
        val restored = restoreSymbol(previous.pointers[index])
        assertTrue(restored is CaPatternBindingSymbol)
        val binding = restored as CaPatternBindingSymbol
        assertSame(token, binding.token)
        assertSame(pattern, binding.psi)
        assertSame(current.cfirIdentity(), binding.cfirIdentity())
        assertNotSame(previous.cfirSymbols[index], binding.cfirIdentity())
        assertSame(current.cfirIdentity(), requireNotNull(pattern.bindingSymbol).cfirIdentity())
        assertTrue(binding.returnType.semanticallyEquals(current.returnType))
        return binding
    }

    private fun CaSession.currentPrimitiveBinding(
        pattern: CjNamedPattern,
        use: CjSimpleNameExpression,
        kind: PrimitiveTypeKind,
        location: CaSymbolLocation,
    ): CaPatternBindingSymbol {
        val current = requireNotNull(pattern.bindingSymbol)
        val resolved = requireNotNull(use.resolveToSymbol())
        assertTrue(resolved is CaPatternBindingSymbol)
        assertSame(pattern, current.psi)
        assertSame(pattern, resolved.psi)
        assertSame(token, current.token)
        assertSame(token, resolved.token)
        assertSame(current.cfirIdentity(), resolved.cfirIdentity())
        assertEquals(location, current.location)
        assertTrue(current.isLet)
        assertFalse(current.isConst)
        assertPrimitive(current, kind)
        assertTrue(current.returnType.semanticallyEquals(requireNotNull(use.expressionType)))
        return current
    }

    private fun assertPrimitive(symbol: CaPatternBindingSymbol, kind: PrimitiveTypeKind) {
        val type = symbol.returnType
        assertTrue(type is CaPrimitiveType)
        assertEquals(kind, (type as CaPrimitiveType).kind)
    }

    private fun CaSymbol.cfirIdentity(): CfirBasedSymbol<*> = (this as CaCfirSymbol<*>).cfirSymbol

    private fun assertDistinctCfir(symbols: List<CaPatternBindingSymbol>) {
        for (index in symbols.indices) {
            for (other in index + 1 until symbols.size) {
                assertNotSame(symbols[index].cfirIdentity(), symbols[other].cfirIdentity())
            }
        }
    }

    private inline fun <reified T : PsiElement> marked(file: CjFile, services: TestServices, marker: String): T =
        services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<T>(file, marker)

    private fun <R> readAction(action: () -> R): R =
        ApplicationManager.getApplication().runReadAction(Computable { action() })
}

/** 对齐 Kotlin firTestUtils.clearCaches：写动作发布全局模块事件，真正失效 LL 并逐出 API 缓存。 */
private fun clearCaches(project: Project) {
    ApplicationManager.getApplication().runWriteAction {
        project.publishGlobalModuleStateModificationEvent()
    }
}

/** 正式 PSI 写入与同一写动作内的失效发布；分析仅在随后新的读动作中进行。 */
private fun editAndInvalidate(project: Project, edit: () -> Unit) {
    WriteCommandAction.runWriteCommandAction(project) {
        edit()
        project.publishGlobalModuleStateModificationEvent()
    }
}
