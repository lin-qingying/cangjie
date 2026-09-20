package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.ConstValue
import PackageFormat.DeclKind
import PackageFormat.Package
import PackageFormat.TypeKind
import PackageFormat.VarInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjoArrayConstValue
import org.cangnova.cangjie.cfir.serialization.cjo.CjoCompositeConstValue
import org.cangnova.cangjie.cfir.serialization.cjo.CjoCompositeValueFieldMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoCompositeValueMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFloat64ConstValue
import org.cangnova.cangjie.cfir.serialization.cjo.CjoInt64ConstValue
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoStringConstValue
import org.cangnova.cangjie.cfir.serialization.cjo.CjoUInt8ConstValue
import org.cangnova.cangjie.cfir.serialization.cjo.CjoVariableInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.cfir.serialization.cjo.CjoTypeMetadata
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith

/** Locks the generated-flatc adapter for every non-table ConstValue shape. */
class CjoConstValueReaderTest {
    @Test
    fun `reads scalar string array and composite const values`() {
        val declarations = listOf(
            CjoPackageDeclaration(
                identifier = "answer",
                kind = DeclKind.VarDecl,
                info = CjoVariableInfo(constValue = CjoInt64ConstValue(42L)),
            ),
            CjoPackageDeclaration(
                identifier = "text",
                kind = DeclKind.VarDecl,
                info = CjoVariableInfo(constValue = CjoStringConstValue("hello")),
            ),
            CjoPackageDeclaration(
                identifier = "values",
                kind = DeclKind.VarDecl,
                info = CjoVariableInfo(
                    constValue = CjoArrayConstValue(
                        listOf(CjoUInt8ConstValue(1u), CjoFloat64ConstValue(2.5)),
                    ),
                ),
            ),
            CjoPackageDeclaration(
                identifier = "composite",
                kind = DeclKind.VarDecl,
                info = CjoVariableInfo(constValue = CjoCompositeConstValue(0u)),
            ),
        )
        val packageData = Package.getRootAsPackage(
            ByteBuffer.wrap(
                CjoPackageWriter.toByteArray(
                    CjoPackageMetadata(
                        fullPackageName = "sample.pkg",
                        moduleName = "sample",
                        declarations = declarations,
                        types = listOf(CjoTypeMetadata(kind = TypeKind.Unit)),
                        values = listOf(
                            CjoCompositeValueMetadata(
                                type = 1u,
                                fields = listOf(
                                    CjoCompositeValueFieldMetadata(
                                        name = "value",
                                        type = 1u,
                                        value = CjoInt64ConstValue(42L),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val answer = CjoConstValueReader.readRaw(
            requireNotNull(packageData.allDecls(0)).info(VarInfo()) as VarInfo,
        )
        assertEquals(CjoConstValueReader.Value.Scalar(ConstValue.Int64Value, 42L), answer)

        val text = CjoConstValueReader.readRaw(
            requireNotNull(packageData.allDecls(1)).info(VarInfo()) as VarInfo,
        )
        assertEquals(CjoConstValueReader.Value.StringValue("hello"), text)

        val values = assertIs<CjoConstValueReader.Value.ArrayValue>(
            CjoConstValueReader.readRaw(
                requireNotNull(packageData.allDecls(2)).info(VarInfo()) as VarInfo,
            ),
        )
        assertEquals(
            listOf(
                CjoConstValueReader.Value.Scalar(ConstValue.UInt8Value, 1u.toUByte()),
                CjoConstValueReader.Value.Scalar(ConstValue.Float64Value, 2.5),
            ),
            values.elements,
        )

        assertEquals(
            CjoConstValueReader.Value.CompositeReference(0u),
            CjoConstValueReader.readRaw(
                requireNotNull(packageData.allDecls(3)).info(VarInfo()) as VarInfo,
            ),
        )
        val expandedComposite = assertIs<CjoConstValueReader.Value.CompositeValue>(
            CjoConstValueReader.read(
                requireNotNull(packageData.allDecls(3)).info(VarInfo()) as VarInfo,
                packageData,
            ),
        )
        assertEquals(1u, expandedComposite.type)
        assertEquals(
            listOf(
                CjoConstValueReader.Value.CompositeValue.Field(
                    name = "value",
                    type = 1u,
                    value = CjoConstValueReader.Value.Scalar(ConstValue.Int64Value, 42L),
                ),
            ),
            expandedComposite.fields,
        )
        assertEquals(1, packageData.allValuesLength)
        assertEquals(1u, packageData.allValues(0)?.type)
        assertEquals("value", packageData.allValues(0)?.fields(0)?.field)
    }

    @Test
    fun `reads empty and nested composite values from the official allValues pool`() {
        val packageData = Package.getRootAsPackage(
            ByteBuffer.wrap(
                CjoPackageWriter.toByteArray(
                    CjoPackageMetadata(
                        fullPackageName = "sample.pkg",
                        moduleName = "sample",
                        declarations = listOf(
                            CjoPackageDeclaration(
                                identifier = "empty",
                                kind = DeclKind.VarDecl,
                                info = CjoVariableInfo(constValue = CjoCompositeConstValue(0u)),
                            ),
                            CjoPackageDeclaration(
                                identifier = "nested",
                                kind = DeclKind.VarDecl,
                                info = CjoVariableInfo(constValue = CjoCompositeConstValue(1u)),
                            ),
                        ),
                        types = listOf(
                            CjoTypeMetadata(kind = TypeKind.Unit),
                            CjoTypeMetadata(kind = TypeKind.Unit),
                        ),
                        values = listOf(
                            CjoCompositeValueMetadata(type = 1u),
                            CjoCompositeValueMetadata(
                                type = 2u,
                                fields = listOf(
                                    CjoCompositeValueFieldMetadata(
                                        name = "inner",
                                        type = 2u,
                                        value = CjoCompositeConstValue(0u),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val valueInfo = requireNotNull(packageData.allDecls(0)).info(VarInfo()) as VarInfo
        val value = assertIs<CjoConstValueReader.Value.CompositeValue>(
            CjoConstValueReader.read(valueInfo, packageData),
        )
        assertEquals(1u, value.type)
        assertEquals(emptyList(), value.fields)

        val nested = CjoConstValueReader.Value.CompositeValue.Field(
            name = "inner",
            type = 2u,
            value = CjoConstValueReader.Value.CompositeValue(
                index = 0u,
                type = 1u,
                fields = emptyList(),
            ),
        )
        val nestedValue = assertIs<CjoConstValueReader.Value.CompositeValue>(
            CjoConstValueReader.read(
                requireNotNull(packageData.allDecls(1)).info(VarInfo()) as VarInfo,
                packageData,
            ),
        )
        assertEquals(nested, nestedValue.fields.single())
    }

    @Test
    fun `rejects a composite value index outside the official allValues pool`() {
        assertFailsWith<IllegalArgumentException> {
            CjoPackageWriter.toByteArray(
                CjoPackageMetadata(
                    fullPackageName = "sample.pkg",
                    moduleName = "sample",
                    declarations = listOf(
                        CjoPackageDeclaration(
                            identifier = "value",
                            kind = DeclKind.VarDecl,
                            info = CjoVariableInfo(constValue = CjoCompositeConstValue(3u)),
                        ),
                    ),
                    values = listOf(CjoCompositeValueMetadata(type = 1u)),
                ),
            )
        }
    }
}
