package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjNamedPattern
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 新 public symbol identity / equivalence 协议回归测试。
 *
 * 这组测试锁定的不是解析结果本身，而是“哪些 symbol 应等价、哪些绝不能等价”：
 * 1. 同一个 extend 经不同 provider 路径拿到后必须等价；
 * 2. property getter 的 PSI 入口与 owning property 派生入口必须等价；
 * 3. anonymous function 跨 analyze 恢复后必须等价；
 * 4. 不同 match 分支中的同名 pattern binding 绝不能等价。
 */
class AnalysisApiSymbolEquivalenceTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/equivalence",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun symbolEquivalence(mainFile: CjFile) {
        val extendDeclaration = mainFile.declarations.filterIsInstance<CjExtend>().single()
        val documentClass = mainFile.declarations.filterIsInstance<CjTypeStatement>()
            .single { declaration -> declaration !is CjExtend && declaration.name == "Document" }
        val propertyDeclaration = PsiTreeUtil.findChildrenOfType(mainFile, CjProperty::class.java).single { property ->
            property.name == "state"
        }
        val getterDeclaration = PsiTreeUtil.findChildrenOfType(mainFile, CjPropertyAccessor::class.java).single { accessor ->
            accessor.isGetter
        }
        val functionLiteral = PsiTreeUtil.findChildrenOfType(mainFile, CjFunctionLiteral::class.java).single()
        val yReferences = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .filter { expression -> expression.referencedName == "y" }
            .filter(CjSimpleNameExpression::isUsageReference)
            .sortedBy { expression -> expression.textOffset }

        lateinit var anonymousPointer: CaSymbolPointer<CaSymbol>

        analyzeForTest(mainFile) {
            val extendSymbol = extendDeclaration.symbol as CaExtendSymbol
            val targetClass = documentClass.classSymbol as CaClassSymbol
            val queriedExtend = getExtendSymbols(targetClass.classId!!).single()
            assertTrue(extendSymbol.isEquivalentTo(queriedExtend))
            assertTrue(queriedExtend.isEquivalentTo(extendSymbol))

            val getterByPsi = getterDeclaration.symbol as CaPropertyGetterSymbol
            val getterByProperty = propertyDeclaration.symbol.getter
            assertNotNull(getterByProperty, "Property getter should be exposed through owning property.")
            assertTrue(getterByPsi.isEquivalentTo(getterByProperty!!))
            assertTrue(getterByProperty.isEquivalentTo(getterByPsi))

            val firstPatternBinding = yReferences[0].resolveToSymbol()
            val secondPatternBinding = yReferences[1].resolveToSymbol()
            assertNotNull(firstPatternBinding)
            assertNotNull(secondPatternBinding)
            assertFalse(firstPatternBinding!!.isEquivalentTo(secondPatternBinding!!))
            assertFalse(secondPatternBinding.isEquivalentTo(firstPatternBinding))

            anonymousPointer = (functionLiteral.symbol as CaAnonymousFunctionSymbol).createPointer()
        }

        analyzeForTest(mainFile) {
            val currentAnonymous = functionLiteral.symbol as CaAnonymousFunctionSymbol
            val restoredAnonymous = restoreSymbol(anonymousPointer) as? CaAnonymousFunctionSymbol
            assertNotNull(restoredAnonymous, "Anonymous-function pointer restore failed.")
            assertTrue(currentAnonymous.isEquivalentTo(restoredAnonymous!!))
            assertTrue(restoredAnonymous.isEquivalentTo(currentAnonymous))

            val extendMemberNames = (extendDeclaration.symbol as CaExtendSymbol)
                .declaredMemberScope
                .callables(Name.identifier("prettyPrint"))
            assertTrue(extendMemberNames.any())
        }
    }
}

private fun CjSimpleNameExpression.isUsageReference(): Boolean {
    return this !is CjBindingPattern &&
        parent !is CjBindingPattern &&
        parent !is CjNamedPattern &&
        parent !is CjVarOrEnumPattern
}
