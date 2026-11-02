package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjInterface
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 库声明导航回归：`extend` 的接收者/父类型与 std.core extend 成员都必须回链到反编译 PSI。
 *
 * 覆盖对齐 Kotlin `KaSymbol.psi` 的两条契约：
 * 1. `LIBRARY` origin 的符号必须能给出 `.cjo` 反编译 PSI（`ToString`、std.core `toString`）；
 * 2. `extend` 没有声明名，接收者类型是引用位置（由 `CjExtend.getNameIdentifier() == null` 保证）。
 */
class AnalysisApiLibraryExtendReferenceTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/libraryExtendReferences",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 注册 `.cjo` 反编译服务：库声明的 PSI 回链依赖 builtins 的 decompiled PSI。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(CaAnalysisApiDecompiledTestServiceRegistrar)

    /**
     * `extend Int8` 内的 `this.toString()` 与 `Int8` 变量上的 `toString()` 必须落到
     * std.core `extend Int8 <: ToString` 的成员；`extend Box <: ToString` 的 `ToString`
     * 必须落到反编译的库接口声明。
     */
    @Test
    fun extendToStringReference(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            val toStringCalls = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
                .filter { it.referencedName == "toString" && it.parent is CjCallExpression }
                .sortedBy { it.textOffset }
            assertEquals(2, toStringCalls.size, "夹具应包含 extend 内与显式接收者两个 `toString()` 调用")

            val declarations = toStringCalls.map { call ->
                val resolved = call.mainReference.resolve() as? CjNamedFunction
                assertNotNull(resolved, "`toString()` 必须回链到反编译的库 extend 成员")
                val extend = resolved?.getStrictParentOfType<CjExtend>()
                assertEquals(
                    "Int8",
                    extend?.receiverTypeReceiver?.text,
                    "`toString()` 必须落到 `extend Int8 <: ToString` 的成员",
                )
                resolved
            }
            assertEquals(declarations[0], declarations[1], "两处 `toString()` 必须指向同一个声明")

            val toStringSuperType = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
                .single { it.referencedName == "ToString" }
            assertTrue(
                toStringSuperType.mainReference.resolve() is CjInterface,
                "`extend Box <: ToString` 的 `ToString` 必须回链到反编译的库接口声明",
            )

            // `?Any` 的库扩展成员（`Option.isNone`）同样必须回链；接收者文本可能是 `?Any` 或 `Option<Any>`。
            val isNoneCall = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
                .single { it.referencedName == "isNone" }
            val isNonePsi = isNoneCall.mainReference.resolve()
            assertTrue(isNonePsi is CjNamedFunction, "`?Any.isNone()` 必须回链到反编译的库 extend 成员")
            val isNoneOwner = (isNonePsi as CjNamedFunction).getStrictParentOfType<CjTypeStatement>()
            assertEquals("Option", isNoneOwner?.name, "`isNone` 必须落在库声明 `Option` 上")

            // 库 extend 声明本身也必须回链到反编译 PSI（按接收者 + 父类型结构签名定位）。
            val int8Type = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
                .single { it.name == "useInt8" }
                .symbol.valueParameters.single().returnType
            val toStringExtend = getExtendSymbols(int8Type).first { extend ->
                extend.extendId.contains("ToString")
            }
            val extendPsi = toStringExtend.psi
            assertTrue(extendPsi is CjExtend, "库 extend 符号必须回链到反编译 `CjExtend`")
            val extendPsiDeclaration = extendPsi as CjExtend
            assertEquals("Int8", extendPsiDeclaration.receiverTypeReceiver?.text, "库 extend 的 PSI 接收者必须是 Int8")
            assertTrue(
                extendPsiDeclaration.superTypeListEntries.any { entry ->
                    entry.typeReference?.getTypeText()?.contains("ToString") == true
                },
                "库 extend 的 PSI 父类型必须是 ToString",
            )
        }
    }
}
