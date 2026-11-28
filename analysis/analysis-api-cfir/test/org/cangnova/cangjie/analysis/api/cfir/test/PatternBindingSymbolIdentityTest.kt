package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedPattern
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTypePattern
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 验证模式声明容器、真实绑定与 constructor/wildcard 在公开 API 中的身份边界。
 *
 * 单名和 tuple 的全局非成员声明均保留容器，变量身份仅属于 entries 中的真实叶子。
 * 类型模式绑定使用显式类型并锚到完整 TypePattern，歧义模式须经语义决议后发布可空绑定。
 * 测试只采用已确认的 let/var、tuple、类型和 enum 模式，不引入 const 或 command 规则。
 */
class PatternBindingSymbolIdentityTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/patternBindingSymbols",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    private val fixturePackage = FqName("test.patterns.symbols")

    /** 单名、嵌套 tuple 和纯 wildcard 容器均不具备 callable 身份；具名 entries 逐叶唯一。 */
    @Test
    fun ownerEntries(mainFile: CjFile) {
        val owners = PsiTreeUtil.findChildrenOfType(mainFile, CjPatternVariable::class.java).sortedBy { it.textOffset }
        val expectedNames = listOf(listOf("singleValue"), listOf("number", "flag", "text"), emptyList())
        assertEquals(expectedNames.size, owners.size)

        analyzeForTest(mainFile) {
            val allEntries = mutableListOf<CaPatternBindingSymbol>()
            owners.forEachIndexed { index, ownerPsi ->
                val symbol: CaSymbol = ownerPsi.symbol
                assertFalse(symbol is CaCallableSymbol, "整条模式声明必须保持非 callable 容器身份。")
                assertTrue(symbol is CaPatternVariableSymbol)
                val owner = symbol as CaPatternVariableSymbol
                assertSame(ownerPsi, owner.psi)
                assertEquals(CaSymbolLocation.TOP_LEVEL, owner.location)
                assertFalse(owner.isVar)
                assertFalse(owner.isConst)

                val entries = owner.entries
                val bindings = PsiTreeUtil.findChildrenOfType(ownerPsi, CjBindingPattern::class.java).sortedBy { it.textOffset }
                assertEquals(expectedNames[index], entries.map { it.name.asString() })
                assertEquals(expectedNames[index], bindings.map { it.name })
                entries.forEachIndexed { entryIndex, entry ->
                    val declarationSymbol = bindings[entryIndex].bindingSymbol
                    assertNotNull(declarationSymbol)
                    assertEquals(entry, declarationSymbol, "entry 与声明 bindingSymbol 必须具有同一声明身份。")
                    assertSame(bindings[entryIndex], entry.psi)
                    assertEquals(CaSymbolLocation.TOP_LEVEL, entry.location)
                    assertEquals(CallableId(fixturePackage, entry.name), entry.callableId)
                    assertTrue(entry.isLet)
                    assertFalse(entry.isConst)
                }
                allEntries += entries
            }
            assertDistinctEntries(allEntries)
        }
    }

    /** 全局单名和 tuple var 在声明及使用处具有一致的 TOP_LEVEL、CallableId 和可变性。 */
    @Test
    fun globalVarDeclarationAndReference(mainFile: CjFile, testServices: TestServices) {
        val bindings = PsiTreeUtil.findChildrenOfType(mainFile, CjBindingPattern::class.java).sortedBy { it.textOffset }
        val expectedNames = listOf("singleValue", "number", "flag", "text")
        assertEquals(expectedNames, bindings.map { it.name })
        val references = expectedNames.map { name ->
            testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, name)
        }

        analyzeForTest(mainFile) {
            val owners = PsiTreeUtil.findChildrenOfType(mainFile, CjPatternVariable::class.java)
            assertEquals(2, owners.size)
            owners.forEach { ownerPsi ->
                val owner = ownerPsi.symbol
                assertTrue(owner.isVar)
                assertFalse(owner.isConst)
            }
            bindings.forEachIndexed { index, bindingPsi ->
                val declaration = bindingPsi.bindingSymbol
                assertNotNull(declaration)
                val declaredSymbol = declaration!!
                val referenceSymbol = references[index].resolveToSymbol()
                assertTrue(referenceSymbol is CaPatternBindingSymbol)
                val usedSymbol = referenceSymbol as CaPatternBindingSymbol
                val expectedId = CallableId(fixturePackage, Name.identifier(expectedNames[index]))

                assertEquals(declaredSymbol, usedSymbol, "使用处和声明处必须指向同一个真实 global var。")
                listOf(declaredSymbol, usedSymbol).forEach { symbol ->
                    assertSame(bindingPsi, symbol.psi)
                    assertEquals(expectedNames[index], symbol.name.asString())
                    assertEquals(CaSymbolLocation.TOP_LEVEL, symbol.location)
                    assertEquals(expectedId, symbol.callableId)
                    assertFalse(symbol.isLet, "var 使用处不得被重新发布为 let。")
                    assertFalse(symbol.isConst)
                }
            }
        }
    }

    /** TypePattern 的规范 PSI 包含名字与显式类型，pointer 必须跨 analyze 恢复该完整节点。 */
    @Test
    fun typePatternCanonicalPsiAndPointer(mainFile: CjFile, testServices: TestServices) {
        val pattern = PsiTreeUtil.findChildrenOfType(mainFile, CjTypePattern::class.java).single()
        val usage = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, "typedUse")

        val pointer = analyzeForTest(pattern) {
            val binding = pattern.bindingSymbol
            assertNotNull(binding)
            val symbol = binding!!
            assertSame(pattern, symbol.psi, "类型绑定必须锚到整个 CjTypePattern。")
            assertEquals("number: Int64", symbol.psi!!.text)
            assertEquals("number", symbol.name.asString())
            assertEquals(CaSymbolLocation.LOCAL, symbol.location)
            assertTrue(symbol.isLet)
            assertFalse(symbol.isConst)
            assertInt64(symbol)
            assertTrue(symbol.returnType.semanticallyEquals(pattern.typeReference!!.type))
            assertEquals(symbol, usage.resolveToSymbol())
            symbol.createPointer()
        }

        analyzeForTest(usage) {
            val restored = restoreSymbol(pointer)
            assertTrue(restored is CaPatternBindingSymbol, "类型绑定 pointer 必须恢复真实 binder。")
            val symbol = restored as CaPatternBindingSymbol
            assertSame(pattern, symbol.psi)
            assertEquals(pattern.bindingSymbol, symbol)
            assertEquals(symbol, usage.resolveToSymbol())
            assertInt64(symbol)
            assertTrue(symbol.returnType.semanticallyEquals(pattern.typeReference!!.type))
        }
    }

    /** 语义为变量的 VarOrEnumPattern 使用规范歧义 PSI，声明、使用和 pointer 恢复身份一致。 */
    @Test
    fun varOrEnumCanonicalPsi(mainFile: CjFile, testServices: TestServices) {
        val patternPsi: PsiElement = PsiTreeUtil.findChildrenOfType(mainFile, CjVarOrEnumPattern::class.java).single()
        assertTrue(patternPsi is CjNamedPattern, "VarOrEnumPattern 必须接入 CjNamedPattern 的可空 bindingSymbol 协议。")
        val pattern = patternPsi as CjNamedPattern
        val usage = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, "capturedUse")

        val pointer = analyzeForTest(mainFile) {
            val binding = pattern.bindingSymbol
            assertNotNull(binding)
            val symbol = binding!!
            assertSame(patternPsi, symbol.psi)
            assertEquals("captured", symbol.name.asString())
            assertEquals(CaSymbolLocation.LOCAL, symbol.location)
            assertTrue(symbol.isLet)
            assertFalse(symbol.isConst)
            assertInt64(symbol)
            assertEquals(symbol, usage.resolveToSymbol())
            symbol.createPointer()
        }

        analyzeForTest(usage) {
            val restored = restoreSymbol(pointer)
            assertTrue(restored is CaPatternBindingSymbol)
            val symbol = restored as CaPatternBindingSymbol
            assertSame(patternPsi, symbol.psi)
            assertEquals(pattern.bindingSymbol, symbol)
            assertEquals(symbol, usage.resolveToSymbol())
        }
    }

    /** enum constructor 和 _: Type 都正常返回 null；constructor 使用仍解析为构造器符号。 */
    @Test
    fun nullableNonBindingPatterns(mainFile: CjFile, testServices: TestServices) {
        val constructorPatternPsi: PsiElement = PsiTreeUtil.findChildrenOfType(mainFile, CjVarOrEnumPattern::class.java).single()
        assertTrue(constructorPatternPsi is CjNamedPattern, "constructor 模式也必须支持可空 bindingSymbol 查询。")
        val constructorPattern = constructorPatternPsi as CjNamedPattern
        val wildcardTypePattern = PsiTreeUtil.findChildrenOfType(mainFile, CjTypePattern::class.java).single()
        val constructorDeclaration = PsiTreeUtil.findChildrenOfType(mainFile, CjEnumConstructor::class.java)
            .single { it.name == "Ready" }
        val constructorUse = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, "constructorUse")

        analyzeForTest(mainFile) {
            assertNull(constructorPattern.bindingSymbol, "语义为 constructor 的 VarOrEnumPattern 不制造 binder。")
            assertNull(wildcardTypePattern.bindingSymbol, "_: Type 不制造 binder。")
            val symbol = constructorUse.resolveToSymbol()
            assertTrue(symbol is CaEnumConstructorSymbol)
            assertEquals(constructorDeclaration.symbol, symbol)
            assertSame(constructorDeclaration, symbol!!.psi)
            assertNull(constructorPattern.bindingSymbol)
            assertNull(wildcardTypePattern.bindingSymbol)
        }
    }

    /** 官方 cjc 验证的泛型上下文模式：完成后每个 binder 与使用表达式都获得最终分量类型。 */
    @Test
    fun contextualProjection(mainFile: CjFile, testServices: TestServices) {
        val patterns = PsiTreeUtil.findChildrenOfType(mainFile, CjVarOrEnumPattern::class.java).associateBy { it.name }
        val numberUse = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, "numberUse")
        val flagUse = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, "flagUse")
        analyzeForTest(mainFile) {
            val number = patterns.getValue("number").bindingSymbol!!
            val flag = patterns.getValue("flag").bindingSymbol!!
            assertInt64(number)
            assertTrue(flag.returnType is CaPrimitiveType)
            assertEquals(PrimitiveTypeKind.BOOLEAN, (flag.returnType as CaPrimitiveType).kind)
            assertEquals(number, numberUse.resolveToSymbol())
            assertEquals(flag, flagUse.resolveToSymbol())
            assertTrue(number.returnType.semanticallyEquals(numberUse.expressionType!!))
            assertTrue(flag.returnType.semanticallyEquals(flagUse.expressionType!!))
        }
    }

    /** 每个真实叶子在当前 session 中都须具有独立符号身份。 */
    private fun assertDistinctEntries(entries: List<CaPatternBindingSymbol>) {
        for (index in entries.indices) {
            for (otherIndex in index + 1 until entries.size) {
                assertNotSame(entries[index], entries[otherIndex], "不同模式叶子不得共享 entry 身份。")
            }
        }
    }

    /** 显式 Int64 和 Int64 selector 的绑定必须暴露真实 primitive 类型。 */
    private fun assertInt64(symbol: CaPatternBindingSymbol) {
        val type = symbol.returnType
        assertTrue(type is CaPrimitiveType)
        assertEquals(PrimitiveTypeKind.INT64, (type as CaPrimitiveType).kind)
    }
}
