package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.PackageKind
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * 验证 CJO 导出顶层名称解析器对物理声明和公开重导出的处理。
 */
class CjoExportedTopLevelNamesResolverTest {
    /**
     * 测试用 CJO 搜索根目录。
     */
    @TempDir
    lateinit var tempDir: Path

    /**
     * 验证解析器保留包内物理声明，并跟随 public import 重导出目标。
     */
    @Test
    fun `resolver keeps physical declarations and follows public import reexports`() {
        writeCjo(
            fileName = "macro-definition.cjo",
            packageFqName = "macro_definition",
            kind = PackageKind.Macro,
            callableNames = listOf("MakeInt64"),
        )
        writeCjo(
            fileName = "derive.cjo",
            packageFqName = "std.deriving",
            kind = PackageKind.Macro,
            callableNames = listOf("Derive"),
        )
        writeCjo(
            fileName = "facade.cjo",
            packageFqName = "a",
            kind = PackageKind.Macro,
            callableNames = emptyList(),
            fileImports = listOf(
                CjoPackageFileImports(
                    imports = listOf(
                        CjoPackageImport(
                            prefixPaths = listOf("std", "deriving"),
                            identifier = "Derive",
                            alias = "DeriveAlias",
                            isDecl = true,
                            withImplicitExport = true,
                        ),
                        CjoPackageImport(
                            prefixPaths = listOf("std", "deriving"),
                            identifier = "Derive",
                            isDecl = true,
                            withImplicitExport = true,
                        ),
                    ),
                ),
            ),
        )

        val resolver = CjoExportedTopLevelNamesResolver(
            CjoManager(
                CjoSearchPath { envName ->
                    when (envName) {
                        "CANGJIE_LIBRARY", "CANGJIE_STDLIB_MODULE" -> tempDir.toString()
                        else -> null
                    }
                },
            ),
        )

        assertEquals(
            setOf("MakeInt64"),
            resolver.resolve(FqName("macro_definition")).callableNames.mapTo(linkedSetOf()) { it.asString() },
        )
        assertEquals(
            setOf("DeriveAlias", "Derive"),
            resolver.resolve(FqName("a")).callableNames.mapTo(linkedSetOf()) { it.asString() },
        )
        assertEquals(
            CjoExportedTopLevelTarget(FqName("std.deriving"), Name.identifier("Derive")),
            resolver.resolve(FqName("a")).callableTargets[Name.identifier("Derive")],
        )
        assertEquals(
            CjoExportedTopLevelTarget(FqName("std.deriving"), Name.identifier("Derive")),
            resolver.resolve(FqName("a")).callableTargets[Name.identifier("DeriveAlias")],
        )
    }

    /**
     * 写入带指定声明和文件导入元数据的测试 CJO 文件。
     */
    private fun writeCjo(
        fileName: String,
        packageFqName: String,
        kind: UByte,
        callableNames: List<String>,
        fileImports: List<CjoPackageFileImports> = emptyList(),
    ) {
        CjoPackageWriter.write(
            tempDir.resolve(fileName),
            CjoPackageMetadata(
                fullPackageName = packageFqName,
                moduleName = "cjo-export-test",
                kind = kind,
                fileImports = fileImports,
                declarations = callableNames.map(::CjoPackageDeclaration),
            ),
        )
    }
}
