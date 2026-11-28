package org.cangnova.cangjie.wasm.codegen.lowering

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirBooleanAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirBinaryOperator
import org.cangnova.cangjie.chir.core.expression.ChirBinaryOperatorFamily
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryOperator
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherOperation
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.allDeclarations
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedVariableValue
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirValue
import org.cangnova.cangjie.wasm.codegen.api.WasmCodegenInput
import org.cangnova.cangjie.wasm.codegen.diagnostics.WasmCodegenException
import org.cangnova.cangjie.wasm.common.WasmTarget
import org.cangnova.cangjie.wasm.ir.WasmFunctionType
import org.cangnova.cangjie.wasm.ir.WasmGlobal
import org.cangnova.cangjie.wasm.ir.WasmGlobalImport
import org.cangnova.cangjie.wasm.ir.WasmImport
import org.cangnova.cangjie.wasm.ir.WasmValueType

/** package、函数与 binary lowering 共用的不可变符号和 import 计划。 */
internal class WasmCodegenPlan(val input: WasmCodegenInput) {
    val chirPackage: ChirPackage = input.chirPackage
    val mapper = WasmTypeMapper(input.chirPackage)
    val targetProfile = input.options.target.profile
    val dataPool = WasmDataPool()

    val importedFunctionDeclarations: List<ChirFunctionDeclaration> =
        chirPackage.members.importedFunctions.distinctBy(ChirFunctionDeclaration::semanticId)

    private val importedDeclarationIds = importedFunctionDeclarations.mapTo(hashSetOf()) { it.semanticId }

    val definedFunctions: List<ChirFunctionDeclaration> = chirPackage.allDeclarations
        .filterIsInstance<ChirFunctionDeclaration>()
        .distinctBy(ChirFunctionDeclaration::semanticId)
        .filterNot { it.semanticId in importedDeclarationIds }
        .sortedBy { it.semanticId.value }

    val functionNameById: Map<ChirSemanticId, String> = definedFunctions
        .mapIndexed { index, function -> function.semanticId to "__wasm_fn_$index" }
        .toMap()

    val functionTypeById: Map<ChirSemanticId, WasmFunctionType> = buildMap {
        definedFunctions.forEach { function ->
            put(
                function.semanticId,
                mapper.functionType(function.parameters.map(ChirVariableDeclaration::type), function.returnType, function.semanticId),
            )
        }
    }

    private val importedValueById: Map<ChirSemanticId, ChirImportedFunctionValue> =
        chirPackage.allDeclarations.filterIsInstance<ChirFunctionDeclaration>()
            .flatMap { function -> function.blocks.flatMap { block -> block.expressions.flatMap(ChirExpression::values) + block.terminator.values() } }
            .filterIsInstance<ChirImportedFunctionValue>()
            .associateBy(ChirImportedFunctionValue::semanticId)

    private val importRegistry = WasmFunctionImportRegistry()
    private val importKeyBySemanticId = linkedMapOf<ChirSemanticId, WasmFunctionImportKey>()
    private val runtimeKeyByExpressionId = linkedMapOf<ChirSemanticId, WasmFunctionImportKey>()
    private val importedGlobalKeyBySemanticId = linkedMapOf<ChirSemanticId, WasmGlobalImportKey>()

    val functionNameByImportedId: Map<ChirSemanticId, String>
    val functionTypeByImportedId: Map<ChirSemanticId, WasmFunctionType>
    val functionIndexById: Map<ChirSemanticId, UInt>
    val functionTableNames: List<String>
    val functionTableName: String = "__wasm_function_table"
    val imports: List<WasmImport>
    val globalImports: List<WasmGlobalImport>

    val globalNameById: Map<ChirSemanticId, String>
    val globals: List<WasmGlobal>
    private var throwImportKey: WasmFunctionImportKey? = null
    private var exceptionMatchesImportKey: WasmFunctionImportKey? = null
    private var allocationImportKey: WasmFunctionImportKey? = null
    private var wasiExitImportKey: WasmFunctionImportKey? = null
    private var stackMarkImportKey: WasmFunctionImportKey? = null
    private var stackAllocateImportKey: WasmFunctionImportKey? = null
    private var stackRestoreImportKey: WasmFunctionImportKey? = null

