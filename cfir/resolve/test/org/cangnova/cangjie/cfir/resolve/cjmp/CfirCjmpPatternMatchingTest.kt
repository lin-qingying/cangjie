@file:OptIn(org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class)

package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildTuplePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.resolve.providers.DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchEvaluator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 官方 v1.1.3 的 pattern 程序保留已接受变量，直接变量失败继续，嵌套和通配立即停止。 */
class CfirCjmpPatternMatchingTest {
    @Test
    fun `ordinary binding root matches its actual variable rather than the pattern container`() {
        val fixture = Fixture()
        val common = fixture.common("a")
        val pattern = fixture.binding("a")
        val specific = fixture.root(pattern)
        fixture.run(specific)

        val binding = requireNotNull(pattern.bindingVariable)
        assertSame(specific, fixture.session.cfirProvider.getCfirPatternVariableForBinding(binding.symbol))
        assertSame(common, fixture.snapshot.commonFor(binding))
        assertEquals(listOf(binding), fixture.snapshot.specificBindingsFor(common))
        assertNull(fixture.snapshot.commonFor(specific), "the root container is not the actual VAR declaration")
        assertTrue(fixture.snapshot.hasResolutionResult(specific))
        assertFalse(fixture.snapshot.isUnmatched(specific))
    }

    @Test
    fun `all direct tuple variables match individually`() {
        val fixture = Fixture()
        val commonA = fixture.common("a")
        val commonB = fixture.common("b")
        val a = fixture.binding("a")
        val b = fixture.binding("b")
        val root = fixture.root(tuple(a, b))
        fixture.run(root)

        fixture.assertBound(a, commonA)
        fixture.assertBound(b, commonB)
        val rootFacts = requireNotNull(root.cjmpMatchFacts)
        assertEquals(listOf(root.symbol), rootFacts.plans.map { it.specific })
        val rootAggregate = CjmpMatchEvaluator.evaluate(rootFacts.plans)
        assertEquals(setOf(root.symbol, requireNotNull(a.bindingVariable).symbol, requireNotNull(b.bindingVariable).symbol), rootAggregate.matched.keys)
        assertEquals(listOf(commonA.symbol, commonB.symbol), rootAggregate.acceptedBindings.map { it.common })
        assertNull(fixture.snapshot.commonFor(root))
        assertTrue(fixture.snapshot.hasResolutionResult(root))
        assertFalse(fixture.snapshot.isUnmatched(root))

        assertSame(rootFacts, root.cjmpMatchFacts, "a declaration owns its published result independently of session aggregates")
    }

    @Test
    fun `failed direct variable does not stop a later successful variable`() {
        val fixture = Fixture()
        val commonB = fixture.common("b")
        val missing = fixture.binding("missing")
        val b = fixture.binding("b")
        val root = fixture.root(tuple(missing, b))
        fixture.run(root)

        assertTrue(fixture.snapshot.isUnmatched(requireNotNull(missing.bindingVariable)))
        assertTrue(fixture.snapshot.hasResolutionResult(requireNotNull(missing.bindingVariable)))
        fixture.assertBound(b, commonB)
        assertTrue(fixture.snapshot.isUnmatched(root))
        assertNull(fixture.snapshot.commonFor(root))
    }

    @Test
    fun `nested tuple stops after preserving the accepted prefix`() {
        val fixture = Fixture()
        val commonA = fixture.common("a")
        val commonB = fixture.common("b")
        val commonC = fixture.common("c")
        val a = fixture.binding("a")
        val b = fixture.binding("b")
        val c = fixture.binding("c")
        val root = fixture.root(tuple(a, tuple(b, c)))
        fixture.run(root)

        fixture.assertBound(a, commonA)
        fixture.assertNotProcessed(b, commonB)
        fixture.assertNotProcessed(c, commonC)
        assertTrue(fixture.snapshot.isUnmatched(root))
        assertNull(fixture.snapshot.commonFor(root))
    }

    @Test
    fun `wildcard stops before later variables while preserving the accepted prefix`() {
        val fixture = Fixture()
        val commonA = fixture.common("a")
        val commonB = fixture.common("b")
        val a = fixture.binding("a")
        val b = fixture.binding("b")
        val root = fixture.root(tuple(a, buildWildcardPattern {}, b))
        fixture.run(root)

        fixture.assertBound(a, commonA)
        fixture.assertNotProcessed(b, commonB)
        assertTrue(fixture.snapshot.isUnmatched(root))
        assertNull(fixture.snapshot.commonFor(root))
    }

