package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.DeclKind
import PackageFormat.Package
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.metadata.model.Attribute
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CJMP common part cjo 的写出 / 读回与加载门验收（计划 Phase 4.2 与 4.3）。
 *
 * 覆盖：
 * - 写侧属性位（COMMON / FROM_COMMON_PART / COMMON_WITH_DEFAULT）与包级 CJMP 字段
 *  （`Package.options`、`FileInfo.feature`）的往返一致；
 * - 加载门四条的判定（版本 / 包名 / features 子集 / 编译选项）；
 * - 版本缺失即拒与包名不符即拒在 [CjoManager] 层的实际拒绝行为。
 */
class CjmpCommonPartCjoTest {
    /** 按官方 AttributePack 位布局构造属性位图。 */
    private fun attributeWords(vararg attributes: Attribute): List<ULong> {
        val max = attributes.maxOf { it.ordinal }
        val words = ULongArray(max / 64 + 1)
        attributes.forEach { attribute ->
            words[attribute.ordinal / 64] = words[attribute.ordinal / 64] or (1uL shl (attribute.ordinal % 64))
        }
        return words.toList()
    }

    private fun cjmpMetadata(
        packageName: String = "sample.cjmp",
        includeCjmpBits: Boolean = true,
        fileFeatures: List<List<String>> = listOf(listOf("Foo"), listOf("Bar", "Baz")),
        options: CjoModuleOptionMetadata? = CjoModuleOptionMetadata(debug = true, optLevel = 2u),
    ): CjoPackageMetadata = CjoPackageMetadata(
        fullPackageName = packageName,
        moduleName = "sample",
        schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
        allFiles = listOf("$packageName/common.cj"),
        fileInfo = listOf(
            CjoFileInfoMetadata(
                fileId = 1u,
                begin = CjoPositionMetadata(file = 1u, line = 1, column = 0),
                end = CjoPositionMetadata(file = 1u, line = 3, column = 1),
                feature = CjoFeaturesDirectiveMetadata(features = fileFeatures),
            ),
        ),
        options = options,
        declarations = listOf(
            CjoPackageDeclaration(
                identifier = "Platform",
                kind = DeclKind.FuncDecl,
                attributes = if (includeCjmpBits) {
                    attributeWords(Attribute.COMMON, Attribute.FROM_COMMON_PART, Attribute.COMMON_WITH_DEFAULT)
                } else {
                    emptyList()
                },
            ),
        ),
    )

    @Test
    fun `cjmp common part cjo round-trips package level cjmp facts`() {
        val bytes = CjoPackageWriter.toByteArray(cjmpMetadata())
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        val header = CjoPackageHeader.fromPackage(pkg)

        assertEquals(CjmpCommonPartLoadGate.supportedVersion, header.cjoVersion)
        assertEquals(CjoModuleOptionInfo(debug = true, optLevel = 2u), header.options)
        assertEquals(
            mapOf("sample.cjmp/common.cj" to listOf(listOf("Foo"), listOf("Bar", "Baz"))),
            header.fileFeatures,
        )
        assertTrue(header.isCjmpCommonPart, "declaration carries CJMP attribute bits")

        val decl = assertNotNull(pkg.allDecls(0))
        val words = (0 until decl.attributesLength).map { decl.attributes(it) }
        assertEquals(
            attributeWords(Attribute.COMMON, Attribute.FROM_COMMON_PART, Attribute.COMMON_WITH_DEFAULT),
            words,
        )
    }

