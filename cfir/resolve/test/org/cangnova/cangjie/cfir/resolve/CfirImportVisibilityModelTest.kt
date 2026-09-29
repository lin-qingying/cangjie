package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedImport
import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedImportCopy
import org.cangnova.cangjie.cfir.resolve.providers.isPackageVisibleSourceImport
import org.cangnova.cangjie.cfir.resolve.providers.isReexportingSourceImport
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 可见性必须由 IR 字段承载，即使 source 不存在，解析包装和复制后也保持同一语义。 */
class CfirImportVisibilityModelTest {
    @Test
    fun `synthetic imports are private and do not become package exports`() {
        val import = buildImport {
            importedFqName = FqName("library.Box")
            isAllUnder = false
        }
        assertSame(Visibilities.Private, import.visibility)
        assertFalse(import.isReexportingSourceImport())
        assertFalse(import.isPackageVisibleSourceImport())
    }

    @Test
    fun `resolved import and copy retain visibility without source syntax`() {
        for (visibility in listOf(Visibilities.Public, Visibilities.Protected, Visibilities.Internal)) {
            val original = buildImport {
                importedFqName = FqName("library.Box")
                this.visibility = visibility
                isAllUnder = false
            }
            val resolved = buildResolvedImport {
                delegate = original
                packageFqName = FqName("library")
            }
            val copied = buildResolvedImportCopy(resolved) {}
            assertSame(visibility, resolved.visibility)
            assertSame(visibility, copied.visibility)
            assertTrue(copied.isPackageVisibleSourceImport())
            if (visibility == Visibilities.Internal) {
                assertFalse(copied.isReexportingSourceImport())
            } else {
                assertTrue(copied.isReexportingSourceImport())
            }
        }
    }
}
