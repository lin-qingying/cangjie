package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.mock.MockProject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.BlockSupportImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleModifiers
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.psi.CjBlockCodeFragment
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjExpressionCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroCodeFragment
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjTypeCodeFragment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import java.awt.EventQueue
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertFailsWith as fails
import kotlin.test.assertFalse as falseValue
import kotlin.test.assertNotNull as present
import kotlin.test.assertNotSame as distinct
import kotlin.test.assertNull as absent
import kotlin.test.assertSame as identical
import kotlin.test.assertTrue as truth

/**
 * 目标编译必须重新产生真实 PSI、属性和后继入口，源码对应不能代替目标文法。
 * operator 对照取自 review-20261108/target-context-structure/cjc-results.json（SDK 1.1.3）；
 * 局部角色、声明文件缺体、FOREIGN varargs 和显式 setter 取自官方 v1.1.3 Parse 实现。
 * 片段使用 SDK 的原生入口；不为独立片段编造 class、func 或其他宿主。
 */
class CjPsiCompilationParsingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    /** 非物理编译产品的行列来自冻结词法文本；CRLF 与补充平面字符都保留编辑器 UTF-16 坐标。 */
    @Test
    fun sourceLineMappingRetainsCrLfAndUtf16Offsets() {
        val source = sourceFile("package example\r\n\r\nfunc value(): Int64 {\r\n    /*🙂*/ 1\r\n}\r\n")
        val product = CangJieParser.parseCompilationFile(source, specificMode)
        truth(!product.file.isPhysical)
        val mapping = product.sourceFileLinesMapping
        equal(59, mapping.lastOffset)
        equal(6, mapping.linesCount)
        equal(listOf(0, 17, 19, 42, 56, 59), (0 until mapping.linesCount).map(mapping::getLineStartOffset))
        equal(0 to 16, mapping.getLineAndColumnByOffset(16))
        equal(1 to 0, mapping.getLineAndColumnByOffset(17))
        equal(3 to 6, mapping.getLineAndColumnByOffset(48))
        equal(3 to 11, mapping.getLineAndColumnByOffset(53))
        equal(5 to 0, mapping.getLineAndColumnByOffset(59))
        product.requireConsistentInput()
    }

    @Test
    fun syntaxOnlySourceProducesIndependentCommonAndSpecificAttributes() {
        val source = sourceFile("specific public func item(): Unit {}")
        val original = functions(source).single()
        val originalShape = grammar(source.node)
        val originalModifiers = modifiers(original)
        val originalSeed = actualSeed(source)
        absent(originalSeed.diagnosticScope)
        val originalScope = present(source.parsingDiagnosticScopeOrNull())
        val originalFacts = source.parsedFileFacts
        val originalDiagnostics = source.collectParsingDiagnostics()
        val common = CangJieParser.parseCompilationFile(source, commonMode)
        val specific = CangJieParser.parseCompilationFile(source, specificMode)

        for (product in listOf(common, specific)) {
            identical(source, product.originFile)
            distinct(source, product.file)
            distinct(source.node, product.file.node)
            equal(source.text, product.file.text)
            identical(product.actualInputSeed, actualSeed(product.file))
            absent(product.actualInputSeed.diagnosticScope)
            equal(emptyList<Long>(), product.diagnosticScope.path)
            truth(originalScope.rootIdentity != product.diagnosticScope.rootIdentity)
            identical(product.diagnosticScope, product.file.parsingDiagnosticScopeOrNull())
            identical(originalSeed.lexicalInput, product.actualInputSeed.lexicalInput)
            equal(originalSeed.sourceContext, product.actualInputSeed.sourceContext)
            product.withTargetFile { identical(product.file, it) }
        }
        val commonFunction = functions(common.file).single()
        val specificFunction = functions(specific.file).single()
        equal(emptySet(), modifiers(commonFunction).attributes)
        equal(setOf(CjModifierKind.SPECIFIC, CjModifierKind.PUBLIC), modifiers(specificFunction).attributes)
        distinct(originalModifiers, modifiers(specificFunction))
        distinct(common.file.node, specific.file.node)
        truth(common.diagnosticScope.rootIdentity != specific.diagnosticScope.rootIdentity)
        truth(common.diagnostics.any { it.message == specificInNonSpecific })
        truth(specific.diagnostics.isEmpty(), specific.diagnostics.toString())
        equal(commonMode, common.file.cjmpParsingContext)
        equal(specificMode, specific.file.cjmpParsingContext)
        equal(originalShape, grammar(source.node))
        identical(original, functions(source).single())
        identical(originalModifiers, modifiers(original))
        identical(originalSeed, actualSeed(source))
        identical(CjmpParsingContext.SyntaxOnly, source.cjmpParsingContext)
        equal(originalFacts, source.parsedFileFacts)
        equal(originalDiagnostics, source.collectParsingDiagnostics())
    }

    @Test
    fun rejectedOperatorChangesTheSignatureAndCannotCorrespondToTheOriginalFunction() {
        val source = sourceFile("class Box { specific operator func +(rhs: Box): Box { this } }")
        val original = functions(source).single()
        val originalShape = grammar(original.node)
        truth(original.node.findChildByType(CjNodeTypes.OPERATION_NAME) != null)
        val common = CangJieParser.parseCompilationFile(source, commonMode)
        val specific = CangJieParser.parseCompilationFile(source, specificMode)
        val rejected = functions(common.file).single()
        val accepted = functions(specific.file).single()

        absent(rejected.node.findChildByType(CjNodeTypes.OPERATION_NAME))
        present(accepted.node.findChildByType(CjNodeTypes.OPERATION_NAME))
        equal(emptySet(), modifiers(rejected).attributes)
        truth(modifiers(accepted).hasModifier(CjModifierKind.OPERATOR))
        truth(grammar(rejected.node) != grammar(accepted.node))
        truth(common.diagnostics.any { it.message == specificInNonSpecific })
        truth(common.diagnostics.any { it.message == "expected a func name after keyword 'func', found '+'" }, common.diagnostics.toString())
        absent(common.correspondingElementOrNull(original))
        absent(common.originalElementOrNull(rejected))
        identical(accepted, specific.correspondingElementOrNull(original),
            "origin=${grammar(source.node)}\ntarget=${grammar(specific.file.node)}\n" +
                "originBody=${original.bodyBlockExpression?.node?.parserSeedOrNull()?.context}\n" +
                "targetBody=${accepted.bodyBlockExpression?.node?.parserSeedOrNull()?.context}")
        identical(original, specific.originalElementOrNull(accepted))
        equal(originalShape, grammar(original.node))
    }

    @Test
    fun aBrokenFunctionKeepsItsInvalidParameterOwnerAndTheEnclosingBrace() {
        val text = "class Owner { func }\nfunc following(): Unit {}"
        val source = sourceFile(text)
        val original = functions(source).single { it.name == null }
        val keyword = text.indexOf("func")
        val brace = text.indexOf('}')
        val expectedName = CangJieParsingBundle.message(
            "parsing.error.expected.declaration.name", "func", "func", "}",
        )
        val expectedParameters = CangJieParsingBundle.message("parsing.error.function.parameter.list", "}")
        for (file in listOf(source, CangJieParser.parseCompilationFile(source, commonMode).file,
                CangJieParser.parseCompilationFile(source, specificMode).file)) {
            val function = functions(file).single { it.name == null }
            val list = function.valueParameterLists.single()
            truth(list.parameters.isEmpty())
            identical(function, list.parent)
            absent(function.bodyExpression)
            truth(functions(file).any { it.name == "following" })
            val diagnostics = file.collectParsingDiagnostics()
            val name = diagnostics.single { it.message == expectedName }
            equal(brace, name.startOffset)
            equal(brace + 1, name.endOffset)
            truth(name.hints.any { it.startOffset == keyword && it.endOffset == keyword + 4 })
            // 原生发布键忽略消息：同一范围的后续缺列表报告保留在 AST，不重复发布。
            falseValue(diagnostics.any { it.message == expectedParameters })
            truth(PsiTreeUtil.findChildrenOfType(list, com.intellij.psi.PsiErrorElement::class.java)
                .any { it.errorDescription == expectedParameters })
            falseValue(diagnostics.any { it.message == CangJieParsingBundle.message("parsing.error.function.body.expected") })
        }
        equal(listOf(original), functions(source).filter { it.name == null })
    }

    @Test
    fun constructorFamiliesPublishTheirOwnNativeMissingBodyPoints() {
        val cases = listOf("init" to "parsing.body.owner.constructor",
            "~init" to "parsing.body.owner.finalizer", "Owner()" to "parsing.body.owner.primary.constructor")
        for ((member, label) in cases) {
            val text = "class Owner { $member }"
            val source = sourceFile(text)
            val point = text.indexOf(member) + member.length
            for (file in listOf(source, CangJieParser.parseCompilationFile(source, commonMode).file)) {
                val owner = CangJieParsingBundle.message(label)
                val name = if (member == "Owner()") " 'Owner'" else ""
                val message = CangJieParsingBundle.message("parsing.error.body.missing", owner, name)
                val diagnostic = file.collectParsingDiagnostics().single { it.message == message }
                equal(point, diagnostic.startOffset)
                equal(point, diagnostic.endOffset)
                equal(org.cangnova.cangjie.messages.ParsingDiagnostic.RangeKind.POINT, diagnostic.rangeKind)
                val range = present(diagnostic.mainRange)
                equal(1 to point + 1, range.begin)
                equal(1 to point + 2, range.end)
                truth(diagnostic.nativeHints.any { it.range == range })
            }
        }
    }

    @Test
    fun repeatedNamesHaveUniqueBidirectionalOwnersAndRejectOtherFilesAndClones() {
        val text = """
            class Left {
                func same(value: Int64): Unit {}
                func same(value: Bool): Unit {}
            }
            class Right {
                func same(value: Int64): Unit {}
                func same(value: Bool): Unit {}
            }
        """.trimIndent()
        val source = sourceFile(text)
        val originals = functions(source)
        val product = CangJieParser.parseCompilationFile(source, specificMode)
        val targets = functions(product.file)
        equal(4, originals.size)
        equal(4, targets.size)
        originals.zip(targets).forEach { (origin, target) ->
            identical(target, product.correspondingElementOrNull(origin))
            identical(origin, product.originalElementOrNull(target))
            distinct(origin, target)
        }
        val another = sourceFile(text)
        val sourceClone = source.copy() as CjFile
        val targetClone = product.file.copy() as CjFile
        equal(grammar(source.node), grammar(sourceClone.node))
        equal(modifiers(originals.first()), modifiers(functions(sourceClone).first()))
        for (foreign in functions(another) + functions(sourceClone) + targets) {
            absent(product.correspondingElementOrNull(foreign))
        }
        for (foreign in functions(targetClone) + functions(another) + originals) {
            absent(product.originalElementOrNull(foreign))
        }
        identical(targets.first(), product.correspondingElementOrNull(originals.first()))
    }

    @Test
    fun declarationSourceRetainsItsNativeMissingBodySemantics() {
        // 官方 CheckFuncBody 的 parseDeclFile 分支接受完整的无体函数签名。
        val source = CjPsiFactory.forMacroExpansion(
            project, markGenerated = false, sourceKind = CjSourceKind.DECLARATION,
        ).createFile("func item(): Unit")
        val original = functions(source).single()
        equal(CjSourceKind.DECLARATION, actualSeed(source).sourceKind)
        val product = CangJieParser.parseCompilationFile(source, specificMode)
        equal(CjSourceKind.DECLARATION, product.inputSeed.sourceKind)
        equal(CjSourceKind.DECLARATION, product.actualInputSeed.sourceKind)
        equal(CjSourceKind.DECLARATION, present(product.file.parsedFileFacts).sourceKind)
        val target = functions(product.file).single()
        absent(target.bodyExpression)
        truth(product.diagnostics.isEmpty(), product.diagnostics.toString())
        identical(target, product.correspondingElementOrNull(original))
        identical(original, product.originalElementOrNull(target))
    }

    @Test
    fun macroCallSourceRetainsTheAnnotationOnlyNativeEntry() {
        val source = factory().createAnnotationOnlyFile("@Tag[1] specific func item(): Unit {}")
        val original = functions(source).single()
        val originalShape = grammar(source.node)
        equal(CjSourceKind.MACRO_CALL, source.sourceKind)
        truth(elements<CjMacroExpression>(source).isEmpty())
        equal("Tag", original.annotationEntries.single().shortName?.asString())
        val product = CangJieParser.parseCompilationFile(source, specificMode)
        equal(CjSourceKind.MACRO_CALL, product.file.sourceKind)
        equal(CjSourceKind.MACRO_CALL, product.actualInputSeed.sourceKind)
        truth(product.actualInputSeed.context.enableCustomAnnotation)
        truth(elements<CjMacroExpression>(product.file).isEmpty())
        val target = functions(product.file).single()
        equal("Tag", target.annotationEntries.single().shortName?.asString())
        identical(target, product.correspondingElementOrNull(original))
        identical(original, product.originalElementOrNull(target))
        equal(originalShape, grammar(source.node))
    }

    @Test
    fun independentBlockFragmentPreservesItsCompleteEntryAndLocalScope() {
        val source = factory().createBlockCodeFragment("let value: Int64 = 1\nvalue + 2", null)
        val product = compileNativeFragment(source)
        val target = product.file as CjBlockCodeFragment
        equal(listOf("let value: Int64 = 1", "value + 2"), target.getContentElement().statements.map { it.text })
        equal(CjScopeKind.FUNC_BODY, product.actualInputSeed.context.statementScope)
        absent(target.context)
        truth(product.diagnostics.isEmpty(), product.diagnostics.toString())
        identical(target.getContentElement(), product.correspondingElementOrNull(source.getContentElement()))
    }

    @Test
    fun independentExpressionFragmentKeepsTheExpressionEntryInsteadOfAHostWrapper() {
        val source = factory().createExpressionCodeFragment("1 + 2", null)
        val product = compileNativeFragment(source)
        val target = product.file as CjExpressionCodeFragment
        equal(CjNodeTypes.BINARY_EXPRESSION, present(target.getContentElement()).node.elementType)
        equal("1 + 2", target.getContentElement()?.text)
        absent(target.context)
        truth(functions(target).isEmpty())
        truth(product.diagnostics.isEmpty(), product.diagnostics.toString())
        identical(target.getContentElement(), product.correspondingElementOrNull(present(source.getContentElement())))
    }

    @Test
    fun independentTypeFragmentKeepsTheTypeRootAndActualLexicalInput() {
        val source = CjTypeCodeFragment(project, "fragment.cj", "Array<Int64>", null)
        val product = compileNativeFragment(source)
        val target = product.file as CjTypeCodeFragment
        equal("Array<Int64>", present(target.getContentElement()).text)
        equal(CjNodeTypes.TYPE_REFERENCE, target.getContentElement()?.node?.elementType)
        absent(target.context)
        truth(functions(target).isEmpty())
        truth(product.diagnostics.isEmpty(), product.diagnostics.toString())
        identical(target.getContentElement(), product.correspondingElementOrNull(present(source.getContentElement())))
    }

    @Test
    fun independentMacroFragmentKeepsExplicitParseNodesScopeAndAllDirectRoots() {
        val entry = CjMacroFragmentContext.Nodes(
            MacroParsingScope.Declaration(CjScopeKind.FUNC_BODY), CjMacroFragmentParent.OTHER,
            outerDeclIdent = null, declarationPrefix = CjMacroDeclarationPrefix.EMPTY,
        )
        val creationSeed = createMacroFragmentParserSeed(
            ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), CjmpParsingContext.SyntaxOnly, entry,
        )
        val source = CjMacroCodeFragment(project, "fragment.cj", "let value = 1\nvalue + 2", null, creationSeed)
        val product = compileNativeFragment(source)
        val target = product.file as CjMacroCodeFragment
        identical(entry, product.inputSeed.macroFragment)
        identical(entry, product.actualInputSeed.macroFragment)
        truth(product.actualInputSeed.context.disableMacroParsing)
        truth(product.actualInputSeed.context.enableCustomAnnotation)
        equal(CjScopeKind.FUNC_BODY, product.actualInputSeed.context.statementScope)
        absent(product.actualInputSeed.context.primaryDeclaration)
        equal(listOf(CjNodeTypes.VARIABLE, CjNodeTypes.BINARY_EXPRESSION), target.getContentElement().roots.map { it.node.elementType })
        equal(listOf("let value = 1", "value + 2"), target.getContentElement().roots.map { it.text })
        source.getContentElement().roots.zip(target.getContentElement().roots).forEach { (origin, destination) ->
            identical(destination, product.correspondingElementOrNull(origin))
            identical(origin, product.originalElementOrNull(destination))
        }
        absent(target.context)
        truth(product.diagnostics.isEmpty(), product.diagnostics.toString())
    }

    @Test
    fun importsInvalidateEitherFragmentWithoutChangingItsText() {
        for (editTarget in listOf(false, true)) {
            val source = factory().createExpressionCodeFragment("1 + 2", null)
            source.addImportsFromString("std.math.*")
            val product = compileNativeFragment(source)
            val target = product.file as CjCodeFragment
            equal(source.importsToString(), target.importsToString())
            val edited = if (editTarget) target else source
            truth(project is MockProject || !edited.viewProvider.isEventSystemEnabled)
            val text = edited.text
            val generation = edited.compilationInputVersion
            edited.addImportsFromString("std.math.*")
            equal(generation, edited.compilationInputVersion)
            product.requireConsistentInput()
            edited.addImportsFromString("std.collection.*")
            equal(text, edited.text)
            truth(edited.compilationInputVersion > generation)
            assertInvalid(product)
        }
    }

    @Test
    fun forcedScopeGenerationInvalidatesEitherFragmentEvenWhenTheScopeIsRestored() {
        for (editTarget in listOf(false, true)) {
            val source = factory().createExpressionCodeFragment("1 + 2", null)
            val scope = GlobalSearchScope.allScope(project)
            source.forceResolveScope(scope)
            val product = compileNativeFragment(source)
            val target = product.file as CjCodeFragment
            identical(scope, target.getForcedResolveScope())
            val edited = if (editTarget) target else source
            val stamp = edited.modificationStamp
            val generation = edited.compilationInputVersion
            edited.forceResolveScope(null)
            edited.forceResolveScope(scope)
            identical(scope, edited.getForcedResolveScope())
            equal(stamp, edited.modificationStamp)
            equal("1 + 2", edited.text)
            truth(edited.compilationInputVersion > generation)
            assertInvalid(product)
        }
    }

    @Test
    fun actualSourceAndTargetHostsInvalidateFragmentsOnTextOrCompilationContextChanges() {
        for (editTargetHost in listOf(false, true)) {
            for (textChange in listOf(false, true)) {
                val host = sourceFile("package application.fragment\nfunc host(): Unit {}")
                val hostProduct = CangJieParser.parseCompilationFile(host, specificMode)
                val sourceContext = functions(host).single()
                val targetContext = functions(hostProduct.file).single()
                val fragment = factory().createExpressionCodeFragment("1 + 2", sourceContext)
                val entry = targetSeed(fragment)
                val product = CangJieParser.parseCompilationFragment(fragment, entry, targetContext)
                identical(entry, product.inputSeed)
                identical(sourceContext, fragment.context)
                identical(targetContext, product.file.context)
                equal("application", product.actualInputSeed.languageModuleName)
                product.requireConsistentInput()
                val edited = if (editTargetHost) hostProduct.file else host
                val fragmentText = fragment.text
                if (textChange) {
                    reparse(edited, "package application.fragment\nfunc changedHost(): Unit {}")
                } else {
                    val stamp = edited.modificationStamp
                    edited.cjmpParsingContext = commonMode
                    equal(stamp, edited.modificationStamp)
                }
                equal(fragmentText, fragment.text)
                assertInvalid(product)
            }
        }
    }

    @Test
    fun cloneImportsAndScopeHaveIndependentGenerationsAndDoNotInvalidateTheOriginalProduct() {
        val source = factory().createExpressionCodeFragment("1 + 2", null)
        source.addImportsFromString("std.math.*")
        val product = compileNativeFragment(source)
        val generation = source.compilationInputVersion
        val clone = source.copy() as CjExpressionCodeFragment
        val cloneProduct = compileNativeFragment(clone)
        equal(generation, clone.compilationInputVersion)
        clone.addImportsFromString("std.collection.*")
        clone.forceResolveScope(GlobalSearchScope.allScope(project))
        truth(clone.compilationInputVersion > generation)
        equal(generation, source.compilationInputVersion)
        equal("import std.math.*", source.importsToString())
        absent(source.getForcedResolveScope())
        assertInvalid(cloneProduct)
        product.requireConsistentInput()
        identical(product.file, product.withTargetFile { it })
        absent(product.correspondingElementOrNull(present(clone.getContentElement())))
    }

    @Test
    fun originalDocumentAndTargetTreeEditsInvalidateProductsWithoutChangingTheOtherTree() {
        for (editTarget in listOf(false, true)) {
            val source = CjPsiFactory(project, markGenerated = false, eventSystemEnabled = true)
                .createFile("func item(): Int64 { 1 }")
            val product = CangJieParser.parseCompilationFile(source, specificMode)
            val originalText = source.text
            val originalDiagnostics = product.diagnostics.toList()
            val other = if (editTarget) source else product.file
            val otherShape = grammar(other.node)
            val otherStamp = other.modificationStamp
            if (editTarget) {
                reparse(product.file, "func item(): Int64 { 2 }")
            } else {
                val document = present(PsiDocumentManager.getInstance(project).getDocument(source))
                changeOnEdt { document.setText("func item(): Int64 { 2 }") }
                equal("func item(): Int64 { 2 }", document.text)
                equal(document.text, source.viewProvider.contents.toString())
                // fixture 使用 mock commit processor；未提交的真实 Document 已是新一代输入。
            }
            equal(originalText, other.text)
            equal(otherShape, grammar(other.node))
            equal(otherStamp, other.modificationStamp)
            equal(originalDiagnostics, product.diagnostics)
            assertInvalid(product)
        }
    }

    @Test
    fun targetConsumptionChecksBeforeAndAfterTheCallbackInTheSameReadAction() {
        val source = factory().createExpressionCodeFragment("1 + 2", null)
        val product = compileNativeFragment(source)
        val target = product.file as CjExpressionCodeFragment
        val value = product.withTargetFile {
            truth(ApplicationManager.getApplication().isReadAccessAllowed)
            identical(target, it)
            identical(target.getContentElement(), product.correspondingElementOrNull(present(source.getContentElement())))
            42
        }
        equal(42, value)
        var consumed = false
        fails<IllegalStateException> {
            product.withTargetFile {
                truth(ApplicationManager.getApplication().isReadAccessAllowed)
                consumed = true
                target.forceResolveScope(GlobalSearchScope.allScope(project))
                "不能提交的旧代次结果"
            }
        }
        truth(consumed, "后验必须发生在实际消费之后")
        assertInvalid(product)
    }

    @Test
    fun localCommonInTheBodyChangesTheTargetRoleAndFollowingSeeds() {
        // 官方 local-role/common 探针：非法局部 common 仍先执行 CheckCJMPModifiers。
        val text = """
            func outer(): Unit {
                common func local(): Unit {}
            }
            specific func following(): Unit {}
        """.trimIndent()
        val source = sourceFile(text)
        val originalShape = grammar(source.node)
        val originalSeed = actualSeed(source)
        val originalFacts = source.parsedFileFacts
        val both = CjmpParsingContext.Compilation(true, true, true, mixedCommonSpecificDeclarations = false)
        val product = CangJieParser.parseCompilationFile(source, both)
        equal(CjmpFileRole.COMMON, present(product.file.parsedFileFacts).cjmpRole)
        val local = functions(product.file).single { it.name == "local" }
        val following = functions(product.file).single { it.name == "following" }
        equal(CjScopeKind.FUNC_BODY, modifiers(local).scope)
        // v1.1.3 GetModifierAttrs 对非法局部修饰符报错后仍插入其属性；它不同于文件角色失败。
        equal(setOf(CjModifierKind.COMMON), modifiers(local).attributes)
        equal(emptySet(), modifiers(following).attributes)
        val followingSeed = present(following.bodyBlockExpression).node.requireParserSeed()
        equal(CjmpFileRole.COMMON, followingSeed.fileRole)
        equal(both, followingSeed.cjmpContext)
        identical(product.actualInputSeed.lexicalInput, followingSeed.lexicalInput)
        val events = present(product.file.collectParsedRoleSnapshot()).events.map { it.kind }
            .filterIsInstance<CjmpRoleEventKind.Check>()
        equal(listOf(CjmpRoleModifiers(true, false, false), CjmpRoleModifiers(false, true, false)), events.map { it.modifiers })
        truth(product.diagnostics.any { it.message == CangJieParsingBundle.message("parsing.error.cjmp.common.and.specific.in.same.file") })
        equal(originalShape, grammar(source.node))
        identical(originalSeed, actualSeed(source))
        equal(originalFacts, source.parsedFileFacts)
    }

    @Test
    fun rejectedForeignVarargsCannotGrantTheOriginalFunctionATargetOwner() {
        // 官方 CheckVariableParams：只有 FOREIGN 可接受末尾 ...；错误角色清除全部返回属性。
        val source = sourceFile("specific foreign func nativeCall(value: Int64, ...): Unit")
        val original = functions(source).single()
        val originalShape = grammar(source.node)
        val specific = CangJieParser.parseCompilationFile(source, specificMode)
        val accepted = functions(specific.file).single()
        truth(modifiers(accepted).hasModifier(CjModifierKind.FOREIGN))
        identical(accepted, specific.correspondingElementOrNull(original))
        identical(original, specific.originalElementOrNull(accepted))
        val common = CangJieParser.parseCompilationFile(source, commonMode)
        truth(common.diagnostics.any { it.message == specificInNonSpecific })
        truth(common.diagnostics.any { it.message == "variable length parameter can only show in the foreign function" }, common.diagnostics.toString())
        // IS_BROKEN 可以使官方声明 vector 丢弃 owner；不要求恢复同名函数或原参数签名。
        absent(common.correspondingElementOrNull(original))
        for (rejected in functions(common.file)) {
            falseValue(modifiers(rejected).hasModifier(CjModifierKind.FOREIGN))
            absent(common.originalElementOrNull(rejected))
        }
        equal(originalShape, grammar(source.node))
    }

    @Test
    fun clearingMutAttributesDoesNotRemoveAnExplicitSetterFromTheTargetGrammar() {
        // 官方 ParsePropDecl 根据返回 MUT 设置 isVar，但 ParsePropBody 独立消费显式 set。
        val source = sourceFile("class Box { specific mut prop value: Int64 { get() { 1 } set(next) {} } }")
        val original = elements<CjProperty>(source).single()
        val originalSetter = present(original.setter)
        val originalModifiers = modifiers(original)
        val common = CangJieParser.parseCompilationFile(source, commonMode)
        val specific = CangJieParser.parseCompilationFile(source, specificMode)
        val rejected = elements<CjProperty>(common.file).single()
        val accepted = elements<CjProperty>(specific.file).single()
        equal(emptySet(), modifiers(rejected).attributes)
        truth(modifiers(accepted).hasModifier(CjModifierKind.MUT))
        present(rejected.getter)
        val rejectedSetter = present(rejected.setter)
        present(accepted.setter)
        equal(originalSetter.text, rejectedSetter.text)
        identical(rejected, common.correspondingElementOrNull(original))
        identical(original, common.originalElementOrNull(rejected))
        identical(rejectedSetter, common.correspondingElementOrNull(originalSetter))
        identical(originalSetter, common.originalElementOrNull(rejectedSetter))
        identical(originalModifiers, modifiers(original))
        identical(originalSetter, original.setter)
    }

    /** PSI局部替换只撤销旧完整结果；后继目标编译必须使用本代来源输入，不能恢复旧facts。 */
    @Test
    fun emptyBodyReplacementUsesCurrentCompilationInput() {
        assertCompilationAfterBodyReplacement("{}", "{ 42 }")
    }

    @Test
    fun inferredBodyReplacementUsesCurrentCompilationInput() {
        assertCompilationAfterBodyReplacement("{ 42 }", "{}")
    }

    private fun assertCompilationAfterBodyReplacement(oldBody: String, newBody: String) {
        val source = sourceFile("package application\nfunc compute() $oldBody\nspecific func following(): Unit {}")
        val original = functions(source).single { it.name == "compute" }
        val following = functions(source).single { it.name == "following" }
        val oldSeed = actualSeed(source)
        val oldProduct = CangJieParser.parseCompilationFile(source, specificMode)
        val oldView = source.currentPsiSourceView()
        changeOnEdt {
            val replacement = factory().createFunction("func replacement() $newBody")
            present(original.bodyExpression).replace(present(replacement.bodyExpression))
        }

        absent(source.parsedFileFacts, "A mixed PSI generation has no new complete parser result")
        absent(source.parsedRoleRegion)
        identical(oldSeed, source.getCopyableUserData(ROOT_PARSER_SEED_KEY), "The old producer input remains historical")
        fails<IllegalStateException> { oldView.requireCurrent() }
        assertInvalid(oldProduct)
        val view = source.currentPsiSourceView()
        distinct(present(oldSeed.lexicalInput), view.lexicalInput)
        equal(source.text, view.sourceContext.snapshot.text)

        val product = CangJieParser.parseCompilationFile(source, specificMode)
        identical(view.lexicalInput, product.actualInputSeed.lexicalInput)
        equal(view.sourceContext, product.actualInputSeed.sourceContext)
        distinct(oldProduct.diagnosticScope.rootIdentity, product.diagnosticScope.rootIdentity)
        absent(source.parsedFileFacts, "Target compilation must not publish its facts on the source tree")
        absent(source.parsedRoleRegion)
        val computeTarget = functions(product.file).single { it.name == "compute" }
        val followingTarget = functions(product.file).single { it.name == "following" }
        equal(newBody, present(computeTarget.bodyExpression).text)
        identical(computeTarget, product.correspondingElementOrNull(original))
        identical(followingTarget, product.correspondingElementOrNull(following))
        identical(original, product.originalElementOrNull(computeTarget))
        identical(following, product.originalElementOrNull(followingTarget))
        equal(view.sourceContext.snapshot.identity, modifiers(computeTarget).sourceToken.sourceIdentity)
        product.requireConsistentInput()
        view.requireCurrent()
    }

    private val commonMode = CjmpParsingContext.Compilation(true, false, true)
    private val specificMode = CjmpParsingContext.Compilation(false, true, true)
    private val specificInNonSpecific: String
        get() = CangJieParsingBundle.message("parsing.error.cjmp.specific.in.non.specific.file")

    private fun factory(): CjPsiFactory = CjPsiFactory(project, markGenerated = false)
    private fun sourceFile(text: String): CjFile = factory().createFile(text)
    private fun functions(root: PsiElement): List<CjNamedFunction> = elements(root)
    private inline fun <reified T : PsiElement> elements(root: PsiElement): List<T> =
        PsiTreeUtil.findChildrenOfType(root, T::class.java).toList()

    /** 只读取当前真实 owner 发布的属性，目标期望不从原 PSI 回贴。 */
    private fun modifiers(owner: PsiElement): CjParsedDeclarationModifiers =
        present(owner.node.declarationModifiersForOwnerOrNull())

    /** 展开当前根后读取其 producer 发布的词法输入，不能另造 SourceContext 或 lexicalInput。 */
    private fun actualSeed(file: CjFile): CjParserSeed {
        grammar(file.node)
        val seed = present(file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        present(seed.sourceContext).requireInput(file.node.chars)
        identical(present(seed.sourceContext).snapshot, present(seed.lexicalInput).source)
        identical(seed.lexicalInput, present(file.parsedRoleRegion).lexicalInput)
        return seed
    }

    private fun targetSeed(fragment: CjCodeFragment): CjParserSeed = actualSeed(fragment).copy(
        cjmpContext = specificMode, fileRole = CjmpFileRole.SPECIFIC,
    )

    /** 调用方入口身份与目标实际发布值分开；完整 seed 的其余文法和来源输入必须保留。 */
    private fun compileNativeFragment(fragment: CjCodeFragment): ParsedCangJiePsiFile {
        val original = actualSeed(fragment)
        val sourceView = fragment.currentPsiSourceView()
        val originalShape = grammar(fragment.node)
        val seed = targetSeed(fragment)
        val product = CangJieParser.parseCompilationFragment(fragment, seed, null)
        identical(fragment, product.originFile)
        distinct(fragment.node, product.file.node)
        identical(seed, product.inputSeed)
        distinct(seed, product.actualInputSeed)
        val targetRoot = actualSeed(product.file)
        identical(product.actualInputSeed, targetRoot)

        // 原/目标实际 ROOT 必须属于不同诊断根；空路径证明目标不是旧根的 lazy 子区域。
        absent(original.diagnosticScope, "独立原片段没有父诊断区域")
        absent(seed.diagnosticScope)
        absent(targetRoot.diagnosticScope, "本次根输出不得反写为输入的父区域")
        val originalScope = present(fragment.parsingDiagnosticScopeOrNull())
        val targetScope = product.diagnosticScope
        identical(targetScope, product.file.parsingDiagnosticScopeOrNull())
        distinct(originalScope, targetScope)
        truth(originalScope.rootIdentity != targetScope.rootIdentity, "独立目标必须分配新诊断根")
        equal(emptyList<Long>(), targetScope.path)

        // 真实目标树及冻结 region 验证完整输入；输出时钟不参与 seed 值。
        val targetContext = present(targetRoot.sourceContext)
        val targetInput = present(targetRoot.lexicalInput)
        val targetRegion = present(product.file.parsedRoleRegion)
        equal(0, targetContext.inputStart)
        equal(product.file.text.length, targetContext.inputEnd)
        targetContext.requireInput(product.file.node.chars)
        targetContext.requireInput(product.file.viewProvider.contents)
        equal(product.file.text, targetInput.source.text)
        identical(targetContext.snapshot, targetInput.source)
        equal(targetContext, targetRegion.sourceContext)
        identical(targetInput, targetRegion.lexicalInput)
        identical(sourceView.lexicalInput, targetInput)
        equal(sourceView.sourceContext, targetContext)
        val targetEntry = present(targetRegion.entrySeed)
        absent(targetEntry.diagnosticScope, "独立目标入口没有父诊断区域")
        equal(seed.copy(sourceContext = sourceView.sourceContext, lexicalInput = sourceView.lexicalInput), targetEntry)
        // 副本保留原producer入口，但实际文件坐标视图可以有独立来源身份；目标消费当前视图。
        equal(seed.copy(sourceContext = sourceView.sourceContext, lexicalInput = sourceView.lexicalInput), product.actualInputSeed)
        identical(sourceView.lexicalInput, product.actualInputSeed.lexicalInput)
        equal(sourceView.sourceContext, product.actualInputSeed.sourceContext)
        equal(original.context, product.actualInputSeed.context)
        equal(original.quotedSyntaxDepth, product.actualInputSeed.quotedSyntaxDepth)
        equal(original.sourceKind, product.actualInputSeed.sourceKind)
        equal(original.languageModuleName, product.actualInputSeed.languageModuleName)
        equal(CjmpFileRole.SPECIFIC, product.actualInputSeed.fileRole)
        equal(specificMode, product.file.cjmpParsingContext)
        equal(originalShape, grammar(fragment.node))
        identical(original, actualSeed(fragment))
        product.requireConsistentInput()
        return product
    }

    /** 先验失效时不允许进入消费者；两个方向的源码对应也必须拒绝旧代次。 */
    private fun assertInvalid(product: ParsedCangJiePsiFile) {
        fails<IllegalStateException> { product.requireConsistentInput() }
        var entered = false
        fails<IllegalStateException> { product.withTargetFile { entered = true } }
        falseValue(entered)
        fails<IllegalStateException> { product.correspondingElementOrNull(product.originFile) }
        fails<IllegalStateException> { product.originalElementOrNull(product.file) }
    }

    private data class Grammar(val kind: IElementType, val text: String, val children: List<Grammar>)

    /** 快照记录真实树结构，既用于展开，也用于确认目标解析没有改动原树。 */
    private fun grammar(node: ASTNode): Grammar {
        val children = generateSequence(node.firstChildNode) { it.treeNext }.map(::grammar).toList()
        return Grammar(node.elementType, node.chars.toString(), children)
    }

    private fun reparse(file: CjFile, newText: String) {
        val oldText = file.text
        val result = BlockSupportImpl.reparse(
            file, file.node, TextRange(0, oldText.length), newText, EmptyProgressIndicator(), oldText,
        )
        changeOnEdt { result.log.doActualPsiChange(file) }
        equal(newText, file.text)
    }

    /** 遵循既有 CjParsingTestCase harness 的 EDT、命令和写事务合同。 */
    private fun changeOnEdt(action: () -> Unit) {
        try {
            EventQueue.invokeAndWait {
                CommandProcessor.getInstance().executeCommand(project, {
                    ApplicationManager.getApplication().runWriteAction { action() }
                }, "修改编译解析测试输入", null)
            }
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
    }
}
