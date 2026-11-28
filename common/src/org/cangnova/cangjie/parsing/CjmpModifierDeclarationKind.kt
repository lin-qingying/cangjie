package org.cangnova.cangjie.parsing

/** 官方修饰符检查的声明文法种类；解析结果与消费方共用此身份，不依赖 PSI 类名猜测。 */
enum class CjmpModifierDeclarationKind {
    CLASS,
    STRUCT,
    ENUM,
    INTERFACE,
    EXTEND,
    FUNCTION,
    VARIABLE,
    PROPERTY,
    PRIMARY_CONSTRUCTOR,
    SECONDARY_CONSTRUCTOR,
    ENUM_CONSTRUCTOR,
    FINALIZER,
    MAIN,
    TYPE_ALIAS,
    MACRO,
    PACKAGE,
    OTHER,
}
