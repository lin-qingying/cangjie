package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateStatus
import org.cangnova.cangjie.analysis.api.evaluation.CaCollectionCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaScalarCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaScalarValueKind
import org.cangnova.cangjie.analysis.api.evaluation.CaTupleCompileTimeValue
import org.cangnova.cangjie.analysis.api.interop.CaInteropBackend
import org.cangnova.cangjie.analysis.api.interop.CaInteropCallingConvention
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CFIR Analysis API 组件的手写执行测试。
 *
 * shared generated suite 覆盖跨平台公开抽象；这里锁定 CFIR 专属执行路径：
 * 补全候选决策、编译期求值、C 互操作信息、obsolete original PSI 记录/回读。
 */
class AnalysisApiCfirComponentExecutionTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/components",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun completionCandidateDecisions(mainFile: CjFile, testServices: TestServices) {
        val functions = testServices.allNamedFunctions()
        val directCandidate = functions.single { it.name == "directCandidate" }
        val importCandidate = functions.single { it.name == "needsImport" }
        val hiddenCandidate = functions.single { it.name == "hiddenCandidate" }
        val useSite = functions.single { it.name == "consume" }
        val qualifiedCall = PsiTreeUtil.findChildrenOfType(mainFile, CjDotQualifiedExpression::class.java)
            .single { it.text == "completion.consumer.directCandidate()" }

        analyzeForTest(mainFile) {
            val directDecision = directCandidate.symbol.checkCompletionCandidate(useSite)
            assertEquals(CaCompletionCandidateStatus.DIRECT, directDecision.status)
            assertNull(directDecision.requiredImport)

            val importDecision = importCandidate.symbol.checkCompletionCandidate(useSite)
            assertEquals(CaCompletionCandidateStatus.REQUIRES_IMPORT, importDecision.status)
            assertEquals("completion.provider.needsImport", importDecision.requiredImport.toString())

            val hiddenDecision = hiddenCandidate.symbol.checkCompletionCandidate(useSite)
            assertEquals(CaCompletionCandidateStatus.HIDDEN, hiddenDecision.status)
            assertNull(hiddenDecision.requiredImport)

            val shorteningOperation = mainFile.collectReferenceShorteningPlan().operations
                .single { it.expression.text == qualifiedCall.text }
            assertEquals("directCandidate", shorteningOperation.shortName.asString())
            assertEquals(directDecision.status, shorteningOperation.decision.status)
            assertEquals(directDecision.requiredImport, shorteningOperation.decision.requiredImport)

            val shorteningCommand = qualifiedCall.collectReferenceShorteningsInElement()
            assertEquals(listOf(qualifiedCall.text), shorteningCommand.operations.map { it.expression.text })
            assertTrue(shorteningCommand.importsToAdd.isEmpty())

            val importPlan = mainFile.collectImportOptimizationPlan()
            assertTrue(importPlan.missingImports.isEmpty())
        }
    }

    @Test
    fun compileTimeEvaluator(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            assertScalar(
                expression = mainFile.initializerByBinding("scalarValue"),
                expectedKind = CaScalarValueKind.INTEGER,
                expectedRenderedText = "42",
            )
            assertScalar(
                expression = mainFile.initializerByBinding("stringValue"),
                expectedKind = CaScalarValueKind.STRING,
                expectedRenderedText = "hello",
            )

            val tuple = assertCompileTimeValue<CaTupleCompileTimeValue>(
                mainFile.initializerByBinding("tupleValue"),
                expectedRenderedText = "(1, hello)",
            )
            assertEquals(listOf("1", "hello"), tuple.elements.map { it.renderedText })

            val collection = assertCompileTimeValue<CaCollectionCompileTimeValue>(
                mainFile.initializerByBinding("collectionValue"),
                expectedRenderedText = "[1, 2, 3]",
            )
            assertEquals(listOf("1", "2", "3"), collection.elements.map { it.renderedText })

            assertScalar(
                expression = mainFile.initializerByBinding("parenthesizedValue"),
                expectedKind = CaScalarValueKind.INTEGER,
                expectedRenderedText = "99",
            )
        }
    }

    @Test
    fun originalPsiProvider(mainFile: CjFile) {
        val originalDeclaration = mainFile.namedFunction("tracked")
        val copiedFile = mainFile.copy() as CjFile
        val copiedDeclaration = copiedFile.namedFunction("tracked")

        analyzeForTest(mainFile) {
            @Suppress("DEPRECATION")
            run {
                assertNull(copiedFile.getOriginalCjFile())
                assertNull(copiedDeclaration.getOriginalDeclaration())

                copiedFile.recordOriginalCjFile(mainFile)
                copiedDeclaration.recordOriginalDeclaration(originalDeclaration)

                assertSame(mainFile, copiedFile.getOriginalCjFile())
                assertSame(originalDeclaration, copiedDeclaration.getOriginalDeclaration())
                assertNull(mainFile.getOriginalCjFile())
                assertNull(originalDeclaration.getOriginalDeclaration())
            }
        }
    }

    @Test
    fun cInteropInfo(mainFile: CjFile) {
        val nativeStruct = mainFile.typeStatement("NativeBox")
        val function = mainFile.namedFunction("nativeSum")

        analyzeForTest(mainFile) {
            val structInfo = nativeStruct.getInteropInfo()
            assertNotNull(structInfo, "@C struct PSI should expose backend interop info")
            assertEquals(listOf(CaInteropBackend.C), structInfo!!.backends)
            assertFalse(structInfo.isForeignDeclaration)
            assertEquals(listOf("C"), structInfo.ffiAnnotationNames)

            val structSymbolInfo = nativeStruct.classSymbol.getInteropInfo()
            assertNotNull(structSymbolInfo, "@C struct symbol should expose backend interop info")
            assertEquals(structInfo.backends, structSymbolInfo!!.backends)
            assertEquals(structInfo.ffiAnnotationNames, structSymbolInfo.ffiAnnotationNames)

            val psiInfo = function.getInteropInfo()
            assertNotNull(psiInfo, "foreign function PSI should expose C interop info")
            assertTrue(psiInfo!!.backends.isEmpty())
            assertTrue(psiInfo.isForeignDeclaration)
            assertTrue(psiInfo.isFastNative)
            assertEquals("native_sum", psiInfo.externalName)
            assertEquals(CaInteropCallingConvention.CDECL, psiInfo.callingConvention)
            assertEquals(listOf("ForeignName", "CallingConv"), psiInfo.ffiAnnotationNames)

            val symbolInfo = function.symbol.getInteropInfo()
            assertNotNull(symbolInfo, "foreign function symbol should expose the same C interop info")
            assertEquals(psiInfo.backends, symbolInfo!!.backends)
            assertEquals(psiInfo.isForeignDeclaration, symbolInfo.isForeignDeclaration)
            assertEquals(psiInfo.isFastNative, symbolInfo.isFastNative)
            assertEquals(psiInfo.externalName, symbolInfo.externalName)
            assertEquals(psiInfo.callingConvention, symbolInfo.callingConvention)
            assertEquals(psiInfo.ffiAnnotationNames, symbolInfo.ffiAnnotationNames)
        }
    }
}

