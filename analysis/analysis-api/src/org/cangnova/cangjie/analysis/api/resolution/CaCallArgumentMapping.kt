package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

/**
 * 单个源码实参与形参的映射结果。
 */
interface CaCallArgumentMapping : CaLifetimeOwner {
    val argumentIndex: Int

    val parameterName: Name?

    val parameterType: CaType?
}
