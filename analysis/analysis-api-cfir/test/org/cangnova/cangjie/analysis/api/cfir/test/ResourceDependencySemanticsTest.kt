package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaErrorType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.cfir.declarations.CfirResourceVariable
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTryResource
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * 官方 cjc 1.1.3 确认的声明依赖边界：名义锚点、无锚点环、注解锚点及 initializer 外层绑定。
 * 每个 fixture 先从源码绑定或引用首查，完成类型和身份验收后才收集完整诊断，禁止诊断预热资源图。
 */
@Timeout(30)
class ResourceDependencySemanticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/resourceDependencySemantics",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** b 的成员接收者通过 a=c 依赖后面的名义锚点；类型正确不消除 c 的初始化前使用错误。 */
    @Test
    fun memberForwardAnchor(mainFile: CjFile, services: TestServices) {
        val fixture = Fixture(mainFile, services)
        fixture.assertResourceShape(annotatedA = false)
        analyzeForTest(mainFile) {
            with(this as CaCfirSession) {
                val first = fixture.resource("b").bindingSymbol!!
                assertResType(first.returnType, fixture)

                val bindings = readResources(fixture, "a", "b", "c")
                bindings.values.forEach { assertResType(it.symbol.returnType, fixture) }
                assertSame(cfirSymbol(first), cfirSymbol(bindings.getValue("b").symbol))

                val forward = assertVariableReference(fixture.use("forwardC"), bindings.getValue("c").symbol)
                assertSame(bindings.getValue("a").node.initializer, forward)
                assertResType(fixture.use("forwardC").expressionType!!, fixture)
                assertVariableReference(fixture.use("receiverA"), bindings.getValue("a").symbol)
                assertResType(fixture.use("receiverA").expressionType!!, fixture)

                assertMemberReference(fixture, bindings.getValue("b"))
                assertConstructorReferences(fixture, "anchorConstructor", "memberBodyConstructor")
                assertExactDiagnostics(mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS), fixture.use("forwardC"))
            }
        }
    }

    /** 无名义锚点的环全部为 Error；已有 Res.next 声明不能使失败成员引用取得猜测的目标。 */
    @Test
    fun memberCycleUnanchored(mainFile: CjFile, services: TestServices) {
        val fixture = Fixture(mainFile, services)
        fixture.assertResourceShape(annotatedA = false)
        analyzeForTest(mainFile) {
            with(this as CaCfirSession) {
                assertErrorType(fixture.resource("b").bindingSymbol!!.returnType)

                val bindings = readResources(fixture, "a", "b", "c")
                bindings.values.forEach {
                    assertErrorType(it.symbol.returnType)
                    assertTrue(it.node.symbol.resolvedReturnType is ConeErrorType)
                }
                val forward = assertVariableReference(fixture.use("forwardC"), bindings.getValue("c").symbol)
                val backward = assertVariableReference(fixture.use("backwardB"), bindings.getValue("b").symbol)
                assertSame(bindings.getValue("a").node.initializer, forward)
                assertSame(bindings.getValue("c").node.initializer, backward)
                assertVariableReference(fixture.use("receiverA"), bindings.getValue("a").symbol)
                listOf("forwardC", "receiverA", "backwardB").forEach {
                    assertErrorType(fixture.use(it).expressionType!!)
                }
                val memberCall = fixture.element<CjDotQualifiedExpression>("memberUse")
                assertErrorType(memberCall.expressionType!!)
                assertTrue(fixture.use("memberUse").resolveToSymbols().isEmpty(), "Invalid 接收者不能命中全局同名 next 声明。")

                // 独立方法体的合法构造器可以解析，但不能反向修复主函数中无锚点的资源环。
                assertConstructorReferences(fixture, "memberBodyConstructor")
                bindings.values.forEach { assertErrorType(it.symbol.returnType) }
                assertTrue(fixture.use("memberUse").resolveToSymbols().isEmpty())
                assertExactDiagnostics(mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS), fixture.use("forwardC"))
            }
        }
    }

    /** 先从 next 引用首查；a 的显式类型打断类型依赖，三个资源仍保留自己的声明身份和值初始化错误。 */
    @Test
    fun memberCycleAnnotated(mainFile: CjFile, services: TestServices) {
        val fixture = Fixture(mainFile, services)
        fixture.assertResourceShape(annotatedA = true)
        analyzeForTest(mainFile) {
            with(this as CaCfirSession) {
                val first = assertInstanceOf(
                    CaNamedFunctionSymbol::class.java,
                    fixture.use("memberUse").resolveToSymbols().single(),
                )

                val bindings = readResources(fixture, "a", "b", "c")
                bindings.values.forEach { assertResType(it.symbol.returnType, fixture) }
                val forward = assertVariableReference(fixture.use("forwardC"), bindings.getValue("c").symbol)
                val backward = assertVariableReference(fixture.use("backwardB"), bindings.getValue("b").symbol)
                assertSame(bindings.getValue("a").node.initializer, forward)
                assertSame(bindings.getValue("c").node.initializer, backward)
                assertVariableReference(fixture.use("receiverA"), bindings.getValue("a").symbol)
                listOf("forwardC", "receiverA", "backwardB").forEach {
                    assertResType(fixture.use(it).expressionType!!, fixture)
                }
                val member = assertMemberReference(fixture, bindings.getValue("b"))
                assertSame(cfirSymbol(first), cfirSymbol(member))
                assertConstructorReferences(fixture, "memberBodyConstructor")
                assertExactDiagnostics(mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS), fixture.use("forwardC"))
            }
        }
    }

    /** initializer 的 b 指向外层模式绑定，try body 的 b 指向新资源绑定，两个符号不可合并。 */
    @Test
    fun selfShadow(mainFile: CjFile, services: TestServices) {
        val fixture = Fixture(mainFile, services)
        assertEquals(listOf("b"), fixture.resources.map { it.name })
        val outerPsi = fixture.element<CjBindingPattern>("outerDeclaration")
        analyzeForTest(mainFile) {
            with(this as CaCfirSession) {
                val first = assertInstanceOf(
                    CaPatternBindingSymbol::class.java,
                    fixture.use("initializerOuter").resolveToSymbols().single(),
                )
                assertSame(outerPsi, first.psi, "首查 initializer 必须解析到外层 b。")

                val outer = outerPsi.bindingSymbol!!
                val binding = readResources(fixture, "b").getValue("b")
                assertEquals(outer, first)
                assertSame(cfirSymbol(outer), cfirSymbol(first))
                assertNotEquals(outer, binding.symbol)
                assertNotSame(cfirSymbol(outer), binding.node.symbol)
                assertResType(outer.returnType, fixture)
                assertResType(binding.symbol.returnType, fixture)

                val initializer = assertVariableReference(fixture.use("initializerOuter"), outer)
                assertSame(binding.node.initializer, initializer)
                assertResType(fixture.use("initializerOuter").expressionType!!, fixture)
                assertVariableReference(fixture.use("bodyResource"), binding.symbol)
                assertResType(fixture.use("bodyResource").expressionType!!, fixture)
                val value = fixture.element<CjBindingPattern>("valueDeclaration").bindingSymbol!!
                assertResType(value.returnType, fixture)
                assertConstructorReferences(fixture, "outerConstructor")
                assertExactDiagnostics(mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS))
            }
        }
    }

    /** 所有资源保持独立的局部变量符号及同一正式 CFIR 节点，公共类型须与 CFIR 最终类型一致。 */
    private fun CaCfirSession.readResources(fixture: Fixture, vararg names: String): Map<String, ResourceBinding> {
        assertEquals(names.toList(), fixture.resources.map { it.name })
        val bindings = names.associateWith { name ->
            val psi = fixture.resource(name)
            val symbol = assertInstanceOf(CaLocalVariableSymbol::class.java, psi.bindingSymbol)
            assertSame(psi, symbol.psi)
            assertEquals(CaSymbolLocation.LOCAL, symbol.location)
            assertNull(symbol.callableId)
            assertTrue(symbol.isLet)
            val node = assertInstanceOf(CfirResourceVariable::class.java, psi.getOrBuildCfir(resolutionFacade))
            assertSame(psi, node.source!!.psi)
            assertSame(node.symbol, cfirSymbol(symbol))
            assertEquals(node.symbol.resolvedReturnType, (symbol.returnType as CaCfirType).coneType)
            assertSame(node, psi.getOrBuildCfir(resolutionFacade))
            ResourceBinding(psi, symbol, node)
        }
        val nodes = bindings.values.toList()
        for (index in nodes.indices) {
            for (other in index + 1 until nodes.size) {
                assertNotEquals(nodes[index].symbol, nodes[other].symbol)
                assertNotSame(nodes[index].node, nodes[other].node)
                assertNotSame(nodes[index].node.symbol, nodes[other].node.symbol)
            }
        }
        return bindings
    }

    /** 共享变量引用入口保留原始符号族；模式绑定与资源局部绑定由各用例独立严格校验。 */
    private fun CaCfirSession.assertVariableReference(reference: CjSimpleNameExpression, expected: CaVariableSymbol): CfirQualifiedAccessExpression {
        assertReferenceIdentity(reference, expected)
        val node = assertInstanceOf(CfirQualifiedAccessExpression::class.java, reference.getOrBuildCfir(resolutionFacade))
        val resolved = assertInstanceOf(CfirResolvedNamedReference::class.java, node.calleeReference)
        assertSame(cfirSymbol(expected), resolved.resolvedSymbol)
        return node
    }

    private fun CaCfirSession.assertReferenceIdentity(reference: CjSimpleNameExpression, expected: CaSymbol) {
        val actual = reference.resolveToSymbols().toList()
        assertEquals(1, actual.size, reference.text)
        assertSame(expected.psi, actual.single().psi)
        assertSame(cfirSymbol(expected), cfirSymbol(actual.single()))
        assertEquals(listOf(expected), actual, reference.text)
    }

    /** next 必须是源码 Res.next，调用树也必须是资源 b 的正式 initializer。 */
    private fun CaCfirSession.assertMemberReference(fixture: Fixture, binding: ResourceBinding): CaNamedFunctionSymbol {
        val declaration = fixture.element<CjNamedFunction>("memberDeclaration")
        val symbol = declaration.symbol
        assertSame(declaration, symbol.psi)
        assertTrue(symbol.valueParameters.isEmpty())
        assertResType(symbol.returnType, fixture)
        val call = fixture.element<CjDotQualifiedExpression>("memberUse")
        val node = assertCallableReference(fixture.use("memberUse"), call, symbol)
        assertSame(binding.node.initializer, node)
        assertResType(call.expressionType!!, fixture)
        return symbol
    }

    /** 独立构造位置必须取得 Res 自身作用域中的同一个构造器，而不是仅返回同名 class。 */
    private fun CaCfirSession.assertConstructorReferences(fixture: Fixture, vararg markers: String) {
        val owner = assertInstanceOf(CaClassLikeSymbol::class.java, fixture.classDeclaration.symbol)
        val constructor = owner.declaredMemberScope.constructors.single()
        assertInstanceOf(CaConstructorSymbol::class.java, constructor)
        assertEquals(fixture.resClassName, constructor.containingClassId!!.asSingleFqName().asString())
        assertTrue(constructor.valueParameters.isEmpty())
        for (marker in markers) {
            val call = fixture.element<CjCallExpression>(marker)
            assertCallableReference(fixture.use(marker), call, constructor)
            assertResType(call.expressionType!!, fixture)
        }
    }

    private fun CaCfirSession.assertCallableReference(reference: CjSimpleNameExpression, call: CjExpression, expected: CaCallableSymbol): CfirQualifiedAccessExpression {
        assertReferenceIdentity(reference, expected)
        val node = assertInstanceOf(CfirQualifiedAccessExpression::class.java, call.getOrBuildCfir(resolutionFacade))
        val resolved = assertInstanceOf(CfirResolvedNamedReference::class.java, node.calleeReference)
        assertSame(cfirSymbol(expected), resolved.resolvedSymbol)
        return node
    }

    private fun assertResType(type: CaType, fixture: Fixture) {
        val actual = assertInstanceOf(CaUsualClassType::class.java, type)
        assertEquals(fixture.resClassName, actual.classId.asSingleFqName().asString())
        assertTrue(actual.typeArguments.isEmpty())
        assertNotNull(actual.symbol)
        assertSame(fixture.classDeclaration, actual.symbol!!.psi)
    }

    private fun assertErrorType(type: CaType) {
        assertTrue(type is CaErrorType, "官方 Invalid 不能变成 Res、Resource、Any 或未完成 TV：$type")
        assertTrue((type as CaCfirType).coneType is ConeErrorType)
    }

    /** 精确比较完整工厂名、全部区间和数量，拒绝额外级联错误、错误范围及重复诊断。 */
    private fun assertExactDiagnostics(diagnostics: Collection<CaDiagnosticWithPsi<*>>, vararg forwardUses: CjSimpleNameExpression) {
        val expected = forwardUses.map { "USED_BEFORE_INITIALIZATION" to listOf(it.textRange) }
        val actual = diagnostics.map { it.factoryName to it.textRanges.toList() }
        assertEquals(expected, actual, actual.joinToString())
    }

    private fun cfirSymbol(symbol: CaSymbol) = (symbol as CaCfirSymbol<*>).cfirSymbol

    /** 只在当前 analyze 生命周期内持有资源的源码、公开符号与正式图节点。 */
    private data class ResourceBinding(val psi: CjTryResource, val symbol: CaLocalVariableSymbol, val node: CfirResourceVariable)

    /** fixture 初始化只读 PSI 和 marker，不能通过类型、引用或 diagnostics 查询预热。 */
    private class Fixture(val file: CjFile, private val services: TestServices) {
        val resources: List<CjTryResource> = PsiTreeUtil.findChildrenOfType(file, CjTryResource::class.java).sortedBy { it.textOffset }
        val classDeclaration: CjClass = element("resDeclaration")
        val resClassName: String = "${file.packageFqName.asString()}.Res"

        fun resource(name: String): CjTryResource = resources.single { it.name == name }

        fun assertResourceShape(annotatedA: Boolean) {
            assertEquals(listOf("a", "b", "c"), resources.map { it.name })
            assertEquals(annotatedA, resource("a").parameter!!.typeReference != null)
            assertNull(resource("b").parameter!!.typeReference)
            assertNull(resource("c").parameter!!.typeReference)
        }

        inline fun <reified T : PsiElement> element(marker: String): T =
            services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<T>(file, marker)

        fun use(marker: String): CjSimpleNameExpression = element(marker)
    }
}
