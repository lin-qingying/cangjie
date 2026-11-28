package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateStatus
import org.cangnova.cangjie.analysis.api.evaluation.CaCollectionCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaScalarCompileTimeValue
import org.cangnova.cangjie.analysis.api.evaluation.CaScalarValueKind
import org.cangnova.cangjie.analysis.api.evaluation.CaTupleCompileTimeValue
import org.cangnova.cangjie.analysis.api.interop.CaInteropBackend
import org.cangnova.cangjie.analysis.api.interop.CaInteropCallingConvention
import org.cangnova.cangjie.annotations.AnnotationVersionSupportStatus
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationOrigin
import org.cangnova.cangjie.annotations.CangjiePlatformAnnotationKind
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjSimpleNameExpression
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
    /**
     * 使用 standalone CFIR 配置运行组件级 Analysis API 用例。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证补全候选、引用缩短计划和 import 优化计划在同一测试文件中的协同结果。
     */
    @Test
    fun completionCandidateDecisions(mainFile: CjFile, testServices: TestServices) {
        val functions = testServices.allNamedFunctions()
        val directCandidate = functions.single { it.name == "directCandidate" }
        val importCandidate = functions.single { it.name == "needsImport" }
        val hiddenCandidate = functions.single { it.name == "hiddenCandidate" }
        val useSite = functions.single { it.name == "consume" }
        val qualifiedCalls = PsiTreeUtil.findChildrenOfType(mainFile, CjDotQualifiedExpression::class.java)
        val importQualifiedCall = qualifiedCalls
            .single { it.text == "provider.needsImport()" }

        analyzeForTest(mainFile) {
            val directDecision = directCandidate.symbol.checkCompletionCandidate(useSite)
            assertEquals(CaCompletionCandidateStatus.DIRECT, directDecision.status)
            assertNull(directDecision.requiredImport)

            val importDecision = importCandidate.symbol.checkCompletionCandidate(useSite)
            assertEquals(CaCompletionCandidateStatus.REQUIRES_IMPORT, importDecision.status)
            assertEquals("completion.provider.needsImport", importDecision.requiredImport.toString())
            val requiredImport = requireNotNull(importDecision.requiredImport)

            val hiddenDecision = hiddenCandidate.symbol.checkCompletionCandidate(useSite)
            assertEquals(CaCompletionCandidateStatus.HIDDEN, hiddenDecision.status)
            assertNull(hiddenDecision.requiredImport)

            val shorteningPlan = mainFile.collectReferenceShorteningPlan()
            val importShorteningOperation = shorteningPlan.operations.single { it.expression.text == importQualifiedCall.text }
            assertEquals("needsImport", importShorteningOperation.shortName.asString())
            assertEquals(CaCompletionCandidateStatus.REQUIRES_IMPORT, importShorteningOperation.decision.status)
            assertEquals(requiredImport, importShorteningOperation.decision.requiredImport)

            val importShorteningCommand = importQualifiedCall.collectReferenceShorteningsInElement()
            assertEquals(listOf(importQualifiedCall.text), importShorteningCommand.operations.map { it.expression.text })
            assertEquals(listOf(requiredImport), importShorteningCommand.importsToAdd.toList())

            val importPlan = mainFile.collectImportOptimizationPlan()
            assertEquals(listOf(requiredImport), importPlan.missingImports)
        }
    }

    /**
     * 短名可达性绑定到**位置**作用域：局部遮蔽、局部候选与成员候选（任务 5.4）。
     *
     * 旧实现只查文件 lookup scope，于是三件事都判不出来：局部遮蔽看不见（顶层同名符号照样
     * 报 DIRECT）、局部候选落 UNKNOWN、成员候选落 UNKNOWN。三条断言各自对应一件：
     * ①同名局部变量在位置上先绑定 → 顶层符号**不再可达**（HIDDEN，不是 REQUIRES_IMPORT：
     * 加 import 也改不了「短名已经绑到别的声明」这个事实，cjc 实测 `let Foo = 3` 之后连
     * 类型位置写 `Foo` 都报 `'Foo' is not a type`）；
     * ②局部候选自身在位置上就是第一个绑定 → DIRECT；
     * ③成员候选在位置上经隐式接收者所在成员层绑定 → DIRECT。
     */
    @Test
    fun completionCandidateShadowing(mainFile: CjFile, testServices: TestServices) {
        val functions = testServices.allNamedFunctions()
        // 同名顶层函数有两个（shadowed.cj 与 provider.cj），按文件区分：本用例要的是被遮蔽的那个。
        val topLevelShadowed = functions.single {
            it.name == "shadowedByLocal" && it.containingFile.name == "shadowed.cj"
        }
        val memberCandidate = functions.single { it.name == "memberCandidate" }
        val memberUseSite = functions.single { it.name == "useMember" }
        // 遮蔽位置取 `consume` 里**使用**局部变量的那个引用：绑定名本身也是简单名（绑定模式），
        // 按文件序取最后一个即使用点，同时显式排除绑定模式，免得定语依赖书写顺序。
        val consumeUseSite = functions.single { it.name == "consume" }
        // 绑定名与使用点都是简单名，按文件序取最后一个即使用点。
        val shadowingPosition = PsiTreeUtil
            .findChildrenOfType(consumeUseSite, CjSimpleNameExpression::class.java)
            .last { it.referencedName == "shadowedByLocal" }
        val memberPosition = PsiTreeUtil
            .findChildrenOfType(memberUseSite, CjSimpleNameExpression::class.java)
            .single { it.referencedName == "memberCandidate" }

        analyzeForTest(mainFile) {
            val topLevelDecision = topLevelShadowed.symbol.checkCompletionCandidate(shadowingPosition)
            assertEquals(
                CaCompletionCandidateStatus.HIDDEN,
                topLevelDecision.status,
                "同名局部变量在位置上先绑定，顶层短名就不可达；给出 REQUIRES_IMPORT 会让用户插入一个解析到别处的短名",
            )

            val localSymbol = requireNotNull(shadowingPosition.resolveToSymbol()) {
                "位置上的 shadowedByLocal 必须解析到局部变量（局部遮蔽顶层同名函数）"
            }
            val localDecision = localSymbol.checkCompletionCandidate(shadowingPosition)
            assertEquals(
                CaCompletionCandidateStatus.DIRECT,
                localDecision.status,
                "局部候选自身就是位置作用域的首个绑定",
            )

            val memberDecision = memberCandidate.symbol.checkCompletionCandidate(memberPosition)
            assertEquals(
                CaCompletionCandidateStatus.DIRECT,
                memberDecision.status,
                "成员候选经隐式接收者所在的成员层绑定，不应落到 UNKNOWN",
            )

            // ④ 缩短回归（5.6①「旧三态调用者」）：被局部遮蔽的短名不得进入缩短规划。
            // 只把状态判成 HIDDEN 还不够——旧三态调用者若按 shouldHide 的反面放行，就会把
            // 「provider.shadowedByLocal()」缩短成一个解析到局部变量的短名。正对照同样必要：
            // 没有它，「全都不缩短」也能让上一条断言变绿。
            val plan = mainFile.collectReferenceShorteningPlan()
            assertTrue(
                plan.operations.none { it.expression.text == "provider.shadowedByLocal()" },
                "短名被局部遮蔽时不得缩短：加 import 也改不了短名绑定，写下的短名会解析到局部变量",
            )
            assertTrue(
                plan.operations.any { it.expression.text == "provider.plain()" },
                "未遮蔽的限定引用必须照常进入缩短规划（正对照）",
            )
        }
    }

    /** 源码 type reference 只能通过当前文件导入的包名或别名访问 package member。 */
    @Test
    fun packageQualifierTypeResolution(mainFile: CjFile) {
        val diagnostics = analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }
        val unresolvedTypeNames = diagnostics.filter { it.factoryName == "UNDECLARED_TYPE_NAME" }

        assertEquals(
            listOf("completion"),
            unresolvedTypeNames.flatMap { diagnostic ->
                diagnostic.textRanges.map { range -> mainFile.text.substring(range.startOffset, range.endOffset) }
            },
            diagnostics.joinToString { diagnostic -> "${diagnostic.factoryName}@${diagnostic.psi.text}" },
        )
    }

    /**
     * 验证 CFIR 编译期求值器能返回标量、元组和集合等 Analysis API 值对象。
     */
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

    /**
     * 验证 obsolete original PSI provider 只记录显式写入的拷贝来源，不污染原始 PSI。
     */
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

    /**
     * 验证 C 互操作信息在 PSI 入口和 symbol 入口上保持一致。
     */
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

            val cAnnotation = nativeStruct.classSymbol.annotations.first { it.builtInKind == BuiltInAnnotationKind.C }
            assertEquals(CangjieAnnotationOrigin.LANGUAGE_BUILT_IN, cAnnotation.origin)
            assertNull(cAnnotation.platformKind)
            assertEquals(AnnotationVersionSupportStatus.SUPPORTED, cAnnotation.versionSupport)

            val structSymbolInfo = nativeStruct.classSymbol.getInteropInfo()
            assertNotNull(structSymbolInfo, "@C struct symbol should expose backend interop info")
            assertEquals(structInfo.backends, structSymbolInfo!!.backends)
            assertEquals(structInfo.ffiAnnotationNames, structSymbolInfo.ffiAnnotationNames)

            val psiInfo = function.getInteropInfo()
            assertNotNull(psiInfo, "foreign function PSI should expose C interop info")
            // Official SetForeignABIAttr materializes the effective C ABI for
            // `foreign`; `isForeignDeclaration` remains the source-level fact.
            assertEquals(listOf(CaInteropBackend.C), psiInfo!!.backends)
            assertTrue(psiInfo.isForeignDeclaration)
            assertTrue(psiInfo.isFastNative)
            assertEquals("native_sum", psiInfo.externalName)
            assertEquals(CaInteropCallingConvention.CDECL, psiInfo.callingConvention)
            assertEquals(listOf("ForeignName", "CallingConv"), psiInfo.ffiAnnotationNames)
            assertTrue(
                psiInfo.ffiAnnotations.any {
                it.builtInKind == BuiltInAnnotationKind.FOREIGN_NAME ||
                    (it.platformKind == CangjiePlatformAnnotationKind.FOREIGN_NAME &&
                        it.origin == org.cangnova.cangjie.annotations.CangjieAnnotationOrigin.PLATFORM_DERIVED)
                },
                "Analysis API must expose resolved platform FFI annotations, not only language builtins",
            )

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

