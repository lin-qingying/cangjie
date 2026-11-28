package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import java.util.Collections

/** 声明 parser 与宏重解析共享的修饰符身份，不包含 CFIR status 或解析阶段状态。 */
enum class CjModifierKind {
    ABSTRACT, MUT, PUBLIC, PRIVATE, INTERNAL, PROTECTED, OPERATOR, FOREIGN, CONST, UNSAFE, SEALED, REDEF, OVERRIDE, OPEN, STATIC,
    INOUT, COMMON, SPECIFIC, PLATFORM,
}

/** 原生直接 AST parent 的文法环境；逗号与 FuncArg 的 inout 处理不由 scope 推导。 */
enum class CjMacroFragmentParent {
    OTHER, ARRAY_LITERAL, TUPLE_LITERAL, FUNCTION_ARGUMENT, FUNCTION_PARAMETER_LIST;

    val isConnectedByComma: Boolean
        get() = this == ARRAY_LITERAL || this == TUPLE_LITERAL || this == FUNCTION_ARGUMENT
}

/**
 * 原调用传给首个声明 root 的不可变前缀；每项保留首次捕获来源与当前已证明的源范围。
 * 修饰符集合只用于属性查询，不能代替原 modifier 对象或其诊断位置。
 */
class CjMacroDeclarationPrefix(
    sourceModifiers: List<CjModifierSourceFact>,
    annotations: List<CjAnnotationSourceFact>,
) {
    val sourceModifiers: List<CjModifierSourceFact> = Collections.unmodifiableList(sourceModifiers.toList())
    val annotations: List<CjAnnotationSourceFact> = Collections.unmodifiableList(annotations.toList())
    val modifiers: Set<CjModifierKind> = Collections.unmodifiableSet(this.sourceModifiers.mapTo(linkedSetOf()) { it.kind })
    val annotationKinds: Set<BuiltInAnnotationKind> = Collections.unmodifiableSet(this.annotations.mapNotNullTo(linkedSetOf()) { it.kind })

    init {
        require(this.sourceModifiers.map { it.kind }.distinct().size == this.sourceModifiers.size) {
            "An inherited modifier set must retain one source object for each native kind."
        }
    }

    override fun equals(other: Any?): Boolean = other is CjMacroDeclarationPrefix &&
        sourceModifiers == other.sourceModifiers && annotations == other.annotations

    override fun hashCode(): Int = 31 * sourceModifiers.hashCode() + annotations.hashCode()

    companion object {
        val EMPTY: CjMacroDeclarationPrefix = CjMacroDeclarationPrefix(emptyList(), emptyList())
    }
}

/**
 * 官方 ParseNodes 的不可变文法输入。Nodes 可返回声明/表达式混合列表；ParameterSlice
 * 来自真实参数替换签名，逐项调用对应参数文法，不创建参数列表或 callable 外壳。
 * Annotation 只解析 annotation root，不借宿主声明构造语法容器。原调用 ASTKind 和
 * 实际 CFIR 替换目标不属于 parser seed，分别由 construction 保存。
 */
sealed interface CjMacroFragmentContext {
    val outerDeclIdent: String?

    data class Nodes(
        val scope: MacroParsingScope,
        val parent: CjMacroFragmentParent,
        override val outerDeclIdent: String?,
        val declarationPrefix: CjMacroDeclarationPrefix,
    ) : CjMacroFragmentContext {
        init {
            require(outerDeclIdent == null || outerDeclIdent.isNotEmpty())
        }
    }

    data class ParameterSlice(
        val grammar: CjParameterGrammar,
        override val outerDeclIdent: String?,
    ) : CjMacroFragmentContext {
        init {
            require(outerDeclIdent == null || outerDeclIdent.isNotEmpty())
        }
    }

    data class Annotation(
        override val outerDeclIdent: String?,
    ) : CjMacroFragmentContext {
        init {
            require(outerDeclIdent == null || outerDeclIdent.isNotEmpty())
        }
    }
}
