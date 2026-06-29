package org.cangnova.cangjie.chir.core.printer

import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.model.ChirPackage

/**
 * CHIR 包结构摘要检查器。
 */
object ChirInspector {
    /**
     * 生成稳定 JSON 风格的包结构摘要。
     */
    fun inspect(chirPackage: ChirPackage): String {
        val sortedModules = chirPackage.modules.sortedBy { it.semanticId.value }
        val functionCount = sortedModules.sumOf { module -> module.declarations.count { it is ChirFunctionDeclaration } }
        val blockCount = sortedModules.sumOf { module ->
            module.declarations.sumOf { declaration ->
                (declaration as? ChirFunctionDeclaration)?.blocks?.size ?: 0
            }
        }
        val expressionCount = sortedModules.sumOf { module ->
            module.declarations.sumOf { declaration ->
                (declaration as? ChirFunctionDeclaration)?.blocks?.sumOf { it.expressions.size } ?: 0
            }
        }

        return buildString {
            appendLine("{")
            appendLine("  \"packageId\": \"${escape(chirPackage.semanticId.value)}\",")
            appendLine("  \"packageName\": \"${escape(chirPackage.name)}\",")
            appendLine("  \"accessLevel\": \"${chirPackage.accessLevel}\",")
            appendLine("  \"packageInitFunctionId\": ${chirPackage.packageInitFunctionId?.let { "\"${escape(it.value)}\"" } ?: "null"},")
            appendLine("  \"packageLiteralInitFunctionId\": ${chirPackage.packageLiteralInitFunctionId?.let { "\"${escape(it.value)}\"" } ?: "null"},")
            appendLine("  \"moduleCount\": ${sortedModules.size},")
            appendLine("  \"globalVariableCount\": ${chirPackage.members.globalVariables.size},")
            appendLine("  \"globalFunctionCount\": ${chirPackage.members.globalFunctions.size},")
            appendLine("  \"importedVariableCount\": ${chirPackage.members.importedVariables.size},")
            appendLine("  \"importedFunctionCount\": ${chirPackage.members.importedFunctions.size},")
            appendLine("  \"typeDefinitionCount\": ${chirPackage.typeDefinitions.size},")
            appendLine("  \"importedTypeDefinitionCount\": ${chirPackage.importedTypeDefinitions.size},")
            appendLine("  \"functionCount\": $functionCount,")
            appendLine("  \"blockCount\": $blockCount,")
            appendLine("  \"expressionCount\": $expressionCount,")
            appendLine("  \"modules\": [")
            sortedModules.forEachIndexed { moduleIndex, module ->
                val functions = module.declarations
                    .asSequence()
                    .mapNotNull { it as? ChirFunctionDeclaration }
                    .sortedBy { it.semanticId.value }
                    .toList()

                appendLine("    {")
                appendLine("      \"id\": \"${escape(module.semanticId.value)}\",")
                appendLine("      \"name\": \"${escape(module.name)}\",")
                appendLine("      \"declarationCount\": ${module.declarations.size},")
                appendLine("      \"functionCount\": ${functions.size},")
                appendLine("      \"functions\": [")
                functions.forEachIndexed { functionIndex, function ->
                    val blockIds = function.blocks
                        .sortedBy { it.semanticId.value }
                        .joinToString(separator = ",") { "\"${escape(it.semanticId.value)}\"" }
                    val functionExpressionCount = function.blocks.sumOf { it.expressions.size }
                    appendLine("        {")
                    appendLine("          \"id\": \"${escape(function.semanticId.value)}\",")
                    appendLine("          \"name\": \"${escape(function.name)}\",")
                    appendLine("          \"returnType\": \"${escape(function.returnType.renderName)}\",")
                    appendLine("          \"entryBlockId\": \"${escape(function.entryBlockId.value)}\",")
                    appendLine("          \"parameterCount\": ${function.parameters.size},")
                    appendLine("          \"blockCount\": ${function.blocks.size},")
                    appendLine("          \"expressionCount\": $functionExpressionCount,")
                    appendLine("          \"blockIds\": [$blockIds]")
                    append("        }")
                    appendLine(if (functionIndex == functions.lastIndex) "" else ",")
                }
                appendLine("      ]")
                append("    }")
                appendLine(if (moduleIndex == sortedModules.lastIndex) "" else ",")
            }
            appendLine("  ]")
            append("}")
        }
    }

    /**
     * 转义 JSON 字符串片段。
     */
    private fun escape(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
}
