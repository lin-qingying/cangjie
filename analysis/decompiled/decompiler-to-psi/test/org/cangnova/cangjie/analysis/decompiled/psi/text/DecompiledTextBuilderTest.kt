package org.cangnova.cangjie.analysis.decompiled.psi.text

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.stubs.ObjectStubSerializer
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.AbstractStringEnumerator
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.CjAnnotations
import org.cangnova.cangjie.psi.CjAbstractClassBody
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.impl.CangJieClassStubImpl
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.CangJieBasicTypeStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieMacroStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieMainFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieModifierListStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieNamedFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieParameterStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePlaceHolderStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePropertyAccessorStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePropertyStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieStructStubImpl
import org.cangnova.cangjie.psi.stubs.impl.deepCopy
import org.cangnova.cangjie.psi.stubs.impl.ModifierMaskUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * 验证 `.cjo` compiled stub 渲染为仓颉反编译文本时的语法格式契约。
 *
 * 测试通过手工构造 stub 或使用轻量 PSI 环境，覆盖函数、宏、主函数、类型成员、
 * 属性访问器和修饰符等反编译输出的关键形态。
 */
class DecompiledTextBuilderTest {
    @Test
    fun annotationArgumentsAndCompileTimePrefixSurviveDecompiledText() {
        withCoreEnvironment { environment ->
            val annotation = "@!APILevel[since: \"22\", permission: \"a\" & \"b\"]"
            val source = PsiFileFactory.getInstance(environment.project).createFileFromText(
                "sample.cj", CangJieFileType.INSTANCE,
                "package sample\n$annotation\nfunc f(): Unit {}",
            ) as CjFile
            val rendered = buildDecompiledText(source.calcStubTree().root as CangJieFileStubImpl)
            assertTrue(rendered.contains(annotation), rendered)
        }
    }

    /** 先检查源码 stub 的表达式结构，避免把序列化无损误当成语义完整。 */
    @Test
    fun binaryAnnotationArgumentRetainsOperandsAndOperator() {
        withCoreEnvironment { environment ->
            val source = PsiFileFactory.getInstance(environment.project).createFileFromText(
                "sample.cj", CangJieFileType.INSTANCE,
                "package sample\n@!APILevel[permission: \"a\" & \"b\"]\nfunc f(): Unit {}",
            ) as CjFile
            val root = source.calcStubTree().root as CangJieFileStubImpl
            val annotation = root.findChildStubByType(CjStubElementTypes.FUNCTION)!!
                .findChildStubByType(CjStubElementTypes.ANNOTATIONS)!!
                .findChildStubByType(CjStubElementTypes.ANNOTATION)!!
            val argument = annotation.findChildStubByType(CjStubElementTypes.VALUE_ARGUMENT_LIST)!!
                .findChildStubByType(CjStubElementTypes.VALUE_ARGUMENT)!!
            val expressions = argument.childrenStubs.filter { it.stubType != CjStubElementTypes.VALUE_ARGUMENT_NAME }
            assertEquals(listOf("BINARY_EXPRESSION"), expressions.map { it.stubType.toString() })
            assertEquals(listOf("STRING_TEMPLATE", "OPERATION_REFERENCE", "STRING_TEMPLATE"),
                expressions.single().childrenStubs.map { it.stubType.toString() })
        }
    }

    /** 注解渲染必须可用于已落盘的 compiled stub，不能借用原始文件 AST。 */
    @Test
    fun annotationArgumentsSurviveSerializationAndDeepCopy() {
        withCoreEnvironment { environment ->
            val annotation = "@!APILevel[since: \"22\", permission: \"a\" & \"b\"]"
            val source = PsiFileFactory.getInstance(environment.project).createFileFromText(
                "sample.cj", CangJieFileType.INSTANCE,
                "package sample\n$annotation\nfunc f(): Unit {}",
            ) as CjFile
            val sourceStub = source.calcStubTree().root as CangJieFileStubImpl
            val restored = (roundTripStub(sourceStub) as CangJieFileStubImpl).deepCopy()
            assertNull(restored.psi, "恢复后的 stub 不得绑定源码 PSI")
            fun shape(stub: StubElement<*>): String = buildString {
                append(stub)
                stub.childrenStubs.forEach { append("\n" + shape(it).prependIndent("  ")) }
            }
            assertEquals(shape(sourceStub), shape(restored), "序列化及复制必须保留所有 stub 字段和子节点")
            println("Restored annotation stub tree:\n${shape(restored)}")
            val rendered = buildDecompiledText(restored)
            assertTrue(rendered.contains(annotation), rendered)
        }
    }

