package org.cangnova.cangjie.analysis.decompiled.psi.text

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.analysis.decompiler.stub.COMPILED_DEFAULT_INITIALIZER
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.deepCopy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 合法声明模式从脱离源码 AST 的 stub 渲染，再重新解析；构造器、绑定和嵌套层次不能丢失。
 * 这里只检验语法/stub/text 合同，不把重新解析等同于正文语义解析或官方 CJO 产物。
 */
class DeclarationPatternTextTest {
    @Test
    fun bindingAndWildcardRetainTheirDistinctNames() {
        withCoreEnvironment { environment ->
            val source = parse(environment, """
                package patterns
                let visible: Int64 = 7
                let _: Int64 = 8
            """.trimIndent())
            val rendered = renderDetached(source)
            assertTrue(rendered.contains("let visible: Int64"), rendered)
            assertTrue(rendered.contains("let _: Int64"), rendered)
            val reparsed = parse(environment, rendered)
            val declarations = PsiTreeUtil.collectElementsOfType(reparsed, CjPatternVariable::class.java)
                .sortedBy { it.textOffset }
            assertEquals(listOf("binding(visible)", "wildcard"), declarations.map { shape(requireNotNull(it.pattern)) })
            declarations.forEach { assertEquals(COMPILED_DEFAULT_INITIALIZER, it.initializer?.text) }
        }
    }

    @Test
    fun recursiveTupleKeepsAllBindingsAndWildcardInOrder() {
        withCoreEnvironment { environment ->
            val source = parse(environment, """
                package patterns
                let (汉, (_, last)): (Int64, (Int64, Int64)) = (1, (2, 3))
            """.trimIndent())
            val expected = patternShape(source)
            assertEquals("tuple(binding(汉),tuple(wildcard,binding(last)))", expected)
            val rendered = renderDetached(source)
            assertTrue(rendered.contains("let (汉, (_, last))"), rendered)
            val reparsed = parse(environment, rendered)
            assertEquals(expected, patternShape(reparsed))
            val declaration = patternVariable(reparsed)
            assertEquals(COMPILED_DEFAULT_INITIALIZER, declaration.initializer?.text)
            assertEquals("(Int64, (Int64, Int64))", declaration.typeReference?.text)
        }
    }

    @Test
    fun singleConstructorEnumKeepsItsReferenceAndNestedTupleArgument() {
        withCoreEnvironment { environment ->
            val source = parse(environment, """
                package patterns
                enum Single {
                    | Only((Int64, Int64))
                }
                let Only((payload, _)): Single = Only((7, 8))
            """.trimIndent())
            val expected = patternShape(source)
            assertEquals("enum(Only;tuple(binding(payload),wildcard))", expected)
            val rendered = renderDetached(source)
            assertTrue(rendered.contains("let Only((payload, _))"), rendered)
            val reparsed = parse(environment, rendered)
            assertEquals(expected, patternShape(reparsed))
            val declaration = patternVariable(reparsed)
            assertEquals("Single", declaration.typeReference?.text)
            assertEquals(COMPILED_DEFAULT_INITIALIZER, declaration.initializer?.text)
        }
    }

    @Test
    fun constAndVarPatternDeclarationsKeepTheirOwnKeywords() {
        withCoreEnvironment { environment ->
            val source = parse(environment, """
                package patterns
                const (constantLeft, constantRight): (Int64, Int64) = (1, 2)
                var (mutableLeft, mutableRight): (Int64, Int64) = (3, 4)
            """.trimIndent())
            val before = PsiTreeUtil.collectElementsOfType(source, CjPatternVariable::class.java)
                .sortedBy { it.textOffset }.map { shape(requireNotNull(it.pattern)) }
            assertEquals(listOf("tuple(binding(constantLeft),binding(constantRight))",
                "tuple(binding(mutableLeft),binding(mutableRight))"), before)
            val rendered = renderDetached(source)
            assertTrue(rendered.contains("const (constantLeft, constantRight)"), rendered)
            assertTrue(rendered.contains("var (mutableLeft, mutableRight)"), rendered)
            val reparsed = parse(environment, rendered)
            val declarations = PsiTreeUtil.collectElementsOfType(reparsed, CjPatternVariable::class.java).sortedBy { it.textOffset }
            assertEquals(before, declarations.map { shape(requireNotNull(it.pattern)) })
            assertTrue(declarations[0].isConst)
            assertFalse(declarations[0].isVar)
            assertFalse(declarations[1].isConst)
            assertTrue(declarations[1].isVar)
        }
    }

    /** deepCopy 后的 file stub 未绑定原 PSI；renderer 必须仅消费该 stub 的真实模式子树。 */
    private fun renderDetached(source: CjFile): String {
        val detached = (source.calcStubTree().root as CangJieFileStubImpl).deepCopy()
        assertNull(detached.psi)
        return buildDecompiledText(detached)
    }

    private fun parse(environment: CangJieCoreEnvironment, text: String): CjFile {
        val file = PsiFileFactory.getInstance(environment.project)
            .createFileFromText("declarationPatterns.cj", CangJieFileType.INSTANCE, text) as CjFile
        assertTrue(PsiTreeUtil.collectElementsOfType(file, PsiErrorElement::class.java).isEmpty(), text)
        return file
    }

    private fun patternVariable(file: CjFile): CjPatternVariable =
        PsiTreeUtil.collectElementsOfType(file, CjPatternVariable::class.java).single()

    private fun patternShape(file: CjFile): String = shape(requireNotNull(patternVariable(file).pattern))

    private fun shape(pattern: CjCasePatternElement): String = when (pattern) {
        is CjBindingPattern -> "binding(${pattern.name})"
        is CjWildcardPattern -> "wildcard"
        is CjTuplePattern -> "tuple(${pattern.patterns.joinToString(",") { shape(it) }})"
        is CjEnumPattern -> "enum(${requireNotNull(pattern.expression).text};${pattern.patterns.joinToString(",") { shape(it) }})"
        else -> error("Unexpected declaration pattern: ${pattern.javaClass.name}")
    }

    private fun withCoreEnvironment(action: (CangJieCoreEnvironment) -> Unit) {
        val disposable = Disposer.newDisposable("DeclarationPatternTextTest")
        try {
            action(CangJieCoreEnvironment.createForTests(disposable))
        } finally {
            val application = ApplicationManager.getApplication()
            if (application == null) Disposer.dispose(disposable)
            else application.runWriteAction { Disposer.dispose(disposable) }
        }
    }
}
