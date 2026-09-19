package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.name.FqName

/**
 * 当前编译 session 明确授权可省略 import 的系统注解集合。
 *
 * 该事实属于平台/编译配置，不属于 CJO 文件路径。普通库默认不授权任何
 * 系统注解；OHOS 或其它平台入口必须在创建 session 时显式注册实现。
 */
public interface CfirImplicitSystemAnnotationsProvider : CfirSessionComponent {
    /** 已获得平台授权的系统注解完整 ClassId。 */
    public val annotations: Set<FqName>
}

/** 默认 session 不启用隐式系统注解。 */
public object DefaultCfirImplicitSystemAnnotationsProvider : CfirImplicitSystemAnnotationsProvider {
    override val annotations: Set<FqName> = emptySet()
}

/** 工厂/平台入口显式注入的系统注解授权。 */
public data class ConfiguredCfirImplicitSystemAnnotationsProvider(
    override val annotations: Set<FqName>,
) : CfirImplicitSystemAnnotationsProvider

/** 当前 session 的显式隐式注解授权。 */
public val CfirSession.implicitSystemAnnotations: Set<FqName>
    get() = implicitSystemAnnotationsProvider.annotations

/** session-owned provider；未注册时使用安全的空实现。 */
public val CfirSession.implicitSystemAnnotationsProvider: CfirImplicitSystemAnnotationsProvider by
    CfirSession.sessionComponentAccessorWithDefault(DefaultCfirImplicitSystemAnnotationsProvider)
