package org.cangnova.cangjie.parsing

/** 原生 TraceScopeInChain 的实际词法 owner；ScopeKind 和用于显示的 AST owner 分别保存。 */
data class CjParsingScopeOwner(val kind: Kind, val origin: CjParsedSourceOrigin) {
    enum class Kind(val nameText: String, val bodyText: String) {
        CLASS("class", "class body"),
        STRUCT("struct", "struct body"),
        INTERFACE("interface", "interface body"),
        ENUM("enum", "enum body"),
        EXTEND("extend", "extend body"),
        FUNCTION("function", "function body"),
        MAIN("main function", "main function body"),
        MACRO("macro", "macro body"),
        GETTER("getter", "getter body"),
        SETTER("setter", "setter body"),
        PRIMARY_CONSTRUCTOR("primary constructor", "primary constructor"),
        BLOCK("block", "block"),
        INTERPOLATION("string interpolation", "string interpolation"),
    }
}
