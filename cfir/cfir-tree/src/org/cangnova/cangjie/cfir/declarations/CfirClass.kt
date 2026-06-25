package org.cangnova.cangjie.cfir.declarations

/**
 * CFIR class-like 声明的具体种类。
 */
enum class CfirClassKind {
    /** 普通 class 声明。 */
    CLASS,
    /** interface 声明。 */
    INTERFACE,
    /** struct 声明。 */
    STRUCT,
    /** enum 声明。 */
    ENUM,
}
