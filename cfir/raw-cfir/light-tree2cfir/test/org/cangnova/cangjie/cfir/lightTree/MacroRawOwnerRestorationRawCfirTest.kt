package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.containingExtend
import org.cangnova.cangjie.cfir.isMemberParameter
import org.cangnova.cangjie.cfir.memberParameterFieldInfo
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroParameterSignature
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawOwnerSnapshot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CjParameterGrammar
import org.cangnova.cangjie.parsing.CjExprKind
import org.cangnova.cangjie.parsing.CjMacroFragmentParent
import org.cangnova.cangjie.parsing.MacroParsingFacts
import org.cangnova.cangjie.parsing.MacroParsingScope
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjReturnExpression
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 只验证新 raw builder 恢复真实宿主上下文的协议，不覆盖宏 parser 或声明 slice 替换。
 * 成员使用成员 annotation 快照；return/参数直接复用宿主源节点，不制造语法 wrapper 或假 owner。
 */
@RunWith(JUnit3RunnerWithInners::class)
class MacroRawOwnerRestorationRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiRestoresGenericClassMember() = assertMemberRestoration(buildHost(RawKind.PSI, CLASS_SOURCE), false)

    fun testLightTreeRestoresGenericClassMember() = assertMemberRestoration(buildHost(RawKind.LIGHT_TREE, CLASS_SOURCE), false)

    fun testPsiRestoresGenericStructMember() = assertMemberRestoration(buildHost(RawKind.PSI, STRUCT_SOURCE), true)

    fun testLightTreeRestoresGenericStructMember() = assertMemberRestoration(buildHost(RawKind.LIGHT_TREE, STRUCT_SOURCE), true)

    fun testPsiRestoresActualExtendMember() = assertExtendRestoration(buildHost(RawKind.PSI, EXTEND_SOURCE))

    fun testLightTreeRestoresActualExtendMember() = assertExtendRestoration(buildHost(RawKind.LIGHT_TREE, EXTEND_SOURCE))

    fun testPsiRestoresActualReturnTarget() = assertReturnRestoration(buildHost(RawKind.PSI, RETURN_SOURCE))

    fun testLightTreeRestoresActualReturnTarget() = assertReturnRestoration(buildHost(RawKind.LIGHT_TREE, RETURN_SOURCE))

    fun testPsiRestoresClassPrimaryParameter() = assertPrimaryRestoration(buildHost(RawKind.PSI, CLASS_PRIMARY_SOURCE), CjParameterGrammar.CLASS_PRIMARY)

    fun testLightTreeRestoresClassPrimaryParameter() = assertPrimaryRestoration(buildHost(RawKind.LIGHT_TREE, CLASS_PRIMARY_SOURCE), CjParameterGrammar.CLASS_PRIMARY)

    fun testPsiRestoresStructPrimaryParameter() = assertPrimaryRestoration(buildHost(RawKind.PSI, STRUCT_PRIMARY_SOURCE), CjParameterGrammar.STRUCT_PRIMARY)

    fun testLightTreeRestoresStructPrimaryParameter() = assertPrimaryRestoration(buildHost(RawKind.LIGHT_TREE, STRUCT_PRIMARY_SOURCE), CjParameterGrammar.STRUCT_PRIMARY)

    private fun assertMemberRestoration(host: Host, isStruct: Boolean) {
        val owner = assertIs<CfirClassLikeDeclaration>(host.raw.cfirFile.declarations.single())
        if (isStruct) assertIs<CfirStruct>(owner) else assertIs<CfirClass>(owner)
        val original = owner.declarations.filterIsInstance<CfirNamedFunction>().single()
        val snapshot = annotationSnapshot(host.raw, original)
        assertNull(snapshot.signature)
        assertNull(snapshot.functionTarget)
        assertSame(owner.symbol, snapshot.containerSymbols.single())
        val generated = buildMember(host, snapshot)

        assertNotSame(original, generated)
        assertNotSame(original.symbol, generated.symbol)
        assertSame(generated, generated.symbol.cfir)
        assertFalse(generated.isLocal)
        assertEquals(CallableId(owner.symbol.classId, Name.identifier("generated")), generated.symbol.callableId)
        assertSame(snapshot.dispatchReceiverType, generated.dispatchReceiverType)
        val receiver = assertIs<ConeClassifierType>(generated.dispatchReceiverType)
        if (isStruct) assertIs<ConeStructType>(receiver) else assertIs<ConeClassLikeType>(receiver)
        assertSame(owner.symbol.toLookupTag(), receiver.lookupTag)
        val argument = assertIs<ConeTypeParameterType>(receiver.typeArguments.single().type)
        assertSame(owner.typeParameters.single().symbol, argument.lookupTag.typeParameterSymbol)
        assertNull(generated.containingExtend)
        val returnExpression = assertIs<CfirReturnExpression>(checkNotNull(generated.body).statements.single())
        assertSame(generated, returnExpression.target.labeledElement)
        assertSame(original, owner.declarations.filterIsInstance<CfirNamedFunction>().single())
    }

    private fun assertExtendRestoration(host: Host) {
        val owner = host.raw.cfirFile.declarations.filterIsInstance<CfirExtend>().single()
        val original = owner.declarations.filterIsInstance<CfirNamedFunction>().single()
        val snapshot = annotationSnapshot(host.raw, original)
        assertNull(snapshot.signature)
        assertNull(snapshot.functionTarget)
        assertNull(snapshot.dispatchReceiverType)
        assertSame(owner.symbol, snapshot.containerSymbols.single())
        val generated = buildMember(host, snapshot)

        assertNotSame(original.symbol, generated.symbol)
        assertSame(owner, original.containingExtend)
        assertSame(owner, generated.containingExtend)
        assertSame(owner, snapshot.containerSymbols.single().cfir)
        assertNull(generated.dispatchReceiverType)
        assertNull(generated.symbol.callableId.classId)
        assertEquals(CallableId(snapshot.packageFqName, Name.identifier("generated")), generated.symbol.callableId)
        assertSame(original, owner.declarations.single())
    }

    private fun assertReturnRestoration(host: Host) {
        val function = assertIs<CfirNamedFunction>(host.raw.cfirFile.declarations.single())
        val original = assertIs<CfirReturnExpression>(checkNotNull(function.body).statements.single())
        val originalSurface = assertIs<MacroSurfaceExpr>(host.raw.surfaces.single { it.replaceHandle.carrier === original.result })
        val expectedFacts = MacroParsingFacts(
            MacroParsingScope.Expression(CjExprKind.ALL),
            CjMacroFragmentParent.OTHER,
            null,
        )
        assertEquals(expectedFacts, originalSurface.parsingFacts)
        val snapshot = originalSurface.replaceHandle.ownerSnapshot
        assertSame(original.target, snapshot.functionTarget)
        assertSame(function, original.target.labeledElement)
        assertSame(function.symbol, snapshot.containerSymbols.single())
        assertNull(snapshot.signature)
        val (generated, surfaces) = when (host.kind) {
            RawKind.PSI -> {
                val syntax = assertIs<CjReturnExpression>(assertIs<CjPsiSourceElement>(original.source).psi)
                val builder = newPsiBuilder(host)
                builder.buildExpressionFragment(syntax, snapshot) to builder.consumeCollectedMacroSurfaces()
            }
            RawKind.LIGHT_TREE -> {
                val syntax = assertIs<CjLightSourceElement>(original.source)
                val builder = newLightBuilder(host, syntax)
                builder.buildExpressionFragment(syntax.lighterASTNode, snapshot) to builder.consumeCollectedMacroSurfaces()
            }
        }
        val restored = assertIs<CfirReturnExpression>(generated)
        assertNotSame(original, restored)
        assertSame(original.target, restored.target)
        assertSame(function, restored.target.labeledElement)
        assertSame(original, checkNotNull(function.body).statements.single())
        val restoredSurface = assertIs<MacroSurfaceExpr>(surfaces.single { it.replaceHandle.carrier === restored.result })
        assertEquals(originalSurface.parsingFacts, restoredSurface.parsingFacts)
        val restoredSnapshot = restoredSurface.replaceHandle.ownerSnapshot
        assertSame(snapshot.functionTarget, restoredSnapshot.functionTarget)
        assertSame(function.symbol, restoredSnapshot.containerSymbols.single())
        assertEquals(snapshot.inLocalContext, restoredSnapshot.inLocalContext)
    }

    private fun assertPrimaryRestoration(host: Host, grammar: CjParameterGrammar) {
        val owner = assertIs<CfirClassLikeDeclaration>(host.raw.cfirFile.declarations.single())
        val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single()
        val original = constructor.valueParameters.single()
        val snapshot = annotationSnapshot(host.raw, original)
        val signature = assertIs<MacroParameterSignature.Declaration>(snapshot.signature)
        assertSame(constructor.symbol, signature.owner)
        assertEquals(grammar, signature.grammar)
        // 主构造器参数属于名义类型下的实际构造器帧，参数区没有正文 return 目标。
        assertEquals(2, snapshot.containerSymbols.size)
        assertSame(owner.symbol, snapshot.containerSymbols[0])
        assertSame(constructor.symbol, snapshot.containerSymbols[1])
        assertNull(snapshot.functionTarget)
        assertTrue(constructor.isPrimary)
        val (generated, surfaces) = when (host.kind) {
            RawKind.PSI -> {
                val syntax = assertIs<CjParameter>(assertIs<CjPsiSourceElement>(original.source).psi)
                val builder = newPsiBuilder(host)
                builder.buildValueParameterFragment(syntax, snapshot) to builder.consumeCollectedMacroSurfaces()
            }
            RawKind.LIGHT_TREE -> {
                val syntax = assertIs<CjLightSourceElement>(original.source)
                val builder = newLightBuilder(host, syntax)
                builder.buildValueParameterFragment(syntax.lighterASTNode, snapshot) to builder.consumeCollectedMacroSurfaces()
            }
        }

        assertNotSame(original, generated)
        assertNotSame(original.symbol, generated.symbol)
        assertSame(constructor.symbol, generated.containingDeclarationSymbol)
        assertSame(original, constructor.valueParameters.single())
        assertTrue(original.isMemberParameter)
        assertTrue(generated.isMemberParameter)
        assertTrue(generated.isNamed)
        val originalField = checkNotNull(original.memberParameterFieldInfo)
        val generatedField = checkNotNull(generated.memberParameterFieldInfo)
        assertNotSame(originalField, generatedField)
        assertTrue(generatedField.isVar)
        assertEquals(Visibilities.Public, generatedField.createFieldStatus().visibility)
        val annotation = assertIs<CfirAnnotationCall>(generated.annotations.single())
        assertSame(constructor.symbol, annotation.containingDeclarationSymbol)
        val generatedSurface = surfaces.single { it.replaceHandle.annotationCarrier?.originalAnnotation === annotation }
        assertSame(generated, checkNotNull(generatedSurface.replaceHandle.annotationCarrier).owner)
        assertSame(generated, checkNotNull(host.raw.annotationMetadata.snapshot(annotation)).owner)
        val restoredSnapshot = generatedSurface.replaceHandle.ownerSnapshot
        val restoredSignature = assertIs<MacroParameterSignature.Declaration>(restoredSnapshot.signature)
        assertSame(signature.owner, restoredSignature.owner)
        assertEquals(grammar, restoredSignature.grammar)
        assertEquals(2, restoredSnapshot.containerSymbols.size)
        assertSame(owner.symbol, restoredSnapshot.containerSymbols[0])
        assertSame(constructor.symbol, restoredSnapshot.containerSymbols[1])
        assertNull(restoredSnapshot.functionTarget)
        assertSame(snapshot.dispatchReceiverType, restoredSnapshot.dispatchReceiverType)
    }

    private fun buildMember(host: Host, snapshot: MacroRawOwnerSnapshot): CfirNamedFunction {
        val declaration = when (host.kind) {
            RawKind.PSI -> newPsiBuilder(host).buildDeclarationFragment(createCjFile("generatedMember", MEMBER_FRAGMENT), snapshot)
            RawKind.LIGHT_TREE -> {
                val parsed = parseLightFile(MEMBER_FRAGMENT)
                LightTreeRawCfirDeclarationBuilder(
                    session = host.session,
                    baseScopeProvider = host.session.cangjieScopeProvider,
                    tree = parsed.tree,
                    source = MEMBER_FRAGMENT,
                    sourceKind = parsed.fileFacts.sourceKind,
                    macroFacts = parsed.macroFacts,
                    nominalFacts = parsed.cjmpNominalFacts,
                    declarationModifiers = parsed.declarationModifiers,
                    annotationMetadata = host.raw.annotationMetadata,
                ).buildDeclarationFragment(parsed.tree.root, snapshot)
            }
        }
        return assertIs<CfirNamedFunction>(declaration)
    }

    private fun annotationSnapshot(raw: PreMacroCfirFile, declaration: CfirDeclaration): MacroRawOwnerSnapshot {
        val annotation = assertIs<CfirAnnotationCall>(declaration.annotations.single())
        val surface = raw.surfaces.single { it.replaceHandle.annotationCarrier?.originalAnnotation === annotation }
        assertSame(declaration, checkNotNull(surface.replaceHandle.annotationCarrier).owner)
        return surface.replaceHandle.ownerSnapshot
    }

    private fun newPsiBuilder(host: Host): PsiRawCfirBuilder =
        PsiRawCfirBuilder(host.session, BodyBuildingMode.NORMAL, host.raw.annotationMetadata)

    private fun newLightBuilder(host: Host, syntax: CjLightSourceElement): LightTreeRawCfirDeclarationBuilder =
        LightTreeRawCfirDeclarationBuilder(
            session = host.session,
            baseScopeProvider = host.session.cangjieScopeProvider,
            tree = syntax.treeStructure,
            source = host.source,
            macroFacts = checkNotNull(host.macroFacts),
            nominalFacts = checkNotNull(host.nominalFacts),
            declarationModifiers = checkNotNull(host.declarationModifiers),
            annotationMetadata = host.raw.annotationMetadata,
        )

    private fun buildHost(kind: RawKind, source: String): Host {
        val session = createTestSession()
        val parsed = if (kind == RawKind.LIGHT_TREE) parseLightFile(source) else null
        val raw = when (kind) {
            RawKind.PSI -> PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL, CfirAnnotationMetadataBuilder())
                .buildPreMacroCfirFile(createCjFile("ownerRestoration", source))
            RawKind.LIGHT_TREE -> {
                val sourceFile = CjInMemoryTextSourceFile("ownerRestoration.cj", null, source)
                LightTree2Cfir(session, session.cangjieScopeProvider)
                    .buildPreMacroCfirFile(checkNotNull(parsed), sourceFile, source.toSourceLinesMapping())
            }
        }
        return Host(kind, session, source, raw, parsed?.macroFacts, parsed?.cjmpNominalFacts, parsed?.declarationModifiers)
    }

    private data class Host(
        val kind: RawKind,
        val session: CfirSession,
        val source: String,
        val raw: PreMacroCfirFile,
        val macroFacts: Map<Int, org.cangnova.cangjie.parsing.MacroParsingFacts>?,
        val nominalFacts: Map<Int, org.cangnova.cangjie.parsing.CjmpNominalSyntaxFacts>?,
        val declarationModifiers: Map<Int, org.cangnova.cangjie.parsing.CjParsedDeclarationModifiers>?,
    )

    private enum class RawKind { PSI, LIGHT_TREE }

    companion object {
        private const val MEMBER_FRAGMENT = "func generated(value: T): T { return value }"

        private val CLASS_SOURCE = """
            package snapshots
            class Owner<T> {
                @!A func original(value: T): T { return value }
            }
        """.trimIndent()

        private val STRUCT_SOURCE = """
            package snapshots
            struct Owner<T> {
                @!A func original(value: T): T { return value }
            }
        """.trimIndent()

        private val EXTEND_SOURCE = """
            package snapshots
            class Box<T> {}
            extend<T> Box<T> {
                @!A func original(value: T): T { return value }
            }
        """.trimIndent()

        private val RETURN_SOURCE = """
            package snapshots
            func original(): Int64 { return @M(1) }
        """.trimIndent()

        private val CLASS_PRIMARY_SOURCE = """
            package snapshots
            class Owner {
                public Owner(@!A public var value!: Int64 = 1) {}
            }
        """.trimIndent()

        private val STRUCT_PRIMARY_SOURCE = """
            package snapshots
            struct Owner {
                public Owner(@!A public var value!: Int64 = 1) {}
            }
        """.trimIndent()
    }
}