    /** 逐节点走真实 serializer，恢复的整棵树只保留协议字段，不保留 PSI/AST。 */
    private fun roundTripStub(
        original: StubElement<*>,
        parent: StubElement<*>? = null,
        strings: AbstractStringEnumerator = TestStringEnumerator(),
    ): StubElement<*> {
        @Suppress("DEPRECATION")
        val serializer = if (original is PsiFileStub<*>) original.type else original.stubType
        @Suppress("UNCHECKED_CAST")
        serializer as ObjectStubSerializer<StubElement<*>, StubElement<*>>
        val bytes = ByteArrayOutputStream()
        StubOutputStream(bytes, strings).use { serializer.serialize(original, it) }
        val restored = StubInputStream(ByteArrayInputStream(bytes.toByteArray()), strings).use { input ->
            serializer.deserialize(input, parent).also {
                assertEquals(-1, input.read(), "stub serializer 与 deserializer 必须消费相同字节")
            }
        }
        original.childrenStubs.forEach { roundTripStub(it, restored, strings) }
        return restored
    }

    private class TestStringEnumerator : AbstractStringEnumerator {
        private val values = linkedMapOf<String, Int>()
        private val strings = mutableListOf<String>()

        override fun enumerate(value: String?): Int {
            if (value == null) return 0
            return values.getOrPut(value) {
                strings += value
                strings.size
            }
        }

        override fun valueOf(idx: Int): String? = if (idx == 0) null else strings[idx - 1]
        override fun markCorrupted(): Unit = error("Unexpected persistent storage operation")
        override fun close(): Unit = error("Unexpected persistent storage operation")
        override fun isDirty(): Boolean = error("Unexpected persistent storage operation")
        override fun force(): Unit = error("Unexpected persistent storage operation")
    }

    /**
     * 验证没有参数列表 stub 的普通 compiled 函数仍会渲染空括号和 compiled body 占位。
     */
    @Test
    fun zeroArgCompiledNamedFunctionWithoutParameterListStillRendersParenthesesAndBody() {
        withCoreEnvironment {
            val rendered = renderManualFileStub { fileStub ->
                val functionStub = CangJieNamedFunctionStubImpl(
                    parent = fileStub,
                    element = CjStubElementTypes.FUNCTION,
                    nameRef = StringRef.fromString("toString"),
                    isTopLevel = true,
                    fqName = samplePackage.child(Name.identifier("toString")),
                    hasBlockBody = true,
                    hasBody = true,
                    hasTypeParameterListBeforeFunctionName = false,
                    origin = null,
                )
                createEmptyHeader(functionStub)
            }

            assertTrue(rendered.contains("func toString() { /* compiled code */ }"), rendered)
        }
    }

    /**
     * 验证 operator 函数在缺少参数列表 stub 时仍保留 `operator` 修饰符、空括号和 body 占位。
     */
    @Test
    fun zeroArgCompiledOperatorWithoutParameterListStillRendersParenthesesAndBody() {
        withCoreEnvironment {
            val rendered = renderManualFileStub { fileStub ->
                val functionStub = CangJieNamedFunctionStubImpl(
                    parent = fileStub,
                    element = CjStubElementTypes.FUNCTION,
                    nameRef = StringRef.fromString("-"),
                    isTopLevel = false,
                    fqName = null,
                    hasBlockBody = true,
                    hasBody = true,
                    hasTypeParameterListBeforeFunctionName = false,
                    origin = null,
                )
                createEmptyHeader(
                    functionStub,
                    modifierMask = computeModifierMask(operator = true),
                )
            }

            assertTrue(rendered.contains("operator func -() { /* compiled code */ }"), rendered)
        }
    }