/**
 * 收集当前测试模块结构中的全部命名函数 PSI。
 */
private fun TestServices.allNamedFunctions(): List<CjNamedFunction> {
    return cjTestModuleStructure.allCjFiles.flatMap { file ->
        PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java)
    }
}

/**
 * 在文件中按名称查找唯一的命名函数。
 */
private fun CjFile.namedFunction(name: String): CjNamedFunction {
    return PsiTreeUtil.findChildrenOfType(this, CjNamedFunction::class.java)
        .single { it.name == name }
}

/**
 * 在文件中按名称查找唯一的类型声明。
 */
private fun CjFile.typeStatement(name: String): CjTypeStatement {
    return PsiTreeUtil.findChildrenOfType(this, CjTypeStatement::class.java)
        .single { it.name == name }
}

/**
 * 查找指定绑定模式变量的初始化表达式。
 */
private fun CjFile.initializerByBinding(name: String): CjExpression {
    val variable = PsiTreeUtil.findChildrenOfType(this, CjPatternVariable::class.java)
        .single { variable -> (variable.pattern as? CjBindingPattern)?.name == name }
    return variable.initializer
        ?: error("Binding `$name` has no initializer.")
}

/**
 * 断言表达式的编译期求值结果为指定种类的标量值。
 */
private fun CaSession.assertScalar(
    expression: CjExpression,
    expectedKind: CaScalarValueKind,
    expectedRenderedText: String,
) {
    val value = assertCompileTimeValue<CaScalarCompileTimeValue>(expression, expectedRenderedText)
    assertEquals(expectedKind, value.kind)
}

/**
 * 断言表达式能求值为指定 Analysis API 编译期值类型，并返回该类型实例继续做结构断言。
 */
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
