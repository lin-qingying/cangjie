package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.IntersectionTypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * CFIR Cone 类型构造器 marker。
 *
 * 该接口把仓颉 Cone 层自己的构造器接入公共类型系统的 [TypeConstructorMarker]。
 */
sealed interface ConeTypeConstructorMarker : TypeConstructorMarker

/**
 * 存根类型构造器。
 *
 * @property variable 该存根关联的类型变量。
 * @property isTypeVariableInSubtyping 当前存根是否来自子类型关系中的类型变量。
 * @property isForFixation 当前存根是否用于类型变量固定阶段。
 */
data class ConeStubTypeConstructor(
    /** 该存根关联的类型变量。 */
    val variable:  ConeTypeVariable,
    /** 当前存根是否来自子类型关系中的类型变量。 */
    val isTypeVariableInSubtyping: Boolean,
    /** 当前存根是否用于类型变量固定阶段。 */
    val isForFixation: Boolean = false,
) : ConeTypeConstructorMarker {
    /**
     * 返回稳定的调试文本。
     */
    override fun toString(): String {
        return "Stub(${variable.typeConstructor.debugName})"
    }
}