    /**
     * 内部名 `*operator_unaryMinus` 必须还原为源码 `-`，不能原样出现在反编译文本中。
     */
    @Test
    fun unaryMinusOperatorInternalNameRendersAsMinusToken() {
        withCoreEnvironment {
            val rendered = renderManualFileStub { fileStub ->
                val functionStub = CangJieNamedFunctionStubImpl(
                    parent = fileStub,
                    element = CjStubElementTypes.FUNCTION,
                    nameRef = StringRef.fromString(OperatorNameConventions.UNARY_MINUS.asString()),
                    isTopLevel = false,
                    fqName = null,
                    hasBlockBody = true,
                    hasBody = true,
                    hasTypeParameterListBeforeFunctionName = false,
                    origin = null,
                )
                createEmptyHeader(
                    functionStub,
                    modifierMask = computeModifierMask(operator = true),
                )
            }

            assertTrue(rendered.contains("operator func -() { /* compiled code */ }"), rendered)
            assertFalse(rendered.contains("*operator_unaryMinus"), rendered)
        }
    }

    /**
     * 验证主函数和宏声明在没有参数列表 stub 时仍渲染符合仓颉语法的空参数列表。
     */
    @Test
    fun zeroArgCompiledMainAndMacroWithoutParameterListStillRenderParentheses() {
        withCoreEnvironment {
            val mainRendered = renderManualFileStub { fileStub ->
                val mainStub = CangJieMainFunctionStubImpl(
                    parent = fileStub,
                    element = CjStubElementTypes.MAIN_FUNC,
                    nameRef = StringRef.fromString("main"),
                    fqName = samplePackage.child(Name.identifier("main")),
                    origin = null,
                )
                createEmptyHeader(mainStub)
            }
            assertTrue(mainRendered.contains("main() { /* compiled code */ }"), mainRendered)

            val macroRendered = renderManualFileStub { fileStub ->
                val macroStub = CangJieMacroStubImpl(
                    parent = fileStub,
                    element = CjStubElementTypes.MACRO,
                    nameRef = StringRef.fromString("trace"),
                    isTopLevel = true,
                    fqName = samplePackage.child(Name.identifier("trace")),
                    hasBlockBody = true,
                    hasBody = true,
                    hasTypeParameterListBeforeFunctionName = false,
                    origin = null,
                )
                createEmptyHeader(macroStub)
            }
            assertTrue(macroRendered.contains("macro trace() { /* compiled code */ }"), macroRendered)
        }
    }

    /**
     * 验证 foreign callable 只渲染声明头和 `foreign` 修饰符，不添加 compiled body 占位。
     */
    @Test
    fun foreignCallableRemainsBodylessAndRendersForeignModifier() {
        withCoreEnvironment {
            val rendered = renderManualFileStub { fileStub ->
                val functionStub = CangJieNamedFunctionStubImpl(
                    parent = fileStub,
                    element = CjStubElementTypes.FUNCTION,
                    nameRef = StringRef.fromString("ccall"),
                    isTopLevel = true,
                    fqName = samplePackage.child(Name.identifier("ccall")),
                    hasBlockBody = false,
                    hasBody = false,
                    hasTypeParameterListBeforeFunctionName = false,
                    origin = null,
                )
                createEmptyHeader(
                    functionStub,
                    modifierMask = computeModifierMask(foreign = true),
                )
            }

            assertTrue(rendered.contains("foreign func ccall()"), rendered)
            assertFalse(rendered.contains("foreign func ccall() { /* compiled code */ }"), rendered)
        }
    }

