package org.cangnova.cangjie.analysis.light.declarations

import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightCallableDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightClassLikeDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightExtendDeclaration

/**
 * 声明视图的内部文本渲染器。
 *
 * `analysis-tools` 与测试都会复用它，保证导出文本和断言口径一致。
 */
object CaLightDeclarationRenderer {
    /**
     * 渲染单个 light declaration 的一行文本。
     */
    fun render(declaration: CaLightDeclaration): String {
        return when (declaration) {
            is CaLightClassLikeDeclaration -> buildString {
                append("class ")
                append(declaration.classId?.asString() ?: declaration.name ?: "<anonymous>")
                if (declaration.typeParameters.isNotEmpty()) {
                    append(declaration.typeParameters.joinToString(prefix = "<", postfix = ">") { it.asString() })
                }
            }

            is CaLightCallableDeclaration -> buildString {
                append("callable ")
                append(declaration.callableId?.toString() ?: declaration.name ?: "<anonymous>")
                declaration.signature?.let { signature ->
                    append(" ")
                    append(signature.toString())
                }
            }

            is CaLightExtendDeclaration -> buildString {
                append("extend ")
                append(declaration.targetClassId?.asString() ?: declaration.name ?: declaration.extendId)
                if (declaration.typeParameters.isNotEmpty()) {
                    append(declaration.typeParameters.joinToString(prefix = "<", postfix = ">") { it.asString() })
                }
            }

            else -> buildString {
                append("package ")
                append(declaration.name ?: "<root>")
            }
        }
    }

    /**
     * 渲染一组 light declarations 的树形文本。
     */
    fun renderTree(declarations: Collection<CaLightDeclaration>): String {
        return declarations.joinToString(separator = System.lineSeparator()) { declaration ->
            renderNode(declaration, 0)
        }
    }

    /**
     * 递归渲染单个节点及其成员子树。
     */
    private fun renderNode(
        declaration: CaLightDeclaration,
        indent: Int,
    ): String {
        val prefix = " ".repeat(indent * 2)
        val rendered = prefix + render(declaration)
        val children = when (declaration) {
            is CaLightClassLikeDeclaration -> declaration.members
            is CaLightExtendDeclaration -> declaration.members
            else -> emptyList()
        }
        if (children.isEmpty()) {
            return rendered
        }

        return buildString {
            appendLine(rendered)
            children.forEachIndexed { index, member ->
                if (index > 0) appendLine()
                append(renderNode(member, indent + 1))
            }
        }.trimEnd()
    }
}