    init {
        registerDeclaredFunctionImports()
        registerImportedFunctionValues()
        registerRuntimeImports()
        registerStaticData()
        dataPool.seal(chirPackage.semanticId)

        imports = importRegistry.freeze()
        functionNameByImportedId = importKeyBySemanticId.mapValues { (_, key) -> importRegistry.functionName(key) }
        functionTypeByImportedId = importKeyBySemanticId.mapValues { (_, key) -> key.type }
        val functionIndices = linkedMapOf<ChirSemanticId, UInt>()
        importKeyBySemanticId.forEach { (semanticId, key) ->
            val internalName = importRegistry.functionName(key)
            val index = imports.indexOfFirst { it.functionName == internalName }
            if (index < 0) fail("imported function ${semanticId.value} has no function index", semanticId)
            functionIndices[semanticId] = index.toUInt()
        }
        definedFunctions.forEachIndexed { index, function ->
            functionIndices[function.semanticId] = (imports.size + index).toUInt()
        }
        functionIndexById = functionIndices
        functionTableNames = imports.map(WasmImport::functionName) + definedFunctions.map { functionNameById.getValue(it.semanticId) }

        val importedGlobals = collectImportedGlobals()
        globalImports = importedGlobals.mapIndexed { index, record ->
            importedGlobalKeyBySemanticId[record.semanticId] = record.key
            WasmGlobalImport(
                moduleName = record.key.moduleName,
                importName = record.key.importName,
                globalName = "__wasm_import_global_$index",
                type = record.key.type,
                mutable = record.key.mutable,
            )
        }
        val globalNames = linkedMapOf<ChirSemanticId, String>()
        importedGlobals.forEachIndexed { index, record -> globalNames[record.semanticId] = "__wasm_import_global_$index" }

        val definedGlobals = chirPackage.members.globalVariables
            .distinctBy(ChirVariableDeclaration::semanticId)
            .sortedBy { it.semanticId.value }
        definedGlobals.forEachIndexed { index, variable -> globalNames[variable.semanticId] = "__wasm_global_$index" }
        globalNameById = globalNames
        globals = definedGlobals.mapIndexed { index, variable ->
            val type = mapper.valueType(variable.type, variable.semanticId)
            WasmGlobal(
                name = "__wasm_global_$index",
                type = type,
                mutable = variable.mutable,
                initializer = WasmLiteralParser.globalInitializer(variable, type, mapper, dataPool),
            )
        }
    }

    fun importedFunctionName(semanticId: ChirSemanticId): String = functionNameByImportedId[semanticId]
        ?: fail("CHIR references unknown imported function ${semanticId.value}", semanticId)

    fun definedFunctionName(semanticId: ChirSemanticId): String = functionNameById[semanticId]
        ?: fail("CHIR references unknown defined function ${semanticId.value}", semanticId)

    fun functionSymbolName(semanticId: ChirSemanticId): String =
        functionNameById[semanticId] ?: functionNameByImportedId[semanticId]
        ?: fail("CHIR references unknown function ${semanticId.value}", semanticId)

    fun functionTableIndex(semanticId: ChirSemanticId): UInt = functionIndexById[semanticId]
        ?: fail("CHIR references function ${semanticId.value} without a table index", semanticId)

    fun functionTypeForValue(typeRef: ChirTypeRef, semanticId: ChirSemanticId): WasmFunctionType =
        functionTypeFromRef(typeRef, semanticId)

    fun runtimeFunctionName(expressionId: ChirSemanticId): String = runtimeKeyByExpressionId[expressionId]
        ?.let(importRegistry::functionName)
        ?: fail("runtime import was not registered for expression ${expressionId.value}", expressionId)

    fun importedGlobalName(semanticId: ChirSemanticId): String = importedGlobalKeyBySemanticId[semanticId]
        ?.let { globalNameById[semanticId] }
        ?: fail("CHIR references unknown imported global ${semanticId.value}", semanticId)