    /**
     * 验证接口抽象成员反编译后保持无函数体形式。
     */
    @Test
    fun interfaceAbstractMemberRemainsBodyless() {
        withCoreEnvironment { environment ->
            val sourceFile = PsiFileFactory.getInstance(environment.project).createFileFromText(
                "sample.cj",
                CangJieFileType.INSTANCE,
                """
                package sample

                interface Box {
                    func toString(): Int64
                }
                """.trimIndent(),
            ) as CjFile
            val rendered = buildDecompiledText(sourceFile.calcStubTree().root as CangJieFileStubImpl)

            assertTrue(rendered.contains("func toString(): Int64"), rendered)
            assertFalse(rendered.contains("func toString(): Int64 { /* compiled code */ }"), rendered)
        }
    }

    /**
     * 验证抽象类型和 open 成员在反编译文本中保留 modality 修饰符。
     */
    @Test
    fun compiledAbstractClassAndOpenMemberRenderModalityModifiers() {
        withCoreEnvironment {
            val rendered = renderManualFileStub { fileStub ->
                val classStub = CangJieClassStubImpl(
                    type = CjStubElementTypes.CLASS,
                    parent = fileStub,
                    qualifiedName = StringRef.fromString(sampleClassFqName.asString()),
                    classId = null,
                    name = StringRef.fromString(sampleClassName.asString()),
                    superNames = emptyArray(),
                )
                createEmptyHeader(
                    classStub,
                    modifierMask = computeModifierMask(abstract = true),
                )
                val classBodyStub = CangJiePlaceHolderStubImpl<CjAbstractClassBody>(
                    classStub,
                    CjStubElementTypes.CLASS_BODY,
                )
                val functionStub = CangJieNamedFunctionStubImpl(
                    parent = classBodyStub,
                    element = CjStubElementTypes.FUNCTION,
                    nameRef = StringRef.fromString("grow"),
                    isTopLevel = false,
                    fqName = null,
                    hasBlockBody = true,
                    hasBody = true,
                    hasTypeParameterListBeforeFunctionName = false,
                    origin = null,
                )
                createEmptyHeader(
                    functionStub,
                    modifierMask = computeModifierMask(open = true),
                )
            }

            assertTrue(rendered.contains("abstract class Widget {"), rendered)
            assertTrue(rendered.contains("open func grow() { /* compiled code */ }"), rendered)
        }
    }

    /**
     * 验证没有显式 accessor stub 的 compiled `prop` 仍会渲染 getter body 占位。
     */
    @Test
    fun compiledPropertyWithoutAccessorStubsStillRendersGetterBody() {
        withCoreEnvironment {
            val rendered = renderManualStructStub { classBodyStub ->
                val propertyStub = CangJiePropertyStubImpl(
                    parent = classBodyStub,
                    name = StringRef.fromString("first"),
                    fqName = sampleStructFqName.child(Name.identifier("first")),
                )
                createEmptyHeader(propertyStub)
                createBasicTypeReference(propertyStub, "Int64")
            }

            assertTrue(rendered.contains("prop first"), rendered)
            assertTrue(rendered.contains("get() { /* compiled code */ }"), rendered)
        }
    }

    /**
     * 验证没有显式 accessor stub 的 `mut prop` 会同时渲染 getter 与 setter body 占位。
     */
    @Test
    fun compiledMutPropertyWithoutAccessorStubsStillRendersGetterAndSetterBodies() {
        withCoreEnvironment {
            val rendered = renderManualStructStub { classBodyStub ->
                val propertyStub = CangJiePropertyStubImpl(
                    parent = classBodyStub,
                    name = StringRef.fromString("state"),
                    fqName = sampleStructFqName.child(Name.identifier("state")),
                )
                createEmptyHeader(
                    propertyStub,
                    modifierMask = computeModifierMask(mut = true),
                )
                createBasicTypeReference(propertyStub, "Int64")
            }

            assertTrue(rendered.contains("mut prop state: Int64 {"), rendered)
            assertTrue(rendered.contains("get() { /* compiled code */ }"), rendered)
            assertTrue(rendered.contains("set(value) { /* compiled code */ }"), rendered)
            assertFalse(rendered.contains("set(value: "), rendered)
        }
    }

