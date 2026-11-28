package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageImport
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

/** 原 immutable routers 经两条 re-export 链到同 fullPkgName 的不同 binding；真实 wire 与 enum 索引保持。 */
class CjoReExportBindingCacheTest {
    @TempDir lateinit var directory: Path

    @Test fun fullCallableAndPromotedEnumLeftThenRightKeepEachOriginalOwner() =
        verify(CjoDeserializationMode.FULL, false)
    @Test fun fullCallableAndPromotedEnumRightThenLeftKeepEachOriginalOwner() =
        verify(CjoDeserializationMode.FULL, true)
    @Test fun signatureCallableAndPromotedEnumLeftThenRightKeepEachOriginalOwner() =
        verify(CjoDeserializationMode.SIGNATURE_ONLY, false)
    @Test fun signatureCallableAndPromotedEnumRightThenLeftKeepEachOriginalOwner() =
        verify(CjoDeserializationMode.SIGNATURE_ONLY, true)

    private fun verify(mode: CjoDeserializationMode, reverse: Boolean) {
        val f = CjoBindingGateFixture(directory)
        val targetFile = f.wire("wire.current")
        // 同一真实来源与 manager 允许两份显式模块图；binding/module/mode records 必须分开。
        val sharedTarget = f.manager(targetFile)
        val sharedSdk = f.manager(f.sdk)
        val sdkLeft = f.frame("sdk-left", setOf(f.sdk), sharedSdk)
        val sdkRight = f.frame("sdk-right", setOf(f.sdk), sharedSdk)
        val left = f.frame("target-left", setOf(targetFile), sharedTarget, sdkLeft.provider)
        val right = f.frame("target-right", setOf(targetFile), sharedTarget, sdkRight.provider)
        fun exports() = listOf(
            CjoPackageImport(listOf("wire", "current"), "first", isDecl = true, withImplicitExport = true),
            CjoPackageImport(listOf("std", "core"), "Type", isDecl = true, withImplicitExport = true),
        )
        val leftFile = f.write("bridge-left.cjo", "bridge.left", imports = listOf("wire.current", "std.core"), exports = exports())
        val rightFile = f.write("bridge-right.cjo", "bridge.right", imports = listOf("wire.current", "std.core"), exports = exports())
        fun router(target: CjoBindingGateFixture.Frame, sdk: CjoBindingGateFixture.Frame) =
            CjoPackageContextProvider { name, requested ->
                val candidates = listOf(target, sdk).mapNotNull { it.provider.getOwnPackageContext(name, requested) }
                check(candidates.size <= 1)
                candidates.singleOrNull()
            }
        val bridgeLeft = f.frame("bridge-owner-left", setOf(leftFile), router = router(left, sdkLeft))
        val bridgeRight = f.frame("bridge-owner-right", setOf(rightFile), router = router(right, sdkRight))
        val rootFile = f.write("root.cjo", "gate.root", imports = listOf("bridge.left", "bridge.right"), exports = listOf(
            CjoPackageImport(listOf("bridge", "left"), "first", "LeftValue", isDecl = true, withImplicitExport = true),
            CjoPackageImport(listOf("bridge", "right"), "first", "RightValue", isDecl = true, withImplicitExport = true),
            CjoPackageImport(listOf("bridge", "left"), "Type", "LeftKind", isDecl = true, withImplicitExport = true),
            CjoPackageImport(listOf("bridge", "right"), "Type", "RightKind", isDecl = true, withImplicitExport = true),
        ))
        val root = f.frame("root-owner", setOf(rootFile), router = CjoPackageContextProvider { name, requested ->
            when (name) {
                "bridge.left" -> bridgeLeft.provider.getOwnPackageContext(name, requested)
                "bridge.right" -> bridgeRight.provider.getOwnPackageContext(name, requested)
                else -> null
            }
        })
        val provider = root.forMode(mode)
        // 在根 provider 的 names cache 建立真实缺失结果；不能遮住另一 binding 的递归 target context。
        assertTrue(provider.symbolNamesProvider.getTopLevelCallableNamesInPackage(FqName("wire.current"))!!.isEmpty())
        val expectedNames = setOf("LeftValue", "RightValue", "LeftKind", "RightKind")
        assertEquals(expectedNames, provider.symbolNamesProvider.getTopLevelCallableNamesInPackage(FqName("gate.root"))!!
            .mapTo(linkedSetOf()) { it.asString() })
        val aliases = if (reverse) listOf("RightValue", "LeftValue", "RightKind", "LeftKind")
            else listOf("LeftValue", "RightValue", "LeftKind", "RightKind")
        val results = aliases.associateWith { alias ->
            val result = provider.getTopLevelCallableSymbols(FqName("gate.root"), Name.identifier(alias)).single()
            val owner = if (alias.startsWith("Left")) left else right
            val sdk = if (alias.startsWith("Left")) sdkLeft else sdkRight
            if (alias.endsWith("Value")) {
                val own = owner.forMode(mode).getTopLevelCallableSymbols(FqName("wire.current"), Name.identifier("first")).single()
                assertSame(own, result)
                assertSame(owner.module, result.cfir.moduleData)
                val wireLeaf = assertIs<CfirPatternBindingSymbol>(result)
                val binding = assertIs<CfirPatternBindingVariable>(wireLeaf.cfir)
                assertSame(wireLeaf, binding.symbol)
                val variable = assertIs<CfirPatternVariable>(binding.patternTreeOwner)
                assertSame(owner.module, variable.moduleData)
                assertSame(binding, variable.pattern.bindingVariables().single())
                assertSame(wireLeaf, variable.pattern.bindingVariables().single().symbol)
                val actualContext = f.context(owner, targetFile, mode)
                val wireIndex = actualContext.header.topLevelCallableNameToIndices.getValue("first").single()
                val actualRoot = assertIs<CfirPatternVariable>(actualContext.createDeclDeserializer().deserializeDecl(wireIndex))
                assertSame(actualRoot, variable)
                val record = actualContext.declCache.getValue(wireIndex)
                assertSame<Any>(wireLeaf, record.wireSymbol)
                assertSame(variable, record.declarationRoot)
                assertSame(variable.initializer, binding.initializer)
                if (mode == CjoDeserializationMode.SIGNATURE_ONLY) assertNull(variable.initializer)
                    else assertNotNull(variable.initializer)
            } else {
                assertIs<CfirEnumConstructorSymbol>(result)
                assertSame(sdk.module, result.cfir.moduleData)
                val enum = assertIs<CfirEnum>(sdk.forMode(mode).getClassLikeSymbolByClassId(
                    ClassId(FqName("std.core"), Name.identifier("AnnotationKind")))!!.cfir)
                assertSame(enum.declarations.filterIsInstance<CfirEnumConstructor>().single { it.name.asString() == "Type" }.symbol, result)
            }
            assertSame(result, provider.getTopLevelCallableSymbols(FqName("gate.root"), Name.identifier(alias)).single())
            result
        }
        assertNotSame(results.getValue("LeftValue"), results.getValue("RightValue"))
        assertNotSame(results.getValue("LeftKind"), results.getValue("RightKind"))
        val leftContext = f.context(left, targetFile, mode)
        val rightContext = f.context(right, targetFile, mode)
        assertSame(leftContext.snapshotIdentity, rightContext.snapshotIdentity)
        assertNotSame(leftContext, rightContext)
        assertNotSame(leftContext.binding, rightContext.binding)
        assertEquals(targetFile.toAbsolutePath().normalize(), leftContext.sourcePath)
        assertEquals(targetFile.toAbsolutePath().normalize(), rightContext.sourcePath)
        assertFalse(root.provider.hasPackage(FqName("wire.current")))
        assertFalse(root.provider.hasPackage(FqName("std.core")))
        val otherMode = if (mode == CjoDeserializationMode.FULL) CjoDeserializationMode.SIGNATURE_ONLY else CjoDeserializationMode.FULL
        assertNotSame(leftContext, f.context(left, targetFile, otherMode))
    }
}
