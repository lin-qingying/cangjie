package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.components.CaScopeContext
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 按位置作用域上下文的执行回归。
 *
 * 覆盖：局部与参数可见性、类型参数、词法优先级次序、同名重载保留、遮蔽次序、
 * match 分支绑定隔离、隐式接收者视图、导入上下文与既有文件声明作用域的兼容、
 * 以及原件 / 分析副本两条路径的等价性。
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

            // 块内 `let` 声明在初始化之后必须进入局部作用域。
            //
            // 这里此前是一条**负例**断言，注释写明「底层修复后补齐正例断言」：
            // LL 的 ContextCollector 把绑定变量的入作用域放在了 pattern variable 的
            // `withLocalVariableBodyCompat` 清理内部，清理回滚后该名字永远不在快照里。
            val locals = context.compositeScope()
                .callables(setOf(Name.identifier("localValue")))
                .filterIsInstance<CaNamedSymbol>()
                .map { it.name.asString() }
                .toList()
            assertTrue(
                locals.contains("localValue"),
                "块内局部声明必须可见，实际 $locals",
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

    /** 泛型函数体内必须同时看到类型参数、值参数与块内局部声明。 */
    @Test
    fun lexicalScopesExposeParametersLocalsAndTypeParameters(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            val context = mainFile.scopeContext(mainFile.usageReference("scopeTarget"))
            val names = context.compositeScope().getAllPossibleNames().map { it.asString() }

            assertTrue("T" in names, "类型参数必须可见，实际 $names")
            assertTrue("value" in names, "第一个值参数必须可见，实际 $names")
            assertTrue("second" in names, "第二个值参数必须可见，实际 $names")
            assertTrue("local" in names, "块内局部声明必须可见，实际 $names")
            assertTrue("nested" !in names, "当前位置所在函数自身不得出现在其体内作用域，实际 $names")
        }
    }

    /**
     * 嵌套块必须给出多层局部作用域，且同名声明按由内到外排列。
     *
     * 断言次序而不是集合：`{shadowed}` 两个都在不算错，
     * 但内层在前才代表「遮蔽后取到的是内层那个」。
     */
    @Test
    fun nestedBlocksKeepInnermostShadowing(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            val inner = mainFile.scopeContext(mainFile.usageReference("innerTarget"))
            val outer = mainFile.scopeContext(mainFile.usageReference("outerTarget"))

            val innerLocalScopes = inner.scopes.filter { it.kind == CaScopeKind.LOCAL_SCOPE }
            assertTrue(
                innerLocalScopes.size >= 2,
                "嵌套块内必须存在块级与函数级两层局部作用域，实际 ${inner.scopes.map { it.kind }}",
            )

            // 由内到外：第一个提供 shadowed 的层必须是块级那层。
            val shadowingScopes = innerLocalScopes.filter { scope ->
                scope.scope.callables(setOf(Name.identifier("shadowed"))).any()
            }
            assertTrue(shadowingScopes.isNotEmpty(), "内层作用域必须提供 shadowed")
            assertEquals(
                innerLocalScopes.first().scope,
                shadowingScopes.first().scope,
                "遮蔽次序错误：最先命中的 shadowed 必须来自最内层作用域",
            )

            // 块外位置看不到块内声明：块级作用域不得泄漏到 `return shadowed` 所在层。
            val outerNames = outer.compositeScope().getAllPossibleNames().map { it.asString() }
            assertTrue("shadowed" in outerNames, "函数级 shadowed 在块外仍可见，实际 $outerNames")
            assertTrue("value" in outerNames, "函数参数在块外仍可见，实际 $outerNames")
        }
    }

    /** match 分支的绑定只在该分支内可见，兄弟分支的绑定不得泄漏。 */
    @Test
    fun matchBindingStaysInsideItsBranch(mainFile: CjFile) {
        analyzeForTest(mainFile) {
            val names = mainFile.scopeContext(mainFile.usageReference("branchTarget"))
                .compositeScope()
                .getAllPossibleNames()
                .map { it.asString() }

            assertTrue("side" in names, "当前分支的绑定必须可见，实际 $names")
            assertTrue("radius" !in names, "兄弟分支的绑定不得泄漏到当前分支，实际 $names")
            assertTrue("shape" in names, "match 的判别式参数必须可见，实际 $names")
        }
    }

    /**
     * 副本文件上的**词法**作用域上下文必须与原件一致。
     *
     * 补全走的是 `analyzeCopy` 的分析副本，副本路径上的 `ContextCollector`
     * 与原件共用同一套清理逻辑，任一侧回滚都会让候选缺失。
     *
     * 断言只覆盖词法层（`LOCAL_SCOPE` 的名字），不比较全局名字集合：
     * 本工作树的 dangling 副本在文件级查找层仍有已知缺口（缺陷 D-3），
     * 拿它做全名相等会把两个独立问题绑死在一个断言里。
     */
    @Test
    fun scopeContextInCopiedFileIsEquivalent(mainFile: CjFile) {
        val copiedFile = mainFile.copy() as CjFile
        val originalPosition = mainFile.usageReference("shadowed")
        val copiedPosition = PsiTreeUtil.findSameElementInCopy(originalPosition, copiedFile)
            ?: error("Cannot map `shadowed` into copied file `${copiedFile.name}`.")

        assertFalse(copiedFile.isPhysical, "前提不成立：副本应是 non-physical 文件")

        fun lexicalNames(context: CaScopeContext): List<String> = context.scopes
            .filter { it.kind == CaScopeKind.LOCAL_SCOPE }
            .flatMap { entry -> entry.scope.callables { true }.toList() }
            .filterIsInstance<CaNamedSymbol>()
            .map { it.name.asString() }
            .sorted()

        val originalNames = mutableListOf<String>()
        analyzeForTest(mainFile) {
            originalNames += lexicalNames(mainFile.scopeContext(originalPosition))
        }

        val copiedNames = mutableListOf<String>()
        analyzeForTest(copiedFile) {
            copiedNames += lexicalNames(copiedFile.scopeContext(copiedPosition))
        }

        assertEquals(
            originalNames.sorted(),
            copiedNames.sorted(),
            "副本与原件的局部作用域名字必须一致",
        )
        assertTrue("localValue" in copiedNames, "副本上也必须看到块内局部声明，实际 $copiedNames")
        assertTrue("shadowed" in copiedNames, "副本上也必须看到后续声明的局部变量，实际 $copiedNames")
    }
}

/**
 * 取出夹具中最后一个引用指定名字的 simple-name 表达式作为查询位置。
 */
private fun CjFile.usageReference(name: String): CjSimpleNameExpression =
    PsiTreeUtil.findChildrenOfType(this, CjSimpleNameExpression::class.java).last { it.referencedName == name }