    /**
     * 验证显式 setter accessor 只渲染参数名，不在 accessor 形参位置补类型文本。
     */
    @Test
    fun propertyAccessorRendersUntypedSetterParameter() {
        withCoreEnvironment {
            val rendered = renderManualStructStub { classBodyStub ->
                val propertyStub = CangJiePropertyStubImpl(
                    parent = classBodyStub,
                    name = StringRef.fromString("state"),
                    fqName = sampleStructFqName.child(Name.identifier("state")),
                )
                createEmptyHeader(
                    propertyStub,
                    modifierMask = computeModifierMask(mut = true),
                )
                createBasicTypeReference(propertyStub, "Int64")
                val propertyBodyStub = CangJiePlaceHolderStubImpl<org.cangnova.cangjie.psi.CjPropertyBody>(
                    propertyStub,
                    CjStubElementTypes.PROPERTY_BODY,
                )
                val getterStub = CangJiePropertyAccessorStubImpl(
                    parent = propertyBodyStub,
                    isGetter = true,
                    hasBody = true,
                    hasBlockBody = true,
                )
                // 与 createPropertyAccessorStub(isGetter=true) 对齐：getter 也有空参数列表
                CangJiePlaceHolderStubImpl<org.cangnova.cangjie.psi.CjParameterList>(
                    getterStub,
                    CjStubElementTypes.VALUE_PARAMETER_LIST,
                )
                val setterStub = CangJiePropertyAccessorStubImpl(
                    parent = propertyBodyStub,
                    isGetter = false,
                    hasBody = true,
                    hasBlockBody = true,
                )
                val parameterListStub = CangJiePlaceHolderStubImpl<org.cangnova.cangjie.psi.CjParameterList>(
                    setterStub,
                    CjStubElementTypes.VALUE_PARAMETER_LIST,
                )
                val setterParameterStub = CangJieParameterStubImpl(
                    parent = parameterListStub,
                    fqName = null,
                    name = StringRef.fromString("newValue"),
                    isMutable = false,
                    hasLetOrVar = false,
                    hasDefaultValue = false,
                    isNamed = false,
                    functionTypeParameterName = null,
                )
                // 对齐 parseValueParameter：value parameter 始终有空 ANNOTATIONS
                CangJiePlaceHolderStubImpl<org.cangnova.cangjie.psi.CjAnnotations>(
                    setterParameterStub,
                    CjStubElementTypes.ANNOTATIONS,
                )
            }

            assertTrue(rendered.contains("get() { /* compiled code */ }"), rendered)
            assertTrue(rendered.contains("set(newValue) { /* compiled code */ }"), rendered)
            assertFalse(rendered.contains("set(newValue: "), rendered)
        }
    }

    /**
     * extend 的基本类型目标（如 Unit）不得用反引号包裹。
     */
    @Test
    fun extendBasicTypeDoesNotWrapTypeKeywordWithBackticks() {
        withCoreEnvironment {
            val rendered = renderManualFileStub { fileStub ->
                val extendStub = org.cangnova.cangjie.psi.stubs.impl.CangJieExtendStubImpl(
                    type = CjStubElementTypes.EXTEND,
                    parent = fileStub,
                    qualifiedName = StringRef.fromString("${samplePackage.asString()}.Unit"),
                    classId = null,
                    name = StringRef.fromString("Unit"),
                    extendIdRef = StringRef.fromString("Unit"),
                    superNames = emptyArray(),
                    receiverTypeName = "Unit",
                )
                createEmptyHeader(extendStub)
                createBasicTypeReference(extendStub, "Unit")
                CangJiePlaceHolderStubImpl<CjAbstractClassBody>(
                    extendStub,
                    CjStubElementTypes.CLASS_BODY,
                )
            }

            assertTrue(rendered.contains("extend Unit {"), rendered)
            assertFalse(rendered.contains("extend `Unit`"), rendered)
        }
    }

