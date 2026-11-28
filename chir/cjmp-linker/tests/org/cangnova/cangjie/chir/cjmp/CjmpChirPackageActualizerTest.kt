package org.cangnova.cangjie.chir.cjmp

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirExtendDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.ChirPackageMembers
import org.cangnova.cangjie.chir.core.model.allDeclarations
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirEnumCaseType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CjmpChirPackageActualizerTest {
    private val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
    private val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)

    @Test
    fun `legacy actualizer implementations retain the shared identity plan entry point`() {
        val sharedIdentity = id("function:shared")
        val common = packageWith()
        val specific = packageWith()
        var receivedIdentities: Set<ChirSemanticId>? = null
        val legacyActualizer = object : CjmpChirPackageActualizer {
            override fun actualize(
                commonPackage: ChirPackage,
                specificPackage: ChirPackage,
                actualizedIdentities: Set<ChirSemanticId>,
            ): ChirPackage {
                receivedIdentities = actualizedIdentities
                return specificPackage
            }
        }

        assertEquals(
            specific,
            legacyActualizer.actualize(common, specific, CjmpChirActualizationPlan(setOf(sharedIdentity))),
        )
        assertEquals(setOf(sharedIdentity), receivedIdentities)
        assertThrows(UnsupportedOperationException::class.java) {
            legacyActualizer.actualize(
                common,
                specific,
                CjmpChirActualizationPlan(
                    declarationPairs = setOf(
                        CjmpChirDeclarationPair(id("common:shared"), id("specific:shared")),
                    ),
                ),
            )
        }
    }

    @Test
    fun `actualizes only matcher approved identities and retains declarations from both packages`() {
        val matchedId = id("function:value")
        val caller = function(
            semanticId = id("function:caller"),
            name = "CallValue",
            returnType = intType,
            returnLiteral = "0",
            expressions = listOf(
                ChirCallExpression(
                    semanticId = id("expression:call-value"),
                    callee = ChirFunctionValue(
                        semanticId = matchedId,
                        type = ChirResolvedTypeRef(
                            ChirFunctionType(parameterTypes = emptyList(), returnType = intType),
                        ),
                        name = "Value",
                    ),
                    arguments = emptyList(),
                    resultType = intType,
                ),
            ),
        )
        val commonOnly = function(id("function:common-only"), "CommonOnly", intType, "11")
        val specificOnly = function(id("function:specific-only"), "SpecificOnly", intType, "22")
        val common = packageWith(
            module(
                "module:shared",
                listOf(
                    function(matchedId, "Value", intType, "1"),
                    caller,
                    commonOnly,
                ),
            ),
        )
        val specific = packageWith(
            module(
                "module:shared",
                listOf(function(matchedId, "Value", intType, "42"), specificOnly),
            ),
        )

        val actualized = DefaultCjmpChirPackageActualizer().actualize(
            common,
            specific,
            setOf(matchedId),
        )

        val actualizedValue = actualized.allDeclarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.semanticId == matchedId }
        val returnValue = (actualizedValue.blocks.single().terminator as ChirReturnTerminator)
            .returnValue as ChirConstantValue
        assertEquals("42", returnValue.literal)
        assertTrue(actualized.allDeclarations.any { it.semanticId == commonOnly.semanticId })
        assertTrue(actualized.allDeclarations.any { it.semanticId == specificOnly.semanticId })
        val retainedCall = actualized.allDeclarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.semanticId == caller.semanticId }
            .blocks.single().expressions.single() as ChirCallExpression
        assertEquals(matchedId, retainedCall.callee.semanticId)
    }

    @Test
    fun `actualizes a common declaration-only function to the specific implementation`() {
        val functionId = id("function:declared-common")
        val commonDeclaration = function(functionId, "Value", intType, null).copy(
            blocks = emptyList(),
            entryBlockId = null,
            hasBody = false,
            isImported = true,
        )
        val specificImplementation = function(functionId, "Value", intType, "42")

        val actualized = DefaultCjmpChirPackageActualizer().actualize(
            packageWith(module("module:shared", listOf(commonDeclaration))),
            packageWith(module("module:shared", listOf(specificImplementation))),
            setOf(functionId),
        )
        val function = actualized.allDeclarations.filterIsInstance<ChirFunctionDeclaration>().single()

        assertTrue(function.hasBody)
        assertFalse(function.isImported)
        assertEquals(specificImplementation.blocks, function.blocks)
        assertEquals(specificImplementation.entryBlockId, function.entryBlockId)
    }

    @Test
    fun `moves matched declarations between different common and specific modules`() {
        val functionId = id("function:moved-between-modules")
        val commonDeclaration = function(functionId, "Value", intType, "1")
        val specificImplementation = function(functionId, "Value", intType, "42")

        val actualized = DefaultCjmpChirPackageActualizer().actualize(
            commonPackage = packageWith(module("module:common-source", listOf(commonDeclaration))),
            specificPackage = packageWith(module("module:specific-source", listOf(specificImplementation))),
            actualizedIdentities = setOf(functionId),
        )

        val function = actualized.allDeclarations.filterIsInstance<ChirFunctionDeclaration>().single()
        val returnValue = (function.blocks.single().terminator as ChirReturnTerminator)
            .returnValue as ChirConstantValue
        assertEquals("42", returnValue.literal)
        assertEquals(
            "specific_source",
            actualized.modules.single { module -> module.declarations.any { it.semanticId == functionId } }.name,
        )
    }

    @Test
    fun `actualizes official package type definition with CFIR module type by nominal identity`() {
        val typeDeclarationId = ChirTypeDeclarationId("sample", "Container")
        val officialTypeId = id("official:v113:definition:7")
        val cfirTypeId = id("@sample.Container")
        val methodId = id("@sample.Container.value")
        val commonType = DefaultChirClassDeclaration(
            semanticId = officialTypeId,
            name = "Container",
            typeDeclarationId = typeDeclarationId,
            memberDeclarations = listOf(function(methodId, "value", intType, "1")),
        )
        val specificType = DefaultChirClassDeclaration(
            semanticId = cfirTypeId,
            name = "Container",
            typeDeclarationId = typeDeclarationId,
            memberDeclarations = listOf(function(methodId, "value", intType, "42")),
        )

        val actualized = DefaultCjmpChirPackageActualizer().actualize(
            commonPackage = packageWith().copy(typeDefinitions = listOf(commonType)),
            specificPackage = packageWith(module("module:specific", listOf(specificType))),
            actualizedIdentities = setOf(officialTypeId, methodId),
        )

        val actualizedType = actualized.typeDefinitions.filterIsInstance<DefaultChirClassDeclaration>().single()
        val actualizedMethod = actualizedType.memberDeclarations.filterIsInstance<ChirFunctionDeclaration>().single()
        val returnedValue = (actualizedMethod.blocks.single().terminator as ChirReturnTerminator)
            .returnValue as ChirConstantValue

        assertEquals(cfirTypeId, actualizedType.semanticId)
        assertEquals("42", returnedValue.literal)
        assertTrue(actualized.modules.single().declarations.none { it.semanticId == cfirTypeId })
    }

    @Test
    fun `rejects same nominal type across package containers without matcher identity`() {
        val typeDeclarationId = ChirTypeDeclarationId("sample", "Container")
        val commonType = DefaultChirClassDeclaration(
            semanticId = id("official:v113:definition:7"),
            name = "Container",
            typeDeclarationId = typeDeclarationId,
        )
        val specificType = DefaultChirClassDeclaration(
            semanticId = id("cfir:specific:container"),
            name = "Container",
            typeDeclarationId = typeDeclarationId,
        )

        assertThrows(CjmpChirPackageActualizationException::class.java) {
            DefaultCjmpChirPackageActualizer().actualize(
                commonPackage = packageWith().copy(typeDefinitions = listOf(commonType)),
                specificPackage = packageWith(module("module:specific", listOf(specificType))),
                actualizedIdentities = emptySet(),
            )
        }
    }

    @Test
    fun `merges moved matched declarations into an existing common destination module`() {
        val classId = id("type:moved-class")
        val commonMemberId = id("function:common-class-member")
        val specificMemberId = id("function:specific-class-member")
        val commonClass = DefaultChirClassDeclaration(
            semanticId = classId,
            name = "Container",
            typeDeclarationId = ChirTypeDeclarationId("sample", "Container"),
            memberDeclarations = listOf(function(commonMemberId, "commonOnly", intType, "1")),
        )
        val specificClass = commonClass.copy(
            memberDeclarations = listOf(function(specificMemberId, "specificOnly", intType, "2")),
        )
        val commonDestination = function(id("function:destination-existing"), "Existing", intType, "0")

        val actualized = DefaultCjmpChirPackageActualizer().actualize(
            commonPackage = packageWith(
                module("module:common-origin", listOf(commonClass)),
                module("module:shared-destination", listOf(commonDestination)),
            ),
            specificPackage = packageWith(
                module("module:shared-destination", listOf(specificClass)),
            ),
            actualizedIdentities = setOf(classId),
        )

        val actualizedClass = actualized.allDeclarations
            .filterIsInstance<DefaultChirClassDeclaration>()
            .single { it.semanticId == classId }
        assertEquals(
            setOf(commonMemberId, specificMemberId),
            actualizedClass.memberDeclarations.mapTo(linkedSetOf()) { it.semanticId },
        )
        assertEquals(1, actualized.allDeclarations.count { it.semanticId == classId })
    }

    @Test
    fun `moves an actualized common imported function into the specific module implementation`() {
        val functionId = id("function:imported-common")
        val commonDeclaration = function(functionId, "Value", intType, null).copy(
            blocks = emptyList(),
            entryBlockId = null,
            hasBody = false,
            isImported = true,
        )
        val specificImplementation = function(functionId, "Value", intType, "42")
        val common = packageWith().copy(
            members = ChirPackageMembers(importedFunctions = listOf(commonDeclaration)),
        )
        val specific = packageWith(
            module("module:specific", listOf(specificImplementation)),
        )

        val actualized = DefaultCjmpChirPackageActualizer().actualize(
            common,
            specific,
            setOf(functionId),
        )

        val actualizedFunctions = actualized.allDeclarations.filterIsInstance<ChirFunctionDeclaration>()
        val actualizedFunction = actualizedFunctions.single { it.semanticId == functionId }
        assertTrue(actualizedFunction.hasBody)
        assertFalse(actualizedFunction.isImported)
        assertEquals(specificImplementation.blocks, actualizedFunction.blocks)
        assertTrue(actualized.members.importedFunctions.none { it.semanticId == functionId })
        assertTrue(actualized.modules.flatMap { it.declarations }
            .filterIsInstance<ChirFunctionDeclaration>()
            .any { it.semanticId == functionId && it.hasBody && !it.isImported })
    }

    @Test
    fun `actualizes a common imported global into the specific package global`() {
        val variableId = id("variable:global")
        val commonVariable = DefaultChirVariableDeclaration(
            semanticId = variableId,
            name = "GLOBAL_VALUE",
            type = intType,
            mutable = true,
        )
        val specificVariable = commonVariable.copy()
        val common = packageWith().copy(
            members = ChirPackageMembers(importedVariables = listOf(commonVariable)),
        )
        val specific = packageWith().copy(
            members = ChirPackageMembers(globalVariables = listOf(specificVariable)),
        )

        val actualized = DefaultCjmpChirPackageActualizer().actualize(
            common,
            specific,
            setOf(variableId),
        )

        assertEquals(listOf(specificVariable), actualized.members.globalVariables)
        assertTrue(actualized.members.importedVariables.isEmpty())
        assertEquals(1, actualized.allDeclarations.count { it.semanticId == variableId })
    }

    @Test
    fun `rejects matched nominal declarations with different stable type identities`() {
        val declarationId = id("type:nominal")
        val commonDeclaration = DefaultChirClassDeclaration(
            semanticId = declarationId,
            name = "Payload",
            typeDeclarationId = ChirTypeDeclarationId("common", "Payload"),
        )
        val specificDeclaration = commonDeclaration.copy(
            typeDeclarationId = ChirTypeDeclarationId("specific", "Payload"),
        )

        assertThrows(CjmpChirPackageActualizationException::class.java) {
            DefaultCjmpChirPackageActualizer().actualize(
                packageWith(module("module:shared", listOf(commonDeclaration))),
                packageWith(module("module:shared", listOf(specificDeclaration))),
                setOf(declarationId),
            )
        }
    }

    @Test
    fun `rejects matched nominal declarations with different generic parameter identities`() {
        val declarationId = id("type:generic")
        val commonDeclaration = DefaultChirClassDeclaration(
            semanticId = declarationId,
            name = "Container",
            typeDeclarationId = ChirTypeDeclarationId("sample", "Container"),
            typeParameters = listOf("T"),
            typeParameterTypes = listOf(
                ChirResolvedTypeRef(ChirGenericType("T", declarationIndex = 0, semanticIdentity = "common.T")),
            ),
        )
        val specificDeclaration = commonDeclaration.copy(
            typeParameterTypes = listOf(
                ChirResolvedTypeRef(ChirGenericType("T", declarationIndex = 0, semanticIdentity = "specific.T")),
            ),
        )

        assertThrows(CjmpChirPackageActualizationException::class.java) {
            DefaultCjmpChirPackageActualizer().actualize(
                packageWith(module("module:shared", listOf(commonDeclaration))),
                packageWith(module("module:shared", listOf(specificDeclaration))),
                setOf(declarationId),
            )
        }
    }

    @Test
    fun `actualizes matcher approved owner and function generic parameter renames`() {
        val classId = id("type:box")
        val methodId = id("function:box-map")
        val commonOwnerParameter = ChirResolvedTypeRef(
            ChirGenericType("T", declarationIndex = 0, semanticIdentity = "common.Box.T"),
        )
        val specificOwnerParameter = ChirResolvedTypeRef(
            ChirGenericType("U", declarationIndex = 0, semanticIdentity = "specific.Box.U"),
        )
        val commonMethodParameter = ChirResolvedTypeRef(
            ChirGenericType("R", declarationIndex = 0, semanticIdentity = "common.Box.map.R"),
        )
        val specificMethodParameter = ChirResolvedTypeRef(
            ChirGenericType("S", declarationIndex = 0, semanticIdentity = "specific.Box.map.S"),
        )
        fun mapMethod(ownerParameter: ChirResolvedTypeRef, methodParameter: ChirResolvedTypeRef) =
            DefaultChirFunctionDeclaration(
                semanticId = methodId,
                name = "map",
                returnType = methodParameter,
                parameters = listOf(
                    DefaultChirVariableDeclaration(
                        semanticId = id("parameter:box-map-value"),
                        name = "value",
                        type = ownerParameter,
                        mutable = false,
                    ),
                ),
                blocks = emptyList(),
                entryBlockId = null,
                hasBody = false,
                isImported = true,
                genericTypeParameters = listOf(methodParameter),
            )
        val commonBox = DefaultChirClassDeclaration(
            semanticId = classId,
            name = "Box",
            typeDeclarationId = ChirTypeDeclarationId("sample", "Box"),
            typeParameters = listOf("T"),
            typeParameterTypes = listOf(commonOwnerParameter),
            memberDeclarations = listOf(mapMethod(commonOwnerParameter, commonMethodParameter)),
        )
        val specificBox = commonBox.copy(
            typeParameters = listOf("U"),
            typeParameterTypes = listOf(specificOwnerParameter),
            memberDeclarations = listOf(mapMethod(specificOwnerParameter, specificMethodParameter)),
        )
        val plan = CjmpChirActualizationPlan(
            declarationPairs = setOf(
                CjmpChirDeclarationPair(classId, classId),
                CjmpChirDeclarationPair(methodId, methodId),
            ),
            typeParameterPairs = setOf(
                CjmpChirTypeParameterPair(classId, 0, classId, 0),
                CjmpChirTypeParameterPair(methodId, 0, methodId, 0),
            ),
        )

        val actualized = DefaultCjmpChirPackageActualizer().actualize(
            packageWith(module("module:shared", listOf(commonBox))),
            packageWith(module("module:shared", listOf(specificBox))),
            plan,
        )
        val actualizedBox = actualized.allDeclarations.filterIsInstance<DefaultChirClassDeclaration>().single()
        val actualizedMap = actualizedBox.memberDeclarations.filterIsInstance<DefaultChirFunctionDeclaration>().single()

        assertEquals(listOf("U"), actualizedBox.typeParameters)
        assertEquals("specific.Box.U", (actualizedMap.parameters.single().type as ChirResolvedTypeRef).type.let { (it as ChirGenericType).semanticIdentity })
        assertEquals("specific.Box.map.S", (actualizedMap.returnType as ChirResolvedTypeRef).type.let { (it as ChirGenericType).semanticIdentity })
        assertEquals("specific.Box.map.S", (actualizedMap.genericTypeParameters.single() as ChirResolvedTypeRef).type.let { (it as ChirGenericType).semanticIdentity })
    }

    @Test
    fun `substitutes owner generics throughout retained common member bodies`() {
        val classId = id("type:box")
        val echoId = id("function:echo")
        val applyId = id("function:box-apply")
        val commonOwnerParameter = ChirResolvedTypeRef(
            ChirGenericType("T", declarationIndex = 0, semanticIdentity = "common.Box.T"),
        )
        val specificOwnerParameter = ChirResolvedTypeRef(
            ChirGenericType("U", declarationIndex = 0, semanticIdentity = "specific.Box.U"),
        )
        val commonEchoParameter = ChirResolvedTypeRef(
            ChirGenericType("R", declarationIndex = 0, semanticIdentity = "common.echo.R"),
        )
        val specificEchoParameter = ChirResolvedTypeRef(
            ChirGenericType("S", declarationIndex = 0, semanticIdentity = "specific.echo.S"),
        )
        val commonExtendParameter = ChirResolvedTypeRef(
            ChirGenericType("E", declarationIndex = 0, semanticIdentity = "common.BoxExtension.E"),
        )
        val specificExtendParameter = ChirResolvedTypeRef(
            ChirGenericType("F", declarationIndex = 0, semanticIdentity = "specific.BoxExtension.F"),
        )
        val commonEnumParameter = ChirResolvedTypeRef(
            ChirGenericType("A", declarationIndex = 0, semanticIdentity = "common.Option.A"),
        )
        val specificEnumParameter = ChirResolvedTypeRef(
            ChirGenericType("B", declarationIndex = 0, semanticIdentity = "specific.Option.B"),
        )
        val retainedMethodParameter = ChirResolvedTypeRef(
            ChirGenericType(
                "M",
                upperBounds = listOf(commonOwnerParameter),
                declarationIndex = 0,
                semanticIdentity = "common.Box.apply.M",
            ),
        )
        val argumentId = id("parameter:box-apply-value")
        val callId = id("expression:box-apply-echo")
        val resultId = id("value:box-apply-result")

        fun echo(parameter: ChirResolvedTypeRef) = DefaultChirFunctionDeclaration(
            semanticId = echoId,
            name = "echo",
            returnType = parameter,
            parameters = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = id("parameter:echo-value"),
                    name = "value",
                    type = parameter,
                    mutable = false,
                ),
            ),
            blocks = emptyList(),
            entryBlockId = null,
            hasBody = false,
            isImported = true,
            genericTypeParameters = listOf(parameter),
        )

        val commonCall = ChirCallExpression(
            semanticId = callId,
            callee = ChirFunctionValue(
                semanticId = echoId,
                type = ChirResolvedTypeRef(
                    ChirFunctionType(
                        parameterTypes = listOf(commonOwnerParameter),
                        returnType = commonOwnerParameter,
                    ),
                ),
                name = "echo",
            ),
            arguments = listOf(
                ChirParameterValue(
                    semanticId = argumentId,
                    type = commonOwnerParameter,
                    name = "value",
                    ownerFunctionId = applyId,
                ),
            ),
            resultType = commonOwnerParameter,
            instantiatedTypeArguments = listOf(commonOwnerParameter),
            resultLocalValue = ChirLocalValue(
                semanticId = resultId,
                type = commonOwnerParameter,
                name = "result",
                associatedExpressionId = callId,
            ),
        )
        val commonApply = DefaultChirFunctionDeclaration(
            semanticId = applyId,
            name = "apply",
            returnType = commonOwnerParameter,
            parameters = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = argumentId,
                    name = "value",
                    type = commonOwnerParameter,
                    mutable = false,
                ),
            ),
            blocks = listOf(
                ChirBlock(
                    semanticId = id("block:box-apply-entry"),
                    name = "entry",
                    expressions = listOf(commonCall),
                    terminator = ChirReturnTerminator(
                        semanticId = id("terminator:box-apply-return"),
                        returnValue = commonCall.resultLocalValue,
                    ),
                ),
            ),
            entryBlockId = id("block:box-apply-entry"),
            genericTypeParameters = listOf(retainedMethodParameter),
        )
        val commonBox = DefaultChirClassDeclaration(
            semanticId = classId,
            name = "Box",
            typeDeclarationId = ChirTypeDeclarationId("sample", "Box"),
            typeParameters = listOf("T"),
            typeParameterTypes = listOf(commonOwnerParameter),
            implementedTypes = listOf(
                ChirResolvedTypeRef(ChirNamedType("Contract", listOf(commonOwnerParameter))),
                ChirResolvedTypeRef(ChirNamedType("CommonContract", listOf(commonOwnerParameter))),
            ),
            memberDeclarations = listOf(commonApply),
        )
        val specificBox = commonBox.copy(
            typeParameters = listOf("U"),
            typeParameterTypes = listOf(specificOwnerParameter),
            implementedTypes = listOf(
                ChirResolvedTypeRef(ChirNamedType("Contract", listOf(specificOwnerParameter))),
            ),
            memberDeclarations = emptyList(),
        )
        val commonExtend = DefaultChirExtendDeclaration(
            semanticId = id("type:box-extension"),
            name = "BoxExtension",
            targetType = ChirResolvedTypeRef(ChirNamedType("Target", listOf(commonExtendParameter))),
            extendedTypes = listOf(
                ChirResolvedTypeRef(ChirNamedType("Extension", listOf(commonExtendParameter))),
            ),
            typeParameters = listOf("E"),
            typeParameterTypes = listOf(commonExtendParameter),
        )
        val specificExtend = commonExtend.copy(
            targetType = ChirResolvedTypeRef(ChirNamedType("Target", listOf(specificExtendParameter))),
            extendedTypes = emptyList(),
            typeParameters = listOf("F"),
            typeParameterTypes = listOf(specificExtendParameter),
        )
        val commonEnum = DefaultChirEnumDeclaration(
            semanticId = id("type:option"),
            name = "Option",
            typeDeclarationId = ChirTypeDeclarationId("sample", "Option"),
            typeParameters = listOf("A"),
            typeParameterTypes = listOf(commonEnumParameter),
            cases = listOf("Existing", "CommonOnly"),
            caseTypes = listOf(
                ChirEnumCaseType("Existing", listOf(commonEnumParameter)),
                ChirEnumCaseType("CommonOnly", listOf(commonEnumParameter)),
            ),
        )
        val specificEnum = commonEnum.copy(
            typeParameters = listOf("B"),
            typeParameterTypes = listOf(specificEnumParameter),
            cases = listOf("Existing"),
            caseTypes = listOf(ChirEnumCaseType("Existing", listOf(specificEnumParameter))),
        )
        val plan = CjmpChirActualizationPlan(
            declarationPairs = setOf(
                CjmpChirDeclarationPair(classId, classId),
                CjmpChirDeclarationPair(echoId, echoId),
                CjmpChirDeclarationPair(commonExtend.semanticId, specificExtend.semanticId),
                CjmpChirDeclarationPair(commonEnum.semanticId, specificEnum.semanticId),
            ),
            typeParameterPairs = setOf(
                CjmpChirTypeParameterPair(classId, 0, classId, 0),
                CjmpChirTypeParameterPair(echoId, 0, echoId, 0),
                CjmpChirTypeParameterPair(commonExtend.semanticId, 0, specificExtend.semanticId, 0),
                CjmpChirTypeParameterPair(commonEnum.semanticId, 0, specificEnum.semanticId, 0),
            ),
        )

        val actualized = DefaultCjmpChirPackageActualizer().actualize(
            packageWith(module("module:shared", listOf(commonBox, commonEnum, commonExtend, echo(commonEchoParameter)))),
            packageWith(module("module:shared", listOf(specificBox, specificEnum, specificExtend, echo(specificEchoParameter)))),
            plan,
        )
        val declarations = actualized.allDeclarations
        val actualizedBox = declarations.filterIsInstance<DefaultChirClassDeclaration>().single()
        val actualizedApply = actualizedBox.memberDeclarations
            .filterIsInstance<DefaultChirFunctionDeclaration>()
            .single()
        val actualizedCall = actualizedApply.blocks.single().expressions.single() as ChirCallExpression
        val actualizedExtend = declarations.filterIsInstance<DefaultChirExtendDeclaration>().single()
        val actualizedEnum = declarations.filterIsInstance<DefaultChirEnumDeclaration>().single()

        assertEquals(specificOwnerParameter, actualizedApply.returnType)
        assertEquals(specificOwnerParameter, actualizedApply.parameters.single().type)
        assertEquals(listOf(specificOwnerParameter), (actualizedApply.genericTypeParameters.single() as ChirResolvedTypeRef).type.let { (it as ChirGenericType).upperBounds })
        assertEquals(specificOwnerParameter, actualizedCall.instantiatedTypeArguments.single())
        assertEquals(specificOwnerParameter, actualizedCall.arguments.single().type)
        assertEquals(specificOwnerParameter, actualizedCall.resultType)
        assertEquals(specificOwnerParameter, actualizedCall.resultLocalValue?.type)
        assertEquals(specificOwnerParameter, (actualizedApply.blocks.single().terminator as ChirReturnTerminator).returnValue?.type)
        assertEquals(
            listOf(
                ChirResolvedTypeRef(ChirNamedType("Contract", listOf(specificOwnerParameter))),
                ChirResolvedTypeRef(ChirNamedType("CommonContract", listOf(specificOwnerParameter))),
            ),
            actualizedBox.implementedTypes,
        )
        assertEquals(
            listOf(ChirResolvedTypeRef(ChirNamedType("Extension", listOf(specificExtendParameter)))),
            actualizedExtend.extendedTypes,
        )
        assertEquals(
            listOf(specificEnumParameter),
            actualizedEnum.caseTypes.single { it.name == "CommonOnly" }.payloadTypes,
        )
    }

    @Test
    fun `rejects matcher type parameter pairs outside the approved owner pair`() {
        val ownerId = id("type:owner")
        val unrelatedOwnerId = id("type:unrelated")
        val ownerType = ChirResolvedTypeRef(ChirGenericType("T", declarationIndex = 0, semanticIdentity = "owner.T"))
        val commonOwner = DefaultChirClassDeclaration(
            semanticId = ownerId,
            name = "Owner",
            typeDeclarationId = ChirTypeDeclarationId("sample", "Owner"),
            typeParameters = listOf("T"),
            typeParameterTypes = listOf(ownerType),
        )
        val specificOwner = commonOwner.copy(
            typeParameterTypes = listOf(
                ChirResolvedTypeRef(ChirGenericType("U", declarationIndex = 0, semanticIdentity = "specific.U")),
            ),
        )

        assertThrows(CjmpChirPackageActualizationException::class.java) {
            DefaultCjmpChirPackageActualizer().actualize(
                packageWith(module("module:shared", listOf(commonOwner))),
                packageWith(module("module:shared", listOf(specificOwner))),
                CjmpChirActualizationPlan(
                    declarationPairs = setOf(CjmpChirDeclarationPair(ownerId, ownerId)),
                    typeParameterPairs = setOf(CjmpChirTypeParameterPair(unrelatedOwnerId, 0, ownerId, 0)),
                ),
            )
        }
    }

    @Test
    fun `rejects declaration identity collision when matcher approved a different pair`() {
        val collisionId = id("function:collision")
        val unrelatedSpecificId = id("type:unrelated")
        val commonFunction = function(collisionId, "Value", intType, "1")
        val specificFunction = function(collisionId, "Value", intType, "42")
        val unrelatedSpecificType = DefaultChirClassDeclaration(
            semanticId = unrelatedSpecificId,
            name = "Other",
            typeDeclarationId = ChirTypeDeclarationId("sample", "Other"),
        )

        assertThrows(CjmpChirPackageActualizationException::class.java) {
            DefaultCjmpChirPackageActualizer().actualize(
                packageWith(module("module:shared", listOf(commonFunction))),
                packageWith(module("module:shared", listOf(specificFunction, unrelatedSpecificType))),
                CjmpChirActualizationPlan(
                    declarationPairs = setOf(CjmpChirDeclarationPair(collisionId, unrelatedSpecificId)),
                ),
            )
        }
    }

    @Test
    fun `rejects unapproved nominal cross container declaration with same type identity`() {
        val commonTypeId = id("type:official")
        val approvedSpecificTypeId = id("type:approved-specific")
        val unapprovedSpecificTypeId = id("type:unapproved-specific")
        val typeDeclarationId = ChirTypeDeclarationId("sample", "Container")
        val commonType = DefaultChirClassDeclaration(
            semanticId = commonTypeId,
            name = "Container",
            typeDeclarationId = typeDeclarationId,
        )
        val approvedSpecificType = commonType.copy(semanticId = approvedSpecificTypeId)
        val unapprovedSpecificType = commonType.copy(semanticId = unapprovedSpecificTypeId)

        val exception = assertThrows(CjmpChirPackageActualizationException::class.java) {
            DefaultCjmpChirPackageActualizer().actualize(
                commonPackage = packageWith().copy(typeDefinitions = listOf(commonType)),
                specificPackage = packageWith(
                    module("module:specific", listOf(approvedSpecificType, unapprovedSpecificType)),
                ),
                plan = CjmpChirActualizationPlan(
                    declarationPairs = setOf(
                        CjmpChirDeclarationPair(commonTypeId, approvedSpecificTypeId),
                    ),
                ),
            )
        }

        assertTrue(exception.message.orEmpty().contains("has no CFIR matcher relation"))
    }

    @Test
    fun `rejects enum payloads with equal display names but different type kinds`() {
        val declarationId = id("type:enum")
        val typeId = ChirTypeDeclarationId("sample", "Payload")
        val commonDeclaration = DefaultChirEnumDeclaration(
            semanticId = declarationId,
            name = "Result",
            typeDeclarationId = ChirTypeDeclarationId("sample", "Result"),
            cases = listOf("Value"),
            caseTypes = listOf(
                ChirEnumCaseType("Value", listOf(ChirResolvedTypeRef(ChirStructType(typeId)))),
            ),
        )
        val specificDeclaration = commonDeclaration.copy(
            caseTypes = listOf(
                ChirEnumCaseType("Value", listOf(ChirResolvedTypeRef(ChirClassType(typeId)))),
            ),
        )
        assertEquals(
            commonDeclaration.caseTypes.single().payloadTypes.single().renderName,
            specificDeclaration.caseTypes.single().payloadTypes.single().renderName,
        )

        assertThrows(CjmpChirPackageActualizationException::class.java) {
            DefaultCjmpChirPackageActualizer().actualize(
                packageWith(module("module:shared", listOf(commonDeclaration))),
                packageWith(module("module:shared", listOf(specificDeclaration))),
                setOf(declarationId),
            )
        }
    }

    @Test
    fun `rejects colliding identities without a matcher relation`() {
        val collision = id("function:collision")
        val common = packageWith(module("module:common", listOf(function(collision, "Value", intType, "1"))))
        val specific = packageWith(module("module:specific", listOf(function(collision, "Value", intType, "2"))))

        assertThrows(CjmpChirPackageActualizationException::class.java) {
            DefaultCjmpChirPackageActualizer().actualize(common, specific, emptySet())
        }
    }

    @Test
    fun `rejects matcher identities missing from either CHIR package`() {
        val common = packageWith()
        val specific = packageWith()

        assertThrows(CjmpChirPackageActualizationException::class.java) {
            DefaultCjmpChirPackageActualizer().actualize(common, specific, setOf(id("function:missing")))
        }
    }

    @Test
    fun `rejects matcher approved declarations with incompatible function signatures`() {
        val matchedId = id("function:value")
        val common = packageWith(module("module:common", listOf(function(matchedId, "Value", intType, "1"))))
        val specific = packageWith(
            module(
                "module:specific",
                listOf(function(matchedId, "Value", ChirResolvedTypeRef(ChirPrimitiveType.BOOL), "true")),
            ),
        )

        assertThrows(CjmpChirPackageActualizationException::class.java) {
            DefaultCjmpChirPackageActualizer().actualize(common, specific, setOf(matchedId))
        }
    }

    @Test
    fun `combines distinct common and specific package initializers`() {
        val commonInitId = id("function:common-init")
        val specificInitId = id("function:specific-init")
        val common = packageWith(
            module("module:common", listOf(function(commonInitId, "CommonInit", unitType, null))),
        ).copy(packageInitFunctionId = commonInitId)
        val specific = packageWith(
            module("module:specific", listOf(function(specificInitId, "SpecificInit", unitType, null))),
        ).copy(packageInitFunctionId = specificInitId)

        val actualized = DefaultCjmpChirPackageActualizer().actualize(common, specific, emptySet())
        val combinedId = requireNotNull(actualized.packageInitFunctionId)
        val combined = actualized.allDeclarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.semanticId == combinedId }
        val calls = combined.blocks.single().expressions.filterIsInstance<ChirCallExpression>()

        assertEquals(listOf(commonInitId, specificInitId), calls.map { it.callee.semanticId })
        assertFalse(combinedId == commonInitId || combinedId == specificInitId)
    }

    private fun packageWith(vararg modules: ChirModule): ChirPackage = ChirPackage(
        semanticId = id("package:sample"),
        name = "sample",
        modules = modules.toList(),
    )

    private fun module(id: String, declarations: List<ChirDeclaration>): ChirModule = ChirModule(
        semanticId = this.id(id),
        name = id.substringAfter(':').replace('-', '_'),
        declarations = declarations,
    )

    private fun function(
        semanticId: ChirSemanticId,
        name: String,
        returnType: ChirResolvedTypeRef,
        returnLiteral: String?,
        expressions: List<ChirCallExpression> = emptyList(),
    ): DefaultChirFunctionDeclaration {
        val blockId = id("${semanticId.value}:entry")
        val returnedValue = returnLiteral?.let {
            ChirConstantValue(id("${semanticId.value}:return-value"), returnType, it)
        }
        return DefaultChirFunctionDeclaration(
            semanticId = semanticId,
            name = name,
            returnType = returnType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = blockId,
                    name = "entry",
                    expressions = expressions,
                    terminator = ChirReturnTerminator(id("${semanticId.value}:return"), returnedValue),
                ),
            ),
            entryBlockId = blockId,
        )
    }

    private fun id(value: String): ChirSemanticId = ChirSemanticId(value)
}