    @Test
    fun `ordinary variable with a different type still establishes its binding`() {
        val fixture = Fixture()
        val common = fixture.common("a", type = ConePrimitiveType.INT64)
        val specific = fixture.binding("a", type = ConePrimitiveType.BOOLEAN)
        val root = fixture.root(specific)
        fixture.run(root)

        fixture.assertBound(specific, common)
        assertFalse(fixture.snapshot.isUnmatched(root))
        assertTrue(fixture.snapshot.mismatchKindsFor(requireNotNull(specific.bindingVariable)).isEmpty())
    }

    @Test
    fun `ordinary var matched to common let still establishes its binding`() {
        val fixture = Fixture()
        val common = fixture.common("a", isVar = false)
        val specific = fixture.binding("a", isVar = true)
        val root = fixture.root(specific, isVar = true)
        fixture.run(root)

        fixture.assertBound(specific, common)
        assertFalse(fixture.snapshot.isUnmatched(root))
        assertTrue(fixture.snapshot.mismatchKindsFor(requireNotNull(specific.bindingVariable)).isEmpty())
    }

    @Test
    fun `duplicate global variables overwrite implementation without try bind occupancy`() {
        val fixture = Fixture()
        val common = fixture.common("a")
        val first = fixture.binding("a")
        val second = fixture.binding("a")
        val firstRoot = fixture.root(first)
        val secondRoot = fixture.root(second)
        fixture.run(firstRoot, secondRoot)

        val firstVariable = requireNotNull(first.bindingVariable)
        val secondVariable = requireNotNull(second.bindingVariable)
        assertNotSame(firstVariable, secondVariable)
        assertNull(fixture.snapshot.commonFor(firstVariable), "the overwritten VAR is only an accepted historical binding")
        assertSame(common, fixture.snapshot.commonFor(secondVariable))
        val accepted = fixture.snapshot.matching.acceptedBindings
            .filter { it.common === common.symbol }
            .map { it.specific.cfir }
        assertEquals(listOf(firstVariable, secondVariable), accepted)
        assertEquals(listOf(secondVariable), fixture.snapshot.specificBindingsFor(common))
        for (variable in listOf(firstVariable, secondVariable)) {
            assertTrue(fixture.snapshot.mismatchKindsFor(variable).isEmpty())
            assertTrue(fixture.snapshot.candidateDiagnosticsFor(variable).isEmpty())
        }
        assertFalse(fixture.snapshot.isUnmatched(firstRoot))
        assertFalse(fixture.snapshot.isUnmatched(secondRoot))
    }

    /** 使用真实模块依赖图、类型组件、已绑定变量与正式声明归属协议。 */
    private class Fixture {
        private val commonSession = CallResolutionTestFixtures.newTestSession()
        val session = CallResolutionTestFixtures.newTestSession(
            dependencies = listOf(commonSession.moduleData),
            refinementDependencies = listOf(commonSession.moduleData),
        )
        private var specificRoots: List<CfirPatternVariable> = emptyList()
        val snapshot: CfirCjmpFamilySnapshot
            get() = session.cjmpFamilyAggregate.matchingGroupFor(specificRoots.first())
        private val commons = mutableListOf<CfirFieldVariable>()

        fun common(name: String, type: ConeCangJieType = ConePrimitiveType.INT64, isVar: Boolean = false): CfirFieldVariable =
            buildFieldVariable {
                moduleData = commonSession.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                status = CfirDeclarationStatusImpl().apply { isCommon = true }
                symbol = CfirFieldVariableSymbol(CallableId(packageName, Name.identifier(name)))
                this.name = Name.identifier(name)
                this.isVar = isVar
                returnTypeRef = type.toCfirResolvedTypeRef(null)
            }.also(commons::add)

        fun binding(name: String, type: ConeCangJieType = ConePrimitiveType.INT64, isVar: Boolean = false): CfirBindingPattern {
            val variable = buildPatternBindingVariable {
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                status = CfirDeclarationStatusImpl().apply { isSpecific = true }
                symbol = CfirPatternBindingSymbol(CallableId(packageName, Name.identifier(name)))
                this.name = Name.identifier(name)
                this.isVar = isVar
                returnTypeRef = type.toCfirResolvedTypeRef(null)
            }
            return buildBindingPattern {
                this.name = variable.name
                bindingVariable = variable
            }
        }

