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

interface ChirBuilder {
    fun registerPackage(chirPackage: ChirPackage): Boolean
    fun registerDeclaration(declaration: ChirDeclaration): Boolean
    fun declareSymbol(symbol: ChirSymbol): Boolean
    fun bindReference(reference: UnboundChirReference): BoundChirReference?
}

class DefaultChirBuilder(
    private val context: ChirContext,
    private val symbolTable: ChirSymbolTable = DefaultChirSymbolTable(),
    private val diagnostics: ChirDiagnosticCollector = NoopChirDiagnosticCollector,
    private val validator: ChirValidator = DefaultChirValidator(),
) : ChirBuilder {

    private val binder = ChirReferenceBinder(symbolTable)

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

    override fun registerDeclaration(declaration: ChirDeclaration): Boolean {
        return runCatching {
            registerDeclarationOrThrow(declaration)
        }.onFailure {
            diagnostics.report(ChirBuildError.InvalidGraph("failed to register declaration ${declaration.name}: ${it.message}"))
        }.isSuccess
    }

    override fun declareSymbol(symbol: ChirSymbol): Boolean {
        return runCatching {
            symbolTable.declare(symbol)
            context.registerSymbol(symbol)
        }.onFailure {
            diagnostics.report(ChirBuildError.DuplicateSymbol(symbol.name, symbol.semanticId, it.message ?: "duplicate symbol"))
        }.isSuccess
    }

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

    private fun registerDeclarationOrThrow(declaration: ChirDeclaration) {
        context.registerDeclaration(declaration)
        if (declaration is ChirFunctionDeclaration) {
            validateFunctionDeclaration(declaration)
        }
    }

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

data class UnboundChirReference(
    val referenceId: ChirSemanticId,
    val targetName: String,
)

data class BoundChirReference(
    val referenceId: ChirSemanticId,
    val targetDeclarationId: ChirSemanticId,
)
