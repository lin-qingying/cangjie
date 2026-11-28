package org.cangnova.cangjie.parsing

/** 官方 v1.1.3 Parser.h 的声明/语句入口；与表达式入口保持联合类型身份。 */
enum class CjScopeKind {
    TOPLEVEL,
    CLASS_BODY,
    INTERFACE_BODY,
    STRUCT_BODY,
    EXTEND_BODY,
    FUNC_BODY,
    MACRO_BODY,
    PRIMARY_CONSTRUCTOR_BODY_FOR_CLASS,
    PRIMARY_CONSTRUCTOR_BODY_FOR_STRUCT,
    PROP_MEMBER_SETTER_BODY,
    PROP_MEMBER_GETTER_BODY,
    ENUM_BODY,
    ENUM_CONSTRUCTOR,
    PRIMARY_CONSTRUCTOR_FUNC_PARAM,
    MAIN_BODY,
    UNKNOWN_SCOPE,
}

/** 官方 v1.1.3 ParserImpl.h 的表达式入口，不能由宏的 CFIR 后代推导。 */
enum class CjExprKind {
    ALL,
    VAR_INIT,
    INDEX_EXPR,
    IF_COND_EXPR,
    WHILE_COND_EXPR,
    LET_PATTERN,
    LEFT_EXPR,
    EXPR_IN_TUPLE,
    EXPR_IN_IF_COND_TUPLE,
    EXPR_IN_WHILE_COND_TUPLE,
    EXPR_IN_ARRAY,
    EXPR_IN_CALLSUFFIX,
    EXPR_IN_ANNOTATION,
    UNKNOWN_EXPR,
}

/** 对位原生 variant<ScopeKind, ExprKind>，两个 UNKNOWN 也不是同一个入口。 */
sealed interface MacroParsingScope {
    data class Declaration(val kind: CjScopeKind) : MacroParsingScope
    data class Expression(val kind: CjExprKind) : MacroParsingScope
}

/**
 * 一次真实 AT occurrence 的语法含义。坐标由根事务保存，不参与语义判等。
 * 直接 parent 与 scope 独立保存；逗号属性仅由 ARRAY_LIT/TUPLE_LIT/FUNC_ARG 决定。
 * outerDeclIdent 对位原生 curPrimaryDeclIdent，是最近 class/struct 的语义名；不包含反引号。
 * 原始拼写身份属于 parser 的独立词法状态，不能与宏展开新 parser 接收的这个字段混同。
 */
data class MacroParsingFacts(
    val scope: MacroParsingScope,
    val parent: CjMacroFragmentParent,
    val outerDeclIdent: String?,
    /** 原宏节点自身持有的重解析前缀，与 invocation.decl 的前缀分开保存。 */
    val declarationPrefix: CjMacroDeclarationPrefix = CjMacroDeclarationPrefix.EMPTY,
) {
    val isConnectedByComma: Boolean get() = parent.isConnectedByComma

    init {
        require(outerDeclIdent == null || outerDeclIdent.isNotEmpty())
    }
}
