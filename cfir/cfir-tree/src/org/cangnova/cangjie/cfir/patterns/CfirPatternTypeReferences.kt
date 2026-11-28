package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.types.CfirTypeRef

/** 声明多个类型引用的模式节点共享的可替换类型引用契约。 */
interface CfirPatternTypeReferences {
    /** 当前模式声明的源码类型引用。 */
    val typeRefs: List<CfirTypeRef>

    /** 恢复候选解析前捕获的类型引用。 */
    fun replaceTypeRefs(newTypeRefs: List<CfirTypeRef>)
}
