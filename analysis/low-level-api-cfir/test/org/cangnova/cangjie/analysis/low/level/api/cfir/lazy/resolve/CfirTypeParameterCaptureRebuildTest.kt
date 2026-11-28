@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirModifiablePsiSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * fresh 自有 header、保留祖先 self 与 constructed-class 引用是三种不同身份。
 * 官方 1.1.3 正例及 static T/自有泛型构造器负例见 review-20261110/type-parameter-capture。
 * 宏 M 的夹具只检查 raw owner，不执行语义诊断；constructor 仅使用当前 ref.symbol 接口。
 */
class CfirTypeParameterCaptureRebuildTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/typeParameterCaptureRebuild",
) {
    override val configurator = analysisApiCfirModifiablePsiSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun freshGenericFunctionHeader(mainFile: CjFile) {
        val file = rawFile(mainFile)
        val original = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        withRebuild(file, original, listOf(file)) { copy, surfaces ->
            val function = copy as CfirNamedFunction
            assertAll(
                { assertFreshHeader(original, function) },
                {
                    val result = requireNotNull(function.body).statements.filterIsInstance<CfirReturnExpression>().single()
                    val surface = surfaces.filterIsInstance<MacroSurfaceExpr>().single { it.replaceHandle.carrier === result.result }
                    assertSame(original, result.target.labeledElement)
                    assertSame(result.target, surface.replaceHandle.ownerSnapshot.functionTarget)
                    assertSame(original.symbol, surface.replaceHandle.ownerSnapshot.containerSymbols.last())
                },
            )
        }
    }

    @Test
    fun freshClassHeaderAndFreshSelf(mainFile: CjFile) = assertFreshNominal(mainFile)

    @Test
    fun freshStructHeaderAndFreshSelf(mainFile: CjFile) = assertFreshNominal(mainFile)

    @Test
    fun freshExtendHeader(mainFile: CjFile) {
        val file = rawFile(mainFile)
        val originalClass = file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
        val original = file.declarations.filterIsInstance<CfirExtend>().single()
        withRebuild(file, original, listOf(file)) { copy, _ ->
            val extend = copy as CfirExtend
            assertFreshHeader(original, extend)
            assertEquals(1, extend.typeParameters.size)
            assertNotSame(originalClass.typeParameters.single().symbol, extend.typeParameters.single().symbol)
            extend.declarations.filterIsInstance<CfirNamedFunction>().forEach { member ->
                assertTrue(member.typeParameters.isEmpty(), "A member must not declare the extend's R as its own header")
            }
        }
    }

    @Test
    fun memberRebuildKeepsOriginalClassSelf(mainFile: CjFile) {
        val file = rawFile(mainFile)
        val owner = file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
        val original = owner.declarations.filterIsInstance<CfirNamedFunction>().single()
        withRebuild(file, original, listOf(file, owner)) { copy, surfaces ->
            val function = copy as CfirNamedFunction
            assertAll(
                { assertFreshHeader(original, function) },
                { assertSelf(requireNotNull(function.dispatchReceiverType), owner) },
                {
                    val result = requireNotNull(function.body).statements.filterIsInstance<CfirReturnExpression>().single()
                    val surface = surfaces.filterIsInstance<MacroSurfaceExpr>().single { it.replaceHandle.carrier === result.result }
                    assertSelf(requireNotNull(surface.replaceHandle.ownerSnapshot.dispatchReceiverType), owner)
                    assertSame(original, result.target.labeledElement)
                },
            )
        }
    }

    @Test
    fun classConstructorsUseConstructedRefs(mainFile: CjFile) = assertInstanceConstructors(mainFile)

    @Test
    fun structConstructorsUseConstructedRefs(mainFile: CjFile) = assertInstanceConstructors(mainFile)

    @Test
    fun implicitClassConstructorUsesConstructedRefs(mainFile: CjFile) = assertInstanceConstructors(mainFile)

    @Test
    fun implicitStructConstructorUsesConstructedRefs(mainFile: CjFile) = assertInstanceConstructors(mainFile)

    @Test
    fun enumCasesUseConstructedRefsAndNoImplicitInit(mainFile: CjFile) {
        val owner = rawFile(mainFile).declarations.filterIsInstance<CfirEnum>().single()
        val cases = owner.declarations.filterIsInstance<CfirEnumConstructor>()
        assertEquals(2, cases.size)
        assertAll(
            { assertTrue(owner.declarations.filterIsInstance<CfirConstructor>().isEmpty(), "An enum has named constructors, not an implicit init") },
            { cases.forEach { item -> assertConstructedRefs(owner, item.typeParameters) } },
        )
    }

    @Test
    fun interfaceHasNoConstructor(mainFile: CjFile) {
        val owner = rawFile(mainFile).declarations.filterIsInstance<CfirInterface>().single()
        assertEquals(1, owner.typeParameters.size)
        assertTrue(owner.declarations.filterIsInstance<CfirConstructor>().isEmpty())
    }

    @Test
    fun staticInitHasNoConstructedRefs(mainFile: CjFile) {
        val owner = rawFile(mainFile).declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
        val initializer = owner.declarations.filterIsInstance<CfirConstructor>().single { it.status.isStatic }
        assertEquals(1, owner.typeParameters.size)
        assertTrue(initializer.typeParameters.isEmpty(), "Static init must not inherit constructed-class type parameters")
    }

    @Test
    fun genericLocalAndLambdaCapture(mainFile: CjFile) {
        val file = rawFile(mainFile)
        val owner = file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
        val original = owner.declarations.filterIsInstance<CfirNamedFunction>().single()
        withRebuild(file, original, listOf(file, owner)) { copy, surfaces ->
            val function = copy as CfirNamedFunction
            val body = requireNotNull(function.body)
            val local = body.statements.filterIsInstance<CfirNamedFunction>().single()
            val call = body.statements.filterIsInstance<CfirFunctionCall>().single()
            val lambda = (call.argumentList.arguments.first() as CfirAnonymousFunctionExpression).anonymousFunction
            assertAll(
                { assertFreshHeader(original, function) },
                {
                    assertEquals(1, local.typeParameters.size, "Only declared U belongs to the local header")
                    val parameter = local.typeParameters.single() as CfirTypeParameter
                    assertSame(local.symbol, parameter.containingDeclarationSymbol)
                    assertSame(parameter, parameter.symbol.cfir)
                    assertNotSame(owner.typeParameters.single().symbol, parameter.symbol)
                    assertNotSame(function.typeParameters.single().symbol, parameter.symbol)
                    assertSame(local, local.symbol.cfir)
                },
                {
                    assertTrue(lambda.typeParameters.isEmpty(), "A lambda has no own generic header")
                    assertSame(lambda, lambda.symbol.cfir)
                    lambda.valueParameters.forEach { assertSame(lambda.symbol, it.containingDeclarationSymbol) }
                },
                {
                    val nested = elements(requireNotNull(local.body)) + elements(requireNotNull(lambda.body))
                    val nestedSurfaces = surfaces.filterIsInstance<MacroSurfaceExpr>().filter { surface -> nested.any { it === surface.replaceHandle.carrier } }
                    assertEquals(2, nestedSurfaces.size)
                    nestedSurfaces.forEach { surface -> assertSelf(requireNotNull(surface.replaceHandle.ownerSnapshot.dispatchReceiverType), owner) }
                    val localReturn = requireNotNull(local.body).statements.filterIsInstance<CfirReturnExpression>().single()
                    val lambdaReturn = requireNotNull(lambda.body).statements.filterIsInstance<CfirReturnExpression>().single()
                    assertSame(local, localReturn.target.labeledElement)
                    assertSame(lambda, lambdaReturn.target.labeledElement)
                },
            )
        }
    }

    private fun assertFreshNominal(mainFile: CjFile) {
        val file = rawFile(mainFile)
        val original = file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
        withRebuild(file, original, listOf(file)) { copy, _ ->
            val nominal = copy as CfirClassLikeDeclaration
            val member = nominal.declarations.filterIsInstance<CfirNamedFunction>().single()
            assertAll(
                { assertFreshHeader(original, nominal) },
                { assertSelf(requireNotNull(member.dispatchReceiverType), nominal) },
                { assertTrue(member.typeParameters.isEmpty()) },
            )
        }
    }

    /** 普通、主、隐式实例构造器都引用实际 constructed class 的参数，不声明自己的泛型。 */
    private fun assertInstanceConstructors(mainFile: CjFile) {
        val file = rawFile(mainFile)
        val owner = file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
        val constructors = owner.declarations.filterIsInstance<CfirConstructor>().filter { !it.status.isStatic }
        assertTrue(constructors.isNotEmpty())
        val primary = constructors.single { it.isPrimary }
        val implicit = primary.psi === owner.psi
        assertEquals(if (implicit) 1 else 2, constructors.size)
        constructors.forEach { original ->
            if (!implicit) assertTrue(original.psi is CjPrimaryConstructor || original.psi is CjSecondaryConstructor)
            withRebuild(file, original, listOf(file, owner)) { copy, _ ->
                val constructor = copy as CfirConstructor
                assertAll(
                    { assertConstructedRefs(owner, original.typeParameters) },
                    { assertConstructedRefs(owner, constructor.typeParameters) },
                    { assertSame(original, original.symbol.cfir) },
                    { assertSame(constructor, constructor.symbol.cfir) },
                    { assertNotSame(original.symbol, constructor.symbol) },
                    { assertSelf(requireNotNull(constructor.dispatchReceiverType), owner) },
                )
            }
        }
    }

    private fun assertConstructedRefs(owner: CfirClassLikeDeclaration, refs: List<CfirTypeParameterRef>) {
        assertEquals(owner.typeParameters.size, refs.size, "Constructed refs must cover the class header in order")
        owner.typeParameters.zip(refs).forEach { (declared, reference) ->
            assertSame(declared.symbol, reference.symbol)
            assertSame(owner.symbol, (reference.symbol.cfir as CfirTypeParameter).containingDeclarationSymbol)
        }
    }

    /** 整体副本只拥有 fresh 的源码声明参数；不把原声明对象塞入新 header。 */
    private fun assertFreshHeader(original: CfirDeclaration, copy: CfirDeclaration) {
        val oldParameters = (original as CfirTypeParameterRefsOwner).typeParameters
        val newParameters = (copy as CfirTypeParameterRefsOwner).typeParameters
        assertNotSame(original, copy)
        assertNotSame(original.symbol, copy.symbol)
        assertSame(original, original.symbol.cfir)
        assertSame(copy, copy.symbol.cfir)
        assertSame(original.psi, copy.psi)
        assertEquals(oldParameters.size, newParameters.size)
        assertTrue(newParameters.isNotEmpty())
        oldParameters.zip(newParameters).forEach { (old, fresh) ->
            val oldDeclaration = old as CfirTypeParameter
            val declaration = fresh as CfirTypeParameter
            assertAll(
                { assertNotSame(old, fresh) },
                { assertNotSame(old.symbol, fresh.symbol) },
                { assertSame(declaration, fresh.symbol.cfir) },
                { assertSame(copy.symbol, declaration.containingDeclarationSymbol) },
                { assertSame(original.symbol, oldDeclaration.containingDeclarationSymbol) },
                { assertSame(oldDeclaration.psi, declaration.psi) },
            )
        }
    }

    /** 完整 nominal 使用自己的 fresh self；成员 designation 使用传入的实际保留祖先。 */
    private fun assertSelf(self: ConeSimpleCangJieType, owner: CfirClassLikeDeclaration) {
        val classifier = self as ConeClassifierType
        assertSame(owner.symbol.toLookupTag(), classifier.lookupTag)
        assertEquals(owner.typeParameters.size, classifier.typeArguments.size)
        classifier.typeArguments.zip(owner.typeParameters).forEach { (argument, parameter) ->
            val type = argument.type as ConeTypeParameterType
            assertSame(parameter.symbol, type.lookupTag.typeParameterSymbol)
        }
    }

    private fun withRebuild(
        file: CfirFile,
        original: CfirDeclaration,
        path: List<CfirDeclaration>,
        inspect: (CfirDeclaration, List<MacroSurface>) -> Unit,
    ) {
        val metadata = CfirAnnotationMetadataBuilder()
        val surfaces = mutableListOf<MacroSurface>()
        val oldHeader = (original as CfirTypeParameterRefsOwner).typeParameters.toList()
        val oldBody = (original as? CfirFunction)?.body
        try {
            assertSame(file, path.first())
            val session = original.moduleData.session
            val copy = RawCfirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
                session, session.cangjieScopeProvider, CfirDesignation(path, original),
                original.psi as CjElement, metadata, surfaces,
            )
            oldHeader.zip((original as CfirTypeParameterRefsOwner).typeParameters).forEach { (before, after) -> assertSame(before, after) }
            if (original is CfirFunction) {
                assertSame(oldBody, original.body)
                // 隐式构造器的真实 body 可以缺失；重建须保留有无状态，有正文时须已物化。
                val newBody = (copy as CfirFunction).body
                assertEquals(oldBody == null, newBody == null)
                assertFalse(newBody is CfirLazyBlock)
            }
            inspect(copy, surfaces)
        } finally {
            metadata.abort()
        }
    }

    private fun elements(root: CfirElement): List<CfirElement> {
        val result = mutableListOf<CfirElement>()
        root.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                result += element
                element.acceptChildren(this)
            }
        })
        return result
    }

    private fun rawFile(mainFile: CjFile): CfirFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
}