    fun throwFunctionName(sourceId: ChirSemanticId): String = throwImportKey
        ?.let(importRegistry::functionName)
        ?: fail("throw runtime import was not registered", sourceId)

    fun exceptionMatchesFunctionName(sourceId: ChirSemanticId): String = exceptionMatchesImportKey
        ?.let(importRegistry::functionName)
        ?: fail("exception matching runtime import was not registered", sourceId)

    fun allocationFunctionName(sourceId: ChirSemanticId): String = allocationImportKey
        ?.let(importRegistry::functionName)
        ?: fail("allocation runtime import was not registered", sourceId)

    fun stackMarkFunctionName(sourceId: ChirSemanticId): String = stackMarkImportKey
        ?.let(importRegistry::functionName)
        ?: fail("stack mark runtime import was not registered", sourceId)

    fun stackAllocateFunctionName(sourceId: ChirSemanticId): String = stackAllocateImportKey
        ?.let(importRegistry::functionName)
        ?: fail("stack allocation runtime import was not registered", sourceId)

    fun stackRestoreFunctionName(sourceId: ChirSemanticId): String = stackRestoreImportKey
        ?.let(importRegistry::functionName)
        ?: fail("stack restore runtime import was not registered", sourceId)

    fun wasiExitFunctionName(sourceId: ChirSemanticId): String = wasiExitImportKey
        ?.let(importRegistry::functionName)
        ?: fail("WASI proc_exit import was not registered", sourceId)

    fun findFunctionByName(name: String): ChirFunctionDeclaration? = definedFunctions.singleOrNull { it.name == name }

    fun definedFunctionSemanticId(name: String, sourceId: ChirSemanticId): ChirSemanticId =
        findFunctionByName(name)?.semanticId
            ?: fail("entry function '$name' is missing or overloaded", sourceId)

    private fun registerDeclaredFunctionImports() {
        importedFunctionDeclarations.forEach { declaration ->
            val type = mapper.functionType(
                declaration.parameters.map(ChirVariableDeclaration::type),
                declaration.returnType,
                declaration.semanticId,
            )
            val key = importRegistry.register(
                moduleName = declaration.attributes.stringAttribute("wasm.import.module") ?: targetProfile.runtimeImportModule,
                importName = declaration.attributes.stringAttribute("wasm.import.name") ?: declaration.name,
                type = type,
                sourceId = declaration.semanticId,
            )
            importKeyBySemanticId[declaration.semanticId] = key
        }
    }

    private fun registerImportedFunctionValues() {
        importedValueById.values.sortedBy { it.semanticId.value }.forEach { value ->
            val type = functionTypeFromRef(value.type, value.semanticId)
            val key = importRegistry.register(
                moduleName = value.attributes.stringAttribute("wasm.import.module") ?: targetProfile.runtimeImportModule,
                importName = value.attributes.stringAttribute("wasm.import.name") ?: value.name,
                type = type,
                sourceId = value.semanticId,
            )
            val existing = importKeyBySemanticId.put(value.semanticId, key)
            if (existing != null && existing != key) {
                fail("imported function ${value.semanticId.value} has conflicting CHIR signatures", value.semanticId)
            }
        }
    }

