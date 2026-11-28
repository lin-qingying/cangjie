@file:OptIn(ExperimentalUnsignedTypes::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.test.bindCjoFixture
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import PackageFormat.Package
import PackageFormat.TypeKind
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirTypeDeserializer
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.withArguments
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

/** 固定 v113 ArrayTy/ReferenceLoader 合同：一个叶子槽、unsigned dims，独立于 VArray size。 */
class CjoRawArrayLayoutTest {
    @Test
    fun loadedArrayKeepsNativeUnsignedDimensionsAndIdentity() {
        for (dimensions in listOf(0u, 1u, 2u, 3u, UInt.MAX_VALUE)) {
            val decoder = decoder(dimensions)
            val type = assertIs<ConeClassLikeType>(decoder.deserializeTypeFromField(1u))
            val layout = requireNotNull(type.rawArrayLayout)
            assertEquals(StdlibClassIds.RawArray, type.classId)
            assertEquals(dimensions, type.rawArrayDimensions)
            assertEquals(dimensions, layout.dimensions)
            assertSame(ConePrimitiveType.INT64, layout.element)
            assertEquals(1, type.typeArguments.size)
            assertSame(type, decoder.deserializeTypeFromField(1u))
        }
    }

    @Test
    fun argumentReplacementAndNestedConstructionKeepThePublishedLayout() {
        val loaded = assertIs<ConeClassLikeType>(decoder(2u).deserializeTypeFromField(1u))
        val substituted = loaded.withArguments(listOf(ConePrimitiveType.BOOLEAN))
        assertEquals(2u, requireNotNull(substituted.rawArrayLayout).dimensions)
        assertSame(ConePrimitiveType.BOOLEAN, requireNotNull(substituted.rawArrayLayout).element)
        assertNotEquals(substituted, ConeClassLikeType(StdlibClassIds.RawArray.toLookupTag(), listOf(ConePrimitiveType.BOOLEAN)))

        val nested = ConeClassLikeType(StdlibClassIds.RawArray.toLookupTag(), listOf(loaded))
        assertEquals(3u, requireNotNull(nested.rawArrayLayout).dimensions)
        assertSame(ConePrimitiveType.INT64, requireNotNull(nested.rawArrayLayout).element)
        assertSame(loaded, nested.typeArguments.single())
    }

    private fun decoder(dimensions: UInt): CfirTypeDeserializer {
        val bytes = CjoPackageWriter.toByteArray(CjoPackageMetadata(
            fullPackageName = "raw.layout",
            moduleName = "raw.layout",
            declarations = emptyList(),
            types = listOf(
                CjoTypeMetadata(TypeKind.Array, listOf(2u), semanticInfo = CjoArrayTypeInfoMetadata(dimensions.toLong())),
                CjoTypeMetadata(TypeKind.Int64),
            ),
        ))
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        val session = object : CfirSession(Kind.Library) {}.apply {
            register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
        }
        val module = CfirSourceModuleData(Name.identifier("raw-layout"), emptyList(), emptyList(),
            CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT).also { it.bindSession(session) }
        val manager = CjoManager(CjoSearchPath { null }, CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT))
        val binding = bindCjoFixture(module, manager, emptySet())
        val context = CfirDeserializationContext(
            pkg, CjoPackageHeader.fromPackage(pkg), module, manager,
            classSymbolLookup = manager.deserializedSymbolLookups.forBinding(binding, module, CjoDeserializationMode.FULL),
        )
        return CfirTypeDeserializer(context)
    }
}
