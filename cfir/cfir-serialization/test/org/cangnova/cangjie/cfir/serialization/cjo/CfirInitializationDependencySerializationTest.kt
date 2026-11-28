@file:OptIn(
    ExperimentalUnsignedTypes::class,
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.test.cjoFixtureModuleDataProvider
import PackageFormat.DeclKind
import PackageFormat.Package
import PackageFormat.TypeKind
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.deserialization.SingleModuleDataProvider
import org.cangnova.cangjie.cfir.entrypoint.configuration.createForCfirFrontend
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.builder.hasCjmpCommonDefault
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.utils.hasImplicitDefaultUpperBound
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyExpression
import org.cangnova.cangjie.cfir.resolve.providers.CfirBuiltinSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisSettings
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirTypeDeserializer
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.resolve.calls.mpp.CjmpContainerKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpContainerMergeEvaluator
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationOrderEvaluator
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMergeMember
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMergeContainer
import org.cangnova.cangjie.resolve.calls.mpp.CjmpTypeParameterSymbolMarker
import org.cangnova.cangjie.resolve.calls.mpp.CjmpVariableWithPosition
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjInterface
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.cangnova.cangjie.test.frontend.CfirCompilerLazyDeclarationResolverWithPhaseChecking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path

/** common Decl.dependencies 必须保存并恢复原始声明身份，specific CJO 不携带这些边。 */
class CfirInitializationDependencySerializationTest :
    CjParsingTestCase("", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition()) {

    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    @Test
    fun producerRejectsUnexpandedFieldInitializer() {
        for (mode in listOf(CfirCjmpMode.COMMON, CfirCjmpMode.SPECIFIC)) {
            val fixture = sourceFile(mode)
            fixture.target.replaceInitializer(buildLazyExpression())

            val exception = assertThrows(IllegalStateException::class.java) {
                CfirCjoPackageMetadataProducer.produce(listOf(fixture.file))
            }
            assertTrue(exception.message.orEmpty().contains("Source export requires materialized expression regions"))
            assertSame(CjmpInitializationDependencies.NotCollected, fixture.target.initializationDependencies)
        }
    }

    @Test
    fun commonProducerWritesPublishedInstanceDependencyAsCurrentPackageFullId() {
        val fixture = sourceFile(CfirCjmpMode.COMMON)
        requireNotNull(fixture.dependent).publishInitializationDependencies(listOf(fixture.target.symbol))

        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(fixture.file))
        val targetIndex = metadata.declarations.indexOfFirst { it.identifier == "target" } + 1
        val dependentIndex = metadata.declarations.indexOfFirst { it.identifier == "dependent" }
        val dependentMetadata = metadata.declarations[dependentIndex]

        assertEquals(
            listOf(CjoAnnotationTargetMetadata(pkgId = -2, index = targetIndex.toUInt())),
            dependentMetadata.dependencies,
        )
        val pkg = encode(metadata)
        val wireDependent = requireNotNull(pkg.allDecls(dependentIndex))
        assertEquals(1, wireDependent.dependenciesLength)
        assertEquals(-2, wireDependent.dependencies(0)!!.pkgId)
        assertEquals(targetIndex.toUInt(), wireDependent.dependencies(0)!!.index)
    }

    @Test
    fun commonProducerRejectsExternalTypeWithoutProviderDeclaration() {
        val fixture = sourceFile(
            CfirCjmpMode.COMMON,
            targetType = ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.Any)),
            dependencies = { module ->
                // 缺失目标声明是此负例的输入；Object 等真实 header 依赖仍须可用。
                CjoSourceFixtureDependenciesWithoutClassifier(
                    module.session.dependenciesSymbolProvider,
                    StdlibClassIds.Any,
                )
            },
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            CfirCjoPackageMetadataProducer.produce(listOf(fixture.file))
        }
        assertTrue(exception.message.orEmpty().contains("no class-like declaration"))
        assertTrue(exception.message.orEmpty().contains("std.core.Any"))
    }

    @Test
    fun commonProducerRoundTripsStandardLibraryExportIdsFromCjo(@TempDir tempDir: Path) {
        val stdCoreBytes = requireNotNull(
            javaClass.classLoader.getResourceAsStream("cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo"),
        ) { "official std.core CJO test fixture must be available" }.use { it.readBytes() }
        val stdCorePath = tempDir.resolve(CjoConstants.packageNameToPath("std.core"))
        Files.createDirectories(stdCorePath.parent)
        Files.write(stdCorePath, stdCoreBytes)

        val fixture = sourceFile(
            CfirCjmpMode.COMMON,
            targetType = ConeTupleType(
                listOf(
                    ConeStructType(ConeClassLikeLookupTagImpl(StdlibClassIds.String)),
                    ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.Object)),
                ),
            ),
            dependencies = { module ->
                CfirDeserializedSymbolProvider(
                    module.session,
                    CjoManager(
                        CjoSearchPath(additionalLibrarySearchPaths = listOf(tempDir.toFile())),
                        CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT),
                    ),
                    CfirCangJieScopeProvider(),
                    module,
                    moduleDataProvider = cjoFixtureModuleDataProvider(module, setOf(tempDir)),
                )
            },
        )

        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(fixture.file))
        val references = metadata.types
            .mapNotNull { it.semanticInfo as? CjoCompositeTypeInfoMetadata }
            .filter { it.declarationKey in setOf("_CNat6StringE", "_CNat6ObjectE") }

        assertEquals(setOf("_CNat6StringE", "_CNat6ObjectE"), references.mapNotNull { it.declarationKey }.toSet())
        assertTrue(references.all { metadata.imports[it.packageId] == "std.core" })
        assertTrue(references.all { it.declarationIndex == 0u })

        val encoded = encode(metadata)
        val cjoManager = CjoManager(
            CjoSearchPath(additionalLibrarySearchPaths = listOf(tempDir.toFile())),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT),
        )
        val decodedType = CfirTypeDeserializer(
            deserializationContext(encoded, cjoManager, setOf(tempDir)),
        ).deserializeTypeFromField(metadata.declarations.single { it.identifier == "target" }.type)
        val decodedTuple = assertInstanceOf(ConeTupleType::class.java, decodedType)
        val decodedString = assertInstanceOf(ConeStructType::class.java, decodedTuple.elementTypes[0])
        val decodedObject = assertInstanceOf(ConeClassLikeType::class.java, decodedTuple.elementTypes[1])
        assertEquals(
            "std.core.String",
            decodedString.classId.asFqNameString(),
        )
        assertEquals("std.core.Object", decodedObject.classId.asFqNameString())
    }

    @Test
    fun stdCoreProducerInjectsProviderBuiltinsAsPackageRoots() {
        val fixture = sourceFile(
            CfirCjmpMode.COMMON,
            targetType = ConeClassLikeType(
                ConeClassLikeLookupTagImpl(StdlibClassIds.RawArray),
                typeArguments = listOf(ConePrimitiveType(PrimitiveTypeKind.INT64)),
            ),
            targetPackageFqName = FqName("std.core"),
            includeCType = true,
            dependencies = { module ->
                CfirCompositeSymbolProvider(
                    module.session,
                    listOf(CfirBuiltinSymbolProvider(module.session, module), module.session.dependenciesSymbolProvider),
                )
            },
        )

        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(fixture.file))
        val builtins = metadata.declarations.filter { it.kind == DeclKind.BuiltInDecl }
        assertEquals(
            listOf("RawArray", "VArray", "CPointer", "CString", "CFunc"),
            builtins.map { it.identifier },
        )
        assertTrue(builtins.all { it.isTopLevel })
        assertTrue(metadata.imports.isEmpty())

        val officialPackage = Package.getRootAsPackage(
            ByteBuffer.wrap(
                requireNotNull(
                    javaClass.classLoader.getResourceAsStream("cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo"),
                ) { "official std.core CJO test fixture must be available" }.use { it.readBytes() },
            ),
        )
        val officialBuiltins = (0 until officialPackage.allDeclsLength).mapNotNull { index ->
            officialPackage.allDecls(index)?.takeIf { it.kind == PackageFormat.DeclKind.BuiltInDecl }
        }
        assertEquals(5, officialBuiltins.size)
        val initializationCheckedMask = 1uL shl Attribute.INITIALIZATION_CHECKED.ordinal
        officialBuiltins.forEach { official ->
            val emitted = builtins.single { it.identifier == official.identifier }
            assertEquals(
                (0 until official.attributesLength).map { index ->
                    val attributes = official.attributes(index)
                    if (index == 0) attributes and initializationCheckedMask.inv() else attributes
                },
                emitted.attributes,
                "${official.identifier} declaration attributes must match the official std.core package",
            )
            assertEquals(
                0uL,
                emitted.attributes.getOrElse(0) { 0uL } and initializationCheckedMask,
                "initialization-check pass state must not be fabricated for synthetic builtin roots",
            )
            val officialType = requireNotNull(officialPackage.allTypes(official.type.toInt() - 1))
            val emittedType = metadata.types[emitted.type.toInt() - 1]
            assertEquals(officialType.kind, emittedType.kind, "${official.identifier} Decl.type kind")
            assertEquals(
                officialType.typeArgsLength,
                emittedType.typeArguments.size,
                "${official.identifier} Decl.type argument count",
            )
            repeat(officialType.typeArgsLength) { typeArgumentIndex ->
                val officialArgument = requireNotNull(
                    officialPackage.allTypes(officialType.typeArgs(typeArgumentIndex).toInt() - 1),
                )
                val emittedArgument = metadata.types[emittedType.typeArguments[typeArgumentIndex].toInt() - 1]
                assertEquals(
                    officialArgument.kind,
                    emittedArgument.kind,
                    "${official.identifier} Decl.type argument $typeArgumentIndex kind",
                )
                val officialGenericInfo = officialArgument.info(PackageFormat.GenericTyInfo())
                    as? PackageFormat.GenericTyInfo
                if (officialGenericInfo != null) {
                    val emittedGenericInfo = metadata.types[
                        emittedType.typeArguments[typeArgumentIndex].toInt() - 1
                    ].semanticInfo as? CjoGenericTypeInfoMetadata
                    assertEquals(
                        emitted.generic!!.typeParameters.single(),
                        emittedGenericInfo?.declarationIndex,
                        "${official.identifier} Decl.type argument $typeArgumentIndex must refer to its own type parameter",
                    )
                }
            }
            when (officialType.kind) {
                PackageFormat.TypeKind.Array,
                PackageFormat.TypeKind.VArray,
                -> {
                    val officialArrayInfo = requireNotNull(
                        officialType.info(PackageFormat.ArrayTyInfo()) as? PackageFormat.ArrayTyInfo,
                    )
                    val emittedArrayInfo = emittedType.semanticInfo as? CjoArrayTypeInfoMetadata
                    assertEquals(
                        officialArrayInfo.dimsOrSize,
                        emittedArrayInfo?.dimsOrSize,
                        "${official.identifier} Decl.type dimensions/size",
                    )
                }
                PackageFormat.TypeKind.Func -> {
                    val officialFunctionInfo = requireNotNull(
                        officialType.info(PackageFormat.FuncTyInfo()) as? PackageFormat.FuncTyInfo,
                    )
                    val emittedFunctionInfo = emittedType.semanticInfo as? CjoFunctionTypeInfoMetadata
                    assertTrue(officialFunctionInfo.isC, "${official.identifier} Decl.type must be C function")
                    assertEquals(
                        officialFunctionInfo.isC,
                        emittedFunctionInfo?.isC,
                        "${official.identifier} Decl.type C function flag",
                    )
                    assertEquals(
                        officialFunctionInfo.hasVariableLenArg,
                        emittedFunctionInfo?.hasVariableLenArg,
                        "${official.identifier} Decl.type variadic flag",
                    )
                    assertEquals(
                        officialType.typeArgs(0),
                        officialFunctionInfo.retType,
                        "${official.identifier} Decl.type returns its generic parameter",
                    )
                    assertEquals(
                        emittedType.typeArguments.single(),
                        emittedFunctionInfo?.returnType,
                        "${official.identifier} Decl.type returns its generic parameter",
                    )
                }
                else -> assertTrue(
                    emittedType.semanticInfo == null,
                    "${official.identifier} Decl.type must not carry semantic info",
                )
            }
            val officialGeneric = official.generic ?: return@forEach
            val emittedGeneric = requireNotNull(emitted.generic)
            assertEquals(officialGeneric.typeParametersLength, emittedGeneric.typeParameters.size)
            assertEquals(
                officialGeneric.constraintsLength,
                emittedGeneric.constraints.size,
                "${official.identifier} generic constraint count",
            )
            repeat(officialGeneric.typeParametersLength) { index ->
                val officialParameter = requireNotNull(
                    officialPackage.allDecls(officialGeneric.typeParameters(index).toInt() - 1),
                )
                val emittedParameterIndex = emittedGeneric.typeParameters[index].toInt() - 1
                val emittedParameter = metadata.declarations[emittedParameterIndex]
                assertEquals(officialParameter.identifier, emittedParameter.identifier)
                val officialParameterAttributes =
                    (0 until officialParameter.attributesLength).map(officialParameter::attributes)
                val compilerAddedMask = 1uL shl Attribute.COMPILER_ADD.ordinal
                assertTrue(
                    (officialParameterAttributes.getOrElse(0) { 0uL } and compilerAddedMask) != 0uL,
                    "${official.identifier} type parameter must be compiler-added",
                )
                assertEquals(
                    officialParameterAttributes.getOrElse(0) { 0uL } and compilerAddedMask,
                    emittedParameter.attributes.getOrElse(0) { 0uL } and compilerAddedMask,
                    "${official.identifier} type-parameter compiler-added attribute",
                )
                assertEquals(
                    0uL,
                    emittedParameter.attributes.getOrElse(0) { 0uL } and initializationCheckedMask,
                    "initialization-check pass state must not be fabricated for synthetic CFIR declarations",
                )
            }
        }
        assertEquals(
            mapOf(
                "RawArray" to PackageFormat.BuiltInType.Array,
                "VArray" to PackageFormat.BuiltInType.VArray,
                "CPointer" to PackageFormat.BuiltInType.CPointer,
                "CString" to PackageFormat.BuiltInType.CString,
                "CFunc" to PackageFormat.BuiltInType.CFunc,
            ),
            builtins.associate { it.identifier to (it.info as CjoBuiltInInfo).builtInType },
        )
        assertEquals(
            mapOf("RawArray" to 1, "VArray" to 1, "CPointer" to 1, "CString" to 0, "CFunc" to 1),
            builtins.associate { it.identifier to (it.generic?.typeParameters?.size ?: 0) },
        )

        val pointer = builtins.single { it.identifier == "CPointer" }
        val cTypeIndex = metadata.declarations.indexOfFirst { it.identifier == "CType" }.toUInt() + 1u
        val pointerBoundTypeIndex = pointer.generic!!.constraints.single().upperBounds.single()
        val pointerBound = metadata.types[pointerBoundTypeIndex.toInt() - 1]
            .semanticInfo as CjoCompositeTypeInfoMetadata
        assertEquals(cTypeIndex, pointerBound.declarationIndex)

        val encoded = encode(metadata)
        val wireBuiltins = (0 until encoded.allDeclsLength).mapNotNull { index ->
            encoded.allDecls(index)?.takeIf { it.kind == PackageFormat.DeclKind.BuiltInDecl }
        }
        assertEquals(5, wireBuiltins.size)
        assertTrue(wireBuiltins.all { it.infoType == PackageFormat.DeclInfo.BuiltInInfo })
        wireBuiltins.forEach { declaration ->
            val type = requireNotNull(encoded.allTypes(declaration.type.toInt() - 1))
            assertEquals(
                metadata.types[declaration.type.toInt() - 1].kind,
                type.kind,
                "${declaration.identifier} Decl.type must be preserved on the CJO wire",
            )
        }
        assertEquals(
            mapOf(
                "RawArray" to PackageFormat.BuiltInType.Array,
                "VArray" to PackageFormat.BuiltInType.VArray,
                "CPointer" to PackageFormat.BuiltInType.CPointer,
                "CString" to PackageFormat.BuiltInType.CString,
                "CFunc" to PackageFormat.BuiltInType.CFunc,
            ),
            wireBuiltins.associate {
                it.identifier!! to (it.info(PackageFormat.BuiltInInfo()) as PackageFormat.BuiltInInfo).builtInType
            },
        )

        val declarationDeserializer = deserializationContext(encoded).createDeclDeserializer()
        val decodedBuiltins = wireBuiltins.associate { wireDeclaration ->
            val declarationIndex = (0 until encoded.allDeclsLength).single { index ->
                encoded.allDecls(index)?.identifier == wireDeclaration.identifier
            }
            wireDeclaration.identifier!! to assertInstanceOf(
                CfirBuiltInDeclaration::class.java,
                declarationDeserializer.deserializeDecl(declarationIndex),
            )
        }
        listOf("RawArray", "VArray", "CFunc").forEach { name ->
            val parameter = decodedBuiltins.getValue(name).typeParameters
                .filterIsInstance<CfirTypeParameter>()
                .single()
            assertTrue(parameter.hasImplicitDefaultUpperBound, "$name must regain its implicit Any bound")
            val upperBound = assertInstanceOf(ConeClassLikeType::class.java, parameter.bounds.single().coneTypeOrNull)
            assertEquals(StdlibClassIds.Any, upperBound.classId, "$name implicit upper bound")
        }
        val pointerParameter = decodedBuiltins.getValue("CPointer").typeParameters
            .filterIsInstance<CfirTypeParameter>()
            .single()
        assertFalse(pointerParameter.hasImplicitDefaultUpperBound, "CPointer's explicit CType bound must remain explicit")
        val pointerUpperBound = assertInstanceOf(
            ConeClassLikeType::class.java,
            pointerParameter.bounds.single().coneTypeOrNull,
        )
        assertEquals(StdlibClassIds.CType, pointerUpperBound.classId, "CPointer explicit upper bound")
    }

    /** 生产准备沿初始化依赖穿过不输出的真实中间声明，最终 wire 才按保存计划投影。 */
    @Test
    fun exportPreparationRetainsUnplannedInitializationDependencyTargets() {
        val fixture = sourceFile(CfirCjmpMode.COMMON)
        val external = externalInstanceField(fixture.target, "external")
        external.publishInitializationDependencies(listOf(fixture.target.symbol))
        val dependent = requireNotNull(fixture.dependent)
        dependent.publishInitializationDependencies(listOf(external.symbol))
        val roots = fixture.file.declarations.toList()
        val prepared = org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisInputPreparation.prepare(
            listOf(fixture.file), roots, ::r38PhysicalMembers)
        assertTrue(prepared.declarations.any { it === external })
        assertTrue(prepared.declarations.any { it === fixture.target })
        val snapshot = org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis.analyze(
            listOf(fixture.file), roots, ::r38PhysicalMembers, preparedInput = prepared)
        val captured = snapshot.initializationDependenciesOf(external)
        assertEquals(listOf(fixture.target.symbol),
            kotlin.test.assertIs<CjmpInitializationDependencies.Collected<CfirBasedSymbol<*>>>(captured).declarations)
        snapshot.validateSourceUnchanged()
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(fixture.file))
        assertFalse(metadata.declarations.any { it.identifier == "external" })
        val targetIndex = metadata.declarations.indexOfFirst { it.identifier == "target" } + 1
        assertTrue(targetIndex > 0)
        val saved = metadata.declarations.single { it.identifier == "dependent" }
        assertEquals(listOf(CjoAnnotationTargetMetadata(pkgId = -2, index = targetIndex.toUInt())), saved.dependencies)
        snapshot.validateSourceUnchanged()
    }

    @Test
    fun commonProducerProjectsDependenciesThroughDeclarationsOutsideThePackageIndex() {
        val fixture = sourceFile(CfirCjmpMode.COMMON)
        val external = externalInstanceField(fixture.target, "external")
        external.publishInitializationDependencies(listOf(fixture.target.symbol))
        requireNotNull(fixture.dependent).publishInitializationDependencies(listOf(external.symbol))

        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(fixture.file))
        val targetIndex = metadata.declarations.indexOfFirst { it.identifier == "target" } + 1
        val dependentIndex = metadata.declarations.indexOfFirst { it.identifier == "dependent" }

        assertEquals(
            listOf(CjoAnnotationTargetMetadata(pkgId = -2, index = targetIndex.toUInt())),
            metadata.declarations[dependentIndex].dependencies,
        )
    }

    @Test
    fun commonDeserializerRestoresDependencyToCanonicalOwnerChildAfterClassMaterialization() {
        val metadata = commonWireMetadata(
            dependency = CjoAnnotationTargetMetadata(pkgId = -2, index = 2u),
            commonPart = true,
        )
        val pkg = encode(metadata)
        val context = deserializationContext(pkg)

        val owner = assertTrueClass(context.createDeclDeserializer().deserializeDecl(0))
        val target = owner.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == "target" }
        val dependent = owner.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == "dependent" }
        val restored = dependent.initializationDependencies as? CjmpInitializationDependencies.Collected
            ?: error("common CJO dependencies must be restored after owner declarations are materialized")

        assertEquals(1, restored.declarations.size)
        assertSame(target.symbol, restored.declarations.single())
        assertSame(target, context.declCache[1])
        assertRestoredDependencyDrivesInitializationOrder(owner, target, dependent)
    }

    @Test
    fun commonDeserializerKeepsOnlyInstanceFieldEdgesFromSharedCjoDependencyVectors() {
        val metadata = commonWireMetadata(
            dependency = CjoAnnotationTargetMetadata(pkgId = -2, index = 2u),
            commonPart = true,
            additionalFieldDependencies = listOf(
                CjoAnnotationTargetMetadata(pkgId = -2, index = 4u),
                CjoAnnotationTargetMetadata(pkgId = -2, index = 5u),
            ),
            functionDependencies = listOf(CjoAnnotationTargetMetadata(pkgId = -2, index = 2u)),
            includePackageGlobal = true,
        )
        val owner = assertTrueClass(
            deserializationContext(encode(metadata)).createDeclDeserializer().deserializeDecl(0),
        )
        val target = owner.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == "target" }
        val dependent = owner.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == "dependent" }
        val helper = owner.declarations.filterIsInstance<CfirFunction>().single()
        val restored = dependent.initializationDependencies as? CjmpInitializationDependencies.Collected
            ?: error("common instance-field dependencies must be restored")

        assertEquals(listOf(target.symbol), restored.declarations)
        assertSame(CjmpInitializationDependencies.NotCollected, helper.initializationDependencies)
    }

    @Test
    fun commonDeserializerRestoresImportedDependencyThroughProviderCanonicalOwner(@TempDir tempDir: Path) {
        val externalPackageName = "initialization.dependencies.external"
        val consumerPackageName = "initialization.dependencies.consumer"
        tempDir.resolve("external.cjo").toFile().writeBytes(
            CjoPackageWriter.toByteArray(externalCommonWireMetadata(externalPackageName)),
        )
        tempDir.resolve("consumer.cjo").toFile().writeBytes(
            CjoPackageWriter.toByteArray(
                commonWireMetadata(
                    dependency = CjoAnnotationTargetMetadata(pkgId = 0, decl = "external::Holder.value"),
                    commonPart = true,
                    fullPackageName = consumerPackageName,
                    imports = listOf(externalPackageName),
                ),
            ),
        )

        val manager = CjoManager(
            CjoSearchPath(additionalLibrarySearchPaths = listOf(tempDir.toFile())),
            CjoFormatVersionPolicy.Compilation(org.cangnova.cangjie.LanguageVersionSettingsImpl.DEFAULT),
        )
        val module = Module(CfirCjmpMode.NONE, CfirSession.Kind.Library)
        val provider = CfirDeserializedSymbolProvider(
            module.session, manager, CfirCangJieScopeProvider(), module,
            moduleDataProvider = cjoFixtureModuleDataProvider(module, setOf(tempDir)),
        )
        module.session.register(CfirSymbolProvider::class, provider)

        val externalClassId = ClassId.topLevel(FqName(externalPackageName).child(Name.identifier("Holder")))
        val consumerClassId = ClassId.topLevel(FqName(consumerPackageName).child(Name.identifier("Box")))
        val externalOwner = assertTrueClass(provider.getClassLikeSymbolByClassId(externalClassId)?.cfir)
        val externalValue = externalOwner.declarations.filterIsInstance<CfirFieldVariable>().single()
        val consumerOwner = assertTrueClass(provider.getClassLikeSymbolByClassId(consumerClassId)?.cfir)
        val dependent = consumerOwner.declarations
            .filterIsInstance<CfirFieldVariable>()
            .single { it.name.asString() == "dependent" }
        val restored = dependent.initializationDependencies as? CjmpInitializationDependencies.Collected
            ?: error("common CJO imported dependencies must be restored through the owning provider")

        assertEquals(listOf(externalValue.symbol), restored.declarations)
        assertSame(externalValue, provider.getClassLikeSymbolByClassId(externalClassId)!!.cfir.declarations
            .filterIsInstance<CfirFieldVariable>().single())
    }

    @Test
    fun specificProducerAndDeserializerKeepInitializationDependenciesUncollectedOnCjoBoundary() {
        val fixture = sourceFile(CfirCjmpMode.SPECIFIC)
        requireNotNull(fixture.dependent).publishInitializationDependencies(listOf(fixture.target.symbol))

        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(fixture.file))
        assertTrue(metadata.declarations.all { it.dependencies.isEmpty() })

        val specificPkg = encode(
            commonWireMetadata(dependency = null, commonPart = false),
        )
        val owner = assertTrueClass(deserializationContext(specificPkg).createDeclDeserializer().deserializeDecl(0))
        val dependent = owner.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == "dependent" }
        assertSame(CjmpInitializationDependencies.NotCollected, dependent.initializationDependencies)
    }

    private fun sourceFile(
        mode: CfirCjmpMode,
        includeDependent: Boolean = true,
        targetType: ConeCangJieType = ConePrimitiveType.INT64,
        targetPackageFqName: FqName = FqName("initialization.dependencies"),
        includeCType: Boolean = false,
        dependencies: (CfirModuleData) -> CfirSymbolProvider = { it.session.dependenciesSymbolProvider },
    ): SourceFixture {
        val module = Module(mode)
        val dependencyProvider = dependencies(module)
        require(!includeCType || targetPackageFqName == FqName("std.core"))
        val packageName = targetPackageFqName
        val sourceText = (if (mode == CfirCjmpMode.COMMON) COMMON_SOURCE else SPECIFIC_SOURCE)
            .replace("package initialization.dependencies", "package ${packageName.asString()}")
            .let { source ->
                if (includeCType) source.replaceFirst("package ${packageName.asString()}", "package ${packageName.asString()}\ninterface CType {}")
                else source
            }
        val psiFile = createPsiFile(
            "initialization-dependencies.cj",
            sourceText,
        ) as CjFile
        val psiClass = psiFile.declarations.filterIsInstance<CjClass>().single { it.name == "Box" }
        val classId = ClassId.topLevel(packageName.child(Name.identifier("Box")))
        val classSymbol = CfirClassSymbol(classId)
        val classType = ConeClassLikeType(classSymbol.toLookupTag())
        val intType = ConePrimitiveType.INT64

        fun nominalAttributes(psi: CjTypeStatement): CfirDeclarationAttributes = CfirDeclarationAttributes().apply {
            cjmpSourceNominalHasDefault = checkNotNull(psi.cjmpNominalSyntaxFacts) {
                "Source nominal declaration is missing its parser facts."
            }.hasCjmpCommonDefault(module.session.languageVersionSettings)
        }

        fun field(name: String, initializer: org.cangnova.cangjie.cfir.expressions.CfirExpression? = null): CfirFieldVariable {
            val psiField = psiClass.variables.single { it.name == name }
            val symbol = CfirFieldVariableSymbol(CallableId(classId, Name.identifier(name)))
            return buildFieldVariable {
                source = psiField.toCjPsiSourceElement()
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                dispatchReceiverType = classType
                status = CfirDeclarationStatusImpl().apply {
                    isCommon = mode == CfirCjmpMode.COMMON && name == "target"
                    isSpecific = mode == CfirCjmpMode.SPECIFIC
                }
                this.initializer = initializer
                isVar = false
                returnTypeRef = (if (name == "target") targetType else intType).toCfirResolvedTypeRef(null)
                this.name = Name.identifier(name)
                this.symbol = symbol
            }.also(symbol::bind)
        }

        val target = field("target")
        val dependent = if (includeDependent) field("dependent") else null
        val owner = buildClass {
            source = psiClass.toCjPsiSourceElement()
            moduleData = module
            resolvePhase = CfirResolvePhase.IMPORTS
            origin = CfirDeclarationOrigin.Source
            attributes = nominalAttributes(psiClass)
            scopeProvider = CfirCangJieScopeProvider()
            status = CfirDeclarationStatusImpl().apply {
                isCommon = mode == CfirCjmpMode.COMMON
                isSpecific = mode == CfirCjmpMode.SPECIFIC
            }
            name = Name.identifier("Box")
            symbol = classSymbol
            declarations += target
            dependent?.let { declarations += it }
        }.also(classSymbol::bind)

        val cTypeDeclaration = if (includeCType) {
            val psiInterface = psiFile.declarations.filterIsInstance<CjInterface>().single()
            val cTypeClassId = ClassId.topLevel(packageName.child(Name.identifier("CType")))
            val cTypeSymbol = CfirInterfaceSymbol(cTypeClassId)
            buildInterface {
                source = psiInterface.toCjPsiSourceElement()
                moduleData = module
                resolvePhase = CfirResolvePhase.IMPORTS
                origin = CfirDeclarationOrigin.Source
                attributes = nominalAttributes(psiInterface)
                scopeProvider = CfirCangJieScopeProvider()
                status = CfirDeclarationStatusImpl()
                name = Name.identifier("CType")
                symbol = cTypeSymbol
            }.also(cTypeSymbol::bind)
        } else {
            null
        }

        val file = buildFile {
            source = psiFile.toCjPsiSourceElement()
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "initialization-dependencies.cj"
            packageDirective = buildPackageDirective {
                packageFqName = packageName
                isMacroPackage = false
            }
            cTypeDeclaration?.let { declarations += it }
            declarations += owner
        }
        registerCjoSourceFixture(file)
        // 原树只登记一次，最终依赖身份在首次 IMPORTS/SUPER_TYPES 之前固定。
        registerCjoSourceFixtureDependencies(module.session, dependencyProvider)
        resolveCjoSourceFixtureStatuses(module.session, file.declarations, file)
        completeCjoSourceFixtureBodies(file)
        return SourceFixture(file, owner, target, dependent)
    }

    private fun externalInstanceField(source: CfirFieldVariable, name: String): CfirFieldVariable {
        val classId = ClassId.topLevel(FqName("external.initialization.dependencies.ExternalOwner"))
        val symbol = CfirFieldVariableSymbol(CallableId(classId, Name.identifier(name)))
        return buildFieldVariable {
            this.source = source.source
            moduleData = source.moduleData
            resolvePhase = source.resolvePhase
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = source.dispatchReceiverType
            status = CfirDeclarationStatusImpl()
            initializer = null
            isVar = false
            returnTypeRef = source.returnTypeRef
            this.name = Name.identifier(name)
            this.symbol = symbol
        }.also {
            symbol.bind(it)
            resolveCjoSourceFixtureStatuses(source.moduleData.session, listOf(it))
        }
    }

    private fun assertRestoredDependencyDrivesInitializationOrder(
        commonOwner: CfirClass,
        commonTarget: CfirFieldVariable,
        commonDependent: CfirFieldVariable,
    ) {
        commonTarget.publishInitializationDependencies(emptyList())
        val specific = sourceFile(CfirCjmpMode.SPECIFIC, includeDependent = false)
        val noTypeParameters = emptyList<InitializationTypeParameter>()
        val commonContainer = CjmpMergeContainer<CfirBasedSymbol<*>, InitializationTypeParameter>(
            symbol = commonOwner.symbol,
            name = commonOwner.name.asString(),
            kind = CjmpContainerKind.CLASS,
            isCommon = true,
            isSpecific = false,
            hasGeneric = false,
            typeParameters = noTypeParameters,
            isNonExhaustive = false,
            doNotExport = false,
            members = listOf(
                mergeMember(commonTarget, isCommon = true, hasCommonDefault = true, fromCommonPart = true, sourceOrder = 0),
                mergeMember(commonDependent, fromCommonPart = true, sourceOrder = 1),
            ),
        )
        val specificContainer = CjmpMergeContainer<CfirBasedSymbol<*>, InitializationTypeParameter>(
            symbol = specific.owner.symbol,
            name = specific.owner.name.asString(),
            kind = CjmpContainerKind.CLASS,
            isCommon = false,
            isSpecific = true,
            hasGeneric = false,
            typeParameters = noTypeParameters,
            isNonExhaustive = false,
            doNotExport = false,
            members = listOf(mergeMember(specific.target, isSpecific = true, sourceOrder = 0)),
        )
        val graph = CjmpContainerMergeEvaluator.evaluateNominals(listOf(commonContainer, specificContainer))

        assertEquals(
            listOf(
                CjmpVariableWithPosition(specific.target.symbol, 0),
                CjmpVariableWithPosition(commonDependent.symbol, 1),
            ),
            CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions(
                specific.owner.symbol,
                graph,
                emptyMap(),
            ),
        )
    }

    private fun mergeMember(
        field: CfirFieldVariable,
        isCommon: Boolean = false,
        isSpecific: Boolean = false,
        hasCommonDefault: Boolean = false,
        fromCommonPart: Boolean = false,
        sourceOrder: Int,
    ): CjmpMergeMember<CfirBasedSymbol<*>> = CjmpMergeMember(
        symbol = field.symbol,
        name = field.name.asString(),
        isInstanceVariable = true,
        isMemberParameter = false,
        isCommon = isCommon,
        isSpecific = isSpecific,
        hasCommonDefault = hasCommonDefault,
        doNotExport = false,
        dependencies = field.initializationDependencies,
        isFromCommonPart = fromCommonPart,
        sourceOrder = sourceOrder,
    )

    private fun commonWireMetadata(
        dependency: CjoAnnotationTargetMetadata?,
        commonPart: Boolean,
        fullPackageName: String = "initialization.dependencies",
        imports: List<String> = emptyList(),
        additionalFieldDependencies: List<CjoAnnotationTargetMetadata> = emptyList(),
        functionDependencies: List<CjoAnnotationTargetMetadata> = emptyList(),
        includePackageGlobal: Boolean = false,
    ): CjoPackageMetadata {
        val role = if (commonPart) Attribute.FROM_COMMON_PART else Attribute.SPECIFIC
        val attributes = MutableList(role.ordinal / 64 + 1) { 0uL }.apply {
            this[role.ordinal / 64] = 1uL shl (role.ordinal % 64)
        }
        return CjoPackageMetadata(
            fullPackageName = fullPackageName,
            moduleName = "initialization-dependencies-test",
            schemaProfile = if (commonPart) CjoSchemaProfile.OFFICIAL_CJMP else CjoSchemaProfile.OFFICIAL_V1_1_3,
            imports = imports,
            declarations = listOf(
                CjoPackageDeclaration(
                    "Box",
                    kind = DeclKind.ClassDecl,
                    type = 2u,
                    attributes = attributes,
                    info = CjoClassInfo(body = listOf(2u, 3u) + if (functionDependencies.isNotEmpty()) listOf(4u) else emptyList()),
                ),
                CjoPackageDeclaration(
                    "target",
                    kind = DeclKind.VarDecl,
                    isTopLevel = false,
                    type = 1u,
                    attributes = attributes,
                    info = CjoVariableInfo(isVar = false, isMemberParam = true),
                ),
                CjoPackageDeclaration(
                    "dependent",
                    kind = DeclKind.VarDecl,
                    isTopLevel = false,
                    type = 1u,
                    attributes = attributes,
                    info = CjoVariableInfo(isVar = false, isMemberParam = true),
                    dependencies = listOfNotNull(dependency) + additionalFieldDependencies,
                ),
            ) + if (functionDependencies.isNotEmpty()) {
                listOf(
                    CjoPackageDeclaration(
                        "helper",
                        kind = DeclKind.FuncDecl,
                        isTopLevel = false,
                        type = 3u,
                        attributes = attributes,
                        info = CjoFunctionInfo(body = CjoFunctionBodyInfo(returnType = 1u)),
                        dependencies = functionDependencies,
                    ),
                )
            } else {
                emptyList()
            } + if (includePackageGlobal) {
                listOf(
                    CjoPackageDeclaration(
                        "packageValue",
                        kind = DeclKind.VarDecl,
                        type = 1u,
                        attributes = attributes,
                        info = CjoVariableInfo(isVar = false, isMemberParam = false),
                    ),
                )
            } else {
                emptyList()
            },
            types = listOf(
                CjoTypeMetadata(TypeKind.Int64),
                CjoTypeMetadata(TypeKind.Class, semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = 1u)),
                // helper() -> Int64 的完整函数类型独立于 FuncBody.retType。
                CjoTypeMetadata(TypeKind.Func,
                    semanticInfo = CjoFunctionTypeInfoMetadata(returnType = 1u, isC = false, hasVariableLenArg = false)),
            ),
        )
    }

    private fun externalCommonWireMetadata(fullPackageName: String): CjoPackageMetadata {
        val fromCommonPart = Attribute.FROM_COMMON_PART
        val attributes = MutableList(fromCommonPart.ordinal / 64 + 1) { 0uL }.apply {
            this[fromCommonPart.ordinal / 64] = 1uL shl (fromCommonPart.ordinal % 64)
        }
        return CjoPackageMetadata(
            fullPackageName = fullPackageName,
            moduleName = "initialization-dependencies-test",
            schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
            declarations = listOf(
                CjoPackageDeclaration(
                    "Holder",
                    kind = DeclKind.ClassDecl,
                    type = 2u,
                    attributes = attributes,
                    info = CjoClassInfo(body = listOf(2u)),
                ),
                CjoPackageDeclaration(
                    "value",
                    kind = DeclKind.VarDecl,
                    isTopLevel = false,
                    exportId = "external::Holder.value",
                    type = 1u,
                    attributes = attributes,
                    info = CjoVariableInfo(isVar = false, isMemberParam = true),
                ),
            ),
            types = listOf(
                CjoTypeMetadata(TypeKind.Int64),
                CjoTypeMetadata(TypeKind.Class, semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = 1u)),
            ),
        )
    }

    /** 内存中的当前包与真实导入包共用 manager；外包声明只从选定快照的 provider context 取得。 */
    private fun deserializationContext(
        pkg: Package,
        cjoManager: CjoManager = CjoManager(
            CjoSearchPath { null },
            CjoFormatVersionPolicy.Compilation(org.cangnova.cangjie.LanguageVersionSettingsImpl.DEFAULT),
        ),
        ownedRoots: Set<Path> = emptySet(),
    ): CfirDeserializationContext {
        val module = Module(CfirCjmpMode.NONE, CfirSession.Kind.Library)
        val session = module.session
        val provider = CfirDeserializedSymbolProvider(
            session, cjoManager, session.cangjieScopeProvider, module,
            moduleDataProvider = cjoFixtureModuleDataProvider(module, ownedRoots),
        )
        session.register(CfirSymbolProvider::class, provider)
        session.register(org.cangnova.cangjie.cfir.resolve.providers.CfirProvider::class,
            org.cangnova.cangjie.cfir.resolve.providers.CfirLibrarySessionProvider(provider))
        return CfirDeserializationContext(
            pkg,
            CjoPackageHeader.fromPackage(pkg),
            module,
            cjoManager,
            classSymbolLookup = provider,
        )
    }

    private fun encode(metadata: CjoPackageMetadata): Package =
        Package.getRootAsPackage(ByteBuffer.wrap(CjoPackageWriter.toByteArray(metadata)))

    private fun assertTrueClass(declaration: CfirDeclaration?): CfirClass {
        assertTrue(declaration is CfirClass)
        return declaration as CfirClass
    }

    private data class SourceFixture(
        val file: CfirFile,
        val owner: CfirClass,
        val target: CfirFieldVariable,
        val dependent: CfirFieldVariable?,
    )

    private data object InitializationTypeParameter : CjmpTypeParameterSymbolMarker

    private companion object {
        val COMMON_SOURCE = """
            package initialization.dependencies
            public common class Box {
                public common let target: Int64 = 1
                public let dependent: Int64 = 2
                public common init()
            }
        """.trimIndent()

        val SPECIFIC_SOURCE = """
            package initialization.dependencies
            public class Box {
                public let target: Int64 = 1
                public let dependent: Int64 = 2
            }
        """.trimIndent()
    }

    /**
     * 使用正式工厂装配源码、库与共享库会话，模块绑定由工厂唯一完成。
     * 本测试的源码 header 与 CJO alias 已发布完成阶段；所有 lazy 请求仍由正式测试
     * resolver 检查阶段契约，未准备好的类型不能靠空实现静默跳过。
     */
    private class Module(
        mode: CfirCjmpMode,
        kind: CfirSession.Kind = CfirSession.Kind.Source,
    ) : CfirModuleData() {
        override val name = Name.identifier("initialization-dependencies-test")
        private val libraryModule = CfirBinaryDependenciesModuleData(
            Name.identifier("initialization-dependencies-library"),
        )
        override val dependencies: List<CfirModuleData> =
            if (kind == CfirSession.Kind.Source) listOf(libraryModule) else emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = mode == CfirCjmpMode.COMMON
        override val stableModuleName = "initialization-dependencies-test"
        override val session: CfirSession
            get() = checkNotNull(boundSession) { "Initialization dependency fixture session is not assembled" }

        init {
            val factory = CfirDefaultSessionFactory()
            val languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
            val libraryContext = sessionFactoryContext(CfirCjmpMode.NONE)
            val shared = factory.createSharedLibrarySession(
                mainModuleName = name,
                extensionRegistrars = emptyList(),
                languageVersionSettings = languageVersionSettings,
                context = libraryContext,
            )
            when (kind) {
                CfirSession.Kind.Source -> {
                    factory.createLibrarySession(
                        sharedLibrarySession = shared,
                        moduleDataProvider = SingleModuleDataProvider(libraryModule),
                        extensionRegistrars = emptyList(),
                        languageVersionSettings = languageVersionSettings,
                        context = libraryContext,
                    )
                    factory.createSourceSession(
                        moduleData = this,
                        extensionRegistrars = emptyList(),
                        configuration = CompilerConfiguration.createForCfirFrontend(),
                        context = sessionFactoryContext(mode),
                    )
                }
                CfirSession.Kind.Library -> factory.createLibrarySession(
                    sharedLibrarySession = shared,
                    moduleDataProvider = SingleModuleDataProvider(this),
                    extensionRegistrars = emptyList(),
                    languageVersionSettings = languageVersionSettings,
                    context = sessionFactoryContext(mode),
                )
            }
        }

        /**
         * 与 CfirFrontendFacade 的阶段检查装配一致；在工厂公共组件之后、provider 使用之前
         * 注入每个会话自己的检查器。保留真实 alias 的 TYPES 请求与声明 phase 校验。
         */
        private fun sessionFactoryContext(mode: CfirCjmpMode) = CfirDefaultSessionFactory.Context(
            cjoManager = CjoManager(CjoSearchPath(), CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT)),
            cjmpSettings = CfirCjmpSettingsComponent(explicitMode = mode),
            exportSettings = CfirExportAnalysisSettings(),
            registerLibrarySessionComponents = {
                register(CfirLazyDeclarationResolver::class, CfirCompilerLazyDeclarationResolverWithPhaseChecking())
            },
            registerSourceSessionComponents = {
                register(CfirLazyDeclarationResolver::class, CfirCompilerLazyDeclarationResolverWithPhaseChecking())
            },
        )
    }
}
