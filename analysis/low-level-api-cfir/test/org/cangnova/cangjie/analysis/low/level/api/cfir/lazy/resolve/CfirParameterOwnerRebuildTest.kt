@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.TargetSourceTestSupport
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirModifiablePsiSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.BodyStateKeepers
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.resolveWithKeeper
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroParameterSignature
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawOwnerSnapshot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistry
import org.cangnova.cangjie.cfir.session.rawMacroSurfaceUsageStoreOrNull
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.parsing.CjParameterGrammar
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 私有 raw 副本的参数属于 fresh 声明；宏词法上下文与 return target 仍指向待采用的实际旧声明。
 * 源码遵循官方 main/宏包/构造器/属性/lambda 文法及现有 raw owner 夹具。
 * A/M 故意不声明，所有用例仅重建 raw CFIR，不采用副本、不执行语义解析或诊断检查。
 */
class CfirParameterOwnerRebuildTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/parameterOwnerRebuild",
) {
    override val configurator = analysisApiCfirModifiablePsiSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun ordinaryFunction(mainFile: CjFile) = assertTopLevelFunction<CfirNamedFunction>(mainFile)

    @Test
    fun mainFunction(mainFile: CjFile) = assertTopLevelFunction<CfirMainFunction>(mainFile)

    @Test
    fun macroDeclaration(mainFile: CjFile) = assertTopLevelFunction<CfirMacroDeclaration>(mainFile)

    @Test
    fun finalizer(mainFile: CjFile) {
        val file = rawFile(mainFile)
        val sources = TargetSourceTestSupport(file, mainFile)
        val owner = file.declarations.filterIsInstance<CfirClass>().single()
        sources.assertTargetElement(mainFile.declarations.single(), owner.psi)
        val original = owner.declarations.filterIsInstance<CfirFinalizer>().single()
        withRebuild(file, original, listOf(file, owner), sources) { rebuilt ->
            assertFunction(original, rebuilt.declaration as CfirFinalizer, rebuilt, rebuilt.prefix)
        }
    }

    @Test
    fun classPrimaryConstructor(mainFile: CjFile) = assertConstructor(mainFile, isStruct = false, isPrimary = true)

    @Test
    fun classSecondaryConstructor(mainFile: CjFile) = assertConstructor(mainFile, isStruct = false, isPrimary = false)

    @Test
    fun structPrimaryConstructor(mainFile: CjFile) = assertConstructor(mainFile, isStruct = true, isPrimary = true)

    @Test
    fun structSecondaryConstructor(mainFile: CjFile) = assertConstructor(mainFile, isStruct = true, isPrimary = false)

    @Test
    fun propertyAccessors(mainFile: CjFile) {
        val file = rawFile(mainFile)
        val sources = TargetSourceTestSupport(file, mainFile)
        val owner = file.declarations.filterIsInstance<CfirClass>().single()
        sources.assertTargetElement(mainFile.declarations.single(), owner.psi)
        val original = owner.declarations.filterIsInstance<CfirProperty>().single()
        val psi = original.psi as CjProperty
        withRebuild(file, original, listOf(file, owner), sources) { rebuilt ->
            val copy = rebuilt.declaration as CfirProperty
            val oldGetter = requireNotNull(original.getter)
            val oldSetter = requireNotNull(original.setter)
            val getter = requireNotNull(copy.getter)
            val setter = requireNotNull(copy.setter)
            assertAll(
                { assertSame(psi, copy.psi) },
                { assertNotSame(original.symbol, copy.symbol) },
                { assertSame(copy, copy.symbol.cfir) },
                { assertSame(original, original.symbol.cfir) },
                { assertSame(psi.getter, getter.psi) },
                { assertSame(psi.setter, setter.psi) },
                { assertTrue(getter.isGetter) },
                { assertFalse(setter.isGetter) },
                { assertSame(copy.symbol, getter.propertySymbol) },
                { assertSame(copy.symbol, setter.propertySymbol) },
                { assertNotSame(getter.symbol, setter.symbol) },
                { assertFunction(oldGetter, getter, rebuilt, rebuilt.prefix + original.symbol, CjParameterGrammar.ACCESSOR_NAME_ONLY) },
                { assertFunction(oldSetter, setter, rebuilt, rebuilt.prefix + original.symbol, CjParameterGrammar.ACCESSOR_NAME_ONLY) },
            )
        }
    }

    @Test
    fun nestedLocalAndLambdas(mainFile: CjFile) {
        val file = rawFile(mainFile)
        val sources = TargetSourceTestSupport(file, mainFile)
        val originPsi = mainFile.declarations.filterIsInstance<CjNamedFunction>().first()
        val psi = sources.targetElement(originPsi) as CjNamedFunction
        val original = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.psi === psi }
        withRebuild(file, original, listOf(file), sources) { rebuilt ->
            val copy = rebuilt.declaration as CfirNamedFunction
            val body = requireNotNull(copy.body)
            val local = body.statements.filterIsInstance<CfirNamedFunction>().single()
            val defaultCall = requireNotNull(copy.valueParameters.single().defaultValue) as CfirFunctionCall
            val bodyCall = body.statements.filterIsInstance<CfirFunctionCall>().single()
            val defaultLambda = (defaultCall.argumentList.arguments.first() as CfirAnonymousFunctionExpression).anonymousFunction
            val bodyLambda = (bodyCall.argumentList.arguments.first() as CfirAnonymousFunctionExpression).anonymousFunction
            val outerPath = rebuilt.prefix + original.symbol
            assertAll(
                {
                    assertFreshFunction(original, copy, sources)
                    assertParameters(copy, original, rebuilt, rebuilt.prefix, CjParameterGrammar.REGULAR_TYPED, checkDefaults = false)
                },
                {
                    assertTrue(local.isLocal)
                    val localPsi = requireNotNull(local.psi)
                    assertSame(sources.targetFile, localPsi.containingFile)
                    assertSame(mainFile, sources.originalElement(localPsi).containingFile)
                    assertSame(local, local.symbol.cfir)
                    assertNotSame(copy.symbol, local.symbol)
                    assertParameters(local, local, rebuilt, outerPath, CjParameterGrammar.REGULAR_TYPED)
                    assertBody(local, local, rebuilt, outerPath)
                },
                { assertLambda(defaultLambda, rebuilt, outerPath) },
                { assertLambda(bodyLambda, rebuilt, outerPath) },
                { assertNotSame(defaultLambda.symbol, bodyLambda.symbol) },
                {
                    val snapshot = expressionSnapshot(rebuilt, defaultCall.argumentList.arguments[1])
                    assertSignature(snapshot, original, CjParameterGrammar.REGULAR_TYPED)
                    assertContainers(snapshot, outerPath)
                    assertNull(snapshot.functionTarget)
                },
                {
                    val snapshot = expressionSnapshot(rebuilt, bodyCall.argumentList.arguments[1])
                    assertContainers(snapshot, outerPath)
                    assertNull(snapshot.signature)
                    assertSame(original, requireNotNull(snapshot.functionTarget).labeledElement)
                },
                {
                    val result = body.statements.filterIsInstance<CfirReturnExpression>().single()
                    val snapshot = expressionSnapshot(rebuilt, result.result)
                    assertSame(original, result.target.labeledElement)
                    assertSame(result.target, snapshot.functionTarget)
                    assertContainers(snapshot, outerPath)
                },
            )
        }
    }

    /** 原 raw 属性已无 getter；同一 PSI 重建出的额外 getter 不能认领旧 getter 身份。 */
    @Test
    fun unusedExtraGetterUsesFreshOwners(mainFile: CjFile) = assertUnusedAccessor(mainFile, removeGetter = true)

    /** 原 raw 属性已无 setter；额外 setter 的参数、注解与词法 signature 都属于自身。 */
    @Test
    fun unusedExtraSetterUsesFreshOwners(mainFile: CjFile) = assertUnusedAccessor(mainFile, removeGetter = false)

    private fun assertUnusedAccessor(mainFile: CjFile, removeGetter: Boolean) {
        val file = rawFile(mainFile)
        val sources = TargetSourceTestSupport(file, mainFile)
        val owner = file.declarations.filterIsInstance<CfirClass>().single()
        sources.assertTargetElement(mainFile.declarations.single(), owner.psi)
        val original = owner.declarations.filterIsInstance<CfirProperty>().single()
        val psi = original.psi as CjProperty
        val oldGetter = requireNotNull(original.getter)
        val oldSetter = requireNotNull(original.setter)
        assertSame(psi.getter, oldGetter.psi)
        assertSame(psi.setter, oldSetter.psi)
        val removed = if (removeGetter) oldGetter else oldSetter
        val retained = if (removeGetter) oldSetter else oldGetter
        val retainedBody = requireNotNull(retained.body)
        assertTrue(retainedBody is CfirLazyBlock)
        try {
            if (removeGetter) original.replaceGetter(null) else original.replaceSetter(null)
            lateinit var extra: CfirPropertyAccessor
            withRebuild(file, original, listOf(file, owner), sources) { rebuilt ->
                val copy = rebuilt.declaration as CfirProperty
                val getter = requireNotNull(copy.getter)
                val setter = requireNotNull(copy.setter)
                extra = if (removeGetter) getter else setter
                val retainedCopy = if (removeGetter) setter else getter
                val prefix = rebuilt.prefix + original.symbol
                assertAll(
                    { assertSame(psi.getter, getter.psi) },
                    { assertSame(psi.setter, setter.psi) },
                    { assertSame(copy, copy.symbol.cfir) },
                    { assertSame(copy.symbol, extra.propertySymbol) },
                    { assertSame(copy.symbol, retainedCopy.propertySymbol) },
                    { assertNotSame(extra.symbol, retainedCopy.symbol) },
                    { assertFreshFunction(removed, extra, sources) },
                    { assertParameters(extra, extra, rebuilt, prefix, CjParameterGrammar.ACCESSOR_NAME_ONLY) },
                    { assertBody(extra, extra, rebuilt, prefix) },
                    { assertFunction(retained, retainedCopy, rebuilt, prefix, CjParameterGrammar.ACCESSOR_NAME_ONLY) },
                    { assertNull(if (removeGetter) original.getter else original.setter) },
                    { assertSame(retained, if (removeGetter) original.setter else original.getter) },
                    { assertSame(retainedBody, retained.body) },
                )
            }

            // 源码仍有两份 accessor，calculator 只采用 actual 原图保留的配对。
            val session = original.moduleData.session
            val usages = requireNotNull(session.rawMacroSurfaceUsageStoreOrNull)
            val originalFileUsages = usages.expressionSurfaces(file)
            val originalPsiUsages = usages.expressionSurfacesForSourceFile(mainFile)
            val designation = CfirDesignation(listOf(file, owner), original)
            CfirLazyBodyPublications(session).use { publications ->
                resolveWithKeeper(
                    original,
                    designation,
                    publications.withStateKeeper(BodyStateKeepers.PROPERTY),
                    prepareTarget = { CfirLazyBodiesCalculator.calculateBodies(designation, publications) },
                ) {}
            }
            val adoptedBody = requireNotNull(retained.body)
            val bodyMacros = elements(adoptedBody).filterIsInstance<CfirErrorExpression>().filter { expression ->
                expression.psi is org.cangnova.cangjie.psi.CjMacroExpression
            }
            assertEquals(1, bodyMacros.size, "Only the retained accessor body macro can be adopted")
            val actualFileUsages = usages.expressionSurfaces(file)
            val actualPsiUsages = usages.expressionSurfacesForSourceFile(mainFile)
            assertAll(
                { assertNull(if (removeGetter) original.getter else original.setter) },
                { assertSame(retained, if (removeGetter) original.setter else original.getter) },
                { assertNotSame(retainedBody, adoptedBody) },
                { assertFalse(adoptedBody is CfirLazyBlock) },
                { assertSame(original, original.symbol.cfir) },
                { assertSame(removed, removed.symbol.cfir) },
                { assertSame(extra, extra.symbol.cfir) },
                {
                    extra.valueParameters.flatMap { it.annotations }.forEach { annotation ->
                        assertNull(session.annotationMetadataRegistry.snapshot(annotation as CfirAnnotationCall))
                    }
                },
                {
                    assertEquals(originalFileUsages.size + bodyMacros.size, actualFileUsages.size)
                    originalFileUsages.forEach { before -> assertTrue(actualFileUsages.any { it === before }) }
                    bodyMacros.forEach { expression -> assertEquals(1, actualFileUsages.count { it.replaceHandle.carrier === expression }) }
                },
                {
                    assertEquals(originalPsiUsages.size + bodyMacros.size, actualPsiUsages.size)
                    originalPsiUsages.forEach { before -> assertTrue(actualPsiUsages.any { it === before }) }
                    bodyMacros.forEach { expression -> assertEquals(1, actualPsiUsages.count { it.replaceHandle.carrier === expression }) }
                },
            )
        } finally {
            // 恢复测试手动移除的 accessor，避免污染后续会话/项目使用。
            original.replaceGetter(oldGetter)
            original.replaceSetter(oldSetter)
        }
    }

    private inline fun <reified T : CfirFunction> assertTopLevelFunction(mainFile: CjFile) {
        val file = rawFile(mainFile)
        val sources = TargetSourceTestSupport(file, mainFile)
        val original = file.declarations.filterIsInstance<T>().single()
        sources.assertTargetElement(mainFile.declarations.single(), original.psi)
        withRebuild(file, original, listOf(file), sources) { rebuilt ->
            assertFunction(original, rebuilt.declaration as T, rebuilt, rebuilt.prefix)
        }
    }

    private fun assertConstructor(mainFile: CjFile, isStruct: Boolean, isPrimary: Boolean) {
        val file = rawFile(mainFile)
        val sources = TargetSourceTestSupport(file, mainFile)
        val owner = file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
        assertTrue(if (isStruct) owner is CfirStruct else owner is CfirClass)
        sources.assertTargetElement(mainFile.declarations.single(), owner.psi)
        val original = owner.declarations.filterIsInstance<CfirConstructor>().single { it.isPrimary == isPrimary }
        assertTrue(if (isPrimary) original.psi is CjPrimaryConstructor else original.psi is CjSecondaryConstructor)
        val grammar = when {
            !isPrimary -> CjParameterGrammar.REGULAR_TYPED
            isStruct -> CjParameterGrammar.STRUCT_PRIMARY
            else -> CjParameterGrammar.CLASS_PRIMARY
        }
        withRebuild(file, original, listOf(file, owner), sources) { rebuilt ->
            val copy = rebuilt.declaration as CfirConstructor
            assertEquals(isPrimary, copy.isPrimary)
            assertFunction(original, copy, rebuilt, rebuilt.prefix, grammar)
        }
    }

    /** 使用实际缓存文件和 designation 祖先；writer 始终 OPEN，结束时终止私有构造。 */
    private fun withRebuild(
        file: CfirFile,
        original: CfirDeclaration,
        path: List<CfirDeclaration>,
        sources: TargetSourceTestSupport,
        checkRebuilt: (Rebuilt) -> Unit,
    ) {
        val session = original.moduleData.session
        val metadata = CfirAnnotationMetadataBuilder()
        val surfaces = mutableListOf<MacroSurface>()
        val designation = CfirDesignation(path, original)
        val originalBody = (original as? CfirFunction)?.body
        val originalParameters = (original as? CfirFunction)?.valueParameters?.toList()
        try {
            assertSame(file, path.first())
            assertSame(file.moduleData.session, session)
            val targetPsi = original.psi as CjElement
            val originPsi = sources.originalElement(targetPsi)
            val copy = RawCfirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
                session,
                session.cangjieScopeProvider,
                designation,
                targetPsi,
                metadata,
                surfaces,
            )
            sources.assertTargetElement(originPsi, copy.psi)
            if (original is CfirFunction) {
                assertSame(originalBody, original.body, "A private rebuild must not adopt its body")
                requireNotNull(originalParameters).zip(original.valueParameters).forEach { (before, after) -> assertSame(before, after) }
            }
            checkRebuilt(Rebuilt(copy, metadata, surfaces, path.map { it.symbol }, sources))
        } finally {
            metadata.abort()
        }
    }

    private fun assertFunction(
        original: CfirFunction,
        copy: CfirFunction,
        rebuilt: Rebuilt,
        prefix: List<CfirBasedSymbol<*>>,
        grammar: CjParameterGrammar = CjParameterGrammar.REGULAR_TYPED,
    ) {
        assertAll(
            { assertFreshFunction(original, copy, rebuilt.sources) },
            { assertParameters(copy, original, rebuilt, prefix, grammar) },
            { assertBody(copy, original, rebuilt, prefix) },
        )
    }

    private fun assertFreshFunction(original: CfirFunction, copy: CfirFunction, sources: TargetSourceTestSupport) {
        assertNotSame(original, copy)
        assertNotSame(original.symbol, copy.symbol)
        assertSame(copy, copy.symbol.cfir)
        assertSame(original, original.symbol.cfir)
        assertSame(original.psi, copy.psi)
        sources.assertTargetElement(sources.originalElement(requireNotNull(original.psi)), copy.psi)
        assertEquals(original.valueParameters.size, copy.valueParameters.size)
        original.valueParameters.zip(copy.valueParameters).forEach { (old, fresh) ->
            assertNotSame(old, fresh)
            assertSame(old.psi, fresh.psi)
            sources.assertTargetElement(sources.originalElement(requireNotNull(old.psi)), fresh.psi)
        }
    }

    /** 参数/注解 carrier 的结构 owner 与宏 signature 的词法 owner 分别验收。 */
    private fun assertParameters(
        copy: CfirFunction,
        lexicalOwner: CfirFunction,
        rebuilt: Rebuilt,
        prefix: List<CfirBasedSymbol<*>>,
        grammar: CjParameterGrammar,
        checkDefaults: Boolean = true,
    ) {
        copy.valueParameters.forEach { parameter ->
            val psi = parameter.psi as CjParameter
            val originPsi = rebuilt.sources.originalElement(psi) as CjParameter
            assertAll(
                { assertSame(copy.symbol, parameter.containingDeclarationSymbol, "A rebuilt parameter must belong to the fresh declaration") },
                {
                    assertEquals(psi.annotationEntries.size, parameter.annotations.size)
                    parameter.annotations.forEachIndexed { index, annotation ->
                        val call = annotation as CfirAnnotationCall
                        val slot = requireNotNull(rebuilt.metadata.snapshot(call))
                        assertSame(parameter, slot.owner)
                        assertSame(call, parameter.annotations[slot.annotationIndex])
                        val callSource = requireNotNull(call.source) as CjPsiSourceElement
                        val annotationSource = slot.annotationSource as CjPsiSourceElement
                        // source wrapper 每次包装都会新建；契约是底层 PSI 身份、精确坐标及来源种类。
                        assertSame(call.psi, annotationSource.psi)
                        assertEquals(callSource.startOffset, annotationSource.startOffset)
                        assertEquals(callSource.endOffset, annotationSource.endOffset)
                        assertEquals(callSource.kind, annotationSource.kind)
                        assertEquals(callSource.elementType, annotationSource.elementType)
                        assertSame(callSource.psi.containingFile, annotationSource.psi.containingFile)
                        assertSame(psi.annotationEntries[index], call.psi)
                        rebuilt.sources.assertTargetElement(originPsi.annotationEntries[index], call.psi)
                        assertSame(copy.symbol, call.containingDeclarationSymbol)
                    }
                },
                {
                    parameter.annotations.forEach { annotation ->
                        val surface = rebuilt.surfaces.single { it.replaceHandle.annotationCarrier?.originalAnnotation === annotation }
                        val carrier = requireNotNull(surface.replaceHandle.annotationCarrier)
                        assertSame(parameter, carrier.owner)
                        val snapshot = surface.replaceHandle.ownerSnapshot
                        assertSignature(snapshot, lexicalOwner, grammar)
                        assertContainers(snapshot, prefix + lexicalOwner.symbol)
                        assertNull(snapshot.functionTarget)
                    }
                },
                {
                    if (checkDefaults && parameter.defaultValue != null) {
                        val expression = requireNotNull(parameter.defaultValue)
                        assertSame(psi.defaultValue, expression.psi)
                        rebuilt.sources.assertTargetElement(requireNotNull(originPsi.defaultValue), expression.psi)
                        val snapshot = expressionSnapshot(rebuilt, expression)
                        assertSignature(snapshot, lexicalOwner, grammar)
                        assertContainers(snapshot, prefix + lexicalOwner.symbol)
                        assertNull(snapshot.functionTarget)
                    }
                },
            )
        }
    }

    private fun assertBody(copy: CfirFunction, lexicalOwner: CfirFunction, rebuilt: Rebuilt, prefix: List<CfirBasedSymbol<*>>) {
        val body = requireNotNull(copy.body)
        assertFalse(body is CfirLazyBlock)
        val elements = elements(body)
        val surfaces = rebuilt.surfaces.filterIsInstance<MacroSurfaceExpr>().filter { surface ->
            elements.any { it === surface.replaceHandle.carrier }
        }
        assertTrue(surfaces.isNotEmpty(), "The fixture must exercise a body macro owner snapshot")
        surfaces.forEach { surface ->
            val snapshot = surface.replaceHandle.ownerSnapshot
            assertContainers(snapshot, prefix + lexicalOwner.symbol)
            assertNull(snapshot.signature)
            assertSame(lexicalOwner, requireNotNull(snapshot.functionTarget).labeledElement)
        }
        elements.filterIsInstance<CfirReturnExpression>().forEach { result ->
            assertSame(lexicalOwner, result.target.labeledElement)
        }
    }

    private fun assertLambda(lambda: CfirAnonymousFunction, rebuilt: Rebuilt, prefix: List<CfirBasedSymbol<*>>) {
        assertTrue(lambda.isLambda)
        assertTrue(lambda.isLocal)
        assertSame(lambda, lambda.symbol.cfir)
        rebuilt.sources.originalElement(requireNotNull(lambda.psi))
        lambda.valueParameters.forEach { parameter ->
            assertSame(lambda.symbol, parameter.containingDeclarationSymbol)
            assertTrue(parameter.psi is CjParameter)
            rebuilt.sources.originalElement(requireNotNull(parameter.psi))
        }
        assertBody(lambda, lambda, rebuilt, prefix)
    }

    private fun assertSignature(snapshot: MacroRawOwnerSnapshot, owner: CfirFunction, grammar: CjParameterGrammar) {
        val signature = snapshot.signature as MacroParameterSignature.Declaration
        assertSame(owner.symbol, signature.owner)
        assertEquals(grammar, signature.grammar)
    }

    private fun assertContainers(snapshot: MacroRawOwnerSnapshot, expected: List<CfirBasedSymbol<*>>) {
        assertEquals(expected.size, snapshot.containerSymbols.size)
        expected.zip(snapshot.containerSymbols).forEach { (owner, actual) -> assertSame(owner, actual) }
    }

    private fun expressionSnapshot(rebuilt: Rebuilt, expression: CfirExpression): MacroRawOwnerSnapshot =
        rebuilt.surfaces.filterIsInstance<MacroSurfaceExpr>().single { it.replaceHandle.carrier === expression }.replaceHandle.ownerSnapshot

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

    private class Rebuilt(
        val declaration: CfirDeclaration,
        val metadata: CfirAnnotationMetadataBuilder,
        val surfaces: List<MacroSurface>,
        val prefix: List<CfirBasedSymbol<*>>,
        val sources: TargetSourceTestSupport,
    )
}
