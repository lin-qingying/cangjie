package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.parsing.CjMacroFragmentParseInfo
import java.util.Collections

/** 保存 Macro ParseNodes 直接产生的 CFIR 根节点，construction 必须保留根类型与原始顺序。 */
class MacroFragmentPayload(
    roots: List<Root>,
    val parsingInfo: CjMacroFragmentParseInfo,
) {
    val roots: List<Root> = Collections.unmodifiableList(roots.toList())

    init {
        require(this.roots.map { it.index } == this.roots.indices.toList()) {
            "Macro fragment roots must retain their contiguous parser order."
        }
    }

    sealed interface Root {
        val index: Int

        data class Declaration(override val index: Int, val declaration: CfirDeclaration) : Root
        data class Expression(override val index: Int, val expression: CfirExpression) : Root
        data class Parameter(override val index: Int, val parameter: CfirValueParameter) : Root
    }

    companion object {
        /** 测试替身与显式无根解析结果共用不可变空产物。 */
        val EMPTY = MacroFragmentPayload(
            emptyList(),
            CjMacroFragmentParseInfo(emptySet(), inheritedPrefixRootIndex = null, hasInvalidRoot = false,
                inheritedPrefix = null, inheritedPrefixRoleCheck = null, parsingContext = null, entryFileRole = null),
        )

        /** 为不运行 parser 的 construction 测试替身封装单个具型 CFIR 节点。 */
        fun single(element: CfirElement): MacroFragmentPayload = when (element) {
            is CfirValueParameter -> MacroFragmentPayload(listOf(Root.Parameter(0, element)), EMPTY.parsingInfo)
            is CfirDeclaration -> MacroFragmentPayload(listOf(Root.Declaration(0, element)), EMPTY.parsingInfo)
            is CfirExpression -> MacroFragmentPayload(listOf(Root.Expression(0, element)), EMPTY.parsingInfo)
            else -> error("Unsupported macro fragment payload root: ${element::class.qualifiedName}")
        }
    }
}
