@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.declarations.ResolveStateAccess::class,
    org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals::class,
)

package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.platform.modification.publishGlobalModuleStateModificationEvent
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjCommandTypePattern
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 复用 HostBindingSymbolTest 的真实 fixture/marker/局部符号协议；command 使用官方 stdx.effect.Command。
 * SDK 没有该库时正向断言必须失败，不能合成 std.core 类型、跳过测试或接受错误类型。
 */
class CommandBindingSymbolTest : AbstractAnalysisApiExecutionTest("analysis/analysis-api-cfir/testData/hostBindingSymbols") {
    override val configurator = CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(FrontendKind.Cfir, TestModuleKind.Source,
            AnalysisSessionMode.Normal, AnalysisApiMode.Ide),
    )

    @Test
    fun commandsNamedBinding(mainFile: CjFile, services: TestServices) {
        val pattern = element<CjCommandTypePattern>(mainFile, services, "commandDeclaration")
        val use = element<CjSimpleNameExpression>(mainFile, services, "commandUse")
        analyzeForTest(mainFile) {
            requireOfficialCommandLibrary()
            val declaration = requireNotNull(pattern.bindingSymbol)
            val resolved = use.resolveToSymbol()
            assertTrue(resolved is CaLocalVariableSymbol)
            assertHost(declaration, pattern)
            assertSame(pattern, requireNotNull(resolved).psi)
            assertSame(declaration.cfirIdentity(), (resolved as CaLocalVariableSymbol).cfirIdentity())
            assertCommandType(declaration.returnType, PrimitiveTypeKind.INT64)
            assertCommandType(requireNotNull(use.expressionType), PrimitiveTypeKind.INT64)
            assertTrue(mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS).isEmpty())
        }
    }

    /** 首查 actual CFIR binding 的 IMPLICIT_TYPES，不通过声明 API/diagnostics 预热整个函数。 */
    @Test
    fun commandsDirectImplicitType(mainFile: CjFile, services: TestServices) {
        val pattern = element<CjCommandTypePattern>(mainFile, services, "commandDeclaration")
        val use = element<CjSimpleNameExpression>(mainFile, services, "commandUse")
        analyzeForTest(mainFile) {
            requireOfficialCommandLibrary()
            val binding = pattern.getOrBuildCfir((this as CaCfirSession).resolutionFacade)
            assertTrue(binding is CfirPatternBindingVariable)
            val actual = binding as CfirPatternBindingVariable
            actual.symbol.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
            assertTrue(actual.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
            val symbol = requireNotNull(pattern.bindingSymbol)
            assertSame(actual.symbol, symbol.cfirIdentity())
            assertHost(symbol, pattern)
            assertCommandType(symbol.returnType, PrimitiveTypeKind.INT64)
            assertSame(actual.symbol, (use.resolveToSymbol() as CaLocalVariableSymbol).cfirIdentity())
            assertCommandType(requireNotNull(use.expressionType), PrimitiveTypeKind.INT64)
        }
    }

    /** 模块事件必须实际失效 LL/token；pointer 只能恢复当前 CFIR，而非 PSI equality 掩盖的旧对象。 */
    @Test
    fun commandsFreshSessionPointer(mainFile: CjFile, services: TestServices) {
        val pattern = element<CjCommandTypePattern>(mainFile, services, "commandDeclaration")
        val use = element<CjSimpleNameExpression>(mainFile, services, "commandUse")
        lateinit var pointer: CaSymbolPointer<CaCallableSymbol>
        lateinit var oldSession: CaCfirSession
        lateinit var oldLl: org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
        lateinit var oldToken: org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
        lateinit var oldIdentity: org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>
        analyzeForTest(mainFile) {
            requireOfficialCommandLibrary()
            val symbol = requireNotNull(pattern.bindingSymbol)
            assertHost(symbol, pattern)
            assertCommandType(symbol.returnType, PrimitiveTypeKind.INT64)
            oldSession = this as CaCfirSession
            oldLl = oldSession.resolutionFacade.useSiteCfirSession
            oldToken = token
            oldIdentity = symbol.cfirIdentity()
            pointer = symbol.createPointer()
        }
        ApplicationManager.getApplication().runWriteAction {
            mainFile.project.publishGlobalModuleStateModificationEvent()
        }
        assertFalse(oldLl.isValid)
        assertFalse(oldToken.isValid())
        analyzeForTest(use) {
            val currentSession = this as CaCfirSession
            assertNotSame(oldSession, currentSession)
            assertNotSame(oldLl, currentSession.resolutionFacade.useSiteCfirSession)
            assertNotSame(oldToken, token)
            assertTrue(token.isValid())
            requireOfficialCommandLibrary()
            val current = requireNotNull(pattern.bindingSymbol)
            val restored = restoreSymbol(pointer)
            assertTrue(restored is CaLocalVariableSymbol)
            val local = restored as CaLocalVariableSymbol
            assertHost(local, pattern)
            assertCommandType(local.returnType, PrimitiveTypeKind.INT64)
            assertSame(current.cfirIdentity(), local.cfirIdentity())
            assertNotSame(oldIdentity, local.cfirIdentity())
            assertSame(token, local.token)
            assertSame(current.cfirIdentity(), (use.resolveToSymbol() as CaLocalVariableSymbol).cfirIdentity())
        }
    }

    @Test
    fun commandsScopeIsolation(mainFile: CjFile, services: TestServices) {
        val outer = element<CjBindingPattern>(mainFile, services, "outerDeclaration")
        val first = element<CjCommandTypePattern>(mainFile, services, "firstDeclaration")
        val second = element<CjCommandTypePattern>(mainFile, services, "secondDeclaration")
        val outerUses = listOf("tryOuter", "finallyOuter", "afterOuter").map {
            element<CjSimpleNameExpression>(mainFile, services, it)
        }
        analyzeForTest(mainFile) {
            requireOfficialCommandLibrary()
            val firstSymbol = requireNotNull(first.bindingSymbol)
            val secondSymbol = requireNotNull(second.bindingSymbol)
            assertHost(firstSymbol, first)
            assertHost(secondSymbol, second)
            assertCommandType(firstSymbol.returnType, PrimitiveTypeKind.INT64)
            assertCommandType(secondSymbol.returnType, PrimitiveTypeKind.BOOLEAN)
            assertNotSame(firstSymbol.cfirIdentity(), secondSymbol.cfirIdentity())
            assertSame(first, element<CjSimpleNameExpression>(mainFile, services, "firstUse").resolveToSymbol()!!.psi)
            assertSame(second, element<CjSimpleNameExpression>(mainFile, services, "secondUse").resolveToSymbol()!!.psi)
            outerUses.forEach { assertSame(outer, it.resolveToSymbol()!!.psi) }
            assertTrue(mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS).isEmpty())
        }
    }

    @Test
    fun commandsBodyRedefinition(mainFile: CjFile, services: TestServices) {
        val pattern = element<CjCommandTypePattern>(mainFile, services, "commandDeclaration")
        val duplicate = element<CjBindingPattern>(mainFile, services, "duplicateDeclaration")
        analyzeForTest(mainFile) {
            requireOfficialCommandLibrary()
            assertHost(requireNotNull(pattern.bindingSymbol), pattern)
            assertCommandType(pattern.bindingSymbol!!.returnType, PrimitiveTypeKind.INT64)
            val diagnostics = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
            assertTrue(diagnostics.any {
                it.factoryName == "REDECLARATION" && duplicate.nameIdentifier!!.textRange in it.textRanges
            }, "command 与 handle body 同级；不能把参数之外另开一层 scope 允许同名声明。")
        }
    }

    @Test
    fun commandsWildcardAndOutOfScope(mainFile: CjFile, services: TestServices) {
        val named = element<CjCommandTypePattern>(mainFile, services, "commandDeclaration")
        val wildcard = element<CjCommandTypePattern>(mainFile, services, "wildcardDeclaration")
        val unavailable = listOf("tryUnavailable", "siblingUnavailable", "finallyUnavailable", "afterUnavailable")
            .map { element<CjSimpleNameExpression>(mainFile, services, it) }
        analyzeForTest(mainFile) {
            requireOfficialCommandLibrary()
            assertHost(requireNotNull(named.bindingSymbol), named)
            assertCommandType(named.bindingSymbol!!.returnType, PrimitiveTypeKind.INT64)
            assertNull(wildcard.bindingSymbol)
            unavailable.forEach { assertNull(it.resolveToSymbol()) }
            val unresolved = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
                .filter { it.factoryName == "UNRESOLVED_REFERENCE" }.flatMap { it.textRanges }
            assertEquals(unavailable.map { it.textRange }.toSet(), unresolved.toSet())
        }
    }

    private fun CaSession.requireOfficialCommandLibrary() {
        val module = useSiteModule as CaSourceModule
        assertTrue(module.languageVersionSettings.supportsFeature(LanguageFeature.EffectHandlers))
        assertNotNull(getClassSymbol(StdlibClassIds.Command),
            "需要 SDK/CANGJIE_PATH 中实际 stdx.effect.Command<T>；cjc 1.1.3 自带 SDK 缺此包，不能造 std.core 代用品。")
    }

    private fun assertCommandType(type: CaType, resultKind: PrimitiveTypeKind) {
        assertTrue(type is CaUsualClassType, "命令绑定必须保持完整 pattern 类型，不能接受 Error/Any/resume T。")
        val command = type as CaUsualClassType
        assertEquals(StdlibClassIds.Command, command.classId)
        val resultType = command.typeArguments.single()
        assertTrue(resultType is CaPrimitiveType)
        assertEquals(resultKind, (resultType as CaPrimitiveType).kind)
    }

    private fun assertHost(symbol: CaLocalVariableSymbol, owner: PsiElement) {
        assertSame(owner, symbol.psi)
        assertEquals(CaSymbolLocation.LOCAL, symbol.location)
        assertNull(symbol.callableId)
        assertTrue(symbol.isLet)
    }
    private fun CaLocalVariableSymbol.cfirIdentity() = (this as CaCfirSymbol<*>).cfirSymbol
    private inline fun <reified T : PsiElement> element(file: CjFile, services: TestServices, marker: String): T =
        services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<T>(file, marker)
}
