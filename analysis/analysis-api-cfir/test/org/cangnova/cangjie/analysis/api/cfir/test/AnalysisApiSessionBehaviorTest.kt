package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjNamedPattern
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 会话层行为回归测试。
 *
 * 这组测试覆盖跨 `analyze {}` 边界恢复公开符号指针的关键语义，
 * 避免后续修改 session / pointer 协议时再次退化为“只能在单次分析调用中使用符号”。
 */
class AnalysisApiSessionBehaviorTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/sessions",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun symbolPointerRestore(mainFile: CjFile) {
        val referenceExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .last { it.referencedName == "greet" }

        lateinit var pointer: CaSymbolPointer<CaSymbol>

        analyzeForTest(referenceExpression) {
            val resolvedSymbol = referenceExpression.resolveToSymbol()
            assertNotNull(resolvedSymbol, "Simple-name reference should resolve to a public symbol.")

            pointer = resolvedSymbol!!.createPointer()
            assertEquals("greet", resolvedSymbol.name?.asString())
        }

        analyzeForTest(referenceExpression) {
            val restoredSymbol = restoreSymbol(pointer)
            assertNotNull(restoredSymbol, "Symbol pointer restore across analyze boundary failed.")
            assertEquals("greet", restoredSymbol!!.name?.asString())

            val restoredPsi = restoredSymbol.psi as? CjNamedDeclaration
            assertNotNull(restoredPsi, "Restored symbol should point back to original PSI.")
            assertEquals("greet", restoredPsi!!.name)
        }
    }

    @Test
    fun topLevelPatternBindingPointerRestore(mainFile: CjFile) {
        val referenceExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .filter(CjSimpleNameExpression::isUsageReference)
            .last { it.referencedName == "topLeft" }

        lateinit var pointer: CaSymbolPointer<CaSymbol>

        analyzeForTest(referenceExpression) {
            val resolvedSymbol = referenceExpression.resolveToSymbol()
            assertNotNull(resolvedSymbol, "Top-level pattern binding should resolve to an independent symbol.")
            pointer = resolvedSymbol!!.createPointer()

            val originalPsi = resolvedSymbol.psi as? CjBindingPattern
            assertNotNull(originalPsi, "Top-level pattern binding should point back to binding pattern PSI.")
            assertEquals("topLeft", originalPsi!!.name)
        }

        analyzeForTest(referenceExpression) {
            val restoredSymbol = restoreSymbol(pointer)
            assertNotNull(restoredSymbol, "Top-level pattern binding pointer restore failed.")

            val restoredPsi = restoredSymbol!!.psi as? CjBindingPattern
            assertNotNull(restoredPsi, "Restored top-level pattern binding should point back to binding pattern PSI.")
            assertEquals("topLeft", restoredPsi!!.name)
        }
    }

    @Test
    fun patternBindingDeclarationRestore(mainFile: CjFile) {
        val references = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .filter(CjSimpleNameExpression::isUsageReference)
        val leftReference = references.last { it.referencedName == "left" }
        val rightReference = references.last { it.referencedName == "right" }

        analyzeForTest(leftReference) {
            val leftSymbol = leftReference.resolveToSymbol()
            val rightSymbol = rightReference.resolveToSymbol()
            assertNotNull(leftSymbol, "Destructuring left should resolve to an independent binding symbol.")
            assertNotNull(rightSymbol, "Destructuring right should resolve to an independent binding symbol.")
            assertNotSame(leftSymbol, rightSymbol, "Different bindings should not share the same symbol.")

            val leftPsi = leftSymbol!!.psi as? CjBindingPattern
            val rightPsi = rightSymbol!!.psi as? CjBindingPattern
            assertNotNull(leftPsi, "Left binding should point back to binding pattern PSI.")
            assertNotNull(rightPsi, "Right binding should point back to binding pattern PSI.")
            assertEquals("left", leftPsi!!.name)
            assertEquals("right", rightPsi!!.name)
        }
    }

    @Test
    fun matchPatternBindingResolve(mainFile: CjFile) {
        val references = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .filter { it.referencedName == "y" }
            .filter(CjSimpleNameExpression::isUsageReference)
            .sortedBy { it.textOffset }
        val firstBranchReference = references[0]
        val secondBranchReference = references[1]

        analyzeForTest(firstBranchReference) {
            val firstSymbol = firstBranchReference.resolveToSymbol()
            val secondSymbol = secondBranchReference.resolveToSymbol()
            assertNotNull(firstSymbol, "First match-branch reference should resolve to a binding symbol.")
            assertNotNull(secondSymbol, "Second match-branch reference should resolve to a binding symbol.")
            assertNotSame(firstSymbol, secondSymbol, "Bindings from different match branches must stay distinct.")

            val firstPsi = firstSymbol!!.psi.asPatternBindingDeclarationPsi()
            val secondPsi = secondSymbol!!.psi.asPatternBindingDeclarationPsi()
            assertNotNull(firstPsi, "First match-branch binding should point back to pattern declaration PSI.")
            assertNotNull(secondPsi, "Second match-branch binding should point back to pattern declaration PSI.")
            assertNotSame(firstPsi, secondPsi, "Different branches must point to different pattern declaration PSI.")
            assertEquals("y", firstPsi!!.name)
            assertEquals("y", secondPsi!!.name)
        }
    }

    @Test
    fun extendSymbolPointerRestore(mainFile: CjFile) {
        val extendDeclaration = mainFile.declarations.filterIsInstance<CjExtend>().single()

        lateinit var pointer: CaSymbolPointer<CaSymbol>

        analyzeForTest(extendDeclaration) {
            val extendSymbol = extendDeclaration.symbol
            pointer = extendSymbol.createPointer()

            assertEquals(CaSymbolOrigin.SOURCE, extendSymbol.origin)
            assertEquals(CaSymbolLocation.TOP_LEVEL, extendSymbol.location)

            val originalPsi = extendSymbol.psi as? CjExtend
            assertNotNull(originalPsi, "Extend symbol should restore to extend PSI.")
            assertEquals(extendDeclaration.getExtendId(), originalPsi!!.getExtendId())

            val extendMember = extendSymbol.declaredMemberScope.callableSymbol("prettyPrint") as? CaNamedFunctionSymbol
            assertNotNull(extendMember, "Extend declaredMemberScope should expose extend members.")
            assertEquals("prettyPrint", extendMember!!.name.asString())
            assertEquals(CaSymbolLocation.EXTEND, extendMember.location)
            assertEquals(extendSymbol, extendMember.containingDeclaration)
        }

        analyzeForTest(extendDeclaration) {
            val restoredSymbol = restoreSymbol(pointer) as? CaExtendSymbol
            assertNotNull(restoredSymbol, "Extend symbol pointer restore failed.")
            assertEquals(CaSymbolOrigin.SOURCE, restoredSymbol!!.origin)
            assertEquals(CaSymbolLocation.TOP_LEVEL, restoredSymbol.location)

            val restoredPsi = restoredSymbol.psi as? CjExtend
            assertNotNull(restoredPsi, "Restored extend symbol should point to extend PSI.")
            assertEquals(extendDeclaration.getExtendId(), restoredPsi!!.getExtendId())
        }
    }

    @Test
    fun propertyAccessorPointerRestore(mainFile: CjFile) {
        val accessors = PsiTreeUtil.findChildrenOfType(mainFile, CjPropertyAccessor::class.java)
            .sortedBy { it.textOffset }
        val getterPsi = accessors.first { it.isGetter }
        val setterPsi = accessors.first { it.isSetter }

        lateinit var getterPointer: CaSymbolPointer<CaSymbol>
        lateinit var setterPointer: CaSymbolPointer<CaSymbol>

        analyzeForTest(getterPsi) {
            val getterSymbol = getterPsi.symbol as CaPropertyGetterSymbol
            val setterSymbol = setterPsi.symbol as CaPropertySetterSymbol
            getterPointer = getterSymbol.createPointer()
            setterPointer = setterSymbol.createPointer()

            assertTrue(getterSymbol.isGetter)
            assertFalse(setterSymbol.isGetter)
            assertEquals(CaSymbolOrigin.SOURCE, getterSymbol.origin)
            assertEquals(CaSymbolLocation.PROPERTY, getterSymbol.location)
            assertEquals(CaSymbolLocation.PROPERTY, setterSymbol.location)
            assertEquals("state", getterSymbol.owningProperty.name.asString())
            assertEquals("state", setterSymbol.owningProperty.name.asString())
            assertEquals("value", setterSymbol.parameter.name.asString())
            assertEquals(setterSymbol, setterSymbol.parameter.containingDeclaration)

            val originalGetterPsi = getterSymbol.psi as? CjPropertyAccessor
            val originalSetterPsi = setterSymbol.psi as? CjPropertyAccessor
            assertNotNull(originalGetterPsi, "Getter symbol should restore to getter accessor PSI.")
            assertNotNull(originalSetterPsi, "Setter symbol should restore to setter accessor PSI.")
            assertTrue(originalGetterPsi!!.isGetter)
            assertTrue(originalSetterPsi!!.isSetter)
        }

        analyzeForTest(getterPsi) {
            val restoredGetter = restoreSymbol(getterPointer) as? CaPropertyGetterSymbol
            val restoredSetter = restoreSymbol(setterPointer) as? CaPropertySetterSymbol
            assertNotNull(restoredGetter, "Getter symbol pointer restore failed.")
            assertNotNull(restoredSetter, "Setter symbol pointer restore failed.")
            assertEquals("state", restoredGetter!!.owningProperty.name.asString())
            assertEquals("state", restoredSetter!!.owningProperty.name.asString())
            assertEquals("value", restoredSetter.parameter.name.asString())
            assertTrue((restoredGetter.psi as? CjPropertyAccessor)?.isGetter == true)
            assertTrue((restoredSetter.psi as? CjPropertyAccessor)?.isSetter == true)
        }
    }

    @Test
    fun anonymousFunctionPointerRestore(mainFile: CjFile) {
        val functionLiteral = PsiTreeUtil.findChildrenOfType(mainFile, CjFunctionLiteral::class.java).single()

        lateinit var pointer: CaSymbolPointer<CaSymbol>

        analyzeForTest(functionLiteral) {
            val anonymousSymbol = functionLiteral.symbol as CaAnonymousFunctionSymbol
            pointer = anonymousSymbol.createPointer()

            assertEquals(CaSymbolOrigin.SOURCE, anonymousSymbol.origin)
            assertEquals(CaSymbolLocation.LOCAL, anonymousSymbol.location)
            assertEquals(1, anonymousSymbol.valueParameters.size)
            assertEquals("input", anonymousSymbol.valueParameters.single().name.asString())

            val containingDeclaration = anonymousSymbol.containingDeclaration as? CaNamedFunctionSymbol
            assertNotNull(containingDeclaration, "Anonymous function should belong to the outer named function.")
            assertEquals("runLambda", containingDeclaration!!.name.asString())

            val originalPsi = anonymousSymbol.psi as? CjFunctionLiteral
            assertNotNull(originalPsi, "Anonymous function symbol should restore to function literal PSI.")
            assertEquals(functionLiteral.text, originalPsi!!.text)
        }

        analyzeForTest(functionLiteral) {
            val restoredSymbol = restoreSymbol(pointer) as? CaAnonymousFunctionSymbol
            assertNotNull(restoredSymbol, "Anonymous function symbol pointer restore failed.")
            assertEquals(CaSymbolLocation.LOCAL, restoredSymbol!!.location)
            assertEquals("input", restoredSymbol.valueParameters.single().name.asString())

            val restoredPsi = restoredSymbol.psi as? CjFunctionLiteral
            assertNotNull(restoredPsi, "Restored anonymous function symbol should point to function literal PSI.")
            assertEquals(functionLiteral.text, restoredPsi!!.text)
        }
    }
}

/**
 * 仅保留真正的“使用点” simple-name。
 *
 * 新的 pattern 语义里，`CjBindingPattern` 与 `CjVarOrEnumPattern` 的名字同样会以下层
 * `CjSimpleNameExpression` 形式出现在 PSI 树中；这些名字属于声明侧，不应混入引用解析断言。
 */
private fun CjSimpleNameExpression.isUsageReference(): Boolean {
    return this !is CjBindingPattern &&
        parent !is CjNamedPattern &&
        parent !is CjVarOrEnumPattern
}

/**
 * `CaPatternBindingSymbol` 需要回到真实源码中的声明节点。
 *
 * 普通绑定模式返回 `CjBindingPattern`，
 * 语法上保留歧义、但语义上收敛为 binding 的场景返回 `CjVarOrEnumPattern`。
 */
private fun Any?.asPatternBindingDeclarationPsi(): PsiNameIdentifierOwner? {
    return when (this) {
        is CjBindingPattern -> this
        is CjVarOrEnumPattern -> this
        else -> null
    }
}

private fun org.cangnova.cangjie.analysis.api.scopes.CaScope.callableSymbol(name: String): CaSymbol {
    return callables(Name.identifier(name)).single()
}
