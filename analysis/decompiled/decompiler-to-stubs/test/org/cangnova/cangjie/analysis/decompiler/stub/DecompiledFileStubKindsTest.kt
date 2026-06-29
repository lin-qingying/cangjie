package org.cangnova.cangjie.analysis.decompiler.stub

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

/**
 * 验证 `.cjo` 反编译文件 stub kind 的推导规则。
 */
class DecompiledFileStubKindsTest {
    /**
     * 验证没有顶层 callable 的 package 会推导为普通 file kind。
     */
    @Test
    fun inferFileWhenPackageHasNoTopLevelCallables() {
        val kind = DecompiledFileStubKinds.inferKind(
            packageFqName = FqName("sample.pkg"),
            sourceFiles = listOf("src/sample/pkg/only.cj"),
            hasTopLevelCallables = false,
        )

        val fileKind = assertInstanceOf(CangJieFileStubKind.WithPackage.File::class.java, kind)
        assertEquals("sample.pkg", fileKind.packageFqName.asString())
    }

    /**
     * 验证单个来源文件提供顶层 callable 时会推导为 simple facade。
     */
    @Test
    fun inferSimpleFacadeWhenSingleFileProvidesTopLevelCallables() {
        val kind = DecompiledFileStubKinds.inferKind(
            packageFqName = FqName("sample.pkg"),
            sourceFiles = listOf("src/sample/pkg/only.cj"),
            hasTopLevelCallables = true,
        )

        val facade = assertInstanceOf(CangJieFileStubKind.WithPackage.Facade.Simple::class.java, kind)
        assertEquals("sample.pkg", facade.facadeFqName.asString())
        assertEquals("pkg", facade.partSimpleName)
    }

    /**
     * 验证多个来源文件提供顶层 callable 时会推导为 multifile facade。
     */
    @Test
    fun inferMultifileFacadeWhenMultipleFilesProvideTopLevelCallables() {
        val kind = DecompiledFileStubKinds.inferKind(
            packageFqName = FqName("sample.pkg"),
            sourceFiles = listOf("src/sample/pkg/alpha.cj", "src/sample/pkg/beta.cj"),
            hasTopLevelCallables = true,
        )

        val multifile = assertInstanceOf(CangJieFileStubKind.WithPackage.Facade.MultifileClass::class.java, kind)
        assertEquals(listOf("alpha", "beta"), multifile.facadePartSimpleNames)
    }

    /**
     * 验证不同路径下同名源文件生成的 facade part name 保持唯一。
     */
    @Test
    fun multifilePartNamesRemainUniqueAcrossSameSimpleFileNames() {
        val partNames = DecompiledFileStubKinds.buildFacadePartSimpleNames(
            listOf(
                "src/first/common.cj",
                "src/second/common.cj",
                "src/third/common.cj",
            ),
        )

        assertEquals(listOf("common", "common_2", "common_3"), partNames)
    }
}
