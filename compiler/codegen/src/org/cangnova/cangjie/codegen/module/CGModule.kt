package org.cangnova.cangjie.codegen.module

import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirConstantExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirFieldExpression
import org.cangnova.cangjie.chir.core.expression.ChirIntrinsicKind
import org.cangnova.cangjie.chir.core.expression.ChirOtherOperation
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirTupleExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirBooleanAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.declaration.ChirCustomTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirNominalTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.allDeclarations
import org.cangnova.cangjie.chir.core.model.effectiveMemberDeclarations
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirGlobalValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedVariableValue
import org.cangnova.cangjie.chir.core.value.ChirValue
import org.cangnova.cangjie.codegen.backend.LlvmBackendApi
import org.cangnova.cangjie.codegen.backend.LlvmBackendEmissionOptions
import org.cangnova.cangjie.codegen.context.CGContext
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.cangnova.cangjie.codegen.dispatcher.ExpressionLoweringDispatcher
import org.cangnova.cangjie.codegen.function.CGFunction
import org.cangnova.cangjie.codegen.ir.LlvmFunctionArtifact
import org.cangnova.cangjie.codegen.ir.LlvmFunctionAbi
import org.cangnova.cangjie.codegen.ir.LlvmModuleArtifact
import org.cangnova.cangjie.codegen.ir.llvmSymbolIdentity
import org.cangnova.cangjie.codegen.ir.llvmSymbolIdentifier
import org.cangnova.cangjie.codegen.ir.parseLlvmSignature
import org.cangnova.cangjie.codegen.ir.sanitizeIdentifier
import org.cangnova.cangjie.codegen.ir.llvmIdentifier
import org.cangnova.cangjie.codegen.types.collectLlvmTypeDeclarations
import org.cangnova.cangjie.codegen.types.collectLlvmRawArrayRuntimePlan
import org.cangnova.cangjie.codegen.types.collectLlvmPowerRuntimePlan
import org.cangnova.cangjie.codegen.types.LLVM_UNIT_TYPE_NAME
import org.cangnova.cangjie.codegen.types.LlvmRawArrayCopyStrategy
import org.cangnova.cangjie.codegen.types.LlvmRawArrayRuntime
import org.cangnova.cangjie.codegen.types.LlvmIntegerCastSemantics
import org.cangnova.cangjie.codegen.types.LlvmOverflowExceptionRuntime
import org.cangnova.cangjie.codegen.types.LlvmStringLiteralRuntime

/**
 * 单个 CHIR module 到 LLVM module 的 lowering 单元。
 */
