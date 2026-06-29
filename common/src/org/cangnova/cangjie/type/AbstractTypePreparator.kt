package org.cangnova.cangjie.type

import org.cangnova.cangjie.type.model.CangJieTypeMarker

/**
 * 类型检查流程中的类型预处理入口。
 *
 * 预处理发生在具体检查或比较之前，用于将类型标记规整到后续阶段期望的形态。
 */
abstract class AbstractTypePreparator {
    /**
     * 返回参与后续类型运算的预处理结果。
     */
    abstract fun prepareType(type: CangJieTypeMarker): CangJieTypeMarker

    /**
     * 默认预处理器，不改变输入类型。
     */
    object Default : AbstractTypePreparator() {
        /**
         * 直接返回原始类型，表示当前上下文不需要额外预处理。
         */
        override fun prepareType(type: CangJieTypeMarker): CangJieTypeMarker = type
    }
}
