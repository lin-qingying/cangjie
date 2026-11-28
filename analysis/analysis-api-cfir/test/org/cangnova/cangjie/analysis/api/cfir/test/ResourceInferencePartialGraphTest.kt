package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.types.CaErrorType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.CfirLazyBodiesCalculator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirResourceVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTryResource
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * 双输入 lambda 的资源试算必须保留整个外层约束图，完成两个输入与 Box2 的同一个泛型实参。
 * 每个入口使用独立 fixture，首查前不收集 diagnostics；语义依据见工作区内的 cjc 1.1.3 证据。
 */
@Timeout(30)
class ResourceInferencePartialGraphTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/resourceInferencePartialGraph",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun multiInputDeclaration(mainFile: CjFile, services: TestServices) =
        verify(mainFile, services, Case.MULTI_INPUT, Entry.DECLARATION)

    @Test
    fun multiInputUse(mainFile: CjFile, services: TestServices) =
        verify(mainFile, services, Case.MULTI_INPUT, Entry.USE)

    @Test
    fun multiInputDirectSymbol(mainFile: CjFile, services: TestServices) =
        verify(mainFile, services, Case.MULTI_INPUT, Entry.DIRECT_SYMBOL)

    @Test
    fun typedOperationDeclaration(mainFile: CjFile, services: TestServices) =
        verify(mainFile, services, Case.TYPED_OPERATION, Entry.DECLARATION)

    @Test
    fun typedOperationUse(mainFile: CjFile, services: TestServices) =
        verify(mainFile, services, Case.TYPED_OPERATION, Entry.USE)

    @Test
    fun typedOperationDirectSymbol(mainFile: CjFile, services: TestServices) =
        verify(mainFile, services, Case.TYPED_OPERATION, Entry.DIRECT_SYMBOL)

    @Test
    fun noReverseDeclaration(mainFile: CjFile, services: TestServices) =
        verify(mainFile, services, Case.NO_REVERSE, Entry.DECLARATION)

    @Test
    fun noReverseUse(mainFile: CjFile, services: TestServices) =
        verify(mainFile, services, Case.NO_REVERSE, Entry.USE)

    @Test
    fun noReverseDirectSymbol(mainFile: CjFile, services: TestServices) =
        verify(mainFile, services, Case.NO_REVERSE, Entry.DIRECT_SYMBOL)

    @Test
    fun orderedDeclaration(mainFile: CjFile, services: TestServices) =
        verify(mainFile, services, Case.ORDERED, Entry.DECLARATION)

    @Test
    fun orderedUse(mainFile: CjFile, services: TestServices) =
        verify(mainFile, services, Case.ORDERED, Entry.USE)

    @Test
    fun orderedDirectSymbol(mainFile: CjFile, services: TestServices) =
        verify(mainFile, services, Case.ORDERED, Entry.DIRECT_SYMBOL)

    private enum class Entry { DECLARATION, USE, DIRECT_SYMBOL }
    private enum class Case { MULTI_INPUT, TYPED_OPERATION, NO_REVERSE, ORDERED }

    /** 独立的声明、使用与 CFIR 符号入口，禁止预查外层函数值或参数来提前完成资源图。 */
    private fun verify(mainFile: CjFile, services: TestServices, case: Case, entry: Entry) {
        val fixture = Fixture(mainFile, services, case)
        val (previousToken, pointers) = analyzeForTest(mainFile) {
            with(this as CaCfirSession) {
                val firstResource = fixture.resource(if (case == Case.NO_REVERSE) "b" else "a")
                val firstType = when (entry) {
                    Entry.DECLARATION -> firstResource.bindingSymbol!!.returnType
                    Entry.USE -> fixture.reference.expressionType!!
                    Entry.DIRECT_SYMBOL -> {
                        val rawNodes = rawResourceNodes(mainFile)
                        val node = rawNodes.single { it.source!!.psi === firstResource }
                        val coneType = node.symbol.resolvedReturnType
                        val direct = cfirSymbolBuilder.buildSymbol(node.symbol) as CaLocalVariableSymbol
                        direct.returnType.also {
                            assertEquals(coneType, (it as CaCfirType).coneType)
                            rawNodes.forEach { original ->
                                assertSame(original, resourceNode(original.source!!.psi as CjTryResource))
                            }
                        }
                    }
                }
                if (case == Case.NO_REVERSE) assertUninferred(firstType) else assertBox2Int64(firstType)
                val symbols = assertGraph(fixture)
                assertExactDiagnostics(fixture)
                token to symbols.map { (psi, symbol) -> psi to symbol.createPointer() }
            }
        }

        CaSessionProvider.getInstance(mainFile.project).clearCaches()
        analyzeForTest(fixture.operationUse) {
            with(this as CaCfirSession) {
                // 只比较失效 token 的对象身份；所有类型均在新会话重新查询。
                assertNotSame(previousToken, token, "pointer 必须在实际新建的会话中恢复。")
                val restored = pointers.map { (psi, pointer) ->
                    val symbol = restoreSymbol(pointer)!!
                    assertSame(psi, symbol.psi)
                    psi to symbol
                }
                val current = assertGraph(fixture)
                restored.zip(current).forEach { (oldEntry, currentEntry) ->
                    assertSame(oldEntry.first, currentEntry.first)
                    assertEquals(currentEntry.second, oldEntry.second)
                    assertSame(
                        (currentEntry.second as CaCfirSymbol<*>).cfirSymbol,
                        (oldEntry.second as CaCfirSymbol<*>).cfirSymbol,
                    )
                    assertEquals(
                        ((currentEntry.second as CaCallableSymbol).returnType as CaCfirType).coneType,
                        ((oldEntry.second as CaCallableSymbol).returnType as CaCfirType).coneType,
                        "恢复后的符号必须在新会话读取相同的最终类型。",
                    )
                }
                assertExactDiagnostics(fixture)
            }
        }
    }

    /** 声明、使用与直接构造必须保持相同 CFIR 声明身份，公开类型必须读取完成后的 Cone 类型。 */
    private fun CaCfirSession.assertGraph(fixture: Fixture): List<Pair<PsiElement, CaSymbol>> {
        val aPsi = fixture.resource("a")
        val bPsi = fixture.resource("b")
        val a = aPsi.bindingSymbol!!
        val b = bPsi.bindingSymbol!!
        val aNode = assertResourceIdentity(aPsi, a)
        val bNode = assertResourceIdentity(bPsi, b)
        assertNotSame(aNode, bNode)
        assertNotSame(aNode.symbol, bNode.symbol, "共享同一资源对象不意味着共享声明符号。")
        assertNotEquals(a, b)

        val referenceSymbol = fixture.reference.resolveToSymbol()!!
        assertEquals(b, referenceSymbol)
        assertSame(bPsi, referenceSymbol.psi)
        assertSame(bNode.symbol, (referenceSymbol as CaCfirSymbol<*>).cfirSymbol)
        val referenceNode = fixture.reference.getOrBuildCfir(resolutionFacade) as CfirQualifiedAccessExpression
        assertSame(bNode.symbol, (referenceNode.calleeReference as CfirResolvedNamedReference).resolvedSymbol)

        if (fixture.case == Case.NO_REVERSE) {
            // cjc 的 b、引用和 Empty() 均为 Invalid；a 的注解不能反向完成 b 的泛型实参。
            assertUninferred(b.returnType)
            assertUninferred(fixture.reference.expressionType!!)
            assertUninferred(fixture.element<CjCallExpression>("uninferredConstructor").expressionType!!)
        } else {
            assertBox2Int64(a.returnType)
            assertBox2Int64(b.returnType)
            assertBox2Int64(fixture.reference.expressionType!!)
            assertBox2Int64(fixture.element<CjCallExpression>("resourceConstructor").expressionType!!)
            val resourceUse = fixture.use("resourceUse")
            assertEquals(a, resourceUse.resolveToSymbol())
            assertSame(aPsi, resourceUse.resolveToSymbol()!!.psi)
            assertSame(aNode.symbol, (resourceUse.resolveToSymbol() as CaCfirSymbol<*>).cfirSymbol)
            assertBox2Int64(resourceUse.expressionType!!)
            val useNode = resourceUse.getOrBuildCfir(resolutionFacade) as CfirQualifiedAccessExpression
            assertSame(aNode.symbol, (useNode.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
            assertInt64(fixture.element<CjDotQualifiedExpression>("resourceGet").expressionType!!)
        }

        val inputs = fixture.parameters.map { it.symbol }
        assertNotEquals(inputs[0], inputs[1], "两个外层输入必须保留各自声明身份。")
        inputs.forEachIndexed { index, input ->
            assertSame(fixture.parameters[index], input.psi)
            assertInt64(input.returnType)
        }
        val leftUses = if (fixture.case == Case.NO_REVERSE) listOf("bodyLeft", "returnLeft")
            else listOf("initializerLeft", "returnLeft")
        leftUses.forEach { marker ->
            val use = fixture.use(marker)
            assertEquals(inputs[0], use.resolveToSymbol())
            assertSame(fixture.parameters[0], use.resolveToSymbol()!!.psi)
            assertInt64(use.expressionType!!)
        }
        if (fixture.case != Case.NO_REVERSE) {
            val right = fixture.use("initializerRight")
            assertEquals(inputs[1], right.resolveToSymbol())
            assertSame(fixture.parameters[1], right.resolveToSymbol()!!.psi)
            assertInt64(right.expressionType!!)
            assertBinaryFunction(fixture.lambda.expressionType!!)
        }

        val operation = fixture.operation.bindingSymbol!!
        val operationType = operation.returnType
        assertBinaryFunction(operationType)
        assertBinaryFunction(fixture.operationUse.expressionType!!)
        assertTrue(operationType.semanticallyEquals(fixture.operationUse.expressionType!!))
        assertEquals(operation, fixture.operationUse.resolveToSymbol())
        assertSame(fixture.operation, fixture.operationUse.resolveToSymbol()!!.psi)
        assertSame(
            (operation as CaCfirSymbol<*>).cfirSymbol,
            (fixture.operationUse.resolveToSymbol() as CaCfirSymbol<*>).cfirSymbol,
        )
        assertInt64(fixture.element<CjCallExpression>("operationUse").expressionType!!)
        val result = fixture.result.bindingSymbol!!
        assertInt64(result.returnType)
        return listOf(aPsi to a, bPsi to b) + fixture.parameters.zip(inputs) +
            listOf(fixture.operation to operation, fixture.result to result)
    }

    private fun CaCfirSession.resourceNode(psi: CjTryResource): CfirResourceVariable =
        assertInstanceOf(CfirResourceVariable::class.java, psi.getOrBuildCfir(resolutionFacade))

    /** 从未解析文件找到原始资源符号，直接入口不得用 getOrBuildCfir 提前完成整个函数正文。 */
    @OptIn(LLCfirInternals::class)
    private fun CaCfirSession.rawResourceNodes(file: CjFile): List<CfirResourceVariable> {
        val cfirFile = file.getOrBuildCfirFile(resolutionFacade)
        val main = cfirFile.declarations.filterIsInstance<CfirMainFunction>().single()
        assertEquals(CfirResolvePhase.RAW_CFIR, main.resolvePhase)
        CfirLazyBodiesCalculator.calculateBodies(CfirDesignation(listOf(cfirFile), main))
        assertEquals(CfirResolvePhase.RAW_CFIR, main.resolvePhase, "重建 raw body 不得推进正文解析阶段。")
        val resources = mutableListOf<CfirResourceVariable>()
        main.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is CfirResourceVariable) resources += element
                element.acceptChildren(this)
            }
        })
        assertEquals(2, resources.size)
        resources.forEach { assertEquals(CfirResolvePhase.RAW_CFIR, it.resolvePhase) }
        assertTrue(resources.single { it.name.asString() == "b" }.returnTypeRef is CfirImplicitTypeRef)
        return resources
    }

    private fun CaCfirSession.assertResourceIdentity(psi: CjTryResource, symbol: CaLocalVariableSymbol): CfirResourceVariable {
        assertSame(psi, symbol.psi)
        assertEquals(CaSymbolLocation.LOCAL, symbol.location)
        assertNull(symbol.callableId)
        assertTrue(symbol.isLet)
        val node = resourceNode(psi)
        assertSame(psi, node.source!!.psi)
        assertSame(node.symbol, (symbol as CaCfirSymbol<*>).cfirSymbol)
        val direct = cfirSymbolBuilder.buildSymbol(node.symbol) as CaLocalVariableSymbol
        assertEquals(symbol, direct)
        assertSame(psi, direct.psi)
        assertSame(node.symbol, (direct as CaCfirSymbol<*>).cfirSymbol)
        assertEquals(node.symbol.resolvedReturnType, (symbol.returnType as CaCfirType).coneType)
        assertEquals(node.symbol.resolvedReturnType, (direct.returnType as CaCfirType).coneType)
        assertSame(node, resourceNode(psi), "试算完成后不得替换资源声明。")
        return node
    }

    private fun CaCfirSession.assertExactDiagnostics(fixture: Fixture) {
        val expected = when (fixture.case) {
            Case.MULTI_INPUT, Case.TYPED_OPERATION -> listOf("USED_BEFORE_INITIALIZATION" to fixture.reference)
            Case.NO_REVERSE -> listOf(
                "USED_BEFORE_INITIALIZATION" to fixture.reference,
                "UNABLE_TO_INFER_GENERIC_FUNC" to fixture.use("uninferredConstructor"),
            )
            Case.ORDERED -> emptyList()
        }
        assertDiagnostics(fixture.file.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS), expected)
    }

    /** 比较完整工厂名、全部区间与重复次数，同时拒绝额外级联错误。 */
    private fun assertDiagnostics(diagnostics: Collection<CaDiagnosticWithPsi<*>>, expected: List<Pair<String, PsiElement>>) {
        assertEquals(
            expected.map { (factory, psi) -> factory to listOf(psi.textRange) }.groupingBy { it }.eachCount(),
            diagnostics.map { it.factoryName to it.textRanges.toList() }.groupingBy { it }.eachCount(),
            diagnostics.joinToString { "${it.factoryName}@${it.textRanges}" },
        )
    }

    private fun assertBinaryFunction(type: CaType) {
        val function = assertInstanceOf(CaFunctionType::class.java, type)
        assertEquals(2, function.parameterTypes.size)
        function.parameterTypes.forEach(::assertInt64)
        assertInt64(function.returnType)
    }

    private fun assertBox2Int64(type: CaType) {
        val box = assertInstanceOf(CaUsualClassType::class.java, type)
        assertEquals("test.resourcepartial.Box2", box.classId.asSingleFqName().asString())
        assertInt64(box.typeArguments.single())
    }

    private fun assertInt64(type: CaType) {
        assertEquals(PrimitiveTypeKind.INT64, assertInstanceOf(CaPrimitiveType::class.java, type).kind)
    }

    private fun assertUninferred(type: CaType) {
        assertTrue(type is CaErrorType, "官方确认未能完成的泛型资源不得发布为具体类型：$type")
    }

    /** 只解析 PSI 结构与 marker，不通过符号查询预热正文。 */
    private class Fixture(val file: CjFile, private val services: TestServices, val case: Case) {
        private val resources = PsiTreeUtil.findChildrenOfType(file, CjTryResource::class.java).sortedBy { it.textOffset }
        val parameters = listOf(element<CjParameter>("leftParameter"), element<CjParameter>("rightParameter"))
        val lambda = PsiTreeUtil.getParentOfType(parameters[0], CjLambdaExpression::class.java)!!
        val operation = element<CjBindingPattern>("operationDeclaration")
        val result = element<CjBindingPattern>("resultDeclaration")
        val reference = use("resourceReference")
        val operationUse = use("operationUse")

        init {
            assertEquals(if (case == Case.ORDERED) listOf("b", "a") else listOf("a", "b"), resources.map { it.name })
            assertEquals(parameters, lambda.valueParameters)
            parameters.forEach { assertNull(it.typeReference, "形参必须通过约束图或函数声明上下文获得类型。") }
            assertFalse(lambda.hasDeclaredReturnType())
            assertNull(resource("b").parameter!!.typeReference)
            if (case == Case.NO_REVERSE) assertNotNull(resource("a").parameter!!.typeReference)
            else assertNull(resource("a").parameter!!.typeReference)
            val operationOwner = PsiTreeUtil.getParentOfType(operation, CjPatternVariable::class.java)!!
            if (case == Case.TYPED_OPERATION || case == Case.NO_REVERSE) assertNotNull(operationOwner.typeReference)
            else assertNull(operationOwner.typeReference)
        }

        fun resource(name: String): CjTryResource = resources.single { it.name == name }

        inline fun <reified T : PsiElement> element(marker: String): T =
            services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<T>(file, marker)

        fun use(marker: String): CjSimpleNameExpression = element(marker)
    }
}