    private fun registerRuntimeImports() {
        val stackFunction = definedFunctions.firstOrNull { function ->
            function.blocks.any { block -> block.expressions.any { expression ->
                expression is org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression &&
                    org.cangnova.cangjie.chir.core.expression.ChirMemoryOperation.parse(expression.operation) ==
                    org.cangnova.cangjie.chir.core.expression.ChirMemoryOperation.ALLOCA
            } }
        }
        stackFunction?.let { function ->
            stackMarkImportKey = importRegistry.register(
                targetProfile.runtimeImportModule,
                "cangjie.stack.mark",
                WasmFunctionType(emptyList(), listOf(WasmValueType.I32)),
                function.semanticId,
            )
            stackAllocateImportKey = importRegistry.register(
                targetProfile.runtimeImportModule,
                "cangjie.stack.alloc",
                WasmFunctionType(listOf(WasmValueType.I32), listOf(WasmValueType.I32)),
                function.semanticId,
            )
            stackRestoreImportKey = importRegistry.register(
                targetProfile.runtimeImportModule,
                "cangjie.stack.restore",
                WasmFunctionType(listOf(WasmValueType.I32), emptyList()),
                function.semanticId,
            )
        }

        chirPackage.allDeclarations.filterIsInstance<ChirFunctionDeclaration>().forEach { function ->
            function.blocks.forEach { block ->
                block.expressions.forEach { expression ->
                    if (expression is ChirOtherExpression) registerRuntimeExpression(expression)
                    if (expression is ChirOtherExpression) registerRuntimeCasts(expression)
                    if (expression is ChirBinaryExpression) {
                        registerFloatingRemainder(expression)
                        registerFloat16Binary(expression)
                        registerGenericBinary(expression)
                    }
                    if (expression is ChirUnaryExpression) {
                        registerFloat16Unary(expression)
                        registerGenericUnary(expression)
                    }
                }
                if (block.terminator is ChirThrowTerminator) {
                    val throwTerminator = block.terminator as ChirThrowTerminator
                    val hasCatchAll = if (throwTerminator.catchDispatches.isEmpty()) {
                        throwTerminator.unwindTargetBlockId != null
                    } else {
                        throwTerminator.catchDispatches.any { it.exceptionTypeName == null }
                    }
                    if (!hasCatchAll) {
                        throwImportKey = importRegistry.register(
                            targetProfile.runtimeImportModule,
                            "cangjie.throw",
                            WasmFunctionType(listOf(WasmValueType.I32), emptyList()),
                            throwTerminator.semanticId,
                        )
                    }
                    if (throwTerminator.catchDispatches.any { it.exceptionTypeName != null }) {
                        exceptionMatchesImportKey = importRegistry.register(
                            targetProfile.runtimeImportModule,
                            "cangjie.exception.matches",
                            WasmFunctionType(listOf(WasmValueType.I32, WasmValueType.I32), listOf(WasmValueType.I32)),
                            throwTerminator.semanticId,
                        )
                    }
                }
            }
        }

        val tuplePackFunction = definedFunctions.firstOrNull { it.blocks.any { block -> block.expressions.any(::isTuplePack) } }
        tuplePackFunction?.let { function ->
            allocationImportKey = importRegistry.register(
                targetProfile.runtimeImportModule,
                "cangjie.alloc",
                WasmFunctionType(listOf(WasmValueType.I32), listOf(WasmValueType.I32)),
                function.semanticId,
            )
        }

        val entryName = input.options.entryFunctionName
        if (input.options.target == WasmTarget.WASI && entryName != null) {
            val entry = findFunctionByName(entryName)
                ?: fail("WASI entry function '$entryName' is missing or overloaded", chirPackage.semanticId)
            if (entry.parameters.isEmpty() && entry.returnType in setOf(
                    ChirResolvedTypeRef(org.cangnova.cangjie.chir.core.type.ChirPrimitiveType.INT32),
                    ChirResolvedTypeRef(org.cangnova.cangjie.chir.core.type.ChirPrimitiveType.UINT32),
                )
            ) {
                wasiExitImportKey = importRegistry.register(
                    targetProfile.wasiImportModule ?: fail("WASI profile has no system import module", entry.semanticId),
                    "proc_exit",
                    WasmFunctionType(listOf(WasmValueType.I32), emptyList()),
                    entry.semanticId,
                )
            }
        }
    }

