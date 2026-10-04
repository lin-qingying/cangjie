package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.components.buildSubstitutor
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjProperty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 连续签名替换必须保留原声明，并覆盖仓颉独立于 field 的 prop 符号。 */
@OptIn(CaExperimentalApi::class)
class AnalysisApiSignatureExecutionTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/signatures",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun chainedFunctionAndVariableSignatures(mainFile: CjFile) {
        val classes = mainFile.declarations.filterIsInstance<CjClass>()
        val identity = mainFile.declarations.filterIsInstance<CjNamedFunction>().single()
        val property = PsiTreeUtil.findChildrenOfType(mainFile, CjProperty::class.java).single()
        val field = PsiTreeUtil.findChildrenOfType(mainFile, CjFieldVariable::class.java).single { it.name == "field" }
        val local = PsiTreeUtil.findChildrenOfType(mainFile, CjPatternVariable::class.java).single()

        analyzeForTest(mainFile) {
            val userType = classes.single { it.name == "User" }.classSymbol.defaultType
            val otherType = classes.single { it.name == "Other" }.classSymbol.defaultType
            val function = identity.symbol
            val (t, u) = function.typeParameters
            val first = buildSubstitutor { substitution(t, buildTypeParameterType(u)) }
            val second = buildSubstitutor { substitution(u, userType) }
            val other = buildSubstitutor { substitution(u, otherType) }
            val original = function.asSignature()
            val intermediate = original.substitute(first)
            val final = intermediate.substitute(second)

            assertTrue(final.returnType.semanticallyEquals(userType))
            assertTrue(final.valueParameters.all { it.returnType.semanticallyEquals(userType) })
            assertTrue(function.isEquivalentTo(final.symbol))
            assertSame(function.psi, final.symbol.psi)
            assertTrue(original.returnType.semanticallyEquals(buildTypeParameterType(t)))
            assertTrue(intermediate.returnType.semanticallyEquals(buildTypeParameterType(u)))
            assertSame(final, final.substitute(CaSubstitutor.Empty(token)))
            assertNotEquals(final, intermediate.substitute(other), "不同实例化类型不能合并为同一签名")
            assertEquals(final, intermediate.substitute(second), "相同替换顺序应保留稳定相等性")
            assertEquals(final.hashCode(), intermediate.substitute(second).hashCode())

            // 反序执行不能偷偷合并映射：先 U -> User、再 T -> U，返回类型应仍是 U。
            val reversed = original.substitute(second).substitute(first)
            assertTrue(reversed.returnType.semanticallyEquals(buildTypeParameterType(u)))

            val (holderT, holderU) = classes.single { it.name == "Holder" }.classSymbol.typeParameters
            val holderFirst = buildSubstitutor { substitution(holderT, buildTypeParameterType(holderU)) }
            val holderSecond = buildSubstitutor { substitution(holderU, userType) }
            val variables: List<CaVariableSymbol> = listOf(property.symbol, field.symbol, local.symbol)
            for (variable in variables) {
                val signature = variable.asSignature()
                val substituted = signature.substitute(holderFirst).substitute(holderSecond)
                assertTrue(variable.isEquivalentTo(substituted.symbol))
                assertSame(variable.psi, substituted.symbol.psi)
                assertTrue(substituted.returnType.semanticallyEquals(userType), variable.toString())
                assertTrue(signature.returnType.semanticallyEquals(buildTypeParameterType(holderT)))
                assertSame(substituted, substituted.substitute(CaSubstitutor.Empty(token)))
            }
        }
    }
}
