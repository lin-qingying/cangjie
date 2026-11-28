package org.cangnova.cangjie.cfir.declarations

/**
 * Raw CFIR 中保留的可调用体存在性事实。
 *
 * STATUS resolve 使用它区分有体 override 与声明文件无体 override；`UNKNOWN` 表示来源未提供事实。
 */
enum class CfirBodyPresence {
    /** 声明有函数体或属性实现体。 */
    PRESENT,

    /** 声明没有函数体或属性实现体。 */
    ABSENT,

    /** 声明来源没有保留函数体事实。 */
    UNKNOWN,
}