        fun root(pattern: CfirPattern, isVar: Boolean = false): CfirPatternVariable = buildPatternVariable {
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl().apply { isSpecific = true }
            symbol = CfirPatternVariableSymbol(CallableId(packageName, Name.special("<pattern-variable>")))
            this.isVar = isVar
            returnTypeRef = typeOf(pattern).toCfirResolvedTypeRef(null)
            this.pattern = pattern
        }

        fun run(vararg roots: CfirPatternVariable) {
            specificRoots = roots.toList()
            val commonFile = ExtendTestFixtures.newFile(commonSession.moduleData, packageName, commons)
            val specificFile = ExtendTestFixtures.newFile(session.moduleData, packageName, roots.toList())
            val commonProvider = FileProvider(commonSession, commonFile)
            val specificProvider = FileProvider(session, specificFile)
            commonSession.register(CfirProvider::class, commonProvider)
            commonSession.register(CfirSymbolProvider::class, commonProvider.symbolProvider)
            session.register(CfirProvider::class, specificProvider)
            session.register(CfirSymbolProvider::class, specificProvider.symbolProvider)
            session.register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, commonProvider.symbolProvider)
            roots.forEach { root ->
                root.publishCjmpMatchFacts(CfirCjmpMatchRunner.prepareDeclaration(root, session))
            }
        }

        fun assertBound(pattern: CfirBindingPattern, common: CfirFieldVariable) {
            val variable = requireNotNull(pattern.bindingVariable)
            assertSame(common, snapshot.commonFor(variable))
            assertEquals(listOf(variable), snapshot.specificBindingsFor(common))
            assertTrue(snapshot.hasResolutionResult(variable))
            assertFalse(snapshot.isUnmatched(variable))
        }

        fun assertNotProcessed(pattern: CfirBindingPattern, common: CfirFieldVariable) {
            val variable = requireNotNull(pattern.bindingVariable)
            assertNull(snapshot.commonFor(variable))
            assertFalse(snapshot.hasResolutionResult(variable), "variables beyond the stopping pattern are not attempted")
            assertTrue(snapshot.specificBindingsFor(common).isEmpty())
        }

        private fun typeOf(pattern: CfirPattern): ConeCangJieType = when (pattern) {
            is CfirBindingPattern -> requireNotNull(pattern.bindingVariable).returnTypeRef.coneType
            is CfirTuplePattern -> ConeTupleType(pattern.elements.map(::typeOf))
            is CfirWildcardPattern -> ConePrimitiveType.INT64
            else -> error("This fixture only builds binding, tuple and wildcard patterns")
        }
    }

    /** 文件结构索引与实际 binding→root 身份表；不实现匹配、取舍或任何存储写入。 */
    private class FileProvider(session: CfirSession, private val file: CfirFile) : CfirProvider() {
        private val roots = file.declarations.filterIsInstance<CfirPatternVariable>()
        private val owners = roots.flatMap { root -> root.pattern.bindingVariables().map { it.symbol to root } }.toMap()
        private val callables = file.declarations.filterIsInstance<CfirFieldVariable>().map { it.symbol } + owners.keys
        override val symbolProvider = object : CfirSymbolProvider(session) {
            override val symbolNamesProvider = CfirNullSymbolNamesProvider
            override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? = null
            override fun hasPackage(fqName: FqName): Boolean = fqName == file.packageDirective.packageFqName
            override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
                destination += callables.filter { it.callableId.packageName == packageFqName && it.name == name }
            }
            override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {}
            override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {}
        }
        override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? = null
        override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile = error("The fixture has no classifiers")
        override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? = null
        override fun getCfirClassifierContainerFileIfAny(symbol: CfirClassLikeSymbol<*>): CfirFile? = null
        override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? =
            if (symbol in callables || roots.any { it.symbol === symbol }) file else null
        override fun getCfirPatternVariableForBinding(symbol: CfirPatternBindingSymbol): CfirPatternVariable? = owners[symbol]
        override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> =
            if (fqName == file.packageDirective.packageFqName) listOf(file) else emptyList()
        override fun getClassNamesInPackage(fqName: FqName): Set<Name> = emptySet()
    }

    private companion object {
        val packageName = FqName("pattern_matching")
        fun tuple(vararg elements: CfirPattern): CfirTuplePattern = buildTuplePattern { this.elements += elements }
    }
}