    private fun registerRuntimeExpression(expression: ChirOtherExpression) {
        val operation = ChirOtherOperation.parse(expression.operation) ?: return
        val runtimeField = when (operation) {
            ChirOtherOperation.STRING_BUILD -> "cangjie.string.build"
            ChirOtherOperation.STRING_APPEND -> "cangjie.string.append"
            ChirOtherOperation.STRING_RESULT -> "cangjie.string.result"
            ChirOtherOperation.TUPLE_PACK -> return
            ChirOtherOperation.TUPLE_EXTRACT -> return
            else -> return
        }
        val parameterTypes = expression.operands.map { mapper.valueType(it.type, expression.semanticId) }
        val resultType = expression.resultType?.let { mapper.valueTypeOrNull(it, expression.semanticId) }
        val type = WasmFunctionType(parameterTypes, resultType?.let(::listOf).orEmpty())
        val suffix = buildString {
            append(type.parameters.joinToString("_") { it.displayName })
            append("_to_")
            append(type.results.firstOrNull()?.displayName ?: "void")
        }
        runtimeKeyByExpressionId[expression.semanticId] = importRegistry.register(
            targetProfile.runtimeImportModule,
            "$runtimeField.$suffix",
            type,
            expression.semanticId,
        )
    }

    private fun registerFloatingRemainder(expression: ChirBinaryExpression) {
        if (ChirBinaryOperator.parse(expression.operator) != ChirBinaryOperator.SIGNED_REM) return
        val valueType = mapper.valueType(expression.left.type, expression.semanticId)
        if (valueType != WasmValueType.F32 && valueType != WasmValueType.F64) return
        val key = importRegistry.register(
            targetProfile.runtimeImportModule,
            "cangjie.float.rem.${valueType.displayName}",
            WasmFunctionType(listOf(valueType, valueType), listOf(valueType)),
            expression.semanticId,
        )
        runtimeKeyByExpressionId[expression.semanticId] = key
    }

    private fun registerFloat16Binary(expression: ChirBinaryExpression) {
        if (!isFloat16(expression.left.type)) return
        val operator = ChirBinaryOperator.parse(expression.operator)
            ?: fail("unsupported Float16 binary operator '${expression.operator}'", expression.semanticId)
        if (operator !in FLOAT16_OPERATORS) {
            fail("Float16 does not support ${operator.family} operation '${operator.canonicalName}'", expression.semanticId)
        }
        val resultType = mapper.valueType(expression.resultType, expression.semanticId)
        val key = importRegistry.register(
            targetProfile.runtimeImportModule,
            "cangjie.float16.binary.${operator.canonicalName}",
            WasmFunctionType(listOf(WasmValueType.I32, WasmValueType.I32), listOf(resultType)),
            expression.semanticId,
        )
        runtimeKeyByExpressionId[expression.semanticId] = key
    }

    private fun registerGenericBinary(expression: ChirBinaryExpression) {
        val identifier = genericIdentifier(expression.left.type) ?: return
        val operator = ChirBinaryOperator.parse(expression.operator)
            ?: fail("unsupported generic binary operator '${expression.operator}'", expression.semanticId)
        val typeId = WasmRuntimeTypeIds.of(identifier)
        val key = importRegistry.register(
            targetProfile.runtimeImportModule,
            "cangjie.generic.binary.${operator.canonicalName}.${typeId.toUInt().toString(16)}",
            WasmFunctionType(
                listOf(WasmValueType.I32, WasmValueType.I32, WasmValueType.I32),
                listOf(mapper.valueType(expression.resultType, expression.semanticId)),
            ),
            expression.semanticId,
        )
        runtimeKeyByExpressionId[expression.semanticId] = key
    }

    private fun registerFloat16Unary(expression: ChirUnaryExpression) {
        if (!isFloat16(expression.operand.type)) return
        val operator = ChirUnaryOperator.parse(expression.operator)
            ?: fail("unsupported Float16 unary operator '${expression.operator}'", expression.semanticId)
        val key = importRegistry.register(
            targetProfile.runtimeImportModule,
            "cangjie.float16.unary.${operator.canonicalName}",
            WasmFunctionType(listOf(WasmValueType.I32), listOf(mapper.valueType(expression.resultType, expression.semanticId))),
            expression.semanticId,
        )
        runtimeKeyByExpressionId[expression.semanticId] = key
    }

