package org.cangnova.cangjie.parsing

/**
 * 声明参数的文法身份；对应官方 ParseParameterList 的作用域输入。
 * 由 parser 与 raw CFIR 共享的不可变语法数据，不依赖 PSI 实现。
 * 访问器只解析名字，与可省略类型的函数类型参数不是同一文法。
 * Lambda 的 ParseFuncParam 保持独立，不进入声明参数的注解/修饰符/宏入口。
 */
enum class CjParameterGrammar {
    REGULAR_TYPED,
    CLASS_PRIMARY,
    STRUCT_PRIMARY,
    ACCESSOR_NAME_ONLY;

    /** 只有 class/struct 的真实 primary 参数列表允许声明成员 let/var。 */
    val isPrimaryConstructor: Boolean
        get() = this == CLASS_PRIMARY || this == STRUCT_PRIMARY
}

/**
 * 名义类型/extend 的成员文法上下文，不保存 CFIR 身份。
 * declarationName 保留原始标识符拼写，为 null 只表示源码名字已破损或 extend 没有声明名。
 * 宏新 parser 只接收原生明确传入的作用域/primary 名，不继承宿主 common/abstract 检查状态。
 */
data class CjMemberParsingContext(
    val kind: Kind,
    val declarationName: String?,
    /** 真实名义声明关键字的词法来源；宏片段只有 scope/name 时明确没有 AST 锚点。 */
    val declarationOrigin: CjParsedSourceOrigin? = null,
) {
    init {
        require(declarationName == null || declarationName.isNotEmpty()) {
            "A missing declaration name must be represented explicitly, not by an empty spelling."
        }
    }

    enum class Kind { CLASS, STRUCT, INTERFACE, ENUM, EXTEND }

    /** 当前名义body自身的原生声明作用域；构造器参数和普通正文另有明确入口。 */
    val declarationScope: CjScopeKind
        get() = when (kind) {
            Kind.CLASS -> CjScopeKind.CLASS_BODY
            Kind.STRUCT -> CjScopeKind.STRUCT_BODY
            Kind.INTERFACE -> CjScopeKind.INTERFACE_BODY
            Kind.ENUM -> CjScopeKind.ENUM_BODY
            Kind.EXTEND -> CjScopeKind.EXTEND_BODY
        }
}

/** 原生 curPrimaryDeclIdent 与 isRaw 标志；用于词法身份，不是可被局部清空的成员文法。 */
data class CjPrimaryParsingName(val identifier: String, val isRaw: Boolean) {
    init {
        require(identifier.isNotEmpty())
    }

    val rawIdentifier: String get() = if (isRaw) "`$identifier`" else identifier
}
