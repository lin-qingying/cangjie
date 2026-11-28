package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.resolve.CfirResolutionSnapshot

/** 真实模式节点的可变引用归属外壳事务，恢复时保留 provider 索引中的绑定身份。 */
internal object PatternStateKeepers {
    val PATTERN: StateKeeper<CfirPattern, Unit> = stateKeeper { builder, pattern, context ->
        builder.add({ CfirPatternMutableState.capture(it) }, { _, state -> state?.restore() })
        when (pattern) {
            is CfirBindingPattern -> builder.entity(pattern.nestedPattern, PATTERN, context)
            is CfirTuplePattern -> builder.entityList(pattern.elements, PATTERN, context)
            is CfirEnumPattern -> builder.entityList(pattern.arguments, PATTERN, context)
            is CfirOrPattern -> builder.entityList(pattern.alternatives, PATTERN, context)
            else -> Unit
        }
    }

    /** 值表达式会原地改写 callee/receiver/实参和子类型，必须保存完整图而不是只存根引用。 */
    val BODY: StateKeeper<CfirPattern, Unit> = stateKeeper { builder, _, _ ->
        builder.add({ CfirResolutionSnapshot.capture(it) }, { _, snapshot -> snapshot.restore() })
    }
}
