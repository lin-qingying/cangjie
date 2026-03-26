package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.IGNORE_LEAKED_INTERNAL_TYPES
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

class CfirResolvedTypesVerifier(
    testServices: TestServices,
) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CfirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        val ignored = IGNORE_LEAKED_INTERNAL_TYPES in module.directives
        var leakedTypesDetected = false
        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            if (CfirDiagnosticsDirectives.VERIFY_RESOLVED_TYPES !in currentModule.directives) continue

            part.firFilesByTestFile.forEach { (testFile, cfirFile) ->
                val problems = collectProblems(cfirFile)
                if (problems.isNotEmpty()) {
                    leakedTypesDetected = true
                    if (ignored) return@forEach
                    testServices.assertions.fail {
                        renderProblems(testFile.relativePath, problems)
                    }
                }
            }
        }

        if (ignored && !leakedTypesDetected) {
            testServices.assertions.fail {
                "There is no leaked internal types in test. Please remove ${IGNORE_LEAKED_INTERNAL_TYPES.name} directive"
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) = Unit

    private fun renderProblems(relativePath: String, problems: List<String>): String {
        return buildString {
            appendLine("VERIFY_RESOLVED_TYPES report for $relativePath")
            if (problems.isEmpty()) {
                append("<no problems>")
            } else {
                problems.forEach { appendLine("- $it") }
            }
        }.trimEnd()
    }

    private fun collectProblems(file: CfirFile): List<String> {
        val collector = ProblemCollector()
        file.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                when (element) {
                    is CfirExpression -> checkExpression(element, collector)
                    is CfirImplicitTypeRef -> Unit
                    is CfirResolvedTypeRef -> checkResolvedTypeRef(element, collector)
                    is CfirErrorTypeRef -> collector.errorTypeRefs += "${element.renderOwner()}: ${element.diagnostic.reason}"
                }
                element.acceptChildren(this)
            }
        })
        return collector.renderProblems()
    }

    private fun checkExpression(expression: CfirExpression, collector: ProblemCollector) {
        if (expression is CfirLazyExpression || expression is CfirErrorExpression) return
        val coneType = expression.coneTypeOrNull
        if (coneType == null) {
            collector.missingExpressionTypes += expression.renderOwner()
            return
        }
        checkConeType(coneType, expression.renderOwner(), collector)
    }

    private fun checkResolvedTypeRef(typeRef: CfirResolvedTypeRef, collector: ProblemCollector) {
        checkConeType(typeRef.coneType, typeRef.renderOwner(), collector)
    }

    private fun checkConeType(type: ConeCangJieType, owner: String, collector: ProblemCollector) {
        when (type) {
            is ConeTypeVariableType -> collector.typeVariableTypes += "$owner: $type"
            is ConeStubType -> collector.stubTypes += "$owner: $type"
            else -> if (type.isError) {
                collector.errorConeTypes += "$owner: $type"
            }
        }

        when (type) {
            is ConeFuncType -> {
                type.parameterTypes.forEachIndexed { index, parameterType ->
                    checkConeType(parameterType, "$owner parameter[$index]", collector)
                }
                checkConeType(type.returnType, "$owner return", collector)
            }
            is ConeClassLikeType -> type.typeArguments.forEachIndexed { index, argument ->
                checkConeType(argument.type, "$owner argument[$index]", collector)
            }
            is ConeStructType -> type.typeArguments.forEachIndexed { index, argument ->
                checkConeType(argument.type, "$owner argument[$index]", collector)
            }
            is ConeEnumType -> type.typeArguments.forEachIndexed { index, argument ->
                checkConeType(argument.type, "$owner argument[$index]", collector)
            }
            else -> Unit
        }
    }

    private fun CfirElement.renderOwner(): String {
        return this::class.simpleName ?: "<anonymous>"
    }

    private class ProblemCollector {
        val missingExpressionTypes = linkedSetOf<String>()
        val typeVariableTypes = linkedSetOf<String>()
        val stubTypes = linkedSetOf<String>()
        val errorTypeRefs = linkedSetOf<String>()
        val errorConeTypes = linkedSetOf<String>()

        fun renderProblems(): List<String> {
            return buildList {
                appendCategory("expressions without resolved type", missingExpressionTypes)
                appendCategory("leaked type variable types", typeVariableTypes)
                appendCategory("leaked stub types", stubTypes)
                appendCategory("error type refs", errorTypeRefs)
                appendCategory("error cone types", errorConeTypes)
            }
        }

        private fun MutableList<String>.appendCategory(header: String, values: Set<String>) {
            if (values.isEmpty()) return
            add("$header:")
            values.sorted().forEach { add("  $it") }
        }
    }
}
