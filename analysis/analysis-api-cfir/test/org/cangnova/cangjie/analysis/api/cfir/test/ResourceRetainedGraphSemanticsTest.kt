@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.declarations.ResolveStateAccess::class,
    org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals::class,
)

package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.PsiElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.util.PsiTreeUtil
import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.platform.modification.publishGlobalModuleStateModificationEvent
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.CfirLazyBodiesCalculator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.impl.CfirEmptyAnnotationArgumentMapping
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeNode
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.*
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.impl.ResolvedImplicitTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.Assertions.assertEquals as checkEquals
import org.junit.jupiter.api.Assertions.assertFalse as checkFalse
import org.junit.jupiter.api.Assertions.assertNotEquals as checkNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame as checkNotSame
import org.junit.jupiter.api.Assertions.assertSame as checkSame
import org.junit.jupiter.api.Assertions.assertTrue as checkTrue

/**
 * 六份原样 cjc 1.1.3 源码 × 三个冷入口。
 * RAW 两事务只复制 initializer；正式入口必须完成原资源与完整正文 CFG。
 * 检查精确 AST/symbol/PSI 身份，不以名字、ClassId、API wrapper equality 或 TV 总数代替所有权。
 */
@Timeout(60)
class ResourceRetainedGraphSemanticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/resourceRetainedGraphSemantics",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** 沿用 standalone stdlib 测试的真实 CJO/builtins 反编译服务，不向源码注入假 std.core。 */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(CaAnalysisApiDecompiledTestServiceRegistrar)

    private inline fun <reified T : Any> checkIs(value: Any?, message: String? = null): T =
        org.junit.jupiter.api.Assertions.assertInstanceOf(T::class.java, value, message)

    private fun <T : Any> checkNotNull(value: T?, message: String? = null): T {
        org.junit.jupiter.api.Assertions.assertNotNull(value, message)
        return kotlin.checkNotNull(value)
    }

    @Test
    fun localClosureForwardDeclaration(file: CjFile, services: TestServices) =
        verify(file, services, Case.LOCAL_CLOSURE_FORWARD, Entry.DECLARATION)

    @Test
    fun localClosureForwardUse(file: CjFile, services: TestServices) =
        verify(file, services, Case.LOCAL_CLOSURE_FORWARD, Entry.USE)

    @Test
    fun localClosureForwardDirectSymbol(file: CjFile, services: TestServices) =
        verify(file, services, Case.LOCAL_CLOSURE_FORWARD, Entry.DIRECT_SYMBOL)

    @Test
    fun sameNamesOwnedDeclarationsDeclaration(file: CjFile, services: TestServices) =
        verify(file, services, Case.SAME_NAMES, Entry.DECLARATION)

    @Test
    fun sameNamesOwnedDeclarationsUse(file: CjFile, services: TestServices) =
        verify(file, services, Case.SAME_NAMES, Entry.USE)

    @Test
    fun sameNamesOwnedDeclarationsDirectSymbol(file: CjFile, services: TestServices) =
        verify(file, services, Case.SAME_NAMES, Entry.DIRECT_SYMBOL)

    @Test
    fun nestedResourceInitializerDeclaration(file: CjFile, services: TestServices) =
        verify(file, services, Case.NESTED_RESOURCES, Entry.DECLARATION)

    @Test
    fun nestedResourceInitializerUse(file: CjFile, services: TestServices) =
        verify(file, services, Case.NESTED_RESOURCES, Entry.USE)

    @Test
    fun nestedResourceInitializerDirectSymbol(file: CjFile, services: TestServices) =
        verify(file, services, Case.NESTED_RESOURCES, Entry.DIRECT_SYMBOL)

    @Test
    fun nestedResourceExceptionalCfgDeclaration(file: CjFile, services: TestServices) =
        verify(file, services, Case.EXCEPTIONAL, Entry.DECLARATION)

    @Test
    fun nestedResourceExceptionalCfgUse(file: CjFile, services: TestServices) =
        verify(file, services, Case.EXCEPTIONAL, Entry.USE)

    @Test
    fun nestedResourceExceptionalCfgDirectSymbol(file: CjFile, services: TestServices) =
        verify(file, services, Case.EXCEPTIONAL, Entry.DIRECT_SYMBOL)


    @Test
    fun sameNamesForwardDeclaration(file: CjFile, services: TestServices) =
        verify(file, services, Case.FORWARD_SAME_NAMES, Entry.DECLARATION)

    @Test
    fun sameNamesForwardUse(file: CjFile, services: TestServices) =
        verify(file, services, Case.FORWARD_SAME_NAMES, Entry.USE)

    @Test
    fun sameNamesForwardDirectSymbol(file: CjFile, services: TestServices) =
        verify(file, services, Case.FORWARD_SAME_NAMES, Entry.DIRECT_SYMBOL)

    @Test
    fun nestedResourceForwardDeclaration(file: CjFile, services: TestServices) =
        verify(file, services, Case.FORWARD_NESTED_RESOURCES, Entry.DECLARATION)

    @Test
    fun nestedResourceForwardUse(file: CjFile, services: TestServices) =
        verify(file, services, Case.FORWARD_NESTED_RESOURCES, Entry.USE)

    @Test
    fun nestedResourceForwardDirectSymbol(file: CjFile, services: TestServices) =
        verify(file, services, Case.FORWARD_NESTED_RESOURCES, Entry.DIRECT_SYMBOL)

    private enum class Entry { DECLARATION, USE, DIRECT_SYMBOL }
    private enum class Case { LOCAL_CLOSURE_FORWARD, SAME_NAMES, NESTED_RESOURCES, EXCEPTIONAL, FORWARD_SAME_NAMES, FORWARD_NESTED_RESOURCES }
    private enum class Kind { RESOURCE, PARAMETER, BINDING, FUNCTION, CATCH_BINDING, TOP_FUNCTION, CONSTRUCTOR }
    private enum class Type { INT64, BOX, BINARY_FUNCTION, UNARY_FUNCTION, STRING_FUNCTION, EXCEPTION, GENERIC }
    private data class DeclarationSpec(val anchor: String, val kind: Kind, val type: Type)
    private data class ReferenceSpec(val anchor: String, val declaration: String)
    private data class SymbolEntry(val id: String, val psi: PsiElement, val symbol: CaSymbol)
    private data class Copy(
        val root: CfirExpression,
        val elements: Map<CfirElement, CfirElement>,
        val symbols: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>>,
    )
    private data class Raw(
        val file: CfirFile,
        val main: CfirMainFunction,
        val sourceElements: Set<CfirElement>,
        val initializer: CfirExpression,
        val initializerElements: Set<CfirElement>,
        val a: CfirResourceVariable,
        val b: CfirResourceVariable,
        val copies: List<Copy>,
    )

    /** 所有入口在任何 symbol/type/diagnostics 预热前，先验证原始 RAW 树和两次实际复制。 */
    private fun verify(file: CjFile, services: TestServices, case: Case, entry: Entry) {
        val fixture = Fixture(file, case)
        val (previousSession, previousToken, pointers) = analyzeForTest(file) {
            with(this as CaCfirSession) {
                val raw = assertRawCopies(fixture)
                val aPsi = fixture.parent<CjTryResource>("resource_a")
                val firstType = when (entry) {
                    Entry.DECLARATION -> checkNotNull(aPsi.bindingSymbol).returnType
                    Entry.USE -> checkNotNull(fixture.reference("body_a_ref").expressionType)
                    Entry.DIRECT_SYMBOL -> {
                        val cone = raw.a.symbol.resolvedReturnType
                        val direct = checkIs<CaLocalVariableSymbol>(cfirSymbolBuilder.buildSymbol(raw.a.symbol))
                        direct.returnType.also { checkEquals(cone, checkIs<CaCfirType>(it).coneType) }
                    }
                }
                assertBox(firstType, raw.file.declarations.filterIsInstance<CfirClass>().single())
                val entries = assertFormal(fixture, raw)
                assertDiagnostics(fixture)
                val stableGraphs = collect(raw.main).filterIsInstance<CfirFunction>().map { function ->
                    function to checkIs<CfirControlFlowGraphReferenceImpl>(function.controlFlowGraphReference).controlFlowGraph
                }
                val sameSession = assertFormal(fixture, raw)
                entries.zip(sameSession).forEach { (before, after) ->
                    checkSame(before.psi, after.psi)
                    checkSame(cfirSymbol(before.symbol), cfirSymbol(after.symbol))
                }
                stableGraphs.forEach { (function, graph) ->
                    checkSame(graph, checkIs<CfirControlFlowGraphReferenceImpl>(function.controlFlowGraphReference).controlFlowGraph)
                }
                Triple(resolutionFacade.useSiteCfirSession, token, entries.map { it.id to (it.psi to it.symbol.createPointer()) })
            }
        }
        ApplicationManager.getApplication().runWriteAction {
            file.project.publishGlobalModuleStateModificationEvent()
        }
        analyzeForTest(fixture.reference("body_a_ref")) {
            with(this as CaCfirSession) {
                checkFalse(previousSession.isValid)
                checkNotSame(previousSession, resolutionFacade.useSiteCfirSession)
                checkTrue(resolutionFacade.useSiteCfirSession.isValid)
                checkNotSame(previousToken, token)
                val current = assertFormal(fixture, raw = null).associateBy { it.id }
                for ((id, saved) in pointers) {
                    val (psi, pointer) = saved
                    val restored = checkNotNull(restoreSymbol(pointer), "pointer restore: " + id)
                    val now = checkNotNull(current[id])
                    checkSame(psi, restored.psi)
                    checkSame(now.psi, restored.psi)
                    checkEquals(now.symbol, restored)
                    checkSame(cfirSymbol(now.symbol), cfirSymbol(restored))
                    checkEquals(
                        checkIs<CaCfirType>(checkIs<CaCallableSymbol>(now.symbol).returnType).coneType,
                        checkIs<CaCfirType>(checkIs<CaCallableSymbol>(restored).returnType).coneType,
                    )
                }
                assertDiagnostics(fixture)
            }
        }
    }

    private fun cfirSymbol(symbol: CaSymbol): CfirBasedSymbol<*> =
        checkIs<CaCfirSymbol<*>>(symbol).cfirSymbol

    /** 只展开惰性 RAW 表达式；文件/所有声明的 phase 与原 symbol binding 都不能推进或替换。 */
    private fun CaCfirSession.assertRawCopies(fixture: Fixture): Raw {
        val file = fixture.file.getOrBuildCfirFile(resolutionFacade)
        checkEquals(CfirResolvePhase.RAW_CFIR, file.resolvePhase)
        val initialDeclarations = collect(file).filterIsInstance<CfirDeclaration>()
        val owners = initialDeclarations.associate { it.symbol to it }
        CfirLazyBodiesCalculator.calculateAllLazyExpressionsInFile(file)
        owners.forEach { (symbol, owner) -> checkSame(owner, symbol.cfir) }
        val all = collect(file)
        all.filterIsInstance<CfirElementWithResolveState>().forEach {
            checkEquals(CfirResolvePhase.RAW_CFIR, it.resolveState.resolvePhase)
            checkIs<CfirResolvedToPhaseState>(it.resolveState)
        }
        all.filterIsInstance<CfirControlFlowGraphOwner>().forEach { checkEquals(null, it.controlFlowGraphReference) }
        checkTrue(all.none { it is CfirLazyBlock || it is CfirLazyExpression })
        val main = file.declarations.filterIsInstance<CfirMainFunction>().single()
        val resources = collect(main).filterIsInstance<CfirResourceVariable>()
        checkEquals(fixture.resourceCount, resources.size)
        val a = resources.single { it.source?.psi === fixture.parent<CjTryResource>("resource_a") }
        val b = resources.single { it.source?.psi === fixture.parent<CjTryResource>("resource_b") }
        checkIs<CfirImplicitTypeRef>(b.returnTypeRef)
        val initializer = checkNotNull(b.initializer)
        val owned = collect(initializer)
        val external = identitySet<CfirElement>().apply { addAll(all.filter { it !in owned }) }
        val borrowedSymbols = identitySet<CfirBasedSymbol<*>>().apply {
            addAll(external.filterIsInstance<CfirDeclaration>().map { it.symbol })
        }
        val handles = identitySet<Any>()
        val ownedSymbols = identitySet<CfirBasedSymbol<*>>().apply {
            addAll(owned.filterIsInstance<CfirDeclaration>().map { it.symbol })
        }

        fun borrow(symbol: CfirBasedSymbol<*>) {
            if (symbol !in ownedSymbols) {
                checkTrue(symbol.isBound)
                borrowedSymbols += symbol
                external += symbol.cfir
            }
        }
        fun cone(type: ConeCangJieType) {
            when (type) {
                is ConeTypeParameterTypeImpl -> borrow(type.lookupTag.typeParameterSymbol)
                is ConeTypeVariableType -> handles.add(type.typeConstructor)
                is ConeLookupTagBasedType -> {
                    val tag = type.lookupTag as? ConeClassLikeLookupTagImpl
                    tag?.boundSymbol?.second?.let(::borrow)
                    tag?.boundSymbol?.first?.let { handles.add(it) }
                }
                else -> Unit
            }
            type.typeArguments.forEach { cone(it.type) }
            when (type) {
                is ConeFunctionType -> { type.parameterTypes.forEach(::cone); cone(type.returnType) }
                is ConeTupleType -> type.elementTypes.forEach(::cone)
                is ConeTypeAliasType -> type.expandedType?.let(::cone)
                is ConeErrorType -> type.delegatedType?.let(::cone)
                is ConeVArrayType -> cone(type.elementType)
                is ConePointerType -> cone(type.pointeeType)
                is ConeUnionType -> type.unionTypes.forEach(::cone)
                else -> Unit
            }
        }
        all.forEach { element ->
            if (element is CfirResolvedTypeRef) cone(element.coneType)
            if (element is CfirExpression) element.coneTypeOrNull?.let(::cone)
            if (element is CfirValueParameter) borrow(element.containingDeclarationSymbol)
            if (element is CfirTypeParameter) borrow(element.containingDeclarationSymbol)
        }
        (all + external).forEach { element ->
            element.source?.let { source ->
                handles += source
                source.psi?.let { psi ->
                    handles += psi
                    psi.containingFile?.let { containingFile ->
                        handles += containingFile
                        containingFile.virtualFile?.let { handles += it }
                    }
                }
            }
            if (element is CfirDeclaration) {
                handles += element.moduleData
                handles.add(element.moduleData.session)
            }
            if (element is CfirClassLikeDeclaration) handles += element.scopeProvider
            if (element is CfirFile) {
                element.sourceFile?.let { handles += it }
                element.sourceFileLinesMapping?.let { handles += it }
            }
        }
        handles += file.moduleData.session.cangjieScopeProvider
        handles += fixture.file
        fixture.file.virtualFile?.let { handles += it }

        val beforeBindings = all.filterIsInstance<CfirDeclaration>().associate { it.symbol to it }
        val copies = List(2) {
            val state = CfirRawCopyState(external.toList(), borrowedSymbols.toList())
            val context = CfirDefaultRawCopyContext(state, handles.toList())
            val clone = DeepCopyCfirTreeWithSymbols(context).copyRoot(initializer)
            handles.forEach { handle -> checkSame(handle, context.borrowedHandle(handle)) }
            borrowedSymbols.forEach { symbol -> checkSame(symbol, state.referencedSymbol(symbol)) }
            Copy(clone, state.elementMapping(), state.symbolMapping())
        }
        beforeBindings.forEach { (symbol, declaration) -> checkSame(declaration, symbol.cfir) }
        for (copy in copies) {
            checkNotSame(initializer, copy.root)
            checkTrue(copy.elements.keys.containsAll(owned), "RAW owned nodes missing from copy mapping: " +
                owned.filter { it !in copy.elements }.joinToString { it.javaClass.simpleName })
            checkTrue(copy.elements.keys.all { it in owned })
            checkEquals(ownedSymbols.size, copy.symbols.size)
            checkTrue(copy.symbols.keys.all { it in ownedSymbols })
            val cloneElements = collect(copy.root)
            checkTrue(copy.elements.values.all { it in cloneElements })
            for ((source, target) in copy.elements) {
                checkEquals(source.javaClass, target.javaClass)
                // RAW 复制器只共享这些显式允许的无状态 singleton，其他所有节点仍必须独立。
                if (source === CfirEmptyArgumentList || source === CfirEmptyAnnotationArgumentMapping ||
                    source === org.cangnova.cangjie.cfir.references.CfirStubReference
                ) {
                    checkSame(source, target)
                } else {
                    checkNotSame(source, target)
                }
                checkSame(source.source, target.source)
                checkSame(source.source?.kind, target.source?.kind)
                if (source is CfirDeclaration) {
                    val declaration = checkIs<CfirDeclaration>(target)
                    checkSame(copy.symbols[source.symbol], declaration.symbol)
                    checkNotSame(source.symbol, declaration.symbol)
                    checkSame(declaration, declaration.symbol.cfir)
                    checkSame(source, source.symbol.cfir)
                    checkSame(source.moduleData, declaration.moduleData)
                    checkNotSame(source.attributes, declaration.attributes)
                }
                if (source is CfirValueParameter) {
                    checkSame(copy.symbols[source.containingDeclarationSymbol] ?: source.containingDeclarationSymbol,
                        checkIs<CfirValueParameter>(target).containingDeclarationSymbol)
                }
                if (source is CfirTypeParameter) {
                    checkSame(copy.symbols[source.containingDeclarationSymbol] ?: source.containingDeclarationSymbol,
                        checkIs<CfirTypeParameter>(target).containingDeclarationSymbol)
                }
                if (source is CfirPatternTreeNode) {
                    val owner = source.patternTreeOwner
                    checkSame(owner?.let { copy.elements[it as CfirElement] ?: it },
                        checkIs<CfirPatternTreeNode>(target).patternTreeOwner)
                }
                if (source is CfirPatternTreeOwner) {
                    checkSame(copy.elements[source.patternTreeRoot()],
                        checkIs<CfirPatternTreeOwner>(target).patternTreeRoot())
                }
                if (source is CfirReturnExpression) {
                    val targetReturn = checkIs<CfirReturnExpression>(target)
                    checkNotSame(source.target, targetReturn.target)
                    if (source.target.isBound) {
                        val oldOwner = source.target.labeledElement as CfirElement
                        checkSame(copy.elements[oldOwner] ?: oldOwner, targetReturn.target.labeledElement)
                    }
                }
            }
            cloneElements.filterIsInstance<CfirControlFlowGraphOwner>().forEach { checkEquals(null, it.controlFlowGraphReference) }
            cloneElements.filterIsInstance<CfirElementWithResolveState>().forEach {
                checkEquals(CfirResolvePhase.RAW_CFIR, it.resolveState.resolvePhase)
            }
            checkFalse(copy.symbols.keys.any { it in borrowedSymbols })
            checkTrue(owned.filterIsInstance<CfirFunction>().isNotEmpty())
            owned.filterIsInstance<CfirFunction>().forEach { function ->
                checkIs<CfirFunction>(copy.elements[function])
                function.valueParameters.forEach { parameter -> checkNotNull(copy.symbols[parameter.symbol]) }
            }
            owned.filterIsInstance<CfirResourceVariable>().forEach {
                checkNotSame(it.initializer, checkIs<CfirResourceVariable>(copy.elements[it]).initializer)
            }
        }
        for (source in owned) {
            val first = checkNotNull(copies[0].elements[source])
            val second = checkNotNull(copies[1].elements[source])
            if (source === CfirEmptyArgumentList || source === CfirEmptyAnnotationArgumentMapping ||
                source === org.cangnova.cangjie.cfir.references.CfirStubReference
            ) {
                checkSame(source, first)
                checkSame(source, second)
            } else {
                checkNotSame(first, second)
            }
        }
        checkTrue(copies[0].symbols.values.none { first -> copies[1].symbols.values.any { it === first } })
        checkTrue(copies.flatMap { it.symbols.values }.none { it in ownedSymbols || it in borrowedSymbols })
        checkEquals(CfirResolvePhase.RAW_CFIR, main.resolvePhase)
        return Raw(file, main, all, initializer, owned, a, b, copies)
    }

    /** 所有正式引用都与源 token 所定位的正式 declaration 比较，而不是从同名 clone 中任选一个。 */
    private fun CaCfirSession.assertFormal(fixture: Fixture, raw: Raw?): List<SymbolEntry> {
        val file = fixture.file.getOrBuildCfirFile(resolutionFacade)
        val aPsi = fixture.parent<CjTryResource>("resource_a")
        val aApi = checkNotNull(aPsi.bindingSymbol)
        val firstType = aApi.returnType
        val all = collect(file)
        val box = file.declarations.filterIsInstance<CfirClass>().single()
        assertBox(firstType, box)
        val declarations = fixture.specs.associate { spec ->
            spec.anchor to declarationAt(all, fixture, spec)
        }
        val entries = declarations.map { (id, declaration) ->
            val api = cfirSymbolBuilder.buildSymbol(declaration.symbol)
            val psi = checkNotNull(declaration.source?.psi, "declaration PSI: " + id)
            checkSame(psi, api.psi)
            checkSame(declaration.symbol, cfirSymbol(api))
            assertType(checkIs<CaCallableSymbol>(api).returnType, fixture.specs.single { it.anchor == id }.type, box)
            SymbolEntry(id, psi, api)
        }.toMutableList()
        val a = checkIs<CfirResourceVariable>(declarations.getValue("resource_a"))
        val b = checkIs<CfirResourceVariable>(declarations.getValue("resource_b"))
        checkSame(a.symbol, cfirSymbol(aApi))
        checkSame(aPsi, aApi.psi)
        checkEquals(CaSymbolLocation.LOCAL, aApi.location)
        checkTrue(aApi.isLet)
        checkEquals(null, aApi.callableId)
        checkNotSame(a.symbol, b.symbol)
        checkNotEquals(entries.single { it.id == "resource_a" }.symbol, entries.single { it.id == "resource_b" }.symbol)
        raw?.let { checkSame(it.a, a); checkSame(it.b, b) }
        val apis = entries.associateBy { it.id }
        for (reference in fixture.references) {
            val use = fixture.reference(reference.anchor)
            val expected = checkNotNull(apis[reference.declaration])
            val resolved = checkNotNull(use.resolveToSymbol(), "reference: " + reference.anchor)
            checkEquals(expected.symbol, resolved)
            checkSame(expected.psi, resolved.psi)
            checkSame(cfirSymbol(expected.symbol), cfirSymbol(resolved))
            val declaration = declarations.getValue(reference.declaration)
            checkSame(declaration, cfirSymbol(resolved).cfir)
            val type = fixture.specs.single { it.anchor == reference.declaration }.type
            if (type != Type.GENERIC && fixture.specs.single { it.anchor == reference.declaration }.kind != Kind.FUNCTION) {
                assertType(checkNotNull(use.expressionType), type, box)
            }
        }
        for (id in fixture.callAnchors) {
            val call = fixture.parent<CjCallExpression>(id)
            val type = checkNotNull(call.expressionType)
            when (id) {
                "initializer_run", "box_call", "nested_box_call" -> assertBox(type, box)
                "normal_record", "catch_record", "finally_record", "outer_body_record" -> assertUnit(type)
                "caught_call" -> assertNominal(type, "std.core.String")
                else -> assertInt64(type)
            }
        }
        val operation = fixture.parent<CjPatternVariable>("operation_decl").initializer
        assertBinary(checkNotNull(checkIs<CjLambdaExpression>(operation).expressionType))
        checkNotSame(declarations.getValue("outer_left").symbol, declarations.getValue("outer_right").symbol)
        if (fixture.case == Case.SAME_NAMES || fixture.case == Case.FORWARD_SAME_NAMES) {
            checkNotSame(declarations.getValue("first_step_decl").symbol, declarations.getValue("second_step_decl").symbol)
            val parameters = listOf("first_value", "first_step_value", "second_value", "second_step_value").map { declarations.getValue(it).symbol }
            checkEquals(4, identitySet<CfirBasedSymbol<*>>().apply { addAll(parameters) }.size)
        }
        val sdkString = checkNotNull(file.moduleData.session.symbolProvider.getClassLikeSymbolByClassId(
            ClassId.topLevel(FqName("std.core.String"))))
        val stringDeclaration = sdkString.cfir
        checkIs<CfirStruct>(stringDeclaration)
        checkNotSame(file.moduleData, stringDeclaration.moduleData)
        checkEquals(CfirDeclarationOrigin.Library, stringDeclaration.origin)
        val stringSymbol = cfirSymbolBuilder.buildSymbol(sdkString)
        checkEquals(CaSymbolOrigin.LIBRARY, stringSymbol.origin)
        val stringPsi = checkIs<CjStruct>(checkNotNull(stringSymbol.psi))
        checkTrue(checkIs<CjFile>(stringPsi.containingFile).isCompiled, "String must come from the real stdlib CJO input")
        assertCfg(fixture, all, raw?.copies.orEmpty())
        return entries
    }

    private fun declarationAt(elements: Set<CfirElement>, fixture: Fixture, spec: DeclarationSpec): CfirDeclaration {
        if (spec.kind == Kind.TOP_FUNCTION) {
            val psi = fixture.topFunction(spec.anchor.removePrefix("top_"))
            return elements.filterIsInstance<CfirNamedFunction>().single { it.source?.psi === psi }
        }
        if (spec.kind == Kind.CONSTRUCTOR) {
            return elements.filterIsInstance<CfirClass>().single().declarations.filterIsInstance<CfirConstructor>().single()
        }
        val token = fixture.token(spec.anchor)
        val candidates = elements.filterIsInstance<CfirDeclaration>().filter { declaration ->
            val kindMatches = when (spec.kind) {
                Kind.RESOURCE -> declaration is CfirResourceVariable
                Kind.PARAMETER -> declaration is CfirValueParameter
                Kind.BINDING, Kind.CATCH_BINDING -> declaration is CfirPatternBindingVariable
                Kind.FUNCTION -> declaration is CfirNamedFunction
                else -> false
            }
            kindMatches && declaration.source?.psi?.textRange?.contains(token.textRange) == true
        }
        val minimum = candidates.minOf { checkNotNull(it.source?.psi).textRange.length }
        return candidates.single { checkNotNull(it.source?.psi).textRange.length == minimum }
    }

    private fun CaCfirSession.assertType(type: CaType, expected: Type, box: CfirClass) {
        when (expected) {
            Type.INT64 -> assertInt64(type)
            Type.BOX -> assertBox(type, box)
            Type.BINARY_FUNCTION -> assertBinary(type)
            Type.UNARY_FUNCTION -> {
                val function = checkIs<CaFunctionType>(type)
                checkEquals(1, function.parameterTypes.size)
                assertInt64(function.parameterTypes.single())
                assertInt64(function.returnType)
            }
            Type.STRING_FUNCTION -> {
                val function = checkIs<CaFunctionType>(type)
                checkTrue(function.parameterTypes.isEmpty())
                assertNominal(function.returnType, "std.core.String")
            }
            Type.EXCEPTION -> assertNominal(type, "std.core.Exception")
            Type.GENERIC -> checkIs<CaCfirType>(type)
        }
    }

    private fun assertInt64(type: CaType) =
        checkEquals(PrimitiveTypeKind.INT64, checkIs<CaPrimitiveType>(type).kind)

    private fun assertUnit(type: CaType) =
        checkEquals(PrimitiveTypeKind.UNIT, checkIs<CaPrimitiveType>(type).kind)

    private fun assertBinary(type: CaType) {
        val function = checkIs<CaFunctionType>(type)
        checkEquals(2, function.parameterTypes.size)
        function.parameterTypes.forEach(::assertInt64)
        assertInt64(function.returnType)
    }

    private fun assertNominal(type: CaType, fqName: String) {
        checkEquals(fqName, checkIs<CaUsualClassType>(type).classId.asSingleFqName().asString())
    }

    private fun assertBox(type: CaType, box: CfirClass) {
        val cone = checkIs<CaCfirType>(type).coneType
        val nominal = checkIs<CaUsualClassType>(type, "Resource result: $cone; diagnostic=${(cone as? ConeErrorType)?.diagnostic?.reason}")
        checkEquals(box.symbol.classId, nominal.classId)
        assertInt64(nominal.typeArguments.single())
        val tag = checkIs<ConeLookupTagBasedType>(cone).lookupTag
        checkSame(box.symbol, checkIs<ConeClassLikeLookupTag>(tag).toSymbol(box.moduleData.session))
    }

    private fun CaCfirSession.assertDiagnostics(fixture: Fixture) {
        val expected = when (fixture.case) {
            Case.LOCAL_CLOSURE_FORWARD, Case.FORWARD_SAME_NAMES ->
                listOf("USED_BEFORE_INITIALIZATION" to listOf(fixture.reference("forward_b").textRange))
            Case.FORWARD_NESTED_RESOURCES -> listOf(
                "USED_BEFORE_INITIALIZATION" to listOf(fixture.reference("forward_b").textRange),
                "USED_BEFORE_INITIALIZATION" to listOf(fixture.reference("nested_forward_p").textRange),
            )
            else -> emptyList()
        }
        val actual = fixture.file.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
        checkEquals(
            expected.groupingBy { it }.eachCount(),
            actual.map { it.factoryName to it.textRanges.toList() }.groupingBy { it }.eachCount(),
            actual.joinToString { it.factoryName + "@" + it.textRanges },
        )
    }

    /**
     * 正式 graph 指向当前正式树及其实际 lambda 合成调用。按 graph identity 去重，保留合法 finally layer，
     * 不对全局 TV 或 CFG 节点总数作固定数量断言。
     */
    private fun CaCfirSession.assertCfg(fixture: Fixture, all: Set<CfirElement>, copies: List<Copy>) {
        val main = all.filterIsInstance<CfirMainFunction>().single()
        val live = collect(main)
        val functions = live.filterIsInstance<CfirFunction>()
        val graphs = identitySet<ControlFlowGraph>()
        fun visit(graph: ControlFlowGraph) {
            if (!graphs.add(graph)) return
            graph.subGraphs.forEach(::visit)
        }
        functions.forEach { function ->
            val graph = checkIs<CfirControlFlowGraphReferenceImpl>(function.controlFlowGraphReference).controlFlowGraph
            checkSame(function, graph.declaration)
            checkSame(function, checkIs<FunctionEnterNode>(graph.enterNode).fir)
            checkSame(function, checkIs<FunctionExitNode>(graph.exitNode).fir)
            visit(graph)
        }
        val privateElements = identitySet<CfirElement>().apply {
            copies.forEach { copy -> addAll(copy.elements.values.filter { it !== CfirEmptyArgumentList && it !== CfirEmptyAnnotationArgumentMapping }) }
        }
        val nodes = identitySet<CFGNode<*>>()
        graphs.forEach { graph ->
            graph.nodes.forEach { node ->
                checkSame(graph, node.owner)
                if (node is StubNode || node is FakeExpressionEnterNode) {
                    checkSame(CfirStub, node.fir)
                } else {
                    val call = node.fir as? CfirFunctionCall
                    val target = when (val reference = call?.calleeReference) {
                        is org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference -> reference.resolvedSymbol
                        is org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate -> reference.candidateSymbol
                        else -> null
                    }
                    // SyntheticCallGenerator 返回实际 lambda；CFG 的调用包装仍持有该同一节点。
                    // 按实际 argument、声明来源及形参 owner 检查，私有 lambda 副本不能获得此身份。
                    val lambda = call?.argumentList?.arguments?.singleOrNull() as? CfirAnonymousFunctionExpression
                    val syntheticFunction = target?.cfir as? CfirNamedFunction
                    val isFormalLambdaCall = lambda != null && lambda in live &&
                        syntheticFunction?.origin == CfirDeclarationOrigin.Synthetic.FakeFunction &&
                        call.source === lambda.source && syntheticFunction.source === lambda.source &&
                        syntheticFunction.valueParameters.singleOrNull()?.containingDeclarationSymbol === target
                    checkTrue(node.fir in live || isFormalLambdaCall,
                        "CFG fir is not in the formal tree: ${node.javaClass.simpleName}; " +
                            "fir=${node.fir.javaClass.simpleName}@${System.identityHashCode(node.fir).toString(16)}, " +
                            "source=${node.fir.source?.kind}, calleeOrigin=${target?.cfir?.origin}, " +
                            "arguments=${call?.argumentList?.arguments?.map { argument -> "${argument.javaClass.simpleName}(formal=${argument in live},private=${argument in privateElements})" }}")
                }
                checkFalse(node.fir in privateElements, "CFG imported a private copy")
                nodes += node
            }
        }
        functions.forEach { function -> checkEquals(1, graphs.count { it.declaration === function }) }
        live.filterIsInstance<CfirFunctionCall>().forEach { call ->
            val exits = nodes.filterIsInstance<FunctionCallExitNode>()
            checkEquals(1, exits.count { it.fir === call },
                "missing or duplicated formal call exit: ${call.javaClass.simpleName}@${System.identityHashCode(call).toString(16)}, " +
                    "origin=${call.origin}, psi=${call.source?.psi?.javaClass?.simpleName}, text=${call.source?.psi?.text?.take(150)}, " +
                    "sameSourceExits=${exits.filter { it.fir.source === call.source }.map { System.identityHashCode(it.fir).toString(16) }}")
        }
        for (id in fixture.callAnchors) {
            val psi = fixture.parent<CjCallExpression>(id)
            val calls = live.filterIsInstance<CfirFunctionCall>().filter { it.source?.psi === psi }
            checkEquals(1, calls.size, "formal call AST: " + id)
            val call = calls.single()
            val exit = nodes.filterIsInstance<FunctionCallExitNode>().single { it.fir === call }
            checkSame(call, exit.fir)
            checkTrue(exit.owner.nodes.any { it === exit })
        }
        val tries = live.filterIsInstance<CfirTryExpression>()
        checkEquals(if (fixture.hasNestedTry) 2 else 1, tries.size)
        for (expression in tries) {
            val enter = nodes.filterIsInstance<TryExpressionEnterNode>().single { it.fir === expression }
            val exit = nodes.filterIsInstance<TryExpressionExitNode>().single { it.fir === expression }
            checkSame(enter.owner, exit.owner)
            expression.resources.forEach { resource ->
                checkEquals(1, nodes.filterIsInstance<VariableDeclarationEnterNode>().count { it.fir === resource })
                checkEquals(1, nodes.filterIsInstance<VariableDeclarationExitNode>().count { it.fir === resource })
            }
            checkTrue(reachable(enter, exit), "try has no formal CFA completion path")
        }
        if (fixture.hasNestedTry) {
            val nestedPsi = fixture.parent<CjTryExpression>("nested_try")
            val nested = tries.single { it.source?.psi === nestedPsi }
            checkEquals(if (fixture.case == Case.EXCEPTIONAL) 1 else 2, nested.resources.size)
            val initializerCall = live.filterIsInstance<CfirFunctionCall>().single { it.source?.psi === fixture.parent<CjCallExpression>("initializer_run") }
            val lambda = collect(initializerCall).filterIsInstance<CfirAnonymousFunctionExpression>().single {
                it.anonymousFunction.body?.let { body -> nested in collect(body) } == true
            }
            val lambdaGraph = checkIs<CfirControlFlowGraphReferenceImpl>(lambda.anonymousFunction.controlFlowGraphReference).controlFlowGraph
            val nestedEnter = nodes.filterIsInstance<TryExpressionEnterNode>().single { it.fir === nested }
            checkSame(lambdaGraph, nestedEnter.owner)
            if (fixture.case == Case.EXCEPTIONAL) {
                val catch = nested.catches.single()
                val catchEnter = nodes.filterIsInstance<CatchClauseEnterNode>().single { it.fir === catch }
                val catchExit = nodes.filterIsInstance<CatchClauseExitNode>().single { it.fir === catch }
                val finalEnters = nodes.filterIsInstance<FinallyBlockEnterNode>().filter { it.fir === nested }
                val finalExits = nodes.filterIsInstance<FinallyBlockExitNode>().filter { it.fir === nested }
                checkTrue(finalEnters.isNotEmpty() && finalExits.isNotEmpty())
                finalExits.forEach { checkTrue(finalEnters.any { enter -> enter === it.enterNode }) }
                val throwPsi = fixture.parent<CjThrowExpression>("throw_expr")
                val thrown = live.filterIsInstance<CfirThrowExpression>().single { it.source?.psi === throwPsi }
                val throwNode = nodes.filterIsInstance<ThrowExceptionNode>().single { it.fir === thrown }
                checkTrue(reachable(throwNode, catchEnter), "throw does not reach its catch")
                checkTrue(reachable(catchEnter, catchExit))
                val normal = nodes.filterIsInstance<FunctionCallExitNode>().single {
                    it.fir.source?.psi === fixture.parent<CjCallExpression>("normal_record")
                }
                val lastBox = nodes.filterIsInstance<FunctionCallExitNode>().single {
                    it.fir.source?.psi === fixture.parent<CjCallExpression>("box_call")
                }
                checkTrue(finalEnters.any { reachable(normal, it) })
                checkTrue(finalEnters.any { reachable(catchExit, it) })
                checkTrue(finalExits.any { reachable(it, lastBox) })
                reportExceptionalCfgWitness(throwNode, normal)
                checkFalse(reachable(throwNode, normal, normalOnly = true), "throw acquired a normal path to the following body call")
                checkFalse(reachable(catchExit, lastBox, blocked = identitySet<CFGNode<*>>().apply { addAll(finalEnters) }))
                val caughtPsi = fixture.parent<CjPatternVariable>("caught_decl").initializer
                val caught = live.filterIsInstance<CfirAnonymousFunctionExpression>().single { it.source?.psi === caughtPsi }
                checkSame(caught.anonymousFunction, checkIs<CfirControlFlowGraphReferenceImpl>(
                    caught.anonymousFunction.controlFlowGraphReference).controlFlowGraph.declaration)
            }
        }
    }

    /** 仅在现有 normal-only 验收失败时记录真实 identity 路径；不改变原验收或图。 */
    private fun reportExceptionalCfgWitness(start: CFGNode<*>, target: CFGNode<*>) {
        val parents = IdentityHashMap<CFGNode<*>, CFGNode<*>?>()
        val queue = ArrayDeque<CFGNode<*>>()
        parents[start] = null
        queue += start
        var found = false
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node === target) {
                found = true
                break
            }
            for (next in node.followingNodes) {
                val edge = node.edgeTo(next)
                if (!edge.kind.usedInCfa || edge.kind.isDead || edge.label !== NormalPath) continue
                if (parents.containsKey(next)) continue
                parents[next] = node
                queue += next
            }
        }
        if (!found) return

        fun identity(value: Any?): String = value?.let {
            "${it.javaClass.simpleName}@${System.identityHashCode(it).toString(16)}"
        } ?: "null"
        fun nodeText(node: CFGNode<*>): String =
            "${identity(node)} id=${node.id} level=${node.level} dead=${node.isDead} " +
                "owner=${identity(node.owner)} declaration=${identity(node.owner.declaration)} " +
                "fir=${identity(node.fir)} source=${identity(node.fir.source)}"
        fun edgeText(from: CFGNode<*>, to: CFGNode<*>): String {
            val edge = from.edgeTo(to)
            return "kind=${edge.kind} label=${identity(edge.label)} " +
                "normal=${edge.label === NormalPath} back=${edge.kind.isBack} " +
                "dead=${edge.kind.isDead} cfa=${edge.kind.usedInCfa}"
        }

        val path = ArrayList<CFGNode<*>>()
        var current: CFGNode<*>? = target
        while (current != null) {
            path += current
            current = parents[current]
        }
        path.reverse()
        System.err.println("RESOURCE_EXCEPTIONAL_CFG_WITNESS start=${nodeText(start)} target=${nodeText(target)}")
        start.followingNodes.forEach { next ->
            System.err.println("RESOURCE_EXCEPTIONAL_CFG_OUT ${nodeText(start)} -> ${nodeText(next)} ${edgeText(start, next)}")
        }
        System.err.println("RESOURCE_EXCEPTIONAL_CFG_NODE index=0 ${nodeText(path.first())}")
        path.zipWithNext().forEachIndexed { index, (from, to) ->
            System.err.println("RESOURCE_EXCEPTIONAL_CFG_EDGE index=$index ${nodeText(from)} -> ${nodeText(to)} ${edgeText(from, to)}")
        }
    }

    private fun reachable(
        start: CFGNode<*>,
        target: CFGNode<*>,
        normalOnly: Boolean = false,
        blocked: Set<CFGNode<*>> = emptySet(),
    ): Boolean {
        val seen = identitySet<CFGNode<*>>()
        val queue = ArrayDeque<CFGNode<*>>()
        queue += start
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node in blocked || !seen.add(node)) continue
            if (node === target) return true
            for (next in node.followingNodes) {
                val edge = node.edgeTo(next)
                if (!edge.kind.usedInCfa || edge.kind.isDead) continue
                if (normalOnly && edge.label !== NormalPath) continue
                queue += next
            }
        }
        return false
    }

    /** children 与 RAW 中可能拥有 AST 的 metadata 使用同一个 identity visited set。 */
    private fun collect(root: CfirElement): Set<CfirElement> {
        val result = identitySet<CfirElement>()
        val visitor = object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (!result.add(element)) return
                if (element is CfirLazyBlock || element is CfirLazyExpression) return
                element.acceptChildren(this)
                if (element is ResolvedImplicitTypeRef) element.typeRef.accept(this)
                if (element is CfirDeclaration) {
                    if (element is CfirMemberDeclaration) element.status.accept(this)
                    element.attributes.typeConstraintDiagnosticData?.typeConstraints?.forEach { constraint ->
                        constraint.boundTypeRefs.forEach { it.accept(this) }
                    }
                    if (element is CfirVariable) {
                        (element.evaluatedInitializer as? CfirEvaluatorResult.Evaluated)?.result?.accept(this)
                    }
                }
                if (element is CfirAnnotationCall) {
                    element.argumentMapping.accept(this)
                    element.argumentMapping.mapping.values.forEach { it.accept(this) }
                    element.argumentView?.entries?.forEach { entry ->
                        entry.argument?.accept(this)
                        entry.constantExpression?.accept(this)
                    }
                }
            }
        }
        root.accept(visitor)
        return result
    }

    private fun <T : Any> identitySet(): MutableSet<T> =
        Collections.newSetFromMap(IdentityHashMap<T, Boolean>())

    /** 注释 anchor 只给出源码 token；不通过名字查找函数、资源、参数或 clone。 */
    private class Fixture(val file: CjFile, val case: Case) {
        val resourceCount: Int get() = when (case) { Case.NESTED_RESOURCES, Case.FORWARD_NESTED_RESOURCES -> 4; Case.EXCEPTIONAL -> 3; else -> 2 }
        val hasNestedTry: Boolean get() = case == Case.NESTED_RESOURCES || case == Case.EXCEPTIONAL || case == Case.FORWARD_NESTED_RESOURCES
        private val offsets: Map<String, Int> = Regex("""/\*@([a-z][a-z0-9_]*)\*/""").findAll(file.text).associate {
            it.groupValues[1] to (it.range.last + 1)
        }
        fun token(id: String): PsiElement {
            val offset = checkNotNull(offsets[id]) { "missing anchor: " + id }
            val token = checkNotNull(file.findElementAt(offset))
            checkEquals(offset, token.textRange.startOffset, "anchor does not point to the token start: " + id)
            return token
        }
        inline fun <reified T : PsiElement> parent(id: String): T =
            checkNotNull(PsiTreeUtil.getParentOfType(token(id), T::class.java, false)) { "wrong PSI parent: " + id }

        fun reference(id: String): CjSimpleNameExpression = parent(id)

        fun topFunction(id: String): CjNamedFunction {
            val matches = Regex("^func " + Regex.escape(id) + "(?:<|\\()", RegexOption.MULTILINE).findAll(file.text).toList()
            val start = matches.single().range.first + "func ".length
            return checkNotNull(PsiTreeUtil.getParentOfType(file.findElementAt(start), CjNamedFunction::class.java, false))
        }

        val specs: List<DeclarationSpec> get() {
            val common = listOf(
                DeclarationSpec("resource_a", Kind.RESOURCE, Type.BOX),
                DeclarationSpec("resource_b", Kind.RESOURCE, Type.BOX),
                DeclarationSpec("outer_left", Kind.PARAMETER, Type.INT64),
                DeclarationSpec("outer_right", Kind.PARAMETER, Type.INT64),
                DeclarationSpec("operation_decl", Kind.BINDING, Type.BINARY_FUNCTION),
                DeclarationSpec("top_run", Kind.TOP_FUNCTION, Type.GENERIC),
                DeclarationSpec("box_ctor", Kind.CONSTRUCTOR, Type.GENERIC),
            )
            return common + when (case) {
                Case.LOCAL_CLOSURE_FORWARD -> listOf(
                    DeclarationSpec("mapper_decl", Kind.BINDING, Type.UNARY_FUNCTION),
                    DeclarationSpec("mapper_value", Kind.PARAMETER, Type.INT64),
                    DeclarationSpec("relay_decl", Kind.FUNCTION, Type.INT64),
                    DeclarationSpec("relay_value", Kind.PARAMETER, Type.INT64),
                    DeclarationSpec("result_decl", Kind.BINDING, Type.INT64),
                )
                Case.SAME_NAMES, Case.FORWARD_SAME_NAMES -> listOf(
                    DeclarationSpec("first_decl", Kind.BINDING, Type.UNARY_FUNCTION),
                    DeclarationSpec("first_value", Kind.PARAMETER, Type.INT64),
                    DeclarationSpec("first_step_decl", Kind.FUNCTION, Type.INT64),
                    DeclarationSpec("first_step_value", Kind.PARAMETER, Type.INT64),
                    DeclarationSpec("second_decl", Kind.BINDING, Type.UNARY_FUNCTION),
                    DeclarationSpec("second_value", Kind.PARAMETER, Type.INT64),
                    DeclarationSpec("second_step_decl", Kind.FUNCTION, Type.INT64),
                    DeclarationSpec("second_step_value", Kind.PARAMETER, Type.INT64),
                    DeclarationSpec("result_decl", Kind.BINDING, Type.INT64),
                )
                Case.NESTED_RESOURCES, Case.FORWARD_NESTED_RESOURCES -> listOf(
                    DeclarationSpec("resource_p", Kind.RESOURCE, Type.BOX),
                    DeclarationSpec("resource_q", Kind.RESOURCE, Type.BOX),
                    DeclarationSpec("mapper_decl", Kind.BINDING, Type.UNARY_FUNCTION),
                    DeclarationSpec("mapper_value", Kind.PARAMETER, Type.INT64),
                    DeclarationSpec("result_decl", Kind.BINDING, Type.INT64),
                )
                Case.EXCEPTIONAL -> listOf(
                    DeclarationSpec("top_record", Kind.TOP_FUNCTION, Type.GENERIC),
                    DeclarationSpec("resource_inner", Kind.RESOURCE, Type.BOX),
                    DeclarationSpec("catch_e", Kind.CATCH_BINDING, Type.EXCEPTION),
                    DeclarationSpec("caught_decl", Kind.BINDING, Type.STRING_FUNCTION),
                    DeclarationSpec("normal_result_decl", Kind.BINDING, Type.INT64),
                    DeclarationSpec("recovered_result_decl", Kind.BINDING, Type.INT64),
                )
            }
        }

        val references: List<ReferenceSpec> get() = when (case) {
            Case.LOCAL_CLOSURE_FORWARD -> listOf(
                ReferenceSpec("relay_mapper_ref", "mapper_decl"),
                ReferenceSpec("relay_value_ref", "relay_value"),
                ReferenceSpec("mapper_capture_left", "outer_left"),
                ReferenceSpec("relay_left_ref", "outer_left"),
                ReferenceSpec("relay_right_ref", "outer_right"),
                ReferenceSpec("forward_b", "resource_b"),
                ReferenceSpec("body_a_ref", "resource_a"),
                ReferenceSpec("outer_return_left", "outer_left"),
                ReferenceSpec("operation_call", "operation_decl"),
                ReferenceSpec("relay_left_call", "relay_decl"),
                ReferenceSpec("relay_right_call", "relay_decl"),
                ReferenceSpec("initializer_run", "top_run"),
                ReferenceSpec("box_call", "box_ctor"),
            )
            Case.SAME_NAMES -> listOf(
                ReferenceSpec("first_step_call", "first_step_decl"),
                ReferenceSpec("second_step_call", "second_step_decl"),
                ReferenceSpec("first_step_value_ref", "first_step_value"),
                ReferenceSpec("second_step_value_ref", "second_step_value"),
                ReferenceSpec("first_value_ref", "first_value"),
                ReferenceSpec("second_value_ref", "second_value"),
                ReferenceSpec("first_capture_left", "outer_left"),
                ReferenceSpec("second_capture_right", "outer_right"),
                ReferenceSpec("ordered_b_ref", "resource_b"),
                ReferenceSpec("body_a_ref", "resource_a"),
                ReferenceSpec("outer_return_left", "outer_left"),
                ReferenceSpec("operation_call", "operation_decl"),
                ReferenceSpec("first_call", "first_decl"),
                ReferenceSpec("second_call", "second_decl"),
                ReferenceSpec("first_outer_left", "outer_left"),
                ReferenceSpec("second_outer_right", "outer_right"),
                ReferenceSpec("initializer_run", "top_run"),
                ReferenceSpec("box_call", "box_ctor"),
            )
            Case.NESTED_RESOURCES -> listOf(
                ReferenceSpec("nested_p_ref", "resource_p"),
                ReferenceSpec("mapper_capture_p", "resource_p"),
                ReferenceSpec("body_q_ref", "resource_q"),
                ReferenceSpec("mapper_call", "mapper_decl"),
                ReferenceSpec("nested_left", "outer_left"),
                ReferenceSpec("nested_right", "outer_right"),
                ReferenceSpec("final_left", "outer_left"),
                ReferenceSpec("final_right", "outer_right"),
                ReferenceSpec("ordered_b_ref", "resource_b"),
                ReferenceSpec("body_a_ref", "resource_a"),
                ReferenceSpec("outer_return_left", "outer_left"),
                ReferenceSpec("operation_call", "operation_decl"),
                ReferenceSpec("initializer_run", "top_run"),
                ReferenceSpec("box_call", "box_ctor"),
                ReferenceSpec("nested_box_call", "box_ctor"),
            )
            Case.EXCEPTIONAL -> listOf(
                ReferenceSpec("catch_capture_e", "catch_e"),
                ReferenceSpec("caught_call", "caught_decl"),
                ReferenceSpec("body_inner_ref", "resource_inner"),
                ReferenceSpec("nested_left", "outer_left"),
                ReferenceSpec("nested_right", "outer_right"),
                ReferenceSpec("final_left", "outer_left"),
                ReferenceSpec("final_right", "outer_right"),
                ReferenceSpec("ordered_b_ref", "resource_b"),
                ReferenceSpec("body_a_ref", "resource_a"),
                ReferenceSpec("outer_return_left", "outer_left"),
                ReferenceSpec("normal_operation_call", "operation_decl"),
                ReferenceSpec("recovered_operation_call", "operation_decl"),
                ReferenceSpec("throw_condition_left", "outer_left"),
                ReferenceSpec("initializer_run", "top_run"),
                ReferenceSpec("box_call", "box_ctor"),
                ReferenceSpec("nested_box_call", "box_ctor"),
                ReferenceSpec("normal_record", "top_record"),
                ReferenceSpec("catch_record", "top_record"),
                ReferenceSpec("finally_record", "top_record"),
                ReferenceSpec("outer_body_record", "top_record"),
            )
            Case.FORWARD_SAME_NAMES -> listOf(
                ReferenceSpec("first_step_call", "first_step_decl"),
                ReferenceSpec("second_step_call", "second_step_decl"),
                ReferenceSpec("first_step_value_ref", "first_step_value"),
                ReferenceSpec("second_step_value_ref", "second_step_value"),
                ReferenceSpec("first_value_ref", "first_value"),
                ReferenceSpec("second_value_ref", "second_value"),
                ReferenceSpec("first_capture_left", "outer_left"),
                ReferenceSpec("second_capture_right", "outer_right"),
                ReferenceSpec("forward_b", "resource_b"),
                ReferenceSpec("body_a_ref", "resource_a"),
                ReferenceSpec("outer_return_left", "outer_left"),
                ReferenceSpec("operation_call", "operation_decl"),
                ReferenceSpec("first_call", "first_decl"),
                ReferenceSpec("second_call", "second_decl"),
                ReferenceSpec("first_outer_left", "outer_left"),
                ReferenceSpec("second_outer_right", "outer_right"),
                ReferenceSpec("initializer_run", "top_run"),
                ReferenceSpec("box_call", "box_ctor"),
            )
            Case.FORWARD_NESTED_RESOURCES -> listOf(
                ReferenceSpec("nested_forward_p", "resource_p"),
                ReferenceSpec("mapper_capture_p", "resource_p"),
                ReferenceSpec("body_q_ref", "resource_q"),
                ReferenceSpec("mapper_call", "mapper_decl"),
                ReferenceSpec("nested_left", "outer_left"),
                ReferenceSpec("nested_right", "outer_right"),
                ReferenceSpec("final_left", "outer_left"),
                ReferenceSpec("final_right", "outer_right"),
                ReferenceSpec("forward_b", "resource_b"),
                ReferenceSpec("body_a_ref", "resource_a"),
                ReferenceSpec("outer_return_left", "outer_left"),
                ReferenceSpec("operation_call", "operation_decl"),
                ReferenceSpec("initializer_run", "top_run"),
                ReferenceSpec("box_call", "box_ctor"),
                ReferenceSpec("nested_box_call", "box_ctor"),
            )
        }

        val callAnchors: List<String> get() = when (case) {
            Case.LOCAL_CLOSURE_FORWARD -> listOf("initializer_run", "box_call", "relay_left_call", "relay_right_call", "operation_call")
            Case.SAME_NAMES, Case.FORWARD_SAME_NAMES -> listOf("initializer_run", "first_step_call", "second_step_call", "box_call", "first_call", "second_call", "operation_call")
            Case.NESTED_RESOURCES, Case.FORWARD_NESTED_RESOURCES -> listOf("initializer_run", "nested_box_call", "mapper_call", "box_call", "operation_call")
            Case.EXCEPTIONAL -> listOf("initializer_run", "nested_box_call", "normal_record", "caught_call", "catch_record",
                "finally_record", "box_call", "outer_body_record", "normal_operation_call", "recovered_operation_call")
        }
    }
}
