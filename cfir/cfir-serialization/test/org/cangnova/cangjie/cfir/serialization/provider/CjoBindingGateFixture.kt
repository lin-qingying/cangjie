package org.cangnova.cangjie.cfir.serialization.provider

import PackageFormat.DeclKind
import PackageFormat.FullId
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.*
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeclarationMaterialization
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.test.cjoFixtureModuleDataProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64

/** 真实磁盘 CJO、原 named producer 与显式 roots；不替换 provider/声明图或内部 record。 */
internal class CjoBindingGateFixture(val directory: Path) {
    val transaction = CjoDeclarationMaterialization()
    val repository: Path = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
        .first { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
    val sdk: Path = repository.resolve("cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative")

    fun manager(vararg roots: Path): CjoManager =
        CjoManager(CjoSearchPath.forRoots(emptyList(), roots.map { it.toFile() }), transaction)

    fun wire(name: String, target: Path = directory.resolve(name + ".cjo")): Path {
        val encoded = Files.readString(repository.resolve("cfir/cfir-serialization/testResources/cjo-wire/" + name + ".cjo.b64"))
        return Files.write(target, Base64.getDecoder().decode(encoded.trim()))
    }

    fun write(file: String, name: String, declarations: List<CjoPackageDeclaration> = emptyList(),
        imports: List<String> = emptyList(), exports: List<CjoPackageImport> = emptyList()): Path =
        directory.resolve(file).also {
            CjoPackageWriter.write(it, CjoPackageMetadata(
                fullPackageName = name, moduleName = "binding-gate", imports = imports,
                declarations = declarations,
                fileImports = if (exports.isEmpty()) emptyList() else listOf(CjoPackageFileImports(exports)),
            ))
        }

    fun classDeclaration(name: String, exportId: String = "gate." + name): CjoPackageDeclaration =
        CjoPackageDeclaration(name, kind = DeclKind.ClassDecl, exportId = exportId, info = CjoClassInfo(),
            attributes = List(org.cangnova.cangjie.metadata.model.Attribute.PUBLIC.ordinal / Long.SIZE_BITS + 1) { word ->
                if (word == org.cangnova.cangjie.metadata.model.Attribute.PUBLIC.ordinal / Long.SIZE_BITS)
                    1uL shl (org.cangnova.cangjie.metadata.model.Attribute.PUBLIC.ordinal % Long.SIZE_BITS) else 0uL
            })

    fun frame(name: String, roots: Set<Path>, manager: CjoManager = manager(*roots.toTypedArray()),
        router: CjoPackageContextProvider? = null, implicitAnnotations: Set<FqName> = emptySet()): Frame {
        val module = Module(name, implicitAnnotations)
        val provider = CfirDeserializedSymbolProvider(module.session, manager, module.session.cangjieScopeProvider, module,
            moduleDataProvider = cjoFixtureModuleDataProvider(module, roots), importedPackageContextProvider = router)
        return Frame(module, manager, provider)
    }

    fun context(frame: Frame, file: Path, mode: CjoDeserializationMode): CfirDeserializationContext {
        val bytes = Files.readAllBytes(file)
        val name = PackageFormat.Package.getRootAsPackage(java.nio.ByteBuffer.wrap(bytes)).fullPkgName!!
        return frame.provider.getOwnPackageContext(name, file, bytes, mode)
    }

    fun importedReference(key: String): FullId {
        val builder = FlatBufferBuilder()
        builder.finish(FullId.createFullId(builder, 0, builder.createString(key), 0u))
        return FullId.getRootAsFullId(builder.dataBuffer())
    }

    class Frame(val module: CfirModuleData, val manager: CjoManager, val provider: CfirDeserializedSymbolProvider) {
        val binding: CjoDeserializationBinding get() = provider.deserializationBinding
        fun forMode(mode: CjoDeserializationMode): CfirDeserializedSymbolProvider =
            manager.deserializedSymbolLookups.forBinding(binding, module, mode)
    }

    private class Module(label: String, annotations: Set<FqName>) : CfirModuleData() {
        override val name = Name.identifier(label)
        override val stableModuleName = label
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val session = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
            it.register(CfirImplicitSystemAnnotationsProvider::class, ConfiguredCfirImplicitSystemAnnotationsProvider(annotations))
        }
        init { bindSession(session) }
    }
}
