package org.cangnova.cangjie.type

import org.cangnova.cangjie.type.model.CangJieTypeMarker

/**
 * 类型检查流程中的类型精化入口。
 *
 * 精化用于在既有类型标记基础上补充上下文敏感信息，使后续比较看到更精确的类型形态。
 */
abstract class AbstractTypeRefiner {
    /**
     * 返回参与后续类型运算的精化结果。
     */
    abstract fun refineType(type: CangJieTypeMarker): CangJieTypeMarker

    /**
     * 默认精化器，不改变输入类型。
     */
    object Default : AbstractTypeRefiner() {
        /**
         * 直接返回原始类型，表示当前上下文不需要额外精化。
         */
        override fun refineType(type: CangJieTypeMarker): CangJieTypeMarker = type
    }
}
