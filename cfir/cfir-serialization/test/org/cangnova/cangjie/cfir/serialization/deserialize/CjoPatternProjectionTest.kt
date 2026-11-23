package org.cangnova.cangjie.cfir.serialization.deserialize

import org.cangnova.cangjie.test.bindCjoFixture
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.hasSerializedInitializer
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.Base64
import kotlin.test.*

/** 官方 cjc 1.0.5 输出的 fixture；验证全局解构已经展开，声明树与 wire 符号仍须一致。 */
class CjoPatternProjectionTest {
    @Test
    fun declarationRootsAndWireSymbols() {
        val bytes = javaClass.classLoader.getResourceAsStream("cjo-wire/goal.patterns.library.cjo.b64")!!
            .use { Base64.getDecoder().decode(it.reader(Charsets.UTF_8).readText().trim()) }
        val directory = Files.createTempDirectory("cjo-pattern-projection-")
        try {
            Files.write(directory.resolve("goal.patterns.library.cjo"), bytes)
            val manager = CjoManager(CjoSearchPath { key -> if (key == "CANGJIE_LIBRARY") directory.toString() else null })
            val loaded = assertNotNull(manager.loadPackageSnapshot("goal.patterns.library"))
            val module = Module()
            val metadataBinding = bindCjoFixture(module, manager, setOf(directory))
            val context = CfirDeserializationContext(loaded.pkg, loaded.header, module, manager,
                manager.deserializedSymbolLookups.forBinding(metadataBinding, module, CjoDeserializationMode.FULL)).withMode(CjoDeserializationMode.SIGNATURE_ONLY)
            val reader = CfirDeclDeserializer.forCompiledFileStub(context, context.createTypeDeserializer())
            for (name in listOf("firstGlobal", "secondGlobal", "globalLimit")) {
                val index = loaded.header.topLevelNameToIndices.getValue(name).single()
                val root = assertIs<CfirPatternVariable>(reader.deserializeDecl(index))
                val symbol = assertIs<CfirPatternBindingSymbol>(reader.deserializeSymbol(index))
                val binding = root.pattern.bindingVariables().single()
                assertSame(binding.symbol, symbol)
                assertSame(root, binding.patternTreeOwner)
                assertEquals(name, binding.name.asString())
                assertSame(root, reader.deserializeDecl(index))
                assertSame(symbol, reader.deserializeSymbol(index))
                assertSame(root, context.declCache[index]!!.declarationRoot)
                assertSame(symbol, context.declCache[index]!!.wireSymbol)
                assertNull(root.initializer, "signature-only mode must preserve wire facts independently of an initializer subtree")
                if (name == "globalLimit") assertTrue(binding.hasSerializedInitializer)
            }
            val holder = assertIs<CfirClass>(reader.deserializeDecl(loaded.header.topLevelNameToIndices.getValue("Holder").single()))
            val fields = holder.declarations.filterIsInstance<CfirFieldVariable>()
            assertEquals(setOf("memberValue", "sharedValue"), fields.map { it.name.asString() }.toSet())
            assertTrue(fields.single { it.name.asString() == "sharedValue" }.status.isStatic)
            val state = assertIs<CfirEnum>(reader.deserializeDecl(loaded.header.topLevelNameToIndices.getValue("State").single()))
            assertIs<CfirEnumConstructorSymbol>(state.declarations.filterIsInstance<CfirEnumConstructor>().single().symbol)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private class Module : CfirModuleData() {
        override val name = Name.identifier("cjo-pattern-test")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "cjo-pattern-test"
        override val session = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }
        init { bindSession(session) }
    }
}
