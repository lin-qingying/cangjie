package org.cangnova.cangjie.cfir.resolve.calls.tower

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * CfirTowerGroup 和 CfirCandidateCollector 的优先级比较测试。
 */
class CfirTowerGroupTest {

    @Nested
    inner class KindPriority {

        @Test
        fun `MEMBER has highest priority`() {
            assertTrue(CfirTowerGroup.MEMBER < CfirTowerGroup.local(0))
            assertTrue(CfirTowerGroup.MEMBER < CfirTowerGroup.EXTEND)
            assertTrue(CfirTowerGroup.MEMBER < CfirTowerGroup.imported(0))
            assertTrue(CfirTowerGroup.MEMBER < CfirTowerGroup.PACKAGE)
        }

        @Test
        fun `LOCAL is higher than EXTEND`() {
            assertTrue(CfirTowerGroup.local(0) < CfirTowerGroup.EXTEND)
        }

        @Test
        fun `EXTEND is higher than IMPORTED`() {
            assertTrue(CfirTowerGroup.EXTEND < CfirTowerGroup.imported(0))
        }

        @Test
        fun `IMPORTED is higher than PACKAGE`() {
            assertTrue(CfirTowerGroup.imported(0) < CfirTowerGroup.PACKAGE)
        }

        @Test
        fun `PACKAGE has lowest priority`() {
            assertTrue(CfirTowerGroup.PACKAGE > CfirTowerGroup.MEMBER)
            assertTrue(CfirTowerGroup.PACKAGE > CfirTowerGroup.local(0))
            assertTrue(CfirTowerGroup.PACKAGE > CfirTowerGroup.EXTEND)
            assertTrue(CfirTowerGroup.PACKAGE > CfirTowerGroup.imported(0))
        }
    }

    @Nested
    inner class DepthPriority {

        @Test
        fun `deeper local scope has higher priority`() {
            // depth 1 (更内层) 应优于 depth 0 (更外层)
            assertTrue(CfirTowerGroup.local(1) < CfirTowerGroup.local(0))
        }

        @Test
        fun `same kind same depth are equal`() {
            assertEquals(0, CfirTowerGroup.local(0).compareTo(CfirTowerGroup.local(0)))
        }

        @Test
        fun `deeper imported scope has higher priority`() {
            assertTrue(CfirTowerGroup.imported(1) < CfirTowerGroup.imported(0))
        }
    }

    @Nested
    inner class Equality {

        @Test
        fun `same kind and depth are equal`() {
            assertEquals(CfirTowerGroup.MEMBER, CfirTowerGroup.MEMBER)
            assertEquals(CfirTowerGroup.local(2), CfirTowerGroup.local(2))
            assertEquals(CfirTowerGroup.PACKAGE, CfirTowerGroup.PACKAGE)
        }

        @Test
        fun `different kind are not equal`() {
            assertNotEquals(CfirTowerGroup.MEMBER, CfirTowerGroup.local(0))
            assertNotEquals(CfirTowerGroup.EXTEND, CfirTowerGroup.PACKAGE)
        }
    }
}
