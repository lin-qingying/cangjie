package org.cangnova.cangjie.chir.cjmp

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirNominalTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirFieldExpression
import org.cangnova.cangjie.chir.core.expression.ChirIntrinsicKind
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherOperation
import org.cangnova.cangjie.chir.core.expression.ChirOverflowStrategy
import org.cangnova.cangjie.chir.core.expression.ChirTupleExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.allDeclarations
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.serializer.DefaultOfficialChirV113ChirPackageImporter
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113ImportException
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113SemanticPackageDecoder
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113SemanticPackage
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113SemanticValue
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113NominalTypeIdentity
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.pipeline.DefaultChirToLlvmCodeGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class OfficialChirV113ChirPackageImporterTest {
    @Test
    fun `legacy linker implementations retain the shared identity plan entry point`() {
        val semanticPackage = OfficialChirV113SemanticPackageDecoder.decode(
            Files.readAllBytes(rawCommonFixture().toPath()),
        )
        val specificPackage = ChirPackage(
            semanticId = ChirSemanticId("test:legacy-linker-compatibility"),
            name = "legacy_linker_compatibility",
            modules = emptyList(),
        )
        val sharedIdentity = ChirSemanticId("function:shared")
        var receivedIdentities: Set<ChirSemanticId>? = null
        val legacyLinker = object : CjmpLegacyChirPackageLinker {
            override fun link(
                commonSemanticPackage: OfficialChirV113SemanticPackage,
                specificPackage: ChirPackage,
                actualizedIdentities: Set<ChirSemanticId>,
            ): ChirPackage {
                receivedIdentities = actualizedIdentities
                return specificPackage
            }
        }

        assertEquals(
            specificPackage,
            legacyLinker.link(semanticPackage, specificPackage, CjmpChirActualizationPlan(setOf(sharedIdentity))),
        )
        assertEquals(setOf(sharedIdentity), receivedIdentities)
        assertThrows(UnsupportedOperationException::class.java) {
            legacyLinker.link(
                semanticPackage,
                specificPackage,
                CjmpChirActualizationPlan(
                    declarationPairs = setOf(
                        CjmpChirDeclarationPair(ChirSemanticId("common:shared"), ChirSemanticId("specific:shared")),
                    ),
                ),
            )
        }
    }

    @Test
    fun `links complete official common package before lowering a specific caller`() {
        val semanticPackage = OfficialChirV113SemanticPackageDecoder.decode(
            Files.readAllBytes(rawCommonFixture().toPath()),
        )
        val packageId = ChirSemanticId("test:official-cjmp-common-call")
        val common = DefaultOfficialChirV113ChirPackageImporter().importPackage(semanticPackage, packageId)
        val commonFunction = common.modules.flatMap { it.declarations }
            .filterIsInstance<DefaultChirFunctionDeclaration>()
            .single { it.name == "Value" }
        val intType = commonFunction.returnType
        val functionType = ChirResolvedTypeRef(
            ChirFunctionType(commonFunction.parameters.map { it.type }, commonFunction.returnType),
        )
        val call = ChirCallExpression(
            semanticId = ChirSemanticId("test:specific:call-common-value"),
            callee = ChirImportedFunctionValue(commonFunction.semanticId, functionType, commonFunction.name),
            arguments = listOf(ChirConstantValue(ChirSemanticId("test:specific:argument"), intType, "7")),
            resultType = intType,
        )
        val blockId = ChirSemanticId("test:specific:entry")
        val specificCaller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("test:specific:caller"),
            name = "platformCaller",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = blockId,
                    name = "entry",
                    expressions = listOf(call),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("test:specific:return"),
                        returnValue = checkNotNull(call.resultLocalValue),
                    ),
                ),
            ),
            entryBlockId = blockId,
        )
        val specific = ChirPackage(
            semanticId = packageId,
            name = common.name,
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("test:specific:module"),
                    name = "specific",
                    declarations = listOf(specificCaller),
                ),
            ),
        )

        val linked = DefaultCjmpLegacyChirPackageLinker().link(
            commonSemanticPackage = semanticPackage,
            specificPackage = specific,
            actualizedIdentities = emptySet(),
        )
        assertTrue(DefaultChirValidator().validatePackage(linked).issues.isEmpty())

        val generated = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = linked,
                options = CodegenOptions(
                    emitBitcode = false,
                    emitObjectCode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )
        val llvmIr = generated.modules.joinToString("\n") { it.ir }
        assertTrue(llvmIr.contains("call i64 ${commonFunction.semanticId.value}(i64 7)"), llvmIr)
        assertTrue(llvmIr.contains("define i64 ${commonFunction.semanticId.value}(i64"), llvmIr)
    }

    @Test
    fun `links matcher approved nominal generic owner through imported definition identity`() {
        val semanticPackage = OfficialChirV113SemanticPackageDecoder.decode(
            Files.readAllBytes(rawCommonFixture().toPath()),
        )
        val packageId = ChirSemanticId("test:official-cjmp-generic-owner")
        val imported = DefaultOfficialChirV113ChirPackageImporter()
            .importPackageWithIdentities(semanticPackage, packageId)
        val common = imported.chirPackage
        val commonArray = common.allDeclarations
            .filterIsInstance<ChirNominalTypeDeclaration>()
            .single { it.typeDeclarationId == ChirTypeDeclarationId("std.core", "Array") }
        val sourceIdentity = OfficialChirV113NominalTypeIdentity(
            commonArray.typeDeclarationId,
            commonArray.definitionKind,
        )
        assertEquals(commonArray.semanticId, imported.nominalTypeIdsBySourceIdentity[sourceIdentity])

        val matcherArrayId = ChirSemanticId("@common.Array")
        val specificArrayId = ChirSemanticId("specific:Array")
        val specificGenericIdentity = "specific:Array:type-parameter:0"
        val specificArray = when (commonArray) {
            is ChirStructDeclaration -> DefaultChirStructDeclaration(
                semanticId = specificArrayId,
                name = commonArray.name,
                typeDeclarationId = commonArray.typeDeclarationId,
                typeParameters = listOf("U"),
                typeParameterTypes = listOf(
                    ChirResolvedTypeRef(
                        ChirGenericType("U", declarationIndex = 0, semanticIdentity = specificGenericIdentity),
                    ),
                ),
            )

            is ChirClassDeclaration -> DefaultChirClassDeclaration(
                semanticId = specificArrayId,
                name = commonArray.name,
                typeDeclarationId = commonArray.typeDeclarationId,
                typeParameters = listOf("U"),
                typeParameterTypes = listOf(
                    ChirResolvedTypeRef(
                        ChirGenericType("U", declarationIndex = 0, semanticIdentity = specificGenericIdentity),
                    ),
                ),
            )

            else -> error("official std.core/Array has unsupported declaration kind: ${commonArray::class.simpleName}")
        }
        val specific = ChirPackage(
            semanticId = packageId,
            name = common.name,
            modules = emptyList(),
            accessLevel = common.accessLevel,
            importedTypeDefinitions = listOf(specificArray),
        )

        val linked = DefaultCjmpLegacyChirPackageLinker().link(
            commonSemanticPackage = semanticPackage,
            specificPackage = specific,
            plan = CjmpChirActualizationPlan(
                declarationPairs = setOf(CjmpChirDeclarationPair(matcherArrayId, specificArrayId)),
                typeParameterPairs = setOf(CjmpChirTypeParameterPair(matcherArrayId, 0, specificArrayId, 0)),
                commonNominalTypeIdentities = mapOf(matcherArrayId to sourceIdentity),
            ),
        )

        assertTrue(DefaultChirValidator().validatePackage(linked).issues.isEmpty())
        val linkedArray = linked.importedTypeDefinitions
            .filterIsInstance<ChirNominalTypeDeclaration>()
            .single { it.typeDeclarationId == commonArray.typeDeclarationId }
        assertEquals(specificArrayId, linkedArray.semanticId)
        val linkedGeneric = (linkedArray.typeParameterTypes.single() as ChirResolvedTypeRef).type as ChirGenericType
        assertEquals(specificGenericIdentity, linkedGeneric.semanticIdentity)
    }

    @Test
    fun `imports an identity-selected common function from official raw CHIR`() {
        val semanticPackage = OfficialChirV113SemanticPackageDecoder.decode(
            Files.readAllBytes(rawCommonFixture().toPath()),
        )
        val packageId = ChirSemanticId("test:official-cjmp-raw-common")

        val imported = DefaultOfficialChirV113ChirPackageImporter().importFunctions(
            semanticPackage = semanticPackage,
            packageSemanticId = packageId,
            functionIdentities = setOf(ChirSemanticId("@_CN18cjmp_raw_chir_v1135ValueHl")),
        )

        assertEquals("cjmp_raw_chir_v113", imported.name)
        assertEquals(packageId, imported.semanticId)
        assertTrue(imported.modules.single().declarations.size > 1)
        val function = assertInstanceOf(
            DefaultChirFunctionDeclaration::class.java,
            imported.modules.single().declarations.single { it.semanticId == ChirSemanticId("@_CN18cjmp_raw_chir_v1135ValueHl") },
        )
        assertEquals("Value", function.name)
        assertEquals(1, function.parameters.size)
        assertEquals(ChirResolvedTypeRef(ChirPrimitiveType.INT64), function.returnType)
        assertTrue(function.blocks.any { it.terminator is ChirReturnTerminator })
        val importedFunctions = imported.modules.single().declarations.filterIsInstance<DefaultChirFunctionDeclaration>()
        val importedExpressions = importedFunctions.flatMap { it.blocks }.flatMap { it.expressions }
        val expressionIds = importedExpressions.map { it.semanticId }.toSet()
        val resultValues = importedExpressions.mapNotNull { it.resultLocalValue }
        assertTrue(resultValues.all { it.semanticId.value.startsWith("official:v113:${packageId.value}:value:") })
        assertTrue(resultValues.all { it.semanticId !in expressionIds })
        assertTrue(resultValues.all { checkNotNull(it.associatedExpressionId) in expressionIds })
        assertEquals(resultValues.size, resultValues.map { it.semanticId }.toSet().size)
        val calls = importedExpressions.filterIsInstance<ChirCallExpression>()
        assertTrue(calls.any { it.thisType != null })
        assertTrue(function.blocks.flatMap { it.expressions }
            .filterIsInstance<ChirMemoryExpression>()
            .map(ChirMemoryExpression::operation)
            .containsAll(setOf("alloca", "store", "load")))
        assertTrue(importedExpressions
            .filterIsInstance<ChirOtherExpression>()
            .any { it.operation == ChirOtherOperation.RAW_ARRAY_ALLOCATE.canonicalName })
        val tuple = importedExpressions
            .filterIsInstance<ChirTupleExpression>()
            .single { it.semanticId.value.endsWith(":expression:441") }
        assertTrue(tuple.resultLocalValue!!.semanticId.value.startsWith("official:v113:${packageId.value}:value:"))
        val tupleType = assertInstanceOf(ChirTupleType::class.java, (tuple.resultType as ChirResolvedTypeRef).type)
        assertEquals(3, tuple.elements.size)
        assertEquals(tupleType.elementTypes, tuple.elements.map { it.type })
        assertTrue(tuple.elements.all { it.semanticId.value.startsWith("official:v113:${packageId.value}:value:") })
        val field = importedExpressions
            .filterIsInstance<ChirFieldExpression>()
            .single { it.semanticId.value.endsWith(":expression:442") }
        assertTrue(field.path.isNotEmpty())
    }

    @Test
    fun `rejects an official function identity that is not present in the package`() {
        val semanticPackage = OfficialChirV113SemanticPackageDecoder.decode(
            Files.readAllBytes(rawCommonFixture().toPath()),
        )

        assertThrows(OfficialChirV113ImportException::class.java) {
            DefaultOfficialChirV113ChirPackageImporter().importFunctions(
                semanticPackage = semanticPackage,
                packageSemanticId = ChirSemanticId("test:official-cjmp-raw-common"),
                functionIdentities = setOf(ChirSemanticId("@missing-official-identity")),
            )
        }
    }

    @Test
    fun `imports official field by name as a validated aggregate field path`() {
        val semanticPackage = OfficialChirV113SemanticPackageDecoder.decode(
            Files.readAllBytes(rawCommonFixture().toPath()),
        )

        val imported = DefaultOfficialChirV113ChirPackageImporter().importFunctions(
            semanticPackage = semanticPackage,
            packageSemanticId = ChirSemanticId("test:official-cjmp-raw-common"),
            functionIdentities = setOf(ChirSemanticId("@_CNat5ArrayIG_E7isLegalHll")),
        )

        val expressions = imported.modules.flatMap { it.declarations }
            .filterIsInstance<DefaultChirFunctionDeclaration>()
            .flatMap { it.blocks }
            .flatMap { it.expressions }
        val fields = expressions.filterIsInstance<ChirFieldExpression>()
        assertTrue(fields.isNotEmpty())
        assertTrue(fields.all { it.path.isNotEmpty() })
        assertTrue(fields.all { it.resultLocalValue?.associatedExpressionId == it.semanticId })
    }

    @Test
    fun `imports every official function in the common package`() {
        val semanticPackage = OfficialChirV113SemanticPackageDecoder.decode(
            Files.readAllBytes(rawCommonFixture().toPath()),
        )
        val identities = semanticPackage.values.asSequence()
            .filter { it.table.schemaType == "Func" }
            .map { function ->
                val valueBase = function.table.fields["base"] as OfficialChirV113SemanticValue.Table
                val identifier = valueBase.value.fields["identifier"] as OfficialChirV113SemanticValue.Scalar
                ChirSemanticId(identifier.value as String)
            }
            .toSet()
        val imported = DefaultOfficialChirV113ChirPackageImporter().importFunctions(
            semanticPackage = semanticPackage,
            packageSemanticId = ChirSemanticId("test:official-cjmp-raw-common"),
            functionIdentities = identities,
        )
        val expressions = imported.modules.flatMap { it.declarations }
            .filterIsInstance<DefaultChirFunctionDeclaration>()
            .flatMap { it.blocks }
            .flatMap { it.expressions }
        val arrayAccesses = expressions.filterIsInstance<ChirOtherExpression>()
            .filter { it.intrinsicKind == ChirIntrinsicKind.ARRAY_GET_UNCHECKED }
        assertTrue(arrayAccesses.isNotEmpty())
        assertTrue(arrayAccesses.all { it.operands.size == 2 && it.resultType != null })
        val rawArrayInitializations = expressions.filterIsInstance<ChirOtherExpression>()
            .filter { it.operation == ChirOtherOperation.RAW_ARRAY_INIT_BY_VALUE.canonicalName }
        assertTrue(rawArrayInitializations.isNotEmpty())
        assertTrue(rawArrayInitializations.all { it.operands.size == 3 && it.resultLocalValue != null })
        assertTrue(expressions.filterIsInstance<ChirBinaryExpression>()
            .any { it.overflowStrategy == ChirOverflowStrategy.THROWING })

    }

    @Test
    fun `imports official package functions variables and type declarations`() {
        val semanticPackage = OfficialChirV113SemanticPackageDecoder.decode(
            Files.readAllBytes(rawCommonFixture().toPath()),
        )
        val imported = DefaultOfficialChirV113ChirPackageImporter().importPackage(
            semanticPackage = semanticPackage,
            packageSemanticId = ChirSemanticId("test:official-cjmp-raw-common-full"),
        )

        val expectedFunctionIds = semanticPackage.values.asSequence()
            .filter { it.table.schemaType in setOf("Func", "ImportedFunc") }
            .map(::officialValueIdentifier)
            .toSet()
        val expectedVariableIds = semanticPackage.values.asSequence()
            .filter { it.table.schemaType in setOf("GlobalVar", "ImportedVar") }
            .map(::officialValueIdentifier)
            .toSet()
        val expectedDefinitionIds = semanticPackage.definitions
            .map { "official:v113:${imported.semanticId.value}:definition:${it.id}" }
            .toSet()
        val expectedBlockIds = semanticPackage.values.asSequence()
            .filter { it.table.schemaType == "BlockGroup" }
            .flatMap { blockGroup ->
                val blockSequence = blockGroup.table.fields["blocks"] as OfficialChirV113SemanticValue.Sequence
                blockSequence.values.asSequence().map { value ->
                    val reference = value as OfficialChirV113SemanticValue.Reference
                    "official:v113:${imported.semanticId.value}:value:${reference.id}"
                }
            }
            .toSet()
        val expectedExpressionIds = semanticPackage.expressions
            .map { "official:v113:${imported.semanticId.value}:expression:${it.id}" }
            .toSet()
        val declarations = imported.allDeclarations
        val functions = declarations.filterIsInstance<DefaultChirFunctionDeclaration>()
        val importedOfficialFunctionIds = functions.map { it.semanticId.value }.filter(expectedFunctionIds::contains)
        assertEquals(expectedFunctionIds, importedOfficialFunctionIds.toSet())
        assertEquals(expectedFunctionIds.size, importedOfficialFunctionIds.size, "official functions must map exactly once")
        assertTrue(functions.filter { it.semanticId.value !in expectedFunctionIds }
            .all { it.isImported && !it.hasBody })
        val importedBlocks = functions.flatMap { it.blocks }
        val importedBlockIds = importedBlocks.map { it.semanticId.value }
        assertEquals(expectedBlockIds, importedBlockIds.toSet(), "every official block must map exactly once")
        assertEquals(expectedBlockIds.size, importedBlockIds.size, "official blocks must not be duplicated")
        val importedExpressionIds = importedBlocks.flatMap { block ->
            block.expressions.map { it.semanticId.value } + listOfNotNull(block.terminator?.semanticId?.value)
        }
            .filter(expectedExpressionIds::contains)
        val importedExpressionIdSet = importedExpressionIds.toSet()
        val missingOfficialExpressions = semanticPackage.expressions.filter { expression ->
            "official:v113:${imported.semanticId.value}:expression:${expression.id}" !in importedExpressionIdSet
        }
        val missingExpressionTypes = missingOfficialExpressions.groupingBy { it.table.schemaType }.eachCount()
        assertTrue(
            missingOfficialExpressions.isEmpty(),
            "official expressions missing by table type: $missingExpressionTypes; ids=${missingOfficialExpressions.take(24).map { it.id }}",
        )
        assertEquals(expectedExpressionIds.size, importedExpressionIds.size, "official expressions must not be duplicated")
        assertTrue(imported.modules.flatMap { it.declarations }
            .filterIsInstance<DefaultChirFunctionDeclaration>()
            .all { it.hasBody && !it.isImported })
        assertTrue(imported.members.importedFunctions
            .all { it.isImported && !it.hasBody && it.blocks.isEmpty() && it.entryBlockId == null })
        val importedOfficialVariableIds = declarations.filterIsInstance<ChirVariableDeclaration>()
            .map { it.semanticId.value }
            .filter(expectedVariableIds::contains)
        assertEquals(expectedVariableIds, importedOfficialVariableIds.toSet())
        assertEquals(expectedVariableIds.size, importedOfficialVariableIds.size, "official variables must map exactly once")
        assertEquals(
            expectedDefinitionIds,
            (imported.typeDefinitions + imported.importedTypeDefinitions).map { it.semanticId.value }.toSet(),
        )
        assertTrue((imported.typeDefinitions + imported.importedTypeDefinitions).any { it.typeParameters.isNotEmpty() })
        assertTrue((imported.typeDefinitions + imported.importedTypeDefinitions).filter { it.typeParameters.isNotEmpty() }
            .all { it.typeParameterTypes.size == it.typeParameters.size })
        assertTrue(DefaultChirValidator().validatePackage(imported).issues.isEmpty())
    }

    @Test
    fun `actualizes an imported declaration produced by the official package importer`() {
        val packageId = ChirSemanticId("test:official-cjmp-imported-member-actualization")
        val common = DefaultOfficialChirV113ChirPackageImporter().importPackage(
            semanticPackage = OfficialChirV113SemanticPackageDecoder.decode(
                Files.readAllBytes(rawCommonFixture().toPath()),
            ),
            packageSemanticId = packageId,
        )
        val commonImportedFunction = assertInstanceOf(
            DefaultChirFunctionDeclaration::class.java,
            common.members.importedFunctions.first(),
        )
        val blockId = ChirSemanticId("${commonImportedFunction.semanticId.value}:specific-entry")
        val specificImplementation = commonImportedFunction.copy(
            blocks = listOf(
                ChirBlock(
                    semanticId = blockId,
                    name = "specific-entry",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("${blockId.value}:loop"),
                        targetBlockId = blockId,
                    ),
                ),
            ),
            entryBlockId = blockId,
            hasBody = true,
            isImported = false,
        )
        val specific = ChirPackage(
            semanticId = packageId,
            name = common.name,
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("test:official-cjmp-specific-module"),
                    name = "specific",
                    declarations = listOf(specificImplementation),
                ),
            ),
            accessLevel = common.accessLevel,
        )

        val actualized = DefaultCjmpChirPackageActualizer().actualize(
            commonPackage = common,
            specificPackage = specific,
            actualizedIdentities = setOf(commonImportedFunction.semanticId),
        )

        val function = actualized.allDeclarations.filterIsInstance<DefaultChirFunctionDeclaration>()
            .single { it.semanticId == commonImportedFunction.semanticId }
        assertTrue(function.hasBody)
        assertTrue(!function.isImported)
        assertTrue(actualized.members.importedFunctions.none { it.semanticId == function.semanticId })
        assertTrue(actualized.modules.flatMap { it.declarations }.any { it.semanticId == function.semanticId })
    }

    private fun rawCommonFixture(): File = repositoryRoot()
        .toPath()
        .resolve("chir/chir-tree/testData/chirParity/official-generated-chir/cjmpRawCommon/cjmp_raw_chir_v113.chir")
        .toFile()
        .also { check(it.isFile) { "missing official RAW CHIR fixture: $it" } }

    private fun officialValueIdentifier(indexed: org.cangnova.cangjie.chir.core.serializer.OfficialChirV113IndexedTable): String {
        var table = indexed.table
        while (true) {
            val identifier = table.fields["identifier"] as? OfficialChirV113SemanticValue.Scalar
            if (identifier != null) return identifier.value as String
            table = (table.fields["base"] as? OfficialChirV113SemanticValue.Table)?.value
                ?: error("official ${table.schemaType} VALUE#${indexed.id} has no Value identifier")
        }
    }

    private fun repositoryRoot(): File {
        var directory = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (File(directory, "settings.gradle.kts").isFile) return directory
            directory = directory.parentFile ?: error("repository root not found")
        }
    }
}
