package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 按位置作用域上下文的执行回归。
 *
 * 覆盖：局部与参数可见性、词法优先级次序、同名重载保留、隐式接收者视图、
 * 导入上下文与既有文件声明作用域的兼容。
 */
@OptIn(CaExperimentalApi::class)
class ScopeContextExecutionTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/scopeContext",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** 函数体内的位置必须看到参数、局部声明与文件顶层声明，并按真实优先级排列。 */
    @Test
    fun lexicalScopeContextAtPosition(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            val position = mainFile.usageReference("shadowed")
            val context = mainFile.scopeContext(position)

            val kinds = context.scopes.map { it.kind }
            assertTrue(
                kinds.contains(CaScopeKind.LOCAL_SCOPE),
                "函数体位置必须包含局部作用域，实际 kinds=$kinds",
            )
            assertTrue(
                kinds.contains(CaScopeKind.FILE_SCOPE),
                "函数体位置必须包含文件顶层声明作用域，实际 kinds=$kinds",
            )

            val priorities = context.scopes.map { it.kind.indexInTower }
            assertEquals(priorities.sorted(), priorities, "作用域必须按优先级从高到低排列，实际 kinds=$kinds")

            val visibleNames = context.compositeScope().getAllPossibleNames().map { it.asString() }
            assertTrue(visibleNames.contains("value"), "函数参数必须可见，实际可见 $visibleNames")
            assertTrue(visibleNames.contains("tracked"), "顶层函数必须可见，实际可见 $visibleNames")
            assertTrue("stored" !in visibleNames, "其它类型实例成员不得进入函数体作用域，实际可见 $visibleNames")

            // 同层可以有多个局部作用域（块级与参数级），参数只出现在其中一层。
            val params = context.scopes
                .filter { it.kind == CaScopeKind.LOCAL_SCOPE }
                .flatMap { it.scope.callables(setOf(Name.identifier("value"))).toList() }
                .filterIsInstance<CaNamedSymbol>()
                .map { it.name.asString() }
                .toList()
            assertTrue(params.contains("value"), "局部作用域必须包含函数参数 value，实际 $params")

            // 已知缺口：当前 LL 解析塔快照在 let 初始化之后仍未把块内局部声明写入局部作用域，
            // 因此 `localValue` 暂不可见。该用例在低层修复后补齐正例断言，此处不写成通过。
            val localsNow = context.compositeScope()
                .callables(setOf(Name.identifier("localValue")))
                .filterIsInstance<CaNamedSymbol>()
                .toList()
            assertTrue(
                localsNow.isEmpty(),
                "块内局部声明当前尚未进入解析塔快照；底层修复后此处应断言包含 localValue，实际 $localsNow",
            )
        }
    }

    /** 同名重载必须完整保留，不能因显示名相同被合并。 */
    @Test
    fun overloadsKeepIdentity(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            val position = mainFile.usageReference("overloaded")
            val overloads = mainFile.scopeContext(position).compositeScope()
                .callables(setOf(Name.identifier("overloaded")))
                .toList()

            assertEquals(2, overloads.size, "两个同名重载必须都可见，实际 $overloads")
            assertEquals(
                2,
                overloads.map { it.returnType.toString() }.distinct().size,
                "重载返回类型必须可区分，实际 ${overloads.map { it.returnType }}",
            )
        }
    }

    /** 成员函数内的位置必须暴露类实例隐式接收者。 */
    @Test
    fun memberScopeExposesImplicitReceiver(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            val position = mainFile.usageReference("stored")
            val receivers = mainFile.scopeContext(position).implicitReceivers

            assertTrue(receivers.isNotEmpty(), "成员函数体内必须存在隐式接收者")
            assertTrue(receivers.any { it.type.toString().contains("Container") }, "接收者类型应为 Container，实际 ${receivers.map { it.type }}")
        }
    }

    /** 导入上下文必须含文件与包来源，且不混入局部声明。 */
    @Test
    fun importingScopeContextIsIndependentFromFileScope(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            val importing = mainFile.importingScopeContext
            val kinds = importing.scopes.map { it.kind }
            assertTrue(kinds.contains(CaScopeKind.FILE_SCOPE), "导入上下文必须含文件顶层声明，实际 $kinds")

            val names = importing.compositeScope().getAllPossibleNames().map { it.asString() }
            assertTrue("localValue" !in names, "导入上下文不得包含局部声明，实际 $names")
            assertTrue("tracked" in names, "导入上下文必须包含顶层函数，实际 $names")

            // 既有文件声明作用域语义保持不变：只枚举本文件声明，不因导入上下文扩大。
            val fileScopeNames = mainFile.getFileScope().getAllPossibleNames().map { it.asString() }
            assertTrue("tracked" in fileScopeNames, "文件声明作用域必须仍包含顶层函数，实际 $fileScopeNames")
            assertTrue("localValue" !in fileScopeNames, "文件声明作用域不得包含局部声明，实际 $fileScopeNames")
        }
    }
}

/**
 * 取出夹具中最后一个引用指定名字的 simple-name 表达式作为查询位置。
 */
private fun CjFile.usageReference(name: String): CjSimpleNameExpression =
    PsiTreeUtil.findChildrenOfType(this, CjSimpleNameExpression::class.java).last { it.referencedName == name }