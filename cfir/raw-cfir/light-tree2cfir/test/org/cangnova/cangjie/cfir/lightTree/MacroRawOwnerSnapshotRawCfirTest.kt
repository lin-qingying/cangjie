package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroParameterSignature
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawOwnerSnapshot
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.parsing.CjMemberParsingContext
import org.cangnova.cangjie.parsing.CjParameterGrammar
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 两条 raw 路径必须在同次构建中保存实际 owner、泛型 self type 与延迟绑定的 target。
 * 通过真实 CFIR 槽位匹配 surface，不用名称、源码位置或 ClassId 重新认领声明。
 * 这里只验证 raw 捕获契约，不代表宏 fragment 解析或 slice 替换已经贯通。
 */
@RunWith(JUnit3RunnerWithInners::class)
class MacroRawOwnerSnapshotRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiUnicodeMacroHead() = assertUnicodeMacroHead(RawKind.PSI)

    fun testLightTreeUnicodeMacroHead() = assertUnicodeMacroHead(RawKind.LIGHT_TREE)

    fun testPsiMacroAnnotationTriviaSource() = assertMacroAnnotationTriviaSource(RawKind.PSI)

    fun testLightTreeMacroAnnotationTriviaSource() = assertMacroAnnotationTriviaSource(RawKind.LIGHT_TREE)

    fun testPsiMalformedMacroAnnotationSource() = assertMalformedMacroAnnotationSource(RawKind.PSI)

    fun testLightTreeMalformedMacroAnnotationSource() = assertMalformedMacroAnnotationSource(RawKind.LIGHT_TREE)

    private fun assertMalformedMacroAnnotationSource(kind: RawKind) {
        val source = "package macro_trivia\nclass Owner { @Foo[\\x] func f(): Unit {} }"
        val raw = build(kind, source)
        val snapshot = checkNotNull(raw.annotationMetadata.snapshotForSurface(raw.surfaces.single()))
        assertEquals(snapshot.rawSyntax, source.substring(snapshot.annotationSource.startOffset, snapshot.annotationSource.endOffset))
        for (token in snapshot.tokens) {
            assertEquals(token.text, source.substring(token.startOffset, token.endOffset))
        }
    }

    private fun assertMacroAnnotationTriviaSource(kind: RawKind) {
        // cjc 1.1.3 接受宏头部的注释和空白，仅报告 Foo 未声明。
        val source = "package macro_trivia\nclass Owner { @ /*inside*/ Foo [value: 1] func f(): Unit {} }"
        val raw = build(kind, source)
        val snapshot = checkNotNull(raw.annotationMetadata.snapshotForSurface(raw.surfaces.single()))
        assertEquals("@ /*inside*/ Foo [value: 1]", snapshot.rawSyntax)
        assertEquals(snapshot.rawSyntax, source.substring(snapshot.annotationSource.startOffset, snapshot.annotationSource.endOffset))
        for (token in snapshot.tokens) {
            assertEquals(token.text, source.substring(token.startOffset, token.endOffset))
        }
    }

    private fun assertUnicodeMacroHead(kind: RawKind) {
        // cjc 1.1.3 将两种名称解析为 café，随后仅因宏未声明而报错。
        for (name in listOf("cafe\u0301", "`cafe\u0301`")) {
            val source = "package unicode_macro\n@$name func f(): Unit {}"
            val surface = build(kind, source).surfaces.single()
            assertEquals("unicode_macro.café", surface.qualifiedName?.asString())
            assertFalse(surface.isQualifiedName)
            assertEquals("@$name func f(): Unit {}", surface.capturedRawSyntax)
        }
    }

    fun testPsiGenericClassPrimaryAndMemberOwners() = assertNominalOwners(build(RawKind.PSI, CLASS_SOURCE), false)

    fun testLightTreeGenericClassPrimaryAndMemberOwners() = assertNominalOwners(build(RawKind.LIGHT_TREE, CLASS_SOURCE), false)

    fun testPsiGenericStructPrimaryAndMemberOwners() = assertNominalOwners(build(RawKind.PSI, STRUCT_SOURCE), true)

    fun testLightTreeGenericStructPrimaryAndMemberOwners() = assertNominalOwners(build(RawKind.LIGHT_TREE, STRUCT_SOURCE), true)

    fun testPsiTopLevelDefaultHasSignatureWithoutFunctionTarget() = assertTopLevelDefault(build(RawKind.PSI, DEFAULT_SOURCE))

    fun testLightTreeTopLevelDefaultHasSignatureWithoutFunctionTarget() = assertTopLevelDefault(build(RawKind.LIGHT_TREE, DEFAULT_SOURCE))

    fun testPsiLambdaBodyMasksAndRestoresDefaultSignature() = assertLambdaDefault(build(RawKind.PSI, LAMBDA_SOURCE))

    fun testLightTreeLambdaBodyMasksAndRestoresDefaultSignature() = assertLambdaDefault(build(RawKind.LIGHT_TREE, LAMBDA_SOURCE))

    fun testPsiLocalFunctionUsesOwnSignatureAndTarget() = assertLocalFunction(build(RawKind.PSI, LOCAL_SOURCE))

    fun testLightTreeLocalFunctionUsesOwnSignatureAndTarget() = assertLocalFunction(build(RawKind.LIGHT_TREE, LOCAL_SOURCE))

    fun testPsiExtendRetainsActualOwnerWithoutNominalReceiver() = assertExtendOwner(build(RawKind.PSI, EXTEND_SOURCE))

    fun testLightTreeExtendRetainsActualOwnerWithoutNominalReceiver() = assertExtendOwner(build(RawKind.LIGHT_TREE, EXTEND_SOURCE))

    fun testPsiContainerSnapshotsSurviveLaterPushAndPop() = assertStableContainers(build(RawKind.PSI, SIBLING_SOURCE))

    fun testLightTreeContainerSnapshotsSurviveLaterPushAndPop() = assertStableContainers(build(RawKind.LIGHT_TREE, SIBLING_SOURCE))

    private fun build(kind: RawKind, source: String): PreMacroCfirFile {
        val session = createTestSession()
        return when (kind) {
            RawKind.PSI -> PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL, CfirAnnotationMetadataBuilder())
                .buildPreMacroCfirFile(createCjFile("ownerSnapshots", source))
            RawKind.LIGHT_TREE -> {
                val sourceFile = CjInMemoryTextSourceFile("ownerSnapshots.cj", null, source)
                LightTree2Cfir(session, session.cangjieScopeProvider)
                    .buildPreMacroCfirFile(parseLightFile(source), sourceFile, source.toSourceLinesMapping())
            }
        }
    }

    private fun assertNominalOwners(raw: PreMacroCfirFile, isStruct: Boolean) {
        val owner = assertIs<CfirClassLikeDeclaration>(raw.cfirFile.declarations.single())
        val grammar = if (isStruct) CjParameterGrammar.STRUCT_PRIMARY else CjParameterGrammar.CLASS_PRIMARY
        val nominalKind = if (isStruct) CjMemberParsingContext.Kind.STRUCT else CjMemberParsingContext.Kind.CLASS
        if (isStruct) assertIs<CfirStruct>(owner) else assertIs<CfirClass>(owner)
        val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single()
        val function = owner.declarations.filterIsInstance<CfirNamedFunction>().single()
        val primaryParameter = constructor.valueParameters.single()
        val regularParameter = function.valueParameters.single()
        assertTrue(constructor.isPrimary)
        assertEquals(4, raw.surfaces.size)

        val primary = annotationSnapshot(raw, primaryParameter)
        val regular = annotationSnapshot(raw, regularParameter)
        val default = expressionSnapshot(raw, checkNotNull(regularParameter.defaultValue))
        val body = returnSnapshot(raw, function)
        assertSignature(primary, constructor, grammar)
        assertSignature(regular, function, CjParameterGrammar.REGULAR_TYPED)
        assertSignature(default, function, CjParameterGrammar.REGULAR_TYPED)
        // 参数区保留所属 callable 帧；词法容器存在不代表已进入可 return 的正文。
        assertContainers(primary, owner.symbol, constructor.symbol)
        assertContainers(regular, owner.symbol, function.symbol)
        assertContainers(default, owner.symbol, function.symbol)
        for (snapshot in listOf(primary, regular, default)) {
            assertFalse(snapshot.inLocalContext)
            assertNull(snapshot.functionTarget)
        }
        assertContainers(body, owner.symbol, function.symbol)
        assertTrue(body.inLocalContext)
        assertNull(body.signature)
        assertTarget(body, function)

        val receiver = assertIs<ConeClassifierType>(function.dispatchReceiverType)
        if (isStruct) assertIs<ConeStructType>(receiver) else assertIs<ConeClassLikeType>(receiver)
        assertSame(owner.symbol.toLookupTag(), receiver.lookupTag)
        val typeArgument = assertIs<ConeTypeParameterType>(receiver.typeArguments.single().type)
        assertSame(owner.typeParameters.single().symbol, typeArgument.lookupTag.typeParameterSymbol)
        for (snapshot in listOf(primary, regular, default, body)) {
            assertSame(receiver, snapshot.dispatchReceiverType)
            assertEquals(CjMemberParsingContext(nominalKind, "Owner"), snapshot.nominalSyntax)
        }
    }

    private fun assertTopLevelDefault(raw: PreMacroCfirFile) {
        val function = assertIs<CfirNamedFunction>(raw.cfirFile.declarations.single())
        val parameter = function.valueParameters.single()
        assertEquals(3, raw.surfaces.size)
        val annotation = annotationSnapshot(raw, parameter)
        val default = expressionSnapshot(raw, checkNotNull(parameter.defaultValue))
        for (snapshot in listOf(annotation, default)) {
            assertSignature(snapshot, function, CjParameterGrammar.REGULAR_TYPED)
            assertContainers(snapshot, function.symbol)
            assertNull(snapshot.functionTarget)
            assertNull(snapshot.dispatchReceiverType)
            assertNull(snapshot.nominalSyntax)
            assertFalse(snapshot.inLocalContext)
        }
        val body = returnSnapshot(raw, function)
        assertContainers(body, function.symbol)
        assertNull(body.signature)
        assertTarget(body, function)
        assertTrue(body.inLocalContext)
    }

    private fun assertLambdaDefault(raw: PreMacroCfirFile) {
        val outer = assertIs<CfirNamedFunction>(raw.cfirFile.declarations.first())
        val parameter = outer.valueParameters.single()
        val call = assertIs<CfirFunctionCall>(parameter.defaultValue)
        assertEquals(2, call.argumentList.arguments.size)
        val lambda = assertIs<CfirAnonymousFunctionExpression>(call.argumentList.arguments[0]).anonymousFunction
        val lambdaBody = returnSnapshot(raw, lambda)
        val followingArgument = expressionSnapshot(raw, call.argumentList.arguments[1])
        assertEquals(3, raw.surfaces.size)

        assertContainers(lambdaBody, outer.symbol, lambda.symbol)
        assertNull(lambdaBody.signature)
        assertTrue(lambdaBody.inLocalContext)
        assertTarget(lambdaBody, lambda)
        assertTrue(checkNotNull(lambdaBody.functionTarget).isLambda)
        assertSignature(followingArgument, outer, CjParameterGrammar.REGULAR_TYPED)
        assertContainers(followingArgument, outer.symbol)
        assertNull(followingArgument.functionTarget)
        assertFalse(followingArgument.inLocalContext)

        val outerBody = returnSnapshot(raw, outer)
        assertContainers(outerBody, outer.symbol)
        assertNull(outerBody.signature)
        assertTarget(outerBody, outer)
        assertNotSame(lambdaBody.functionTarget, outerBody.functionTarget)
    }

    private fun assertLocalFunction(raw: PreMacroCfirFile) {
        val outer = assertIs<CfirNamedFunction>(raw.cfirFile.declarations.single())
        val local = checkNotNull(outer.body).statements.filterIsInstance<CfirNamedFunction>().single()
        val outerParameter = outer.valueParameters.single()
        val localParameter = local.valueParameters.single()
        assertTrue(local.isLocal)
        assertEquals(6, raw.surfaces.size)
        val outerAnnotation = annotationSnapshot(raw, outerParameter)
        val outerDefault = expressionSnapshot(raw, checkNotNull(outerParameter.defaultValue))
        for (snapshot in listOf(outerAnnotation, outerDefault)) {
            assertSignature(snapshot, outer, CjParameterGrammar.REGULAR_TYPED)
            assertContainers(snapshot, outer.symbol)
            assertNull(snapshot.functionTarget)
        }

        val localAnnotation = annotationSnapshot(raw, localParameter)
        val localDefault = expressionSnapshot(raw, checkNotNull(localParameter.defaultValue))
        val outerBody = returnSnapshot(raw, outer)
        for (snapshot in listOf(localAnnotation, localDefault)) {
            assertSignature(snapshot, local, CjParameterGrammar.REGULAR_TYPED)
            assertContainers(snapshot, outer.symbol, local.symbol)
            assertTrue(snapshot.inLocalContext)
            // 官方禁止默认值 return 到 local 或 outer；这里保留声明路径并屏蔽正文目标。
            assertNull(snapshot.functionTarget)
            assertNotSame(outerBody.functionTarget, snapshot.functionTarget)
        }
        val localBody = returnSnapshot(raw, local)
        assertContainers(localBody, outer.symbol, local.symbol)
        assertNull(localBody.signature)
        assertTarget(localBody, local)
        assertNotSame(outerBody.functionTarget, localBody.functionTarget)
        assertContainers(outerBody, outer.symbol)
        assertNull(outerBody.signature)
        assertTarget(outerBody, outer)
    }

    private fun assertExtendOwner(raw: PreMacroCfirFile) {
        val owner = raw.cfirFile.declarations.filterIsInstance<CfirExtend>().single()
        val function = owner.declarations.filterIsInstance<CfirNamedFunction>().single()
        val parameter = function.valueParameters.single()
        val annotation = annotationSnapshot(raw, parameter)
        val default = expressionSnapshot(raw, checkNotNull(parameter.defaultValue))
        val body = returnSnapshot(raw, function)
        assertEquals(1, owner.typeParameters.size)
        assertEquals(3, raw.surfaces.size)
        for (snapshot in listOf(annotation, default)) {
            assertContainers(snapshot, owner.symbol, function.symbol)
            assertSignature(snapshot, function, CjParameterGrammar.REGULAR_TYPED)
            assertNull(snapshot.functionTarget)
        }
        assertContainers(body, owner.symbol, function.symbol)
        assertNull(body.signature)
        assertTarget(body, function)
        for (snapshot in listOf(annotation, default, body)) {
            assertNull(snapshot.dispatchReceiverType)
            assertEquals(CjMemberParsingContext(CjMemberParsingContext.Kind.EXTEND, null), snapshot.nominalSyntax)
            assertSame(owner, snapshot.containerSymbols.first().cfir)
        }
    }

    private fun assertStableContainers(raw: PreMacroCfirFile) {
        val owner = raw.cfirFile.declarations.filterIsInstance<CfirClass>().single()
        val members = owner.declarations.filterIsInstance<CfirNamedFunction>()
        val topLevel = raw.cfirFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertEquals(2, members.size)
        assertEquals(3, raw.surfaces.size)
        val first = returnSnapshot(raw, members[0])
        val second = returnSnapshot(raw, members[1])
        val last = returnSnapshot(raw, topLevel)
        assertContainers(first, owner.symbol, members[0].symbol)
        assertContainers(second, owner.symbol, members[1].symbol)
        assertContainers(last, topLevel.symbol)
        assertNotSame(first.containerSymbols, second.containerSymbols)
        assertNotSame(second.containerSymbols, last.containerSymbols)
        assertSame(first.dispatchReceiverType, second.dispatchReceiverType)
        assertNull(last.dispatchReceiverType)
        assertNull(last.nominalSyntax)

        // 文件构建已完成所有 push/pop；早先快照仍保留原路径，并拒绝外部修改。
        for (snapshot in listOf(first, second, last)) {
            @Suppress("UNCHECKED_CAST")
            val path = snapshot.containerSymbols as MutableList<CfirBasedSymbol<*>>
            assertFailsWith<UnsupportedOperationException> { path.clear() }
        }
        assertContainers(first, owner.symbol, members[0].symbol)
        assertContainers(second, owner.symbol, members[1].symbol)
        assertContainers(last, topLevel.symbol)
    }

    private fun annotationSnapshot(raw: PreMacroCfirFile, parameter: CfirValueParameter): MacroRawOwnerSnapshot {
        val annotation = assertIs<CfirAnnotationCall>(parameter.annotations.single())
        val surface = raw.surfaces.single { it.replaceHandle.annotationCarrier?.originalAnnotation === annotation }
        val carrier = checkNotNull(surface.replaceHandle.annotationCarrier)
        assertSame(parameter, carrier.owner)
        assertSame(parameter.containingDeclarationSymbol, annotation.containingDeclarationSymbol)
        assertSame(parameter, checkNotNull(raw.annotationMetadata.snapshot(annotation)).owner)
        assertSame(parameter.containingDeclarationSymbol, checkNotNull(surface.replaceHandle.ownerSnapshot.signature).owner)
        return surface.replaceHandle.ownerSnapshot
    }

    private fun expressionSnapshot(raw: PreMacroCfirFile, expression: CfirExpression): MacroRawOwnerSnapshot =
        raw.surfaces.single { it.replaceHandle.carrier === expression }.replaceHandle.ownerSnapshot

    private fun returnSnapshot(raw: PreMacroCfirFile, function: CfirFunction): MacroRawOwnerSnapshot {
        val returnExpression = checkNotNull(function.body).statements.filterIsInstance<CfirReturnExpression>().single()
        return expressionSnapshot(raw, returnExpression.result).also { snapshot ->
            assertSame(returnExpression.target, snapshot.functionTarget)
        }
    }

    private fun assertSignature(snapshot: MacroRawOwnerSnapshot, function: CfirFunction, grammar: CjParameterGrammar) {
        val signature = assertIs<MacroParameterSignature.Declaration>(snapshot.signature)
        assertSame(function.symbol, signature.owner)
        assertSame(function, signature.owner.cfir)
        assertEquals(grammar, signature.grammar)
    }

    private fun assertTarget(snapshot: MacroRawOwnerSnapshot, function: CfirFunction) {
        val target = checkNotNull(snapshot.functionTarget)
        assertTrue(target.isBound)
        assertSame(function, target.labeledElement)
        assertSame(function.symbol, target.labeledElement.symbol)
    }

    private fun assertContainers(snapshot: MacroRawOwnerSnapshot, vararg symbols: CfirBasedSymbol<*>) {
        assertEquals(FqName("snapshots"), snapshot.packageFqName)
        assertEquals(symbols.size, snapshot.containerSymbols.size)
        symbols.zip(snapshot.containerSymbols).forEach { (expected, actual) -> assertSame(expected, actual) }
    }

    private enum class RawKind { PSI, LIGHT_TREE }

    companion object {
        private val CLASS_SOURCE = """
            package snapshots
            class Owner<T> {
                public Owner(@!A public let value: T) {}
                func read(@!A fallback!: T = @M(value)): T { return @M(fallback) }
            }
        """.trimIndent()

        private val STRUCT_SOURCE = """
            package snapshots
            struct Owner<T> {
                public Owner(@!A public let value: T) {}
                func read(@!A fallback!: T = @M(value)): T { return @M(fallback) }
            }
        """.trimIndent()

        private val DEFAULT_SOURCE = """
            package snapshots
            func outer(@!A value!: Int64 = @M(1)): Int64 { return @M(value) }
        """.trimIndent()

        private val LAMBDA_SOURCE = """
            package snapshots
            func outer(value!: Int64 = combine({ => return @M(1) }, @M(2))): Int64 {
                return @M(value)
            }
            func combine(action: () -> Int64, value: Int64): Int64 { return action() + value }
        """.trimIndent()

        private val LOCAL_SOURCE = """
            package snapshots
            func outer(@!A seed!: Int64 = @M(0)): Int64 {
                func local(@!A value!: Int64 = @M(seed)): Int64 { return @M(value) }
                return @M(seed)
            }
        """.trimIndent()

        private val EXTEND_SOURCE = """
            package snapshots
            class Box<T> {}
            extend<T> Box<T> {
                func read(@!A value!: Int64 = @M(1)): Int64 { return @M(value) }
            }
        """.trimIndent()

        private val SIBLING_SOURCE = """
            package snapshots
            class Owner<T> {
                func first(): Int64 { return @M(1) }
                func second(): Int64 { return @M(2) }
            }
            func last(): Int64 { return @M(3) }
        """.trimIndent()
    }
}
