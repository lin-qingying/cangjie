@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.base.index

import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexNameFilter
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CaSymbolIndexNameFilterTest {
    @Test
    fun shortPrefixesOnlyMatchTheStart() {
        assertTrue(CaSymbolIndexNameFilter("Ind").matches(Name.identifier("IndexedWidget")))
        assertFalse(CaSymbolIndexNameFilter("Wid").matches(Name.identifier("IndexedWidget")))
    }

    @Test
    fun longPrefixesAndRepeatedInvocationsAllowSubstrings() {
        assertTrue(CaSymbolIndexNameFilter("Widget").matches(Name.identifier("IndexedWidget")))
        assertTrue(CaSymbolIndexNameFilter("Wid", isRepeatedInvocation = true).matches(Name.identifier("IndexedWidget")))
        assertFalse(CaSymbolIndexNameFilter("missing", isRepeatedInvocation = true).matches(Name.identifier("IndexedWidget")))
    }

    @Test
    fun emptyPrefixAcceptsRegularNamesButNotSpecialNames() {
        assertTrue(CaSymbolIndexNameFilter("").matches(Name.identifier("IndexedWidget")))
        assertFalse(CaSymbolIndexNameFilter("").matches(Name.special("<anonymous>")))
    }
}
