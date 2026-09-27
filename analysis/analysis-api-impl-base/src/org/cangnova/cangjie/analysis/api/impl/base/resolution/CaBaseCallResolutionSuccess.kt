package org.cangnova.cangjie.analysis.api.impl.base.resolution

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaCallResolutionSuccess

/** Analysis API 实现基类中的成功调用解析尝试。 */
@CaImplementationDetail
class CaBaseCallResolutionSuccess(
    override val call: CaCall,
) : CaCallResolutionSuccess {
    override val token: CaLifetimeToken get() = call.token
}
