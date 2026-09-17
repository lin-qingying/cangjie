package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.annotations.BuiltInAnnotationDescriptor
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.stubs.CangJieFeaturesDirectiveStub
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * FFI 声明容器及注解语法结构契约。
 *
 * 语法依据官方 896235c9fd18f22d570a9818ac672838c36c3932 的 ParseAnnotations.cpp；
 * 这里断言 token、参数和归属，合法声明目标由 FFI 双路语义矩阵另行验证。
 */
class ForeignAndAnnotationParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /** foreign block 保留自己的函数容器，后续顶层声明不能被其吞入。 */
    @Test
    fun foreignBlockPreservesFunctionOwnershipAndForeignToken() {
        val file = parse(
            "foreignOwnership",
            """
            foreign {
                func readNative(value: CPointer<Int32>): Int32
                func writeNative(value: CPointer<Int32>, replacement: Int32): Unit
            }
            foreign func directNative(): Unit
            func ordinary(): Unit {}
            """.trimIndent(),
        )
        val directive = PsiTreeUtil.getChildrenOfTypeAsList(file, CjForeignDirective::class.java).single()
        assertNotNull(directive.node.findChildByType(CjTokens.FOREIGN_KEYWORD))
        val body = checkNotNull(directive.body)
        val members = body.declarations.filterIsInstance<CjNamedFunction>()
        assertEquals(listOf("readNative", "writeNative"), members.map { it.name })
        members.forEach { assertSame(body, it.parent) }

        val topLevelFunctions = PsiTreeUtil.getChildrenOfTypeAsList(file, CjNamedFunction::class.java)
        assertEquals(listOf("directNative", "ordinary"), topLevelFunctions.map { it.name })
        assertTrue(topLevelFunctions.first().hasModifier(CjTokens.FOREIGN_KEYWORD))
        assertFalse(topLevelFunctions.last().hasModifier(CjTokens.FOREIGN_KEYWORD))
    }

    /** foreign C 的变长参数保留为签名标记，不伪造参数名或参数类型。 */
    @Test
    fun foreignVariadicParameterPreservesEllipsisMarker() {
        val file = parse(
            "foreignVariadicParameter",
            "foreign func printf(format: CPointer<UInt8>, ...): Int32",
        )
        val function = file.declarations.filterIsInstance<CjNamedFunction>().single()
        assertEquals(listOf("format: CPointer<UInt8>", "..."), function.valueParameterList?.parameters?.map { it.text.trim() })
    }

    /** 专用参数语法必须各自保留完整注解节点，不被解析成调用或宏输入碎片。 */
    @Test
    fun builtInArgumentFormsPreserveTheirAnnotationNodes() {
        val annotationForms = listOf(
            "C" to "@C",
            "Java" to "@Java[\"java.lang.String\"]",
            "JavaMirror" to "@JavaMirror[\"java.lang.Object\"]",
            "JavaImpl" to "@JavaImpl",
            "JavaHasDefault" to "@JavaHasDefault",
            "ObjCMirror" to "@ObjCMirror[\"NSObject\"]",
            "ObjCImpl" to "@ObjCImpl",
            "ObjCInit" to "@ObjCInit",
            "ObjCOptional" to "@ObjCOptional",
            "ForeignName" to "@ForeignName[\"nativeName\"]",
            "ForeignGetterName" to "@ForeignGetterName[\"getValue\"]",
            "ForeignSetterName" to "@ForeignSetterName[\"setValue:\"]",
            "CallingConv" to "@CallingConv[CDECL]",
            "Attribute" to "@Attribute[customFlag, \"backendFlag\"]",
            "Intrinsic" to "@Intrinsic",
            "OverflowThrowing" to "@OverflowThrowing",
            "OverflowWrapping" to "@OverflowWrapping[wrapping]",
            "OverflowSaturating" to "@OverflowSaturating",
            "FastNative" to "@FastNative",
            "ConstSafe" to "@ConstSafe",
            "Annotation" to "@Annotation[target: [Type, GlobalFunction, Extension]]",
            "Deprecated" to "@Deprecated[message: \"old\", since: \"1.1\", strict: true]",
            "Frozen" to "@Frozen",
        )
        for ((name, form) in annotationForms) {
            val descriptor = checkNotNull(BuiltInAnnotationRegistry.find(name) as? BuiltInAnnotationDescriptor)
            val packageHeader = if (descriptor.standardLibraryOnly) "package std.annotation_forms\n\n" else ""
            val file = parse("annotation$name", "$packageHeader$form\nfunc annotated(): Unit {}")
            val annotation = annotations(file).singleOrNull()
            assertNotNull(annotation, "no annotation PSI for $name: $form")
            assertEquals(
                expected = name,
                actual = annotation.shortName?.asString(),
                message = "$form; type=${annotation.typeReference?.text}; short=${annotation.shortName?.asString()?.toList()}",
            )
            assertEquals(expected = form, actual = annotation.text, message = form)
            assertFalse(annotation.isCompileTimeVisible, form)
            if (name == "CallingConv") {
                assertEquals(CallingConvention.CDECL, annotation.callingConvention, form)
            }
        }
    }

    /** @ 与 @! 的区别属于语法来源，不能从名称或声明类型推断。 */
    @Test
    fun systemAnnotationsPreserveCompileTimeVisibilityAndNamedArguments() {
        val file = parse(
            "compileTimeVisibility",
            """
            @Deprecated[message: "runtime"]
            @!APILevel[since: "1.1", syscap: "SystemCapability.Test"]
            @!Hide[isChecked: true]
            func platformFunction(): Unit {}
            """.trimIndent(),
        )
        val annotations = annotations(file)
        assertEquals(listOf("Deprecated", "APILevel", "Hide"), annotations.map { it.shortName?.asString() })
        assertEquals(listOf(false, true, true), annotations.map { it.isCompileTimeVisible })
        val apiLevel = annotations[1]
        assertEquals(listOf("since", "syscap"), apiLevel.valueArguments.map { it.getArgumentName()?.asName?.asString() })
        assertEquals(listOf("\"1.1\"", "\"SystemCapability.Test\""), apiLevel.valueArguments.map { it.getArgumentExpression()?.text })
        assertEquals(listOf("isChecked"), annotations[2].valueArguments.map { it.getArgumentName()?.asName?.asString() })
    }

    /** 官方保留字内置注解不允许用 @! 转成自定义注解。 */
    @Test
    fun compileTimePrefixRejectsReservedBuiltInSpellings() {
        for (name in listOf("C", "Deprecated", "CallingConv")) {
            val source = "@!$name\nfunc rejected(): Unit {}"
            val file = createPsiFile("reservedCompileTime$name", source) as CjFile
            assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isNotEmpty(), source)
        }
    }

    /** 注解化 lambda 保留独立注解身份，mock 的 test mode 限制属于后续语义阶段。 */
    @Test
    fun lambdaAnnotationsKeepTheirExpressionOwnership() {
        val file = parse(
            "annotationLambdas",
            """
            func expressions(): Unit {
                let mockable = @EnsurePreparedToMock { => () }
                let wrapping = { => 1 }
            }
            """.trimIndent(),
        )
        val annotatedExpressions = PsiTreeUtil.findChildrenOfType(file, CjAnnotatedExpression::class.java)
        assertEquals(listOf("EnsurePreparedToMock"),
            annotatedExpressions.flatMap { it.annotationEntries }.map { it.shortName?.asString() })
        assertEquals(2, PsiTreeUtil.findChildrenOfType(file, CjLambdaExpression::class.java).size)
    }

    /** NonProduct 属于 features directive，不能漂移成后续函数的声明注解。 */
    @Test
    fun nonProductStaysOnFeaturesDirective() {
        val file = parse(
            "nonProductFeatures",
            """
            @NonProduct
            features { sample.api, sample.detail }

            package feature_contract

            func following(): Unit {}
            """.trimIndent(),
        )
        assertEquals(listOf("NonProduct"), file.featuresDirective?.annotationEntries?.map { it.shortName?.asString() })
        assertEquals(listOf("sample.api", "sample.detail"), file.featuresDirective?.featureIds)
        val following = PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java).single()
        assertEquals("following", following.name)
        assertTrue(PsiTreeUtil.findChildrenOfType(following, CjAnnotation::class.java).isEmpty())
    }

    /** features header 在 source PSI 与 file stub 之间保留同一组 feature id。 */
    @Test
    fun featuresDirectiveIsAvailableFromFileStub() {
        val file = parse(
            "featuresStub",
            """
            @NonProduct
            features { sample.api, sample.detail }
            package feature_contract
            func following(): Unit {}
            """.trimIndent(),
        )
        val stub = (CjFileStubBuilder().buildStubTree(file) as CangJieFileStub)
            .childrenStubs
            .filterIsInstance<CangJieFeaturesDirectiveStub>()
            .single()
        assertEquals(listOf("sample.api", "sample.detail"), stub.featureIds)
        assertEquals(stub.featureIds, stub.psi.featureIds)
        assertEquals(listOf("NonProduct"), stub.psi.annotationEntries.map { it.shortName?.asString() })
    }

    /** IfAvailable 使用圆括号的独立宏表达式，不能登记为声明注解。 */
    @Test
    fun ifAvailableKeepsItsExpressionForm() {
        val file = parse(
            "ifAvailableExpression",
            """
            func available(): Unit {
                @IfAvailable(level: 19, { => () }, { => () })
            }
            """.trimIndent(),
        )
        assertTrue(annotations(file).none { it.shortName?.asString() == "IfAvailable" })
        val expressions = PsiTreeUtil.findChildrenOfType(file, CjIfAvailableExpression::class.java)
        assertEquals(1, expressions.size)
        assertEquals("IfAvailable", expressions.single().text.substringBefore('(').removePrefix("@"))
        assertEquals("level", expressions.single().conditionName)
    }

    private fun parse(name: String, source: String): CjFile {
        val file = createPsiFile(name, source) as CjFile
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(errors.isEmpty(), "$name: ${errors.joinToString { it.errorDescription }}\n$source")
        return file
    }

    private fun annotations(file: CjFile): List<CjAnnotation> =
        file.declarations
            .filterIsInstance<CjAnnotated>()
            .flatMap { it.annotationEntries }
            .sortedBy { it.textOffset }
}
