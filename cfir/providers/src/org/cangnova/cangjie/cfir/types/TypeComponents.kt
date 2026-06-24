package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.languageVersionSettings

/**
 * 类型系统相关 session 组件集合。
 *
 * @property session 当前组件绑定的 CFIR session。
 */
class TypeComponents(val session: CfirSession) : CfirSessionComponent {
    /**
     * 当前 session 使用的 cone inference/type context。
     */
    val typeContext: ConeInferenceContext = object : ConeInferenceContext {
        /**
         * 将匿名上下文回指到外层 session。
         */
        override val session: CfirSession
            get() = this@TypeComponents.session
    }

    /**
     * 当前 session 使用的类型近似器。
     */
    val typeApproximator: ConeTypeApproximator = ConeTypeApproximator(typeContext, session.languageVersionSettings)
}

/**
 * 当前 session 注册的类型组件集合。
 */
private val CfirSession.typeComponents: TypeComponents by CfirSession.sessionComponentAccessor()

/**
 * 当前 session 的类型上下文。
 */
val CfirSession.typeContext: ConeInferenceContext
    get() = typeComponents.typeContext

/**
 * 当前 session 的类型近似器。
 */
val CfirSession.typeApproximator: ConeTypeApproximator
    get() = typeComponents.typeApproximator
