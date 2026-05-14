package org.cangnova.cangjie.analysis.api.resolution

/**
 * 调用的"主体形态"分类。
 *
 * 当前仅区分函数调用,后续可按需扩展(例如属性访问、操作符约定调用等)。
 */
enum class CaCallKind {
    /**
     * 函数调用。
     */
    FUNCTION,
}