    /**
     * 创建测试用仓颉核心环境，并在执行结束后按 IntelliJ 规则释放 disposable。
     */
    private fun withCoreEnvironment(action: (CangJieCoreEnvironment) -> Unit) {
        val disposable = Disposer.newDisposable("DecompiledTextBuilderTest")
        try {
            val environment = CangJieCoreEnvironment.createForTests(disposable)
            action(environment)
        } finally {
            val application = ApplicationManager.getApplication()
            if (application != null) {
                application.runWriteAction {
                    Disposer.dispose(disposable)
                }
            } else {
                Disposer.dispose(disposable)
            }
        }
    }

    /**
     * 构造带 sample package 的手工文件 stub，并返回反编译文本。
     */
    private fun renderManualFileStub(build: (CangJieFileStubImpl) -> Unit): String {
        val fileStub = CangJieFileStubImpl.forFile(samplePackage)
        build(fileStub)
        return buildDecompiledText(fileStub)
    }

    /**
     * 构造 sample struct 及其 class body stub，并把 body 交给调用方填充成员。
     */
    private fun renderManualStructStub(build: (CangJiePlaceHolderStubImpl<CjAbstractClassBody>) -> Unit): String {
        return renderManualFileStub { fileStub ->
            val structStub = CangJieStructStubImpl(
                type = CjStubElementTypes.STRUCT,
                parent = fileStub,
                qualifiedName = StringRef.fromString(sampleStructFqName.asString()),
                classId = null,
                name = StringRef.fromString(sampleStructName.asString()),
                superNames = emptyArray(),
            )
            createEmptyHeader(structStub)
            val classBodyStub = CangJiePlaceHolderStubImpl<CjAbstractClassBody>(
                structStub,
                CjStubElementTypes.CLASS_BODY,
            )
            build(classBodyStub)
        }
    }

    /**
     * 为手工声明 stub 创建空注解列表和修饰符列表头部。
     */
    private fun createEmptyHeader(parent: StubElement<*>, modifierMask: Long = 0L) {
        CangJiePlaceHolderStubImpl<CjAnnotations>(parent, CjStubElementTypes.ANNOTATIONS)
        CangJieModifierListStubImpl(parent, modifierMask, CjStubElementTypes.MODIFIER_LIST)
    }

    /**
     * 在指定声明 stub 下创建一个基础类型引用 stub。
     */
    private fun createBasicTypeReference(parent: StubElement<*>, typeName: String) {
        val typeReferenceStub = CangJiePlaceHolderStubImpl<org.cangnova.cangjie.psi.CjTypeReference>(
            parent,
            CjStubElementTypes.TYPE_REFERENCE,
        )
        CangJieBasicTypeStubImpl(typeReferenceStub, typeName)
    }

    /**
     * 根据测试需要的修饰符布尔值构造 PSI stub 使用的修饰符 bit mask。
     */
    private fun computeModifierMask(
        operator: Boolean = false,
        foreign: Boolean = false,
        mut: Boolean = false,
        open: Boolean = false,
        abstract: Boolean = false,
    ): Long {
        return ModifierMaskUtils.computeMask(
            hasModifier = { modifier ->
                when (modifier) {
                    CjTokens.OPERATOR_KEYWORD -> operator
                    CjTokens.MUT_KEYWORD -> mut
                    CjTokens.OPEN_KEYWORD -> open
                    CjTokens.ABSTRACT_KEYWORD -> abstract
                    else -> false
                }
            },
            hasAdditionalModifier = { keyword ->
                keyword == CjTokens.FOREIGN_KEYWORD && foreign
            },
        )
    }

    private companion object {
        /** 手工 stub 测试使用的示例包名。 */
        val samplePackage: FqName = FqName("sample")

        /** 手工 class stub 测试使用的示例类名。 */
        val sampleClassName: Name = Name.identifier("Widget")

        /** 手工 class stub 测试使用的示例类全限定名。 */
        val sampleClassFqName: FqName = samplePackage.child(sampleClassName)

        /** 手工 struct stub 测试使用的示例结构体名。 */
        val sampleStructName: Name = Name.identifier("Box")

        /** 手工 struct stub 测试使用的示例结构体全限定名。 */
        val sampleStructFqName: FqName = samplePackage.child(sampleStructName)
    }
}
