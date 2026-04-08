package org.cangnova.cangjie.analysis.decompiled.stubs

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class CaDecompiledFileStubKindsTest {
    @Test
    fun inferFileWhenPackageHasNoTopLevelCallables() {
        val kind = CaDecompiledFileStubKinds.inferKind(
            packageFqName = FqName("sample.pkg"),
            sourceFiles = listOf("src/sample/pkg/only.cj"),
            hasTopLevelCallables = false,
        )

        val fileKind = assertInstanceOf(CangJieFileStubKind.WithPackage.File::class.java, kind)
        assertEquals("sample.pkg", fileKind.packageFqName.asString())
    }

    @Test
    fun inferSimpleFacadeWhenSingleFileProvidesTopLevelCallables() {
        val kind = CaDecompiledFileStubKinds.inferKind(
            packageFqName = FqName("sample.pkg"),
            sourceFiles = listOf("src/sample/pkg/only.cj"),
            hasTopLevelCallables = true,
        )

        val facade = assertInstanceOf(CangJieFileStubKind.WithPackage.Facade.Simple::class.java, kind)
        assertEquals("sample.pkg", facade.facadeFqName.asString())
        assertEquals("pkg", facade.partSimpleName)
    }

    @Test
    fun inferMultifileFacadeWhenMultipleFilesProvideTopLevelCallables() {
        val kind = CaDecompiledFileStubKinds.inferKind(
            packageFqName = FqName("sample.pkg"),
            sourceFiles = listOf("src/sample/pkg/alpha.cj", "src/sample/pkg/beta.cj"),
            hasTopLevelCallables = true,
        )

        val multifile = assertInstanceOf(CangJieFileStubKind.WithPackage.Facade.MultifileClass::class.java, kind)
        assertEquals(listOf("alpha", "beta"), multifile.facadePartSimpleNames)
    }

    @Test
    fun multifilePartNamesRemainUniqueAcrossSameSimpleFileNames() {
        val partNames = CaDecompiledFileStubKinds.buildFacadePartSimpleNames(
            listOf(
                "src/first/common.cj",
                "src/second/common.cj",
                "src/third/common.cj",
            ),
        )

        assertEquals(listOf("common", "common_2", "common_3"), partNames)
    }
}