    private fun registerGenericUnary(expression: ChirUnaryExpression) {
        val identifier = genericIdentifier(expression.operand.type) ?: return
        val operator = ChirUnaryOperator.parse(expression.operator)
            ?: fail("unsupported generic unary operator '${expression.operator}'", expression.semanticId)
        val typeId = WasmRuntimeTypeIds.of(identifier)
        val key = importRegistry.register(
            targetProfile.runtimeImportModule,
            "cangjie.generic.unary.${operator.canonicalName}.${typeId.toUInt().toString(16)}",
            WasmFunctionType(
                listOf(WasmValueType.I32, WasmValueType.I32),
                listOf(mapper.valueType(expression.resultType, expression.semanticId)),
            ),
            expression.semanticId,
        )
        runtimeKeyByExpressionId[expression.semanticId] = key
    }

    private fun registerRuntimeCasts(expression: ChirOtherExpression) {
        val operation = ChirOtherOperation.parse(expression.operation) ?: return
        if (operation !in RUNTIME_CAST_OPERATIONS || expression.operands.size != 1) return
        val targetType = expression.resultType ?: return
        val sourceType = expression.operands.single().type
        val sourceGeneric = genericIdentifier(sourceType)
        val targetGeneric = genericIdentifier(targetType)
        val sourceHalf = isFloat16(sourceType)
        val targetHalf = isFloat16(targetType)
        if (sourceGeneric == null && targetGeneric == null && !sourceHalf && !targetHalf) return

        val sourceId = WasmRuntimeTypeIds.of(sourceGeneric ?: sourceType.renderName)
        val targetId = WasmRuntimeTypeIds.of(targetGeneric ?: targetType.renderName)
        val runtimeParameters = listOf(mapper.valueType(sourceType, expression.semanticId)) +
            if (sourceGeneric != null || targetGeneric != null) {
                listOf(WasmValueType.I32, WasmValueType.I32)
            } else {
                emptyList()
            }
        val (fieldName, parameters) = when {
            sourceHalf || targetHalf ->
                "cangjie.float16.cast.${operation.canonicalName}.${sourceId.toUInt().toString(16)}_${targetId.toUInt().toString(16)}" to
                    runtimeParameters
            else ->
                "cangjie.generic.cast.${operation.canonicalName}.${sourceId.toUInt().toString(16)}_${targetId.toUInt().toString(16)}" to runtimeParameters
        }
        val key = importRegistry.register(
            targetProfile.runtimeImportModule,
            fieldName,
            WasmFunctionType(parameters, listOf(mapper.valueType(targetType, expression.semanticId))),
            expression.semanticId,
        )
        runtimeKeyByExpressionId[expression.semanticId] = key
    }

    private fun genericIdentifier(typeRef: ChirTypeRef): String? =
        ((typeRef as? ChirResolvedTypeRef)?.type as? ChirGenericType)?.identifier

    private fun isFloat16(typeRef: ChirTypeRef): Boolean =
        (typeRef as? ChirResolvedTypeRef)?.type == ChirPrimitiveType.FLOAT16

    private fun collectImportedGlobals(): List<ImportedGlobalRecord> {
        val declarations = chirPackage.members.importedVariables.map { variable ->
            ImportedGlobalSource(
                semanticId = variable.semanticId,
                name = variable.name,
                typeRef = variable.type,
                mutable = variable.mutable,
                attributes = variable.attributes,
            )
        }
        val values = chirPackage.allDeclarations.filterIsInstance<ChirFunctionDeclaration>()
            .flatMap { function -> function.blocks.flatMap { block -> block.expressions.flatMap(ChirExpression::values) + block.terminator.values() } }
            .filterIsInstance<ChirImportedVariableValue>()
            .map { value ->
                ImportedGlobalSource(
                    semanticId = value.semanticId,
                    name = value.name,
                    typeRef = value.type,
                    mutable = value.attributes.booleanAttribute("mutable") == true,
                    attributes = value.attributes,
                )
            }
        val unique = (declarations + values).distinctBy(ImportedGlobalSource::semanticId).sortedBy { it.semanticId.value }
        return unique.map { source ->
            val key = WasmGlobalImportKey(
                moduleName = source.attributes.stringAttribute("wasm.import.module") ?: targetProfile.runtimeImportModule,
                importName = source.attributes.stringAttribute("wasm.import.name") ?: source.name,
                type = mapper.valueType(source.typeRef, source.semanticId),
                mutable = source.mutable,
            )
            importedGlobalKeyBySemanticId[source.semanticId] = key
            ImportedGlobalRecord(source.semanticId, key)
        }
    }

