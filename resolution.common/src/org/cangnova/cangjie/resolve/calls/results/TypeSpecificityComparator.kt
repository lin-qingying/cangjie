/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.results

import org.cangnova.cangjie.type.model.*


/**
 * 比较两个类型在重载解析中的特化程度。
 */
interface TypeSpecificityComparator {
    /**
     * 判断 [specific] 是否确定比 [general] 更不特化。
     */
    fun isDefinitelyLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean

    /**
     * 不提供任何额外特化判断的空比较器。
     */
    object NONE : TypeSpecificityComparator {
        /**
         * 空比较器始终认为不存在“确定更不特化”关系。
         */
        override fun isDefinitelyLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker) = false
    }

    /**
     * 将多个类型特化比较器组合为一个比较器。
     */
    class Composed(
        /** 按顺序参与判断的子比较器。 */
        val comparators: List<TypeSpecificityComparator>
    ) : TypeSpecificityComparator {
        /**
         * 任一子比较器认为更不特化时返回 true。
         */
        override fun isDefinitelyLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean {
            return comparators.any { it.isDefinitelyLessSpecific(specific, general) }
        }
    }
}
