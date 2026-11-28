package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 作用域种类的**塔序契约**（任务 3.8 的第二半）。
 *
 * `CaScopeKind.indexInTower` 决定同名声明在按位置作用域上下文里的先后。撞值会让两类
 * 作用域在排序里**等价**，而等价是静默的：不报错，只是顺序看起来没道理——
 * `CODE_FRAGMENT_MEMBER_SCOPE` 取 4 而不是复用 3，正是为了不和 `STATIC_MEMBER_SCOPE`
 * 撞值。因此这里把两件事钉死：**序号唯一**、**升序等于枚举文档写下的塔序**。
 *
 * 本用例只读枚举，不跑分析：次序是**规范**，不该靠某个夹具恰好复现才有保障。
 * 与之配套的执行侧断言在 `ScopeContextExecutionTest`（观察到的上下文必须按优先级排列）。
 */
class CaScopeKindTowerOrderContractTest {
    /**
     * 文档写明的塔序：从最内层到最外层（值越小越先被查找）。
     *
     * 与 `CaScopeContext.kt` 的枚举声明顺序一致；这里是它的**可执行副本**，
     * 枚举那边改了值而没改语义，本用例就该红。
     */
    private val documentedTowerOrder = listOf(
        CaScopeKind.LOCAL_SCOPE,
        CaScopeKind.TYPE_PARAMETER_SCOPE,
        CaScopeKind.TYPE_SCOPE,
        CaScopeKind.STATIC_MEMBER_SCOPE,
        CaScopeKind.CODE_FRAGMENT_MEMBER_SCOPE,
        CaScopeKind.FILE_SCOPE,
        CaScopeKind.PACKAGE_MEMBER_SCOPE,
        CaScopeKind.EXPLICIT_SIMPLE_IMPORTING_SCOPE,
        CaScopeKind.EXPLICIT_STAR_IMPORTING_SCOPE,
        CaScopeKind.DEFAULT_SIMPLE_IMPORTING_SCOPE,
        CaScopeKind.DEFAULT_STAR_IMPORTING_SCOPE,
        CaScopeKind.BUILTIN_SCOPE,
        CaScopeKind.OTHER,
    )

    /**
     * 每个种类都必须在塔序清单里，且升序排列后与文档塔序一致。
     *
     * 新增种类时漏补清单会在这里红——而不是让它带着一个随手的序号静默排到某个位置去。
     */
    @Test
    fun indexInTowerAscendingMatchesDocumentedTowerOrder() {
        assertEquals(
            documentedTowerOrder.toSet(),
            CaScopeKind.entries.toSet(),
            "新增/删除种类必须同步更新塔序清单：漏补的种类会静默排到任意位置",
        )
        assertEquals(
            documentedTowerOrder,
            CaScopeKind.entries.sortedBy { it.indexInTower },
            "indexInTower 升序必须等于文档塔序",
        )
    }

    /**
     * 序号必须唯一。
     *
     * 撞值是本条契约里最危险的失效：排序仍能跑、不报错，只是两类作用域的先后由
     * 枚举迭代顺序（而非规范）决定。
     */
    @Test
    fun indexInTowerIsUnique() {
        val duplicated = CaScopeKind.entries
            .groupBy { it.indexInTower }
            .filterValues { it.size > 1 }
        assertTrue(
            duplicated.isEmpty(),
            "indexInTower 必须唯一；撞值会让两类作用域排序等价并掩盖真实次序：$duplicated",
        )
    }

    /**
     * import 来源必须整段落在「包作用域之后、内建作用域之前」。
     *
     * 这正是语言的导入语义：显式 import 不得盖过本文件/本包的声明（文件内声明要能遮蔽
     * import 进来的同名符号），但必须盖过内建兜底。
     */
    @Test
    fun importingScopesSitBetweenPackageAndBuiltins() {
        val packageIndex = CaScopeKind.PACKAGE_MEMBER_SCOPE.indexInTower
        val builtinIndex = CaScopeKind.BUILTIN_SCOPE.indexInTower
        val importing = CaScopeKind.entries.filter { it.isImporting }

        assertTrue(importing.isNotEmpty(), "import 来源不能为空——全丢则导入语义无从表达")
        importing.forEach { kind ->
            assertTrue(
                kind.indexInTower > packageIndex,
                "$kind 必须排在包成员作用域之后（文件/包内声明要能遮蔽 import），实际 ${kind.indexInTower} <= $packageIndex",
            )
            assertTrue(
                kind.indexInTower < builtinIndex,
                "$kind 必须排在内建作用域之前（import 要能遮蔽内建），实际 ${kind.indexInTower} >= $builtinIndex",
            )
        }
    }

    /**
     * `OTHER` 是唯一的兜底类，必须严格排在最后。
     */
    @Test
    fun otherIsStrictlyLast() {
        val maxOthers = CaScopeKind.entries.filter { it.indexInTower == CaScopeKind.OTHER.indexInTower }
        assertEquals(listOf(CaScopeKind.OTHER), maxOthers, "除 OTHER 外不得有别的兜底种类")
        assertEquals(
            listOf(CaScopeKind.OTHER),
            CaScopeKind.entries.filter { it.indexInTower >= CaScopeKind.OTHER.indexInTower },
            "OTHER 必须严格大于其余全部种类的序号",
        )
    }

    /**
     * 显式 import 必须排在默认 import 之前（显式优先于语言默认导入）。
     */
    @Test
    fun explicitImportsOutrankDefaultImports() {
        listOf(CaScopeKind.EXPLICIT_SIMPLE_IMPORTING_SCOPE, CaScopeKind.EXPLICIT_STAR_IMPORTING_SCOPE).forEach { explicit ->
            assertTrue(explicit.isExplicitImporting, "$explicit 应被识别为显式导入")
            assertTrue(
                explicit.indexInTower < CaScopeKind.DEFAULT_SIMPLE_IMPORTING_SCOPE.indexInTower &&
                    explicit.indexInTower < CaScopeKind.DEFAULT_STAR_IMPORTING_SCOPE.indexInTower,
                "$explicit 必须排在默认导入之前，实际 ${explicit.indexInTower}",
            )
        }
    }
}