    private fun registerStaticData() {
        chirPackage.allDeclarations.filterIsInstance<ChirFunctionDeclaration>().forEach { function ->
            function.blocks.forEach { block ->
                block.expressions.flatMap(ChirExpression::values)
                    .filterIsInstance<ChirConstantValue>()
                    .forEach { WasmLiteralParser.registerStaticData(it, mapper, dataPool) }
                block.terminator.values().filterIsInstance<ChirConstantValue>()
                    .forEach { WasmLiteralParser.registerStaticData(it, mapper, dataPool) }
            }
        }
        chirPackage.members.globalVariables.forEach { WasmLiteralParser.registerGlobalStaticData(it, dataPool) }
    }

    private fun functionTypeFromRef(typeRef: ChirTypeRef, semanticId: ChirSemanticId): WasmFunctionType {
        val resolved = typeRef as? ChirResolvedTypeRef
            ?: fail("imported function ${semanticId.value} has unresolved type ${typeRef.renderName}", semanticId)
        val functionType = resolved.type as? ChirFunctionType
            ?: fail("imported value ${semanticId.value} does not have a CHIR function type", semanticId)
        val parameterTypes = functionType.receiverType?.let { listOf(it) + functionType.parameterTypes }
            ?: functionType.parameterTypes
        return mapper.functionType(parameterTypes, functionType.returnType, semanticId)
    }

    private fun isTuplePack(expression: ChirExpression): Boolean =
        expression is ChirOtherExpression && ChirOtherOperation.parse(expression.operation) == ChirOtherOperation.TUPLE_PACK

    private fun Sequence<ChirValue>.importedFunctions(): List<ChirImportedFunctionValue> =
        filterIsInstance<ChirImportedFunctionValue>().distinctBy(ChirImportedFunctionValue::semanticId).toList()

    private fun fail(message: String, semanticId: ChirSemanticId): Nothing =
        throw WasmCodegenException(message, semanticId)

    private data class ImportedGlobalSource(
        val semanticId: ChirSemanticId,
        val name: String,
        val typeRef: ChirTypeRef,
        val mutable: Boolean,
        val attributes: Set<ChirAttribute>,
    )

    private data class ImportedGlobalRecord(val semanticId: ChirSemanticId, val key: WasmGlobalImportKey)

    private companion object {
        val FLOAT16_OPERATORS = setOf(
            ChirBinaryOperator.ADD,
            ChirBinaryOperator.SUB,
            ChirBinaryOperator.MUL,
            ChirBinaryOperator.SIGNED_DIV,
            ChirBinaryOperator.SIGNED_REM,
            ChirBinaryOperator.EQUAL,
            ChirBinaryOperator.NOT_EQUAL,
            ChirBinaryOperator.SIGNED_LESS,
            ChirBinaryOperator.SIGNED_LESS_OR_EQUAL,
            ChirBinaryOperator.SIGNED_GREATER,
            ChirBinaryOperator.SIGNED_GREATER_OR_EQUAL,
            ChirBinaryOperator.FLOAT_EQUAL,
            ChirBinaryOperator.FLOAT_NOT_EQUAL,
            ChirBinaryOperator.FLOAT_LESS,
            ChirBinaryOperator.FLOAT_LESS_OR_EQUAL,
            ChirBinaryOperator.FLOAT_GREATER,
            ChirBinaryOperator.FLOAT_GREATER_OR_EQUAL,
        )
        val RUNTIME_CAST_OPERATIONS = setOf(
            ChirOtherOperation.BITCAST,
            ChirOtherOperation.PTRTOINT,
            ChirOtherOperation.INTTOPTR,
            ChirOtherOperation.TRUNC,
            ChirOtherOperation.ZEXT,
            ChirOtherOperation.SEXT,
            ChirOtherOperation.FPTRUNC,
            ChirOtherOperation.FPEXT,
            ChirOtherOperation.SITOFP,
            ChirOtherOperation.UITOFP,
            ChirOtherOperation.FPTOSI,
            ChirOtherOperation.FPTOUI,
        )
    }
}

