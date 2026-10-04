package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjNamedFunction
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
    /**
     * 使用 standalone CFIR 配置运行 public symbol 等价性协议测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证不同入口得到的同一语义 symbol 等价，而不同局部绑定保持不等价。
     */
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

    /**
     * 同名重载绝不能被判为同一符号，且各自必须恢复到自身而不是兄弟重载。
     *
     * 这是缺陷 D-2 的回归锁定。`CallableId` 只含包名与短名，
     * 单靠 `callableId + kind` 做缓存键时，任意两个重载都会得到同一个键：
     * `isEquivalentTo` 对不同声明返回 `true`，缓存互相覆盖，
     * 恢复时 `singleOrNull` 因「命中多个」返回 `null`，两个符号一起丢失。
     */
    @Test
    fun overloadIdentity(mainFile: CjFile) {
        val functions = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
        val groups = listOf(
            "顶层函数" to functions.overloadsNamed("format"),
            "类成员函数" to functions.overloadsNamed("render"),
            "extend 成员函数" to functions.overloadsNamed("describe"),
        )

        // CaFunctionSymbol.createPointer() 的静态返回类型是 CaSymbolPointer<CaFunctionSymbol>，
        // 恢复后再断言具体种类——pointer 层的 castOrNull 会挡住类型不符的恢复。
        val captured = mutableListOf<Pair<CaNamedFunctionSymbol, CaSymbolPointer<CaFunctionSymbol>>>()

        analyzeForTest(mainFile) {
            for ((label, overloads) in groups) {
                // `.symbol` 是会话提供的属性，必须在 analyze {} 内取；groups 里存的是 PSI。
                val first = overloads.first.symbol
                val second = overloads.second.symbol
                assertFalse(first.isEquivalentTo(second), "$label 的两个重载被判定为同一符号：${first.name}")
                assertFalse(second.isEquivalentTo(first), "$label 的两个重载被判定为同一符号（反向）：${second.name}")
                captured.add(first to first.createPointer())
                captured.add(second to second.createPointer())
            }
        }

        analyzeForTest(mainFile) {
            groups.forEachIndexed { index, (label, _) ->
                val (firstOriginal, firstPointer) = captured[index * 2]
                val (secondOriginal, secondPointer) = captured[index * 2 + 1]

                val restoredFirst = restoreSymbol(firstPointer)
                val restoredSecond = restoreSymbol(secondPointer)
                assertNotNull(restoredFirst, "$label：${firstOriginal.name} 的重载指针恢复失败")
                assertNotNull(restoredSecond, "$label：${secondOriginal.name} 的重载指针恢复失败")

                assertTrue(
                    firstOriginal.isEquivalentTo(restoredFirst!!),
                    "$label：${firstOriginal.name} 的重载恢复到了别的声明",
                )
                assertTrue(
                    secondOriginal.isEquivalentTo(restoredSecond!!),
                    "$label：${secondOriginal.name} 的重载恢复到了别的声明",
                )
                // 恢复后仍必须两两不等价：若恢复落到兄弟重载，只看「恢复成功」发现不了。
                assertFalse(
                    restoredFirst.isEquivalentTo(restoredSecond),
                    "$label：两个重载恢复后仍被判为同一符号",
                )
            }
        }
    }
}

/**
 * 按名字取出恰好两个重载声明，按出现次序排列。
 *
 * 数量不符直接失败：夹具变了却仍然断言「两个重载」，等于把覆盖悄悄丢掉。
 */
private fun Collection<CjNamedFunction>.overloadsNamed(name: String): Pair<CjNamedFunction, CjNamedFunction> {
    val matching = filter { it.name == name }.sortedBy { it.textOffset }
    if (matching.size != 2) error("夹具中 $name 应有 2 个重载，实际 ${matching.size} 个")
    return matching[0] to matching[1]
}

/**
 * 判断 simple-name 是否处于真实引用位置，排除 pattern 声明侧名称。
 */
private fun CjSimpleNameExpression.isUsageReference(): Boolean {
    return this !is CjBindingPattern &&
        parent !is CjBindingPattern &&
        parent !is CjNamedPattern &&
        parent !is CjVarOrEnumPattern
}
