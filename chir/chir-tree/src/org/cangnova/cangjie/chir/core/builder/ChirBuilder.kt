package org.cangnova.cangjie.chir.core.builder

import org.cangnova.cangjie.chir.core.checker.ChirValidationReportFormatter
import org.cangnova.cangjie.chir.core.checker.ChirValidator
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.context.ChirContext
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.validateMinimalControlFlow
import org.cangnova.cangjie.chir.core.symbol.ChirReferenceBinder
import org.cangnova.cangjie.chir.core.symbol.ChirSymbol
import org.cangnova.cangjie.chir.core.symbol.ChirSymbolTable
import org.cangnova.cangjie.chir.core.symbol.DefaultChirSymbolTable

/**
 * CHIR 构建器接口。
 */
interface ChirBuilder {
    /**
     * 注册完整 CHIR 包及其包含的模块、声明和包级成员。
     */
    fun registerPackage(chirPackage: ChirPackage): Boolean

    /**
     * 注册单个 CHIR 声明。
     */
    fun registerDeclaration(declaration: ChirDeclaration): Boolean

    /**
     * 声明一个可被引用绑定解析的符号。
     */
    fun declareSymbol(symbol: ChirSymbol): Boolean

    /**
     * 将未绑定引用解析为已绑定引用。
     */
    fun bindReference(reference: UnboundChirReference): BoundChirReference?
}

/**
 * 默认 CHIR 构建器实现。
 */
class DefaultChirBuilder(
    /**
     * 构建过程中写入的 CHIR 上下文。
     */
    private val context: ChirContext,

    /**
     * 符号声明和查找表。
     */
    private val symbolTable: ChirSymbolTable = DefaultChirSymbolTable(),

    /**
     * 构建诊断收集器。
     */
    private val diagnostics: ChirDiagnosticCollector = NoopChirDiagnosticCollector,

    /**
     * 图结构校验器。
     */
    private val validator: ChirValidator = DefaultChirValidator(),
) : ChirBuilder {

    /**
     * 未绑定引用到符号表的绑定器。
     */
    private val binder = ChirReferenceBinder(symbolTable)

    /**
     * 校验并注册完整 CHIR 包。
     */
    override fun registerPackage(chirPackage: ChirPackage): Boolean {
        val report = validator.validatePackage(chirPackage, context)
        if (report.hasErrors) {
            diagnostics.report(
                ChirBuildError.InvalidGraph(
                    "package ${chirPackage.name} failed validation: ${ChirValidationReportFormatter.render(report)}",
                ),
            )
            return false
        }
        return runCatching {
            context.registerPackage(chirPackage)
            chirPackage.members.globalVariables.forEach(::registerDeclarationOrThrow)
            chirPackage.members.globalFunctions.forEach(::registerDeclarationOrThrow)
            chirPackage.members.importedVariables.forEach(::registerDeclarationOrThrow)
            chirPackage.members.importedFunctions.forEach(::registerDeclarationOrThrow)
            chirPackage.typeDefinitions.forEach(::registerDeclarationOrThrow)
            chirPackage.importedTypeDefinitions.forEach(::registerDeclarationOrThrow)
            chirPackage.modules.forEach { module ->
                context.registerModule(module)
                module.declarations.forEach(::registerDeclarationOrThrow)
            }
        }.onFailure {
            diagnostics.report(ChirBuildError.InvalidGraph("failed to register package ${chirPackage.name}: ${it.message}"))
        }.isSuccess
    }

    /**
     * 注册单个声明，失败时转换为构建诊断。
     */
    override fun registerDeclaration(declaration: ChirDeclaration): Boolean {
        return runCatching {
            registerDeclarationOrThrow(declaration)
        }.onFailure {
            diagnostics.report(ChirBuildError.InvalidGraph("failed to register declaration ${declaration.name}: ${it.message}"))
        }.isSuccess
    }

    /**
     * 注册符号并同步写入上下文。
     */
    override fun declareSymbol(symbol: ChirSymbol): Boolean {
        return runCatching {
            symbolTable.declare(symbol)
            context.registerSymbol(symbol)
        }.onFailure {
            diagnostics.report(ChirBuildError.DuplicateSymbol(symbol.name, symbol.semanticId, it.message ?: "duplicate symbol"))
        }.isSuccess
    }

    /**
     * 绑定引用，无法解析时报告未解析引用诊断。
     */
    override fun bindReference(reference: UnboundChirReference): BoundChirReference? {
        return binder.bind(reference).also {
            if (it == null) {
                diagnostics.report(
                    ChirBuildError.UnresolvedReference(
                        reference.referenceId,
                        reference.targetName,
                    ),
                )
            }
        }
    }

    /**
     * 注册声明并在函数声明上执行控制流基本校验。
     */
    private fun registerDeclarationOrThrow(declaration: ChirDeclaration) {
        context.registerDeclaration(declaration)
        if (declaration is ChirFunctionDeclaration) {
            validateFunctionDeclaration(declaration)
        }
    }

    /**
     * 校验函数声明的名称、基本块集合和最小控制流结构。
     */
    private fun validateFunctionDeclaration(function: ChirFunctionDeclaration) {
        if (function.name.isBlank()) {
            throw IllegalArgumentException("function id=${function.semanticId} has blank name")
        }
        if (function.blocks.isEmpty()) {
            throw IllegalArgumentException("function ${function.name} must have at least one block")
        }
        val duplicatedBlockIds = function.blocks
            .groupBy(ChirBlock::semanticId)
            .filterValues { it.size > 1 }
            .keys
        if (duplicatedBlockIds.isNotEmpty()) {
            throw IllegalArgumentException("function ${function.name} has duplicate block ids: $duplicatedBlockIds")
        }
        val errors = validateMinimalControlFlow(function)
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(errors.joinToString("; "))
        }
    }
}

/**
 * 尚未绑定到声明的 CHIR 引用。
 */
data class UnboundChirReference(
    /**
     * 引用自身的语义标识。
     */
    val referenceId: ChirSemanticId,

    /**
     * 引用目标名称。
     */
    val targetName: String,
)

/**
 * 已绑定到目标声明的 CHIR 引用。
 */
data class BoundChirReference(
    /**
     * 引用自身的语义标识。
     */
    val referenceId: ChirSemanticId,

    /**
     * 被绑定的目标声明标识。
     */
    val targetDeclarationId: ChirSemanticId,
)