internal data class WasmFunctionImportKey(
    val moduleName: String,
    val importName: String,
    val type: WasmFunctionType,
)

internal data class WasmGlobalImportKey(
    val moduleName: String,
    val importName: String,
    val type: WasmValueType,
    val mutable: Boolean,
)

private class WasmFunctionImportRegistry {
    private val keys = linkedSetOf<WasmFunctionImportKey>()
    private val typesByExternalName = linkedMapOf<Pair<String, String>, WasmFunctionType>()
    private var frozenImports: List<WasmImport>? = null
    private var nameByKey: Map<WasmFunctionImportKey, String>? = null

    fun register(
        moduleName: String,
        importName: String,
        type: WasmFunctionType,
        sourceId: ChirSemanticId,
    ): WasmFunctionImportKey {
        check(frozenImports == null) { "cannot register imports after freezing the Wasm module plan" }
        val key = WasmFunctionImportKey(moduleName, importName, type)
        val previousType = typesByExternalName.putIfAbsent(moduleName to importName, type)
        if (previousType != null && previousType != type) {
            throw WasmCodegenException(
                "host import '$moduleName.$importName' is requested with incompatible Wasm signatures",
                sourceId,
            )
        }
        keys += key
        return key
    }

    fun freeze(): List<WasmImport> {
        frozenImports?.let { return it }
        val orderedKeys = keys.sortedWith(compareBy<WasmFunctionImportKey>({ it.moduleName }, { it.importName }, { it.type.signatureKey() }))
        val names = orderedKeys.mapIndexed { index, key -> key to "__wasm_import_$index" }.toMap()
        nameByKey = names
        return orderedKeys.map { key ->
            WasmImport(key.moduleName, key.importName, names.getValue(key), key.type)
        }.also { frozenImports = it }
    }

    fun functionName(key: WasmFunctionImportKey): String = nameByKey?.get(key)
        ?: error("Wasm imports must be frozen before resolving a function symbol")

    private fun WasmFunctionType.signatureKey(): String =
        parameters.joinToString(prefix = "(", postfix = ")") { it.displayName } +
            results.joinToString(prefix = "->", transform = WasmValueType::displayName)
}

private fun ChirExpression.values(): List<ChirValue> = when (this) {
    is org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression -> listOf(operand)
    is org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression -> listOf(left, right)
    is org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression -> listOfNotNull(address, value)
    is ChirCallExpression -> listOf(callee) + arguments
    is ChirOtherExpression -> operands
    else -> throw WasmCodegenException("unsupported CHIR expression ${this::class.simpleName}", semanticId)
}

private fun ChirTerminator.values(): List<ChirValue> = when (this) {
    is ChirReturnTerminator -> listOfNotNull(returnValue)
    is org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator -> emptyList()
    is ChirConditionalBranchTerminator -> listOf(condition)
    is ChirThrowTerminator -> listOf(exceptionValue)
    is org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator -> emptyList()
    else -> throw WasmCodegenException("unsupported CHIR terminator ${this::class.simpleName}", semanticId)
}

private fun Set<ChirAttribute>.stringAttribute(name: String): String? =
    filterIsInstance<ChirStringAttribute>().firstOrNull { it.key == name }?.value

private fun Set<ChirAttribute>.booleanAttribute(name: String): Boolean? =
    filterIsInstance<ChirBooleanAttribute>().firstOrNull { it.key == name }?.enabled

private fun ChirFunctionDeclaration.blocksExpressions(): List<ChirExpression> = blocks.flatMap { it.expressions }

