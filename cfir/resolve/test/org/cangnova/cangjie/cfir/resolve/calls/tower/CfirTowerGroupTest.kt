@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.calls.tower

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [CfirTowerGroup] 的优先级与深度比较测试。
 *
 * 层级优先级：`EXPLICIT_MEMBER > LOCAL > EXTEND > NON_LOCAL > IMPLICIT_MEMBER > IMPORTED > PACKAGE`；
 * 同一层级中 depth 越小越靠近当前词法位置（`local(0) < local(1)`）。
 */
class CfirTowerGroupTest {

    @Nested
    inner class KindPriority {
        @Test
        fun `explicit member group is ordered before all other groups`() {
            val explicitMember = CfirTowerGroup.EXPLICIT_MEMBER
            assertTrue(explicitMember < CfirTowerGroup.local(0))
            assertTrue(explicitMember < CfirTowerGroup.EXTEND)
            assertTrue(explicitMember < CfirTowerGroup.NON_LOCAL)
            assertTrue(explicitMember < CfirTowerGroup.IMPLICIT_MEMBER)
            assertTrue(explicitMember < CfirTowerGroup.imported(0))
            assertTrue(explicitMember < CfirTowerGroup.PACKAGE)
        }

        @Test
        fun `local group is ordered before extend non local and implicit member groups`() {
            val local = CfirTowerGroup.local(0)
            assertTrue(local < CfirTowerGroup.EXTEND)
            assertTrue(local < CfirTowerGroup.NON_LOCAL)
            assertTrue(local < CfirTowerGroup.IMPLICIT_MEMBER)
            assertTrue(local < CfirTowerGroup.imported(0))
            assertTrue(local < CfirTowerGroup.PACKAGE)
        }

        @Test
        fun `extend group is ordered after local but before non local groups`() {
            val extend = CfirTowerGroup.EXTEND
            assertTrue(CfirTowerGroup.local(0) < extend)
            assertTrue(extend < CfirTowerGroup.NON_LOCAL)
            assertTrue(extend < CfirTowerGroup.IMPLICIT_MEMBER)
            assertTrue(extend < CfirTowerGroup.imported(0))
            assertTrue(extend < CfirTowerGroup.PACKAGE)
        }

        @Test
        fun `imported group is ordered before package group`() {
            assertTrue(CfirTowerGroup.imported(0) < CfirTowerGroup.PACKAGE)
        }

        @Test
        fun `package group is ordered last`() {
            assertTrue(CfirTowerGroup.PACKAGE > CfirTowerGroup.local(Int.MAX_VALUE))
        }
    }

    @Nested
    inner class DepthPriority {
        @Test
        fun `shallower local groups are preferred over deeper ones`() {
            assertTrue(CfirTowerGroup.local(0) < CfirTowerGroup.local(1))
        }

        @Test
        fun `shallower imported groups are preferred over deeper ones`() {
            assertTrue(CfirTowerGroup.imported(0) < CfirTowerGroup.imported(1))
        }

        @Test
        fun `same kind and depth groups are equal`() {
            assertEquals(CfirTowerGroup.local(1), CfirTowerGroup.local(1))
            assertEquals(CfirTowerGroup.imported(2), CfirTowerGroup.imported(2))
        }
    }
}