private fun TestServices.allNamedFunctions(): List<CjNamedFunction> {
    return cjTestModuleStructure.allCjFiles.flatMap { file ->
        PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java)
    }
}

private fun CjFile.namedFunction(name: String): CjNamedFunction {
    return PsiTreeUtil.findChildrenOfType(this, CjNamedFunction::class.java)
        .single { it.name == name }
}

private fun CjFile.typeStatement(name: String): CjTypeStatement {
    return PsiTreeUtil.findChildrenOfType(this, CjTypeStatement::class.java)
        .single { it.name == name }
}

private fun CjFile.initializerByBinding(name: String): CjExpression {
    val variable = PsiTreeUtil.findChildrenOfType(this, CjPatternVariable::class.java)
        .single { variable -> (variable.pattern as? CjBindingPattern)?.name == name }
    return variable.initializer
        ?: error("Binding `$name` has no initializer.")
}

private fun CaSession.assertScalar(
    expression: CjExpression,
    expectedKind: CaScalarValueKind,
    expectedRenderedText: String,
) {
    val value = assertCompileTimeValue<CaScalarCompileTimeValue>(expression, expectedRenderedText)
    assertEquals(expectedKind, value.kind)
}

private inline fun <reified T : CaCompileTimeValue> CaSession.assertCompileTimeValue(
    expression: CjExpression,
    expectedRenderedText: String,
): T {
    val value = expression.evaluate()
    assertTrue(value is T, "Expected ${T::class.simpleName} for `${expression.text}`, got ${value?.let { it::class.simpleName }}")
    value as T
    assertEquals(expectedRenderedText, value.renderedText)
    return value
}
