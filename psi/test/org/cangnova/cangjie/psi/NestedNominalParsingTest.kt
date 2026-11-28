package org.cangnova.cangjie.psi

import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.AbstractCangJieParsing.ParsingContext
import org.cangnova.cangjie.parsing.CangJieParsing
import org.cangnova.cangjie.parsing.SemanticWhitespaceAwarePsiBuilderImpl
import org.cangnova.cangjie.parsing.collectParsingDiagnostics
import org.cangnova.cangjie.parsing.CjParsingErrorElementType
import org.cangnova.cangjie.parsing.assertPublishedParsingDiagnosticParity
import org.cangnova.cangjie.parsing.parseLightWithPsiInput
import org.cangnova.cangjie.parsing.publishedParserSeed
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 官方scope错误只标记nominal关键字，完成的声明仍须保留真实父子层次。 */
class NestedNominalParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    /** authority-nested-scope-matrix/authority-nested-extend-scope：主范围仅为关键字。 */
    @Test
    fun nestedNominalScopeErrorsKeepKeywordRangesAndCompletedSiblingOrder() {
        val nestedDeclarations = listOf(
            "class" to "public class NestedClass { init() {} }",
            "interface" to "public interface NestedInterface {}",
            "struct" to "public struct NestedStruct { init() {} }",
            "enum" to "public enum NestedEnum { Case }",
            "extend" to "extend Int32 {}",
        )
        val cases = buildList {
            for (ownerKind in listOf("class", "extend")) {
                for (nested in nestedDeclarations) add(ownerKind to nested)
            }
            for (ownerKind in listOf("interface", "struct", "enum")) {
                add(ownerKind to nestedDeclarations.first())
            }
        }
        for ((index, case) in cases.withIndex()) {
            val (ownerKind, nested) = case
            val (nestedKind, nestedSource) = nested
            val ownerHeader = if (ownerKind == "extend") "extend Int64" else "$ownerKind Owner"
            val enumCase = if (ownerKind == "enum") "Case" else ""
            val text = """
                package nestedscope
                $ownerHeader {
                    $enumCase
                    func before(): Int64 { 1 }
                    $nestedSource
                    func after(): Int64 { 2 }
                }
                func following(): Int64 { 3 }
            """.trimIndent()
            val file = parseBoth("nestedScope$index", text)
            val keywordOffset = text.indexOf(nestedSource) + nestedSource.indexOf(nestedKind)
            val diagnostic = file.collectParsingDiagnostics().single()
            assertEquals(keywordOffset, diagnostic.startOffset, text)
            assertEquals(keywordOffset + nestedKind.length, diagnostic.endOffset, text)
            assertEquals(ParsingDiagnostic.Origin.INPUT, diagnostic.origin, text)
            kotlin.test.assertEquals(
                CangJieParsingBundle.message("parsing.error.invalid.declaration.in.class", nestedKind,
                    CangJieParsingBundle.message("parsing.scope.$ownerKind")),
                diagnostic.message,
                text,
            )
            val owner = file.declarations.filterIsInstance<CjTypeStatement>().single()
            val declarations = checkNotNull(owner.body).declarations
            val beforeIndex = declarations.indexOfFirst { (it as? CjNamedDeclaration)?.name == "before" }
            assertTrue(beforeIndex >= 0, text)
            val nestedDeclaration = declarations[beforeIndex + 1]
            assertTrue(nestedDeclaration is CjTypeStatement, text)
            kotlin.test.assertEquals(nestedSource, nestedDeclaration.text, text)
            kotlin.test.assertEquals("after", (declarations[beforeIndex + 2] as? CjNamedDeclaration)?.name, text)
            assertEquals(beforeIndex + 3, declarations.size, text)
            assertEquals(listOf("following"), file.declarations.filterIsInstance<CjNamedFunction>().map { it.name })
        }
    }

    /** 具名primary ctor是Inner()，不能把init() secondary ctor当作本测试的证据。 */
    @Test
    fun nestedPrimaryConstructorDoesNotBecomeTheOuterPrimaryConstructor() {
        val text = """
            package nestedconstructor
            class Outer {
                class Inner {
                    Inner() {}
                }
                func after(): Int64 { 1 }
            }
        """.trimIndent()
        val file = parseBoth("nestedPrimaryConstructor", text)
        val offset = text.indexOf("class Inner")
        assertEquals(listOf(offset to offset + "class".length), file.collectParsingDiagnostics().map {
            it.startOffset to it.endOffset
        })
        val outer = file.declarations.filterIsInstance<CjClass>().single()
        val inner = checkNotNull(outer.body).declarations.filterIsInstance<CjClass>().single()
        assertNull(outer.primaryConstructor)
        assertTrue(outer.primaryConstructors.isEmpty())
        assertTrue(outer.secondaryConstructors.isEmpty())
        assertNotNull(inner.primaryConstructor)
        assertEquals(1, inner.primaryConstructors.size)
        assertEquals("Inner", inner.primaryConstructor?.name)
        assertEquals(listOf("Inner", "after"), outer.body?.declarations?.map { (it as? CjNamedDeclaration)?.name })
    }

    /** authority-broken-*十五份探针：只有class/interface缺名抑制scope，不能泛化为所有签名错误。 */
    @Test
    fun brokenNominalSignaturesUseTheirActualScopeCheckOrderWithoutLosingFollowingDeclarations() {
        val cases = listOf(
            Triple("class", "class { }", false),
            Triple("interface", "interface { }", false),
            Triple("enum", "enum { A }", true),
            Triple("struct", "struct {}", true),
            Triple("extend", "extend {}", true),
            Triple("class", "class Inner<> { }", true),
            Triple("class", "class Inner<T { }", true),
            Triple("class", "class Inner <: { }", true),
            Triple("class", "class Inner<T> where T <: { }", true),
            Triple("class", "class Inner where { }", true),
            Triple("interface", "interface Inner<> { }", true),
            Triple("interface", "interface Inner<T { }", true),
            Triple("interface", "interface Inner <: { }", true),
            Triple("interface", "interface Inner<T> where T <: { }", true),
            Triple("interface", "interface Inner where { }", true),
        )
        // 普通缺失语法符号遵守IntelliJ/Kotlin的零宽插入点契约，不混用native MainHint。
        // WHERE实际消费五个字符；scope关键字范围和官方诊断数量仍严格验证。
        val signatureRanges = listOf(
            10 to 10, 14 to 14, null, null, 12 to 12,
            17 to 17, 18 to 18, 20 to 20, 31 to 31, 17 to 22,
            21 to 21, 22 to 22, 24 to 24, 35 to 35, 21 to 26,
        )
        for ((index, case) in cases.withIndex()) {
            val (kind, declaration, hasScopeError) = case
            val text = """
                package remainingparsing

                class Outer {
                    $declaration
                    func after(): Unit {}
                }

                class Following {}
            """.trimIndent()
            val file = parseBoth("brokenNestedSignature$index", text)
            val diagnostics = file.collectParsingDiagnostics()
            assertTrue(diagnostics.isNotEmpty(), text)
            val scopeMessage = CangJieParsingBundle.message(
                "parsing.error.invalid.declaration.in.class", kind,
                CangJieParsingBundle.message("parsing.scope.class"),
            )
            val scopeErrors = diagnostics.filter { it.message == scopeMessage }
            assertEquals(if (hasScopeError) 1 else 0, scopeErrors.size, text)
            val signatureRange = signatureRanges[index]
            val expectedCount = (if (hasScopeError) 1 else 0) + (if (signatureRange != null) 1 else 0)
            assertEquals(expectedCount, diagnostics.size, text)
            val signatureErrors = diagnostics.filter { it.message != scopeMessage }
            if (signatureRange != null) {
                val lineStart = text.indexOf(declaration) - 4
                val signatureError = signatureErrors.single()
                assertEquals(lineStart + signatureRange.first - 1, signatureError.startOffset, text)
                assertEquals(lineStart + signatureRange.second - 1, signatureError.endOffset, text)
                assertEquals(ParsingDiagnostic.Origin.INPUT, signatureError.origin, text)
            } else {
                assertTrue(signatureErrors.isEmpty(), text)
            }
            if (hasScopeError) {
                val keywordOffset = text.indexOf(declaration)
                val scopeError = scopeErrors.single()
                assertEquals(keywordOffset, scopeError.startOffset, text)
                assertEquals(keywordOffset + kind.length, scopeError.endOffset, text)
                assertEquals(ParsingDiagnostic.Origin.INPUT, scopeError.origin, text)
            }
            val classes = file.declarations.filterIsInstance<CjClass>()
            assertEquals(listOf("Outer", "Following"), classes.map { it.name }, text)
            val members = checkNotNull(classes.first().body).declarations
            assertEquals(2, members.size, text)
            assertTrue(members.first() is CjTypeStatement, text)
            kotlin.test.assertEquals("after", (members.last() as? CjNamedDeclaration)?.name, text)
        }
    }

    /** authority-where-nongeneric-*：无自身泛型的函数、两类构造器及宏输入只报告WHERE。 */
    @Test
    fun nonGenericWhereDoesNotCreateConstraintsOrConsumeTheDeclarationBody() {
        val declarations = listOf(
            "func before() where {}",
            "init() where {}",
            "Outer() where {}",
            "@Unknown func before() where {}",
        )
        for ((index, declaration) in declarations.withIndex()) {
            val text = """
                package remainingparsing

                class Outer {
                    $declaration
                    func after(): Unit {}
                }

                interface I {}
                class Following {}
            """.trimIndent()
            val file = parseBoth("nonGenericWhere$index", text)
            val diagnostics = file.collectParsingDiagnostics()
            assertEquals(1, diagnostics.size, "$text\n$diagnostics")
            val error = diagnostics.single()
            kotlin.test.assertEquals(CangJieParsingBundle.message("parsing.error.unexpected.where"), error.message)
            assertEquals(text.indexOf("where"), error.startOffset)
            assertEquals(text.indexOf("where") + 5, error.endOffset)
            assertEquals(ParsingDiagnostic.Origin.INPUT, error.origin)
            val outer = file.declarations.filterIsInstance<CjClass>().first()
            val ownerBody = kotlin.test.assertNotNull(outer.body)
            val members = ownerBody.declarations
            if (declaration.startsWith("@")) {
                // 宏是直接 expression carrier，declarations 只收集真实 CjDeclaration 子节点。
                val macro = ownerBody.children.filterIsInstance<CjMacroExpression>().single()
                kotlin.test.assertEquals(declaration, macro.text)
                kotlin.test.assertSame(ownerBody, macro.parent)
                val inputDeclaration = macro.unwrappedDeclaration as CjNamedFunction
                kotlin.test.assertSame(macro.input, inputDeclaration.parent)
                kotlin.test.assertEquals("before", inputDeclaration.name)
                kotlin.test.assertEquals(declaration.removePrefix("@Unknown "), inputDeclaration.text)
                kotlin.test.assertEquals("{}", kotlin.test.assertNotNull(inputDeclaration.bodyExpression).text)
                assertEquals(1, members.size)
            } else {
                assertEquals(2, members.size)
                kotlin.test.assertEquals(declaration, members.first().text)
            }
            kotlin.test.assertEquals("after", (members.last() as CjNamedFunction).name)
            assertTrue(PsiTreeUtil.findChildrenOfType(file, CjTypeConstraintList::class.java).isEmpty())
            assertEquals(listOf("Outer", "I", "Following"), file.declarations.map { (it as CjNamedDeclaration).name })
        }
    }

    /** 官方primary自身泛型由语义层禁止，普通、const和声明宏输入必须完整保留其签名。 */
    @Test
    fun primaryOwnGenericSignatureKeepsParametersConstraintsAndBodyAcrossDeclarationCarriers() {
        val declarations = listOf(
            "Outer<T>() where T <: I {}",
            "const Outer<T>() where T <: I {}",
            "@Unknown Outer<T>() where T <: I {}",
            "@Unknown const Outer<T>() where T <: I {}",
        )
        for ((index, declaration) in declarations.withIndex()) {
            val text = """
                package remainingparsing

                interface I {}
                class Outer {
                    $declaration
                    func after(): Unit {}
                }
                class Following {}
            """.trimIndent()
            val file = parseBoth("primaryOwnGenericSignature$index", text)
            assertTrue(file.collectParsingDiagnostics().isEmpty(), text)
            val outer = file.declarations.filterIsInstance<CjClass>().first()
            val ownerBody = kotlin.test.assertNotNull(outer.body)
            val constructor = PsiTreeUtil.findChildrenOfType(ownerBody, CjPrimaryConstructor::class.java).single()
            kotlin.test.assertEquals("Outer", constructor.name, text)
            assertEquals(declaration.contains("const "), constructor.hasModifier(CjTokens.CONST_KEYWORD), text)
            val parameterList = kotlin.test.assertNotNull(constructor.typeParameterList)
            val valueParameterList = kotlin.test.assertNotNull(constructor.valueParameterList)
            val constraintList = kotlin.test.assertNotNull(constructor.typeConstraintList)
            val constructorBody = kotlin.test.assertNotNull(constructor.bodyExpression)
            for (child in listOf(parameterList, valueParameterList, constraintList, constructorBody)) {
                kotlin.test.assertSame(constructor, child.parent, text)
            }
            assertEquals(listOf("T"), constructor.typeParameters.map { it.name }, text)
            kotlin.test.assertEquals("<T>", parameterList.text, text)
            kotlin.test.assertEquals("()", valueParameterList.text, text)
            assertTrue(constructor.valueParameters.isEmpty(), text)
            // 与 Kotlin 一致：WHERE先消费，TYPE_CONSTRAINT_LIST从首个约束开始。
            kotlin.test.assertEquals("T <: I", constraintList.text, text)
            val constraint = constructor.typeConstraints.single()
            kotlin.test.assertEquals("T", constraint.subjectTypeParameterName?.text, text)
            kotlin.test.assertEquals("I", constraint.boundTypeReference?.text, text)
            kotlin.test.assertEquals("{}", constructorBody.text, text)
            assertTrue(outer.typeParameters.isEmpty(), text)
            if (declaration.startsWith("@")) {
                val macro = PsiTreeUtil.findChildrenOfType(ownerBody, CjMacroExpression::class.java).single()
                kotlin.test.assertSame(constructor, macro.unwrappedDeclaration, text)
                kotlin.test.assertSame(macro.input, constructor.parent, text)
                assertNull(outer.primaryConstructor, text)
                assertTrue(outer.primaryConstructors.isEmpty(), text)
            } else {
                kotlin.test.assertSame(ownerBody, constructor.parent, text)
                kotlin.test.assertSame(constructor, outer.primaryConstructor, text)
                assertEquals(listOf(constructor), outer.primaryConstructors, text)
            }
            assertEquals(listOf("after"), ownerBody.declarations.filterIsInstance<CjNamedFunction>().map { it.name }, text)
            assertEquals(listOf("I", "Outer", "Following"), file.declarations.map { (it as CjNamedDeclaration).name }, text)

            val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
            val tree = parseLightWithPsiInput(file, builder).file.tree
            val root = snapshot(tree.root, tree)
            val lightConstructor = root.descendants().single { it.type == CjNodeTypes.PRIMARY_CONSTRUCTOR }
            assertEquals(snapshot(constructor.node), lightConstructor, text)
            for ((child, source) in listOf(
                parameterList to "<T>",
                valueParameterList to "()",
                constraintList to "T <: I",
                constructorBody to "{}",
            )) {
                val start = text.indexOf(source, text.indexOf(declaration))
                assertEquals(start to start + source.length, child.textRange.startOffset to child.textRange.endOffset, text)
                assertEquals(snapshot(child.node), lightConstructor.children.single { it.type == child.node.elementType }, text)
            }
            assertTrue(root.descendants().none { it.type == TokenType.ERROR_ELEMENT }, text)
        }
    }

    /** authority-nested-complete-signature仅报scope，延迟marker不能改变真实签名节点所属。 */
    @Test
    fun completeNestedSignatureKeepsDirectTypeParameterInheritanceAndConstraintNodes() {
        val text = """
            package remainingparsing

            class Outer {
                class Inner<T> <: I where T <: I {}
                func after(): Unit {}
            }

            interface I {}
            class Following {}
        """.trimIndent()
        val file = parseBoth("nestedCompleteSignature", text)
        val diagnostic = file.collectParsingDiagnostics().single()
        val keywordOffset = text.indexOf("class Inner")
        assertEquals(keywordOffset, diagnostic.startOffset)
        assertEquals(keywordOffset + 5, diagnostic.endOffset)
        val outer = file.declarations.filterIsInstance<CjClass>().first()
        val inner = checkNotNull(outer.body).declarations.filterIsInstance<CjClass>().single()
        val parameterList = kotlin.test.assertNotNull(inner.typeParameterList)
        val superTypeList = kotlin.test.assertNotNull(inner.getSuperTypeList())
        val constraintList = kotlin.test.assertNotNull(inner.typeConstraintList)
        kotlin.test.assertSame(inner, parameterList.parent)
        kotlin.test.assertSame(inner, superTypeList.parent)
        kotlin.test.assertSame(inner, constraintList.parent)
        assertEquals(listOf("T"), inner.typeParameters.map { it.name })
        val inheritedType = kotlin.test.assertNotNull(inner.superTypeListEntries.single().typeReference)
        kotlin.test.assertEquals("I", inheritedType.text)
        assertEquals(text.indexOf("I where"), inheritedType.textRange.startOffset)
        assertEquals(text.indexOf("I where") + 1, inheritedType.textRange.endOffset)
        val constraint = inner.typeConstraints.single()
        kotlin.test.assertEquals("T", constraint.subjectTypeParameterName?.text)
        kotlin.test.assertEquals("I", constraint.boundTypeReference?.text)
        assertEquals(text.indexOf("T <: I"), constraint.textRange.startOffset)
        assertEquals(text.indexOf("T <: I") + "T <: I".length, constraint.textRange.endOffset)
        kotlin.test.assertEquals("<T>", parameterList.text)
        assertEquals(text.indexOf("<T>"), parameterList.textRange.startOffset)
        assertEquals(text.indexOf("<T>") + 3, parameterList.textRange.endOffset)
        kotlin.test.assertEquals(listOf("Inner", "after"), outer.body?.declarations?.map { (it as? CjNamedDeclaration)?.name })
        kotlin.test.assertEquals(listOf("Outer", "I", "Following"), file.declarations.map { (it as? CjNamedDeclaration)?.name })

        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val tree = parseLightWithPsiInput(file, builder).file.tree
        val root = snapshot(tree.root, tree)
        val lightOuter = root.children.first { it.type == CjNodeTypes.CLASS }
        val lightBody = lightOuter.children.single { it.type == CjNodeTypes.CLASS_BODY }
        val lightInner = lightBody.children.single { it.type == CjNodeTypes.CLASS }
        for (signatureNode in listOf(parameterList.node, superTypeList.node, constraintList.node)) {
            assertEquals(snapshot(signatureNode), lightInner.children.single { it.type == signatureNode.elementType })
        }
        assertEquals(keywordOffset, lightInner.start)
        assertEquals(text.indexOf("{}", keywordOffset) + 2, lightInner.end)
        val errors = lightInner.descendants().filter { it.type == TokenType.ERROR_ELEMENT }.toList()
        assertEquals(listOf(keywordOffset to keywordOffset + 5), errors.map { it.start to it.end })
    }

    /** 真实builder双入口完成marker；shouldReportError必须用具名参数，首个布尔参数不是它。 */
    @Test
    fun disabledScopeReportingBalancesMarkersAndPreservesBothDeclarationsInBothTrees() {
        val text = "class First<T> {} class Second {}"
        for (shouldReportError in listOf(false, true)) {
            val representations = listOf(false, true).map { light ->
                val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
                val parser = CangJieParsing.createForTopLevelNonLazy(SemanticWhitespaceAwarePsiBuilderImpl(builder))
                val root = builder.mark()
                with(ParsingContext(shouldReportError = shouldReportError)) {
                    repeat(2) {
                        val declaration = builder.mark()
                        declaration.done(parser.parseClass(
                            CangJieParsing.ModifierDetector(),
                            enclosingDeclarationKeyword = CjTokens.CLASS_KEYWORD,
                        ))
                    }
                }
                assertTrue(builder.eof())
                root.done(CjNodeTypes.CJ_FILE)
                if (light) {
                    val tree = builder.lightTree
                    snapshot(tree.root, tree)
                } else {
                    snapshot(builder.treeBuilt)
                }
            }
            assertEquals(representations.first(), representations.last())
            val syntax = representations.first()
            val classes = syntax.children.filter { it.type == CjNodeTypes.CLASS }
            assertEquals(2, classes.size)
            assertEquals(0 to text.indexOf(" class Second"), classes[0].start to classes[0].end)
            assertEquals(text.indexOf("class Second") to text.length, classes[1].start to classes[1].end)
            val parameterList = classes[0].children.single { it.type == CjNodeTypes.TYPE_PARAMETER_LIST }
            assertEquals(text.indexOf("<T>") to text.indexOf("<T>") + 3, parameterList.start to parameterList.end)
            assertEquals(1, classes[0].children.count { it.type == CjNodeTypes.CLASS_BODY })
            assertEquals(1, classes[1].children.count { it.type == CjNodeTypes.CLASS_BODY })
            val errors = syntax.descendants().filter { it.type == TokenType.ERROR_ELEMENT }.toList()
            val expectedRanges = if (shouldReportError) {
                listOf(0 to 5, text.indexOf("class Second") to text.indexOf("class Second") + 5)
            } else emptyList()
            assertEquals(expectedRanges, errors.map { it.start to it.end })
        }
    }

    /** 官方返回类型错误只覆盖真实type；冒号、约束、函数体仍属于原声明。 */
    @Test
    fun constructorAndFinalizerReturnTypesKeepOnlyTheTypeInsideTheDiagnosticWrapper() {
        val cases = listOf(
            "Outer(): Array<Int64> { let body = 1 }" to "primary.constructor",
            "init(): Array<Int64> { let body = 1 }" to "constructor",
            "@Unknown Outer(): Array<Int64> { let body = 1 }" to "primary.constructor",
            "@Unknown init(): Array<Int64> { let body = 1 }" to "constructor",
            "Outer<T>(): Array<Int64> where T <: I { let body = 1 }" to "primary.constructor",
            "~init(): Array<Int64> { let body = 1 }" to "finalizer",
            "~init<T>(): Array<Int64> where T <: I { let body = 1 }" to "finalizer",
        )
        for ((index, case) in cases.withIndex()) {
            val (declaration, role) = case
            val text = constructorRecoverySource(declaration)
            val file = parseBoth("constructorReturnType$index", text)
            val diagnostic = file.collectParsingDiagnostics().single()
            kotlin.test.assertEquals(
                CangJieParsingBundle.message("parsing.error.$role.return.type"),
                diagnostic.message,
                text,
            )
            val typeStart = text.indexOf("Array<Int64>")
            assertEquals(typeStart to typeStart + "Array<Int64>".length,
                diagnostic.startOffset to diagnostic.endOffset, text)
            assertEquals(ParsingDiagnostic.Origin.INPUT, diagnostic.origin, text)
            val function = assertRecoveredConstructorFunction(file, declaration, text)
            assertNull(function.typeReference, text)
            assertTrue(!function.hasDeclaredReturnType(), text)
            val error = PsiTreeUtil.findChildrenOfType(function, com.intellij.psi.PsiErrorElement::class.java).single()
            kotlin.test.assertSame(function, error.parent, text)
            val type = error.children.filterIsInstance<CjTypeReference>().single()
            kotlin.test.assertEquals("Array<Int64>", type.text, text)
            val typeArgument = PsiTreeUtil.findChildrenOfType(type, CjTypeReference::class.java).single()
            kotlin.test.assertEquals("Int64", typeArgument.text, text)
            kotlin.test.assertEquals(type.text, error.text, text)
            assertEquals(typeStart to typeStart + type.textLength,
                type.textRange.startOffset to type.textRange.endOffset, text)
            assertEquals(type.textRange, error.textRange, text)
            val colon = kotlin.test.assertNotNull(function.node.findChildByType(CjTokens.COLON))
            kotlin.test.assertEquals(":", colon.text, text)
            assertEquals(typeStart - 2, colon.startOffset, text)
            assertTrue(error.children.none { it is CjBlockExpression || it is CjTypeConstraintList }, text)
            assertRecoveredOwnConstraints(function, declaration.contains("<T>"), text)
        }
    }

    /** 官方comment探针：两侧注释不能被附着进非法返回类型的ERROR或TYPE_REFERENCE。 */
    @Test
    fun constructorReturnTypeDiagnosticsExcludeBothAdjacentCommentsInBothTrees() {
        val cases = listOf(
            "class Outer { Outer(): /*a*/ Unit /*b*/ { let body=1 }; func after():Unit{} }" to "primary.constructor",
            "class Outer { init(): /*a*/ Unit /*b*/ { let body=1 }; func after():Unit{} }" to "constructor",
        )
        for ((index, case) in cases.withIndex()) {
            val (text, role) = case
            val file = parseBoth("constructorReturnComments$index", text)
            val diagnostic = file.collectParsingDiagnostics().single()
            kotlin.test.assertEquals(CangJieParsingBundle.message("parsing.error.$role.return.type"), diagnostic.message, text)
            assertEquals(ParsingDiagnostic.Origin.INPUT, diagnostic.origin, text)
            val start = text.indexOf("Unit")
            assertEquals(start to start + 4, diagnostic.startOffset to diagnostic.endOffset, text)
            val owner = file.declarations.filterIsInstance<CjClass>().single()
            val ownerBody = kotlin.test.assertNotNull(owner.body)
            assertEquals(2, ownerBody.declarations.size, text)
            val constructor = ownerBody.declarations.filterIsInstance<CjConstructor<*>>().single()
            assertEquals(index == 0, constructor is CjPrimaryConstructor, text)
            assertEquals(index == 1, constructor is CjSecondaryConstructor, text)
            kotlin.test.assertSame(ownerBody, constructor.parent, text)
            assertNull(constructor.typeReference, text)
            assertTrue(!constructor.hasDeclaredReturnType(), text)
            val error = PsiTreeUtil.findChildrenOfType(constructor, com.intellij.psi.PsiErrorElement::class.java).single()
            val type = error.children.filterIsInstance<CjTypeReference>().single()
            kotlin.test.assertSame(constructor, error.parent, text)
            kotlin.test.assertEquals("Unit", error.text, text)
            kotlin.test.assertEquals("Unit", type.text, text)
            assertEquals(start to start + 4, error.textRange.startOffset to error.textRange.endOffset, text)
            assertEquals(error.textRange, type.textRange, text)
            val colon = kotlin.test.assertNotNull(constructor.colon)
            kotlin.test.assertSame(constructor, colon.parent, text)
            for (commentSource in listOf("/*a*/", "/*b*/")) {
                val comment = kotlin.test.assertNotNull(file.findElementAt(text.indexOf(commentSource)))
                kotlin.test.assertEquals(commentSource, comment.text, text)
                assertTrue(!PsiTreeUtil.isAncestor(error, comment, false), text)
                assertTrue(!PsiTreeUtil.isAncestor(type, comment, false), text)
            }
            val body = kotlin.test.assertNotNull(constructor.bodyExpression)
            kotlin.test.assertSame(constructor, body.parent, text)
            kotlin.test.assertEquals("{ let body=1 }", body.text, text)
            val variable = PsiTreeUtil.findChildrenOfType(body, CjPatternVariable::class.java).single()
            kotlin.test.assertEquals("body", (variable.pattern as CjBindingPattern).name, text)
            val after = ownerBody.declarations.filterIsInstance<CjNamedFunction>().single()
            kotlin.test.assertSame(ownerBody, after.parent, text)
            kotlin.test.assertEquals("after", after.name, text)
            kotlin.test.assertEquals("func after():Unit{}", after.text, text)
            kotlin.test.assertEquals("Unit", kotlin.test.assertNotNull(after.typeReference).text, text)

            val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
            val tree = parseLightWithPsiInput(file, builder).file.tree
            val root = snapshot(tree.root, tree)
            val heavy = snapshot(constructor.node)
            val light = root.descendants().single { it.type == heavy.type && it.start == heavy.start }
            assertEquals(heavy, light, text)
            val lightError = light.descendants().single { it.type == TokenType.ERROR_ELEMENT }
            assertEquals(start to start + 4, lightError.start to lightError.end, text)
            val lightType = lightError.children.single { it.type == CjNodeTypes.TYPE_REFERENCE }
            assertEquals(start to start + 4, lightType.start to lightType.end, text)
        }
    }

    /** authority-constructor-return-secondary-missing：空type不能触发额外invalid-return诊断。 */
    @Test
    fun missingConstructorReturnTypeKeepsTheBodyWithoutInventingADeclaredResultType() {
        val declaration = "init(): { let body = 1 }"
        val text = constructorRecoverySource(declaration)
        val file = parseBoth("missingConstructorReturnType", text)
        val diagnostic = file.collectParsingDiagnostics().single()
        assertEquals(ParsingDiagnostic.Origin.INPUT, diagnostic.origin, text)
        kotlin.test.assertEquals(
            CangJieParsingBundle.message("parsing.error.expecting.found", "type name", "{"),
            diagnostic.message,
            text,
        )
        assertTrue(listOf("primary.constructor", "constructor", "finalizer").none {
            diagnostic.message == CangJieParsingBundle.message("parsing.error.$it.return.type")
        }, text)
        val function = assertRecoveredConstructorFunction(file, declaration, text)
        assertNull(function.typeReference, text)
        assertTrue(!function.hasDeclaredReturnType(), text)
        val type = function.children.filterIsInstance<CjTypeReference>().single()
        assertEquals(0, type.textLength, text)
        val error = PsiTreeUtil.findChildrenOfType(function, com.intellij.psi.PsiErrorElement::class.java).single()
        assertTrue(error.text.isEmpty(), text)
        assertTrue(error.textRange.endOffset <= kotlin.test.assertNotNull(function.bodyExpression).textRange.startOffset, text)
        assertRecoveredOwnConstraints(function, false, text)
    }

    /** 独立七份破损泛型probe：缺GT/LPAREN不能消费WHERE或真正的body起点。 */
    @Test
    fun brokenOwnGenericConstructorAndFinalizerHeadersRecoverWithoutConsumingConstraintsOrBodies() {
        val cases = listOf(
            "Outer<T { let body = 1 }" to 2,
            "init<T { let body = 1 }" to 2,
            "Outer<T where T <: I { let body = 1 }" to 2,
            "init<T where T <: I { let body = 1 }" to 2,
            "Outer<T> { let body = 1 }" to 1,
            "init<T> where T <: I { let body = 1 }" to 1,
            "~init<T where T <: I { let body = 1 }" to 2,
        )
        for ((index, case) in cases.withIndex()) {
            val (declaration, errorCount) = case
            val text = constructorRecoverySource(declaration)
            val file = parseBoth("brokenConstructorOwnGeneric$index", text)
            val diagnostics = file.collectParsingDiagnostics()
            assertEquals(errorCount, diagnostics.size, "$text\n$diagnostics")
            assertTrue(diagnostics.all { it.origin == ParsingDiagnostic.Origin.INPUT }, text)
            val function = assertRecoveredConstructorFunction(file, declaration, text)
            assertEquals(listOf("T"), function.typeParameters.map { it.name }, text)
            val parameters = kotlin.test.assertNotNull(function.typeParameterList)
            kotlin.test.assertSame(function, parameters.parent, text)
            assertTrue(!parameters.text.contains("where") && !parameters.text.contains("{"), text)
            assertRecoveredOwnConstraints(function, declaration.contains("where"), text)
            // 插入点遵守installed IntelliJ契约，完整Heavy/Light节点与diagnostic相等已严格验证。
            val errors = PsiTreeUtil.findChildrenOfType(function, com.intellij.psi.PsiErrorElement::class.java)
            assertEquals(errorCount, errors.size, text)
            assertTrue(errors.all { it.text.isEmpty() }, text)
        }
    }

    /** FINALIZER拥有独立语义禁泛型规则，parser应保留自身泛型或只消费无泛型WHERE。 */
    @Test
    fun finalizerOwnGenericAndNonGenericWhereKeepTheirActualSignatureAndFollowingMember() {
        val declarations = listOf(
            "~init<T>() where T <: I { let body = 1 }",
            "~init() where { let body = 1 }",
        )
        for ((index, declaration) in declarations.withIndex()) {
            val text = constructorRecoverySource(declaration)
            val file = parseBoth("finalizerOwnSignature$index", text)
            val hasOwnGeneric = index == 0
            val diagnostics = file.collectParsingDiagnostics()
            assertEquals(if (hasOwnGeneric) 0 else 1, diagnostics.size, text)
            if (!hasOwnGeneric) {
                val error = diagnostics.single()
                kotlin.test.assertEquals(CangJieParsingBundle.message("parsing.error.unexpected.where"), error.message, text)
                assertEquals(text.indexOf("where") to text.indexOf("where") + 5,
                    error.startOffset to error.endOffset, text)
                assertEquals(ParsingDiagnostic.Origin.INPUT, error.origin, text)
            }
            val function = assertRecoveredConstructorFunction(file, declaration, text)
            assertTrue(function is CjFinalizer, text)
            assertRecoveredOwnConstraints(function, hasOwnGeneric, text)
            assertNull(function.typeReference, text)
            assertTrue(!function.hasDeclaredReturnType(), text)
        }
    }

    /** 与authority probe保持完全相同的声明、body变量和after布局。 */
    private fun constructorRecoverySource(declaration: String): String {
        val header = if (declaration.contains("where T <: I")) "interface I {}\n" else ""
        return header + """
            class Outer {
                $declaration
                func after() {}
            }
        """.trimIndent()
    }

    /** 原声明及宏input完整保留；只比较真实节点，不用whole-declaration错误包装替代它。 */
    private fun assertRecoveredConstructorFunction(file: CjFile, declaration: String, text: String): CjFunction {
        val owner = file.declarations.filterIsInstance<CjClass>().single()
        val ownerBody = kotlin.test.assertNotNull(owner.body)
        val function = if (declaration.startsWith("@")) {
            val macro = ownerBody.children.filterIsInstance<CjMacroExpression>().single()
            kotlin.test.assertEquals(declaration, macro.text, text)
            kotlin.test.assertSame(ownerBody, macro.parent, text)
            val inputFunction = macro.unwrappedDeclaration as CjFunction
            kotlin.test.assertSame(macro.input, inputFunction.parent, text)
            assertTrue(owner.primaryConstructors.isEmpty(), text)
            assertTrue(owner.secondaryConstructors.isEmpty(), text)
            assertEquals(1, ownerBody.declarations.size, text)
            inputFunction
        } else {
            assertEquals(2, ownerBody.declarations.size, text)
            val directFunction = if (declaration.startsWith("~init")) {
                ownerBody.declarations.filterIsInstance<CjFinalizer>().single()
            } else {
                ownerBody.declarations.first() as CjFunction
            }
            kotlin.test.assertSame(ownerBody, directFunction.parent, text)
            directFunction
        }
        val signature = declaration.removePrefix("@Unknown ")
        when {
            signature.startsWith("Outer") -> assertTrue(function is CjPrimaryConstructor, text)
            signature.startsWith("init") -> assertTrue(function is CjSecondaryConstructor, text)
            signature.startsWith("~init") -> assertTrue(function is CjFinalizer, text)
        }
        kotlin.test.assertEquals(signature, function.text, text)
        val body = kotlin.test.assertNotNull(function.bodyExpression)
        kotlin.test.assertSame(function, body.parent, text)
        kotlin.test.assertEquals("{ let body = 1 }", body.text, text)
        val variable = PsiTreeUtil.findChildrenOfType(body, CjPatternVariable::class.java).single()
        kotlin.test.assertEquals("let body = 1", variable.text, text)
        kotlin.test.assertEquals("body", (variable.pattern as CjBindingPattern).name, text)
        assertEquals(listOf("after"), ownerBody.declarations.filterIsInstance<CjNamedFunction>().map { it.name }, text)
        val after = ownerBody.declarations.filterIsInstance<CjNamedFunction>().single()
        kotlin.test.assertSame(ownerBody, after.parent, text)
        kotlin.test.assertEquals("func after() {}", after.text, text)

        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val tree = parseLightWithPsiInput(file, builder).file.tree
        val root = snapshot(tree.root, tree)
        val heavy = snapshot(function.node)
        val light = root.descendants().single { it.type == heavy.type && it.start == heavy.start }
        assertEquals(heavy, light, text)
        return function
    }

    /** 参数与WHERE只属于当前函数；约束列表与Kotlin一样从首个T开始。 */
    private fun assertRecoveredOwnConstraints(function: CjFunction, hasConstraints: Boolean, text: String) {
        if (hasConstraints) {
            assertEquals(listOf("T"), function.typeParameters.map { it.name }, text)
            val list = kotlin.test.assertNotNull(function.typeConstraintList)
            kotlin.test.assertSame(function, list.parent, text)
            kotlin.test.assertEquals("T <: I", list.text, text)
            val constraint = function.typeConstraints.single()
            kotlin.test.assertEquals("T", constraint.subjectTypeParameterName?.text, text)
            kotlin.test.assertEquals("I", constraint.boundTypeReference?.text, text)
            val where = kotlin.test.assertNotNull(function.node.findChildByType(CjTokens.WHERE_KEYWORD))
            assertEquals(text.indexOf("where"), where.startOffset, text)
        } else {
            assertNull(function.typeConstraintList, text)
            assertTrue(function.typeConstraints.isEmpty(), text)
        }
    }

    /** 固化真实树的节点类型、绝对范围与直接子序列，不重造parser或PSI。 */
    private data class SyntaxNode(
        val type: IElementType,
        val start: Int,
        val end: Int,
        val children: List<SyntaxNode>,
    ) {
        fun descendants(): Sequence<SyntaxNode> = sequence {
            yield(this@SyntaxNode)
            for (child in children) yieldAll(child.descendants())
        }
    }

    private fun snapshot(node: ASTNode): SyntaxNode = SyntaxNode(
        node.elementType, node.startOffset, node.startOffset + node.textLength,
        generateSequence(node.firstChildNode) { it.treeNext }.map(::snapshot).toList(),
    )

    private fun snapshot(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): SyntaxNode {
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get()
        val snapshots = if (children == null) emptyList() else try {
            (0 until count).map { snapshot(checkNotNull(children[it]), tree) }
        } finally {
            tree.disposeChildren(children, count)
        }
        // typed LT 错误正式构造成 AST ERROR_ELEMENT；只映射该类型，保留范围与全部直接子结构。
        val type = if (node.tokenType is CjParsingErrorElementType) TokenType.ERROR_ELEMENT else node.tokenType
        return SyntaxNode(type, node.startOffset, node.endOffset, snapshots)
    }

    /** 与EnumConstructorAnnotationParsingTest相同的括号token输入，不向宿主提升其中的声明。 */
    @Test
    fun parenthesizedMacroInputPrimaryConstructorDoesNotBecomeTheOwnerConstructor() {
        val text = """
            package macroconstructor
            class Owner {
                @Outer(@Inner class Input { Input() {} })
                func after(): Int64 { 1 }
            }
        """.trimIndent()
        val file = parseBoth("macroInputPrimaryConstructor", text)
        assertTrue(file.collectParsingDiagnostics().isEmpty(), text)
        val owner = file.declarations.filterIsInstance<CjClass>().single()
        assertTrue(PsiTreeUtil.findChildrenOfType(owner, CjClass::class.java).isEmpty())
        val macro = PsiTreeUtil.findChildrenOfType(owner, CjMacroExpression::class.java).single()
        assertEquals("(@Inner class Input { Input() {} })", macro.input?.text)
        assertTrue(PsiTreeUtil.findChildrenOfType(macro, CjMacroExpression::class.java).isEmpty())
        assertNull(owner.primaryConstructor)
        assertTrue(owner.primaryConstructors.isEmpty())
        assertTrue(owner.secondaryConstructors.isEmpty())
        assertTrue(owner.body?.declarations?.none { it is CjClass } == true)
        assertEquals(listOf("after"), owner.body?.declarations?.filterIsInstance<CjNamedFunction>()?.map { it.name })
    }

    /** authority-function-local-primary：Local()可解析，非法的是function body内class的scope。 */
    @Test
    fun functionBodyPrimaryConstructorDoesNotBecomeTheOwnerConstructor() {
        val text = """
            package remainingparsing

            class Outer {
                func helper() {
                    class Local {
                        Local() {}
                    }
                }
            }
        """.trimIndent()
        val file = parseBoth("functionLocalPrimaryConstructor", text)
        val offset = text.indexOf("class Local")
        val diagnostic = file.collectParsingDiagnostics().single()
        assertEquals(offset, diagnostic.startOffset)
        assertEquals(offset + "class".length, diagnostic.endOffset)
        assertEquals(ParsingDiagnostic.Origin.INPUT, diagnostic.origin)
        assertEquals(
            CangJieParsingBundle.message("parsing.error.invalid.declaration.in.class", "class",
                CangJieParsingBundle.message("parsing.scope.func")),
            diagnostic.message,
        )
        val outer = file.declarations.filterIsInstance<CjClass>().single()
        val local = PsiTreeUtil.findChildrenOfType(outer, CjClass::class.java).single()
        assertEquals("Local", local.name)
        assertNotNull(local.primaryConstructor)
        assertEquals(1, local.primaryConstructors.size)
        assertNull(outer.primaryConstructor)
        assertTrue(outer.primaryConstructors.isEmpty())
        assertTrue(outer.secondaryConstructors.isEmpty())
        assertEquals(listOf("helper"), outer.body?.declarations?.map { (it as? CjNamedDeclaration)?.name })
    }

    /** 比较完整diagnostic，包括message与origin，而非仅比较节点数量或起点。 */
    private fun parseBoth(name: String, text: String): CjFile {
        val file = createPsiFile(name, text) as CjFile
        val heavyDiagnostics = file.collectParsingDiagnostics()
        val lightDiagnostics = mutableListOf<ParsingDiagnostic>()
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        lightDiagnostics += parseLightWithPsiInput(file, builder).diagnostics
        assertPublishedParsingDiagnosticParity(heavyDiagnostics, lightDiagnostics,
            checkNotNull(publishedParserSeed(file).sourceContext), text)
        return file
    }
}
