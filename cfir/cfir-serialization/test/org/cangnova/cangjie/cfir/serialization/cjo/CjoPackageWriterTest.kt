package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.DeclKind
import PackageFormat.AnnoKind
import PackageFormat.DeclInfo
import PackageFormat.FuncInfo
import PackageFormat.FuncTyInfo
import PackageFormat.OverflowPolicy
import PackageFormat.ConstValue
import PackageFormat.VarInfo
import PackageFormat.VarWithPatternInfo
import PackageFormat.PatternKind
import PackageFormat.ExprInfo
import PackageFormat.ExprKind
import PackageFormat.LitConstInfo
import PackageFormat.LitConstKind
import PackageFormat.Package
import PackageFormat.PackageKind
import PackageFormat.ReferenceInfo
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 验证 CJO 包写入器产出的 FlatBuffers 数据可被包头读取器消费。
 */
class CjoPackageWriterTest {
    @Test
    fun `encodes semantic type info with the package builder`() {
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "sample.pkg",
                moduleName = "sample",
                types = listOf(
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Func,
                        typeArguments = listOf(1u),
                        semanticInfo = CjoFunctionTypeInfoMetadata(
                            returnType = 1u,
                            isC = true,
                            hasVariableLenArg = true,
                        ),
                    ),
                    CjoTypeMetadata(kind = PackageFormat.TypeKind.Int64),
                ),
            ),
        )

        val functionType = requireNotNull(Package.getRootAsPackage(ByteBuffer.wrap(bytes)).allTypes(0))
        val info = assertNotNull(functionType.info(FuncTyInfo()) as? FuncTyInfo)
        assertEquals(PackageFormat.SemaTyInfo.FuncTyInfo, functionType.infoType)
        assertEquals(1u, info.retType)
        assertTrue(info.isC)
        assertTrue(info.hasVariableLenArg)
    }

    @Test
    fun `writes VarWithPatternDecl with official VarWithPatternInfo union`() {
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "sample.pkg",
                moduleName = "sample",
                declarations = listOf(
                    CjoPackageDeclaration(
                        identifier = "binding",
                    ),
                    CjoPackageDeclaration(
                        identifier = "owner",
                        kind = DeclKind.VarWithPatternDecl,
                        info = CjoVarWithPatternInfo(
                            isVar = true,
                            irrefutablePattern = CjoPatternMetadata(
                                kind = PatternKind.VarPattern,
                                types = listOf(1u),
                                exprs = listOf(1u),
                            ),
                        ),
                    ),
                ),
                types = listOf(CjoTypeMetadata(kind = PackageFormat.TypeKind.Unit)),
            ),
        )
        val declaration = requireNotNull(Package.getRootAsPackage(ByteBuffer.wrap(bytes)).allDecls(1))
        assertEquals(DeclKind.VarWithPatternDecl, declaration.kind)
        assertEquals(DeclInfo.VarWithPatternInfo, declaration.infoType)
        val info = assertNotNull(declaration.info(VarWithPatternInfo()) as? VarWithPatternInfo)
        assertTrue(info.isVar)
        assertEquals(PatternKind.VarPattern, info.irrefutablePattern?.kind)
    }

    @Test
    fun `rejects VarPattern without declaration and type references`() {
        assertFailsWith<IllegalArgumentException> {
            CjoPackageWriter.toByteArray(
                CjoPackageMetadata(
                    fullPackageName = "sample.pkg",
                    moduleName = "sample",
                    declarations = listOf(
                        CjoPackageDeclaration(
                            identifier = "owner",
                            kind = DeclKind.VarWithPatternDecl,
                            info = CjoVarWithPatternInfo(
                                irrefutablePattern = CjoPatternMetadata(kind = PatternKind.VarPattern),
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `official schema profile rejects repository-only anno target extension`() {
        assertFailsWith<IllegalArgumentException> {
            CjoPackageWriter.toByteArray(
                CjoPackageMetadata(
                    fullPackageName = "sample.pkg",
                    moduleName = "sample",
                    schemaProfile = CjoSchemaProfile.OFFICIAL_V1_0_0,
                    declarations = listOf(
                        CjoPackageDeclaration(
                            identifier = "f",
                            annotations = listOf(
                                CjoAnnotationMetadata(
                                    kind = AnnoKind.Custom,
                                    identifier = "Custom",
                                    target = CjoAnnotationTargetMetadata(decl = "Owner"),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `official cjo format version is independent from language release version`() {
        assertEquals(0, org.cangnova.cangjie.cfir.serialization.CjoConstants.VERSION_MAJOR)
        assertEquals(1, org.cangnova.cangjie.cfir.serialization.CjoConstants.VERSION_MINOR)
        assertEquals(0, org.cangnova.cangjie.cfir.serialization.CjoConstants.VERSION_PATCH)

        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(fullPackageName = "version.pkg", moduleName = "version"),
        )
        val version = requireNotNull(Package.getRootAsPackage(ByteBuffer.wrap(bytes)).cjoVersion)
        assertEquals(0u, version.majorNum)
        assertEquals(1u, version.minorNum)
        assertEquals(0u, version.patchNum)
    }

    /**
     * 验证宏包元数据写入后能被包头读取器还原。
     */
    @Test
    fun `writes macro package metadata readable by package header`() {
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "macros.pkg",
                moduleName = "macro-module",
                kind = PackageKind.Macro,
                version = "1.0.0",
                cjoVersion = CjoFormatVersion(1u, 2u, 3u),
                imports = listOf("std.core"),
                allFiles = listOf("src/macros/pkg/Macro.cj"),
                declarations = listOf(
                    CjoPackageDeclaration("Beta"),
                    CjoPackageDeclaration("Alpha", exportId = "macros.pkg.Alpha"),
                ),
            )
        )

        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        val header = CjoPackageHeader.fromPackage(pkg)

        assertTrue(Package.PackageBufferHasIdentifier(ByteBuffer.wrap(bytes)))
        assertEquals("macros.pkg", header.fullPkgName)
        assertEquals("macro-module", header.moduleName)
        assertEquals(PackageKind.Macro, header.kind)
        assertEquals("1.0.0", pkg.version)
        assertEquals(1u, pkg.cjoVersion?.majorNum)
        assertEquals(2u, pkg.cjoVersion?.minorNum)
        assertEquals(3u, pkg.cjoVersion?.patchNum)
        assertEquals(listOf("std.core"), header.imports)
        assertEquals(listOf("src/macros/pkg/Macro.cj"), header.allFiles)
        assertEquals(listOf("Alpha", "Beta"), header.topLevelCallableNames.map { it.asString() }.sorted())
        assertEquals(2, header.topLevelNameToIndices.size)
        assertEquals(1, header.fullIdReferenceKeyToIndex["macros.pkg.Alpha"])
    }

    /**
     * 验证分类器声明和文件导入元数据会写入 CJO 包。
     */
    @Test
    fun `writes classifier declarations and file import metadata`() {
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "sample.pkg",
                moduleName = "sample",
                allFiles = listOf("src/sample/pkg/first.cj", "src/sample/pkg/second.cj"),
                fileImports = listOf(
                    CjoPackageFileImports(
                        imports = listOf(
                            CjoPackageImport(
                                prefixPaths = listOf("org", "sample"),
                                identifier = "Thing",
                                alias = "AliasThing",
                                hasDoubleColon = true,
                            ),
                            CjoPackageImport(
                                prefixPaths = listOf("sample", "star"),
                                identifier = "*",
                            ),
                        ),
                    ),
                ),
                declarations = listOf(
                    CjoPackageDeclaration("Box", kind = DeclKind.ClassDecl),
                    CjoPackageDeclaration("makeBox", kind = DeclKind.FuncDecl),
                ),
            )
        )

        val header = CjoPackageHeader.fromPackage(Package.getRootAsPackage(ByteBuffer.wrap(bytes)))

        assertEquals(listOf("org::sample.Thing as AliasThing", "sample.star.*"), header.decompiledImportTexts)
        assertEquals(setOf("Box"), header.topLevelClassNames.mapTo(mutableSetOf()) { it.asString() })
        assertEquals(setOf("makeBox"), header.topLevelCallableNames.mapTo(mutableSetOf()) { it.asString() })
    }

    /** Decl 的 attributes、annotation kind、参数映射和目标引用必须进入 CJO。 */
    @Test
    fun `writes declaration semantic annotation metadata`() {
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "sample.pkg",
                moduleName = "sample",
                schemaProfile = CjoSchemaProfile.REPOSITORY_EXTENDED,
                declarations = listOf(
                    CjoPackageDeclaration(
                        identifier = "f",
                        attributes = listOf(0x20UL),
                        annotations = listOf(
                            CjoAnnotationMetadata(
                                kind = AnnoKind.Deprecated,
                                identifier = "Deprecated",
                                arguments = listOf(CjoAnnotationArgumentMetadata("message", 7u)),
                                target = CjoAnnotationTargetMetadata(decl = "Owner"),
                            ),
                        ),
                    ),
                ),
                // AnnoArg.expr is a 1-based reference.  Raw index 7 therefore
                // resolves to allExprs[6], which must be a real literal node.
                expressions = List(6) { CjoExpressionMetadata() } +
                    CjoExpressionMetadata(
                        kind = ExprKind.LitConstExpr,
                        literal = CjoLiteralExpressionMetadata(
                            value = "message",
                            constKind = LitConstKind.String,
                        ),
                    ),
            ),
        )

        val declaration = Package.getRootAsPackage(ByteBuffer.wrap(bytes)).allDecls(0)
        requireNotNull(declaration)
        assertEquals(1, declaration.attributesLength)
        assertEquals(0x20UL, declaration.attributes(0))
        assertEquals(1, declaration.annotationsLength)
        val annotation = requireNotNull(declaration.annotations(0))
        assertEquals(AnnoKind.Deprecated, annotation.kind)
        assertEquals("Deprecated", annotation.identifier)
        assertEquals(1, annotation.argsLength)
        assertEquals("message", annotation.args(0)?.name)
        assertEquals(7u, annotation.args(0)?.expr)
        assertEquals("Owner", annotation.target?.decl)
        assertEquals(7, Package.getRootAsPackage(ByteBuffer.wrap(bytes)).allExprsLength)
        val expression = requireNotNull(Package.getRootAsPackage(ByteBuffer.wrap(bytes)).allExprs(6))
        assertEquals(ExprKind.LitConstExpr, expression.kind)
        assertEquals(ExprInfo.LitConstInfo, expression.infoType)
        assertEquals("message", (expression.info(LitConstInfo()) as LitConstInfo).strValue)
    }

    @Test
    fun `writes declaration info union for ffi and overflow facts`() {
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "sample.pkg",
                moduleName = "sample",
                schemaProfile = CjoSchemaProfile.REPOSITORY_EXTENDED,
                declarations = listOf(
                    CjoPackageDeclaration(
                        identifier = "x",
                        kind = DeclKind.FuncParam,
                        type = 1u,
                    ),
                    CjoPackageDeclaration(
                        identifier = "nativeCall",
                        type = 1u,
                        info = CjoFunctionInfo(
                            overflowStrategy = CangjieOverflowStrategy.SATURATING,
                            body = CjoFunctionBodyInfo(
                                parameterLists = listOf(listOf(1u)),
                                desugaredParameterLists = listOf(listOf(0u)),
                                returnType = 1u,
                            ),
                            isConst = true,
                            isInline = true,
                            isFastNative = true,
                        ),
                    ),
                ),
                types = listOf(CjoTypeMetadata(kind = PackageFormat.TypeKind.Int64)),
            ),
        )

        val packageData = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        val declaration = requireNotNull(packageData.allDecls(1))
        assertEquals(DeclInfo.FuncInfo, declaration.infoType)
        assertEquals(1u, declaration.type)
        val info = declaration.info(FuncInfo()) as FuncInfo
        assertNotNull(info.funcBody)
        assertEquals(1, info.funcBody!!.paramListsLength)
        assertEquals(1u, info.funcBody!!.paramLists(0)!!.params(0))
        assertEquals(1u, info.funcBody!!.retType)
        assertEquals(OverflowPolicy.Saturating, info.overflowPolicy)
        assertTrue(info.isConst)
        assertTrue(info.isInline)
        assertTrue(info.isFastNative)
    }

    @Test
    fun `rejects declaration info union with incompatible declaration kind`() {
        assertFailsWith<IllegalArgumentException> {
            CjoPackageDeclaration(
                identifier = "bad",
                kind = DeclKind.VarDecl,
                info = CjoFunctionInfo(),
            )
        }
    }

    @Test
    fun `rejects parameter desugaring vectors with different arity`() {
        assertFailsWith<IllegalArgumentException> {
            CjoPackageWriter.toByteArray(
                CjoPackageMetadata(
                    fullPackageName = "sample.pkg",
                    moduleName = "sample",
                    declarations = listOf(
                        CjoPackageDeclaration(
                            identifier = "f",
                            info = CjoFunctionInfo(
                                body = CjoFunctionBodyInfo(
                                    parameterLists = listOf(listOf(1u)),
                                    desugaredParameterLists = listOf(emptyList()),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `writes variable int64 const value union`() {
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "sample.pkg",
                moduleName = "sample",
                declarations = listOf(
                    CjoPackageDeclaration(
                        identifier = "answer",
                        kind = DeclKind.VarDecl,
                        info = CjoVariableInfo(
                            isConst = true,
                            constValue = CjoInt64ConstValue(42L),
                        ),
                    ),
                ),
            ),
        )
        val declaration = requireNotNull(Package.getRootAsPackage(ByteBuffer.wrap(bytes)).allDecls(0))
        val info = declaration.info(VarInfo()) as VarInfo
        assertEquals(ConstValue.Int64Value, info.valueType)
    }

    @Test
    fun `writes annotation expression pool for arrays and references`() {
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "sample.pkg",
                moduleName = "sample",
                schemaProfile = CjoSchemaProfile.REPOSITORY_EXTENDED,
                declarations = listOf(
                    CjoPackageDeclaration(
                        identifier = "AnnotationType",
                        annotations = listOf(
                            CjoAnnotationMetadata(
                                kind = AnnoKind.Annotation,
                                identifier = "Annotation",
                                arguments = listOf(CjoAnnotationArgumentMetadata(expr = 3u)),
                            ),
                        ),
                    ),
                ),
                expressions = listOf(
                    CjoExpressionMetadata(
                        kind = ExprKind.LitConstExpr,
                        literal = CjoLiteralExpressionMetadata(
                            value = "Target",
                            constKind = LitConstKind.String,
                        ),
                    ),
                    CjoExpressionMetadata(
                        kind = ExprKind.RefExpr,
                        reference = CjoReferenceExpressionMetadata(reference = "Target"),
                    ),
                    CjoExpressionMetadata(
                        kind = ExprKind.ArrayLit,
                        operands = listOf(1u, 2u),
                    ),
                ),
            ),
        )

        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        assertEquals(3, pkg.allExprsLength)
        assertEquals(ExprKind.RefExpr, pkg.allExprs(1)?.kind)
        assertEquals("Target", (pkg.allExprs(1)?.info(ReferenceInfo()) as ReferenceInfo).reference)
        assertEquals(ExprKind.ArrayLit, pkg.allExprs(2)?.kind)
        assertEquals(listOf(1u, 2u), (0 until pkg.allExprs(2)!!.operandsLength)
            .map { pkg.allExprs(2)!!.operands(it) })
        assertEquals(3u, pkg.allDecls(0)?.annotations(0)?.args(0)?.expr)
    }

    /**
     * 验证写入器会创建目标路径并把 CJO 文件写到指定位置。
     */
    @Test
    fun `writes cjo file to target path`() {
        val dir = Files.createTempDirectory("cjo-writer-test")
        val path = dir.resolve(Path.of("nested", "macro.cjo"))

        CjoPackageWriter.write(
            path,
            CjoPackageMetadata(
                fullPackageName = "written.pkg",
                moduleName = "written",
                kind = PackageKind.Macro,
                declarations = listOf(CjoPackageDeclaration("Generated")),
            ),
        )

        val header = CjoPackageHeader.fromPackage(Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(path))))
        assertEquals("written.pkg", header.fullPkgName)
        assertEquals(setOf("Generated"), header.topLevelCallableNames.mapTo(mutableSetOf()) { it.asString() })
    }
}
