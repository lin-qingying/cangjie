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
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.IGNORE_LEAKED_INTERNAL_TYPES
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 表示 `CfirResolvedTypesVerifier`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirResolvedTypesVerifier(
    testServices: TestServices,
) : CfirAnalysisHandler(testServices) {
    /**
     * 保存 `directiveContainers`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CfirDiagnosticsDirectives)

    /**
     * 执行 `processModule` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
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

    /**
     * 执行 `processAfterAllModules` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) = Unit

    /**
     * 提供 `renderProblems` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
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

    /**
     * 提供 `collectProblems` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
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

    /**
     * 提供 `checkExpression` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun checkExpression(expression: CfirExpression, collector: ProblemCollector) {
        if (expression is CfirLazyExpression || expression is CfirErrorExpression) return
        val coneType = expression.coneTypeOrNull
        if (coneType == null) {
            collector.missingExpressionTypes += expression.renderOwner()
            return
        }
        checkConeType(coneType, expression.renderOwner(), collector)
    }

    /**
     * 提供 `checkResolvedTypeRef` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun checkResolvedTypeRef(typeRef: CfirResolvedTypeRef, collector: ProblemCollector) {
        checkConeType(typeRef.coneType, typeRef.renderOwner(), collector)
    }

    /**
     * 提供 `checkConeType` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun checkConeType(type: ConeCangJieType, owner: String, collector: ProblemCollector) {
        when (type) {
            is ConeTypeVariableType -> collector.typeVariableTypes += "$owner: $type"
            is ConeStubType -> collector.stubTypes += "$owner: $type"
            else -> if (type.isError) {
                collector.errorConeTypes += "$owner: $type"
            }
        }

        when (type) {
            is ConeFunctionType -> {
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

    /**
     * 提供 `renderOwner` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun CfirElement.renderOwner(): String {
        return this::class.simpleName ?: "<anonymous>"
    }

    /**
     * 表示 `ProblemCollector`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
     */
    private class ProblemCollector {
        /**
         * 保存 `missingExpressionTypes`，供CFIR 前端测试在测试执行期间读取或传递。
         */
        val missingExpressionTypes = linkedSetOf<String>()
        /**
         * 保存 `typeVariableTypes`，供CFIR 前端测试在测试执行期间读取或传递。
         */
        val typeVariableTypes = linkedSetOf<String>()
        /**
         * 保存 `stubTypes`，供CFIR 前端测试在测试执行期间读取或传递。
         */
        val stubTypes = linkedSetOf<String>()
        /**
         * 保存 `errorTypeRefs`，供CFIR 前端测试在测试执行期间读取或传递。
         */
        val errorTypeRefs = linkedSetOf<String>()
        /**
         * 保存 `errorConeTypes`，供CFIR 前端测试在测试执行期间读取或传递。
         */
        val errorConeTypes = linkedSetOf<String>()

        /**
         * 执行 `renderProblems` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
         */
        fun renderProblems(): List<String> {
            return buildList {
                appendCategory("expressions without resolved type", missingExpressionTypes)
                appendCategory("leaked type variable types", typeVariableTypes)
                appendCategory("leaked stub types", stubTypes)
                appendCategory("error type refs", errorTypeRefs)
                appendCategory("error cone types", errorConeTypes)
            }
        }

        /**
         * 提供 `appendCategory` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
         */
        private fun MutableList<String>.appendCategory(header: String, values: Set<String>) {
            if (values.isEmpty()) return
            add("$header:")
            values.sorted().forEach { add("  $it") }
        }
    }
}