    @Test
    fun `cjmp facts stay absent for plain packages`() {
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "sample.plain",
                moduleName = "sample",
                schemaProfile = CjoSchemaProfile.OFFICIAL_V1_1_3,
            ),
        )
        val header = CjoPackageHeader.fromPackage(Package.getRootAsPackage(ByteBuffer.wrap(bytes)))
        assertNull(header.options)
        assertTrue(header.fileFeatures.isEmpty())
        assertTrue(!header.isCjmpCommonPart)
    }

    @Test
    fun `format version gate rejects missing or unsupported versions`() {
        val missing = CjmpCommonPartLoadGate.checkFormatVersion("sample.cjmp", null)
        assertNotNull(missing)
        assertEquals(org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnosticKind.CJO_VERSION, missing.kind)

        val tooNew = CjmpCommonPartLoadGate.checkFormatVersion(
            "sample.cjmp",
            CjoModuleVersion(major = 0u, minor = 2u, patch = 0u),
        )
        assertNotNull(tooNew)

        val wrongMajor = CjmpCommonPartLoadGate.checkFormatVersion(
            "sample.cjmp",
            CjoModuleVersion(major = 1u, minor = 0u, patch = 0u),
        )
        assertNotNull(wrongMajor)

        assertNull(
            CjmpCommonPartLoadGate.checkFormatVersion("sample.cjmp", CjmpCommonPartLoadGate.supportedVersion),
        )
    }

    @Test
    fun `package name gate reports mismatch`() {
        assertNull(CjmpCommonPartLoadGate.checkPackageName("sample.cjmp", "sample.cjmp"))
        val mismatch = CjmpCommonPartLoadGate.checkPackageName("sample.cjmp", "other.pkg")
        assertNotNull(mismatch)
        assertEquals(org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnosticKind.WRONG_PACKAGE, mismatch.kind)
    }

    @Test
    fun `features gate requires common features to be a subset of the child set`() {
        val commonFileFeatures = mapOf("common.cj" to listOf(listOf("Foo"), listOf("Bar", "Baz")))
        assertTrue(
            CjmpCommonPartLoadGate.checkFeaturesSubset(
                packageName = "sample.cjmp",
                commonFileFeatures = commonFileFeatures,
                specificFeatures = setOf("Foo", "Bar.Baz", "Extra"),
            ).isEmpty(),
        )
        val notSubset = CjmpCommonPartLoadGate.checkFeaturesSubset(
            packageName = "sample.cjmp",
            commonFileFeatures = commonFileFeatures,
            specificFeatures = setOf("Foo"),
        ).single()
        assertEquals(
            org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnosticKind.FEATURE_NOT_SUBSET,
            notSubset.kind,
        )
        assertTrue(notSubset.message.contains("'Bar.Baz'"), notSubset.message)
    }

    @Test
    fun `options gate reports missing and mismatched compile options`() {
        val missing = CjmpCommonPartLoadGate.checkOptions(
            packageName = "sample.cjmp",
            options = null,
            debug = false,
            optLevel = 0u,
        )
        assertEquals(1, missing.size)
        assertTrue(!missing.single().isError, "missing options is a warning (module_common_cjo_no_options)")

        val mismatched = CjmpCommonPartLoadGate.checkOptions(
            packageName = "sample.cjmp",
            options = CjoModuleOptionInfo(debug = true, optLevel = 2u),
            debug = false,
            optLevel = 1u,
        )
        assertEquals(2, mismatched.size)

        assertTrue(
            CjmpCommonPartLoadGate.checkOptions(
                packageName = "sample.cjmp",
                options = CjoModuleOptionInfo(debug = false, optLevel = 1u),
                debug = false,
                optLevel = 1u,
            ).isEmpty(),
        )
    }

    @Test
    fun `cjo manager refuses a cjo whose package name does not match the request`() {
        val root = Files.createTempDirectory("cjmp-cjo-gate")
        try {
            // 旧路径约定：`sample/cjmp.cjo` 对应包 `sample.cjmp`，但包头写的是别的包名。
            val target: Path = root.resolve(CjoConstants.packageNameToPath("sample.cjmp"))
            target.parent.createDirectories()
            Files.write(
                target,
                CjoPackageWriter.toByteArray(cjmpMetadata(packageName = "other.pkg")),
            )

            val manager = CjoManager(
                CjoSearchPath { name -> if (name == "CANGJIE_LIBRARY") root.toString() else null },
            )
            assertNull(manager.loadPackageSnapshot("sample.cjmp"), "mismatched package must be refused")

            val diagnostics = manager.loadDiagnostics("sample.cjmp")
            assertEquals(1, diagnostics.size)
            assertEquals(
                org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnosticKind.WRONG_PACKAGE,
                diagnostics.single().kind,
            )
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `cjo manager accepts a cjmp cjo with a matching package name`() {
        val root = Files.createTempDirectory("cjmp-cjo-ok")
        try {
            val target: Path = root.resolve(CjoConstants.packageNameToPath("sample.cjmp"))
            target.parent.createDirectories()
            Files.write(target, CjoPackageWriter.toByteArray(cjmpMetadata()))

            val manager = CjoManager(
                CjoSearchPath { name -> if (name == "CANGJIE_LIBRARY") root.toString() else null },
            )
            val loaded = assertNotNull(manager.loadPackageSnapshot("sample.cjmp"))
            assertTrue(loaded.header.isCjmpCommonPart)
            assertEquals(CjoModuleOptionInfo(debug = true, optLevel = 2u), loaded.header.options)
            assertTrue(manager.loadDiagnostics("sample.cjmp").isEmpty())
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
