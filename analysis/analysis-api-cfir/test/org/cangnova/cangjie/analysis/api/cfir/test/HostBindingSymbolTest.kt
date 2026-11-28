package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaErrorType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.diagnostic.ConeConstraintSystemHasContradiction
import org.cangnova.cangjie.analysis.api.types.CaTypeParameterType
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjCatchParameter
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTryResource
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/** 资源和 catch 的真实绑定必须保持专用 PSI、局部变量符号族与跨会话 pointer 身份。 */
class HostBindingSymbolTest : AbstractAnalysisApiExecutionTest("analysis/analysis-api-cfir/testData/hostBindingSymbols") {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun resources(mainFile: CjFile, services: TestServices) {
        val resources = PsiTreeUtil.findChildrenOfType(mainFile, CjTryResource::class.java).toList()
        val firstUse = services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, "firstUse")
        val secondUse = services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, "secondUse")
        val following = services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, "following")
        lateinit var pointer: CaSymbolPointer<CaSymbol>
        analyzeForTest(mainFile) {
            val first = resources[0].bindingSymbol!!
            val second = resources[1].bindingSymbol!!
            assertLocal(first, resources[0])
            assertLocal(second, resources[1])
            assertSame(resources[0], firstUse.resolveToSymbol()!!.psi)
            assertSame(resources[0], following.resolveToSymbol()!!.psi)
            assertSame(resources[1], secondUse.resolveToSymbol()!!.psi)
            pointer = first.createPointer()
        }
        analyzeForTest(firstUse) { assertSame(resources[0], restoreSymbol(pointer)!!.psi) }
    }

    /** 首查隐式 first 的类型必须推导后序资源 initializer，而不能只发布后序资源名字。 */
    @Test
    fun resourcesForwardImplicit(mainFile: CjFile, services: TestServices) {
        val resources = PsiTreeUtil.findChildrenOfType(mainFile, CjTryResource::class.java).sortedBy { it.textOffset }
        assertEquals(listOf("first", "later"), resources.map { it.name })
        resources.forEach { assertNull(it.parameter!!.typeReference, "隐式资源用例不得依赖类型注解。") }
        val forward = services.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, "forward")
        val firstUse = services.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, "firstUse")
        val laterUse = services.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, "laterUse")

        analyzeForTest(mainFile) {
            // 先请求 first 的 returnType，不预查 later 类型，也不通过文件 diagnostics 预热正文。
            val first = resources[0].bindingSymbol!!
            assertManagedResourceType(first.returnType)
            val forwardSymbol = forward.resolveToSymbol()!!
            assertSame(resources[1], forwardSymbol.psi)
            val later = resources[1].bindingSymbol!!
            assertLocal(first, resources[0])
            assertLocal(later, resources[1])
            assertEquals(later, forwardSymbol)
            assertManagedResourceType(later.returnType)
            assertManagedResourceType(forward.expressionType!!)
            assertSame(resources[0], firstUse.resolveToSymbol()!!.psi)
            assertSame(resources[1], laterUse.resolveToSymbol()!!.psi)
            assertManagedResourceType(firstUse.expressionType!!)
            assertManagedResourceType(laterUse.expressionType!!)

            val diagnostics = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
            val rendered = diagnostics.joinToString { "${it.factoryName}@${it.textRanges}" }
            val beforeInitialization = diagnostics.filter { it.factoryName == "USED_BEFORE_INITIALIZATION" }
            assertEquals(1, diagnostics.size, "纯隐式用例只能产生官方确认的初始化前使用诊断：$rendered")
            assertEquals(1, beforeInitialization.size, rendered)
            assertEquals(listOf(forward.textRange), beforeInitialization.single().textRanges.toList(), rendered)
        }
    }

    /**
     * 官方资源名统一收集到 try 作用域，初始化按资源顺序检查；自身 initializer 排除当前声明。
     * 同名外层变量不得截获前向引用，资源与 try body 同级，退出 try 后仅外层声明可见。
     */
    @Test
    fun resourcesForward(mainFile: CjFile, services: TestServices) {
        fun use(marker: String): CjSimpleNameExpression =
            services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, marker)

        val resources = PsiTreeUtil.findChildrenOfType(mainFile, CjTryResource::class.java).sortedBy { it.textOffset }
        assertEquals(listOf("first", "later", "same"), resources.map { it.name })
        resources.take(2).forEach { assertNotNull(it.parameter!!.typeReference, "遮蔽用例同时覆盖显式资源类型。") }
        val outerDeclaration = services.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjBindingPattern>(mainFile, "outerDeclaration")
        val duplicateDeclaration = services.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjBindingPattern>(mainFile, "duplicateDeclaration")
        val forward = use("forward")
        val firstUse = use("firstUse")
        val laterUse = use("laterUse")
        val outerUses = listOf("selfInitializer", "catchOuter", "finallyOuter", "afterOuter").map(::use)
        val unavailableUses = listOf("catchUnavailable", "finallyUnavailable", "afterUnavailable").map(::use)
        lateinit var pointer: CaSymbolPointer<CaSymbol>

        analyzeForTest(mainFile) {
            // 首次符号请求从前向使用处进入，不能通过预查后序资源声明掩盖名字发布时序问题。
            val forwardSymbol = forward.resolveToSymbol()!!
            assertSame(resources[1], forwardSymbol.psi, "前向引用必须命中后序资源，不能命中外层同名变量。")
            assertTrue(forwardSymbol is CaLocalVariableSymbol)
            assertManagedResourceType((forwardSymbol as CaLocalVariableSymbol).returnType)
            val first = resources[0].bindingSymbol!!
            val later = resources[1].bindingSymbol!!
            assertLocal(first, resources[0])
            assertLocal(later, resources[1])
            assertManagedResourceType(first.returnType)
            assertManagedResourceType(later.returnType)
            assertManagedResourceType(forward.expressionType!!)
            assertEquals(later, forwardSymbol)
            assertSame(resources[0], firstUse.resolveToSymbol()!!.psi)
            assertSame(resources[1], laterUse.resolveToSymbol()!!.psi)
            val outer = outerDeclaration.bindingSymbol!!
            outerUses.forEach { expression ->
                val resolved = expression.resolveToSymbol()
                assertEquals(outer, resolved, "自身 initializer、catch/finally 和 try 后只能见外层 later。")
                assertSame(outerDeclaration, resolved!!.psi)
            }
            unavailableUses.forEach { expression ->
                assertNull(expression.resolveToSymbol(), "资源 first 不得泄漏到 catch/finally 或 try 之后。")
            }
            pointer = later.createPointer()

            val diagnostics = mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
            val rendered = diagnostics.joinToString { "${it.factoryName}@${it.textRanges}" }
            // sema_used_before_initialization → CfirErrors.USED_BEFORE_INITIALIZATION；API 去除 CFIR_ 前缀。
            val beforeInitialization = diagnostics.filter { it.factoryName == "USED_BEFORE_INITIALIZATION" }
            assertEquals(1, beforeInitialization.size, rendered)
            assertEquals(listOf(forward.textRange), beforeInitialization.single().textRanges.toList(), rendered)
            assertEquals(
                unavailableUses.map { it.textRange }.toSet(),
                diagnostics.filter { it.factoryName == "UNRESOLVED_REFERENCE" }.flatMap { it.textRanges }.toSet(),
                rendered,
            )
            assertTrue(
                diagnostics.any {
                    it.factoryName == "REDECLARATION" && duplicateDeclaration.nameIdentifier!!.textRange in it.textRanges
                },
                "资源与 try body 同级，同名声明必须报告重定义：$rendered",
            )
        }
        analyzeForTest(forward) { assertSame(resources[1], restoreSymbol(pointer)!!.psi) }
    }

    /** a → b → c 均无类型注解；首次请求 a 类型必须完成整条依赖链。 */
    @Test
    fun resourcesForwardChain(mainFile: CjFile, services: TestServices) {
        val resources = resourceDeclarations(mainFile, "a", "b", "c")
        resources.forEach { assertNull(it.parameter!!.typeReference) }
        val toMiddle = use(mainFile, services, "toMiddle")
        val toLast = use(mainFile, services, "toLast")
        val uses = listOf("firstUse", "middleUse", "lastUse").map { use(mainFile, services, it) }
        val value = element<CjDotQualifiedExpression>(mainFile, services, "firstValue")
        lateinit var pointers: List<CaSymbolPointer<CaSymbol>>

        analyzeForTest(mainFile) {
            val first = resources[0].bindingSymbol!!
            assertManagedResourceType(first.returnType)
            val bindings = listOf(first, resources[1].bindingSymbol!!, resources[2].bindingSymbol!!)
            bindings.zip(resources).forEach { (symbol, psi) ->
                assertLocal(symbol, psi)
                assertManagedResourceType(symbol.returnType)
            }
            assertEquals(bindings[1], toMiddle.resolveToSymbol())
            assertEquals(bindings[2], toLast.resolveToSymbol())
            assertManagedResourceType(toMiddle.expressionType!!)
            assertManagedResourceType(toLast.expressionType!!)
            uses.zip(bindings).forEach { (expression, symbol) ->
                assertEquals(symbol, expression.resolveToSymbol())
                assertSame(symbol.psi, expression.resolveToSymbol()!!.psi)
                assertManagedResourceType(expression.expressionType!!)
            }
            assertInt64(value.expressionType!!)
            pointers = bindings.map { it.createPointer() }
            assertDiagnostics(
                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS),
                "USED_BEFORE_INITIALIZATION" to toMiddle,
                "USED_BEFORE_INITIALIZATION" to toLast,
            )
        }
        analyzeForTest(toMiddle) {
            pointers.zip(resources).forEach { (pointer, psi) ->
                val restored = restoreSymbol(pointer) as CaLocalVariableSymbol
                assertLocal(restored, psi)
                assertManagedResourceType(restored.returnType)
            }
        }
    }

    /** 前向推断同时穿过资源引用、成员调用和 Box 构造器的泛型实参。 */
    @Test
    fun resourcesForwardGenericChain(mainFile: CjFile, services: TestServices) {
        val resources = resourceDeclarations(mainFile, "a", "b", "c")
        resources.forEach { assertNull(it.parameter!!.typeReference) }
        val toLater = use(mainFile, services, "toLater")
        val toLast = use(mainFile, services, "toLast")
        val firstUse = use(mainFile, services, "firstUse")
        val laterUse = use(mainFile, services, "laterUse")
        val lastUse = use(mainFile, services, "lastUse")
        val firstGet = element<CjDotQualifiedExpression>(mainFile, services, "firstGet")
        lateinit var pointer: CaSymbolPointer<CaSymbol>

        analyzeForTest(mainFile) {
            val first = resources[0].bindingSymbol!!
            assertInt64(assertClassType(first.returnType, "Box").typeArguments.single())
            val later = resources[1].bindingSymbol!!
            val last = resources[2].bindingSymbol!!
            listOf(first, later, last).zip(resources).forEach { (symbol, psi) -> assertLocal(symbol, psi) }
            assertInt64(assertClassType(later.returnType, "Box").typeArguments.single())
            assertManagedResourceType(last.returnType)
            assertEquals(later, toLater.resolveToSymbol())
            assertEquals(last, toLast.resolveToSymbol())
            assertEquals(first, firstUse.resolveToSymbol())
            assertEquals(later, laterUse.resolveToSymbol())
            assertEquals(last, lastUse.resolveToSymbol())
            assertInt64(assertClassType(firstUse.expressionType!!, "Box").typeArguments.single())
            assertInt64(assertClassType(toLater.expressionType!!, "Box").typeArguments.single())
            assertInt64(firstGet.expressionType!!)
            pointer = first.createPointer()
            assertDiagnostics(
                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS),
                "USED_BEFORE_INITIALIZATION" to toLater,
                "USED_BEFORE_INITIALIZATION" to toLast,
            )
        }
        analyzeForTest(firstUse) {
            val restored = restoreSymbol(pointer) as CaLocalVariableSymbol
            assertLocal(restored, resources[0])
            assertInt64(assertClassType(restored.returnType, "Box").typeArguments.single())
        }
    }

    /**
     * 在 a 的嵌套 lambda 内首次引用 b 时，b 的 initializer 必须使用资源组准备时的上下文。
     * lambda/body 的 String item 不得覆盖函数形参 item: T，Box<T> 的 T 必须属于外层函数。
     */
    @Test
    fun resourcesPreparationContext(mainFile: CjFile, services: TestServices) {
        val resources = resourceDeclarations(mainFile, "a", "b")
        resources.forEach { assertNull(it.parameter!!.typeReference) }
        val typeParameter = element<CjTypeParameter>(mainFile, services, "typeParameter")
        val parameter = element<CjParameter>(mainFile, services, "parameter")
        val lambdaDeclaration = element<CjBindingPattern>(mainFile, services, "lambdaDeclaration")
        val bodyDeclaration = element<CjBindingPattern>(mainFile, services, "bodyDeclaration")
        val value = element<CjBindingPattern>(mainFile, services, "valueDeclaration")
        val other = element<CjBindingPattern>(mainFile, services, "otherDeclaration")
        val toLater = use(mainFile, services, "toLater")
        val initializerParameter = use(mainFile, services, "initializerParameter")
        val lambdaUse = use(mainFile, services, "lambdaUse")
        val bodyUse = use(mainFile, services, "bodyUse")
        val firstUse = use(mainFile, services, "firstUse")
        val laterUse = use(mainFile, services, "laterUse")
        lateinit var pointers: List<CaSymbolPointer<CaSymbol>>

        analyzeForTest(mainFile) {
            // 先求 a 的完整函数实参类型；不能预查 b 或 diagnostics 来绕过嵌套上下文。
            val first = resources[0].bindingSymbol!!
            val closure = assertClassType(first.returnType, "Box").typeArguments.single()
            assertTrue(closure is CaFunctionType)
            assertTrue((closure as CaFunctionType).parameterTypes.isEmpty())
            assertTypeParameter(closure.returnType, typeParameter)
            val later = resources[1].bindingSymbol!!
            assertTypeParameter(assertClassType(later.returnType, "Box").typeArguments.single(), typeParameter)
            assertLocal(first, resources[0])
            assertLocal(later, resources[1])
            assertEquals(later, toLater.resolveToSymbol())
            assertSame(resources[1], toLater.resolveToSymbol()!!.psi)
            assertSame(parameter, initializerParameter.resolveToSymbol()!!.psi)
            assertEquals(parameter.symbol, initializerParameter.resolveToSymbol())
            assertTypeParameter(initializerParameter.expressionType!!, typeParameter)
            assertEquals(lambdaDeclaration.bindingSymbol, lambdaUse.resolveToSymbol())
            assertSame(lambdaDeclaration, lambdaUse.resolveToSymbol()!!.psi)
            assertStringType(lambdaDeclaration.bindingSymbol!!.returnType)
            assertStringType(lambdaUse.expressionType!!)
            assertEquals(bodyDeclaration.bindingSymbol, bodyUse.resolveToSymbol())
            assertSame(bodyDeclaration, bodyUse.resolveToSymbol()!!.psi)
            assertStringType(bodyDeclaration.bindingSymbol!!.returnType)
            assertStringType(bodyUse.expressionType!!)
            assertEquals(first, firstUse.resolveToSymbol())
            assertEquals(later, laterUse.resolveToSymbol())
            assertTypeParameter(value.bindingSymbol!!.returnType, typeParameter)
            assertTypeParameter(other.bindingSymbol!!.returnType, typeParameter)
            pointers = listOf(first, later, parameter.symbol).map { it.createPointer() }
            assertDiagnostics(
                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS),
                "CAPTURE_BEFORE_INITIALIZATION" to toLater,
            )
        }
        analyzeForTest(toLater) {
            assertSame(resources[0], restoreSymbol(pointers[0])!!.psi)
            val later = restoreSymbol(pointers[1]) as CaLocalVariableSymbol
            assertLocal(later, resources[1])
            assertTypeParameter(assertClassType(later.returnType, "Box").typeArguments.single(), typeParameter)
            assertSame(parameter, restoreSymbol(pointers[2])!!.psi)
        }
    }

    /** a 的 initializer 命中资源 r；r 的自身 initializer 排除自己，命中外层 Int64 r。 */
    @Test
    fun resourcesForwardOwnInitializer(mainFile: CjFile, services: TestServices) {
        val resources = resourceDeclarations(mainFile, "a", "r")
        resources.forEach { assertNull(it.parameter!!.typeReference) }
        val outerDeclaration = element<CjBindingPattern>(mainFile, services, "outerDeclaration")
        val forward = use(mainFile, services, "toLater")
        val ownInitializer = use(mainFile, services, "selfInitializer")
        val afterOuter = use(mainFile, services, "afterOuter")
        val firstUse = use(mainFile, services, "firstUse")
        val itemUse = use(mainFile, services, "itemUse")
        lateinit var pointers: List<CaSymbolPointer<CaSymbol>>

        analyzeForTest(mainFile) {
            val first = resources[0].bindingSymbol!!
            assertInt64(assertClassType(first.returnType, "Box").typeArguments.single())
            val resource = resources[1].bindingSymbol!!
            val outer = outerDeclaration.bindingSymbol!!
            assertLocal(first, resources[0])
            assertLocal(resource, resources[1])
            assertInt64(assertClassType(resource.returnType, "Box").typeArguments.single())
            assertInt64(outer.returnType)
            assertEquals(resource, forward.resolveToSymbol())
            assertSame(resources[1], forward.resolveToSymbol()!!.psi)
            listOf(ownInitializer, afterOuter).forEach {
                assertEquals(outer, it.resolveToSymbol())
                assertSame(outerDeclaration, it.resolveToSymbol()!!.psi)
                assertInt64(it.expressionType!!)
            }
            assertEquals(first, firstUse.resolveToSymbol())
            assertEquals(resource, itemUse.resolveToSymbol())
            pointers = listOf(first, resource, outer).map { it.createPointer() }
            assertDiagnostics(
                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS),
                "USED_BEFORE_INITIALIZATION" to forward,
            )
        }
        analyzeForTest(ownInitializer) {
            pointers.zip(listOf(resources[0], resources[1], outerDeclaration)).forEach { (pointer, psi) ->
                assertSame(psi, restoreSymbol(pointer)!!.psi)
            }
        }
    }

    /** 外层资源型 r 是自身 initializer 的合法目标；两个同名绑定仍须保持不同身份。 */
    @Test
    fun resourcesOwnInitializerOuter(mainFile: CjFile, services: TestServices) {
        val resource = resourceDeclarations(mainFile, "r").single()
        assertNull(resource.parameter!!.typeReference)
        val outerDeclaration = element<CjBindingPattern>(mainFile, services, "outerDeclaration")
        val ownInitializer = use(mainFile, services, "selfInitializer")
        val resourceUse = use(mainFile, services, "resourceUse")
        val afterOuter = use(mainFile, services, "afterOuter")
        lateinit var pointers: List<CaSymbolPointer<CaSymbol>>

        analyzeForTest(mainFile) {
            val inner = resource.bindingSymbol!!
            assertManagedResourceType(inner.returnType)
            val outer = outerDeclaration.bindingSymbol!!
            assertLocal(inner, resource)
            assertManagedResourceType(outer.returnType)
            assertNotEquals(outer, inner)
            listOf(ownInitializer, afterOuter).forEach {
                assertEquals(outer, it.resolveToSymbol())
                assertSame(outerDeclaration, it.resolveToSymbol()!!.psi)
            }
            assertEquals(inner, resourceUse.resolveToSymbol())
            assertSame(resource, resourceUse.resolveToSymbol()!!.psi)
            pointers = listOf(inner, outer).map { it.createPointer() }
            assertDiagnostics(mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS))
        }
        analyzeForTest(resourceUse) {
            assertSame(resource, restoreSymbol(pointers[0])!!.psi)
            assertSame(outerDeclaration, restoreSymbol(pointers[1])!!.psi)
        }
    }

    /** Box 从构造实参推断；Empty 从自身声明期望类型推断，分别覆盖 Int64 和函数 T。 */
    @Test
    fun resourcesGenericContext(mainFile: CjFile, services: TestServices) {
        val resources = resourceDeclarations(mainFile, "box", "empty", "box", "copy", "empty")
        assertNull(resources[0].parameter!!.typeReference)
        assertNotNull(resources[1].parameter!!.typeReference)
        resources.slice(2..3).forEach { assertNull(it.parameter!!.typeReference) }
        assertNotNull(resources[4].parameter!!.typeReference)
        val typeParameter = element<CjTypeParameter>(mainFile, services, "typeParameter")
        val parameter = element<CjParameter>(mainFile, services, "parameter")
        val value = element<CjBindingPattern>(mainFile, services, "valueDeclaration")
        val initializerParameter = use(mainFile, services, "initializerParameter")
        val markers = listOf("concreteBox", "concreteEmpty", "toBox", "genericCopy", "genericEmpty")
            .map { use(mainFile, services, it) }
        lateinit var pointers: List<CaSymbolPointer<CaSymbol>>

        analyzeForTest(mainFile) {
            val concreteBox = resources[0].bindingSymbol!!
            assertInt64(assertClassType(concreteBox.returnType, "Box").typeArguments.single())
            val concreteEmpty = resources[1].bindingSymbol!!
            assertInt64(assertClassType(concreteEmpty.returnType, "Empty").typeArguments.single())
            val genericBox = resources[2].bindingSymbol!!
            assertTypeParameter(assertClassType(genericBox.returnType, "Box").typeArguments.single(), typeParameter)
            val copy = resources[3].bindingSymbol!!
            val genericEmpty = resources[4].bindingSymbol!!
            assertTypeParameter(assertClassType(copy.returnType, "Box").typeArguments.single(), typeParameter)
            assertTypeParameter(assertClassType(genericEmpty.returnType, "Empty").typeArguments.single(), typeParameter)
            val bindings = listOf(concreteBox, concreteEmpty, genericBox, copy, genericEmpty)
            bindings.zip(resources).forEach { (symbol, psi) -> assertLocal(symbol, psi) }
            markers.zip(bindings).forEach { (expression, symbol) ->
                assertEquals(symbol, expression.resolveToSymbol())
                assertSame(symbol.psi, expression.resolveToSymbol()!!.psi)
            }
            assertEquals(parameter.symbol, initializerParameter.resolveToSymbol())
            assertSame(parameter, initializerParameter.resolveToSymbol()!!.psi)
            assertTypeParameter(value.bindingSymbol!!.returnType, typeParameter)
            pointers = bindings.map { it.createPointer() }
            assertDiagnostics(mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS))
        }
        analyzeForTest(markers[3]) {
            pointers.zip(resources).forEach { (pointer, psi) -> assertSame(psi, restoreSymbol(pointer)!!.psi) }
        }
    }

    /** a 的 Empty<Int64> 不能反向充当 b = Empty() 的声明期望类型。 */
    @Test
    fun resourcesNoExpectedTypeBackpropagation(mainFile: CjFile, services: TestServices) {
        val resources = resourceDeclarations(mainFile, "a", "b")
        assertNotNull(resources[0].parameter!!.typeReference)
        assertNull(resources[1].parameter!!.typeReference)
        val forward = use(mainFile, services, "toLater")
        val constructor = use(mainFile, services, "uninferredConstructor")
        lateinit var pointer: CaSymbolPointer<CaSymbol>

        analyzeForTest(mainFile) {
            val first = resources[0].bindingSymbol!!
            assertInt64(assertClassType(first.returnType, "Empty").typeArguments.single())
            val later = resources[1].bindingSymbol!!
            assertLocal(first, resources[0])
            assertLocal(later, resources[1])
            assertEquals(later, forward.resolveToSymbol())
            assertSame(resources[1], forward.resolveToSymbol()!!.psi)
            pointer = later.createPointer()
            assertDiagnostics(
                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS),
                "UNABLE_TO_INFER_GENERIC_FUNC" to constructor,
                "USED_BEFORE_INITIALIZATION" to forward,
            )
        }
        analyzeForTest(forward) { assertSame(resources[1], restoreSymbol(pointer)!!.psi) }
    }

    /** b = Box(1) 独立合成为 Box<Int64>，不能被前序 a: Box<String> 改写。 */
    @Test
    fun resourcesGenericForwardMismatch(mainFile: CjFile, services: TestServices) {
        val resources = resourceDeclarations(mainFile, "a", "b")
        assertNotNull(resources[0].parameter!!.typeReference)
        assertNull(resources[1].parameter!!.typeReference)
        val forward = use(mainFile, services, "toLater")
        lateinit var pointer: CaSymbolPointer<CaSymbol>
        analyzeForTest(mainFile) {
            val first = resources[0].bindingSymbol!!
            assertStringType(assertClassType(first.returnType, "Box").typeArguments.single())
            val later = resources[1].bindingSymbol!!
            assertInt64(assertClassType(later.returnType, "Box").typeArguments.single())
            assertLocal(first, resources[0])
            assertLocal(later, resources[1])
            assertSame(resources[1], forward.resolveToSymbol()!!.psi)
            assertEquals(later, forward.resolveToSymbol())
            assertInt64(assertClassType(forward.expressionType!!, "Box").typeArguments.single())
            pointer = later.createPointer()
            assertDiagnostics(
                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS),
                "TYPE_MISMATCH" to forward,
            )
        }
        analyzeForTest(forward) {
            val restored = restoreSymbol(pointer) as CaLocalVariableSymbol
            assertLocal(restored, resources[1])
            assertInt64(assertClassType(restored.returnType, "Box").typeArguments.single())
        }
    }

    /** 四种官方拒绝的循环必须结束解析；无类型锚点时不编造具体类型或额外推断错误。 */
    @Test
    @Timeout(30)
    fun resourcesCycles(mainFile: CjFile, services: TestServices) {
        val resources = resourceDeclarations(mainFile, "a", "b", "a", "b", "a", "b", "a", "b")
        val forwards = listOf("implicitForward", "explicitForward", "memberForward", "anchoredForward")
            .map { use(mainFile, services, it) }
        val backwards = listOf("implicitBackward", "explicitBackward", "memberBackward", "anchoredBackward")
            .map { use(mainFile, services, it) }
        lateinit var pointers: List<CaSymbolPointer<CaSymbol>>
        analyzeForTest(mainFile) {
            val bindings = resources.map { it.bindingSymbol!! }
            bindings.zip(resources).forEach { (symbol, psi) -> assertLocal(symbol, psi) }
            // returnType 首查和 diagnostics 都必须终止；循环错误类型的内部表示不属于语义验收契约。
            bindings.forEach { assertNotNull(it.returnType) }
            forwards.forEachIndexed { index, expression ->
                assertEquals(bindings[index * 2 + 1], expression.resolveToSymbol())
                assertSame(resources[index * 2 + 1], expression.resolveToSymbol()!!.psi)
            }
            backwards.forEachIndexed { index, expression ->
                assertEquals(bindings[index * 2], expression.resolveToSymbol())
                assertSame(resources[index * 2], expression.resolveToSymbol()!!.psi)
            }
            pointers = bindings.map { it.createPointer() }
            assertDiagnostics(
                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS),
                *forwards.map { "USED_BEFORE_INITIALIZATION" to it }.toTypedArray(),
            )
        }
        analyzeForTest(forwards[0]) {
            pointers.zip(resources).forEach { (pointer, psi) -> assertSame(psi, restoreSymbol(pointer)!!.psi) }
        }
    }

    /** 官方捕获错误不能合并成直接读取错误，且 Box 的函数实参仍可推导资源返回类型。 */
    @Test
    fun resourcesForwardCapture(mainFile: CjFile, services: TestServices) {
        val resources = resourceDeclarations(mainFile, "a", "b")
        val capture = use(mainFile, services, "capture")
        val firstUse = use(mainFile, services, "firstUse")
        val laterUse = use(mainFile, services, "laterUse")
        val value = element<CjBindingPattern>(mainFile, services, "valueDeclaration")
        lateinit var pointer: CaSymbolPointer<CaSymbol>
        analyzeForTest(mainFile) {
            val first = resources[0].bindingSymbol!!
            val closure = assertClassType(first.returnType, "Box").typeArguments.single()
            assertTrue(closure is CaFunctionType)
            assertTrue((closure as CaFunctionType).parameterTypes.isEmpty())
            assertManagedResourceType(closure.returnType)
            val later = resources[1].bindingSymbol!!
            assertLocal(first, resources[0])
            assertLocal(later, resources[1])
            assertManagedResourceType(later.returnType)
            assertManagedResourceType(value.bindingSymbol!!.returnType)
            assertEquals(later, capture.resolveToSymbol())
            assertSame(resources[1], capture.resolveToSymbol()!!.psi)
            assertManagedResourceType(capture.expressionType!!)
            assertEquals(first, firstUse.resolveToSymbol())
            assertEquals(later, laterUse.resolveToSymbol())
            pointer = first.createPointer()
            assertDiagnostics(
                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS),
                "CAPTURE_BEFORE_INITIALIZATION" to capture,
            )
        }
        analyzeForTest(capture) { assertSame(resources[0], restoreSymbol(pointer)!!.psi) }
    }

    /**
     * 无声明期望类型、无形参注解的 lambda 通过 synthetic outer call 进入 PCLA。
     * b = Box(item) 试算引入构造器类型变量，但必须保留外层 item 的变量身份，
     * 由 try 主体的 acceptInt(a.get()) 约束最终完成 (Int64) -> Int64。
     */
    @Test
    fun resourcesPclaIncomingVariables(mainFile: CjFile, services: TestServices) {
        val resources = resourceDeclarations(mainFile, "a", "b")
        resources.forEach { assertNull(it.parameter!!.typeReference) }
        val parameter = element<CjParameter>(mainFile, services, "parameter")
        val lambda = PsiTreeUtil.getParentOfType(parameter, CjLambdaExpression::class.java)!!
        assertNull(parameter.typeReference, "必须从未标注 lambda 输入开始推断。")
        assertSame(parameter, lambda.valueParameters.single())
        assertFalse(lambda.hasDeclaredReturnType())
        val operationDeclaration = element<CjBindingPattern>(mainFile, services, "operationDeclaration")
        val resultDeclaration = element<CjBindingPattern>(mainFile, services, "resultDeclaration")
        val forward = use(mainFile, services, "forward")
        val initializerParameter = use(mainFile, services, "initializerParameter")
        val returnParameter = use(mainFile, services, "returnParameter")
        val resourceUse = use(mainFile, services, "resourceUse")
        val resourceGet = element<CjDotQualifiedExpression>(mainFile, services, "resourceGet")
        val operationUse = use(mainFile, services, "operationUse")
        val operationCall = element<CjCallExpression>(mainFile, services, "operationUse")
        lateinit var pointers: List<CaSymbolPointer<CaSymbol>>

        analyzeForTest(mainFile) {
            // 首查资源类型；不预查外层参数、operation 类型或 diagnostics 来预先固定输入。
            val first = resources[0].bindingSymbol!!
            assertInt64(assertClassType(first.returnType, "Box").typeArguments.single())
            val later = resources[1].bindingSymbol!!
            assertInt64(assertClassType(later.returnType, "Box").typeArguments.single())
            assertLocal(first, resources[0])
            assertLocal(later, resources[1])
            assertEquals(later, forward.resolveToSymbol())
            assertSame(resources[1], forward.resolveToSymbol()!!.psi)
            assertInt64(assertClassType(forward.expressionType!!, "Box").typeArguments.single())
            assertEquals(first, resourceUse.resolveToSymbol())
            assertInt64(resourceGet.expressionType!!)

            val input = parameter.symbol
            assertInt64(input.returnType)
            listOf(initializerParameter, returnParameter).forEach {
                assertEquals(input, it.resolveToSymbol())
                assertSame(parameter, it.resolveToSymbol()!!.psi)
                assertInt64(it.expressionType!!)
            }
            val operation = operationDeclaration.bindingSymbol!!
            assertUnaryFunctionType(operation.returnType, ::assertInt64)
            assertUnaryFunctionType(lambda.expressionType!!, ::assertInt64)
            assertUnaryFunctionType(operationUse.expressionType!!, ::assertInt64)
            assertEquals(operation, operationUse.resolveToSymbol())
            assertSame(operationDeclaration, operationUse.resolveToSymbol()!!.psi)
            assertInt64(operationCall.expressionType!!)
            assertInt64(resultDeclaration.bindingSymbol!!.returnType)
            pointers = listOf(first, later, input, operation).map { it.createPointer() }
            assertDiagnostics(
                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS),
                "USED_BEFORE_INITIALIZATION" to forward,
            )
        }
        analyzeForTest(operationUse) {
            pointers.zip(listOf(resources[0], resources[1], parameter, operationDeclaration)).forEach { (pointer, psi) ->
                assertSame(psi, restoreSymbol(pointer)!!.psi)
            }
            resources.forEach {
                assertInt64(assertClassType(it.bindingSymbol!!.returnType, "Box").typeArguments.single())
            }
            assertInt64(parameter.symbol.returnType)
            assertUnaryFunctionType(operationDeclaration.bindingSymbol!!.returnType, ::assertInt64)
        }
    }

    /**
     * 未标注 lambda 输入先由独立语句固定为 ManagedResource，再进入资源组试算。
     * a 的嵌套 lambda 中 String item 不能污染 b initializer 的准备点上下文或撤销外层固定；
     * 试算前后、lambda 返回及函数值调用必须保持同一个输入符号与完整资源类型。
     */
    @Test
    fun resourcesPclaPreparedContext(mainFile: CjFile, services: TestServices) {
        val resources = resourceDeclarations(mainFile, "a", "b")
        resources.forEach { assertNull(it.parameter!!.typeReference) }
        val parameter = element<CjParameter>(mainFile, services, "parameter")
        val lambda = PsiTreeUtil.getParentOfType(parameter, CjLambdaExpression::class.java)!!
        assertNull(parameter.typeReference, "前序语句必须在 PCLA 中约束未标注输入。")
        assertSame(parameter, lambda.valueParameters.single())
        assertFalse(lambda.hasDeclaredReturnType())
        val operationDeclaration = element<CjBindingPattern>(mainFile, services, "operationDeclaration")
        val resultDeclaration = element<CjBindingPattern>(mainFile, services, "resultDeclaration")
        val lambdaDeclaration = element<CjBindingPattern>(mainFile, services, "lambdaDeclaration")
        val bodyDeclaration = element<CjBindingPattern>(mainFile, services, "bodyDeclaration")
        val parameterUses = listOf("beforeParameter", "initializerParameter", "afterParameter", "returnParameter")
            .map { use(mainFile, services, it) }
        val forward = use(mainFile, services, "forward")
        val nestedGet = element<CjDotQualifiedExpression>(mainFile, services, "forward")
        val firstUse = use(mainFile, services, "firstUse")
        val laterUse = use(mainFile, services, "laterUse")
        val lambdaUse = use(mainFile, services, "lambdaUse")
        val bodyUse = use(mainFile, services, "bodyUse")
        val operationUse = use(mainFile, services, "operationUse")
        val operationCall = element<CjCallExpression>(mainFile, services, "operationUse")
        lateinit var pointers: List<CaSymbolPointer<CaSymbol>>

        analyzeForTest(mainFile) {
            val first = resources[0].bindingSymbol!!
            val closure = assertClassType(first.returnType, "Box").typeArguments.single()
            assertTrue(closure is CaFunctionType)
            assertTrue((closure as CaFunctionType).parameterTypes.isEmpty())
            assertManagedResourceType(closure.returnType)
            val later = resources[1].bindingSymbol!!
            assertManagedResourceType(assertClassType(later.returnType, "Box").typeArguments.single())
            assertLocal(first, resources[0])
            assertLocal(later, resources[1])
            assertEquals(later, forward.resolveToSymbol())
            assertSame(resources[1], forward.resolveToSymbol()!!.psi)
            assertManagedResourceType(nestedGet.expressionType!!)
            assertEquals(first, firstUse.resolveToSymbol())
            assertEquals(later, laterUse.resolveToSymbol())

            val input = parameter.symbol
            assertManagedResourceType(input.returnType)
            parameterUses.forEach {
                assertEquals(input, it.resolveToSymbol())
                assertSame(parameter, it.resolveToSymbol()!!.psi)
                assertManagedResourceType(it.expressionType!!)
            }
            val nested = lambdaDeclaration.bindingSymbol!!
            val body = bodyDeclaration.bindingSymbol!!
            assertEquals(nested, lambdaUse.resolveToSymbol())
            assertSame(lambdaDeclaration, lambdaUse.resolveToSymbol()!!.psi)
            assertEquals(body, bodyUse.resolveToSymbol())
            assertSame(bodyDeclaration, bodyUse.resolveToSymbol()!!.psi)
            assertStringType(nested.returnType)
            assertStringType(body.returnType)
            assertStringType(lambdaUse.expressionType!!)
            assertStringType(bodyUse.expressionType!!)
            val operation = operationDeclaration.bindingSymbol!!
            assertUnaryFunctionType(operation.returnType, ::assertManagedResourceType)
            assertUnaryFunctionType(lambda.expressionType!!, ::assertManagedResourceType)
            assertUnaryFunctionType(
                assertTypePresent(operationUse.expressionType, "函数值 callee 使用处必须取得函数类型。"),
                ::assertManagedResourceType,
            )
            assertEquals(operation, operationUse.resolveToSymbol())
            assertSame(operationDeclaration, operationUse.resolveToSymbol()!!.psi)
            assertManagedResourceType(operationCall.expressionType!!)
            assertManagedResourceType(resultDeclaration.bindingSymbol!!.returnType)
            pointers = listOf(first, later, input, operation, nested, body).map { it.createPointer() }
            assertDiagnostics(
                mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS),
                "CAPTURE_BEFORE_INITIALIZATION" to forward,
            )
        }
        analyzeForTest(operationUse) {
            pointers.zip(listOf(resources[0], resources[1], parameter, operationDeclaration, lambdaDeclaration, bodyDeclaration))
                .forEach { (pointer, psi) -> assertSame(psi, restoreSymbol(pointer)!!.psi) }
            assertManagedResourceType(assertClassType(resources[1].bindingSymbol!!.returnType, "Box").typeArguments.single())
            assertManagedResourceType(parameter.symbol.returnType)
            assertUnaryFunctionType(operationDeclaration.bindingSymbol!!.returnType, ::assertManagedResourceType)
        }
    }

    @Test
    fun catches(mainFile: CjFile, services: TestServices) {
        val parameters = PsiTreeUtil.findChildrenOfType(mainFile, CjCatchParameter::class.java).toList()
        val use = services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, "catchUse")
        lateinit var pointer: CaSymbolPointer<CaSymbol>
        analyzeForTest(mainFile) {
            val binding = parameters[0].bindingSymbol!!
            assertLocal(binding, parameters[0])
            assertSame(parameters[0], use.resolveToSymbol()!!.psi)
            assertNull(parameters[1].bindingSymbol)
            pointer = binding.createPointer()
        }
        analyzeForTest(use) { assertSame(parameters[0], restoreSymbol(pointer)!!.psi) }
    }

    private fun resourceDeclarations(mainFile: CjFile, vararg names: String): List<CjTryResource> =
        PsiTreeUtil.findChildrenOfType(mainFile, CjTryResource::class.java).sortedBy { it.textOffset }.also {
            assertEquals(names.toList(), it.map { resource -> resource.name })
        }

    private inline fun <reified T : PsiElement> element(mainFile: CjFile, services: TestServices, marker: String): T =
        services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<T>(mainFile, marker)

    private fun use(mainFile: CjFile, services: TestServices, marker: String): CjSimpleNameExpression =
        element(mainFile, services, marker)

    /** 同时比较工厂名、全部区间和重复次数，禁止额外或重复的级联诊断。 */
    private fun assertDiagnostics(
        diagnostics: Collection<CaDiagnosticWithPsi<*>>,
        vararg expected: Pair<String, PsiElement>,
    ) {
        val rendered = diagnostics.joinToString { "${it.factoryName}@${it.textRanges}" }
        assertEquals(
            expected.map { (factory, psi) -> factory to listOf(psi.textRange) }.groupingBy { it }.eachCount(),
            diagnostics.map { it.factoryName to it.textRanges.toList() }.groupingBy { it }.eachCount(),
            rendered,
        )
    }

    /** 输入与返回都必须完成定型，不能把试算拥有的变量或未完成外层占位暴露为函数类型。 */
    private fun assertUnaryFunctionType(type: CaType, assertValueType: (CaType) -> Unit) {
        assertTrue(type is CaFunctionType)
        val function = type as CaFunctionType
        assertValueType(function.parameterTypes.single())
        assertValueType(function.returnType)
    }

    private fun assertClassType(type: CaType, name: String): CaUsualClassType {
        assertTrue(type is CaUsualClassType, "必须取得完整类类型：$name。实际类型：${describeType(type)}")
        return (type as CaUsualClassType).also {
            assertEquals("test.hosts.$name", it.classId.asSingleFqName().asString())
        }
    }

    private fun assertInt64(type: CaType) {
        assertTrue(type is CaPrimitiveType, "必须取得 primitive 类型，实际类型：${describeType(type)}")
        assertEquals(PrimitiveTypeKind.INT64, (type as CaPrimitiveType).kind)
    }

    private fun assertStringType(type: CaType) {
        assertTrue(type is CaUsualClassType)
        assertEquals("std.core.String", (type as CaUsualClassType).classId.asSingleFqName().asString())
    }

    /** 同名 T 不足以证明上下文正确，必须命中指定函数的类型参数声明。 */
    private fun assertTypeParameter(type: CaType, declaration: CjTypeParameter) {
        assertTrue(type is CaTypeParameterType, "必须取得指定类型参数，实际类型：${describeType(type)}")
        assertSame(declaration, (type as CaTypeParameterType).symbol.psi)
    }

    private fun describeType(type: CaType): String {
        val message = if (type is CaErrorType) type.errorMessage else type.toString()
        var diagnostic = ((type as? CaCfirType)?.coneType as? ConeErrorType)?.diagnostic
        while (diagnostic is ConeUnreportedDuplicateDiagnostic) diagnostic = diagnostic.original
        val errors = (diagnostic as? ConeConstraintSystemHasContradiction)?.candidate?.errors
        return "${type::class.simpleName}: $message${errors?.let { "; $it" }.orEmpty()}"
    }

    private fun assertTypePresent(type: CaType?, message: String): CaType {
        assertNotNull(type, message)
        return requireNotNull(type)
    }

    private fun assertLocal(symbol: CaLocalVariableSymbol, psi: PsiElement) {
        assertSame(psi, symbol.psi)
        assertEquals(CaSymbolLocation.LOCAL, symbol.location)
        assertNull(symbol.callableId)
        assertTrue(symbol.isLet)
    }

    /** 名字解析成功仍不足以通过：资源类型必须收敛到成功解析的完整类身份。 */
    private fun assertManagedResourceType(type: CaType) {
        assertTrue(type is CaUsualClassType, "资源类型必须收敛为真实类类型。")
        assertEquals("test.hosts.ManagedResource", (type as CaUsualClassType).classId.asSingleFqName().asString())
    }
}