class CGModule(
    /**
     * 当前 codegen 共享上下文。
     */
    private val context: CGContext,
    /**
     * 待降低的 CHIR module。
     */
    private val module: ChirModule,
    /**
     * LLVM 后端 API，用于按选项生成 bitcode/object code。
     */
    private val backendApi: LlvmBackendApi,
    /**
     * 函数体表达式 lowering 分派器。
     */
    private val dispatcher: ExpressionLoweringDispatcher = ExpressionLoweringDispatcher(),
    /**
     * 当前 module 是否负责发射包级全局定义。
     */
    private val emitPackageDefinitions: Boolean = true,
) {
    private data class FunctionReference(
        val value: ChirValue,
        val abi: LlvmFunctionAbi,
    )

    /**
     * 降低当前 CHIR module 并返回 LLVM module 产物。
     */
    fun lower(): LlvmModuleArtifact {
        val lines = mutableListOf<String>()
        val functionOwnersById = functionOwnersBySemanticId()
        val allDeclarations = context.inputPackage.allDeclarations + module.allDeclarations
        val nominalTypeDeclarationsById = buildMap<ChirTypeDeclarationId, ChirNominalTypeDeclaration> {
            allDeclarations.filterIsInstance<ChirNominalTypeDeclaration>().forEach { declaration ->
                val previous = putIfAbsent(declaration.typeDeclarationId, declaration)
                check(previous == null || previous == declaration) {
                    "conflicting CHIR declarations for ${declaration.typeDeclarationId.qualifiedName} " +
                        "while lowering module ${module.name}"
                }
            }
        }
        val functionDeclarationsById = allDeclarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .associateBy { it.semanticId }
        val staticMemberFunctionIds = functionOwnersById.keys.filterTo(linkedSetOf()) { functionId ->
            functionDeclarationsById[functionId]?.isStatic == true
        }
        val functionsToLower = (packageFunctionsToLower() + module.allDeclarations.filterIsInstance<ChirFunctionDeclaration>())
            .distinctBy { it.semanticId }
            .filter { it.hasBody && !it.isImported }
        val functionReferences = collectFunctionReferences(
            functionsToLower,
            functionOwnersById,
            staticMemberFunctionIds,
        )
        val globalReferences = collectGlobalReferences(functionsToLower)
        val overflowExceptionMessages = LlvmIntegerCastSemantics.throwingMessages(functionsToLower)
        val stringLiteralRuntimePlan = LlvmStringLiteralRuntime.plan(
            literals = LlvmStringLiteralRuntime.collectLiterals(functionsToLower),
            additionalLiteralContents = overflowExceptionMessages,
        )
        val overflowExceptionRuntimePlan = LlvmOverflowExceptionRuntime.plan(overflowExceptionMessages)
        val rawArrayRuntimePlan = collectLlvmRawArrayRuntimePlan(
            context.inputPackage,
            module,
            context.typeLowering,
            additionalReferencedArrayTypes = listOfNotNull(stringLiteralRuntimePlan.requiredByteArrayTypeInfo),
        )
        val powerRuntimePlan = collectLlvmPowerRuntimePlan(functionsToLower)
        val usesCttzIntrinsic = functionsToLower.asSequence()
            .flatMap { it.blocks.asSequence() }
            .flatMap { it.expressions.asSequence() }
            .filterIsInstance<ChirOtherExpression>()
            .any {
                it.intrinsicKind == ChirIntrinsicKind.VECTOR_COMPARE_32 ||
                    it.intrinsicKind == ChirIntrinsicKind.VECTOR_INDEX_BYTE_32
            }
        val usesRawArrayInitialization = functionsToLower.asSequence()
            .flatMap { it.blocks.asSequence() }
            .flatMap { it.expressions.asSequence() }
            .filterIsInstance<ChirOtherExpression>()
            .any { ChirOtherOperation.parse(it.operation) == ChirOtherOperation.RAW_ARRAY_INIT_BY_VALUE }
        val rawArrayCopyStrategies = functionsToLower.asSequence()
            .flatMap { it.blocks.asSequence() }
            .flatMap { it.expressions.asSequence() }
            .filterIsInstance<ChirOtherExpression>()
            .filter { it.intrinsicKind == ChirIntrinsicKind.ARRAY_BUILT_IN_COPY_TO }
            .map { expression ->
                val source = expression.operands.firstOrNull() ?: throw CodegenLoweringException(
                    "ARRAY_BUILT_IN_COPY_TO requires a source array operand",
                    expression.semanticId,
                )
                LlvmRawArrayRuntime.copyStrategy(
                    LlvmRawArrayRuntime.arrayType(source.type, expression.semanticId).elementType,
                )
            }
            .toSet()
        if (context.options.emitModuleHeader) {
            lines += "; module ${context.moduleName(module)}"
            lines += "target triple = \"${context.options.targetTriple}\""
            lines += "target datalayout = \"${context.options.targetDataLayout}\""
        }
        if (context.options.emitComments) {
            lines += "; package ${context.inputPackage.name} access=${context.inputPackage.accessLevel}"
        }

        lines += (emitTypeDeclarations() + stringLiteralRuntimePlan.typeDeclarations + rawArrayRuntimePlan.typeDeclarations).distinct()
        lines += emitGlobalDeclarations(
            rawArrayRuntimePlan.runtimeFunctionAbis + overflowExceptionRuntimePlan.runtimeFunctionAbis,
            overflowExceptionRuntimePlan.overriddenImportedFunctionSymbols,
            functionOwnersById,
            functionReferences,
            globalReferences,
            functionsToLower.mapTo(linkedSetOf()) { it.semanticId },
        )
        lines += rawArrayRuntimePlan.globalDeclarations
        lines += stringLiteralRuntimePlan.globalDeclarations
        lines += overflowExceptionRuntimePlan.globalDeclarations
        lines += emitRuntimeDeclarations()
        lines += rawArrayRuntimePlan.runtimeDeclarations
        lines += overflowExceptionRuntimePlan.functionDeclarations
        lines += powerRuntimePlan.declarations
        if (usesCttzIntrinsic) {
            lines += "declare i32 @llvm.cttz.i32(i32, i1 immarg)"
        }
        if (usesRawArrayInitialization) {
            lines += "declare void @llvm.cj.gcwrite.ref(ptr addrspace(1), ptr addrspace(1), ptr addrspace(1))"
            lines += "declare void @llvm.cj.gcwrite.struct.p0(ptr addrspace(1), ptr addrspace(1), ptr, i64)"
        }
        if (LlvmRawArrayCopyStrategy.MEMMOVE in rawArrayCopyStrategies) {
            lines += "declare void @llvm.memmove.p1.p1.i64(ptr addrspace(1), ptr addrspace(1), i64, i1 immarg)"
        }
        if (LlvmRawArrayCopyStrategy.REFERENCE in rawArrayCopyStrategies) {
            lines += "declare void @llvm.cj.array.copy.ref.i64(" +
                "ptr addrspace(1), ptr addrspace(1), ptr addrspace(1), ptr addrspace(1), i64)"
        }
        if (LlvmRawArrayCopyStrategy.STRUCT in rawArrayCopyStrategies) {
            lines += "declare void @llvm.cj.array.copy.struct.i64(" +
                "ptr addrspace(1), ptr addrspace(1), ptr addrspace(1), ptr addrspace(1), i64)"
        }
        if (LlvmRawArrayCopyStrategy.GENERIC in rawArrayCopyStrategies) {
            lines += "declare void @llvm.cj.array.copy.generic.i64(" +
                "ptr addrspace(1), ptr addrspace(1), ptr addrspace(1), ptr addrspace(1), i64)"
        }
        lines += emitRuntimeEntryMappings()

        val functions = functionsToLower
            .map { declaration ->
                CGFunction(
                    declaration = declaration,
                    context = context,
                    dispatcher = dispatcher,
                    ownerType = functionOwnersById[declaration.semanticId],
                    memberFunctionIds = functionOwnersById.keys,
                    staticMemberFunctionIds = staticMemberFunctionIds,
                    memberFunctionOwnersById = functionOwnersById,
                    nominalTypeDeclarationsById = nominalTypeDeclarationsById,
                    stringLiteralValues = stringLiteralRuntimePlan.literalValues,
                ).lower()
            }
            .toList()
        lines += functions.flatMap { it.ir.lines() }
        lines += powerRuntimePlan.functionDefinitions

        val moduleIr = lines.filter { it.isNotBlank() }.joinToString(System.lineSeparator())
        val moduleName = context.moduleName(module)
        val emissionOptions = LlvmBackendEmissionOptions(
            targetTriple = context.options.targetTriple,
            targetDataLayout = context.options.targetDataLayout,
            targetCpu = context.options.targetCpu,
            targetFeatures = context.options.targetFeatures,
            optimizeModule = context.options.optimizeLlvmModule,
            optimizationLevel = context.options.optimizationLevel,
            relocationMode = context.options.relocationMode,
            codeModel = context.options.codeModel,
        )
        return LlvmModuleArtifact(
            name = moduleName,
            ir = moduleIr,
            functions = functions,
            bitcode = if (context.options.emitBitcode) {
                backendApi.emitBitcode(moduleName, moduleIr, emissionOptions)
            } else {
                null
            },
            objectCode = if (context.options.emitObjectCode) {
                backendApi.emitObjectCode(moduleName, moduleIr, emissionOptions)
            } else {
                null
            },
        )
    }

    /**
     * 收集当前 module 需要的 LLVM identified type 声明。
     */
    private fun emitTypeDeclarations(): List<String> {
        return collectLlvmTypeDeclarations(context.inputPackage, module, context.typeLowering)
    }

    /**
     * 发射包级全局变量、导入变量和导入函数声明。
     */
    private fun emitGlobalDeclarations(
        runtimeFunctionAbis: List<LlvmFunctionAbi>,
        overriddenImportedFunctionSymbols: Set<String>,
        functionOwnersById: Map<org.cangnova.cangjie.chir.core.identity.ChirSemanticId, ChirCustomTypeDeclaration>,
        functionReferences: List<FunctionReference>,
        globalReferences: List<ChirValue>,
        definedFunctionIds: Set<org.cangnova.cangjie.chir.core.identity.ChirSemanticId>,
    ): List<String> {
        val declarations = mutableListOf<String>()
        if (emitPackageDefinitions) {
            context.inputPackage.members.globalVariables.forEach { variable ->
                val ty = context.typeLowering.lower(variable.type)
                val name = llvmSymbolIdentifier(variable.name, variable.semanticId)
                val linkage = attributeValue(variable.attributes, "linkage")
                val initializer = attributeValue(variable.attributes, "initializer")
                    ?: defaultGlobalInitializer(variable.name, ty, variable.semanticId)
                val storageClass = if (variable.mutable) "global" else "constant"
                declarations += buildString {
                    append("@$name = ")
                    if (!linkage.isNullOrBlank()) {
                        append(linkage)
                        append(' ')
                    }
                    append("$storageClass $ty $initializer")
                }
            }
        }
        val declaredGlobalIds = context.inputPackage.members.importedVariables.mapTo(linkedSetOf()) { it.semanticId }
        context.inputPackage.members.importedVariables.forEach { variable ->
            val ty = context.typeLowering.lower(variable.type)
            val name = llvmSymbolIdentifier(variable.name, variable.semanticId)
            val linkage = attributeValue(variable.attributes, "linkage")
            declarations += buildString {
                append("@$name = ")
                append(if (linkage.isNullOrBlank()) "external" else linkage)
                append(" global $ty")
            }
        }
        val definedGlobalIds = if (emitPackageDefinitions) {
            context.inputPackage.members.globalVariables.mapTo(linkedSetOf()) { it.semanticId }
        } else {
            emptySet()
        }
        val referencedGlobalsBySymbol = linkedMapOf<String, String>()
        globalReferences.forEach { value ->
            if (value.semanticId in declaredGlobalIds || value.semanticId in definedGlobalIds) return@forEach
            val name = when (value) {
                is ChirGlobalValue -> value.name
                is ChirImportedVariableValue -> value.name
                else -> error("global reference collector returned ${value::class.simpleName}")
            }
            val llvmType = context.typeLowering.lower(value.type)
            val symbol = llvmSymbolIdentity(name, value.semanticId)
            val previousType = referencedGlobalsBySymbol.putIfAbsent(symbol, llvmType)
            if (previousType != null && previousType != llvmType) {
                throw CodegenLoweringException(
                    "global references to @$symbol have conflicting LLVM types $previousType and $llvmType",
                    value.semanticId,
                )
            }
            if (previousType == null) {
                declarations += "@${llvmIdentifier(symbol)} = external global $llvmType"
            }
        }
        val runtimeFunctionsBySymbol = linkedMapOf<String, LlvmFunctionAbi>()
        fun registerRuntimeAbi(abi: LlvmFunctionAbi) {
            val previous = runtimeFunctionsBySymbol.putIfAbsent(abi.symbol, abi)
            if (previous != null && previous != abi) {
                throw CodegenLoweringException(
                    "runtime symbol @${abi.symbol} has conflicting LLVM ABIs",
                    null,
                )
            }
        }
        runtimeFunctionAbis.forEach(::registerRuntimeAbi)
        context.runtimeSymbols.allSymbols().forEach { symbol ->
            val signature = parseLlvmSignature(symbol.llvmSignature) ?: throw CodegenLoweringException(
                "runtime symbol ${symbol.name} has invalid LLVM signature '${symbol.llvmSignature}'",
                null,
            )
            registerRuntimeAbi(LlvmFunctionAbi(symbol.name, signature.returnType, signature.argumentTypes))
        }
        val allFunctions = context.inputPackage.allDeclarations.filterIsInstance<ChirFunctionDeclaration>()
        val importedFunctionsBySymbol = linkedMapOf<String, LlvmFunctionAbi>()
        allFunctions
            .filter { it.isImported || !it.hasBody }
            .forEach { function ->
                val ownerType = functionOwnersById[function.semanticId]
                val symbol = llvmSymbolIdentity(function.name, function.semanticId)
                val functionAbi = importedFunctionAbi(function, ownerType)
                val prior = importedFunctionsBySymbol.putIfAbsent(symbol, functionAbi)
                if (prior != null && prior != functionAbi) {
                    throw CodegenLoweringException(
                        "imported function declarations for LLVM symbol @$symbol have conflicting ABIs",
                        function.semanticId,
                    )
                }
                val runtimeAbi = runtimeFunctionsBySymbol[symbol]
                if (runtimeAbi != null && symbol !in overriddenImportedFunctionSymbols) {
                    if (runtimeAbi.returnType != functionAbi.returnType || runtimeAbi.argumentTypes != functionAbi.argumentTypes) {
                        throw CodegenLoweringException(
                            "imported function ${function.name} has ABI ${functionAbi.returnType}(${functionAbi.argumentTypes.joinToString()}) " +
                                "but the runtime ABI for @$symbol is ${runtimeAbi.returnType}(${runtimeAbi.argumentTypes.joinToString()})",
                            function.semanticId,
                        )
                    }
                } else if (prior == null && symbol !in overriddenImportedFunctionSymbols) {
                    declarations += renderImportedFunctionDeclaration(function, ownerType)
                }
            }
        val referencedFunctionsBySymbol = linkedMapOf<String, LlvmFunctionAbi>()
        functionReferences.forEach { reference ->
            val abi = reference.abi
            if (reference.value.semanticId in definedFunctionIds) return@forEach
            val importedAbi = importedFunctionsBySymbol[abi.symbol]
            if (importedAbi != null && importedAbi != abi) {
                throw CodegenLoweringException(
                    "function reference ${reference.value.displayName} conflicts with imported declaration ABI for @${abi.symbol}",
                    reference.value.semanticId,
                )
            }
            val runtimeAbi = runtimeFunctionsBySymbol[abi.symbol]
            if (runtimeAbi != null && runtimeAbi != abi) {
                throw CodegenLoweringException(
                    "function reference ${reference.value.displayName} has ABI ${abi.returnType}(${abi.argumentTypes.joinToString()}) " +
                        "but runtime symbol @${abi.symbol} has ABI ${runtimeAbi.returnType}(${runtimeAbi.argumentTypes.joinToString()})",
                    reference.value.semanticId,
                )
            }
            val previous = referencedFunctionsBySymbol.putIfAbsent(abi.symbol, abi)
            if (previous != null && previous != abi) {
                throw CodegenLoweringException(
                    "function references to @${abi.symbol} have conflicting LLVM ABIs",
                    reference.value.semanticId,
                )
            }
            if (previous == null && importedAbi == null && runtimeAbi == null) {
                declarations += renderFunctionReferenceDeclaration(reference)
            }
        }
        return declarations
    }

    private fun collectFunctionReferences(
        functions: List<ChirFunctionDeclaration>,
        functionOwnersById: Map<org.cangnova.cangjie.chir.core.identity.ChirSemanticId, ChirCustomTypeDeclaration>,
        staticMemberFunctionIds: Set<org.cangnova.cangjie.chir.core.identity.ChirSemanticId>,
    ): List<FunctionReference> {
        val references = linkedMapOf<org.cangnova.cangjie.chir.core.identity.ChirSemanticId, FunctionReference>()
        functions.asSequence()
            .flatMap { function -> function.blocks.asSequence().flatMap { it.expressions.asSequence() } }
            .filterIsInstance<ChirCallExpression>()
            .forEach { call ->
                if (call.virtualMethodContext != null) return@forEach
                val value = when (val callee = call.callee) {
                    is ChirFunctionValue, is ChirImportedFunctionValue -> callee
                    else -> return@forEach
                }
                val functionType = (value.type as? ChirResolvedTypeRef)?.type as? ChirFunctionType
                    ?: throw CodegenLoweringException(
                        "function reference ${value.displayName} does not have a resolved function type",
                        call.semanticId,
                    )
                val ownerType = functionOwnersById[value.semanticId]
                val memberTypeInfoCount = when {
                    ownerType == null -> 0
                    value.semanticId in staticMemberFunctionIds -> 2
                    else -> 1
                }
                val hiddenTypeInfoCount = if (ownerType == null && call.thisType != null) {
                    1
                } else {
                    memberTypeInfoCount
                }
                val abi = LlvmFunctionAbi(
                    symbol = llvmSymbolIdentity(value.displayName.orEmpty(), value.semanticId),
                    returnType = context.typeLowering.lowerFunctionReturn(functionType.returnType),
                    argumentTypes = functionType.parameterTypes.map(context.typeLowering::lower) +
                        List(call.instantiatedTypeArguments.size) { "ptr" } +
                        List(hiddenTypeInfoCount) { "ptr" },
                )
                val reference = FunctionReference(value, abi)
                val previous = references.putIfAbsent(value.semanticId, reference)
                if (previous != null && previous.abi != abi) {
                    throw CodegenLoweringException(
                        "function references to ${value.displayName} have conflicting LLVM ABIs",
                        call.semanticId,
                    )
                }
            }
        return references.values.toList()
    }

    private fun collectGlobalReferences(functions: List<ChirFunctionDeclaration>): List<ChirValue> {
        val references = linkedMapOf<org.cangnova.cangjie.chir.core.identity.ChirSemanticId, ChirValue>()
        fun visitValue(value: ChirValue) {
            when (value) {
                is ChirGlobalValue, is ChirImportedVariableValue -> references.putIfAbsent(value.semanticId, value)
                else -> Unit
            }
        }
        fun visitExpression(expression: ChirExpression) {
            when (expression) {
                is ChirUnaryExpression -> visitValue(expression.operand)
                is ChirBinaryExpression -> {
                    visitValue(expression.left)
                    visitValue(expression.right)
                }
                is ChirMemoryExpression -> {
                    visitValue(expression.address)
                    expression.value?.let(::visitValue)
                }
                is ChirCallExpression -> {
                    if (expression.virtualMethodContext == null) visitValue(expression.callee)
                    expression.dispatchReceiver?.let(::visitValue)
                    expression.arguments.forEach(::visitValue)
                }
                is ChirConstantExpression -> visitValue(expression.value)
                is ChirOtherExpression -> expression.operands.forEach(::visitValue)
                is ChirTupleExpression -> expression.elements.forEach(::visitValue)
                is ChirFieldExpression -> visitValue(expression.base)
            }
        }
        functions.forEach { function ->
            function.blocks.forEach { block ->
                block.expressions.forEach(::visitExpression)
                when (val terminator = block.terminator) {
                    is ChirReturnTerminator -> terminator.returnValue?.let(::visitValue)
                    is ChirConditionalBranchTerminator -> visitValue(terminator.condition)
                    is ChirThrowTerminator -> visitValue(terminator.exceptionValue)
                    else -> Unit
                }
            }
        }
        return references.values.toList()
    }

    /**
     * 从嵌套声明关系恢复函数 owner，使 LLVM ABI 能区分普通函数与类型成员函数。
     */
    private fun functionOwnersBySemanticId(): Map<org.cangnova.cangjie.chir.core.identity.ChirSemanticId, ChirCustomTypeDeclaration> {
        val owners = linkedMapOf<org.cangnova.cangjie.chir.core.identity.ChirSemanticId, ChirCustomTypeDeclaration>()

        fun visit(owner: ChirCustomTypeDeclaration) {
            owner.effectiveMemberDeclarations().forEach { declaration ->
                when (declaration) {
                    is ChirFunctionDeclaration -> {
                        val previous = owners.putIfAbsent(declaration.semanticId, owner)
                        if (previous != null && previous.semanticId != owner.semanticId) {
                            throw CodegenLoweringException(
                                "function ${declaration.name} has multiple containing type declarations",
                                declaration.semanticId,
                            )
                        }
                    }
                    is ChirCustomTypeDeclaration -> visit(declaration)
                    else -> Unit
                }
            }
        }

        (context.inputPackage.allDeclarations + module.allDeclarations)
            .filterIsInstance<ChirCustomTypeDeclaration>()
            .distinctBy { it.semanticId }
            .forEach(::visit)
        return owners
    }

    /**
     * 发射运行时符号声明。
     */
    private fun emitRuntimeDeclarations(): List<String> {
        if (!context.options.emitRuntimeDeclarations) return emptyList()
        return context.runtimeSymbols.allSymbols().map { symbol ->
            val signature = parseLlvmSignature(symbol.llvmSignature)
                ?: throw CodegenLoweringException(
                    "runtime symbol ${symbol.name} has invalid LLVM signature '${symbol.llvmSignature}'",
                    null,
                )
            "declare ${signature.returnType} @${symbol.name}(${signature.argumentTypes.joinToString(", ")})"
        }
    }

    /**
     * 发射包初始化函数与包字面量初始化函数到运行时入口符号的映射。
     */
    private fun emitRuntimeEntryMappings(): List<String> {
        if (!emitPackageDefinitions) return emptyList()
        val localFunctionsById = (context.inputPackage.members.globalFunctions + context.inputPackage.modules.flatMap { it.declarations }
            .filterIsInstance<ChirFunctionDeclaration>())
            .associateBy { it.semanticId }
        val mappings = mutableListOf<String>()

        fun addMapping(functionId: org.cangnova.cangjie.chir.core.identity.ChirSemanticId?, symbolName: String) {
            if (functionId == null) return
            val function = localFunctionsById[functionId] ?: throw CodegenLoweringException(
                "package runtime entry $symbolName references missing function ${functionId.value}",
                functionId,
            )
            val targetName = llvmSymbolIdentifier(function.name, function.semanticId)
            mappings += "@$symbolName = internal constant ptr @$targetName"
        }

        addMapping(context.inputPackage.packageInitFunctionId, "cangjie.package.init")
        addMapping(context.inputPackage.packageLiteralInitFunctionId, "cangjie.package.literal_init")

        return mappings
    }

    /**
     * 返回当前 module 需要一并 lowering 的包级函数。
     */
    private fun packageFunctionsToLower(): List<ChirFunctionDeclaration> {
        if (!emitPackageDefinitions) return emptyList()

        fun memberFunctions(type: ChirCustomTypeDeclaration): List<ChirFunctionDeclaration> =
            type.effectiveMemberDeclarations().flatMap { declaration ->
                when (declaration) {
                    is ChirFunctionDeclaration -> listOf(declaration)
                    is ChirCustomTypeDeclaration -> memberFunctions(declaration)
                    else -> emptyList()
                }
            }

        val typeMemberFunctions = context.inputPackage.typeDefinitions
            .filterIsInstance<ChirCustomTypeDeclaration>()
            .flatMap(::memberFunctions)

        return context.inputPackage.members.globalFunctions + typeMemberFunctions
    }

    /**
     * 为未显式指定 initializer 的基础类型全局变量生成默认 initializer。
     */
    private fun defaultGlobalInitializer(
        variableName: String,
        llvmType: String,
        semanticId: org.cangnova.cangjie.chir.core.identity.ChirSemanticId,
    ): String {
        return when {
            llvmType == "i1" -> "0"
            llvmType.matches(Regex("i\\d+")) -> "0"
            llvmType == "half" || llvmType == "float" || llvmType == "double" -> "0.0"
            llvmType == "ptr" -> "null"
            llvmType == LLVM_UNIT_TYPE_NAME -> "zeroinitializer"
            else -> throw CodegenLoweringException(
                "global variable $variableName of LLVM type $llvmType requires an explicit initializer",
                semanticId,
            )
        }
    }

    /**
     * 渲染导入函数的 LLVM `declare` 行。
     */
    private fun importedFunctionAbi(
        function: ChirFunctionDeclaration,
        ownerType: ChirCustomTypeDeclaration?,
    ): LlvmFunctionAbi = LlvmFunctionAbi(
        symbol = llvmSymbolIdentity(function.name, function.semanticId),
        returnType = context.typeLowering.lowerFunctionReturn(function.returnType),
        argumentTypes = function.parameters.map { parameter -> context.typeLowering.lower(parameter.type) } +
            function.genericTypeParameters.map { "ptr" } +
            memberTypeInfoParameterTypes(function, ownerType),
    )

    /** 返回成员函数 ABI 中按官方顺序排列的隐藏类型信息参数。 */
    private fun memberTypeInfoParameterTypes(
        function: ChirFunctionDeclaration,
        ownerType: ChirCustomTypeDeclaration?,
    ): List<String> = when {
        ownerType == null -> emptyList()
        function.isStatic -> listOf("ptr", "ptr")
        else -> listOf("ptr")
    }

    private fun renderImportedFunctionDeclaration(
        function: ChirFunctionDeclaration,
        ownerType: ChirCustomTypeDeclaration?,
    ): String {
        val name = llvmSymbolIdentifier(function.name, function.semanticId)
        val returnType = context.typeLowering.lowerFunctionReturn(function.returnType)
        val linkage = attributeValue(function.attributes, "linkage")
        val callingConvention = attributeValue(function.attributes, "calling_conv")
            ?: attributeValue(function.attributes, "cc")
        val params = (function.parameters.map { parameter ->
            val parameterType = context.typeLowering.lower(parameter.type)
            val attrs = collectAttributeTokens(parameter.attributes)
            if (attrs.isEmpty()) parameterType else "$parameterType ${attrs.joinToString(" ")}"
        } + function.genericTypeParameters.map { "ptr" } + memberTypeInfoParameterTypes(function, ownerType)).joinToString(", ")
        val functionAttrs = collectAttributeTokens(function.attributes)

        return buildString {
            append("declare ")
            if (!linkage.isNullOrBlank()) {
                append(linkage)
                append(' ')
            }
            if (!callingConvention.isNullOrBlank()) {
                append(callingConvention)
                append(' ')
            }
            append(returnType)
            append(" @")
            append(name)
            append('(')
            append(params)
            append(')')
            if (functionAttrs.isNotEmpty()) {
                append(' ')
                append(functionAttrs.joinToString(" "))
            }
        }
    }

    private fun renderFunctionReferenceDeclaration(reference: FunctionReference): String {
        val value = reference.value
        val abi = reference.abi
        val linkage = attributeValue(value.attributes, "linkage")
        val callingConvention = attributeValue(value.attributes, "calling_conv")
            ?: attributeValue(value.attributes, "cc")
        val functionAttrs = collectAttributeTokens(value.attributes)
        return buildString {
            append("declare ")
            if (!linkage.isNullOrBlank()) {
                append(linkage)
                append(' ')
            }
            if (!callingConvention.isNullOrBlank()) {
                append(callingConvention)
                append(' ')
            }
            append(abi.returnType)
            append(" @")
            append(llvmIdentifier(abi.symbol))
            append('(')
            append(abi.argumentTypes.joinToString(", "))
            append(')')
            if (functionAttrs.isNotEmpty()) {
                append(' ')
                append(functionAttrs.joinToString(" "))
            }
        }
    }

    /**
     * 收集可直接打印到 LLVM IR 的属性 token。
     */
    private fun collectAttributeTokens(attributes: Set<ChirAttribute>): List<String> {
        val tokens = mutableListOf<String>()
        attributes.forEach { attribute ->
            when (attribute) {
                is ChirBooleanAttribute -> {
                    if (attribute.enabled && attribute.key !in reservedFunctionAttributeKeys) {
                        tokens += attribute.key
                    }
                }
                is ChirStringAttribute -> {
                    if (attribute.key == "align") {
                        tokens += "align ${attribute.value}"
                    }
                }
            }
        }
        return tokens
    }

    /**
     * 从属性集合中读取指定字符串属性值。
     */
    private fun attributeValue(attributes: Set<ChirAttribute>, key: String): String? {
        return attributes.asSequence()
            .filterIsInstance<ChirStringAttribute>()
            .firstOrNull { it.key == key }
            ?.value
    }

    private companion object {
        val reservedFunctionAttributeKeys = setOf("linkage", "personality", "calling_conv", "cc")
    }
}
